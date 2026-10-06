package cn.sux1ng.client.mod.mods.combat;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.combat.AimMotion;
import cn.sux1ng.client.combat.FrameAimMotion;
import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.CameraEvent;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Visible camera assistance only; clicking, attacking and item use remain with the player. */
public final class AimbotMod extends Mod {
    public ModeValue mode = new ModeValue("Mode", "Assist", new String[]{"Assist", "Track"});
    public ModeValue aimPoint = new ModeValue("AimPoint", "Closest", new String[]{"Closest", "Body", "Head"});
    public NumberValue range = new NumberValue("Range", 3.6, 1, 6, 0.1);
    public NumberValue fov = new NumberValue("FOV", 45, 10, 120, 5);
    public NumberValue response = new NumberValue("ResponseTime", 220, 80, 600, 20);
    public NumberValue horizontalSpeed = new NumberValue("HorizontalSpeed", 80, 10, 240, 5);
    public NumberValue verticalSpeed = new NumberValue("VerticalSpeed", 40, 0, 120, 5);
    public NumberValue strength = new NumberValue("Strength", 0.65, 0.1, 1, 0.05);
    public NumberValue reactionDelay = new NumberValue("ReactionDelay", 100, 0, 400, 25);
    public NumberValue deadZone = new NumberValue("DeadZone", 0.12, 0, 1, 0.02);
    public BooleanValue clickOnly = new BooleanValue("ClickOnly", true);
    public BooleanValue weaponOnly = new BooleanValue("WeaponOnly", true);
    public BooleanValue manualPriority = new BooleanValue("MousePriority", true);
    public BooleanValue stopOnTarget = new BooleanValue("StopOnTarget", true);
    public BooleanValue ignoreTeams = new BooleanValue("IgnoreTeams", true);
    public BooleanValue players = new BooleanValue("Players", true);
    public BooleanValue mobs = new BooleanValue("Mobs", false);
    public BooleanValue animals = new BooleanValue("Animals", false);

    private final LongSupplier clock;
    private final FrameAimMotion motion = new FrameAimMotion();
    private EntityLivingBase target;
    private EntityPlayerSP player;
    private World world;
    private long lastFrame, acquiredAt, yieldUntil;

    public AimbotMod() { this(System::nanoTime); }
    public AimbotMod(LongSupplier clock) {
        super("Aimbot", Category.COMBAT); this.clock = Objects.requireNonNull(clock, "clock");
        addValues(mode, aimPoint, range, fov, response, horizontalSpeed, verticalSpeed, strength, reactionDelay,
                deadZone, clickOnly, weaponOnly, manualPriority, stopOnTarget, ignoreTeams, players, mobs, animals);
    }
    @Override public void enable() { reset(); }
    @Override public void disable() { reset(); }
    private void reset() { target = null; player = null; world = null; lastFrame = acquiredAt = yieldUntil = 0; motion.reset(); }
    private void dropTarget() { target = null; acquiredAt = 0; motion.reset(); }
    private boolean allowed(CameraEvent event) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null || mc.playerController == null || mc.gameSettings == null
                || !event.focused || !mc.inGameHasFocus || mc.currentScreen != null || mc.isGamePaused()
                || mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0 || mc.thePlayer.isUsingItem() || mc.playerController.isSpectator()) return false;
        if (clickOnly.getValue() && !mc.gameSettings.keyBindAttack.isKeyDown()) return false;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (weaponOnly.getValue() && (held == null || !(held.getItem() instanceof ItemSword) && !(held.getItem() instanceof ItemAxe))) return false;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mc.gameSettings.keyBindAttack.isKeyDown()) return false;
        KillAuraMod aura = MeowClient.modManager == null ? null : MeowClient.modManager.getByClass(KillAuraMod.class);
        return aura == null || !aura.isEnable() || aura.getTarget() == null || aura.rotMode.is("None");
    }
    @EventTarget public void onCamera(CameraEvent event) {
        long now = clock.getAsLong();
        if (!allowed(event)) { reset(); return; }
        if (player != mc.thePlayer || world != mc.theWorld) { reset(); player = mc.thePlayer; world = mc.theWorld; }
        double dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000.0; lastFrame = now;
        if (dt <= 0) return;
        if (dt > 0.1) { dropTarget(); return; }
        Vec3 eyes = eyes(event.partialTicks);
        if (!world.loadedEntityList.contains(target) || !valid(target, eyes, event.partialTicks)) {
            target = chooseTarget(eyes, event.partialTicks); acquiredAt = now; motion.reset();
        }
        if (target == null || now - acquiredAt < reactionDelay.getValue() * 1_000_000.0) return;
        float[] wanted = rotations(eyes, point(target, eyes, event.partialTicks));
        float yawError = AimMotion.wrap(wanted[0] - player.rotationYaw), pitchError = wanted[1] - player.rotationPitch;
        if (manualPriority.getValue()) {
            boolean opposing = event.mouseYaw * yawError < 0 && Math.abs(event.mouseYaw) > 0.02
                    || event.mousePitch * pitchError < 0 && Math.abs(event.mousePitch) > 0.02;
            if (opposing || Math.hypot(event.mouseYaw, event.mousePitch) > Math.max(3, horizontalSpeed.getValue() * dt * 2.5)) {
                yieldUntil = now + 120_000_000L; motion.reset(); return;
            }
            if (now < yieldUntil) return;
        }
        if (stopOnTarget.getValue() && onTarget(target, eyes, event.partialTicks)) { motion.reset(); return; }
        double time = response.getValue() * (mode.is("Assist") ? 1.25 : 1);
        float[] delta = motion.step(yawError, pitchError, dt, time, horizontalSpeed.getValue(), verticalSpeed.getValue(),
                strength.getValue(), deadZone.getValue());
        player.setAngles(delta[0] / 0.15f, -delta[1] / 0.15f);
    }
    private boolean valid(EntityLivingBase entity, Vec3 eyes, float partialTicks) {
        if (entity == null || entity == player || entity.isDead || entity.getHealth() <= 0 || entity.worldObj != world
                || entity.isInvisible()) return false;
        boolean type = entity instanceof EntityPlayer && players.getValue() && !((EntityPlayer)entity).isSpectator()
                || entity instanceof EntityMob && mobs.getValue() || entity instanceof EntityAnimal && animals.getValue();
        if (!type || ignoreTeams.getValue() && player.isOnSameTeam(entity)) return false;
        AxisAlignedBB box = box(entity, partialTicks);
        Vec3 nearest = clamp(eyes, box, 0);
        if (eyes.squareDistanceTo(nearest) > range.getValue() * range.getValue()) return false;
        Vec3 aim = point(entity, eyes, partialTicks); float[] angles = rotations(eyes, aim);
        if (Math.hypot(AimMotion.wrap(angles[0] - player.rotationYaw), angles[1] - player.rotationPitch) > fov.getValue() / 2) return false;
        return world.rayTraceBlocks(eyes, aim, false, true, false) == null;
    }
    private EntityLivingBase chooseTarget(Vec3 eyes, float partialTicks) {
        EntityLivingBase best = null; double score = Double.POSITIVE_INFINITY;
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || !valid((EntityLivingBase)entity, eyes, partialTicks)) continue;
            float[] angles = rotations(eyes, point((EntityLivingBase)entity, eyes, partialTicks));
            double value = Math.hypot(AimMotion.wrap(angles[0] - player.rotationYaw), angles[1] - player.rotationPitch)
                    + Math.sqrt(player.getDistanceSqToEntity(entity)) * 0.3;
            if (value < score) { score = value; best = (EntityLivingBase)entity; }
        }
        return best;
    }
    private Vec3 eyes(float partialTicks) {
        return new Vec3(player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks,
                player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.getEyeHeight(),
                player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks);
    }
    private AxisAlignedBB box(Entity entity, float partialTicks) {
        return entity.getEntityBoundingBox().offset((entity.lastTickPosX - entity.posX) * (1 - partialTicks),
                (entity.lastTickPosY - entity.posY) * (1 - partialTicks), (entity.lastTickPosZ - entity.posZ) * (1 - partialTicks));
    }
    private Vec3 point(EntityLivingBase entity, Vec3 eyes, float partialTicks) {
        AxisAlignedBB box = box(entity, partialTicks);
        if (aimPoint.is("Head") || aimPoint.is("Body")) return new Vec3((box.minX + box.maxX) * 0.5,
                box.minY + (box.maxY - box.minY) * (aimPoint.is("Head") ? 0.9 : 0.65), (box.minZ + box.maxZ) * 0.5);
        Vec3 center = new Vec3((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
        Vec3 look = look(player.rotationYaw, player.rotationPitch);
        double depth = Math.max(0, center.subtract(eyes).dotProduct(look));
        return clamp(eyes.addVector(look.xCoord * depth, look.yCoord * depth, look.zCoord * depth), box, 0.03);
    }
    private static Vec3 clamp(Vec3 point, AxisAlignedBB box, double margin) {
        return new Vec3(Math.max(box.minX + margin, Math.min(box.maxX - margin, point.xCoord)),
                Math.max(box.minY + margin, Math.min(box.maxY - margin, point.yCoord)),
                Math.max(box.minZ + margin, Math.min(box.maxZ - margin, point.zCoord)));
    }
    private boolean onTarget(Entity entity, Vec3 eyes, float partialTicks) {
        AxisAlignedBB box = box(entity, partialTicks); Vec3 look = look(player.rotationYaw, player.rotationPitch);
        return box.isVecInside(eyes) || box.calculateIntercept(eyes, eyes.addVector(look.xCoord * range.getValue(), look.yCoord * range.getValue(), look.zCoord * range.getValue())) != null;
    }
    private static Vec3 look(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }
    private static float[] rotations(Vec3 from, Vec3 to) {
        double x = to.xCoord - from.xCoord, y = to.yCoord - from.yCoord, z = to.zCoord - from.zCoord;
        return new float[]{(float)Math.toDegrees(Math.atan2(z, x)) - 90, (float)-Math.toDegrees(Math.atan2(y, Math.hypot(x, z)))};
    }
    public EntityLivingBase getTarget() { return target; }
}
