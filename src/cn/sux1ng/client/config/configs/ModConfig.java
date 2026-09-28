package cn.sux1ng.client.config.configs;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.config.Config;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import cn.sux1ng.client.value.TextValue;
import cn.sux1ng.client.value.Value;
import cn.sux1ng.client.value.ValueGroup;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Module settings are restored at startup; activation waits for the first player update. */
public class ModConfig extends Config {
    private final Set<Mod> pendingEnabled = new LinkedHashSet<>();

    public ModConfig() {
        super("Mod");
    }

    @Override
    public void load() {
        try {
            loadFrom(getPath(), MeowClient.modManager.getMods());
        } catch (IOException | RuntimeException failure) {
            System.err.println("Config load failed: " + failure.getMessage());
        }
    }

    public void loadFrom(Path path, List<? extends Mod> modules) throws IOException {
        if (!Files.exists(path)) return;
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length == 0) return;
        JsonElement parsed = new JsonParser().parse(new String(bytes, StandardCharsets.UTF_8));
        if (parsed != null && parsed.isJsonObject()) {
            restoreSettings(parsed.getAsJsonObject(), modules);
        }
    }

    /** Restores values first, then records modules that must be enabled after world entry. */
    public void restoreSettings(JsonObject root, List<? extends Mod> modules) {
        pendingEnabled.clear();
        for (Mod mod : modules) {
            try {
                JsonElement entry = root.get(mod.getName());
                if (entry == null || !entry.isJsonObject()) continue;
                JsonObject modJson = entry.getAsJsonObject();

                if (modJson.has("key")) {
                    try {
                        mod.setKey(modJson.get("key").getAsInt());
                    } catch (RuntimeException failure) {
                        System.err.println("Invalid key for " + mod.getName() + ": " + failure.getMessage());
                    }
                }
                if (modJson.has("bindMode")) {
                    try {
                        mod.setBindMode(Mod.BindMode.valueOf(
                                modJson.get("bindMode").getAsString().toUpperCase(Locale.ROOT)));
                    } catch (RuntimeException ignored) {
                        // Keep the module's default binding for an unknown mode.
                    }
                }
                JsonElement values = modJson.get("values");
                if (values != null && values.isJsonObject()) {
                    JsonObject settings = values.getAsJsonObject();
                    for (Value<?> value : mod.getValues()) restoreValue(settings, value);
                }

                if (modJson.has("enable")) {
                    try {
                        if (modJson.get("enable").getAsBoolean() && mod.getBindMode() != Mod.BindMode.HOLD) {
                            if (!mod.isEnable()) pendingEnabled.add(mod);
                        } else if (mod.isEnable()) {
                            mod.setEnable(false);
                        }
                    } catch (RuntimeException failure) {
                        System.err.println("Invalid enabled state for " + mod.getName() + ": " + failure.getMessage());
                    }
                }
            } catch (RuntimeException failure) {
                System.err.println("Config entry for " + mod.getName() + " failed: " + failure.getMessage());
            }
        }
    }

    private void restoreValue(JsonObject settings, Value<?> value) {
        if (value instanceof ValueGroup) {
            JsonElement group = settings.get(value.getName());
            JsonObject children = group != null && group.isJsonObject() ? group.getAsJsonObject() : settings;
            for (Value<?> child : ((ValueGroup) value).getChildren()) restoreValue(children, child);
            return;
        }
        JsonElement element = settings.get(value.getName());
        if (element == null && "BlockHit".equals(value.getName())) {
            // R4/R5 AutoClicker called this setting "Blatant"; preserve old configs.
            element = settings.get("Blatant");
        }
        if (element == null || element.isJsonNull()) return;
        try {
            if (value instanceof BooleanValue) {
                ((BooleanValue) value).setValue(element.getAsBoolean());
            } else if (value instanceof NumberValue) {
                ((NumberValue) value).setValue(element.getAsDouble());
            } else if (value instanceof ModeValue) {
                ((ModeValue) value).setValue(element.getAsString());
            } else if (value instanceof TextValue) {
                ((TextValue) value).setValue(element.getAsString());
            } else if (value instanceof ColorValue) {
                ColorValue color = (ColorValue) value;
                if (element.isJsonObject()) {
                    JsonObject object = element.getAsJsonObject();
                    if (object.has("hue") && object.has("saturation")
                            && object.has("brightness") && object.has("alpha")) {
                        color.setHue(object.get("hue").getAsFloat());
                        color.setSaturation(object.get("saturation").getAsFloat());
                        color.setBrightness(object.get("brightness").getAsFloat());
                        color.setAlpha(object.get("alpha").getAsInt());
                    } else if (object.has("rgb")) {
                        color.setValue(object.get("rgb").getAsInt());
                    }
                } else {
                    color.setValue(element.getAsInt());
                }
            }
        } catch (RuntimeException failure) {
            System.err.println("Config value " + value.getName() + " failed: " + failure.getMessage());
        }
    }

    /** Called once a player update proves that world and player objects exist. */
    public void activatePendingModules() {
        List<Mod> toEnable = new ArrayList<>(pendingEnabled);
        pendingEnabled.clear();
        for (Mod mod : toEnable) {
            try {
                mod.setEnable(true);
            } catch (RuntimeException failure) {
                System.err.println("Could not enable " + mod.getName() + ": " + failure.getMessage());
            }
        }
    }

    public JsonObject serializeModules(List<? extends Mod> modules) {
        JsonObject root = new JsonObject();
        for (Mod mod : modules) {
            JsonObject entry = new JsonObject();
            entry.addProperty("enable", mod.getBindMode() != Mod.BindMode.HOLD
                    && (mod.isEnable() || pendingEnabled.contains(mod)));
            entry.addProperty("key", mod.getKey());
            entry.addProperty("bindMode", mod.getBindMode().name());
            JsonObject settings = new JsonObject();
            for (Value<?> value : mod.getValues()) saveValue(settings, value);
            if (settings.entrySet().size() > 0) entry.add("values", settings);
            root.add(mod.getName(), entry);
        }
        return root;
    }

    private void saveValue(JsonObject settings, Value<?> value) {
        if (value instanceof ValueGroup) {
            JsonObject children = new JsonObject();
            for (Value<?> child : ((ValueGroup) value).getChildren()) saveValue(children, child);
            settings.add(value.getName(), children);
        } else if (value instanceof BooleanValue) {
            settings.addProperty(value.getName(), ((BooleanValue) value).getValue());
        } else if (value instanceof NumberValue) {
            settings.addProperty(value.getName(), ((NumberValue) value).getValue());
        } else if (value instanceof ModeValue) {
            settings.addProperty(value.getName(), ((ModeValue) value).getValue());
        } else if (value instanceof TextValue) {
            settings.addProperty(value.getName(), ((TextValue) value).getValue());
        } else if (value instanceof ColorValue) {
            ColorValue color = (ColorValue) value;
            JsonObject object = new JsonObject();
            object.addProperty("rgb", color.getRGB());
            object.addProperty("hue", color.getHue());
            object.addProperty("saturation", color.getSaturation());
            object.addProperty("brightness", color.getBrightness());
            object.addProperty("alpha", color.getAlpha());
            settings.add(value.getName(), object);
        }
    }

    @Override
    public void save() {
        try {
            saveTo(getPath(), MeowClient.modManager.getMods());
        } catch (IOException failure) {
            throw new RuntimeException("Could not save module config", failure);
        }
    }

    public void saveTo(Path path, List<? extends Mod> modules) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "Mod-", ".tmp");
        try {
            Files.write(temporary, new GsonBuilder().setPrettyPrinting().create()
                    .toJson(serializeModules(modules)).getBytes(StandardCharsets.UTF_8));
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
