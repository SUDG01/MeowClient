package cn.sux1ng.client.mod.mods.movement;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;

public class NoJumpDelayMod extends Mod {

    // 是否移除跳跃延迟
    public BooleanValue removeDelay = new BooleanValue("RemoveDelay", true);

    public NoJumpDelayMod() {
        super("NoJumpDelay", Category.MOVEMENT);
        addValues(removeDelay);
    }

    @Override
    public void update() {
        if (!this.isEnable()) return;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        if (removeDelay.getValue()) {
            try {
                player.jumpTicks = 0;
            } catch (Exception ignored) { }
        }
    }
}
