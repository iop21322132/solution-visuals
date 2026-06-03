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
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@ModuleInfo(name = "Range Distance", category = ModuleCategory.ENVIRONMENT, description = "Визуализирует радиус атаки игроков")
public class RangeDistance extends Module implements QuickImports {

    public final BooleanSetting renderSelf = new BooleanSetting("Рендерить себя", this);
    public final BooleanSetting renderOthers = new BooleanSetting("Рендерить других", this);
    public final SliderSetting lineWidth = new SliderSetting("Толщина линии", this, 2.0f, 0.5f, 5.0f, 0.1f);
    public final ColorSetting playerColor = new ColorSetting("Цвет игрока", this, new FixColor(0, 255, 0, 180).getRGB());
    public final ColorSetting otherPlayerColor = new ColorSetting("Цвет других игроков", this, new FixColor(0, 255, 0, 180).getRGB());
    public final ColorSetting targetableColor = new ColorSetting("Цвет цели", this, new FixColor(255, 0, 0, 180).getRGB());

    public RangeDistance() {
        renderSelf.setEnabled(true);
        renderOthers.setEnabled(true);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.player == null || mc.world == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false); // Render through blocks
        RenderSystem.enableDepthTest(); // Still respect depth for other objects

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        GL11.glLineWidth(lineWidth.getValue());

        MatrixStack ms = e.getMatrices();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && !renderSelf.isEnabled()) continue;
            if (player != mc.player && !renderOthers.isEnabled()) continue;
            if (player != mc.player && isNakedInvisible(player)) continue;

            Vec3d playerPos = player.getLerpedPos(e.getTickDelta());
            double attackRange = getAttackRange(player);

            Color circleColor;

            // Check if this player is targetable by mc.player OR mc.player is targetable by this player
            boolean isTargetableByLocalPlayer = mc.player.distanceTo(player) <= getAttackRange(mc.player);
            boolean isLocalPlayerTargetableByThis = player.distanceTo(mc.player) <= attackRange;

            if ((isTargetableByLocalPlayer || isLocalPlayerTargetableByThis) && player != mc.player) {
                circleColor = new Color(targetableColor.get(), true);
            } else if (player == mc.player) {
                circleColor = new Color(this.playerColor.get(), true);
            } else {
                circleColor = new Color(otherPlayerColor.get(), true);
            }

            drawCircle(ms, playerPos.x, playerPos.y, playerPos.z, (float) attackRange, circleColor);
        }

        GL11.glLineWidth(1.0f); // Reset line width
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private double getAttackRange(PlayerEntity player) {
        // В версии 1.21.4 за дистанцию удара по игрокам отвечает 
        // атрибут ENTITY_INTERACTION_RANGE. По умолчанию он равен 3.0.
        double range = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
        return range > 0 ? range : 3.0;
    }

    private void drawCircle(MatrixStack ms, double x, double y, double z, float radius, Color color) {
        ms.push();
        ms.translate(x, y, z);

        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        int segments = 60; // Number of segments to draw the circle

        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            float dx = (float) (radius * Math.cos(angle));
            float dz = (float) (radius * Math.sin(angle));
            buffer.vertex(matrix, dx, 0.01f, dz).color(r, g, b, a); // 0.01f to avoid Z-fighting with ground
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        ms.pop();
    }

    private boolean isNakedInvisible(PlayerEntity player) {
        if (!player.isInvisible()) return false;
        return player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
            && player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
            && player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
            && player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }
}