package cn.sux1ng.client.mod.mods.movement;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.potion.Potion;

public class SpeedMod extends Mod {

    public ModeValue mode = new ModeValue("Mode", "AutoJump", new String[]{"AutoJump", "Vanilla", "NCP"});
    public NumberValue speed = new NumberValue("Speed", 1.0, 0.1, 10.0, 0.1);

    // NCP 模式需要的变量
    private double moveSpeed;
    private double lastDist;

    public SpeedMod() {
        super("Speed", Category.MOVEMENT);
        addValues(mode, speed);
    }


    @Override
    public void enable() {
        super.enable(); // 调用父类的 enable (虽然是空的，但保持习惯是个好事情)

        // 初始化变量
        moveSpeed = getBaseMoveSpeed();
        lastDist = 0;
    }

    @Override
    public void disable() {
        super.disable(); // 调用父类的 disable

        // 还原计时器速度
        mc.timer.timerSpeed = 1.0F;
    }
    // ===================================================

    @Override
    public void update() {
        if (!isEnable()) return;

        String currentMode = mode.getValue();

        // 1. AutoJump (保持不变)
        if (currentMode.equals("AutoJump")) {
            if (isMoving() && mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }
        }

        // 2. Vanilla (保持不变)
        else if (currentMode.equals("Vanilla")) {
            if (isMoving()) {
                if (mc.thePlayer.onGround) mc.thePlayer.jump();
                setMoveSpeed(speed.getValue());
            } else {
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionZ = 0;
            }
        }

        // 3. NCP 模式
        else if (currentMode.equals("NCP")) {
            if (!isMoving()) {
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionZ = 0;
                moveSpeed = getBaseMoveSpeed();
                return;
            }

            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                moveSpeed = getBaseMoveSpeed() * 1.7; // 爆发速度
            } else {
                moveSpeed = lastDist - lastDist / 159.0; // 空气阻力
            }

            moveSpeed = Math.max(moveSpeed, getBaseMoveSpeed());
            setMoveSpeed(moveSpeed);

            double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
            double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
            lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
        }
    }

    // 计算基础速度 (考虑药水)
    private double getBaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if (mc.thePlayer != null && mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            int amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= (1.0 + 0.2 * (amplifier + 1));
        }
        return baseSpeed;
    }

    // 判断移动
    private boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.moveForward != 0f || mc.thePlayer.moveStrafing != 0f);
    }

    // 设置速度
    private void setMoveSpeed(double moveSpeed) {
        float forward = mc.thePlayer.moveForward;
        float strafe = mc.thePlayer.moveStrafing;
        float yaw = mc.thePlayer.rotationYaw;

        if (forward == 0.0F && strafe == 0.0F) {
            mc.thePlayer.motionX = 0.0D;
            mc.thePlayer.motionZ = 0.0D;
        } else {
            if (forward != 0.0F) {
                if (strafe > 0.0F) {
                    yaw += (float)(forward > 0.0F ? -45 : 45);
                } else if (strafe < 0.0F) {
                    yaw += (float)(forward > 0.0F ? 45 : -45);
                }
                strafe = 0.0F;
                if (forward > 0.0F) {
                    forward = 1.0F;
                } else if (forward < 0.0F) {
                    forward = -1.0F;
                }
            }
            double cos = Math.cos(Math.toRadians(yaw + 90.0F));
            double sin = Math.sin(Math.toRadians(yaw + 90.0F));
            mc.thePlayer.motionX = (forward * moveSpeed * cos + strafe * moveSpeed * sin);
            mc.thePlayer.motionZ = (forward * moveSpeed * sin - strafe * moveSpeed * cos);
        }
    }
}
