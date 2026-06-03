package farvix.solution.client.modules.impl.player;

import farvix.solution.api.events.impl.game.EventUpdate;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.StringSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;

@ModuleInfo(name = "Auto Respawn", category = ModuleCategory.PLAYER,
        description = "Автоматически возрождает игрока и выполняет команду после смерти")
public class AutoRespawn extends Module {

    /** Автоматически нажимать кнопку "Возродиться" */
    public final BooleanSetting autoRespawn = new BooleanSetting(
            "Авто возрождение", this);

    /** Отправлять сообщение/команду после возрождения */
    public final BooleanSetting runCommand = new BooleanSetting(
            "Команда после респавна", this);

    /** Текст для отправки. Начинается с / — команда, иначе — чат */
    public final StringSetting command = new StringSetting(
            "Текст или /команда", "Введите текст или /команду", "", 100, this);

    private boolean commandSent    = false;
    private boolean wasOnDeathScreen = false;

    public AutoRespawn() {
        autoRespawn.setEnabled(true);
        // Поле ввода всегда видно когда настройки раскрыты
        // (пользователь сам решает использовать его или нет)
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        // ── Авто-респавн ──────────────────────────────────────────────────────
        if (mc.currentScreen instanceof DeathScreen) {
            wasOnDeathScreen = true;
            commandSent = false;

            if (autoRespawn.isEnabled()) {
                mc.player.requestRespawn();
                mc.setScreen(null);
            }
            return;
        }

        // ── Команда/сообщение после возрождения ───────────────────────────────
        if (wasOnDeathScreen && !commandSent
                && runCommand.isEnabled()
                && mc.player.isAlive()) {

            String text = command.getText().trim();
            if (!text.isEmpty()) {
                if (text.startsWith("/")) {
                    // Команда — убираем / и отправляем как команду
                    mc.player.networkHandler.sendCommand(text.substring(1));
                } else {
                    // Обычный текст — отправляем в чат
                    mc.player.networkHandler.sendChatMessage(text);
                }
            }
            commandSent = true;
            wasOnDeathScreen = false;
        }

        // Сброс если игрок жив и команда уже отправлена
        if (wasOnDeathScreen && mc.player.isAlive() && commandSent) {
            wasOnDeathScreen = false;
        }
    }
}
