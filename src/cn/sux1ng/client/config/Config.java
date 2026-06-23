package cn.sux1ng.client.config;

import cn.sux1ng.client.MeowClient;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    private final String name;

    public Config(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Path getPath(){
        return Paths.get(Minecraft.getMinecraft().mcDataDir.getAbsolutePath(), MeowClient.NAME,"config",name + ".json");
    }

    public void load(){

    }

    public void save(){

    }
}
