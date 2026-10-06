package cn.sux1ng.client.targeting;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.mod.mods.misc.AntiBotMod;
import cn.sux1ng.client.mod.mods.misc.TargetMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

/** One selection rule for automatic actions and entity visuals. All calls are on the client thread. */
public final class TargetRules {
    private static ModManager manager;
    private static int moduleCount = -1;
    private static TargetMod target;
    private static AntiBotMod antiBot;
    private TargetRules() {}

    private static void refreshModules() {
        ModManager current = MeowClient.modManager;
        int count = current == null ? 0 : current.getMods().size();
        if (current != manager || count != moduleCount) {
            manager = current; moduleCount = count;
            target = current == null ? null : current.getByClass(TargetMod.class);
            antiBot = current == null ? null : current.getByClass(AntiBotMod.class);
        }
    }
    public static boolean isUnified() { refreshModules(); return target != null && target.isEnable(); }
    public static boolean canAttack(Entity entity) { return canAttack(entity, true, true, true, true); }
    public static boolean canRender(Entity entity) { return canRender(entity, true, true, true, true); }
    public static boolean canAttack(Entity entity, boolean players, boolean mobs, boolean animals, boolean invisible) {
        if (!matches(entity, players, mobs, animals, invisible)) return false;
        float health = ((EntityLivingBase)entity).getHealth();
        return Float.isFinite(health) && health > 0 && (antiBot == null || !antiBot.isEnable() || antiBot.allowsAutomatic(entity));
    }
    public static boolean canRender(Entity entity, boolean players, boolean mobs, boolean animals, boolean invisible) {
        return matches(entity, players, mobs, animals, invisible)
                && (antiBot == null || !antiBot.isEnable() || !antiBot.hideBots.getValue() || !antiBot.isBot(entity));
    }
    private static boolean matches(Entity entity, boolean players, boolean mobs, boolean animals, boolean invisible) {
        refreshModules(); Minecraft mc = Minecraft.getMinecraft();
        if (!(entity instanceof EntityLivingBase) || entity.isDead || mc == null || mc.thePlayer == null || mc.theWorld == null
                || entity == mc.thePlayer || entity.worldObj != mc.theWorld) return false;
        if (entity instanceof EntityPlayer && ((EntityPlayer)entity).isSpectator()) return false;
        if (target != null && target.isEnable()) {
            if (entity.isInvisible() && !target.invisible.getValue()) return false;
            if (entity instanceof EntityPlayer) return target.players.getValue();
            if (entity instanceof IMob) return target.mobs.getValue();
            return entity instanceof EntityLiving && target.friendlies.getValue();
        }
        if (entity.isInvisible() && !invisible) return false;
        return entity instanceof EntityPlayer && players || entity instanceof EntityMob && mobs || entity instanceof EntityAnimal && animals;
    }
}
