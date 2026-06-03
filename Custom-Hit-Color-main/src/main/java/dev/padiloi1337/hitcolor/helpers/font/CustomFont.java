package dev.padiloi1337.hitcolor.helpers.font;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.padiloi1337.hitcolor.HitColor;
import dev.padiloi1337.hitcolor.helpers.misc.TextureHelper;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;

public class CustomFont {
	
	public static final Tessellator TESSELLATOR = Tessellator.getInstance();
	public static final BufferBuilder BUFFER_BUILDER = TESSELLATOR.getBuffer();
	private Map<Character, Glyph> glyphs = new HashMap<>();
	private float fontHeight, stretching, spacing, lifting;
	private int texId, imgSize;
	private String fontName;
	
	public CustomFont(String fileName, int size, boolean fractionalMetrics, float stretching, float spacing, float lifting) {
		String path = "/assets/" + HitColor.MOD_ID + "/font/".concat(fileName);
		Font font = null;
		
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, HitColor.class.getResourceAsStream(path))
				.deriveFont(Font.PLAIN, size);
		} catch (FontFormatException | IOException e) {
			e.printStackTrace();
		}
		
		this.stretching = stretching;
		this.spacing = spacing;
		this.lifting = lifting;
		fontName = font.getFontName(Locale.ENGLISH);
		
		char[] chars = new char[128];
		
		FontRenderContext fontRenderContext = new FontRenderContext(font.getTransform(), true, fractionalMetrics);
		double maxWidth = 0;
		double maxHeight = 0;
		
		for(int i = 0; i <= chars.length-1; i++) {
			chars[i] = (char)i;
			Rectangle2D bound = font.getStringBounds(Character.toString(chars[i]), fontRenderContext);
			maxWidth = Math.max(maxWidth, bound.getWidth());
			maxHeight = Math.max(maxHeight, bound.getHeight());
		}
		
		fontHeight = (float)(maxHeight / 2);
		imgSize = (int)Math.ceil(Math.sqrt((maxHeight + 2) * (maxWidth + 2) * chars.length));
		
		BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = bufferedImage.createGraphics();
		graphics.setFont(font);
		graphics.setColor(new Color(255, 255, 255, 0));
		graphics.fillRect(0, 0, imgSize, imgSize);
		graphics.setColor(Color.WHITE);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, 
				fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,	RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		FontMetrics fontMetrics = graphics.getFontMetrics();
		int posX = 1;
		int posY = 2;
		
		for(char c : chars) {
			Glyph glyph = new Glyph();
			Rectangle2D bounds = fontMetrics.getStringBounds(Character.toString(c), graphics);
			glyph.width = (int)bounds.getWidth() + 1;
			glyph.height = (int)bounds.getHeight() + 2;

			if(posX + glyph.width >= imgSize) {
				posX = 1;
				posY += maxHeight + fontMetrics.getDescent() + 1;
			}

			glyph.x = posX;
			glyph.y = posY;
			
			graphics.drawString(Character.toString(c), posX, posY + fontMetrics.getAscent());
			
			posX += glyph.width + 6;
			glyphs.put(c, glyph);
		}
		
		if(RenderSystem.isOnRenderThread())
			loadTex(bufferedImage);
		else
			RenderSystem.recordRenderCall(() -> {
				loadTex(bufferedImage);
			});
	}
	
	public void loadTex(BufferedImage tex) {
		try {
			texId = TextureHelper.loadTexture(tex);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void bindTex() {
		GlStateManager.bindTexture(texId);
	}

	public void unbindTex() {
		GlStateManager.bindTexture(0);
	}
	
	public float renderChar(Matrix4f matrix, char c, float x, float y, float red, float green, float blue, float alpha) {
		Glyph glyph = glyphs.get(c);
		
		if(glyph == null) 
			return 0;
		
		float pageX = glyph.x / (float)imgSize;
		float pageY = glyph.y / (float)imgSize;
		float pageWidth = glyph.width / (float)imgSize;
		float pageHeight = glyph.height / (float)imgSize;
		float width = glyph.width + stretching;
		float height = glyph.height;
		
		BUFFER_BUILDER.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX);
		BUFFER_BUILDER.pos(matrix, x, y + height, 0).color(red, green, blue, alpha)
				.tex(pageX, pageY + pageHeight).endVertex();
		BUFFER_BUILDER.pos(matrix, x + width, y + height, 0).color(red, green, blue, alpha)
				.tex(pageX + pageWidth, pageY + pageHeight).endVertex();
		BUFFER_BUILDER.pos(matrix, x + width, y, 0).color(red, green, blue, alpha)
				.tex(pageX + pageWidth, pageY).endVertex();
		BUFFER_BUILDER.pos(matrix, x, y, 0).color(red, green, blue, alpha)
				.tex(pageX, pageY).endVertex();
		TESSELLATOR.draw();
		
		return width + spacing;
	}
	
	public float getWidth(char ch) {
		return glyphs.get(ch).width + stretching;
	}
	
	public float getWidth(String text) {
		float width = 0.0f;
		for (int i = 0; i < text.length(); i++) {
			width += getWidth(text.charAt(i)) + spacing;
		}
		return (width - spacing) / 2f;
	}
	
	public float getLifting() {
		return lifting;
	}
	
	public float getSpacing() {
		return spacing;
	}
	
	public String getFontName() {
		return fontName;
	}
	
	public float getFontHeight() {
		return fontHeight;
	}
	
	private static class Glyph {
		private int x;
		private int y;
		private int width;
		private int height;
	}
	
}
