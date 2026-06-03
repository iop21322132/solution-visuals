package farvix.solution.client.modules.impl.environment;

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
import net.minecraft.util.math.Box;
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
        RenderSystem.enableDepthTest(); // Включаем тест глубины, чтобы окружность не рендерилась сквозь блоки
        RenderSystem.depthMask(true); // Включаем запись в буфер глубины

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        MatrixStack ms = e.getMatrices();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && !renderSelf.isEnabled()) continue;
            if (player != mc.player && !renderOthers.isEnabled()) continue;
            if (player != mc.player && isNakedInvisible(player)) continue;

            Vec3d playerPos = player.getLerpedPos(e.getTickDelta());
            double attackRange = getAttackRange(player);

            Color circleColor = getCircleColor(player, e.getTickDelta());

            // Рисуем "жирную" окружность с помощью TRIANGLE_STRIP
            drawBoldCircle(ms, playerPos.x, playerPos.y, playerPos.z, (float) attackRange, circleColor);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private Color getCircleColor(PlayerEntity player, float partialTicks) {
        boolean hasTarget = false;
        double reach = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
        if (reach <= 0) reach = 3.0;

        // Позиция глаз атакующего с учетом плавности движения
        Vec3d eyePos = player.getLerpedPos(partialTicks).add(0, player.getEyeHeight(player.getPose()), 0);

        for (PlayerEntity other : mc.world.getPlayers()) {
            if (other == player || !other.isAlive()) continue;
            if (other != mc.player && isNakedInvisible(other)) continue;

            // Получаем актуальный хитбокс цели в пространстве
            Box box = other.getDimensions(other.getPose()).getBoxAt(other.getLerpedPos(partialTicks));
            
            // Находим кратчайшее расстояние от глаз до любой точки на поверхности хитбокса
            double x = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
            double y = Math.max(box.minY, Math.min(eyePos.y, box.maxY));
            double z = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));

            if (eyePos.distanceTo(new Vec3d(x, y, z)) <= reach) {
                hasTarget = true;
                break;
            }
        }

        if (hasTarget) return new Color(targetableColor.get(), true);
        if (player == mc.player) return new Color(playerColor.get(), true);
        return new Color(otherPlayerColor.get(), true);
    }

    private double getAttackRange(PlayerEntity player) {
        // В версии 1.21.4 за дистанцию удара по игрокам отвечает 
        // атрибут ENTITY_INTERACTION_RANGE. По умолчанию он равен 3.0.
        double range = player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
        return (range > 0 ? range : 3.0);
    }

    private void drawBoldCircle(MatrixStack ms, double x, double y, double z, float radius, Color color) {
        ms.push();
        ms.translate(x, y, z);

        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        // Вычисляем внутренний и внешний радиус для создания толщины
        float thickness = lineWidth.getValue() * 0.04f;
        float inner = radius - thickness / 2f;
        float outer = radius + thickness / 2f;
        int segments = 64;

        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            // Рисуем две вершины для формирования сегмента кольца (приподнято на 0.05f)
            buffer.vertex(matrix, inner * cos, 0.05f, inner * sin).color(r, g, b, a);
            buffer.vertex(matrix, outer * cos, 0.05f, outer * sin).color(r, g, b, a);
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