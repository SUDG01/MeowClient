package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.events.EventRender2D;
import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.NumberValue;
import org.lwjgl.input.Keyboard;

public class ZoomMod extends Mod {
    // 缩放 FOV（越小越放大）
    public NumberValue zoomFOV = new NumberValue("ZoomFOV", 30.0, 5.0, 80.0, 1.0);
    // 平滑过渡速度
    public NumberValue smoothSpeed = new NumberValue("Smoothness", 0.08, 0.01, 0.5, 0.01);

    private float oldFov;
    private boolean oldSmooth;

    public ZoomMod() {
        super("Zoom", Category.RENDER);
        setKey(Keyboard.KEY_C);
        addValues(zoomFOV, smoothSpeed);
    }

    @Override
    public void enable() {
        oldFov = mc.gameSettings.fovSetting;
        oldSmooth = mc.gameSettings.smoothCamera;
        super.enable();
    }

    @Override
    public void disable() {
        mc.gameSettings.fovSetting = oldFov;
        mc.gameSettings.smoothCamera = oldSmooth;
        super.disable();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        mc.gameSettings.smoothCamera = true;
        float diff = zoomFOV.getValue().floatValue() - mc.gameSettings.fovSetting;
        if (Math.abs(diff) > 0.01) {
            mc.gameSettings.fovSetting += diff * smoothSpeed.getValue().floatValue();
        }
    }
}
