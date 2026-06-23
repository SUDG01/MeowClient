package cn.sux1ng.client.ui.notification; // 改包名

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    // 使用线程安全的 List，防止遍历时报错
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    // 发送通知的方法
    public static void show(String title, String description, NotificationType type) {
        // 默认显示 2.5 秒，根据文字长度适当增加
        long duration = 2000 + description.length() * 20L;
        notifications.add(new Notification(title, description, type, duration));
    }

    // 在 Render2D 事件中调用这个方法
    public static void render() {
        if (notifications.isEmpty()) return;

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        // 初始 Y 坐标：屏幕右下角往上一点
        float bottomY = sr.getScaledHeight() - 40;

        for (Notification notification : notifications) {
            // 渲染通知，并传入目标 Y 坐标
            notification.render(bottomY);

            // 如果通知已经跑出屏幕了，就从列表删除
            if (notification.shouldDelete()) {
                notifications.remove(notification);
            }

            // 下一个通知的位置往上挪 (35 是高度 + 间距)
            bottomY -= 35;
        }
    }
}