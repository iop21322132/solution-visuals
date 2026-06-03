package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.game.EventJump;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.animations.Animation;
import farvix.solution.api.util.animations.Easing;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(name = "Jump Circle", category = ModuleCategory.VISUALS, description = "Круг при прыжке")
public class JumpCircle extends Module {

    // ── Settings ──────────────────────────────────────────────────────────────
    public final ModeSetting texture = new ModeSetting("jumpcircle.texture", this,
            "jumpcircle.texture.circle",
            "jumpcircle.texture.vertyxua",
            "jumpcircle.texture.pentogram");

    public final SliderSetting lifetime = new SliderSetting(
            "jumpcircle.lifetime", this, 2f, 1f, 5f, 0.5f);

    public final SliderSetting radius = new SliderSetting(
            "jumpcircle.radius", this, 1.5f, 0.5f, 4f, 0.1f);

    public final ColorSetting color = new ColorSetting(
            "Цвет", this, new FixColor(100, 180, 255, 255).getRGB());

    // ── State ─────────────────────────────────────────────────────────────────
    private final CopyOnWriteArrayList<Circle> circles = new CopyOnWriteArrayList<>();

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onJump(EventJump e) {
        if (mc.player == null) return;
        Vec3d pos = mc.player.getPos().add(0, 0.05, 0);
        circles.add(new Circle(pos, (long)(lifetime.getValue() * 1000f)));
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.player == null || mc.world == null || circles.isEmpty()) return;

        MatrixStack ms = e.getMatrices();
        ms.push();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();

        RenderSystem.setShaderTexture(0, getCircleTexture());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        java.awt.Color col = new java.awt.Color(color.get(), true);
        int r = col.getRed(), g = col.getGreen(), b = col.getBlue();

        circles.removeIf(c -> System.currentTimeMillis() - c.spawnTime > c.lifetimeMs);

        for (Circle c : circles) {
            long elapsed  = System.currentTimeMillis() - c.spawnTime;
            float progress = MathHelper.clamp((float) elapsed / c.lifetimeMs, 0f, 1f);

            // Expand radius with EASE_OUT_CIRC, fade alpha out
            float expandedRadius = radius.getValue() * (float) Easing.EASE_OUT_CIRC.apply((double) progress);
            float alpha = 1f - progress; // linear fade

            int rgb = new FixColor(r, g, b, (int)(255 * alpha)).getRGB();

            float half = expandedRadius / 2f;
            Vec3d p = c.pos;

            Matrix4f m = ms.peek().getPositionMatrix();

            // Flat quad on the ground (XZ plane)
            buf.vertex(m, (float)(p.x - half), (float) p.y, (float)(p.z - half)).texture(0, 0).color(rgb);
            buf.vertex(m, (float)(p.x + half), (float) p.y, (float)(p.z - half)).texture(1, 0).color(rgb);
            buf.vertex(m, (float)(p.x + half), (float) p.y, (float)(p.z + half)).texture(1, 1).color(rgb);
            buf.vertex(m, (float)(p.x - half), (float) p.y, (float)(p.z + half)).texture(0, 1).color(rgb);
        }

        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        ms.pop();
    }

    private Identifier getCircleTexture() {
        return switch (texture.getCurrentMode()) {
            case "jumpcircle.texture.vertyxua" -> Identifier.of("solution", "textures/vertyxua.png");
            case "jumpcircle.texture.pentogram" -> Identifier.of("solution", "textures/pentogram.png");
            default -> Identifier.of("solution", "textures/circle.png");
        };
    }

    // ── Circle data ───────────────────────────────────────────────────────────

    private static class Circle {
        final Vec3d pos;
        final long  spawnTime   = System.currentTimeMillis();
        final long  lifetimeMs;

        Circle(Vec3d pos, long lifetimeMs) {
            this.pos        = pos;
            this.lifetimeMs = lifetimeMs;
        }
    }
}
