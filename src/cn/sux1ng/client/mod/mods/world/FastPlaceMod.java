package cn.sux1ng.client.mod.mods.world;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;

public class FastPlaceMod extends Mod {

    // 自定义放置延迟（0=最快，4=原版默认）
    public NumberValue delayTicks = new NumberValue("Delay", 0.0, 0.0, 4.0, 1.0);

    public FastPlaceMod() {
        super("FastPlace", Category.WORLD);
        addValues(delayTicks);
    }

    @Override
    public void update() {
        if (Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() != null) {
            if (Minecraft.getMinecraft().thePlayer.getCurrentEquippedItem().getItem() instanceof ItemBlock) {
                Minecraft.getMinecraft().rightClickDelayTimer = delayTicks.getValue().intValue();
            } else {
                Minecraft.getMinecraft().rightClickDelayTimer = 4;
            }
        }
    }
}
