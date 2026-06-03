package farvix.solution.api.events.impl.entity;

import farvix.solution.api.events.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;

@Getter
public class EventAttackEntity extends Event {
    private final Entity target;
    @Setter
    private boolean cancelled = false;

    public EventAttackEntity(Entity target) {
        this.target = target;
    }
}
