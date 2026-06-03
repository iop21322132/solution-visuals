package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.NoRender;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void noFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        NoRender noRender = Client.getInstance().getModuleManager().get(NoRender.class);
        if (noRender != null && noRender.isEnabled() && noRender.noFire.isEnabled()) {
            ci.cancel();
        }
    }
}
