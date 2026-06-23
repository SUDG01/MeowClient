package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;

/**
 * 第一人称动画模块
 * 控制手持物品的渲染动画风格
 * 效果通过 ItemRenderer 中的 GL 变换实现
 */
public class AnimationsMod extends Mod {

    // 1. 动画风格
    public ModeValue mode = new ModeValue("Mode", "1.7", new String[]{"1.7", "Exhibition", "Old", "Chill"});

    // 2. 自定义位置微调
    public NumberValue x = new NumberValue("X", 0.0, -2.0, 2.0, 0.1);
    public NumberValue y = new NumberValue("Y", 0.0, -2.0, 2.0, 0.1);
    public NumberValue z = new NumberValue("Z", 0.0, -2.0, 2.0, 0.1);
    public NumberValue scale = new NumberValue("Scale", 1.0, 0.1, 2.0, 0.1);

    // 动画进度跟踪
    private float animationProgress = 0.0f;
    private float prevAnimationProgress = 0.0f;

    public AnimationsMod() {
        super("Animations", Category.PLAYER);
        addValues(mode, x, y, z, scale);
    }

    @Override
    public void render(float partialTicks) {
        if (mc.thePlayer == null) return;

        // 跟踪挥动动画进度
        prevAnimationProgress = animationProgress;
        if (mc.thePlayer.isSwingInProgress) {
            animationProgress += 0.3f;
            if (animationProgress > 1.0f) animationProgress = 1.0f;
        } else {
            animationProgress -= 0.3f;
            if (animationProgress < 0.0f) animationProgress = 0.0f;
        }
    }

    /**
     * 获取当前动画进度（平滑过渡）
     */
    public float getAnimationProgress(float partialTicks) {
        return prevAnimationProgress + (animationProgress - prevAnimationProgress) * partialTicks;
    }

    /**
     * 获取当前模式下物品的 X 轴偏移
     */
    public float getItemX() {
        String currentMode = mode.getValue();
        switch (currentMode) {
            case "1.7":       return 1.0f + x.getValue().floatValue();
            case "Exhibition": return 1.2f + x.getValue().floatValue();
            case "Old":        return 0.56f + x.getValue().floatValue();
            case "Chill":      return 0.8f + x.getValue().floatValue();
            default:           return 1.0f + x.getValue().floatValue();
        }
    }

    /**
     * 获取当前模式下物品的 Y 轴偏移
     */
    public float getItemY() {
        String currentMode = mode.getValue();
        switch (currentMode) {
            case "1.7":       return -0.2f + y.getValue().floatValue();
            case "Exhibition": return -0.3f + y.getValue().floatValue();
            case "Old":        return 0.0f + y.getValue().floatValue();
            case "Chill":      return -0.1f + y.getValue().floatValue();
            default:           return 0.0f + y.getValue().floatValue();
        }
    }

    /**
     * 获取缩放系数
     */
    public float getItemScale() {
        return scale.getValue().floatValue();
    }

    /**
     * 获取 Z 轴偏移
     */
    public float getItemZ() {
        return z.getValue().floatValue();
    }
}
