package com.yourname.betterplayerhud;

import java.util.LinkedList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeysDisplayHandler {
    private final Minecraft mc = Minecraft.getMinecraft();

    // 按键状态跟踪
    private boolean[] keyStates = new boolean[8]; // 0:LMB, 1:RMB, 2:W, 3:A, 4:S, 5:D, 6:Shift, 7:Space
    private long[] keyPressTimes = new long[8];
    private float[] keyIntensities = new float[8];

    // CPS 跟踪（滑动窗口：记录每次点击的时间戳）
    private final LinkedList<Long> leftClicks = new LinkedList<>();
    private final LinkedList<Long> rightClicks = new LinkedList<>();
    private float leftCps = 0f;
    private float rightCps = 0f;
    private static final long CPS_WINDOW_MS = 1000L;

    // 动画配置
    private static final long PRESS_ANIMATION_DURATION = 200L;
    private static final float MAX_PRESS_INTENSITY = 1.0f;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !BetterPlayerHUD.config.enableKeysDisplay) return;
        if (mc.thePlayer == null) return;

        updateKeyStates();
        updateAnimations();
        updateCps();
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (shouldSkipRendering(event)) return;

        if (!BetterPlayerHUD.config.enableKeysDisplay) {
            if (HUDEditManager.isEditing()) {
                ScaledResolution sr = event.resolution;
                int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
                int ks = BetterPlayerHUD.config.keysSize;
                int ksp = BetterPlayerHUD.config.keysSpacing;
                int dw = 3 * ks + 2 * ksp;
                int dh = 4 * ks + 3 * ksp;
                int offsetX = BetterPlayerHUD.config.keysDisplayX;
                int xPos = offsetX >= 0 ? offsetX : sw + offsetX - dw;
                int offsetY = BetterPlayerHUD.config.keysDisplayY;
                int yPos = offsetY >= 0 ? offsetY : sh + offsetY - dh;
                HUDEditManager.report("按键显示", xPos, yPos, dw, dh);
            }
            return;
        }

        ScaledResolution scaledResolution = event.resolution;
        renderKeysDisplay(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
    }

    private boolean shouldSkipRendering(RenderGameOverlayEvent event) {
        return event.type != RenderGameOverlayEvent.ElementType.TEXT ||
                mc.thePlayer == null ||
                mc.gameSettings.hideGUI;
    }

    private void updateKeyStates() {
        long currentTime = System.currentTimeMillis();

        // 更新鼠标按键状态
        boolean leftPressed = Mouse.isButtonDown(0);
        if (leftPressed != keyStates[0]) {
            keyStates[0] = leftPressed;
            if (leftPressed) {
                keyPressTimes[0] = currentTime;
                leftClicks.addLast(currentTime);
            }
        }

        boolean rightPressed = Mouse.isButtonDown(1);
        if (rightPressed != keyStates[1]) {
            keyStates[1] = rightPressed;
            if (rightPressed) {
                keyPressTimes[1] = currentTime;
                rightClicks.addLast(currentTime);
            }
        }

        // 更新键盘按键状态
        KeyBinding[] keys = {
                mc.gameSettings.keyBindForward,    // W
                mc.gameSettings.keyBindLeft,       // A
                mc.gameSettings.keyBindBack,       // S
                mc.gameSettings.keyBindRight,      // D
                mc.gameSettings.keyBindSneak,      // Shift
                mc.gameSettings.keyBindJump        // Space
        };

        for (int i = 0; i < keys.length; i++) {
            boolean isPressed = Keyboard.isKeyDown(keys[i].getKeyCode());
            int stateIndex = i + 2; // 前两个是鼠标按键

            if (isPressed != keyStates[stateIndex]) {
                keyStates[stateIndex] = isPressed;
                if (isPressed) keyPressTimes[stateIndex] = currentTime;
            }
        }
    }

    private void updateAnimations() {
        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < keyStates.length; i++) {
            if (keyStates[i]) {
                long timeSincePress = currentTime - keyPressTimes[i];
                float progress = Math.min((float) timeSincePress / PRESS_ANIMATION_DURATION, 1.0f);
                keyIntensities[i] = progress * MAX_PRESS_INTENSITY;
            } else {
                if (keyIntensities[i] > 0) {
                    keyIntensities[i] = Math.max(0, keyIntensities[i] - 0.1f);
                }
            }
        }
    }

    /**
     * 滑动窗口 CPS 计算。
     * 每帧去掉超出1秒窗口的旧时间戳，队列长度 = 实时 CPS。
     */
    private void updateCps() {
        long now = System.currentTimeMillis();
        long cutoff = now - CPS_WINDOW_MS;

        while (!leftClicks.isEmpty() && leftClicks.getFirst() < cutoff) {
            leftClicks.removeFirst();
        }
        while (!rightClicks.isEmpty() && rightClicks.getFirst() < cutoff) {
            rightClicks.removeFirst();
        }

        leftCps = leftClicks.size();
        rightCps = rightClicks.size();
    }

    private void renderKeysDisplay(int screenWidth, int screenHeight) {
        // 应用缩放（使用 GlStateManager）
        GlStateManager.pushMatrix();
        float scale = BetterPlayerHUD.config.keysScale;
        GlStateManager.scale(scale, scale, 1.0f);

        // 调整坐标以考虑缩放
        screenWidth = (int)(screenWidth / scale);
        screenHeight = (int)(screenHeight / scale);

        FontRenderer fr = mc.fontRendererObj;
        int keySize = BetterPlayerHUD.config.keysSize;
        int keySpacing = BetterPlayerHUD.config.keysSpacing;

        // 计算显示区域大小
        int displayWidth = 3 * keySize + 2 * keySpacing;
        int displayHeight = 4 * keySize + 3 * keySpacing;

        // 计算位置
        int xPos = calculateDisplayXPosition(screenWidth, displayWidth);
        int yPos = calculateDisplayYPosition(screenHeight, displayHeight);

        // 绘制背景（可选）
        if (BetterPlayerHUD.config.showKeysBackground) {
            drawRoundedRect(xPos - 5, yPos - 5, xPos + displayWidth + 5, yPos + displayHeight + 5, 5, applyAlpha(0x80000000, BetterPlayerHUD.config.keysOpacity));
        }

        // 严格按照指定格式渲染
        renderKeysLayout(xPos, yPos, keySize, keySpacing, fr);

        // 在按键下方显示 CPS
        int leftInt = (int)(leftCps * 10f + 0.5f);
        int rightInt = (int)(rightCps * 10f + 0.5f);
        String cpsText = (leftInt / 10) + "." + (leftInt % 10) + " | " + (rightInt / 10) + "." + (rightInt % 10);
        int cpsWidth = fr.getStringWidth(cpsText);
        int cpsX = xPos + (displayWidth - cpsWidth) / 2;
        int cpsY = yPos + displayHeight + 4;
        int cpsColor = applyAlpha(0xFFAAAAAA, BetterPlayerHUD.config.keysOpacity);
        fr.drawStringWithShadow(cpsText, cpsX, cpsY, cpsColor);

        GlStateManager.popMatrix();

        if (HUDEditManager.isEditing())
            HUDEditManager.report("按键显示", xPos, yPos, displayWidth, cpsY + 10 - yPos);
    }

    private void renderKeysLayout(int startX, int startY, int keySize, int keySpacing, FontRenderer fr) {
        // 计算长按键的宽度（等于A+S+D的总宽度）
        int longKeyWidth = 3 * keySize + 2 * keySpacing;

        // 第一行: LMB W RMB
        int row1Y = startY;
        renderKey(startX, row1Y, keySize, keySize, 0, "LMB", fr); // LMB
        renderKey(startX + keySize + keySpacing, row1Y, keySize, keySize, 2, "W", fr); // W
        renderKey(startX + 2 * (keySize + keySpacing), row1Y, keySize, keySize, 1, "RMB", fr); // RMB

        // 第二行: A S D
        int row2Y = startY + keySize + keySpacing;
        renderKey(startX, row2Y, keySize, keySize, 3, "A", fr); // A
        renderKey(startX + keySize + keySpacing, row2Y, keySize, keySize, 4, "S", fr); // S
        renderKey(startX + 2 * (keySize + keySpacing), row2Y, keySize, keySize, 5, "D", fr); // D

        // 第三行: SHIFT (长按键，与A+S+D总宽度一致)
        int row3Y = startY + 2 * (keySize + keySpacing);
        renderKey(startX, row3Y, longKeyWidth, keySize, 6, "SHIFT", fr); // SHIFT

        // 第四行: SPACE (长按键，与A+S+D总宽度一致)
        int row4Y = startY + 3 * (keySize + keySpacing);
        renderKey(startX, row4Y, longKeyWidth, keySize, 7, "SPACE", fr); // SPACE
    }

    private void renderKey(int x, int y, int width, int height, int keyIndex, String label, FontRenderer fr) {
        boolean isPressed = keyStates[keyIndex];
        float intensity = keyIntensities[keyIndex];
        float opacity = BetterPlayerHUD.config.keysOpacity;

        // 计算按键颜色（乘以全局透明度）
        int keyColor = applyAlpha(calculateKeyColor(isPressed, intensity), opacity);
        int textColor = applyAlpha(BetterPlayerHUD.config.keysTextColor, opacity);

        // 绘制按键背景
        drawRoundedRect(x, y, x + width, y + height, 3, keyColor);

        // 绘制按键标签（居中）
        int textWidth = fr.getStringWidth(label);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;

        fr.drawStringWithShadow(label, textX, textY, textColor);

        // 绘制按下效果
        if (isPressed || intensity > 0) {
            int glowAlpha = (int) (0x40 * intensity * opacity);
            int glowColor = glowAlpha << 24 | (BetterPlayerHUD.config.keysActiveColor & 0xFFFFFF);
            drawRoundedRect(x - 2, y - 2, x + width + 2, y + height + 2, 5, glowColor);
        }
    }

    /** 将颜色 ARGB 的 alpha 通道乘以 opacity (0.0~1.0) */
    private int applyAlpha(int color, float opacity) {
        if (opacity >= 0.999f) return color;
        int a = (int)(((color >> 24) & 0xFF) * opacity);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private int calculateKeyColor(boolean isPressed, float intensity) {
        if (isPressed) {
            int baseColor = BetterPlayerHUD.config.keysActiveColor;
            int intensityValue = (int) (0x55 * intensity);
            return (baseColor & 0xFF000000) |
                    ((Math.min(0xFF, ((baseColor >> 16) & 0xFF) + intensityValue)) << 16) |
                    (baseColor & 0x0000FF00) |
                    ((Math.min(0xFF, ((baseColor >> 0) & 0xFF) + intensityValue)) << 0);
        } else {
            return BetterPlayerHUD.config.keysInactiveColor;
        }
    }

    private int calculateDisplayXPosition(int screenWidth, int displayWidth) {
        int offset = BetterPlayerHUD.config.keysDisplayX;
        if (offset >= 0) {
            return offset;
        } else {
            return screenWidth + offset - displayWidth;
        }
    }

    private int calculateDisplayYPosition(int screenHeight, int displayHeight) {
        int offset = BetterPlayerHUD.config.keysDisplayY;
        if (offset >= 0) {
            return offset;
        } else {
            return screenHeight + offset - displayHeight;
        }
    }

    // 工具方法：绘制矩形（使用 GlStateManager + Tessellator）
    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int temp = left;
            left = right;
            right = temp;
        }
        if (top < bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos((double)left, (double)bottom, 0.0D).endVertex();
        worldRenderer.pos((double)right, (double)bottom, 0.0D).endVertex();
        worldRenderer.pos((double)right, (double)top, 0.0D).endVertex();
        worldRenderer.pos((double)left, (double)top, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // ── 合批渲染：一个 drawRoundedRect = 一次 Tessellator 调用 ──

    /** 向 WorldRenderer 添加一个矩形（4 顶点，GL_QUADS 模式） */
    private void addQuad(WorldRenderer wr, int left, int top, int right, int bottom) {
        wr.pos((double)left,  (double)bottom, 0.0D).endVertex();
        wr.pos((double)right, (double)bottom, 0.0D).endVertex();
        wr.pos((double)right, (double)top,    0.0D).endVertex();
        wr.pos((double)left,  (double)top,    0.0D).endVertex();
    }

    /** 向 WorldRenderer 添加一个圆角象限的像素（批量 1px 矩形） */
    private void addCirclePixels(WorldRenderer wr, int centerX, int centerY, int radius, int quadrant) {
        for (int i = 0; i <= radius; i++) {
            for (int j = 0; j <= radius; j++) {
                if (i * i + j * j <= radius * radius) {
                    int drawX = centerX, drawY = centerY;
                    switch (quadrant) {
                        case 1: drawX = centerX - i; drawY = centerY - j; break;
                        case 2: drawX = centerX + i; drawY = centerY - j; break;
                        case 3: drawX = centerX - i; drawY = centerY + j; break;
                        case 4: drawX = centerX + i; drawY = centerY + j; break;
                    }
                    addQuad(wr, drawX, drawY, drawX + 1, drawY + 1);
                }
            }
        }
    }

    /** 绘制圆角矩形 — 整个用一个 Tessellator 批次完成 */
    private void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        int left   = Math.min(x, width);
        int right  = Math.max(x, width);
        int top    = Math.min(y, height);
        int bottom = Math.max(y, height);

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red   = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8  & 255) / 255.0F;
        float blue  = (float)(color       & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION);

        // 中央矩形区域
        addQuad(wr, left + radius, top,              right - radius, bottom);
        addQuad(wr, left,          top + radius,      right,          bottom - radius);

        // 四个圆角（每个角包含 radius×radius 个 1px 矩形）
        addCirclePixels(wr, left  + radius, top    + radius, radius, 1);
        addCirclePixels(wr, right - radius - 1, top    + radius, radius, 2);
        addCirclePixels(wr, left  + radius, bottom - radius - 1, radius, 3);
        addCirclePixels(wr, right - radius - 1, bottom - radius - 1, radius, 4);

        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // 在CompassMod主类中注册
    public static void register() {
        KeysDisplayHandler handler = new KeysDisplayHandler();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(handler);
    }
}