package farvix.solution.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import farvix.solution.Client;
import farvix.solution.api.events.impl.game.EventRotationVector;
import farvix.solution.api.events.impl.game.EventTrace;
import farvix.solution.api.interfaces.QuickImports;

@Mixin(value = Entity.class)
public abstract class EntityMixin implements QuickImports {

    @Shadow public abstract ActionResult interact(PlayerEntity player, Hand hand);

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    private boolean fixFallDistanceCalculation(boolean original) {
        if ((Object) this == mc.player) {
            return false;
        }

        return original;
    }

    @ModifyArgs(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    private void modifyRotationVectordArgs(Args args) {
        if ((Object) this == mc.player) {
            EventRotationVector event = new EventRotationVector(mc.player.getYaw(), mc.player.getPitch()).call();

            float customYaw = event.getYaw();
            args.set(2, customYaw);
        }
    }

    @ModifyArgs(method = "getRotationVector()Lnet/minecraft/util/math/Vec3d;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"))
    private void modifyRotationVectorArgs(Args args) {
        if ((Object) this == mc.player) {
            EventRotationVector event = new EventRotationVector(mc.player.getYaw(), mc.player.getPitch()).call();

            float customPitch = event.getPitch();
            float customYaw = event.getYaw();

            args.set(0, customPitch);
            args.set(1, customYaw);
        }
    }

    @ModifyArgs(method = "getRotationVec", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"))
    private void modifyRotationLookArgs(Args args) {
        if ((Object) this == mc.player) {
            EventTrace event = new EventTrace(mc.player.getYaw(), mc.player.getPitch()).call();
            if (event.isCancelled()) {
                float customPitch = event.getPitch();
                float customYaw = event.getYaw();

                args.set(0, customPitch);
                args.set(1, customYaw);
            }
        }
    }
}
