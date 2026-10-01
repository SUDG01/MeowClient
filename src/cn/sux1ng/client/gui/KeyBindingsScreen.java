package cn.sux1ng.client.gui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.KeyBindings;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Searchable binding list. A conflicting binding is highlighted before it is changed. */
public class KeyBindingsScreen extends GuiScreen {
    private final GuiScreen parent;
    private GuiTextField search;
    private Mod capturing;
    private int scroll;
    public KeyBindingsScreen(GuiScreen parent) { this.parent = parent; }
    private int panelWidth() { return Math.min(520, width - 24); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return 16; }
    private int bottom() { return height - 44; }
    @Override public void initGui() {
        search = new GuiTextField(0, fontRendererObj, left() + 14, top() + 29, panelWidth() - 28, 18);
        search.setMaxStringLength(80);
        search.setEnableBackgroundDrawing(false);
        search.setTextColor(MeowTheme.current().text);
    }
    private List<Mod> modules() {
        List<Mod> result = new ArrayList<>();
        String query = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        for (Mod mod : MeowClient.modManager.getMods()) {
            if (query.isEmpty() || mod.getName().toLowerCase(Locale.ROOT).contains(query)
                    || ClientLanguage.module(mod).toLowerCase(Locale.ROOT).contains(query)) result.add(mod);
        }
        return result;
    }
    private int rows() { return Math.max(1, (bottom() - top() - 58) / 25); }
    private void clamp() { scroll = Math.max(0, Math.min(scroll, Math.max(0, modules().size() - rows()))); }
    @Override public void drawScreen(int mx, int my, float partialTicks) {
        MeowTheme.Palette theme = MeowTheme.current();
        MeowTheme.backdrop(width, height);
        MeowTheme.panel(left(), top(), panelWidth(), height - 32, 12);
        fontRendererObj.drawStringWithShadow(ClientLanguage.ui("Key bindings"), left() + 14, top() + 12, theme.text);
        DrawUtil.drawRoundedRect(left() + 9, top() + 25, panelWidth() - 18, 23, 7, theme.card);
        search.drawTextBox();
        if (search.getText().isEmpty() && !search.isFocused()) fontRendererObj.drawString(ClientLanguage.ui("Search"), left() + 14, top() + 34, theme.muted);
        List<Mod> mods = modules();
        clamp();
        for (int row = 0; row < rows() && row + scroll < mods.size(); row++) {
            Mod mod = mods.get(row + scroll);
            int y = top() + 56 + row * 25;
            boolean hovered = mx >= left() + 9 && mx <= left() + panelWidth() - 9 && my >= y && my < y + 22;
            List<Mod> conflicts = KeyBindings.conflicts(mod, MeowClient.modManager.getMods());
            DrawUtil.drawRoundedRect(left() + 9, y, panelWidth() - 18, 22, 6, hovered ? theme.hover : theme.card);
            fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(ClientLanguage.module(mod), panelWidth() - 190), left() + 16, y + 7, theme.text);
            String key = capturing == mod ? ClientLanguage.ui("Press key...") : mod.getKey() == 0 ? ClientLanguage.ui("None") : Keyboard.getKeyName(mod.getKey());
            fontRendererObj.drawStringWithShadow(key == null ? String.valueOf(mod.getKey()) : key,
                    left() + panelWidth() - 169, y + 7, conflicts.isEmpty() ? theme.accent : 0xFFFF8D9B);
            fontRendererObj.drawStringWithShadow(ClientLanguage.bindMode(mod.getBindMode()), left() + panelWidth() - 65, y + 7, theme.muted);
            if (hovered && !conflicts.isEmpty()) {
                StringBuilder text = new StringBuilder(ClientLanguage.ui("Conflict") + ": ");
                for (int i = 0; i < conflicts.size(); i++) {
                    if (i > 0) text.append(", "); text.append(ClientLanguage.module(conflicts.get(i)));
                }
                fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(text.toString(), panelWidth() - 100), left() + 14, height - 30, 0xFFFF8D9B);
            }
        }
        if (mods.size() > rows()) {
            int track = rows() * 25;
            int thumb = Math.max(14, track * rows() / mods.size());
            int offset = (track - thumb) * scroll / (mods.size() - rows());
            DrawUtil.drawRoundedRect(left() + panelWidth() - 6, top() + 56 + offset, 2, thumb, 1, theme.accent);
        }
        DrawUtil.drawRoundedRect(left() + panelWidth() - 80, height - 39, 66, 23, 6, theme.card);
        fontRendererObj.drawStringWithShadow(ClientLanguage.ui("Back"), left() + panelWidth() - 66, height - 31, theme.text);
    }
    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button == 0 && mx >= left() + panelWidth() - 80 && mx < left() + panelWidth() - 14 && my >= height - 39 && my < height - 16) {
            mc.displayGuiScreen(parent); return;
        }
        if (capturing != null) { capturing = null; return; }
        search.mouseClicked(mx, my, button);
        List<Mod> mods = modules();
        int row = (my - top() - 56) / 25;
        if (my < top() + 56 || row >= rows() || row + scroll >= mods.size()
                || mx < left() + 9 || mx > left() + panelWidth() - 9) return;
        Mod mod = mods.get(row + scroll);
        if (button == 1) MeowClient.modManager.setBinding(mod, 0, mod.getBindMode());
        else if (button == 0 && mx >= left() + panelWidth() - 80) {
            MeowClient.modManager.setBinding(mod, mod.getKey(), KeyBindings.nextMode(mod.getBindMode()));
        } else if (button == 0) { capturing = mod; search.setFocused(false); }
    }
    @Override protected void keyTyped(char character, int key) throws IOException {
        if (capturing != null) {
            if (key != Keyboard.KEY_ESCAPE) MeowClient.modManager.setBinding(capturing,
                    key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK ? 0 : key, capturing.getBindMode());
            capturing = null; return;
        }
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if (search.textboxKeyTyped(character, key)) { scroll = 0; return; }
        if (key == Keyboard.KEY_UP) scroll--;
        if (key == Keyboard.KEY_DOWN) scroll++;
        clamp();
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) { scroll += wheel < 0 ? 3 : -3; clamp(); }
    }
    @Override public void updateScreen() { search.updateCursorCounter(); }
    @Override public void onGuiClosed() { if (MeowClient.configManager != null) MeowClient.configManager.save(); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
