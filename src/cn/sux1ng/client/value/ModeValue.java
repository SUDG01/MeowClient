package cn.sux1ng.client.value;

public class ModeValue extends Value<String> {
    private final String[] modes; // 所有的可用模式

    public ModeValue(String name, String current, String[] modes) {
        super(name, current);
        this.modes = modes;
    }

    public String[] getModes() {
        return modes;
    }

    @Override
    public void setValue(String value) {
        for (String mode : modes) {
            if (mode.equals(value)) {
                super.setValue(value);
                return;
            }
        }
    }

    // 切换到下一个模式
    public void cycle() {
        String current = getValue();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(current)) {
                // 如果是最后一个，就回到第一个；否则取下一个
                String nextMode = (i == modes.length - 1) ? modes[0] : modes[i + 1];
                setValue(nextMode);
                return;
            }
        }
        // 防崩：如果没找到当前值，重置为第一个
        if (modes.length > 0) setValue(modes[0]);
    }

    // 判断当前是不是某个模式(简便方法)
    public boolean is(String modeName) {
        return getValue().equalsIgnoreCase(modeName);
    }
}
