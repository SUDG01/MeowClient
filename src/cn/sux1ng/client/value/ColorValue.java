package cn.sux1ng.client.value;

import java.awt.Color;

/**
 * 颜色配置项
 * 内部使用 HSB（色相/饱和度/亮度）+ Alpha 存储，同时兼容现有的 RGB int 接口
 */
public class ColorValue extends Value<Integer> {
    private float hue;        // 色相 0.0 ~ 1.0
    private float saturation; // 饱和度 0.0 ~ 1.0
    private float brightness; // 亮度 0.0 ~ 1.0
    private int alpha;        // 透明度 0 ~ 255

    /**
     * 从 java.awt.Color 创建
     */
    public ColorValue(String name, Color color) {
        super(name, color.getRGB());
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = color.getAlpha();
    }

    /**
     * 从 RGB int 创建
     */
    public ColorValue(String name, int rgb) {
        this(name, new Color(rgb, true));
    }

    // ========== 获取颜色 ==========

    /**
     * 获取 java.awt.Color 对象
     */
    public Color getColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
    }

    /**
     * 获取 RGB int（兼容 drawRect 等现有方法）
     */
    public int getRGB() {
        return getColor().getRGB();
    }

    /**
     * 获取十六进制颜色代码（如 "FF69B4"）
     */
    public String getHexCode() {
        Color c = getColor();
        return String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * 获取一个更暗的版本（用于对比色 / alt color）
     */
    public Color getAltColor() {
        Color c = getColor();
        return new Color(
                Math.max(0, c.getRed() - 60),
                Math.max(0, c.getGreen() - 60),
                Math.max(0, c.getBlue() - 60),
                alpha
        );
    }

    // ========== 设置颜色 ==========

    /**
     * 用 Color 对象设置颜色，自动解析为 HSB
     */
    public void setColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = color.getAlpha();
        syncRGB();
    }

    @Override
    public void setValue(Integer rgb) {
        if (rgb == null) throw new IllegalArgumentException("Color cannot be null");
        setColor(new Color(rgb, true));
    }

    // ========== HSB 组件操作 ==========

    public float getHue() { return hue; }
    public void setHue(float hue) {
        this.hue = Math.max(0, Math.min(1, hue));
        syncRGB();
    }

    public float getSaturation() { return saturation; }
    public void setSaturation(float saturation) {
        this.saturation = Math.max(0, Math.min(1, saturation));
        syncRGB();
    }

    public float getBrightness() { return brightness; }
    public void setBrightness(float brightness) {
        this.brightness = Math.max(0, Math.min(1, brightness));
        syncRGB();
    }

    public int getAlpha() { return alpha; }
    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
        syncRGB();
    }

    /**
     * 同步 RGB int 到父类的 value
     */
    private void syncRGB() {
        super.setValue(getColor().getRGB());
    }

    // ========== 链式调用 ==========

    @Override
    public ColorValue setVisibility(java.util.function.Supplier<Boolean> visibility) {
        super.setVisibility(visibility);
        return this;
    }
}
