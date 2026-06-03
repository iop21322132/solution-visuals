package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.environment.HitSound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public class PlayerAttackSoundMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance sound, CallbackInfo ci) {
        if (sound != null && sound.getId() != null) {
            String path = sound.getId().getPath();
            if ("entity.player.attack.crit".equals(path)) {
                try {
                    HitSound hitSound = Client.getInstance().getModuleManager().get(HitSound.class);
                    if (hitSound != null && hitSound.isEnabled()) {
                        ci.cancel();
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}

