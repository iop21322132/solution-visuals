package farvix.solution.api.render.rect.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import farvix.solution.api.render.rect.Shape;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.rect.api.DrawEngine;
import farvix.solution.api.util.color.ColorUtility;

public class Rectangle implements Shape, QuickImports {
    private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("solution", "core/round"), VertexFormats.POSITION, Defines.EMPTY);
    
    // Кэшируем векторы для переиспользования
    private final Vector3f cachedPos = new Vector3f();
    private final Vector3f cachedSize = new Vector3f();
    private final Vector4f cachedRound = new Vector4f();

    @Override
    public void render(ShapeProperties shape) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = RenderSystem.getShaderColor()[3];

        Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
        
        // Переиспользуем кэшированные векторы
        matrix4f.transformPosition(shape.getX(), shape.getY(), 0, cachedPos).mul(scale);
        matrix4f.getScale(cachedSize).mul(scale);
        shape.getRound().mul(cachedSize.y, cachedRound);

        float softness = shape.getSoftness();
        float thickness = shape.getThickness();
        float width = shape.getWidth() * cachedSize.x;
        float height = shape.getHeight() * cachedSize.y;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        DrawEngine.quad(matrix4f, buffer, shape.getX() - softness / 2, shape.getY() - softness / 2, shape.getWidth() + softness, shape.getHeight() + softness);

        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        shader.getUniformOrDefault("size").set(width, height);
        shader.getUniformOrDefault("location").set(cachedPos.x, mc.getWindow().getHeight() - height - cachedPos.y);
        shader.getUniformOrDefault("radius").set(cachedRound);
        shader.getUniformOrDefault("softness").set(softness);
        shader.getUniformOrDefault("thickness").set(thickness);
        shader.getUniformOrDefault("color1").set(ColorUtility.redf(shape.getColor().x), ColorUtility.greenf(shape.getColor().x), ColorUtility.bluef(shape.getColor().x), ColorUtility.alphaf(ColorUtility.multAlpha(shape.getColor().x, alpha)));
        shader.getUniformOrDefault("color2").set(ColorUtility.redf(shape.getColor().y), ColorUtility.greenf(shape.getColor().y), ColorUtility.bluef(shape.getColor().y), ColorUtility.alphaf(ColorUtility.multAlpha(shape.getColor().y, alpha)));
        shader.getUniformOrDefault("color3").set(ColorUtility.redf(shape.getColor().z), ColorUtility.greenf(shape.getColor().z), ColorUtility.bluef(shape.getColor().z), ColorUtility.alphaf(ColorUtility.multAlpha(shape.getColor().z, alpha)));
        shader.getUniformOrDefault("color4").set(ColorUtility.redf(shape.getColor().w), ColorUtility.greenf(shape.getColor().w), ColorUtility.bluef(shape.getColor().w), ColorUtility.alphaf(ColorUtility.multAlpha(shape.getColor().w, alpha)));
        shader.getUniformOrDefault("outlineColor").set(ColorUtility.redf(shape.getOutlineColor()), ColorUtility.greenf(shape.getOutlineColor()), ColorUtility.bluef(shape.getOutlineColor()), ColorUtility.alphaf(ColorUtility.multAlpha(shape.getOutlineColor(), alpha)));

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }
}
