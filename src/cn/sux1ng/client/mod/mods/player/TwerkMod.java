package cn.sux1ng.client.mod.mods.player;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.NumberValue;

/**
 * Twerk — 自动蹲起
 * 定时器交替按下/松开 Shift 键
 */
public class TwerkMod extends Mod {

    public NumberValue speed = new NumberValue("Speed", 5, 1, 30, 1);

    private int tick = 0;
    private boolean sneaking = false;

    public TwerkMod() {
        super("Twerk", Category.PLAYER);
        addValues(speed);
    }

    @Override
    public void update() {
        if (mc.thePlayer == null) return;

        tick++;
        int interval = Math.max(1, (int)(20 - speed.getValue() + 1));
        if (tick % interval == 0) {
            sneaking = !sneaking;
            mc.gameSettings.keyBindSneak.pressed = sneaking;
        }
    }

    @Override
    public void disable() {
        // 恢复潜行状态
        if (mc.thePlayer != null && !org.lwjgl.input.Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            mc.gameSettings.keyBindSneak.pressed = false;
        }
    }
}
