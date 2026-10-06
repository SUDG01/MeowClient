import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.mod.mods.render.CapeMod;
import cn.sux1ng.client.util.CapeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import sun.misc.Unsafe;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.*;

/** Exercises the actual cape layer and dynamic textures in an offscreen GPU context. */
public class R7CapeRenderRegressionTest {
    private static final Unsafe UNSAFE = unsafe();
    private static final int SIZE = 256;
    public static void main(String[] args) throws Exception {
        Pbuffer buffer = new Pbuffer(SIZE, SIZE, new PixelFormat().withDepthBits(24), null, null);
        Path directory = Files.createTempDirectory("meow-local-cape-check-");
        Path clientDirectory = directory.resolve("MeowClient"), png = clientDirectory.resolve("cape.png"), jpg = clientDirectory.resolve("cape.jpg");
        try {
            buffer.makeCurrent(); FixtureMinecraft mc = allocate(FixtureMinecraft.class); set(Minecraft.class, null, "theMinecraft", mc);
            mc.gameSettings = new GameSettings(); mc.displayWidth = mc.displayHeight = SIZE;
            mc.textures = new TextureManager(new SimpleReloadableResourceManager(new IMetadataSerializer()));
            set(Minecraft.class, mc, "mcDataDir", directory.toFile());
            Player player = allocate(Player.class); player.height = 1.8f; mc.thePlayer = player;
            RenderManager manager = allocate(RenderManager.class); manager.renderEngine = mc.textures; mc.manager = manager;
            OpenGlHelper.initializeTextures(); MeowClient.modManager = new ModManager();
            CapeMod cape = new CapeMod(); cape.enable();
            ResourceLocation location = player.getLocationCape();
            require(location != null && "meowclient".equals(location.getResourceDomain()), "default Meow cape had no texture");
            ITextureObject first = mc.textures.getTexture(location); int firstId = first.getGlTextureId();
            int firstPixel = ((DynamicTexture)first).getTextureData()[65];
            for (int frame = 0; frame < 300; frame++) require(location.equals(player.getLocationCape()), "default cape changed its texture key");
            require(first == mc.textures.getTexture(location), "frames recreated the default dynamic texture");
            RecordingPlayerRenderer renderer = new RecordingPlayerRenderer(manager);
            LayerCape layer = new LayerCape(renderer); camera(); player.lookups = 0;
            layer.doRenderLayer(player, 0, 0, 1, 0, 0, 0, 0.0625f);
            require(!player.hasPlayerInfo() && location.equals(renderer.bound) && player.lookups == 1,
                    "local cape layer required server texture info or fetched its texture twice");
            require(coloredPixels() > 1000, "default Meow cape was not drawn by the actual model");
            if (args.length > 0) saveFrame(Paths.get(args[0]).resolve("meow-cape.png"));
            System.out.println("PASS default Meow cape renders without server cape info and reuses its texture");

            cape.capeColor.setValue(new Color(104, 193, 225).getRGB()); cape.update();
            require(location.equals(player.getLocationCape()) && ((DynamicTexture)mc.textures.getTexture(location)).getTextureData()[65] != firstPixel,
                    "cape color setting did not change the generated image");
            require(!GL11.glIsTexture(firstId), "changing the cape color leaked its previous GPU texture");
            System.out.println("PASS Meow color changes replace and release the old GPU texture");

            Files.createDirectories(clientDirectory);
            BufferedImage image = new BufferedImage(22, 17, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, 0xffc071a4);
            ImageIO.write(image, "png", png.toFile()); cape.disable(); cape.enable();
            ResourceLocation local = player.getLocationCape();
            require(local != null && !local.equals(location) && mc.textures.getTexture(local) instanceof DynamicTexture, "local cape PNG was not selected");
            DynamicTexture localTexture = (DynamicTexture)mc.textures.getTexture(local);
            require(localTexture.getTextureData().length == 64 * 32 && localTexture.getTextureData()[65] == 0xffc071a4,
                    "local cape UV normalization changed its pixels");
            int localId = localTexture.getGlTextureId(); cape.disable(); cape.enable(); player.getLocationCape();
            require(!GL11.glIsTexture(localId), "reloading a local cape leaked its previous GPU texture");
            camera(); layer.doRenderLayer(player, 0, 0, 1, 0, 0, 0, 0.0625f);
            require(coloredPixels() > 1000 && local.equals(renderer.bound), "local cape failed to draw through the actual cape layer");
            System.out.println("PASS local cape PNG is padded to the vanilla atlas and renders through LayerCape");

            Files.delete(png);
            BufferedImage rgb = new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) rgb.setRGB(x, y, 0xff406fc9);
            require(ImageIO.write(rgb, "jpg", jpg.toFile()), "JPEG encoder was not available");
            cape.disable(); cape.enable(); require(local.equals(player.getLocationCape()), "JPEG local cape was not loaded");
            int jpegColor = ((DynamicTexture)mc.textures.getTexture(local)).getTextureData()[65];
            require((jpegColor & 255) > (jpegColor >> 16 & 255), "JPEG local cape did not keep its blue color");
            Files.delete(jpg); cape.disable(); cape.enable(); require(location.equals(player.getLocationCape()), "missing local image did not restore the default Meow cape");
            cape.disable(); require(player.getLocationCape() == null, "disabled custom cape did not restore the absent original cape");
            System.out.println("PASS JPEG fallback, missing local image and disabling the module restore the expected cape");
            System.out.println("R7 cape rendering checks passed");
        } finally {
            Files.deleteIfExists(png); Files.deleteIfExists(jpg); Files.deleteIfExists(clientDirectory); Files.deleteIfExists(directory);
            buffer.destroy();
        }
    }
    private static void camera() {
        GlStateManager.viewport(0, 0, SIZE, SIZE); GlStateManager.clearColor(0, 0, 0, 0); GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity(); GL11.glOrtho(-0.8, 0.8, 1.2, -0.2, -5, 5);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity(); GlStateManager.disableCull(); GlStateManager.disableLighting(); GlStateManager.disableDepth();
        GlStateManager.enableTexture2D(); GlStateManager.enableAlpha(); GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f); GlStateManager.color(1, 1, 1, 1);
    }
    private static ByteBuffer pixels() { ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4); GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels); return pixels; }
    private static int coloredPixels() { ByteBuffer pixels = pixels(); int count = 0; for (int i = 0; i < SIZE * SIZE; i++) if (pixels.get(i * 4) != 0 || pixels.get(i * 4 + 1) != 0 || pixels.get(i * 4 + 2) != 0) count++; return count; }
    private static void saveFrame(Path file) throws Exception {
        ByteBuffer pixels = pixels(); BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) { int i = (y * SIZE + x) * 4; image.setRGB(x, SIZE - y - 1, 0xff000000 | (pixels.get(i) & 255) << 16 | (pixels.get(i + 1) & 255) << 8 | pixels.get(i + 2) & 255); }
        Files.createDirectories(file.getParent()); ImageIO.write(image, "png", file.toFile());
    }
    private static class Player extends EntityPlayerSP {
        int lookups; Player() { super(null, null, null, null); }
        @Override protected NetworkPlayerInfo getPlayerInfo() { return null; }
        @Override public boolean hasPlayerInfo() { return false; }
        @Override public boolean isWearing(EnumPlayerModelParts part) { return true; }
        @Override public boolean isInvisible() { return false; }
        @Override public boolean isSneaking() { return false; }
        @Override public ResourceLocation getLocationCape() { lookups++; return super.getLocationCape(); }
    }
    private static class RecordingPlayerRenderer extends RenderPlayer {
        ResourceLocation bound; RecordingPlayerRenderer(RenderManager manager) { super(manager, false); }
        @Override public void bindTexture(ResourceLocation location) { bound = location; super.bindTexture(location); }
    }
    private static class FixtureMinecraft extends Minecraft {
        TextureManager textures; RenderManager manager; FixtureMinecraft() { super(null); }
        @Override public TextureManager getTextureManager() { return textures; }
        @Override public RenderManager getRenderManager() { return manager; }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static <T> T allocate(Class<T> type) throws Exception { return type.cast(UNSAFE.allocateInstance(type)); }
    private static void set(Class<?> type, Object instance, String name, Object value) throws Exception { Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value); }
    private static Unsafe unsafe() { try { Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true); return (Unsafe)field.get(null); } catch (Exception failure) { throw new RuntimeException(failure); } }
}
