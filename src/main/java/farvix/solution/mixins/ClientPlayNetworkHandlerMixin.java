package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.api.events.impl.game.EventMessage;
import farvix.solution.client.modules.impl.environment.ClickUI;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        ClickUI.onServerJoin();
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        EventMessage event = new EventMessage(message).call();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
