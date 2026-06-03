package farvix.solution.api.render.rect.api;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4i;
import farvix.solution.api.interfaces.QuickImports;

import static net.minecraft.client.render.VertexFormat.DrawMode.QUADS;
import static net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR;

@UtilityClass
public class DrawEngine implements QuickImports {
    
    public void quad(Matrix4f matrix4f,BufferBuilder buffer, float x, float y, float width, float height) {
        buffer.vertex(matrix4f, x, y, 0);
        buffer.vertex(matrix4f,x, y + height, 0);
        buffer.vertex(matrix4f,x + width, y + height, 0);
        buffer.vertex(matrix4f,x + width, y, 0);
    }

    
    public void quad(Matrix4f matrix4f,BufferBuilder buffer, float x, float y, float width, float height, int color) {
        buffer.vertex(matrix4f, x, y, 0).color(color);
        buffer.vertex(matrix4f,x, y + height, 0).color(color);
        buffer.vertex(matrix4f,x + width, y + height, 0).color(color);
        buffer.vertex(matrix4f,x + width, y, 0).color(color);
    }

    
    public void quad(Matrix4f matrix4f, float x, float y, float width, float height, int color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(QUADS, POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix4f, x, y + height, 0).texture(0, 0).color(color);
        buffer.vertex(matrix4f, x + width, y + height, 0).texture(0, 1).color(color);
        buffer.vertex(matrix4f, x + width, y, 0).texture(1, 1).color(color);
        buffer.vertex(matrix4f, x, y, 0).texture(1, 0).color(color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    
    public void quadTexture(MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
        buffer.vertex(entry, x, y + height, 0).texture(0, 0).color(color.x);
        buffer.vertex(entry, x + width, y + height, 0).texture(0, 1).color(color.y);
        buffer.vertex(entry, x + width, y, 0).texture(1, 1).color(color.w);
        buffer.vertex(entry, x, y, 0).texture(1, 0).color(color.z);
    }
}
