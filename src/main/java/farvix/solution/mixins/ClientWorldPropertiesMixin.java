package farvix.solution.mixins;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.WorldVisuals;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {

    @Shadow private long timeOfDay;

    // ── Время суток ───────────────────────────────────────────────────────────

    @Inject(method = "setTimeOfDay", at = @At("HEAD"), cancellable = true)
    public void setTimeOfDayHook(long timeOfDay, CallbackInfo ci) {
        WorldVisuals visuals = Client.getInstance().getModuleManager().get(WorldVisuals.class);
        if (visuals.isEnabled() && visuals.modes.get(1).isEnabled()) {
            this.timeOfDay = (long) (visuals.time.getValue());
            ci.cancel();
        }
    }

    // ── Погода: дождь ─────────────────────────────────────────────────────────

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    public void isRainingHook(CallbackInfoReturnable<Boolean> cir) {
        WorldVisuals visuals = Client.getInstance().getModuleManager().get(WorldVisuals.class);
        if (visuals != null && visuals.isWeatherOverridden()) {
            cir.setReturnValue(visuals.isClientRaining());
        }
    }

    // ── Погода: гроза ─────────────────────────────────────────────────────────

    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    public void isThunderingHook(CallbackInfoReturnable<Boolean> cir) {
        WorldVisuals visuals = Client.getInstance().getModuleManager().get(WorldVisuals.class);
        if (visuals != null && visuals.isWeatherOverridden()) {
            cir.setReturnValue(visuals.isClientThundering());
        }
    }
}
