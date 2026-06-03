package farvix.solution.api.events.impl.entity;

import farvix.solution.api.events.Event;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.jetbrains.annotations.Nullable;

@Getter
public class EventEntityDeath extends Event {
    private final LivingEntity entity;
    private final DamageSource source;

    public EventEntityDeath(LivingEntity entity, DamageSource source) {
        this.entity = entity;
        this.source = source;
    }

    /** Returns the entity that killed this entity, or null if unknown. */
    @Nullable
    public LivingEntity getKillerEntity() {
        return entity.getPrimeAdversary();
    }
}
