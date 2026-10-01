package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.ProjectilePrediction;
import cn.sux1ng.client.util.RenderState;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import java.awt.Color;

public class TrajectoriesMod extends Mod {
    public BooleanValue idleBow = new BooleanValue("IdleBow", true);
    public BooleanValue landing = new BooleanValue("Landing", true);
    public NumberValue lineWidth = new NumberValue("LineWidth", 2.5, 1, 6, 0.5);
    public ColorValue color = new ColorValue("Color", new Color(246, 175, 203));

    public TrajectoriesMod() {
        super("Trajectories", Category.RENDER);
        addValues(idleBow, landing, lineWidth, color);
    }

    public ProjectilePrediction.Result predict(float partialTicks) {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return null;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return null;
        Item item = held.getItem();
        boolean arrow = item instanceof ItemBow;
        if (!arrow && !(item instanceof ItemSnowball) && !(item instanceof ItemEgg) && !(item instanceof ItemEnderPearl)) return null;
        if (arrow && !mc.thePlayer.isUsingItem() && !idleBow.getValue()) return null;
        double speed = arrow ? ProjectilePrediction.bowSpeed(mc.thePlayer.isUsingItem() ? mc.thePlayer.getItemInUseDuration() : 20) : 1.5;
        if (speed == 0) return null;
        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double pitch = Math.toRadians(mc.thePlayer.rotationPitch);
        Vec3 start = new Vec3(
                mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks - Math.cos(yaw) * 0.16f,
                mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks + mc.thePlayer.getEyeHeight() - 0.1f,
                mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks - Math.sin(yaw) * 0.16f);
        Vec3 motion = new Vec3(-Math.sin(yaw) * Math.cos(pitch) * speed,
                -Math.sin(pitch) * speed, Math.cos(yaw) * Math.cos(pitch) * speed);
        return ProjectilePrediction.simulate(start, motion, arrow, new ProjectilePrediction.Environment() {
            public boolean isLoaded(Vec3 position) { return mc.theWorld.isBlockLoaded(new BlockPos(position)); }
            public boolean isWater(Vec3 position) { return mc.theWorld.getBlockState(new BlockPos(position)).getBlock().getMaterial() == Material.water; }
            public MovingObjectPosition trace(Vec3 from, Vec3 to) {
                MovingObjectPosition closest = mc.theWorld.rayTraceBlocks(from, to, false, true, false);
                double distance = closest == null ? from.squareDistanceTo(to) : from.squareDistanceTo(closest.hitVec);
                AxisAlignedBB area = new AxisAlignedBB(Math.min(from.xCoord, to.xCoord), Math.min(from.yCoord, to.yCoord), Math.min(from.zCoord, to.zCoord),
                        Math.max(from.xCoord, to.xCoord), Math.max(from.yCoord, to.yCoord), Math.max(from.zCoord, to.zCoord)).expand(1, 1, 1);
                for (Entity entity : mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, area)) {
                    if (entity.isDead || !entity.canBeCollidedWith()) continue;
                    MovingObjectPosition intercept = entity.getEntityBoundingBox().expand(0.3, 0.3, 0.3).calculateIntercept(from, to);
                    if (intercept != null && from.squareDistanceTo(intercept.hitVec) < distance) {
                        distance = from.squareDistanceTo(intercept.hitVec);
                        closest = new MovingObjectPosition(entity, intercept.hitVec);
                    }
                }
                return closest;
            }
        });
    }

    @Override
    public void render(float partialTicks) {
        ProjectilePrediction.Result result = predict(partialTicks);
        if (result == null || result.points.size() < 2) return;
        RenderManager rm = mc.getRenderManager();
        Color c = color.getColor();
        try (RenderState state = RenderState.capture()) {
            RenderState.setupWorldEffect();
            GL11.glLineWidth(lineWidth.getValue().floatValue());
            WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
            wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (Vec3 point : result.points) {
                wr.pos(point.xCoord - rm.renderPosX, point.yCoord - rm.renderPosY, point.zCoord - rm.renderPosZ)
                        .color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f).endVertex();
            }
            Tessellator.getInstance().draw();
            if (landing.getValue() && result.hit != null) {
                Vec3 hit = result.hit.hitVec;
                GlStateManager.translate(hit.xCoord - rm.renderPosX, hit.yCoord - rm.renderPosY, hit.zCoord - rm.renderPosZ);
                EnumFacing face = result.hit.sideHit;
                if (face == EnumFacing.NORTH || face == EnumFacing.SOUTH) GlStateManager.rotate(90, 1, 0, 0);
                if (face == EnumFacing.EAST || face == EnumFacing.WEST) GlStateManager.rotate(90, 0, 0, 1);
                wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i < 40; i++) {
                    double angle = Math.PI * 2 * i / 40;
                    wr.pos(Math.cos(angle) * 0.22, 0.015, Math.sin(angle) * 0.22)
                            .color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f).endVertex();
                }
                Tessellator.getInstance().draw();
            }
        }
    }
}
