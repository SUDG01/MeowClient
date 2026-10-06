package cn.sux1ng.client.mod.mods.combat;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import cn.sux1ng.client.util.TimerUtil;
import cn.sux1ng.client.targeting.TargetRules;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class AutoClickerMod extends Mod {

    // 1. 模式选择
    public ModeValue mode = new ModeValue("Mode", "Legit", new String[]{"Fixed", "Legit"});

    // 2. CPS 设置
    public NumberValue cps = new NumberValue("CPS", 10.0, 1.0, 20.0, 1.0)
            .setVisibility(() -> mode.is("Fixed"));

    public NumberValue minCps = new NumberValue("MinCPS", 8.0, 1.0, 20.0, 1.0)
            .setVisibility(() -> mode.is("Legit"));

    public NumberValue maxCps = new NumberValue("MaxCPS", 12.0, 1.0, 20.0, 1.0)
            .setVisibility(() -> mode.is("Legit"));


    public BooleanValue blockHit = new BooleanValue("BlockHit", false);

    private TimerUtil timer = new TimerUtil();

    public AutoClickerMod() {
        super("AutoClicker", Category.COMBAT);
        // 记得把 blockHit 加入参数列表
        addValues(mode, cps, minCps, maxCps, blockHit);
    }

    @Override
    public void update() {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && !TargetRules.canAttack(mc.objectMouseOver.entityHit)) { timer.reset(); return; }
        if (cn.sux1ng.client.MeowClient.modManager != null) {
            KillAuraMod aura = cn.sux1ng.client.MeowClient.modManager.getByClass(KillAuraMod.class);
            if (aura != null && aura.isEnable() && aura.getTarget() != null) { timer.reset(); return; }
        }
        // 只有按住左键才工作
        if (mc.gameSettings.keyBindAttack.isKeyDown()) {

            // 计算 CPS
            double currentCps;
            if (mode.is("Fixed")) {
                currentCps = cps.getValue();
            } else {
                double min = minCps.getValue();
                double max = maxCps.getValue();
                if (min > max) min = max;
                currentCps = min + (Math.random() * (max - min));
            }

            long delay = (long) (1000 / currentCps);

            if (timer.hasTimePassed(delay)) {

                // 检查是否正在防御 (手里拿剑 + 按住右键)
                boolean isBlocking = mc.thePlayer.isUsingItem()
                        && mc.thePlayer.getHeldItem() != null
                        && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;

                // ================= 安全检查 =================
                // 如果正在防御，并且用户【没开启】BlockHit 功能
                // 那就直接停止攻击，什么都不做，跳过这次点击
                if (isBlocking && !blockHit.getValue()) {
                    return;
                }
                // ===========================================

                // 如果代码能走到这里，说明要么没防御，要么开启了 BlockHit

                if (isBlocking) {
                    // 发包：假装松手
                    mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                    ));
                }

                // 物理攻击
                mc.leftClickCounter = 0;
                mc.clickMouse();

                if (isBlocking) {
                    // 发包：假装又挡住了
                    mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(
                            mc.thePlayer.inventory.getCurrentItem()
                    ));
                }

                timer.reset();
            }
        }
    }
}
