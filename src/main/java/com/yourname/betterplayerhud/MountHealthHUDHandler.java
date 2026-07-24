package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 模块：坐骑血量条 HUD
 *
 * 当玩家骑乘实体时，在屏幕中下方显示坐骑的血量条。
 * 原版 1.8.9 无坐骑血量条，因此不取消任何原版事件。
 * 显示坐骑名称和血量百分比，颜色根据血量变化：
 *   - >60%：绿色
 *   - 30%~60%：黄色
 *   - <30%：红色
 */
@SideOnly(Side.CLIENT)
public class MountHealthHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── 颜色常量 ──
    private static final int COLOR_GREEN  = 0xFF00FF00;
    private static final int COLOR_YELLOW = 0xFFFFFF00;
    private static final int COLOR_RED    = 0xFFFF0000;
    private static final int COLOR_BG     = 0x66000000;
    private static final int COLOR_TEXT   = 0xFFFFFFFF;
    private static final int COLOR_NAME   = 0xFFFFFFFF;

    // ================================================================
    //  Post(TEXT) — 自定义坐骑血量条渲染（不取消原版事件）
    // ================================================================

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.enableMountHP) return;

        Entity riding = mc.thePlayer.ridingEntity;
        if (riding == null) return;

        ScaledResolution sr = event.resolution;
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        int barWidth  = BetterPlayerHUD.config.mountHPBarWidth;
        int barHeight = BetterPlayerHUD.config.mountHPBarHeight;
        int offsetX   = BetterPlayerHUD.config.mountHPX;
        int offsetY   = BetterPlayerHUD.config.mountHPY;

        int barX = sw / 2 - barWidth / 2 + offsetX;
        int barY = sh - 60 + offsetY;

        // 获取坐骑血量
        float health = 1.0f;
        float maxHealth = 1.0f;
        String name = riding.getName();

        if (riding instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) riding;
            health = living.getHealth();
            maxHealth = living.getMaxHealth();
        }

        float ratio = Math.min(1.0f, Math.max(0.0f, health / maxHealth));
        int fillWidth = (int) (barWidth * ratio);

        // 根据血量选择颜色
        int barColor;
        if (ratio > 0.6f) {
            barColor = COLOR_GREEN;
        } else if (ratio > 0.3f) {
            barColor = COLOR_YELLOW;
        } else {
            barColor = COLOR_RED;
        }

        // ── 渲染 ──
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // 背景
        Gui.drawRect(barX, barY, barX + barWidth, barY + barHeight, COLOR_BG);

        // 血量条填充
        if (fillWidth > 0) {
            Gui.drawRect(barX, barY, barX + fillWidth, barY + barHeight, barColor);
        }

        // 坐骑名称（条上方）
        int nameX = barX;
        int nameY = barY - mc.fontRendererObj.FONT_HEIGHT - 2;
        mc.fontRendererObj.drawString(name, nameX, nameY, COLOR_NAME, true);

        // 血量百分比（条右侧）
        String percentText = String.format("%d%%", Math.round(ratio * 100));
        int textX = barX + barWidth + 4;
        int textY = barY + (barHeight - mc.fontRendererObj.FONT_HEIGHT) / 2;
        mc.fontRendererObj.drawString(percentText, textX, textY, COLOR_TEXT, true);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
