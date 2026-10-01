package cn.sux1ng.client.gui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.ClientIcons;
import cn.sux1ng.client.input.HighPollingInput;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** The avatar opens this screen from either ClickGUI style. */
public class ClientSettingsScreen extends GuiScreen {
    private final GuiScreen parent;

    public ClientSettingsScreen(GuiScreen parent) { this.parent = parent; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MeowTheme.Palette theme = MeowTheme.current();
        MeowTheme.backdrop(width, height);
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        MeowTheme.panel(left, top, panelWidth, 226, 13);
        fontRendererObj.drawStringWithShadow(MeowClient.NAME, left + 23, top + 21, theme.text);
        fontRendererObj.drawString(ClientLanguage.ui("ClientSetting"), left + 23, top + 42, theme.muted);

        boolean hovered = hit(mouseX, mouseY, left + 18, top + 61, panelWidth - 36, 52);
        DrawUtil.drawRoundedRect(left + 18, top + 61, panelWidth - 36, 52, 9,
                hovered ? theme.hover : theme.card);
        fontRendererObj.drawStringWithShadow(ClientLanguage.ui("Chinese"),
                left + 32, top + 74, theme.text);
        fontRendererObj.drawString(ClientLanguage.ui("Show client controls in Chinese"),
                left + 32, top + 93, theme.muted);
        int toggleX = left + panelWidth - 82;
        DrawUtil.drawRoundedRect(toggleX, top + 76, 49, 22, 11,
                ClientLanguage.isChinese() ? theme.accentSoft : theme.sidebar);
        DrawUtil.drawCircle(ClientLanguage.isChinese() ? toggleX + 37 : toggleX + 12,
                top + 87, 8, theme.text);

        boolean rawHover = hit(mouseX, mouseY, left + 18, top + 121, panelWidth - 36, 52);
        DrawUtil.drawRoundedRect(left + 18, top + 121, panelWidth - 36, 52, 9, rawHover ? theme.hover : theme.card);
        fontRendererObj.drawStringWithShadow(ClientLanguage.ui("Raw mouse input"), left + 32, top + 134, theme.text);
        String rawDescription = HighPollingInput.isEnabled() && HighPollingInput.isAvailable()
                ? "Unaccelerated relative motion" : "Standard input fallback";
        fontRendererObj.drawString(ClientLanguage.ui(rawDescription), left + 32, top + 153, theme.muted);
        DrawUtil.drawRoundedRect(toggleX, top + 136, 49, 22, 11, HighPollingInput.isEnabled() ? theme.accentSoft : theme.sidebar);
        DrawUtil.drawCircle(HighPollingInput.isEnabled() ? toggleX + 37 : toggleX + 12, top + 147, 8, theme.text);

        int backColor = hit(mouseX, mouseY, left + 18, top + 182, 72, 27)
                ? theme.hover : theme.card;
        DrawUtil.drawRoundedRect(left + 18, top + 182, 72, 27, 7, backColor);
        fontRendererObj.drawStringWithShadow(ClientLanguage.ui("Back"),
                left + 31, top + 191, theme.text);
        int brushX = left + panelWidth - 62, keyX = left + panelWidth - 28, toolsY = top + 195;
        boolean brushHover = ClientIcons.hit(mouseX, mouseY, brushX, toolsY, 13);
        boolean keyHover = ClientIcons.hit(mouseX, mouseY, keyX, toolsY, 13);
        DrawUtil.drawCircle(brushX, toolsY, 13, brushHover ? theme.hover : theme.card);
        DrawUtil.drawCircle(keyX, toolsY, 13, keyHover ? theme.hover : theme.card);
        ClientIcons.brush(brushX, toolsY, theme.accent);
        ClientIcons.keyboard(keyX, toolsY, theme.accent);
        if (brushHover || keyHover) {
            String label = ClientLanguage.ui(brushHover ? "HUD editor" : "Key bindings");
            fontRendererObj.drawStringWithShadow(label, left + panelWidth - fontRendererObj.getStringWidth(label) - 18,
                    top + 176, theme.muted);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            int left = panelLeft();
            int top = panelTop();
            if (ClientIcons.hit(mouseX, mouseY, left + panelWidth() - 62, top + 195, 13)) {
                mc.displayGuiScreen(new HudEditorScreen(this)); return;
            }
            if (ClientIcons.hit(mouseX, mouseY, left + panelWidth() - 28, top + 195, 13)) {
                mc.displayGuiScreen(new KeyBindingsScreen(this)); return;
            }
            if (hit(mouseX, mouseY, left + 18, top + 61, panelWidth() - 36, 52)) {
                ClientLanguage.setChinese(!ClientLanguage.isChinese());
                if (MeowClient.configManager != null) MeowClient.configManager.saveClientSettings();
                return;
            }
            if (hit(mouseX, mouseY, left + 18, top + 121, panelWidth() - 36, 52)) {
                HighPollingInput.setEnabled(!HighPollingInput.isEnabled());
                if (MeowClient.configManager != null) MeowClient.configManager.saveClientSettings();
                return;
            }
            if (hit(mouseX, mouseY, left + 18, top + 182, 72, 27)) {
                mc.displayGuiScreen(parent);
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char character, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else super.keyTyped(character, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private int panelWidth() { return Math.min(384, width - 30); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int panelTop() { return (height - 226) / 2; }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
