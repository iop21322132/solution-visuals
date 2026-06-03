package farvix.solution.client.modules.impl.visuals;

import lombok.Getter;
import farvix.solution.api.settings.impl.ItemListSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

/**
 * CustomItem — Visual module.
 *
 * Allows selecting specific Minecraft items that will be rendered
 * larger when lying on the ground. Scale 1.0 = normal, 5.0 = 5× bigger.
 */
@Getter
@ModuleInfo(name = "Custom Item", category = ModuleCategory.VISUALS, description = "Увеличивает выбранные предметы на земле")
public class CustomItem extends Module {

    /** Scale multiplier: 1.0 = normal size, 5.0 = five times bigger */
    public final SliderSetting scale = new SliderSetting(
            "Размер", this, 2.0f, 1.0f, 5.0f, 0.1f
    );

    /** The set of items that should be scaled up */
    public final ItemListSetting itemList = new ItemListSetting(
            "customitem.items", this
    );
}
