package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * DamageParticles — 浮动伤害数字
 * 攻击实体时在目标上方显示浮空上升渐隐的伤害数值
 */
public class DamageParticlesMod extends Mod {

    public NumberValue fontSize = new NumberValue("Size", 2.0, 1.0, 5.0, 0.5);
    public NumberValue lifeTime = new NumberValue("Duration", 1500, 500, 5000, 250);
    public ColorValue textColor = new ColorValue("Color", new Color(255, 80, 80));

    private final List<DamageParticle> particles = new ArrayList<>();

    public DamageParticlesMod() {
        super("DamageParticles", Category.RENDER);
        addValues(fontSize, lifeTime, textColor);
    }

    private static class DamageParticle {
        double x, y, z;
        float value;
        long startTime;
        DamageParticle(double x, double y, double z, float v) { this.x = x; this.y = y; this.z = z; this.value = v; this.startTime = System.currentTimeMillis(); }
    }

    @Override
    public void render(float partialTicks) {
        if (mc.thePlayer == null) return;

        // 检测攻击
        if (mc.thePlayer.swingProgress > 0 && mc.thePlayer.swingProgress < 0.1f
                && mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
            float dmg = (float)(1 + Math.random() * 4); // 模拟伤害值（实际伤害需通过数据包获取）
            particles.add(new DamageParticle(
                    mc.objectMouseOver.entityHit.posX,
                    mc.objectMouseOver.entityHit.posY + mc.objectMouseOver.entityHit.height + 0.5,
                    mc.objectMouseOver.entityHit.posZ, dmg));
        }

        // 渲染浮空数字
        long maxAge = (long) lifeTime.getValue();
        particles.removeIf(p -> System.currentTimeMillis() - p.startTime > maxAge);
        if (particles.isEmpty()) return;

        RenderManager rm = mc.getRenderManager();
        DecimalFormat df = new DecimalFormat("0.0");

        for (DamageParticle dp : particles) {
            float progress = (float)(System.currentTimeMillis() - dp.startTime) / maxAge;
            if (progress > 1) continue;

            // 浮空上升
            double drawY = dp.y + progress * 1.5;
            // 渐隐
            int alpha = (int)((1f - progress) * 255);
            if (alpha < 0) alpha = 0;

            // 3D→2D 投影
            double dx = dp.x - rm.renderPosX;
            double dy = drawY - rm.renderPosY;
            double dz = dp.z - rm.renderPosZ;

            GlStateManager.pushMatrix();
            GlStateManager.translate(dx, dy, dz);
            GlStateManager.rotate(-rm.playerViewY, 0, 1, 0);
            GlStateManager.rotate(rm.playerViewX, 1, 0, 0);
            float scale = fontSize.getValue().floatValue() * 0.02f;
            GlStateManager.scale(-scale, -scale, scale);

            String text = df.format(dp.value);
            int w = mc.fontRendererObj.getStringWidth(text) / 2;
            Color c = textColor.getColor();
            mc.fontRendererObj.drawStringWithShadow(text, -w, 0,
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha).getRGB());

            GlStateManager.popMatrix();
        }
    }
}
