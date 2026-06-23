package cn.sux1ng.client.mod.mods.world;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;

public class TimeChangerMod extends Mod {

    // 时间设置
    public NumberValue time = new NumberValue("Time", 12000.0, 0.0, 24000.0, 500.0);

    // 天气控制
    public ModeValue weather = new ModeValue("Weather", "None", new String[]{"None", "Clear", "Rain", "Thunder"});

    public TimeChangerMod() {
        super("TimeChanger", Category.WORLD);
        addValues(time, weather);
    }

    @Override
    public void update() {
        if (mc.theWorld == null) return;

        // 强制设置世界时间
        mc.theWorld.setWorldTime(time.getValue().longValue());

        // 天气控制
        if (weather.is("Clear")) {
            mc.theWorld.setRainStrength(0);
            mc.theWorld.setThunderStrength(0);
        } else if (weather.is("Rain")) {
            mc.theWorld.setRainStrength(1);
            mc.theWorld.setThunderStrength(0);
        } else if (weather.is("Thunder")) {
            mc.theWorld.setRainStrength(1);
            mc.theWorld.setThunderStrength(1);
        }
        // "None" 模式：不干预天气
    }
}
