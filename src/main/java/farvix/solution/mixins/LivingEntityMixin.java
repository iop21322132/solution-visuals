package farvix.solution.mixins;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import farvix.solution.api.events.impl.entity.EventEntityDeath;
import farvix.solution.api.events.impl.game.EventJump;
import farvix.solution.api.events.impl.game.EventRotationVector;
import farvix.solution.api.interfaces.QuickImports;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements QuickImports {

    @Shadow protected abstract float getJumpVelocity();

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onEntityDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        new EventEntityDeath(self, source).call();
    }

    @Redirect(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float modifySlidingPitch(LivingEntity instance) {
        if (instance == mc.player) {
            EventRotationVector event = new EventRotationVector(instance.getYaw(), instance.getPitch()).call();
            return event.getPitch();
        }
        return instance.getPitch();
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void onJump(CallbackInfo ci) {
        if ((Object) this == mc.player) {

            float f = this.getJumpVelocity();
            float rotationYaw = mc.player.getYaw();

            EventJump event = new EventJump(f, rotationYaw).call();
            f = event.getMotion();
            rotationYaw = event.getYaw();

            if (event.isCancelled()) {
                ci.cancel();
                return;
            }

            if (!(f <= 1.0E-5F)) {
                Vec3d vec3d = mc.player.getVelocity();
                mc.player.setVelocity(vec3d.x, Math.max((double)f, vec3d.y), vec3d.z);
                if (mc.player.isSprinting()) {
                    float g = rotationYaw * ((float)Math.PI / 180F);
                    mc.player.addVelocityInternal(new Vec3d((double)(-MathHelper.sin(g)) * 0.2, (double)0.0F, (double)MathHelper.cos(g) * 0.2));
                }

                mc.player.velocityDirty = true;
            }
            ci.cancel();
        }
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void onGetHandSwingDuration(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == mc.player) {
            farvix.solution.client.modules.impl.visuals.HoldMyItems holdMyItems = farvix.solution.Client.getInstance().getModuleManager().get(farvix.solution.client.modules.impl.visuals.HoldMyItems.class);
            if (holdMyItems != null && holdMyItems.isEnabled()) {
                cir.setReturnValue((int) holdMyItems.swingSpeed.getValue());
            }
        }
    }
}
