package cn.sux1ng.client.ui.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;

public class Notification {
    private final String description;
    private final String title;
    private final NotificationType type;
    private final long duration;
    private final long start;

    // 动画相关的变量
    private float x, y;
    private float width, height;
    private boolean isExiting = false;
    private final Minecraft mc = Minecraft.getMinecraft();

    public Notification(String title, String description, NotificationType type, long duration) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.duration = duration;
        this.start = System.currentTimeMillis();

        // 计算宽度：取标题和内容中较长的那个 + 一些边距
        FontRenderer fr = mc.fontRendererObj;
        this.width = Math.max(fr.getStringWidth(title), fr.getStringWidth(description)) + 40;
        this.height = 30;

        // 初始位置设定在屏幕外右侧
        ScaledResolution sr = new ScaledResolution(mc);
        this.x = sr.getScaledWidth();
    }

    public void render(float targetY) {
        long timeElapsed = System.currentTimeMillis() - start;
        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;

        // 1. 动画逻辑 (仿 Reversal 的 Lerp)
        // 如果时间快到了，或者被标记为退出，目标 X 设为屏幕外
        float targetX;
        if (timeElapsed > duration) {
            isExiting = true;
            targetX = sr.getScaledWidth() + width + 10; // 移出屏幕
        } else {
            targetX = sr.getScaledWidth() - width - 5; // 目标位置
        }

        // 核心动画算法：当前值 = 当前值 + (目标值 - 当前值) * 速度
        // 0.2F 是动画速度，越大越快
        this.x = lerp(this.x, targetX, 0.15f);
        this.y = lerp(this.y, targetY, 0.15f);

        // 如果完全移出屏幕了，就不渲染了
        if (Math.abs(this.x - targetX) < 1 && isExiting) {
            return;
        }

        // 2. 绘制背景 (这里用简单的矩形，如果你有 RenderUtil.drawRoundedRect 可以替换)
        // 背景黑底半透明
        Gui.drawRect((int)x, (int)y, (int)(x + width), (int)(y + height), new Color(0, 0, 0, 180).getRGB());

        // 3. 绘制进度条 (底部的一条线)
        float progress = (float) (duration - timeElapsed) / duration; // 剩余时间百分比
        float barWidth = width * progress;
        // 进度条颜色根据类型变化
        int color = type.getColor().getRGB();
        Gui.drawRect((int)x, (int)(y + height - 2), (int)(x + barWidth), (int)(y + height), color);

        // 4. 绘制图标/文字
        // 绘制标题
        fr.drawStringWithShadow(title, x + 5, y + 4, -1);
        // 绘制内容
        fr.drawString(description, (int) (x + 5), (int)(y + 16), new Color(220, 220, 220).getRGB());

        // 绘制右侧的状态图标 (这里用简单的字母代替，Reversal用的是图标字体)
        // 放大一点显示类型首字母
        fr.drawStringWithShadow(type.getIcon(), x + width - 15, y + 10, type.getColor().getRGB());
    }

    public boolean shouldDelete() {
        return isExiting && this.x > new ScaledResolution(mc).getScaledWidth();
    }

    // 简单的线性插值函数
    private float lerp(float start, float end, float factor) {
        return start + factor * (end - start);
    }
}