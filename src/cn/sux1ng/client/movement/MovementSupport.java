package cn.sux1ng.client.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.*;

public final class MovementSupport {
    private MovementSupport() {}
    public static boolean active(Minecraft mc) {
        return mc != null && mc.thePlayer != null && mc.theWorld != null && mc.thePlayer.worldObj == mc.theWorld
                && mc.gameSettings != null && mc.inGameHasFocus && mc.currentScreen == null && !mc.isGamePaused()
                && !mc.thePlayer.isDead && mc.thePlayer.getHealth() > 0 && !mc.thePlayer.isRiding() && !mc.thePlayer.isSpectator();
    }
    public static double potionFactor(EntityPlayerSP player) {
        double factor = 1;
        PotionEffect fast = player.isPotionActive(Potion.moveSpeed) ? player.getActivePotionEffect(Potion.moveSpeed) : null;
        PotionEffect slow = player.isPotionActive(Potion.moveSlowdown) ? player.getActivePotionEffect(Potion.moveSlowdown) : null;
        if (fast != null) factor *= 1 + 0.2 * (fast.getAmplifier() + 1);
        if (slow != null) factor *= Math.max(0.1, 1 - 0.15 * (slow.getAmplifier() + 1));
        return factor;
    }
    public static double baseSpeed(EntityPlayerSP player) { return (player.isSprinting() ? 0.2873 : 0.221) * potionFactor(player); }
    public static double[] direction(float forward, float strafe, float yaw, double speed) {
        double length = Math.hypot(forward, strafe);
        if (length < 1e-4) return new double[]{0, 0};
        double angle = Math.toRadians(yaw), scale = speed * Math.min(1, length) / length;
        return new double[]{(-Math.sin(angle) * forward + Math.cos(angle) * strafe) * scale,
                (Math.cos(angle) * forward + Math.sin(angle) * strafe) * scale};
    }
}
