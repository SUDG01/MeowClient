package cn.sux1ng.client.config.configs;

import cn.sux1ng.client.config.Config;
import cn.sux1ng.client.ui.ClientLanguage;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Client-wide display settings, kept separate from module configuration. */
public class ClientConfig extends Config {
    public ClientConfig() { super("Client"); }

    @Override
    public void load() {
        try {
            loadFrom(getPath());
        } catch (IOException | RuntimeException failure) {
            ClientLanguage.setChinese(false);
            System.err.println("Client settings load failed: " + failure.getMessage());
        }
    }

    public void loadFrom(Path path) throws IOException {
        ClientLanguage.setChinese(false);
        if (!Files.exists(path)) return;
        JsonElement parsed = new JsonParser().parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
        if (parsed == null || !parsed.isJsonObject()) return;
        JsonObject object = parsed.getAsJsonObject();
        if (object.has("Chinese")) ClientLanguage.setChinese(object.get("Chinese").getAsBoolean());
    }

    @Override
    public void save() {
        try {
            saveTo(getPath());
        } catch (IOException failure) {
            throw new RuntimeException("Could not save client settings", failure);
        }
    }

    public void saveTo(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        JsonObject object = new JsonObject();
        object.addProperty("Chinese", ClientLanguage.isChinese());
        Path temporary = Files.createTempFile(path.getParent(), "Client-", ".tmp");
        try {
            Files.write(temporary, new GsonBuilder().setPrettyPrinting().create()
                    .toJson(object).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
