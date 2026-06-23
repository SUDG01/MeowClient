package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.Minecraft;

public class FullBrightMod extends Mod {

    private final Minecraft mc = Minecraft.getMinecraft();
    private float oldGamma; // 保存原来的亮度

    // 亮度值
    public NumberValue gamma = new NumberValue("Gamma", 16.0, 1.0, 16.0, 1.0);
    // 亮度模式
    public ModeValue mode = new ModeValue("Mode", "Gamma", new String[]{"Gamma", "NightVision"});

    public FullBrightMod() {
        super("FullBright", Category.RENDER);
        addValues(gamma, mode);
    }

    @Override
    public void setEnable(boolean enable) {
        super.setEnable(enable);

        if (enable) {
            oldGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = gamma.getValue().floatValue();
        } else {
            mc.gameSettings.gammaSetting = oldGamma;
        }
    }

    @Override
    public void update() {
        if (isEnable()) {
            if (mode.is("Gamma")) {
                mc.gameSettings.gammaSetting = gamma.getValue().floatValue();
            }
            // NightVision 模式：通过药水效果实现，此处通过 gamma 保持整体亮度
            // 完整实现需要注入 PotionEffect，暂时使用 gamma 替代
        }
    }
}
