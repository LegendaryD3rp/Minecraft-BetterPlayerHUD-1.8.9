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
 * 模块：跳跃蓄力条 HUD
 *
 * 当玩家按住空格键且在地面上时，屏幕中央偏下位置显示一条蓄力条。
 * 蓄力从 0 到 1 循环，每次 tick 增加 chargeSpeed（默认 0.02）。
 * 颜色从绿色渐变到黄色再到红色，表示蓄力程度。
 */
@SideOnly(Side.CLIENT)
public class JumpBarHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /** 当前蓄力值 [0.0 ~ 1.0] */
    private float charge = 0.0f;

    // ── 默认颜色常量（当无法通过配置获取时使用） ──
    private static final int COLOR_GREEN  = 0xFF00FF00;
    private static final int COLOR_YELLOW = 0xFFFFFF00;
    private static final int COLOR_RED    = 0xFFFF0000;
    private static final int COLOR_BG     = 0x66000000;

    // ================================================================
    //  Pre — 取消原版 JUMPBAR（1.8.9 中不存在，仅做安全检查）
    // ================================================================

    @SubscribeEvent
    public void onPreRender(RenderGameOverlayEvent.Pre event) {
        // 1.8.9 Forge 无 ElementType.JUMPBAR，此方法仅保留框架
        // 若 enableJumpBar 为 false 则不做任何操作
        if (!BetterPlayerHUD.config.enableJumpBar) {
            return;
        }
    }

    // ================================================================
    //  Post(TEXT) — 自定义跳跃蓄力条渲染
    // ================================================================

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.enableJumpBar) return;

        // 检测条件：在地面上且按住空格
        if (!mc.thePlayer.onGround) return;
        if (!mc.gameSettings.keyBindJump.isKeyDown()) return;

        // 每 tick 增加蓄力
        charge += BetterPlayerHUD.config.jumpBarChargeSpeed;
        if (charge >= 1.0f) {
            charge = 0.0f;
        }

        ScaledResolution sr = event.resolution;
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        int barWidth  = BetterPlayerHUD.config.jumpBarWidth;
        int barHeight = BetterPlayerHUD.config.jumpBarHeight;
        int barX = sw / 2 - barWidth / 2;
        int barY = sh - 50;

        // 计算渐变色
        int barColor = getChargeColor(charge);

        // ── 绘制 ──
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // 背景
        Gui.drawRect(barX, barY, barX + barWidth, barY + barHeight, COLOR_BG);

        // 蓄力条填充
        int fillWidth = (int) (barWidth * charge);
        if (fillWidth > 0) {
            Gui.drawRect(barX, barY, barX + fillWidth, barY + barHeight, barColor);
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 根据蓄力进度返回渐变色：0→绿色，0.5→黄色，1.0→红色
     */
    private static int getChargeColor(float progress) {
        int r, g, b;
        if (progress < 0.5f) {
            // 绿色 → 黄色
            float t = progress / 0.5f;
            r = (int) (0x00 + (0xFF - 0x00) * t);
            g = 0xFF;
            b = (int) (0x00 + (0x00 - 0x00) * t);
        } else {
            // 黄色 → 红色
            float t = (progress - 0.5f) / 0.5f;
            r = 0xFF;
            g = (int) (0xFF - (0xFF - 0x00) * t);
            b = 0x00;
        }
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
