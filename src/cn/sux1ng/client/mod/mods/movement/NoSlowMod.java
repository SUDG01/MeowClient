package cn.sux1ng.client.mod.mods.movement;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;

/**
 * 无减速模块
 * 使用物品时（吃东西、喝药水、格挡等）抵消原版移动减速
 */
public class NoSlowMod extends Mod {

    // 模式: Vanilla 直接补偿 / NCP 仅地面补偿（减少反作弊检测）
    public ModeValue mode = new ModeValue("Mode", "Vanilla", new String[]{"Vanilla", "NCP"});
    // 减速补偿比例 (0.0=不补偿, 1.0=完全抵消减速)
    public NumberValue amount = new NumberValue("Amount", 1.0, 0.0, 1.0, 0.1);

    public NoSlowMod() {
        super("NoSlow", Category.MOVEMENT);
        addValues(mode, amount);
    }

    @Override
    public void update() {
        if (!isEnable()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // 只有正在使用物品才需要处理
        if (!mc.thePlayer.isUsingItem()) return;

        // 没有在移动就不需要补偿
        if (mc.thePlayer.moveForward == 0 && mc.thePlayer.moveStrafing == 0) return;

        // 原版使用物品时 moveForward/moveStrafing 会被乘以 0.2
        // 我们通过调整 motion 来补偿这个减速
        float compensation = 1.0f + (4.0f * amount.getValue().floatValue());
        // amount=1.0 → compensation=5.0 (完全抵消 0.2 倍减速)
        // amount=0.5 → compensation=3.0 (中等抵消)
        // amount=0.0 → compensation=1.0 (无效果)

        if (mode.is("Vanilla")) {
            mc.thePlayer.motionX *= compensation;
            mc.thePlayer.motionZ *= compensation;
        } else if (mode.is("NCP")) {
            // NCP 模式: 只在地面时补偿，空中不补偿以减少反作弊检测
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionX *= compensation;
                mc.thePlayer.motionZ *= compensation;
            }
        }
    }
}
