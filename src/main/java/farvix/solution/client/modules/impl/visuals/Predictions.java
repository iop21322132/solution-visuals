package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.api.util.render.ProjectionUtility;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(name = "Predictions", category = ModuleCategory.VISUALS,
        description = "Траектория снарядов")
public class Predictions extends Module implements QuickImports {

    // ── Settings ──────────────────────────────────────────────────────────────
    public final SliderSetting lineWidth = new SliderSetting(
            "Толщина линии", this, 2.0f, 0.5f, 6.0f, 0.5f);

    public final ColorSetting color = new ColorSetting(
            "Цвет", this, new FixColor(100, 220, 255, 255).getRGB());

    // ── State ─────────────────────────────────────────────────────────────────
    private record LandingPoint(Vec3d pos, net.minecraft.util.math.Direction side, int ticks, int color) {}
    private final List<LandingPoint> landingPoints = new CopyOnWriteArrayList<>();

    // Cache: UUID → last known trajectory (points + landing + last velocity for extrapolation)
    private record CachedTrajectory(List<Vec3d> points, LandingPoint landing, long lastSeen,
                                     Vec3d lastPos, Vec3d lastVel, double drag, double gravity) {}
    private final java.util.Map<java.util.UUID, CachedTrajectory> trajectoryCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_MS = 10_000; // keep for 10s after disappearing

    private static final double MIN_SPEED = 0.05;
    private static final Identifier CIRCLE_TEX =
            Identifier.of("solution", "textures/circle.png");

    @Override
    public void onDisable() {
        super.onDisable();
        trajectoryCache.clear();
        landingPoints.clear();
    }

    // ── 3D рендер ─────────────────────────────────────────────────────────────

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.player == null || mc.world == null) return;

        landingPoints.clear();

        java.awt.Color c = new java.awt.Color(color.get(), true);
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue();

        int coreColor  = new FixColor(r, g, b, 230).getRGB();
        int glowColor  = new FixColor(r, g, b, 60).getRGB();

        MatrixStack ms = e.getMatrices();
        ms.push();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();

        float lw = lineWidth.getValue() * 0.01f;

        Camera cam2 = mc.gameRenderer.getCamera();
        RenderSystem.setShaderTexture(0, Identifier.of("solution", "textures/hud/bloom.png"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        final long now = System.currentTimeMillis();

        // ── Предсказание снаряда в руке (до броска) ───────────────────────────
        net.minecraft.item.ItemStack held = mc.player.getMainHandStack();
        boolean isThrowable = (held.getItem() instanceof net.minecraft.item.EnderPearlItem
                || held.getItem() instanceof net.minecraft.item.SnowballItem
                || held.getItem() instanceof net.minecraft.item.EggItem)
                && !(held.getItem() instanceof net.minecraft.item.PotionItem)
                && !(held.getItem() instanceof net.minecraft.item.ExperienceBottleItem);
        boolean isBow = held.getItem() instanceof net.minecraft.item.BowItem
                || held.getItem() instanceof net.minecraft.item.CrossbowItem;
        boolean isTrident = held.getItem() instanceof net.minecraft.item.TridentItem;

        // Для метательных (жемчуг, снежок, яйцо) — скрываем когда летит
        // Для лука/трезубца — показываем пока снаряд в воздухе
        boolean hasActiveArrow = false;
        boolean hasActiveThrowable = false;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof net.minecraft.entity.projectile.ProjectileEntity proj
                    && proj.getOwner() == mc.player) {
                // Считаем активным только если снаряд ещё движется (не приземлился)
                boolean isMoving = entity.getVelocity().lengthSquared() > 0.001;
                if (entity instanceof ArrowEntity || entity instanceof TridentEntity) {
                    if (isMoving) hasActiveArrow = true;
                } else {
                    if (isMoving) hasActiveThrowable = true;
                }
            }
        }

        boolean showPreview = (isThrowable && !hasActiveThrowable)
                || ((isBow || isTrident) && !hasActiveArrow); // лук/трезубец — только пока не выстрелил

        if (showPreview) {
            // Параметры снаряда
            double drag, gravity, speed;
            if (isThrowable) {
                drag = 0.99; gravity = 0.03; speed = 1.5;
            } else if (isTrident) {
                drag = 0.99; gravity = 0.05; speed = 2.5;
            } else {
                // Лук — скорость зависит от натяжения
                drag = 0.99; gravity = 0.05; speed = 3.0;
            }

            // Начальная позиция — интерполированные глаза игрока
            float td = mc.getRenderTickCounter().getTickDelta(false);
            double ix = net.minecraft.util.math.MathHelper.lerp(td, mc.player.prevX, mc.player.getX());
            double iy = net.minecraft.util.math.MathHelper.lerp(td, mc.player.prevY, mc.player.getY());
            double iz = net.minecraft.util.math.MathHelper.lerp(td, mc.player.prevZ, mc.player.getZ());
            Vec3d eyePos = new Vec3d(ix, iy + mc.player.getEyeHeight(mc.player.getPose()), iz);
            // Интерполированное направление взгляда
            Vec3d look = mc.player.getRotationVec(td);
            Vec3d vel = look.multiply(speed);

            java.util.List<Vec3d> previewPoints = new java.util.ArrayList<>();
            previewPoints.add(eyePos);
            Vec3d pos = eyePos;
            LandingPoint previewLanding = null;

            for (int i = 0; i < 200; i++) {
                Vec3d nextPos = pos.add(vel);
                BlockHitResult hit = mc.world.raycast(new RaycastContext(
                        pos, nextPos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE, mc.player));
                boolean hitBlock = hit.getType() == HitResult.Type.BLOCK;
                if (hitBlock) nextPos = hit.getPos();
                previewPoints.add(nextPos);
                if (hitBlock || nextPos.y < mc.world.getBottomY()) {
                    net.minecraft.util.math.Direction side = hitBlock ? hit.getSide() : net.minecraft.util.math.Direction.UP;
                    previewLanding = new LandingPoint(nextPos, side, i, coreColor);
                    break;
                }
                vel = new Vec3d(vel.x * drag, vel.y * drag - gravity, vel.z * drag);
                pos = nextPos;
            }

            // Рисуем предсказание
            if (previewPoints.size() >= 2) {
                drawRibbon(ms, cam2, previewPoints, lw * 3f, glowColor);
                drawRibbon(ms, cam2, previewPoints, lw, coreColor);
            }

            if (previewLanding != null) {
                landingPoints.add(previewLanding);
                // Большой круг и крест убраны — только малый круг из landingPoints
            }
        }

        // ── Update cache for visible entities ─────────────────────────────────
        java.util.Set<java.util.UUID> visibleIds = new java.util.HashSet<>();
        for (Entity entity : mc.world.getEntities()) {
            double drag, gravity;
            if      (entity instanceof EnderPearlEntity) { drag=0.99; gravity=0.03; }
            else if (entity instanceof ArrowEntity)      { drag=0.99; gravity=0.05; }
            else if (entity instanceof TridentEntity)    { drag=0.99; gravity=0.05; }
            else continue;

            Vec3d vel = entity.getVelocity();
            if (vel.length() < MIN_SPEED) continue;

            visibleIds.add(entity.getUuid());

            java.util.List<Vec3d> points = new java.util.ArrayList<>();
            Vec3d pos = entity.getPos();
            points.add(pos);
            int ticks = 0;
            LandingPoint lp = null;

            for (int i = 0; i < 150; i++) {
                Vec3d nextPos = pos.add(vel);
                BlockHitResult hit = mc.world.raycast(new RaycastContext(
                        pos, nextPos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE, entity));
                boolean hitBlock = hit.getType() == HitResult.Type.BLOCK;
                if (hitBlock) nextPos = hit.getPos();
                points.add(nextPos);
                if (hitBlock || nextPos.y < mc.world.getBottomY()) {
                    net.minecraft.util.math.Direction side = hitBlock ? hit.getSide() : net.minecraft.util.math.Direction.UP;
                    lp = new LandingPoint(nextPos, side, ticks, coreColor);
                    break;
                }
                vel = new Vec3d(vel.x * drag, vel.y * drag - gravity, vel.z * drag);
                pos = nextPos;
                ticks++;
            }

            // Update cache with full trajectory data
            trajectoryCache.put(entity.getUuid(),
                    new CachedTrajectory(points, lp, now,
                            entity.getPos(), entity.getVelocity(), drag, gravity));
        }

        // ── Remove expired entries ────────────────────────────────────────────
        trajectoryCache.entrySet().removeIf(entry -> {
            CachedTrajectory ct = entry.getValue();
            boolean visible = visibleIds.contains(entry.getKey());
            if (visible) return false;
            long age = now - ct.lastSeen();
            if (ct.landing() != null) return age > 0;
            return age > CACHE_EXPIRE_MS;
        });

        // ── Render all cached trajectories (visible + recently disappeared) ───
        java.util.Map<java.util.UUID, CachedTrajectory> updates = new java.util.HashMap<>();

        for (java.util.Map.Entry<java.util.UUID, CachedTrajectory> entry : trajectoryCache.entrySet()) {
            CachedTrajectory cached = entry.getValue();
            boolean isVisible = visibleIds.contains(entry.getKey());

            List<Vec3d> points;
            LandingPoint landing;

            if (isVisible) {
                points  = cached.points();
                landing = cached.landing();
            } else {
                // If we already have a landing point — don't extrapolate further,
                // just show the landing marker briefly
                if (cached.landing() != null) {
                    points  = cached.points();
                    landing = cached.landing();
                } else {
                    // Extrapolate: advance simulation by elapsed ticks since last seen
                    long elapsedMs = now - cached.lastSeen();
                    int elapsedTicks = (int)(elapsedMs / 50);

                    Vec3d vel = cached.lastVel();
                    Vec3d pos = cached.lastPos();

                    // Advance to current estimated position
                    for (int t = 0; t < elapsedTicks && vel.length() > MIN_SPEED; t++) {
                        vel = new Vec3d(vel.x * cached.drag(), vel.y * cached.drag() - cached.gravity(), vel.z * cached.drag());
                        pos = pos.add(vel);
                    }

                    // Simulate remaining trajectory from current estimated position
                    points = new java.util.ArrayList<>();
                    points.add(pos); // current estimated position of projectile
                    landing = null;
                    int ticks = 0;
                    Vec3d simVel = vel;
                    Vec3d simPos = pos;
                    for (int i = 0; i < 150; i++) {
                        Vec3d nextPos = simPos.add(simVel);
                        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                                simPos, nextPos, RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.NONE, mc.player));
                        boolean hitBlock = hit.getType() == HitResult.Type.BLOCK;
                        if (hitBlock) nextPos = hit.getPos();
                        points.add(nextPos);
                        if (hitBlock || nextPos.y < mc.world.getBottomY()) {
                            net.minecraft.util.math.Direction side = hitBlock ? hit.getSide() : net.minecraft.util.math.Direction.UP;
                            landing = new LandingPoint(nextPos, side, ticks, coreColor);
                            break;
                        }
                        simVel = new Vec3d(simVel.x * cached.drag(), simVel.y * cached.drag() - cached.gravity(), simVel.z * cached.drag());
                        simPos = nextPos;
                        ticks++;
                    }

                    // If we found a landing point — save to updates map (not directly to cache during iteration)
                    if (landing != null) {
                        updates.put(entry.getKey(),
                                new CachedTrajectory(points, landing, cached.lastSeen(),
                                        cached.lastPos(), cached.lastVel(),
                                        cached.drag(), cached.gravity()));
                    }
                }
            }

            float ageFade = isVisible ? 1f :
                    Math.max(0f, 1f - (float)(now - cached.lastSeen()) / CACHE_EXPIRE_MS);
            int fadedCore = withAlpha(coreColor, (int)(230 * ageFade));
            int fadedGlow = withAlpha(glowColor, (int)(60  * ageFade));

            if (points.size() >= 2) {
                drawRibbon(ms, cam2, points, lw * 3f, fadedGlow);
                drawRibbon(ms, cam2, points, lw, fadedCore);
            }

            if (landing != null) {
                // If already landed (not visible) — show ticks=0 (arrived)
                int displayTicks = isVisible ? landing.ticks() : 0;
                landingPoints.add(new LandingPoint(
                        landing.pos(), landing.side(), displayTicks,
                        withAlpha(coreColor, (int)(200 * ageFade))));
            }
        }

        // ── Круг в точке приземления (circle.png) ────────────────────────────
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, CIRCLE_TEX);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Camera cam = mc.gameRenderer.getCamera();
        for (LandingPoint lp : landingPoints) {
            float sz = 0.3f; // радиус круга в мировых единицах
            ms.push();
            
            // Смещение и поворот в соответствии с гранью блока
            net.minecraft.util.math.Direction side = lp.side();
            double offsetX = side.getVector().getX() * 0.015;
            double offsetY = side.getVector().getY() * 0.015;
            double offsetZ = side.getVector().getZ() * 0.015;
            ms.translate(lp.pos().x + offsetX, lp.pos().y + offsetY, lp.pos().z + offsetZ);
            
            org.joml.Quaternionf q = new org.joml.Quaternionf();
            switch (side) {
                case DOWN:
                    q.rotationX((float) Math.toRadians(180));
                    break;
                case UP:
                    break;
                case NORTH:
                    q.rotationX((float) Math.toRadians(-90));
                    break;
                case SOUTH:
                    q.rotationX((float) Math.toRadians(90));
                    break;
                case WEST:
                    q.rotationZ((float) Math.toRadians(90));
                    break;
                case EAST:
                    q.rotationZ((float) Math.toRadians(-90));
                    break;
            }
            ms.multiply(q);

            // Плоский круг на земле (XZ плоскость)
            Matrix4f flat = ms.peek().getPositionMatrix();
            BufferBuilder circle = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            int cc = new FixColor(r, g, b, 200).getRGB();
            circle.vertex(flat, -sz, 0, -sz).texture(0,0).color(cc);
            circle.vertex(flat, -sz, 0,  sz).texture(0,1).color(cc);
            circle.vertex(flat,  sz, 0,  sz).texture(1,1).color(cc);
            circle.vertex(flat,  sz, 0, -sz).texture(1,0).color(cc);
            BuiltBuffer bc = circle.endNullable();
            if (bc != null) BufferRenderer.drawWithGlobalProgram(bc);
            ms.pop();
        }

        // ── Крест в точке приземления (линии НЕ выходят за круг) ─────────────
        RenderSystem.setShaderTexture(0, Identifier.of("solution", "textures/hud/bloom.png"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (LandingPoint lp : landingPoints) {
            float sz = 0.3f; // радиус круга
            float lineLen = sz * 0.85f; // линии короче радиуса, чтобы не выходить за круг
            float lineH = 0.015f;
            int lc = new FixColor(r, g, b, 255).getRGB();

            ms.push();
            
            // Смещение и поворот в соответствии с гранью блока (чуть больше offset чем круг, чтобы поверх круга рисовать)
            net.minecraft.util.math.Direction side = lp.side();
            double offsetX = side.getVector().getX() * 0.02;
            double offsetY = side.getVector().getY() * 0.02;
            double offsetZ = side.getVector().getZ() * 0.02;
            ms.translate(lp.pos().x + offsetX, lp.pos().y + offsetY, lp.pos().z + offsetZ);
            
            org.joml.Quaternionf q = new org.joml.Quaternionf();
            switch (side) {
                case DOWN:
                    q.rotationX((float) Math.toRadians(180));
                    break;
                case UP:
                    break;
                case NORTH:
                    q.rotationX((float) Math.toRadians(-90));
                    break;
                case SOUTH:
                    q.rotationX((float) Math.toRadians(90));
                    break;
                case WEST:
                    q.rotationZ((float) Math.toRadians(90));
                    break;
                case EAST:
                    q.rotationZ((float) Math.toRadians(-90));
                    break;
            }
            ms.multiply(q);

            Matrix4f m = ms.peek().getPositionMatrix();

            // Линия по X
            BufferBuilder lx = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            lx.vertex(m, -lineLen, 0, -lineH).texture(0,0).color(lc);
            lx.vertex(m, -lineLen, 0,  lineH).texture(0,1).color(lc);
            lx.vertex(m,  lineLen, 0,  lineH).texture(1,1).color(lc);
            lx.vertex(m,  lineLen, 0, -lineH).texture(1,0).color(lc);
            BuiltBuffer bx = lx.endNullable();
            if (bx != null) BufferRenderer.drawWithGlobalProgram(bx);

            // Линия по Z
            BufferBuilder lz = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            lz.vertex(m, -lineH, 0, -lineLen).texture(0,0).color(lc);
            lz.vertex(m, -lineH, 0,  lineLen).texture(0,1).color(lc);
            lz.vertex(m,  lineH, 0,  lineLen).texture(1,1).color(lc);
            lz.vertex(m,  lineH, 0, -lineLen).texture(1,0).color(lc);
            BuiltBuffer bz = lz.endNullable();
            if (bz != null) BufferRenderer.drawWithGlobalProgram(bz);

            ms.pop();
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }

    // ── 2D метка времени ──────────────────────────────────────────────────────

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null) return;

        for (LandingPoint lp : landingPoints) {
            Vec3d screen = ProjectionUtility.worldSpaceToScreenSpace(lp.pos());
            if (screen.z <= 0 || screen.z >= 1) continue;

            float sx = (float) screen.x;
            float sy = (float) screen.y;

            double timeSec = lp.ticks() * 0.05;
            String text = String.format("%.1fс", timeSec);

            float tw   = Fonts.DEFAULT.get(10).getStringWidth(text);
            float th   = 7f; // фиксированная высота — точно по тексту
            float padX = 3f;
            float padY = 1f;
            float bgW  = tw + padX * 2;
            float bgH  = th + padY * 2;

            float bgX = sx + 8f;
            float bgY = sy - bgH / 2f;

            blur.render(ShapeProperties.create(e.getContext().getMatrices(),
                    bgX, bgY, bgW, bgH)
                    .round(2)
                    .thickness(1f)
                    .outlineColor(new FixColor(
                            new java.awt.Color(color.get(), true).getRed(),
                            new java.awt.Color(color.get(), true).getGreen(),
                            new java.awt.Color(color.get(), true).getBlue(), 100).getRGB())
                    .color(new FixColor(10, 10, 10, 180).getRGB())
                    .build());

            Fonts.DEFAULT.get(10).drawString(
                    e.getContext().getMatrices(), text,
                    bgX + padX,
                    bgY + padY + 2f,
                    lp.color());
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    /** Рисует круг + крест в точке приземления */
    private void drawLandingMarker(MatrixStack ms, Vec3d pos, net.minecraft.util.math.Direction side, int r, int g, int b) {
        RenderSystem.setShaderTexture(0, CIRCLE_TEX);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        float sz = 0.4f;
        ms.push();
        
        double offsetX = side.getVector().getX() * 0.02;
        double offsetY = side.getVector().getY() * 0.02;
        double offsetZ = side.getVector().getZ() * 0.02;
        ms.translate(pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ);
        
        org.joml.Quaternionf q = new org.joml.Quaternionf();
        switch (side) {
            case DOWN:
                q.rotationX((float) Math.toRadians(180));
                break;
            case UP:
                break;
            case NORTH:
                q.rotationX((float) Math.toRadians(-90));
                break;
            case SOUTH:
                q.rotationX((float) Math.toRadians(90));
                break;
            case WEST:
                q.rotationZ((float) Math.toRadians(90));
                break;
            case EAST:
                q.rotationZ((float) Math.toRadians(-90));
                break;
        }
        ms.multiply(q);
        
        Matrix4f flat = ms.peek().getPositionMatrix();

        // Круг
        int cc = new FixColor(r, g, b, 200).getRGB();
        BufferBuilder circle = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        circle.vertex(flat, -sz, 0, -sz).texture(0,0).color(cc);
        circle.vertex(flat, -sz, 0,  sz).texture(0,1).color(cc);
        circle.vertex(flat,  sz, 0,  sz).texture(1,1).color(cc);
        circle.vertex(flat,  sz, 0, -sz).texture(1,0).color(cc);
        BuiltBuffer bc = circle.endNullable();
        if (bc != null) BufferRenderer.drawWithGlobalProgram(bc);
        ms.pop();

        // Крест — две линии через центр
        RenderSystem.setShaderTexture(0, Identifier.of("solution", "textures/hud/bloom.png"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        float lineLen = 0.6f;
        float lineH   = 0.015f;
        int lc = new FixColor(r, g, b, 255).getRGB();

        ms.push();
        ms.translate(pos.x + offsetX * 1.5, pos.y + offsetY * 1.5, pos.z + offsetZ * 1.5);
        ms.multiply(q);
        Matrix4f m = ms.peek().getPositionMatrix();

        // Линия по X
        BufferBuilder lx = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        lx.vertex(m, -lineLen, 0, -lineH).texture(0,0).color(lc);
        lx.vertex(m, -lineLen, 0,  lineH).texture(0,1).color(lc);
        lx.vertex(m,  lineLen, 0,  lineH).texture(1,1).color(lc);
        lx.vertex(m,  lineLen, 0, -lineH).texture(1,0).color(lc);
        BuiltBuffer bx = lx.endNullable();
        if (bx != null) BufferRenderer.drawWithGlobalProgram(bx);

        // Линия по Z
        BufferBuilder lz = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        lz.vertex(m, -lineH, 0, -lineLen).texture(0,0).color(lc);
        lz.vertex(m, -lineH, 0,  lineLen).texture(0,1).color(lc);
        lz.vertex(m,  lineH, 0,  lineLen).texture(1,1).color(lc);
        lz.vertex(m,  lineH, 0, -lineLen).texture(1,0).color(lc);
        BuiltBuffer bz = lz.endNullable();
        if (bz != null) BufferRenderer.drawWithGlobalProgram(bz);

        ms.pop();
    }

    /**
     * Рисует непрерывную ленту через все точки траектории.
     * Каждая точка получает два вертекса (left/right) — лента без зазоров.
     */
    private void drawRibbon(MatrixStack ms, Camera cam,
                             java.util.List<Vec3d> points, float halfW, int color) {
        if (points.size() < 2) return;

        // TRIANGLE_STRIP: каждая пара вертексов продолжает ленту без разрывов
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();

        for (int i = 0; i < points.size(); i++) {
            Vec3d p = points.get(i);

            // Направление ленты в этой точке
            Vec3d dir;
            if (i == 0) {
                dir = points.get(1).subtract(points.get(0)).normalize();
            } else if (i == points.size() - 1) {
                dir = points.get(i).subtract(points.get(i - 1)).normalize();
            } else {
                dir = points.get(i + 1).subtract(points.get(i - 1)).normalize();
            }

            // Перпендикуляр к направлению и к камере
            Vec3d toCam = cam.getPos().subtract(p).normalize();
            Vec3d perp  = dir.crossProduct(toCam).normalize().multiply(halfW);

            float lx = (float)(p.x + perp.x), ly = (float)(p.y + perp.y), lz = (float)(p.z + perp.z);
            float rx = (float)(p.x - perp.x), ry = (float)(p.y - perp.y), rz = (float)(p.z - perp.z);

            float u = (float) i / (points.size() - 1);
            buf.vertex(m, lx, ly, lz).texture(u, 0).color(color);
            buf.vertex(m, rx, ry, rz).texture(u, 1).color(color);
        }

        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }
}
