package farvix.solution.mixins;

import farvix.solution.api.ui.mainmenu.MainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        MinecraftClient.getInstance().setScreen(new MainMenu());
        ci.cancel();
    }
}
