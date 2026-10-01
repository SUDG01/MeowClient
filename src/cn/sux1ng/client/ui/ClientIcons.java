package cn.sux1ng.client.ui;

import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.util.RenderState;
import net.minecraft.client.renderer.GlStateManager;

/** Small vector icons for the two tools in ClientSetting. */
public final class ClientIcons {
    private ClientIcons() {}
    public static void brush(int x, int y, int color) {
        try (RenderState state = RenderState.capture()) {
            GlStateManager.translate(x, y, 0);
            GlStateManager.rotate(38, 0, 0, 1);
            DrawUtil.drawRoundedRect(-1.5, -9, 3, 11, 1, color);
            DrawUtil.drawRect(-3, 2, 6, 3, color);
            DrawUtil.drawRoundedRect(-4, 5, 8, 5, 2, color);
            DrawUtil.drawRect(-3, 9, 2, 2, color);
        }
    }
    public static void keyboard(int x, int y, int color) {
        DrawUtil.drawRoundedOutline(x - 9, y - 6, 18, 12, 3, 1, color);
        for (int row = 0; row < 2; row++) for (int column = 0; column < 4; column++) {
            DrawUtil.drawRect(x - 6 + column * 3, y - 3 + row * 3, 2, 2, color);
        }
        DrawUtil.drawRect(x - 4, y + 3, 8, 1, color);
    }
    public static boolean hit(int mx, int my, int cx, int cy, int radius) {
        int dx = mx - cx, dy = my - cy;
        return dx * dx + dy * dy <= radius * radius;
    }
}
