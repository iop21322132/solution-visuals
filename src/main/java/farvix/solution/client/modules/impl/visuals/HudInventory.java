package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import lombok.Getter;

@Getter
@ModuleInfo(name = "Hud Inventory", category = ModuleCategory.VISUALS,
        description = "Кастомный инвентарь")
public class HudInventory extends Module {

    public final ModeSetting theme = new ModeSetting("Тема", this,
            "Dark",
            "Классический"
    );

    public final SliderSetting bgAlpha = new SliderSetting(
            "Прозрачность фона", this, 0.85f, 0f, 1f, 0.05f)
            .setVisible(() -> isDark());

    public boolean isDark() {
        return isEnabled() && theme.is("Dark");
    }
}
