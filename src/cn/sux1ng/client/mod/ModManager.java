package cn.sux1ng.client.mod;

import cn.sux1ng.client.mod.mods.combat.AutoClickerMod;
import cn.sux1ng.client.mod.mods.combat.ClickSoundMod;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.mod.mods.combat.NoClickDelayMod;
import cn.sux1ng.client.mod.mods.draw.*;
import cn.sux1ng.client.mod.mods.movement.NoJumpDelayMod;
import cn.sux1ng.client.mod.mods.movement.NoSlowMod;
import cn.sux1ng.client.mod.mods.movement.SpeedMod;
import cn.sux1ng.client.mod.mods.movement.SprintMod;
import cn.sux1ng.client.mod.mods.player.DerpMod;
import cn.sux1ng.client.mod.mods.player.SkinDerpMod;
import cn.sux1ng.client.mod.mods.player.TwerkMod;
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
    private long lastKeyTime = 0;       // SMART 模式用：上次按下时间
    private int lastKeyCode = -1;       // SMART 模式用：上次按下的键

    public List<Mod> getMods() {
        return mods;
    }

    public List<Mod> getEnableMods(){
        return mods.stream().filter(Mod::isEnable).collect(Collectors.toList());
    }

    /**
     * 按键按下时调用
     */
    public void onKey(int key){
        long now = System.currentTimeMillis();
        for (Mod mod : mods) {
            if (mod.getKey() != key) continue;

            switch (mod.getBindMode()) {
                case TOGGLE:
                    mod.setEnable(!mod.isEnable());
                    break;
                case HOLD:
                    mod.setEnable(true);
                    break;
                case SMART:
                    // 短按 (<250ms) = 切换，长按 = HOLD
                    if (lastKeyCode == key && (now - lastKeyTime) < 400) {
                        // 快速双击 = toggle
                        mod.setEnable(!mod.isEnable());
                        mod.setSmartHolding(false);
                    } else {
                        mod.setEnable(true);
                        mod.setSmartHolding(true);
                    }
                    break;
            }
        }
        lastKeyCode = key;
        lastKeyTime = now;
    }

    /**
     * 按键松开时调用（HOLD/SMART 模式需要）
     */
    public void onKeyRelease(int key) {
        for (Mod mod : mods) {
            if (mod.getKey() != key) continue;
            switch (mod.getBindMode()) {
                case HOLD:
                    mod.setEnable(false);
                    break;
                case SMART:
                    if (mod.isSmartHolding()) {
                        mod.setEnable(false);
                        mod.setSmartHolding(false);
                    }
                    break;
                case TOGGLE:
                default:
                    break;
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
        // 趣味/视觉模块
        mods.add(new BreadcrumbsMod());
        mods.add(new JumpEffectMod());
        mods.add(new HitParticlesMod());
        mods.add(new DamageParticlesMod());
        mods.add(new TracersMod());
        mods.add(new DerpMod());
        mods.add(new SkinDerpMod());
        mods.add(new TwerkMod());

        // 预设绑定模式
        EagleMod eagle = getByClass(EagleMod.class);
        eagle.setBindMode(Mod.BindMode.HOLD);
        SprintMod sprint = getByClass(SprintMod.class);
        sprint.setBindMode(Mod.BindMode.HOLD);
        ZoomMod zoom = getByClass(ZoomMod.class);
        zoom.setBindMode(Mod.BindMode.HOLD);

        // 预设 tag
        KillAuraMod ka = getByClass(KillAuraMod.class);
        ka.tagBy(ka.targetMode);  // 在 ArrayList 中显示目标模式
        SpeedMod sp = getByClass(SpeedMod.class);
        sp.tagBy(sp.mode);
        AutoClickerMod ac = getByClass(AutoClickerMod.class);
        ac.tagBy(ac.mode);
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
