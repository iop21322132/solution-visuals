package farvix.solution.client.modules.impl.visuals;

import lombok.Getter;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

@ModuleInfo(name = "Aspect Ratio", category = ModuleCategory.VISUALS, description = "Изменяет соотношение сторон экрана")
public class AspectRatio extends Module {
    @Getter
    private final SliderSetting sliderSetting = new SliderSetting("aspectratio.value", this, 1.4F, 0.5F, 2, 0.01F);
}
