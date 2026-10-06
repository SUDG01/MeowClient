package cn.sux1ng.client.mod.mods.movement;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.*;
import cn.sux1ng.client.mod.*;
import cn.sux1ng.client.movement.MovementSupport;
import cn.sux1ng.client.value.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.*;
import net.minecraft.world.World;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.Objects;

/** Fresh input selects jump intent; MoveEvent controls the final horizontal displacement. */
public final class SpeedMod extends Mod {
    public ModeValue mode = new ModeValue("Mode", "AutoJump", new String[]{"AutoJump", "Legit", "Vanilla", "Ground", "Strafe", "BHop", "LowHop", "NCP", "Grim"});
    public NumberValue speed = new NumberValue("Speed", 0.35, 0.1, 2.0, 0.05).setVisibility(() -> mode.is("Vanilla") || mode.is("Ground"));
    public NumberValue boost = new NumberValue("Boost", 1.35, 1, 2, 0.05).setVisibility(() -> mode.is("BHop") || mode.is("LowHop") || mode.is("NCP"));
    public NumberValue jumpHeight = new NumberValue("JumpHeight", 0.26, 0.15, 0.42, 0.01).setVisibility(() -> mode.is("LowHop"));
    public BooleanValue autoSprint = new BooleanValue("AutoSprint", true);
    public BooleanValue pauseUsing = new BooleanValue("PauseUsingItem", true);
    public BooleanValue pauseCorrection = new BooleanValue("PauseOnCorrection", true);
    public NumberValue recovery = new NumberValue("RecoveryTime", 1000, 250, 5000, 250);
    private final LongSupplier clock;
    private final AtomicLong pauseUntil = new AtomicLong();
    private EntityPlayerSP player;
    private World world;
    private String previousMode;
    private int inputTick = Integer.MIN_VALUE, moveTick = Integer.MIN_VALUE, motionTick = Integer.MIN_VALUE, airTicks;
    private boolean groundInput, ownsSprint;
    private double lastDistance;

    public SpeedMod() { this(() -> System.nanoTime() / 1_000_000L); }
    public SpeedMod(LongSupplier clock) { super("Speed", Category.MOVEMENT); this.clock = Objects.requireNonNull(clock); addValues(mode, speed, boost, jumpHeight, autoSprint, pauseUsing, pauseCorrection, recovery); tagBy(mode); }
    @Override public void enable() { reset(); }
    @Override public void disable() { releaseSprint(); reset(); }
    @Override public void update() { if (!MovementSupport.active(mc)) { releaseSprint(); reset(); } }
    private void reset() { player = null; world = null; previousMode = null; inputTick = moveTick = motionTick = Integer.MIN_VALUE; airTicks = 0; lastDistance = 0; ownsSprint = false; pauseUntil.set(0); }
    private void releaseSprint() {
        SprintMod sprint = MeowClient.modManager == null ? null : MeowClient.modManager.getByClass(SprintMod.class);
        if (ownsSprint && mc != null && mc.thePlayer == player && mc.theWorld == world && player.isSprinting()
                && mc.gameSettings != null && !mc.gameSettings.keyBindSprint.isKeyDown() && (sprint == null || !sprint.isEnable())) player.setSprinting(false);
        ownsSprint = false;
    }
    private boolean playable() {
        if (!MovementSupport.active(mc)) return false;
        if (player != mc.thePlayer || world != mc.theWorld || !mode.getValue().equals(previousMode)) {
            releaseSprint(); long paused = pauseUntil.get(); reset(); pauseUntil.set(paused); player = mc.thePlayer; world = mc.theWorld; previousMode = mode.getValue();
        }
        return clock.getAsLong() >= pauseUntil.get() && !player.capabilities.isFlying && !player.isSneaking()
                && !player.isInWater() && !player.isInLava() && !player.isOnLadder() && !player.isCollidedHorizontally
                && (!pauseUsing.getValue() || !player.isUsingItem());
    }
    @EventTarget public void onInput(MovementInputEvent event) {
        if (mc == null || event.player != mc.thePlayer || !playable() || inputTick == player.ticksExisted) return;
        inputTick = player.ticksExisted;
        boolean moving = Math.hypot(event.input.moveForward, event.input.moveStrafe) > 1e-4;
        if (!moving) { airTicks = 0; lastDistance = 0; return; }
        groundInput = player.onGround;
        if (autoSprint.getValue() && event.input.moveForward >= 0.8 && !event.input.sneak && !player.isUsingItem()
                && player.getFoodStats().getFoodLevel() > 6 && !player.isPotionActive(Potion.blindness)) {
            if (!player.isSprinting()) { player.setSprinting(true); ownsSprint = !mc.gameSettings.keyBindSprint.isKeyDown(); }
            if (mc.gameSettings.keyBindSprint.isKeyDown()) ownsSprint = false;
        }
        if (player.onGround) airTicks = 0; else airTicks++;
        boolean hopping = !mode.is("Ground") && !mode.is("Strafe");
        if (mode.is("Legit")) hopping = player.isSprinting() && event.input.moveForward >= 0.8;
        // Vanilla owns the actual takeoff and sprint impulse, including a manually held jump.
        if (hopping && player.onGround && player.motionY <= 0) event.input.jump = true;
    }
    @EventTarget public void onStrafe(StrafeEvent event) {
        if (!playable() || inputTick != player.ticksExisted) return;
        if (mode.is("LowHop") && groundInput && player.motionY > 0) {
            PotionEffect jump = player.isPotionActive(Potion.jump) ? player.getActivePotionEffect(Potion.jump) : null;
            player.motionY = jumpHeight.getValue() + (jump == null ? 0 : 0.1 * (jump.getAmplifier() + 1));
        }
    }
    @EventTarget public void onMove(MoveEvent event) {
        if (!playable() || inputTick != player.ticksExisted || moveTick == player.ticksExisted || mode.is("AutoJump") || mode.is("Legit")) return;
        moveTick = player.ticksExisted;
        float forward = player.movementInput.moveForward, strafe = player.movementInput.moveStrafe;
        if (Math.hypot(forward, strafe) < 1e-4) return;
        double base = MovementSupport.baseSpeed(player), desired = Math.hypot(event.x, event.z);
        if (mode.is("Ground") && !groundInput || mode.is("Grim") && !groundInput) return;
        if (mode.is("Vanilla") || mode.is("Ground")) desired = speed.getValue() * MovementSupport.potionFactor(player);
        else if (mode.is("Grim")) desired = Math.max(base, Math.min(0.36 * MovementSupport.potionFactor(player), desired));
        else if (mode.is("Strafe")) desired = Math.max(base, desired);
        else if (groundInput) desired = base * boost.getValue();
        else if (mode.is("NCP")) desired = Math.max(base, airTicks <= 1 ? lastDistance - 0.66 * (lastDistance - base) : lastDistance * (158.0 / 159.0));
        else desired = Math.max(base, lastDistance * 0.91 + (player.isSprinting() ? 0.026 : 0.02));
        double[] direction = MovementSupport.direction(forward, strafe, player.rotationYaw, desired);
        event.x = direction[0]; event.z = direction[1]; player.motionX = event.x; player.motionZ = event.z;
    }
    @EventTarget public void onMotion(MotionEvent event) {
        if (event.type != MotionEvent.Type.POST || !MovementSupport.active(mc) || motionTick == mc.thePlayer.ticksExisted) return;
        motionTick = mc.thePlayer.ticksExisted;
        if (player == mc.thePlayer && world == mc.theWorld) lastDistance = Math.hypot(player.posX - player.prevPosX, player.posZ - player.prevPosZ);
    }
    @EventTarget public void onReceive(PacketReceiveEvent event) {
        if (mc == null || mc.thePlayer == null) return;
        if (pauseCorrection.getValue() && event.getPacket() instanceof S08PacketPlayerPosLook)
            pauseUntil.accumulateAndGet(clock.getAsLong() + recovery.getValue().longValue(), Math::max);
        if (event.getPacket() instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity)event.getPacket()).getEntityID() == mc.thePlayer.getEntityId()
                || event.getPacket() instanceof S27PacketExplosion) pauseUntil.accumulateAndGet(clock.getAsLong() + 500, Math::max);
    }
}
