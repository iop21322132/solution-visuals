package farvix.solution.api.events.impl.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.screen.slot.SlotActionType;
import farvix.solution.api.events.Event;

@Getter
@Setter
@AllArgsConstructor
public class EventClickSlot extends Event {
    private final SlotActionType slotActionType;
    private final int slot, button, id;
}
