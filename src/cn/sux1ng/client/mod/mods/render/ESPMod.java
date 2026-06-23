package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.ModeValue;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ESPMod extends Mod {

    // 1. 模式选择
    public ModeValue mode = new ModeValue("Mode", "2D", new String[]{"Box3D", "2D"});

    // 2. 颜色设置 — 使用 ColorValue 替代原来的 R/G/B 三滑块
    public ColorValue visibleColor = new ColorValue("VisibleColor", new Color(255, 255, 255));
    public ColorValue invisibleColor = new ColorValue("InvisibleColor", Color.RED);

    // 3. 装备显示 (仅在 2D 模式下有效)
    public BooleanValue showArmor = new BooleanValue("Armor", true).setVisibility(() -> mode.is("2D"));

    public ESPMod() {
        super("ESP", Category.RENDER);
        addValues(mode, visibleColor, invisibleColor, showArmor);
    }

    @Override
    public void render(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                // 隐身玩家用红色，否则用自定义颜色
                Color color = entity.isInvisible() ? invisibleColor.getColor() : visibleColor.getColor();

                if (mode.is("Box3D")) {
                    renderBox3D((EntityPlayer) entity, color, partialTicks);
                } else if (mode.is("2D")) {
                    renderBox2D((EntityPlayer) entity, color, partialTicks);
                }
            }
        }
    }

    // ==================== 3D Box 逻辑 ====================
    private void renderBox3D(EntityPlayer entity, Color color, float partialTicks) {
        RenderManager rm = mc.getRenderManager();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.renderPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.renderPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.renderPosZ;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        double width = entity.width / 2.0 + 0.1;
        double height = entity.height + 0.1;
        AxisAlignedBB bb = new AxisAlignedBB(-width, 0, -width, width, height, width);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(1.5F);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        GL11.glColor4f(r, g, b, 1.0F);

        drawBoundingBox(bb);

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    // ==================== 2D Box 逻辑 (CSGO Style) ====================
    private void renderBox2D(EntityPlayer entity, Color color, float partialTicks) {
        RenderManager rm = mc.getRenderManager();

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.renderPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.renderPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.renderPosZ;

        double topY = y + entity.height + 0.1;
        double botY = y - 0.1;

        double[] posTop = projectToScreen(x, topY, z);
        double[] posBot = projectToScreen(x, botY, z);

        if (posTop == null || posBot == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        mc.entityRenderer.setupOverlayRendering();

        double topScreenY = posTop[1];
        double botScreenY = posBot[1];
        double centerX = posTop[0];

        double height = botScreenY - topScreenY;
        double width = height / 2.0;

        double left = centerX - width / 2.0;
        double right = centerX + width / 2.0;

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        GL11.glColor4f(r, g, b, 1.0F);

        // 画空心框
        drawRect(left, topScreenY, right, topScreenY + 1, color.getRGB());
        drawRect(left, botScreenY - 1, right, botScreenY, color.getRGB());
        drawRect(left, topScreenY, left + 1, botScreenY, color.getRGB());
        drawRect(right - 1, topScreenY, right, botScreenY, color.getRGB());

        // 装备显示
        if (showArmor.getValue()) {
            renderArmor(entity, (float)right + 2, (float)topScreenY, (float)height);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    // ==================== 装备显示逻辑 ====================
    private void renderArmor(EntityPlayer entity, float x, float y, float height) {
        List<ItemStack> itemsToRender = new ArrayList<>();

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = entity.inventory.armorInventory[i];
            if (stack != null) itemsToRender.add(stack);
        }
        if (entity.getHeldItem() != null) {
            itemsToRender.add(entity.getHeldItem());
        }

        float itemY = y;
        RenderHelper.enableGUIStandardItemLighting();

        for (ItemStack stack : itemsToRender) {
            GlStateManager.pushMatrix();
            float scale = 0.5f;
            GlStateManager.translate(x, itemY, 0);
            GlStateManager.scale(scale, scale, scale);
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
            mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, 0, 0);
            GlStateManager.popMatrix();
            itemY += 16 * scale;
        }

        RenderHelper.disableStandardItemLighting();
    }

    // ==================== 数学工具 ====================
    private final FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);

    private double[] projectToScreen(double x, double y, double z) {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        boolean result = GLU.gluProject((float) x, (float) y, (float) z, modelview, projection, viewport, screenCoords);

        if (result) {
            ScaledResolution sr = new ScaledResolution(mc);
            double screenX = screenCoords.get(0) / sr.getScaleFactor();
            double screenY = (Display.getHeight() - screenCoords.get(1)) / sr.getScaleFactor();
            if (screenCoords.get(2) > 1.0f) return null;
            return new double[]{screenX, screenY};
        }
        return null;
    }

    // ==================== 辅助绘制方法 ====================
    public static void drawBoundingBox(AxisAlignedBB aa) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(3, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(3, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(1, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        tessellator.draw();
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        if (left < right) { double i = left; left = right; right = i; }
        if (top < bottom) { double j = top; top = bottom; bottom = j; }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f, f1, f2, f3);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
