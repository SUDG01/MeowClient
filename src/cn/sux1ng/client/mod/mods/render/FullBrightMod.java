package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;

public class FullBrightMod extends Mod {

    private float oldGamma; // 保存原来的亮度
    private boolean gammaSaved;

    // 亮度值
    public NumberValue gamma = new NumberValue("Gamma", 16.0, 1.0, 16.0, 1.0)
            .setVisibility(() -> this.mode.is("Gamma"));
    // 亮度模式
    public ModeValue mode = new ModeValue("Mode", "Gamma", new String[]{"Gamma", "NightVision"});

    public FullBrightMod() {
        super("FullBright", Category.RENDER);
        addValues(gamma, mode);
    }

    @Override
    public void enable() {
        if (mc != null && mc.gameSettings != null) {
            oldGamma = mc.gameSettings.gammaSetting;
            gammaSaved = true;
            update();
        }
    }

    @Override
    public void disable() {
        if (gammaSaved && mc != null && mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = oldGamma;
        }
        gammaSaved = false;
    }

    @Override
    public void update() {
        if (gammaSaved && mc != null && mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = mode.is("Gamma") ? gamma.getValue().floatValue() : oldGamma;
        }
    }

    /** Used by the vanilla lightmap and fog hooks; does not modify real potion effects. */
    public static boolean isNightVisionActive() {
        if (MeowClient.modManager == null) return false;
        FullBrightMod mod = MeowClient.modManager.getByClass(FullBrightMod.class);
        return mod != null && mod.isEnable() && mod.mode.is("NightVision");
    }
}
