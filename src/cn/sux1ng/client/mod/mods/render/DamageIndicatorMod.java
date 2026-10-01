package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.RenderState;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

public class DamageIndicatorMod extends Mod {
    public NumberValue duration = new NumberValue("Duration", 1200, 200, 3000, 100);
    public NumberValue radius = new NumberValue("Radius", 42, 24, 90, 2);
    public BooleanValue inferMelee = new BooleanValue("InferMelee", true);
    public ColorValue color = new ColorValue("Color", new Color(255, 141, 155));
    private final LongSupplier clock;
    private final List<Hit> hits = new ArrayList<>();
    private World world;
    private EntityPlayerSP player;
    private int lastHurt;

    public DamageIndicatorMod() { this(System::currentTimeMillis); }
    public DamageIndicatorMod(LongSupplier clock) {
        super("DamageIndicator", Category.RENDER);
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        addValues(duration, radius, inferMelee, color);
    }
    @Override public void enable() { clear(); }
    @Override public void disable() { clear(); }
    private void clear() { hits.clear(); world = null; player = null; lastHurt = 0; }
    private boolean syncWorld() {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) { clear(); return false; }
        if (world != mc.theWorld || player != mc.thePlayer) {
            clear(); world = mc.theWorld; player = mc.thePlayer; lastHurt = player.hurtTime;
        }
        return true;
    }

    public static final class Source {
        public final EntityLivingBase entity;
        public final boolean inferred;
        Source(EntityLivingBase entity, boolean inferred) { this.entity = entity; this.inferred = inferred; }
    }

    public static Source findSource(EntityLivingBase victim, List<Entity> entities, boolean infer) {
        EntityLivingBase attacker = victim.getLastAttacker();
        if (attacker != null && attacker != victim && !attacker.isDead
                && victim.ticksExisted - victim.getLastAttackerTime() <= 20) return new Source(attacker, false);
        for (Entity entity : entities) {
            if (entity instanceof EntityArrow && !entity.isDead && entity.getDistanceSqToEntity(victim) < 9) {
                EntityArrow arrow = (EntityArrow) entity;
                if (arrow.shootingEntity instanceof EntityLivingBase && arrow.shootingEntity != victim
                        && arrow.motionX * arrow.motionX + arrow.motionY * arrow.motionY + arrow.motionZ * arrow.motionZ > 0.01) {
                    double approach = (victim.posX - arrow.posX) * arrow.motionX
                            + (victim.posY + victim.height * 0.5 - arrow.posY) * arrow.motionY
                            + (victim.posZ - arrow.posZ) * arrow.motionZ;
                    if (approach >= 0) return new Source((EntityLivingBase) arrow.shootingEntity, false);
                }
            }
        }
        if (!infer) return null;
        double bestDistance = 25;
        EntityLivingBase best = null;
        for (Entity entity : entities) {
            if (!(entity instanceof EntityLivingBase) || entity == victim || entity.isDead) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            if (!living.isSwingInProgress && living.swingProgress <= 0) continue;
            double distance = victim.getDistanceSqToEntity(entity);
            if (distance >= bestDistance) continue;
            double dx = victim.posX - entity.posX, dz = victim.posZ - entity.posZ;
            Vec3 look = entity.getLookVec();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (look != null && length > 0.01 && (look.xCoord * dx + look.zCoord * dz) / length >= 0.5
                    && victim.canEntityBeSeen(entity)) { best = living; bestDistance = distance; }
        }
        return best == null ? null : new Source(best, true);
    }

    public static float relativeYaw(double dx, double dz, float yaw) {
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(-dx, dz)) - yaw);
    }

    @Override public void update() {
        if (!syncWorld()) return;
        long now = clock.getAsLong();
        hits.removeIf(hit -> now - hit.time >= duration.getValue().longValue());
        if (player.hurtTime > lastHurt) {
            Source source = findSource(player, world.loadedEntityList, inferMelee.getValue());
            if (source != null) hits.add(new Hit(source.entity.posX, source.entity.posZ, now, source.inferred));
            while (hits.size() > 4) hits.remove(0);
        }
        lastHurt = player.hurtTime;
    }

    @Override public void draw() {
        if (!syncWorld() || mc.currentScreen != null) return;
        long now = clock.getAsLong();
        hits.removeIf(hit -> now - hit.time >= duration.getValue().longValue());
        if (hits.isEmpty()) return;
        ScaledResolution sr = new ScaledResolution(mc);
        Color c = color.getColor();
        try (RenderState state = RenderState.capture()) {
            RenderState.setupWorldEffect();
            for (Hit hit : hits) {
                float alpha = (1 - (now - hit.time) / duration.getValue().floatValue()) * c.getAlpha() / 255f
                        * (hit.inferred ? 0.55f : 1);
                GlStateManager.pushMatrix();
                GlStateManager.translate(sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f, 0);
                GlStateManager.rotate(relativeYaw(hit.x - player.posX, hit.z - player.posZ, player.rotationYaw), 0, 0, 1);
                double r = radius.getValue();
                WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
                wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(0, -r - 6, 0).color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha).endVertex();
                wr.pos(-5, -r + 4, 0).color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha).endVertex();
                wr.pos(5, -r + 4, 0).color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha).endVertex();
                Tessellator.getInstance().draw();
                GlStateManager.popMatrix();
            }
        }
    }

    private static final class Hit {
        final double x, z; final long time; final boolean inferred;
        Hit(double x, double z, long time, boolean inferred) { this.x = x; this.z = z; this.time = time; this.inferred = inferred; }
    }
}
