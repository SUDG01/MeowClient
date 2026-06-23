package cn.sux1ng.client.mod;

import cn.sux1ng.client.gui.clickgui.ClickGUI;
import cn.sux1ng.client.mod.mods.combat.AutoClickerMod;
import cn.sux1ng.client.mod.mods.combat.ClickSoundMod;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.mod.mods.combat.NoClickDelayMod;
import cn.sux1ng.client.mod.mods.draw.*;
import cn.sux1ng.client.mod.mods.movement.NoJumpDelayMod;
import cn.sux1ng.client.mod.mods.movement.NoSlowMod;
import cn.sux1ng.client.mod.mods.movement.SpeedMod;
import cn.sux1ng.client.mod.mods.movement.SprintMod;
import cn.sux1ng.client.mod.mods.render.*;
import cn.sux1ng.client.mod.mods.world.TimeChangerMod;
import cn.sux1ng.client.mod.mods.world.AutoGGMod;
import cn.sux1ng.client.mod.mods.world.AutoToolMod;
import cn.sux1ng.client.mod.mods.world.EagleMod;
import cn.sux1ng.client.mod.mods.world.FastPlaceMod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModManager {
    private final List<Mod> mods = new ArrayList<>();

    public List<Mod> getMods() {
        return mods;
    }

    public List<Mod> getEnableMods(){
        return mods.stream().filter(Mod::isEnable).collect(Collectors.toList());
    }

    public void onKey(int key){
        for (Mod enableMod : mods) {
            if (enableMod.getKey() == key) {
                enableMod.setEnable(!enableMod.isEnable());
            }
        }
    }

    public void load(){
        mods.add(new LogoMod());
        mods.add(new SprintMod());
        mods.add(new ArrayListMod());
        mods.add(new ESPMod());
        mods.add(new TabMod());
        mods.add(new FastPlaceMod());
        mods.add(new NoClickDelayMod());
        mods.add(new AnimationsMod());
        mods.add(new FullBrightMod());
        mods.add(new AutoToolMod());
        mods.add(new NameTagMod());
        mods.add(new ClickGUIMod());
        mods.add(new AutoClickerMod());
        mods.add(new NoJumpDelayMod());
        mods.add(new EagleMod());
        mods.add(new TimeChangerMod());
        mods.add(new AutoGGMod());
        mods.add(new InfoHUDMod());
        mods.add(new ArmorHUDMod());
        mods.add(new SpeedMod());
        mods.add(new KillAuraMod());
        mods.add(new TargetHUDMod());
        mods.add(new ZoomMod());
        mods.add(new ClickSoundMod());
        mods.add(new CapeMod());
        mods.add(new NoSlowMod());
    }

    public Mod getByName(String name){
        for (Mod mod : mods) {
            if (name.equalsIgnoreCase(mod.getName())) {
                return mod;
            }
        }
        return null;
    }


    public List<Mod> getByCategory(Category category){
        return mods.stream().filter(mod -> mod.getCategory() == category).collect(Collectors.toList());
    }

    public <T extends Mod> T getByClass(Class<T> modClass){
        for (Mod mod : mods) {
            if (mod.getClass() == modClass) {
                return (T) mod;
            }
        }
        return null;
    }
}
