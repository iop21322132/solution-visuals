package farvix.solution.api.events.impl.input;

import lombok.Getter;
import farvix.solution.api.events.Event;

@Getter
public class EventMouseDrag extends Event {
    private final double mouseX, mouseY;

    public EventMouseDrag(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
