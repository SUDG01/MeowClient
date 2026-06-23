package cn.sux1ng.client.value;

import java.util.ArrayList;
import java.util.List;

/**
 * 值分组容器 — 将相关配置项放在一个标题下
 * 用法: ValueGroup group = new ValueGroup("Targeting", targetMode, range, fov);
 */
public class ValueGroup extends Value<String> {
    private final List<Value<?>> children = new ArrayList<>();

    public ValueGroup(String name, Value<?>... values) {
        super(name, name);
        for (Value<?> v : values) {
            children.add(v);
        }
    }

    public List<Value<?>> getChildren() {
        return children;
    }
}
