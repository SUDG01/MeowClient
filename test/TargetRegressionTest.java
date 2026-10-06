import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.config.configs.ModConfig;
import cn.sux1ng.client.events.EventManager;
import cn.sux1ng.client.events.impl.*;
import cn.sux1ng.client.mod.*;
import cn.sux1ng.client.mod.mods.combat.*;
import cn.sux1ng.client.mod.mods.misc.*;
import cn.sux1ng.client.mod.mods.render.*;
import cn.sux1ng.client.targeting.TargetRules;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.*;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.network.*;
import net.minecraft.client.settings.*;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Shared rules and actual automatic handlers with deterministic server player metadata. */
public class TargetRegressionTest {
    private static final Unsafe UNSAFE = unsafe();
    private static FixtureMinecraft mc;
    private static Player player;
    private static World world;
    private static Connection connection;
    private static TargetMod target;
    private static AntiBotMod bots;
    private static AtomicLong clock;
    private static int failures;
    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        check("Target and AntiBot are enabled in Misc without Blatant", () -> {
            setup(); require(target.getCategory() == Category.MISC && bots.getCategory() == Category.MISC && target.isEnable() && bots.isEnable(), "missing default Misc controls");
            require(!BlatantPolicy.requiresBlatant(target) && !BlatantPolicy.requiresBlatant(bots), "filters were gated as automatic gameplay");
        });
        check("global player selection overrides local categories and hides duplicate settings", () -> {
            setup(); Remote human = remote("Human", 2, 0, 3, 100); tab(human);
            require(TargetRules.canAttack(human, false, false, false, false), "global player option did not override local flags");
            require(!MeowClient.modManager.getByClass(KillAuraMod.class).players.isVisible(), "local category was still shown while unified");
            target.players.setValue(false); require(!TargetRules.canAttack(human) && !TargetRules.canRender(human), "disabled players remained targets");
            target.setEnable(false);
            require(TargetRules.canAttack(human, true, false, false, false) && !TargetRules.canAttack(human, false, true, true, true), "legacy selection was not restored");
            require(MeowClient.modManager.getByClass(KillAuraMod.class).players.isVisible(), "legacy setting stayed hidden");
        });
        check("hostile category includes ghasts and slimes outside EntityMob", () -> {
            setup(); target.mobs.setValue(true);
            for (Class<? extends EntityLivingBase> type : Arrays.asList(EntityZombie.class, EntityGhast.class, EntitySlime.class)) {
                EntityLivingBase entity = living(type, 0, 3); require(TargetRules.canAttack(entity) && TargetRules.canRender(entity), "hostile omitted: " + type);
                target.mobs.setValue(false); require(!TargetRules.canAttack(entity) && !TargetRules.canRender(entity), "hostile option was ignored"); target.mobs.setValue(true);
            }
        });
        check("friendly category includes villagers, golems, bats, squid and animals", () -> {
            setup(); target.friendlies.setValue(true);
            for (Class<? extends EntityLivingBase> type : Arrays.asList(EntityVillager.class, EntityIronGolem.class, EntityBat.class, EntitySquid.class, EntityPig.class)) {
                EntityLivingBase entity = living(type, 0, 3); require(TargetRules.canAttack(entity) && TargetRules.canRender(entity), "friendly omitted: " + type);
            }
            require(!TargetRules.canAttack(living(net.minecraft.entity.item.EntityArmorStand.class, 0, 3)), "decorative armor stand became a friendly target");
            target.friendlies.setValue(false); require(!TargetRules.canRender(living(EntityVillager.class, 0, 3)), "friendly visuals ignored the option");
        });
        check("invisibility requires both the category and invisibility permission", () -> {
            setup(); Remote human = remote("HiddenHuman", 2, 0, 3, 100); tab(human); human.setInvisible(true);
            require(!TargetRules.canAttack(human) && !TargetRules.canRender(human), "invisible target bypassed permission");
            target.invisible.setValue(true); require(TargetRules.canAttack(human) && TargetRules.canRender(human), "valid invisible player was considered a bot");
            target.players.setValue(false); require(!TargetRules.canAttack(human), "invisibility bypassed the player category");
        });
        check("self, spectators and dead entities stay excluded", () -> {
            setup(); Remote human = remote("Human", 2, 0, 3, 100); NetworkPlayerInfo info = tab(human);
            require(!TargetRules.canAttack(player) && !TargetRules.canRender(player), "local player became a target");
            set(NetworkPlayerInfo.class, info, "gameType", WorldSettings.GameType.SPECTATOR); require(!TargetRules.canAttack(human), "spectator remained targetable");
            set(NetworkPlayerInfo.class, info, "gameType", WorldSettings.GameType.SURVIVAL); human.isDead = true; require(!TargetRules.canRender(human), "dead entity was displayed");
        });
        check("new valid players wait before automation without being hidden as bots", () -> {
            setup(); Remote human = remote("NewHuman", 2, 0, 3, 0); tab(human);
            require(!TargetRules.canAttack(human) && TargetRules.canRender(human) && !bots.isBot(human), "new player was attacked or incorrectly hidden");
            clock.addAndGet(500); require(TargetRules.canAttack(human) && !bots.isBot(human), "new valid player never completed observation");
        });
        check("missing Tab blocks automation immediately and hides only after observation", () -> {
            setup(); Remote ghost = remote("Ghost", 2, 0, 3, 0);
            require(!TargetRules.canAttack(ghost) && TargetRules.canRender(ghost), "unknown profile was attacked or hidden too early");
            clock.addAndGet(1000); require(bots.isBot(ghost) && !TargetRules.canRender(ghost), "missing Tab bot was not identified");
            bots.hideBots.setValue(false); require(TargetRules.canRender(ghost) && !TargetRules.canAttack(ghost), "visual choice changed attack protection");
            tab(ghost); require(TargetRules.canAttack(ghost) && !bots.isBot(ghost), "late Tab entry did not recover the real player");
        });
        check("normal zero ping and airborne players remain allowed with default checks", () -> {
            setup(); Remote human = remote("AirHuman", 2, 0, 3, 100); tab(human); human.onGround = false;
            require(TargetRules.canAttack(human) && !bots.isBot(human), "zero ping or airborne player was excluded");
        });
        check("mismatched profiles and missing game modes are rejected and can recover", () -> {
            setup(); Remote human = remote("Human", 2, 0, 3, 100);
            NetworkPlayerInfo wrong = new NetworkPlayerInfo(new GameProfile(human.getUniqueID(), "DifferentName"));
            set(NetworkPlayerInfo.class, wrong, "gameType", WorldSettings.GameType.SURVIVAL); connection.users.put(human.getUniqueID(), wrong);
            require(!TargetRules.canAttack(human) && bots.isBot(human), "mismatched Tab profile was accepted");
            NetworkPlayerInfo valid = tab(human); require(TargetRules.canAttack(human), "corrected profile did not recover");
            set(NetworkPlayerInfo.class, valid, "gameType", WorldSettings.GameType.NOT_SET); require(!TargetRules.canAttack(human), "incomplete game mode was accepted");
            bots.profileCheck.setValue(false); require(TargetRules.canAttack(human), "disabling profile check had no effect");
        });
        check("duplicate identity preserves the older genuine player", () -> {
            setup(); Remote human = remote("Human", 2, 0, 3, 100); tab(human);
            Remote clone = remote("Human", 3, 1, 3, 0); set(Entity.class, clone, "entityUniqueID", human.getUniqueID()); set(EntityPlayer.class, clone, "gameProfile", human.getGameProfile());
            require(TargetRules.canAttack(human) && !TargetRules.canAttack(clone), "duplicate UUID displaced the genuine player");
            clock.addAndGet(1000); require(bots.isBot(clone) && !bots.isBot(human), "duplicate identity marked both players as bots");
            world.loadedEntityList.remove(human); player.ticksExisted++; require(TargetRules.canAttack(clone), "removed identity continued to poison the remaining entity");
        });
        check("different UUIDs with a duplicate name preserve the older profile", () -> {
            setup(); Remote human = remote("Human", 2, 0, 3, 100); tab(human); Remote clone = remote("human", 3, 1, 3, 100); tab(clone);
            require(TargetRules.canAttack(human) && !TargetRules.canAttack(clone), "duplicate name was not filtered deterministically");
        });
        check("optional ground evidence exempts creative flight and tracks landing", () -> {
            setup(); bots.groundCheck.setValue(true); Remote human = remote("AirHuman", 2, 0, 3, 100); NetworkPlayerInfo info = tab(human);
            require(!TargetRules.canAttack(human), "ground evidence requirement was ignored");
            set(NetworkPlayerInfo.class, info, "gameType", WorldSettings.GameType.CREATIVE); require(TargetRules.canAttack(human), "creative flight was blocked");
            set(NetworkPlayerInfo.class, info, "gameType", WorldSettings.GameType.SURVIVAL); human.onGround = true; require(TargetRules.canAttack(human), "landing was not observed");
            human.onGround = false; require(TargetRules.canAttack(human), "jumping forgot earlier ground evidence");
        });
        check("world replacement and toggling AntiBot clear observation state", () -> {
            setup(); Remote ghost = remote("Ghost", 2, 0, 3, 100); require(bots.isBot(ghost), "fixture ghost was not identified");
            world = makeWorld(); mc.theWorld = world; player.worldObj = world;
            Remote human = remote("Ghost", 2, 0, 3, 100); tab(human); require(TargetRules.canAttack(human) && !bots.isBot(human), "old world bot status survived");
            connection.users.clear(); require(!TargetRules.canAttack(human), "missing metadata was ignored");
            bots.setEnable(false); require(TargetRules.canAttack(human), "disabled AntiBot still filtered"); bots.setEnable(true); tab(human); require(TargetRules.canAttack(human), "reenabling retained old bot decisions");
            mc.theWorld = null; require(!TargetRules.canAttack(human) && !TargetRules.canRender(human), "disconnected world retained a target");
        });
        check("singleplayer skips server bot heuristics", () -> {
            setup(); mc.singleplayer = true; Remote human = remote("OfflineHuman", 2, 0, 3, 0);
            require(TargetRules.canAttack(human) && !bots.isBot(human), "singleplayer player entity needed server Tab information");
        });
        check("actual KillAura handlers ignore a nearby fake and choose the genuine player", () -> {
            setup(); Remote ghost = remote("Ghost", 3, 1, 2, 100), human = remote("Human", 2, 0, 3, 100); tab(human);
            MeowClient.modManager.getMods().remove(MeowClient.modManager.getByClass(KillAuraMod.class));
            KillAuraMod aura = new KillAuraMod(clock::get, new Random(42)); MeowClient.modManager.getMods().add(aura);
            unlock(); aura.rotMode.setValue("Lock"); aura.maxTurnSpeed.setValue(90.0); aura.reactionDelay.setValue(0.0); aura.autoBlock.setValue(false); aura.setEnable(true);
            for (int i = 0; i < 20; i++) motionTick();
            require(aura.getTarget() == human && !connection.attacks.isEmpty() && !connection.attacks.contains(ghost), "Aura followed or attacked the fake player");
            int before = connection.attacks.size(); MotionEvent pre = new MotionEvent(MotionEvent.Type.PRE, 0, 0, 0, player.rotationYaw, player.rotationPitch, true);
            player.ticksExisted++; EventManager.call(pre); target.players.setValue(false); EventManager.call(new MotionEvent(MotionEvent.Type.POST, 0, 0, 0, pre.yaw, pre.pitch, true));
            require(connection.attacks.size() == before, "category changed after PRE but POST still attacked");
        });
        check("Aimbot and TargetHUD use the same allowed entity", () -> {
            setup(); Remote ghost = remote("Ghost", 3, 0.5, 2, 100); unlock(); AimbotMod aim = MeowClient.modManager.getByClass(AimbotMod.class);
            // Replace its clock while keeping the same real event handler registration.
            MeowClient.modManager.getMods().remove(aim); aim = new AimbotMod(() -> clock.get() * 1_000_000L); MeowClient.modManager.getMods().add(aim); aim.setEnable(true);
            for (int i = 0; i < 20; i++) cameraFrame(); require(aim.getTarget() == null && player.rotationYaw == 0, "aim correction followed the fake");
            Remote human = remote("Human", 2, 0.9, 3, 100); tab(human);
            for (int i = 0; i < 30; i++) cameraFrame(); require(aim.getTarget() == human && player.rotationYaw < -1, "aim did not follow the genuine player");
            TargetHUDMod hud = MeowClient.modManager.getByClass(TargetHUDMod.class); require(hud.getDisplayTarget() == human, "HUD did not use the aim target");
            target.players.setValue(false); require(hud.getDisplayTarget() == null, "HUD retained an excluded target");
        });
        check("AutoClicker refuses fake cursor targets while permitted manual-style clicks remain possible", () -> {
            setup(); Remote ghost = remote("Ghost", 3, 0, 3, 100); Remote human = remote("Human", 2, 0, 3, 100); tab(human);
            AutoClickerMod clicker = new AutoClickerMod(); mc.objectMouseOver = new MovingObjectPosition(ghost);
            for (int i = 0; i < 50; i++) clicker.update(); require(mc.clicks == 0, "AutoClicker clicked a fake entity");
            clicker = new AutoClickerMod(); mc.objectMouseOver = new MovingObjectPosition(human); clicker.update(); require(mc.clicks == 1, "allowed cursor target could not be clicked");
            clicker = new AutoClickerMod(); target.players.setValue(false); clicker.update(); require(mc.clicks == 1, "global player exclusion was ignored by AutoClicker");
        });
        check("shared target and AntiBot settings survive config restoration", () -> {
            setup(); target.friendlies.setValue(true); target.players.setValue(false); bots.groundCheck.setValue(true); bots.spawnDelay.setValue(750.0); bots.hideBots.setValue(false);
            ModConfig config = new ModConfig(); JsonObject saved = config.serializeModules(MeowClient.modManager.getMods());
            setup(); config.restoreSettings(saved, MeowClient.modManager.getMods()); config.activatePendingModules();
            require(target.friendlies.getValue() && !target.players.getValue() && bots.groundCheck.getValue() && bots.spawnDelay.getValue() == 750 && !bots.hideBots.getValue(), "shared settings were lost");
        });
        cleanup(); if (failures != 0) throw new AssertionError(failures + " target checks failed"); System.out.println("Target and AntiBot checks passed");
    }
    private static void setup() throws Exception {
        cleanup(); mc = allocate(FixtureMinecraft.class); set(Minecraft.class, null, "theMinecraft", mc); mc.gameSettings = new GameSettings(); mc.inGameHasFocus = true;
        mc.fontRendererObj = allocate(Font.class); world = makeWorld(); mc.theWorld = world; player = living(Player.class, 0, 0); player.ticksExisted = 100;
        player.inventory = new InventoryPlayer(player); player.inventory.mainInventory[0] = new ItemStack(Items.iron_sword); set(EntityPlayer.class, player, "gameProfile", new GameProfile(player.getUniqueID(), "MeowOwner")); mc.thePlayer = player;
        connection = allocate(Connection.class); connection.users = new HashMap<>(); connection.attacks = new ArrayList<>(); set(EntityPlayerSP.class, player, "sendQueue", connection);
        mc.playerController = new PlayerControllerMP(mc, connection); clock = new AtomicLong(1000); MeowClient.modManager = new ModManager(); MeowClient.modManager.load();
        AntiBotMod defaultBots = MeowClient.modManager.getByClass(AntiBotMod.class); defaultBots.setEnable(false); MeowClient.modManager.getMods().remove(defaultBots);
        bots = new AntiBotMod(clock::get); MeowClient.modManager.getMods().add(bots); bots.setEnable(true); target = MeowClient.modManager.getByClass(TargetMod.class);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
    }
    private static void cleanup() { if (MeowClient.modManager != null) for (Mod mod : new ArrayList<>(MeowClient.modManager.getMods())) if (mod.isEnable()) mod.setEnable(false); }
    private static World makeWorld() throws Exception { World world = allocate(World.class); set(net.minecraft.world.World.class, world, "loadedEntityList", new ArrayList<Entity>()); return world; }
    private static <T extends EntityLivingBase> T living(Class<T> type, double x, double z) throws Exception {
        T entity = allocate(type); entity.worldObj = world; entity.width = 0.6f; entity.height = 1.8f; entity.posX = entity.lastTickPosX = x; entity.posZ = entity.lastTickPosZ = z;
        entity.setEntityBoundingBox(new AxisAlignedBB(x - 0.3, 0, z - 0.3, x + 0.3, 1.8, z + 0.3)); set(Entity.class, entity, "entityUniqueID", UUID.randomUUID());
        DataWatcher watcher = new DataWatcher(entity); watcher.addObject(0, (byte)0); watcher.addObject(6, 20f); set(Entity.class, entity, "dataWatcher", watcher); return entity;
    }
    private static Remote remote(String name, int id, double x, double z, int age) throws Exception { Remote entity = living(Remote.class, x, z); entity.setEntityId(id); entity.ticksExisted = age; set(EntityPlayer.class, entity, "gameProfile", new GameProfile(entity.getUniqueID(), name)); world.loadedEntityList.add(entity); return entity; }
    private static NetworkPlayerInfo tab(Remote entity) throws Exception { NetworkPlayerInfo info = new NetworkPlayerInfo(entity.getGameProfile()); set(NetworkPlayerInfo.class, info, "gameType", WorldSettings.GameType.SURVIVAL); connection.users.put(entity.getUniqueID(), info); return info; }
    private static void unlock() { MeowClient.modManager.getByClass(BlatantMod.class).setEnable(true); }
    private static void cameraFrame() { clock.addAndGet(17); player.ticksExisted++; EventManager.call(new CameraEvent(1, 0, 0, true)); }
    private static void motionTick() { clock.addAndGet(50); player.ticksExisted++; MotionEvent pre = new MotionEvent(MotionEvent.Type.PRE, 0, 0, 0, player.rotationYaw, player.rotationPitch, true); EventManager.call(pre); EventManager.call(new MotionEvent(MotionEvent.Type.POST, 0, 0, 0, pre.yaw, pre.pitch, true)); }
    private static class FixtureMinecraft extends Minecraft { int clicks; boolean singleplayer; FixtureMinecraft() { super(null); } @Override public void clickMouse() { clicks++; } @Override public boolean isSingleplayer() { return singleplayer; } @Override public boolean isUnicode() { return false; } }
    private static class Player extends EntityPlayerSP { Player() { super(null, null, null, null); } @Override public void swingItem() {} @Override public void attackTargetEntityWithCurrentItem(Entity target) {} @Override public boolean isOnSameTeam(EntityLivingBase entity) { return false; } }
    private static class Remote extends EntityOtherPlayerMP { Remote() { super(null, null); } }
    private static class Connection extends NetHandlerPlayClient {
        Map<UUID, NetworkPlayerInfo> users; List<Entity> attacks; Connection() { super(null, null, null, null); }
        @Override public NetworkPlayerInfo getPlayerInfo(UUID id) { return users.get(id); }
        @Override public Collection<NetworkPlayerInfo> getPlayerInfoMap() { return users.values(); }
        @Override public void addToSendQueue(Packet packet) { if (packet instanceof C02PacketUseEntity && ((C02PacketUseEntity)packet).getAction() == C02PacketUseEntity.Action.ATTACK) attacks.add(((C02PacketUseEntity)packet).getEntityFromWorld(world)); }
    }
    private static class World extends WorldClient { World() { super(null, null, 0, null, null); } @Override public MovingObjectPosition rayTraceBlocks(Vec3 a, Vec3 b, boolean l, boolean n, boolean r) { return null; } @Override public Entity getEntityByID(int id) { for (Entity entity : loadedEntityList) if (entity.getEntityId() == id) return entity; return null; } }
    private static class Font extends FontRenderer { Font() { super(null, null, null, false); } @Override public int getStringWidth(String text) { return text.length() * 6; } }
    private static void check(String name, Checked test) { try { test.run(); System.out.println("PASS " + name); } catch (Throwable failure) { failures++; System.err.println("FAIL " + name + ": " + failure); if (!(failure instanceof AssertionError)) failure.printStackTrace(); } }
    private interface Checked { void run() throws Exception; }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static <T> T allocate(Class<T> type) throws Exception { return type.cast(UNSAFE.allocateInstance(type)); }
    private static void set(Class<?> type, Object instance, String name, Object value) throws Exception { Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value); }
    private static Unsafe unsafe() { try { Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true); return (Unsafe)field.get(null); } catch (Exception failure) { throw new RuntimeException(failure); } }
}
