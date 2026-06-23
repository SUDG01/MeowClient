package cn.sux1ng.client.value;

import java.util.function.Supplier; // 记得导入这个

public class Value<T> {
    private String name;
    private T value;

    // 【新增】可见性检查器 (如果不设置，默认一直显示)
    private Supplier<Boolean> visibility;

    public Value(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }

    // 【新增】判断是否显示
    public boolean isVisible() {
        // 如果没有设置规则，就默认显示(true)；否则运行规则看结果
        return visibility == null || visibility.get();
    }

    // 【新增】设置显示规则 (链式调用，写起来很帅)
    public Value<T> setVisibility(Supplier<Boolean> visibility) {
        this.visibility = visibility;
        return this;
    }
}