package cn.sux1ng.client.mod.mods.player;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.NumberValue;

/**
 * SkinDerp — 皮肤层闪烁
 * 随机开关玩家皮肤覆盖层（帽子/外套/袖子/裤子/披风）
 */
public class SkinDerpMod extends Mod {

    public NumberValue speed = new NumberValue("Speed", 5, 1, 20, 1);
    public BooleanValue hat = new BooleanValue("Hat", true);
    public BooleanValue jacket = new BooleanValue("Jacket", true);
    public BooleanValue sleeve = new BooleanValue("Sleeve", true);
    public BooleanValue pants = new BooleanValue("Pants", true);
    public BooleanValue cape = new BooleanValue("Cape", false);

    private int tick = 0;
    private boolean on = true;

    public SkinDerpMod() {
        super("SkinDerp", Category.PLAYER);
        addValues(speed, hat, jacket, sleeve, pants, cape);
    }

    @Override
    public void update() {
        if (mc.thePlayer == null) return;

        tick++;
        int interval = Math.max(1, (int)(20 - speed.getValue()));
        if (tick % interval == 0) {
            on = !on;
        }
    }

    /**
     * 供外部在渲染时查询某层是否可见
     * 在 PlayerRenderer 渲染第二层时调用
     */
    public boolean shouldShowLayer(String layer) {
        if (!isEnable()) return true; // 关闭时显示全部
        switch (layer) {
            case "hat": return hat.getValue() && on;
            case "jacket": return jacket.getValue() && on;
            case "sleeve": return sleeve.getValue() && on;
            case "pants": return pants.getValue() && on;
            case "cape": return cape.getValue() && on;
            default: return true;
        }
    }
}
