package farvix.solution.client.modules.impl.environment;

import farvix.solution.api.events.impl.game.EventUpdate;
import meteordevelopment.orbit.EventHandler;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

@ModuleInfo(name = "Item Scroller", category = ModuleCategory.ENVIRONMENT,
        description = "Shift+ЛКМ — быстрое перемещение предметов")
public class ItemScroller extends Module {

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (!ClickUI.isToolrise()) {
            this.setEnabled(false);
        }
    }
}
