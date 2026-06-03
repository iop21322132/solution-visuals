package farvix.solution.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.WorldVisuals;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {

    @ModifyExpressionValue(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object injectXRayFullBright(Object original) {
        WorldVisuals visuals = Client.getInstance().getModuleManager().get(WorldVisuals.class);
        if (visuals.isEnabled() && visuals.modes.get(0).isEnabled()) {
            return Math.max((double) original, visuals.bright.getValue() / 10);
        }
        return original;
    }
}
