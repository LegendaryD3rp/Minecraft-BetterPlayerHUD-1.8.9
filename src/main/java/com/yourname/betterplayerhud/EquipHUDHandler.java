package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.DecimalFormat;

/**
 * 装备&手持物品 HUD
 *
 * 装甲栏贴在物品栏两侧：
 *   头盔     护腿
 *  [=== 物品栏 ===]
 *   胸甲     靴子
 *
 * 手持物品信息在屏幕左下方：
 *   [图标] 物品名 x数量  |  伤害值
 */
@SideOnly(Side.CLIENT)
public class EquipHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat DF = new DecimalFormat("#.#");

    // ================================================================
    //  事件 — Post(ALL) 确保在 Hotbar 渲染后
    // ================================================================

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return;
        if (mc.gameSettings.hideGUI) return;

        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;

        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();

        if (cfg.enableArmorHUD) {
            renderArmorAroundHotbar(w, h);
        }

        if (cfg.enableHeldItemHUD) {
            renderHeldItemInfo(w, h);
        }

        // 物品栏上方物品数量统计（跟随 enableHeldItemHUD 开关）
        if (cfg.enableHeldItemHUD) {
            renderSlotCountAboveHotbar(w, h);
        }
    }

    // ================================================================
    //  装甲栏：贴在物品栏两侧
    // ================================================================

    private void renderArmorAroundHotbar(int screenWidth, int screenHeight) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        // 物品栏位置（Forge 1.8.9 标准）
        int hotbarY = screenHeight - 22 + cfg.armorYOffset;
        int hotbarLeft = screenWidth / 2 - 91 + cfg.armorXOffset;

        // 装甲槽排序：0=靴子 1=护腿 2=胸甲 3=头盔
        ItemStack[] armor = mc.thePlayer.inventory.armorInventory;

        // 左侧：头盔(上) 胸甲(下)
        // 右侧：护腿(上) 靴子(下)
        int slotSize = 20;
        int gapX = 4;  // 与物品栏的间距
        int gapY = 2;  // 上下间距

        // 左列 X
        int leftX = hotbarLeft - gapX - slotSize;
        // 右列 X
        int rightX = hotbarLeft + 182 + gapX;

        // 头盔（左列，上面）
        renderSlot(armor[3], leftX, hotbarY - slotSize - gapY);
        // 胸甲（左列，下面，与物品栏平齐）
        renderSlot(armor[2], leftX, hotbarY);

        // 护腿（右列，上面）
        renderSlot(armor[1], rightX, hotbarY - slotSize - gapY);
        // 靴子（右列，下面，与物品栏平齐）
        renderSlot(armor[0], rightX, hotbarY);

        if (HUDEditManager.isEditing())
            HUDEditManager.report("装甲栏", leftX, hotbarY - slotSize - gapY, rightX + slotSize - leftX, slotSize * 2 + gapY);
    }

    /** 画一个物品图标 + 耐久数字 */
    private void renderSlot(ItemStack stack, int x, int y) {
        if (stack == null) return;

        // 半透明背景
        Gui.drawRect(x, y, x + 20, y + 20, 0x44000000);

        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x + 2, y + 2);
        mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, x + 2, y + 2);

        // 已损耐久显示：剩余耐久
        if (stack.isItemStackDamageable()) {
            int maxDmg = stack.getMaxDamage();
            int curDmg = stack.getItemDamage();
            int remaining = maxDmg - curDmg;
            if (remaining < maxDmg) {  // 有损耗才显示
                String durStr = String.valueOf(remaining);
                int tw = mc.fontRendererObj.getStringWidth(durStr);
                mc.fontRendererObj.drawStringWithShadow(durStr,
                        x + 20 - tw, y + 20 - mc.fontRendererObj.FONT_HEIGHT, 0xFFFFFFFF);
            }
        }
        RenderHelper.disableStandardItemLighting();
    }

    // ================================================================
    //  手持物品信息：屏幕左下方
    // ================================================================

    private void renderHeldItemInfo(int screenWidth, int screenHeight) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        ItemStack held = mc.thePlayer.getHeldItem();

        int x = 2 + cfg.heldItemXOffset;
        int y = screenHeight - mc.fontRendererObj.FONT_HEIGHT - 2 + cfg.heldItemYOffset;

        if (held != null) {
            // 图标
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(held, x, y - 2);
            mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, held, x, y - 2);
            RenderHelper.disableStandardItemLighting();

            // 文字：物品名
            int tx = x + 20;
            String name = held.getDisplayName();
            mc.fontRendererObj.drawStringWithShadow(name, tx, y + 1, 0xFFFFFFFF);
            tx += mc.fontRendererObj.getStringWidth(name) + 4;

            // 可堆叠物品 → 始终显示数量（如 64）
            if (held.getItem() != null && held.getMaxStackSize() > 1) {
                String countStr = String.valueOf(held.stackSize);
                mc.fontRendererObj.drawStringWithShadow(countStr, tx, y + 1, 0xAAAAAAFF);
                tx += mc.fontRendererObj.getStringWidth(countStr) + 4;
            }

            // 如果是武器，显示伤害 (总伤害 = 武器伤害 + 空手基础 1.0)
            String damageStr = getWeaponDamage(held);
            if (damageStr != null) {
                mc.fontRendererObj.drawStringWithShadow("§7" + damageStr, tx, y + 1, 0xFFFFFFAA);
            }
        }

        // 即使空手也 report，确保 F7 编辑模式下能看到模块
        if (HUDEditManager.isEditing())
            HUDEditManager.report("手持物品", x, y - 2, 200, 12);
    }

    /** 物品栏上方：当前格物品数量 / 背包总计（在物品名正上方） */
    private void renderSlotCountAboveHotbar(int screenWidth, int screenHeight) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        int slot = mc.thePlayer.inventory.currentItem;
        ItemStack held = mc.thePlayer.inventory.getStackInSlot(slot);
        if (held == null) return;

        int stackSize = held.stackSize;

        // 统计背包中相同 item + damage + NBT 的总数
        int total = 0;
        ItemStack[] mainInv = mc.thePlayer.inventory.mainInventory;
        for (ItemStack s : mainInv) {
            if (s != null && s.isItemEqual(held) && ItemStack.areItemStackTagsEqual(s, held)) {
                total += s.stackSize;
            }
        }

        String text = stackSize + "/" + total;
        int tw = mc.fontRendererObj.getStringWidth(text);

        // 原版物品名位置：height-59(创造) / height-45(生存)，放在名字正上方
        int nameY = screenHeight - 59;
        if (!mc.playerController.shouldDrawHUD()) nameY += 14;
        int cx = screenWidth / 2;
        int textY = nameY - mc.fontRendererObj.FONT_HEIGHT - 4 + cfg.slotCountYOffset;

        mc.fontRendererObj.drawStringWithShadow(text, cx - tw / 2, textY, 0xFFFFFFFF);
    }

    /** 获取武器伤害描述，非武器返回 null */
    private String getWeaponDamage(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof ItemSword) {
            // 剑：从属性修饰符读取真实攻击伤害（7.0），不能用 getDamageVsEntity()
            // 那个方法在 MCP 1.8.9 中返回的是 material.getDamageVsEntity()（3.0）
            float baseDmg = getToolDamage(stack) + 1.0f;
            float sharpBonus = getSharpnessBonus(stack);
            return DF.format(baseDmg + sharpBonus);
        }

        if (item instanceof ItemTool && !(item instanceof ItemBow)) {
            // 工具类 (斧/镐/铲)：总伤害 = 属性修饰符 + 空手基础 1.0
            float baseDmg = getToolDamage(stack) + 1.0f;
            float sharpBonus = getSharpnessBonus(stack);
            return DF.format(baseDmg + sharpBonus);
        }

        if (item instanceof ItemBow) {
            // 弓：满蓄力最大伤害估算 10 + Power等级*2
            int powerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
            float maxDmg = 10.0f + powerLevel * 2.0f;
            return "弓 " + DF.format(maxDmg);
        }

        return null;
    }

    /** 计算锋利附魔的额外伤害（1.8.9 MCP 公式：level * 1.25） */
    private static float getSharpnessBonus(ItemStack stack) {
        int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
        if (level <= 0) return 0;
        return level * 1.25f;
    }

    /** 从 ItemTool 的属性修饰符取得武器伤害修正值（不含空手基础 1.0） */
    private static float getToolDamage(ItemStack stack) {
        // 1.8.9 中 ItemTool 没有 getDamageVsEntity，通过属性修饰符取得
        com.google.common.collect.Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier> map =
                stack.getAttributeModifiers();
        java.util.Collection<net.minecraft.entity.ai.attributes.AttributeModifier> mods =
                (java.util.Collection<net.minecraft.entity.ai.attributes.AttributeModifier>)
                        (java.util.Collection<?>) map.get("generic.attackDamage");
        double dmg = 0;
        if (mods != null) {
            for (net.minecraft.entity.ai.attributes.AttributeModifier mod : mods) {
                dmg += mod.getAmount();
            }
        }
        return (float) dmg;
    }
}
