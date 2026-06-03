package farvix.solution.api.util.game;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimerUtility {
    private long startTime = System.currentTimeMillis();

    private long millis;

    public TimerUtility() {
        reset();
    }

    public static TimerUtility create() {
        return new TimerUtility();
    }

    public boolean finished(long delay) {
        return System.currentTimeMillis() - delay >= millis;
    }

    public void reset() {
        this.millis = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.millis;
    }

    public boolean every(long ms) {
        boolean passed = getMillis(System.nanoTime() - millis) >= ms;
        if (passed)
            reset();
        return passed;
    }
    public boolean passed(long time) {
        return System.currentTimeMillis() - startTime > time;
    }
    public long getMillis(long time) {
        return time / 1000000L;
    }
}
