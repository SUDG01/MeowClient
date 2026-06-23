package cn.sux1ng.client.util;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public class DrawUtil {

    public static void drawRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }

    public static void drawRect(double x, double y, double width, double height, int color) {
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    /**
     * 圆角矩形 — Gui.drawRect 版（直角，稳定可靠）
     */
    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color) {
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    public static void drawRoundedOutline(double x, double y, double width, double height,
                                          double radius, double borderWidth, int color) {
        // 画边框（四条线）
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + borderWidth), color);
        Gui.drawRect((int) x, (int) (y + height - borderWidth), (int) (x + width), (int) (y + height), color);
        Gui.drawRect((int) x, (int) y, (int) (x + borderWidth), (int) (y + height), color);
        Gui.drawRect((int) (x + width - borderWidth), (int) y, (int) (x + width), (int) (y + height), color);
    }

    /**
     * 垂直渐变 — 纯色矩形版（稳定）
     */
    public static void drawGradientVertical(double x, double y, double width, double height,
                                            int colorTop, int colorBottom) {
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), colorTop);
    }

    /**
     * 水平渐变 — 纯色矩形版（稳定）
     */
    public static void drawGradientHorizontal(double x, double y, double width, double height,
                                              int colorLeft, int colorRight) {
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), colorLeft);
    }

    /**
     * 实心圆 — 小矩形版（稳定）
     */
    public static void drawCircle(double centerX, double centerY, double radius, int color) {
        int r = (int) radius;
        Gui.drawRect((int) (centerX - r), (int) (centerY - r),
                (int) (centerX + r), (int) (centerY + r), color);
    }
}
