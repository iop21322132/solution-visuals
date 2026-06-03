package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ColorSetting;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import java.util.List;
import java.util.ArrayList;

@ModuleInfo(name = "Block OutLine", category = ModuleCategory.VISUALS, description = "Подсвечивает блок, на который вы смотрите, с плавной анимацией")
public class BlockOutline extends Module {

    private final SliderSetting speed = new SliderSetting("Скорость перемещения", this, 0.5f, 0.0f, 1.5f, 0.1f);
    private final BooleanSetting fill = new BooleanSetting("Заливка", this);
    private final ColorSetting fillColor = new ColorSetting("Цвет заливки", this, new FixColor(255, 255, 255, 100).getRGB());
    private final ColorSetting lineColor = new ColorSetting("Цвет линий", this, new FixColor(255, 255, 255, 255).getRGB());

    private final Animation fadeAnimation = new Animation(250L, 1.0f, false, Easing.BOTH_CUBIC);
    private Box currentBox = null;
    private Box lastTargetOverallBox = null;
    private List<Box> lastTargetBoxes = new ArrayList<>();
    private long lastUpdateTime = System.currentTimeMillis();

    public BlockOutline() {
        fill.setValue(true);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.world == null || mc.player == null) return;

        boolean hasTarget = (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK);
        fadeAnimation.update(hasTarget);

        float fade = fadeAnimation.getValue();
        if (fade <= 0) {
            currentBox = null;
            lastTargetOverallBox = null;
            lastTargetBoxes.clear();
            return;
        }

        long now = System.currentTimeMillis();
        float deltaTime = (now - lastUpdateTime) / 1000f;
        lastUpdateTime = now;

        if (hasTarget) {
            BlockPos targetPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            VoxelShape shape = mc.world.getBlockState(targetPos).getOutlineShape(mc.world, targetPos);
            
            Box targetOverallBox;
            List<Box> targetBoxes = new ArrayList<>();
            
            if (shape.isEmpty()) {
                targetOverallBox = new Box(targetPos);
                targetBoxes.add(targetOverallBox);
            } else {
                targetOverallBox = shape.getBoundingBox().offset(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                for (Box b : shape.getBoundingBoxes()) {
                    targetBoxes.add(b.offset(targetPos.getX(), targetPos.getY(), targetPos.getZ()));
                }
            }
            
            lastTargetOverallBox = targetOverallBox;
            lastTargetBoxes = targetBoxes;
            
            if (currentBox == null || speed.getValue() <= 0) {
                currentBox = targetOverallBox;
            } else {
                float interpolationFactor = MathHelper.clamp(deltaTime / Math.max(0.01f, speed.getValue()), 0f, 1f);
                currentBox = lerpBox(currentBox, targetOverallBox, interpolationFactor);
            }
        }

        if (currentBox != null) {
            renderBlock(e.getMatrices(), currentBox, fade);
        }
    }

    private Box lerpBox(Box from, Box to, float delta) {
        double minX = from.minX + (to.minX - from.minX) * delta;
        double minY = from.minY + (to.minY - from.minY) * delta;
        double minZ = from.minZ + (to.minZ - from.minZ) * delta;
        double maxX = from.maxX + (to.maxX - from.maxX) * delta;
        double maxY = from.maxY + (to.maxY - from.maxY) * delta;
        double maxZ = from.maxZ + (to.maxZ - from.maxZ) * delta;
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private Box mapBox(Box b, Box T, Box C) {
        double tLenX = T.maxX - T.minX;
        double tLenY = T.maxY - T.minY;
        double tLenZ = T.maxZ - T.minZ;

        double cLenX = C.maxX - C.minX;
        double cLenY = C.maxY - C.minY;
        double cLenZ = C.maxZ - C.minZ;

        double minX = mapCoord(b.minX, T.minX, tLenX, C.minX, cLenX);
        double minY = mapCoord(b.minY, T.minY, tLenY, C.minY, cLenY);
        double minZ = mapCoord(b.minZ, T.minZ, tLenZ, C.minZ, cLenZ);
        double maxX = mapCoord(b.maxX, T.minX, tLenX, C.minX, cLenX);
        double maxY = mapCoord(b.maxY, T.minY, tLenY, C.minY, cLenY);
        double maxZ = mapCoord(b.maxZ, T.minZ, tLenZ, C.minZ, cLenZ);

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private double mapCoord(double val, double tMin, double tLen, double cMin, double cLen) {
        if (tLen <= 0.0) return cMin;
        return cMin + ((val - tMin) / tLen) * cLen;
    }

    private void renderBlock(MatrixStack ms, Box box, float fade) {
        if (lastTargetOverallBox == null || lastTargetBoxes.isEmpty()) return;

        ms.push();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableCull();
        
        // Отключаем тест глубины, чтобы видеть ВЕСЬ блок (все грани и линии)
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        for (Box b : lastTargetBoxes) {
            Box mappedBox = mapBox(b, lastTargetOverallBox, box);
            
            if (fill.getValue()) {
                renderBoxFill(ms, mappedBox, getFadeColor(fillColor.get(), fade));
            }
            
            renderBoxOutline(ms, mappedBox, getFadeColor(lineColor.get(), fade));
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }

    private int getFadeColor(int color, float fade) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return ((int)(a * fade) << 24) | (r << 16) | (g << 8) | b;
    }

    private void renderBoxFill(MatrixStack ms, Box box, int color) {
        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        buffer.vertex(matrix, minX, minY, minZ).color(color);
        buffer.vertex(matrix, maxX, minY, minZ).color(color);
        buffer.vertex(matrix, maxX, minY, maxZ).color(color);
        buffer.vertex(matrix, minX, minY, maxZ).color(color);

        buffer.vertex(matrix, minX, maxY, minZ).color(color);
        buffer.vertex(matrix, minX, maxY, maxZ).color(color);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(color);
        buffer.vertex(matrix, maxX, maxY, minZ).color(color);

        buffer.vertex(matrix, minX, minY, minZ).color(color);
        buffer.vertex(matrix, minX, maxY, minZ).color(color);
        buffer.vertex(matrix, maxX, maxY, minZ).color(color);
        buffer.vertex(matrix, maxX, minY, minZ).color(color);

        buffer.vertex(matrix, minX, minY, maxZ).color(color);
        buffer.vertex(matrix, maxX, minY, maxZ).color(color);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(color);
        buffer.vertex(matrix, minX, maxY, maxZ).color(color);

        buffer.vertex(matrix, minX, minY, minZ).color(color);
        buffer.vertex(matrix, minX, minY, maxZ).color(color);
        buffer.vertex(matrix, minX, maxY, maxZ).color(color);
        buffer.vertex(matrix, minX, maxY, minZ).color(color);

        buffer.vertex(matrix, maxX, minY, minZ).color(color);
        buffer.vertex(matrix, maxX, maxY, minZ).color(color);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(color);
        buffer.vertex(matrix, maxX, minY, maxZ).color(color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderBoxOutline(MatrixStack ms, Box box, int color) {
        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom
        buffer.vertex(matrix, minX, minY, minZ).color(color);
        buffer.vertex(matrix, maxX, minY, minZ).color(color);
        buffer.vertex(matrix, maxX, minY, minZ).color(color);
        buffer.vertex(matrix, maxX, minY, maxZ).color(color);
        buffer.vertex(matrix, maxX, minY, maxZ).color(color);
        buffer.vertex(matrix, minX, minY, maxZ).color(color);
        buffer.vertex(matrix, minX, minY, maxZ).color(color);
        buffer.vertex(matrix, minX, minY, minZ).color(color);

        // Top
        buffer.vertex(matrix, minX, maxY, minZ).color(color);
        buffer.vertex(matrix, maxX, maxY, minZ).color(color);
        buffer.vertex(matrix, maxX, maxY, minZ).color(color);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(color);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(color);
        buffer.vertex(matrix, minX, maxY, maxZ).color(color);
        buffer.vertex(matrix, minX, maxY, maxZ).color(color);
        buffer.vertex(matrix, minX, maxY, minZ).color(color);

        // Verticals
        buffer.vertex(matrix, minX, minY, minZ).color(color);
        buffer.vertex(matrix, minX, maxY, minZ).color(color);
        buffer.vertex(matrix, maxX, minY, minZ).color(color);
        buffer.vertex(matrix, maxX, maxY, minZ).color(color);
        buffer.vertex(matrix, maxX, minY, maxZ).color(color);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(color);
        buffer.vertex(matrix, minX, minY, maxZ).color(color);
        buffer.vertex(matrix, minX, maxY, maxZ).color(color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentBox = null;
        lastTargetOverallBox = null;
        lastTargetBoxes.clear();
        fadeAnimation.reset();
    }
}
