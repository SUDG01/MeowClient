package cn.sux1ng.client.mod.mods.world;

import cn.sux1ng.client.events.EventPacket;
import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.TextValue;
import net.minecraft.network.play.server.S02PacketChat;

/**
 * AutoGG 模块
 * 检测游戏结束（胜利）并自动发送 GG 消息
 */
public class AutoGGMod extends Mod {

    // 发消息的模式
    public ModeValue msgMode = new ModeValue("Message", "gg", new String[]{"gg", "GF", "Good Game", "ez", "Custom"});
    // 自定义消息（仅当 msgMode 为 Custom 时才显示和使用）
    public TextValue customMsg = new TextValue("CustomMsg", "gg")
            .setVisibility(() -> msgMode.is("Custom"));
    // 是否自动发送
    public BooleanValue autoSend = new BooleanValue("AutoSend", true);

    public AutoGGMod() {
        super("AutoGG", Category.WORLD);
        addValues(msgMode, customMsg, autoSend);
    }

    /**
     * 监听收包事件，检测游戏结束消息
     */
    @EventTarget
    public void onPacket(EventPacket event) {
        if (!autoSend.getValue()) return;

        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat chatPacket = (S02PacketChat) event.getPacket();
            String message = chatPacket.getChatComponent().getUnformattedText();

            // 常见的游戏结束关键词（支持中文和英文服务器）
            if (message.contains("Victory!") || message.contains("Winner")
                    || message.contains("赢了") || message.contains("胜利")
                    || message.contains("1st Killer") || message.contains("WINNER")
                    || message.contains("You won") || message.contains("Winner:")
                    || message.contains("Game Over") || message.contains("1st Place")) {

                String msg;
                if (msgMode.is("Custom")) {
                    msg = customMsg.getValue();
                } else {
                    msg = msgMode.getValue().toLowerCase();
                }

                mc.thePlayer.sendChatMessage(msg);
            }
        }
    }
}
