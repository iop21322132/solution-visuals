package farvix.solution.api.util.math;

public class TimerUtils {
    private long startTime = System.currentTimeMillis();

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public boolean passed(long time) {
        return System.currentTimeMillis() - startTime > time;
    }

    public long getElapsed() {
        return System.currentTimeMillis() - startTime;
    }
}
