package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

/**
 * Tracers — 实体追踪线
 * 从屏幕中心向附近实体画出彩色线
 */
public class TracersMod extends Mod {

    public NumberValue range = new NumberValue("Range", 50, 10, 200, 5);
    public BooleanValue players = new BooleanValue("Players", true);
    public BooleanValue mobs = new BooleanValue("Mobs", false);
    public BooleanValue animals = new BooleanValue("Animals", false);
    public ColorValue lineColor = new ColorValue("Color", new java.awt.Color(255, 183, 178));

    public TracersMod() {
        super("Tracers", Category.RENDER);
        addValues(range, players, mobs, animals, lineColor);
    }

    @Override
    public void render(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GL11.glLineWidth(1.5f);

        double rx = mc.getRenderManager().renderPosX;
        double ry = mc.getRenderManager().renderPosY;
        double rz = mc.getRenderManager().renderPosZ;
        double rangeSq = range.getValue() * range.getValue();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        java.awt.Color c = lineColor.getColor();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer) continue;
            if (!isValidTarget(entity)) continue;

            double dist = entity.getDistanceSqToEntity(mc.thePlayer);
            if (dist > rangeSq) continue;

            // 目标实体中心
            double ex = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rx;
            double ey = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - ry + entity.height / 2;
            double ez = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rz;

            // 从玩家眼睛位置出发
            double px = mc.thePlayer.posX - rx;
            double py = mc.thePlayer.posY - ry + mc.thePlayer.getEyeHeight();
            double pz = mc.thePlayer.posZ - rz;

            wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(px, py, pz).color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 0.8f).endVertex();
            wr.pos(ex, ey, ez).color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 0.4f).endVertex();
            tess.draw();
        }

        GL11.glLineWidth(1f);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private boolean isValidTarget(Entity e) {
        if (e instanceof EntityPlayer && players.getValue()) return true;
        if (e instanceof EntityMob && mobs.getValue()) return true;
        if (e instanceof EntityAnimal && animals.getValue()) return true;
        return false;
    }
}
