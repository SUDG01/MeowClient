package cn.sux1ng.client.mod.mods.combat;

import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.EventUpdate;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.TimerUtil;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KillAuraMod extends Mod {

    // Settings
    public ModeValue targetMode = new ModeValue("Target", "Switch", new String[]{"Single", "Switch"});
    public NumberValue range = new NumberValue("Range", 4.2, 3.0, 6.0, 0.1);
    public NumberValue fov = new NumberValue("FOV", 360.0, 10.0, 360.0, 10.0);
    public NumberValue minCPS = new NumberValue("MinCPS", 10.0, 1.0, 20.0, 1.0);
    public NumberValue maxCPS = new NumberValue("MaxCPS", 13.0, 1.0, 20.0, 1.0);
    public ModeValue rotMode = new ModeValue("Rotation", "Silent", new String[]{"Lock", "Smooth", "Silent", "None"});
    public NumberValue maxTurnSpeed = new NumberValue("MaxTurnSpeed", 180.0, 10.0, 180.0, 10.0);
    public NumberValue smoothness = new NumberValue("Smoothness", 15.0, 1.0, 50.0, 1.0);

    // Targets
    public BooleanValue players = new BooleanValue("Players", true);
    public BooleanValue animals = new BooleanValue("Animals", false);
    public BooleanValue mobs = new BooleanValue("Mobs", true);
    public BooleanValue invisible = new BooleanValue("Invisibles", false);

    // Misc
    public BooleanValue autoBlock = new BooleanValue("AutoBlock", true);

    private EntityLivingBase target;
    private List<EntityLivingBase> targets = new ArrayList<>();
    private TimerUtil timer = new TimerUtil();
    private int switchIndex = 0;
    private Random random = new Random();

    public KillAuraMod() {
        super("KillAura", Category.COMBAT);
        addValues(targetMode, range, fov, minCPS, maxCPS, rotMode, maxTurnSpeed, smoothness, players, animals, mobs, invisible, autoBlock);
    }

    @Override
    public void disable() {
        super.disable();
        target = null;
        targets.clear();
        unblock();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // Pre: Rotations
        if (event.isPre()) {
            updateTargets();

            if (targets.isEmpty()) {
                target = null;
                unblock();
                return;
            }

            if (targetMode.is("Switch")) {
                if (switchIndex >= targets.size()) switchIndex = 0;
                target = targets.get(switchIndex);
            } else {
                target = targets.get(0);
            }

            handleRotations(event);
        }

        // Post: Attack & Block
        if (event.isPost() && target != null) {
            boolean shouldBlock = autoBlock.getValue() && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;

            if (shouldBlock) block();

            double minDelay = 1000.0 / maxCPS.getValue();
            double maxDelay = 1000.0 / minCPS.getValue();
            double randomDelay = minDelay + (maxDelay - minDelay) * random.nextDouble();

            if (timer.hasReached((long) randomDelay)) {
                if (shouldBlock) unblock();

                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, target);

                if (targetMode.is("Switch")) switchIndex++;
                if (shouldBlock) block();

                timer.reset();
            }
        }
    }

    private void handleRotations(EventUpdate event) {
        if (rotMode.is("None")) return;

        float[] destRotations = getRotations(target);
        float destYaw = destRotations[0];
        float destPitch = destRotations[1];

        float currentYaw = event.getYaw();
        float currentPitch = event.getPitch();

        float yaw = destYaw;
        float pitch = destPitch;

        // Apply Smoothing
        if (rotMode.is("Smooth")) {
            float smoothVal = smoothness.getValue().floatValue();
            yaw = nonLinearSmooth(currentYaw, destYaw, smoothVal);
            pitch = nonLinearSmooth(currentPitch, destPitch, smoothVal);
        }

        // Apply Speed Limit
        float maxSpeed = maxTurnSpeed.getValue().floatValue();
        yaw = updateRotation(currentYaw, yaw, maxSpeed);
        pitch = updateRotation(currentPitch, pitch, maxSpeed);

        // Apply to Event (Server)
        event.setYaw(yaw);
        event.setPitch(pitch);

        // Apply to Client (Visual) if not Silent
        if (!rotMode.is("Silent")) {
            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = pitch;
            mc.thePlayer.rotationYawHead = yaw;
            mc.thePlayer.renderYawOffset = yaw;
        }
    }

    private void updateTargets() {
        targets.clear();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase ent = (EntityLivingBase) entity;
                if (isValid(ent)) {
                    targets.add(ent);
                }
            }
        }
        targets.sort(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)));
    }

    private boolean isValid(EntityLivingBase ent) {
        if (ent == mc.thePlayer) return false;
        if (ent.getHealth() <= 0) return false;
        if (mc.thePlayer.getDistanceToEntity(ent) > range.getValue()) return false;
        if (ent.isInvisible() && !invisible.getValue()) return false;

        if (ent instanceof EntityPlayer && !players.getValue()) return false;
        if (ent instanceof EntityAnimal && !animals.getValue()) return false;
        if (ent instanceof EntityMob && !mobs.getValue()) return false;

        return isInFOV(ent);
    }

    private boolean isInFOV(EntityLivingBase entity) {
        if (fov.getValue() >= 360.0) return true;
        float[] rotations = getRotations(entity);
        float diff = Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - rotations[0]));
        return diff <= fov.getValue() / 2.0;
    }

    private float updateRotation(float current, float intended, float maxChange) {
        float change = MathHelper.wrapAngleTo180_float(intended - current);
        if (change > maxChange) change = maxChange;
        if (change < -maxChange) change = -maxChange;
        return current + change;
    }

    private float nonLinearSmooth(float current, float target, float smooth) {
        float diff = MathHelper.wrapAngleTo180_float(target - current);
        return current + (diff / smooth);
    }

    private float[] getRotations(Entity e) {
        double x = e.posX + (e.posX - e.lastTickPosX) - mc.thePlayer.posX;
        double z = e.posZ + (e.posZ - e.lastTickPosZ) - mc.thePlayer.posZ;
        double y = e.posY + e.getEyeHeight() - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * 180.0D / Math.PI));
        return new float[]{yaw, pitch};
    }

    private void block() {
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
        mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
    }

    private void unblock() {
        mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
    }

    public EntityLivingBase getTarget() {
        return target;
    }
}