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

public class Arc implements Shape, QuickImports {
    private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("solution", "core/arc"), VertexFormats.POSITION, Defines.EMPTY);
    
    // Кэшируем векторы
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

        float width = shape.getWidth() * cachedSize.x;
        float height = shape.getHeight() * cachedSize.y;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        DrawEngine.quad(matrix4f, buffer, shape.getX(), shape.getY(), shape.getWidth(), shape.getHeight());

        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        shader.getUniformOrDefault("size").set(width, height);
        shader.getUniformOrDefault("location").set(cachedPos.x, mc.getWindow().getHeight() - height - cachedPos.y);
        shader.getUniformOrDefault("radius").set(cachedRound.x);
        shader.getUniformOrDefault("thickness").set(shape.getThickness());
        shader.getUniformOrDefault("start").set(shape.getStart());
        shader.getUniformOrDefault("end").set(shape.getEnd());
        shader.getUniformOrDefault("color1").set(ColorUtility.redf(shape.getColor().x),ColorUtility.greenf(shape.getColor().x),ColorUtility.bluef(shape.getColor().x),ColorUtility.alphaf(ColorUtility.multAlpha(shape.getColor().x,alpha)));
        shader.getUniformOrDefault("color2").set(ColorUtility.redf(shape.getColor().y),ColorUtility.greenf(shape.getColor().y),ColorUtility.bluef(shape.getColor().y),ColorUtility.alphaf(ColorUtility.multAlpha(shape.getColor().y,alpha)));

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }
}
