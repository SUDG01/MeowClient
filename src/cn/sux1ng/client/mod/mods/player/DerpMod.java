package cn.sux1ng.client.mod.mods.player;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;

/**
 * Derp — 扭头
 * 强制玩家头部朝各种奇怪的方向转
 */
public class DerpMod extends Mod {

    public ModeValue mode = new ModeValue("Mode", "Spin", new String[]{"Spin", "Jitter", "Static", "Backward"});
    public NumberValue speed = new NumberValue("Speed", 10, 1, 50, 1);

    private float spinAngle = 0;

    public DerpMod() {
        super("Derp", Category.PLAYER);
        addValues(mode, speed);
    }

    @Override
    public void update() {
        if (mc.thePlayer == null) return;

        float spd = speed.getValue().floatValue();

        switch (mode.getValue()) {
            case "Spin":
                spinAngle += spd;
                mc.thePlayer.rotationYawHead = spinAngle % 360;
                break;
            case "Jitter":
                mc.thePlayer.rotationYawHead += (Math.random() - 0.5) * spd * 2;
                mc.thePlayer.rotationPitch = (float) ((Math.random() - 0.5) * 90);
                break;
            case "Static":
                mc.thePlayer.rotationYawHead = 180;
                mc.thePlayer.rotationPitch = 45;
                break;
            case "Backward":
                mc.thePlayer.rotationYawHead = mc.thePlayer.rotationYaw + 180;
                break;
        }
    }
}
