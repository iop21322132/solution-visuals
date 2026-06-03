package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.api.util.FriendManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import farvix.solution.api.events.impl.game.EventClickSlot;
import farvix.solution.api.interfaces.QuickImports;

@Mixin(value = ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin implements QuickImports {

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    public void clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (mc.player == null || mc.world == null) return;
        if (new EventClickSlot(actionType, slotId, button, syncId).call().isCancelled()) ci.cancel();
    }

    /**
     * Блокируем отправку пакета атаки на сервер если цель — друг
     */
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target instanceof farvix.solution.client.modules.impl.environment.FakePlayer.ClientFakePlayer fakePlayer) {
            player.swingHand(Hand.MAIN_HAND);
            new farvix.solution.api.events.impl.entity.EventAttackEntity(fakePlayer).call();
            fakePlayer.damage(player.getDamageSources().playerAttack(player), 1.0f);
            player.resetLastAttackedTicks();
            ci.cancel();
            return;
        }

        try {
            farvix.solution.client.modules.impl.environment.NoFriendDamage module =
                Client.getInstance().getModuleManager().get(
                    farvix.solution.client.modules.impl.environment.NoFriendDamage.class);

            if (module == null || !module.isEnabled()) return;
            if (!(target instanceof PlayerEntity targetPlayer)) return;

            String name = targetPlayer.getName().getString();
            if (FriendManager.isFriend(name)) {
                ci.cancel(); // блокируем пакет на сервер
            }
        } catch (Exception e) {
            // Игнорируем
        }
    }
}
