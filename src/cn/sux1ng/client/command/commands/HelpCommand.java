package cn.sux1ng.client.command.commands;

import cn.sux1ng.client.command.Command;
import cn.sux1ng.client.util.ClientUtil;
import net.minecraft.util.EnumChatFormatting;

public class HelpCommand extends Command {

    public HelpCommand() {
        super(new String[]{"h", "help"});
    }

    @Override
    public void run(String[] args) {
        // --- 头部装饰 ---
        ClientUtil.sendClientMessage(EnumChatFormatting.LIGHT_PURPLE + "====== " + EnumChatFormatting.AQUA + "MeowClient Help" + EnumChatFormatting.LIGHT_PURPLE + " ======");

        // --- 指令列表 ---
        // 格式：.指令 <参数> - 说明
        sendHelp(".help", "显示帮助列表");
        sendHelp(".enable <mod>", "开关指定功能");
        sendHelp(".bind <mod> <key>", "绑定功能按键");

    }

    // 这是一个小小的辅助方法，专门用来发统一格式的帮助，让代码整齐一点
    private void sendHelp(String syntax, String description) {
        ClientUtil.sendClientMessage(
                EnumChatFormatting.AQUA + syntax +
                        EnumChatFormatting.GRAY + " - " +
                        EnumChatFormatting.WHITE + description
        );
    }
}