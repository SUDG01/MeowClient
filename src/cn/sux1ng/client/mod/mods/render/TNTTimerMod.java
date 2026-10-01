package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.util.RenderState;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import java.util.Locale;

public class TNTTimerMod extends Mod {
    public NumberValue range = new NumberValue("Range", 64, 8, 128, 4);
    public NumberValue size = new NumberValue("Size", 1, 0.5, 2, 0.1);
    public TNTTimerMod() { super("TNTTimer", Category.RENDER); addValues(range, size); }

    public static String label(int fuse, float partialTicks) {
        return String.format(Locale.ROOT, "%.1f", Math.max(0, fuse - partialTicks) / 20f)
                + (ClientLanguage.isChinese() ? "秒" : "s");
    }

    @Override public void render(float partialTicks) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        RenderManager rm = mc.getRenderManager();
        try (RenderState state = RenderState.capture()) {
            RenderState.setupWorldEffect();
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityTNTPrimed) || entity.isDead || entity.getDistanceSqToEntity(mc.thePlayer) > range.getValue() * range.getValue()) continue;
                EntityTNTPrimed tnt = (EntityTNTPrimed) entity;
                GlStateManager.pushMatrix();
                GlStateManager.translate(tnt.lastTickPosX + (tnt.posX - tnt.lastTickPosX) * partialTicks - rm.renderPosX,
                        tnt.lastTickPosY + (tnt.posY - tnt.lastTickPosY) * partialTicks - rm.renderPosY + tnt.height + 0.25,
                        tnt.lastTickPosZ + (tnt.posZ - tnt.lastTickPosZ) * partialTicks - rm.renderPosZ);
                GlStateManager.rotate(-rm.playerViewY, 0, 1, 0);
                GlStateManager.rotate(rm.playerViewX * (mc.gameSettings.thirdPersonView == 2 ? -1 : 1), 1, 0, 0);
                float scale = size.getValue().floatValue() * 0.025f;
                GlStateManager.scale(-scale, -scale, scale);
                String text = label(tnt.fuse, partialTicks);
                int width = mc.fontRendererObj.getStringWidth(text);
                DrawUtil.drawRoundedRect(-width / 2.0 - 5, -4, width + 10, 17, 4, MeowTheme.current().surface);
                GlStateManager.enableTexture2D();
                mc.fontRendererObj.drawStringWithShadow(text, -width / 2f, 0,
                        tnt.fuse <= 20 ? 0xFFFF8D9B : MeowTheme.current().text);
                GlStateManager.disableTexture2D();
                GlStateManager.popMatrix();
            }
        }
    }
}
