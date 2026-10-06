package cn.sux1ng.client.mod.mods.movement;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.*;
import cn.sux1ng.client.mod.*;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.movement.*;
import cn.sux1ng.client.value.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** Input compensation plus optional sword packet strategies; vanilla keeps acceleration and friction. */
public final class NoSlowMod extends Mod {
    public ModeValue mode = new ModeValue("Mode", "Vanilla", new String[]{"Vanilla", "Ground", "Air", "Adaptive", "NCP", "SwitchItem", "Grim"});
    public NumberValue amount = new NumberValue("Amount", 1.0, 0, 1, 0.1);
    public BooleanValue swords = new BooleanValue("Swords", true);
    public BooleanValue consume = new BooleanValue("Consume", true).setVisibility(() -> !packetMode());
    public BooleanValue bows = new BooleanValue("Bows", true).setVisibility(() -> !packetMode());
    public NumberValue pulseDelay = new NumberValue("PulseDelay", 150, 50, 250, 25).setVisibility(() -> mode.is("Grim"));
    private final LongSupplier clock;
    private final PacketBuffer buffer = new PacketBuffer(64);
    private final AtomicLong pauseUntil = new AtomicLong();
    private EntityPlayerSP player;
    private World world;
    private NetHandlerPlayClient connection;
    private ItemStack released, bufferedStack;
    private int preparedTick = Integer.MIN_VALUE;
    private volatile boolean buffering;
    private long started;
    private String previousMode;

    public NoSlowMod() { this(() -> System.nanoTime() / 1_000_000L); }
    public NoSlowMod(LongSupplier clock) { super("NoSlow", Category.MOVEMENT); this.clock = Objects.requireNonNull(clock); addValues(mode, amount, swords, consume, bows, pulseDelay); tagBy(mode); }
    private boolean packetMode() { return mode.is("NCP") || mode.is("SwitchItem") || mode.is("Grim"); }
    @Override public void enable() { clear(); pauseUntil.set(0); }
    @Override public void disable() { finish(); clear(); pauseUntil.set(0); }
    private void clear() { buffer.clear(); buffering = false; released = bufferedStack = null; player = null; world = null; connection = null; started = 0; preparedTick = Integer.MIN_VALUE; previousMode = null; }
    private boolean active() {
        if (!MovementSupport.active(mc) || clock.getAsLong() < pauseUntil.get()) return false;
        if (player != mc.thePlayer || world != mc.theWorld || connection != mc.getNetHandler()) { clear(); player = mc.thePlayer; world = mc.theWorld; connection = mc.getNetHandler(); }
        if (!mode.getValue().equals(previousMode)) { finish(); released = null; previousMode = mode.getValue(); }
        return true;
    }
    private boolean allowedItem() {
        if (amount.getValue() <= 0 || !player.isUsingItem() || player.getItemInUse() == null) return false;
        Item item = player.getItemInUse().getItem();
        if (packetMode()) {
            KillAuraMod aura = MeowClient.modManager == null ? null : MeowClient.modManager.getByClass(KillAuraMod.class);
            return swords.getValue() && item instanceof ItemSword && connection != null && (aura == null || !aura.isEnable() || aura.getTarget() == null);
        }
        return item instanceof ItemSword && swords.getValue() || item instanceof ItemBow && bows.getValue()
                || (item instanceof ItemFood || item instanceof ItemPotion || item instanceof ItemBucketMilk) && consume.getValue();
    }
    @Override public void update() {
        if (!active()) { if (clock.getAsLong() >= pauseUntil.get()) finish(); else buffer.clear(); buffering = false; released = null; return; }
        if (!allowedItem()) { finish(); released = null; return; }
        if (buffering && clock.getAsLong() - started >= pulseDelay.getValue()) finish();
    }
    @EventTarget public void onSlowdown(ItemSlowdownEvent event) {
        if (mc == null || event.player != mc.thePlayer || !active() || !allowedItem()) return;
        if (mode.is("Ground") && !player.onGround || mode.is("Air") && player.onGround) return;
        double strength = amount.getValue() * (mode.is("Adaptive") && !player.onGround ? 0.5 : 1);
        event.forward = event.strafe = (float)(0.2 + 0.8 * strength);
        if (mode.is("Grim") && !buffering && Math.hypot(player.movementInput.moveForward, player.movementInput.moveStrafe) > 1e-4) {
            bufferedStack = player.getItemInUse(); buffering = true; started = clock.getAsLong();
        }
    }
    @EventTarget public void onMotion(MotionEvent event) {
        if (!active() || !allowedItem() || !packetMode() || mode.is("Grim")) return;
        if (event.type == MotionEvent.Type.PRE) {
            if (preparedTick == player.ticksExisted || Math.hypot(player.movementInput.moveForward, player.movementInput.moveStrafe) < 1e-4) return;
            preparedTick = player.ticksExisted; released = player.getItemInUse();
            if (mode.is("NCP")) connection.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            else swapSlot();
        } else if (preparedTick == player.ticksExisted && released != null) {
            if (player.getItemInUse() == released && player.getHeldItem() == released) connection.addToSendQueue(new C08PacketPlayerBlockPlacement(released));
            released = null;
        }
    }
    @EventTarget public void onSend(PacketSendEvent event) {
        if (PacketReplay.active() || !buffering) return;
        if (mc == null || player != mc.thePlayer || world != mc.theWorld || connection != mc.getNetHandler()) { buffer.clear(); buffering = false; return; }
        if (clock.getAsLong() < pauseUntil.get()) { buffer.clear(); buffering = false; return; }
        Packet<?> packet = event.getPacket();
        if (packet instanceof C07PacketPlayerDigging || packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C09PacketHeldItemChange) { finish(); return; }
        if (packet instanceof C03PacketPlayer || packet instanceof C02PacketUseEntity || packet instanceof C0APacketAnimation) {
            if (buffer.offer(packet)) event.setCancelled(true);
            else { event.setCancelled(true); finish(packet); }
        }
    }
    private void swapSlot() {
        int slot = player.inventory.currentItem;
        PacketReplay.send(connection, new C09PacketHeldItemChange((slot + 1) % 9)); PacketReplay.send(connection, new C09PacketHeldItemChange(slot));
    }
    private void finish() { finish(null); }
    private void finish(Packet<?> trailing) {
        boolean same = mc != null && player == mc.thePlayer && world == mc.theWorld && connection != null && connection == mc.getNetHandler();
        boolean had = buffering; ItemStack ownedUse = released != null ? released : bufferedStack; buffering = false;
        if (!same || clock.getAsLong() < pauseUntil.get()) { buffer.clear(); return; }
        if (had) swapSlot();
        for (Packet<?> packet : buffer.takeAll()) PacketReplay.send(connection, packet);
        if (trailing != null) PacketReplay.send(connection, trailing);
        if ((had || released != null) && ownedUse != null && player.isUsingItem() && player.getItemInUse() == ownedUse && player.getHeldItem() == ownedUse)
            PacketReplay.send(connection, new C08PacketPlayerBlockPlacement(ownedUse));
        released = bufferedStack = null;
    }
    @EventTarget public void onReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook || event.getPacket() instanceof S09PacketHeldItemChange
                || event.getPacket() instanceof S07PacketRespawn || event.getPacket() instanceof S01PacketJoinGame || event.getPacket() instanceof S40PacketDisconnect) {
            pauseUntil.accumulateAndGet(clock.getAsLong() + 1000, Math::max); buffer.clear(); buffering = false;
        }
    }
    public int pendingPackets() { return buffer.size(); }
}
