package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.FreeLook;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdateHead(BlockView area, Entity focusedEntity,
                              boolean thirdPerson, boolean inverseView,
                              float tickDelta, CallbackInfo ci) {
        FreeLook module = Client.getInstance().getModuleManager().get(FreeLook.class);
        if (module == null || !module.isEnabled() || !FreeLook.active) return;
        if (focusedEntity == null) return;

        FreeLook.realYaw   = focusedEntity.getYaw();
        FreeLook.realPitch = focusedEntity.getPitch();
        focusedEntity.setYaw(FreeLook.cameraYaw);
        focusedEntity.setPitch(FreeLook.cameraPitch);
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdateTail(BlockView area, Entity focusedEntity,
                              boolean thirdPerson, boolean inverseView,
                              float tickDelta, CallbackInfo ci) {
        FreeLook module = Client.getInstance().getModuleManager().get(FreeLook.class);
        if (module == null || !module.isEnabled() || !FreeLook.active) return;
        if (focusedEntity == null) return;

        // Возвращаем реальный yaw/pitch игроку
        focusedEntity.setYaw(FreeLook.realYaw);
        focusedEntity.setPitch(FreeLook.realPitch);
    }

    /**
     * Подменяем аргумент clipToSpace — именно он задаёт желаемую дистанцию камеры.
     * clipToSpace принимает желаемую дистанцию и возвращает реальную (с учётом стен).
     */
    @ModifyArg(
        method = "update",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"),
        index = 0
    )
    private float modifyClipToSpace(float original) {
        FreeLook module = Client.getInstance().getModuleManager().get(FreeLook.class);
        if (module == null || !module.isEnabled() || !FreeLook.active) return original;
        return module.distance.getValue();
    }
}
