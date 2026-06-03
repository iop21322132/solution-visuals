package dev.padiloi1337.hitcolor.helpers.misc;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import dev.padiloi1337.hitcolor.Wrapper;

public class TextureHelper implements Wrapper {
	
 	public static int loadTexture(BufferedImage image) throws Exception {
		int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length * 4);
        
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();
        
		int textureID = GL30.glGenTextures();
		GL30.glBindTexture(GL30.GL_TEXTURE_2D, textureID);
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
		GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, buffer);
		GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);
		return textureID;
	}
 	
}
	