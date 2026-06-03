package dev.padiloi1337.hitcolor;

import dev.padiloi1337.hitcolor.helpers.font.CustomFont;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;

public interface Wrapper {
		
	HitColor CLIENT = HitColor.getInstance();
	Minecraft MC = Minecraft.getInstance();
	MainWindow WINDOW = MC.getMainWindow();
	CustomFont DEFAULT_20 = new CustomFont("Biko.ttf", 20, true, 0.0f, 0.0f, -1.0f);
	CustomFont DEFAULT_24 = new CustomFont("Biko.ttf", 24, true, 0.0f, 0.0f, -1.0f);
	CustomFont DEFAULT_16 = new CustomFont("Biko.ttf", 16, true, 0.0f, 0.0f, -0.5f);
	CustomFont ICONS_22 = new CustomFont("Icons.ttf", 22, true, 0.0f, 0.0f, 0.0f);
	CustomFont ICONS_42 = new CustomFont("Icons.ttf", 42, true, 0.0f, 0.0f, 0.0f);
	
}
