package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.AttackEvent;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.targeting.TargetRules;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;

import java.util.Random;

/** One configured particle burst per actual attack, including controller-driven attacks. */
public class HitParticlesMod extends Mod {
    public ModeValue particleMode = new ModeValue("Effect", "Heart", new String[]{"Heart", "Flame", "Crit", "Slime", "Portal", "Smoke"});
    public NumberValue count = new NumberValue("Count", 10, 1, 50, 1);
    private final Random random = new Random();

    public HitParticlesMod() {
        super("HitParticles", Category.RENDER);
        addValues(particleMode, count);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null || event.getPlayer() != mc.thePlayer) return;
        Entity target = event.getTarget();
        if (target == null || target.worldObj != mc.theWorld || !TargetRules.canRender(target)) return;
        EnumParticleTypes type;
        switch (particleMode.getValue()) {
            case "Flame": type = EnumParticleTypes.FLAME; break;
            case "Crit": type = EnumParticleTypes.CRIT; break;
            case "Slime": type = EnumParticleTypes.SLIME; break;
            case "Portal": type = EnumParticleTypes.PORTAL; break;
            case "Smoke": type = EnumParticleTypes.SMOKE_LARGE; break;
            default: type = EnumParticleTypes.HEART;
        }
        for (int i = 0; i < count.getValue().intValue(); i++) {
            double spread = Math.max(0.25, target.width * 0.7);
            double x = target.posX + (random.nextDouble() - 0.5) * spread;
            double y = target.posY + target.height * (0.35 + random.nextDouble() * 0.5);
            double z = target.posZ + (random.nextDouble() - 0.5) * spread;
            double vx = (random.nextDouble() - 0.5) * 0.12;
            double vy = 0.03 + random.nextDouble() * 0.06;
            double vz = (random.nextDouble() - 0.5) * 0.12;
            // Explicit client effects remain visible when vanilla particle detail is reduced.
            mc.theWorld.spawnParticle(type, true, x, y, z, vx, vy, vz);
        }
    }
}
