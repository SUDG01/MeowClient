package cn.sux1ng.client.util;

import java.awt.Color;

public class ColorUtil {

    // 1. 经典的彩虹色 (你原来的 Kawaii Rainbow)
    public static int getRainbow(int speed, int offset) {
        float hue = (float) (System.currentTimeMillis() % speed + offset) / speed;
        return Color.getHSBColor(hue, 0.55f, 1.0f).getRGB();
    }

    // 2. Astolfo 渐变 (高端客户端标配，粉色到蓝色的柔和渐变)
    public static int getAstolfo(int offset) {
        float speed = 3000f;
        float hue = (float) (System.currentTimeMillis() % (int)speed + offset) / speed;
        // Astolfo 的特点是色调在 0.5 左右徘徊
        if (hue > 0.5) hue = 0.5f - (hue - 0.5f);
        hue += 0.5f;
        return Color.getHSBColor(hue, 0.5f, 1.0f).getRGB();
    }

    // 3. 呼吸/脉冲效果 (单色变暗再变亮)
    public static int getPulse(Color color, int index, int count) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);

        float brightness = Math.abs((System.currentTimeMillis() % 2000L) / 1000.0f + index / (float)count * 2.0f - 1.0f);
        brightness = 0.5f + 0.5f * brightness; // 限制亮度最低 0.5

        hsb[2] = brightness % 2.0f;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]).getRGB();
    }
}