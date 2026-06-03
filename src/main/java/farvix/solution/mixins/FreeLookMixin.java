package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.FreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class FreeLookMixin {

    @Shadow private MinecraftClient client;
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouse(CallbackInfo ci) {
        FreeLook module = Client.getInstance().getModuleManager().get(FreeLook.class);
        if (module == null || !module.isEnabled()) return;

        boolean held = FreeLook.isKeyHeld(module);

        if (held) {
            if (!FreeLook.active) {
                // Только что нажали — сохраняем взгляд и перспективу, переключаем на 3-е лицо
                FreeLook.active = true;
                if (client.player != null) {
                    FreeLook.savedYaw   = client.player.getYaw();
                    FreeLook.savedPitch = client.player.getPitch();
                    FreeLook.cameraYaw   = client.player.getYaw();
                    FreeLook.cameraPitch = client.player.getPitch();
                }
                // Сохраняем текущую перспективу и переключаем на вид сзади
                FreeLook.savedPerspective = client.options.getPerspective();
                client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                client.gameRenderer.onCameraEntitySet(client.getCameraEntity());
            }

            // Чувствительность мыши из настроек Minecraft, умноженная на скорость вращения
            double sensitivity = client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
            double speedMult = module.rotationSpeed.getValue() / 5.0; // 5 = дефолт (нейтральный)
            double scale = sensitivity * sensitivity * sensitivity * 8.0 * speedMult;

            float deltaX = (float)(cursorDeltaX * scale);
            float deltaY = (float)(cursorDeltaY * scale);

            if (module.invertX.isEnabled()) deltaX = -deltaX;
            if (module.invertY.isEnabled()) deltaY = -deltaY;

            FreeLook.cameraYaw   += deltaX;
            FreeLook.cameraPitch += deltaY;
            FreeLook.cameraPitch  = Math.max(-90f, Math.min(90f, FreeLook.cameraPitch));

            // Отменяем стандартную обработку — игрок не поворачивается
            ci.cancel();
        } else {
            if (FreeLook.active) {
                // Отпустили — восстанавливаем перспективу и взгляд игрока
                FreeLook.active = false;
                client.options.setPerspective(FreeLook.savedPerspective);
                client.gameRenderer.onCameraEntitySet(client.getCameraEntity());
                if (client.player != null) {
                    client.player.setYaw(FreeLook.savedYaw);
                    client.player.setPitch(FreeLook.savedPitch);
                }
            }
        }
    }
}
