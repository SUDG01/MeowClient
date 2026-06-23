package cn.sux1ng.client.util.animation;

import java.util.function.Function;

/**
 * 缓动函数库
 * 参考 Reversal Easing.java，提供常用缓动曲线
 */
public enum Easing {
    LINEAR(x -> x),

    EASE_IN_QUAD(x -> x * x),
    EASE_OUT_QUAD(x -> x * (2 - x)),
    EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2 * x * x : -1 + (4 - 2 * x) * x),

    EASE_IN_CUBIC(x -> x * x * x),
    EASE_OUT_CUBIC(x -> {
        double x1 = x - 1;
        return x1 * x1 * x1 + 1;
    }),
    EASE_IN_OUT_CUBIC(x -> x < 0.5 ? 4 * x * x * x : (x - 1) * (2 * x - 2) * (2 * x - 2) + 1),

    EASE_OUT_EXPO(x -> x == 1 ? 1 : 1 - Math.pow(2, -10 * x)),

    EASE_OUT_BOUNCE(x -> {
        double n1 = 7.5625;
        double d1 = 2.75;
        if (x < 1 / d1) return n1 * x * x;
        else if (x < 2 / d1) return n1 * (x -= 1.5 / d1) * x + 0.75;
        else if (x < 2.5 / d1) return n1 * (x -= 2.25 / d1) * x + 0.9375;
        else return n1 * (x -= 2.625 / d1) * x + 0.984375;
    });

    private final Function<Double, Double> function;

    Easing(Function<Double, Double> function) {
        this.function = function;
    }

    /**
     * 计算缓动值
     * @param x 输入进度 [0.0, 1.0]
     * @return 缓动后的值 [0.0, 1.0]
     */
    public double ease(double x) {
        if (x <= 0) return 0;
        if (x >= 1) return 1;
        return function.apply(x);
    }
}
