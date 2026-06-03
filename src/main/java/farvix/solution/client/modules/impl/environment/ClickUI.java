package farvix.solution.client.modules.impl.environment;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.lwjgl.glfw.GLFW;
import farvix.solution.api.ui.clickgui.InterfaceScreen;
import farvix.solution.api.ui.solution.SolutionGuiScreen;
import farvix.solution.Client;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI;
import farvix.solution.client.modules.impl.environment.interfaces.api.UIElement;
import farvix.solution.api.settings.impl.BooleanSetting;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Setter @Getter
@ModuleInfo(name = "Click UI", category = ModuleCategory.ENVIRONMENT, key = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickUI extends Module {

    private static final Map<String, Boolean> savedStates = new HashMap<>();
    private static final java.util.List<String> TOOLRISE_DOMAINS = java.util.Arrays.asList(
            "mc.toolrise.space", "eu.toolrise.space", "tt.toolrise.space", "yt.toolrise.space");

    public final BooleanSetting particles = new BooleanSetting("Particles", "Показывать летние частицы на фоне GUI", this);

    private static String lastAddress = "";
    private static boolean isUpdating = false; // Защита от рекурсии (краша)

    public ClickUI() {
        particles.setValue(true);
    }

    private static boolean isAddressToolrise(String addr) {
        if (addr == null || addr.isEmpty() || addr.equalsIgnoreCase("none") || addr.contains("127.0.0.1")) return false;
        String cleanAddr = addr.split(":")[0].toLowerCase();
        for (String domain : TOOLRISE_DOMAINS) {
            if (cleanAddr.equals(domain) || cleanAddr.endsWith("." + domain)) return true;
        }
        return false;
    }

    public static boolean isToolrise() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        
        // В одиночной игре всегда разрешено
        if (mc.isInSingleplayer()) return true;

        net.minecraft.client.network.ServerInfo server = mc.getCurrentServerEntry();
        if (server == null) return true; // Разрешаем в главном меню для настройки биндов

        String currentAddr = server.address.toLowerCase();

        // Принудительно обновляем ограничения, если адрес изменился
        if (!isUpdating && !currentAddr.equals(lastAddress)) {
            lastAddress = currentAddr;
            updateModuleRestrictions(isAddressToolrise(currentAddr));
        }

        return isAddressToolrise(currentAddr);
    }

    /**
     * Главный метод для интерфейса: нужно ли отображать этот модуль прямо сейчас
     */
    public static boolean shouldShowModule(Module m) {
        if (!isRestricted(m)) return true;
        return isToolrise();
    }

    public static boolean isRestricted(Module m) {
        if (m == null) return false;
        
        // Проверка по классу
        if (m instanceof ItemScroller || 
            m instanceof NoFriendDamage || 
            m instanceof FastXP || 
            m instanceof CartHelper ||
            m instanceof PVPSafe) return true;
            
        // Проверка по имени (максимально широкий охват)
        String name = m.getName().toLowerCase().replace(" ", "").replace("_", "").replace("-", "");
        return name.contains("autoscroller") || 
               name.contains("itemscroller") || 
               name.contains("fastxp") || 
               name.contains("xp") ||
               name.contains("nofriend") || 
               name.contains("frienddamage") || 
               name.contains("carthelper") || 
               name.contains("pvpsafe");
    }

    /**
     * Вызывается из миксинов при входе на сервер.
     */
    public static void onServerJoin() {
        lastAddress = ""; // Сбрасываем кэш адреса, чтобы при входе форсировать проверку
        isToolrise();
    }

    public static void onServerJoin(boolean toolrise) {
        updateModuleRestrictions(toolrise);
    }

    private static void updateModuleRestrictions(boolean toolrise) {
        if (isUpdating) return;
        isUpdating = true;

        try {
            for (Module m : Client.getInstance().getModuleManager().getModules()) {
                if (isRestricted(m)) {
                    if (toolrise) {
                        if (savedStates.getOrDefault(m.getName(), false)) {
                            m.setEnabled(true);
                        }
                        savedStates.remove(m.getName());
                    } else {
                        if (m.isEnabled()) {
                            savedStates.put(m.getName(), true);
                            m.setEnabled(false);
                        }
                    }
                }
            }

            // Обновляем список модулей в GUI
            if (net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof InterfaceScreen gui) {
                gui.rebuildModules();
            }
        } finally {
            isUpdating = false;
        }
    }

    @Override
    public void setKey(int key) {
        // Запрещаем сброс бинда на NONE (-1)
        if (key == -1) {
            return; // Игнорируем попытку сброса
        }
        super.setKey(key);
    }

    @Override
    public void toggle() {
        // Проверяем, не находимся ли мы в меню
        if (mc.currentScreen instanceof TitleScreen ||
            mc.currentScreen instanceof MultiplayerScreen ||
            mc.currentScreen instanceof SelectWorldScreen ||
            mc.currentScreen instanceof CreateWorldScreen) {
            // Не открываем ClickUI в этих экранах
            return;
        }
        
        // Просто переключаем состояние, не вызывая родительский toggle()
        // Родительский toggle() делает this.enabled = !this.enabled, что переключит обратно
        setEnabled(!this.isEnabled());
    }

    @Override
    public void onEnable() {
        // Сбрасываем кеш адреса, чтобы принудительно перепроверить сервер при открытии меню
        lastAddress = ""; 
        onServerJoin();
        
        // Дополнительная проверка при включении
        if (mc.currentScreen instanceof TitleScreen ||
            mc.currentScreen instanceof MultiplayerScreen ||
            mc.currentScreen instanceof SelectWorldScreen ||
            mc.currentScreen instanceof CreateWorldScreen) {
            // Отменяем включение
            setEnabled(false);
            return;
        }
        
        farvix.solution.client.modules.Module.playClickSound2();
        // Открываем старый InterfaceScreen
        mc.setScreen(new InterfaceScreen());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.currentScreen instanceof InterfaceScreen screen) {
            screen.setClosing(true);
        }
        if (mc.currentScreen instanceof SolutionGuiScreen) {
            mc.setScreen(null);
        }
    }
}
