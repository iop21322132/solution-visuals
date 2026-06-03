package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.NoRender;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    // Сигнатура в 1.21.4: (Camera, float, VertexConsumerProvider.Immediate)
    @Inject(
        method = "renderParticles(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
        at = @At("HEAD"), cancellable = true, require = 0
    )
    public void noParticlesHook(Camera camera, float tickDelta,
                                 VertexConsumerProvider.Immediate vertexConsumers,
                                 CallbackInfo ci) {
        NoRender m = Client.getInstance().getModuleManager().get(NoRender.class);
        if (m != null && m.isEnabled() && m.noParticles.isEnabled()) ci.cancel();
    }
}
