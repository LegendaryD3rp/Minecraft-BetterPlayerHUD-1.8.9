package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 模块：氧气条 HUD
 *
 * 当玩家在水下时，在屏幕中下方显示自定义氧气条。
 * 取消原版 AIR 渲染，改用自定义样式。
 * 氧气值通过 {@code mc.thePlayer.getAir()} 获取，最大值 300（对应 15 秒）。
 * 条的颜色从蓝色渐变为红色，并在氧气接近耗尽时闪烁提示。
 */
@SideOnly(Side.CLIENT)
public class AirHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── 颜色常量 ──
    private static final int COLOR_BLUE      = 0xFF33AAFF;
    private static final int COLOR_RED       = 0xFFFF3333;
    private static final int COLOR_BG        = 0x66000000;
    private static final int COLOR_TEXT      = 0xFFFFFFFF;

    /** 玩家最大氧气值（300 = 15 秒） */
    private static final int MAX_AIR = 300;

    // ================================================================
    //  Pre — 取消原版氧气条
    // ================================================================

    @SubscribeEvent
    public void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (event.type == ElementType.AIR) {
            if (BetterPlayerHUD.config.enableAirHUD) {
                event.setCanceled(true);
            }
        }
    }

    // ================================================================
    //  Post(TEXT) — 自定义氧气条渲染
    // ================================================================

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.enableAirHUD) return;

        int air = mc.thePlayer.getAir();
        // 氧气满时（300）不显示，只有在水下消耗氧气时才渲染
        if (air >= MAX_AIR) return;

        ScaledResolution sr = event.resolution;
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        int barWidth  = BetterPlayerHUD.config.airBarWidth;
        int barHeight = BetterPlayerHUD.config.airBarHeight;
        int offsetX   = BetterPlayerHUD.config.airHudX;
        int offsetY   = BetterPlayerHUD.config.airHudY;

        int barX = sw / 2 - barWidth / 2 + offsetX;
        int barY = sh - 80 + offsetY;

        float ratio = (float) air / (float) MAX_AIR;
        int fillWidth = (int) (barWidth * ratio);

        // 计算颜色：氧气充足时蓝色，接近耗尽时红色
        int barColor = getAirColor(ratio);

        // ── 渲染 ──
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // 背景
        Gui.drawRect(barX, barY, barX + barWidth, barY + barHeight, COLOR_BG);

        // 氧气条填充
        if (fillWidth > 0) {
            Gui.drawRect(barX, barY, barX + fillWidth, barY + barHeight, barColor);
        }

        // 百分比文字（条右侧）
        String percentText = String.format("%d%%", Math.round(ratio * 100));
        int textX = barX + barWidth + 4;
        int textY = barY + (barHeight - mc.fontRendererObj.FONT_HEIGHT) / 2;
        mc.fontRendererObj.drawString(percentText, textX, textY, COLOR_TEXT, true);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 根据氧气剩余比例返回颜色。
     * 比例 > 0.33（约 5 秒以上）→ 蓝色渐变
     * 比例 <= 0.33（约 5 秒以下）→ 红色渐变
     */
    private static int getAirColor(float ratio) {
        int r, g, b;
        if (ratio > 0.33f) {
            // 蓝色 → 蓝紫过渡
            float t = (ratio - 0.33f) / 0.67f; // 0~1 映射
            r = (int) (0x33 + (0xFF - 0x33) * (1.0f - t));
            g = (int) (0xAA + (0x33 - 0xAA) * (1.0f - t));
            b = 0xFF;
        } else {
            // 蓝色 → 红色（紧急）
            float t = ratio / 0.33f;
            r = 0xFF;
            g = (int) (0x33 * t);
            b = (int) (0xFF * t);
        }
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
