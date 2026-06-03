package farvix.solution.api.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import farvix.solution.Client;
import farvix.solution.client.managers.MacroManager;
import farvix.solution.client.managers.WaypointManager;
import farvix.solution.client.modules.Module;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private final File configDir;
    private final Gson gson;
    private final List<Config> configs;
    
    public ConfigManager() {
        // Получаем путь к корневой папке Minecraft (где находится папка resourcepacks)
        File minecraftDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().toFile();
        this.configDir = new File(minecraftDir, "solution-configs");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configs = new ArrayList<>();
        
        loadConfigList();
    }
    
    public File getConfigDir() {
        return configDir;
    }
    
    public void saveConfig(String name) {
        try {
            File configFile = new File(configDir, name + ".sol");
            JsonObject root = new JsonObject();
            JsonObject modulesObj = new JsonObject();
            
            for (Module module : Client.getInstance().getModuleManager().getModules()) {
                JsonObject moduleObj = new JsonObject();
                moduleObj.addProperty("enabled", module.isEnabled());
                moduleObj.addProperty("key", module.getKey());
                
                // Сохраняем настройки модуля
                JsonObject settingsObj = new JsonObject();
                module.getSettings().forEach(setting -> {
                    settingsObj.add(setting.getName(), setting.save());
                });
                moduleObj.add("settings", settingsObj);
                
                modulesObj.add(module.getName(), moduleObj);
            }
            
            root.add("modules", modulesObj);

            // Сохраняем тему
            JsonObject themeObj = new JsonObject();
            farvix.solution.client.managers.ThemeManager.Theme theme =
                    farvix.solution.client.managers.ThemeManager.getInstance().getCurrentTheme();
            themeObj.addProperty("name", theme.getName());
            java.awt.Color bg = theme.getBackgroundColor();
            themeObj.addProperty("r", bg.getRed());
            themeObj.addProperty("g", bg.getGreen());
            themeObj.addProperty("b", bg.getBlue());
            themeObj.addProperty("a", bg.getAlpha());
            root.add("theme", themeObj);

            // Сохраняем тему главного меню
            root.addProperty("mainMenuThemeIndex", farvix.solution.api.ui.mainmenu.MainMenu.themeIndex);
            root.addProperty("syncColorsWithTheme", farvix.solution.api.settings.impl.ColorSetting.syncWithTheme);
            root.addProperty("extendedMode", farvix.solution.api.ui.clickgui.impl.sidebar.Sidebar.extendedMode);

            // Сохраняем список друзей (Friends)
            JsonArray friendsArray = new JsonArray();
            for (String friend : farvix.solution.api.util.FriendManager.getFriends()) {
                friendsArray.add(friend);
            }
            root.add("friends", friendsArray);

            // Сохраняем позиции HUD-элементов
            JsonObject hudObj = new JsonObject();
            hudObj.addProperty("targetHudX", farvix.solution.client.modules.impl.visuals.TargetHud.hudX);
            hudObj.addProperty("targetHudY", farvix.solution.client.modules.impl.visuals.TargetHud.hudY);
            hudObj.addProperty("potionHudX", farvix.solution.client.modules.impl.visuals.PotionEffects.hudX);
            hudObj.addProperty("potionHudY", farvix.solution.client.modules.impl.visuals.PotionEffects.hudY);
            hudObj.addProperty("inventoryHudX", farvix.solution.client.modules.impl.visuals.InventoryHUD.hudX);
            hudObj.addProperty("inventoryHudY", farvix.solution.client.modules.impl.visuals.InventoryHUD.hudY);
            hudObj.addProperty("watermarkSolutionX", farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.solutionX);
            hudObj.addProperty("watermarkSolutionY", farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.solutionY);
            hudObj.addProperty("watermarkTimeX",  farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.timeX);
            hudObj.addProperty("watermarkTimeY",  farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.timeY);
            hudObj.addProperty("watermarkFpsX",   farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.fpsX);
            hudObj.addProperty("watermarkFpsY",   farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.fpsY);
            hudObj.addProperty("watermarkCoordsX", farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.coordsX);
            hudObj.addProperty("watermarkCoordsY", farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.coordsY);
            hudObj.addProperty("guiX", farvix.solution.api.ui.clickgui.InterfaceScreen.savedX);
            hudObj.addProperty("guiY", farvix.solution.api.ui.clickgui.InterfaceScreen.savedY);
            hudObj.addProperty("armorHudX", farvix.solution.client.modules.impl.visuals.ArmorHUD.hudX);
            hudObj.addProperty("armorHudY", farvix.solution.client.modules.impl.visuals.ArmorHUD.hudY);
            hudObj.addProperty("hotKeysX", farvix.solution.client.modules.impl.environment.ModuleHotKeys.hudX);
            hudObj.addProperty("hotKeysY", farvix.solution.client.modules.impl.environment.ModuleHotKeys.hudY);
            hudObj.addProperty("scoreboardHudX", farvix.solution.client.modules.impl.visuals.ScoreboardHud.hudX);
            hudObj.addProperty("scoreboardHudY", farvix.solution.client.modules.impl.visuals.ScoreboardHud.hudY);
            hudObj.addProperty("noteHudX", farvix.solution.client.modules.impl.visuals.Note.hudX);
            hudObj.addProperty("noteHudY", farvix.solution.client.modules.impl.visuals.Note.hudY);
            root.add("hud", hudObj);
            
            // Сохраняем Waypoints
            JsonArray waypointsArray = new JsonArray();
            for (WaypointManager.Waypoint wp : WaypointManager.list()) {
                JsonObject wpObj = new JsonObject();
                wpObj.addProperty("name", wp.getName());
                wpObj.addProperty("x", wp.getX());
                wpObj.addProperty("y", wp.getY());
                wpObj.addProperty("z", wp.getZ());
                waypointsArray.add(wpObj);
            }
            root.add("waypoints", waypointsArray);
            
            // Сохраняем Macros
            JsonArray macrosArray = new JsonArray();
            for (MacroManager.Macro macro : MacroManager.getInstance().getMacros()) {
                JsonObject macroObj = new JsonObject();
                macroObj.addProperty("message", macro.getMessage());
                macroObj.addProperty("key", macro.getKey());
                macrosArray.add(macroObj);
            }
            root.add("macros", macrosArray);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }
            
            // Обновляем список конфигов
            Config config = configs.stream()
                    .filter(c -> c.name.equals(name))
                    .findFirst()
                    .orElse(null);
            
            if (config == null) {
                config = new Config(name);
                configs.add(config);
            }
            
            config.lastModified = System.currentTimeMillis();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void loadConfig(String name) {
        try {
            File configFile = new File(configDir, name + ".sol");
            if (!configFile.exists()) {
                return;
            }
            
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                JsonObject modulesObj = root.getAsJsonObject("modules");
                
                for (Module module : Client.getInstance().getModuleManager().getModules()) {
                    // Скрытые модули всегда включены — не трогаем их состояние
                    boolean isHidden = module instanceof farvix.solution.client.modules.impl.visuals.PlayerRadialMenu
                            || module instanceof farvix.solution.client.modules.impl.environment.DiscordRPC
                            || module instanceof farvix.solution.client.modules.impl.visuals.ContextMenuModule;
                    if (isHidden) continue;

                    if (modulesObj.has(module.getName())) {
                        JsonObject moduleObj = modulesObj.getAsJsonObject(module.getName());
                        
                        boolean enabled = moduleObj.get("enabled").getAsBoolean();
                        int key = moduleObj.get("key").getAsInt();
                        
                        // Only change enabled state if it differs to avoid double subscribe
                        if (module.isEnabled() != enabled) {
                            module.setEnabled(enabled);
                        }
                        module.setKey(key);
                        
                        // Загружаем настройки
                        if (moduleObj.has("settings")) {
                            JsonObject settingsObj = moduleObj.getAsJsonObject("settings");
                            module.getSettings().forEach(setting -> {
                                if (settingsObj.has(setting.getName())) {
                                    setting.load(settingsObj.get(setting.getName()));
                                }
                            });
                        }
                    }
                }

                // Загружаем тему
                if (root.has("theme")) {
                    JsonObject themeObj = root.getAsJsonObject("theme");
                    int r = themeObj.get("r").getAsInt();
                    int g = themeObj.get("g").getAsInt();
                    int b = themeObj.get("b").getAsInt();
                    int a = themeObj.get("a").getAsInt();
                    String themeName = themeObj.get("name").getAsString();
                    java.awt.Color color = new java.awt.Color(r, g, b, a);
                    farvix.solution.client.managers.ThemeManager.getInstance().setTheme(
                            new farvix.solution.api.ui.clickgui.impl.theme.ThemeScreen.SingleColorTheme(themeName, color)
                    );
                    farvix.solution.api.TempColor.setThemeBackground(color);
                }

                // Загружаем тему главного меню
                if (root.has("mainMenuThemeIndex")) {
                    farvix.solution.api.ui.mainmenu.MainMenu.themeIndex = root.get("mainMenuThemeIndex").getAsInt();
                }
                if (root.has("syncColorsWithTheme")) {
                    farvix.solution.api.settings.impl.ColorSetting.syncWithTheme = root.get("syncColorsWithTheme").getAsBoolean();
                }
                if (root.has("extendedMode")) {
                    farvix.solution.api.ui.clickgui.impl.sidebar.Sidebar.extendedMode = root.get("extendedMode").getAsBoolean();
                }

                // Загружаем список друзей (Friends)
                if (root.has("friends")) {
                    farvix.solution.api.util.FriendManager.getFriends().clear();
                    JsonArray friendsArray = root.getAsJsonArray("friends");
                    for (int i = 0; i < friendsArray.size(); i++) {
                        farvix.solution.api.util.FriendManager.addFriend(friendsArray.get(i).getAsString());
                    }
                }

                // Загружаем позиции HUD-элементов
                if (root.has("hud")) {
                    JsonObject h = root.getAsJsonObject("hud");
                    if (h.has("targetHudX"))       farvix.solution.client.modules.impl.visuals.TargetHud.hudX    = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("targetHudX").getAsFloat(), 0.0104f);
                    if (h.has("targetHudY"))       farvix.solution.client.modules.impl.visuals.TargetHud.hudY    = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("targetHudY").getAsFloat(), 0.1111f);
                    if (h.has("potionHudX"))       farvix.solution.client.modules.impl.visuals.PotionEffects.hudX = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("potionHudX").getAsFloat(), 0.0104f);
                    if (h.has("potionHudY"))       farvix.solution.client.modules.impl.visuals.PotionEffects.hudY = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("potionHudY").getAsFloat(), 0.0185f);
                    if (h.has("inventoryHudX"))    farvix.solution.client.modules.impl.visuals.InventoryHUD.hudX  = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("inventoryHudX").getAsFloat(), 0.0104f);
                    if (h.has("inventoryHudY"))    farvix.solution.client.modules.impl.visuals.InventoryHUD.hudY  = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("inventoryHudY").getAsFloat(), 0.0185f);
                    if (h.has("watermarkSolutionX"))  farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.solutionX = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("watermarkSolutionX").getAsFloat(), 0.0052f);
                    if (h.has("watermarkSolutionY"))  farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.solutionY = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("watermarkSolutionY").getAsFloat(), 0.0093f);
                    if (h.has("watermarkTimeX"))   farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.timeX  = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("watermarkTimeX").getAsFloat(), -1f);
                    if (h.has("watermarkTimeY"))   farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.timeY  = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("watermarkTimeY").getAsFloat(), 0.0093f);
                    if (h.has("watermarkFpsX"))    farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.fpsX   = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("watermarkFpsX").getAsFloat(), 0.0052f);
                    if (h.has("watermarkFpsY"))    farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.fpsY   = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("watermarkFpsY").getAsFloat(), 0.0389f);
                    if (h.has("watermarkCoordsX")) farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.coordsX = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("watermarkCoordsX").getAsFloat(), 0.0052f);
                    if (h.has("watermarkCoordsY")) farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI.coordsY = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("watermarkCoordsY").getAsFloat(), 0.0685f);
                    if (h.has("guiX"))             farvix.solution.api.ui.clickgui.InterfaceScreen.savedX = h.get("guiX").getAsFloat();
                    if (h.has("guiY"))             farvix.solution.api.ui.clickgui.InterfaceScreen.savedY = h.get("guiY").getAsFloat();
                    if (h.has("armorHudX"))        farvix.solution.client.modules.impl.visuals.ArmorHUD.hudX = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("armorHudX").getAsFloat(), -1f);
                    if (h.has("armorHudY"))        farvix.solution.client.modules.impl.visuals.ArmorHUD.hudY = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("armorHudY").getAsFloat(), -1f);
                    if (h.has("hotKeysX"))         farvix.solution.client.modules.impl.environment.ModuleHotKeys.hudX = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("hotKeysX").getAsFloat(), -1f);
                    else if (h.has("arrayListX"))  farvix.solution.client.modules.impl.environment.ModuleHotKeys.hudX = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("arrayListX").getAsFloat(), -1f);
                    if (h.has("hotKeysY"))         farvix.solution.client.modules.impl.environment.ModuleHotKeys.hudY = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("hotKeysY").getAsFloat(), 0.0185f);
                    else if (h.has("arrayListY"))  farvix.solution.client.modules.impl.environment.ModuleHotKeys.hudY = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("arrayListY").getAsFloat(), 0.0185f);
                    if (h.has("scoreboardHudX"))   farvix.solution.client.modules.impl.visuals.ScoreboardHud.hudX = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("scoreboardHudX").getAsFloat(), -1f);
                    if (h.has("scoreboardHudY"))   farvix.solution.client.modules.impl.visuals.ScoreboardHud.hudY = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("scoreboardHudY").getAsFloat(), -1f);
                    if (h.has("noteHudX"))         farvix.solution.client.modules.impl.visuals.Note.hudX = farvix.solution.api.ui.hud.HudPositionHelper.readX(h.get("noteHudX").getAsFloat(), 0.0104f);
                    if (h.has("noteHudY"))         farvix.solution.client.modules.impl.visuals.Note.hudY = farvix.solution.api.ui.hud.HudPositionHelper.readY(h.get("noteHudY").getAsFloat(), 0.0185f);
                }
                
                // Загружаем Waypoints
                if (root.has("waypoints")) {
                    WaypointManager.clear();
                    JsonArray waypointsArray = root.getAsJsonArray("waypoints");
                    for (int i = 0; i < waypointsArray.size(); i++) {
                        JsonObject wpObj = waypointsArray.get(i).getAsJsonObject();
                        String wName = wpObj.get("name").getAsString();
                        double wX = wpObj.get("x").getAsDouble();
                        double wY = wpObj.get("y").getAsDouble();
                        double wZ = wpObj.get("z").getAsDouble();
                        WaypointManager.add(wName, wX, wY, wZ);
                    }
                }
                
                // Загружаем Macros
                if (root.has("macros")) {
                    MacroManager.getInstance().clear();
                    JsonArray macrosArray = root.getAsJsonArray("macros");
                    for (int i = 0; i < macrosArray.size(); i++) {
                        JsonObject macroObj = macrosArray.get(i).getAsJsonObject();
                        String mMessage = macroObj.get("message").getAsString();
                        int mKey = macroObj.get("key").getAsInt();
                        MacroManager.getInstance().addMacro(mMessage, mKey);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void deleteConfig(String name) {
        try {
            File configFile = new File(configDir, name + ".sol");
            if (configFile.exists()) {
                configFile.delete();
            }
            
            configs.removeIf(c -> c.name.equals(name));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<Config> getConfigs() {
        return configs;
    }
    
    private void loadConfigList() {
        configs.clear();
        
        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".sol"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".sol", "");
                Config config = new Config(name);
                config.lastModified = file.lastModified();
                configs.add(config);
            }
        }
    }
    
    public void refreshConfigs() {
        loadConfigList();
    }
}
