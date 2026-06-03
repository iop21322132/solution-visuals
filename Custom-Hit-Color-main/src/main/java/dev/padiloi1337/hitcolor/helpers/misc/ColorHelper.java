package dev.padiloi1337.hitcolor.helpers.misc;

import java.awt.Color;

import dev.padiloi1337.hitcolor.Wrapper;
import net.minecraft.entity.Entity;

public class ColorHelper implements Wrapper {
	
	// alpha [0, 255]
	public static Color injectAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
	
	// alpha [0, 1]
	public static Color injectAlpha(Color color, float alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255.0f));
	}
	
	public static Color getColor(int color) {
		int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int a = color >> 24 & 0xFF;
		return new Color(r, g, b, a);
	}
	
	public static String hex(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
	
	public static float[] getColorCompsf(Color color) {
		return new float[] {color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f};
	}
	
	public static Color getEntityColor(Entity entity) {
		return CLIENT.settings.color.getColor();
	}
    
}
