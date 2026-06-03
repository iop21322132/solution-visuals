package farvix.solution.client.modules.impl.visuals;

import lombok.Getter;
import farvix.solution.api.settings.impl.BindSetting;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import farvix.solution.api.events.impl.input.EventInput;
import farvix.solution.api.events.impl.game.EventMessage;
import farvix.solution.mixins.accessors.IBossBarHud;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Getter
@ModuleInfo(name = "NoChat", category = ModuleCategory.VISUALS, description = "Позволяет скрыть чат и автоматически скрывать его во время боя")
public class NoChat extends Module {

    public final BindSetting chatToggleBind = new BindSetting(
            "Кнопка скрытия чата", this, GLFW.GLFW_KEY_UNKNOWN
    );

    public final BooleanSetting hideInCombat = new BooleanSetting(
            "Скрывать при КТ", this
    );

    private boolean chatHidden = false;

    public NoChat() {
    }

    public boolean shouldHideChat() {
        if (!isEnabled()) {
            return false;
        }

        if (chatHidden) {
            return true;
        }

        if (hideInCombat.isEnabled() && isInCombat()) {
            return true;
        }

        return false;
    }

    public boolean isInCombat() {
        if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) {
            return false;
        }
        try {
            var bossBars = ((IBossBarHud) mc.inGameHud.getBossBarHud()).getBossBars();
            for (ClientBossBar bossBar : bossBars.values()) {
                String name = bossBar.getName().getString();
                String nameLower = name.toLowerCase();

                if (nameLower.contains("pvp") ||
                    nameLower.contains("пвп") ||
                    nameLower.contains("combat") ||
                    nameLower.contains("бой") ||
                    nameLower.contains("схватки") ||
                    nameLower.contains("fight") ||
                    nameLower.contains("battle")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @EventHandler
    public void onInput(EventInput e) {
        if (mc.player == null || mc.world == null) return;

        // Блокируем клавишу если открыто меню или инвентарь (кроме нашего ClickUI)
        if (mc.currentScreen != null && !(mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen)) {
            return;
        }

        int bindKey = chatToggleBind.getKey();
        if (bindKey > 0 && e.isPressed(bindKey)) {
            chatHidden = !chatHidden;
            playClickSound2();
            mc.player.sendMessage(Text.literal("§f§lSolution Visual §7» Чат " + (chatHidden ? "§cскрыт" : "§aпоказан")), false);
        }
    }

    @EventHandler
    public void onMessage(EventMessage e) {
        if (mc.player == null) return;

        // Если включена функция КТ и игрок находится в бою
        if (hideInCombat.isEnabled() && isInCombat()) {
            String msg = e.getMessage().trim();
            // Разрешаем только команды (начинающиеся с '/' или '.')
            if (!msg.startsWith("/") && !msg.startsWith(".")) {
                e.setCancelled(true);
                mc.player.sendMessage(Text.literal("§f§lSolution Visual §7» §cВы не можете отправлять обычные сообщения во время боя! Разрешены только команды."), false);
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        chatHidden = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        chatHidden = false;
    }
}
