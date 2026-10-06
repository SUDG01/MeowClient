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
    // 是否显示其他玩家的 OptiFine 披风
    public BooleanValue optifine = new BooleanValue("ShowOptiFine", true);
    // 内置 Meow 披风的颜色；本地图片保留自身颜色
    public ColorValue capeColor = new ColorValue("CapeColor", new Color(255, 105, 180))
            .setVisibility(() -> capeStyle.is("Meow") && CapeManager.localCape == null);

    public CapeMod() {
        super("CustomCape", Category.PLAYER);
        addValues(capeStyle, optifine, capeColor);
    }

    @Override
    public void enable() {
        update();
        CapeManager.loadLocalCape();
        CapeManager.ENABLED = true;
    }

    @Override
    public void disable() {
        CapeManager.ENABLED = false;
    }

    @Override
    public void update() {
        CapeManager.configure(capeStyle.getValue(), optifine.getValue(), capeColor.getColor().getRGB());
    }

    @Override public void render(float partialTicks) { update(); }
}
