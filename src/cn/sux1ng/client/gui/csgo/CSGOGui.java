package cn.sux1ng.client.gui.csgo;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.draw.ClickGUIMod; // 导入这个
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import cn.sux1ng.client.value.Value;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

public class CSGOGui extends GuiScreen {

    // 窗口参数
    private int x = 100, y = 100;
    private int width = 450, height = 320;
    private int sidebarWidth = 80;
    private int moduleListWidth = 140;

    private Category currentCategory = Category.COMBAT;
    private Mod currentMod = null;
    private boolean dragging = false;
    private int dragX, dragY;

    // 动态获取颜色
    private int getBgColor() {
        if (ClickGUIMod.theme.is("Light")) return 0xFFF0F0F0; // 亮白
        return new Color(29, 29, 29).getRGB(); // 深灰
    }

    private int getSidebarColor() {
        if (ClickGUIMod.theme.is("Light")) return 0xFFE0E0E0; // 浅灰
        return new Color(24, 24, 24).getRGB(); // 更深灰
    }

    private int getAccentColor() {
        if (ClickGUIMod.theme.is("Skeet")) return new Color(150, 200, 0).getRGB(); // Skeet 绿
        if (ClickGUIMod.theme.is("Light")) return 0xFF007BFF; // 科技蓝
        return 0xFFFFB7B2; // Kawaii 粉
    }

    private int getTextColor() {
        if (ClickGUIMod.theme.is("Light")) return 0xFF333333; // 深灰字
        return 0xFFE0E0E0; // 亮白字
    }

    private int getDisableTextColor() {
        if (ClickGUIMod.theme.is("Light")) return 0xFF888888;
        return 0xFF707070;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 获取当前主题色
        int BG = getBgColor();
        int SIDEBAR = getSidebarColor();
        int ACCENT = getAccentColor();
        int TEXT = getTextColor();
        int TEXT_DIS = getDisableTextColor();

        // 1. 背景
        Gui.drawRect(x, y, x + width, y + height, BG);
        Gui.drawRect(x, y, x + sidebarWidth, y + height, SIDEBAR);

        // 分隔线 (根据模式微调颜色，Light模式用深一点的灰)
        int lineColor = ClickGUIMod.theme.is("Light") ? 0xFFAAAAAA : 0xFF353535;
        Gui.drawRect(x + sidebarWidth, y, x + sidebarWidth + 1, y + height, lineColor);
        Gui.drawRect(x + sidebarWidth + moduleListWidth, y + 10, x + sidebarWidth + moduleListWidth + 1, y + height - 10, lineColor);

        // 顶部装饰条
        Gui.drawRect(x, y, x + width, y + 2, ACCENT);

        // Logo
        fontRendererObj.drawString("Meow", x + 10, y + 10, ACCENT);
        // Light 模式下 Client 字样用深色
        fontRendererObj.drawString("Client", x + 10 + fontRendererObj.getStringWidth("Meow"), y + 10, TEXT);

        // 2. 左侧：分类
        int catY = y + 40;
        for (Category category : Category.values()) {
            boolean selected = category == currentCategory;
            if (selected) Gui.drawRect(x, catY - 4, x + 2, catY + 12, ACCENT);
            fontRendererObj.drawString(category.name(), x + 15, catY, selected ? ACCENT : TEXT_DIS);
            catY += 25;
        }

        // 3. 中间：功能列表
        List<Mod> mods = MeowClient.modManager.getByCategory(currentCategory);
        int modX = x + sidebarWidth + 10;
        int modY = y + 15;
        if (mods != null) {
            for (Mod mod : mods) {
                if (mod == currentMod) {
                    // 选中高亮色：Light模式用浅灰，Dark模式用深灰
                    int hoverColor = ClickGUIMod.theme.is("Light") ? 0xFFFFFFFF : 0xFF2A2A2A;
                    Gui.drawRect(x + sidebarWidth + 5, modY - 4, x + sidebarWidth + moduleListWidth - 5, modY + 12, hoverColor);
                }
                fontRendererObj.drawString(mod.getName(), modX, modY, mod.isEnable() ? TEXT : TEXT_DIS);
                if (mod.isEnable()) Gui.drawRect(x + sidebarWidth + moduleListWidth - 15, modY + 2, x + sidebarWidth + moduleListWidth - 11, modY + 6, ACCENT);
                modY += 20;
            }
        }

        // 4. 右侧：设置
        int settingX = x + sidebarWidth + moduleListWidth + 20;
        int settingY = y + 35;
        if (currentMod != null) {
            fontRendererObj.drawString(currentMod.getName() + " Settings", settingX, y + 10, TEXT);
            Gui.drawRect(settingX, y + 22, settingX + 100, y + 23, ACCENT);

            if (currentMod.getValues().isEmpty()) {
                fontRendererObj.drawString("No settings :(", settingX, settingY, TEXT_DIS);
            } else {
                for (Value<?> value : currentMod.getValues()) {
                    if (!value.isVisible()) continue;

                    if (value instanceof BooleanValue) {
                        BooleanValue bool = (BooleanValue) value;
                        // 框框颜色
                        int boxColor = ClickGUIMod.theme.is("Light") ? 0xFFAAAAAA : 0xFF404040;
                        Gui.drawRect(settingX, settingY, settingX + 10, settingY + 10, boxColor);
                        if (bool.getValue()) Gui.drawRect(settingX + 2, settingY + 2, settingX + 8, settingY + 8, ACCENT);
                        fontRendererObj.drawString(bool.getName(), settingX + 16, settingY + 1, TEXT);
                        settingY += 16;
                    }
                    else if (value instanceof NumberValue) {
                        NumberValue num = (NumberValue) value;
                        fontRendererObj.drawString(num.getName() + ": " + num.getValue(), settingX, settingY, TEXT);
                        settingY += 12;
                        int barBg = ClickGUIMod.theme.is("Light") ? 0xFFAAAAAA : 0xFF404040;
                        Gui.drawRect(settingX, settingY, settingX + 120, settingY + 4, barBg);
                        double percent = (num.getValue() - num.getMin()) / (num.getMax() - num.getMin());
                        Gui.drawRect(settingX, settingY, settingX + (int)(120 * percent), settingY + 4, ACCENT);

                        // 拖动
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
                        fontRendererObj.drawString(mode.getValue(), settingX + fontRendererObj.getStringWidth(mode.getName() + ":") + 4, settingY, ACCENT);
                        settingY += 16;
                    }
                }
            }
        } else {
            fontRendererObj.drawString("Select a module", settingX, y + height / 2 - 10, TEXT_DIS);
        }

        // 拖动逻辑
        if (dragging) { x = mouseX - dragX; y = mouseY - dragY; }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ... mouseClicked 等逻辑保持不变 ...
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // 记得在这里也要加上拖动滑块的逻辑判断，或者像上面 drawScreen 那样直接处理
        // 复制之前 CSGOGui 的 mouseClicked 逻辑即可
        // 唯一注意的是：处理点击分类和模块时，不需要改动，因为位置逻辑没变

        // ... (此处省略重复代码，请保留原来的点击逻辑) ...

        // 必须补全拖动和分类点击逻辑，否则点不动哦！
        // 1. 窗口拖动
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
            dragging = true; dragX = mouseX - x; dragY = mouseY - y; return;
        }
        // 2. 分类点击
        int catY = y + 40;
        for (Category category : Category.values()) {
            if (mouseX >= x && mouseX <= x + sidebarWidth && mouseY >= catY - 5 && mouseY <= catY + 15) {
                currentCategory = category; currentMod = null; return;
            }
            catY += 25;
        }
        // 3. 模块点击
        List<Mod> mods = MeowClient.modManager.getByCategory(currentCategory);
        int modX = x + sidebarWidth + 10;
        int modY = y + 15;
        if (mods != null) {
            for (Mod mod : mods) {
                if (mouseX >= modX && mouseX <= modX + moduleListWidth - 20 && mouseY >= modY - 4 && mouseY <= modY + 12) {
                    if (mouseButton == 0) mod.setEnable(!mod.isEnable());
                    else if (mouseButton == 1) currentMod = mod;
                    return;
                }
                modY += 20;
            }
        }
        // 4. 设置点击 (略，同上)
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
    public boolean doesGuiPauseGame() { return false; }
}