package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 模块：调试信息 HUD
 *
 * 取消原版 DEBUG 信息（F3 的 B 面），在屏幕左上角显示简洁调试信息：
 * - FPS
 * - 坐标 XYZ
 * - 内存用量
 *
 * 仅在配置启用时显示，默认关闭。
 */
@SideOnly(Side.CLIENT)
public class DebugInfoHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SHADOW = 0x80000000;

    @SubscribeEvent
    public void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (event.type == ElementType.DEBUG) {
            // 仅当我们的模块启用时取消，否则保留原版 F3
            // 注意：原版 F3 是另一个层，DEBUG ElementType 用于简化调试文本覆盖
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.TEXT) return;
        if (mc.thePlayer == null) return;
        if (mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.enableDebugInfoHUD) return;
        // 如果原版 F3 调试信息显示，不重复叠加
        if (mc.gameSettings.showDebugInfo) return;

        ScaledResolution sr = event.resolution;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        int x = 2 + cfg.debugInfoXOffset;
        int y = 2 + cfg.debugInfoYOffset;

        // FPS
        int fps = mc.getDebugFPS();
        drawLine(x, y, "FPS: " + fps); y += 10;

        // 坐标
        int px = (int) Math.floor(mc.thePlayer.posX);
        int py = (int) Math.floor(mc.thePlayer.posY);
        int pz = (int) Math.floor(mc.thePlayer.posZ);
        drawLine(x, y, "XYZ: " + px + " / " + py + " / " + pz); y += 10;

        // 内存
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long max = rt.maxMemory() / (1024 * 1024);
        drawLine(x, y, "Mem: " + used + "MB / " + max + "MB");
    }

    private void drawLine(int x, int y, String text) {
        mc.fontRendererObj.drawString(text, x + 1, y + 1, COLOR_SHADOW, false);
        mc.fontRendererObj.drawString(text, x, y, COLOR_TEXT, false);
    }
}
