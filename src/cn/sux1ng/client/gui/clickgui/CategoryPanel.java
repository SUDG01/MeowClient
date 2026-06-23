package cn.sux1ng.client.gui.clickgui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.DrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryPanel {
    public int x, y; // public 方便调试
    private final Category category;
    private final int width = 100; // 稍微宽一点，大气
    private final int height = 20;

    private int prevX, prevY;
    private boolean press;
    private boolean hovered;
    private final List<ModPanel> modPanels = new ArrayList<>();
    private boolean displayMod = true; // 默认展开

    public CategoryPanel(int x, int y, Category category){
        this.x = x;
        this.y = y;
        this.category = category;
        for (Mod mod : MeowClient.modManager.getByCategory(category)) {
            // 注意：这里传 this 进去是为了让 ModPanel 知道它的宽度 (如果你想改结构)
            // 但为了兼容你现在的 ModPanel，我们保持原样，只在下面 draw 的时候控制位置
            modPanels.add(new ModPanel(mod));
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 拖拽逻辑
        if (press) {
            x = mouseX + prevX;
            y = mouseY + prevY;
        }

        hovered = mouseX >= x && mouseY >= y && mouseX < x + this.width && mouseY < this.y + this.height;

        // --- Kawaii 渲染 ---

        // 樱花粉色标题栏 (0xFFFF9AA2)
        int headerColor = 0xFFFF9AA2;

        // 简单的标题栏
        DrawUtil.drawRect(x, y, width, height, headerColor);

        // 标题文字
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String title = category.name();

        // 加个可爱的小装饰，比如 "Combat" -> "Combat >w<" 或者单纯加粗
        // 这里简单居中
        fr.drawStringWithShadow(title, x + (width / 2 - fr.getStringWidth(title) / 2), y + (height / 2 - fr.FONT_HEIGHT / 2), 0xFFFFFFFF);

        // 如果展开，绘制下面的模块
        if(displayMod){
            int currentY = y + height; // 从标题栏下方开始

            for (ModPanel modPanel : modPanels) {
                // 设置当前 modPanel 的位置
                modPanel.x = x;
                modPanel.y = currentY;
                modPanel.width = width; // 确保宽度一致

                // 绘制
                modPanel.drawScreen(mouseX, mouseY, partialTicks);

                // 【核心修改】
                // 下一个按钮的 Y 坐标 = 当前 Y + 当前按钮的实际高度 (finalHeight)
                // 这样如果上面的按钮展开了，下面的按钮就会自动被顶下去
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
            // 播放个点击音效会更可爱 (可选)
            Minecraft.getMinecraft().thePlayer.playSound("random.click", 1, 1);
        }

        if(displayMod){
            modPanels.forEach(it -> it.mouseClicked(mouseX,mouseY,mouseButton));
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        // 如果面板是展开的，就把按键事件传给下面的每一个按钮
        if (displayMod) {
            for (ModPanel modPanel : modPanels) {
                modPanel.keyTyped(typedChar, keyCode);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        press = false;
        if(displayMod){
            modPanels.forEach(it -> it.mouseReleased(mouseX,mouseY,state));
        }
    }
}
