import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.events.EventManager;
import cn.sux1ng.client.events.impl.*;
import cn.sux1ng.client.mod.*;
import cn.sux1ng.client.mod.mods.misc.*;
import cn.sux1ng.client.mod.mods.movement.NoSlowMod;
import cn.sux1ng.client.movement.PacketReplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.*;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.*;
import io.netty.channel.embedded.EmbeddedChannel;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Outbound packet order, receive boundaries, deadlines and the real NetworkManager hook. */
public class PacketFlowRegressionTest {
    private static final Unsafe UNSAFE = unsafe();
    private static FixtureMinecraft mc;
    private static Player player;
    private static Connection connection;
    private static AtomicLong clock;
    private static int failures;
    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        check("Disabler requires Blatant and is off in a default registry", () -> {
            setup(); DisablerMod disabler = new DisablerMod(clock::get); MeowClient.modManager.getMods().add(disabler);
            MeowClient.modManager.getByClass(BlatantMod.class).setEnable(false); disabler.setEnable(true);
            require(!disabler.isEnable() && BlatantPolicy.requiresBlatant(disabler), "Disabler bypassed the permission switch");
        });
        check("Basic removes duplicate sprint and slot state without removing transitions", () -> {
            setup(); DisablerMod disabler = disabler("Basic");
            send(new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING)); send(new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING));
            send(new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING)); send(new C09PacketHeldItemChange(0)); send(new C09PacketHeldItemChange(0)); send(new C09PacketHeldItemChange(1));
            require(connection.sent.size() == 4, "duplicate state handling lost a transition: " + connection.sent.size());
            disabler.deduplicate.setValue(false); disabler.update(); send(new C09PacketHeldItemChange(1)); send(new C09PacketHeldItemChange(1)); require(connection.sent.size() == 6, "disabled deduplication still cancelled packets");
        });
        check("Grim delays only negative window-zero confirmations and preserves original order", () -> {
            setup(); DisablerMod disabler = disabler("Grim"); disabler.delay.setValue(500.0); disabler.update();
            Packet<?> one = new C0FPacketConfirmTransaction(0, (short)-1, true), two = new C0FPacketConfirmTransaction(0, (short)-2, true);
            send(one); send(two); Packet<?> alive = new C00PacketKeepAlive(123); send(alive);
            require(disabler.pendingPackets() == 2 && connection.sent.size() == 1 && connection.sent.get(0) == alive, "critical heartbeat was delayed by the preset");
            clock.addAndGet(500); disabler.update(); require(connection.sent.size() == 3 && connection.sent.get(1) == one && connection.sent.get(2) == two, "queued confirmations were reordered or copied");
            Packet<?> gui = new C0FPacketConfirmTransaction(4, (short)3, true); send(gui); require(connection.sent.get(3) == gui, "GUI confirmation was delayed");
        });
        check("GUI barriers and queue limits flush acknowledgements without loss", () -> {
            setup(); DisablerMod disabler = disabler("Grim"); disabler.delay.setValue(750.0); disabler.queueLimit.setValue(16.0); disabler.update();
            for (int i = 1; i <= 17; i++) send(new C0FPacketConfirmTransaction(0, (short)-i, true));
            require(connection.sent.size() == 16 && disabler.pendingPackets() == 1, "queue limit did not flush its original FIFO");
            Packet<?> gui = new C0FPacketConfirmTransaction(1, (short)1, true); send(gui);
            require(connection.sent.size() == 18 && disabler.pendingPackets() == 0 && connection.sent.get(17) == gui, "GUI packet passed pending acknowledgements");
        });
        check("a packet deadline drains even when no client tick runs", () -> {
            setup(); DisablerMod disabler = disabler("Grim"); disabler.delay.setValue(50.0); disabler.update();
            Packet<?> packet = new C0FPacketConfirmTransaction(0, (short)-1, true); send(packet);
            long limit = System.nanoTime() + 2_000_000_000L;
            while (connection.sent.isEmpty() && System.nanoTime() < limit) Thread.sleep(5);
            require(connection.sent.size() == 1 && connection.sent.get(0) == packet && disabler.pendingPackets() == 0, "deadline depended on tick processing");
        });
        check("correction boundaries flush acknowledgements and pass teleport responses unchanged", () -> {
            setup(); DisablerMod disabler = disabler("Grim"); send(new C0FPacketConfirmTransaction(0, (short)-1, true));
            PacketReceiveEvent correction = correction(); EventManager.call(correction); Packet<?> reply = new C03PacketPlayer.C06PacketPlayerPosLook(1, 2, 3, 4, 5, false); send(reply);
            require(!correction.isCancelled() && connection.sent.size() == 2 && connection.sent.get(1) == reply && disabler.pendingPackets() == 0, "correction was cancelled or moved behind a queue");
        });
        check("disabling drains the current connection and a new connection never receives old packets", () -> {
            setup(); DisablerMod disabler = disabler("Grim"); send(new C0FPacketConfirmTransaction(0, (short)-1, true)); disabler.setEnable(false);
            require(connection.sent.size() == 1 && disabler.pendingPackets() == 0, "disable discarded a live acknowledgement");
            disabler.setEnable(true); send(new C0FPacketConfirmTransaction(0, (short)-2, true)); Connection old = connection;
            connection = newConnection(); set(EntityPlayerSP.class, player, "sendQueue", connection); disabler.update(); disabler.setEnable(false);
            require(connection.sent.isEmpty() && old.sent.size() == 1, "old acknowledgement leaked into the new session");
        });
        check("NCP idle reduction retains state changes and a periodic movement heartbeat", () -> {
            setup(); DisablerMod disabler = disabler("NCP");
            for (int tick = 0; tick <= 40; tick++) { player.ticksExisted = tick; send(new C03PacketPlayer(true)); }
            require(connection.sent.size() == 3, "idle movement heartbeat was lost: " + connection.sent.size());
            send(new C03PacketPlayer(false)); send(new C03PacketPlayer.C04PacketPlayerPosition(1, 0, 0, false)); require(connection.sent.size() == 5, "real movement or ground transition was cancelled");
        });
        check("optional placement alias keeps face meaning and leaves item-use packets intact", () -> {
            setup(); DisablerMod disabler = disabler("Grim"); disabler.grimPlace.setValue(true); disabler.update();
            for (int face = 0; face < 6; face++) { send(new C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, face, player.getHeldItem(), 0.2f, 0.3f, 0.4f)); C08PacketPlayerBlockPlacement packet = (C08PacketPlayerBlockPlacement)connection.sent.get(face); require(packet.getPlacedBlockDirection() % 6 == face, "placement changed its vanilla face"); }
            Packet<?> use = new C08PacketPlayerBlockPlacement(player.getHeldItem()); send(use); require(connection.sent.get(6) == use, "right-click item use was remapped");
        });
        check("NCP and SwitchItem sword strategies run PRE, movement, POST once", () -> {
            for (String mode : new String[]{"NCP", "SwitchItem"}) {
                setup(); NoSlowMod slow = slow(mode); ItemSlowdownEvent input = new ItemSlowdownEvent(player); EventManager.call(input);
                EventManager.call(motion(MotionEvent.Type.PRE)); EventManager.call(motion(MotionEvent.Type.PRE)); send(new C03PacketPlayer(true)); EventManager.call(motion(MotionEvent.Type.POST)); EventManager.call(motion(MotionEvent.Type.POST));
                require(input.forward == 1 && connection.sent.size() == (mode.equals("NCP") ? 3 : 4), "strategy duplicated its PRE/POST work");
                require(connection.sent.get(connection.sent.size() - 1) instanceof C08PacketPlayerBlockPlacement && player.isUsingItem(), "strategy failed to restore the held block");
            }
        });
        check("disabling after PRE restores only the original item-use owner", () -> {
            setup(); NoSlowMod slow = slow("NCP"); EventManager.call(new ItemSlowdownEvent(player)); EventManager.call(motion(MotionEvent.Type.PRE)); slow.setEnable(false);
            require(connection.sent.size() == 2 && connection.sent.get(1) instanceof C08PacketPlayerBlockPlacement, "disable left the server item use released");
        });
        check("Grim sword buffer replays movement before attacks and never captures its replay", () -> {
            setup(); NoSlowMod slow = slow("Grim"); ItemSlowdownEvent input = new ItemSlowdownEvent(player); EventManager.call(input);
            Packet<?> movement = new C03PacketPlayer.C04PacketPlayerPosition(0, 0, 0.3, true), swing = new C0APacketAnimation(); send(movement); send(swing);
            require(input.forward == 1 && connection.sent.isEmpty() && slow.pendingPackets() == 2, "buffer did not capture movement");
            clock.addAndGet(150); slow.update();
            require(connection.sent.size() == 5 && connection.sent.get(0) instanceof C09PacketHeldItemChange && connection.sent.get(1) instanceof C09PacketHeldItemChange
                    && connection.sent.get(2) == movement && connection.sent.get(3) == swing && connection.sent.get(4) instanceof C08PacketPlayerBlockPlacement && slow.pendingPackets() == 0,
                    "buffer reorder, replay loop or item-use restoration failed");
        });
        check("Grim corrections drop old movement and accept the server correction", () -> {
            setup(); NoSlowMod slow = slow("Grim"); EventManager.call(new ItemSlowdownEvent(player)); send(new C03PacketPlayer(true));
            PacketReceiveEvent correction = correction(); Thread receiver = new Thread(() -> EventManager.call(correction)); receiver.start(); receiver.join();
            require(!correction.isCancelled() && slow.pendingPackets() == 0, "correction retained stale positions");
            ItemSlowdownEvent input = new ItemSlowdownEvent(player); EventManager.call(input); require(input.forward == 0.2f, "correction did not pause compensation");
            send(new C03PacketPlayer.C06PacketPlayerPosLook(0, 0, 0, 0, 0, false)); require(connection.sent.size() == 1, "teleport response entered the movement buffer");
        });
        check("Grim queue pressure keeps the current movement before restoring use", () -> {
            setup(); NoSlowMod slow = slow("Grim"); EventManager.call(new ItemSlowdownEvent(player));
            for (int i = 0; i < 65; i++) send(new C03PacketPlayer.C04PacketPlayerPosition(0, 0, i * 0.1, true));
            require(slow.pendingPackets() == 0 && connection.sent.size() == 68, "movement queue overflow lost or duplicated packets");
            require(connection.sent.get(66) instanceof C03PacketPlayer && connection.sent.get(67) instanceof C08PacketPlayerBlockPlacement, "current movement was emitted after item-use restoration");
        });
        check("hotbar and item-use barriers drain previous movement without restarting new food", () -> {
            setup(); NoSlowMod slow = slow("Grim"); EventManager.call(new ItemSlowdownEvent(player)); send(new C03PacketPlayer(true));
            player.clearItemInUse(); ItemStack food = new ItemStack(Items.apple); player.inventory.mainInventory[0] = food; player.setItemInUse(food, 32);
            Packet<?> startFood = new C08PacketPlayerBlockPlacement(food); send(startFood);
            require(slow.pendingPackets() == 0 && connection.sent.size() == 4 && connection.sent.get(3) == startFood,
                    "item-use barrier restarted food or retained old movement");
        });
        check("the real NetworkManager packet hook delays and releases the original packet", () -> {
            setup(); EmbeddedChannel channel = new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter()); NetworkManager network = new NetworkManager(EnumPacketDirection.CLIENTBOUND); set(NetworkManager.class, network, "channel", channel); network.setConnectionState(EnumConnectionState.PLAY); connection.network = network;
            try {
                DisablerMod disabler = disabler("Grim"); Packet<?> packet = new C0FPacketConfirmTransaction(0, (short)-1, true); send(packet);
                require(channel.readOutbound() == null && disabler.pendingPackets() == 1, "NetworkManager ignored packet cancellation");
                clock.addAndGet(150); disabler.update(); channel.runPendingTasks(); require(channel.readOutbound() == packet && channel.readOutbound() == null, "NetworkManager released a duplicate or replacement");
                disabler.setEnable(false);
            } finally { connection.network = null; channel.finish(); }
        });
        cleanup(); if (failures != 0) throw new AssertionError(failures + " packet flow checks failed"); System.out.println("Packet flow checks passed");
    }
    private static DisablerMod disabler(String mode) { DisablerMod mod = new DisablerMod(clock::get); mod.mode.setValue(mode); MeowClient.modManager.getMods().add(mod); mod.setEnable(true); mod.update(); return mod; }
    private static NoSlowMod slow(String mode) { NoSlowMod mod = new NoSlowMod(clock::get); mod.mode.setValue(mode); MeowClient.modManager.getMods().add(mod); mod.setEnable(true); mod.update(); return mod; }
    private static MotionEvent motion(MotionEvent.Type type) { return new MotionEvent(type, 0, 0, 0, 0, 0, true); }
    private static PacketReceiveEvent correction() { return new PacketReceiveEvent(new S08PacketPlayerPosLook(0, 0, 0, 0, 0, EnumSet.noneOf(S08PacketPlayerPosLook.EnumFlags.class))); }
    private static void send(Packet<?> packet) { connection.addToSendQueue(packet); }
    private static void setup() throws Exception {
        cleanup(); mc = allocate(FixtureMinecraft.class); set(Minecraft.class, null, "theMinecraft", mc); mc.gameSettings = new GameSettings(); mc.inGameHasFocus = true; mc.fontRendererObj = allocate(Font.class);
        mc.theWorld = allocate(WorldClient.class); player = allocate(Player.class); player.worldObj = mc.theWorld; player.inventory = new InventoryPlayer(player); player.inventory.mainInventory[0] = new ItemStack(Items.iron_sword); player.capabilities = new PlayerCapabilities(); player.movementInput = new MovementInput(); player.movementInput.moveForward = 1; player.onGround = true;
        DataWatcher watcher = new DataWatcher(player); watcher.addObject(0, (byte)0); watcher.addObject(6, 20f); set(Entity.class, player, "dataWatcher", watcher); mc.thePlayer = player;
        connection = newConnection(); set(EntityPlayerSP.class, player, "sendQueue", connection); player.setItemInUse(player.getHeldItem(), 72000);
        clock = new AtomicLong(1000); MeowClient.modManager = new ModManager(); BlatantMod blatant = new BlatantMod(); MeowClient.modManager.getMods().add(blatant); blatant.setEnable(true);
    }
    private static Connection newConnection() throws Exception { Connection connection = allocate(Connection.class); connection.sent = Collections.synchronizedList(new ArrayList<Packet<?>>()); return connection; }
    private static void cleanup() { if (MeowClient.modManager != null) for (Mod mod : new ArrayList<>(MeowClient.modManager.getMods())) if (mod.isEnable()) mod.setEnable(false); }
    private static class FixtureMinecraft extends Minecraft { FixtureMinecraft() { super(null); } @Override public boolean isUnicode() { return false; } @Override public boolean isSingleplayer() { return false; } }
    private static class Player extends EntityPlayerSP { Player() { super(null, null, null, null); } @Override public boolean isSpectator() { return false; } }
    private static class Connection extends NetHandlerPlayClient {
        List<Packet<?>> sent; NetworkManager network; Connection() { super(null, null, null, null); }
        @Override public void addToSendQueue(Packet packet) { if (network != null) { network.sendPacket(packet); return; } PacketSendEvent event = new PacketSendEvent(packet); EventManager.call(event); if (!event.isCancelled()) sent.add(event.getPacket()); }
    }
    private static class Font extends FontRenderer { Font() { super(null, null, null, false); } @Override public int getStringWidth(String text) { return text.length() * 6; } }
    private static void check(String name, Checked test) { try { test.run(); System.out.println("PASS " + name); } catch (Throwable failure) { failures++; System.err.println("FAIL " + name + ": " + failure); if (!(failure instanceof AssertionError)) failure.printStackTrace(); } }
    private interface Checked { void run() throws Exception; }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static <T> T allocate(Class<T> type) throws Exception { return type.cast(UNSAFE.allocateInstance(type)); }
    private static void set(Class<?> type, Object instance, String name, Object value) throws Exception { Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value); }
    private static Unsafe unsafe() { try { Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true); return (Unsafe)field.get(null); } catch (Exception failure) { throw new RuntimeException(failure); } }
}
