package cn.sux1ng.client.gui.clickgui;

import cn.sux1ng.client.mod.Category;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends GuiScreen {

    private final List<CategoryPanel> categoryPanels = new ArrayList<>();

    public ClickGUI(){
        int x = 20; // 起始 X 坐标
        // 遍历所有分类
        for (Category value : Category.values()) {
            // 每个面板间隔 105 像素 (宽度80 + 间距25)
            categoryPanels.add(new CategoryPanel(x, 20, value));
            x += 105;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 画一个全屏的半透明黑色背景，让游戏画面变暗，突出 GUI (可选)
        // drawDefaultBackground();

        categoryPanels.forEach(it -> it.drawScreen(mouseX, mouseY, partialTicks));
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ... 下面的 mouseClicked, mouseReleased, doesGuiPauseGame 保持不变 ...
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        categoryPanels.forEach(it -> it.mouseClicked(mouseX,mouseY,mouseButton));
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        categoryPanels.forEach(it -> it.mouseReleased(mouseX,mouseY,state));
        super.mouseReleased(mouseX, mouseY, state);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // 把事件传给每一个分类面板
        for (CategoryPanel panel : categoryPanels) {
            panel.keyTyped(typedChar, keyCode);
        }

        // 记得保留父类的方法，处理 ESC 关闭界面等逻辑
        super.keyTyped(typedChar, keyCode);
    }
}