package farvix.solution.api.util.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL13;
import farvix.solution.api.interfaces.QuickImports;

@UtilityClass
public class SaturationPass implements QuickImports {

    private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
            Identifier.of("solution", "core/saturation"), VertexFormats.POSITION, Defines.EMPTY);

    private static Framebuffer copyBuffer;
    private static boolean unsupported;

    public static void apply(float saturation) {
        if (unsupported || saturation == 1.0f || mc.world == null) {
            return;
        }

        try {
            Framebuffer main = mc.getFramebuffer();
            int w = main.textureWidth;
            int h = main.textureHeight;
            if (w <= 0 || h <= 0) {
                return;
            }

            if (copyBuffer == null || copyBuffer.textureWidth != w || copyBuffer.textureHeight != h) {
                if (copyBuffer != null) {
                    copyBuffer.delete();
                }
                copyBuffer = new SimpleFramebuffer(w, h, false);
            }

            copyBuffer.beginWrite(false);
            main.draw(w, h);
            main.beginWrite(false);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0f, w, h, 0f, -1000f, 1000f),
                    ProjectionType.ORTHOGRAPHIC);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();

            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.bindTexture(copyBuffer.getColorAttachment());

            ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
            shader.getUniformOrDefault("InputResolution").set((float) w, (float) h);
            shader.getUniformOrDefault("Saturation").set(saturation);

            Matrix4f matrix = new Matrix4f();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            buffer.vertex(matrix, 0f, 0f, 0f);
            buffer.vertex(matrix, 0f, h, 0f);
            buffer.vertex(matrix, w, h, 0f);
            buffer.vertex(matrix, w, 0f, 0f);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        } catch (Exception ignored) {
            unsupported = true;
        }
    }
}
