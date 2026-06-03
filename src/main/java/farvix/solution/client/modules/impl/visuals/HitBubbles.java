package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.entity.EventAttackEntity;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(name = "Hit Bubbles", category = ModuleCategory.VISUALS,
        description = "Картинка пузырей при ударе по сущности")
public class HitBubbles extends Module implements QuickImports {

    public final ColorSetting color = new ColorSetting(
            "Цвет", this, new FixColor(255, 255, 255, 220).getRGB());

    public final SliderSetting size = new SliderSetting(
            "Размер", this, 1.0f, 0.3f, 3.0f, 0.1f);

    public final SliderSetting lifetime = new SliderSetting(
            "Время жизни (сек)", this, 1.5f, 0.5f, 5.0f, 0.5f);

    private static final Identifier BUBBLE_TEX =
            Identifier.of("solution", "textures/bubble2.png");

    // ── Hit record ────────────────────────────────────────────────────────────

    private static class HitEffect {
        double x, y, z;
        float progress;   // 0..1
        long spawnTime;
        float maxLifeMs;  // в миллисекундах

        HitEffect(double x, double y, double z, float lifeSec) {
            this.x = x; this.y = y; this.z = z;
            this.spawnTime = System.currentTimeMillis();
            this.maxLifeMs = lifeSec * 1000f;
            this.progress = 1f;
        }

        void update() {
            float elapsed = (System.currentTimeMillis() - spawnTime) / maxLifeMs;
            progress = 1f - Math.min(1f, elapsed);
        }

        boolean isDead() { return progress <= 0f; }
    }

    private final List<HitEffect> effects = new ArrayList<>();

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (mc.player == null) return;
        Entity target = e.getTarget();

        float td = mc.getRenderTickCounter().getTickDelta(false);

        // Точка попадания — берём из crosshairTarget если это та же сущность
        double hitX, hitY, hitZ;
        if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr
                && ehr.getEntity() == target) {
            // Точная точка куда смотрел прицел
            hitX = ehr.getPos().x;
            hitY = ehr.getPos().y;
            hitZ = ehr.getPos().z;
        } else {
            // Fallback — центр сущности
            hitX = MathHelper.lerp(td, target.prevX, target.getX());
            hitY = MathHelper.lerp(td, target.prevY, target.getY()) + target.getHeight() * 0.5;
            hitZ = MathHelper.lerp(td, target.prevZ, target.getZ());
        }

        // Направление взгляда (от нас к точке удара)
        double px = MathHelper.lerp(td, mc.player.prevX, mc.player.getX());
        double py = MathHelper.lerp(td, mc.player.prevY, mc.player.getY()) + mc.player.getEyeHeight(mc.player.getPose());
        double pz = MathHelper.lerp(td, mc.player.prevZ, mc.player.getZ());

        double dx = hitX - px;
        double dy = hitY - py;
        double dz = hitZ - pz;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Спавним на 0.75 блока от точки удара назад к нам
        double spawnX, spawnY, spawnZ;
        if (dist > 0.001) {
            double nx = dx / dist;
            double ny = dy / dist;
            double nz = dz / dist;
            spawnX = hitX - nx * 0.75;
            spawnY = hitY - ny * 0.75;
            spawnZ = hitZ - nz * 0.75;
        } else {
            spawnX = hitX; spawnY = hitY; spawnZ = hitZ;
        }

        effects.add(new HitEffect(spawnX, spawnY, spawnZ, lifetime.getValue()));
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.player == null || effects.isEmpty()) return;

        // Обновляем эффекты по реальному времени
        Iterator<HitEffect> it = effects.iterator();
        while (it.hasNext()) {
            HitEffect ef = it.next();
            ef.update();
            if (ef.isDead()) it.remove();
        }
        if (effects.isEmpty()) return;

        Camera camera = mc.gameRenderer.getCamera();

        MatrixStack ms = e.getMatrices();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, BUBBLE_TEX);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);

        int baseColor = color.get();
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >>  8) & 0xFF;
        int b =  baseColor        & 0xFF;
        int baseAlpha = (baseColor >> 24) & 0xFF;

        float sz = size.getValue();

        for (HitEffect ef : effects) {
            float p = MathHelper.clamp(ef.progress, 0f, 1f);
            float eased = 1f - (float) Math.pow(1f - p, 2f);
            float scale = sz * (0.8f + 0.4f * (1f - p));
            int alpha = (int)(baseAlpha * eased);
            if (alpha <= 0) continue;

            // Мировые координаты — матрица уже содержит camera offset
            double wx = ef.x;
            double wy = ef.y;
            double wz = ef.z;

            // Glow слои (additive)
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA,
                    GlStateManager.DstFactor.ONE);
            for (int layer = 3; layer >= 1; layer--) {
                float glowScale = scale * (1f + layer * 0.4f);
                int glowAlpha = (int)(alpha * 0.2f / layer);
                if (glowAlpha > 0) {
                    drawBillboard(ms, camera, wx, wy, wz, glowScale, r, g, b, glowAlpha);
                }
            }

            // Основная текстура
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA,
                    GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawBillboard(ms, camera, wx, wy, wz, scale, r, g, b, alpha);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // ── Billboard ─────────────────────────────────────────────────────────────

    private void drawBillboard(MatrixStack ms, Camera camera,
                                double rx, double ry, double rz,
                                float size,
                                int r, int g, int b, int a) {
        ms.push();
        ms.translate(rx, ry, rz);
        // Поворачиваем billboard лицом к камере (полный spherical billboard)
        ms.multiply(camera.getRotation());

        float half = size / 2f;
        Matrix4f m = ms.peek().getPositionMatrix();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);

        buf.vertex(m, -half, -half, 0).texture(0f, 1f).color(r, g, b, a);
        buf.vertex(m,  half, -half, 0).texture(1f, 1f).color(r, g, b, a);
        buf.vertex(m,  half,  half, 0).texture(1f, 0f).color(r, g, b, a);
        buf.vertex(m, -half,  half, 0).texture(0f, 0f).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        ms.pop();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        effects.clear();
    }
}
