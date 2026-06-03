package farvix.solution.api.logger.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.LivingEntity;
import farvix.solution.api.events.Event;

@Getter @Setter
@AllArgsConstructor
public class EventDeath extends Event {
    LivingEntity entity;
}
