package cn.sux1ng.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import java.awt.Color;

public class MeowButton extends GuiButton {

    // 动画过渡用的变量
    private float hoverOpacity = 0.0f;

    public MeowButton(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, buttonText);
    }

    public MeowButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            FontRenderer fontrenderer = mc.fontRendererObj;

            // 1. 判断悬停
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

            // 2. 动画逻辑 (保持不变)
            if (this.hovered) {
                if (hoverOpacity < 1.0f) hoverOpacity += 0.1f;
            } else {
                if (hoverOpacity > 0.0f) hoverOpacity -= 0.1f;
            }
            if (hoverOpacity > 1) hoverOpacity = 1;
            if (hoverOpacity < 0) hoverOpacity = 0;

            // 3. 绘制背景 (半透明黑)
            int baseColor = new Color(0, 0, 0, 100).getRGB();
            int hoverColor = new Color(30, 30, 30, 150).getRGB();

            // 绘制背景
            Gui.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, this.hovered ? hoverColor : baseColor);

            // 4. 绘制粉色边框 (呼吸效果)
            if (hoverOpacity > 0.1f) {
                int borderColor = new Color(255, 183, 178, (int)(255 * hoverOpacity)).getRGB(); // 樱花粉

                Gui.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + 1, borderColor); // 上
                Gui.drawRect(this.xPosition, this.yPosition + this.height - 1, this.xPosition + this.width, this.yPosition + this.height, borderColor); // 下
                Gui.drawRect(this.xPosition, this.yPosition, this.xPosition + 1, this.yPosition + this.height, borderColor); // 左
                Gui.drawRect(this.xPosition + this.width - 1, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, borderColor); // 右
            }

            // 5. 拖动处理 (保持原版逻辑)
            this.mouseDragged(mc, mouseX, mouseY);

            // 6. 【修复】文字颜色逻辑
            int textColor = 14737632; // 默认灰白色

            if (!this.enabled) {
                textColor = 10526880; // 不可用时深灰
            } else if (this.hovered) {
                // 悬停时：变成樱花粉 (和你老婆的背景更配！)
                // 如果觉得粉色字看不清，可以改回 16777120 (原版亮黄) 或 0xFFFFFFFF (纯白)
                textColor = 0xFFFFB7B2;
            }

            // 绘制文字
            this.drawCenteredString(fontrenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, textColor);
        }
    }
}