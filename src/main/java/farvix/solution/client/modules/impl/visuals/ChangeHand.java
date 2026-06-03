package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.events.impl.input.EventInput;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Arm;

@ModuleInfo(name = "Change Hand", category = ModuleCategory.VISUALS, description = "Свапает руки при нажатии ЛКМ")
public class ChangeHand extends Module implements QuickImports {

    private Arm originalArm = null;

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[ChangeHand] Module enabled!");
        if (mc.options != null) {
            originalArm = mc.options.getMainArm().getValue();
            System.out.println("[ChangeHand] originalArm = " + originalArm);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[ChangeHand] Module disabled!");
        // Аккуратно возвращаем исходную руку при выключении модуля
        if (mc.options != null && originalArm != null) {
            System.out.println("[ChangeHand] Restoring originalArm to " + originalArm);
            mc.options.getMainArm().setValue(originalArm);
            if (mc.player != null) {
                mc.player.setMainArm(originalArm);
            }
        }
    }

    @EventHandler
    public void onInput(EventInput event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        // 0 - Левая кнопка мыши (LKM / GLFW_MOUSE_BUTTON_LEFT)
        if (event.isPressed(0)) {
            System.out.println("[ChangeHand] Left mouse button clicked detected! Scheduling swap in 30ms...");
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ignored) {}
            }).thenRunAsync(() -> {
                if (mc.options != null && mc.player != null) {
                    Arm currentArm = mc.options.getMainArm().getValue();
                    Arm newArm = currentArm == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT;
                    System.out.println("[ChangeHand] Swapping main arm from " + currentArm + " to " + newArm + " after 30ms");
                    mc.options.getMainArm().setValue(newArm);
                    mc.player.setMainArm(newArm);
                }
            }, mc);
        }
    }
}