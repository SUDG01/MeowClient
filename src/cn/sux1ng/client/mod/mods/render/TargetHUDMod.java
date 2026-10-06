package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.mod.mods.combat.AimbotMod;
import cn.sux1ng.client.targeting.TargetRules;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.ui.MeowTheme;
import cn.sux1ng.client.util.DrawUtil;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import java.awt.Color;
import java.text.DecimalFormat;

public class TargetHUDMod extends Mod {

    public NumberValue x = new NumberValue("X", 100.0, 0.0, 1000.0, 1.0);
    public NumberValue y = new NumberValue("Y", 100.0, 0.0, 1000.0, 1.0);
    public ModeValue style = new ModeValue("Style", "Neon", new String[]{"Neon", "Simple"});

    private double hpWidth = 0;

    public TargetHUDMod() {
        super("TargetHUD", Category.HUD);
        addValues(x, y, style);
    }

    public EntityLivingBase getDisplayTarget() {
        if (mc == null || mc.thePlayer == null || MeowClient.modManager == null) return null;
        KillAuraMod ka = MeowClient.modManager.getByClass(KillAuraMod.class);
        EntityLivingBase target = null;
        if (ka != null && ka.isEnable()) {
            target = ka.getTarget();
        }
        if (target != null && !TargetRules.canRender(target)) target = null;
        AimbotMod aim = MeowClient.modManager.getByClass(AimbotMod.class);
        if (target == null && aim != null && aim.isEnable() && TargetRules.canRender(aim.getTarget())) target = aim.getTarget();
        if (target == null && mc.currentScreen != null) {
            target = mc.thePlayer;
        }
        return target;
    }

    @Override
    public void draw() {
        EntityLivingBase target = getDisplayTarget();
        if (target == null) return;

        if (style.is("Neon")) {
            renderNeon(target);
        } else {
            renderSimple(target);
        }
    }

    private void renderNeon(EntityLivingBase target) {
        MeowTheme.Palette theme = MeowTheme.current();
        float startX = x.getValue().floatValue();
        float startY = y.getValue().floatValue();
        float width = 140;
        float height = 48;

        // 阴影
        DrawUtil.drawRoundedRect(startX + 2, startY + 3, width, height, 8, 0x55000000);

        // 背景 — 圆角 + 微渐变
        DrawUtil.drawRoundedRect(startX, startY, width, height, 8, theme.surface);
        DrawUtil.drawRoundedOutline(startX, startY, width, height, 8, 1, theme.outline);

        // 顶部 accent 渐变
        DrawUtil.drawGradientHorizontal(startX + 4, startY, width - 8, 2,
                theme.accent, theme.secondary);

        // 3D 头像
        GlStateManager.color(1, 1, 1, 1);
        try {
            GuiInventory.drawEntityOnScreen((int)(startX + 22), (int)(startY + 42), 18, target.rotationYaw, target.rotationPitch, target);
        } catch (Exception e) {}

        // 名字
        mc.fontRendererObj.drawStringWithShadow(target.getName(), startX + 42, startY + 8, theme.text);

        // 血量
        DecimalFormat df = new DecimalFormat("0.0");
        String hpStr = df.format(target.getHealth()) + (ClientLanguage.isChinese() ? " 生命" : " HP");
        mc.fontRendererObj.drawStringWithShadow(hpStr, startX + 42, startY + 20, theme.muted);

        // 平滑血条
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float hpPercentage = MathHelper.clamp_float(health / maxHealth, 0, 1);
        double targetWidth = (width - 50) * hpPercentage;
        hpWidth += (targetWidth - hpWidth) * 0.12;  // 平滑

        // 血条背景
        DrawUtil.drawRoundedRect(startX + 42, startY + 34, width - 50, 5, 2.5, theme.outline);
        // 血条前景
        int color = getHealthColor(health, maxHealth);
        DrawUtil.drawRoundedRect(startX + 42, startY + 34, hpWidth, 5, 2.5, color);
    }

    private void renderSimple(EntityLivingBase target) {
        float startX = x.getValue().floatValue();
        float startY = y.getValue().floatValue();
        String text = target.getName() + " §c" + (int)target.getHealth() + "❤";
        int strWidth = mc.fontRendererObj.getStringWidth(text);
        DrawUtil.drawRoundedRect(startX - 3, startY - 3, strWidth + 10, 15, 4,
                MeowTheme.current().surface);
        mc.fontRendererObj.drawStringWithShadow(text, startX + 2, startY,
                MeowTheme.current().text);
    }

    private int getHealthColor(float health, float maxHealth) {
        float p = health / maxHealth;
        // 4段渐变: 绿→黄绿→橙→红
        if (p > 0.66f) {
            float f = (p - 0.66f) / 0.34f;
            return new Color((int)((1-f)*170), 255, 0).getRGB();        // 黄绿(170,255,0) → 绿(0,255,0)
        } else if (p > 0.33f) {
            float f = (p - 0.33f) / 0.33f;
            return new Color(255, (int)(170*f + 255*(1-f)), 0).getRGB(); // 橙(255,170,0) → 黄绿(170,255,0)
        } else {
            float f = p / 0.33f;
            return new Color(255, (int)(170*f), 0).getRGB();             // 红(255,0,0) → 橙(255,170,0)
        }
    }
}
