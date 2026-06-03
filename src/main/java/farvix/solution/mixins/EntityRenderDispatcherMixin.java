package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.CustomHitbox;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    // In 1.21.4 renderHitbox signature: (MatrixStack, VertexConsumer, Entity, float r, float g, float b, float a)
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void onRenderHitbox(MatrixStack matrices, VertexConsumer vertices,
                                       Entity entity,
                                       float r, float g, float b, float a,
                                       CallbackInfo ci) {
        CustomHitbox module = Client.getInstance().getModuleManager().get(CustomHitbox.class);
        if (module == null || !module.isEnabled()) return;

        // Cancel vanilla hitbox rendering — our module renders via EventRender3D
        ci.cancel();
    }
}
