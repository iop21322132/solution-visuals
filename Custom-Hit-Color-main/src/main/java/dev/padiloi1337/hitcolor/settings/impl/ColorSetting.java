package dev.padiloi1337.hitcolor.settings.impl;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.padiloi1337.hitcolor.helpers.font.FontRenderer;
import dev.padiloi1337.hitcolor.helpers.misc.DataHandlers;
import dev.padiloi1337.hitcolor.helpers.render.DrawHelper;
import dev.padiloi1337.hitcolor.settings.Setting;
import dev.padiloi1337.hitcolor.ui.Button;

public class ColorSetting extends Setting<Color> {
	
	private boolean independent;
	private Button button = new Button(8, 8, () -> CLIENT.guiScreen.colorPicker.toggle(this),
			(matrices, x, y, width, height, title) -> {
				DrawHelper.drawRoundedRect(x, y, width, height, 1.5, value);
			});

	public ColorSetting(String title, Color defaultColor, boolean independent) {
		super(DataHandlers.COLOR, title, defaultColor);
		this.independent = independent;
	}
	
	public ColorSetting(String title, Color defaultColor) {
		this(title, defaultColor, false);
	}
	
	@Override
	public void render(MatrixStack matrices, double mouseX, double mouseY) {
		if(independent)
			FontRenderer.drawCenteredYString(matrices, DEFAULT_24, title, x, y - height / 2, Color.WHITE);
		button.render(matrices, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		return button.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public void setCoords(double x, double y) {
		super.setCoords(x, y);
		button.setCoords(x + width - 26.5, y - 4);
	}
	
	public boolean isIndependent() {
		return independent;
	}
	
}
