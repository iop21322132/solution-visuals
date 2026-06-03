package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.environment.PVPSafe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("RETURN"))
    private void onInitWidgets(CallbackInfo ci) {
        PVPSafe pvpSafe = Client.getInstance().getModuleManager().get(PVPSafe.class);
        if (pvpSafe != null && pvpSafe.isEnabled() && pvpSafe.isPvpMode()) {
            // Блокируем кнопку "Disconnect" (выход на сервер)
            // Ищем кнопку по тексту
            this.children().stream()
                .filter(element -> element instanceof ButtonWidget)
                .map(element -> (ButtonWidget) element)
                .filter(button -> {
                    String message = button.getMessage().getString();
                    return message.contains("Disconnect") || 
                           message.contains("disconnect") ||
                           message.contains("Save and Quit") ||
                           message.contains("Выйти");
                })
                .forEach(button -> button.active = false);
        }
    }

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void onDisconnect(CallbackInfo ci) {
        PVPSafe pvpSafe = Client.getInstance().getModuleManager().get(PVPSafe.class);
        if (pvpSafe != null && pvpSafe.isEnabled() && pvpSafe.isPvpMode()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§c[PVP Safe] §fНельзя выйти из игры во время боя!"), false);
            }
            ci.cancel();
        }
    }
}
