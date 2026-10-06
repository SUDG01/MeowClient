import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.events.Event;
import cn.sux1ng.client.events.EventManager;
import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.PacketReceiveEvent;
import cn.sux1ng.client.events.impl.PacketSendEvent;
import cn.sux1ng.client.config.configs.ModConfig;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.BlatantPolicy;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.mod.mods.combat.AutoClickerMod;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.mod.mods.misc.BlatantMod;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import cn.sux1ng.client.value.TextValue;
import cn.sux1ng.client.value.ValueGroup;
import com.google.gson.JsonObject;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/** Run with assertions enabled; does not start Minecraft or require JUnit. */
public class R5RegressionTest {
    private static int failures;

    public static void main(String[] args) {
        check("module enable without a world", () -> {
            TestMod mod = new TestMod(false);
            mod.setEnable(true);
            require(mod.isEnable(), "module was not enabled");
        });
        check("failed enable rolls back state", () -> {
            TestMod mod = new TestMod(true);
            try {
                mod.setEnable(true);
                throw new AssertionError("enable should have thrown");
            } catch (IllegalStateException expected) {
                require(!mod.isEnable(), "failed module remained enabled");
            }
        });
        check("number settings respect range and increment", () -> {
            NumberValue number = new NumberValue("Steps", 1, 1, 5, 0.5);
            number.setValue(2.6);
            require(number.getValue() == 2.5, "expected 2.5, got " + number.getValue());
            number.setValue(100.0);
            require(number.getValue() == 5.0, "value escaped maximum");
        });
        check("mode settings reject unknown values", () -> {
            ModeValue mode = new ModeValue("Mode", "A", new String[]{"A", "B"});
            mode.setValue("missing");
            require("A".equals(mode.getValue()), "unknown mode was accepted");
        });
        check("legacy RGB settings update displayed color", () -> {
            ColorValue color = new ColorValue("Color", Color.RED);
            color.setValue(Color.BLUE.getRGB());
            require(color.getRGB() == Color.BLUE.getRGB(), "RGB and HSB state disagree");
        });
        check("event listener registration is idempotent", () -> {
            Listener listener = new Listener();
            EventManager.register(listener);
            EventManager.register(listener);
            EventManager.call(new TestEvent());
            EventManager.unregister(listener);
            require(listener.calls == 1, "listener ran " + listener.calls + " times");
        });
        check("packet events expose replacement and cancellation", () -> {
            Packet<?> original = new C03PacketPlayer();
            Packet<?> replacement = new C03PacketPlayer();
            PacketSendEvent sent = new PacketSendEvent(original);
            PacketReceiveEvent received = new PacketReceiveEvent(original);
            sent.setPacket(replacement);
            received.setPacket(replacement);
            sent.setCancelled(true);
            received.setCancelled(true);
            require(sent.getPacket() == replacement && sent.isCancelled(), "send event lost changes");
            require(received.getPacket() == replacement && received.isCancelled(), "receive event lost changes");
        });
        check("config restores settings before activation", () -> {
            ConfigTestMod mod = new ConfigTestMod("ConfigTest");
            JsonObject settings = new JsonObject();
            settings.addProperty("Power", 3.0);
            settings.addProperty("Label", "ready");
            JsonObject group = new JsonObject();
            group.addProperty("Grouped", true);
            settings.add("Extra", group);
            JsonObject entry = new JsonObject();
            entry.addProperty("enable", true);
            entry.addProperty("key", 17);
            entry.addProperty("bindMode", "SMART");
            entry.add("values", settings);
            JsonObject root = new JsonObject();
            root.add(mod.getName(), entry);

            ModConfig config = new ModConfig();
            config.restoreSettings(root, Arrays.asList(mod));
            require(!mod.isEnable(), "module activated before the world was ready");
            require(mod.power.getValue() == 3.0, "number was not restored");
            require("ready".equals(mod.label.getValue()), "text was not restored");
            require(mod.grouped.getValue(), "grouped value was not restored");
            require(mod.getKey() == 17 && mod.getBindMode() == Mod.BindMode.SMART, "bind was not restored");
            JsonObject saved = config.serializeModules(Arrays.asList(mod)).getAsJsonObject(mod.getName());
            require(saved.get("enable").getAsBoolean(), "pending enabled state was not saved");
            require("SMART".equals(saved.get("bindMode").getAsString()), "bind mode was not saved");
            require(saved.getAsJsonObject("values").getAsJsonObject("Extra")
                    .get("Grouped").getAsBoolean(), "grouped value was not saved");
            config.activatePendingModules();
            require(mod.isEnable() && mod.powerAtEnable == 3.0, "module saw default settings on enable");
            config.activatePendingModules();
            require(mod.enableCount == 1, "module activated more than once");
        });
        check("bad module config does not block later modules", () -> {
            ConfigTestMod first = new ConfigTestMod("Broken");
            ConfigTestMod second = new ConfigTestMod("Good");
            JsonObject root = new JsonObject();
            root.addProperty("Broken", "wrong shape");
            JsonObject good = new JsonObject();
            good.addProperty("enable", true);
            root.add("Good", good);
            ModConfig config = new ModConfig();
            config.restoreSettings(root, Arrays.asList(first, second));
            config.activatePendingModules();
            require(second.isEnable(), "valid module after malformed entry was skipped");
        });
        check("bad key does not hide valid settings in the same module", () -> {
            ConfigTestMod mod = new ConfigTestMod("Partial");
            JsonObject values = new JsonObject();
            values.addProperty("Power", 4.0);
            JsonObject entry = new JsonObject();
            entry.addProperty("key", "invalid");
            entry.addProperty("enable", true);
            entry.add("values", values);
            JsonObject root = new JsonObject();
            root.add("Partial", entry);
            ModConfig config = new ModConfig();
            config.restoreSettings(root, Arrays.asList(mod));
            config.activatePendingModules();
            require(mod.isEnable() && mod.power.getValue() == 4.0,
                    "valid settings were skipped after a bad key");
        });
        check("module config survives a file round trip", () -> {
            Path directory = null;
            Path file = null;
            try {
                directory = Files.createTempDirectory("meow-r5-");
                file = directory.resolve("Mod.json");
                ConfigTestMod original = new ConfigTestMod("RoundTrip");
                original.power.setValue(4.0);
                original.label.setValue("saved text");
                original.grouped.setValue(true);
                original.setBindMode(Mod.BindMode.SMART);
                original.setEnable(true);
                new ModConfig().saveTo(file, Arrays.asList(original));
                original.label.setValue("saved text again");
                new ModConfig().saveTo(file, Arrays.asList(original));

                ConfigTestMod restored = new ConfigTestMod("RoundTrip");
                ModConfig loaded = new ModConfig();
                loaded.loadFrom(file, Arrays.asList(restored));
                require(!restored.isEnable(), "file load activated before world entry");
                loaded.activatePendingModules();
                require(restored.isEnable() && restored.power.getValue() == 4.0, "enabled value did not round trip");
                require("saved text again".equals(restored.label.getValue()) && restored.grouped.getValue(),
                        "text or grouped value did not round trip");
                require(restored.getBindMode() == Mod.BindMode.SMART, "bind mode did not round trip");
            } catch (IOException failure) {
                throw new RuntimeException(failure);
            } finally {
                try {
                    if (file != null) Files.deleteIfExists(file);
                    if (directory != null) Files.deleteIfExists(directory);
                } catch (IOException cleanupFailure) {
                    throw new RuntimeException(cleanupFailure);
                }
            }
        });
        check("hold bindings do not persist temporary activation", () -> {
            ConfigTestMod mod = new ConfigTestMod("Held");
            mod.setBindMode(Mod.BindMode.HOLD);
            mod.setEnable(true);
            ModConfig config = new ModConfig();
            JsonObject saved = config.serializeModules(Arrays.asList(mod));
            require(!saved.getAsJsonObject("Held").get("enable").getAsBoolean(),
                    "held module was saved as permanently enabled");
            JsonObject legacy = new JsonObject();
            JsonObject entry = new JsonObject();
            entry.addProperty("enable", true);
            entry.addProperty("bindMode", "HOLD");
            legacy.add("Held", entry);
            ConfigTestMod restored = new ConfigTestMod("Held");
            config.restoreSettings(legacy, Arrays.asList(restored));
            config.activatePendingModules();
            require(!restored.isEnable(), "held module reactivated without a key press");
        });
        check("smart binding distinguishes tap from hold", () -> {
            AtomicLong clock = new AtomicLong(1000);
            ModManager manager = new ModManager(clock::get);
            TestMod mod = new TestMod(false);
            mod.setKey(42);
            mod.setBindMode(Mod.BindMode.SMART);
            manager.getMods().add(mod);
            manager.onKey(42);
            clock.addAndGet(100);
            manager.onKeyRelease(42);
            require(mod.isEnable(), "short tap should toggle on");
            manager.onKey(42);
            clock.addAndGet(100);
            manager.onKeyRelease(42);
            require(!mod.isEnable(), "second short tap should toggle off");
            manager.onKey(42);
            clock.addAndGet(300);
            require(mod.isEnable(), "long hold should remain enabled while pressed");
            manager.onKeyRelease(42);
            require(!mod.isEnable(), "long hold should restore prior state");
        });
        check("smart binding does not repeat one-shot modules", () -> {
            AtomicLong clock = new AtomicLong(1000);
            ModManager manager = new ModManager(clock::get);
            OneShotMod mod = new OneShotMod();
            mod.setKey(43);
            mod.setBindMode(Mod.BindMode.SMART);
            manager.getMods().add(mod);
            manager.onKey(43);
            clock.addAndGet(100);
            manager.onKeyRelease(43);
            require(mod.activations == 1 && !mod.isEnable(), "one press activated a one-shot module twice");
        });
        check("Blatant gates gameplay modules but keeps visuals available", () -> {
            ModManager manager = new ModManager();
            MeowClient.modManager = manager;
            manager.load();
            BlatantMod blatant = manager.getByClass(BlatantMod.class);
            require(blatant != null && blatant.getCategory() == Category.MISC && !blatant.isEnable(),
                    "Blatant should start disabled in Misc");
            String[] restricted = {"KillAura", "Aimbot", "AutoClicker", "NoClickDelay", "Speed", "NoSlow", "Disabler",
                    "NoJumpDelay", "Eagle", "FastPlace", "AutoTool", "Derp", "SkinDerp", "Twerk"};
            for (String name : restricted) {
                Mod mod = manager.getByName(name);
                require(mod != null && BlatantPolicy.requiresBlatant(mod), name + " was not classified");
                mod.setEnable(true);
                require(!mod.isEnable(), name + " enabled while Blatant was off");
            }
            for (String name : new String[]{"ESP", "Animations", "CustomCape", "ArrayList",
                    "ClickSound", "Sprint", "AutoGG", "TimeChanger"}) {
                Mod mod = manager.getByName(name);
                require(mod != null && !BlatantPolicy.requiresBlatant(mod), name + " was incorrectly restricted");
            }
            Mod visual = manager.getByName("ESP");
            visual.setEnable(true);
            require(visual.isEnable(), "visual module could not be enabled");
            blatant.setEnable(true);
            Mod derp = manager.getByName("Derp");
            Mod skinDerp = manager.getByName("SkinDerp");
            Mod aimbot = manager.getByName("Aimbot");
            derp.setEnable(true);
            skinDerp.setEnable(true);
            aimbot.setEnable(true);
            require(derp.isEnable() && skinDerp.isEnable() && aimbot.isEnable(), "Blatant did not unlock gameplay modules");
            blatant.setEnable(false);
            require(!derp.isEnable() && !skinDerp.isEnable() && !aimbot.isEnable(), "restricted modules stayed on after Blatant closed");
            require(visual.isEnable(), "closing Blatant disabled a visual module");
        });
        check("config enables Blatant before restoring restricted modules", () -> {
            ModManager manager = new ModManager();
            MeowClient.modManager = manager;
            manager.load();
            JsonObject root = new JsonObject();
            JsonObject enabled = new JsonObject();
            enabled.addProperty("enable", true);
            root.add("Blatant", enabled);
            root.add("Derp", enabled);
            root.add("Aimbot", enabled);
            ModConfig config = new ModConfig();
            config.restoreSettings(root, manager.getMods());
            config.activatePendingModules();
            require(manager.getByName("Blatant").isEnable() && manager.getByName("Derp").isEnable() && manager.getByName("Aimbot").isEnable(),
                    "saved Blatant permission was not applied first");
            manager.getByName("Blatant").setEnable(false);
        });
        check("config cannot restore restricted modules while Blatant is off", () -> {
            ModManager manager = new ModManager();
            MeowClient.modManager = manager;
            manager.load();
            JsonObject root = new JsonObject();
            JsonObject enabled = new JsonObject();
            enabled.addProperty("enable", true);
            root.add("Derp", enabled);
            root.add("Aimbot", enabled);
            ModConfig config = new ModConfig();
            config.restoreSettings(root, manager.getMods());
            config.activatePendingModules();
            require(!manager.getByName("Derp").isEnable() && !manager.getByName("Aimbot").isEnable(),
                    "restricted module bypassed Blatant through config load");
        });
        check("old AutoClicker Blatant setting migrates to BlockHit", () -> {
            AutoClickerMod autoClicker = new AutoClickerMod();
            JsonObject settings = new JsonObject();
            settings.addProperty("Blatant", true);
            JsonObject entry = new JsonObject();
            entry.add("values", settings);
            JsonObject root = new JsonObject();
            root.add("AutoClicker", entry);
            ModConfig config = new ModConfig();
            config.restoreSettings(root, Arrays.asList(autoClicker));
            require("BlockHit".equals(autoClicker.blockHit.getName()) && autoClicker.blockHit.getValue(),
                    "old block-hit setting was not restored");
            require(config.serializeModules(Arrays.asList(autoClicker)).getAsJsonObject("AutoClicker")
                    .getAsJsonObject("values").has("BlockHit"), "new block-hit name was not saved");
        });
        check("KillAura can be closed after the player leaves a world", () -> {
            ModManager manager = new ModManager();
            MeowClient.modManager = manager;
            manager.load();
            manager.getByClass(BlatantMod.class).setEnable(true);
            KillAuraMod killAura = manager.getByClass(KillAuraMod.class);
            killAura.setEnable(true);
            killAura.setEnable(false);
            require(!killAura.isEnable(), "KillAura stayed enabled");
        });
        if (failures != 0) throw new AssertionError(failures + " R5 regression checks failed");
        System.out.println("R5 regression checks passed");
    }

    private static void check(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            failures++;
            System.err.println("FAIL " + name + ": " + failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class TestMod extends Mod {
        private final boolean fail;

        TestMod(boolean fail) {
            super("R5Test", Category.MISC);
            this.fail = fail;
        }

        @Override
        public void enable() {
            if (fail) throw new IllegalStateException("intentional failure");
        }
    }

    private static class TestEvent extends Event {}

    private static class OneShotMod extends Mod {
        int activations;

        OneShotMod() { super("OneShot", Category.MISC); }

        @Override
        public void enable() {
            activations++;
            setEnable(false);
        }
    }

    private static class ConfigTestMod extends Mod {
        final NumberValue power = new NumberValue("Power", 1, 1, 5, 0.5);
        final TextValue label = new TextValue("Label", "default");
        final cn.sux1ng.client.value.BooleanValue grouped =
                new cn.sux1ng.client.value.BooleanValue("Grouped", false);
        double powerAtEnable;
        int enableCount;

        ConfigTestMod(String name) {
            super(name, Category.MISC);
            addValues(power, label, new ValueGroup("Extra", grouped));
        }

        @Override
        public void enable() {
            powerAtEnable = power.getValue();
            enableCount++;
        }
    }

    private static class Listener {
        private int calls;

        @EventTarget
        public void onTest(TestEvent event) {
            calls++;
        }
    }
}
