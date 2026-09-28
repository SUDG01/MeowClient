package cn.sux1ng.client.command.commands;

import cn.sux1ng.client.command.Command;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.util.ClientUtil;
import net.minecraft.util.EnumChatFormatting;

public class HelpCommand extends Command {

    public HelpCommand() {
        super(new String[]{"h", "help"});
    }

    @Override
    public void run(String[] args) {
        // --- 头部装饰 ---
        ClientUtil.sendClientMessage(EnumChatFormatting.LIGHT_PURPLE + "MeowClient "
                + EnumChatFormatting.AQUA + ClientLanguage.ui("Help"));

        // --- 指令列表 ---
        // 格式：.指令 <参数> - 说明
        sendHelp(".help", ClientLanguage.ui("Show help"));
        sendHelp(".enable <mod>", ClientLanguage.ui("Toggle module"));
        sendHelp(".bind <mod> <key>", ClientLanguage.ui("Bind module key"));

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
