package farvix.solution.mixins;

import farvix.solution.api.events.impl.entity.EventAttackEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class AttackEntityMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if ((Object) this != mc.player) return;
        EventAttackEntity event = new EventAttackEntity(target);
        event.call();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
