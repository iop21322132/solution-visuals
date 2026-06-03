package farvix.solution.api.events.impl.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import farvix.solution.api.events.Event;

@Getter @AllArgsConstructor
public class EventMessage extends Event {
    final String message;
}
