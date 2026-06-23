package cn.sux1ng.client.mod.mods.world;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.block.BlockAir;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

public class EagleMod extends Mod {

    // 到达边缘后潜行的延迟（防止抖动）
    public NumberValue sneakDelay = new NumberValue("SneakDelay", 0.0, 0.0, 500.0, 50.0);
    // 仅在地面时触发
    public BooleanValue onlyOnGround = new BooleanValue("OnlyOnGround", true);

    private long lastEdgeTime = 0;

    public EagleMod() {
        super("Eagle", Category.WORLD);
        setKey(Keyboard.KEY_X);
        addValues(sneakDelay, onlyOnGround);
    }

    @Override
    public void update() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);

        boolean isOnEdge = mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir;
        boolean shouldSneak = isOnEdge;

        // 仅地面触发选项
        if (onlyOnGround.getValue() && !mc.thePlayer.onGround) {
            shouldSneak = false;
        }

        if (shouldSneak) {
            // 检查延迟
            if (sneakDelay.getValue() > 0) {
                if (lastEdgeTime == 0) {
                    lastEdgeTime = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - lastEdgeTime < sneakDelay.getValue()) {
                    return; // 延迟未过，不触发
                }
            }
            mc.gameSettings.keyBindSneak.pressed = true;
        } else {
            lastEdgeTime = 0;
            if (!Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                mc.gameSettings.keyBindSneak.pressed = false;
            }
        }
    }

    @Override
    public void disable() {
        lastEdgeTime = 0;
        if (mc.thePlayer != null) {
            if (!Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                mc.gameSettings.keyBindSneak.pressed = false;
            }
        }
    }
}
