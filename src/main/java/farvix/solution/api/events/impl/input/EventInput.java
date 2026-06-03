package farvix.solution.api.events.impl.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import farvix.solution.api.events.Event;

@Getter
@AllArgsConstructor
public class EventInput extends Event {
    int key, scancode;
    boolean released;

    public boolean isPressed(int key) {
        return !this.released && key == this.key && key != -1;
    }

    public boolean isReleased(int key) {
        return this.released && key == this.key && key != -1;
    }
}
