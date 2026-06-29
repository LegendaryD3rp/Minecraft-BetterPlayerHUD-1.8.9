package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RealTimeHUDHandler {

    private Minecraft mc = Minecraft.getMinecraft();
    private SimpleDateFormat dateFormat;
    private long lastFormatUpdate = 0;
    private String currentFormat = "";

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        // 检查是否启用时间显示
        if (!BetterPlayerHUD.config.showRealTimeHUD) {
            return;
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        renderRealTimeHUD(screenWidth, screenHeight);
    }

    private void renderRealTimeHUD(int screenWidth, int screenHeight) {
        FontRenderer fr = mc.fontRendererObj;

        // 获取当前时间
        String timeText = getFormattedTime();

        // 计算文本宽度和高度（多行文本）
        String[] lines = timeText.split("\n");
        int textWidth = 0;
        for (String line : lines) {
            int lineWidth = fr.getStringWidth(line);
            if (lineWidth > textWidth) {
                textWidth = lineWidth;
            }
        }
        int textHeight = lines.length * 10; // 每行约10像素高度

        // 计算位置（默认右下角）
        int xPos = calculateTimeXPosition(screenWidth, textWidth);
        int yPos = calculateTimeYPosition(screenHeight, textHeight);

        // 绘制时间文本（多行支持）
        for (int i = 0; i < lines.length; i++) {
            fr.drawStringWithShadow(
                    lines[i],
                    xPos,
                    yPos + (i * 10), // 每行间隔10像素
                    BetterPlayerHUD.config.timeColor
            );
        }
    }

    private String getFormattedTime() {
        // 检查格式是否更新，避免频繁创建SimpleDateFormat对象
        String configFormat = BetterPlayerHUD.config.timeFormat;
        if (!configFormat.equals(currentFormat) || dateFormat == null) {
            try {
                dateFormat = new SimpleDateFormat(configFormat);
                currentFormat = configFormat;
                lastFormatUpdate = System.currentTimeMillis();
            } catch (IllegalArgumentException e) {
                // 格式错误时使用默认格式
                dateFormat = new SimpleDateFormat("yyyy/M/d\nHH:mm:ss");
                currentFormat = "yyyy/M/d\nHH:mm:ss";
            }
        }

        return dateFormat.format(new Date());
    }

    private int calculateTimeXPosition(int screenWidth, int textWidth) {
        int offset = BetterPlayerHUD.config.timeHudX;

        if (offset >= 0) {
            return offset; // 正数：从左侧计算
        } else {
            return screenWidth + offset - textWidth; // 负数：从右侧计算
        }
    }

    private int calculateTimeYPosition(int screenHeight, int textHeight) {
        int offset = BetterPlayerHUD.config.timeHudY;

        if (offset >= 0) {
            return offset; // 正数：从顶部计算
        } else {
            return screenHeight + offset - textHeight; // 负数：从底部计算
        }
    }
}