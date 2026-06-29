package com.yourname.compassmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemHoe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemInfoHUDHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.hideGUI) {
            return;
        }

        // 检查是否启用物品信息HUD
        if (!BetterPlayerHUD.config.enableItemInfoHUD) {
            return;
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        renderItemInfoHUD(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
    }

    private void renderItemInfoHUD(int screenWidth, int screenHeight) {
        int xPos = BetterPlayerHUD.config.itemHudX;
        int yPos = BetterPlayerHUD.config.itemHudY;
        int currentY = yPos;

        // 绘制背景
        if (BetterPlayerHUD.config.itemShowBackground) {
            drawBackground(xPos, yPos, BetterPlayerHUD.config.itemHudWidth, BetterPlayerHUD.config.itemHudHeight);
        }

        // 显示主手物品数量（独立显示）- 在物品栏上方
        if (BetterPlayerHUD.config.showMainHandCountSeparate) {
            ItemStack mainHand = mc.thePlayer.getHeldItem();
            if (mainHand != null && mainHand.stackSize > 1) {
                FontRenderer fr = mc.fontRendererObj;
                String countText = "数量: " + mainHand.stackSize;
                int countX = BetterPlayerHUD.config.mainHandCountX;
                int countY = BetterPlayerHUD.config.mainHandCountY;

                // 如果坐标为负数，从屏幕右侧/底部计算
                if (countX < 0) countX = screenWidth + countX;
                if (countY < 0) countY = screenHeight + countY;

                int textColor = getCountColor(mainHand.stackSize);
                fr.drawStringWithShadow(countText, countX, countY, textColor);
            }
        }

        // 主手物品详细信息
        if (BetterPlayerHUD.config.showMainHandItem) {
            ItemStack mainHand = mc.thePlayer.getHeldItem();
            if (mainHand != null) {
                currentY = renderItemInfo(mainHand, xPos + 5, currentY + 5, true);
                currentY += 5; // 间距
            }
        }

        // 装备栏物品（1.8.9没有副手，只显示盔甲）
        if (BetterPlayerHUD.config.showArmorItems) {
            renderArmorItems(xPos + 5, currentY);
        }
    }

    private int renderItemInfo(ItemStack stack, int x, int y, boolean isMainHand) {
        FontRenderer fr = mc.fontRendererObj;
        int startY = y;

        List<String> lines = new ArrayList<>();

        // 物品名称
        if (BetterPlayerHUD.config.showItemName) {
            String name = stack.getDisplayName();
            if (BetterPlayerHUD.config.itemUseRarityColors) {
                int rarityColor = getRarityColor(stack);
                lines.add(getRarityFormatting(rarityColor) + name);
            } else {
                lines.add(name);
            }
        }

        // 物品ID/类型（调试用）
        if (BetterPlayerHUD.config.showItemType) {
            String itemId = Item.getIdFromItem(stack.getItem()) + ":" + stack.getItemDamage();
            lines.add(EnumChatFormatting.GRAY + "ID: " + itemId);
        }

        // 物品数量（在详细信息中显示）
        if (BetterPlayerHUD.config.showItemCount && stack.stackSize > 1) {
            lines.add(EnumChatFormatting.GRAY + "堆叠: " + EnumChatFormatting.WHITE + stack.stackSize);
        }

        // 如果手持弓，显示箭的数量
        if (isMainHand && BetterPlayerHUD.config.showArrowCount && stack.getItem() instanceof ItemBow) {
            int arrowCount = countArrows();
            String arrowText = arrowCount > 0 ?
                    EnumChatFormatting.GRAY + "箭矢: " + EnumChatFormatting.WHITE + arrowCount :
                    EnumChatFormatting.RED + "无箭矢";
            lines.add(arrowText);
        }

        // 耐久度信息（与伤害值合并显示）
        String durabilityText = "";
        if (stack.isItemStackDamageable() && BetterPlayerHUD.config.showDurability) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = maxDamage - stack.getItemDamage();
            float durabilityPercent = ((float) currentDamage / maxDamage) * 100;

            String durabilityColor = durabilityPercent <= BetterPlayerHUD.config.itemLowDurabilityThreshold ?
                    EnumChatFormatting.RED.toString() : EnumChatFormatting.GREEN.toString();

            durabilityText = String.format("%s耐久: %d/%d (%.1f%%)",
                    durabilityColor, currentDamage, maxDamage, durabilityPercent);

            // 在这里合并显示伤害
            if (BetterPlayerHUD.config.showItemDamage) {
                String damageText = getItemDamageText(stack);
                if (!damageText.isEmpty()) {
                    // 将伤害信息附加到耐久度文本的末尾
                    // 根据是否有锋利附魔使用不同颜色
                    if (stack.isItemEnchanted() &&
                            EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) > 0) {
                        durabilityText += " | " + EnumChatFormatting.LIGHT_PURPLE + damageText;
                    } else {
                        durabilityText += " | " + EnumChatFormatting.GRAY + damageText;
                    }
                }
            }

            lines.add(durabilityText);

            // 耐久度进度条
            if (BetterPlayerHUD.config.showDurabilityBar) {
                lines.add(renderDurabilityBar(currentDamage, maxDamage));
            }
        } else if (BetterPlayerHUD.config.showItemDamage) {
            // 如果物品没有耐久度，但需要显示伤害，则单独显示伤害
            String damageText = getItemDamageText(stack);
            if (!damageText.isEmpty()) {
                // 根据是否有锋利附魔使用不同颜色
                if (stack.isItemEnchanted() &&
                        EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) > 0) {
                    lines.add(EnumChatFormatting.LIGHT_PURPLE + damageText);
                } else {
                    lines.add(EnumChatFormatting.GRAY + damageText);
                }
            }
        }

        // 附魔信息
        if (stack.isItemEnchanted() && BetterPlayerHUD.config.showEnchantments) {
            Map<Integer, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
            for (Map.Entry<Integer, Integer> entry : enchantments.entrySet()) {
                Enchantment enchant = Enchantment.getEnchantmentById(entry.getKey());
                if (enchant != null) {
                    String enchName = enchant.getTranslatedName(entry.getValue());
                    lines.add(EnumChatFormatting.BLUE + enchName);
                }
            }
        }

        // 物品图标
        if (BetterPlayerHUD.config.showItemIcon) {
            renderItemIcon(stack, x, startY);
        }

        int textX = x + (BetterPlayerHUD.config.showItemIcon ? 20 : 0);

        // 绘制所有文本行
        for (int i = 0; i < lines.size(); i++) {
            fr.drawStringWithShadow(lines.get(i), textX, startY + i * 10, 0xFFFFFF);
        }

        return y + Math.max(20, lines.size() * 10);
    }

    private void renderArmorItems(int x, int y) {
        ItemStack[] armorSlots = mc.thePlayer.inventory.armorInventory;
        String[] slotNames = {"头盔", "胸甲", "护腿", "靴子"};

        int currentY = y;

        for (int i = 3; i >= 0; i--) { // 从头到脚顺序
            ItemStack armor = armorSlots[i];
            if (armor != null && armor.stackSize > 0) {
                currentY = renderArmorPiece(armor, slotNames[i], x, currentY) + 5;
            }
        }
    }

    private int renderArmorPiece(ItemStack armor, String slotName, int x, int y) {
        FontRenderer fr = mc.fontRendererObj;
        int startY = y;

        List<String> lines = new ArrayList<>();

        // 装备名称
        if (BetterPlayerHUD.config.showArmorName) {
            String name = armor.getDisplayName();
            if (BetterPlayerHUD.config.itemUseRarityColors) {
                int rarityColor = getRarityColor(armor);
                lines.add(EnumChatFormatting.GRAY + slotName + ": " + getRarityFormatting(rarityColor) + name);
            } else {
                lines.add(EnumChatFormatting.GRAY + slotName + ": " + EnumChatFormatting.WHITE + name);
            }
        }

        // 耐久度信息（与伤害值合并显示）
        String durabilityText = "";
        if (armor.isItemStackDamageable() && BetterPlayerHUD.config.showArmorDurability) {
            int maxDamage = armor.getMaxDamage();
            int currentDamage = maxDamage - armor.getItemDamage();
            float durabilityPercent = ((float) currentDamage / maxDamage) * 100;

            String durabilityColor = durabilityPercent <= BetterPlayerHUD.config.itemLowDurabilityThreshold ?
                    EnumChatFormatting.RED.toString() : EnumChatFormatting.GREEN.toString();

            durabilityText = String.format("%s耐久: %d/%d (%.1f%%)",
                    durabilityColor, currentDamage, maxDamage, durabilityPercent);

            // 在这里合并显示伤害
            if (BetterPlayerHUD.config.showItemDamage) {
                String damageText = getItemDamageText(armor);
                if (!damageText.isEmpty()) {
                    // 将伤害信息附加到耐久度文本的末尾
                    if (armor.isItemEnchanted() &&
                            EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, armor) > 0) {
                        durabilityText += " | " + EnumChatFormatting.LIGHT_PURPLE + damageText;
                    } else {
                        durabilityText += " | " + EnumChatFormatting.GRAY + damageText;
                    }
                }
            }

            lines.add(durabilityText);
        } else if (BetterPlayerHUD.config.showItemDamage) {
            // 如果装备没有耐久度，但需要显示伤害，则单独显示伤害
            String damageText = getItemDamageText(armor);
            if (!damageText.isEmpty()) {
                if (armor.isItemEnchanted() &&
                        EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, armor) > 0) {
                    lines.add(EnumChatFormatting.LIGHT_PURPLE + damageText);
                } else {
                    lines.add(EnumChatFormatting.GRAY + damageText);
                }
            }
        }

        // 附魔信息
        if (armor.isItemEnchanted() && BetterPlayerHUD.config.showArmorEnchantments) {
            Map<Integer, Integer> enchantments = EnchantmentHelper.getEnchantments(armor);
            for (Map.Entry<Integer, Integer> entry : enchantments.entrySet()) {
                Enchantment enchant = Enchantment.getEnchantmentById(entry.getKey());
                if (enchant != null) {
                    String enchName = enchant.getTranslatedName(entry.getValue());
                    lines.add(EnumChatFormatting.BLUE + enchName);
                }
            }
        }

        // 装备图标
        if (BetterPlayerHUD.config.showArmorIcon) {
            renderItemIcon(armor, x, startY);
        }

        int textX = x + (BetterPlayerHUD.config.showArmorIcon ? 20 : 0);

        // 绘制所有文本行
        for (int i = 0; i < lines.size(); i++) {
            fr.drawStringWithShadow(lines.get(i), textX, startY + i * 10, 0xFFFFFF);
        }

        return y + Math.max(20, lines.size() * 10);
    }

    private void renderItemIcon(ItemStack stack, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();

        // 绘制物品图标
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);

        // 绘制数量（物品图标上的小数字）
        if (stack.stackSize > 1) {
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, x, y, null);
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private String renderDurabilityBar(int current, int max) {
        int barLength = 20;
        int filled = (int)((float)current / max * barLength);
        StringBuilder bar = new StringBuilder();

        bar.append(EnumChatFormatting.GREEN);
        for (int i = 0; i < filled; i++) {
            bar.append("|");
        }

        bar.append(EnumChatFormatting.RED);
        for (int i = filled; i < barLength; i++) {
            bar.append("|");
        }

        return bar.toString();
    }

    private int countArrows() {
        int count = 0;
        for (int i = 0; i < mc.thePlayer.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null) {
                Item item = stack.getItem();
                if (item != null && item.getUnlocalizedName().contains("arrow")) {
                    count += stack.stackSize;
                }
            }
        }
        return count;
    }

    private int getRarityColor(ItemStack stack) {
        // 根据物品的稀有度返回颜色代码
        if (stack.hasDisplayName() && stack.getDisplayName().contains(EnumChatFormatting.LIGHT_PURPLE.toString())) {
            return 13; // 紫色（史诗）
        } else if (stack.isItemEnchanted()) {
            return 6;  // 金色（附魔）
        } else if (stack.getMaxDamage() > 0) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = maxDamage - stack.getItemDamage();
            float durability = (float)currentDamage / maxDamage;

            if (durability > 0.75) return 2;  // 绿色
            else if (durability > 0.5) return 6;  // 金色
            else if (durability > 0.25) return 4;  // 红色
            else return 8;  // 深灰色
        }
        return 15; // 白色（普通）
    }

    private int getCountColor(int count) {
        if (count >= 64) return 0x00FF00; // 绿色（满堆叠）
        else if (count >= 32) return 0xFFFF00; // 黄色
        else if (count >= 16) return 0xFF9900; // 橙色
        else return 0xFF5555; // 红色（数量少）
    }

    private void drawBackground(int x, int y, int width, int height) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float alpha = BetterPlayerHUD.config.itemBackgroundOpacity;
        int color = 0x000000;
        int a = (int)(alpha * 255) << 24;

        // 绘制背景矩形
        drawRect(x, y, x + width, y + height, a | (color & 0xFFFFFF));

        // 绘制边框
        if (BetterPlayerHUD.config.itemShowBorder) {
            int borderColor = BetterPlayerHUD.config.itemBorderColor;
            drawRect(x, y, x + width, y + 1, borderColor); // 上边框
            drawRect(x, y + height - 1, x + width, y + height, borderColor); // 下边框
            drawRect(x, y, x + 1, y + height, borderColor); // 左边框
            drawRect(x + width - 1, y, x + width, y + height, borderColor); // 右边框
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int temp = left;
            left = right;
            right = temp;
        }

        if (top < bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }

        float alpha = (float)((color >> 24) & 255) / 255.0F;
        float red = (float)((color >> 16) & 255) / 255.0F;
        float green = (float)((color >> 8) & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private EnumChatFormatting getRarityFormatting(int rarityColor) {
        switch (rarityColor) {
            case 0: return EnumChatFormatting.BLACK;
            case 1: return EnumChatFormatting.DARK_BLUE;
            case 2: return EnumChatFormatting.DARK_GREEN;
            case 3: return EnumChatFormatting.DARK_AQUA;
            case 4: return EnumChatFormatting.DARK_RED;
            case 5: return EnumChatFormatting.DARK_PURPLE;
            case 6: return EnumChatFormatting.GOLD;
            case 7: return EnumChatFormatting.GRAY;
            case 8: return EnumChatFormatting.DARK_GRAY;
            case 9: return EnumChatFormatting.BLUE;
            case 10: return EnumChatFormatting.GREEN;
            case 11: return EnumChatFormatting.AQUA;
            case 12: return EnumChatFormatting.RED;
            case 13: return EnumChatFormatting.LIGHT_PURPLE;
            case 14: return EnumChatFormatting.YELLOW;
            case 15: return EnumChatFormatting.WHITE;
            default: return EnumChatFormatting.WHITE;
        }
    }

    // === 修复版：物品伤害计算方法（支持附魔伤害显示）===
    private String getItemDamageText(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "";
        }

        Item item = stack.getItem();
        float baseDamage = 0.0f;

        // 1. 获取基础伤害（对于工具类物品）
        if (item instanceof net.minecraft.item.ItemSword) {
            baseDamage = getSwordBaseDamage((net.minecraft.item.ItemSword) item);
        } else if (item instanceof net.minecraft.item.ItemAxe) {
            baseDamage = getAxeBaseDamage((net.minecraft.item.ItemAxe) item);
        } else if (item instanceof net.minecraft.item.ItemPickaxe) {
            baseDamage = getPickaxeBaseDamage((net.minecraft.item.ItemPickaxe) item);
        } else if (item instanceof net.minecraft.item.ItemSpade) {
            baseDamage = getSpadeBaseDamage((net.minecraft.item.ItemSpade) item);
        } else if (item instanceof net.minecraft.item.ItemHoe) {
            // 锄头本身没有基础伤害，但允许显示附魔伤害
            baseDamage = 0.0f;
        } else if (item instanceof net.minecraft.item.ItemTool) {
            // 其他工具
            baseDamage = 2.0f;
        }
        // 注意：普通物品（如木棍）基础伤害为0，但允许显示附魔伤害

        // 2. 计算锋利附魔加成
        float sharpnessBonus = 0.0f;
        if (stack.isItemEnchanted()) {
            int sharpnessLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
            if (sharpnessLevel > 0) {
                // 1.8.9版本锋利附魔公式：每级+1.25伤害
                sharpnessBonus = sharpnessLevel * 1.25f;
            }
        }

        // 3. 计算总伤害
        float totalDamage = baseDamage + sharpnessBonus;

        // 4. 判断是否显示：只要有伤害（基础伤害或附魔伤害）就显示
        if (totalDamage <= 0 && sharpnessBonus <= 0) {
            return "";
        }

        // 5. 格式化输出
        if (BetterPlayerHUD.config.showDamageBreakdown && sharpnessBonus > 0) {
            if (baseDamage > 0) {
                // 有基础伤害和附魔
                return String.format("伤害: %.2f (基%.1f+锋%.2f)",
                        totalDamage, baseDamage, sharpnessBonus);
            } else {
                // 只有附魔伤害（如附魔锄头、木棍等）
                return String.format("伤害: %.2f (仅锋%.2f)",
                        totalDamage, sharpnessBonus);
            }
        } else {
            // 简洁模式
            return String.format("伤害: %.2f", totalDamage);
        }
    }

    // 辅助方法：获取剑的基础伤害（保持不变）
    private float getSwordBaseDamage(ItemSword sword) {
        try {
            java.lang.reflect.Field field = ItemSword.class.getDeclaredField("toolMaterial");
            field.setAccessible(true);
            net.minecraft.item.Item.ToolMaterial material = (net.minecraft.item.Item.ToolMaterial) field.get(sword);

            if (material != null) {
                return 4.0f + material.getDamageVsEntity();
            }
        } catch (Exception e) {
            // 如果反射失败，回退到通过物品ID判断
        }

        int itemId = Item.getIdFromItem(sword);
        switch (itemId) {
            case 268: return 4.0f; // 木剑
            case 272: return 5.0f; // 石剑
            case 267: return 6.0f; // 铁剑
            case 276: return 7.0f; // 钻石剑
            case 283: return 4.0f; // 金剑
            default: return 4.0f;  // 默认
        }
    }

    // 辅助方法：获取斧的基础伤害（保持不变）
    private float getAxeBaseDamage(ItemAxe axe) {
        int itemId = Item.getIdFromItem(axe);
        switch (itemId) {
            case 271: return 3.0f; // 木斧
            case 275: return 4.0f; // 石斧
            case 258: return 5.0f; // 铁斧
            case 279: return 6.0f; // 钻石斧
            case 286: return 3.0f; // 金斧
            default: return 3.0f;  // 默认
        }
    }

    // 辅助方法：获取镐的基础伤害（保持不变）
    private float getPickaxeBaseDamage(ItemPickaxe pickaxe) {
        int itemId = Item.getIdFromItem(pickaxe);
        switch (itemId) {
            case 270: return 2.0f; // 木镐
            case 274: return 3.0f; // 石镐
            case 257: return 4.0f; // 铁镐
            case 278: return 5.0f; // 钻石镐
            case 285: return 2.0f; // 金镐
            default: return 2.0f;  // 默认
        }
    }

    // 辅助方法：获取锹的基础伤害（保持不变）
    private float getSpadeBaseDamage(ItemSpade spade) {
        int itemId = Item.getIdFromItem(spade);
        switch (itemId) {
            case 269: return 1.0f; // 木锹
            case 273: return 2.0f; // 石锹
            case 256: return 3.0f; // 铁锹
            case 277: return 4.0f; // 钻石锹
            case 284: return 1.0f; // 金锹
            default: return 1.0f;  // 默认
        }
    }
}