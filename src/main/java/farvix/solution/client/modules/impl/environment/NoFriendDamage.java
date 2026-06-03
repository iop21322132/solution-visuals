package farvix.solution.client.modules.impl.environment;

import farvix.solution.api.events.impl.entity.EventAttackEntity;
import farvix.solution.api.events.impl.game.EventUpdate;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.util.FriendManager;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

@ModuleInfo(name = "No Friend Damage", category = ModuleCategory.ENVIRONMENT,
        description = "Блокирует нанесение урона друзьям")
public class NoFriendDamage extends Module implements QuickImports {

    @Override
    public void onEnable() {
        super.onEnable(); // подписываемся на события
    }

    @Override
    public void onDisable() {
        super.onDisable(); // отписываемся от событий
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (!ClickUI.isToolrise()) {
            this.setEnabled(false);
            return;
        }
    }

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (!ClickUI.isToolrise()) return;
        if (!(e.getTarget() instanceof PlayerEntity target)) return;

        String name = target.getName().getString();

        if (FriendManager.isFriend(name)) {
            e.setCancelled(true);
        }
    }
}
