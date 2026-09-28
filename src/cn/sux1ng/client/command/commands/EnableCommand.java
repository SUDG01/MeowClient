package cn.sux1ng.client.command.commands;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.command.Command;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.util.ClientUtil;
import net.minecraft.util.EnumChatFormatting;

public class EnableCommand extends Command {
    public EnableCommand() {
        super(new String[]{"t", "toggle", "enable"});
    }

    @Override
    public void run(String[] args) {
        // 1. 检查参数：如果没有输入模块名
        if (args.length < 1) {
            ClientUtil.sendClientMessage(ClientLanguage.ui("Usage") + ": .enable <mod>");
            return;
        }

        // 2. 查找模块
        String modName = args[0];
        Mod mod = MeowClient.modManager.getByName(modName);

        if (mod != null) {
            mod.setEnable(!mod.isEnable());

            // 3. 准备提示信息
            String statusName;
            EnumChatFormatting color;

            if (mod.isEnable()) {
                statusName = ClientLanguage.ui("Enabled");
                color = EnumChatFormatting.GREEN;
            } else {
                statusName = ClientLanguage.ui("Disabled");
                color = EnumChatFormatting.RED;
            }

            // 发送消息：[Meow] KillAura was Enabled
            ClientUtil.sendClientMessage(EnumChatFormatting.AQUA + ClientLanguage.module(mod)
                    + " " + color + statusName);

        } else {
            // 模块不存在
            ClientUtil.sendClientMessage(ClientLanguage.ui("Module not found") + ": "
                    + EnumChatFormatting.RED + modName);
        }
    }
}
