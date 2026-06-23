package cn.sux1ng.client.ui.notification;

import java.awt.Color;

public enum NotificationType {
    INFO(new Color(255, 255, 255), "ⓘ"),       // ⓘ
    WARNING(new Color(255, 255, 120), "⚠"),     // ⚠
    ERROR(new Color(255, 80, 80), "✗"),         // ✗
    SUCCESS(new Color(100, 255, 100), "✓");     // ✓

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
