package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
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
 * 装甲栏贴在物品栏两侧，每件护甲显示槽位名+附魔+耐久 (cur/max)：
 *   头盔 保护1 (240/250)     护腿 保护1 (235/250)
 *   胸甲 保护1 (230/250)     靴子 保护1 (245/250)
 *  [============= 物品栏 =============]
 *
 * 手持物品信息在屏幕左下方：
 *   [图标] 物品名 x64  |  7.0  (156/1562)
 */
@SideOnly(Side.CLIENT)
public class EquipHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat DF = new DecimalFormat("#.##");

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

        // 物品栏当前栏位物品数量统计
        if (cfg.enableItemCount) {
            if (cfg.showItemCountLeft || cfg.showItemCountRight) {
                renderItemCount(w, h);
            }
        } else if (HUDEditManager.isEditing()) {
            // 禁用时在编辑模式下仍上报 placeholder
            int hotbarLeft = w / 2 - 91;
            int baseX = hotbarLeft + 3 + cfg.itemCountX;
            int baseY = h - 22 - 10 + cfg.itemCountY;
            HUDEditManager.report("物品数量", baseX, baseY, 80, 10);
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

        // ── 左侧（text ← icon）：头盔(上) 胸甲(下)
        renderArmorSlot(armor[3], leftX, hotbarY - slotSize - gapY, "头盔", true);
        renderArmorSlot(armor[2], leftX, hotbarY, "胸甲", true);

        // ── 右侧（icon → text）：护腿(上) 靴子(下)
        renderArmorSlot(armor[1], rightX, hotbarY - slotSize - gapY, "护腿", false);
        renderArmorSlot(armor[0], rightX, hotbarY, "靴子", false);

        if (HUDEditManager.isEditing())
            HUDEditManager.report("装甲栏", leftX, hotbarY - slotSize - gapY, rightX + slotSize - leftX, slotSize * 2 + gapY);
    }

    /** 画一个护甲槽：图标 + 附魔（每行一个）+ 耐久 (cur/max) — 无槽位名 */
    private void renderArmorSlot(ItemStack stack, int x, int y, String slotName, boolean leftSide) {
        if (stack == null) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;

        // 半透明背景（仅限图标区域）
        if (cfg.showArmorBackground) {
            Gui.drawRect(x, y, x + 20, y + 20, 0x44000000);
        }

        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x + 2, y + 2);
        RenderHelper.disableStandardItemLighting();

        // ── 构建附魔行列表（每行一个附魔） ──
        java.util.List<String> lines = new java.util.ArrayList<>();

        if (stack.isItemEnchanted()) {
            java.util.Map<Integer, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
            for (java.util.Map.Entry<Integer, Integer> entry : enchMap.entrySet()) {
                Enchantment ench = Enchantment.getEnchantmentById(entry.getKey());
                if (ench == null) continue;
                lines.add("§b" + ench.getTranslatedName(entry.getValue()));
            }
        }

        // 耐久行
        String durText = "";
        if (stack.isItemStackDamageable()) {
            int maxDmg = stack.getMaxDamage();
            int curDmg = maxDmg - stack.getItemDamage();
            durText = "§7(" + curDmg + "/" + maxDmg + ")";
            lines.add(durText);
        }

        if (lines.isEmpty()) return; // 无附魔也无耐久则只画图标

        int lineHeight = mc.fontRendererObj.FONT_HEIGHT + 1;
        int totalTextH = lines.size() * lineHeight;

        // 文字起始 Y：让文字块垂直居中于图标（图标 20px，文字块从图标中间开始）
        int textStartY = y + (20 - totalTextH) / 2;

        if (leftSide) {
            // 左列：文字在图标左侧，右对齐
            // 先算最宽一行
            int maxW = 0;
            for (String ln : lines) maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(ln));
            int tx = x - 4 - maxW;
            if (tx < 2) tx = 2;
            for (int i = 0; i < lines.size(); i++) {
                int tw = mc.fontRendererObj.getStringWidth(lines.get(i));
                mc.fontRendererObj.drawStringWithShadow(lines.get(i), tx + (maxW - tw), textStartY + i * lineHeight, 0xFFFFFFFF);
            }
        } else {
            // 右列：文字在图标右侧，左对齐
            int tx = x + 22;
            int maxW = 0;
            for (String ln : lines) maxW = Math.max(maxW, mc.fontRendererObj.getStringWidth(ln));
            for (int i = 0; i < lines.size(); i++) {
                mc.fontRendererObj.drawStringWithShadow(lines.get(i), tx, textStartY + i * lineHeight, 0xFFFFFFFF);
            }
        }
    }

    /** 构建物品附魔文本行 — 已弃用，保留兼容 */
    private String buildEnchantmentLine(ItemStack stack) {
        java.util.Map<Integer, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (java.util.Map.Entry<Integer, Integer> entry : enchMap.entrySet()) {
            Enchantment ench = Enchantment.getEnchantmentById(entry.getKey());
            if (ench == null) continue;
            if (!first) sb.append("  ");
            first = false;
            sb.append("§b").append(ench.getTranslatedName(entry.getValue()));
        }
        return sb.toString();
    }

    // ================================================================
    //  手持物品信息：屏幕左下方
    // ================================================================

    private void renderHeldItemInfo(int screenWidth, int screenHeight) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        ItemStack held = mc.thePlayer.getHeldItem();

        int x = 2 + cfg.heldItemXOffset;
        int y = screenHeight - mc.fontRendererObj.FONT_HEIGHT - 2 + cfg.heldItemYOffset;

        int totalH = 12; // 基础行高

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
                tx += mc.fontRendererObj.getStringWidth(" " + damageStr) + 4;
            }

            // 耐久度 (cur/max)
            if (held.isItemStackDamageable()) {
                int maxDmg = held.getMaxDamage();
                int curDmg = maxDmg - held.getItemDamage();
                String durStr = "§7(" + curDmg + "/" + maxDmg + ")";
                mc.fontRendererObj.drawStringWithShadow(durStr, tx, y + 1, 0xAAAAAA);
                tx += mc.fontRendererObj.getStringWidth(durStr) + 4;
            }

            // 附魔显示（第二行起）
            int enchY = y + mc.fontRendererObj.FONT_HEIGHT + 4;
            if (cfg.showHeldItemEnchants && held.isItemEnchanted()) {
                String enchLine = buildEnchantmentLine(held);
                if (enchLine.length() > 0) {
                    mc.fontRendererObj.drawStringWithShadow(enchLine, x + 2, enchY, 0xFFFFFFAA);
                    enchY += mc.fontRendererObj.FONT_HEIGHT + 2;
                    totalH += mc.fontRendererObj.FONT_HEIGHT + 2;
                }
            }
        }

        // 即使空手也 report，确保 F7 编辑模式下能看到模块
        if (HUDEditManager.isEditing())
            HUDEditManager.report("手持物品", x, y - 2, 200, totalH);
    }

    // ================================================================
    //  物品栏当前栏位物品数量
    //  显示格式：[当前格数量]/[背包中该物品总数]，示例：64/128
    //  左右两部分可单独开关
    // ================================================================

    private void renderItemCount(int screenWidth, int screenHeight) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        ItemStack held = mc.thePlayer.getHeldItem();

        // 固定位置计算（无论是否有手持物品都报，F7编辑模式需要）
        int hotbarLeft = screenWidth / 2 - 91;
        int hotbarY = screenHeight - 22;
        int baseX = hotbarLeft + 3 + cfg.itemCountX;  // 略左偏，贴近物品栏
        int baseY = hotbarY - 10 + cfg.itemCountY;
        int totalW = 80; // 默认占位宽度

        if (held == null) {
            HUDEditManager.report("物品数量", baseX, baseY, totalW, 10);
            return;
        }

        // 统计背包+盔甲栏中该物品的总数
        int total = 0;
        ItemStack[] mainInv = mc.thePlayer.inventory.mainInventory;
        for (ItemStack s : mainInv) {
            if (s != null && s.isItemEqual(held) && ItemStack.areItemStackTagsEqual(s, held)) {
                total += s.stackSize;
            }
        }
        for (ItemStack s : mc.thePlayer.inventory.armorInventory) {
            if (s != null && s.isItemEqual(held) && ItemStack.areItemStackTagsEqual(s, held)) {
                total += s.stackSize;
            }
        }

        // ── 低存货颜色过渡（仅对可堆叠物品生效） ──
        String left = String.valueOf(held.stackSize);
        String right = String.valueOf(total);
        boolean isStackable = held.getMaxStackSize() > 1;
        int lowStockColor = 0xFFFFFF; // 默认不变

        if (isStackable) {
            // 以"几组"为单位：2+组 → 安全色，0组 → 危险色
            double stacks = (double) total / (double) held.getMaxStackSize();
            double t = Math.max(0.0, Math.min(1.0, 1.0 - stacks / 2.0));
            // t=0(2组+) → 绿(0xFF55FF55), t=0.5 → 橙(0xFFFFAA55), t=1(0组)→ 红(0xFFFF5555)
            if (t > 0.01) {
                int r, g, b;
                if (t < 0.5) {
                    // 绿→橙
                    double p = t * 2.0;
                    r = (int)(0x55 + (0xFF - 0x55) * p);
                    g = (int)(0xFF - (0xFF - 0xAA) * p);
                    b = (int)(0x55 - (0x55 - 0x55) * p);
                } else {
                    // 橙→红
                    double p = (t - 0.5) * 2.0;
                    r = (int)(0xFF - (0xFF - 0xFF) * p);
                    g = (int)(0xAA - (0xAA - 0x55) * p);
                    b = (int)(0x55 - (0x55 - 0x55) * p);
                }
                lowStockColor = 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            }
        }

        // 组装文本（使用低存货颜色或默认）
        boolean useLowStock = isStackable && (lowStockColor & 0xFFFFFF) != 0xFFFFFF;
        String fullLeft;
        String fullRight;
        int leftColor, rightColor;
        if (useLowStock) {
            fullLeft = left;
            fullRight = right;
            leftColor = lowStockColor;
            rightColor = lowStockColor;
        } else {
            fullLeft = "§e" + left;
            fullRight = "§7" + right;
            leftColor = 0xFFFFFF;
            rightColor = 0xFFFFFF;
        }

        int lw = mc.fontRendererObj.getStringWidth(fullLeft);
        int rw = mc.fontRendererObj.getStringWidth(fullRight);
        int slashW = mc.fontRendererObj.getStringWidth("§8/");
        totalW = lw + (cfg.showItemCountLeft && cfg.showItemCountRight ? slashW : 0) + rw;

        // 位置：物品栏正上方居中 + 偏移（已在前面声明）
        // 始终上报（编辑模式用，运行时也上报以便 F7 打开时已有正确 rect）
        HUDEditManager.report("物品数量", baseX, baseY, totalW + 8, 10);

        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        int tx = baseX + 4;
        if (cfg.showItemCountLeft) {
            mc.fontRendererObj.drawStringWithShadow(fullLeft, tx, baseY, leftColor);
            tx += lw;
        }
        if (cfg.showItemCountLeft && cfg.showItemCountRight) {
            mc.fontRendererObj.drawStringWithShadow("§8/", tx, baseY, 0x888888);
            tx += slashW;
        }
        if (cfg.showItemCountRight) {
            mc.fontRendererObj.drawStringWithShadow(fullRight, tx, baseY, rightColor);
        }

        GlStateManager.disableBlend();
    }

    /** 获取武器伤害描述，非武器返回 null（CompassMod 同款逻辑，硬编码物品ID映射） */
    private String getWeaponDamage(ItemStack stack) {
        Item item = stack.getItem();
        float baseDamage = 0.0f;

        if (item instanceof ItemSword) {
            baseDamage = getSwordBaseDamage((ItemSword) item);
        } else if (item instanceof ItemAxe) {
            baseDamage = getAxeBaseDamage((ItemAxe) item);
        } else if (item instanceof ItemPickaxe) {
            baseDamage = getPickaxeBaseDamage((ItemPickaxe) item);
        } else if (item instanceof ItemSpade) {
            baseDamage = getSpadeBaseDamage((ItemSpade) item);
        } else if (item instanceof ItemBow) {
            int powerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
            return "弓 " + DF.format(10.0f + powerLevel * 2.0f);
        } else {
            return null;
        }

        float sharpnessBonus = getSharpnessBonus(stack);
        return DF.format(baseDamage + sharpnessBonus);
    }

    /** 剑基础伤害（CompassMod 同款硬编码） */
    private static float getSwordBaseDamage(ItemSword sword) {
        switch (Item.getIdFromItem(sword)) {
            case 268: return 4.0f; // 木剑
            case 272: return 5.0f; // 石剑
            case 267: return 6.0f; // 铁剑
            case 276: return 7.0f; // 钻石剑
            case 283: return 4.0f; // 金剑
            default:  return 4.0f;
        }
    }

    /** 斧基础伤害（CompassMod 同款硬编码） */
    private static float getAxeBaseDamage(ItemAxe axe) {
        switch (Item.getIdFromItem(axe)) {
            case 271: return 3.0f; // 木斧
            case 275: return 4.0f; // 石斧
            case 258: return 5.0f; // 铁斧
            case 279: return 6.0f; // 钻石斧
            case 286: return 3.0f; // 金斧
            default:  return 3.0f;
        }
    }

    /** 镐基础伤害（CompassMod 同款硬编码） */
    private static float getPickaxeBaseDamage(ItemPickaxe pickaxe) {
        switch (Item.getIdFromItem(pickaxe)) {
            case 270: return 2.0f; // 木镐
            case 274: return 3.0f; // 石镐
            case 257: return 4.0f; // 铁镐
            case 278: return 5.0f; // 钻石镐
            case 285: return 2.0f; // 金镐
            default:  return 2.0f;
        }
    }

    /** 锹基础伤害（CompassMod 同款硬编码） */
    private static float getSpadeBaseDamage(ItemSpade spade) {
        switch (Item.getIdFromItem(spade)) {
            case 269: return 1.0f; // 木锹
            case 273: return 2.0f; // 石锹
            case 256: return 3.0f; // 铁锹
            case 277: return 4.0f; // 钻石锹
            case 284: return 1.0f; // 金锹
            default:  return 1.0f;
        }
    }

    /** 计算锋利附魔的额外伤害（1.8.9 MCP 公式：level * 1.25） */
    private static float getSharpnessBonus(ItemStack stack) {
        int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
        if (level <= 0) return 0;
        return level * 1.25f;
    }
}
