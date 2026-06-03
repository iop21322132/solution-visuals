package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.DynamicIsland;
import farvix.solution.client.modules.impl.visuals.Zoom;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import farvix.solution.api.events.impl.input.EventInput;
import farvix.solution.api.interfaces.QuickImports;

@Mixin(Mouse.class)
public class MouseMixin implements QuickImports {

    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onKey(long windowPointer, int button, int action, int mods, CallbackInfo ci) {
        Window currentWindow = mc.getWindow();
        if (currentWindow == null || windowPointer != currentWindow.getHandle()) return;

        // Dynamic Island click — работает всегда, даже при открытом GUI
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            DynamicIsland di = Client.getInstance().getModuleManager().get(DynamicIsland.class);
            if (di != null && di.isEnabled()) {
                double scaledX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
                double scaledY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
                di.handleClick(scaledX, scaledY);
            }
        }

        // PlayerRadialMenu теперь опрашивает GLFW сам в onRender2D — здесь ничего не нужно


        if (mc.currentScreen != null) return;
        new EventInput(button, button, action == GLFW.GLFW_RELEASE).call();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long windowPointer, double horizontal, double vertical, CallbackInfo ci) {
        Window currentWindow = mc.getWindow();
        if (currentWindow == null || windowPointer != currentWindow.getHandle()) return;
        if (mc.currentScreen != null) return;

        // Вызываем событие EventMouseScroll
        farvix.solution.api.events.impl.input.EventMouseScroll scrollEvent = 
            new farvix.solution.api.events.impl.input.EventMouseScroll(horizontal, vertical);
        scrollEvent.call();
        
        if (scrollEvent.isCancelled()) {
            ci.cancel();
            return;
        }

        Zoom zoomModule = Client.getInstance().getModuleManager().get(Zoom.class);
        if (Zoom.isZoomKeyHeld(zoomModule)) {
            Zoom.onScroll(vertical);
            ci.cancel();
        }
    }
}
