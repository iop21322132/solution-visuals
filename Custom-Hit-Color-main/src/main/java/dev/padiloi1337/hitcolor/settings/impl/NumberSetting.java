package dev.padiloi1337.hitcolor.settings.impl;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.padiloi1337.hitcolor.helpers.font.FontRenderer;
import dev.padiloi1337.hitcolor.helpers.misc.DataHandlers;
import dev.padiloi1337.hitcolor.helpers.misc.HoverUtil;
import dev.padiloi1337.hitcolor.helpers.render.DrawHelper;
import dev.padiloi1337.hitcolor.settings.Setting;
import dev.padiloi1337.hitcolor.ui.GuiScreen;

public class NumberSetting extends Setting<Float> {
	
	private final float min, max, increment;
	private boolean dragging = false;
	
	public NumberSetting(String name, float defaultValue, float min, float max, float increment) {
		super(DataHandlers.FLOAT, name, defaultValue);
		this.min = min;
		this.max = max;
		this.increment = increment;
	}
	
	@Override
	public void render(MatrixStack matrices, double mouseX, double mouseY) {
		FontRenderer.drawCenteredYString(matrices, DEFAULT_24, title, x, y - height / 2, Color.WHITE);
		
		// Slider background
		DrawHelper.drawRoundedRect(x + width - 80, y - 4, 70, 8, 2, GuiScreen.BACKGROUND_SECONDARY);
		
		// Slider progress
		float progress = (value - min) / (max - min);
		DrawHelper.drawRoundedRect(x + width - 80, y - 4, 70 * progress, 8, 2, GuiScreen.MAIN);
		
		// Value text
		String valueText = String.format("%.1f", value);
		FontRenderer.drawCenteredYString(matrices, DEFAULT_24, valueText, x + width - 45, y - height / 2, Color.WHITE);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if(HoverUtil.hovered(mouseX, mouseY, x + width - 80, y - 4, 70, 8)) {
			dragging = true;
			updateValue(mouseX);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if(dragging) {
			updateValue(mouseX);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if(dragging) {
			dragging = false;
			return true;
		}
		return false;
	}
	
	private void updateValue(double mouseX) {
		double sliderX = x + width - 80;
		double progress = Math.max(0, Math.min(1, (mouseX - sliderX) / 70));
		float newValue = min + (max - min) * (float)progress;
		
		// Round to increment
		newValue = Math.round(newValue / increment) * increment;
		newValue = Math.max(min, Math.min(max, newValue));
		
		this.value = newValue;
	}
	
	public float getMin() {
		return min;
	}
	
	public float getMax() {
		return max;
	}
	
	public float getIncrement() {
		return increment;
	}
}