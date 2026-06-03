package farvix.solution.client.modules.impl.environment;

import farvix.solution.api.events.impl.game.EventUpdate;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Fast XP", category = ModuleCategory.ENVIRONMENT,
        description = "Быстро бросает пузырьки опыта при зажатии ПКМ")
public class FastXP extends Module {

    public final SliderSetting delay = new SliderSetting(
            "Задержка (мс)", this, 10f, 0f, 50f, 1f);

    private long lastUseTime = 0;

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (!ClickUI.isToolrise()) {
            this.setEnabled(false);
            return;
        }
        if (mc.player == null || mc.world == null) return;

        // Не работаем если открыт любой экран (GUI, чат, инвентарь и т.д.)
        if (mc.currentScreen != null) return;

        // Проверяем что ПКМ зажата
        boolean rmb = GLFW.glfwGetMouseButton(
                mc.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (!rmb) return;

        // Проверяем что в руке именно пузырёк опыта (Experience Bottle)
        // Проверяем обе руки — основную и вспомогательную
        boolean hasXpBottle =
                mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE) ||
                mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE);

        if (!hasXpBottle) return;

        // Проверяем задержку
        long now = System.currentTimeMillis();
        long delayMs = (long) delay.getValue();
        if (now - lastUseTime < delayMs) return;
        lastUseTime = now;

        // Определяем какую руку использовать
        Hand hand = mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                ? Hand.MAIN_HAND
                : Hand.OFF_HAND;

        // Отправляем пакет использования предмета
        mc.player.networkHandler.sendPacket(
                new PlayerInteractItemC2SPacket(hand, 0,
                        mc.player.getYaw(), mc.player.getPitch()));

        // Уменьшаем стак вручную на клиенте для визуального отклика
        mc.player.swingHand(hand);
    }
}
