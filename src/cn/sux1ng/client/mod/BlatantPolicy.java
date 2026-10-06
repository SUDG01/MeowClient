package cn.sux1ng.client.mod;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.mods.combat.AutoClickerMod;
import cn.sux1ng.client.mod.mods.combat.AimbotMod;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.mod.mods.combat.NoClickDelayMod;
import cn.sux1ng.client.mod.mods.misc.BlatantMod;
import cn.sux1ng.client.mod.mods.misc.DisablerMod;
import cn.sux1ng.client.mod.mods.movement.NoJumpDelayMod;
import cn.sux1ng.client.mod.mods.movement.NoSlowMod;
import cn.sux1ng.client.mod.mods.movement.SpeedMod;
import cn.sux1ng.client.mod.mods.player.DerpMod;
import cn.sux1ng.client.mod.mods.player.SkinDerpMod;
import cn.sux1ng.client.mod.mods.player.TwerkMod;
import cn.sux1ng.client.mod.mods.world.AutoToolMod;
import cn.sux1ng.client.mod.mods.world.EagleMod;
import cn.sux1ng.client.mod.mods.world.FastPlaceMod;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Central catalog of modules whose behavior changes gameplay or player actions. */
public final class BlatantPolicy {
    private static final Set<Class<? extends Mod>> RESTRICTED = Collections.unmodifiableSet(
            new HashSet<Class<? extends Mod>>(Arrays.asList(
                    KillAuraMod.class, AimbotMod.class, AutoClickerMod.class, NoClickDelayMod.class,
                    SpeedMod.class, NoSlowMod.class, NoJumpDelayMod.class, DisablerMod.class,
                    EagleMod.class, FastPlaceMod.class, AutoToolMod.class,
                    DerpMod.class, SkinDerpMod.class, TwerkMod.class)));

    private BlatantPolicy() {}

    public static boolean requiresBlatant(Mod mod) {
        for (Class<? extends Mod> type : RESTRICTED) {
            if (type.isInstance(mod)) return true;
        }
        return false;
    }

    public static boolean canEnable(Mod mod) {
        if (!requiresBlatant(mod)) return true;
        ModManager manager = MeowClient.modManager;
        BlatantMod blatant = manager == null ? null : manager.getByClass(BlatantMod.class);
        return blatant != null && blatant.isEnable();
    }
}
