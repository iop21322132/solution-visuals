package farvix.solution.api.events.impl.game;

import lombok.Getter;
import lombok.Setter;
import farvix.solution.api.events.Event;

@Getter @Setter
public class EventFog extends Event {
    float distance;
    int color;
}
