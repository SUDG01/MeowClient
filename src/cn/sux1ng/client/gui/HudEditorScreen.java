package cn.sux1ng.client.gui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.HudLayout;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.io.IOException;

/** Drag widgets directly over the live game view; no module is enabled just to preview it. */
public class HudEditorScreen extends GuiScreen {
    private final GuiScreen parent;
    private Mod selected;
    private boolean dragging;
    private float offsetX, offsetY;

    public HudEditorScreen(GuiScreen parent) { this.parent = parent; }
    private HudLayout.Bounds original(Mod mod) { return HudLayout.originalBounds(mod, width); }
    private HudLayout.Bounds bounds(Mod mod) { return HudLayout.transform(mod.getName(), original(mod), width, height); }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (MeowClient.modManager == null) return;
        if (dragging && selected != null) move(mouseX - offsetX, mouseY - offsetY);
        MeowTheme.Palette theme = MeowTheme.current();
        for (String name : HudLayout.WIDGETS) {
            Mod mod = MeowClient.modManager.getByName(name);
            if (mod == null) continue;
            HudLayout.drawWidget(mod, true);
            HudLayout.Bounds b = bounds(mod);
            DrawUtil.drawRoundedOutline(b.x - 2, b.y - 2, b.width + 4, b.height + 4, 4, 1,
                    mod == selected ? theme.accent : MeowTheme.withAlpha(theme.outline, mod.isEnable() ? 210 : 100));
            if (mod == selected || b.contains(mouseX, mouseY)) {
                String label = ClientLanguage.module(mod) + " · " + Math.round(HudLayout.scale(name) * 100) + "%";
                int labelWidth = fontRendererObj.getStringWidth(label) + 8;
                float labelX = Math.max(2, Math.min(b.x, width - labelWidth - 2));
                int labelY = (int) Math.max(2, b.y - 13);
                DrawUtil.drawRoundedRect(labelX, labelY - 2, labelWidth, 13, 4, theme.surface);
                fontRendererObj.drawStringWithShadow(label, labelX + 4, labelY, theme.text);
            }
        }
        int left = 12, top = height - 40;
        MeowTheme.panel(left, top, width - 24, 29, 8);
        fontRendererObj.drawStringWithShadow(ClientLanguage.ui("HUD editor"), left + 10, top + 10, theme.text);
        button(width - 170, top + 4, 72, ClientLanguage.ui(selected == null ? "Reset all" : "Reset"), mouseX, mouseY);
        button(width - 90, top + 4, 68, ClientLanguage.ui("Done"), mouseX, mouseY);
        if (selected != null && !dragging) {
            String hint = ClientLanguage.ui("Drag to move · Scroll to scale · Right click to toggle");
            fontRendererObj.drawStringWithShadow(hint, 14, height - 53, theme.muted);
        }
    }

    private void button(int x, int y, int w, String label, int mx, int my) {
        MeowTheme.Palette theme = MeowTheme.current();
        DrawUtil.drawRoundedRect(x, y, w, 21, 6, hit(mx, my, x, y, w, 21) ? theme.hover : theme.card);
        fontRendererObj.drawStringWithShadow(label, x + (w - fontRendererObj.getStringWidth(label)) / 2f, y + 7, theme.text);
    }
    private Mod hovered(int mx, int my) {
        if (selected != null && bounds(selected).contains(mx, my)) return selected;
        for (int i = HudLayout.WIDGETS.size() - 1; i >= 0; i--) {
            Mod mod = MeowClient.modManager.getByName(HudLayout.WIDGETS.get(i));
            if (mod != null && bounds(mod).contains(mx, my)) return mod;
        }
        return null;
    }
    private void move(float x, float y) {
        if (Keyboard.isCreated() && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
            x = Math.round(x / 5) * 5; y = Math.round(y / 5) * 5;
        }
        HudLayout.place(selected.getName(), original(selected), width, height, x, y, HudLayout.scale(selected.getName()));
    }
    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button == 0 && hit(mx, my, width - 90, height - 36, 68, 21)) { mc.displayGuiScreen(parent); return; }
        if (button == 0 && hit(mx, my, width - 170, height - 36, 72, 21)) {
            if (selected == null) HudLayout.resetAll(); else HudLayout.reset(selected.getName());
            return;
        }
        if (my >= height - 44) return;
        selected = hovered(mx, my);
        if (selected == null) return;
        if (button == 1) { selected.setEnable(!selected.isEnable()); return; }
        if (button == 0) {
            HudLayout.Bounds b = bounds(selected);
            offsetX = mx - b.x; offsetY = my - b.y; dragging = true;
        }
    }
    @Override protected void mouseReleased(int mx, int my, int state) { dragging = false; }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        Mod mod = hovered(mx, my);
        if (mod == null || my >= height - 44) return;
        selected = mod;
        HudLayout.Bounds b = bounds(mod);
        float scale = Math.max(0.5f, Math.min(2, HudLayout.scale(mod.getName()) + (wheel > 0 ? 0.05f : -0.05f)));
        HudLayout.place(mod.getName(), original(mod), width, height, b.x, b.y, scale);
    }
    @Override protected void keyTyped(char character, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if (key == Keyboard.KEY_TAB) {
            int index = selected == null ? -1 : HudLayout.WIDGETS.indexOf(selected.getName());
            selected = MeowClient.modManager.getByName(HudLayout.WIDGETS.get((index + 1) % HudLayout.WIDGETS.size()));
            return;
        }
        if (selected == null) return;
        if (key == Keyboard.KEY_DELETE) { HudLayout.reset(selected.getName()); return; }
        HudLayout.Bounds b = bounds(selected);
        int step = Keyboard.isCreated() && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) ? 5 : 1;
        if (key == Keyboard.KEY_LEFT) move(b.x - step, b.y);
        if (key == Keyboard.KEY_RIGHT) move(b.x + step, b.y);
        if (key == Keyboard.KEY_UP) move(b.x, b.y - step);
        if (key == Keyboard.KEY_DOWN) move(b.x, b.y + step);
    }
    @Override public void onGuiClosed() { if (MeowClient.configManager != null) MeowClient.configManager.save(); }
    @Override public boolean doesGuiPauseGame() { return false; }
    private static boolean hit(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
}
