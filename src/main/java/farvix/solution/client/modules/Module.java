package farvix.solution.client.modules;

import lombok.Getter;
import lombok.Setter;
import farvix.solution.Client;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.api.Parent;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Module extends Parent implements QuickImports {
    private final ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);

    private final String name;
    private final String description;
    private final ModuleCategory category;

    @Setter
    private int key;
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    private final List<Setting> settings = new ArrayList<>();

    public Module() {
        this.name = getModuleInfo().name();
        this.description = getModuleInfo().description();
        this.category = getModuleInfo().category();
        this.key = getModuleInfo().key();
    }

    public Module(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = -1;
    }

    public void onEnable() {
        Client.getInstance().getBus().unsubscribe(this);
        Client.getInstance().getBus().subscribe(this);
    }

    public void onDisable() {
        Client.getInstance().getBus().unsubscribe(this);
    }

    public void toggle() {
        this.enabled = !this.enabled;

        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }

        // Play click sound via Java AudioSystem (supports .wav directly)
        playClickSound();
        
        // Show Dynamic Island notification
        showDynamicIslandNotification();

        // Autosave config when module toggled
        try {
            Client.getInstance().getConfigManager().saveConfig("_autosave");
        } catch (Throwable ignored) {}
    }
    
    private void showDynamicIslandNotification() {
        try {
            farvix.solution.client.modules.impl.visuals.DynamicIsland dynamicIsland = 
                Client.getInstance().getModuleManager().get(
                    farvix.solution.client.modules.impl.visuals.DynamicIsland.class);
            if (dynamicIsland != null && dynamicIsland.isEnabled()) {
                dynamicIsland.showModuleNotification(this.name, this.enabled);
            }
        } catch (Exception ignored) {
            // Ignore if DynamicIsland is not available
        }
    }

    private static void playClickSound() {
        playWav("/assets/solution/Click.wav");
    }

    public static void playClickSound2() {
        playWav("/assets/solution/Click2.wav");
    }

    private static void playWav(String resourcePath) {
        try {
            java.io.InputStream stream = Module.class.getResourceAsStream(resourcePath);
            if (stream == null) {
                // Try without leading slash
                stream = Module.class.getClassLoader().getResourceAsStream(
                        resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
            }
            if (stream == null) {
                System.out.println("[Sound] Not found: " + resourcePath);
                return;
            }
            javax.sound.sampled.AudioInputStream audio =
                    javax.sound.sampled.AudioSystem.getAudioInputStream(
                            new java.io.BufferedInputStream(stream));
            javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(audio);
            clip.start();
        } catch (Throwable t) {
            System.out.println("[Sound] Error playing " + resourcePath + ": " + t.getMessage());
        }
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        this.enabled = enabled;

        // Autosave config when module state set
        try {
            Client.getInstance().getConfigManager().saveConfig("_autosave");
        } catch (Throwable ignored) {}
    }

    public static boolean fullNullCheck() {
        return mc.player == null || mc.world == null;
    }
}
