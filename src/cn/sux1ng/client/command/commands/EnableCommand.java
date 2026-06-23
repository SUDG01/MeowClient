package cn.sux1ng.client.command.commands;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.command.Command;
import cn.sux1ng.client.mod.Mod;
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
            ClientUtil.sendClientMessage("Usage: .enable <module>");
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
                statusName = "Enabled";
                color = EnumChatFormatting.GREEN;
            } else {
                statusName = "Disabled";
                color = EnumChatFormatting.RED;
            }

            // 发送消息：[Meow] KillAura was Enabled
            ClientUtil.sendClientMessage(String.format("%s%s %swas %s%s",
                    EnumChatFormatting.AQUA, mod.getName(), // 模块名 (蓝色)
                    EnumChatFormatting.GRAY,                // 连接词 (灰色)
                    color, statusName                       // 状态 (红/绿)
            ));

        } else {
            // 模块不存在
            ClientUtil.sendClientMessage("Module '" + EnumChatFormatting.RED + modName + EnumChatFormatting.GRAY + "' not found!");
        }
    }
}
