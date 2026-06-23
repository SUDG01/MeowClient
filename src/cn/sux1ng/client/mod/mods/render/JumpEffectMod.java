package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * JumpEffect — 跳跃光环
 * 检测玩家跳跃，在跳跃位置画出扩展+淡出的彩色圆环
 */
public class JumpEffectMod extends Mod {

    public NumberValue ringCount = new NumberValue("Rings", 3, 1, 8, 1);
    public NumberValue duration = new NumberValue("Duration", 800, 200, 3000, 100);
    public ColorValue ringColor = new ColorValue("Color", new Color(255, 183, 178));

    private boolean wasOnGround = true;
    private final List<JumpRing> rings = new ArrayList<>();

    public JumpEffectMod() {
        super("JumpEffect", Category.RENDER);
        addValues(ringCount, duration, ringColor);
    }

    private static class JumpRing {
        double x, y, z;
        long startTime;
        JumpRing(double x, double y, double z) { this.x = x; this.y = y; this.z = z; this.startTime = System.currentTimeMillis(); }
    }

    @Override
    public void render(float partialTicks) {
        if (mc.thePlayer == null) return;

        // 检测跳跃
        if (!wasOnGround && mc.thePlayer.onGround) {
            // 落地不触发
        }
        if (wasOnGround && !mc.thePlayer.onGround) {
            // 起跳！记录位置
            double px = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
            double py = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
            double pz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
            for (int i = 0; i < ringCount.getValue().intValue(); i++) {
                rings.add(new JumpRing(px, py, pz));
            }
        }
        wasOnGround = mc.thePlayer.onGround;

        // 渲染光环
        long maxAge = duration.getValue().longValue();
        rings.removeIf(r -> System.currentTimeMillis() - r.startTime > maxAge);

        double rx = mc.getRenderManager().renderPosX;
        double ry = mc.getRenderManager().renderPosY;
        double rz = mc.getRenderManager().renderPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        for (JumpRing ring : rings) {
            float progress = (float)(System.currentTimeMillis() - ring.startTime) / maxAge;
            if (progress > 1) continue;
            float radius = progress * 2.5f;  // 逐渐扩大
            float alpha = 1f - progress;     // 逐渐淡出
            Color c = ringColor.getColor();

            wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(ring.x - rx, ring.y - ry, ring.z - rz).color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 0f).endVertex();
            int segs = 24;
            for (int i = 0; i <= segs; i++) {
                double angle = Math.PI * 2 * i / segs;
                wr.pos(ring.x - rx + Math.cos(angle) * radius, ring.y - ry, ring.z - rz + Math.sin(angle) * radius)
                        .color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, alpha).endVertex();
            }
            tess.draw();
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
