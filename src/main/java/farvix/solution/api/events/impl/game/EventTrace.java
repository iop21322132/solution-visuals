package farvix.solution.api.events.impl.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import farvix.solution.api.events.Event;

@Setter @Getter
@AllArgsConstructor
public class EventTrace extends Event {
	private float yaw, pitch;
}
