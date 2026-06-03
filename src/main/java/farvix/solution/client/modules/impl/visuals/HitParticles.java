package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.entity.EventAttackEntity;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(name = "Hit Particles", category = ModuleCategory.PLAYER,
        description = "Частицы при ударе по сущности")
public class HitParticles extends Module {

    // ── Settings ──────────────────────────────────────────────────────────────
    public final ModeSetting target = new ModeSetting(
            "hitparticles.target", this,
            "hitparticles.target.all",
            "hitparticles.target.players",
            "hitparticles.target.mobs"
    );

    public final ModeSetting effect = new ModeSetting(
            "hitparticles.effect", this,
            "hitparticles.effect.crown",
            "hitparticles.effect.dollar",
            "hitparticles.effect.heart",
            "hitparticles.effect.lightning",
            "hitparticles.effect.show1",
            "hitparticles.effect.snowbag",
            "hitparticles.effect.snowblast",
            "hitparticles.effect.snowbrick",
            "hitparticles.effect.snowflake",
            "hitparticles.effect.snownew",
            "hitparticles.effect.sparkle",
            "hitparticles.effect.star",
            "hitparticles.effect.thor"
    );

    public final ColorSetting color = new ColorSetting(
            "hitparticles.color", this, new FixColor(255, 255, 255, 255).getRGB());
    
    public final SliderSetting count = new SliderSetting(
            "hitparticles.count", this, 15f, 5f, 50f, 1f);
    
    public final SliderSetting lifetime = new SliderSetting(
            "hitparticles.lifetime", this, 1.0f, 0.2f, 3.0f, 0.1f);
    
    public final SliderSetting gravity = new SliderSetting(
            "hitparticles.gravity", this, 0.3f, 0.0f, 3.0f, 0.05f);
    
    public final SliderSetting spread = new SliderSetting(
            "hitparticles.spread", this, 0.8f, 0.1f, 2.0f, 0.1f);
    
    public final SliderSetting brightness = new SliderSetting(
            "Яркость", this, 1.0f, 0.0f, 1.0f, 0.1f);

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<Particle> particles = new CopyOnWriteArrayList<>();
    private static final Random RNG = new Random();

    @Override
    public void onDisable() {
        super.onDisable();
        particles.clear();
    }

    private Identifier getTexture() {
        return switch (effect.getCurrentMode()) {
            case "hitparticles.effect.crown" -> Identifier.of("solution", "textures/effect/crown.png");
            case "hitparticles.effect.dollar" -> Identifier.of("solution", "textures/effect/dollar.png");
            case "hitparticles.effect.heart" -> Identifier.of("solution", "textures/effect/heart.png");
            case "hitparticles.effect.lightning" -> Identifier.of("solution", "textures/effect/lightning.png");
            case "hitparticles.effect.show1" -> Identifier.of("solution", "textures/effect/show1.png");
            case "hitparticles.effect.snowbag" -> Identifier.of("solution", "textures/effect/snowbag1.png");
            case "hitparticles.effect.snowblast" -> Identifier.of("solution", "textures/effect/snowblast1.png");
            case "hitparticles.effect.snowbrick" -> Identifier.of("solution", "textures/effect/snowbrich1.png");
            case "hitparticles.effect.snowflake" -> Identifier.of("solution", "textures/effect/snowflake.png");
            case "hitparticles.effect.snownew" -> Identifier.of("solution", "textures/effect/snownew1.png");
            case "hitparticles.effect.sparkle" -> Identifier.of("solution", "textures/effect/sparkle.png");
            case "hitparticles.effect.star" -> Identifier.of("solution", "textures/effect/star.png");
            case "hitparticles.effect.thor" -> Identifier.of("solution", "textures/effect/thor.png");
            default -> null;
        };
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (e.getTarget() == null || mc.player == null) return;
        if (!(e.getTarget() instanceof LivingEntity living)) return;

        // Проверка соответствия цели настройкам
        String t = target.getCurrentMode();
        boolean isPlayer = living instanceof PlayerEntity;
        boolean isMob = living instanceof MobEntity && !isPlayer;

        if (t.equals("hitparticles.target.players") && !isPlayer) return;
        if (t.equals("hitparticles.target.mobs") && !isMob) return;

        // Позиция спавна — центр тела цели
        double halfHeight = living.getHeight() / 2.0;
        Vec3d spawnPos = living.getPos().add(0, halfHeight, 0);

        int pCount = (int) count.getValue();
        double pSpread = spread.getValue();
        double pLifetime = lifetime.getValue();

        for (int i = 0; i < pCount; i++) {
            // Вектор разлёта во всех направлениях
            double angle1 = RNG.nextDouble() * Math.PI * 2;
            double angle2 = Math.acos(2.0 * RNG.nextDouble() - 1.0);
            
            double speedVal = 0.05 + RNG.nextDouble() * pSpread * 0.15;
            double vx = Math.sin(angle2) * Math.cos(angle1) * speedVal;
            double vy = (Math.sin(angle2) * Math.sin(angle1) * 0.5 + 0.5) * speedVal; // небольшой импульс вверх
            double vz = Math.cos(angle2) * speedVal;

            Vec3d motion = new Vec3d(vx, vy, vz);
            
            // Вращение по оси Z
            Vec3d rotMotion = new Vec3d(
                    0, 0,
                    randomRange(-1.0, 1.0) * 0.2
            );

            float sz = (float) (0.15f * randomRange(0.6, 1.4));
            long liveMs = (long) (pLifetime * 1000f * randomRange(0.8, 1.2));

            particles.add(new Particle(spawnPos, Vec3d.ZERO, motion, rotMotion, liveMs, sz));
        }

        // Ограничение по максимальному количеству активных частиц (оптимизация)
        if (particles.size() > 400) {
            particles.subList(0, particles.size() - 400).clear();
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (particles.isEmpty()) return;

        double grav = gravity.getValue() * 0.015;
        for (Particle p : particles) {
            p.tick(grav, mc.world);
        }

        particles.removeIf(Particle::isDead);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.player == null || mc.world == null || particles.isEmpty()) return;

        Identifier tex = getTexture();
        if (tex == null) return;

        MatrixStack ms = e.getMatrices();
        Camera camera = mc.gameRenderer.getCamera();
        float pt = e.getPartialTicks();

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
    }

    private void renderParticleBatch(MatrixStack ms, Camera camera, float pt, float layerMultiplier) {
        BufferBuilder bloom = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (Particle p : particles) {
            Vec3d pos = lerp(p.prev, p.pos, pt);
            Vec3d rot = lerp(p.prevRot, p.rotate, pt);
            float big = 4.0f * p.size;
            int col = withAlpha(p.getAlpha(), layerMultiplier);

            ms.push();
            ms.translate(pos.x, pos.y, pos.z);
            ms.multiply(camera.getRotation());
            boolean isCrownOrHeart = effect.getCurrentMode().equals("hitparticles.effect.crown") || effect.getCurrentMode().equals("hitparticles.effect.heart");
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int withAlpha(float alpha, float layerMultiplier) {
        java.awt.Color c = new java.awt.Color(color.get(), true);
        int baseAlpha = c.getAlpha(); // Берем прозрачность из настроек цвета
        int a = Math.max(0, Math.min(255, (int)(baseAlpha * alpha * layerMultiplier)));
        return new FixColor(c.getRed(), c.getGreen(), c.getBlue(), a).getRGB();
    }

    private static Vec3d lerp(Vec3d prev, Vec3d cur, float t) {
        return new Vec3d(
                prev.x + (cur.x - prev.x) * t,
                prev.y + (cur.y - prev.y) * t,
                prev.z + (cur.z - prev.z) * t);
    }

    private static double randomRange(double min, double max) {
        return min + RNG.nextDouble() * (max - min);
    }

    // ── Particle ──────────────────────────────────────────────────────────────

    private static class Particle {
        Vec3d prev, pos;
        Vec3d prevRot, rotate;
        Vec3d motion;
        Vec3d rotMotion;
        final long liveTicks;
        final float size;
        final long startTime = System.currentTimeMillis();

        Particle(Vec3d pos, Vec3d rotate, Vec3d motion, Vec3d rotMotion,
                 long liveTicks, float size) {
            this.pos      = this.prev    = pos;
            this.rotate   = this.prevRot = rotate;
            this.motion   = motion;
            this.rotMotion = rotMotion;
            this.liveTicks = liveTicks;
            this.size      = size;
        }

        void tick(double grav, net.minecraft.client.world.ClientWorld world) {
            prev      = pos;
            prevRot   = rotate;
            
            Vec3d nextPos = pos.add(motion);
            boolean collided = false;
            
            if (world != null) {
                BlockPos nextBlockPos = BlockPos.ofFloored(nextPos.x, nextPos.y, nextPos.z);
                BlockState state = world.getBlockState(nextBlockPos);
                if (!state.getCollisionShape(world, nextBlockPos).isEmpty()) {
                    collided = true;
                    if (motion.y < 0) {
                        pos = new Vec3d(nextPos.x, nextBlockPos.getY() + 1.0, nextPos.z);
                        motion = new Vec3d(motion.x * 0.8, 0, motion.z * 0.8);
                        rotMotion = rotMotion.multiply(0.8);
                    } else {
                        motion = Vec3d.ZERO;
                        rotMotion = Vec3d.ZERO;
                    }
                }
            }
            
            if (!collided) {
                pos = nextPos;
                motion = new Vec3d(motion.x * 0.96, (motion.y - grav) * 0.96, motion.z * 0.96);
                rotMotion = rotMotion.multiply(0.96);
            }
            
            rotate = rotate.add(rotMotion);
        }

        float getAlpha() {
            long el = System.currentTimeMillis() - startTime;
            return 1f - MathHelper.clamp((float) el / liveTicks, 0f, 1f);
        }

        boolean isDead() {
            return System.currentTimeMillis() - startTime > liveTicks;
        }
    }
}
