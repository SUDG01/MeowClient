package cn.sux1ng.client.mod.mods.draw;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.MathHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class InfoHUDMod extends Mod {

    // 1. 位置参数
    public NumberValue x = new NumberValue("X", 2.0, 0.0, 1000.0, 1.0);
    public NumberValue y = new NumberValue("Y", 2.0, 0.0, 1000.0, 1.0);

    // 2. 颜色设置 — 使用 ColorValue 替代 R/G/B 三滑块
    public BooleanValue rainbow = new BooleanValue("Rainbow", true);
    public ColorValue staticColor = new ColorValue("Color", new Color(255, 183, 178))
            .setVisibility(() -> !rainbow.getValue());

    // 3. 内容开关
    public BooleanValue showFPS = new BooleanValue("FPS", true);
    public BooleanValue showBPS = new BooleanValue("BPS", true);
    public BooleanValue showXYZ = new BooleanValue("XYZ", true);
    public BooleanValue showBackground = new BooleanValue("Background", false);

    public InfoHUDMod() {
        super("InfoHUD", Category.DRAW);
        addValues(x, y, rainbow, staticColor, showFPS, showBPS, showXYZ, showBackground);
        setEnable(true);
    }

    @Override
    public void draw() {
        if (mc.thePlayer == null || mc.gameSettings.showDebugInfo) return;

        FontRenderer fr = mc.fontRendererObj;
        List<String> lines = new ArrayList<>();

        // 准备数据
        if (showFPS.getValue()) {
            lines.add("FPS: §f" + Minecraft.getDebugFPS());
        }

        if (showBPS.getValue()) {
            double dist = Math.hypot(mc.thePlayer.posX - mc.thePlayer.lastTickPosX, mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ);
            double bps = dist * 20.0 * mc.timer.timerSpeed;
            lines.add("BPS: §f" + String.format("%.2f", bps));
        }

        if (showXYZ.getValue()) {
            int px = MathHelper.floor_double(mc.thePlayer.posX);
            int py = MathHelper.floor_double(mc.thePlayer.posY);
            int pz = MathHelper.floor_double(mc.thePlayer.posZ);
            lines.add("XYZ: §f" + px + " " + py + " " + pz);
        }

        float startX = x.getValue().floatValue();
        float startY = y.getValue().floatValue();

        // 固定颜色
        int staticRGB = staticColor.getRGB();

        // 可选圆角背景
        if (showBackground.getValue() && !lines.isEmpty()) {
            float bgW = 0;
            for (String line : lines) {
                float w = fr.getStringWidth(line);
                if (w > bgW) bgW = w;
            }
            float bgH = lines.size() * (fr.FONT_HEIGHT + 2) + 4;
            DrawUtil.drawRoundedRect(startX - 3, startY - 3, bgW + 10, bgH, 4, 0x60000000);
        }

        int count = 0;
        for (String line : lines) {
            int color;

            if (rainbow.getValue()) {
                color = getKawaiiRainbow(count * 200);
            } else {
                color = staticRGB;
            }

            fr.drawStringWithShadow(line, startX, startY, color);

            startY += fr.FONT_HEIGHT + 2;
            count++;
        }
    }

    private int getKawaiiRainbow(int offset) {
        float speed = 4000f;
        float hue = (float) (System.currentTimeMillis() % (int)speed + offset) / speed;
        return Color.getHSBColor(hue, 0.55f, 1.0f).getRGB();
    }
}
