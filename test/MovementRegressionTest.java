import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.mod.mods.misc.BlatantMod;
import cn.sux1ng.client.mod.mods.movement.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.*;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.entity.SharedMonsterAttributes;
import cn.sux1ng.client.events.EventManager;
import cn.sux1ng.client.events.impl.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Timer;
import net.minecraft.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.util.FoodStats;
import java.util.Collections;
import java.util.List;
import sun.misc.Unsafe;
import java.lang.reflect.Field;

/** Movement lifecycle and item use compensation regressions, without native windows. */
public class MovementRegressionTest {
    private static final Unsafe UNSAFE = unsafe();
    private static FixtureMinecraft mc;
    private static Player player;
    private static int failures;
    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        check("all Speed modes handle an absent player and world", () -> {
            for (String mode : new String[]{"AutoJump", "Vanilla", "NCP"}) {
                setup(); mc.thePlayer = null; mc.theWorld = null;
                SpeedMod speed = new SpeedMod(); speed.mode.setValue(mode); MeowClient.modManager.getMods().add(speed);
                speed.setEnable(true); speed.update(); speed.setEnable(false);
            }
        });
        check("Speed does not reset a timer it never changed", () -> {
            setup(); mc.timer.timerSpeed = 1.25f;
            SpeedMod speed = new SpeedMod(); MeowClient.modManager.getMods().add(speed); speed.setEnable(true); speed.setEnable(false);
            require(mc.timer.timerSpeed == 1.25f, "unrelated timer speed was reset to " + mc.timer.timerSpeed);
        });
        check("zero NoSlow amount preserves existing motion while using an item", () -> {
            setup(); player.setItemInUse(player.getHeldItem(), 72000); player.onGround = true; player.moveForward = 1; player.motionX = 0.03;
            NoSlowMod slow = new NoSlowMod(); MeowClient.modManager.getMods().add(slow); slow.amount.setValue(0.0); slow.setEnable(true); slow.update();
            require(Math.abs(player.motionX - 0.03) < 1e-9 && player.motionZ == 0, "zero compensation changed motion to " + player.motionX);
        });
        check("full NoSlow reaches the actual fresh input slowdown in onLivingUpdate", () -> {
            setup(); player.setItemInUse(player.getHeldItem(), 72000); player.onGround = true;
            NoSlowMod slow = new NoSlowMod(); MeowClient.modManager.getMods().add(slow); slow.setEnable(true); slow.update(); player.onLivingUpdate();
            require(Math.abs(player.travelForward - 0.98f) < 0.00001, "full input was still reduced to " + player.travelForward);
        });
        check("Speed and a held jump key produce one takeoff per tick", () -> {
            setup(); player.onGround = true; player.moveForward = 1; ((ReplayInput)player.movementInput).heldJump = true;
            SpeedMod speed = new SpeedMod(); MeowClient.modManager.getMods().add(speed); speed.setEnable(true); speed.update(); player.onLivingUpdate();
            require(player.jumps == 1, "automatic and manual jumping stacked " + player.jumps + " takeoffs");
        });
        check("input compensation modes retain vanilla at zero and scale ground and air", () -> {
            for (String mode : new String[]{"Vanilla", "Ground", "Air", "Adaptive"}) for (boolean ground : new boolean[]{true, false}) for (double amount : new double[]{0, 0.5, 1}) {
                setup(); player.setItemInUse(player.getHeldItem(), 72000); player.onGround = ground;
                NoSlowMod slow = new NoSlowMod(); slow.mode.setValue(mode); slow.amount.setValue(amount); MeowClient.modManager.getMods().add(slow); slow.setEnable(true); player.onLivingUpdate();
                double factor = 0.2 + 0.8 * amount * (mode.equals("Adaptive") && !ground ? 0.5 : 1);
                if (mode.equals("Ground") && !ground || mode.equals("Air") && ground) factor = 0.2;
                require(Math.abs(player.travelForward - 0.98 * factor) < 1e-5, mode + " changed input compensation: " + player.travelForward);
            }
        });
        check("food, drinks and bow input remain independently selectable", () -> {
            for (net.minecraft.item.Item item : new net.minecraft.item.Item[]{Items.apple, Items.potionitem, Items.milk_bucket, Items.bow}) {
                setup(); ItemStack stack = new ItemStack(item); player.inventory.mainInventory[0] = stack; player.setItemInUse(stack, stack.getMaxItemUseDuration());
                NoSlowMod slow = new NoSlowMod(); MeowClient.modManager.getMods().add(slow); slow.setEnable(true); player.onLivingUpdate(); require(Math.abs(player.travelForward - 0.98) < 1e-5, "selected item stayed slow: " + item);
                slow.consume.setValue(false); slow.bows.setValue(false); player.onLivingUpdate(); require(Math.abs(player.travelForward - 0.196) < 1e-5, "disabled item compensation was ignored");
            }
        });
        check("all Speed modes use fresh physics input and normalized diagonal movement", () -> {
            for (String mode : new SpeedMod().mode.getModes()) {
                setup(); player.physics = true; player.onGround = true; player.motionY = 0;
                SpeedMod speed = new SpeedMod(); speed.mode.setValue(mode); speed.speed.setValue(0.4); MeowClient.modManager.getMods().add(speed); speed.setEnable(true);
                for (int tick = 0; tick < 20; tick++) {
                    player.prevPosX = player.posX; player.prevPosZ = player.posZ; player.ticksExisted++;
                    player.onLivingUpdate(); EventManager.call(new MotionEvent(MotionEvent.Type.POST, player.posX, player.posY, player.posZ, 0, 0, player.onGround));
                    require(Double.isFinite(player.posX) && Double.isFinite(player.posY) && Double.isFinite(player.posZ), mode + " produced invalid physics");
                }
                require(player.posZ > 2, mode + " did not move the actual player");
                require(mode.equals("Ground") || mode.equals("Strafe") ? player.jumps == 0 : player.jumps > 0, mode + " had the wrong jump behavior");
            }
            double[] direction = cn.sux1ng.client.movement.MovementSupport.direction(1, 1, 0, 0.4);
            require(Math.abs(Math.hypot(direction[0], direction[1]) - 0.4) < 1e-8, "diagonal input amplified horizontal speed");
        });
        check("packet corrections pause movement assistance without cancelling correction", () -> {
            setup(); java.util.concurrent.atomic.AtomicLong time = new java.util.concurrent.atomic.AtomicLong(1000);
            SpeedMod speed = new SpeedMod(time::get); MeowClient.modManager.getMods().add(speed); speed.setEnable(true); player.onGround = true;
            PacketReceiveEvent correction = new PacketReceiveEvent(new S08PacketPlayerPosLook(0, 0, 0, 0, 0, java.util.EnumSet.noneOf(S08PacketPlayerPosLook.EnumFlags.class))); EventManager.call(correction);
            player.onLivingUpdate(); require(player.jumps == 0 && !correction.isCancelled(), "setback was rejected or instantly boosted again");
            time.addAndGet(1000); player.ticksExisted++; player.onLivingUpdate(); require(player.jumps == 1, "movement did not recover after the cooldown");
        });
        if (failures != 0) throw new AssertionError(failures + " movement checks failed");
        System.out.println("Movement checks passed");
    }
    private static void setup() throws Exception {
        if (MeowClient.modManager != null) for (cn.sux1ng.client.mod.Mod mod : MeowClient.modManager.getMods()) if (mod.isEnable()) mod.setEnable(false);
        mc = allocate(FixtureMinecraft.class); set(Minecraft.class, null, "theMinecraft", mc); mc.gameSettings = new GameSettings(); mc.inGameHasFocus = true;
        mc.fontRendererObj = allocate(Font.class); set(Minecraft.class, mc, "timer", allocate(Timer.class)); mc.timer.timerSpeed = 1;
        mc.theWorld = allocate(World.class); set(net.minecraft.world.World.class, mc.theWorld, "isRemote", true); set(net.minecraft.world.World.class, mc.theWorld, "theProfiler", new Profiler());
        player = allocate(Player.class); player.worldObj = mc.theWorld; player.width = 0.6f; player.height = 1.8f;
        set(EntityPlayerSP.class, player, "mc", mc); player.setEntityBoundingBox(new AxisAlignedBB(-0.3, 0, -0.3, 0.3, 1.8, 0.3));
        player.capabilities = new PlayerCapabilities(); player.movementInput = new ReplayInput(); set(net.minecraft.entity.player.EntityPlayer.class, player, "speedInAir", 0.02f); player.jumpMovementFactor = 0.02f; set(net.minecraft.entity.player.EntityPlayer.class, player, "foodStats", new FoodStats());
        player.getAttributeMap().registerAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.1); player.getAttributeMap().registerAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20);
        player.inventory = new InventoryPlayer(player); player.inventory.mainInventory[0] = new ItemStack(Items.iron_sword);
        DataWatcher watcher = new DataWatcher(player); watcher.addObject(0, (byte)0); watcher.addObject(6, 20f); set(Entity.class, player, "dataWatcher", watcher); mc.thePlayer = player; set(Minecraft.class, mc, "renderViewEntity", player);
        MeowClient.modManager = new ModManager(); BlatantMod blatant = new BlatantMod(); MeowClient.modManager.getMods().add(blatant); blatant.setEnable(true);
    }
    private static class FixtureMinecraft extends Minecraft { FixtureMinecraft() { super(null); } @Override public boolean isUnicode() { return false; } }
    private static class Player extends EntityPlayerSP {
        float travelForward, travelStrafe; int jumps; boolean physics; Player() { super(null, null, null, null); }
        @Override public boolean isPotionActive(net.minecraft.potion.Potion potion) { return false; }
        @Override protected boolean pushOutOfBlocks(double x, double y, double z) { return false; }
        @Override public boolean isSpectator() { return false; }
        @Override public void moveEntityWithHeading(float strafe, float forward) { travelForward = forward; travelStrafe = strafe; if (physics) super.moveEntityWithHeading(strafe, forward); }
        @Override public void jump() { jumps++; super.jump(); }
        @Override protected void doBlockCollisions() {}
        @Override protected void playStepSound(BlockPos pos, net.minecraft.block.Block block) {}
        @Override public boolean isInLava() { return false; }
        @Override public boolean isWet() { return false; }
        @Override public void fall(float distance, float multiplier) {}
    }
    private static class ReplayInput extends MovementInput { boolean heldJump; @Override public void updatePlayerMoveState() { moveForward = 1; moveStrafe = 0; jump = heldJump; sneak = false; } }
    private static class World extends WorldClient {
        World() { super(null, null, 0, null, null); }
        @Override public EnumDifficulty getDifficulty() { return EnumDifficulty.NORMAL; }
        @Override public List<Entity> getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB box) { return Collections.emptyList(); }
        @Override public IBlockState getBlockState(BlockPos pos) { return (pos.getY() < 0 ? Blocks.stone : Blocks.air).getDefaultState(); }
        @Override public boolean isBlockLoaded(BlockPos pos) { return true; }
        @Override public boolean handleMaterialAcceleration(AxisAlignedBB box, net.minecraft.block.material.Material material, Entity entity) { return false; }
        @Override public boolean isFlammableWithin(AxisAlignedBB box) { return false; }
        @Override public net.minecraft.world.chunk.Chunk getChunkFromBlockCoords(BlockPos pos) { try { net.minecraft.world.chunk.Chunk chunk = allocate(net.minecraft.world.chunk.Chunk.class); set(net.minecraft.world.chunk.Chunk.class, chunk, "isChunkLoaded", true); return chunk; } catch (Exception failure) { throw new RuntimeException(failure); } }
        @Override public List<AxisAlignedBB> getCollidingBoundingBoxes(Entity entity, AxisAlignedBB box) {
            AxisAlignedBB floor = new AxisAlignedBB(-100, -1, -100, 100, 0, 100);
            return floor.intersectsWith(box) ? Collections.singletonList(floor) : Collections.emptyList();
        }
    }
    private static class Font extends FontRenderer { Font() { super(null, null, null, false); } @Override public int getStringWidth(String text) { return text.length() * 6; } }
    private static void check(String name, Checked test) { try { test.run(); System.out.println("PASS " + name); } catch (Throwable failure) { failures++; System.err.println("FAIL " + name + ": " + failure); if (!(failure instanceof AssertionError)) failure.printStackTrace(); } }
    private interface Checked { void run() throws Exception; }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static <T> T allocate(Class<T> type) throws Exception { return type.cast(UNSAFE.allocateInstance(type)); }
    private static void set(Class<?> type, Object instance, String name, Object value) throws Exception { Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value); }
    private static Unsafe unsafe() { try { Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true); return (Unsafe)field.get(null); } catch (Exception failure) { throw new RuntimeException(failure); } }
}
