package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.game.EventUpdate;
import farvix.solution.api.events.impl.render.EventRender3D;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(name = "World Particles", category = ModuleCategory.VISUALS,
        description = "Частицы вокруг игрока")
public class WorldParticles extends Module {

    // ── Settings ──────────────────────────────────────────────────────────────
    public final ModeSetting type = new ModeSetting(
            "worldparticles.type", this,
            "worldparticles.type.cube",
            "worldparticles.type.glow",
            "worldparticles.type.dollar",
            "worldparticles.type.crown",
            "worldparticles.type.heart",
            "worldparticles.type.star",
            "worldparticles.type.lightning",
            "worldparticles.type.snowbag",
            "worldparticles.type.snowblast",
            "worldparticles.type.snowbrick",
            "worldparticles.type.sparkle"
    );

    public final ColorSetting color = new ColorSetting(
            "worldparticles.color", this, new FixColor(255, 255, 255, 255).getRGB());
    public final SliderSetting count = new SliderSetting(
            "worldparticles.count", this, 60f, 10f, 200f, 5f);
    public final SliderSetting radius = new SliderSetting(
            "worldparticles.radius", this, 20f, 10f, 100f, 1f);
    public final SliderSetting particleSize = new SliderSetting(
            "worldparticles.size", this, 0.2f, 0.05f, 1.0f, 0.05f);
    public final SliderSetting brightness = new SliderSetting(
            "Яркость", this, 1.0f, 0.0f, 1.0f, 0.1f);

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<Particle> particles = new CopyOnWriteArrayList<>();
    private static final Random RNG = new Random();

    private Identifier getTexture() {
        return switch (type.getCurrentMode()) {
            case "worldparticles.type.glow" -> Identifier.of("solution", "textures/hud/bloom.png");
            case "worldparticles.type.dollar" -> Identifier.of("solution", "textures/effect/dollar.png");
            case "worldparticles.type.crown" -> Identifier.of("solution", "textures/effect/crown.png");
            case "worldparticles.type.heart" -> Identifier.of("solution", "textures/effect/heart.png");
            case "worldparticles.type.star" -> Identifier.of("solution", "textures/effect/star.png");
            case "worldparticles.type.lightning" -> Identifier.of("solution", "textures/effect/lightning.png");
            case "worldparticles.type.snowbag" -> Identifier.of("solution", "textures/effect/snowbag1.png");
            case "worldparticles.type.snowblast" -> Identifier.of("solution", "textures/effect/snowblast1.png");
            case "worldparticles.type.snowbrick" -> Identifier.of("solution", "textures/effect/snowbrich1.png");
            case "worldparticles.type.sparkle" -> Identifier.of("solution", "textures/effect/sparkle.png");
            default -> null;
        };
    }

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        Vec3d playerPos = mc.player.getPos();
        double r = radius.getValue();

        // Тикаем живые и обновляем alpha
        for (Particle p : particles) {
            p.tick();
            p.updateAlpha();
        }

        // Удаляем мёртвые
        particles.removeIf(Particle::isDead);

        // Частицы которые улетели за радиус — телепортируем обратно к игроку
        double eyeY = playerPos.y + 1.62;
        for (Particle p : particles) {
            double dx = p.pos.x - playerPos.x;
            double dy = p.pos.y - eyeY;
            double dz = p.pos.z - playerPos.z;
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            if (horizDist > r || Math.abs(dy) > r * 0.6) {
                respawn(p, playerPos, r);
            }
        }

        // Добавляем новые частицы до нужного кол-ва
        int maxCount = (int) count.getValue();
        while (particles.size() < maxCount) {
            particles.add(spawnNear(playerPos, r));
        }

        // Если кол-во уменьшили — удаляем лишние
        while (particles.size() > maxCount) {
            particles.remove(particles.size() - 1);
        }
    }

    /** Создаёт новую частицу рядом с игроком */
    private Particle spawnNear(Vec3d playerPos, double r) {
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist  = r * 0.3 + RNG.nextDouble() * r * 0.7;
        double ox = Math.cos(angle) * dist;
        double oz = Math.sin(angle) * dist;
        // Центр на уровне глаз игрока, симметрично ±radius/2 по Y
        double eyeY = playerPos.y + 1.62;
        double oy = eyeY + randomRange(-r * 0.4, r * 0.4);

        Vec3d pos = new Vec3d(playerPos.x + ox, oy, playerPos.z + oz);
        Vec3d motion = new Vec3d(
                randomRange(-1.0, 1.0),
                randomRange(-0.3, 0.3),
                randomRange(-1.0, 1.0)).multiply(0.015); // очень медленно
        Vec3d rotMotion = new Vec3d(
                randomRange(-1.0, 1.0),
                randomRange(-1.0, 1.0),
                randomRange(-1.0, 1.0)).multiply(0.04);
        long life = (long) randomRange(2000.0, 5000.0);
        float size = (float)(particleSize.getValue() * randomRange(0.6, 1.4));
        return new Particle(pos, Vec3d.ZERO, motion, rotMotion, life, size);
    }

    /** Телепортирует существующую частицу обратно к игроку */
    private void respawn(Particle p, Vec3d playerPos, double r) {
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist  = r * 0.3 + RNG.nextDouble() * r * 0.7;
        double ox = Math.cos(angle) * dist;
        double oz = Math.sin(angle) * dist;
        double eyeY = playerPos.y + 1.62;
        double oy = eyeY + randomRange(-r * 0.4, r * 0.4);

        Vec3d newPos = new Vec3d(playerPos.x + ox, oy, playerPos.z + oz);
        p.pos    = newPos;
        p.prev   = newPos;
        p.motion = new Vec3d(
                randomRange(-1.0, 1.0),
                randomRange(-0.3, 0.3),
                randomRange(-1.0, 1.0)).multiply(0.015);
        p.resetTimer();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.player == null || mc.world == null || particles.isEmpty()) return;

        MatrixStack ms = e.getMatrices();
        Camera camera = mc.gameRenderer.getCamera();
        float pt = e.getPartialTicks();

        Identifier tex = getTexture();

        if (tex != null) {
            // ── Billboards ────────────────────────────────────────────────────────
            ms.push();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, tex);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

            float bright = brightness.getValue();
            float normalBlendAlpha = 1.0f - bright;

            // --- ПРОХОД 1: Обычный рендер (Не светящаяся ПНГ) ---
            if (normalBlendAlpha > 0.0f) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
                renderParticleBatch(ms, camera, pt, normalBlendAlpha);
            }

            // --- ПРОХОД 2: Аддитивный рендер (Светящаяся картинка) ---
            if (bright > 0.0f) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                renderParticleBatch(ms, camera, pt, bright);
            }

            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            ms.pop();
        } else if (type.is("worldparticles.type.cube")) {
            // ── Wireframe cubes ───────────────────────────────────────────────────
            ms.push();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            BufferBuilder lines = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            Box unit = new Box(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
            for (Particle p : particles) {
                Vec3d pos = lerp(p.prev, p.pos, pt);
                Vec3d rot = lerp(p.prevRot, p.rotate, pt);

                ms.push();
                ms.translate(pos.x, pos.y, pos.z);
                ms.multiply(new Quaternionf().rotationXYZ((float)rot.x, (float)rot.y, (float)rot.z));
                ms.scale(p.size, p.size, p.size);

                renderDiagonals(ms, lines, unit, withAlpha(p.alpha * 0.4f));
                renderOutline(ms, lines, unit, withAlpha(p.alpha * 0.8f));
                ms.pop();
            }

            BuiltBuffer builtLines = lines.endNullable();
            if (builtLines != null) BufferRenderer.drawWithGlobalProgram(builtLines);

            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            ms.pop();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int withAlpha(float alpha) {
        return withAlpha(alpha, 1.0f);
    }

    private int withAlpha(float alpha, float layerMultiplier) {
        java.awt.Color c = new java.awt.Color(color.get(), true);
        int baseAlpha = c.getAlpha(); // Работает ползунок прозрачности цвета!
        int a = Math.max(0, Math.min(255, (int)(baseAlpha * alpha * layerMultiplier)));
        return new FixColor(c.getRed(), c.getGreen(), c.getBlue(), a).getRGB();
    }

    private static Vec3d lerp(Vec3d prev, Vec3d cur, float t) {
        return new Vec3d(
                prev.x + (cur.x - prev.x) * t,
                prev.y + (cur.y - prev.y) * t,
                prev.z + (cur.z - prev.z) * t);
    }

    private void renderParticleBatch(MatrixStack ms, Camera camera, float pt, float layerMultiplier) {
        BufferBuilder bloom = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (Particle p : particles) {
            Vec3d pos = lerp(p.prev, p.pos, pt);
            Vec3d rot = lerp(p.prevRot, p.rotate, pt);
            float big = 4.0f * p.size;
            boolean isGlow = type.is("worldparticles.type.glow");
            int col = withAlpha(p.alpha * (isGlow ? 0.4f : 0.8f), layerMultiplier);

            ms.push();
            ms.translate(pos.x, pos.y, pos.z);
            ms.multiply(camera.getRotation());
            boolean isCrownOrHeart = type.getCurrentMode().equals("worldparticles.type.crown") || type.getCurrentMode().equals("worldparticles.type.heart");
            float zRot = (float) rot.z;
            if (isCrownOrHeart) {
                zRot += (float) Math.PI;
            }
            ms.multiply(new Quaternionf().rotationZ(zRot));
            
            Matrix4f m = ms.peek().getPositionMatrix();
            bloom.vertex(m, -big/2f,  big/2f, 0).texture(0,1).color(col);
            bloom.vertex(m,  big/2f,  big/2f, 0).texture(1,1).color(col);
            bloom.vertex(m,  big/2f, -big/2f, 0).texture(1,0).color(col);
            bloom.vertex(m, -big/2f, -big/2f, 0).texture(0,0).color(col);
            ms.pop();
        }

        BuiltBuffer builtBloom = bloom.endNullable();
        if (builtBloom != null) BufferRenderer.drawWithGlobalProgram(builtBloom);
    }

    private static double randomRange(double min, double max) {
        return min + RNG.nextDouble() * (max - min);
    }

    private static void renderDiagonals(MatrixStack ms, BufferBuilder b, Box box, int color) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float x1=(float)box.minX, y1=(float)box.minY, z1=(float)box.minZ;
        float x2=(float)box.maxX, y2=(float)box.maxY, z2=(float)box.maxZ;
        b.vertex(m,x1,y1,z1).color(color); b.vertex(m,x2,y2,z2).color(color);
        b.vertex(m,x2,y1,z1).color(color); b.vertex(m,x1,y2,z2).color(color);
        b.vertex(m,x1,y1,z2).color(color); b.vertex(m,x2,y2,z1).color(color);
        b.vertex(m,x2,y1,z2).color(color); b.vertex(m,x1,y2,z1).color(color);
    }

    private static void renderOutline(MatrixStack ms, BufferBuilder b, Box box, int color) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float x1=(float)box.minX, y1=(float)box.minY, z1=(float)box.minZ;
        float x2=(float)box.maxX, y2=(float)box.maxY, z2=(float)box.maxZ;
        // bottom
        b.vertex(m,x1,y1,z1).color(color); b.vertex(m,x2,y1,z1).color(color);
        b.vertex(m,x2,y1,z1).color(color); b.vertex(m,x2,y1,z2).color(color);
        b.vertex(m,x2,y1,z2).color(color); b.vertex(m,x1,y1,z2).color(color);
        b.vertex(m,x1,y1,z2).color(color); b.vertex(m,x1,y1,z1).color(color);
        // top
        b.vertex(m,x1,y2,z1).color(color); b.vertex(m,x2,y2,z1).color(color);
        b.vertex(m,x2,y2,z1).color(color); b.vertex(m,x2,y2,z2).color(color);
        b.vertex(m,x2,y2,z2).color(color); b.vertex(m,x1,y2,z2).color(color);
        b.vertex(m,x1,y2,z2).color(color); b.vertex(m,x1,y2,z1).color(color);
        // verticals
        b.vertex(m,x1,y1,z1).color(color); b.vertex(m,x1,y2,z1).color(color);
        b.vertex(m,x2,y1,z1).color(color); b.vertex(m,x2,y2,z1).color(color);
        b.vertex(m,x2,y1,z2).color(color); b.vertex(m,x2,y2,z2).color(color);
        b.vertex(m,x1,y1,z2).color(color); b.vertex(m,x1,y2,z2).color(color);
    }

    // ── Particle ──────────────────────────────────────────────────────────────

    private static class Particle {
        Vec3d prev, pos;
        Vec3d prevRot, rotate;
        Vec3d motion, rotMotion;
        final long liveTicks;
        final long fadeIn  = 500;
        final long fadeOut = 500;
        final float size;
        float alpha = 0f;
        private long startTime = System.currentTimeMillis();

        Particle(Vec3d pos, Vec3d rotate, Vec3d motion, Vec3d rotMotion,
                 long liveTicks, float size) {
            this.pos      = this.prev    = pos;
            this.rotate   = this.prevRot = rotate;
            this.motion   = motion;
            this.rotMotion = rotMotion;
            this.liveTicks = liveTicks;
            this.size      = size;
        }

        void tick() {
            prev      = pos;
            prevRot   = rotate;
            pos       = pos.add(motion);
            rotate    = rotate.add(rotMotion);
            motion    = motion.multiply(0.98);
            rotMotion = rotMotion.multiply(0.98);
        }

        void updateAlpha() {
            long el = System.currentTimeMillis() - startTime;
            if (el < fadeIn) {
                alpha = Math.min(1f, el / (float) fadeIn);
            } else if (el > liveTicks - fadeOut) {
                long left = liveTicks - el;
                alpha = Math.max(0f, left / (float) fadeOut);
            } else {
                alpha = 1f;
            }
        }

        /** Сбрасывает таймер при телепортации — плавное появление заново */
        void resetTimer() {
            startTime = System.currentTimeMillis();
            alpha = 0f;
        }

        boolean isDead() {
            return System.currentTimeMillis() - startTime > liveTicks;
        }
    }
}
