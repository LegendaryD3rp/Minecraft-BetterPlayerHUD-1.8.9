package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SpeedHUDHandler {

    private Minecraft mc = Minecraft.getMinecraft();
    private long lastUpdateTime = 0;
    private double smoothedHorizontalSpeed = 0;
    private double smoothedVerticalSpeed = 0;

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        // 检查是否启用速度显示
        if (!BetterPlayerHUD.config.showSpeedHUD) {
            return;
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        renderSpeedHUD(screenWidth, screenHeight);
    }

    private void renderSpeedHUD(int screenWidth, int screenHeight) {
        FontRenderer fr = mc.fontRendererObj;

        // 获取玩家速度信息
        String[] speedTexts = getFormattedSpeed();

        // 计算文本尺寸（多行支持）
        int maxWidth = 0;
        for (String text : speedTexts) {
            int width = fr.getStringWidth(text);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        int textHeight = speedTexts.length * 10;

        // 计算位置
        int xPos = calculateSpeedXPosition(screenWidth, maxWidth);
        int yPos = calculateSpeedYPosition(screenHeight, textHeight);

        // 绘制速度文本（多行支持）
        for (int i = 0; i < speedTexts.length; i++) {
            fr.drawStringWithShadow(
                    speedTexts[i],
                    xPos,
                    yPos + (i * 10),
                    BetterPlayerHUD.config.speedColor
            );
        }
    }

    private String[] getFormattedSpeed() {
        // 计算当前速度
        double horizontalSpeed = Math.sqrt(
                mc.thePlayer.motionX * mc.thePlayer.motionX +
                        mc.thePlayer.motionZ * mc.thePlayer.motionZ
        );

        double verticalSpeed = mc.thePlayer.motionY;

        // 平滑速度显示（避免数字跳动太快）
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000.0f, 0.1f);
        lastUpdateTime = currentTime;

        double smoothingFactor = 0.7; // 平滑系数（0-1，越大越平滑）
        smoothedHorizontalSpeed = smoothedHorizontalSpeed * smoothingFactor +
                horizontalSpeed * (1 - smoothingFactor);
        smoothedVerticalSpeed = smoothedVerticalSpeed * smoothingFactor +
                verticalSpeed * (1 - smoothingFactor);

        // 根据配置单位转换速度
        double displayHorizontalSpeed = convertSpeedUnit(smoothedHorizontalSpeed);
        double displayVerticalSpeed = convertSpeedUnit(smoothedVerticalSpeed);

        // 格式化速度文本
        String formatString = "%." + BetterPlayerHUD.config.speedPrecision + "f";
        String horizontalSpeedText = String.format(formatString, displayHorizontalSpeed);
        String verticalSpeedText = String.format(formatString, displayVerticalSpeed);

        // 构建速度文本数组
        if (BetterPlayerHUD.config.showVerticalSpeed) {
            return new String[]{
                    horizontalSpeedText + " " + BetterPlayerHUD.config.speedUnit,
                    verticalSpeedText + " " + BetterPlayerHUD.config.speedUnit + " (垂直)"
            };
        } else {
            return new String[]{
                    horizontalSpeedText + " " + BetterPlayerHUD.config.speedUnit
            };
        }
    }

    private double convertSpeedUnit(double speedInMetersPerSecond) {
        String unit = BetterPlayerHUD.config.speedUnit.toLowerCase();

        switch (unit) {
            case "km/h":
                return speedInMetersPerSecond * 3.6;
            case "mph":
                return speedInMetersPerSecond * 2.23694;
            case "kn":
                return speedInMetersPerSecond * 1.94384;
            case "ft/s":
                return speedInMetersPerSecond * 3.28084;
            default: // m/s
                return speedInMetersPerSecond;
        }
    }

    private int calculateSpeedXPosition(int screenWidth, int textWidth) {
        int offset = BetterPlayerHUD.config.speedHudX;

        if (offset >= 0) {
            return offset;
        } else {
            return screenWidth + offset - textWidth;
        }
    }

    private int calculateSpeedYPosition(int screenHeight, int textHeight) {
        int offset = BetterPlayerHUD.config.speedHudY;

        if (offset >= 0) {
            return offset;
        } else {
            return screenHeight + offset - textHeight;
        }
    }
}