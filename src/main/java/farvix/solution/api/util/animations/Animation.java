package farvix.solution.api.util.animations;

import farvix.solution.api.util.math.TimerUtils;

public class Animation {
    private final TimerUtils timer = new TimerUtils();
    private long duration;
    private final Easing easing;
    private boolean forward;
    private double value;

    public Animation(long duration, double value, boolean forward, Easing easing) {
        this.duration = duration;
        this.value = value;
        this.forward = forward;
        this.easing = easing;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void update(boolean forward) {
        if (this.forward != forward) {
            this.forward = forward;
            timer.setStartTime((long) (System.currentTimeMillis() - (value - Math.min(value, timer.getElapsed()))));
        }
    }

    public boolean finished(boolean forward) {
        return timer.passed(duration) && (forward == this.forward);
    }

    public boolean finished() {
        return timer.passed(duration) && this.forward;
    }

    public float getValue() {
        if (forward) {
            if (timer.passed(duration)) return (float) value;
            return (float) (easing.apply(timer.getElapsed() / (double) duration) * value);
        } else {
            if (timer.passed(duration)) return 0.0f;
            return (float) ((1 - easing.apply(timer.getElapsed() / (double) duration)) * value);
        }
    }

    public float getLinear() {
        if (forward) {
            if (timer.passed(duration)) return (float) value;
            return (float) (timer.getElapsed() / (double) duration * value);
        } else {
            if (timer.passed(duration)) return 0.0f;
            return (float) ((1 - timer.getElapsed() / (double) duration) * value);
        }
    }

    public float getReversedValue() {
        return 1 - getValue();
    }

    public void reset() {
        timer.reset();
    }

    public void update() {
        if (finished()) update(false);
        else if (finished(false)) update(true);
    }
}
