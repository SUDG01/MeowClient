package cn.sux1ng.client.gui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.ClientIcons;
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
        MeowTheme.panel(left, top, panelWidth, 198, 13);
        fontRendererObj.drawStringWithShadow(MeowClient.NAME, left + 23, top + 21, theme.text);
        fontRendererObj.drawString(ClientLanguage.ui("ClientSetting"), left + 23, top + 56, theme.muted);

        boolean hovered = hit(mouseX, mouseY, left + 18, top + 81, panelWidth - 36, 61);
        DrawUtil.drawRoundedRect(left + 18, top + 81, panelWidth - 36, 61, 9,
                hovered ? theme.hover : theme.card);
        fontRendererObj.drawStringWithShadow(ClientLanguage.ui("Chinese"),
                left + 32, top + 96, theme.text);
        fontRendererObj.drawString(ClientLanguage.ui("Show client controls in Chinese"),
                left + 32, top + 115, theme.muted);
        int toggleX = left + panelWidth - 82;
        DrawUtil.drawRoundedRect(toggleX, top + 98, 49, 22, 11,
                ClientLanguage.isChinese() ? theme.accentSoft : theme.sidebar);
        DrawUtil.drawCircle(ClientLanguage.isChinese() ? toggleX + 37 : toggleX + 12,
                top + 109, 8, theme.text);

        int backColor = hit(mouseX, mouseY, left + 18, top + 156, 72, 27)
                ? theme.hover : theme.card;
        DrawUtil.drawRoundedRect(left + 18, top + 156, 72, 27, 7, backColor);
        fontRendererObj.drawStringWithShadow(ClientLanguage.ui("Back"),
                left + 31, top + 165, theme.text);
        int brushX = left + panelWidth - 62, keyX = left + panelWidth - 28, toolsY = top + 170;
        boolean brushHover = ClientIcons.hit(mouseX, mouseY, brushX, toolsY, 13);
        boolean keyHover = ClientIcons.hit(mouseX, mouseY, keyX, toolsY, 13);
        DrawUtil.drawCircle(brushX, toolsY, 13, brushHover ? theme.hover : theme.card);
        DrawUtil.drawCircle(keyX, toolsY, 13, keyHover ? theme.hover : theme.card);
        ClientIcons.brush(brushX, toolsY, theme.accent);
        ClientIcons.keyboard(keyX, toolsY, theme.accent);
        if (brushHover || keyHover) {
            String label = ClientLanguage.ui(brushHover ? "HUD editor" : "Key bindings");
            fontRendererObj.drawStringWithShadow(label, left + panelWidth - fontRendererObj.getStringWidth(label) - 18,
                    top + 145, theme.muted);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            int left = panelLeft();
            int top = panelTop();
            if (ClientIcons.hit(mouseX, mouseY, left + panelWidth() - 62, top + 170, 13)) {
                mc.displayGuiScreen(new HudEditorScreen(this)); return;
            }
            if (ClientIcons.hit(mouseX, mouseY, left + panelWidth() - 28, top + 170, 13)) {
                mc.displayGuiScreen(new KeyBindingsScreen(this)); return;
            }
            if (hit(mouseX, mouseY, left + 18, top + 81, panelWidth() - 36, 61)) {
                ClientLanguage.setChinese(!ClientLanguage.isChinese());
                if (MeowClient.configManager != null) MeowClient.configManager.saveClientSettings();
                return;
            }
            if (hit(mouseX, mouseY, left + 18, top + 156, 72, 27)) {
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
    private int panelTop() { return (height - 198) / 2; }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
