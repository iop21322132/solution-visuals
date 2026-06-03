package dev.simplevisuals.client.util.shape.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.client.util.ColorUtils;
import dev.simplevisuals.client.util.draw.DrawEngine;
import dev.simplevisuals.client.util.draw.DrawEngineImpl;
import dev.simplevisuals.client.util.shape.Shape;
import dev.simplevisuals.client.util.shape.ShapeProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;

import dev.simplevisuals.client.util.math.ProjectionUtil;

public class Blur implements Shape {
    MinecraftClient mc = MinecraftClient.getInstance();
    Window window = mc.getWindow();
    Tessellator tessellator = Tessellator.getInstance();
    DrawEngine drawEngine = new DrawEngineImpl();
    private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/blur"), VertexFormats.POSITION, Defines.EMPTY);
    public Framebuffer input;
    public Vector2f resolution = new Vector2f();

    @Override
    public void render(ShapeProperties shape) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = RenderSystem.getShaderColor()[3];
        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0, new Vector3f()).mul(scale);
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
        Vector4f round = shape.getRound().mul(size.y);
        float quality = shape.getQuality();
        float softness = shape.getSoftness();
        float thickness = shape.getThickness();
        float width = shape.getWidth() * size.x;
        float height = shape.getHeight() * size.y;

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix4f, buffer, shape.getX() - softness / 2, shape.getY() - softness / 2, shape.getWidth() + softness, shape.getHeight() + softness);

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        if (input != null) RenderSystem.bindTexture(input.getColorAttachment());
        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        shader.getUniformOrDefault("size").set(width, height);
        shader.getUniformOrDefault("location").set(pos.x, window.getHeight() - height - pos.y);
        shader.getUniformOrDefault("radius").set(round);
        shader.getUniformOrDefault("softness").set(softness);
        shader.getUniformOrDefault("thickness").set(thickness);
        shader.getUniformOrDefault("Quality").set(quality);
        shader.getUniformOrDefault("color1").set(ColorUtils.redf(shape.getColor().x), ColorUtils.greenf(shape.getColor().x), ColorUtils.bluef(shape.getColor().x), ColorUtils.alphaf(ColorUtils.multAlpha(shape.getColor().x, alpha)));
        shader.getUniformOrDefault("color2").set(ColorUtils.redf(shape.getColor().y), ColorUtils.greenf(shape.getColor().y), ColorUtils.bluef(shape.getColor().y), ColorUtils.alphaf(ColorUtils.multAlpha(shape.getColor().y, alpha)));
        shader.getUniformOrDefault("color3").set(ColorUtils.redf(shape.getColor().z), ColorUtils.greenf(shape.getColor().z), ColorUtils.bluef(shape.getColor().z), ColorUtils.alphaf(ColorUtils.multAlpha(shape.getColor().z, alpha)));
        shader.getUniformOrDefault("color4").set(ColorUtils.redf(shape.getColor().w), ColorUtils.greenf(shape.getColor().w), ColorUtils.bluef(shape.getColor().w), ColorUtils.alphaf(ColorUtils.multAlpha(shape.getColor().w, alpha)));
        shader.getUniformOrDefault("outlineColor").set(ColorUtils.redf(shape.getOutlineColor()), ColorUtils.greenf(shape.getOutlineColor()), ColorUtils.bluef(shape.getOutlineColor()), ColorUtils.alphaf(ColorUtils.multAlpha(shape.getOutlineColor(), alpha)));
        shader.getUniformOrDefault("InputResolution").set(resolution.x, resolution.y);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    public void setup() {
        Framebuffer buffer = mc.getFramebuffer();
        if (input == null) {
            input = new SimpleFramebuffer(mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), false);
        }
        input.beginWrite(false);
        buffer.draw(input.textureWidth, input.textureHeight);
        buffer.beginWrite(false);
        if (input != null && (input.textureWidth != mc.getWindow().getFramebufferWidth() || input.textureHeight != mc.getWindow().getFramebufferHeight())) {
            input.resize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        }
        resolution.set((float) buffer.textureWidth, (float) buffer.textureHeight);
    }
}
