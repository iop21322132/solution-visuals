package farvix.solution.mixins;

import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseCursorMixin {
    
    @Shadow @Final private MinecraftClient client;
    
    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    private void onLockCursor(CallbackInfo ci) {
        try {
            if (client == null || client.getWindow() == null) return;
            // Когда открыт GUI, не блокируем курсор - оставляем системный видимым
            if (client.currentScreen != null) {
                long window = client.getWindow().getHandle();
                GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
                ci.cancel();
            }
        } catch (Exception ignored) {}
    }
    
    @Inject(method = "unlockCursor", at = @At("RETURN"))
    private void onUnlockCursor(CallbackInfo ci) {
        try {
            if (client == null || client.getWindow() == null) return;
            // Форсим нормальный курсор когда разблокируем
            long window = client.getWindow().getHandle();
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        } catch (Exception ignored) {}
    }
}
