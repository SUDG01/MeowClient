package cn.sux1ng.client.gui.csgo;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.gui.ClientSettingsScreen;
import cn.sux1ng.client.gui.SkinAvatar;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.draw.ClickGUIMod;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import cn.sux1ng.client.value.TextValue;
import cn.sux1ng.client.value.Value;
import cn.sux1ng.client.value.ValueGroup;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;

public class CSGOGui extends GuiScreen {

    private int x = 100, y = 100;
    private int width = 480, height = 334;
    private int sidebarWidth = 96;
    private int moduleListWidth = 142;
    private boolean positioned;

    private Category currentCategory = Category.COMBAT;
    private Mod currentMod = null;
    private boolean dragging = false;
    private int dragX, dragY;
    private String searchText = "";
    private boolean typingSearch = false;
    private boolean bindingKey = false;
    private TextValue editingText;
    private int settingsScroll;

    @Override
    public void initGui() {
        width = Math.min(480, Math.max(300, super.width - 16));
        height = Math.min(334, Math.max(210, super.height - 16));
        sidebarWidth = Math.min(96, Math.max(72, width / 5));
        moduleListWidth = Math.min(142, Math.max(116, width / 3));
        if (!positioned) {
            x = Math.max(8, (super.width - width) / 2);
            y = Math.max(8, (super.height - height) / 2);
            positioned = true;
        } else {
            x = Math.max(8, Math.min(super.width - width - 8, x));
            y = Math.max(8, Math.min(super.height - height - 8, y));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MeowTheme.Palette theme = MeowTheme.current();
        int ACCENT = theme.accent;
        int TEXT = theme.text;
        int TEXT_DIS = theme.muted;
        MeowTheme.backdrop(super.width, super.height);

        int radius = 12;
        MeowTheme.panel(x, y, width, height, radius);
        DrawUtil.drawRoundedRect(x + 4, y + 4, sidebarWidth - 4, height - 8, 9, theme.sidebar);

        int lineCol = theme.outline;
        DrawUtil.drawRect(x + sidebarWidth, y + radius, 1, height - radius * 2, lineCol);
        DrawUtil.drawRect(x + sidebarWidth + moduleListWidth, y + radius + 5, 1, height - radius * 2 - 10, lineCol);

        DrawUtil.drawGradientHorizontal(x + radius, y + 3, width - radius * 2, 3,
                ACCENT, theme.secondary);
        fontRendererObj.drawStringWithShadow(MeowClient.NAME, x + 11, y + 12, TEXT);

        // 2. 分类列表
        int catY = y + 40;
        for (Category category : Category.values()) {
            boolean selected = category == currentCategory;
            if (selected) {
                DrawUtil.drawRoundedRect(x + 8, catY - 5, sidebarWidth - 14, 19, 6, theme.card);
                DrawUtil.drawRoundedRect(x + 9, catY - 2, 3, 12, 2, ACCENT);
            }
            fontRendererObj.drawString(ClientLanguage.category(category), x + 20, catY,
                    selected ? TEXT : TEXT_DIS);
            catY += categoryStep();
        }

        int avatarX = x + sidebarWidth / 2;
        int avatarY = y + height - 42;
        boolean avatarHover = SkinAvatar.hit(mouseX, mouseY, avatarX, avatarY, 15)
                || hit(mouseX, mouseY, x + 8, y + height - 25, sidebarWidth - 14, 22);
        SkinAvatar.draw(mc, avatarX, avatarY, 15, avatarHover);
        String settingsLabel = ClientLanguage.ui("Settings");
        fontRendererObj.drawString(settingsLabel,
                avatarX - fontRendererObj.getStringWidth(settingsLabel) / 2,
                y + height - 17, avatarHover ? ACCENT : TEXT_DIS);

        // 3. 搜索框
        int searchY = y + 15;
        DrawUtil.drawRoundedRect(x + sidebarWidth + 6, searchY - 5, moduleListWidth - 12, 18, 6, theme.card);
        String searchDisplay = searchText.isEmpty() ? ClientLanguage.ui("Search") + "..." : searchText;
        fontRendererObj.drawString(searchDisplay, x + sidebarWidth + 10, searchY - 3,
                searchText.isEmpty() ? TEXT_DIS : TEXT);

        // 4. 模块列表（按搜索过滤）
        List<Mod> mods = MeowClient.modManager.getByCategory(currentCategory);
        int modX = x + sidebarWidth + 10;
        int modY = searchY + 16;
        if (mods != null) {
            for (Mod mod : mods) {
                // 搜索过滤
                if (!matchesSearch(mod)) {
                    continue;
                }
                if (mod == currentMod) {
                    DrawUtil.drawRoundedRect(x + sidebarWidth + 5, modY - 5,
                            moduleListWidth - 10, 18, 6, theme.card);
                }
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientLanguage.module(mod),
                        moduleListWidth - 36), modX, modY, mod.isEnable() ? TEXT : TEXT_DIS);
                // 启用指示器 → 实心圆
                if (mod.isEnable()) {
                    DrawUtil.drawCircle(x + sidebarWidth + moduleListWidth - 13, modY + 4, 3, ACCENT);
                }
                modY += 20;
            }
        }

        // 4. 设置区域
        int settingX = x + sidebarWidth + moduleListWidth + 20;
        if (currentMod != null) {
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(
                    ClientLanguage.module(currentMod) + " · " + ClientLanguage.ui("Settings"),
                    settingsWidth()), settingX, y + 12, TEXT);
            DrawUtil.drawRoundedRect(settingX, y + 27, 90, 2, 1, ACCENT);
            int maxScroll = Math.max(0, settingsContentHeight() - (height - 43));
            if (inSettingsArea(mouseX, mouseY)) {
                int wheel = Mouse.getDWheel();
                if (wheel != 0) settingsScroll -= Integer.signum(wheel) * 18;
            }
            settingsScroll = Math.max(0, Math.min(maxScroll, settingsScroll));
            ScaledResolution resolution = new ScaledResolution(mc);
            int scale = resolution.getScaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((settingX - 4) * scale,
                    (resolution.getScaledHeight() - (y + height - 8)) * scale,
                    (width - (settingX - x) - 4) * scale, (height - 38) * scale);
            drawSettings(settingX, y + 35 - settingsScroll, mouseX, mouseY, TEXT, ACCENT);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            fontRendererObj.drawString(ClientLanguage.ui("Select a module"),
                    settingX, y + height / 2 - 10, TEXT_DIS);
        }

        if (dragging) {
            x = Math.max(8, Math.min(super.width - width - 8, mouseX - dragX));
            y = Math.max(8, Math.min(super.height - height - 8, mouseY - dragY));
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        editingText = null;
        bindingKey = false;
        if (mouseButton == 0 && (SkinAvatar.hit(mouseX, mouseY,
                x + sidebarWidth / 2, y + height - 42, 15)
                || hit(mouseX, mouseY, x + 8, y + height - 25, sidebarWidth - 14, 22))) {
            mc.displayGuiScreen(new ClientSettingsScreen(this));
            return;
        }
        if (mouseX >= x + sidebarWidth + 5 && mouseX <= x + sidebarWidth + moduleListWidth - 5
                && mouseY >= y + 11 && mouseY <= y + 29) {
            typingSearch = true;
            return;
        }
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
            dragging = true; dragX = mouseX - x; dragY = mouseY - y; return;
        }
        int catY = y + 40;
        for (Category category : Category.values()) {
            if (mouseX >= x && mouseX <= x + sidebarWidth && mouseY >= catY - 5 && mouseY <= catY + 15) {
                currentCategory = category; currentMod = null; settingsScroll = 0; return;
            }
            catY += categoryStep();
        }
        typingSearch = false;
        List<Mod> mods = MeowClient.modManager.getByCategory(currentCategory);
        int modX = x + sidebarWidth + 10;
        int modY = y + 33;  // offset for search box
        if (mods != null) {
            for (Mod mod : mods) {
                if (!matchesSearch(mod)) {
                    continue;
                }
                if (mouseX >= modX && mouseX <= modX + moduleListWidth - 20 && mouseY >= modY - 4 && mouseY <= modY + 12) {
                    if (mouseButton == 0 && !(mod instanceof ClickGUIMod)) mod.setEnable(!mod.isEnable());
                    else if (mouseButton == 1) { currentMod = mod; settingsScroll = 0; }
                    return;
                }
                modY += 20;
            }
        }
        if (currentMod != null && inSettingsArea(mouseX, mouseY)) {
            int settingX = x + sidebarWidth + moduleListWidth + 20;
            int settingY = y + 35 - settingsScroll;
            if (hit(mouseX, mouseY, settingX, settingY, settingsWidth(), 16) && mouseButton == 0) {
                bindingKey = true;
                return;
            }
            settingY += 18;
            if (hit(mouseX, mouseY, settingX, settingY, settingsWidth(), 16) && mouseButton == 0) {
                Mod.BindMode[] modes = Mod.BindMode.values();
                currentMod.setBindMode(modes[(currentMod.getBindMode().ordinal() + 1) % modes.length]);
                return;
            }
            settingY += 18;
            for (Value<?> value : currentMod.getValues()) {
                if (!value.isVisible()) continue;
                settingY = clickValue(value, settingX, settingY, mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) { dragging = false; }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (bindingKey && currentMod != null) {
            currentMod.setKey(keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE ? 0 : keyCode);
            bindingKey = false;
            return;
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
            return;
        }
        if (typingSearch) {
            if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                if (!searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1);
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                searchText = "";
                typingSearch = false;
            } else if (typedChar >= 32 && typedChar != 127 && searchText.length() < 40) {
                searchText += typedChar;
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void drawSettings(int settingX, int settingY, int mouseX, int mouseY, int textColor, int accent) {
        String key = bindingKey ? ClientLanguage.ui("Press key...") : Keyboard.getKeyName(currentMod.getKey());
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(
                ClientLanguage.ui("Key") + ": " + key, settingsWidth()), settingX, settingY, textColor);
        settingY += 18;
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(
                ClientLanguage.ui("Bind") + ": " + ClientLanguage.bindMode(currentMod.getBindMode()),
                settingsWidth()), settingX, settingY, accent);
        settingY += 18;
        for (Value<?> value : currentMod.getValues()) {
            if (value.isVisible()) settingY = drawValue(value, settingX, settingY, mouseX, mouseY, textColor, accent);
        }
    }

    private int drawValue(Value<?> value, int x, int y, int mouseX, int mouseY, int textColor, int accent) {
        MeowTheme.Palette theme = MeowTheme.current();
        if (value instanceof ValueGroup) {
            fontRendererObj.drawString("§l" + ClientLanguage.value(value), x, y, accent);
            y += 14;
            for (Value<?> child : ((ValueGroup) value).getChildren()) {
                if (child.isVisible()) y = drawValue(child, x + 6, y, mouseX, mouseY, textColor, accent);
            }
            return y;
        }
        if (value instanceof BooleanValue) {
            BooleanValue bool = (BooleanValue) value;
            DrawUtil.drawRoundedRect(x, y, 10, 10, 3, theme.card);
            if (bool.getValue()) DrawUtil.drawRoundedRect(x + 2, y + 2, 6, 6, 1.5, accent);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ClientLanguage.value(bool),
                    settingsWidth() - 22), x + 16, y + 1, textColor);
            return y + 16;
        }
        if (value instanceof NumberValue) {
            NumberValue number = (NumberValue) value;
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(
                    ClientLanguage.value(number) + ": " + number.getValue(), settingsWidth()),
                    x, y, textColor);
            int barY = y + 12;
            double range = number.getMax() - number.getMin();
            double percent = range == 0 ? 1 : (number.getValue() - number.getMin()) / range;
            int sliderWidth = Math.min(120, settingsWidth() - 10);
            DrawUtil.drawRoundedRect(x, barY, sliderWidth, 4, 2, theme.outline);
            DrawUtil.drawRoundedRect(x, barY, (int) (sliderWidth * percent), 4, 2, accent);
            DrawUtil.drawCircle(x + (int) (sliderWidth * percent), barY + 2, 4, accent);
            if (Mouse.isButtonDown(0) && inSettingsArea(mouseX, mouseY)
                    && hit(mouseX, mouseY, x, barY - 2, sliderWidth, 8)) {
                number.setValue(number.getMin() + (mouseX - x) / (double) sliderWidth * range);
            }
            return y + 26;
        }
        if (value instanceof ModeValue) {
            ModeValue mode = (ModeValue) value;
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(
                    ClientLanguage.value(mode) + ": " + ClientLanguage.mode(mode),
                    settingsWidth()), x, y, textColor);
            return y + 16;
        }
        if (value instanceof ColorValue) {
            ColorValue color = (ColorValue) value;
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(
                    ClientLanguage.value(value) + ": #" + color.getHexCode()
                            + " A" + color.getAlpha(), settingsWidth() - 24), x, y, textColor);
            DrawUtil.drawRoundedRect(x + settingsWidth() - 20, y, 12, 10, 2, color.getRGB());
            return y + 18;
        }
        if (value instanceof TextValue) {
            TextValue text = (TextValue) value;
            String display = ClientLanguage.value(text) + ": " + text.getValue()
                    + (editingText == text ? "|" : "");
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(display, settingsWidth()), x, y, textColor);
            return y + 18;
        }
        return y;
    }

    private int clickValue(Value<?> value, int x, int y, int mouseX, int mouseY, int button) {
        if (value instanceof ValueGroup) {
            y += 14;
            for (Value<?> child : ((ValueGroup) value).getChildren()) {
                if (child.isVisible()) y = clickValue(child, x + 6, y, mouseX, mouseY, button);
            }
            return y;
        }
        if (value instanceof NumberValue) return y + 26;
        int rowHeight = value instanceof ColorValue || value instanceof TextValue ? 18 : 16;
        if (hit(mouseX, mouseY, x, y, settingsWidth(), rowHeight)) {
            if (value instanceof BooleanValue && button == 0) {
                BooleanValue bool = (BooleanValue) value;
                bool.setValue(!bool.getValue());
            } else if (value instanceof ModeValue && button == 0) {
                ((ModeValue) value).cycle();
            } else if (value instanceof TextValue && button == 0) {
                editingText = (TextValue) value;
            } else if (value instanceof ColorValue) {
                ColorValue color = (ColorValue) value;
                if (button == 0 && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
                    color.setAlpha((color.getAlpha() + 32) % 256);
                } else if (button == 0) {
                    color.setHue((color.getHue() + 0.05f) % 1.0f);
                } else if (button == 1) {
                    color.setSaturation(color.getSaturation() >= 0.9f ? 0.3f : color.getSaturation() + 0.2f);
                } else if (button == 2) {
                    color.setBrightness(color.getBrightness() >= 0.9f ? 0.3f : color.getBrightness() + 0.2f);
                }
            }
        }
        return y + rowHeight;
    }

    private boolean hit(int mx, int my, int rx, int ry, int width, int height) {
        return mx >= rx && mx < rx + width && my >= ry && my < ry + height;
    }

    private boolean matchesSearch(Mod mod) {
        if (searchText.isEmpty()) return true;
        String needle = searchText.toLowerCase(java.util.Locale.ROOT);
        return mod.getName().toLowerCase(java.util.Locale.ROOT).contains(needle)
                || ClientLanguage.module(mod).toLowerCase(java.util.Locale.ROOT).contains(needle);
    }

    private boolean inSettingsArea(int mouseX, int mouseY) {
        int left = x + sidebarWidth + moduleListWidth + 16;
        return hit(mouseX, mouseY, left, y + 30, width - (left - x) - 8, height - 38);
    }

    private int categoryStep() {
        return Math.min(25, Math.max(17, (height - 100) / Category.values().length));
    }

    private int settingsWidth() {
        return width - sidebarWidth - moduleListWidth - 28;
    }

    private int settingsContentHeight() {
        int total = 36; // Key and Bind rows.
        for (Value<?> value : currentMod.getValues()) {
            if (value.isVisible()) total += valueHeight(value);
        }
        return total;
    }

    private int valueHeight(Value<?> value) {
        if (value instanceof ValueGroup) {
            int total = 14;
            for (Value<?> child : ((ValueGroup) value).getChildren()) {
                if (child.isVisible()) total += valueHeight(child);
            }
            return total;
        }
        if (value instanceof NumberValue) return 26;
        return value instanceof ColorValue || value instanceof TextValue ? 18 : 16;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
