import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.config.configs.ClientConfig;
import cn.sux1ng.client.gui.SkinAvatar;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Headless checks for the curated client translations and the Chinese setting. */
public class ClientUiRegressionTest {
    public static void main(String[] args) throws IOException {
        ModManager manager = new ModManager();
        MeowClient.modManager = manager;
        manager.load();
        ClientLanguage.setChinese(true);
        for (Category category : Category.values()) {
            require(!ClientLanguage.category(category).equals(category.name()), "category: " + category);
        }
        Set<String> technical = new HashSet<>(Arrays.asList("X", "Y", "Z", "FPS", "BPS", "XYZ"));
        Set<String> properOptions = new HashSet<>(Arrays.asList("NCP", "Grim", "OptiFine", "Minecon", "1.7", "2D"));
        for (Mod mod : manager.getMods()) {
            require(!ClientLanguage.module(mod).equals(mod.getName()), "module: " + mod.getName());
            for (Value<?> value : mod.getValues()) {
                if (!technical.contains(value.getName())) {
                    require(!ClientLanguage.value(value).equals(value.getName()), "setting: " + value.getName());
                }
                if (value instanceof ModeValue) {
                    ModeValue mode = (ModeValue) value;
                    for (String option : mode.getModes()) {
                        if (properOptions.contains(option) || "Message".equals(mode.getName())
                                && !"Custom".equals(option)) continue;
                        require(!ClientLanguage.option(mode.getName(), option).equals(option),
                                "option: " + mode.getName() + "/" + option);
                    }
                }
            }
        }
        require("MeowClient".equals(ClientLanguage.module("MeowClient")), "client name was translated");
        require("客户端设置".equals(ClientLanguage.ui("ClientSetting")),
                "ClientSetting title was not translated");
        require(manager.getByName("自动攻击") == manager.getByName("KillAura"),
                "translated module name cannot be used in commands");
        require(SkinAvatar.hit(20, 20, 20, 20, 16)
                && !SkinAvatar.hit(50, 50, 20, 20, 16), "avatar hit target is wrong");
        require(DrawUtil.blend(0xFF000000, 0xFFFFFFFF, 0) == 0xFF000000
                && DrawUtil.blend(0xFF000000, 0xFFFFFFFF, 1) == 0xFFFFFFFF,
                "theme interpolation changed its endpoints");
        Path directory = Files.createTempDirectory("meow-client-ui-");
        Path file = directory.resolve("Client.json");
        try {
            ClientConfig config = new ClientConfig();
            config.saveTo(file);
            ClientLanguage.setChinese(false);
            config.loadFrom(file);
            require(ClientLanguage.isChinese(), "Chinese setting did not survive a restart");
            ClientLanguage.setChinese(false);
            config.saveTo(file);
            ClientLanguage.setChinese(true);
            config.loadFrom(file);
            require(!ClientLanguage.isChinese(), "English setting did not survive a restart");
            require("KillAura".equals(ClientLanguage.module("KillAura")), "English labels did not return");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
            ClientLanguage.setChinese(false);
        }
        System.out.println("Client UI translation and config checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
