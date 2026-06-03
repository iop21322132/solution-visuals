package farvix.solution.mixins;

import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import farvix.solution.api.events.impl.input.EventInput;
import farvix.solution.api.interfaces.QuickImports;

@Mixin(Keyboard.class)
public class KeyboardMixin implements QuickImports {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        Window currentWindow = mc.getWindow();
        
        if (currentWindow == null || windowPointer != currentWindow.getHandle()) {
            return;
        }
        
        // Блокировка правого Shift в меню
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT && action == GLFW.GLFW_PRESS) {
            if (mc.currentScreen instanceof TitleScreen ||
                mc.currentScreen instanceof MultiplayerScreen ||
                mc.currentScreen instanceof SelectWorldScreen ||
                mc.currentScreen instanceof CreateWorldScreen) {
                // Блокируем нажатие правого Shift в этих экранах
                ci.cancel();
                return;
            }
        }
        
        if (action == GLFW.GLFW_PRESS) {
            new EventInput(key, scanCode, false).call();
        } else if (action == GLFW.GLFW_RELEASE) {
            new EventInput(key, scanCode, true).call();
        }
    }
}
