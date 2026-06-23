package cn.sux1ng.client.util;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * 绘制工具类
 * 提供圆角矩形、渐变、圆形等绘图方法
 */
public class DrawUtil {

    /**
     * 画实心矩形
     */
    public static void drawRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }

    /**
     * 画实心矩形 (double 坐标版本)
     */
    public static void drawRect(double x, double y, double width, double height, int color) {
        drawRect((int) x, (int) y, (int) width, (int) height, color);
    }

    /**
     * 画圆角矩形
     * 使用 GL_TRIANGLE_FAN 在每个角画弧线，不依赖着色器
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
        GlStateManager.color(red, grn, blu, a);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        // 四个圆角的中心坐标
        double[][] corners = {
                {x + r, y + r},           // 左上
                {x + width - r, y + r},   // 右上
                {x + width - r, y + height - r}, // 右下
                {x + r, y + height - r}   // 左下
        };
        // 每个圆角的起始角度（弧度）
        double[] startAngles = {Math.PI, Math.PI * 1.5, 0, Math.PI * 0.5};

        // 画四个圆角扇形
        int segments = 8;  // 每个角的三角面数，越多越圆滑
        for (int i = 0; i < 4; i++) {
            double cx = corners[i][0];
            double cy = corners[i][1];
            double start = startAngles[i];
            double end = start + Math.PI / 2;

            wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
            wr.pos(cx, cy, 0).endVertex();  // 圆心
            for (int j = 0; j <= segments; j++) {
                double angle = start + (end - start) * j / segments;
                double px = cx + Math.cos(angle) * r;
                double py = cy + Math.sin(angle) * r;
                wr.pos(px, py, 0).endVertex();
            }
            tessellator.draw();
        }

        // 画中央填充矩形
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x + r, y, 0).endVertex();
        wr.pos(x + width - r, y, 0).endVertex();
        wr.pos(x + width - r, y + height, 0).endVertex();
        wr.pos(x + r, y + height, 0).endVertex();
        tessellator.draw();

        // 画上边条
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x + r, y, 0).endVertex();
        wr.pos(x + width - r, y, 0).endVertex();
        wr.pos(x + width - r, y + r, 0).endVertex();
        wr.pos(x + r, y + r, 0).endVertex();
        tessellator.draw();

        // 画下边条
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x + r, y + height - r, 0).endVertex();
        wr.pos(x + width - r, y + height - r, 0).endVertex();
        wr.pos(x + width - r, y + height, 0).endVertex();
        wr.pos(x + r, y + height, 0).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * 画圆角空心框
     */
    public static void drawRoundedOutline(double x, double y, double width, double height, double radius, double borderWidth, int color) {
        // 简化实现：用圆角矩形 + 内部裁切
        drawRoundedRect(x, y, width, height, radius, color);
        drawRoundedRect(x + borderWidth, y + borderWidth,
                width - borderWidth * 2, height - borderWidth * 2,
                Math.max(0, radius - borderWidth), 0x00000000);
    }

    /**
     * 画垂直渐变矩形
     */
    public static void drawGradientVertical(double x, double y, double width, double height, int colorTop, int colorBottom) {
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
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x + width, y, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y + height, 0).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + width, y + height, 0).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    /**
     * 画水平渐变矩形
     */
    public static void drawGradientHorizontal(double x, double y, double width, double height, int colorLeft, int colorRight) {
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
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(x, y + height, 0).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + width, y + height, 0).color(r2, g2, b2, a2).endVertex();
        wr.pos(x + width, y, 0).color(r1, g1, b1, a1).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    /**
     * 画实心圆
     */
    public static void drawCircle(double centerX, double centerY, double radius, int color) {
        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(centerX, centerY, 0).endVertex();

        int segments = 24;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            double px = centerX + Math.cos(angle) * radius;
            double py = centerY + Math.sin(angle) * radius;
            wr.pos(px, py, 0).endVertex();
        }
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
