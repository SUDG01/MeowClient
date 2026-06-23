package cn.sux1ng.client.value;

public class BooleanValue extends Value<Boolean> {
    public BooleanValue(String name, boolean value) {
        super(name, value);
    }

    @Override
    public BooleanValue setVisibility(java.util.function.Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this; // 返回 NumberValue 自己，解决类型不匹配
    }
}