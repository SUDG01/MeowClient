package cn.sux1ng.client.value;

/**
 * 文本配置项
 * 用于保存用户输入的自定义文本（如 AutoGG 自定义消息、自定义名字等）
 */
public class TextValue extends Value<String> {

    public TextValue(String name, String text) {
        super(name, text);
    }

    @Override
    public TextValue setVisibility(java.util.function.Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this;
    }
}
