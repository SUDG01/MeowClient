package cn.sux1ng.client.util;

import java.awt.Color;

public class ColorUtil {

    // ========== 彩虹 / 动态颜色 ==========

    /**
     * 经典彩虹色
     */
    public static int getRainbow(int speed, int offset) {
        float hue = (float) (System.currentTimeMillis() % speed + offset) / speed;
        return Color.getHSBColor(hue, 0.55f, 1.0f).getRGB();
    }

    /**
     * Astolfo 渐变 (粉→紫→蓝)
     */
    public static int getAstolfo(int offset) {
        float speed = 3000f;
        float hue = (float) (System.currentTimeMillis() % (int)speed + offset) / speed;
        if (hue > 0.5) hue = 0.5f - (hue - 0.5f);
        hue += 0.5f;
        return Color.getHSBColor(hue, 0.5f, 1.0f).getRGB();
    }

    /**
     * 呼吸/脉冲效果
     */
    public static int getPulse(Color color, int index, int count) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        float brightness = Math.abs((System.currentTimeMillis() % 2000L) / 1000.0f + index / (float)count * 2.0f - 1.0f);
        brightness = 0.5f + 0.5f * brightness;
        hsb[2] = brightness % 2.0f;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]).getRGB();
    }

    // ========== Alpha 操作 ==========

    /**
     * 替换 Alpha 通道
     */
    public static int reAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    /**
     * 乘算透明度
     */
    public static int applyOpacity(int color, float opacity) {
        int alpha = (int) (((color >> 24) & 0xFF) * Math.max(0, Math.min(1, opacity)));
        return reAlpha(color, alpha);
    }

    // ========== 亮度操作 ==========

    /**
     * 增亮
     */
    public static int brighter(int color, float factor) {
        int r = (int) Math.min(255, ((color >> 16) & 0xFF) * (1 + factor));
        int g = (int) Math.min(255, ((color >> 8) & 0xFF) * (1 + factor));
        int b = (int) Math.min(255, (color & 0xFF) * (1 + factor));
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /**
     * 变暗
     */
    public static int darker(int color, float factor) {
        int r = (int) Math.max(0, ((color >> 16) & 0xFF) * (1 - factor));
        int g = (int) Math.max(0, ((color >> 8) & 0xFF) * (1 - factor));
        int b = (int) Math.max(0, (color & 0xFF) * (1 - factor));
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    // ========== 颜色插值 ==========

    /**
     * 在两个颜色之间线性插值
     */
    public static int interpolateColor(int color1, int color2, double progress) {
        double p = Math.max(0, Math.min(1, progress));
        int a1 = (color1 >> 24) & 0xFF, r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF, r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * p);
        int r = (int) (r1 + (r2 - r1) * p);
        int g = (int) (g1 + (g2 - g1) * p);
        int b = (int) (b1 + (b2 - b1) * p);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
