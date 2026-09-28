package cn.sux1ng.client.util;

import net.minecraft.client.gui.Gui;
public class DrawUtil {

    public static void drawRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }

    public static void drawRect(double x, double y, double width, double height, int color) {
        Gui.drawRect((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color) {
        int left = (int) Math.round(x);
        int top = (int) Math.round(y);
        int w = (int) Math.round(width);
        int h = (int) Math.round(height);
        if (w <= 0 || h <= 0) return;
        int r = Math.min((int) Math.round(radius), Math.min(w, h) / 2);
        if (r <= 0) {
            Gui.drawRect(left, top, left + w, top + h, color);
            return;
        }
        Gui.drawRect(left, top + r, left + w, top + h - r, color);
        for (int row = 0; row < r; row++) {
            int inset = cornerInset(r, row);
            Gui.drawRect(left + inset, top + row, left + w - inset, top + row + 1, color);
            Gui.drawRect(left + inset, top + h - row - 1, left + w - inset, top + h - row, color);
        }
    }

    public static void drawRoundedOutline(double x, double y, double width, double height,
                                          double radius, double borderWidth, int color) {
        int left = (int) Math.round(x);
        int top = (int) Math.round(y);
        int w = (int) Math.round(width);
        int h = (int) Math.round(height);
        if (w <= 0 || h <= 0) return;
        int r = Math.min((int) Math.round(radius), Math.min(w, h) / 2);
        int b = Math.max(1, (int) Math.round(borderWidth));
        for (int row = 0; row < r; row++) {
            int outerInset = cornerInset(r, row);
            if (row < b) {
                Gui.drawRect(left + outerInset, top + row,
                        left + w - outerInset, top + row + 1, color);
                Gui.drawRect(left + outerInset, top + h - row - 1,
                        left + w - outerInset, top + h - row, color);
            } else {
                int innerInset = b + cornerInset(Math.max(0, r - b), row - b);
                Gui.drawRect(left + outerInset, top + row,
                        left + innerInset, top + row + 1, color);
                Gui.drawRect(left + w - innerInset, top + row,
                        left + w - outerInset, top + row + 1, color);
                Gui.drawRect(left + outerInset, top + h - row - 1,
                        left + innerInset, top + h - row, color);
                Gui.drawRect(left + w - innerInset, top + h - row - 1,
                        left + w - outerInset, top + h - row, color);
            }
        }
        if (h > r * 2) {
            Gui.drawRect(left, top + r, left + b, top + h - r, color);
            Gui.drawRect(left + w - b, top + r, left + w, top + h - r, color);
        }
    }

    public static void drawGradientVertical(double x, double y, double width, double height,
                                            int colorTop, int colorBottom) {
        int steps = Math.max(1, Math.min(48, (int) Math.ceil(height)));
        for (int i = 0; i < steps; i++) {
            int y0 = (int) Math.round(y + height * i / steps);
            int y1 = (int) Math.round(y + height * (i + 1) / steps);
            if (y1 > y0) Gui.drawRect((int) x, y0, (int) (x + width), y1,
                    blend(colorTop, colorBottom, (i + 0.5f) / steps));
        }
    }

    public static void drawGradientHorizontal(double x, double y, double width, double height,
                                              int colorLeft, int colorRight) {
        int steps = Math.max(1, Math.min(48, (int) Math.ceil(width)));
        for (int i = 0; i < steps; i++) {
            int x0 = (int) Math.round(x + width * i / steps);
            int x1 = (int) Math.round(x + width * (i + 1) / steps);
            if (x1 > x0) Gui.drawRect(x0, (int) y, x1, (int) (y + height),
                    blend(colorLeft, colorRight, (i + 0.5f) / steps));
        }
    }

    public static void drawCircle(double centerX, double centerY, double radius, int color) {
        int r = Math.max(0, (int) Math.round(radius));
        for (int dy = -r; dy <= r; dy++) {
            double span = Math.sqrt(Math.max(0, radius * radius - dy * dy));
            Gui.drawRect((int) Math.floor(centerX - span), (int) Math.round(centerY + dy),
                    (int) Math.ceil(centerX + span) + 1, (int) Math.round(centerY + dy) + 1, color);
        }
    }

    private static int cornerInset(int radius, int row) {
        double distance = radius - row - 0.5;
        return (int) Math.ceil(radius - Math.sqrt(Math.max(0, radius * radius - distance * distance)));
    }

    public static int blend(int first, int second, float amount) {
        float t = Math.max(0, Math.min(1, amount));
        int a = Math.round(((first >>> 24) & 255) * (1 - t) + ((second >>> 24) & 255) * t);
        int r = Math.round(((first >>> 16) & 255) * (1 - t) + ((second >>> 16) & 255) * t);
        int g = Math.round(((first >>> 8) & 255) * (1 - t) + ((second >>> 8) & 255) * t);
        int b = Math.round((first & 255) * (1 - t) + (second & 255) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
