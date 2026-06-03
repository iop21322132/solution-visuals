package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.WorldVisuals;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldWeatherMixin {

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void getRainGradientHook(float delta, CallbackInfoReturnable<Float> cir) {
        WorldVisuals visuals = Client.getInstance().getModuleManager().get(WorldVisuals.class);
        if (visuals == null || !visuals.isWeatherOverridden()) return;
        cir.setReturnValue(visuals.isClientRaining() ? 1.0f : 0.0f);
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void getThunderGradientHook(float delta, CallbackInfoReturnable<Float> cir) {
        WorldVisuals visuals = Client.getInstance().getModuleManager().get(WorldVisuals.class);
        if (visuals == null || !visuals.isWeatherOverridden()) return;
        cir.setReturnValue(visuals.isClientThundering() ? 1.0f : 0.0f);
    }
}
