package dev.padiloi1337.hitcolor.helpers.render;

import static com.mojang.blaze3d.platform.GlStateManager.*;
import static org.lwjgl.opengl.GL30.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.padiloi1337.hitcolor.Wrapper;
import dev.padiloi1337.hitcolor.helpers.misc.ColorHelper;
import dev.padiloi1337.hitcolor.helpers.misc.TextureHelper;

public class DrawHelper implements Wrapper {
	
	public static final HashMap<Integer, Integer> glowCache = new HashMap<Integer, Integer>();
	private static final Shader ROUNDED = new Shader("rounded.frag");
    private static final Shader ROUNDED_GRADIENT = new Shader("rounded_gradient.frag");
	public static final int STEPS = 60;
	public static final double ANGLE =  Math.PI * 2 / STEPS;
	public static final int EX_STEPS = 120;
	public static final double EX_ANGLE =  Math.PI * 2 / EX_STEPS;
	
	public static void preInit() {
		ROUNDED.toString();
		ROUNDED_GRADIENT.toString();
		getGlowTexture(174, 190, 14);
		getGlowTexture(160, 130, 14);
	}
	
	public static void postInit() {
		getGlowTexture(10, 10, 7);
		getGlowTexture(134, 32, 15);
		getGlowTexture(10, 20, 20);
	}
	
	public static void drawCircle(double x, double y, double radius, Color color) {
		drawSetup();
		applyColor(color);
		
		glBegin(GL_TRIANGLE_FAN);
	    for(int i = 0; i <= STEPS; i++) {
	    	glVertex2d(x + radius * Math.sin(ANGLE * i),
	    					y + radius * Math.cos(ANGLE * i)
	    	);
	    }
	    glEnd();
	    
		glLineWidth(1.5f);
		glEnable(GL_LINE_SMOOTH);
		
		glBegin(GL_LINE_LOOP);
		for(int i = 0; i <= STEPS; i++) {
	    	glVertex2d(x + radius * Math.sin(ANGLE * i),
	    					y + radius * Math.cos(ANGLE * i)
	    	);
	    }
		glEnd();
		
		glDisable(GL_LINE_SMOOTH);
		drawFinish();
	}
	
	public static void drawCircleOutline(double x, double y, double radius, float thikness, Color color) {
		drawSetup();
		glEnable(GL_LINE_SMOOTH);
		glLineWidth(thikness);
		applyColor(color);
		
		glBegin(GL_LINE_LOOP);
		for(int i = 0; i <= STEPS; i++) {
	    	glVertex2d(x + radius * Math.sin(ANGLE * i),
	    					y + radius * Math.cos(ANGLE * i)
	    	);
	    }
		glEnd();
		
		glDisable(GL_LINE_SMOOTH);
		drawFinish();
	}
	
	public static void drawRainbowCircle(double x, double y, double radius, double blurRadius) {
		drawSetup();
		glEnable(GL_ALPHA_TEST);
	    glAlphaFunc(GL_GREATER, 0.0001f);
		glShadeModel(GL_SMOOTH);
		applyColor(Color.WHITE);
			
		glBegin(GL_TRIANGLE_FAN);
		glVertex2d(x, y);
	    for(int i = 0; i <= EX_STEPS; i++) {
	    	applyColor(Color.getHSBColor((float)i / EX_STEPS, 1f, 1f));
	    	glVertex2d(x + radius * Math.sin(EX_ANGLE * i),
	    					y + radius * Math.cos(EX_ANGLE * i)
	    	);
	    }
	    glEnd();
	    
	    glBegin(GL_TRIANGLE_STRIP);
	    for(int i = 0; i <= EX_STEPS + 1; i++) {
			if(i % 2 == 1) {
				applyColor(ColorHelper.injectAlpha(Color.getHSBColor((float)i / EX_STEPS, 1f, 1f), 0));
				glVertex2d(x + (radius + blurRadius) * Math.sin(EX_ANGLE * i), 
						y + (radius + blurRadius) * Math.cos(EX_ANGLE * i));
			} else {
				applyColor(Color.getHSBColor((float)i / EX_STEPS, 1f, 1f));
				glVertex2d(x + radius * Math.sin(EX_ANGLE * i), 
						y + radius * Math.cos(EX_ANGLE * i));
			}
		}
		glEnd();
		
		glShadeModel(GL_FLAT);
		glDisable(GL_ALPHA_TEST);
		drawFinish();
	}
	
	public static void drawRect(double x, double y, double width, double height, Color color) {
		drawSetup();
		applyColor(color);
		
		glBegin(GL_QUADS);
		glVertex2d(x, y);
		glVertex2d(x + width, y);
		glVertex2d(x + width, y - height);
		glVertex2d(x, y - height);
		glEnd();
		
		drawFinish();
	}
	
	public static void drawRoundedRect(double x, double y, double width, double height, double radius, Color color) {
		float[] c = ColorHelper.getColorCompsf(color);
		
        drawSetup();
        
        ROUNDED.load();
        ROUNDED.setUniformf("size", (float)width * 2, (float)height * 2);
        ROUNDED.setUniformf("round", (float)radius * 2);
        ROUNDED.setUniformf("color", c[0], c[1], c[2], c[3]);
        Shader.draw(x, y - height, width, height);
        ROUNDED.unload();
        
		drawFinish();
    }

	public static void drawRoundedGradientRect(double x, double y, double width, double height, double radius, Color... colors) {
		float[] c = ColorHelper.getColorCompsf(colors[0]);
        float[] c1 = ColorHelper.getColorCompsf(colors[1]);
        float[] c2 = ColorHelper.getColorCompsf(colors[2]);
        float[] c3 = ColorHelper.getColorCompsf(colors[3]);
        
        drawSetup();
        
        ROUNDED_GRADIENT.load();
        ROUNDED_GRADIENT.setUniformf("size", (float)width * 2, (float)height * 2);
		ROUNDED_GRADIENT.setUniformf("round", (float)radius * 2);
		ROUNDED_GRADIENT.setUniformf("color1", c[0], c[1], c[2], c[3]);
		ROUNDED_GRADIENT.setUniformf("color2", c1[0], c1[1], c1[2], c1[3]);
		ROUNDED_GRADIENT.setUniformf("color3", c2[0], c2[1], c2[2], c2[3]);
		ROUNDED_GRADIENT.setUniformf("color4", c3[0], c3[1], c3[2], c3[3]);
		Shader.draw(x, y - height, width, height);
		ROUNDED_GRADIENT.unload();
        
		drawFinish();
	}
	
	public static void drawGlow(double x, double y, int width, int height, int glowRadius, Color color) {
		enableBlend();
		blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.0001f);

		bindTexture(getGlowTexture(width, height, glowRadius));
		width += glowRadius * 2;
		height += glowRadius * 2;
		x -= glowRadius;
		y -= height - glowRadius;
		
		applyColor(color);
		glBegin(GL_QUADS);
		glTexCoord2d(0, 1);
		glVertex2d(x, y);
		glTexCoord2d(0, 0);
		glVertex2d(x, y + height);
		glTexCoord2d(1, 0);
		glVertex2d(x + width, y + height);
		glTexCoord2d(1, 1);
		glVertex2d(x + width, y);
		glEnd();
        
		bindTexture(0);
		glDisable(GL_ALPHA_TEST);
		disableBlend();
    }
	
	public static int getGlowTexture(int width, int height, int blurRadius) {
		int identifier = (width * 401 + height) * 407 + blurRadius;
		int texId = glowCache.getOrDefault(identifier, -1);
		
        if(texId == -1) {
            BufferedImage original = new BufferedImage((int)(width + blurRadius * 2), (int)(height + blurRadius * 2), BufferedImage.TYPE_INT_ARGB_PRE);
            
            Graphics g = original.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(blurRadius, blurRadius, (int)width, (int)height);
            g.dispose();

            GlowFilter glow = new GlowFilter(blurRadius);
            BufferedImage blurred = glow.filter(original, null);
            try {
    			texId = TextureHelper.loadTexture(blurred);
    			glowCache.put(identifier, texId);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }
        return texId;
	}
	
	public static void applyColor(Color color) {
		glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
	}
	
	public static void resetColor() {
		glColor4f(1f, 1f, 1f, 1f);
	}
	
	public static void drawSetup() {
		disableTexture();
		enableBlend();
		blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	}
	
	public static void drawFinish() {
		enableTexture();
		disableBlend();
		resetColor();
	}
	
	public static void drawLine(MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, Color color) {
		// Используем более современный подход с матрицами
		matrices.push();
		
		// Настройка рендеринга
		enableBlend();
		disableTexture();
		blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		applyColor(color);
		
		glEnable(GL_LINE_SMOOTH);
		glLineWidth(2.0f);
		
		// Используем устаревший immediate mode только если необходимо
		glBegin(GL_LINES);
		glVertex3f(x1, y1, z1);
		glVertex3f(x2, y2, z2);
		glEnd();
		
		glDisable(GL_LINE_SMOOTH);
		
		// Восстановление состояния
		enableTexture();
		disableBlend();
		resetColor();
		
		matrices.pop();
	}
	
}
