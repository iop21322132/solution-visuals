package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.CustomItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    /**
     * Stores the item being rendered so we can check it in the render inject.
     * ThreadLocal is safe for concurrent rendering.
     */
    @Unique
    private static final ThreadLocal<Item> CURRENT_ITEM = new ThreadLocal<>();

    /**
     * Capture the item from the entity before render state is built.
     * updateRenderState is called right before render() with the live entity.
     */
    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",
        at = @At("HEAD")
    )
    private void captureItem(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        CURRENT_ITEM.set(entity.getStack().getItem());
    }

    /**
     * Apply scale before the item is rendered if it's in the selected list.
     */
    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void onRenderHead(ItemEntityRenderState state, MatrixStack matrices,
                              VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        CustomItem module = Client.getInstance().getModuleManager().get(CustomItem.class);
        if (module == null || !module.isEnabled()) return;

        Item item = CURRENT_ITEM.get();
        if (item == null || !module.getItemList().contains(item)) return;

        float s = module.getScale().getValue();
        matrices.scale(s, s, s);
    }
}
