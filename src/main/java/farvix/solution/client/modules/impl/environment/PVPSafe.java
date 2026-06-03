package farvix.solution.client.modules.impl.environment;

import farvix.solution.api.events.impl.game.EventUpdate;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import farvix.solution.mixins.accessors.IBossBarHud;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.hud.ClientBossBar;

@ModuleInfo(name = "PVP Safe", category = ModuleCategory.ENVIRONMENT,
        description = "Блокирует выход во время боя")
public class PVPSafe extends Module implements QuickImports {

    private boolean wasInPvp = false;

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        boolean inPvp = isPvpMode();
        
        // Обновляем состояние
        wasInPvp = inPvp;
    }

    /**
     * Проверяет, находится ли игрок в PVP режиме
     * Использует ту же логику, что и Dynamic Island
     */
    public boolean isPvpMode() {
        if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) {
            return false;
        }
        
        var bossBars = ((IBossBarHud) mc.inGameHud.getBossBarHud()).getBossBars();
        
        for (ClientBossBar bossBar : bossBars.values()) {
            String name = bossBar.getName().getString();
            String nameLower = name.toLowerCase();
            
            // Проверяем различные варианты текста PVP
            if (nameLower.contains("pvp") || 
                nameLower.contains("пвп") ||
                nameLower.contains("combat") ||
                nameLower.contains("бой") ||
                nameLower.contains("схватки") ||  // "до конца схватки"
                nameLower.contains("fight") ||
                nameLower.contains("battle")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        wasInPvp = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        wasInPvp = false;
    }
}
