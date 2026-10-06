import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.events.EventManager;
import cn.sux1ng.client.events.impl.MotionEvent;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.mod.mods.misc.BlatantMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import sun.misc.Unsafe;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Runs the real walking-player PRE -> movement -> POST pipeline against a packet recorder. */
public class R7CombatRegressionTest {
    private static final Unsafe UNSAFE = unsafe();
    private static FixtureMinecraft mc;
    private static FixturePlayer player;
    private static FixtureWorld world;
    private static Connection connection;
    private static AtomicLong clock;
    private static KillAuraMod aura;
    private static int failures;

    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        check("Silent retains its previous motion rotation", () -> {
            setup(); aura.autoBlock.setValue(false); aura.rotMode.setValue("Silent"); aura.maxTurnSpeed.setValue(10.0);
            Target target = target(1, 2.5, 2.5); world.loadedEntityList.add(target);
            tick(); float first = lastLookYaw(); tick(); float second = lastLookYaw();
            require(Math.abs(second) > Math.abs(first) + 0.1, "Silent restarted from the camera each tick: " + first + " -> " + second);
            require(player.rotationYaw == 0 && player.rotationPitch == 0, "Silent changed the camera");
        });
        check("Single keeps a valid target when distances cross", () -> {
            setup(); aura.autoBlock.setValue(false); aura.targetMode.setValue("Single");
            Target first = target(1, 0, 2.5), second = target(2, 0.5, 3);
            world.loadedEntityList.add(first); world.loadedEntityList.add(second);
            tick(); require(aura.getTarget() == first, "initial target was not selected");
            second.posZ = 2; second.setEntityBoundingBox(new AxisAlignedBB(0.2, 0, 1.7, 0.8, 1.8, 2.3));
            tick(); require(aura.getTarget() == first, "Single flickered to the newly nearest entity");
        });
        check("no instant attack before acquiring and aiming", () -> {
            setup(); aura.autoBlock.setValue(false); world.loadedEntityList.add(target(1, 0, 3));
            tick(); require(attacks() == 0, "first target acquisition attacked immediately");
        });
        check("None requires the manually aimed target", () -> {
            setup(); aura.autoBlock.setValue(false); aura.rotMode.setValue("None");
            world.loadedEntityList.add(target(1, 2.5, 0));
            for (int i = 0; i < 20; i++) tick();
            require(attacks() == 0, "None attacked a target beside the crosshair");
        });
        check("POST cannot attack without a PRE plan", () -> {
            setup(); aura.autoBlock.setValue(false); world.loadedEntityList.add(target(1, 0, 3));
            tick(); connection.packets.clear(); clock.addAndGet(1000);
            EventManager.call(new MotionEvent(MotionEvent.Type.POST, 0, 0, 0, 0, 0, true));
            require(attacks() == 0, "an unpaired POST attacked a stale target");
        });
        check("no attacks through an intervening block", () -> {
            setup(); aura.autoBlock.setValue(false); world.loadedEntityList.add(target(1, 0, 3)); world.wall = true;
            for (int i = 0; i < 25; i++) tick();
            require(attacks() == 0, "a wall did not prevent attacks");
        });
        check("manual sword blocking is not released by Aura", () -> {
            setup(); ItemStack sword = new ItemStack(Items.iron_sword); player.inventory.mainInventory[0] = sword;
            player.setItemInUse(sword, 72000); connection.packets.clear(); aura.setEnable(false);
            require(count(C07PacketPlayerDigging.class) == 0 && player.getItemInUse() == sword,
                    "disabling Aura released a block it did not start");
        });
        for (String mode : new String[]{"Lock", "Smooth", "Silent", "None"}) {
            check(mode + " attacks only after movement and emits one attack per tick", () -> {
                setup(); aura.rotMode.setValue(mode); aura.autoBlock.setValue(false);
                world.loadedEntityList.add(target(1, 0, 3));
                int events[] = {0}; Object listener = new Object() {
                    @cn.sux1ng.client.events.EventTarget public void attack(cn.sux1ng.client.events.impl.AttackEvent event) { events[0]++; }
                };
                EventManager.register(listener);
                try {
                    for (int tick = 0; tick < 80; tick++) {
                        int before = attacks(); tick();
                        require(attacks() - before <= 1, "more than one attack in a tick");
                        if (attacks() > before) {
                            int index = lastIndex(C02PacketUseEntity.class);
                            require(index > 0 && connection.packets.get(index - 1) instanceof C0APacketAnimation,
                                    "attack did not follow a swing");
                            boolean movement = false;
                            for (int i = index - 2; i >= 0; i--) {
                                if (connection.packets.get(i) instanceof C02PacketUseEntity) break;
                                if (connection.packets.get(i) instanceof C03PacketPlayer) { movement = true; break; }
                            }
                            require(movement, "attack did not follow movement submission");
                        }
                        int after = attacks();
                        EventManager.call(new MotionEvent(MotionEvent.Type.POST, 0, 0, 0, lastLookYaw(), player.rotationPitch, true));
                        require(attacks() == after, "duplicate POST attacked again");
                    }
                    require(attacks() >= 25, "mode never finished acquiring/aiming: " + attacks());
                    require(events[0] == attacks(), "attack effects were emitted twice or skipped");
                } finally { EventManager.unregister(listener); }
            });
        }
        check("Switch holds a target before cycling by identity", () -> {
            setup(); aura.rotMode.setValue("Lock"); aura.autoBlock.setValue(false);
            Target first = target(1, 0, 2.5), second = target(2, 0.5, 3); world.loadedEntityList.add(first); world.loadedEntityList.add(second);
            tick(); require(aura.getTarget() == first, "first target missing");
            for (int i = 0; i < 8; i++) { tick(); require(aura.getTarget() == first, "switched on every hit instead of holding the target"); }
            for (int i = 0; i < 6; i++) tick();
            require(aura.getTarget() == second, "Switch did not cycle after a successful attack and dwell time");
        });
        check("block release, swing, attack and later block use standard controller order", () -> {
            setup(); aura.rotMode.setValue("Lock"); world.loadedEntityList.add(target(1, 0, 3));
            player.inventory.mainInventory[0] = new ItemStack(Items.iron_sword);
            for (int i = 0; i < 30; i++) tick();
            require(attacks() > 0 && count(C08PacketPlayerBlockPlacement.class) > 0 && count(C07PacketPlayerDigging.class) > 0, "block cycle did not run");
            int index = lastIndex(C02PacketUseEntity.class);
            require(index >= 2 && connection.packets.get(index - 2) instanceof C07PacketPlayerDigging
                    && connection.packets.get(index - 1) instanceof C0APacketAnimation, "owned block was not released before swinging");
            require(index + 1 == connection.packets.size() || connection.packets.get(index + 1) instanceof C03PacketPlayer,
                    "block was re-entered in the attack tick");
        });
        check("a changed final movement look prevents an off-axis attack", () -> {
            setup(); aura.rotMode.setValue("Lock"); aura.autoBlock.setValue(false); world.loadedEntityList.add(target(1, 0, 3));
            for (int i = 0; i < 8; i++) tick(); connection.packets.clear(); clock.addAndGet(200);
            player.ticksExisted++; MotionEvent pre = new MotionEvent(MotionEvent.Type.PRE, 0, 0, 0, 0, 0, true);
            EventManager.call(pre);
            EventManager.call(new MotionEvent(MotionEvent.Type.POST, 0, 0, 0, 90, 0, true));
            require(attacks() == 0, "attack trusted the planned rather than the submitted look");
        });
        check("manual attacks share the cadence instead of double firing", () -> {
            setup(); aura.rotMode.setValue("None"); aura.autoBlock.setValue(false); world.loadedEntityList.add(target(1, 0, 3));
            for (int i = 0; i < 4; i++) tick(); clock.addAndGet(1000); player.ticksExisted++;
            EventManager.call(new MotionEvent(MotionEvent.Type.PRE, 0, 0, 0, 0, 0, true));
            mc.playerController.attackEntity(player, aura.getTarget()); int before = attacks();
            EventManager.call(new MotionEvent(MotionEvent.Type.POST, 0, 0, 0, 0, 0, true));
            require(attacks() == before, "automatic combat doubled a manual attack");
        });
        check("menus and world changes invalidate prepared attacks", () -> {
            setup(); aura.autoBlock.setValue(false); world.loadedEntityList.add(target(1, 0, 3));
            for (int i = 0; i < 6; i++) tick();
            mc.currentScreen = new GuiScreen() {}; aura.render(1);
            require(aura.getTarget() == null, "menu left the old target active");
            mc.currentScreen = null; tick(); connection.packets.clear(); mc.theWorld = allocate(FixtureWorld.class);
            EventManager.call(new MotionEvent(MotionEvent.Type.POST, 0, 0, 0, 0, 0, true));
            require(attacks() == 0 && aura.getTarget() == null, "world change attacked a stale entity");
        });
        check("duplicate PRE preserves Silent look without accelerating twice", () -> {
            setup(); aura.rotMode.setValue("Silent"); aura.autoBlock.setValue(false); world.loadedEntityList.add(target(1, 2, 2));
            tick(); float yaw = lastLookYaw();
            MotionEvent duplicate = new MotionEvent(MotionEvent.Type.PRE, 0, 0, 0, 0, 0, true);
            EventManager.call(duplicate); require(duplicate.yaw == yaw, "duplicate PRE reset or advanced the rotation");
        });
        check("AutoClicker defers while Aura owns a target", () -> {
            setup(); aura.autoBlock.setValue(false); world.loadedEntityList.add(target(1, 0, 3)); tick();
            cn.sux1ng.client.mod.mods.combat.AutoClickerMod clicker = new cn.sux1ng.client.mod.mods.combat.AutoClickerMod();
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
            try { clicker.update(); require(mc.clicks == 0, "AutoClicker issued an extra click"); }
            finally { net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false); }
        });
        check("aim takes the shortest wrapped path and respects mouse increments", () -> {
            for (String mode : new String[]{"Lock", "Smooth", "Silent"}) {
                cn.sux1ng.client.combat.AimMotion aim = new cn.sux1ng.client.combat.AimMotion();
                float[] result = aim.turn(mode, 179, 0, -179, 0, 45, 15, 0.5f);
                require(result[0] > 179 && result[0] < 182, "rotation crossed the long side of 180 degrees");
                float counts = (result[0] - 179) / 0.15f;
                require(Math.abs(counts - Math.round(counts)) < 0.001, "rotation ignored mouse sensitivity increments");
            }
        });
        check("all turning modes remain bounded while targets change", () -> {
            for (String mode : new String[]{"Lock", "Smooth", "Silent"}) {
                cn.sux1ng.client.combat.AimMotion aim = new cn.sux1ng.client.combat.AimMotion();
                float yaw = 0, pitch = 0;
                for (int tick = 0; tick < 100; tick++) {
                    float destinationYaw = (float)Math.sin(tick * 0.3) * 175, destinationPitch = (float)Math.cos(tick * 0.4) * 89;
                    float[] next = aim.turn(mode, mode.equals("Silent") ? 0 : yaw, mode.equals("Silent") ? 0 : pitch,
                            destinationYaw, destinationPitch, 20, 15, 0.5f);
                    require(Math.hypot(next[0] - yaw, next[1] - pitch) <= 20.25, "rotation exceeded its configured speed");
                    require(next[1] >= -90 && next[1] <= 90, "pitch exceeded the player's range");
                    yaw = next[0]; pitch = next[1];
                }
            }
        });
        check("click interval is held instead of redrawn while waiting", () -> {
            CountingRandom random = new CountingRandom(); cn.sux1ng.client.combat.AttackRhythm rhythm = new cn.sux1ng.client.combat.AttackRhythm(random);
            rhythm.configure(10, 13); rhythm.acquire(1000, 150); rhythm.aligned(true, 1000); int samples = random.calls;
            for (int tick = 0; tick < 3; tick++) { rhythm.aligned(true, 1000 + tick * 50); rhythm.ready(1000 + tick * 50, tick); }
            require(random.calls == samples, "waiting resampled the click interval");
        });
        check("cadence averages the configured rate and avoids catch-up bursts", () -> {
            cn.sux1ng.client.combat.AttackRhythm rhythm = new cn.sux1ng.client.combat.AttackRhythm(new Random(19));
            rhythm.configure(13, 10); rhythm.acquire(0, 0); rhythm.aligned(true, 0);
            int count = 0; Set<Long> gaps = new HashSet<>(); long previous = 0;
            for (int tick = 1; tick <= 200; tick++) {
                long now = tick * 50; rhythm.aligned(true, now);
                if (rhythm.ready(now, tick)) {
                    if (count > 0) gaps.add(now - previous); previous = now; count++; rhythm.attacked(now, tick);
                    require(!rhythm.ready(now, tick), "one deadline was consumed twice");
                }
            }
            require(count >= 95 && count <= 135 && gaps.size() > 1, "cadence remained mechanically fixed or out of range: " + count + "/" + gaps);
            rhythm.attacked(30000, 201); require(!rhythm.ready(30000, 202), "lag left an immediately overdue attack");
        });
        check("AutoBlock off releases only the owned use action", () -> {
            setup(); aura.rotMode.setValue("Lock"); world.loadedEntityList.add(target(1, 0, 3)); player.inventory.mainInventory[0] = new ItemStack(Items.iron_sword);
            for (int i = 0; i < 20 && !player.isUsingItem(); i++) tick();
            require(player.isUsingItem(), "automatic block did not enter use state");
            aura.autoBlock.setValue(false); connection.packets.clear(); tick();
            require(!player.isUsingItem() && count(C07PacketPlayerDigging.class) == 1, "owned block was not cleanly released");
        });
        if (aura != null) aura.setEnable(false);
        if (failures != 0) throw new AssertionError(failures + " R7 combat checks failed");
        System.out.println("R7 combat checks passed");
    }

    private static void setup() throws Exception {
        if (aura != null) aura.setEnable(false);
        mc = allocate(FixtureMinecraft.class); set(Minecraft.class, null, "theMinecraft", mc);
        mc.gameSettings = new GameSettings(); mc.displayWidth = 854; mc.displayHeight = 480; mc.inGameHasFocus = true;
        mc.fontRendererObj = allocate(FixtureFont.class);
        world = allocate(FixtureWorld.class); set(WorldClient.class.getSuperclass(), world, "loadedEntityList", new ArrayList<Entity>());
        set(WorldClient.class.getSuperclass(), world, "isRemote", true); mc.theWorld = world;
        player = allocate(FixturePlayer.class); player.height = 1.8f; player.width = 0.6f; player.worldObj = world;
        player.inventory = new InventoryPlayer(player); player.setEntityBoundingBox(new AxisAlignedBB(-0.3, 0, -0.3, 0.3, 1.8, 0.3));
        watcher(player); mc.thePlayer = player; set(Minecraft.class, mc, "renderViewEntity", player);
        connection = allocate(Connection.class); connection.packets = new ArrayList<>(); mc.connection = connection;
        set(EntityPlayerSP.class, player, "mc", mc); set(EntityPlayerSP.class, player, "sendQueue", connection);
        mc.playerController = new PlayerControllerMP(mc, connection);
        clock = new AtomicLong(1000);
        try { aura = KillAuraMod.class.getConstructor(java.util.function.LongSupplier.class, Random.class).newInstance((java.util.function.LongSupplier)clock::get, new Random(42)); }
        catch (NoSuchMethodException original) { aura = new KillAuraMod(); }
        ModManager manager = new ModManager(); MeowClient.modManager = manager;
        BlatantMod blatant = new BlatantMod(); manager.getMods().add(blatant); manager.getMods().add(aura);
        blatant.setEnable(true); aura.setEnable(true); connection.packets.clear();
        aura.fov.setValue(360.0);
    }
    private static Target target(int id, double x, double z) throws Exception {
        Target target = allocate(Target.class); target.worldObj = world; target.height = 1.8f; target.width = 0.6f;
        target.posX = target.lastTickPosX = x; target.posZ = target.lastTickPosZ = z;
        target.setEntityBoundingBox(new AxisAlignedBB(x - 0.3, 0, z - 0.3, x + 0.3, 1.8, z + 0.3));
        target.inventory = new InventoryPlayer(target); set(Entity.class, target, "entityId", id); watcher(target); return target;
    }
    private static void watcher(Entity entity) throws Exception {
        DataWatcher watcher = new DataWatcher(entity); watcher.addObject(0, (byte)0); watcher.addObject(6, 20f);
        set(Entity.class, entity, "dataWatcher", watcher);
    }
    private static void tick() { clock.addAndGet(50); player.ticksExisted++; player.onUpdateWalkingPlayer(); }
    private static int attacks() { return count(C02PacketUseEntity.class); }
    private static int lastIndex(Class<?> type) { for (int i = connection.packets.size() - 1; i >= 0; i--) if (type.isInstance(connection.packets.get(i))) return i; return -1; }
    private static int count(Class<?> type) { int count = 0; for (Packet packet : connection.packets) if (type.isInstance(packet)) count++; return count; }
    private static float lastLookYaw() {
        float yaw = 0; for (Packet packet : connection.packets) if (packet instanceof C03PacketPlayer && ((C03PacketPlayer)packet).getRotating()) yaw = ((C03PacketPlayer)packet).getYaw();
        return yaw;
    }
    private interface Checked { void run() throws Exception; }
    private static void check(String name, Checked test) { try { test.run(); System.out.println("PASS " + name); } catch (Throwable failure) { failures++; System.err.println("FAIL " + name + ": " + failure); if (!(failure instanceof AssertionError)) failure.printStackTrace(); } }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static <T> T allocate(Class<T> type) throws Exception { return type.cast(UNSAFE.allocateInstance(type)); }
    private static void set(Class<?> type, Object instance, String name, Object value) throws Exception { Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value); }
    private static Unsafe unsafe() { try { Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true); return (Unsafe)field.get(null); } catch (Exception failure) { throw new RuntimeException(failure); } }
    private static class FixtureMinecraft extends Minecraft {
        Connection connection; int clicks; FixtureMinecraft() { super(null); }
        @Override public NetHandlerPlayClient getNetHandler() { return connection; }
        @Override public boolean isUnicode() { return false; }
        @Override public void clickMouse() { clicks++; }
    }
    private static class FixturePlayer extends EntityPlayerSP {
        FixturePlayer() { super(null, null, null, null); }
        @Override public boolean isSprinting() { return false; }
        @Override public boolean isSneaking() { return false; }
        @Override public boolean isPotionActive(Potion potion) { return false; }
        @Override public void attackTargetEntityWithCurrentItem(Entity target) {}
    }
    private static class Target extends EntityOtherPlayerMP {
        Target() { super(null, null); }
        @Override public boolean isSpectator() { return false; }
    }
    private static class Connection extends NetHandlerPlayClient {
        List<Packet> packets; Connection() { super(null, null, null, null); }
        @Override public void addToSendQueue(Packet packet) { packets.add(packet); }
    }
    private static class FixtureFont extends FontRenderer {
        FixtureFont() { super(null, null, null, false); }
        @Override public int getStringWidth(String text) { return text.length() * 6; }
    }
    private static class FixtureWorld extends WorldClient {
        boolean wall; FixtureWorld() { super(null, null, 0, null, null); }
        @Override public MovingObjectPosition rayTraceBlocks(Vec3 from, Vec3 to) { return rayTraceBlocks(from, to, false, true, false); }
        @Override public MovingObjectPosition rayTraceBlocks(Vec3 from, Vec3 to, boolean liquid, boolean noBox, boolean last) {
            return wall ? new MovingObjectPosition(from.addVector((to.xCoord - from.xCoord) * 0.2, 0, (to.zCoord - from.zCoord) * 0.2), EnumFacing.NORTH, new BlockPos(0, 1, 1)) : null;
        }
    }
    private static class CountingRandom extends Random {
        int calls;
        @Override public double nextDouble() { calls++; return 0.5; }
    }
}
