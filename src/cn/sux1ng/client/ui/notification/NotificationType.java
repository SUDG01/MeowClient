package cn.sux1ng.client.ui.notification; // 记得改成你自己的包名

import java.awt.Color;

public enum NotificationType {
    INFO(new Color(255, 255, 255), "I"),
    WARNING(new Color(255, 255, 120), "W"),
    ERROR(new Color(255, 80, 80), "E"),
    SUCCESS(new Color(100, 255, 100), "S");

    private final Color color;
    private final String icon;

    NotificationType(Color color, String icon) {
        this.color = color;
        this.icon = icon;
    }

    public Color getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }
}