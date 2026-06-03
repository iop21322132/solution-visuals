package farvix.solution.mixins;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import farvix.solution.api.events.impl.game.EventFog;
import farvix.solution.api.util.color.FixColor;

@Mixin(BackgroundRenderer.class)
public class BackGroundRendererMixin {

    @Inject(method = "getFogColor", at = @At(value = "HEAD"), cancellable = true)
    private static void getFogColorHook(Camera camera, float tickDelta, ClientWorld world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        EventFog event = new EventFog().call();
        if (event.isCancelled()) {
            int color = event.getColor();
            cir.setReturnValue(new Vector4f(FixColor.redf(color), FixColor.greenf(color), FixColor.bluef(color), FixColor.alphaf(color)));
        }
    }

    @Inject(method = "applyFog", at = @At(value = "HEAD"), cancellable = true)
    private static void modifyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        EventFog event = new EventFog().call();
        if (event.isCancelled()) {
            int color1 = event.getColor();
            cir.setReturnValue(new Fog(2.0F, event.getDistance(),  FogShape.CYLINDER, FixColor.redf(color1), FixColor.greenf(color1), FixColor.bluef(color1), FixColor.alphaf(color1)));
        }
    }
}
