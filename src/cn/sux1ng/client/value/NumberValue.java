package cn.sux1ng.client.value;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberValue extends Value<Double> {
    private final double min;
    private final double max;
    private final double increment; // 每次增加多少 (比如 0.1 或 1.0)

    public NumberValue(String name, double current, double min, double max, double increment) {
        super(name, current);
        if (min > max || increment <= 0 || !Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(increment)) {
            throw new IllegalArgumentException("Invalid range or increment for " + name);
        }
        this.min = min;
        this.max = max;
        this.increment = increment;
        setValue(current);
    }

    @Override
    public void setValue(Double value) {
        if (value == null || !Double.isFinite(value)) {
            throw new IllegalArgumentException("Invalid number for " + getName());
        }
        double clamped = Math.max(min, Math.min(max, value));
        BigDecimal steps = BigDecimal.valueOf(clamped).subtract(BigDecimal.valueOf(min))
                .divide(BigDecimal.valueOf(increment), 0, RoundingMode.HALF_UP);
        double snapped = BigDecimal.valueOf(min)
                .add(BigDecimal.valueOf(increment).multiply(steps)).doubleValue();
        super.setValue(Math.max(min, Math.min(max, snapped)));
    }

    @Override
    public NumberValue setVisibility(java.util.function.Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this; // 返回 NumberValue 自己，解决类型不匹配
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getIncrement() { return increment; }
}
