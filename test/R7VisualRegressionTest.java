import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.mod.mods.render.CapeMod;
import cn.sux1ng.client.mod.mods.render.TrajectoriesMod;
import cn.sux1ng.client.util.CapeManager;
import cn.sux1ng.client.util.ProjectilePrediction;
import com.mojang.authlib.GameProfile;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.*;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.WorldProviderSurface;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;

/** Actual trajectory entry point and repeated player cape lookups; downloads are recorded, not sent. */
public class R7VisualRegressionTest {
    private static final Unsafe UNSAFE = unsafe();
    private static FixtureMinecraft mc;
    private static Player player;
    private static World world;
    private static NetworkPlayerInfo info;
    private static int failures;
    private static final ResourceLocation LOCAL = new ResourceLocation("meowclient/test/local_cape");
    private static final ResourceLocation ORIGINAL = new ResourceLocation("meowclient/test/original_cape");
    private static final ResourceLocation OPTIFINE = new ResourceLocation("meowclient/test/optifine_cape");

    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        check("trajectory follows the camera interpolated heading during visible rotation", () -> {
            setup(); player.inventory.mainInventory[0] = new ItemStack(Items.bow);
            player.prevRotationYaw = -30; player.rotationYaw = 40;
            player.prevRotationPitch = -15; player.rotationPitch = 25;
            player.lastTickPosX = player.prevPosX = 2; player.posX = 2.2;
            for (float partial : new float[]{0, 0.25f, 0.75f, 1}) {
                Player shooter = allocate(Player.class); shooter.height = 1.8f;
                shooter.posX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partial;
                shooter.rotationYaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partial;
                shooter.rotationPitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partial;
                EntityArrow actual = new CentreArrow(world, shooter, 2);
                ProjectilePrediction.Result predicted = new TrajectoriesMod().predict(partial);
                Vec3 origin = new Vec3(actual.posX, actual.posY, actual.posZ);
                Vec3 next = origin.addVector(actual.motionX, actual.motionY, actual.motionZ);
                require(predicted.points.get(0).distanceTo(origin) < 0.0005 && predicted.points.get(1).distanceTo(next) < 0.0005,
                        "preview diverged from visible launch at partial " + partial + ": " + predicted.points.get(1).distanceTo(next) + " blocks");
            }
        });
        check("missing capes do not create a downloader on every player texture lookup", () -> {
            setup(); set(NetworkPlayerInfo.class, info, "locationCape", null);
            CapeMod cape = new CapeMod(); cape.capeStyle.setValue("OptiFine"); cape.update();
            for (int frame = 0; frame < 32; frame++) player.getLocationCape();
            require(mc.textures.downloads == 1, "32 lookups created " + mc.textures.downloads + " HTTP texture downloads");
            require(player.getLocationCape() == null, "pending download exposed an unready texture");
            ThreadDownloadImageData texture = (ThreadDownloadImageData)mc.textures.objects.values().iterator().next();
            texture.setBufferedImage(CapeManager.createMeowCape(0xffff69b4));
            ResourceLocation available = player.getLocationCape(); require(available != null, "successful download was never published");
            NetworkPlayerInfo recreated = new NetworkPlayerInfo(new GameProfile(UUID.randomUUID(), "meowtester"));
            require(available.equals(recreated.getLocationOptifineCape()) && mc.textures.downloads == 1,
                    "a recreated player info duplicated the same cape download");
        });
        check("water entry uses the same drag timing as the actual arrow update", () -> {
            setup(); world.water = true; player.inventory.mainInventory[0] = new ItemStack(Items.bow);
            EntityArrow actual = new CentreArrow(world, player, 2);
            ProjectilePrediction.Result predicted = new TrajectoriesMod().predict(1);
            for (int tick = 1; tick <= 6; tick++) {
                actual.onUpdate(); Vec3 position = new Vec3(actual.posX, actual.posY, actual.posZ);
                require(predicted.points.get(tick).distanceTo(position) < 0.001,
                        "water drag changed trajectory at tick " + tick + " by " + predicted.points.get(tick).distanceTo(position) + " blocks");
            }
        });
        check("Meow style selects the local cape with ShowOptiFine enabled", () -> {
            setup(); CapeMod cape = new CapeMod(); CapeManager.localCape = LOCAL;
            set(NetworkPlayerInfo.class, info, "locationOptifineCape", OPTIFINE);
            cape.capeStyle.setValue("Meow"); cape.optifine.setValue(true); cape.update();
            require(LOCAL.equals(player.getLocationCape()), "Meow style was replaced by the OptiFine option");
        });
        check("Minecon style preserves the original cape and disabled custom capes restore it", () -> {
            setup(); CapeMod cape = new CapeMod(); set(NetworkPlayerInfo.class, info, "locationOptifineCape", OPTIFINE);
            cape.capeStyle.setValue("Minecon"); cape.update();
            require(ORIGINAL.equals(player.getLocationCape()), "Minecon style used an unrelated cape");
            cape.capeStyle.setValue("OptiFine"); cape.update(); cape.disable();
            require(ORIGINAL.equals(player.getLocationCape()), "disabled module kept overriding the original cape");
        });
        check("disabled custom capes do not download when vanilla skin and cape getters repeat", () -> {
            setup(); CapeManager.ENABLED = false;
            set(NetworkPlayerInfo.class, info, "locationCape", null);
            for (int i = 0; i < 100; i++) { player.getLocationCape(); info.getLocationSkin(); }
            require(mc.textures.downloads == 0, "disabled custom capes still downloaded OptiFine textures");
        });
        check("real HTTP failures are shared and the downloader uses at most two workers", R7VisualRegressionTest::httpDownloads);
        if (failures != 0) throw new AssertionError(failures + " R7 visual checks failed");
        System.out.println("R7 visual checks passed");
    }
    private static void setup() throws Exception {
        mc = allocate(FixtureMinecraft.class); set(Minecraft.class, null, "theMinecraft", mc);
        mc.gameSettings = new GameSettings(); mc.textures = new RecordingTextures();
        world = allocate(World.class); set(net.minecraft.world.World.class, world, "provider", new WorldProviderSurface());
        mc.theWorld = world;
        player = allocate(Player.class); player.height = 1.8f; player.width = 0.6f; player.worldObj = world;
        player.inventory = new InventoryPlayer(player); mc.thePlayer = player;
        set(Entity.class, player, "entityUniqueID", UUID.fromString("12345678-1234-1234-1234-123456789abc"));
        info = new NetworkPlayerInfo(new GameProfile(player.getUniqueID(), "MeowTester"));
        set(NetworkPlayerInfo.class, info, "playerTexturesLoaded", true); set(NetworkPlayerInfo.class, info, "locationCape", ORIGINAL);
        set(AbstractClientPlayer.class, player, "playerInfo", info);
        CapeManager.ENABLED = true; CapeManager.OPTIFINE_MODE = true; CapeManager.localCape = null;
        MeowClient.modManager = new ModManager();
    }
    private static void httpDownloads() throws Exception {
        setup(); AtomicInteger requests = new AtomicInteger(), active = new AtomicInteger(), peak = new AtomicInteger();
        CountDownLatch firstTwo = new CountDownLatch(2), release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService handlers = Executors.newCachedThreadPool(runnable -> { Thread t = new Thread(runnable, "Cape test HTTP"); t.setDaemon(true); return t; });
        server.setExecutor(handlers);
        server.createContext("/missing", exchange -> {
            requests.incrementAndGet(); int count = active.incrementAndGet(); peak.accumulateAndGet(count, Math::max); firstTwo.countDown();
            try { release.await(3, TimeUnit.SECONDS); exchange.sendResponseHeaders(404, -1); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { active.decrementAndGet(); exchange.close(); }
        });
        ByteArrayOutputStream encoded = new ByteArrayOutputStream(); ImageIO.write(CapeManager.createMeowCape(0xff9a86ce), "png", encoded);
        server.createContext("/ready", exchange -> {
            requests.incrementAndGet(); byte[] png = encoded.toByteArray(); exchange.sendResponseHeaders(200, png.length);
            try (OutputStream output = exchange.getResponseBody()) { output.write(png); } finally { exchange.close(); }
        });
        server.createContext("/error", exchange -> { requests.incrementAndGet(); exchange.sendResponseHeaders(503, -1); exchange.close(); });
        server.start(); String address = "http://127.0.0.1:" + server.getAddress().getPort();
        Path cacheDirectory = Files.createTempDirectory("meow-cape-cache-check-"); Path cache = cacheDirectory.resolve("cape.png");
        List<ThreadDownloadImageData> textures = new ArrayList<>();
        try {
            for (int i = 0; i < 6; i++) {
                CapeManager.getOptiFineCape("Missing" + i);
                ThreadDownloadImageData texture = (ThreadDownloadImageData)mc.textures.objects.get(new ResourceLocation("meowclient", "capes/optifine/missing" + i));
                set(texture.getClass(), texture, "url", address + "/missing"); texture.loadTexture(null); textures.add(texture);
            }
            require(firstTwo.await(2, TimeUnit.SECONDS), "cape workers did not start");
            for (int frame = 0; frame < 1000; frame++) for (int i = 0; i < 6; i++) CapeManager.getOptiFineCape("Missing" + i);
            require(requests.get() == 2 && peak.get() == 2, "cape requests exceeded the worker limit: " + requests + "/" + peak);
            require(mc.textures.downloads == 6, "pending frames created replacement textures");
            release.countDown();
            for (ThreadDownloadImageData texture : textures) waitUntil(() -> !((AtomicBoolean)get(texture, "loading")).get(), "missing cape completion");
            for (int frame = 0; frame < 1000; frame++) for (int i = 0; i < 6; i++) {
                require(CapeManager.getOptiFineCape("Missing" + i) == null, "404 returned a cape texture");
            }
            require(requests.get() == 6 && mc.textures.downloads == 6, "negative cache retried failed capes on every frame");
            CapeManager.getOptiFineCape("ReadyCape");
            ThreadDownloadImageData success = (ThreadDownloadImageData)mc.textures.objects.get(new ResourceLocation("meowclient", "capes/optifine/readycape"));
            set(success.getClass(), success, "url", address + "/ready"); set(success.getClass(), success, "cache", cache.toFile()); success.loadTexture(null);
            waitUntil(() -> CapeManager.getOptiFineCape("ReadyCape") != null, "ready cape publication");
            for (int frame = 0; frame < 1000; frame++) CapeManager.getOptiFineCape("ReadyCape");
            require(requests.get() == 7 && mc.textures.downloads == 7, "successful cape downloaded again");
            waitUntil(() -> !((AtomicBoolean)get(success, "loading")).get(), "disk cache write");
            require(Files.isRegularFile(cache), "successful cape did not persist its disk cache");
            CapeManager.getOptiFineCape("CachedCape");
            ThreadDownloadImageData cached = (ThreadDownloadImageData)mc.textures.objects.get(new ResourceLocation("meowclient", "capes/optifine/cachedcape"));
            set(cached.getClass(), cached, "url", address + "/error"); set(cached.getClass(), cached, "cache", cache.toFile()); cached.loadTexture(null);
            waitUntil(() -> CapeManager.getOptiFineCape("CachedCape") != null, "disk cache reuse");
            require(requests.get() == 7, "cached cape still contacted the network");
            ThreadDownloadImageData retry = textures.get(0); set(retry.getClass(), retry, "retryAfter", System.nanoTime() - 1);
            set(retry.getClass(), retry, "url", address + "/error");
            CapeManager.getOptiFineCape("Missing0"); retry.loadTexture(null);
            waitUntil(() -> !((AtomicBoolean)get(retry, "loading")).get(), "backoff retry completion");
            require(requests.get() == 8 && mc.textures.objects.get(new ResourceLocation("meowclient", "capes/optifine/missing0")) == retry,
                    "retry did not reuse the existing texture");
            for (int i = 0; i < 1000; i++) CapeManager.getOptiFineCape("Missing0");
            require(requests.get() == 8, "HTTP error did not enter backoff");
        } finally { release.countDown(); server.stop(0); handlers.shutdownNow(); Files.deleteIfExists(cache); Files.deleteIfExists(cacheDirectory); }
    }
    private static void waitUntil(Condition condition, String name) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.ready()) { if (System.nanoTime() > deadline) throw new AssertionError("timed out: " + name); Thread.sleep(5); }
    }
    private interface Condition { boolean ready() throws Exception; }
    private static Object get(Object instance, String name) throws Exception { Field field = instance.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(instance); }
    private static class CentreArrow extends EntityArrow {
        CentreArrow(World world, Player shooter, float speed) { super(world, shooter, speed); }
        @Override public void setThrowableHeading(double x, double y, double z, float velocity, float spread) { super.setThrowableHeading(x, y, z, velocity, 0); }
        @Override public void onEntityUpdate() { inWater = ((World)worldObj).water && posZ >= 4; }
        @Override protected void doBlockCollisions() {}
        @Override public boolean isWet() { return isInWater(); }
    }
    private static class Player extends EntityPlayerSP { Player() { super(null, null, null, null); } }
    private static class World extends WorldClient {
        boolean water;
        World() { super(null, null, 0, null, null); }
        @Override public boolean isBlockLoaded(BlockPos pos) { return true; }
        @Override public IBlockState getBlockState(BlockPos pos) { return (water && pos.getZ() >= 4 ? Blocks.water : Blocks.air).getDefaultState(); }
        @Override public MovingObjectPosition rayTraceBlocks(Vec3 from, Vec3 to, boolean liquids, boolean noBox, boolean last) { return null; }
        @Override public List<Entity> getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB box) { return Collections.emptyList(); }
        @Override public void spawnParticle(EnumParticleTypes type, double x, double y, double z, double dx, double dy, double dz, int... args) {}
    }
    private static class FixtureMinecraft extends Minecraft {
        RecordingTextures textures; FixtureMinecraft() { super(null); }
        @Override public TextureManager getTextureManager() { return textures; }
    }
    private static class RecordingTextures extends TextureManager {
        int downloads; Map<ResourceLocation, ITextureObject> objects = new HashMap<>();
        RecordingTextures() { super(null); }
        @Override public boolean loadTexture(ResourceLocation location, ITextureObject texture) {
            if (texture instanceof ThreadDownloadImageData) downloads++;
            objects.put(location, texture); return true;
        }
        @Override public ITextureObject getTexture(ResourceLocation location) { return objects.get(location); }
    }
    private static void check(String name, Checked test) { try { test.run(); System.out.println("PASS " + name); } catch (Throwable failure) { failures++; System.err.println("FAIL " + name + ": " + failure); if (!(failure instanceof AssertionError)) failure.printStackTrace(); } }
    private interface Checked { void run() throws Exception; }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static <T> T allocate(Class<T> type) throws Exception { return type.cast(UNSAFE.allocateInstance(type)); }
    private static void set(Class<?> type, Object instance, String name, Object value) throws Exception { Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value); }
    private static Unsafe unsafe() { try { Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true); return (Unsafe)field.get(null); } catch (Exception failure) { throw new RuntimeException(failure); } }
}
