package cn.sux1ng.client.value;

public class NumberValue extends Value<Double> {
    private final double min;
    private final double max;
    private final double increment; // 每次增加多少 (比如 0.1 或 1.0)

    public NumberValue(String name, double current, double min, double max, double increment) {
        super(name, current);
        this.min = min;
        this.max = max;
        this.increment = increment;
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