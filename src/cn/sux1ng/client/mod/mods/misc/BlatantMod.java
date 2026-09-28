package cn.sux1ng.client.mod.mods.misc;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.BlatantPolicy;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.ModManager;

/** Permission switch for gameplay-changing modules. Disabled by default. */
public class BlatantMod extends Mod {
    public BlatantMod() {
        super("Blatant", Category.MISC);
    }

    @Override
    public void disable() {
        ModManager manager = MeowClient.modManager;
        if (manager == null) return;
        for (Mod mod : manager.getEnableMods()) {
            if (BlatantPolicy.requiresBlatant(mod)) {
                try {
                    mod.setEnable(false);
                } catch (RuntimeException failure) {
                    // The base state is already off; continue closing the remaining modules.
                    System.err.println("Could not cleanly disable " + mod.getName() + ": " + failure.getMessage());
                }
            }
        }
    }
}
