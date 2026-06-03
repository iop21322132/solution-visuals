package farvix.solution.api.events.impl.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import farvix.solution.api.events.Event;

@Getter @Setter
@AllArgsConstructor
public class EventRotationVector extends Event {
	private float yaw, pitch;
}
