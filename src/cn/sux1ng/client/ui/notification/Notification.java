package cn.sux1ng.client.ui.notification;

import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.util.animation.Animation;
import cn.sux1ng.client.util.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;

public class Notification {
    private final String description;
    private final String title;
    private final NotificationType type;
    private final long duration;
    private final long start;

    private float y;
    private float width;
    private final float height = 32;
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
        this.width = Math.max(fr.getStringWidth(title), fr.getStringWidth(description)) + 50;
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

        // 阴影层
        DrawUtil.drawRoundedRect(x + 2, y + 2, width, height, 6, new Color(0, 0, 0, 80).getRGB());

        // 主体背景 — 圆角
        DrawUtil.drawRoundedRect(x, y, width, height, 6, new Color(20, 20, 20, 220).getRGB());

        // 左侧色条
        int typeColor = type.getColor().getRGB();
        DrawUtil.drawRoundedRect(x + 2, y + 4, 3, height - 8, 2, typeColor);

        // 圆形图标
        DrawUtil.drawCircle(x + 14, y + height / 2, 7, typeColor);
        fr.drawStringWithShadow(type.getIcon(), x + 11, y + height / 2 - fr.FONT_HEIGHT / 2, 0xFFFFFFFF);

        // 标题
        fr.drawStringWithShadow(title, x + 26, y + 4, 0xFFFFFFFF);

        // 描述
        fr.drawString(description, (int) (x + 26), (int)(y + 18), new Color(200, 200, 200).getRGB());

        // 进度条 — 圆角
        float progress = 1.0f - (float) timeElapsed / duration;
        progress = Math.max(0, Math.min(1, progress));
        float barWidth = (width - 6) * progress;
        DrawUtil.drawRoundedRect(x + 3, y + height - 4, barWidth, 2, 1, typeColor);
    }

    public boolean shouldDelete() {
        return isExiting && animX > new ScaledResolution(mc).getScaledWidth() + width - 5;
    }
}
