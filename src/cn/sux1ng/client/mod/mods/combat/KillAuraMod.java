package cn.sux1ng.client.mod.mods.combat;

import cn.sux1ng.client.combat.AimMotion;
import cn.sux1ng.client.combat.AttackRhythm;
import cn.sux1ng.client.targeting.TargetRules;
import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.AttackEvent;
import cn.sux1ng.client.events.impl.MotionEvent;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.LongSupplier;

/** PRE selects and turns; POST validates the submitted look and performs at most one attack. */
public class KillAuraMod extends Mod {
    public ModeValue targetMode = new ModeValue("Target", "Switch", new String[]{"Single", "Switch"});
    public NumberValue range = new NumberValue("Range", 4.2, 3.0, 6.0, 0.1);
    public NumberValue fov = new NumberValue("FOV", 120, 10, 360, 10);
    public NumberValue minCPS = new NumberValue("MinCPS", 10, 1, 20, 1);
    public NumberValue maxCPS = new NumberValue("MaxCPS", 13, 1, 20, 1);
    public ModeValue rotMode = new ModeValue("Rotation", "Smooth", new String[]{"Lock", "Smooth", "Silent", "None"});
    public NumberValue maxTurnSpeed = new NumberValue("MaxTurnSpeed", 45, 5, 90, 5)
            .setVisibility(() -> !rotMode.is("None"));
    public NumberValue smoothness = new NumberValue("Smoothness", 15, 1, 50, 1)
            .setVisibility(() -> rotMode.is("Smooth") || rotMode.is("Silent"));
    public NumberValue reactionDelay = new NumberValue("ReactionDelay", 150, 0, 500, 25);
    public NumberValue switchDelay = new NumberValue("SwitchDelay", 600, 200, 2000, 50)
            .setVisibility(() -> targetMode.is("Switch"));
    public NumberValue aimVariation = new NumberValue("AimVariation", 0.12, 0, 0.3, 0.01)
            .setVisibility(() -> !rotMode.is("None"));
    public BooleanValue players = new BooleanValue("Players", true).setVisibility(() -> !TargetRules.isUnified());
    public BooleanValue animals = new BooleanValue("Animals", false).setVisibility(() -> !TargetRules.isUnified());
    public BooleanValue mobs = new BooleanValue("Mobs", true).setVisibility(() -> !TargetRules.isUnified());
    public BooleanValue invisible = new BooleanValue("Invisibles", false).setVisibility(() -> !TargetRules.isUnified());
    public BooleanValue autoBlock = new BooleanValue("AutoBlock", true);

    private final LongSupplier clock;
    private final Random random;
    private final AimMotion aim = new AimMotion();
    private final AttackRhythm rhythm;
    private final List<EntityLivingBase> targets = new ArrayList<>();
    private EntityLivingBase target;
    private EntityPlayerSP player, blockedPlayer;
    private World world, blockedWorld;
    private ItemStack blockedStack;
    private boolean ownsBlock, prepared, selfAttack;
    private int preparedTick = Integer.MIN_VALUE, lastPreTick = Integer.MIN_VALUE, lastAttackTick = Integer.MIN_VALUE;
    private long selectedAt, nextAimChange;
    private int targetHits;
    private String previousTargetMode, previousRotation;
    private double offsetX, offsetY, offsetZ, goalX, goalY, goalZ;
    private float preparedYaw, preparedPitch;

    public KillAuraMod() { this(() -> System.nanoTime() / 1000000L, new Random()); }
    public KillAuraMod(LongSupplier clock, Random random) {
        super("KillAura", Category.COMBAT);
        this.clock = Objects.requireNonNull(clock, "clock"); this.random = Objects.requireNonNull(random, "random");
        rhythm = new AttackRhythm(random);
        addValues(targetMode, range, fov, minCPS, maxCPS, rotMode, maxTurnSpeed, smoothness,
                reactionDelay, switchDelay, aimVariation, players, animals, mobs, invisible, autoBlock);
    }
    @Override public void enable() { reset(); }
    @Override public void disable() { reset(); }
    @Override public void update() { if (!playable()) reset(); }
    @Override public void render(float partialTicks) { if (!playable()) reset(); }

    private boolean playable() {
        return mc != null && mc.thePlayer != null && mc.theWorld != null && mc.playerController != null
                && mc.currentScreen == null && mc.inGameHasFocus && !mc.isGamePaused()
                && !mc.thePlayer.isDead && mc.thePlayer.getHealth() > 0 && !mc.thePlayer.isRiding() && !mc.playerController.isSpectator();
    }
    private void reset() {
        releaseBlock(); target = null; targets.clear(); player = null; world = null; prepared = false;
        lastPreTick = preparedTick = lastAttackTick = Integer.MIN_VALUE;
        previousTargetMode = previousRotation = null; targetHits = 0; aim.reset(); rhythm.reset();
    }
    private boolean syncContext() {
        if (!playable()) { reset(); return false; }
        if (player != mc.thePlayer || world != mc.theWorld) { reset(); player = mc.thePlayer; world = mc.theWorld; }
        // A manual use action takes precedence over automatic combat.
        if (ownsBlock && (player.getItemInUse() != blockedStack || mc.gameSettings.keyBindUseItem.isKeyDown())) relinquishBlock();
        if (player.isUsingItem() && !ownsBlock) { clearTarget(); return false; }
        return true;
    }
    private void clearTarget() {
        releaseBlock(); target = null; targets.clear(); prepared = false; aim.reset(); rhythm.reset(); targetHits = 0;
    }

    @EventTarget public void onMotion(MotionEvent event) {
        if (!syncContext()) return;
        long now = clock.getAsLong();
        if (event.type == MotionEvent.Type.PRE) {
            if (lastPreTick == player.ticksExisted) {
                if (target != null && !rotMode.is("None")) { event.yaw = preparedYaw; event.pitch = preparedPitch; }
                return;
            }
            lastPreTick = player.ticksExisted; prepared = false;
            updateTargets();
            if (targets.isEmpty()) { clearTarget(); return; }
            EntityLivingBase next = selectTarget(now);
            if (next == null) { clearTarget(); return; }
            boolean modeChanged = !targetMode.getValue().equals(previousTargetMode) || !rotMode.getValue().equals(previousRotation);
            if (target != next || modeChanged) {
                releaseBlock(); target = next; selectedAt = now; targetHits = 0;
                rhythm.configure(minCPS.getValue(), maxCPS.getValue()); rhythm.acquire(now, reactionDelay.getValue().longValue());
                aim.targetChanged(); chooseAimPoint(now); offsetX = goalX; offsetY = goalY; offsetZ = goalZ;
                if (modeChanged) aim.reset();
            }
            previousTargetMode = targetMode.getValue(); previousRotation = rotMode.getValue();
            rhythm.configure(minCPS.getValue(), maxCPS.getValue());
            if (!rotMode.is("None")) {
                float[] desired = desiredRotation(now);
                float[] rotated = aim.turn(rotMode.getValue(), event.yaw, event.pitch, desired[0], desired[1],
                        maxTurnSpeed.getValue().floatValue(), smoothness.getValue().floatValue(), mc.gameSettings.mouseSensitivity);
                event.yaw = rotated[0]; event.pitch = rotated[1];
                if (!rotMode.is("Silent")) { player.rotationYaw = event.yaw; player.rotationPitch = event.pitch; }
            }
            prepared = true; preparedTick = player.ticksExisted;
            preparedYaw = event.yaw; preparedPitch = event.pitch;
            return;
        }
        if (!prepared || preparedTick != player.ticksExisted) return;
        prepared = false;
        if (!isValid(target) || !world.loadedEntityList.contains(target)) { clearTarget(); return; }
        if (!rotMode.is("None")) aim.observe(event.yaw, event.pitch);
        boolean aligned = rayTarget(event.yaw, event.pitch) == target;
        rhythm.aligned(aligned, now);
        if (!autoBlock.getValue() || !aligned) releaseBlock();
        if (rhythm.ready(now, player.ticksExisted)) {
            releaseBlock();
            selfAttack = true;
            try {
                player.swingItem(); mc.playerController.attackEntity(player, target);
                rhythm.attacked(now, player.ticksExisted); targetHits++; lastAttackTick = player.ticksExisted;
            } finally { selfAttack = false; }
            return; // A later movement tick may re-enter blocking; do not release/attack/block in one tick.
        }
        if (aligned && rhythm.reactionPassed(now) && player.ticksExisted > lastAttackTick) beginBlock();
    }

    @EventTarget public void onAttack(AttackEvent event) {
        if (selfAttack || mc == null || event.getPlayer() != mc.thePlayer) return;
        rhythm.configure(minCPS.getValue(), maxCPS.getValue()); rhythm.externalAttack(clock.getAsLong(), mc.thePlayer.ticksExisted);
        lastAttackTick = mc.thePlayer.ticksExisted;
        if (event.getTarget() == target) targetHits++;
    }

    private void updateTargets() {
        targets.clear();
        for (Entity entity : world.loadedEntityList) if (entity instanceof EntityLivingBase && isValid((EntityLivingBase)entity)) targets.add((EntityLivingBase)entity);
        targets.sort(Comparator.comparingDouble((EntityLivingBase entity) -> player.getDistanceSqToEntity(entity)).thenComparingInt(Entity::getEntityId));
    }
    private EntityLivingBase selectTarget(long now) {
        if (rotMode.is("None")) {
            Entity entity = rayTarget(player.rotationYaw, player.rotationPitch);
            return entity instanceof EntityLivingBase && targets.contains(entity) ? (EntityLivingBase)entity : null;
        }
        if (target == null || !targets.contains(target)) return targets.get(0);
        if (!targetMode.is("Switch") || targetHits == 0 || now - selectedAt < switchDelay.getValue().longValue() || targets.size() < 2) return target;
        EntityLivingBase next = null, first = null;
        for (EntityLivingBase candidate : targets) {
            if (first == null || candidate.getEntityId() < first.getEntityId()) first = candidate;
            if (candidate.getEntityId() > target.getEntityId() && (next == null || candidate.getEntityId() < next.getEntityId())) next = candidate;
        }
        return next == null ? first : next;
    }
    private boolean isValid(EntityLivingBase entity) {
        if (entity == null || entity == player || entity.worldObj != world || entity.isDead || entity.getHealth() <= 0
                || player.getDistanceSqToEntity(entity) > range.getValue() * range.getValue()) return false;
        if (!TargetRules.canAttack(entity, players.getValue(), mobs.getValue(), animals.getValue(), invisible.getValue())) return false;
        float yaw = (float)Math.toDegrees(Math.atan2(entity.posZ - player.posZ, entity.posX - player.posX)) - 90;
        return fov.getValue() >= 360 || Math.abs(AimMotion.wrap(player.rotationYaw - yaw)) <= fov.getValue() * 0.5;
    }
    private void chooseAimPoint(long now) {
        double variation = aimVariation.getValue();
        goalX = (random.nextDouble() - 0.5) * variation;
        goalY = (random.nextDouble() - 0.5) * variation;
        goalZ = (random.nextDouble() - 0.5) * variation;
        nextAimChange = now + 350 + random.nextInt(350);
    }
    private float[] desiredRotation(long now) {
        if (now >= nextAimChange) chooseAimPoint(now);
        offsetX += (goalX - offsetX) * 0.2; offsetY += (goalY - offsetY) * 0.2; offsetZ += (goalZ - offsetZ) * 0.2;
        AxisAlignedBB box = target.getEntityBoundingBox();
        double x = (box.minX + box.maxX) * 0.5 + offsetX * (box.maxX - box.minX) - player.posX;
        double y = box.minY + (0.65 + offsetY) * (box.maxY - box.minY) - player.posY - player.getEyeHeight();
        double z = (box.minZ + box.maxZ) * 0.5 + offsetZ * (box.maxZ - box.minZ) - player.posZ;
        return new float[]{(float)Math.toDegrees(Math.atan2(z, x)) - 90,
                (float)-Math.toDegrees(Math.atan2(y, Math.hypot(x, z)))};
    }
    private Entity rayTarget(float yaw, float pitch) {
        Vec3 from = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch), reach = range.getValue();
        Vec3 to = from.addVector(-Math.sin(y) * Math.cos(p) * reach, -Math.sin(p) * reach, Math.cos(y) * Math.cos(p) * reach);
        MovingObjectPosition block = world.rayTraceBlocks(from, to, false, true, false);
        double closest = block == null ? reach * reach : from.squareDistanceTo(block.hitVec);
        Entity hit = null;
        for (Entity entity : world.loadedEntityList) {
            if (entity == player || entity.isDead || !entity.canBeCollidedWith()) continue;
            AxisAlignedBB box = entity.getEntityBoundingBox().expand(entity.getCollisionBorderSize(), entity.getCollisionBorderSize(), entity.getCollisionBorderSize());
            MovingObjectPosition intercept = box.calculateIntercept(from, to);
            double distance = box.isVecInside(from) ? 0 : intercept == null ? Double.POSITIVE_INFINITY : from.squareDistanceTo(intercept.hitVec);
            if (distance < closest) { closest = distance; hit = entity; }
        }
        return hit;
    }
    private void beginBlock() {
        ItemStack held = player.getHeldItem();
        if (!autoBlock.getValue() || ownsBlock || player.isUsingItem() || held == null || !(held.getItem() instanceof ItemSword)) return;
        mc.playerController.sendUseItem(player, world, held);
        if (player.getItemInUse() == held) { ownsBlock = true; blockedStack = held; blockedPlayer = player; blockedWorld = world; }
    }
    private void releaseBlock() {
        boolean release = ownsBlock && mc != null && mc.thePlayer == blockedPlayer && mc.theWorld == blockedWorld
                && mc.playerController != null && blockedPlayer.getItemInUse() == blockedStack;
        EntityPlayerSP owner = blockedPlayer; relinquishBlock();
        if (release) mc.playerController.onStoppedUsingItem(owner);
    }
    private void relinquishBlock() { ownsBlock = false; blockedStack = null; blockedPlayer = null; blockedWorld = null; }
    public EntityLivingBase getTarget() { return target; }
}
