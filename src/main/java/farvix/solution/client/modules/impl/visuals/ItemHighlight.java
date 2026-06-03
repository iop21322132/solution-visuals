package farvix.solution.client.modules.impl.visuals;

import lombok.Getter;
import net.minecraft.item.Item;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

import java.util.HashSet;
import java.util.Set;

@ModuleInfo(name = "Item Highlight", category = ModuleCategory.VISUALS, description = "Подсвечивает предметы в инвентаре")
public class ItemHighlight extends Module {

    @Getter
    public final ColorSetting color = new ColorSetting("Цвет", this,
            new FixColor(123, 47, 190, 160).getRGB());

    @Getter
    private final Set<Item> highlightedItems = new HashSet<>();

    public int getHighlightColor() {
        return color.get();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        highlightedItems.clear();
    }
}
