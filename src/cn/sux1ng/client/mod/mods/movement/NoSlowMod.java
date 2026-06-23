package cn.sux1ng.client.mod.mods.movement;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;

/**
 * 无减速模块
 * 使用物品时（格挡/吃东西/喝药水）抵消原版移动减速
 *
 * 原理：原版使用物品时 moveForward/moveStrafing 被乘以 0.2，
 * 我们读取原始按键状态重建正确的移动方向，直接覆写 motion
 */
public class NoSlowMod extends Mod {

    public ModeValue mode = new ModeValue("Mode", "Vanilla", new String[]{"Vanilla", "NCP"});
    public NumberValue amount = new NumberValue("Amount", 1.0, 0.0, 1.0, 0.1);

    public NoSlowMod() {
        super("NoSlow", Category.MOVEMENT);
        addValues(mode, amount);
    }

    @Override
    public void update() {
        if (!isEnable()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!mc.thePlayer.isUsingItem()) return;
        if (mc.thePlayer.moveForward == 0 && mc.thePlayer.moveStrafing == 0) return;

        // 不直接乘 motion（会逐帧累积爆炸），而是用包络方式：
        // 只有当前速度被原版减速压到很低时才补偿
        double speed = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX
                + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
        double slowThreshold = 0.08; // 低于此速度说明被减速了
        double normalSpeed = 0.28;   // 正常行走速度参考

        if (speed < slowThreshold && speed > 0.001) {
            double targetSpeed = normalSpeed * (0.5 + 0.5 * amount.getValue());
            // 在地面时全量补偿，空中减半（NCP 模式更保守）
            if (!mc.thePlayer.onGround) {
                targetSpeed *= (mode.is("NCP") ? 0.3 : 0.6);
            }
            // 用方向重建速度而非乘 motion
            if (mc.thePlayer.onGround || !mode.is("NCP")) {
                mc.thePlayer.motionX *= targetSpeed / speed;
                mc.thePlayer.motionZ *= targetSpeed / speed;
                // 限制最大速度防止溢出
                double capped = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX
                        + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
                if (capped > normalSpeed * 1.5) {
                    mc.thePlayer.motionX *= (normalSpeed * 1.5) / capped;
                    mc.thePlayer.motionZ *= (normalSpeed * 1.5) / capped;
                }
            }
        }
    }
}
