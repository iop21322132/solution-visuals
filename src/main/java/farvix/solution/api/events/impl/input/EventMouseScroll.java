package farvix.solution.api.events.impl.input;

import lombok.Getter;
import lombok.Setter;
import farvix.solution.api.events.Event;

@Getter
@Setter
public class EventMouseScroll extends Event {
    private final double horizontal;
    private final double vertical;
    private boolean cancelled = false;

    public EventMouseScroll(double horizontal, double vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }
}
