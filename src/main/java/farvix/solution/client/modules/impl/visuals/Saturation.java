package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

@ModuleInfo(name = "Saturation", category = ModuleCategory.VISUALS,
        description = "Настройка насыщенности цветов")
public class Saturation extends Module {

    /** 1.0 = ванильная насыщенность, 0 = ч/б, 2 = усиленная */
    public final SliderSetting amount = new SliderSetting("saturation.amount", this,
            1.0f, 0.0f, 2.0f, 0.05f);

    @Override
    public void onDisable() {
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
