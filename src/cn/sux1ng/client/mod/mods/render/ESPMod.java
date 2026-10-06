package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.RenderState;
import cn.sux1ng.client.targeting.TargetRules;
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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.BufferUtils;
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
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        try (RenderState state = RenderState.capture()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (TargetRules.canRender(entity, true, false, false, true)) {
                    Color color = entity.isInvisible() ? invisibleColor.getColor() : visibleColor.getColor();
                    if (mode.is("Box3D")) {
                        renderBox3D((EntityLivingBase) entity, color, partialTicks);
                    } else if (mode.is("2D")) {
                        renderBox2D((EntityLivingBase) entity, color, partialTicks);
                    }
                }
            }
        }
    }

    // ==================== 3D Box 逻辑 ====================
    private void renderBox3D(EntityLivingBase entity, Color color, float partialTicks) {
        RenderManager rm = mc.getRenderManager();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.renderPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.renderPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.renderPosZ;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        double width = entity.width / 2.0 + 0.1;
        double height = entity.height + 0.1;
        AxisAlignedBB bb = new AxisAlignedBB(-width, 0, -width, width, height, width);

        RenderState.setupWorldEffect();
        GL11.glLineWidth(1.5F);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        GlStateManager.color(r, g, b, color.getAlpha() / 255f);

        drawBoundingBox(bb);

        GL11.glPopMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // ==================== 2D Box 逻辑 (CSGO Style) ====================
    private void renderBox2D(EntityLivingBase entity, Color color, float partialTicks) {
        RenderManager rm = mc.getRenderManager();

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.renderPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.renderPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.renderPosZ;

        double topY = y + entity.height + 0.1;
        double botY = y - 0.1;

        double[] posTop = projectToScreen(x, topY, z);
        double[] posBot = projectToScreen(x, botY, z);

        if (posTop == null || posBot == null || posBot[1] <= posTop[1]) return;

        try (RenderState state = RenderState.capture()) {
            RenderState.setupWorldEffect();
            ScaledResolution sr = new ScaledResolution(mc);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0, sr.getScaledWidth_double(), sr.getScaledHeight_double(), 0, 1000, 3000);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0, 0, -2000);

            double topScreenY = posTop[1];
            double botScreenY = posBot[1];
            double height = botScreenY - topScreenY;
            double width = height / 2.0;
            double left = posTop[0] - width / 2.0;
            double right = posTop[0] + width / 2.0;
            drawRect(left, topScreenY, right, topScreenY + 1, color.getRGB());
            drawRect(left, botScreenY - 1, right, botScreenY, color.getRGB());
            drawRect(left, topScreenY, left + 1, botScreenY, color.getRGB());
            drawRect(right - 1, topScreenY, right, botScreenY, color.getRGB());

            if (showArmor.getValue()) {
                renderArmor(entity, (float)right + 2, (float)topScreenY, (float)height);
            }
        }
    }

    // ==================== 装备显示逻辑 ====================
    private void renderArmor(EntityLivingBase entity, float x, float y, float height) {
        List<ItemStack> itemsToRender = new ArrayList<>();

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = entity.getEquipmentInSlot(i + 1);
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
            double screenY = (viewport.get(1) + viewport.get(3) - screenCoords.get(1)) / sr.getScaleFactor();
            if (screenCoords.get(2) < 0.0f || screenCoords.get(2) > 1.0f) return null;
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
