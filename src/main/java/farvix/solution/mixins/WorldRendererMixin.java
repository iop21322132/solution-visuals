package farvix.solution.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.util.world.WorldUtils;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        WorldUtils.lastWorld.set(positionMatrix);
        WorldUtils.lastProj.set(RenderSystem.getProjectionMatrix());
        WorldUtils.lastModelView.set(RenderSystem.getModelViewMatrix());
    }
}
