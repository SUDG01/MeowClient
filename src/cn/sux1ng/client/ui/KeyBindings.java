package cn.sux1ng.client.ui;

import cn.sux1ng.client.mod.Mod;
import java.util.ArrayList;
import java.util.List;

public final class KeyBindings {
    private KeyBindings() {}
    public static List<Mod> conflicts(Mod mod, List<Mod> modules) {
        List<Mod> result = new ArrayList<>();
        if (mod.getKey() == 0) return result;
        for (Mod other : modules) if (other != mod && other.getKey() == mod.getKey()) result.add(other);
        return result;
    }
    public static Mod.BindMode nextMode(Mod.BindMode mode) {
        return Mod.BindMode.values()[(mode.ordinal() + 1) % Mod.BindMode.values().length];
    }
}
