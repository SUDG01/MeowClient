import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.config.configs.ClientConfig;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.mod.BlatantPolicy;
import cn.sux1ng.client.mod.mods.render.TNTTimerMod;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.HudLayout;
import cn.sux1ng.client.ui.KeyBindings;
import cn.sux1ng.client.util.ProjectilePrediction;
import com.google.gson.JsonObject;
import net.minecraft.util.*;
import java.nio.file.*;
import java.util.Arrays;

/** Fast checks for trajectory collisions, HUD persistence and binding transitions. */
public class R6FeatureRegressionTest {
    public static void main(String[] args) throws Exception {
        require(ProjectilePrediction.bowSpeed(0) == 0 && ProjectilePrediction.bowSpeed(20) == 3
                && ProjectilePrediction.bowSpeed(200) == 3, "bow charge limits are wrong");
        ProjectilePrediction.Environment air = environment(false, false);
        ProjectilePrediction.Result arrow = ProjectilePrediction.simulate(new Vec3(0, 10, 0), new Vec3(0, 0, 3), true, air);
        require(near(arrow.points.get(2).yCoord, 9.95) && near(arrow.points.get(2).zCoord, 5.97), "arrow drag/gravity differ from vanilla");
        ProjectilePrediction.Result water = ProjectilePrediction.simulate(new Vec3(0, 10, 0), new Vec3(0, 0, 1.5), false, environment(true, false));
        require(near(water.points.get(2).zCoord, 2.7) && near(water.points.get(2).yCoord, 9.97), "throwable water drag/gravity differ from vanilla");
        ProjectilePrediction.Result collision = ProjectilePrediction.simulate(new Vec3(0, 10, 0), new Vec3(0, 0, 3), true, environment(false, true));
        require(collision.hit != null && collision.points.get(collision.points.size() - 1).zCoord == 5,
                "trajectory passed through its first collision");
        require(collision.points.size() == 3, "prediction continued after impact");
        System.out.println("PASS projectile charge, air/water physics and collision stopping");

        HudLayout.Bounds base = new HudLayout.Bounds(4, 4, 100, 40);
        HudLayout.place("Logo", base, 640, 480, 245, 210, 1.5f);
        HudLayout.Bounds resized = HudLayout.transform("Logo", base, 1280, 960);
        require(near(resized.x, 565) && near(resized.y, 450) && resized.width == 150,
                "HUD layout did not preserve relative placement after resizing");
        Path directory = Files.createTempDirectory("meow-r6-hud-");
        Path file = directory.resolve("Client.json");
        try {
            ClientLanguage.setChinese(true);
            new ClientConfig().saveTo(file);
            HudLayout.resetAll(); ClientLanguage.setChinese(false);
            new ClientConfig().loadFrom(file);
            require(ClientLanguage.isChinese() && near(HudLayout.scale("Logo"), 1.5), "HUD/Chinese did not survive saving");
            JsonObject bad = HudLayout.serialize(); bad.addProperty("InfoHUD", "malformed");
            HudLayout.restore(bad);
            require(near(HudLayout.scale("Logo"), 1.5), "a malformed widget discarded another widget");
            HudLayout.place("Logo", base, 640, 480, 9999, -500, 99);
            HudLayout.Bounds clamped = HudLayout.transform("Logo", base, 640, 480);
            require(clamped.x == 440 && clamped.y == 0 && clamped.width == 200, "HUD drag/scale escaped screen bounds");
        } finally {
            Files.deleteIfExists(file); Files.deleteIfExists(directory); HudLayout.resetAll(); ClientLanguage.setChinese(false);
        }
        System.out.println("PASS HUD layout file round trip, resolution change and bounds");

        ModManager manager = new ModManager();
        Mod first = new Mod("First", Category.MISC), second = new Mod("Second", Category.MISC);
        manager.getMods().addAll(Arrays.asList(first, second));
        require(KeyBindings.conflicts(first, manager.getMods()).isEmpty(), "unbound modules were marked conflicting");
        first.setKey(42); second.setKey(42);
        require(KeyBindings.conflicts(first, manager.getMods()).equals(Arrays.asList(second)), "binding collision was not found");
        first.setBindMode(Mod.BindMode.HOLD);
        manager.onKey(42);
        manager.setBinding(first, 43, Mod.BindMode.HOLD);
        require(!first.isEnable(), "rebound Hold module remained active");
        manager.onKey(43); require(first.isEnable(), "new Hold key was not active");
        manager.onKeyRelease(43); require(!first.isEnable(), "new Hold key did not release");
        manager.setBinding(first, 44, Mod.BindMode.SMART);
        manager.onKey(44);
        manager.setBinding(first, 45, Mod.BindMode.TOGGLE);
        require(!first.isEnable(), "rebinding Smart retained temporary activation");
        System.out.println("PASS binding conflicts and Hold/Smart rebinding cleanup");

        MeowClient.modManager = new ModManager(); MeowClient.modManager.load();
        for (String name : new String[]{"Trajectories", "TNTTimer", "DamageIndicator"}) {
            Mod mod = MeowClient.modManager.getByName(name);
            require(mod != null && !BlatantPolicy.requiresBlatant(mod), name + " is missing or restricted");
            mod.setEnable(true); require(mod.isEnable(), name + " is blocked by Blatant"); mod.setEnable(false);
        }
        require("4.0s".equals(TNTTimerMod.label(80, 0)) && "0.0s".equals(TNTTimerMod.label(0, 1)), "TNT labels are wrong");
        System.out.println("PASS new visual modules work without Blatant and TNT labels stay nonnegative");
        cn.sux1ng.client.mod.mods.draw.LogoMod logo = MeowClient.modManager.getByClass(cn.sux1ng.client.mod.mods.draw.LogoMod.class);
        HudLayout.place("Logo", base, 640, 480, 50, 60, 1);
        logo.x.setValue(logo.x.getValue() + 10);
        require(near(HudLayout.transform("Logo", base, 640, 480).x, 60), "HUD editor disabled the existing X/Y controls");
        JsonObject layout = HudLayout.serialize(); HudLayout.resetAll(); HudLayout.restore(layout);
        require(near(HudLayout.transform("Logo", base, 640, 480).x, 60), "saved layout lost the X/Y adjustment");
        HudLayout.resetAll();
        System.out.println("PASS existing position settings remain active after HUD editing");
        System.out.println("R6 feature checks passed");
    }
    private static boolean near(double a, double b) { return Math.abs(a - b) < 0.0001; }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static ProjectilePrediction.Environment environment(boolean water, boolean collide) {
        return new ProjectilePrediction.Environment() {
            public boolean isWater(Vec3 position) { return water; }
            public boolean isLoaded(Vec3 position) { return position.zCoord < 100; }
            public MovingObjectPosition trace(Vec3 from, Vec3 to) {
                if (!collide || from.zCoord > 5 || to.zCoord < 5) return null;
                double t = (5 - from.zCoord) / (to.zCoord - from.zCoord);
                return new MovingObjectPosition(new Vec3(from.xCoord + (to.xCoord - from.xCoord) * t,
                        from.yCoord + (to.yCoord - from.yCoord) * t, 5), EnumFacing.NORTH, new BlockPos(0, 9, 5));
            }
        };
    }
}
