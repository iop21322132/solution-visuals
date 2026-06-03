package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.api.util.HitColorTintState;
import farvix.solution.client.modules.impl.visuals.HitColor;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRendererMixin {

    @ModifyVariable(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 2,
        require = 0
    )
    private VertexConsumerProvider solution$wrapVcp(VertexConsumerProvider vcp) {
        HitColor module = Client.getInstance().getModuleManager().get(HitColor.class);
        if (module == null || !module.isEnabled() || !module.tintArmor.isEnabled()) return vcp;
        if (!Boolean.TRUE.equals(HitColorTintState.SHOULD_TINT.get())) return vcp;

        int argb = module.getMixColor();
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ((argb      ) & 0xFF) / 255f;
        final float fr = r, fg = g, fb = b;
        return layer -> new TintedVertexConsumer(vcp.getBuffer(layer), fr, fg, fb);
    }

    private static class TintedVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float tr, tg, tb;

        TintedVertexConsumer(VertexConsumer delegate, float r, float g, float b) {
            this.delegate = delegate;
            this.tr = r; this.tg = g; this.tb = b;
        }

        @Override public VertexConsumer vertex(float x, float y, float z) {
            delegate.vertex(x, y, z); return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            int nr = (int)(r + (tr * 255f - r) * 0.9f);
            int ng = (int)(g + (tg * 255f - g) * 0.9f);
            int nb = (int)(b + (tb * 255f - b) * 0.9f);
            delegate.color(Math.max(0, Math.min(255, nr)), Math.max(0, Math.min(255, ng)), Math.max(0, Math.min(255, nb)), a);
            return this;
        }
        @Override public VertexConsumer texture(float u, float v) { delegate.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { delegate.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { delegate.light(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
    }
}
