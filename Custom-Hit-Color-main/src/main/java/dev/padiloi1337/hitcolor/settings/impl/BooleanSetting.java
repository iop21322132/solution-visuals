package dev.padiloi1337.hitcolor.settings.impl;

import java.awt.Color;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.matrix.MatrixStack;

import dev.padiloi1337.hitcolor.animations.LinearAnimation;
import dev.padiloi1337.hitcolor.helpers.font.FontRenderer;
import dev.padiloi1337.hitcolor.helpers.misc.ColorHelper;
import dev.padiloi1337.hitcolor.helpers.misc.DataHandlers;
import dev.padiloi1337.hitcolor.helpers.misc.HoverUtil;
import dev.padiloi1337.hitcolor.helpers.render.DrawHelper;
import dev.padiloi1337.hitcolor.settings.Setting;
import dev.padiloi1337.hitcolor.ui.GuiScreen;
import net.minecraft.item.Item;

public class BooleanSetting extends Setting<Boolean> {
	
	private LinearAnimation toggleAnim = new LinearAnimation();
	private Item item;
	
	public BooleanSetting(String name, boolean defaultValue) {
		super(DataHandlers.BOOLEAN, name, defaultValue);
		toggleAnim.setValue(value ? 0.99f : 0.0f);
	}
	
	public BooleanSetting(String name, boolean defaultValue, Item item) {
		this(name, defaultValue);
		this.item = item;
	}
	
	@Override
	public void render(MatrixStack matrices, double mouseX, double mouseY) {
		double p = x;
		if(item != null) {
			MC.getItemRenderer().renderItemIntoGUI(item.getDefaultInstance(), (int)p - 2, (int)(y - height));
			p += 16;
		}
		
		FontRenderer.drawCenteredYString(matrices, DEFAULT_24, title, p, y - height / 2, Color.WHITE);
		DrawHelper.drawRoundedRect(x + width - 10, y - 4, 8, 8, 1.5, GuiScreen.BACKGROUND_SECONDARY);
		
		if(value || toggleAnim.isAnimating()) {
			Color color = ColorHelper.injectAlpha(GuiScreen.MAIN, (float)toggleAnim.getAndUpdate());
			DrawHelper.drawGlow(x + width - 11, y - 3, 10, 10, 7, color);
			DrawHelper.drawRoundedRect(x + width - 11, y - 3, 10, 10, 1.5, color);
		}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if(HoverUtil.hovered(mouseX, mouseY, x, y, width, height)) {
			this.value = !value;
			if(value)
				toggleAnim.begin(toggleAnim.get(), 0.99f, 1.4f);
			else
				toggleAnim.begin(toggleAnim.get(), 0.0f, 1.4f);
			return true;
		}
		return false;
	}
	
	@Override
	public void read(JsonElement je) {
		super.read(je);
		toggleAnim.setValue(value ? 0.99f : 0.0f);
	}
	
}
