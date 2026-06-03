package farvix.solution.api.logger;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import farvix.solution.Client;
import farvix.solution.api.events.impl.game.EventUpdate;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.logger.events.EventDeath;

public class GameLogger implements QuickImports {

    @EventHandler
    public void onUpdate(EventUpdate eventUpdate) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                if (livingEntity.isDead() && livingEntity.deathTime < 1) {
                    new EventDeath(livingEntity).call();
                }
            }
        }
    }
}
