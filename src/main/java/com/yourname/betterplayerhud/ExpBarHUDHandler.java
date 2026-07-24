package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 模块：经验条 HUD
 *
 * 取消原版 EXPERIENCE 渲染，在屏幕底部中央（快捷栏正上方）绘制自定义经验条：
 * - 深灰色背景条 + 蓝色渐变进度条
 * - 等级文字居中显示在进度条上方
 * - 满级闪烁效果（经验归零时等级文字闪烁）
 *
 * 位置固定屏幕底部中央，提供 X/Y Offset 微调（非拖拽模式）。
 */
@SideOnly(Side.CLIENT)
public class ExpBarHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── 颜色常量 ──
    private static final int COLOR_BG = 0x66000000;
    private static final int COLOR_BAR_START = 0xFF33AAFF;  // 左（亮蓝）
    private static final int COLOR_BAR_END   = 0xFF0066DD;  // 右（深蓝）
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_FLASH = 0xFFFFFF55;      // 闪烁黄白

    // ================================================================
    //  Pre — 取消原版经验条
    // ================================================================

    @SubscribeEvent
    public void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (event.type == ElementType.EXPERIENCE) {
            if (BetterPlayerHUD.config.enableXpBarHUD) {
                event.setCanceled(true);
            }
        }
    }

    // ================================================================
    //  Post(TEXT) — 自定义经验条渲染
    // ================================================================

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.enableXpBarHUD) return;

        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        ScaledResolution sr = event.resolution;
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        float exp = mc.thePlayer.experience;
        int level = mc.thePlayer.experienceLevel;
        int barWidth = cfg.xpBarWidth;
        int barHeight = cfg.xpBarHeight;
        int xOff = cfg.xpBarXOffset;
        int yOff = cfg.xpBarYOffset;

        // 位置：屏幕底部中央，快捷栏正上方（快捷栏默认 Y = sh - 22，经验条在其上方）
        int barX = sw / 2 - barWidth / 2 + xOff;
        int barY = sh - 32 + yOff;  // 快捷栏上方留出间距

        // ── 背景条 ──
        Gui.drawRect(barX, barY, barX + barWidth, barY + barHeight, COLOR_BG);

        // ── 进度条（渐变蓝） ──
        int filledWidth = (int) (barWidth * exp);
        if (filledWidth > 0) {
            drawGradientRect(barX, barY, barX + filledWidth, barY + barHeight,
                    COLOR_BAR_START, COLOR_BAR_END);
        }

        // ── 等级文字 ──
        if (level > 0) {
            String text = String.valueOf(level);
            int textColor = COLOR_TEXT;
            float alpha = 1.0f;

            // 等级 > 0 且经验为 0 时闪烁（满级/经验归零状态）
            if (mc.thePlayer.experience == 0.0f) {
                double tick = mc.thePlayer.ticksExisted;
                alpha = (float) (Math.sin(tick * 0.3) * 0.3 + 0.7);
                textColor = COLOR_FLASH;
            }

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            int textX = barX + barWidth / 2 - mc.fontRendererObj.getStringWidth(text) / 2;
            int textY = barY - mc.fontRendererObj.FONT_HEIGHT - 2;
            // 阴影
            mc.fontRendererObj.drawString(text, textX + 1, textY + 1, 0x80000000, false);
            // 主文字（带透明度）
            int a = (int) (alpha * 255);
            int color = (textColor & 0x00FFFFFF) | (a << 24);
            mc.fontRendererObj.drawString(text, textX, textY, color, false);
            GlStateManager.disableBlend();
        }
    }

    /**
     * 水平渐变矩形绘制（左→右）。
     * 使用 Tessellator 逐顶点着色实现。
     */
    private void drawGradientRect(int left, int top, int right, int bottom,
                                  int startColor, int endColor) {
        float startR = (float)(startColor >> 16 & 255) / 255.0F;
        float startG = (float)(startColor >> 8  & 255) / 255.0F;
        float startB = (float)(startColor       & 255) / 255.0F;
        float startA = (float)(startColor >> 24 & 255) / 255.0F;
        float endR   = (float)(endColor   >> 16 & 255) / 255.0F;
        float endG   = (float)(endColor   >> 8  & 255) / 255.0F;
        float endB   = (float)(endColor         & 255) / 255.0F;
        float endA   = (float)(endColor   >> 24 & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425); // GL_SMOOTH

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        // 左上（startColor）
        wr.pos(left,  top,    0.0).color(startR, startG, startB, startA).endVertex();
        // 左下（startColor）
        wr.pos(left,  bottom, 0.0).color(startR, startG, startB, startA).endVertex();
        // 右下（endColor）
        wr.pos(right, bottom, 0.0).color(endR,   endG,   endB,   endA).endVertex();
        // 右上（endColor）
        wr.pos(right, top,    0.0).color(endR,   endG,   endB,   endA).endVertex();
        tess.draw();

        GlStateManager.shadeModel(7424); // GL_FLAT
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }
}
