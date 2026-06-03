package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

@ModuleInfo(name = "Hit Color", category = ModuleCategory.PLAYER,
        description = "Цвет вспышки при уроне")
public class HitColor extends Module {

    public final ColorSetting color = new ColorSetting(
            "Цвет", this, new FixColor(255, 255, 255, 200).getRGB());

    public final BooleanSetting tintArmor = new BooleanSetting("С бронёй", this);

    public HitColor() {
        tintArmor.setEnabled(true);
    }

    public int getMixColor() {
        return color.get();
    }
}
