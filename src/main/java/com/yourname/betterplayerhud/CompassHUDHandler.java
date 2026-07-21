package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CompassHUDHandler {

    private Minecraft mc = Minecraft.getMinecraft();
    private long lastUpdateTime = 0;
    private float smoothYaw = 0;

    /** 地平线字符串缓存（避免每帧 StringBuilder 分配） */
    private static final String HORIZON_LINE;
    static {
        StringBuilder sb = new StringBuilder(40);
        for (int i = 0; i < 40; i++) sb.append('―');
        HORIZON_LINE = sb.toString();
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        if (!BetterPlayerHUD.config.enableCompass) {
            if (HUDEditManager.isEditing()) {
                ScaledResolution sr = event.resolution;
                int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
                int offsetX = BetterPlayerHUD.config.xPosition;
                int xPos = offsetX > 0 ? offsetX : (offsetX < 0 ? sw + offsetX - 240 : (sw - 240) / 2);
                int offsetY = BetterPlayerHUD.config.yPosition;
                int yPos = offsetY >= 0 ? offsetY : sh + offsetY - 40;
                HUDEditManager.report("罗盘", xPos, yPos, 240, 30);
            }
            return;
        }

        ScaledResolution scaledResolution = event.resolution;
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        // 平滑玩家朝向
        float playerYaw = mc.thePlayer.rotationYaw;
        smoothYaw = smoothAngle(smoothYaw, playerYaw);
        float normalizedYaw = (smoothYaw % 360 + 360) % 360;

        // 通过CompassMod.config实例访问配置
        switch(BetterPlayerHUD.config.displayStyle) {
            case BetterPlayerHUDConfig.STYLE_MINIMAL:
                renderMinimalCompass(normalizedYaw, screenWidth, screenHeight, event.partialTicks);
                break;
            case BetterPlayerHUDConfig.STYLE_SIMPLE:
                renderSimpleCompass(normalizedYaw, screenWidth, screenHeight, event.partialTicks);
                break;
            default: // DETAILED
                renderApexStyleCompass(normalizedYaw, screenWidth, screenHeight, event.partialTicks);
        }
    }

    // APEX风格罗盘 - 与F3显示完全一致
    private void renderApexStyleCompass(float yaw, int screenWidth, int screenHeight, float partialTicks) {
        GlStateManager.pushMatrix();

        int xPos = calculateXPosition(screenWidth, 240); // 240 pixels wide, corresponds to 240 degrees of view
        int yPos = calculateYPosition(screenHeight);

        // Access config through the instance
        float scale = BetterPlayerHUD.config.scale;
        if (BetterPlayerHUD.config.dynamicScaling) {
            double speed = Math.sqrt(
                    mc.thePlayer.motionX * mc.thePlayer.motionX +
                            mc.thePlayer.motionZ * mc.thePlayer.motionZ
            );
            scale *= (1.0f + (float)speed * 0.5f);
        }

        GlStateManager.translate((float)(xPos + 120), (float)(yPos + 20), 0.0F);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate((float)-(xPos + 120), (float)-(yPos + 20), 0.0F);

        FontRenderer fr = mc.fontRendererObj;
        int color = BetterPlayerHUD.config.compassColor;

        int centerX = xPos + 120; // 中心位置（玩家当前朝向）
        int compassTopY = yPos;           // 刻度线位置
        int directionY = yPos - 10;        // 方向标记位置（刻度线上方）
        int currentAngleY = yPos + 15;     // 当前角度显示位置

        // 直接使用Minecraft的原始角度（与F3一致）
        float f3Yaw = getF3CompassAngle(yaw);

        // === 1. 绘制刻度线 ===
        if (BetterPlayerHUD.config.showDegreeMarks) {
            int interval = BetterPlayerHUD.config.degreeMarkInterval;

            // 绘制当前视野范围内的刻度（中心±120度）
            for (int deg = 0; deg < 360; deg += interval) {
                // 计算这个刻度相对于玩家当前F3角度的偏移
                float angleDiff = deg - f3Yaw;
                if (angleDiff > 180) angleDiff -= 360;
                if (angleDiff < -180) angleDiff += 360;

                // 只显示在视野范围内的刻度（±120度）
                if (angleDiff >= -120 && angleDiff <= 120) {
                    int markX = centerX + (int)(angleDiff * 2); // 每度2像素

                    if (markX >= xPos && markX <= xPos + 240) {
                        // 绘制刻度线
                        String mark = "|";
                        fr.drawStringWithShadow(mark, markX, compassTopY, 0x888888);

                        // 在特定角度绘制数字
                        if (deg % 30 == 0) {
                            String degText = String.valueOf(deg);
                            int textWidth = fr.getStringWidth(degText);
                            fr.drawStringWithShadow(degText, markX - textWidth/2, compassTopY + 10, 0xAAAAAA);
                        }
                    }
                }
            }
        }

        // === 2. 绘制方向标记（与F3完全一致） ===
        // F3实际方向：0°=南，90°=西，180°=北，270°=东
        String[] directions = {"S", "W", "N", "E"};
        int[] dirAngles = {0, 90, 180, 270};

        for (int i = 0; i < directions.length; i++) {
            // 计算这个方向相对于玩家当前F3角度的偏移
            float angleDiff = dirAngles[i] - f3Yaw;
            if (angleDiff > 180) angleDiff -= 360;
            if (angleDiff < -180) angleDiff += 360;

            // 只显示在视野范围内的方向（±120度）
            if (angleDiff >= -120 && angleDiff <= 120) {
                int dirX = centerX + (int)(angleDiff * 2); // 每度2像素

                if (dirX >= xPos && dirX <= xPos + 240) {
                    String dirText = directions[i];
                    int textWidth = fr.getStringWidth(dirText);

                    // 绘制方向标记（在刻度线上方）
                    fr.drawStringWithShadow(dirText, dirX - textWidth/2, directionY, 0xFFFFFF);
                }
            }
        }

        // === 3. 绘制中心指针和当前角度 ===
        if (BetterPlayerHUD.config.showCompassNeedle) {
            // 绘制红色指针
            fr.drawStringWithShadow(
                    EnumChatFormatting.RED + "▼",
                    centerX - 3,
                    compassTopY - 5,
                    0xFF0000
            );

            // 显示当前精确角度（F3角度）
            if (BetterPlayerHUD.config.showExactAngle) {
                String currentAngle = String.valueOf((int)f3Yaw);
                int angleWidth = fr.getStringWidth(currentAngle);

                // 绘制黄色角度
                fr.drawStringWithShadow(
                        EnumChatFormatting.YELLOW + currentAngle,
                        centerX - angleWidth/2,
                        currentAngleY,
                        0xFFFF00
                );
            }
        }

        // === 4. 绘制地平线（可选） ===
        if (BetterPlayerHUD.config.showHorizon) {
            fr.drawStringWithShadow(HORIZON_LINE, xPos, compassTopY + 5, 0x888888);
        }

        GlStateManager.popMatrix();

        if (HUDEditManager.isEditing()) {
            // 罗盘使用 GlStateManager.scale(scale) 围绕中心 (xPos+120, yPos+20) 缩放
            // 实际渲染尺寸 = 逻辑尺寸 × scale
            float s = BetterPlayerHUD.config.scale;
            int cx = xPos + 120;
            int cy = yPos + 20;
            int actualW = (int)(240 * s);
            int actualH = (int)(30 * s);
            int actualX = cx - (int)(120 * s);
            int actualY = cy - (int)(20 * s);
            HUDEditManager.report("罗盘", actualX, actualY, actualW, actualH);
        }
    }

    // 修正：直接使用Minecraft的角度系统（与F3完全一致）
    private float getF3CompassAngle(float minecraftYaw) {
        // Minecraft F3角度系统：0°=南，90°=西，180°=北，270°=东
        // 直接返回原始角度，确保与F3显示一致
        float f3Angle = minecraftYaw % 360;
        if (f3Angle < 0) f3Angle += 360;
        return f3Angle;
    }

    // 计算基数方向（基于F3实际方向）
    private String getCardinalDirection(float f3Angle) {
        // F3实际方向：0°=南，90°=西，180°=北，270°=东
        if (f3Angle >= 337.5 || f3Angle < 22.5) return "S";  // 南
        if (f3Angle >= 22.5 && f3Angle < 67.5) return "SW";  // 西南
        if (f3Angle >= 67.5 && f3Angle < 112.5) return "W";   // 西
        if (f3Angle >= 112.5 && f3Angle < 157.5) return "NW"; // 西北
        if (f3Angle >= 157.5 && f3Angle < 202.5) return "N";  // 北
        if (f3Angle >= 202.5 && f3Angle < 247.5) return "NE"; // 东北
        if (f3Angle >= 247.5 && f3Angle < 292.5) return "E";  // 东
        return "SE"; // 东南 (292.5-337.5)
    }

    // 简化罗盘样式 - 基于F3实际方向
    private void renderSimpleCompass(float yaw, int screenWidth, int screenHeight, float partialTicks) {
        FontRenderer fr = mc.fontRendererObj;

        // 使用F3角度
        float f3Yaw = getF3CompassAngle(yaw);
        String direction = getCardinalDirection(f3Yaw);

        // 显示方向+F3角度数值
        String displayText = direction + " " + (int)f3Yaw;

        int xPos = calculateXPosition(screenWidth, fr.getStringWidth(displayText));
        int yPos = calculateYPosition(screenHeight);

        fr.drawStringWithShadow(displayText, xPos, yPos, BetterPlayerHUD.config.compassColor);
    }

    // 极简罗盘样式 - 基于F3实际方向
    private void renderMinimalCompass(float yaw, int screenWidth, int screenHeight, float partialTicks) {
        FontRenderer fr = mc.fontRendererObj;

        // 使用F3角度
        float f3Yaw = getF3CompassAngle(yaw);
        String direction = getCardinalDirection(f3Yaw);

        // 显示箭头+方向首字母
        String displayText = "↑" + direction.substring(0, 1);

        int xPos = calculateXPosition(screenWidth, fr.getStringWidth(displayText));
        int yPos = calculateYPosition(screenHeight);

        fr.drawStringWithShadow(displayText, xPos, yPos, BetterPlayerHUD.config.compassColor);
    }

    // 平滑角度变化
    private float smoothAngle(float current, float target) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        if (deltaTime > 0.1f) deltaTime = 0.1f;

        float diff = target - current;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;

        return current + diff * deltaTime * 10;
    }

    private int calculateXPosition(int screenWidth, int elementWidth) {
        int offset = BetterPlayerHUD.config.xPosition;

        if (offset > 0) {
            return offset;
        } else if (offset < 0) {
            return screenWidth + offset - elementWidth;
        } else {
            return (screenWidth - elementWidth) / 2;
        }
    }

    private int calculateYPosition(int screenHeight) {
        int offset = BetterPlayerHUD.config.yPosition;

        if (offset >= 0) {
            return offset;
        } else {
            return screenHeight + offset - 20;
        }
    }
}