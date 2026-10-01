package cn.sux1ng.client.input;

/** Consumes both axes together; focus epochs reject packets from an earlier capture. */
public final class MotionAccumulator {
    private long x, y, epoch;
    private boolean captured;

    public synchronized void setCaptured(boolean value) {
        if (captured == value) return;
        captured = value; epoch++; x = y = 0;
    }
    public synchronized long epoch() { return epoch; }
    public synchronized boolean isCaptured() { return captured; }
    public synchronized void add(long dx, long dy, long token) {
        if (captured && token == epoch) { x += dx; y += dy; }
    }
    public synchronized void clear() { epoch++; x = y = 0; }
    public synchronized int[] consume() {
        int[] result = {(int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, x)),
                (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, y))};
        x = y = 0; return result;
    }
}
