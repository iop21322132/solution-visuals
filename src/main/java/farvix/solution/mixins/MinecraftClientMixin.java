package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.environment.PVPSafe;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import farvix.solution.api.events.impl.game.EventTick;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements QuickImports {

    @Inject(at = @At("TAIL"), method = "<init>")
    private void onInit(RunArgs args, CallbackInfo ci) {
        Fonts.init();
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void onWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Solution Visual");
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo callbackInfo) {
        new EventTick().call();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"), cancellable = true)
    private void onDisconnect(Screen screen, boolean transferring, CallbackInfo ci) {
        PVPSafe pvpSafe = Client.getInstance().getModuleManager().get(PVPSafe.class);
        if (pvpSafe != null && pvpSafe.isEnabled() && pvpSafe.isPvpMode()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.literal("§c[PVP Safe] §fНельзя выйти из игры во время боя!"), false);
            }
            ci.cancel();
        }
    }
}
