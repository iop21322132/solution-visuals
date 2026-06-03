package dev.padiloi1337.hitcolor.animations;

public class EaseAnimation extends AbstractAnimation {
	
	private Type type;
	
	public enum Type {
		OUT,
		IN;
	}
	
	public EaseAnimation(Type type, double def) {
		this.type = type;
		this.now = def;
	}
	
	public EaseAnimation(Type type) {
		this(type, 0);
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
			now = start + diff * (type == Type.OUT ? easeOutValue(getTimeFraction())
					: easeInValue(getTimeFraction()));
			check();
		}
		return now;
	}
	
	private static double easeOutValue(double f) {
		return Math.pow(1-f, 2)*3*f + (1-f)*3*Math.pow(f, 2)*0.95f + Math.pow(f, 3);
	}
	
	private static double easeInValue(double f) {
		return Math.pow(1-f, 2)*3*f*0.02 + (1-f)*3*Math.pow(f, 2) + Math.pow(f, 3);
	}
	
}
