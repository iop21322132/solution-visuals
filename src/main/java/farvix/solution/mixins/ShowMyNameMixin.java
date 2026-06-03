package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.ShowMyName;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class ShowMyNameMixin<T extends Entity, S extends EntityRenderState> {

    @Shadow
    protected abstract void renderLabelIfPresent(S state, Text text, MatrixStack matrices,
                                                VertexConsumerProvider vertexConsumers, int light);

    private ThreadLocal<Entity> currentEntity = ThreadLocal.withInitial(() -> null);

    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
        at = @At("HEAD")
    )
    private void captureEntityForName(T entity, S state, float tickDelta, CallbackInfo ci) {
        currentEntity.set(entity);
    }

    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private void renderOwnPlayerName(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                    int light, CallbackInfo ci) {
        try {
            ShowMyName showMyName = Client.getInstance().getModuleManager().get(ShowMyName.class);
            if (showMyName == null || !showMyName.isEnabled()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            Entity entity = currentEntity.get();

            if (mc.player != null && entity instanceof ClientPlayerEntity &&
                entity.equals(mc.player) && !mc.options.getPerspective().isFirstPerson()) {

                Text displayName = mc.player.getDisplayName();
                if (displayName == null) displayName = mc.player.getName();
                renderLabelIfPresent(state, displayName, matrices, vertexConsumers, light);
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        } finally {
            currentEntity.remove();
        }
    }
}
