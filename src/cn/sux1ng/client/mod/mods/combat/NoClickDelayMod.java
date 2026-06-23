package cn.sux1ng.client.mod.mods.combat;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import net.minecraft.client.Minecraft;

public class NoClickDelayMod extends Mod {

    private final Minecraft mc = Minecraft.getMinecraft();

    // 仅在按住攻击键时移除延迟（开启: 安全模式 / 关闭: 始终移除）
    public BooleanValue onlyWhileAttacking = new BooleanValue("OnlyWhileAttacking", true);

    public NoClickDelayMod() {
        super("NoClickDelay", Category.COMBAT);
        addValues(onlyWhileAttacking);
    }

    @Override
    public void update() {
        if(!isEnable()) return;
        if(mc.thePlayer != null && mc.theWorld != null){
            if (onlyWhileAttacking.getValue()) {
                // 仅在按住攻击键时移除延迟
                if(mc.gameSettings.keyBindAttack.isKeyDown()) {
                    mc.leftClickCounter = 0;
                }
            } else {
                // 始终移除点击延迟
                mc.leftClickCounter = 0;
            }
        }
    }
}
