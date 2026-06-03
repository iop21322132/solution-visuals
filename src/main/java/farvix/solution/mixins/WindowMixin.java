package farvix.solution.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {

    private static double getScale(Window window) {
        double physicalWidth = window.getWidth();
        if (physicalWidth <= 0) {
            return 1.0;
        }
        double customScale = physicalWidth / 1440.0;
        if (customScale < 0.5) customScale = 0.5;
        return customScale;
    }

    @Inject(method = "getScaleFactor", at = @At("HEAD"), cancellable = true)
    private void onGetScaleFactor(CallbackInfoReturnable<Double> cir) {
        Window window = (Window) (Object) this;
        cir.setReturnValue(getScale(window));
    }

    @Inject(method = "getScaledWidth", at = @At("HEAD"), cancellable = true)
    private void onGetScaledWidth(CallbackInfoReturnable<Integer> cir) {
        Window window = (Window) (Object) this;
        double scale = getScale(window);
        int scaledWidth = (int) (window.getWidth() / scale);
        if (scaledWidth <= 0) scaledWidth = 1;
        cir.setReturnValue(scaledWidth);
    }

    @Inject(method = "getScaledHeight", at = @At("HEAD"), cancellable = true)
    private void onGetScaledHeight(CallbackInfoReturnable<Integer> cir) {
        Window window = (Window) (Object) this;
        double scale = getScale(window);
        double physicalHeight = window.getHeight();
        if (physicalHeight <= 0) {
            cir.setReturnValue(540);
            return;
        }
        int scaledHeight = (int) (physicalHeight / scale);
        if (scaledHeight <= 0) scaledHeight = 1;
        cir.setReturnValue(scaledHeight);
    }
}
