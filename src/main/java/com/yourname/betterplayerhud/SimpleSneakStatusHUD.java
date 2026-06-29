package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SimpleSneakStatusHUD {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private SimpleForceSneakHandler sneakHandler;

    public void setSneakHandler(SimpleForceSneakHandler handler) {
        this.sneakHandler = handler;
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!BetterPlayerHUD.config.enableForceSneak || !BetterPlayerHUD.config.showSneakStatus) return;
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (sneakHandler == null) return;

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        renderSneakStatus(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
    }

    private void renderSneakStatus(int screenWidth, int screenHeight) {
        FontRenderer fr = mc.fontRendererObj;

        String statusText = "强制潜行: " + (sneakHandler.isForceSneaking() ? "开" : "关");
        int textWidth = fr.getStringWidth(statusText);
        int textHeight = 10;

        // 计算位置（默认左上角）
        int xPos = BetterPlayerHUD.config.sneakStatusX;
        int yPos = BetterPlayerHUD.config.sneakStatusY;

        // 如果坐标为负数，从右侧/底部计算
        if (xPos < 0) xPos = screenWidth + xPos - textWidth;
        if (yPos < 0) yPos = screenHeight + yPos - textHeight;

        // 绘制状态文本
        fr.drawStringWithShadow(
                statusText,
                xPos,
                yPos,
                sneakHandler.isForceSneaking() ? BetterPlayerHUD.config.sneakStatusColor : 0xFFFF0000
        );
    }
}