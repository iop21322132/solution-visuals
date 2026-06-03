package farvix.solution.api.render.rect.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.rect.Shape;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.render.rect.api.DrawEngine;

@Setter
@Accessors(chain = true)
public class Image implements Shape, QuickImports {
    private String texture;

    @Override
    public void render(ShapeProperties shape) {
        MatrixStack matrix = shape.getMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, Identifier.of(texture));

        float width = shape.getWidth();
        float x = shape.getX() + width;
        float y = shape.getY();

        matrix.push();
        matrix.translate(x, y, 0.0F);
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
        matrix.translate(-x, -y, 0.0F);

        DrawEngine.quad(matrix.peek().getPositionMatrix(), x, y, shape.getHeight(), width, shape.getColor().x);

        matrix.pop();

        RenderSystem.disableBlend();
    }
}
