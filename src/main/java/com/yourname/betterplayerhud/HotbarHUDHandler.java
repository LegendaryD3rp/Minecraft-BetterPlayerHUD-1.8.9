package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 模块：快捷栏 HUD
 *
 * 取消原版 HOTBAR 渲染，在屏幕底部正中绘制自定义 9 槽快捷栏：
 * - 深色半透明背景槽位
 * - 选中槽位白色边框高亮
 * - 物品图标 + 数量
 * - 物品耐久条
 *
 * P0 版本简洁实现，不做飞入/切换动画。
 */
@SideOnly(Side.CLIENT)
public class HotbarHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── 布局常量 ──
    private static final int SLOT_SIZE = 20;           // 每个槽位像素宽高
    private static final int SLOT_GAP = 1;              // 槽位间距
    private static final int SLOT_TOTAL = SLOT_SIZE + SLOT_GAP;
    private static final int HOTBAR_SLOTS = 9;

    // ── 颜色常量 ──
    private static final int COLOR_SLOT_BG    = 0x66000000;
    private static final int COLOR_SELECTED   = 0xFFFFFFFF;  // 选中边框
    private static final int COLOR_DURABILITY_GREEN = 0xFF00FF00;
    private static final int COLOR_DURABILITY_YELLOW = 0xFFFFFF00;
    private static final int COLOR_DURABILITY_RED    = 0xFFFF0000;

    // ================================================================
    //  Pre — 取消原版快捷栏
    // ================================================================

    @SubscribeEvent
    public void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (event.type == ElementType.HOTBAR) {
            if (BetterPlayerHUD.config.enableHotbarHUD) {
                event.setCanceled(true);
            }
        }
    }

    // ================================================================
    //  Post(TEXT) — 自定义快捷栏渲染
    // ================================================================

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return;
        if (mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.enableHotbarHUD) return;

        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        ScaledResolution sr = event.resolution;
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        int yOff = cfg.hotbarYOffset;

        // 总宽度
        int totalWidth = HOTBAR_SLOTS * SLOT_TOTAL - SLOT_GAP;
        int startX = sw / 2 - totalWidth / 2;
        int baseY = sh - SLOT_SIZE - 2 + yOff;

        ItemStack[] mainInv = mc.thePlayer.inventory.mainInventory;
        int selectedSlot = mc.thePlayer.inventory.currentItem;

        // 先绘制所有槽位背景（避免物品叠在透明背景上时的顺序问题）
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int x = startX + i * SLOT_TOTAL;
            Gui.drawRect(x, baseY, x + SLOT_SIZE, baseY + SLOT_SIZE, COLOR_SLOT_BG);
        }

        // 再绘制选中边框
        int selX = startX + selectedSlot * SLOT_TOTAL;
        Gui.drawRect(selX, baseY, selX + SLOT_SIZE, baseY + 1, COLOR_SELECTED);                         // 上
        Gui.drawRect(selX, baseY + SLOT_SIZE - 1, selX + SLOT_SIZE, baseY + SLOT_SIZE, COLOR_SELECTED);  // 下
        Gui.drawRect(selX, baseY, selX + 1, baseY + SLOT_SIZE, COLOR_SELECTED);                          // 左
        Gui.drawRect(selX + SLOT_SIZE - 1, baseY, selX + SLOT_SIZE, baseY + SLOT_SIZE, COLOR_SELECTED);  // 右

        // 渲染物品
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int x = startX + i * SLOT_TOTAL + 2;  // 2px 内边距
            int y = baseY + 2;
            ItemStack stack = (i < mainInv.length) ? mainInv[i] : null;

            if (stack != null) {
                // 物品图标
                mc.getRenderItem().renderItemIntoGUI(stack, x, y);
                // 物品数量（及耐久条）
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, x, y, null);

                // 耐久条（仅对可损坏物品绘制）
                if (stack.isItemDamaged()) {
                    int maxDmg = stack.getMaxDamage();
                    int curDmg = stack.getItemDamage();
                    float ratio = 1.0f - (float) curDmg / (float) maxDmg;

                    int barX = x;
                    int barY = y + SLOT_SIZE - 4;  // 槽位底部
                    int barW = SLOT_SIZE - 4;       // 留出内边距
                    int barH = 2;

                    // 背景（黑色）
                    Gui.drawRect(barX, barY, barX + barW, barY + barH, 0xFF000000);
                    // 进度条颜色
                    int durColor;
                    if (ratio > 0.6f) {
                        durColor = COLOR_DURABILITY_GREEN;
                    } else if (ratio > 0.3f) {
                        durColor = COLOR_DURABILITY_YELLOW;
                    } else {
                        durColor = COLOR_DURABILITY_RED;
                    }
                    int filledW = (int) (barW * ratio);
                    if (filledW > 0) {
                        Gui.drawRect(barX, barY, barX + filledW, barY + barH, durColor);
                    }
                }
            }
        }

        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
    }
}
