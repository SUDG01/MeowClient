package cn.sux1ng.client.gui.csgo;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.draw.ClickGUIMod;
import cn.sux1ng.client.util.ColorUtil;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import cn.sux1ng.client.value.Value;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

public class CSGOGui extends GuiScreen {

    private int x = 100, y = 100;
    private int width = 450, height = 320;
    private int sidebarWidth = 80;
    private int moduleListWidth = 140;

    private Category currentCategory = Category.COMBAT;
    private Mod currentMod = null;
    private boolean dragging = false;
    private int dragX, dragY;
    private String searchText = "";
    private boolean typingSearch = false;

    // ========== 主题颜色 ==========

    private int getBgColor() {
        if (ClickGUIMod.theme.is("Light")) return new Color(240, 240, 240, 240).getRGB();
        return new Color(29, 29, 29, 240).getRGB();
    }

    private int getSidebarColor() {
        if (ClickGUIMod.theme.is("Light")) return new Color(224, 224, 224, 200).getRGB();
        return new Color(24, 24, 24, 200).getRGB();
    }

    private int getAccentColor() {
        if (ClickGUIMod.theme.is("Skeet")) return new Color(150, 200, 0).getRGB();
        if (ClickGUIMod.theme.is("Light")) return 0xFF007BFF;
        return 0xFFFFB7B2;
    }

    private int getTextColor() {
        if (ClickGUIMod.theme.is("Light")) return 0xFF333333;
        return 0xFFE0E0E0;
    }

    private int getDisableTextColor() {
        if (ClickGUIMod.theme.is("Light")) return 0xFF888888;
        return 0xFF707070;
    }

    private int getLineColor() {
        return ClickGUIMod.theme.is("Light") ? 0xFFAAAAAA : 0xFF353535;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int BG = getBgColor();
        int SIDEBAR = getSidebarColor();
        int ACCENT = getAccentColor();
        int TEXT = getTextColor();
        int TEXT_DIS = getDisableTextColor();

        // 1. 圆角窗口背景
        int radius = 6;
        DrawUtil.drawRoundedRect(x, y, width, height, radius, BG);
        // 侧边栏（左侧圆角，右侧直角）
        DrawUtil.drawRoundedRect(x, y, sidebarWidth, height, radius, SIDEBAR);
        DrawUtil.drawRect(x + sidebarWidth - radius, y, radius, height, SIDEBAR); // 补右侧直角

        // 分隔线
        int lineCol = getLineColor();
        DrawUtil.drawRect(x + sidebarWidth, y + radius, 1, height - radius * 2, lineCol);
        DrawUtil.drawRect(x + sidebarWidth + moduleListWidth, y + radius + 5, 1, height - radius * 2 - 10, lineCol);

        // 顶部渐变装饰条
        DrawUtil.drawGradientHorizontal(x + radius, y, width - radius * 2, 2, ACCENT,
                ColorUtil.applyOpacity(ACCENT, 0.3f));

        // Logo
        fontRendererObj.drawString("Meow", x + 12, y + 10, ACCENT);
        fontRendererObj.drawString("Client", x + 12 + fontRendererObj.getStringWidth("Meow"), y + 10, TEXT);

        // 2. 分类列表
        int catY = y + 40;
        for (Category category : Category.values()) {
            boolean selected = category == currentCategory;
            if (selected) {
                DrawUtil.drawCircle(x + 5, catY + 4, 3, ACCENT);  // 圆点指示器
            }
            fontRendererObj.drawString(category.name(), x + 15, catY, selected ? ACCENT : TEXT_DIS);
            catY += 25;
        }

        // 3. 搜索框
        int searchY = y + 15;
        DrawUtil.drawRoundedRect(x + sidebarWidth + 5, searchY - 4, moduleListWidth - 10, 14, 3, BG);
        String searchDisplay = searchText.isEmpty() ? "§7Search..." : "§f" + searchText;
        fontRendererObj.drawString(searchDisplay, x + sidebarWidth + 10, searchY - 3,
                searchText.isEmpty() ? TEXT_DIS : TEXT);

        // 4. 模块列表（按搜索过滤）
        List<Mod> mods = MeowClient.modManager.getByCategory(currentCategory);
        int modX = x + sidebarWidth + 10;
        int modY = searchY + 16;
        if (mods != null) {
            for (Mod mod : mods) {
                // 搜索过滤
                if (!searchText.isEmpty() && !mod.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    continue;
                }
                if (mod == currentMod) {
                    int hlColor = ClickGUIMod.theme.is("Light") ? new Color(255, 255, 255, 80).getRGB() : new Color(42, 42, 42, 100).getRGB();
                    DrawUtil.drawRoundedRect(x + sidebarWidth + 5, modY - 4, moduleListWidth - 10, 16, 3, hlColor);
                }
                fontRendererObj.drawString(mod.getName(), modX, modY, mod.isEnable() ? TEXT : TEXT_DIS);
                // 启用指示器 → 实心圆
                if (mod.isEnable()) {
                    DrawUtil.drawCircle(x + sidebarWidth + moduleListWidth - 13, modY + 4, 2.5, ACCENT);
                }
                modY += 20;
            }
        }

        // 4. 设置区域
        int settingX = x + sidebarWidth + moduleListWidth + 20;
        int settingY = y + 35;
        if (currentMod != null) {
            fontRendererObj.drawString(currentMod.getName() + " Settings", settingX, y + 10, TEXT);
            // 分隔线 → 圆角
            DrawUtil.drawRoundedRect(settingX, y + 22, 100, 1.5, 1, ACCENT);

            if (currentMod.getValues().isEmpty()) {
                fontRendererObj.drawString("No settings :(", settingX, settingY, TEXT_DIS);
            } else {
                for (Value<?> value : currentMod.getValues()) {
                    if (!value.isVisible()) continue;

                    if (value instanceof BooleanValue) {
                        BooleanValue bool = (BooleanValue) value;
                        int boxColor = ClickGUIMod.theme.is("Light") ? 0xFFAAAAAA : 0xFF404040;
                        // 圆角 checkbox
                        DrawUtil.drawRoundedRect(settingX, settingY, 10, 10, 2, boxColor);
                        if (bool.getValue()) {
                            DrawUtil.drawRoundedRect(settingX + 2, settingY + 2, 6, 6, 1.5, ACCENT);
                        }
                        fontRendererObj.drawString(bool.getName(), settingX + 16, settingY + 1, TEXT);
                        settingY += 16;
                    }
                    else if (value instanceof NumberValue) {
                        NumberValue num = (NumberValue) value;
                        fontRendererObj.drawString(num.getName() + ": " + num.getValue(), settingX, settingY, TEXT);
                        settingY += 12;
                        int barBg = ClickGUIMod.theme.is("Light") ? 0xFFAAAAAA : 0xFF404040;
                        DrawUtil.drawRoundedRect(settingX, settingY, 120, 4, 2, barBg);
                        double percent = (num.getValue() - num.getMin()) / (num.getMax() - num.getMin());
                        DrawUtil.drawRoundedRect(settingX, settingY, (int)(120 * percent), 4, 2, ACCENT);
                        // 圆形滑块头
                        DrawUtil.drawCircle(settingX + (int)(120 * percent), settingY + 2, 4, ACCENT);

                        if (Mouse.isButtonDown(0)) {
                            if (mouseX >= settingX && mouseX <= settingX + 120 && mouseY >= settingY - 2 && mouseY <= settingY + 6) {
                                double val = (mouseX - settingX) / 120.0 * (num.getMax() - num.getMin()) + num.getMin();
                                num.setValue(Math.round(val * 10.0) / 10.0);
                            }
                        }
                        settingY += 14;
                    }
                    else if (value instanceof ModeValue) {
                        ModeValue mode = (ModeValue) value;
                        fontRendererObj.drawString(mode.getName() + ":", settingX, settingY, TEXT);
                        fontRendererObj.drawString(mode.getValue(),
                                settingX + fontRendererObj.getStringWidth(mode.getName() + ":") + 4,
                                settingY, ACCENT);
                        settingY += 16;
                    }
                }
            }
        } else {
            fontRendererObj.drawString("Select a module", settingX, y + height / 2 - 10, TEXT_DIS);
        }

        if (dragging) { x = mouseX - dragX; y = mouseY - dragY; }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
            dragging = true; dragX = mouseX - x; dragY = mouseY - y; return;
        }
        int catY = y + 40;
        for (Category category : Category.values()) {
            if (mouseX >= x && mouseX <= x + sidebarWidth && mouseY >= catY - 5 && mouseY <= catY + 15) {
                currentCategory = category; currentMod = null; return;
            }
            catY += 25;
        }
        // 搜索框点击
        if (mouseX >= x + sidebarWidth + 5 && mouseX <= x + sidebarWidth + moduleListWidth - 5
                && mouseY >= y + 11 && mouseY <= y + 29) {
            typingSearch = true; return;
        }
        typingSearch = false;
        List<Mod> mods = MeowClient.modManager.getByCategory(currentCategory);
        int modX = x + sidebarWidth + 10;
        int modY = y + 33;  // offset for search box
        if (mods != null) {
            for (Mod mod : mods) {
                if (!searchText.isEmpty() && !mod.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    continue;
                }
                if (mouseX >= modX && mouseX <= modX + moduleListWidth - 20 && mouseY >= modY - 4 && mouseY <= modY + 12) {
                    if (mouseButton == 0) mod.setEnable(!mod.isEnable());
                    else if (mouseButton == 1) currentMod = mod;
                    return;
                }
                modY += 20;
            }
        }
        if (currentMod != null) {
            int settingX = x + sidebarWidth + moduleListWidth + 20;
            int settingY = y + 35;
            for (Value<?> value : currentMod.getValues()) {
                if (!value.isVisible()) continue;
                if (value instanceof BooleanValue) {
                    if (mouseX >= settingX && mouseX <= settingX + 100 && mouseY >= settingY && mouseY <= settingY + 10) {
                        ((BooleanValue) value).setValue(!((BooleanValue) value).getValue());
                    }
                    settingY += 16;
                } else if (value instanceof ModeValue) {
                    if (mouseX >= settingX && mouseX <= settingX + 100 && mouseY >= settingY && mouseY <= settingY + 10) {
                        ((ModeValue) value).cycle();
                    }
                    settingY += 16;
                } else if (value instanceof NumberValue) settingY += 26;
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) { dragging = false; }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (typingSearch && typedChar != 0) {
            if (keyCode == org.lwjgl.input.Keyboard.KEY_BACK || keyCode == org.lwjgl.input.Keyboard.KEY_DELETE) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
            } else if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                searchText = "";
                typingSearch = false;
            } else if (typedChar >= 32 && typedChar < 127) {
                searchText += typedChar;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
