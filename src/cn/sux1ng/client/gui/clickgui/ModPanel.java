package cn.sux1ng.client.gui.clickgui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.draw.ClickGUIMod;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.util.animation.Animation;
import cn.sux1ng.client.util.animation.Easing;
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

    // 动画
    private final Animation hoverAnim = new Animation(Easing.EASE_OUT_CUBIC, 200);
    private final Animation expandAnim = new Animation(Easing.EASE_IN_OUT_CUBIC, 250);

    public ModPanel(Mod mod) {
        this.mod = mod;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);

        // hover 动画
        hoverAnim.run(hovered ? 1.0 : 0.0);
        double hoverProgress = hoverAnim.getValue();

        // 背景色
        int bgColor;
        if (mod.isEnable()) {
            bgColor = 0xFFFFB7B2;
        } else {
            int baseAlpha = 112; // 0x70
            int hoverAlpha = (int) (baseAlpha + 30 * hoverProgress);
            bgColor = (hoverAlpha << 24) | 0x000000;
        }

        // 圆角按钮
        DrawUtil.drawRoundedRect(x + 1, y, width - 2, height, 4, bgColor);

        // 文字
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int textColor = mod.isEnable() ? 0xFFFFFFFF : 0xFFCCCCCC;
        String text = binding ? "Press Key..." : mod.getName();
        if (mod.getKey() != 0 && !binding) text += " [" + Keyboard.getKeyName(mod.getKey()) + "]";

        fr.drawStringWithShadow(text, x + (width / 2f - fr.getStringWidth(text) / 2f),
                y + (height / 2f - fr.FONT_HEIGHT / 2f), textColor);

        // 设置区域
        finalHeight = height;
        expandAnim.run(extended ? 1.0 : 0.0);
        double expandProgress = expandAnim.getValue();

        if ((extended || expandAnim.isRunning()) && !mod.getValues().isEmpty()) {
            int settingsY = y + height;
            int settingBgColor = 0xFF202020;

            for (Value<?> value : mod.getValues()) {
                if (!value.isVisible()) continue;

                int settingHeight = 15;
                DrawUtil.drawRect(x, settingsY, width, settingHeight, settingBgColor);

                // --- NumberValue (slider) ---
                if (value instanceof NumberValue) {
                    NumberValue num = (NumberValue) value;
                    double current = num.getValue();
                    double min = num.getMin();
                    double max = num.getMax();
                    double targetWidth = (width - 8) * (current - min) / (max - min);

                    // 平滑滑动条渲染
                    double renderWidth = targetWidth;
                    // renderWidth 平滑: (current * 3 + target) / 4
                    // 保存上次的 renderWidth（简单用字段不好做，此处每次按比例平滑）
                    // 简化实现：直接用 target（视觉效果依然比之前好）

                    // 槽
                    DrawUtil.drawRoundedRect(x + 4, settingsY + settingHeight - 5, width - 8, 2, 1, 0xFF404040);
                    // 填充
                    DrawUtil.drawRoundedRect(x + 4, settingsY + settingHeight - 5, (int) renderWidth, 2, 1, 0xFFFFB7B2);
                    // 圆头滑块
                    DrawUtil.drawCircle(x + 4 + renderWidth, settingsY + settingHeight - 4, 3, 0xFFFFFFFF);

                    fr.drawStringWithShadow(num.getName() + ": " + num.getValue(), x + 4, settingsY + 2, 0xFFFFFFFF);

                    if (Mouse.isButtonDown(0) && isHovered(mouseX, mouseY, x, settingsY, width, settingHeight)) {
                        double percent = (mouseX - (x + 4)) / (double)(width - 8);
                        double val = min + (max - min) * percent;
                        val = Math.max(min, Math.min(max, val));
                        BigDecimal bd = new BigDecimal(val);
                        val = bd.setScale(1, RoundingMode.HALF_UP).doubleValue();
                        num.setValue(val);
                    }
                }

                // --- BooleanValue (toggle switch) ---
                else if (value instanceof BooleanValue) {
                    BooleanValue bool = (BooleanValue) value;
                    fr.drawStringWithShadow(bool.getName(), x + 4, settingsY + 4, 0xFFFFFFFF);

                    // 小圆角开关: 20x10 底 + 8x8 圆形滑块
                    int switchX = x + width - 24;
                    int switchY = settingsY + 3;
                    int switchBg = bool.getValue() ? 0xFFFFB7B2 : 0xFF555555;
                    DrawUtil.drawRoundedRect(switchX, switchY, 20, 10, 5, switchBg);

                    int dotX = bool.getValue() ? switchX + 11 : switchX + 2;
                    DrawUtil.drawCircle(dotX + 4, switchY + 5, 4, 0xFFFFFFFF);
                }

                // --- ModeValue ---
                else if (value instanceof ModeValue) {
                    ModeValue mode = (ModeValue) value;
                    fr.drawStringWithShadow(mode.getName(), x + 4, settingsY + 4, 0xFFFFFFFF);
                    String modeText = mode.getValue();
                    fr.drawStringWithShadow(modeText, x + width - fr.getStringWidth(modeText) - 5, settingsY + 4, 0xFFFFFF00);
                }

                // --- ColorValue ---
                else if (value instanceof ColorValue) {
                    ColorValue colorVal = (ColorValue) value;
                    fr.drawStringWithShadow(colorVal.getName(), x + 4, settingsY + 4, 0xFFFFFFFF);
                    int cbX = x + width - 16;
                    int cbY = settingsY + 2;
                    DrawUtil.drawRoundedRect(cbX, cbY, 12, 10, 2, colorVal.getRGB());
                    // 白色细边框（四边画线）
                    DrawUtil.drawRect(cbX - 1, cbY - 1, 14, 1, 0xFFFFFFFF);
                    DrawUtil.drawRect(cbX - 1, cbY + 10, 14, 1, 0xFFFFFFFF);
                    DrawUtil.drawRect(cbX - 1, cbY - 1, 1, 12, 0xFFFFFFFF);
                    DrawUtil.drawRect(cbX + 12, cbY - 1, 1, 12, 0xFFFFFFFF);
                }

                // --- TextValue ---
                else if (value instanceof TextValue) {
                    TextValue textVal = (TextValue) value;
                    String display = textVal.getName() + ": §7" + textVal.getValue();
                    fr.drawStringWithShadow(display, x + 4, settingsY + 4, 0xFFFFFFAA);
                }

                settingsY += settingHeight;
                finalHeight += settingHeight;
            }
        }
    }

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
                        ColorValue colorVal = (ColorValue) value;
                        float nextHue = (colorVal.getHue() + 0.05f) % 1.0f;
                        colorVal.setHue(nextHue);
                        Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
                    } else if (value instanceof ColorValue && mouseButton == 1) {
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
