package farvix.solution.api.util.math;

public class CustRandom {
    private static final int N = 624;
    private static final int M = 397;
    private static final int MATRIX_A = 0x9908b0df;
    private static final int UPPER_MASK = 0x80000000;
    private static final int LOWER_MASK = 0x7fffffff;

    private int[] mt = new int[N];
    private int index = N + 1;
    private long lastGenerationTime = 0;
    private long delay = 0;
    private float lastGeneratedNumber = 0;

    public CustRandom(long seed) {
        init(seed);
    }

    public CustRandom() {
        this(System.currentTimeMillis());
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getDelay() {
        return delay;
    }

    private void init(long seed) {
        mt[0] = (int) (seed & 0xffffffffL);
        for (index = 1; index < N; index++) {
            mt[index] = (1812433253 * (mt[index - 1] ^ (mt[index - 1] >>> 30)) + index);
            mt[index] &= 0xffffffff;
        }
    }

    private int nextInt() {
        if (index >= N) {
            twist();
        }

        int y = mt[index++];
        y ^= (y >>> 11);
        y ^= (y << 7) & 0x9d2c5680;
        y ^= (y << 15) & 0xefc60000;
        y ^= (y >>> 18);

        return y;
    }

    public float nextFloat() {
        return (nextInt() & 0xffffffffL) / (float) (1L << 32);
    }

    public float randomNumber(float firstNumber, float secondNumber, boolean isInt) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGenerationTime < delay) {
            return lastGeneratedNumber;
        }

        if (firstNumber > secondNumber) {
            float temp = firstNumber;
            firstNumber = secondNumber;
            secondNumber = temp;
        }

        float result;
        do {
            float random = nextFloat();
            result = firstNumber + random * (secondNumber - firstNumber);

            if (isInt) {
                result = (int) result;
            }
        } while (result == lastGeneratedNumber); // повторенье мать у тебя сдохла

        lastGenerationTime = currentTime;
        lastGeneratedNumber = result;

        return result;
    }

    private void twist() {
        for (int i = 0; i < N; i++) {
            int x = (mt[i] & UPPER_MASK) + (mt[(i + 1) % N] & LOWER_MASK);
            int xA = x >>> 1;
            if ((x % 2) != 0) {
                xA ^= MATRIX_A;
            }
            mt[i] = mt[(i + M) % N] ^ xA;
        }
        index = 0;
    }
}
