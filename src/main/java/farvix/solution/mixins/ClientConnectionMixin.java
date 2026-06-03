package farvix.solution.mixins;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import farvix.solution.api.events.impl.game.EventReceivePacket;
import farvix.solution.api.interfaces.QuickImports;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin implements QuickImports {

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo info) {
        if (new EventReceivePacket(packet).call().isCancelled()) info.cancel();
    }

}
