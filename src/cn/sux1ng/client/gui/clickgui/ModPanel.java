package cn.sux1ng.client.gui.clickgui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.draw.ClickGUIMod;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import cn.sux1ng.client.value.TextValue;
import cn.sux1ng.client.value.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ModPanel {
    public Mod mod;
    public int x, y, width;
    public int height = 20;
    public int finalHeight;

    public boolean extended = false;
    private boolean binding = false;

    public ModPanel(Mod mod) {
        this.mod = mod;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // --- 1. 绘制按钮 ---
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);

        // 背景色逻辑
        int color = mod.isEnable() ? 0xFFFFB7B2 : (hovered ? 0x90FFFFFF : 0x70000000); // 关闭时用半透明黑，对比度更高

        // 【核心修复】字体颜色逻辑
        // 开启时：纯白 (0xFFFFFFFF)
        // 关闭时：亮灰 (0xFFCCCCCC) -> 之前是 0xFF555555 太黑了，所以才糊！
        int textColor = mod.isEnable() ? 0xFFFFFFFF : 0xFFCCCCCC;

        DrawUtil.drawRect(x, y, width, height, color);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String text = binding ? "Press Key..." : mod.getName();
        if (mod.getKey() != 0 && !binding) text += " [" + Keyboard.getKeyName(mod.getKey()) + "]";

        // 绘制文字
        fr.drawStringWithShadow(text, x + (width / 2f - fr.getStringWidth(text) / 2f), y + (height / 2f - fr.FONT_HEIGHT / 2f), textColor);

        // --- 2. 绘制设置区域 ---
        finalHeight = height;

        if (this.extended && !mod.getValues().isEmpty()) {
            int settingsY = y + height;

            // 设置区域背景色 (深黑)
            int settingBgColor = 0xFF202020;

            for (Value<?> value : mod.getValues()) {
                if (!value.isVisible()) continue;

                int settingHeight = 15;
                DrawUtil.drawRect(x, settingsY, width, settingHeight, settingBgColor);

                // --- NumberValue ---
                if (value instanceof NumberValue) {
                    NumberValue num = (NumberValue) value;
                    double current = num.getValue();
                    double min = num.getMin();
                    double max = num.getMax();
                    double renderWidth = (width - 6) * (current - min) / (max - min);

                    // 槽 (深灰)
                    DrawUtil.drawRect(x + 3, settingsY + settingHeight - 5, width - 6, 2, 0xFF404040);
                    // 条 (粉色)
                    DrawUtil.drawRect(x + 3, settingsY + settingHeight - 5, (int)renderWidth, 2, 0xFFFFB7B2);
                    // 头 (白)
                    DrawUtil.drawRect(x + 3 + (int)renderWidth - 1, settingsY + settingHeight - 7, 2, 6, 0xFFFFFFFF);

                    fr.drawStringWithShadow(num.getName() + ": " + num.getValue(), x + 3, settingsY + 2, 0xFFFFFFFF);

                    // 拖动逻辑
                    if (Mouse.isButtonDown(0) && isHovered(mouseX, mouseY, x, settingsY, width, settingHeight)) {
                        double percent = (mouseX - (x + 3)) / (double)(width - 6);
                        double val = min + (max - min) * percent;
                        val = Math.max(min, Math.min(max, val));
                        BigDecimal bd = new BigDecimal(val);
                        val = bd.setScale(1, RoundingMode.HALF_UP).doubleValue();
                        num.setValue(val);
                    }
                }

                // --- BooleanValue ---
                else if (value instanceof BooleanValue) {
                    BooleanValue bool = (BooleanValue) value;
                    fr.drawStringWithShadow(bool.getName(), x + 3, settingsY + 4, 0xFFFFFFFF);
                    if (bool.getValue()) {
                        fr.drawStringWithShadow("On", x + width - 15, settingsY + 4, 0xFF00FF00);
                    } else {
                        fr.drawStringWithShadow("Off", x + width - 18, settingsY + 4, 0xFFAAAAAA);
                    }
                }

                // --- ModeValue ---
                else if (value instanceof ModeValue) {
                    ModeValue mode = (ModeValue) value;
                    fr.drawStringWithShadow(mode.getName(), x + 3, settingsY + 4, 0xFFFFFFFF);
                    String modeText = mode.getValue();
                    fr.drawStringWithShadow(modeText, x + width - fr.getStringWidth(modeText) - 5, settingsY + 4, 0xFFFFFF00);
                }

                // --- ColorValue ---
                else if (value instanceof ColorValue) {
                    ColorValue colorVal = (ColorValue) value;
                    fr.drawStringWithShadow(colorVal.getName(), x + 3, settingsY + 4, 0xFFFFFFFF);
                    // 画一个小色块作为颜色预览
                    int colorBlockX = x + width - 16;
                    int colorBlockY = settingsY + 2;
                    DrawUtil.drawRect(colorBlockX, colorBlockY, 12, 10, colorVal.getRGB());
                    // 色块边框
                    DrawUtil.drawRect(colorBlockX - 1, colorBlockY - 1, 1, 12, 0xFFFFFFFF);
                    DrawUtil.drawRect(colorBlockX + 12, colorBlockY - 1, 1, 12, 0xFFFFFFFF);
                    DrawUtil.drawRect(colorBlockX - 1, colorBlockY - 1, 14, 1, 0xFFFFFFFF);
                    DrawUtil.drawRect(colorBlockX - 1, colorBlockY + 10, 14, 1, 0xFFFFFFFF);
                }

                // --- TextValue ---
                else if (value instanceof TextValue) {
                    TextValue textVal = (TextValue) value;
                    String display = textVal.getName() + ": §7" + textVal.getValue();
                    fr.drawStringWithShadow(display, x + 3, settingsY + 4, 0xFFFFFFAA);
                }

                settingsY += settingHeight;
                finalHeight += settingHeight;
            }
        }
    }

    // ... (mouseClicked, keyTyped, mouseReleased 保持不变) ...
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY, x, y, width, height)) {
            if (mouseButton == 0) {
                Mod clickGUIMod = MeowClient.modManager.getByClass(ClickGUIMod.class);
                if (mod != clickGUIMod) {
                    mod.setEnable(!mod.isEnable());
                    Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
                }
            } else if (mouseButton == 2) {
                this.binding = !this.binding;
            } else if (mouseButton == 1) {
                this.extended = !this.extended;
                Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
            }
            return;
        }
        if (this.extended && !mod.getValues().isEmpty()) {
            int settingsY = y + height;
            for (Value<?> value : mod.getValues()) {
                if (!value.isVisible()) continue;
                int settingHeight = 15;
                if (isHovered(mouseX, mouseY, x, settingsY, width, settingHeight)) {
                    if (value instanceof BooleanValue && mouseButton == 0) {
                        BooleanValue bool = (BooleanValue) value;
                        bool.setValue(!bool.getValue());
                        Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
                    } else if (value instanceof ModeValue && mouseButton == 0) {
                        ModeValue mode = (ModeValue) value;
                        mode.cycle();
                        Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
                    } else if (value instanceof ColorValue && mouseButton == 0) {
                        // 左键点击：循环切换色相（简单交互）
                        ColorValue colorVal = (ColorValue) value;
                        float nextHue = (colorVal.getHue() + 0.05f) % 1.0f;
                        colorVal.setHue(nextHue);
                        Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
                    } else if (value instanceof ColorValue && mouseButton == 1) {
                        // 右键点击：切换饱和度
                        ColorValue colorVal = (ColorValue) value;
                        float nextSat = colorVal.getSaturation() >= 0.9f ? 0.3f : colorVal.getSaturation() + 0.2f;
                        colorVal.setSaturation(nextSat);
                        Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
                    }
                }
                settingsY += settingHeight;
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (this.binding) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) mod.setKey(0);
            else mod.setKey(keyCode);
            this.binding = false;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {}

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}