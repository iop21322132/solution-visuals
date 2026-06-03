package dev.padiloi1337.hitcolor.ui;

import java.awt.Color;
import java.lang.reflect.Field;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;

import dev.padiloi1337.hitcolor.ConfigManager;
import dev.padiloi1337.hitcolor.Wrapper;
import dev.padiloi1337.hitcolor.animations.LinearAnimation;
import dev.padiloi1337.hitcolor.helpers.font.FontRenderer;
import dev.padiloi1337.hitcolor.helpers.misc.ColorHelper;
import dev.padiloi1337.hitcolor.helpers.misc.MathHelper;
import dev.padiloi1337.hitcolor.helpers.render.DrawHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class GuiScreen extends Screen implements Wrapper {
	
	public static final Color MAIN = new Color(255, 255, 255, 255);
	public static final Color BACKGROUND_PRIMARY = new Color(6, 6, 6);
	public static final Color BACKGROUND_SECONDARY = new Color(33, 33, 33);
	private static double scaledWidth, scaledHeight, scale, speed;
	private LinearAnimation colorAnim = new LinearAnimation();
	public final ColorPicker colorPicker = new ColorPicker();
	public final MainPane mainPane = new MainPane();

	public GuiScreen() {
		super(new StringTextComponent(""));
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
		mouseX /= scale;
		mouseY /= scale;
		GlStateManager.pushMatrix();
		GlStateManager.scaled(scale, scale, 1);
		DrawHelper.drawRect(0, scaledHeight, scaledWidth, scaledHeight,
				ColorHelper.injectAlpha(Color.BLACK, (float)colorAnim.getAndUpdate()));
		
		// watermark
		DrawHelper.drawGlow(20, 56, 187, 34, 15, BACKGROUND_PRIMARY);
		DrawHelper.drawRoundedRect(20, 56, 187, 34, 4, BACKGROUND_PRIMARY);
		DrawHelper.drawGlow(25, 39, (int) (10 + DEFAULT_24.getWidth("custom Hit Color")), 10, 20, new Color(0x7EFFFFFF, true));
//		FontRenderer.drawString(matrices, ICONS_42, "a", 27, 46, MAIN);
		FontRenderer.drawCenteredYString(matrices, DEFAULT_24, "Custom Hit Color", 30, 31, Color.WHITE);
		FontRenderer.drawCenteredYString(matrices, DEFAULT_20, "https://vk.com/padiloi1337", 30, 43, Color.white);
		
		if(colorPicker.isOpened())
			colorPicker.render(matrices, mouseX, mouseY);
		mainPane.render(matrices, mouseX, mouseY);
		GlStateManager.popMatrix();
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if(mouseButton == 0) {
			mouseX /= scale;
			mouseY /= scale;
			if(colorPicker.isOpened())
				colorPicker.mouseClicked(mouseX, mouseY, mouseButton);
			mainPane.mouseClicked(mouseX, mouseY, mouseButton);
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
		mouseX /= scale;
		mouseY /= scale;
		deltaX /= scale;
		deltaY /= scale;
		if(colorPicker.isOpened())
			colorPicker.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
		mainPane.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
		return super.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
		mouseX /= scale;
		mouseY /= scale;
		if(colorPicker.isOpened())
			colorPicker.mouseReleased(mouseX, mouseY, mouseButton);
		mainPane.mouseReleased(mouseX, mouseY, mouseButton);
		return super.mouseReleased(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public void tick() {
//		int fps = ((AccessorMinecraft)MC).getFps();
		int fps = 0;
		try {
			Field field = Minecraft.class.getDeclaredFields()[91];
			field.setAccessible(true);
			fps = field.getInt(MC);
		} catch (Exception e) {}
		speed = MathHelper.round(110f / (fps == 0 ? 100 : fps), 2);
	}
	
	public void toggle() {
		if(MC.currentScreen == this) 
			onClose();
		else
			MC.displayGuiScreen(this);
	}
	
	@Override
	public void init() {
		tick();
		switch ((int)WINDOW.getGuiScaleFactor()) {
		case 1:
			scale = 2 * 0.8;
			break;
		case 2:
			scale = 1;
			break;
		case 3:
			scale = 2 / 3d * 1.2;
			break;
		case 4:
			scale = 0.5 * 1.3;
			break;
		}

		scaledWidth = WINDOW.getScaledWidth() / scale;
		scaledHeight = WINDOW.getScaledHeight() / scale;
		colorPicker.init();
		mainPane.init();
		colorAnim.begin(0.01, 0.4, 1.5);
	}
	
	@Override
	public void onClose() {
		ConfigManager.save();
		colorPicker.onClose();
		mainPane.onClose();
	}
	
	public static double getSpeed() {
		return speed;
	}
	
	public static double getScaledWidth() {
		return scaledWidth;
	}
	
	public static double getScaledHeight() {
		return scaledHeight;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

}
