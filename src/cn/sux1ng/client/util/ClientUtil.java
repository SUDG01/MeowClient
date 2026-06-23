package cn.sux1ng.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class ClientUtil {

    /**
     * 发送一条只有客户端能看见的消息
     * @param message 消息内容
     */
    public static void sendClientMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.BLUE + "[Meow] " + EnumChatFormatting.GRAY + message
            ));
        }
    }
}