package dev.padiloi1337.hitcolor.animations;

public class LinearAnimation extends AbstractAnimation {
	
	public LinearAnimation(double def) {
		this.now = def;
	}
	
	public LinearAnimation() {
		this(0);
	}
	
	@Override
	public void begin(double start, double end, double speed, Runnable todo) {
		super.begin(start, end, speed, todo);
	}
	
	@Override
	public void begin(double start, double end, double speed) {
		super.begin(start, end, speed);
	}

	@Override
	public double getAndUpdate() {
		if(animating) {
			now = start + diff * getTimeFraction();
			check();
		}
		return now;
	}
	
}
