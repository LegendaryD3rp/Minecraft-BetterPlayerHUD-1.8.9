package com.yourname.compassmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.LinkedList;
import java.util.Queue;

public class CPSHUDHandler {

    private Minecraft mc = Minecraft.getMinecraft();

    // CPS统计相关变量 - 使用更高效的数据结构
    private LinkedList<Long> leftClicks = new LinkedList<>();
    private LinkedList<Long> rightClicks = new LinkedList<>();
    private int leftCPS = 0;
    private int rightCPS = 0;
    private long lastUpdateTime = 0;
    private long lastClickProcessTime = 0;

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (!BetterPlayerHUD.config.showCPSHUD) return;

        long currentTime = System.currentTimeMillis();

        if (event.button == 0 && event.buttonstate) { // 左键按下
            leftClicks.add(currentTime);
        } else if (event.button == 1 && event.buttonstate) { // 右键按下
            rightClicks.add(currentTime);
        }

        // 立即处理点击，减少延迟
        if (currentTime - lastClickProcessTime > 50) { // 每50ms处理一次点击
            cleanupOldClicks(leftClicks, currentTime);
            cleanupOldClicks(rightClicks, currentTime);
            updateCPS();
            lastClickProcessTime = currentTime;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!BetterPlayerHUD.config.showCPSHUD) return;

        long currentTime = System.currentTimeMillis();

        // 更频繁地更新CPS计算
        if (currentTime - lastUpdateTime >= BetterPlayerHUD.config.cpsUpdateInterval) {
            cleanupOldClicks(leftClicks, currentTime);
            cleanupOldClicks(rightClicks, currentTime);
            updateCPS();
            lastUpdateTime = currentTime;
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.showCPSHUD) return;

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        renderCPSHUD(screenWidth, screenHeight);
    }

    private void updateCPS() {
        long currentTime = System.currentTimeMillis();
        long oneSecondAgo = currentTime - 1000;

        // 计算左键CPS（移除过期点击的同时计数）
        int leftCount = 0;
        for (Long clickTime : leftClicks) {
            if (clickTime >= oneSecondAgo) {
                leftCount++;
            }
        }
        leftCPS = leftCount;

        // 计算右键CPS
        int rightCount = 0;
        for (Long clickTime : rightClicks) {
            if (clickTime >= oneSecondAgo) {
                rightCount++;
            }
        }
        rightCPS = rightCount;
    }

    private void cleanupOldClicks(LinkedList<Long> clicks, long currentTime) {
        long oneSecondAgo = currentTime - 1000;

        // 从链表头部移除过期点击（链表按时间顺序排列）
        while (!clicks.isEmpty() && clicks.getFirst() < oneSecondAgo) {
            clicks.removeFirst();
        }
    }

    private void renderCPSHUD(int screenWidth, int screenHeight) {
        FontRenderer fr = mc.fontRendererObj;

        // 构建CPS显示文本
        String leftCPSText = "LCPS: " + leftCPS;
        String rightCPSText = "RCPS: " + rightCPS;

        // 计算文本尺寸
        int leftWidth = fr.getStringWidth(leftCPSText);
        int rightWidth = fr.getStringWidth(rightCPSText);
        int maxWidth = Math.max(leftWidth, rightWidth);
        int totalHeight = 20; // 两行文本的高度

        // 计算位置
        int xPos = calculateCPSXPosition(screenWidth, maxWidth);
        int yPos = calculateCPSYPosition(screenHeight, totalHeight);

        // 直接绘制CPS文本，无背景
        fr.drawStringWithShadow(
                leftCPSText,
                xPos,
                yPos,
                BetterPlayerHUD.config.leftCPSColor
        );

        fr.drawStringWithShadow(
                rightCPSText,
                xPos,
                yPos + 10,
                BetterPlayerHUD.config.rightCPSColor
        );
    }

    private int calculateCPSXPosition(int screenWidth, int textWidth) {
        int offset = BetterPlayerHUD.config.cpsHudX;

        if (offset >= 0) {
            return offset; // 正数：从左侧计算
        } else {
            return screenWidth + offset - textWidth; // 负数：从右侧计算
        }
    }

    private int calculateCPSYPosition(int screenHeight, int textHeight) {
        int offset = BetterPlayerHUD.config.cpsHudY;

        if (offset >= 0) {
            return offset; // 正数：从顶部计算
        } else {
            return screenHeight + offset - textHeight; // 负数：从底部计算
        }
    }
}