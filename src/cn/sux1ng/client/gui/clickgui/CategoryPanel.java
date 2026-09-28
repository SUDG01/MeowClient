package cn.sux1ng.client.gui.clickgui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {
    public int x, y;
    private final Category category;
    private final int width = 94;
    private final int height = 24;

    private int prevX, prevY;
    private boolean press;
    private boolean hovered;
    private final List<ModPanel> modPanels = new ArrayList<>();
    private boolean displayMod = true;

    public CategoryPanel(int x, int y, Category category){
        this.x = x;
        this.y = y;
        this.category = category;
        for (Mod mod : MeowClient.modManager.getByCategory(category)) {
            modPanels.add(new ModPanel(mod));
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (press) {
            net.minecraft.client.gui.ScaledResolution screen =
                    new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft());
            x = Math.max(8, Math.min(screen.getScaledWidth() - width - 8, mouseX + prevX));
            y = Math.max(50, Math.min(screen.getScaledHeight() - 58 - height, mouseY + prevY));
        }

        hovered = mouseX >= x && mouseY >= y && mouseX < x + this.width && mouseY < this.y + this.height;

        MeowTheme.Palette theme = MeowTheme.current();
        DrawUtil.drawRoundedRect(x + 1, y + 2, width, height, 8, 0x45000000);
        DrawUtil.drawRoundedRect(x, y, width, height, 8, hovered ? theme.hover : theme.surface);
        DrawUtil.drawRoundedOutline(x, y, width, height, 8, 1, theme.outline);
        DrawUtil.drawRoundedRect(x + 6, y + 6, 3, height - 12, 2, theme.accent);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String title = ClientLanguage.category(category);
        fr.drawStringWithShadow(title, x + (width / 2 - fr.getStringWidth(title) / 2),
                y + (height / 2 - fr.FONT_HEIGHT / 2), theme.text);

        // 展开模块列表
        if (displayMod) {
            int currentY = y + height;
            for (ModPanel modPanel : modPanels) {
                modPanel.x = x;
                modPanel.y = currentY;
                modPanel.width = width;
                modPanel.drawScreen(mouseX, mouseY, partialTicks);
                currentY += modPanel.finalHeight;
            }
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton)  {
        if (hovered && mouseButton == 0){
            press = true;
            prevX = x - mouseX;
            prevY = y - mouseY;
        } else if (hovered && mouseButton == 1) {
            displayMod = !displayMod;
        }

        if (displayMod) {
            modPanels.forEach(it -> it.mouseClicked(mouseX, mouseY, mouseButton));
        }
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (displayMod) {
            for (ModPanel modPanel : modPanels) {
                if (modPanel.keyTyped(typedChar, keyCode)) return true;
            }
        }
        return false;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        press = false;
        if (displayMod) {
            modPanels.forEach(it -> it.mouseReleased(mouseX, mouseY, state));
        }
    }

    public int getContentHeight() {
        int total = height;
        if (displayMod) {
            for (ModPanel panel : modPanels) total += Math.max(panel.height, panel.finalHeight);
        }
        return total;
    }
}
