package cn.sux1ng.client.mod.mods.draw;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogoMod extends Mod {
    public NumberValue x = new NumberValue("X", 4.0, 0.0, 1000.0, 1.0);
    public NumberValue y = new NumberValue("Y", 4.0, 0.0, 1000.0, 1.0);
    public ModeValue mode = new ModeValue("Mode", "Normal",
            new String[]{"Normal", "Sense", "CSGO(Gamesense)", "Simple"});

    public LogoMod() {
        super("Logo", Category.HUD);
        addValues(x, y, mode);
    }

    @Override
    public void draw() {
        FontRenderer font = mc.fontRendererObj;
        int left = x.getValue().intValue();
        int top = y.getValue().intValue();
        MeowTheme.Palette theme = MeowTheme.current();

        if (mode.is("Normal")) {
            int width = font.getStringWidth(MeowClient.NAME) + font.getStringWidth(MeowClient.VERSION) + 26;
            MeowTheme.panel(left, top, width, 23, 8);
            font.drawStringWithShadow(MeowClient.NAME, left + 9, top + 7, theme.text);
            font.drawString(MeowClient.VERSION, left + width - font.getStringWidth(MeowClient.VERSION) - 9,
                    top + 7, theme.muted);
        } else if (mode.is("Sense")) {
            font.drawStringWithShadow("M", left, top, theme.accent);
            font.drawStringWithShadow("eowClient " + MeowClient.VERSION,
                    left + font.getStringWidth("M"), top, theme.text);
        } else if (mode.is("CSGO(Gamesense)")) {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String text = MeowClient.NAME + " | " + Minecraft.getDebugFPS() + " FPS | " + time;
            int width = font.getStringWidth(text) + 14;
            DrawUtil.drawRoundedRect(left, top, width, 20, 7, theme.surface);
            DrawUtil.drawRoundedOutline(left, top, width, 20, 7, 1, theme.outline);
            DrawUtil.drawGradientHorizontal(left + 7, top + 2, width - 14, 2,
                    theme.accent, theme.secondary);
            font.drawStringWithShadow(text, left + 7, top + 7, theme.text);
        } else {
            font.drawStringWithShadow(MeowClient.NAME + " " + MeowClient.VERSION,
                    left, top, theme.text);
        }
    }
}
