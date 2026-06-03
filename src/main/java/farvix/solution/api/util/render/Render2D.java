package farvix.solution.api.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.awt.*;

public class Render2D {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void drawTexture(MatrixStack stack, float x, float y, float width, float height,
                                   float radius, Identifier texture, Color color) {
        drawTexture(stack, x, y, width, height, radius, mc.getTextureManager().getTexture(texture), color);
    }

    public static void drawTexture(MatrixStack stack, float x, float y, float width, float height,
                                   float radius, Identifier texture, Color color, boolean additiveBlend) {
        drawTexture(stack, x, y, width, height, radius, mc.getTextureManager().getTexture(texture), color, additiveBlend);
    }

    public static void drawTexture(MatrixStack stack, float x, float y, float width, float height,
                                   float radius, AbstractTexture texture, Color color) {
        drawTexture(stack, x, y, width, height, radius, texture, color, false);
    }

    public static void drawTexture(MatrixStack stack, float x, float y, float width, float height,
                                   float radius, AbstractTexture texture, Color color, boolean additiveBlend) {
        RenderSystem.setShaderTexture(0, texture.getGlId());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        
        if (additiveBlend) {
            RenderSystem.blendFunc(770, 1); // SRC_ALPHA, ONE
        } else {
            RenderSystem.defaultBlendFunc();
        }

        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        int colorRGB = color.getRGB();
        int r = (colorRGB >> 16) & 0xFF;
        int g = (colorRGB >> 8) & 0xFF;
        int b = colorRGB & 0xFF;
        int a = (colorRGB >> 24) & 0xFF;

        buffer.vertex(matrix, x, y, 0).texture(0f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, x, y + height, 0).texture(0f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, x + width, y + height, 0).texture(1f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, x + width, y, 0).texture(1f, 0f).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static void drawRoundedRect(MatrixStack stack, float x, float y, float width, float height, float radius, Color color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        int segments = 8;
        float[] cx = {x + radius, x + width - radius, x + width - radius, x + radius};
        float[] cy = {y + radius, y + radius, y + height - radius, y + height - radius};
        float[] startAngle = {(float)Math.PI, (float)(3 * Math.PI / 2), 0, (float)(Math.PI / 2)};

        // Center vertex
        buffer.vertex(matrix, x + width / 2f, y + height / 2f, 0).color(r, g, b, a);

        for (int corner = 0; corner < 4; corner++) {
            for (int i = 0; i <= segments; i++) {
                float angle = startAngle[corner] + (float)Math.PI / 2f * i / segments;
                float vx = cx[corner] + radius * (float)Math.cos(angle);
                float vy = cy[corner] + radius * (float)Math.sin(angle);
                buffer.vertex(matrix, vx, vy, 0).color(r, g, b, a);
            }
        }
        // Close the fan
        float vx = cx[0] + radius * (float)Math.cos(startAngle[0]);
        float vy = cy[0] + radius * (float)Math.sin(startAngle[0]);
        buffer.vertex(matrix, vx, vy, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }
}
