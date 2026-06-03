package dev.padiloi1337.hitcolor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.padiloi1337.hitcolor.helpers.render.DrawHelper;
import dev.padiloi1337.hitcolor.settings.Setting;
import dev.padiloi1337.hitcolor.settings.impl.ColorSetting;
import dev.padiloi1337.hitcolor.settings.impl.MultiSetting;
import dev.padiloi1337.hitcolor.features.CustomHitbox;
import dev.padiloi1337.hitcolor.ui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HitColor.MOD_ID)
public final class HitColor implements Wrapper {
	
	public static final String MOD_ID = "hitcolor";
	private static HitColor instance;
	public final Settings settings;
	public final GuiScreen guiScreen;
	public final CustomHitbox customHitbox;
	
	// stage 1
	public HitColor() {
		instance = this;
		settings = new Settings();
		guiScreen = new GuiScreen();
		customHitbox = new CustomHitbox();
		settings.registerCustomHitboxSettings();
		RenderSystem.recordRenderCall(DrawHelper::preInit);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
    }

	// stage 2
	private void init(final FMLCommonSetupEvent event) {
		ConfigManager.load();
		ClientRegistry.registerKeyBinding(settings.toggleGuikey);
		RenderSystem.recordRenderCall(DrawHelper::postInit);
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(customHitbox);
		System.out.println("HitColor: CustomHitbox registered to event bus");
	}
	
	@SubscribeEvent
    public void onPress(InputEvent.KeyInputEvent event) {
//		for (Entity ent : MC.world.getAllEntities()) {
//			if (ent instanceof LivingEntity) {
//				((LivingEntity) ent).hurtTime = 50;
//			}
//		}
//		if (event.getKey() == GLFW.GLFW_KEY_O) {
//			LivingRenderer.getPackedOverlay(MC.player, MC.getRenderPartialTicks());
//		}
    	if((MC.currentScreen == null || MC.currentScreen == guiScreen) && event.getAction() == 1 && event.getKey() == settings.toggleGuikey.getKey().getKeyCode())
    		guiScreen.toggle();
    }
	
	public static HitColor getInstance() {
		return instance;
	}
	
	public final class Settings {
	
		public final List<Setting<?>> settingsList = new ArrayList<>();
		public final List<Setting<?>> values = new ArrayList<>();
		public final MultiSetting color = register(new MultiSetting("Hit Color", true, new ColorSetting("Color Picker", Color.red)));
		
		// Custom Hitbox settings
		public final MultiSetting customHitboxEnabled = register(new MultiSetting("Custom Hitbox", false, customHitbox.getColor()));
		
		public final KeyBinding toggleGuikey = new KeyBinding("Hit Color GUI", 345,
				"" + HitColor.MOD_ID);
		
		private <T extends Setting<?>> T register(T setting) {
			settingsList.add(setting);
			values.add(setting);
			if(setting instanceof MultiSetting)
				values.add(((MultiSetting)setting).getSubSetting());
    		return setting;
    	}
    	
    	// Добавляем отдельные настройки для CustomHitbox
    	public void registerCustomHitboxSettings() {
    		settingsList.add(customHitbox.getHideEyeLine());
    		settingsList.add(customHitbox.getSize());
    		settingsList.add(customHitbox.getEntityTypes());
    		
    		values.add(customHitbox.getHideEyeLine());
    		values.add(customHitbox.getSize());
    		values.add(customHitbox.getEntityTypes());
    	}
		
	}

}
