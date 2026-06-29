package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SprintStatusHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private ForceSprintHandler forceSprintHandler;

    public void setForceSprintHandler(ForceSprintHandler handler) {
        this.forceSprintHandler = handler;
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.enableForceSprint || !BetterPlayerHUD.config.showSprintStatus) return;

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        renderSprintStatus(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
    }

    private void renderSprintStatus(int screenWidth, int screenHeight) {
        if (forceSprintHandler == null) return;

        FontRenderer fr = mc.fontRendererObj;

        // 直接从处理器获取实际状态，而不是从配置
        boolean isEnabled = forceSprintHandler.isSprintForced();

        // 构建状态文本
        String statusText = "强制疾跑: " + (isEnabled ? "开" : "关");
        int textWidth = fr.getStringWidth(statusText);
        int textHeight = 10;

        // 应用缩放（使用 GlStateManager）
        GlStateManager.pushMatrix();
        float scale = BetterPlayerHUD.config.sprintStatusScale;
        GlStateManager.scale(scale, scale, 1.0f);

        // 调整坐标以考虑缩放
        screenWidth = (int)(screenWidth / scale);
        screenHeight = (int)(screenHeight / scale);

        // 计算位置
        int xPos = calculateSprintStatusXPosition(screenWidth, textWidth);
        int yPos = calculateSprintStatusYPosition(screenHeight, textHeight);

        // 绘制状态文本
        fr.drawStringWithShadow(
                statusText,
                xPos,
                yPos,
                isEnabled ? BetterPlayerHUD.config.sprintStatusColor : 0xFF0000
        );

        GlStateManager.popMatrix();
    }

    private int calculateSprintStatusXPosition(int screenWidth, int textWidth) {
        int offset = BetterPlayerHUD.config.sprintStatusX;
        if (offset >= 0) {
            return offset;
        } else {
            return screenWidth + offset - textWidth;
        }
    }

    private int calculateSprintStatusYPosition(int screenHeight, int textHeight) {
        int offset = BetterPlayerHUD.config.sprintStatusY;
        if (offset >= 0) {
            return offset;
        } else {
            return screenHeight + offset - textHeight;
        }
    }
}