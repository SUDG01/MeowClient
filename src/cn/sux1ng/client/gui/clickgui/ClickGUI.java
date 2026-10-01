package cn.sux1ng.client.gui.clickgui;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.gui.ClientSettingsScreen;
import cn.sux1ng.client.gui.SkinAvatar;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends GuiScreen {
    private final List<CategoryPanel> categoryPanels = new ArrayList<>();
    private int scrollX;
    private int scrollY;

    public ClickGUI() {
        int x = 14;
        for (Category category : Category.values()) {
            categoryPanels.add(new CategoryPanel(x, 54, category));
            x += 103;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MeowTheme.Palette theme = MeowTheme.current();
        MeowTheme.backdrop(width, height);
        handleScroll(mouseY);
        MeowTheme.panel(12, 10, width - 24, 34, 10);
        fontRendererObj.drawStringWithShadow(MeowClient.NAME, 24, 19, theme.text);
        fontRendererObj.drawString(MeowClient.VERSION,
                31 + fontRendererObj.getStringWidth(MeowClient.NAME), 19, theme.muted);
        if (maxScrollX() > 0) {
            int trackX = width - 102;
            DrawUtil.drawRoundedRect(trackX, 35, 82, 3, 1, theme.outline);
            int thumb = Math.max(17, 82 * width / (width + maxScrollX()));
            int offset = (82 - thumb) * scrollX / maxScrollX();
            DrawUtil.drawRoundedRect(trackX + offset, 35, thumb, 3, 1, theme.accent);
        }

        int contentBottom = height - 58;
        if (contentBottom > 50) {
            ScaledResolution resolution = new ScaledResolution(mc);
            int scale = resolution.getScaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(8 * scale, (resolution.getScaledHeight() - contentBottom) * scale,
                    Math.max(1, width - 16) * scale, (contentBottom - 50) * scale);
            for (CategoryPanel panel : categoryPanels) panel.drawScreen(mouseX, mouseY, partialTicks);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        int avatarX = 34;
        int avatarY = height - 31;
        boolean avatarHover = SkinAvatar.hit(mouseX, mouseY, avatarX, avatarY, 17)
                || mouseX >= 59 && mouseX < 167 && mouseY >= height - 44 && mouseY < height - 17;
        SkinAvatar.draw(mc, avatarX, avatarY, 17, avatarHover);
        DrawUtil.drawRoundedRect(59, height - 44, 108, 27, 7,
                avatarHover ? theme.hover : theme.card);
        fontRendererObj.drawStringWithShadow(ClientLanguage.ui("ClientSetting"),
                67, height - 35, theme.text);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && (SkinAvatar.hit(mouseX, mouseY, 34, height - 31, 17)
                || mouseX >= 59 && mouseX < 167 && mouseY >= height - 44 && mouseY < height - 17)) {
            mc.displayGuiScreen(new ClientSettingsScreen(this));
            return;
        }
        if (mouseY >= 50 && mouseY < height - 58) {
            for (CategoryPanel panel : categoryPanels) panel.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (CategoryPanel panel : categoryPanels) panel.mouseReleased(mouseX, mouseY, state);
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char character, int keyCode) throws IOException {
        for (CategoryPanel panel : categoryPanels) {
            if (panel.keyTyped(character, keyCode)) return;
        }
        super.keyTyped(character, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private void handleScroll(int mouseY) {
        int wheel = Mouse.getDWheel();
        if (wheel == 0) return;
        boolean horizontal = mouseY < 50 || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) || maxScrollY() == 0;
        if (horizontal) {
            int next = Math.max(0, Math.min(maxScrollX(), scrollX - Integer.signum(wheel) * 70));
            for (CategoryPanel panel : categoryPanels) panel.x -= next - scrollX;
            scrollX = next;
        } else {
            int next = Math.max(0, Math.min(maxScrollY(), scrollY - Integer.signum(wheel) * 28));
            for (CategoryPanel panel : categoryPanels) panel.y -= next - scrollY;
            scrollY = next;
        }
    }

    private int maxScrollX() {
        int right = 14 + (categoryPanels.size() - 1) * 103 + 94;
        return Math.max(0, right - (width - 12));
    }

    private int maxScrollY() {
        int tallest = 0;
        for (CategoryPanel panel : categoryPanels) {
            tallest = Math.max(tallest, panel.getContentHeight());
        }
        return Math.max(0, 54 + tallest - (height - 58));
    }
}
