package com.yourname.compassmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GameTimeHUDHandler {

    private Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        // 检查是否启用游戏时间显示
        if (!BetterPlayerHUD.config.showGameTimeHUD) {
            return;
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        renderGameTimeHUD(screenWidth, screenHeight);
    }

    private void renderGameTimeHUD(int screenWidth, int screenHeight) {
        FontRenderer fr = mc.fontRendererObj;

        // 获取游戏时间信息
        String[] timeTexts = getFormattedGameTime();

        // 计算文本尺寸（多行支持）
        int maxWidth = 0;
        for (String text : timeTexts) {
            int width = fr.getStringWidth(text);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        int textHeight = timeTexts.length * 10;

        // 计算位置
        int xPos = calculateGameTimeXPosition(screenWidth, maxWidth);
        int yPos = calculateGameTimeYPosition(screenHeight, textHeight);

        // 绘制时间文本（多行支持）
        for (int i = 0; i < timeTexts.length; i++) {
            fr.drawStringWithShadow(
                    timeTexts[i],
                    xPos,
                    yPos + (i * 10),
                    BetterPlayerHUD.config.gameTimeColor
            );
        }
    }

    private String[] getFormattedGameTime() {
        World world = mc.theWorld;
        if (world == null) {
            return new String[]{"世界未加载"};
        }

        // 获取游戏时间（刻数）
        long worldTime = world.getWorldTime();

        // 计算游戏天数（每24000刻=1天）
        long gameDay = (worldTime / 24000) + 1; // Minecraft天数从1开始

        // 计算当天的时间（取模24000）
        long timeOfDay = worldTime % 24000;

        // 将游戏刻转换为24小时制
        // Minecraft时间：0刻=6:00, 6000刻=12:00, 12000刻=18:00, 18000刻=0:00
        double hours = (timeOfDay / 1000.0) + 6; // 加上6小时偏移
        if (hours >= 24) hours -= 24;

        // 转换为12小时制（如果需要）
        if (!BetterPlayerHUD.config.gameTime24Hour) {
            String period = "AM";
            if (hours >= 12) {
                period = "PM";
                if (hours > 12) hours -= 12;
            }
            if (hours == 0) hours = 12;

            if (BetterPlayerHUD.config.showGameDay) {
                return new String[]{
                        String.format("第%d天", gameDay),
                        String.format("%02d:%02d %s", (int)hours, (int)((hours % 1) * 60), period)
                };
            } else {
                return new String[]{
                        String.format("%02d:%02d %s", (int)hours, (int)((hours % 1) * 60), period)
                };
            }
        } else {
            // 24小时制
            if (BetterPlayerHUD.config.showGameDay) {
                return new String[]{
                        String.format("第%d天", gameDay),
                        String.format("%02d:%02d", (int)hours, (int)((hours % 1) * 60))
                };
            } else {
                return new String[]{
                        String.format("%02d:%02d", (int)hours, (int)((hours % 1) * 60))
                };
            }
        }
    }

    private int calculateGameTimeXPosition(int screenWidth, int textWidth) {
        int offset = BetterPlayerHUD.config.gameTimeHudX;

        if (offset >= 0) {
            return offset; // 正数：从左侧计算
        } else {
            return screenWidth + offset - textWidth; // 负数：从右侧计算
        }
    }

    private int calculateGameTimeYPosition(int screenHeight, int textHeight) {
        int offset = BetterPlayerHUD.config.gameTimeHudY;

        if (offset >= 0) {
            return offset; // 正数：从顶部计算
        } else {
            return screenHeight + offset - textHeight; // 负数：从底部计算
        }
    }
}