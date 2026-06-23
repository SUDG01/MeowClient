package cn.sux1ng.client.mod.mods.draw;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.DrawUtil; // 需要用到你的画图工具
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogoMod extends Mod {

    // 1. 位置参数 (范围给大一点，方便拖到屏幕任意位置)
    public NumberValue x = new NumberValue("X", 4.0, 0.0, 1000.0, 1.0);
    public NumberValue y = new NumberValue("Y", 4.0, 0.0, 1000.0, 1.0);

    // 2. 模式选择
    public ModeValue mode = new ModeValue("Mode", "Normal", new String[]{"Normal", "Sense", "CSGO(Gamesense)", "Simple"});

    public LogoMod() {
        super("Logo", Category.DRAW);
        addValues(x, y, mode);
    }

    @Override
    public void draw() {
        FontRenderer fr = mc.fontRendererObj;

        // 获取当前设置的坐标
        float posX = x.getValue().floatValue();
        float posY = y.getValue().floatValue();

        String currentMode = mode.getValue();

        // --- 模式 1: Normal (你原来的风格) ---
        if (currentMode.equals("Normal")) {
            GL11.glPushMatrix();
            // 先移动到指定位置
            GL11.glTranslated(posX, posY, 0);
            // 再放大
            GL11.glScalef(1.5f, 1.5f, 1.5f);

            // 此时坐标 0,0 就是我们设定的 posX, posY
            fr.drawStringWithShadow("Meow", 0, 0, 0xFF69B4); // 深粉色
            fr.drawStringWithShadow("Client", fr.getStringWidth("Meow"), 0, -1); // 白色

            GL11.glPopMatrix();

            // 版本号画在下面 (因为上面放大了1.5倍，大概占了14高度，所以这里+14)
            fr.drawStringWithShadow("Ver: " + MeowClient.VERSION, posX + 2, posY + 14, 0xAAAAAA);
        }

        // --- 模式 2: Sense (首字母变色风格) ---
        else if (currentMode.equals("Sense")) {
            // M (粉色)
            fr.drawStringWithShadow("M", posX, posY, 0xFF69B4);
            // eowClient (白色)
            fr.drawStringWithShadow("eowClient " + MeowClient.VERSION, posX + fr.getStringWidth("M"), posY, -1);
        }

        // --- 模式 3: CSGO (黑底 + 顶部线条 + 时间) ---
        else if (currentMode.equals("CSGO")) {
            // 准备文字： MeowClient | FPS | 12:30:45
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String text = "MeowClient | " + Minecraft.getDebugFPS() + " fps | " + time;

            float width = fr.getStringWidth(text) + 6;
            float height = 12;

            // 画圆角背景
            DrawUtil.drawRoundedRect(posX, posY, width, height, 3, 0x80000000);
            // 画顶部圆角线条
            DrawUtil.drawRoundedRect(posX, posY, width, 2, 1, 0xFFFFB7B2);

            // 画文字 (居中一点)
            fr.drawStringWithShadow(text, posX + 3, posY + 3, -1);
        }

        // --- 模式 4: Simple (纯文本) ---
        else if (currentMode.equals("Simple")) {
            fr.drawStringWithShadow("MeowClient " + MeowClient.VERSION, posX, posY, 0xFFFFFF);
        }
    }
}