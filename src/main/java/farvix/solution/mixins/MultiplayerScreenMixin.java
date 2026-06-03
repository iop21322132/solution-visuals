package farvix.solution.mixins;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin {
    @Shadow
    private MultiplayerServerListWidget serverListWidget;
    @Shadow
    private ButtonWidget buttonDelete;
    @Shadow
    private ButtonWidget buttonEdit;

    @Inject(method = "updateButtonActivationStates", at = @At("TAIL"))
    private void onUpdateButtons(CallbackInfo ci) {
        if (this.serverListWidget == null) return;
        
        MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
        
        if (entry instanceof MultiplayerServerListWidget.ServerEntry serverEntry) {
            ServerInfo info = serverEntry.getServer();
            
            // Если выбран наш сервер — блокируем кнопки удаления и редактирования
            if (info.address != null && info.address.toLowerCase().contains("mc.toolrise.space")) {
                if (buttonDelete != null) buttonDelete.active = false;
                if (buttonEdit != null) buttonEdit.active = false;
            }
        }
    }
}