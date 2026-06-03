package farvix.solution.api.events.impl.game;

import lombok.Getter;
import lombok.Setter;
import farvix.solution.api.events.Event;

@Getter
public class EventMotion extends Event {
	public static float lastYaw, lastPitch;
	
	private float yaw, pitch;
	@Setter
	private boolean ground;
	
	public EventMotion(float yaw, float pitch, boolean ground) {
		this.yaw = yaw;
		this.pitch = pitch;
		this.ground = ground;
	}
	
	public void setYaw(float yaw) {
		this.yaw = yaw;
		lastYaw = yaw;
	}
	
	public void setPitch(float pitch) {
		this.pitch = pitch;
		lastPitch = pitch;
	}
}
