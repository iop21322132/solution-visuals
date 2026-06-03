package dev.padiloi1337.hitcolor.settings.impl;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.item.Item;

public class MultiSetting extends BooleanSetting {
	
	private ColorSetting colorSetting;
	
	public MultiSetting(String name, boolean defaultValue, ColorSetting colorSetting, Item item) {
		super(name, defaultValue, item);
		this.colorSetting = colorSetting;
	}
	
	public MultiSetting(String name, boolean defaultValue, ColorSetting colorSetting) {
		this(name, defaultValue, colorSetting, null);
	}
	
	@Override
	public void render(MatrixStack matrices, double mouseX, double mouseY) {
		super.render(matrices, mouseX, mouseY);
		colorSetting.render(matrices, mouseX, mouseY);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if(colorSetting.mouseClicked(mouseX, mouseY, mouseButton)) return true;
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public void setCoords(double x, double y) {
		super.setCoords(x, y);
		colorSetting.setCoords(x, y);
	}
	
	@Override
	public void init() {
		super.init();
		colorSetting.init();
	}
	
	@Override
	public void onClose() {
		super.onClose();
		colorSetting.onClose();
	}
	
	public ColorSetting getSubSetting() {
		return colorSetting;
	}
	
	public Color getColor() {
		return colorSetting.getValue();
	}

}
