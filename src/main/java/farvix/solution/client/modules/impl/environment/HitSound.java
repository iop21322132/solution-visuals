package farvix.solution.client.modules.impl.environment;

import farvix.solution.api.events.impl.entity.EventAttackEntity;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

@ModuleInfo(name = "Hit Sound", category = ModuleCategory.ENVIRONMENT, description = "Воспроизводит звук при ударе по цели")
public class HitSound extends Module implements QuickImports {

    public final ModeSetting sound = new ModeSetting(
            "Звуки", this, "Глухо", "Колокольчик", "Дубинка");

    public final ModeSetting target = new ModeSetting(
            "Цели", this, "Все", "Игроки", "Мобы");

    public final SliderSetting volume = new SliderSetting(
            "Громкость", this, 1.0f, 0.1f, 2.0f, 0.1f);

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (!(e.getTarget() instanceof LivingEntity living)) return;

        // Только крит: игрок в воздухе и падает
        if (mc.player == null) return;
        if (mc.player.fallDistance <= 0 || mc.player.isOnGround()) return;

        // Фильтр по типу цели
        String targetMode = target.getCurrentMode();
        if (targetMode.equals("Игроки") && !(living instanceof PlayerEntity)) return;
        if (targetMode.equals("Мобы") && !(living instanceof MobEntity)) return;

        // Воспроизводим звук
        String soundMode = sound.getCurrentMode();
        String file = switch (soundMode) {
            case "Колокольчик" -> "bell.wav";
            case "Дубинка"     -> "bonk.wav";
            default            -> "crit.wav";
        };

        playWav(file, volume.getValue());
    }

    private void playWav(String fileName, float vol) {
        try {
            InputStream is = HitSound.class.getClassLoader()
                    .getResourceAsStream("assets/solution/" + fileName);
            if (is == null) {
                System.err.println("[HitSound] File not found: " + fileName);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(is));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);

            // Применяем громкость
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float)(20.0 * Math.log10(Math.max(0.0001f, vol)));
                dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                gain.setValue(dB);
            }

            clip.start();
            // Освобождаем ресурсы после воспроизведения
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
        } catch (Exception ex) {
            System.err.println("[HitSound] Error playing " + fileName + ": " + ex.getMessage());
        }
    }
}
