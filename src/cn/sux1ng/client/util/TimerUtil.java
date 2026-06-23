package cn.sux1ng.client.util;

public class TimerUtil {
    // 记录上一次重置的时间点
    private long lastMs = 0;

    /**
     * 检查是否过去了指定的时间
     * @param delay 需要等待的毫秒数 (比如 1000ms = 1秒)
     * @return 如果时间到了返回 true，否则返回 false
     */
    public boolean hasTimePassed(long delay) {
        return System.currentTimeMillis() - lastMs >= delay;
    }

    /**
     * 重置计时器 (通常在执行完一次操作后调用)
     */
    public void reset() {
        lastMs = System.currentTimeMillis();
    }

    /**
     * 获取当前系统时间 (辅助用，偶尔会用到)
     */
    public long getTime() {
        return System.currentTimeMillis() - lastMs;
    }

    // 有些时候为了兼容性，也会叫 hasReached，跟 hasTimePassed 是一样的
    public boolean hasReached(long delay) {
        return hasTimePassed(delay);
    }
}