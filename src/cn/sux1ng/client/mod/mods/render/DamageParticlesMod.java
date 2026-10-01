package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.AttackEvent;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.RenderState;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.world.World;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Floating hit damage: observed health changes replace an explicitly labelled estimate. */
public class DamageParticlesMod extends Mod {
    public NumberValue fontSize = new NumberValue("Size", 2.0, 1.0, 5.0, 0.5);
    public NumberValue lifeTime = new NumberValue("Duration", 1500, 500, 5000, 250);
    public ColorValue textColor = new ColorValue("Color", new Color(255, 80, 80));

    private final List<DamageParticle> particles = new ArrayList<>();
    private final DecimalFormat format = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private final LongSupplier clock;
    private World world;
    private EntityPlayerSP player;

    public DamageParticlesMod() { this(System::currentTimeMillis); }

    public DamageParticlesMod(LongSupplier clock) {
        super("DamageParticles", Category.RENDER);
        this.clock = Objects.requireNonNull(clock, "clock");
        addValues(fontSize, lifeTime, textColor);
    }

    @Override public void enable() { clear(); }
    @Override public void disable() { clear(); }

    private void clear() { particles.clear(); world = null; player = null; }

    private boolean syncWorld() {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) { clear(); return false; }
        if (world != mc.theWorld || player != mc.thePlayer) {
            clear(); world = mc.theWorld; player = mc.thePlayer;
        }
        return true;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!syncWorld() || event.getPlayer() != player || !(event.getTarget() instanceof EntityLivingBase)) return;
        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        if (target.isDead || target.worldObj != world) return;
        IAttributeInstance attack = player.getEntityAttribute(SharedMonsterAttributes.attackDamage);
        float estimate = attack == null ? 1f : (float) attack.getAttributeValue();
        estimate += EnchantmentHelper.func_152377_a(player.getHeldItem(), target.getCreatureAttribute());
        particles.add(new DamageParticle(target, Math.max(0, estimate), clock.getAsLong()));
        while (particles.size() > 64) particles.remove(0);
    }

    @Override
    public void update() {
        if (!syncWorld()) return;
        long now = clock.getAsLong();
        particles.removeIf(particle -> now - particle.startTime >= lifeTime.getValue().longValue());
        for (DamageParticle particle : particles) {
            if (!particle.estimated || now - particle.startTime > 500) continue;
            float health = particle.target.getHealth();
            if (health < particle.healthBefore) {
                particle.value = particle.healthBefore - health;
                particle.estimated = false;
                // One observed health delta belongs to one pending hit, rather than every hit.
                for (DamageParticle pending : particles) {
                    if (pending.target == particle.target && pending.estimated) pending.healthBefore = health;
                }
            }
        }
    }

    @Override
    public void render(float partialTicks) {
        if (!syncWorld()) return;
        long now = clock.getAsLong();
        long maxAge = lifeTime.getValue().longValue();
        particles.removeIf(particle -> now - particle.startTime >= maxAge);
        if (particles.isEmpty()) return;
        RenderManager rm = mc.getRenderManager();
        Color color = textColor.getColor();
        try (RenderState state = RenderState.capture()) {
            RenderState.setupWorldEffect();
            GlStateManager.enableTexture2D();
            for (DamageParticle particle : particles) {
                float progress = Math.max(0, (now - particle.startTime) / (float) maxAge);
                int alpha = (int) ((1 - progress) * color.getAlpha());
                // Vanilla FontRenderer interprets tiny alpha values as fully opaque.
                if (alpha < 5) continue;
                GlStateManager.pushMatrix();
                GlStateManager.translate(particle.x - rm.renderPosX,
                        particle.y + progress * 1.1 - rm.renderPosY, particle.z - rm.renderPosZ);
                GlStateManager.rotate(-rm.playerViewY, 0, 1, 0);
                GlStateManager.rotate(rm.playerViewX * (mc.gameSettings.thirdPersonView == 2 ? -1 : 1), 1, 0, 0);
                float scale = fontSize.getValue().floatValue() * 0.02f;
                GlStateManager.scale(-scale, -scale, scale);
                String text = (particle.estimated ? "~" : "") + format.format(particle.value);
                mc.fontRendererObj.drawStringWithShadow(text, -mc.fontRendererObj.getStringWidth(text) / 2f, 0,
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB());
                GlStateManager.popMatrix();
            }
        }
    }

    private static final class DamageParticle {
        final EntityLivingBase target;
        final double x, y, z;
        final long startTime;
        float healthBefore, value;
        boolean estimated = true;
        DamageParticle(EntityLivingBase target, float estimate, long startTime) {
            this.target = target;
            this.x = target.posX; this.y = target.posY + target.height + 0.25; this.z = target.posZ;
            this.healthBefore = target.getHealth(); this.value = estimate; this.startTime = startTime;
        }
    }
}
