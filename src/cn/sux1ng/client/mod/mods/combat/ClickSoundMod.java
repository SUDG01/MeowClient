package cn.sux1ng.client.mod.mods.combat;

import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.ClickEvent;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.SoundUtil;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import org.apache.commons.lang3.RandomUtils;

public class ClickSoundMod extends Mod {

    public ModeValue mode = new ModeValue("Mode", "Normal", new String[]{"Normal", "Jitter", "Double"});
    public NumberValue volume = new NumberValue("Volume", 0.5D, 0.1D, 2.0D, 0.1D);
    public NumberValue variation = new NumberValue("Variation", 5.0D, 0.0D, 100.0D, 1.0D); // 音调浮动范围

    public ClickSoundMod() {
        super("ClickSound", Category.COMBAT);
        addValues(mode, volume, variation);
    }

    @EventTarget
    public void onClick(ClickEvent event) {
        // System.out.println("DEBUG: Click!"); // 调试完可以注释掉，免得刷屏

        if (event.getType() != ClickEvent.ClickType.MIDDLE) {
            float pitch = 1.0f + RandomUtils.nextFloat(0.0f, this.variation.getValue().floatValue() / 100.0f);

            // 如果你想让音调有高有低 (比如 0.9 ~ 1.1)，可以用这个公式：
            // float pitch = 1.0f + RandomUtils.nextFloat(-this.variation.getValue().floatValue()/100.0f, this.variation.getValue().floatValue()/100.0f);

            switch (this.mode.getValue()) {
                case "Normal":
                    SoundUtil.playsound("nc.wav", this.volume.getValue().floatValue(), pitch);
                    break;
                case "Jitter":
                    SoundUtil.playsound("jc.wav", this.volume.getValue().floatValue(), pitch);
                    break;
                case "Double":
                    SoundUtil.playsound("dbc.wav", this.volume.getValue().floatValue(), pitch);
                    break;
            }
        }
    }
}