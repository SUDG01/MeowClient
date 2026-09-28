package cn.sux1ng.client.gui;

import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Circular face and hat layer from the current player's skin. */
public final class SkinAvatar {
    private SkinAvatar() {}

    public static void draw(Minecraft mc, int centerX, int centerY, int radius, boolean hovered) {
        MeowTheme.Palette theme = MeowTheme.current();
        DrawUtil.drawCircle(centerX, centerY, radius + 4, hovered ? theme.accent : theme.outline);
        DrawUtil.drawCircle(centerX, centerY, radius + 2, theme.surface);
        ResourceLocation skin = mc.thePlayer == null
                ? DefaultPlayerSkin.getDefaultSkinLegacy() : mc.thePlayer.getLocationSkin();

        boolean wasBlending = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasCulling = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(skin);
        drawFaceLayer(centerX, centerY, radius, 8, 8);
        drawFaceLayer(centerX, centerY, radius, 40, 8);
        GlStateManager.color(1, 1, 1, 1);
        if (!wasBlending) GlStateManager.disableBlend();
        if (wasDepth) GlStateManager.enableDepth();
        if (wasCulling) GlStateManager.enableCull();
    }

    private static void drawFaceLayer(int centerX, int centerY, int radius, int textureX, int textureY) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertices = tessellator.getWorldRenderer();
        vertices.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX);
        vertices.pos(centerX, centerY, 0).tex((textureX + 4.0) / 64.0, (textureY + 4.0) / 64.0).endVertex();
        for (int i = 0; i <= 40; i++) {
            double angle = Math.PI * 2.0 * i / 40.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            vertices.pos(centerX + cos * radius, centerY + sin * radius, 0)
                    .tex((textureX + 4.0 + cos * 4.0) / 64.0,
                            (textureY + 4.0 + sin * 4.0) / 64.0).endVertex();
        }
        tessellator.draw();
    }

    public static boolean hit(int mouseX, int mouseY, int centerX, int centerY, int radius) {
        int dx = mouseX - centerX;
        int dy = mouseY - centerY;
        return dx * dx + dy * dy <= (radius + 4) * (radius + 4);
    }
}
