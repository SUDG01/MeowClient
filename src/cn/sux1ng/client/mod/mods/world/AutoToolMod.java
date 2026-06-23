package cn.sux1ng.client.mod.mods.world;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

public class AutoToolMod extends Mod {

    // 剑保护
    public BooleanValue saveSword = new BooleanValue("SwordProtect", true);
    // 吃喝保护
    public BooleanValue eatProtect = new BooleanValue("EatProtect", true);
    // 偏好精准采集工具（挖玻璃/矿石时不切换到无精准采集的工具）
    public BooleanValue preferSilkTouch = new BooleanValue("PreferSilkTouch", false);
    // 最小效率提升阈值（避免为了极小提升频繁换工具）
    public NumberValue minImprovement = new NumberValue("MinImprovement", 0.1, 0.0, 5.0, 0.1);

    private final Minecraft mc = Minecraft.getMinecraft();

    public AutoToolMod() {
        super("AutoTool", Category.WORLD);
        addValues(saveSword, eatProtect, preferSilkTouch, minImprovement);
    }

    @Override
    public void update() {
        if (!isEnable()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // 没按攻击键不处理
        if (!mc.gameSettings.keyBindAttack.isKeyDown()) return;

        // 正在使用物品且开启了保护
        if (eatProtect.getValue() && mc.thePlayer.isUsingItem()) {
            return;
        }

        // 手持剑且开启了保护
        if (saveSword.getValue()) {
            ItemStack heldItem = mc.thePlayer.getHeldItem();
            if (heldItem != null && heldItem.getItem() instanceof ItemSword) {
                return;
            }
        }

        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null) return;

        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();
        if (block == null || block.getMaterial() == Material.air) return;

        int current = mc.thePlayer.inventory.currentItem;
        int best = findBestHotbarSlot(block);

        if (best == -1) return;

        float currentSpeed = getDestroySpeed(mc.thePlayer.inventory.getCurrentItem(), block);
        float bestSpeed    = getDestroySpeed(mc.thePlayer.inventory.mainInventory[best], block);

        if (best != current && bestSpeed > currentSpeed + minImprovement.getValue()) {
            mc.thePlayer.inventory.currentItem = best;
            try {
                mc.playerController.updateController();
            } catch (Throwable t) {
                try {
                    mc.playerController.syncCurrentPlayItem();
                } catch (Throwable ignored) {}
            }
        }
    }

    private float getDestroySpeed(ItemStack stack, Block block) {
        if (stack == null) return 1.0F;
        Item item = stack.getItem();
        if (item == null) return 1.0F;
        try {
            return item.getStrVsBlock(stack, block);
        } catch (Throwable ignored) {
            return 1.0F;
        }
    }

    private int findBestHotbarSlot(Block block) {
        int best = -1;
        float bestSpeed = 1.0F;
        ItemStack[] inv = mc.thePlayer.inventory.mainInventory;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv[i];

            // 如果开启了精准采集偏好，跳过没有精准采集的工具
            if (preferSilkTouch.getValue() && stack != null && stack.getItem() instanceof ItemTool) {
                if (EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, stack) <= 0) {
                    continue; // 无精准采集，跳过此工具
                }
            }

            float speed = getDestroySpeed(stack, block);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = i;
            }
        }
        return best;
    }
}
