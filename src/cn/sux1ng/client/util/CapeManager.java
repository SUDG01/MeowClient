package cn.sux1ng.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;

public class CapeManager {

    // 存我们本地披风的 ResourceLocation
    public static ResourceLocation localCape = null;

    // 开关状态 (由你的 CapeMod 控制)
    public static boolean ENABLED = true;
    public static boolean OPTIFINE_MODE = true; // 是否显示 OptiFine 披风

    // 1. 加载本地披风 (放在 Config 同级目录)
    public static void loadLocalCape() {
        try {
            // 假设你的客户端名字叫 MeowClient
            // 路径大概是: .minecraft/MeowClient/cape.png
            File clientDir = new File(Minecraft.getMinecraft().mcDataDir, "MeowClient");
            if (!clientDir.exists()) clientDir.mkdirs();

            File capeFile = new File(clientDir, "cape.png");
            if (!capeFile.exists()) {
                capeFile = new File(clientDir, "cape.jpg"); // 试试 jpg
            }

            if (capeFile.exists()) {
                System.out.println("🔥 发现本地披风: " + capeFile.getAbsolutePath());
                BufferedImage image = ImageIO.read(new FileInputStream(capeFile));

                // 将图片注册进 Minecraft 的纹理管理器
                DynamicTexture texture = new DynamicTexture(image);
                localCape = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("meowclient_cape", texture);
            } else {
                localCape = null; // 没找到文件
            }
        } catch (Exception e) {
            e.printStackTrace();
            localCape = null;
        }
    }

    // 2. 获取 OptiFine 披风的 URL
    public static String getOptiFineCapeUrl(String username) {
        return "http://s.optifine.net/capes/" + username + ".png";
    }
}