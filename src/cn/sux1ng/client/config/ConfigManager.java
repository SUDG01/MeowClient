package cn.sux1ng.client.config;

import cn.sux1ng.client.config.configs.ClientConfig;
import cn.sux1ng.client.config.configs.ModConfig;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private final ClientConfig clientConfig = new ClientConfig();
    private final ModConfig modConfig = new ModConfig();
    private final List<Config> configs = new ArrayList<Config>(){
        {
            add(clientConfig);
            add(modConfig);
        }
    };

    public void load(){
        for (Config config : configs) {
            config.load();
        }
    }

    public void save(){
        for (Config config : configs) {
            config.save();
        }
    }

    public void saveClientSettings() {
        clientConfig.save();
    }

    public void activatePendingModules() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null && mc.theWorld != null) {
            modConfig.activatePendingModules();
        }
    }
}
