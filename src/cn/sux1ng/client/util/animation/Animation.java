package cn.sux1ng.client.util.animation;

/**
 * 缓动动画类
 * 替代项目中的手动 lerp()，提供带缓动函数的平滑过渡
 *
 * 用法:
 *   Animation anim = new Animation(Easing.EASE_OUT_EXPO, 300);
 *   anim.run(1.0);  // 从当前值动画到 1.0
 *   double val = anim.getValue();  // 每帧调用获取当前值
 */
public class Animation {
    private final Easing easing;
    private final long duration;  // 动画时长（毫秒）

    private double from;
    private double to;
    private long startTime;
    private boolean running;

    public Animation(Easing easing, long durationMs) {
        this.easing = easing;
        this.duration = durationMs;
    }

    /**
     * 启动动画，从当前值过渡到目标值
     */
    public void run(double target) {
        this.from = getValue();  // 从当前渲染值开始，避免跳变
        this.to = target;
        this.startTime = System.currentTimeMillis();
        this.running = true;
    }

    /**
     * 直接设置值（无动画）
     */
    public void setValue(double value) {
        this.from = value;
        this.to = value;
        this.running = false;
    }

    /**
     * 获取当前动画值
     */
    public double getValue() {
        if (!running) return to;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= duration) {
            running = false;
            return to;
        }

        double progress = (double) elapsed / duration;
        double easedProgress = easing.ease(progress);
        return from + (to - from) * easedProgress;
    }

    /**
     * 动画是否进行中
     */
    public boolean isRunning() {
        if (!running) return false;
        if (System.currentTimeMillis() - startTime >= duration) {
            running = false;
            return false;
        }
        return true;
    }

    /**
     * 立即完成动画
     */
    public void finish() {
        this.from = to;
        this.running = false;
    }

    /**
     * 重置到初始值
     */
    public void reset(double value) {
        this.from = value;
        this.to = value;
        this.running = false;
    }
}
