package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.WorldVisuals;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import farvix.solution.api.events.impl.game.EventWorldLoad;
import farvix.solution.api.interfaces.QuickImports;
import net.minecraft.client.world.ClientWorld;

// Два отдельных миксина: один на ClientWorld (для init), другой на World (для погоды)
@Mixin(ClientWorld.class)
public class ClientWorldMixin implements QuickImports {

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initHook(CallbackInfo info) {
        new EventWorldLoad().call();
    }
}
