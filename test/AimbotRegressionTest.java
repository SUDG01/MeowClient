import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.combat.FrameAimMotion;
import cn.sux1ng.client.events.EventManager;
import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.CameraEvent;
import cn.sux1ng.client.events.impl.MotionEvent;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.mod.mods.combat.AimbotMod;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.mod.mods.misc.BlatantMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.*;
import org.lwjgl.opengl.Display;
import sun.misc.Unsafe;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Fixed-time camera replays plus the actual Aimbot event handler with a packet recorder. */
public class AimbotRegressionTest {
    private static final Unsafe UNSAFE = unsafe();
    private static FixtureMinecraft mc;
    private static Player player;
    private static World world;
    private static AimbotMod aim;
    private static KillAuraMod aura;
    private static AtomicLong clock;
    private static int packets, failures;

    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        check("spring convergence is consistent across frame rates", () -> {
            double reference = 0;
            for (int fps : new int[]{30, 60, 144, 360, 1000}) {
                FrameAimMotion motion = new FrameAimMotion(); double yaw = 0;
                for (int frame = 0; frame < fps; frame++) yaw += motion.step((float)(30 - yaw), 0, 1.0 / fps, 220, 1000, 1000, 1, 0)[0];
                if (reference == 0) reference = yaw;
                require(Math.abs(yaw - reference) < 0.03 && yaw > 29.8, "FPS changed convergence: " + fps + "/" + yaw);
            }
        });
        check("limited corrections are small and never overshoot", () -> {
            for (int fps : new int[]{30, 144, 1000}) {
                FrameAimMotion motion = new FrameAimMotion(); float yaw = 0, pitch = 0;
                for (int frame = 0; frame < fps * 2; frame++) {
                    float[] delta = motion.step(-20 - yaw, 10 - pitch, 1.0 / fps, 220, 80, 40, 0.65, 0.12);
                    require(Math.abs(delta[0]) <= 52.0 / fps + 0.0001 && Math.abs(delta[1]) <= 26.0 / fps + 0.0001, "frame correction exceeded speed");
                    yaw += delta[0]; pitch += delta[1];
                    require(yaw >= -20.001 && pitch <= 10.001, "spring overshot the desired point");
                }
            }
        });
        for (String mode : new String[]{"Assist", "Track"}) {
            check(mode + " follows smoothly without generating attack packets", () -> {
                setup(); aim.mode.setValue(mode); world.loadedEntityList.add(target(1, 0.9, 3));
                for (int frame = 0; frame < 90; frame++) frame(0, 0);
                require(player.rotationYaw < -2 && player.rotationYaw > -22.5, "mode did not make bounded corrections: " + player.rotationYaw);
                require(packets == 0, "camera assistance sent gameplay packets");
            });
        }
        check("moving targets remain consistent across frame rates and tick interpolation", () -> {
            float referenceYaw = 0, referencePitch = 0;
            for (int fps : new int[]{30, 60, 144, 360, 1000}) {
                // Continuous tracking is measured separately from the deliberate hitbox stop.
                setup(); aim.aimPoint.setValue("Body"); aim.stopOnTarget.setValue(false);
                Target moving = target(1, 0.9, 3); world.loadedEntityList.add(moving);
                for (int frame = 0; frame <= fps * 2; frame++) {
                    double seconds = (double)frame / fps, tick = Math.floor(seconds * 20);
                    moving.lastTickPosX = 0.9 + 0.35 * Math.sin(tick * 0.15);
                    moving.posX = 0.9 + 0.35 * Math.sin((tick + 1) * 0.15);
                    moving.setEntityBoundingBox(new AxisAlignedBB(moving.posX - 0.3, 0, 2.7, moving.posX + 0.3, 1.8, 3.3));
                    clock.set(1_000_000_000L + Math.round(seconds * 1_000_000_000L));
                    EventManager.call(new CameraEvent((float)(seconds * 20 - tick), 0, 0, true));
                }
                if (fps == 30) { referenceYaw = player.rotationYaw; referencePitch = player.rotationPitch; }
                require(Math.abs(player.rotationYaw - referenceYaw) < 0.3 && Math.abs(player.rotationPitch - referencePitch) < 0.15,
                        "moving target differed at " + fps + " FPS: " + player.rotationYaw + "/" + player.rotationPitch + " vs " + referenceYaw + "/" + referencePitch);
            }
        });
        check("click and weapon conditions gate camera changes", () -> {
            setup(); world.loadedEntityList.add(target(1, 0.9, 3));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            for (int i = 0; i < 30; i++) frame(0, 0);
            require(player.rotationYaw == 0, "assistance ran without held attack");
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true); player.inventory.mainInventory[0] = null;
            for (int i = 0; i < 30; i++) frame(0, 0);
            require(player.rotationYaw == 0, "assistance ran without a weapon");
        });
        check("manual movement away from a target takes precedence", () -> {
            setup(); world.loadedEntityList.add(target(1, 0.9, 3));
            for (int i = 0; i < 20; i++) frame(0, 0);
            player.setAngles(0.5f / 0.15f, 0); float afterMouse = player.rotationYaw;
            frame(0.5f, 0); require(player.rotationYaw == afterMouse, "assistance fought opposite mouse input");
            for (int i = 0; i < 5; i++) frame(0, 0);
            require(player.rotationYaw == afterMouse, "mouse priority did not retain its brief yield interval");
        });
        check("a fast manual flick toward the target also yields control", () -> {
            setup(); world.loadedEntityList.add(target(1, 0.9, 3));
            for (int i = 0; i < 20; i++) frame(0, 0);
            player.setAngles(-4f / 0.15f, 0); float afterMouse = player.rotationYaw;
            frame(-4, 0); require(player.rotationYaw == afterMouse, "assistance added correction to a fast manual flick");
        });
        check("crosshair inside the target does not pull toward a perfect centre", () -> {
            setup(); world.loadedEntityList.add(target(1, 0, 3));
            for (int i = 0; i < 60; i++) frame(0, 0);
            require(player.rotationYaw == 0 && player.rotationPitch == 0, "already valid aim was changed");
        });
        check("walls, out-of-cone targets and teammates are excluded", () -> {
            setup(); Target near = target(1, 0.9, 3); world.loadedEntityList.add(near); world.wall = true;
            for (int i = 0; i < 30; i++) frame(0, 0); require(aim.getTarget() == null && player.rotationYaw == 0, "target through wall was selected");
            world.wall = false; near.team = true;
            for (int i = 0; i < 30; i++) frame(0, 0); require(aim.getTarget() == null, "teammate was selected");
            world.loadedEntityList.clear(); world.loadedEntityList.add(target(2, 3, 0));
            for (int i = 0; i < 30; i++) frame(0, 0); require(player.rotationYaw == 0, "target outside FOV pulled the camera");
        });
        check("stable targets do not switch when nearby priorities cross", () -> {
            setup(); Target one = target(1, 0.9, 3), two = target(2, -1.1, 3); world.loadedEntityList.add(one); world.loadedEntityList.add(two);
            for (int i = 0; i < 12; i++) frame(0, 0); require(aim.getTarget() == one, "initial target was not chosen");
            two.posX = -0.7; two.lastTickPosX = -0.7; two.setEntityBoundingBox(new AxisAlignedBB(-1, 0, 2.7, -0.4, 1.8, 3.3));
            frame(0, 0); require(aim.getTarget() == one, "stable target switched due to a small priority change");
        });
        check("menu, lost focus and lag reset assistance", () -> {
            setup(); world.loadedEntityList.add(target(1, 0.9, 3)); for (int i = 0; i < 12; i++) frame(0, 0);
            mc.currentScreen = new GuiScreen() {}; frame(0, 0); require(aim.getTarget() == null, "menu retained aim state");
            mc.currentScreen = null; frame(0, 0); EventManager.call(new CameraEvent(1, 0, 0, false)); require(aim.getTarget() == null, "focus loss retained aim state");
            for (int i = 0; i < 12; i++) frame(0, 0); float before = player.rotationYaw;
            clock.addAndGet(1_000_000_000L); frame(0, 0); require(player.rotationYaw == before, "lag caused a catch-up jump");
        });
        check("mining and manual item use suppress assistance", () -> {
            setup(); world.loadedEntityList.add(target(1, 0.9, 3));
            mc.objectMouseOver = new MovingObjectPosition(new Vec3(0, 1, 1), EnumFacing.NORTH, new BlockPos(0, 1, 1));
            for (int i = 0; i < 30; i++) frame(0, 0); require(player.rotationYaw == 0, "assistance fought block mining");
            mc.objectMouseOver = null; player.setItemInUse(player.getHeldItem(), 72000);
            for (int i = 0; i < 30; i++) frame(0, 0); require(player.rotationYaw == 0, "assistance fought manual item use");
        });
        check("KillAura rotation has ownership while None permits camera assistance", () -> {
            setup(); Target target = target(1, 0.9, 3); world.loadedEntityList.add(target);
            aura = new KillAuraMod(); MeowClient.modManager.getMods().add(aura); aura.rotMode.setValue("Silent"); aura.setEnable(true);
            EventManager.call(new MotionEvent(MotionEvent.Type.PRE, 0, 0, 0, 0, 0, true));
            require(aura.getTarget() == target, "KillAura did not acquire the shared target");
            for (int i = 0; i < 30; i++) frame(0, 0);
            require(aim.getTarget() == null && player.rotationYaw == 0, "camera assist fought KillAura rotation");
            aura.rotMode.setValue("None");
            for (int i = 0; i < 30; i++) frame(0, 0);
            require(aim.getTarget() == target && player.rotationYaw < -2, "None rotation blocked camera assistance");
            aura.setEnable(false);
        });
        if (Boolean.getBoolean("meow.test.nativeCamera")) check("real render entry emits camera events after vanilla mouse input", AimbotRegressionTest::cameraHook);
        if (aura != null) aura.setEnable(false);
        if (aim != null) aim.setEnable(false);
        if (failures != 0) throw new AssertionError(failures + " Aimbot checks failed");
        System.out.println("Aimbot checks passed");
    }
    private static void setup() throws Exception {
        if (aura != null) { aura.setEnable(false); aura = null; }
        if (aim != null) aim.setEnable(false);
        mc = allocate(FixtureMinecraft.class); set(Minecraft.class, null, "theMinecraft", mc);
        mc.gameSettings = new GameSettings(); mc.inGameHasFocus = true; mc.displayWidth = 854; mc.displayHeight = 480;
        mc.fontRendererObj = allocate(Font.class);
        world = allocate(World.class); set(WorldClient.class.getSuperclass(), world, "loadedEntityList", new ArrayList<Entity>());
        set(WorldClient.class.getSuperclass(), world, "isRemote", true); mc.theWorld = world;
        player = allocate(Player.class); player.worldObj = world; player.height = 1.8f; player.width = 0.6f;
        player.inventory = new InventoryPlayer(player); player.inventory.mainInventory[0] = new ItemStack(Items.iron_sword);
        player.setEntityBoundingBox(new AxisAlignedBB(-0.3, 0, -0.3, 0.3, 1.8, 0.3)); watcher(player); mc.thePlayer = player;
        mc.playerController = new PlayerControllerMP(mc, allocate(Connection.class));
        clock = new AtomicLong(1_000_000_000L); aim = new AimbotMod(clock::get);
        MeowClient.modManager = new ModManager(); BlatantMod blatant = new BlatantMod();
        MeowClient.modManager.getMods().add(blatant); MeowClient.modManager.getMods().add(aim); blatant.setEnable(true); aim.setEnable(true);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true); packets = 0;
    }
    /** Substitute only window focus and hardware deltas; execute the actual EntityRenderer entry point. */
    private static void cameraHook() throws Exception {
        setup(); aim.setEnable(false); mc.skipRenderWorld = true; mc.gameSettings.mouseSensitivity = 0.5f;
        mc.gameSettings.pauseOnLostFocus = false; set(Minecraft.class, mc, "mcProfiler", new Profiler());
        EntityRenderer renderer = allocate(EntityRenderer.class); set(EntityRenderer.class, renderer, "mc", mc);
        ReplayMouse mouse = new ReplayMouse(); mc.mouseHelper = mouse;
        CameraRecorder recorder = new CameraRecorder(); EventManager.register(recorder);
        Field implementation = Display.class.getDeclaredField("display_impl"); implementation.setAccessible(true);
        Field created = Display.class.getDeclaredField("window_created"); created.setAccessible(true);
        Object previousImplementation = implementation.get(null); boolean previousCreated = created.getBoolean(null);
        Object focus = Proxy.newProxyInstance(Display.class.getClassLoader(), new Class[]{implementation.getType()}, (proxy, method, args) -> {
            if (method.getName().equals("isActive")) return mc.inGameHasFocus;
            throw new UnsupportedOperationException(method.getName());
        });
        try {
            UNSAFE.putObject(UNSAFE.staticFieldBase(implementation), UNSAFE.staticFieldOffset(implementation), focus); created.setBoolean(null, true);
            mouse.x = 4; mouse.y = -3; renderFrame(renderer);
            require(recorder.calls == 1 && Math.abs(recorder.event.mouseYaw - 0.6f) < 0.0001 && Math.abs(recorder.event.mousePitch - 0.45f) < 0.0001,
                    "hook did not preserve sensitivity-scaled mouse angles");
            require(Math.abs(recorder.yaw - 0.6f) < 0.0001 && Math.abs(recorder.pitch - 0.45f) < 0.0001,
                    "camera event ran before vanilla mouse input");
            mc.gameSettings.invertMouse = true; renderFrame(renderer);
            require(recorder.event.mousePitch < 0 && Math.abs(player.rotationPitch) < 0.0001, "hook ignored inverted mouse input");
            mc.gameSettings.invertMouse = false; player.rotationYaw = player.rotationPitch = player.prevRotationYaw = player.prevRotationPitch = 0;
            mouse.x = mouse.y = 0; world.loadedEntityList.add(target(1, 0.9, 3)); aim.setEnable(true);
            for (int i = 0; i < 60; i++) renderFrame(renderer);
            require(player.rotationYaw < -2 && packets == 0, "render hook did not drive bounded camera assistance");
            float beforeMouse = player.rotationYaw; mouse.x = 4; renderFrame(renderer);
            require(Math.abs(player.rotationYaw - beforeMouse - 0.6f) < 0.0001, "real mouse input did not take precedence");
            mc.inGameHasFocus = false; renderFrame(renderer);
            require(!recorder.event.focused && recorder.event.mouseYaw == 0 && recorder.event.mousePitch == 0 && aim.getTarget() == null,
                    "unfocused render retained camera correction");
            require(recorder.calls == 64, "camera hook emitted more than one event per frame");
        } finally {
            created.setBoolean(null, previousCreated);
            UNSAFE.putObject(UNSAFE.staticFieldBase(implementation), UNSAFE.staticFieldOffset(implementation), previousImplementation);
            EventManager.unregister(recorder);
        }
    }
    private static void renderFrame(EntityRenderer renderer) { clock.addAndGet(16_666_667L); renderer.func_181560_a(1, 0); }
    private static Target target(int id, double x, double z) throws Exception {
        Target target = allocate(Target.class); target.worldObj = world; target.height = 1.8f; target.width = 0.6f;
        target.posX = target.lastTickPosX = x; target.posZ = target.lastTickPosZ = z;
        target.setEntityBoundingBox(new AxisAlignedBB(x - 0.3, 0, z - 0.3, x + 0.3, 1.8, z + 0.3)); watcher(target);
        set(Entity.class, target, "entityId", id); return target;
    }
    private static void frame(float yaw, float pitch) { clock.addAndGet(16_666_667L); EventManager.call(new CameraEvent(1, yaw, pitch, true)); }
    private static void watcher(Entity entity) throws Exception { DataWatcher watcher = new DataWatcher(entity); watcher.addObject(0, (byte)0); watcher.addObject(6, 20f); set(Entity.class, entity, "dataWatcher", watcher); }
    private static void check(String name, Checked test) { try { test.run(); System.out.println("PASS " + name); } catch (Throwable failure) { failures++; System.err.println("FAIL " + name + ": " + failure); if (!(failure instanceof AssertionError)) failure.printStackTrace(); } }
    private interface Checked { void run() throws Exception; }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static <T> T allocate(Class<T> type) throws Exception { return type.cast(UNSAFE.allocateInstance(type)); }
    private static void set(Class<?> type, Object instance, String name, Object value) throws Exception { Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value); }
    private static Unsafe unsafe() { try { Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true); return (Unsafe)field.get(null); } catch (Exception failure) { throw new RuntimeException(failure); } }
    private static class FixtureMinecraft extends Minecraft { FixtureMinecraft() { super(null); } @Override public boolean isUnicode() { return false; } }
    private static class Player extends EntityPlayerSP {
        Player() { super(null, null, null, null); }
        @Override public boolean isOnSameTeam(EntityLivingBase entity) { return entity instanceof Target && ((Target)entity).team; }
    }
    private static class Target extends EntityOtherPlayerMP { boolean team; Target() { super(null, null); } @Override public boolean isSpectator() { return false; } }
    private static class Connection extends NetHandlerPlayClient { Connection() { super(null, null, null, null); } @Override public void addToSendQueue(Packet packet) { packets++; } }
    private static class Font extends FontRenderer { Font() { super(null, null, null, false); } @Override public int getStringWidth(String text) { return text.length() * 6; } }
    private static class ReplayMouse extends MouseHelper { int x, y; @Override public void mouseXYChange() { deltaX = x; deltaY = y; } }
    private static class CameraRecorder {
        CameraEvent event; float yaw, pitch; int calls;
        @EventTarget public void onCamera(CameraEvent event) { this.event = event; yaw = player.rotationYaw; pitch = player.rotationPitch; calls++; }
    }
    private static class World extends WorldClient {
        boolean wall; World() { super(null, null, 0, null, null); }
        @Override public MovingObjectPosition rayTraceBlocks(Vec3 from, Vec3 to, boolean liquid, boolean noBox, boolean last) {
            return wall ? new MovingObjectPosition(new Vec3(0, 1, 1), EnumFacing.NORTH, new BlockPos(0, 1, 1)) : null;
        }
    }
}
