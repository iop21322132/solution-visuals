package farvix.solution.api.render.rect.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.interfaces.QuickImports;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;

import java.awt.*;

/**
 * Рендерит текстуру с закруглёнными углами используя SDF шейдер
 */
public class RoundedTexture implements QuickImports {

    private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
            Identifier.of("solution", "core/rounded_texture"),
            VertexFormats.POSITION,
            Defines.EMPTY
    );

    /**
     * Рисует текстуру с закруглёнными углами
     *
     * @param matrices  MatrixStack
     * @param x         X позиция
     * @param y         Y позиция
     * @param width     Ширина
     * @param height    Высота
     * @param radius    Радиус закругления
     * @param texture   Идентификатор текстуры
     * @param color     Цвет (tint)
     */
    public void render(MatrixStack matrices, float x, float y, float width, float height,
                       float radius, Identifier texture, Color color) {
        AbstractTexture tex = mc.getTextureManager().getTexture(texture);
        render(matrices, x, y, width, height, radius, tex, color);
    }

    public void render(MatrixStack matrices, float x, float y, float width, float height,
                       float radius, AbstractTexture texture, Color color) {
        render(matrices, x, y, width, height, new Vector4f(radius), texture, color);
    }

    public void render(MatrixStack matrices, float x, float y, float width, float height,
                       Vector4f radii, AbstractTexture texture, Color color) {
        if (texture == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float scale = (float) mc.getWindow().getScaleFactor();
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // Вычисляем позицию в экранных координатах
        org.joml.Vector3f pos = new org.joml.Vector3f();
        matrix4f.transformPosition(x, y, 0, pos).mul(scale);
        org.joml.Vector3f sizeVec = new org.joml.Vector3f();
        matrix4f.getScale(sizeVec).mul(scale);

        float scaledW = width * sizeVec.x;
        float scaledH = height * sizeVec.y;
        Vector4f scaledRadius = new Vector4f(radii).mul(sizeVec.y);

        // Рисуем quad
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buffer.vertex(matrix4f, x - 1, y - 1, 0);
        buffer.vertex(matrix4f, x - 1, y + height + 1, 0);
        buffer.vertex(matrix4f, x + width + 1, y + height + 1, 0);
        buffer.vertex(matrix4f, x + width + 1, y - 1, 0);

        // Биндим текстуру ДО установки шейдера
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(texture.getGlId());

        var shader = RenderSystem.setShader(SHADER_KEY);

        if (shader != null) {
            shader.getUniformOrDefault("size").set(scaledW, scaledH);
            shader.getUniformOrDefault("location").set(pos.x, mc.getWindow().getHeight() - scaledH - pos.y);
            shader.getUniformOrDefault("radius").set(scaledRadius);
            shader.getUniformOrDefault("softness").set(1.5f);
            shader.getUniformOrDefault("InputResolution").set(
                    (float) mc.getWindow().getFramebufferWidth(),
                    (float) mc.getWindow().getFramebufferHeight());
            shader.getUniformOrDefault("color1").set(
                    color.getRed() / 255f,
                    color.getGreen() / 255f,
                    color.getBlue() / 255f,
                    color.getAlpha() / 255f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }
}
