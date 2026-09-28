package cn.sux1ng.client.ui.notification;

import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.util.animation.Animation;
import cn.sux1ng.client.util.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;

public class Notification {
    private final String description;
    private final String title;
    private final NotificationType type;
    private final long duration;
    private final long start;

    private float y;
    private float width;
    private final float height = 36;
    private boolean isExiting = false;
    private final Minecraft mc = Minecraft.getMinecraft();

    // 缓动动画
    private final Animation slideAnim = new Animation(Easing.EASE_OUT_EXPO, 400);
    private float animX;

    public Notification(String title, String description, NotificationType type, long duration) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.duration = duration;
        this.start = System.currentTimeMillis();

        FontRenderer fr = mc.fontRendererObj;
        this.width = Math.max(138, Math.max(fr.getStringWidth(title),
                fr.getStringWidth(description)) + 49);
        ScaledResolution sr = new ScaledResolution(mc);
        this.animX = sr.getScaledWidth();
    }

    public void render(float targetY) {
        long timeElapsed = System.currentTimeMillis() - start;
        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;

        // 缓动动画
        float targetX;
        if (timeElapsed > duration) {
            if (!isExiting) {
                isExiting = true;
                slideAnim.run(sr.getScaledWidth() + width + 10);
            }
            targetX = sr.getScaledWidth() + width + 10;
        } else {
            if (!isExiting && !slideAnim.isRunning()) {
                slideAnim.run(sr.getScaledWidth() - width - 5);
            }
            targetX = sr.getScaledWidth() - width - 5;
        }

        if (slideAnim.isRunning() || !isExiting) {
            animX = (float) slideAnim.getValue();
        }

        // y 轴也做平滑
        float dy = targetY - this.y;
        this.y += dy * 0.2f;
        if (Math.abs(dy) < 0.5f) this.y = targetY;

        // 已完全滑出屏幕就不渲染
        if (isExiting && animX > sr.getScaledWidth() + width - 5) {
            return;
        }

        float x = animX;

        MeowTheme.Palette theme = MeowTheme.current();
        int typeColor = type == NotificationType.ERROR ? 0xFFE592AD
                : type == NotificationType.WARNING ? theme.secondary : theme.accent;
        DrawUtil.drawRoundedRect(x + 2, y + 3, width, height, 8, 0x55000000);
        DrawUtil.drawRoundedRect(x, y, width, height, 8, theme.surface);
        DrawUtil.drawRoundedOutline(x, y, width, height, 8, 1, theme.outline);
        DrawUtil.drawCircle(x + 17, y + 17, 8, typeColor);
        DrawUtil.drawCircle(x + 17, y + 17, 3, theme.surface);
        fr.drawStringWithShadow(title, x + 32, y + 6, theme.text);
        fr.drawString(description, (int) (x + 32), (int)(y + 20), theme.muted);

        float progress = 1.0f - (float) timeElapsed / duration;
        progress = Math.max(0, Math.min(1, progress));
        float barWidth = (width - 14) * progress;
        DrawUtil.drawRoundedRect(x + 7, y + height - 4, barWidth, 2, 1, typeColor);
    }

    public boolean shouldDelete() {
        return isExiting && animX > new ScaledResolution(mc).getScaledWidth() + width - 5;
    }
}
