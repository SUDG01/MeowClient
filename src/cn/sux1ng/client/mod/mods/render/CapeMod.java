package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.CapeManager;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.ModeValue;

import java.awt.Color;

public class CapeMod extends Mod {

    // 披风样式
    public ModeValue capeStyle = new ModeValue("Style", "Meow", new String[]{"Meow", "OptiFine", "Minecon"});
    // 显示 OptiFine 披风
    public BooleanValue optifine = new BooleanValue("ShowOptiFine", true);
    // 自定义披风颜色（Meow 样式时生效）
    public ColorValue capeColor = new ColorValue("CapeColor", new Color(255, 105, 180))
            .setVisibility(() -> capeStyle.is("Meow"));

    public CapeMod() {
        super("CustomCape", Category.RENDER);
        addValues(capeStyle, optifine, capeColor);
    }

    @Override
    public void enable() {
        CapeManager.ENABLED = true;
        CapeManager.loadLocalCape();
    }

    @Override
    public void disable() {
        CapeManager.ENABLED = false;
    }

    @Override
    public void update() {
        CapeManager.OPTIFINE_MODE = optifine.getValue();
    }
}
