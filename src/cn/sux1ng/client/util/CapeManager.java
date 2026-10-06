package cn.sux1ng.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import java.util.regex.Pattern;

/** Resolves styles on the render thread; texture lookups share local and remote resources. */
public final class CapeManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation MEOW = new ResourceLocation("meowclient", "capes/meow");
    private static final ResourceLocation LOCAL = new ResourceLocation("meowclient", "capes/local");
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    public static ResourceLocation localCape;
    public static boolean ENABLED;
    public static boolean OPTIFINE_MODE;
    private static String style = "Meow";
    private static int color = 0xffff69b4, renderedColor;
    private static TextureManager meowTextures;

    private CapeManager() {}
    public static void configure(String selectedStyle, boolean showOthers, int argb) {
        style = selectedStyle; OPTIFINE_MODE = showOthers; color = argb;
    }
    public static ResourceLocation resolve(AbstractClientPlayer player, NetworkPlayerInfo info) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ENABLED || mc == null || mc.thePlayer == null) return original(info);
        boolean self = player == mc.thePlayer || mc.thePlayer.getUniqueID().equals(player.getUniqueID());
        if (self && "Meow".equals(style)) return localCape != null ? localCape : getMeowCape();
        if (self && "Minecon".equals(style)) return original(info);
        if ((self && "OptiFine".equals(style)) || (!self && OPTIFINE_MODE)) {
            ResourceLocation optifine = info == null ? null : info.getLocationOptifineCape();
            if (optifine != null) return optifine;
        }
        return original(info);
    }
    private static ResourceLocation original(NetworkPlayerInfo info) { return info == null ? null : info.getLocationCape(); }

    public static ResourceLocation getOptiFineCape(String username) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ENABLED || mc == null || mc.getTextureManager() == null || username == null || !USERNAME.matcher(username).matches()) return null;
        String key = username.toLowerCase(Locale.ROOT);
        ResourceLocation location = new ResourceLocation("meowclient", "capes/optifine/" + key);
        TextureManager textures = mc.getTextureManager();
        ITextureObject existing = textures.getTexture(location);
        CapeTexture texture;
        if (existing instanceof CapeTexture) {
            texture = (CapeTexture)existing;
            if (texture.shouldRetry()) textures.loadTexture(location, texture);
        } else {
            File cache = mc.mcDataDir == null ? null : new File(mc.mcDataDir, "MeowClient/cache/capes/" + key + ".png");
            texture = new CapeTexture(username, cache); textures.loadTexture(location, texture);
        }
        return texture.isReady() ? location : null;
    }
    public static String getOptiFineCapeUrl(String username) { return "http://s.optifine.net/capes/" + username + ".png"; }

    public static void loadLocalCape() {
        localCape = null;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.mcDataDir == null || mc.getTextureManager() == null) return;
        File file = new File(mc.mcDataDir, "MeowClient/cape.png");
        if (!file.isFile()) file = new File(mc.mcDataDir, "MeowClient/cape.jpg");
        if (!file.isFile()) return;
        try {
            BufferedImage image = normalizeCape(ImageIO.read(file));
            if (image == null) throw new IOException("unsupported cape image");
            if (replace(mc.getTextureManager(), LOCAL, image)) localCape = LOCAL;
        } catch (IOException | IllegalArgumentException failure) {
            LOGGER.warn("Local cape unavailable: {}", failure.getMessage());
        }
    }
    private static ResourceLocation getMeowCape() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getTextureManager() == null) return null;
        TextureManager textures = mc.getTextureManager();
        if (meowTextures != textures || renderedColor != color || textures.getTexture(MEOW) == null) {
            if (!replace(textures, MEOW, createMeowCape(color))) return null;
            meowTextures = textures; renderedColor = color;
        }
        return MEOW;
    }
    private static boolean replace(TextureManager textures, ResourceLocation location, BufferedImage image) {
        DynamicTexture replacement = new DynamicTexture(image);
        ITextureObject previous = textures.getTexture(location);
        if (previous instanceof AbstractTexture) ((AbstractTexture)previous).deleteGlTexture();
        return textures.loadTexture(location, replacement);
    }

    /** Preserve the cape UV atlas and pad common 22x17 OptiFine images to 64x32. */
    public static BufferedImage normalizeCape(BufferedImage image) {
        if (image == null || image.getWidth() < 1 || image.getHeight() < 1 || image.getWidth() > 1024 || image.getHeight() > 512) return null;
        int width = 64, height = 32;
        while (width < image.getWidth() || height < image.getHeight()) { width *= 2; height *= 2; }
        BufferedImage atlas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        try { graphics.drawImage(image, 0, 0, null); } finally { graphics.dispose(); }
        return atlas;
    }
    /** A small cat motif with the selected accent, laid out for the vanilla cape model. */
    public static BufferedImage createMeowCape(int argb) {
        BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        Color accent = new Color(argb, true);
        for (int y = 0; y <= 16; y++) for (int x = 0; x <= 21; x++) {
            double blend = 0.15 + y / 16.0 * 0.45;
            int r = (int)(accent.getRed() * (1 - blend) + 54 * blend);
            int g = (int)(accent.getGreen() * (1 - blend) + 43 * blend);
            int b = (int)(accent.getBlue() * (1 - blend) + 74 * blend);
            image.setRGB(x, y, new Color(r, g, b, accent.getAlpha()).getRGB());
        }
        int cream = 0xfffff2e8, ink = 0xff453851;
        for (int face : new int[]{1, 12}) {
            for (int y = 5; y <= 9; y++) for (int x = 2; x <= 7; x++) image.setRGB(face + x, y, cream);
            image.setRGB(face + 2, 3, cream); image.setRGB(face + 2, 4, cream); image.setRGB(face + 3, 4, cream);
            image.setRGB(face + 7, 3, cream); image.setRGB(face + 6, 4, cream); image.setRGB(face + 7, 4, cream);
            image.setRGB(face + 3, 6, ink); image.setRGB(face + 6, 6, ink);
            image.setRGB(face + 2, 7, argb); image.setRGB(face + 7, 7, argb);
            image.setRGB(face + 1, 7, cream); image.setRGB(face + 8, 7, cream);
            image.setRGB(face + 4, 8, ink); image.setRGB(face + 5, 8, ink);
            for (int x = 1; x <= 8; x++) image.setRGB(face + x, 13, cream);
        }
        return image;
    }
}
