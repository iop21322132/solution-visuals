package dev.padiloi1337.hitcolor.settings.impl;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.matrix.MatrixStack;

import dev.padiloi1337.hitcolor.helpers.font.FontRenderer;
import dev.padiloi1337.hitcolor.helpers.misc.DataHandlers;
import dev.padiloi1337.hitcolor.helpers.misc.DataHandlers.DataHandler;
import dev.padiloi1337.hitcolor.settings.Setting;

public class ListSetting extends Setting<List<BooleanSetting>> {
	
	private boolean expanded = false;
	
	// Создаем специальный DataHandler для списка BooleanSetting
	private static final DataHandler<List<BooleanSetting>> LIST_HANDLER = new DataHandler<List<BooleanSetting>>() {
		@Override
		public JsonElement save(List<BooleanSetting> value) {
			JsonArray array = new JsonArray();
			for(BooleanSetting setting : value) {
				array.add(setting.save());
			}
			return array;
		}
		
		@Override
		public List<BooleanSetting> read(JsonElement je) {
			// Для чтения нужно будет реализовать логику восстановления настроек
			// Пока возвращаем null, так как это сложная операция
			return null;
		}
	};
	
	public ListSetting(String name, BooleanSetting... values) {
		super(LIST_HANDLER, name, Arrays.asList(values));
		this.height = 16; // Base height
	}
	
	@Override
	public void render(MatrixStack matrices, double mouseX, double mouseY) {
		FontRenderer.drawCenteredYString(matrices, DEFAULT_24, title, x, y - height / 2, Color.WHITE);
		
		// Render expand/collapse indicator
		String indicator = expanded ? "▼" : "▶";
		FontRenderer.drawCenteredYString(matrices, DEFAULT_24, indicator, x + width - 10, y - height / 2, Color.WHITE);
		
		if(expanded) {
			double currentY = y + 20;
			for(BooleanSetting setting : value) {
				setting.setCoords(x + 10, currentY);
				setting.render(matrices, mouseX, mouseY);
				currentY += 20;
			}
		}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		// Check if clicked on main setting to expand/collapse
		if(mouseY >= y - height && mouseY <= y && mouseX >= x && mouseX <= x + width) {
			expanded = !expanded;
			updateHeight();
			return true;
		}
		
		// Check clicks on sub-settings when expanded
		if(expanded) {
			for(BooleanSetting setting : value) {
				if(setting.mouseClicked(mouseX, mouseY, mouseButton)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private void updateHeight() {
		if(expanded) {
			this.height = 16 + (value.size() * 20);
		} else {
			this.height = 16;
		}
	}
	
	@Override
	public void init() {
		super.init();
		for(BooleanSetting setting : value) {
			setting.init();
		}
	}
	
	@Override
	public void onClose() {
		super.onClose();
		for(BooleanSetting setting : value) {
			setting.onClose();
		}
	}
	
	public BooleanSetting getSetting(String name) {
		return value.stream()
			.filter(setting -> setting.title.equalsIgnoreCase(name))
			.findFirst()
			.orElse(null);
	}
	
	public List<BooleanSetting> getEnabledSettings() {
		return value.stream()
			.filter(BooleanSetting::getValue)
			.toList();
	}
}