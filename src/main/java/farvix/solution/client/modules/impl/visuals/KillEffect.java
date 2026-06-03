package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.entity.EventAttackEntity;
import farvix.solution.api.events.impl.entity.EventEntityDeath;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import farvix.solution.api.events.impl.game.EventUpdate;

@ModuleInfo(name = "Kill Effect", category = ModuleCategory.PLAYER, description = "Эффекты при убийстве")
public class KillEffect extends Module {

    private static final Logger LOG = LoggerFactory.getLogger("KillEffect");

    // ── Settings ──────────────────────────────────────────────────────────────
    private final ModeSetting mode = new ModeSetting("Эффекты", this,
            "Частицы", // 8 символов - задает ширину левой колонки
            "Молния",  // 7 символов - задает ширину правой колонки
            "Метеор", 
            "Душа",   
            "GG"       
    );

    private final ModeSetting target = new ModeSetting("Цель", this,
            "Все",
            "Игроки",
            "Мобы"
    );

    // Particle-only settings — always visible so user can configure before switching
    private final BooleanSetting gravity = new BooleanSetting("Гравитация", this);

    private final SliderSetting lifetime = new SliderSetting(
            "Время жизни", this, 5f, 1f, 25f, 1f);

    private final SliderSetting particleCount = new SliderSetting(
            "Количество", this, 5f, 1f, 25f, 1f);

    private final SliderSetting thickness = new SliderSetting(
            "Толщина", this, 1f, 1f, 25f, 1f);

    private final SliderSetting soulSpeed = new SliderSetting(
            "Скорость души", this, 1f, 0.5f, 5f, 0.5f);

    private final SliderSetting soulRotation = new SliderSetting(
            "Вращение души", this, 2f, 0f, 10f, 0.5f);

    private final BooleanSetting soulName = new BooleanSetting("С ником", this);

    {
        gravity.setVisible(() -> mode.is("Частицы"));
        particleCount.setVisible(() -> !mode.is("Метеор") && !mode.is("Душа"));
        thickness.setVisible(() -> !mode.is("Метеор") && !mode.is("Душа"));
        soulSpeed.setVisible(() -> mode.is("Душа"));
        soulRotation.setVisible(() -> mode.is("Душа"));
        soulName.setVisible(() -> mode.is("Душа"));
    }

    // Color setting — hidden for meteor and soul modes
    private final ColorSetting colorSetting = new ColorSetting(
            "Цвет", this, new FixColor(100, 150, 255, 255).getRGB());

    {
        colorSetting.setVisible(() -> !mode.is("Метеор") && !mode.is("Душа"));
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<DeathEffect>  effects     = new CopyOnWriteArrayList<>();
    private final List<Particle>     particles   = new CopyOnWriteArrayList<>();
    private final List<GGEffect>     ggEffects   = new CopyOnWriteArrayList<>();
    private final List<MeteorEffect> meteorEffects = new CopyOnWriteArrayList<>();
    private final List<SoulEffect>   soulEffects = new CopyOnWriteArrayList<>();
    private static final Random      RNG         = new Random();

    // UUID → время последней атаки (работает и для игроков и для мобов)
    private final Map<java.util.UUID, Long> attackedEntities = new java.util.concurrent.ConcurrentHashMap<>();
    // UUID → были ли они живы в прошлом тике (чтобы не спамить эффект)
    private final Map<java.util.UUID, Boolean> wasAlive = new java.util.concurrent.ConcurrentHashMap<>();

    // 5 секунд — если умер в течение 5 сек после нашей атаки
    private static final long KILL_WINDOW_MS = 5_000L;

    public KillEffect() {
        gravity.setEnabled(true);
    }

    private FixColor getColor() {
        java.awt.Color c = new java.awt.Color(colorSetting.get(), true);
        return new FixColor(c.getRed(), c.getGreen(), c.getBlue(), 255);
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (e.getTarget() instanceof LivingEntity living) {
            attackedEntities.put(living.getUuid(), System.currentTimeMillis());
            LOG.info("[KillEffect] Attacked: {} uuid={} isPlayer={}",
                    living.getName().getString(), living.getUuid(), living instanceof PlayerEntity);
        }
    }

    /**
     * Для мобов — через EventEntityDeath (onDeath вызывается на клиенте).
     * Для игроков — НЕ работает на серверах, используем onUpdate вместо этого.
     */
    @EventHandler
    public void onEntityDeath(EventEntityDeath e) {
        if (mc.player == null || mc.world == null) return;
        LivingEntity entity = e.getEntity();
        // Игроков пропускаем — их смерть ловим в onUpdate
        if (entity instanceof PlayerEntity) return;

        java.util.UUID uuid = entity.getUuid();
        Long lastHit = attackedEntities.get(uuid);
        long now = System.currentTimeMillis();

        boolean killedByUs = (entity.getPrimeAdversary() == mc.player)
                || (lastHit != null && (now - lastHit) <= KILL_WINDOW_MS);

        if (!killedByUs) return;
        attackedEntities.remove(uuid);
        spawnEffect(entity);
    }

    /**
     * Для игроков на серверах — проверяем здоровье каждый тик.
     * Если игрок которого мы атаковали стал мёртвым (health <= 0 или isDead) — спавним эффект.
     */
    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        // Обновляем полет душ независимо от того, атакуем ли мы кого-то сейчас
        if (!soulEffects.isEmpty()) {
            soulEffects.removeIf(SoulEffect::isFinished);
            double liftPerTick = soulSpeed.getValue() / 20.0;
            for (SoulEffect soul : soulEffects) {
                soul.update(liftPerTick);
            }
        }

        if (attackedEntities.isEmpty()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            java.util.UUID uuid = player.getUuid();
            Long lastHit = attackedEntities.get(uuid);
            if (lastHit == null) continue;

            long now = System.currentTimeMillis();
            if (now - lastHit > KILL_WINDOW_MS) {
                attackedEntities.remove(uuid);
                wasAlive.remove(uuid);
                continue;
            }

            boolean alive = player.getHealth() > 0 && !player.isDead() && !player.isRemoved();
            Boolean prevAlive = wasAlive.get(uuid);

            if (prevAlive == null) {
                wasAlive.put(uuid, alive);
                continue;
            }

            // Был жив → стал мёртв = убийство
            if (prevAlive && !alive) {
                LOG.info("[KillEffect] Player death detected via health check: {} uuid={}", player.getName().getString(), uuid);
                attackedEntities.remove(uuid);
                wasAlive.remove(uuid);
                spawnEffect(player);
            } else {
                wasAlive.put(uuid, alive);
            }
        }
    }

    private void spawnEffect(LivingEntity entity) {

        String t = target.getCurrentMode();
        boolean isPlayer = entity instanceof PlayerEntity;
        boolean isMob    = entity instanceof MobEntity && !isPlayer;

        LOG.info("[KillEffect] target={} isPlayer={} isMob={}", t, isPlayer, isMob);

        if (t.equals("Игроки") && !isPlayer) { LOG.info("[KillEffect] SKIP: target=players but not player"); return; }
        if (t.equals("Мобы")    && !isMob)    { LOG.info("[KillEffect] SKIP: target=mobs but not mob"); return; }

        FixColor c = getColor();
        LOG.info("[KillEffect] SPAWNING effect mode={} pos={}", mode.getCurrentMode(), entity.getPos());
        if (mode.is("Молния")) {
            float boltT = 0.01f + (thickness.getValue() - 1f) / 24f * 0.24f;
            effects.add(new DeathEffect(entity.getPos(), c, boltT,
                    (long)(lifetime.getValue() * 1000f)));
        } else if (mode.is("GG")) {
            Vec3d spawnPos = entity.getPos().add(0, entity.getHeight() / 2.0, 0);
            long dur = (long)(lifetime.getValue() * 1000f);
            float dotSize = 0.04f + (thickness.getValue() - 1f) / 24f * 0.16f;
            ggEffects.add(new GGEffect(spawnPos, c, dur, dotSize));
        } else if (mode.is("Метеор")) {
            LOG.info("[KillEffect] Spawning METEOR at pos={}", entity.getPos());
            meteorEffects.add(new MeteorEffect(entity.getPos()));
            LOG.info("[KillEffect] meteorEffects size={}", meteorEffects.size());
        } else if (mode.is("Душа")) {
            soulEffects.add(new SoulEffect(entity, entity.getPos()));
        } else {
            spawnParticles(entity, c, gravity.isEnabled(),
                    (long)(lifetime.getValue() * 1000f),
                    (int) particleCount.getValue(),
                    thickness.getValue() * 0.01f);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.player == null || mc.world == null) return;
        if (effects.isEmpty() && particles.isEmpty() && ggEffects.isEmpty() && meteorEffects.isEmpty() && soulEffects.isEmpty()) return;

        MatrixStack ms = e.getMatrices();
        Camera camera  = mc.gameRenderer.getCamera();

        // Рендерим души ПЕРЕД включением аддитивного смешивания и текстур, 
        // так как у моделей сущностей свой сложный рендер
        for (SoulEffect soul : soulEffects) {
            soul.render(ms, camera, e.getTickDelta());
        }

        ms.push();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Identifier glowTex = Identifier.of("solution", "textures/hud/bloom.png");
        RenderSystem.setShaderTexture(0, glowTex);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder builder = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (DeathEffect effect : effects) effect.render(builder, ms, camera);
        for (Particle p : particles) {
            if (!p.isDead()) { p.update(); p.render(builder, ms, camera); }
        }
        for (GGEffect gg : ggEffects) gg.render(builder, ms, camera);
        for (MeteorEffect meteor : meteorEffects) meteor.render(builder, ms, camera);

        BuiltBuffer built = builder.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        particles.removeIf(Particle::isDead);
        effects.removeIf(DeathEffect::isFinished);
        ggEffects.removeIf(GGEffect::isFinished);
        meteorEffects.removeIf(MeteorEffect::isFinished);

        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        ms.pop();
    }

    // ── Particle spawning ─────────────────────────────────────────────────────

    private void spawnParticles(LivingEntity entity, FixColor c,
                                boolean hasGravity, long lifetimeMs, int countMul, float sz) {
        Vec3d pos    = entity.getPos();
        float width  = entity.getWidth();
        float height = entity.getHeight();
        float yaw    = (float) Math.toRadians(-entity.prevYaw + 90f);
        int   base   = 20 * countMul; // 1=20, 25=500 particles

        spawnSphere(pos.add(0, height - 0.2, 0), width * 0.4f, base / 10, c, hasGravity, lifetimeMs, sz);
        spawnBody(pos, width * 0.4f, height * 0.85f, width * 0.4f, width * 0.2f, yaw, base / 4, c, hasGravity, lifetimeMs, sz);
        for (int i = -1; i <= 1; i += 2) {
            Vec3d arm = new Vec3d(Math.sin(yaw)*width*0.5*i, height*0.75, Math.cos(yaw)*width*0.5*i);
            spawnLimb(pos.add(arm), width*0.4f, width*0.15f, base/8, c, hasGravity, lifetimeMs, sz);
            Vec3d leg = new Vec3d(Math.sin(yaw)*width*0.15*i, height*0.4, Math.cos(yaw)*width*0.15*i);
            spawnLimb(pos.add(leg), height*0.45f, width*0.15f, base/6, c, hasGravity, lifetimeMs, sz);
        }
    }

    private void spawnSphere(Vec3d pos, float radius, int count, FixColor c, boolean hg, long lt, float sz) {
        for (int i = 0; i < count; i++) {
            float a1=RNG.nextFloat()*(float)(Math.PI*2), a2=(float)Math.acos(2f*RNG.nextFloat()-1f);
            float r=radius*(float)Math.cbrt(RNG.nextFloat());
            float sp=hg?0.02f:0.008f;
            particles.add(new Particle(pos,
                r*(float)(Math.sin(a2)*Math.cos(a1)), r*(float)(Math.sin(a2)*Math.sin(a1)), r*(float)Math.cos(a2),
                (RNG.nextFloat()-.5f)*sp, hg?0.03f+RNG.nextFloat()*0.04f:(RNG.nextFloat()-.5f)*0.008f,
                (RNG.nextFloat()-.5f)*sp, c, hg, lt, sz));
        }
    }

    private void spawnBody(Vec3d pos, float rx, float height, float rz, float spread,
                           float yaw, int count, FixColor c, boolean hg, long lt, float sz) {
        for (int i = 0; i < count; i++) {
            float ox=(RNG.nextFloat()-.5f)*rx*2, oy=RNG.nextFloat()*height, oz=(RNG.nextFloat()-.5f)*spread*2;
            float sp=hg?0.025f:0.01f;
            particles.add(new Particle(pos,
                (float)(ox*Math.cos(yaw)-oz*Math.sin(yaw)), oy, (float)(ox*Math.sin(yaw)+oz*Math.cos(yaw)),
                (RNG.nextFloat()-.5f)*sp, hg?0.04f+RNG.nextFloat()*0.05f:(RNG.nextFloat()-.5f)*0.01f,
                (RNG.nextFloat()-.5f)*sp, c, hg, lt, sz));
        }
    }

    private void spawnLimb(Vec3d pos, float height, float radius, int count, FixColor c, boolean hg, long lt, float sz) {
        for (int i = 0; i < count; i++) {
            float sp=hg?0.018f:0.006f;
            particles.add(new Particle(pos,
                (RNG.nextFloat()-.5f)*radius*2, -RNG.nextFloat()*height, (RNG.nextFloat()-.5f)*radius*2,
                (RNG.nextFloat()-.5f)*sp, hg?0.025f+RNG.nextFloat()*0.035f:(RNG.nextFloat()-.5f)*0.006f,
                (RNG.nextFloat()-.5f)*sp, c, hg, lt, sz));
        }
    }

    // ── Particle ──────────────────────────────────────────────────────────────

    private class Particle {
        double x, y, z;
        float  vx, vy, vz, gravityFactor, friction, sz;
        long   startTime, lastUpdate, lifeTime;
        boolean hasGravity;
        FixColor color;

        Particle(Vec3d o, float ox, float oy, float oz,
                 float vx, float vy, float vz, FixColor color, boolean hg, long lt, float sz) {
            x=o.x+ox; y=o.y+oy; z=o.z+oz;
            this.color=color; startTime=lastUpdate=System.currentTimeMillis();
            hasGravity=hg; gravityFactor=0.005f+RNG.nextFloat()*0.005f;
            lifeTime=lt + RNG.nextInt((int)Math.max(1, lt/3));
            friction=hg?0.999f:0.995f;
            this.sz = Math.max(0.02f, sz);
            float sp=hg?0.04f:0.03f;
            this.vx=(RNG.nextFloat()-.5f)*sp;
            this.vy=hg?0.025f+RNG.nextFloat()*0.035f:(RNG.nextFloat()-.5f)*0.03f;
            this.vz=(RNG.nextFloat()-.5f)*sp;
        }

        void update() {
            long now=System.currentTimeMillis();
            float d=Math.min(5f,(float)(now-lastUpdate)/16.67f); lastUpdate=now;
            if (hasGravity) vy-=gravityFactor*d;
            float drag=(float)Math.pow(friction,d); vx*=drag; vy*=drag; vz*=drag;
            double nx=x+vx*d, ny=y+vy*d, nz=z+vz*d;
            if (hasGravity && mc.world!=null) {
                if (!mc.world.getBlockState(BlockPos.ofFloored(x,ny-0.05,z)).isAir()){vy=-vy*0.5f;ny=y;}
                if (!mc.world.getBlockState(BlockPos.ofFloored(nx,y,z)).isAir())     {vx=-vx*0.5f;nx=x;}
                if (!mc.world.getBlockState(BlockPos.ofFloored(x,y,nz)).isAir())     {vz=-vz*0.5f;nz=z;}
            }
            if (Math.abs(vy)<=1e-4f){vx=0;vz=0;}
            x=nx; y=ny; z=nz;
        }

        float getAlpha() { return 1f-MathHelper.clamp((float)(System.currentTimeMillis()-startTime)/lifeTime,0f,1f); }
        boolean isDead() { return System.currentTimeMillis()-startTime>lifeTime; }

        void render(BufferBuilder b, MatrixStack ms, Camera cam) {
            float a=getAlpha();
            ms.push(); ms.translate(x,y,z); ms.multiply(cam.getRotation());
            Matrix4f m=ms.peek().getPositionMatrix();
            int rgb=color.alpha(0.9f*a).getRGB();
            b.vertex(m,-sz/2,-sz/2,0).texture(0,0).color(rgb);
            b.vertex(m,-sz/2, sz/2,0).texture(0,1).color(rgb);
            b.vertex(m, sz/2, sz/2,0).texture(1,1).color(rgb);
            b.vertex(m, sz/2,-sz/2,0).texture(1,0).color(rgb);
            ms.pop();
        }
    }

    // ── Lightning effect ──────────────────────────────────────────────────────

    private static class DeathEffect {
        final Vec3d    pos;
        final FixColor color;
        final float    boltThickness;
        final long     startTime = System.currentTimeMillis();
        final long     duration;  // from lifetime setting
        final List<List<Vec3d>> bolts = new ArrayList<>();

        DeathEffect(Vec3d pos, FixColor color, float thickness, long durationMs) {
            this.pos           = pos;
            this.color         = color;
            this.boltThickness = Math.max(0.01f, thickness);
            this.duration      = durationMs;
            for (int b = 0; b < 3; b++) {
                bolts.add(buildBolt(pos.add(
                        (RNG.nextFloat()-.5f)*0.3, 0, (RNG.nextFloat()-.5f)*0.3),
                        3.5f+RNG.nextFloat()*1.5f, 6));
            }
            for (int b = 0; b < 2; b++) {
                bolts.add(buildBolt(pos.add(
                        (RNG.nextFloat()-.5f)*0.5, 0, (RNG.nextFloat()-.5f)*0.5),
                        2f+RNG.nextFloat(), 4));
            }
        }

        /**
         * Builds a single lightning bolt using midpoint displacement.
         * Returns a flat list of [p0, p1, p1, p2, p2, p3, ...] line segments.
         */
        private static List<Vec3d> buildBolt(Vec3d start, float height, int subdivisions) {
            List<Vec3d> points = new ArrayList<>();
            points.add(start);
            points.add(start.add(0, height, 0));

            float displacement = height * 0.4f;
            for (int s = 0; s < subdivisions; s++) {
                List<Vec3d> next = new ArrayList<>();
                for (int i = 0; i < points.size() - 1; i++) {
                    Vec3d a = points.get(i);
                    Vec3d b = points.get(i + 1);
                    Vec3d mid = a.add(b).multiply(0.5)
                            .add((RNG.nextFloat() - .5f) * displacement,
                                 (RNG.nextFloat() - .5f) * displacement * 0.3f,
                                 (RNG.nextFloat() - .5f) * displacement);
                    next.add(a);
                    next.add(mid);
                }
                next.add(points.get(points.size() - 1));
                points = next;
                displacement *= 0.5f;
            }

            // Convert to line segment pairs [p0,p1, p1,p2, ...]
            List<Vec3d> segments = new ArrayList<>();
            for (int i = 0; i < points.size() - 1; i++) {
                segments.add(points.get(i));
                segments.add(points.get(i + 1));
            }
            return segments;
        }

        boolean isFinished() { return System.currentTimeMillis() - startTime > duration; }

        void render(BufferBuilder b, MatrixStack ms, Camera cam) {
            long el = System.currentTimeMillis() - startTime;
            float fadeStart = duration - 400L;
            float alpha;
            if (el < 150)            alpha = el / 150f;
            else if (el < fadeStart) alpha = 1f;
            else                     alpha = 1f - (el - fadeStart) / 400f;
            alpha = MathHelper.clamp(alpha, 0f, 1f);
            if (alpha <= 0.001f) return;

            float flicker = 0.75f + RNG.nextFloat() * 0.25f;
            float finalA  = alpha * flicker;

            for (int boltIdx = 0; boltIdx < bolts.size(); boltIdx++) {
                List<Vec3d> segs = bolts.get(boltIdx);
                // Main bolts brighter and bigger glow, side bolts smaller
                float glowSize = boltIdx < 3 ? boltThickness * 2.0f : boltThickness * 1.0f;
                float coreSize = boltIdx < 3 ? boltThickness * 0.5f : boltThickness * 0.25f;

                // Draw every point along the bolt as a glow billboard
                // segs is [p0,p1, p1,p2, ...] — collect unique points
                for (int i = 0; i < segs.size(); i++) {
                    Vec3d p = segs.get(i);
                    // Outer glow (large, low alpha)
                    renderBillboard(b, ms, cam, p, glowSize, finalA * 0.35f);
                    // Inner core (small, high alpha)
                    renderBillboard(b, ms, cam, p, coreSize, finalA * 0.9f);
                }
            }
        }

        /**
         * Renders a camera-facing glow quad at world position p.
         * size = diameter of the billboard in world units.
         */
        private void renderBillboard(BufferBuilder b, MatrixStack ms, Camera cam,
                                     Vec3d p, float size, float alpha) {
            float half = size / 2f;
            int   rgb  = color.alpha(alpha).getRGB();
            ms.push();
            ms.translate(p.x, p.y, p.z);
            ms.multiply(cam.getRotation());
            Matrix4f m = ms.peek().getPositionMatrix();
            b.vertex(m, -half, -half, 0).texture(0, 0).color(rgb);
            b.vertex(m, -half,  half, 0).texture(0, 1).color(rgb);
            b.vertex(m,  half,  half, 0).texture(1, 1).color(rgb);
            b.vertex(m,  half, -half, 0).texture(1, 0).color(rgb);
            ms.pop();
        }
    }

    // ── GG Effect ─────────────────────────────────────────────────────────────

    /**
     * Renders "GG" as a camera-facing billboard made of overlapping glow particles.
     * Uses a 7×9 bitmap per letter so dots overlap and form a solid filled shape.
     * Dot size is driven by the {@code thickness} setting.
     */
    private class GGEffect {

        // ── 7×9 bitmap for letter "G" ─────────────────────────────────────────
        // Wider bitmap = more dots = more filled look
        // Row 0 = top, row 8 = bottom
        private static final int[][] G_BITMAP = {
            {0,1,1,1,1,1,0},
            {1,1,1,1,1,1,1},
            {1,1,0,0,0,0,0},
            {1,1,0,0,0,0,0},
            {1,1,0,1,1,1,1},
            {1,1,0,1,1,1,1},
            {1,1,0,0,1,1,1},
            {1,1,1,1,1,1,1},
            {0,1,1,1,1,1,0},
        };

        final Vec3d    startPos;
        final FixColor color;
        final long     startTime = System.currentTimeMillis();
        final long     duration;
        final float    dotSize;

        private static final float RISE_SPEED = 0.3f;
        // Gap between letters in dot-units (tight)
        private static final float LETTER_GAP = 2.0f;
        GGEffect(Vec3d pos, FixColor color, long durationMs, float dotSize) {
            this.startPos = pos;
            this.color    = color;
            this.duration = durationMs;
            this.dotSize  = dotSize;
        }

        boolean isFinished() { return System.currentTimeMillis() - startTime > duration; }

        void render(BufferBuilder b, MatrixStack ms, Camera cam) {
            long  el       = System.currentTimeMillis() - startTime;
            float progress = MathHelper.clamp((float) el / duration, 0f, 1f);

            float alpha;
            if      (progress < 0.1f) alpha = progress / 0.1f;
            else if (progress < 0.7f) alpha = 1f;
            else                      alpha = 1f - (progress - 0.7f) / 0.3f;
            alpha = MathHelper.clamp(alpha, 0f, 1f);
            if (alpha <= 0.001f) return;

            float rise = RISE_SPEED * (el / 1000f);

            int cols = G_BITMAP[0].length; // 7
            int rows = G_BITMAP.length;    // 9

            // step < dotSize → dots overlap → solid filled look
            float step    = dotSize * 0.85f;
            float letterW = cols * step;
            float letterH = rows * step;
            float totalW  = letterW * 2 + LETTER_GAP * step;

            double cx = startPos.x;
            double cy = startPos.y + rise;
            double cz = startPos.z;

            for (int letter = 0; letter < 2; letter++) {
                float offX = -totalW / 2f + letter * (letterW + LETTER_GAP * step);
                float offY = -letterH / 2f;
                renderLetter(b, ms, cam, cx, cy, cz, offX, offY, step, alpha);
            }
        }

        private void renderLetter(BufferBuilder b, MatrixStack ms, Camera cam,
                                   double cx, double cy, double cz,
                                   float offX, float offY, float step, float alpha) {
            // Pre-push camera transform once per letter for efficiency
            ms.push();
            ms.translate(cx, cy, cz);
            ms.multiply(cam.getRotation());
            Matrix4f m = ms.peek().getPositionMatrix();

            for (int row = 0; row < G_BITMAP.length; row++) {
                for (int col = 0; col < G_BITMAP[row].length; col++) {
                    if (G_BITMAP[row][col] == 0) continue;

                    float lx = offX + col * step;
                    // Y is flipped: row 0 = top = positive Y in camera space
                    float ly = -(offY + row * step);

                    float half = dotSize / 2f;
                    // Outer glow — large, soft, low alpha
                    float gh = dotSize * 1.4f;
                    int outerRgb = color.alpha(alpha * 0.3f).getRGB();
                    b.vertex(m, lx - gh, ly - gh, 0).texture(0, 0).color(outerRgb);
                    b.vertex(m, lx - gh, ly + gh, 0).texture(0, 1).color(outerRgb);
                    b.vertex(m, lx + gh, ly + gh, 0).texture(1, 1).color(outerRgb);
                    b.vertex(m, lx + gh, ly - gh, 0).texture(1, 0).color(outerRgb);

                    // Inner core — sharp, bright
                    int coreRgb = color.alpha(alpha).getRGB();
                    b.vertex(m, lx - half, ly - half, 0).texture(0, 0).color(coreRgb);
                    b.vertex(m, lx - half, ly + half, 0).texture(0, 1).color(coreRgb);
                    b.vertex(m, lx + half, ly + half, 0).texture(1, 1).color(coreRgb);
                    b.vertex(m, lx + half, ly - half, 0).texture(1, 0).color(coreRgb);
                }
            }
            ms.pop();
        }
    }

    // ── Meteor Effect ─────────────────────────────────────────────────────────

    private class MeteorEffect {
        // Точка приземления (где умер враг)
        final Vec3d landPos;
        // Начальная точка — 8 блоков в случайном направлении, высоко вверх
        final Vec3d startPos;
        // Конечная точка — половина метеора в земле
        final Vec3d endPos;

        final long startTime = System.currentTimeMillis();
        // Фаза полёта: 1.5 сек
        static final long FALL_DURATION = 1500L;
        // Фаза застревания: 4 сек
        static final long STUCK_DURATION = 4000L;
        static final long TOTAL_DURATION = FALL_DURATION + STUCK_DURATION;

        // Размер метеора — в 3 раза больше
        static final float SIZE = 2.4f;
        // Длина хвоста
        static final float TAIL_LENGTH = 6.0f;

        MeteorEffect(Vec3d land) {
            this.landPos = land;

            // Стартуем с противоположной стороны от игрока — метеор летит навстречу игроку
            Vec3d playerPos = mc.player != null ? mc.player.getPos() : land.add(8, 0, 0);
            Vec3d dirFromPlayer = land.subtract(playerPos).normalize();

            // Старт: с противоположной стороны от игрока
            double radius = 7 + RNG.nextDouble() * 2;
            double startX = land.x + dirFromPlayer.x * radius + (RNG.nextDouble() - 0.5) * 2;
            double startZ = land.z + dirFromPlayer.z * radius + (RNG.nextDouble() - 0.5) * 2;
            double startY = land.y + radius;

            this.startPos = new Vec3d(startX, startY, startZ);
            this.endPos = new Vec3d(land.x, land.y - SIZE * 0.5 + 1.25, land.z);
        }

        boolean isFinished() {
            return System.currentTimeMillis() - startTime > TOTAL_DURATION;
        }

        void render(BufferBuilder b, MatrixStack ms, Camera cam) {
            long el = System.currentTimeMillis() - startTime;
            if (el % 500 < 20) LOG.info("[Meteor] render el={} landPos={}", el, landPos);

            Vec3d currentPos;
            float alpha;

            if (el < FALL_DURATION) {
                // Фаза полёта — интерполируем от start к end
                float t = (float) el / FALL_DURATION;
                // Easing: ускорение при падении
                t = t * t;
                currentPos = startPos.lerp(endPos, t);
                alpha = Math.min(1f, t * 3f); // быстро появляется
            } else {
                // Фаза застревания — центр метеора на уровне земли, верхняя половина торчит
                // endPos уже = land.y - SIZE*0.5, значит верхний край = land.y + SIZE*0.5
                currentPos = endPos;
                float stuckT = (float)(el - FALL_DURATION) / STUCK_DURATION;
                // Плавное исчезновение в конце
                alpha = stuckT > 0.7f ? 1f - (stuckT - 0.7f) / 0.3f : 1f;
            }

            if (alpha <= 0.001f) return;

            // Направление движения (для хвоста)
            Vec3d dir = endPos.subtract(startPos).normalize();

            // Рисуем ядро метеора
            renderMeteorCore(b, ms, cam, currentPos, alpha);

            // Рисуем хвост только во время полёта
            if (el < FALL_DURATION) {
                float t = (float) el / FALL_DURATION;
                renderMeteorTail(b, ms, cam, currentPos, dir, alpha, t);
            }
        }

        private void renderMeteorCore(BufferBuilder b, MatrixStack ms, Camera cam,
                                       Vec3d pos, float alpha) {
            // Внешнее свечение — большое
            float glowSize = SIZE * 2.0f;
            renderBillboard(b, ms, cam, pos, glowSize, alpha * 0.3f,
                    new FixColor(255, 80, 20, 255));
            // Среднее свечение
            float midSize = SIZE * 1.4f;
            renderBillboard(b, ms, cam, pos, midSize, alpha * 0.5f,
                    new FixColor(255, 120, 40, 255));
            // Основной шар
            renderBillboard(b, ms, cam, pos, SIZE, alpha * 0.9f,
                    new FixColor(255, 200, 80, 255));
            // Яркое ядро
            renderBillboard(b, ms, cam, pos, SIZE * 0.4f, alpha,
                    new FixColor(255, 255, 220, 255));
        }

        private void renderMeteorTail(BufferBuilder b, MatrixStack ms, Camera cam,
                                       Vec3d pos, Vec3d dir, float alpha, float progress) {
            int segments = 12;
            for (int i = 1; i <= segments; i++) {
                float t = (float) i / segments;
                float segAlpha = alpha * (1f - t) * 0.6f;
                float segSize = SIZE * (1f - t * 0.8f);

                // Позиция сегмента хвоста — назад по направлению движения
                Vec3d segPos = pos.subtract(dir.multiply(t * TAIL_LENGTH * progress));

                // Цвет от оранжевого к красному к тёмному
                int r = (int)(255 * (1f - t * 0.3f));
                int g = (int)(Math.max(0, 120 - t * 120));
                int bCol = 0;
                renderBillboard(b, ms, cam, segPos, segSize, segAlpha,
                        new FixColor(r, g, bCol, 255));
            }
        }

        private void renderBillboard(BufferBuilder b, MatrixStack ms, Camera cam,
                                      Vec3d pos, float size, float alpha, FixColor color) {
            float half = size / 2f;
            int rgb = color.alpha(alpha).getRGB();
            ms.push();
            ms.translate(pos.x, pos.y, pos.z);
            ms.multiply(cam.getRotation());
            Matrix4f m = ms.peek().getPositionMatrix();
            b.vertex(m, -half, -half, 0).texture(0, 0).color(rgb);
            b.vertex(m, -half,  half, 0).texture(0, 1).color(rgb);
            b.vertex(m,  half,  half, 0).texture(1, 1).color(rgb);
            b.vertex(m,  half, -half, 0).texture(1, 0).color(rgb);
            ms.pop();
        }
    }

    private class SoulEffect {
        final LivingEntity entity;
        Vec3d pos;
        Vec3d prevPos; // Добавляем предыдущую позицию для интерполяции
        float rotation;
        float prevRotation;
        final long startTime = System.currentTimeMillis();

        SoulEffect(LivingEntity entity, Vec3d pos) {
            this.entity = entity;
            this.pos = pos;
            this.prevPos = pos; // Инициализируем prevPos
            this.rotation = 0;
            this.prevRotation = 0;
        }

        boolean isFinished() {
            return System.currentTimeMillis() - startTime > (long)(lifetime.getValue() * 1000f);
        }

        void update(double lift) {
            prevPos = pos; // Сохраняем текущую позицию как предыдущую
            pos = pos.add(0, lift, 0);
            
            prevRotation = rotation;
            rotation += soulRotation.getValue() * 3.0f; // Применяем вращение
        }

        void render(MatrixStack ms, Camera cam, float tickDelta) {
            long elapsed = System.currentTimeMillis() - startTime;
            float durationMs = lifetime.getValue() * 1000f;
            float progress = (float) elapsed / durationMs;
            float alpha = 0.5f * (1.0f - progress);

            ms.push();
            // Интерполируем позицию для плавного движения между тиками
            Vec3d interpolatedPos = MathHelper.lerp(tickDelta, prevPos, pos);
            ms.translate(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);

            // Плавное вращение вокруг вертикальной оси
            float renderRotation = MathHelper.lerp(tickDelta, prevRotation, rotation);
            ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(renderRotation));

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha); // Устанавливаем прозрачность 50% и затухание

            // Сохраняем состояние для "чистого" рендера души
            int origHurt = entity.hurtTime;
            int origDeath = entity.deathTime; // Добавляем сохранение времени смерти
            int origFire = entity.getFireTicks();
            boolean origGlowing = entity.isGlowing(); // Сохраняем состояние свечения
            boolean origNameVisible = entity.isCustomNameVisible(); // Сохраняем видимость ника
            boolean origInvisible = entity.isInvisible(); // Сохраняем состояние невидимости
            net.minecraft.text.Text origCustomName = entity.getCustomName(); // Сохраняем имя
            net.minecraft.util.math.Box origBB = entity.getBoundingBox(); // Сохраняем оригинальный хитбокс
            
            // Сохраняем настройки команды для скрытия префиксов/суффиксов
            net.minecraft.scoreboard.AbstractTeam team = entity.getScoreboardTeam();
            net.minecraft.scoreboard.AbstractTeam.VisibilityRule origVisibility = team != null ? team.getNameTagVisibilityRule() : null;
            
            // Сохраняем флаги (огонь, присед)
            boolean wasOnFire = entity.isOnFire();
            boolean wasSneaking = entity.isSneaking();
            
            // Сохраняем экипировку (броня и предметы в руках)
            ItemStack head = entity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legs = entity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feet = entity.getEquippedStack(EquipmentSlot.FEET);
            ItemStack main = entity.getEquippedStack(EquipmentSlot.MAINHAND);
            ItemStack off = entity.getEquippedStack(EquipmentSlot.OFFHAND);

            // Убираем всё лишнее
            entity.hurtTime = 0; // Игнорируем HitColor/покраснение
            entity.deathTime = 0; // Убираем покраснение от анимации смерти
            entity.setFireTicks(0); // Убираем огонь
            entity.setGlowing(false); // Убираем эффект свечения

            // Принудительно отключаем флаги через setFlag (теперь доступно через AW)
            entity.setFlag(0, false); // Гасим огонь (ON_FIRE_FLAG)
            entity.setFlag(1, false); // Отключаем присед (SNEAKING_FLAG)
            entity.setFlag(3, false); // Отключаем спринт (SPRINTING_FLAG)

            // Профессиональное управление ником и видимостью (Minecraft 1.21.4)
            if (soulName.isEnabled()) {
                entity.setCustomName(entity.getName()); // Устанавливаем оригинальное имя
                entity.setCustomNameVisible(true);
                entity.setInvisible(false); // Делаем видимым для рендерера
            } else {
                // Устанавливаем абсолютно пустое имя
                entity.setCustomName(net.minecraft.text.Text.empty());
                entity.setCustomNameVisible(false); // Скрываем видимость кастомного имени
                entity.setInvisible(false); // Модель ДОЛЖНА быть видимой
                
                // Включаем флаг скрытности (помогает подавить ники игроков)
                entity.setSneaking(true);
                
                // Если игрок в команде, принудительно выключаем отображение имен этой команды
                if (team instanceof net.minecraft.scoreboard.Team scoreboardTeam) {
                    scoreboardTeam.setNameTagVisibilityRule(net.minecraft.scoreboard.AbstractTeam.VisibilityRule.NEVER);
                }

                // Хак: перемещаем хитбокс далеко вниз. Рендерер ника (и других оверлеев) 
                // привязан к верхней границе хитбокса. Модель игрока остается на месте.
                entity.setBoundingBox(new net.minecraft.util.math.Box(0, -2000, 0, 0, -2000, 0));
            }

            entity.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            entity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

            var dispatcher = mc.getEntityRenderDispatcher();
            var buffer = mc.getBufferBuilders().getEntityVertexConsumers();
            float yaw = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
            dispatcher.render(entity, 0.0, 0.0, 0.0, yaw, ms, buffer, 15728880);
            buffer.draw();

            // Восстанавливаем состояние сущности
            entity.hurtTime = origHurt;
            entity.deathTime = origDeath; // Восстанавливаем время смерти
            entity.setFireTicks(origFire);
            entity.setGlowing(origGlowing); // Восстанавливаем состояние свечения

            entity.setFlag(0, wasOnFire);
            entity.setFlag(1, wasSneaking);
            entity.setCustomNameVisible(origNameVisible); // Восстанавливаем оригинальную видимость
            entity.setInvisible(origInvisible); // Восстанавливаем невидимость
            entity.setCustomName(origCustomName); // Восстанавливаем оригинальное имя
            entity.setBoundingBox(origBB); // Восстанавливаем оригинальный хитбокс
            
            // Восстанавливаем видимость команды
            if (team instanceof net.minecraft.scoreboard.Team scoreboardTeam && origVisibility != null) {
                scoreboardTeam.setNameTagVisibilityRule(origVisibility);
            }

            entity.equipStack(EquipmentSlot.HEAD, head);
            entity.equipStack(EquipmentSlot.CHEST, chest);
            entity.equipStack(EquipmentSlot.LEGS, legs);
            entity.equipStack(EquipmentSlot.FEET, feet);
            entity.equipStack(EquipmentSlot.MAINHAND, main);
            entity.equipStack(EquipmentSlot.OFFHAND, off);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            ms.pop();
        }
    }
}
