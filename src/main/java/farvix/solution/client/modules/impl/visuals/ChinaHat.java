package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "ChinaHat", category = ModuleCategory.PLAYER,
        description = "Китайская шляпка над головой игроков")
public class ChinaHat extends Module implements QuickImports {

    private static final int SEGMENTS = 64;

    // ── Для себя ─────────────────────────────────────────────────────────────
    public final BooleanSetting renderSelf = new BooleanSetting(
            "Для себя", this);

    public final SliderSetting radiusSelf = new SliderSetting(
            "Ширина (себя)", this, 0.7f, 0.3f, 1.4f, 0.05f)
            .setVisible(() -> renderSelf.isEnabled());

    public final ColorSetting colorSelf = new ColorSetting(
            "Цвет (себя)", this, new FixColor(100, 150, 200, 255).getRGB())
            {{ /* visible handled by component */ }};

    // ── Для других ───────────────────────────────────────────────────────────
    public final BooleanSetting renderOthers = new BooleanSetting(
            "Для других", this);

    public final SliderSetting radiusOthers = new SliderSetting(
            "Ширина (других)", this, 0.7f, 0.3f, 1.4f, 0.05f)
            .setVisible(() -> renderOthers.isEnabled());

    public final ColorSetting colorOthers = new ColorSetting(
            "Цвет (других)", this, new FixColor(100, 150, 200, 255).getRGB())
            {{ /* visible handled by component */ }};

    // ─────────────────────────────────────────────────────────────────────────

    public ChinaHat() {
        renderSelf.setEnabled(true);   // показываем себя по умолчанию
        renderOthers.setEnabled(true);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.world == null || mc.player == null) return;

        float tickDelta = e.getTickDelta();
        MatrixStack matrices = e.getMatrices();

        // Enable blend ONCE before all hats
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        for (PlayerEntity player : mc.world.getPlayers()) {
            boolean isSelf = player == mc.player;

            if (isSelf  && !renderSelf.isEnabled())  continue;
            if (!isSelf && !renderOthers.isEnabled()) continue;
            // В первом лице себя не видно
            if (isSelf  && mc.options.getPerspective().isFirstPerson()) continue;
            // Для других: не показываем если полностью голые (нет ни одного элемента брони)
            if (!isSelf && !hasAnyArmor(player)) continue;

            float radius = isSelf ? radiusSelf.getValue()  : radiusOthers.getValue();
            java.awt.Color c = new java.awt.Color(isSelf ? colorSelf.get() : colorOthers.get(), true);
            float r = c.getRed() / 255f, g = c.getGreen() / 255f, b = c.getBlue() / 255f;
            float alpha = c.getAlpha() / 255f; // alpha from color picker right slider

            renderHat(matrices, player, tickDelta, radius, alpha, r, g, b);
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private boolean hasAnyArmor(PlayerEntity player) {
        return !player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isEmpty()
            || !player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isEmpty()
            || !player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS).isEmpty()
            || !player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET).isEmpty();
    }

    private void renderHat(MatrixStack matrices, PlayerEntity player, float tickDelta,
                            float radius, float alpha, float r, float g, float b) {
        Vec3d pos = player.getLerpedPos(tickDelta);

        matrices.push();

        // EventRender3D.Game матрица работает в абсолютных мировых координатах
        matrices.translate(pos.x, pos.y, pos.z);

        float bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

        float eyeHeight = player.getEyeHeight(player.getPose());
        matrices.translate(0.0, eyeHeight - 0.15f, 0.0);

        float headYawAbs = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);
        float netHeadYaw = headYawAbs - bodyYaw;
        float headPitch  = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-netHeadYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch));

        float baseToCrown   = player.isInSneakingPose() ? 0.305f : 0.265f;
        float pitchRad      = (float) Math.toRadians(MathHelper.clamp(headPitch, -90f, 90f));
        float cosPitch      = MathHelper.clamp((float) Math.cos(pitchRad), 0f, 1f);
        float tiltFactor    = 1f - cosPitch;
        float upMul         = headPitch < 0f ? 1.6f : 1f;
        float clearance     = 0.03f * cosPitch + (headPitch < 0f ? 0.008f * tiltFactor : 0f);
        float dynamicOffset = -0.05f + 0.04f + 0.08f * tiltFactor * upMul;
        float forwardOffset = -0.06f * tiltFactor * Math.signum(headPitch) * upMul;
        matrices.translate(0.0, (baseToCrown + clearance) + dynamicOffset, forwardOffset);

        drawCone(matrices, radius, 0.35f, r, g, b, alpha);

        matrices.pop();
    }

    private static void drawCone(MatrixStack matrices, float radius, float height,
                                  float r, float g, float b, float alpha) {
        if (alpha <= 0.01f) return;

        float tipAlpha  = MathHelper.clamp(alpha, 0f, 1f);
        float baseAlpha = MathHelper.clamp(tipAlpha * 0.7f, 0f, 1f);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        GL11.glDisable(GL11.GL_CULL_FACE);

        Matrix4f m = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();

        BufferBuilder buf = tess.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = (i / (double) SEGMENTS) * Math.PI * 2.0;
            float x = (float)(Math.cos(angle) * radius);
            float z = (float)(Math.sin(angle) * radius);
            buf.vertex(m, x, 0f, z).color(r, g, b, baseAlpha);
            buf.vertex(m, 0f, height, 0f).color(r, g, b, tipAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        GL11.glEnable(GL11.GL_CULL_FACE);
    }
}
