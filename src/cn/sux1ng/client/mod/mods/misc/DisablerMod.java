package cn.sux1ng.client.mod.mods.misc;

import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.*;
import cn.sux1ng.client.mod.*;
import cn.sux1ng.client.movement.*;
import cn.sux1ng.client.value.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.world.World;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/** Selective packet profiles with a bounded FIFO, deadlines and connection ownership. */
public final class DisablerMod extends Mod {
    public ModeValue mode = new ModeValue("Mode", "Basic", new String[]{"Basic", "Grim", "NCP", "Custom"});
    public NumberValue delay = new NumberValue("PacketDelay", 150, 0, 750, 25).setVisibility(() -> mode.is("Grim") || mode.is("Custom"));
    public NumberValue queueLimit = new NumberValue("QueueLimit", 128, 16, 256, 16).setVisibility(() -> mode.is("Grim") || mode.is("Custom"));
    public BooleanValue transactions = new BooleanValue("DelayTransactions", true).setVisibility(() -> mode.is("Custom"));
    public BooleanValue keepAlive = new BooleanValue("DelayKeepAlive", false).setVisibility(() -> mode.is("Custom"));
    public BooleanValue deduplicate = new BooleanValue("Deduplicate", true).setVisibility(() -> mode.is("Basic") || mode.is("Custom"));
    public BooleanValue idlePackets = new BooleanValue("IdlePackets", true).setVisibility(() -> mode.is("NCP") || mode.is("Custom"));
    public BooleanValue grimPlace = new BooleanValue("GrimPlace", false).setVisibility(() -> mode.is("Grim") || mode.is("Custom"));
    private static final ScheduledExecutorService DEADLINES = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Meow Packet Deadlines"); thread.setDaemon(true); return thread;
    });
    private final Object lock = new Object();
    private final PacketBuffer buffer = new PacketBuffer(256);
    private final LongSupplier clock;
    private final AtomicBoolean boundary = new AtomicBoolean(), disconnected = new AtomicBoolean();
    private NetHandlerPlayClient connection;
    private EntityPlayerSP player;
    private World world;
    private Settings settings;
    private ScheduledFuture<?> deadline;
    private long due, epoch, deadlineSequence;
    private Boolean sprintState, sneakState, idleGround;
    private int heldSlot = -1, lastIdleTick = Integer.MIN_VALUE;
    private boolean running;

    public DisablerMod() { this(() -> System.nanoTime() / 1_000_000L); }
    public DisablerMod(LongSupplier clock) {
        super("Disabler", Category.MISC); this.clock = Objects.requireNonNull(clock);
        addValues(mode, delay, queueLimit, transactions, keepAlive, deduplicate, idlePackets, grimPlace); tagBy(mode);
    }
    @Override public void enable() { synchronized (lock) { running = true; reset(); settings = readSettings(); } }
    @Override public void disable() { synchronized (lock) { flushCurrent(); running = false; reset(); } }
    @Override public void update() { synchronized (lock) { if (context()) { refreshSettings(); if (boundary.getAndSet(false) || clock.getAsLong() >= due) flush(); } } }
    private Settings readSettings() {
        boolean custom = mode.is("Custom"), grim = mode.is("Grim"), ncp = mode.is("NCP");
        return new Settings(mode.getValue(), (grim || custom && transactions.getValue()) && delay.getValue() > 0,
                custom && keepAlive.getValue() && delay.getValue() > 0, custom || mode.is("Basic") ? deduplicate.getValue() : true,
                (ncp || custom) && idlePackets.getValue(), (grim || custom) && grimPlace.getValue(), delay.getValue().longValue(), queueLimit.getValue().intValue());
    }
    private void refreshSettings() {
        Settings next = readSettings();
        if (settings != null && !settings.same(next)) { flush(); resetStates(); }
        settings = next;
    }
    private boolean context() {
        if (!running || mc == null || mc.thePlayer == null || mc.theWorld == null || mc.isSingleplayer() || mc.getNetHandler() == null) {
            flushCurrent(); reset(); return false;
        }
        NetHandlerPlayClient current = mc.getNetHandler();
        if (connection != current) { reset(); connection = current; player = mc.thePlayer; world = mc.theWorld; }
        else if (player != mc.thePlayer || world != mc.theWorld) { flush(); resetStates(); player = mc.thePlayer; world = mc.theWorld; }
        if (disconnected.getAndSet(false)) { reset(); return false; }
        return true;
    }
    private void resetStates() { sprintState = sneakState = idleGround = null; heldSlot = -1; lastIdleTick = Integer.MIN_VALUE; }
    private void reset() { epoch++; cancelDeadline(); buffer.clear(); connection = null; player = null; world = null; due = 0; resetStates(); boundary.set(false); disconnected.set(false); }
    private void cancelDeadline() { deadlineSequence++; if (deadline != null) { deadline.cancel(false); deadline = null; } }
    @EventTarget public void onSend(PacketSendEvent event) {
        if (PacketReplay.active()) return;
        synchronized (lock) {
            if (!context()) return;
            if (settings == null) settings = readSettings();
            if (boundary.getAndSet(false) || buffer.size() > 0 && clock.getAsLong() >= due) { flush(); resetStates(); }
            Packet<?> packet = event.getPacket();
            if (settings.dedup && duplicate(packet)) { event.setCancelled(true); return; }
            if (settings.idle && packet instanceof C03PacketPlayer) {
                C03PacketPlayer movement = (C03PacketPlayer)packet;
                if (!movement.isMoving() && !movement.getRotating()) {
                    int tick = player.ticksExisted;
                    if (idleGround != null && idleGround == movement.isOnGround() && tick >= lastIdleTick && tick - lastIdleTick < 20) { event.setCancelled(true); return; }
                    idleGround = movement.isOnGround(); lastIdleTick = tick;
                } else { idleGround = null; lastIdleTick = player.ticksExisted; }
            }
            if (settings.place && packet instanceof C08PacketPlayerBlockPlacement) {
                C08PacketPlayerBlockPlacement placement = (C08PacketPlayerBlockPlacement)packet;
                int face = placement.getPlacedBlockDirection();
                if (face >= 0 && face <= 5) event.setPacket(new C08PacketPlayerBlockPlacement(placement.getPosition(),
                        face + 6 * (face + 1), placement.getStack(), placement.getPlacedBlockOffsetX(), placement.getPlacedBlockOffsetY(), placement.getPlacedBlockOffsetZ()));
            }
            boolean transaction = packet instanceof C0FPacketConfirmTransaction && ((C0FPacketConfirmTransaction)packet).getWindowId() == 0
                    && ((C0FPacketConfirmTransaction)packet).getUid() < 0;
            boolean delayed = settings.tx && transaction || settings.ping && packet instanceof C00PacketKeepAlive;
            // GUI confirmations are kept in sequence with any preceding queued ping transactions.
            if (packet instanceof C0FPacketConfirmTransaction && !delayed && buffer.size() > 0) flush();
            if (!delayed) return;
            if (buffer.size() >= settings.limit) flush();
            if (buffer.size() == 0) {
                due = clock.getAsLong() + settings.delay; long generation = epoch;
                cancelDeadline(); long job = deadlineSequence; deadline = DEADLINES.schedule(() -> flushDeadline(generation, job), settings.delay, TimeUnit.MILLISECONDS);
            }
            if (buffer.offer(packet)) event.setCancelled(true);
        }
    }
    private boolean duplicate(Packet<?> packet) {
        if (packet instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction.Action action = ((C0BPacketEntityAction)packet).getAction();
            if (action == C0BPacketEntityAction.Action.START_SPRINTING || action == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                boolean next = action == C0BPacketEntityAction.Action.START_SPRINTING; boolean same = sprintState != null && sprintState == next; sprintState = next; return same;
            }
            if (action == C0BPacketEntityAction.Action.START_SNEAKING || action == C0BPacketEntityAction.Action.STOP_SNEAKING) {
                boolean next = action == C0BPacketEntityAction.Action.START_SNEAKING; boolean same = sneakState != null && sneakState == next; sneakState = next; return same;
            }
        } else if (packet instanceof C09PacketHeldItemChange) {
            int slot = ((C09PacketHeldItemChange)packet).getSlotId(); boolean same = heldSlot == slot; heldSlot = slot; return same;
        }
        return false;
    }
    private void flushDeadline(long generation, long job) { synchronized (lock) { if (running && generation == epoch && job == deadlineSequence) flushCurrent(); } }
    private void flushCurrent() {
        if (mc != null && connection != null && connection == mc.getNetHandler() && !disconnected.get()) flush();
        else { cancelDeadline(); buffer.clear(); due = 0; }
    }
    private void flush() {
        cancelDeadline(); due = 0;
        if (connection == null) { buffer.clear(); return; }
        for (Packet<?> packet : buffer.takeAll()) PacketReplay.send(connection, packet);
    }
    @EventTarget public void onReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S40PacketDisconnect) disconnected.set(true);
        if (event.getPacket() instanceof S08PacketPlayerPosLook || event.getPacket() instanceof S07PacketRespawn
                || event.getPacket() instanceof S01PacketJoinGame) boundary.set(true);
    }
    public int pendingPackets() { return buffer.size(); }
    private static final class Settings {
        final String mode; final boolean tx, ping, dedup, idle, place; final long delay; final int limit;
        Settings(String mode, boolean tx, boolean ping, boolean dedup, boolean idle, boolean place, long delay, int limit) { this.mode = mode; this.tx = tx; this.ping = ping; this.dedup = dedup; this.idle = idle; this.place = place; this.delay = delay; this.limit = limit; }
        boolean same(Settings other) { return mode.equals(other.mode) && tx == other.tx && ping == other.ping && dedup == other.dedup && idle == other.idle && place == other.place && delay == other.delay && limit == other.limit; }
    }
}
