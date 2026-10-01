package cn.sux1ng.client.ui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.gui.HudEditorScreen;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.draw.*;
import cn.sux1ng.client.mod.mods.render.ArmorHUDMod;
import cn.sux1ng.client.mod.mods.render.TargetHUDMod;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.util.RenderState;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import java.util.*;

/** Per-widget positions are normalized to the available screen space and saved in Client.json. */
public final class HudLayout {
    public static final List<String> WIDGETS = Collections.unmodifiableList(Arrays.asList(
            "Logo", "InfoHUD", "ArrayList", "ArmorHUD", "TargetHUD", "Tab"));
    private static final Map<String, Placement> placements = new HashMap<>();
    private HudLayout() {}

    public static final class Bounds {
        public final float x, y, width, height;
        public Bounds(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
        public boolean contains(float px, float py) { return px >= x && px <= x + width && py >= y && py <= y + height; }
    }
    private static final class Placement {
        final float x, y, scale, sourceX, sourceY;
        Placement(float x, float y, float scale, float sourceX, float sourceY) {
            this.x = x; this.y = y; this.scale = scale; this.sourceX = sourceX; this.sourceY = sourceY;
        }
    }
    public static void resetAll() { placements.clear(); }
    public static void reset(String name) { placements.remove(name); }
    public static float scale(String name) { Placement p = placements.get(name); return p == null ? 1 : p.scale; }

    public static Bounds transform(String name, Bounds original, int screenWidth, int screenHeight) {
        Placement p = placements.get(name);
        if (p == null) return original;
        float width = original.width * p.scale, height = original.height * p.scale;
        float[] source = sourcePosition(name);
        float availableX = Math.max(0, screenWidth - width), availableY = Math.max(0, screenHeight - height);
        return new Bounds(Math.max(0, Math.min(availableX, p.x * availableX + (source[0] - p.sourceX) * p.scale)),
                Math.max(0, Math.min(availableY, p.y * availableY + (source[1] - p.sourceY) * p.scale)), width, height);
    }
    public static void place(String name, Bounds original, int screenWidth, int screenHeight, float x, float y, float scale) {
        if (!WIDGETS.contains(name) || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(scale)) return;
        scale = Math.max(0.5f, Math.min(2, scale));
        float availableX = Math.max(0, screenWidth - original.width * scale);
        float availableY = Math.max(0, screenHeight - original.height * scale);
        float[] source = sourcePosition(name);
        placements.put(name, new Placement(availableX == 0 ? 0 : Math.max(0, Math.min(1, x / availableX)),
                availableY == 0 ? 0 : Math.max(0, Math.min(1, y / availableY)), scale, source[0], source[1]));
    }
    private static float[] sourcePosition(String name) {
        Mod mod = MeowClient.modManager == null ? null : MeowClient.modManager.getByName(name);
        if (mod instanceof LogoMod) return new float[]{((LogoMod) mod).x.getValue().floatValue(), ((LogoMod) mod).y.getValue().floatValue()};
        if (mod instanceof InfoHUDMod) return new float[]{((InfoHUDMod) mod).x.getValue().floatValue(), ((InfoHUDMod) mod).y.getValue().floatValue()};
        if (mod instanceof ArmorHUDMod) return new float[]{((ArmorHUDMod) mod).x.getValue().floatValue(), ((ArmorHUDMod) mod).y.getValue().floatValue()};
        if (mod instanceof TargetHUDMod) return new float[]{((TargetHUDMod) mod).x.getValue().floatValue(), ((TargetHUDMod) mod).y.getValue().floatValue()};
        return new float[]{0, 0};
    }
    public static JsonObject serialize() {
        JsonObject object = new JsonObject();
        for (String name : WIDGETS) {
            Placement p = placements.get(name);
            if (p == null) continue;
            JsonObject value = new JsonObject();
            value.addProperty("x", p.x); value.addProperty("y", p.y); value.addProperty("scale", p.scale);
            value.addProperty("sourceX", p.sourceX); value.addProperty("sourceY", p.sourceY);
            object.add(name, value);
        }
        return object;
    }
    public static void restore(JsonObject object) {
        resetAll();
        for (String name : WIDGETS) {
            try {
                if (!object.has(name) || !object.get(name).isJsonObject()) continue;
                JsonObject value = object.getAsJsonObject(name);
                float x = value.get("x").getAsFloat(), y = value.get("y").getAsFloat(), scale = value.get("scale").getAsFloat();
                float[] source = sourcePosition(name);
                float sourceX = value.has("sourceX") ? value.get("sourceX").getAsFloat() : source[0];
                float sourceY = value.has("sourceY") ? value.get("sourceY").getAsFloat() : source[1];
                if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(scale) && Float.isFinite(sourceX) && Float.isFinite(sourceY)) {
                    placements.put(name, new Placement(Math.max(0, Math.min(1, x)), Math.max(0, Math.min(1, y)),
                            Math.max(0.5f, Math.min(2, scale)), sourceX, sourceY));
                }
            } catch (RuntimeException ignored) { /* A malformed widget must not hide other layouts. */ }
        }
    }

    public static Bounds originalBounds(Mod mod, int screenWidth) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRendererObj;
        if (mod instanceof LogoMod) {
            LogoMod logo = (LogoMod) mod;
            float width = font.getStringWidth(MeowClient.NAME + " " + MeowClient.VERSION);
            float height = font.FONT_HEIGHT;
            if (logo.mode.is("Normal")) { width = font.getStringWidth(MeowClient.NAME) + font.getStringWidth(MeowClient.VERSION) + 26; height = 23; }
            if (logo.mode.is("CSGO(Gamesense)")) { width = font.getStringWidth(MeowClient.NAME + " | " + Minecraft.getDebugFPS() + " FPS | 00:00:00") + 14; height = 20; }
            return new Bounds(logo.x.getValue().floatValue(), logo.y.getValue().floatValue(), width, height);
        }
        if (mod instanceof InfoHUDMod) {
            InfoHUDMod info = (InfoHUDMod) mod;
            List<String> lines = info.getLines();
            int width = 40;
            for (String line : lines) width = Math.max(width, font.getStringWidth(line));
            int padding = info.showBackground.getValue() ? 4 : 0;
            return new Bounds(info.x.getValue().floatValue() - padding, info.y.getValue().floatValue() - padding,
                    width + padding * 3, Math.max(12, lines.size() * (font.FONT_HEIGHT + 2) + padding * 2));
        }
        if (mod instanceof ArmorHUDMod) {
            ArmorHUDMod armor = (ArmorHUDMod) mod;
            int count = 0;
            if (mc.thePlayer != null) {
                for (net.minecraft.item.ItemStack stack : mc.thePlayer.inventory.armorInventory) if (stack != null) count++;
                if (armor.showHeldItem.getValue() && mc.thePlayer.getHeldItem() != null) count++;
            }
            count = Math.max(1, count == 0 ? (armor.showHeldItem.getValue() ? 5 : 4) : count);
            return new Bounds(armor.x.getValue().floatValue(), armor.y.getValue().floatValue(),
                    armor.mode.is("Horizontal") ? count * 18 : 18, armor.mode.is("Horizontal") ? 18 : count * 18);
        }
        if (mod instanceof TargetHUDMod) {
            TargetHUDMod target = (TargetHUDMod) mod;
            EntityLivingBase entity = target.getDisplayTarget();
            if (target.style.is("Simple")) {
                int width = entity == null ? 100 : font.getStringWidth(entity.getName() + " §c" + (int) entity.getHealth() + "❤") + 10;
                return new Bounds(target.x.getValue().floatValue() - 3, target.y.getValue().floatValue() - 3, width, 15);
            }
            return new Bounds(target.x.getValue().floatValue(), target.y.getValue().floatValue(), 142, 51);
        }
        if (mod instanceof ArrayListMod) {
            int width = 60, count = 0;
            for (Mod enabled : MeowClient.modManager.getEnableMods()) {
                width = Math.max(width, font.getStringWidth(((ArrayListMod) mod).getDisplayName(enabled))); count++;
            }
            return new Bounds(screenWidth - width - 8, 1, width + 8, Math.max(16, count * (font.FONT_HEIGHT + 2)));
        }
        if (mod instanceof TabMod) {
            TabMod tab = (TabMod) mod;
            int width = 0;
            for (cn.sux1ng.client.mod.Category category : cn.sux1ng.client.mod.Category.values()) width = Math.max(width, font.getStringWidth(ClientLanguage.category(category)) + 12);
            int rowHeight = (int) (12 * tab.uiScale.getValue());
            int height = cn.sux1ng.client.mod.Category.values().length * rowHeight;
            if (tab.isModulesOpen()) {
                List<Mod> modules = MeowClient.modManager.getByCategory(cn.sux1ng.client.mod.Category.values()[tab.selectedCategory()]);
                int moduleWidth = 0;
                for (Mod module : modules) moduleWidth = Math.max(moduleWidth, font.getStringWidth(ClientLanguage.module(module)) + 12);
                width += moduleWidth + 2; height = Math.max(height, (tab.selectedCategory() + modules.size()) * rowHeight);
            }
            return new Bounds(2, 35, width, height);
        }
        return null;
    }

    public static void render(Mod mod) {
        Minecraft mc = Minecraft.getMinecraft();
        if (WIDGETS.contains(mod.getName()) && mc.currentScreen instanceof HudEditorScreen) return;
        drawWidget(mod, false);
    }
    public static void drawWidget(Mod mod, boolean preview) {
        if (!WIDGETS.contains(mod.getName())) { mod.draw(); return; }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        Bounds original = originalBounds(mod, sr.getScaledWidth());
        Bounds target = transform(mod.getName(), original, sr.getScaledWidth(), sr.getScaledHeight());
        float scale = scale(mod.getName());
        try (RenderState state = RenderState.capture()) {
            GlStateManager.translate(target.x - original.x * scale, target.y - original.y * scale, 0);
            GlStateManager.scale(scale, scale, 1);
            if (preview && (!mod.isEnable() || mc.thePlayer == null)) {
                DrawUtil.drawRoundedRect(original.x, original.y, original.width, original.height, 5,
                        MeowTheme.withAlpha(MeowTheme.current().surface, 120));
                mc.fontRendererObj.drawStringWithShadow(mc.fontRendererObj.trimStringToWidth(ClientLanguage.module(mod), Math.max(0, (int) original.width - 8)),
                        original.x + 4, original.y + 4, MeowTheme.current().muted);
            } else mod.draw();
        }
    }
}
