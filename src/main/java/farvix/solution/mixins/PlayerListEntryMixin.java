package farvix.solution.mixins;

import farvix.solution.api.util.FriendManager;
import farvix.solution.api.util.FriendTextUtil;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void modifyFriendDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerListEntry entry = (PlayerListEntry)(Object) this;
        String name = entry.getProfile().getName();
        if (name == null || !FriendManager.isFriend(name)) return;
        Text original = cir.getReturnValue();
        if (original == null) return;
        cir.setReturnValue(FriendTextUtil.recolorName(original, name));
    }
}
