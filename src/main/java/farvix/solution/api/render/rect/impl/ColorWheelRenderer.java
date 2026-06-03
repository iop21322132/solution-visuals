package farvix.solution.api.render.rect.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Renders a circular HSB color wheel using a GLSL shader.
 * The wheel maps hue to angle and saturation to radius.
 * Brightness is passed as a uniform.
 */
public class ColorWheelRenderer {

    private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
            Identifier.of("solution", "core/colorwheel"),
            VertexFormats.POSITION,
            net.minecraft.client.gl.Defines.EMPTY
    );

    /**
     * Draw the HSB picker box.
     *
     * @param matrices   current MatrixStack
     * @param x          top-left X in GUI coords
     * @param y          top-left Y in GUI coords
     * @param width      width in GUI pixels
     * @param height     height in GUI pixels
     * @param brightness 0..1
     * @param guiAlpha   GUI fade alpha 0..1
     */
    public static void draw(MatrixStack matrices, float x, float y, float width, float height,
                            float brightness, float guiAlpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Window window = mc.getWindow();
        float scale = (float) window.getScaleFactor();

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Compute screen-space position (bottom-left, Y flipped for OpenGL)
        Vector3f pos = matrix.transformPosition(x, y, 0, new Vector3f()).mul(scale);
        
        // Extract matrix scale factor (includes custom clickgui scaling, animations, etc.)
        Vector3f cachedSize = new Vector3f();
        matrix.getScale(cachedSize).mul(scale);

        float screenW = width * cachedSize.x;
        float screenH = height * cachedSize.y;
        float screenX = pos.x;
        float screenY = mc.getWindow().getHeight() - screenH - pos.y;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buf.vertex(matrix, x,         y,          0);
        buf.vertex(matrix, x,         y + height, 0);
        buf.vertex(matrix, x + width, y + height, 0);
        buf.vertex(matrix, x + width, y,          0);

        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        shader.getUniformOrDefault("size")      .set(screenW, screenH);
        shader.getUniformOrDefault("location")  .set(screenX, screenY);
        shader.getUniformOrDefault("brightness").set(brightness);
        shader.getUniformOrDefault("alpha")     .set(guiAlpha);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
