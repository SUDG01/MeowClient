package cn.sux1ng.client.util;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class DrawUtil {

    public static void drawRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }

    public static void drawRect(double x, double y, double width, double height, int color) {
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    /**
     * 圆角矩形
     * 使用与 Gui.drawRect 完全一致的 GL 状态模式，加 GL immediate mode 画角弧
     */
    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color) {
        double r = Math.min(radius, Math.min(width / 2, height / 2));
        if (r <= 0) {
            drawRect(x, y, width, height, color);
            return;
        }

        float a = (color >> 24 & 255) / 255f;
        float red = (color >> 16 & 255) / 255f;
        float grn = (color >> 8 & 255) / 255f;
        float blu = (color & 255) / 255f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glColor4f(red, grn, blu, a);

        // 中央主体
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(x + r, y);
        GL11.glVertex2d(x + width - r, y);
        GL11.glVertex2d(x + width - r, y + height);
        GL11.glVertex2d(x + r, y + height);
        GL11.glEnd();

        // 上边条
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(x + r, y);
        GL11.glVertex2d(x + width - r, y);
        GL11.glVertex2d(x + width - r, y + r);
        GL11.glVertex2d(x + r, y + r);
        GL11.glEnd();

        // 下边条
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(x + r, y + height - r);
        GL11.glVertex2d(x + width - r, y + height - r);
        GL11.glVertex2d(x + width - r, y + height);
        GL11.glVertex2d(x + r, y + height);
        GL11.glEnd();

        // 四个圆角
        double[][] corners = {
                {x + r, y + r, Math.PI, Math.PI * 1.5},           // 左上
                {x + width - r, y + r, Math.PI * 1.5, Math.PI * 2}, // 右上
                {x + width - r, y + height - r, 0, Math.PI * 0.5},  // 右下
                {x + r, y + height - r, Math.PI * 0.5, Math.PI}     // 左下
        };
        int seg = 8;

        for (double[] corner : corners) {
            double cx = corner[0];
            double cy = corner[1];
            double start = corner[2];
            double end = corner[3];

            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2d(cx, cy);
            for (int i = 0; i <= seg; i++) {
                double angle = start + (end - start) * i / seg;
                GL11.glVertex2d(cx + Math.cos(angle) * r, cy + Math.sin(angle) * r);
            }
            GL11.glEnd();
        }

        GL11.glColor4f(1, 1, 1, 1);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * 圆角空心框
     */
    public static void drawRoundedOutline(double x, double y, double width, double height,
                                          double radius, double borderWidth, int color) {
        drawRoundedRect(x, y, width, height, radius, color);
        drawRoundedRect(x + borderWidth, y + borderWidth,
                width - borderWidth * 2, height - borderWidth * 2,
                Math.max(0, radius - borderWidth), 0xFF101010);
    }

    /**
     * 垂直渐变矩形
     */
    public static void drawGradientVertical(double x, double y, double width, double height,
                                            int colorTop, int colorBottom) {
        float a1 = (colorTop >> 24 & 255) / 255f;
        float r1 = (colorTop >> 16 & 255) / 255f;
        float g1 = (colorTop >> 8 & 255) / 255f;
        float b1 = (colorTop & 255) / 255f;

        float a2 = (colorBottom >> 24 & 255) / 255f;
        float r2 = (colorBottom >> 16 & 255) / 255f;
        float g2 = (colorBottom >> 8 & 255) / 255f;
        float b2 = (colorBottom & 255) / 255f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(r1, g1, b1, a1);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + width, y);
        GL11.glColor4f(r2, g2, b2, a2);
        GL11.glVertex2d(x + width, y + height);
        GL11.glVertex2d(x, y + height);
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glColor4f(1, 1, 1, 1);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    /**
     * 水平渐变矩形
     */
    public static void drawGradientHorizontal(double x, double y, double width, double height,
                                              int colorLeft, int colorRight) {
        float a1 = (colorLeft >> 24 & 255) / 255f;
        float r1 = (colorLeft >> 16 & 255) / 255f;
        float g1 = (colorLeft >> 8 & 255) / 255f;
        float b1 = (colorLeft & 255) / 255f;

        float a2 = (colorRight >> 24 & 255) / 255f;
        float r2 = (colorRight >> 16 & 255) / 255f;
        float g2 = (colorRight >> 8 & 255) / 255f;
        float b2 = (colorRight & 255) / 255f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(r1, g1, b1, a1);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x, y + height);
        GL11.glColor4f(r2, g2, b2, a2);
        GL11.glVertex2d(x + width, y + height);
        GL11.glVertex2d(x + width, y);
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glColor4f(1, 1, 1, 1);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    /**
     * 实心圆
     */
    public static void drawCircle(double centerX, double centerY, double radius, int color) {
        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glColor4f(r, g, b, a);

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2d(centerX, centerY);
        int segments = 24;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            GL11.glVertex2d(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GL11.glColor4f(1, 1, 1, 1);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
