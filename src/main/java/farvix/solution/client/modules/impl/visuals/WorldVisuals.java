package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.events.impl.game.EventFog;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.MultiModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;

@ModuleInfo(name = "World Visuals", category = ModuleCategory.VISUALS, description = "Яркость, время и туман")
public class WorldVisuals extends Module {
    public final MultiModeSetting modes = new MultiModeSetting("worldvisuals.modes", this, "worldvisuals.modes.brightness",
            "worldvisuals.modes.time", "worldvisuals.modes.fog");

    public final SliderSetting bright = new SliderSetting("worldvisuals.modes.brightness",
            this,100.0F, 1, 100.0F,1).setVisible(() -> modes.get(0).isEnabled());
    public final SliderSetting time = new SliderSetting("worldvisuals.modes.time",
            this,12000, 0, 24000,1).setVisible(() -> modes.get(1).isEnabled());
    public final SliderSetting fogDst = new SliderSetting("worldvisuals.modes.fog",
            this, 128,3,256,1).setVisible(() -> modes.get(2).isEnabled());

    public final ColorSetting fogColor = new ColorSetting("Цвет тумана", this,
            new FixColor(200, 220, 255, 255).getRGB());

    {
        fogColor.setVisible(() -> modes.get(2).isEnabled());
    }

    // ── Погода ────────────────────────────────────────────────────────────────
    public final ModeSetting weather = new ModeSetting("worldvisuals.weather", this,
            "worldvisuals.weather.default",
            "worldvisuals.weather.clear",
            "worldvisuals.weather.rain",
            "worldvisuals.weather.thunder");

    /** Клиентски идёт дождь? */
    public boolean isClientRaining() {
        if (!isEnabled()) return false;
        String w = weather.getCurrentMode();
        return w.equals("worldvisuals.weather.rain") || w.equals("worldvisuals.weather.thunder");
    }

    /** Клиентски идёт гроза? */
    public boolean isClientThundering() {
        if (!isEnabled()) return false;
        return weather.getCurrentMode().equals("worldvisuals.weather.thunder");
    }

    /** Погода переопределена (не дефолт)? */
    public boolean isWeatherOverridden() {
        if (!isEnabled()) return false;
        return !weather.getCurrentMode().equals("worldvisuals.weather.default");
    }

    @EventHandler
    public void onFog(EventFog e) {
        if (modes.get(2).isEnabled()) {
            e.setDistance(fogDst.getValue());
            e.setColor(fogColor.get());
            e.setCancelled(true);
        }
    }
}
