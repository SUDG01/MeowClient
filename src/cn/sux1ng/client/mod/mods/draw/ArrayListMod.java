package cn.sux1ng.client.mod.mods.draw;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.ColorUtil;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

public class ArrayListMod extends Mod {

    // 1. 颜色模式
    public ModeValue colorMode = new ModeValue("ColorMode", "Rainbow", new String[]{"Rainbow", "Astolfo", "Pulse", "Static"});

    // 2. 视觉开关
    public BooleanValue background = new BooleanValue("Background", true);
    public BooleanValue sidebar = new BooleanValue("Sidebar", true);

    // 3. 静态/Pulse 颜色 — 使用 ColorValue 替代 R/G/B 三滑块
    public ColorValue staticColor = new ColorValue("StaticColor", new Color(255, 105, 180))
            .setVisibility(() -> colorMode.is("Static") || colorMode.is("Pulse"));

    public ArrayListMod() {
        super("ArrayList", Category.DRAW);
        addValues(colorMode, background, sidebar, staticColor);
        setEnable(true);
    }

    @Override
    public void draw() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;

        List<Mod> enableMods = MeowClient.modManager.getEnableMods();
        // 长度长的在上面
        enableMods.sort((o1, o2) -> getWidth(o2) - getWidth(o1));

        int y = 2;
        int count = 0;

        for (Mod mod : enableMods) {
            String displayName = getDisplayName(mod);

            int moduleWidth = font.getStringWidth(displayName);
            int x = sr.getScaledWidth() - moduleWidth - 4;
            int height = font.FONT_HEIGHT + 2;

            // 根据模式获取颜色
            int color = 0;
            String mode = colorMode.getValue();

            switch (mode) {
                case "Rainbow":
                    color = ColorUtil.getRainbow(4000, count * 200);
                    break;
                case "Astolfo":
                    color = ColorUtil.getAstolfo(count * 200);
                    break;
                case "Pulse":
                    color = ColorUtil.getPulse(staticColor.getColor(), count, enableMods.size());
                    break;
                case "Static":
                    color = staticColor.getRGB();
                    break;
            }

            // 圆角背景
            if (background.getValue()) {
                cn.sux1ng.client.util.DrawUtil.drawRoundedRect(x - 3, y - 1, sr.getScaledWidth() - x + 3, height, 3, 0x60000000);
            }

            // 圆角侧边条
            if (sidebar.getValue()) {
                cn.sux1ng.client.util.DrawUtil.drawRoundedRect(sr.getScaledWidth() - 3, y - 1, 3, height, 1.5, color);
            }

            // 文字
            font.drawStringWithShadow(displayName, x, y, color);

            y += height;
            count++;
        }
        cn.sux1ng.client.events.EventManager.call(new cn.sux1ng.client.events.EventRender2D(1.0F));
    }

    private int getWidth(Mod mod) {
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(getDisplayName(mod));
    }

    private String getDisplayName(Mod mod) {
        return mod.getName();
    }
}
