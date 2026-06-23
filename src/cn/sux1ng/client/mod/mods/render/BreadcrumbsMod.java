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
import java.util.LinkedList;

/**
 * Breadcrumbs — 移动轨迹
 * 保存玩家最近的位置，在世界中画出彩色尾迹
 */
public class BreadcrumbsMod extends Mod {

    public NumberValue maxPoints = new NumberValue("Length", 30, 10, 100, 5);
    public NumberValue fadeTime = new NumberValue("FadeTime", 3000, 1000, 10000, 500);
    public ColorValue trailColor = new ColorValue("Color", new Color(255, 183, 178));

    private final LinkedList<double[]> trail = new LinkedList<>();

    public BreadcrumbsMod() {
        super("Breadcrumbs", Category.RENDER);
        addValues(maxPoints, fadeTime, trailColor);
    }

    @Override
    public void render(float partialTicks) {
        if (mc.thePlayer == null) return;

        // 记录当前位置
        double x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        trail.addFirst(new double[]{x, y, z, System.currentTimeMillis()});

        // 限制长度
        long maxAge = (long) fadeTime.getValue();
        while (trail.size() > maxPoints.getValue() ||
                (trail.size() > 1 && System.currentTimeMillis() - trail.peekLast()[3] > maxAge)) {
            trail.pollLast();
        }
        if (trail.size() < 2) return;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GL11.glLineWidth(2f);

        // 转换到视口坐标
        double rx = mc.getRenderManager().renderPosX;
        double ry = mc.getRenderManager().renderPosY;
        double rz = mc.getRenderManager().renderPosZ;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        float hue = (System.currentTimeMillis() % 5000) / 5000f;
        for (int i = 0; i < trail.size(); i++) {
            double[] pt = trail.get(i);
            float alpha = 1f - (float)(System.currentTimeMillis() - pt[3]) / maxAge;
            if (alpha < 0) alpha = 0;
            float progressHue = (hue + i * 0.02f) % 1f;
            Color c = Color.getHSBColor(progressHue, 0.7f, 1f);
            wr.pos(pt[0] - rx, pt[1] - ry, pt[2] - rz).color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, alpha).endVertex();
        }
        tess.draw();

        GL11.glLineWidth(1f);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
