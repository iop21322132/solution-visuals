package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.api.util.HitColorTintState;
import farvix.solution.client.modules.impl.visuals.HitColor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<S>> {

    @org.spongepowered.asm.mixin.Shadow
    protected M model;

    private static final ThreadLocal<Boolean> SHOULD_TINT = HitColorTintState.SHOULD_TINT;
    private static final Identifier WHITE_TEX =
            Identifier.of("solution", "textures/white.png");

    /**
     * Перехватываем updateRenderState чтобы установить флаг тинта
     * ДО того как state.hurt будет использован в getMixColor
     */
    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
        at = @At("RETURN")
    )
    private void solution$onUpdateRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        HitColor module = Client.getInstance().getModuleManager().get(HitColor.class);
        if (module != null && module.isEnabled() && state.hurt) {
            SHOULD_TINT.set(true);
            state.hurt = false; // отключаем ванильный красный
        } else {
            SHOULD_TINT.set(false);
        }

        farvix.solution.client.modules.impl.visuals.HoldMyItems holds = Client.getInstance().getModuleManager().get(farvix.solution.client.modules.impl.visuals.HoldMyItems.class);
        if (holds != null && holds.isEnabled()) {
            if (entity instanceof net.minecraft.entity.player.PlayerEntity) {
                net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) entity;
                boolean shouldSwim = holds.swimmingAnimation.getValue() && (player.isSwimming() || player.isSubmergedInWater());
                boolean shouldCrawl = holds.climbAndCrawl.getValue() && (player.isCrawling() || player.isClimbing());
                if (shouldSwim || shouldCrawl) {
                    state.pose = net.minecraft.entity.EntityPose.SWIMMING;
                }
            }
        }
    }

    /** Сбрасываем флаг после рендера */
    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("RETURN")
    )
    private void solution$clearTint(S state, MatrixStack matrices,
                                     VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        SHOULD_TINT.set(false);
    }

    /**
     * @author Solution
     * @reason Custom hit color
     */
    @Overwrite
    public int getMixColor(S state) {
        HitColor module = Client.getInstance().getModuleManager().get(HitColor.class);
        if (module != null && module.isEnabled() && Boolean.TRUE.equals(SHOULD_TINT.get())) {
            int c = module.getMixColor();
            // Minecraft использует -1 (0xFFFFFFFF) как "нет тинта"
            // Если цвет белый — возвращаем 0xFFFFFFFE чтобы тинт применился
            return (c == -1) ? 0xFFFFFFFE : c;
        }
        return -1;
    }

    @Inject(
        method = "getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;",
        at = @At("HEAD"), cancellable = true
    )
    private void solution$forceTranslucentLayer(S state, boolean showBody,
                                                 boolean translucent, boolean showOutline,
                                                 CallbackInfoReturnable<RenderLayer> cir) {
        HitColor module = Client.getInstance().getModuleManager().get(HitColor.class);
        if (module != null && module.isEnabled() && Boolean.TRUE.equals(SHOULD_TINT.get())) {
            cir.setReturnValue(RenderLayer.getEntityTranslucent(WHITE_TEX));
        }
    }

}
