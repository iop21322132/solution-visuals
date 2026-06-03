package dev.padiloi1337.hitcolor.ui;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.padiloi1337.hitcolor.helpers.font.FontRenderer;
import dev.padiloi1337.hitcolor.helpers.render.DrawHelper;

public class MainPane extends Element {

	public MainPane() {
		super(174, 28, "Hit Color Panel");
		height += CLIENT.settings.settingsList.size() * (16 + 2);
	}

	@Override
	public void render(MatrixStack matrices, double mouseX, double mouseY) {
		DrawHelper.drawGlow(x, y, (int)width, (int)height, 14, GuiScreen.BACKGROUND_PRIMARY);
		DrawHelper.drawRoundedRect(x, y, width, height, 5, GuiScreen.BACKGROUND_PRIMARY);
		/*FontRenderer.drawCenteredXString(matrices, DEFAULT_24, title, x + width / 2 + 0.85, y - height + 18 + 0.85, Color.cyan);*/
		FontRenderer.drawCenteredXString(matrices, DEFAULT_24, title, x + width / 2, y - height + 18, Color.white);
		
		double y1 = y - height + 22;
		for(Element setting : CLIENT.settings.settingsList) {
			setting.setCoords(x + 8, y1 += setting.getHeight() + 2);
			setting.render(matrices, mouseX, mouseY);
		}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		for(Element setting : CLIENT.settings.settingsList)
			if(setting.mouseClicked(mouseX, mouseY, mouseButton)) return true;
		return false;
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		for(Element setting : CLIENT.settings.settingsList)
			if(setting.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
		return false;
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		for(Element setting : CLIENT.settings.settingsList)
			if(setting.mouseReleased(mouseX, mouseY, button)) return true;
		return false;
	}
	
	@Override
	public void init() {
		x = GuiScreen.getScaledWidth() / 2 - width - 15;
		y = (GuiScreen.getScaledHeight() + height) / 2;
		for(Element setting : CLIENT.settings.settingsList)
			setting.init();
	}
	
	@Override
	public void onClose() {
		for(Element setting : CLIENT.settings.settingsList)
			setting.onClose();
	}
	
}
