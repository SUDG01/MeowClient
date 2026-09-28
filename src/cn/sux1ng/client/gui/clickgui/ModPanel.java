package cn.sux1ng.client.gui.clickgui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.draw.ClickGUIMod;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.util.animation.Animation;
import cn.sux1ng.client.util.animation.Easing;
import cn.sux1ng.client.value.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ModPanel {
    public Mod mod;
    public int x, y, width;
    public int height = 22;
    public int finalHeight;

    public boolean extended = false;
    private boolean binding = false;
    private TextValue editingText;

    private final Animation hoverAnim = new Animation(Easing.EASE_OUT_CUBIC, 200);
    private final Animation expandAnim = new Animation(Easing.EASE_IN_OUT_CUBIC, 250);

    public ModPanel(Mod mod) {
        this.mod = mod;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, height);
        hoverAnim.run(hovered ? 1.0 : 0.0);
        double hp = hoverAnim.getValue();

        MeowTheme.Palette theme = MeowTheme.current();
        int bgColor = mod.isEnable() ? theme.accentSoft
                : hp > 0.1 ? theme.hover : theme.card;
        DrawUtil.drawRoundedRect(x + 1, y + 2, width - 2, height - 2, 6, bgColor);
        if (mod.isEnable()) DrawUtil.drawRoundedRect(x + 3, y + 6, 2, height - 10, 1, theme.accent);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int textColor = mod.isEnable() ? theme.text : theme.muted;
        String text = binding ? ClientLanguage.ui("Press key...") : ClientLanguage.module(mod);
        fr.drawStringWithShadow(fr.trimStringToWidth(text, width - 17), x + 9,
                y + (height / 2f - fr.FONT_HEIGHT / 2f), textColor);
        DrawUtil.drawCircle(x + width - 9, y + height / 2.0, 2.5,
                mod.isEnable() ? theme.accent : theme.outline);

        finalHeight = height;
        expandAnim.run(extended ? 1.0 : 0.0);

        if (extended) {
            int sy = y + height;
            DrawUtil.drawRoundedRect(x + 1, sy, width - 2, 15, 4, theme.sidebar);
            fr.drawStringWithShadow(ClientLanguage.ui("Bind") + ": " + ClientLanguage.bindMode(mod.getBindMode()),
                    x + 5, sy + 4, theme.text);
            sy += 15;
            DrawUtil.drawRoundedRect(x + 1, sy, width - 2, 15, 4, theme.sidebar);
            String key = binding ? ClientLanguage.ui("Press key...") : Keyboard.getKeyName(mod.getKey());
            fr.drawStringWithShadow(fr.trimStringToWidth(ClientLanguage.ui("Key") + ": " + key, width - 10),
                    x + 5, sy + 4, theme.muted);
            sy += 15;
            for (Value<?> value : mod.getValues()) {
                if (!value.isVisible()) continue;
                sy = renderSingleValue(value, sy, fr, mouseX, mouseY);
            }
            finalHeight = sy - y;
        }
    }

    private int renderSingleValue(Value<?> value, int sy, FontRenderer fr, int mx, int my) {
        MeowTheme.Palette theme = MeowTheme.current();
        int bg = theme.sidebar;
        int sh = 15;

        // --- ValueGroup ---
        if (value instanceof ValueGroup) {
            ValueGroup g = (ValueGroup) value;
            DrawUtil.drawRoundedRect(x + 1, sy, width - 2, 12, 3, theme.card);
            fr.drawStringWithShadow("§l" + ClientLanguage.value(g), x + 5, sy + 2, theme.secondary);
            sy += 12;
            for (Value<?> child : g.getChildren()) {
                if (!child.isVisible()) continue;
                sy = renderSingleValue(child, sy, fr, mx, my);
            }
            return sy;
        }

        DrawUtil.drawRoundedRect(x + 1, sy, width - 2, sh, 3, bg);

        // --- NumberValue ---
        if (value instanceof NumberValue) {
            NumberValue num = (NumberValue) value;
            double cur = num.getValue();
            double range = num.getMax() - num.getMin();
            double tw = range == 0 ? width - 10 : (width - 10) * (cur - num.getMin()) / range;
            DrawUtil.drawRoundedRect(x + 5, sy + sh - 4, width - 10, 2, 1, theme.outline);
            DrawUtil.drawRoundedRect(x + 5, sy + sh - 4, tw, 2, 1, theme.accent);
            fr.drawStringWithShadow(fr.trimStringToWidth(ClientLanguage.value(num) + ": " + num.getValue(), width - 10),
                    x + 5, sy + 2, theme.text);
            if (Mouse.isButtonDown(0) && isHovered(mx, my, x, sy, width, sh)) {
                double pct = (mx - (x + 4)) / (double)(width - 8);
                double val = num.getMin() + (num.getMax() - num.getMin()) * Math.max(0, Math.min(1, pct));
                num.setValue(val);
            }
        }
        // --- BooleanValue ---
        else if (value instanceof BooleanValue) {
            BooleanValue bool = (BooleanValue) value;
            fr.drawStringWithShadow(fr.trimStringToWidth(ClientLanguage.value(bool), width - 30),
                    x + 5, sy + 4, theme.text);
            int swX = x + width - 24, swY = sy + 3;
            DrawUtil.drawRoundedRect(swX, swY, 20, 10, 5, bool.getValue() ? theme.accentSoft : theme.card);
            DrawUtil.drawCircle(bool.getValue() ? swX + 15 : swX + 5, swY + 5, 3, theme.text);
        }
        // --- ModeValue ---
        else if (value instanceof ModeValue) {
            ModeValue mode = (ModeValue) value;
            String label = ClientLanguage.value(mode) + ": " + ClientLanguage.mode(mode);
            fr.drawStringWithShadow(fr.trimStringToWidth(label, width - 10), x + 5, sy + 4, theme.text);
        }
        // --- ColorValue ---
        else if (value instanceof ColorValue) {
            ColorValue cv = (ColorValue) value;
            fr.drawStringWithShadow(fr.trimStringToWidth(ClientLanguage.value(cv), width - 29),
                    x + 5, sy + 4, theme.text);
            int cbX = x + width - 16;
            DrawUtil.drawRoundedRect(cbX, sy + 2, 12, 10, 2, cv.getRGB());
            DrawUtil.drawRoundedOutline(cbX - 1, sy + 1, 14, 12, 3, 1, theme.text);
        }
        // --- TextValue ---
        else if (value instanceof TextValue) {
            TextValue tv = (TextValue) value;
            String text = ClientLanguage.value(tv) + ": " + tv.getValue() + (editingText == tv ? "|" : "");
            fr.drawStringWithShadow(fr.trimStringToWidth(text, width - 10), x + 5, sy + 4, theme.text);
        }
        return sy + sh;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        editingText = null;
        if (isHovered(mouseX, mouseY, x, y, width, height)) {
            if (mouseButton == 0) {
                Mod clickGUIMod = MeowClient.modManager.getByClass(ClickGUIMod.class);
                if (mod != clickGUIMod) {
                    mod.setEnable(!mod.isEnable());
                }
            } else if (mouseButton == 2) {
                this.binding = !this.binding;
            } else if (mouseButton == 1) {
                this.extended = !this.extended;
            }
            return;
        }
        if (this.extended) {
            int sy = y + height;
            if (isHovered(mouseX, mouseY, x, sy, width, 15) && mouseButton == 0) {
                Mod.BindMode[] modes = Mod.BindMode.values();
                mod.setBindMode(modes[(mod.getBindMode().ordinal() + 1) % modes.length]);
                return;
            }
            sy += 15;
            if (isHovered(mouseX, mouseY, x, sy, width, 15) && mouseButton == 0) {
                binding = true;
                return;
            }
            sy += 15;
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
            } else if (value instanceof ModeValue && mb == 0) {
                ((ModeValue) value).cycle();
            } else if (value instanceof ColorValue && mb == 0) {
                ColorValue cv = (ColorValue) value;
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                    cv.setAlpha((cv.getAlpha() + 32) % 256);
                } else {
                    cv.setHue((cv.getHue() + 0.05f) % 1.0f);
                }
            } else if (value instanceof ColorValue && mb == 1) {
                ColorValue cv = (ColorValue) value;
                cv.setSaturation(cv.getSaturation() >= 0.9f ? 0.3f : cv.getSaturation() + 0.2f);
            } else if (value instanceof ColorValue && mb == 2) {
                ColorValue cv = (ColorValue) value;
                cv.setBrightness(cv.getBrightness() >= 0.9f ? 0.3f : cv.getBrightness() + 0.2f);
            } else if (value instanceof TextValue && mb == 0) {
                editingText = (TextValue) value;
            }
        }
        return sy + sh;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (this.binding) {
            mod.setKey(keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE ? 0 : keyCode);
            this.binding = false;
            return true;
        }
        if (editingText != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                editingText = null;
            } else if (keyCode == Keyboard.KEY_BACK) {
                String value = editingText.getValue();
                if (!value.isEmpty()) editingText.setValue(value.substring(0, value.length() - 1));
            } else if (typedChar >= 32 && typedChar != 127 && editingText.getValue().length() < 80) {
                editingText.setValue(editingText.getValue() + typedChar);
            }
            return true;
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {}

    private boolean isHovered(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && my >= ry && mx < rx + rw && my < ry + rh;
    }
}
