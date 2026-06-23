package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ModeValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ArmorHUDMod extends Mod {

    // 1. 位置控制 (你可以像 LogoMod 那样用滑块，或者接入拖拽系统)
    public NumberValue x = new NumberValue("X", 10.0, 0.0, 1000.0, 1.0);
    public NumberValue y = new NumberValue("Y", 200.0, 0.0, 1000.0, 1.0);

    // 2. 布局模式 (Horizontal: 横排, Vertical: 竖排)
    public ModeValue mode = new ModeValue("Mode", "Horizontal", new String[]{"Horizontal", "Vertical"});

    // 3. 是否显示手持物品
    public BooleanValue showHeldItem = new BooleanValue("ShowHeld", true);

    public ArmorHUDMod() {
        super("ArmorHUD", Category.RENDER); // 也可以放在 DRAW 分类
        addValues(x, y, mode, showHeldItem);
    }

    @Override
    public void draw() {
        // 1. 获取屏幕缩放，确保渲染比例正确
        ScaledResolution sr = new ScaledResolution(mc);

        // 2. 准备要渲染的物品列表
        List<ItemStack> itemsToRender = new ArrayList<>();

        // Minecraft 的装备栏索引：3=头盔, 2=胸甲, 1=裤子, 0=鞋子
        // 我们通常希望从头盔开始画，所以倒序遍历
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.thePlayer.inventory.armorInventory[i];
            if (stack != null) {
                itemsToRender.add(stack);
            }
        }

        // 如果开启了显示手持物品，并且手里有东西
        if (showHeldItem.getValue() && mc.thePlayer.getCurrentEquippedItem() != null) {
            itemsToRender.add(mc.thePlayer.getCurrentEquippedItem());
        }

        // 如果没穿装备也没拿东西，直接不画
        if (itemsToRender.isEmpty()) return;

        // 3. 开始绘制
        GL11.glPushMatrix();

        // 开启物品渲染所需的标准光照 (否则物品会黑黑的)
        RenderHelper.enableGUIStandardItemLighting();

        float currentX = x.getValue().floatValue();
        float currentY = y.getValue().floatValue();

        // 物品之间的间距
        int spacing = 18;

        for (ItemStack stack : itemsToRender) {
            // A. 绘制物品图标
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int)currentX, (int)currentY);

            // B. 绘制物品覆盖层 (数量、耐久条)
            mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, (int)currentX, (int)currentY);

            // C. 移动坐标，为下一个物品腾位置
            if (mode.is("Horizontal")) {
                currentX += spacing; // 横向移动
            } else {
                currentY += spacing; // 纵向移动
            }
        }

        // 关闭光照，恢复状态
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();

        GL11.glPopMatrix();
    }
}