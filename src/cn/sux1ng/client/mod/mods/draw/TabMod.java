package cn.sux1ng.client.mod.mods.draw;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;

public class TabMod extends Mod {
    // 选中/高亮颜色（樱花粉）
    public ColorValue accentColor = new ColorValue("AccentColor", new Color(246, 175, 203));
    // 背景颜色（半透明黑）
    public ColorValue bgColor = new ColorValue("BackgroundColor", new Color(38, 34, 55, 190));
    // 整体缩放
    public NumberValue uiScale = new NumberValue("Scale", 1.0, 0.5, 2.0, 0.1);

    private int currentCategory = 0;
    private int currentMod = 0;
    private boolean mod = false;

    public TabMod() {
        super("Tab", Category.HUD);
        addValues(accentColor, bgColor, uiScale);
    }

    @Override
    public void draw() {
        drawTab(2, 35);
    }

    public boolean isModulesOpen() { return mod; }
    public int selectedCategory() { return currentCategory; }

    @Override
    public void key(int key) {
        if (!mod) {
            if (key == Keyboard.KEY_UP) {
                currentCategory = (currentCategory > 0) ? currentCategory - 1 : Category.values().length - 1;
            } else if (key == Keyboard.KEY_DOWN) {
                currentCategory = (currentCategory < Category.values().length - 1) ? currentCategory + 1 : 0;
            } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_RIGHT) {
                mod = true;
                currentMod = 0;
            }
        } else {
            List<Mod> mods = MeowClient.modManager.getByCategory(Category.values()[currentCategory]);
            if (key == Keyboard.KEY_UP) {
                currentMod = (currentMod > 0) ? currentMod - 1 : mods.size() - 1;
            } else if (key == Keyboard.KEY_DOWN) {
                currentMod = (currentMod < mods.size() - 1) ? currentMod + 1 : 0;
            } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_RIGHT) {
                Mod m = mods.get(currentMod);
                m.setEnable(!m.isEnable());
            } else if (key == Keyboard.KEY_LEFT) {
                mod = false;
            }
        }
    }

    private void drawTab(int x, int y) {
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        int height = (int) (12 * uiScale.getValue().floatValue());

        // 计算分类面板宽度
        int categoryWidth = 0;
        for (Category c : Category.values()) {
            int w = font.getStringWidth(ClientLanguage.category(c)) + 12;
            if (w > categoryWidth) categoryWidth = w;
        }

        // 背景
        DrawUtil.drawRoundedRect(x, y, categoryWidth, Category.values().length * height, 6,
                bgColor.getRGB());

        // 选中条
        int selectedY = y + currentCategory * height;
        DrawUtil.drawRoundedRect(x + 2, selectedY + 1, categoryWidth - 4, height - 2, 4,
                MeowTheme.withAlpha(accentColor.getRGB(), 150));

        // 绘制文字
        for (int i = 0; i < Category.values().length; i++) {
            Category category = Category.values()[i];
            String name = ClientLanguage.category(category);
            int color = (i == currentCategory) ? MeowTheme.current().text : MeowTheme.current().muted;
            font.drawStringWithShadow(name, x + 4, y + i * height + (height / 2f - font.FONT_HEIGHT / 2f), color);
        }

        // 绘制模块列表
        if (mod) {
            int modX = x + categoryWidth + 2;
            int modY = selectedY;

            List<Mod> mods = MeowClient.modManager.getByCategory(Category.values()[currentCategory]);

            int modWidth = 0;
            for (Mod m : mods) {
                int w = font.getStringWidth(ClientLanguage.module(m)) + 12;
                if (w > modWidth) modWidth = w;
            }

            DrawUtil.drawRoundedRect(modX, modY, modWidth, mods.size() * height, 6, bgColor.getRGB());

            DrawUtil.drawRoundedRect(modX + 2, modY + currentMod * height + 1,
                    modWidth - 4, height - 2, 4, MeowTheme.withAlpha(accentColor.getRGB(), 150));

            for (int i = 0; i < mods.size(); i++) {
                Mod m = mods.get(i);
                int color;
                if (i == currentMod) {
                    color = MeowTheme.current().text;
                } else {
                    color = m.isEnable() ? MeowTheme.current().accent : MeowTheme.current().muted;
                }
                font.drawStringWithShadow(ClientLanguage.module(m), modX + 5,
                        modY + i * height + (height / 2f - font.FONT_HEIGHT / 2f), color);
            }
        }
    }

}
