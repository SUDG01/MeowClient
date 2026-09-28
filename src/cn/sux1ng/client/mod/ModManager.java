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
import cn.sux1ng.client.mod.mods.misc.BlatantMod;
import cn.sux1ng.client.mod.mods.player.DerpMod;
import cn.sux1ng.client.mod.mods.player.SkinDerpMod;
import cn.sux1ng.client.mod.mods.player.TwerkMod;
import cn.sux1ng.client.mod.mods.render.*;
import cn.sux1ng.client.mod.mods.world.TimeChangerMod;
import cn.sux1ng.client.mod.mods.world.AutoGGMod;
import cn.sux1ng.client.mod.mods.world.AutoToolMod;
import cn.sux1ng.client.mod.mods.world.EagleMod;
import cn.sux1ng.client.mod.mods.world.FastPlaceMod;
import cn.sux1ng.client.ui.ClientLanguage;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

public class ModManager {
    private final List<Mod> mods = new ArrayList<>();
    private final LongSupplier clock;
    private final Map<Mod, Long> smartPressTimes = new IdentityHashMap<>();
    private final Map<Mod, Boolean> smartInitialStates = new IdentityHashMap<>();

    public ModManager() {
        this(System::currentTimeMillis);
    }

    public ModManager(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

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
        if (key == 0) return;
        long now = clock.getAsLong();
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
                    if (!smartPressTimes.containsKey(mod)) {
                        smartPressTimes.put(mod, now);
                        smartInitialStates.put(mod, mod.isEnable());
                        try {
                            mod.setEnable(true);
                            if (!mod.isEnable()) {
                                // Action modules such as ClickGUI disable themselves after opening.
                                smartPressTimes.remove(mod);
                                smartInitialStates.remove(mod);
                            }
                        } catch (RuntimeException | Error failure) {
                            smartPressTimes.remove(mod);
                            smartInitialStates.remove(mod);
                            throw failure;
                        }
                    }
                    break;
            }
        }
    }

    /**
     * 按键松开时调用（HOLD/SMART 模式需要）
     */
    public void onKeyRelease(int key) {
        if (key == 0) return;
        long now = clock.getAsLong();
        for (Mod mod : mods) {
            if (mod.getKey() != key) continue;
            Long pressedAt = smartPressTimes.remove(mod);
            Boolean initiallyEnabled = smartInitialStates.remove(mod);
            switch (mod.getBindMode()) {
                case HOLD:
                    mod.setEnable(false);
                    break;
                case SMART:
                    if (pressedAt != null && initiallyEnabled != null) {
                        mod.setEnable(now - pressedAt < 250 ? !initiallyEnabled : initiallyEnabled);
                    }
                    break;
                case TOGGLE:
                default:
                    break;
            }
        }
    }

    public void load(){
        // Restore this permission before any restricted modules during config activation.
        mods.add(new BlatantMod());
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
            if (name.equalsIgnoreCase(mod.getName()) || name.equals(ClientLanguage.module(mod))) {
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
