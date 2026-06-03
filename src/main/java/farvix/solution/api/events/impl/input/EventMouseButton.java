package farvix.solution.api.events.impl.input;

import lombok.Getter;
import farvix.solution.api.events.Event;

@Getter
public class EventMouseButton extends Event {
    private final double mouseX, mouseY;
    private final int button, action;

    public EventMouseButton(double mouseX, double mouseY, int button, int action) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.button = button;
        this.action = action;
    }
}
