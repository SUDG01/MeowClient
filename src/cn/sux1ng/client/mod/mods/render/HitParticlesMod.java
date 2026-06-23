package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;

/**
 * HitParticles — 攻击粒子
 * 攻击实体时在原版粒子系统刷出特效粒子
 */
public class HitParticlesMod extends Mod {

    public ModeValue particleMode = new ModeValue("Effect", "Heart", new String[]{"Heart", "Flame", "Crit", "Slime", "Portal", "Smoke"});
    public NumberValue count = new NumberValue("Count", 10, 1, 50, 1);

    public HitParticlesMod() {
        super("HitParticles", Category.RENDER);
        addValues(particleMode, count);
    }

    @Override
    public void render(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // 检测攻击：swingProgress 变化表示刚挥剑
        if (mc.thePlayer.swingProgress > 0 && mc.thePlayer.swingProgress < 0.1f
                && mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
            spawnParticles(mc.objectMouseOver.entityHit.posX,
                    mc.objectMouseOver.entityHit.posY + mc.objectMouseOver.entityHit.height / 2,
                    mc.objectMouseOver.entityHit.posZ);
        }
    }

    private void spawnParticles(double x, double y, double z) {
        String mode = particleMode.getValue();
        int n = count.getValue().intValue();
        for (int i = 0; i < n; i++) {
            double ox = (Math.random() - 0.5) * 0.8;
            double oy = Math.random() * 1.5;
            double oz = (Math.random() - 0.5) * 0.8;

            switch (mode) {
                case "Heart":
                    mc.theWorld.spawnParticle(EnumParticleTypes.HEART, x + ox, y + oy, z + oz, 0, 0.05, 0);
                    break;
                case "Flame":
                    mc.theWorld.spawnParticle(EnumParticleTypes.FLAME, x + ox, y + oy, z + oz, 0, 0.02, 0);
                    break;
                case "Crit":
                    mc.effectRenderer.emitParticleAtEntity(mc.objectMouseOver.entityHit, EnumParticleTypes.CRIT);
                    break;
                case "Slime":
                    mc.theWorld.spawnParticle(EnumParticleTypes.SLIME, x + ox, y + oy, z + oz, 0, 0, 0);
                    break;
                case "Portal":
                    mc.theWorld.spawnParticle(EnumParticleTypes.PORTAL, x + ox, y + oy, z + oz, Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);
                    break;
                case "Smoke":
                    mc.theWorld.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x + ox, y + oy, z + oz, 0, 0.1, 0);
                    break;
            }
        }
    }
}
