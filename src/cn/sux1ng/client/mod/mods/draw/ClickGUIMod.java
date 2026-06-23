package cn.sux1ng.client.mod.mods.draw;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import org.lwjgl.input.Keyboard;

public class ClickGUIMod extends Mod {

    // 这里定义两种风格：Dropdown (下拉式-你现在的), CSGO (窗口式-新做的)
    public static ModeValue style = new ModeValue("Style", "CSGO", new String[]{"Dropdown", "CSGO"});
    public static ModeValue theme = new ModeValue("Theme", "MeowClient", new String[]{"MeowClient", "Skeet", "Light"});

    public ClickGUIMod() {
        super("ClickGUI", Category.DRAW);
        setKey(Keyboard.KEY_RSHIFT);
        addValues(style,theme);
    }

    @Override
    public void enable() {
        // 根据当前的 style 决定打开哪个 GUI
        if (style.is("Dropdown")) {
            mc.displayGuiScreen(MeowClient.clickGUI); // 你原来的那个
        } else if (style.is("CSGO")) {
            // 我们马上要新建这个 CSGOGui 类
            mc.displayGuiScreen(new cn.sux1ng.client.gui.csgo.CSGOGui());
        }

        // 打开 GUI 后立刻把自己关掉，防止无限循环
        this.setEnable(false);
    }
}
