package cn.sux1ng.client.command;

import cn.sux1ng.client.command.commands.BindCommand;
import cn.sux1ng.client.command.commands.EnableCommand;
import cn.sux1ng.client.command.commands.HelpCommand;
import cn.sux1ng.client.ui.ClientLanguage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.util.*;

public class CommandManager {
    private final Map<String[], Command> commandMap = new HashMap<>();

    public CommandManager(){

    }

    public void load(){
        HelpCommand helpCommand = new HelpCommand();
        commandMap.put(helpCommand.getKey(),helpCommand);
        EnableCommand enableCommand = new EnableCommand();
        commandMap.put(enableCommand.getKey(),enableCommand);
        BindCommand bindCommand = new BindCommand();
        commandMap.put(bindCommand.getKey(),bindCommand);
    }

    public boolean run(String message){
        if (message == null || message.isEmpty()) return false;
        if ('.' == message.charAt(0)) {
            String substring = message.substring(1);
            String[] s = substring.split(" ");
            String key = s[0];
            Command command = getCommand(key);
            if (command != null) {
                List<String> args = new ArrayList<>();
                Collections.addAll(args,s);
                args.remove(0);
                command.run(args.toArray(new String[0]));
            } else {
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(
                        "§c" + ClientLanguage.ui("Unknown command") + ": " + key));
            }
            return true;
        }
        return false;
    }

    public Command getCommand(String key){
        for (Map.Entry<String[], Command> commandEntry : commandMap.entrySet()) {
            for (String k : commandEntry.getKey()) {
                if (k.equals(key)) {
                    return commandEntry.getValue();
                }
            }
        }
        return null;
    }
}

