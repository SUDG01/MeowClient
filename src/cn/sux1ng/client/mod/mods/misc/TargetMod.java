package cn.sux1ng.client.mod.mods.misc;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;

/** Shared type selection; disabling it restores each caller's legacy settings. */
public final class TargetMod extends Mod {
    public final BooleanValue players = new BooleanValue("Players", true);
    public final BooleanValue mobs = new BooleanValue("Mobs", false);
    public final BooleanValue friendlies = new BooleanValue("Friendlies", false);
    public final BooleanValue invisible = new BooleanValue("Invisibles", false);

    public TargetMod() {
        super("Target", Category.MISC);
        addValues(players, mobs, friendlies, invisible);
    }
}
