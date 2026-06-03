package dev.padiloi1337.hitcolor.animations;

import dev.padiloi1337.hitcolor.ui.GuiScreen;

public abstract class AbstractAnimation {
	
	protected boolean animating;
	protected double start, diff, now;
	protected int tick, maxTick;
	protected Runnable todo;
	
	protected void begin(double start, double end, double speed, Runnable todo) {
		this.animating = true;
		this.start = start;
		this.diff = end - start;
		this.now = start;
		this.maxTick = (int)(30.0f / (speed  * GuiScreen.getSpeed()));
		this.tick = 0;
		this.todo = todo;
	}
	
	protected void begin(double min, double max, double speed) {
		begin(min, max, speed, null);
	}
	
	public abstract double getAndUpdate();
	
	public double get() {
		return now;
	}
	
	public void setValue(double value) {
		this.now = value;
	}
	
	public double getTimeFraction() {
		tick = Math.min(tick + 1, maxTick);
		return (float)tick / maxTick;
	}
	
	protected void check() {
		if(tick == maxTick) {
			animating = false;
			if(todo != null)
				todo.run();
		}
	}
	
	public void stopAndSet(double v) {
		animating = false;
		now = v;
	}
	
	public void stop() {
		animating = false;
	}
	
	public boolean isAnimating() {
		return animating;
	}

}
