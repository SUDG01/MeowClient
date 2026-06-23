package cn.sux1ng.client.gui.clickgui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.DrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {
    public int x, y;
    private final Category category;
    private final int width = 100;
    private final int height = 20;

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
            x = mouseX + prevX;
            y = mouseY + prevY;
        }

        hovered = mouseX >= x && mouseY >= y && mouseX < x + this.width && mouseY < this.y + this.height;

        // 圆角标题栏
        int headerColor = hovered ? 0xFFFFC2CA : 0xFFFF9AA2;
        DrawUtil.drawRoundedRect(x, y, width, height, 4, headerColor);
        // 标题栏下方直角收尾
        DrawUtil.drawRect(x, y + height - 4, width, 4, headerColor);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String title = category.name();
        fr.drawStringWithShadow(title, x + (width / 2 - fr.getStringWidth(title) / 2),
                y + (height / 2 - fr.FONT_HEIGHT / 2), 0xFFFFFFFF);

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
            Minecraft.getMinecraft().thePlayer.playSound("random.click", 1, 1);
        }

        if (displayMod) {
            modPanels.forEach(it -> it.mouseClicked(mouseX, mouseY, mouseButton));
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (displayMod) {
            for (ModPanel modPanel : modPanels) {
                modPanel.keyTyped(typedChar, keyCode);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        press = false;
        if (displayMod) {
            modPanels.forEach(it -> it.mouseReleased(mouseX, mouseY, state));
        }
    }
}
