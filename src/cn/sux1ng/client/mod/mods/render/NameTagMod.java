package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.RenderState;
import cn.sux1ng.client.targeting.TargetRules;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper; // 导入附魔助手
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class NameTagMod extends Mod {
    public ModeValue style = new ModeValue("Style", "Skeet", new String[]{"Skeet", "Outline", "Rect", "Clear"});
    public NumberValue scaleValue = new NumberValue("Size", 1.0, 0.5, 2.0, 0.1);
    public NumberValue heightValue = new NumberValue("Height", 0.3, -0.5, 1.0, 0.1);
    public NumberValue opacity = new NumberValue("Opacity", 100.0, 0.0, 255.0, 5.0)
            .setVisibility(() -> !style.is("Clear") && !style.is("Outline"));

    public NameTagMod() {
        super("NameTag", Category.RENDER);
        addValues(style, scaleValue, heightValue, opacity);
    }

    @Override
    public void render(float partialTicks) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        try (RenderState state = RenderState.capture()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (TargetRules.canRender(entity, true, false, false, true)) {
                    EntityLivingBase livingEntity = (EntityLivingBase) entity;
                    double x = livingEntity.lastTickPosX + (livingEntity.posX - livingEntity.lastTickPosX) * partialTicks - mc.getRenderManager().renderPosX;
                    double y = livingEntity.lastTickPosY + (livingEntity.posY - livingEntity.lastTickPosY) * partialTicks - mc.getRenderManager().renderPosY;
                    double z = livingEntity.lastTickPosZ + (livingEntity.posZ - livingEntity.lastTickPosZ) * partialTicks - mc.getRenderManager().renderPosZ;
                    renderNameTag(livingEntity, x, y, z, partialTicks);
                }
            }
        }
    }

    private void renderNameTag(EntityLivingBase entity, double x, double y, double z, float partialTicks) {
        int health = Math.round(entity.getHealth());
        String hpColor = getHealthColor(health, entity.getMaxHealth());

        // --- 核心修改 1: 装备对比逻辑 ---
        String prefix = "";

        // 只有当对象是玩家时才比较装备 (怪物没有附魔保护的概念)
        if (entity instanceof EntityPlayer) {
            int myScore = getDefenseScore(mc.thePlayer);
            int targetScore = getDefenseScore((EntityPlayer) entity);

            // 如果对面分比我高，加个红色的警告符号
            if (targetScore > myScore) {
                prefix = EnumChatFormatting.DARK_RED + "⚠ " + EnumChatFormatting.RESET;
            }
        }

        // 把警告符号加在名字最前面
        String displayTag = prefix + entity.getDisplayName().getFormattedText() + " " + hpColor + health;
        // -------------------------------

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entity.height + heightValue.getValue(), z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX * (mc.gameSettings.thirdPersonView == 2 ? -1 : 1),
                1.0F, 0.0F, 0.0F);

        float distance = mc.thePlayer.getDistanceToEntity(entity);
        float baseScale = (distance / 375f) * scaleValue.getValue().floatValue();
        float minScale = 0.026f * scaleValue.getValue().floatValue();
        if (baseScale < minScale) baseScale = minScale;

        GlStateManager.scale(-baseScale, -baseScale, baseScale);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableFog();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        FontRenderer font = mc.fontRendererObj;
        int width = font.getStringWidth(displayTag) / 2;
        int colorInt = getHealthColorInt(health, entity.getMaxHealth());

        String currentStyle = style.getValue();
        int bgAlpha = opacity.getValue().intValue();
        int bgColor = new Color(0, 0, 0, bgAlpha).getRGB();
        int blackColor = new Color(0, 0, 0, 255).getRGB();

        if (currentStyle.equals("Skeet")) {
            drawRect(-width - 2, -2, width + 2, 10, bgColor);
            drawRect(-width - 2, -3, width + 2, -2, colorInt);
        }
        else if (currentStyle.equals("Outline")) {
            drawRect(-width - 3, -3, width + 3, -2, blackColor);
            drawRect(-width - 3, 10, width + 3, 11, blackColor);
            drawRect(-width - 3, -2, -width - 2, 10, blackColor);
            drawRect(width + 2, -2, width + 3, 10, blackColor);
        }
        else if (currentStyle.equals("Rect")) {
            drawRect(-width - 2, -2, width + 2, 10, bgColor);
        }

        drawRect(-width - 2, 10, width + 2, 12, 0x60000000);

        float hpPercentage = entity.getHealth() / entity.getMaxHealth();
        if (hpPercentage > 1) hpPercentage = 1;

        drawRect(-width - 2, 10, (int) (-width - 2 + ((width * 2 + 4) * hpPercentage)), 12, colorInt);

        font.drawString(displayTag, -width, 0, -1);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    // --- 核心修改 2: 计算防御评分的方法 ---
    private int getDefenseScore(EntityPlayer player) {
        int score = 0;

        // 1. 加上基础护甲值 (Total Armor Value)
        score += player.getTotalArmorValue();

        // 2. 加上保护附魔等级 (Protection Level)
        // 遍历 4 个装备槽 (头、胸、腿、脚)
        for (ItemStack stack : player.inventory.armorInventory) {
            if (stack != null) {
                // 获取保护附魔等级 (附魔ID 0 是保护 Protection)
                int protLevel = EnchantmentHelper.getEnchantmentLevel(0, stack);
                score += protLevel;
            }
        }
        return score;
    }
    // --------------------------------------

    private String getHealthColor(float health, float maxHealth) {
        float percentage = health / maxHealth;
        if (percentage > 0.75) return EnumChatFormatting.GREEN.toString();
        if (percentage > 0.5) return EnumChatFormatting.YELLOW.toString();
        if (percentage > 0.25) return EnumChatFormatting.RED.toString();
        return EnumChatFormatting.DARK_RED.toString();
    }

    private int getHealthColorInt(float health, float maxHealth) {
        float percentage = health / maxHealth;
        if (percentage > 0.75) return new Color(0, 255, 0, 255).getRGB();
        if (percentage > 0.5) return new Color(255, 255, 0, 255).getRGB();
        if (percentage > 0.25) return new Color(255, 0, 0, 255).getRGB();
        return new Color(139, 0, 0, 255).getRGB();
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) { int i = left; left = right; right = i; }
        if (top < bottom) { int j = top; top = bottom; bottom = j; }
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
        worldrenderer.pos((double)left, (double)bottom, 0.0D).endVertex();
        worldrenderer.pos((double)right, (double)bottom, 0.0D).endVertex();
        worldrenderer.pos((double)right, (double)top, 0.0D).endVertex();
        worldrenderer.pos((double)left, (double)top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
