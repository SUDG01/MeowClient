package cn.sux1ng.client.util;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;

/** Saves the states used by client effects through Minecraft's GL cache, plus both matrices. */
public final class RenderState implements AutoCloseable {
    private static final FloatBuffer COLOR_BUFFER = BufferUtils.createFloatBuffer(16);
    private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    private final boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
    private final boolean light0 = GL11.glIsEnabled(GL11.GL_LIGHT0);
    private final boolean light1 = GL11.glIsEnabled(GL11.GL_LIGHT1);
    private final boolean colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
    private final boolean rescaleNormal = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    private final boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
    private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final boolean lineSmooth = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);
    private final int blendSrc = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int blendDst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
    private final float lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    private final float red, green, blue, opacity;

    private RenderState() {
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, COLOR_BUFFER);
        red = COLOR_BUFFER.get(0); green = COLOR_BUFFER.get(1);
        blue = COLOR_BUFFER.get(2); opacity = COLOR_BUFFER.get(3);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
    }

    public static RenderState capture() { return new RenderState(); }

    /** Untextured effects are visible on either side and do not alter the world's depth buffer. */
    public static void setupWorldEffect() {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableFog();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
    }

    @Override
    public void close() {
        GL11.glPopAttrib();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(matrixMode);
        GlStateManager.setActiveTexture(activeTexture);
        GlStateManager.bindTexture(textureBinding);
        if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        if (light0) GlStateManager.enableLight(0); else GlStateManager.disableLight(0);
        if (light1) GlStateManager.enableLight(1); else GlStateManager.disableLight(1);
        if (colorMaterial) GlStateManager.enableColorMaterial(); else GlStateManager.disableColorMaterial();
        if (rescaleNormal) GlStateManager.enableRescaleNormal(); else GlStateManager.disableRescaleNormal();
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (fog) GlStateManager.enableFog(); else GlStateManager.disableFog();
        GlStateManager.depthMask(depthMask);
        GlStateManager.tryBlendFuncSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha);
        GlStateManager.shadeModel(shadeModel);
        GL11.glLineWidth(lineWidth);
        if (lineSmooth) GL11.glEnable(GL11.GL_LINE_SMOOTH); else GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.color(red, green, blue, opacity);
    }
}
