package cn.sux1ng.client.gui.clickgui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.draw.ClickGUIMod;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.util.animation.Animation;
import cn.sux1ng.client.util.animation.Easing;
import cn.sux1ng.client.value.*;
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

    private final Animation hoverAnim = new Animation(Easing.EASE_OUT_CUBIC, 200);
    private final Animation expandAnim = new Animation(Easing.EASE_IN_OUT_CUBIC, 250);

    public ModPanel(Mod mod) {
        this.mod = mod;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);
        hoverAnim.run(hovered ? 1.0 : 0.0);
        double hp = hoverAnim.getValue();

        int bgColor = mod.isEnable() ? 0xFFFFB7B2 : ((112 + (int)(30 * hp)) << 24);
        DrawUtil.drawRoundedRect(x + 1, y, width - 2, height, 4, bgColor);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int textColor = mod.isEnable() ? 0xFFFFFFFF : 0xFFCCCCCC;
        String text = binding ? "Press Key..." : mod.getName();
        if (mod.getKey() != 0 && !binding) text += " [" + Keyboard.getKeyName(mod.getKey()) + "]";
        fr.drawStringWithShadow(text, x + (width / 2f - fr.getStringWidth(text) / 2f),
                y + (height / 2f - fr.FONT_HEIGHT / 2f), textColor);

        finalHeight = height;
        expandAnim.run(extended ? 1.0 : 0.0);

        if (extended && !mod.getValues().isEmpty()) {
            int sy = y + height;
            for (Value<?> value : mod.getValues()) {
                if (!value.isVisible()) continue;
                sy = renderSingleValue(value, sy, fr, mouseX, mouseY);
            }
            finalHeight = sy - y;
        }
    }

    private int renderSingleValue(Value<?> value, int sy, FontRenderer fr, int mx, int my) {
        int bg = 0xFF202020;
        int sh = 15;

        // --- ValueGroup ---
        if (value instanceof ValueGroup) {
            ValueGroup g = (ValueGroup) value;
            DrawUtil.drawRect(x, sy, width, 12, 0xFF2A2A2A);
            fr.drawStringWithShadow("§l" + g.getName(), x + 4, sy + 2, 0xFFFFFF88);
            sy += 12;
            for (Value<?> child : g.getChildren()) {
                if (!child.isVisible()) continue;
                sy = renderSingleValue(child, sy, fr, mx, my);
            }
            return sy;
        }

        DrawUtil.drawRect(x, sy, width, sh, bg);

        // --- NumberValue ---
        if (value instanceof NumberValue) {
            NumberValue num = (NumberValue) value;
            double cur = num.getValue();
            double tw = (width - 8) * (cur - num.getMin()) / (num.getMax() - num.getMin());
            DrawUtil.drawRoundedRect(x + 4, sy + sh - 5, width - 8, 2, 1, 0xFF404040);
            DrawUtil.drawRoundedRect(x + 4, sy + sh - 5, (int) tw, 2, 1, 0xFFFFB7B2);
            DrawUtil.drawCircle(x + 4 + tw, sy + sh - 4, 3, 0xFFFFFFFF);
            fr.drawStringWithShadow(num.getName() + ": " + num.getValue(), x + 4, sy + 2, 0xFFFFFFFF);
            if (Mouse.isButtonDown(0) && isHovered(mx, my, x, sy, width, sh)) {
                double pct = (mx - (x + 4)) / (double)(width - 8);
                double val = num.getMin() + (num.getMax() - num.getMin()) * Math.max(0, Math.min(1, pct));
                num.setValue(new BigDecimal(val).setScale(1, RoundingMode.HALF_UP).doubleValue());
            }
        }
        // --- BooleanValue ---
        else if (value instanceof BooleanValue) {
            BooleanValue bool = (BooleanValue) value;
            fr.drawStringWithShadow(bool.getName(), x + 4, sy + 4, 0xFFFFFFFF);
            int swX = x + width - 24, swY = sy + 3;
            DrawUtil.drawRoundedRect(swX, swY, 20, 10, 5, bool.getValue() ? 0xFFFFB7B2 : 0xFF555555);
            DrawUtil.drawCircle(bool.getValue() ? swX + 15 : swX + 5, swY + 5, 4, 0xFFFFFFFF);
        }
        // --- ModeValue ---
        else if (value instanceof ModeValue) {
            ModeValue mode = (ModeValue) value;
            fr.drawStringWithShadow(mode.getName(), x + 4, sy + 4, 0xFFFFFFFF);
            String mt = mode.getValue();
            fr.drawStringWithShadow(mt, x + width - fr.getStringWidth(mt) - 5, sy + 4, 0xFFFFFF00);
        }
        // --- ColorValue ---
        else if (value instanceof ColorValue) {
            ColorValue cv = (ColorValue) value;
            fr.drawStringWithShadow(cv.getName(), x + 4, sy + 4, 0xFFFFFFFF);
            int cbX = x + width - 16;
            DrawUtil.drawRoundedRect(cbX, sy + 2, 12, 10, 2, cv.getRGB());
            DrawUtil.drawRect(cbX - 1, sy + 1, 14, 1, 0xFFFFFFFF);
            DrawUtil.drawRect(cbX - 1, sy + 11, 14, 1, 0xFFFFFFFF);
            DrawUtil.drawRect(cbX - 1, sy + 1, 1, 12, 0xFFFFFFFF);
            DrawUtil.drawRect(cbX + 12, sy + 1, 1, 12, 0xFFFFFFFF);
        }
        // --- TextValue ---
        else if (value instanceof TextValue) {
            TextValue tv = (TextValue) value;
            fr.drawStringWithShadow(tv.getName() + ": §7" + tv.getValue(), x + 4, sy + 4, 0xFFFFFFAA);
        }
        return sy + sh;
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
            int sy = y + height;
            for (Value<?> value : mod.getValues()) {
                if (!value.isVisible()) continue;
                if (value instanceof ValueGroup) {
                    ValueGroup g = (ValueGroup) value;
                    sy += 12;
                    for (Value<?> child : g.getChildren()) {
                        if (!child.isVisible()) continue;
                        sy = handleValueClick(child, sy, mouseX, mouseY, mouseButton);
                    }
                    continue;
                }
                sy = handleValueClick(value, sy, mouseX, mouseY, mouseButton);
            }
        }
    }

    private int handleValueClick(Value<?> value, int sy, int mx, int my, int mb) {
        int sh = 15;
        if (isHovered(mx, my, x, sy, width, sh)) {
            if (value instanceof BooleanValue && mb == 0) {
                ((BooleanValue) value).setValue(!((BooleanValue) value).getValue());
                Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
            } else if (value instanceof ModeValue && mb == 0) {
                ((ModeValue) value).cycle();
                Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
            } else if (value instanceof ColorValue && mb == 0) {
                ColorValue cv = (ColorValue) value;
                cv.setHue((cv.getHue() + 0.05f) % 1.0f);
                Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
            } else if (value instanceof ColorValue && mb == 1) {
                ColorValue cv = (ColorValue) value;
                cv.setSaturation(cv.getSaturation() >= 0.9f ? 0.3f : cv.getSaturation() + 0.2f);
                Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1, 1);
            }
        }
        return sy + sh;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (this.binding) {
            mod.setKey(keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE ? 0 : keyCode);
            this.binding = false;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {}

    private boolean isHovered(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && my >= ry && mx < rx + rw && my < ry + rh;
    }
}
