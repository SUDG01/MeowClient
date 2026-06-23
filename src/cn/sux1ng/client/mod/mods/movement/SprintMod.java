package cn.sux1ng.client.mod.mods.movement;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ModeValue;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class SprintMod extends Mod {

    // 疾跑模式: Legit = 仅向前 / Omni = 全方向
    public ModeValue mode = new ModeValue("Mode", "Legit", new String[]{"Legit", "Omni"});
    // 攻击后是否保持疾跑
    public BooleanValue keepSprint = new BooleanValue("KeepSprint", true);

    public SprintMod() {
        super("Sprint", Category.MOVEMENT);
        setKey(Keyboard.KEY_V);
        addValues(mode, keepSprint);
    }

    @Override
    public void update() {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.thePlayer == null || mc.theWorld == null) return;

        boolean isAttacking = mc.thePlayer.swingProgress > 0 &&
                mc.objectMouseOver != null &&
                mc.objectMouseOver.entityHit != null;

        boolean shouldSprint;

        if (mode.is("Omni")) {
            // 全方向疾跑：任意方向移动即可
            shouldSprint = (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0)
                    && !mc.thePlayer.isSneaking()
                    && !mc.thePlayer.isUsingItem()
                    && !mc.thePlayer.isCollidedHorizontally
                    && mc.thePlayer.getFoodStats().getFoodLevel() > 6
                    && !mc.thePlayer.isInWater()
                    && (keepSprint.getValue() ? true : !isAttacking);
        } else {
            // Legit 模式：仅向前疾跑
            shouldSprint = mc.thePlayer.moveForward > 0
                    && !mc.thePlayer.isSneaking()
                    && !mc.thePlayer.isUsingItem()
                    && !mc.thePlayer.isCollidedHorizontally
                    && mc.thePlayer.getFoodStats().getFoodLevel() > 6
                    && !mc.thePlayer.isInWater()
                    && (keepSprint.getValue() ? true : !isAttacking);
        }

        mc.thePlayer.setSprinting(shouldSprint);
    }
}
