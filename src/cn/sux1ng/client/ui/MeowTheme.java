package cn.sux1ng.client.ui;

import cn.sux1ng.client.mod.mods.draw.ClickGUIMod;
import cn.sux1ng.client.util.DrawUtil;

/** Shared sakura and violet colors for the client UI and HUD. */
public final class MeowTheme {
    public static final class Palette {
        public final int backdropTop, backdropBottom, surface, sidebar, card, hover;
        public final int accent, accentSoft, secondary, outline, text, muted;

        private Palette(int backdropTop, int backdropBottom, int surface, int sidebar,
                        int card, int hover, int accent, int accentSoft, int secondary,
                        int outline, int text, int muted) {
            this.backdropTop = backdropTop;
            this.backdropBottom = backdropBottom;
            this.surface = surface;
            this.sidebar = sidebar;
            this.card = card;
            this.hover = hover;
            this.accent = accent;
            this.accentSoft = accentSoft;
            this.secondary = secondary;
            this.outline = outline;
            this.text = text;
            this.muted = muted;
        }
    }

    private static final Palette SAKURA = new Palette(
            0xD51B1A2B, 0xDA302337, 0xF0262237, 0xF01F1E32,
            0xDC373048, 0xF04A3A55, 0xFFF6AFCB, 0xAFB673A0,
            0xFFCBB8F5, 0x856F587D, 0xFFFFF2FA, 0xFFC5B8CE);
    private static final Palette TWILIGHT = new Palette(
            0xD3191B31, 0xDA292341, 0xF022233A, 0xF01B1D33,
            0xDC30334F, 0xF03E4261, 0xFFD3B6FF, 0xAF8C75B5,
            0xFFF4BDD7, 0x856B6596, 0xFFF8F4FF, 0xFFBDB8D4);
    private static final Palette CREAM = new Palette(
            0xDDEDDDE5, 0xDFDCD2E8, 0xF8FFF8FA, 0xF7F5ECF3,
            0xE9F4EAF1, 0xF3ECDCE9, 0xFFE890B5, 0xAFCB9AAD,
            0xFF9A82C8, 0x77775D78, 0xFF3D324A, 0xFF766A7C);

    private MeowTheme() {}

    public static Palette current() {
        if (ClickGUIMod.theme.is("Light")) return CREAM;
        if (ClickGUIMod.theme.is("Skeet")) return TWILIGHT;
        return SAKURA;
    }

    public static void backdrop(int width, int height) {
        Palette palette = current();
        DrawUtil.drawGradientVertical(0, 0, width, height,
                palette.backdropTop, palette.backdropBottom);
        DrawUtil.drawCircle(width - 20, 25, 33, withAlpha(palette.secondary, 22));
        DrawUtil.drawCircle(16, height - 17, 27, withAlpha(palette.accent, 18));
    }

    public static void panel(double x, double y, double width, double height, double radius) {
        Palette palette = current();
        DrawUtil.drawRoundedRect(x + 2, y + 3, width, height, radius, 0x55000000);
        DrawUtil.drawRoundedRect(x, y, width, height, radius, palette.surface);
        DrawUtil.drawRoundedOutline(x, y, width, height, radius, 1, palette.outline);
    }

    public static void paw(double x, double y, int color) {
        DrawUtil.drawCircle(x, y + 4, 4.4, color);
        DrawUtil.drawCircle(x - 6, y - 2, 1.8, color);
        DrawUtil.drawCircle(x - 2, y - 5, 1.8, color);
        DrawUtil.drawCircle(x + 3, y - 5, 1.8, color);
        DrawUtil.drawCircle(x + 7, y - 2, 1.8, color);
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}
