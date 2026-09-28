package cn.sux1ng.client.command.commands;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.command.Command;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.util.ClientUtil; // 记得导入刚才写的 ClientUtil
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

public class BindCommand extends Command {

    public BindCommand() {
        // 注意：这里要看你的 Command 构造函数是怎么写的
        // 如果你的 Command(String name) {...}，那就用 super("bind");
        // 如果是 screenshot 里的 super(new String[]{"bind"}); 那就保持你截图里的样子
        super(new String[]{"bind","b"});
    }

    @Override
    public void run(String[] args) { // 【修改】把 boolean 改成 void
        // 用户输入：.bind killaura r

        if (args.length < 2) {
            ClientUtil.sendClientMessage(ClientLanguage.ui("Usage") + ": .bind <mod> <key>");
            return; // 【修改】这里直接 return，不要 return false
        }

        String modName = args[0];
        String keyName = args[1];

        // 使用你现有的 getByName 方法
        Mod mod = MeowClient.modManager.getByName(modName);
        if (mod == null) {
            ClientUtil.sendClientMessage(ClientLanguage.ui("Module not found") + ": " + modName);
            return; // 【修改】直接 return
        }

        // 解析按键
        int keyCode = Keyboard.getKeyIndex(keyName.toUpperCase());

        // 设置按键
        mod.setKey(keyCode);

        // 发送成功提示 (带颜色)
        ClientUtil.sendClientMessage(
                ClientLanguage.ui("Bound") + " " + EnumChatFormatting.AQUA
                        + ClientLanguage.module(mod) + EnumChatFormatting.GRAY + " → "
                        + EnumChatFormatting.RED + Keyboard.getKeyName(keyCode)
        );

        // 如果你有保存配置的方法，可以在这里调用
        // MeowClient.configManager.save();
    }
}
