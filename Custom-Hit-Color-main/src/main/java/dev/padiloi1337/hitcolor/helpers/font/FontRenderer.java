package dev.padiloi1337.hitcolor.helpers.font;

import java.awt.Color;

import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.util.math.vector.Matrix4f;

public class FontRenderer {
	
	public static void drawString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color) {
		renderString(matrices, font, text, x, y, color);
	}
	
	public static void drawCenteredXString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color) {
		renderString(matrices, font, text, x - font.getWidth(text) / 2, y, color);
	}

	public static void drawCenteredYString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color) {
		renderString(matrices, font, text, x, y + font.getFontHeight() / 2 + 0.5f, color);
	}
	
	public static void drawCenteredXYString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color) {
		renderString(matrices, font, text, x - font.getWidth(text) / 2, y + font.getFontHeight() / 2 + 0.5f, color);
	}
	
	public static void drawShadowedString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color) {
		renderStringWithShadow(matrices, font, text, x, y, color, getShadowColor(color));
	}
	
	public static void drawShadowedCenteredXString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color) {
		renderStringWithShadow(matrices, font, text, x - font.getWidth(text) / 2, y, color, getShadowColor(color));
	}
	
	public static void drawShadowedCenteredYString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color) {
		renderStringWithShadow(matrices, font, text, x, y + font.getFontHeight() / 2 + 0.5f, color, getShadowColor(color));
	}
	
	public static void drawShadowedCenteredXYString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color) {
		renderStringWithShadow(matrices, font, text, x - font.getWidth(text) / 2, y + font.getFontHeight() / 2 + 0.5f, color, getShadowColor(color));
	}
	
	public static void drawShadowedString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color, Color shadowColor) {
		renderStringWithShadow(matrices, font, text, x, y, color, shadowColor);
	}
	
	public static void drawShadowedCenteredXString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color, Color shadowColor) {
		renderStringWithShadow(matrices, font, text, x - font.getWidth(text) / 2, y, color, shadowColor);
	}
	
	public static void drawShadowedCenteredYString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color, Color shadowColor) {
		renderStringWithShadow(matrices, font, text, x, y + font.getFontHeight() / 2 + 0.5f, color, shadowColor);
	}
	
	public static void drawShadowedCenteredXYString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color, Color shadowColor) {
		renderStringWithShadow(matrices, font, text, x - font.getWidth(text) / 2, y + font.getFontHeight() / 2 + 0.5f, color, shadowColor);
	}
	
	private static void renderStringWithShadow(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color, Color shadowColor) {
			y -= 1f;
			renderString(matrices, font, text, x + 1.0F, y + 1.0F, shadowColor);
			renderString(matrices, font, text, x, y, color);
	}

	private static void renderString(MatrixStack matrices, CustomFont font, String text, double x, double y, Color color) {
		y -= font.getFontHeight() + font.getLifting();
		float posX = (float)x * 2f;
		float posY = (float)y * 2f;
		float red = (float) color.getRed() / 255.0f;
		float green = (float)color.getGreen() / 255.0f;
		float blue = (float)color.getBlue() / 255.0f;
		float alpha = (float)color.getAlpha() / 255.0f;
		
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
		matrices.push();
		matrices.scale(0.5f, 0.5f, 1f);
		Matrix4f matrix = matrices.getLast().getMatrix();
		font.bindTex();
		for(int i = 0; i < text.length(); i++) {
			posX += font.renderChar(matrix, text.charAt(i), posX, posY, red, green, blue, alpha);
		}
		font.unbindTex();
		matrices.pop();
		GlStateManager.disableBlend();
	}
	
	public static Color getShadowColor(Color color) {
		return new Color((color.getRGB() & 16579836) >> 2 | color.getRGB()  & -16777216);
	}
	
}
