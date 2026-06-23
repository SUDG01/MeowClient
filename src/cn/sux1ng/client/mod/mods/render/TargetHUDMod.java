package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.MeowClient; // 记得改成你的主类引用
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.text.DecimalFormat;

public class TargetHUDMod extends Mod {

    // 1. 位置和样式
    public NumberValue x = new NumberValue("X", 100.0, 0.0, 1000.0, 1.0);
    public NumberValue y = new NumberValue("Y", 100.0, 0.0, 1000.0, 1.0);
    public ModeValue style = new ModeValue("Style", "Neon", new String[]{"Neon", "Simple"});

    // 用于平滑血条动画的变量
    private double hpWidth = 0;

    public TargetHUDMod() {
        super("TargetHUD", Category.RENDER);
        addValues(x, y, style);
    }

    // 这里我们用 onRender2D 或者 draw 方法 (取决于你的事件系统，通常是 draw)
    @Override
    public void draw() {
        // 1. 获取 KillAura 实例
        KillAuraMod ka = (KillAuraMod) MeowClient.modManager.getByClass(KillAuraMod.class);

        EntityLivingBase target = null;

        // 尝试从 KillAura 获取真实目标
        if (ka != null && ka.isEnable()) {
            target = ka.getTarget();
        }

        // ================== 核心修复：预览功能 ==================
        // 如果当前没有攻击目标，但是你打开了 GUI (比如 ClickGUI, 聊天栏)
        // 我们就把【你自己】当成目标画出来，方便你调整 X 和 Y 位置！
        if (target == null && mc.currentScreen != null) {
            target = mc.thePlayer;
        }
        // ======================================================

        // 如果既没目标，也没开 GUI，那就啥都不画
        if (target == null) return;

        // 3. 开始绘制
        if (style.is("Neon")) {
            renderNeon(target);
        } else {
            renderSimple(target);
        }
    }

    // ================== Neon 风格 (高端黑底 + 动态血条) ==================
    private void renderNeon(EntityLivingBase target) {
        float startX = x.getValue().floatValue();
        float startY = y.getValue().floatValue();
        float width = 140;
        float height = 45;

        // 背景 (半透明黑)
        Gui.drawRect((int)startX, (int)startY, (int)(startX + width), (int)(startY + height), new Color(0, 0, 0, 180).getRGB());

        // 绘制玩家头像 (3D 实体或者是皮肤)
        // 这里为了帅气，我们画一个小的 3D 实体模型
        GlStateManager.color(1, 1, 1, 1);
        try {
            GuiInventory.drawEntityOnScreen((int)(startX + 20), (int)(startY + 40), 18, target.rotationYaw, target.rotationPitch, target);
        } catch (Exception e) {
            // 防止渲染出错崩端
        }

        // 名字
        mc.fontRendererObj.drawStringWithShadow(target.getName(), startX + 40, startY + 6, -1);

        // 血量数值 (例如 15.5 ❤)
        DecimalFormat df = new DecimalFormat("0.0");
        String hpStr = df.format(target.getHealth()) + " HP";
        mc.fontRendererObj.drawStringWithShadow(hpStr, startX + 40, startY + 18, new Color(255, 255, 255).getRGB());

        // --- 平滑血条核心逻辑 ---
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float hpPercentage = MathHelper.clamp_float(health / maxHealth, 0, 1);

        // 目标宽度
        double targetWidth = (width - 45) * hpPercentage;
        // 动画插值 (让当前宽度慢慢接近目标宽度)
        // 0.1 是平滑速度，越大越快
        hpWidth = lerp(hpWidth, targetWidth, 0.1);

        // 血条背景 (灰条)
        Gui.drawRect((int)(startX + 40), (int)(startY + 32), (int)(startX + width - 5), (int)(startY + 36), new Color(60, 60, 60).getRGB());

        // 实际血条 (根据血量变色)
        int color = getHealthColor(health, maxHealth);
        Gui.drawRect((int)(startX + 40), (int)(startY + 32), (int)(startX + 40 + hpWidth), (int)(startY + 36), color);
        // -----------------------
    }

    // ================== Simple 风格 (类似原版) ==================
    private void renderSimple(EntityLivingBase target) {
        float startX = x.getValue().floatValue();
        float startY = y.getValue().floatValue();

        // 简单的名字和血量
        String text = target.getName() + " " + EnumChatFormatting.RED + (int)target.getHealth() + "❤";

        // 背景框
        int strWidth = mc.fontRendererObj.getStringWidth(text);
        Gui.drawRect((int)startX - 2, (int)startY - 2, (int)(startX + strWidth + 2), (int)(startY + 12), new Color(0,0,0,120).getRGB());

        // 文字
        mc.fontRendererObj.drawStringWithShadow(text, startX, startY, -1);
    }

    // ================== 辅助工具 ==================

    // 线性插值 (用于动画)
    private double lerp(double current, double target, double speed) {
        return current + (target - current) * speed;
    }

    // 根据血量获取颜色 (绿 -> 黄 -> 红)
    private int getHealthColor(float health, float maxHealth) {
        float percentage = health / maxHealth;
        // 简单的 RGB 混合
        if (percentage > 0.5f) {
            // 绿 到 黄
            // 0.5 ~ 1.0 -> 0 ~ 1
            float f = (percentage - 0.5f) * 2;
            return new Color((int)((1-f)*255), 255, 0).getRGB();
        } else {
            // 黄 到 红
            float f = percentage * 2;
            return new Color(255, (int)(f*255), 0).getRGB();
        }
    }
}