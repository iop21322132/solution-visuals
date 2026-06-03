package farvix.solution.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import farvix.solution.Client;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.util.render.ProjectionUtility;
import farvix.solution.api.util.render.SaturationPass;
import farvix.solution.client.modules.impl.visuals.AspectRatio;
import farvix.solution.client.modules.impl.visuals.NoRender;
import farvix.solution.client.modules.impl.visuals.Saturation;
import farvix.solution.client.modules.impl.visuals.Zoom;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Final
    @Shadow private MinecraftClient client;

    @Shadow
    private float zoom;

    @Shadow
    private float zoomX;

    @Shadow
    private float zoomY;

    @Shadow
    private float viewDistance;

    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {

        // ── Zoom ──────────────────────────────────────────────────────────────
        Zoom zoomModule = Client.getInstance().getModuleManager().get(Zoom.class);
        boolean zoomHeld = Zoom.isZoomKeyHeld(zoomModule);

        // Обновляем цель зума
        if (zoomHeld) {
            Zoom.targetFovMultiplier = 1.0f / Zoom.zoomLevel;
        } else {
            // Отпустили — сбрасываем уровень зума к дефолту и возвращаем FOV
            if (Zoom.targetFovMultiplier < 1.0f) {
                Zoom.resetZoomLevel();
            }
            Zoom.targetFovMultiplier = 1.0f;
        }
        // Тикаем анимацию (FPS-независимо)
        Zoom.tick();

        float effectiveFov = fovDegrees * Zoom.currentFovMultiplier;

        // ── AspectRatio ───────────────────────────────────────────────────────
        AspectRatio aspectRatio = Client.getInstance().getModuleManager().get(AspectRatio.class);

        // Если ни зум ни aspect ratio не активны — выходим
        if (!zoomHeld && Zoom.currentFovMultiplier > 0.999f
                && (aspectRatio == null || !aspectRatio.isEnabled())) {
            return;
        }

        Matrix4f projectionMatrix = new Matrix4f().setPerspective(
                effectiveFov * (float) Math.PI / 180.0f,
                aspectRatio != null && aspectRatio.isEnabled()
                        ? aspectRatio.getSliderSetting().getValue()
                        : (float) client.getWindow().getFramebufferWidth()
                          / client.getWindow().getFramebufferHeight(),
                0.05f,
                viewDistance * 4.0f
        );

        if (zoom != 1.0f) {
            Matrix4f zoomMatrix = new Matrix4f();
            zoomMatrix.translate(zoomX, -zoomY, 0.0f);
            zoomMatrix.scale(this.zoom, this.zoom, 1.0f);
            zoomMatrix.mul(projectionMatrix);
            cir.setReturnValue(zoomMatrix);
        } else {
            cir.setReturnValue(projectionMatrix);
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(matrix4f);
        matrixStack.translate(client.getEntityRenderDispatcher().camera.getPos().negate());

        ProjectionUtility.lastProjMat = RenderSystem.getProjectionMatrix();
        ProjectionUtility.lastWorldSpaceMatrix = matrixStack.peek();

        new EventRender3D.Game(tickCounter, matrixStack).call();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
            shift = At.Shift.AFTER))
    private void applySaturationAfterWorld(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        Saturation saturation = Client.getInstance().getModuleManager().get(Saturation.class);
        if (saturation != null && saturation.isEnabled()) {
            SaturationPass.apply(saturation.amount.getValue());
        }
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    public void noHurtCamHook(CallbackInfo ci) {
        NoRender noRender = Client.getInstance().getModuleManager().get(NoRender.class);
        if (noRender != null && noRender.isEnabled() && noRender.noHurtCam.isEnabled()) {
            ci.cancel();
        }
    }
}
