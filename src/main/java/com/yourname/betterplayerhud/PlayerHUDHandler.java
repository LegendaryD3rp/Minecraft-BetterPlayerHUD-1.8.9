package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class PlayerHUDHandler {

    private Minecraft mc = Minecraft.getMinecraft();
    private long lastBlinkTime = 0;
    private boolean blinkState = false;

    /**
     * Pre 取消：抢在原版渲染之前血量（HEALTH）和饱食度（FOOD）
     * 前提：模块启用且对应子开关打开时，原版不再绘制。
     */
    @SubscribeEvent
    public void onPreRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.HEALTH) {
            if (BetterPlayerHUD.config.enableHealthHUD && BetterPlayerHUD.config.showHealthHUD) {
                event.setCanceled(true);
            }
        }
        if (event.type == RenderGameOverlayEvent.ElementType.FOOD) {
            if (BetterPlayerHUD.config.enableHealthHUD && BetterPlayerHUD.config.showHungerHUD) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        if (!BetterPlayerHUD.config.enableHealthHUD) {
            if (HUDEditManager.isEditing()) {
                ScaledResolution sr = event.resolution;
                int sh = sr.getScaledHeight();
                int hudTop = BetterPlayerHUD.config.healthHudY >= 0
                        ? BetterPlayerHUD.config.healthHudY
                        : sh + BetterPlayerHUD.config.healthHudY - 25;
                HUDEditManager.report("状态栏", BetterPlayerHUD.config.healthHudX, hudTop, 200, 30);
            }
            return;
        }

        ScaledResolution scaledResolution = event.resolution;
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        // 渲染饱食度HUD（在护甲栏正上方）
        if (BetterPlayerHUD.config.showHungerHUD) {
            renderHungerHUD(screenWidth, screenHeight);
        }

        // 渲染护甲HUD（紧贴饱食度下方）
        if (BetterPlayerHUD.config.showArmorHUD && mc.thePlayer.getTotalArmorValue() > 0) {
            renderArmorHUD(screenWidth, screenHeight);
        }

        // 渲染血量HUD
        if (BetterPlayerHUD.config.showHealthHUD) {
            renderPlayerHUD(screenWidth, screenHeight);
        }
    }

    private void renderPlayerHUD(int screenWidth, int screenHeight) {
        String username = mc.thePlayer.getGameProfile().getName();

        // 直接获取玩家当前血量，包含所有效果和修改
        float health = mc.thePlayer.getHealth();
        float maxHealth = mc.thePlayer.getMaxHealth();

        int displayHealth = (int)(health * 5); // 转换为100基准

        int hudLeft = BetterPlayerHUD.config.healthHudX;
        int hudTop;

        if (BetterPlayerHUD.config.healthHudY >= 0) {
            hudTop = BetterPlayerHUD.config.healthHudY;
        } else {
            hudTop = screenHeight + BetterPlayerHUD.config.healthHudY - 25;
        }

        int barWidth = 100;
        int barHeight = 5;
        int backgroundColor = 0xFF555555;
        int outlineColor = 0x66000000; // 半透明黑色描边

        // 计算头像相关尺寸
        int headSize = BetterPlayerHUD.config.showPlayerHead ? BetterPlayerHUD.config.headSize : 0;
        int headSpacing = BetterPlayerHUD.config.showPlayerHead ? BetterPlayerHUD.config.headTextSpacing : 0;
        int totalLeftOffset = headSize + headSpacing;

        // 调整血条位置（如果有头像则向右偏移）
        int adjustedHudLeft = hudLeft + totalLeftOffset;

        // 绘制描边（上下左右各扩展1像素）
        drawRect(adjustedHudLeft - 1, hudTop - 1, adjustedHudLeft + barWidth + 1, hudTop, outlineColor); // 上描边
        drawRect(adjustedHudLeft - 1, hudTop + barHeight, adjustedHudLeft + barWidth + 1, hudTop + barHeight + 1, outlineColor); // 下描边
        drawRect(adjustedHudLeft - 1, hudTop, adjustedHudLeft, hudTop + barHeight, outlineColor); // 左描边
        drawRect(adjustedHudLeft + barWidth, hudTop, adjustedHudLeft + barWidth + 1, hudTop + barHeight, outlineColor); // 右描边

        // 绘制背景
        drawRect(adjustedHudLeft, hudTop, adjustedHudLeft + barWidth, hudTop + barHeight, backgroundColor);

        // 计算血量比例（用于进度条和闪烁效果）
        float healthPercentage = health / maxHealth;

        // 闪烁效果逻辑
        int barColor = getHealthColorWithBlink(healthPercentage);

        // 血条样式
        String style = BetterPlayerHUD.config.healthBarStyle;
        boolean isModern = "modern".equals(style);
        boolean isPixel = "pixel".equals(style);

        // 只有在非闪烁状态或闪烁状态为显示时才绘制血量条
        if (barColor != 0) {
            int currentWidth = (int)(barWidth * healthPercentage);
            if (isModern) {
                // 现代样式：渐变 + 更高 + 内发光
                int barHeightModern = barHeight + 2;
                // 背景
                drawRect(adjustedHudLeft, hudTop - 1, adjustedHudLeft + barWidth, hudTop - 1 + barHeightModern, 0xFF333333);
                // 血量部分（渐变）
                for (int w = 0; w < currentWidth; w++) {
                    float t = (float) w / barWidth;
                    int r = (int)((0xFF & (barColor >> 16)) * (1 - t * 0.3f));
                    int g = (int)((0xFF & (barColor >> 8)) * (1 - t * 0.3f));
                    int b = (int)(0xFF & barColor);
                    int gradColor = (0xFF << 24) | (r << 16) | (g << 8) | b;
                    drawRect(adjustedHudLeft + w, hudTop - 1, adjustedHudLeft + w + 1, hudTop - 1 + barHeightModern, gradColor);
                }
                // 内发光（顶部高光线）
                drawRect(adjustedHudLeft, hudTop - 1, adjustedHudLeft + currentWidth, hudTop, 0x55FFFFFF);
            } else if (isPixel) {
                // 像素样式：块状
                int pixelSize = 4;
                int pixels = currentWidth / pixelSize;
                int barHeightPixel = barHeight + 2;
                drawRect(adjustedHudLeft, hudTop - 1, adjustedHudLeft + barWidth, hudTop - 1 + barHeightPixel, 0xFF222222);
                for (int p = 0; p < pixels; p++) {
                    int px = adjustedHudLeft + p * pixelSize;
                    int color = (p % 2 == 0) ? barColor : lightenColor(barColor, 0.8f);
                    drawRect(px, hudTop - 1, px + pixelSize - 1, hudTop - 1 + barHeightPixel, color);
                }
            } else {
                // 默认样式（原有逻辑）
                drawRect(adjustedHudLeft, hudTop, adjustedHudLeft + currentWidth, hudTop + barHeight, barColor);
            }
        }

        FontRenderer fr = mc.fontRendererObj;

        // 只显示当前血量数值
        String healthText = String.valueOf(displayHealth);

        int textX = adjustedHudLeft + barWidth + 5;
        int textY = hudTop + (barHeight - 8) / 2;

        // 血量文本也参与闪烁
        int textColor = getTextColorWithBlink(healthPercentage);
        if (textColor != 0) {
            fr.drawStringWithShadow(healthText, textX, textY, textColor);
        }

        // 绘制玩家头像（如果启用）
        if (BetterPlayerHUD.config.showPlayerHead) {
            renderPlayerHead(hudLeft, hudTop, headSize);
        }

        // 玩家名（不参与闪烁）
        int nameY = hudTop + barHeight + 2;
        int nameX = adjustedHudLeft; // 名称与血条左对齐

        fr.drawStringWithShadow(username, nameX, nameY, 0xAAAAAA);

        if (HUDEditManager.isEditing())
            HUDEditManager.report("状态栏", hudLeft, hudTop, barWidth + headSize + headSpacing + 20, barHeight + 20);
    }

    /**
     * 渲染玩家头像
     */
    private void renderPlayerHead(int x, int y, int size) {
        try {
            ResourceLocation skin = mc.thePlayer.getLocationSkin();
            if (skin == null) return;

            // 绑定皮肤纹理
            mc.getTextureManager().bindTexture(skin);

            // 设置OpenGL状态（使用 GlStateManager，避免 raw GL 绕过缓存）
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            // 保存当前矩阵
            GlStateManager.pushMatrix();

            // 皮肤纹理中头像的UV坐标（8x8像素的头像区域）
            float uMin = 8.0F / 64.0F;
            float uMax = 16.0F / 64.0F;
            float vMin = 8.0F / 64.0F;
            float vMax = 16.0F / 64.0F;

            // 绘制头像（正面部分）
            drawTexturedModalRect(x, y, size, size, uMin, vMin, uMax, vMax);

            // 绘制头像 overlay（帽子层）
            float overlayUMin = 40.0F / 64.0F;
            float overlayUMax = 48.0F / 64.0F;
            drawTexturedModalRect(x, y, size, size, overlayUMin, vMin, overlayUMax, vMax);

            // 恢复状态
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();

        } catch (Exception e) {
            // 如果渲染头像失败，不影响其他部分的显示
            // 注：bindTexture/enableBlend/pushMatrix 任意一步抛异常都会导致 OpenGL 状态残留，
            // 但实际中这些操作极少抛异常，且 Mod 在下一帧会被重新正常绘制，不影响整体稳定性。
            System.err.println("[PlayerHUD] 渲染玩家头像失败: " + e.getMessage());
        }
    }

    /**
     * 绘制纹理矩形（支持UV坐标）
     */
    private void drawTexturedModalRect(int x, int y, int width, int height, float uMin, float vMin, float uMax, float vMax) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height, 0.0D).tex(uMin, vMax).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).tex(uMax, vMax).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).tex(uMax, vMin).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(uMin, vMin).endVertex();
        tessellator.draw();
    }

    // ═══════════════════════════════════════════════════════════════
    //  饱食度HUD（护甲栏正上方）
    // ═══════════════════════════════════════════════════════════════
    private void renderHungerHUD(int screenWidth, int screenHeight) {
        // 位置与护甲栏对齐，正上方紧贴
        int hudLeft = BetterPlayerHUD.config.healthHudX;
        int armorY;
        if (BetterPlayerHUD.config.healthHudY >= 0) {
            armorY = BetterPlayerHUD.config.healthHudY - 10;
        } else {
            armorY = screenHeight + BetterPlayerHUD.config.healthHudY - 35;
        }

        int barWidth = 100;
        int barHeight = 5;
        int gap = 2;

        int hungerY = armorY - barHeight - gap;

        // 头像偏移
        int headSize = BetterPlayerHUD.config.showPlayerHead ? BetterPlayerHUD.config.headSize : 0;
        int headSpacing = BetterPlayerHUD.config.showPlayerHead ? BetterPlayerHUD.config.headTextSpacing : 0;
        int totalLeftOffset = headSize + headSpacing;
        int adjustedHudLeft = hudLeft + totalLeftOffset;

        // 饥饿度: 食物等级 * 5（最大20级 → 100）
        int foodLevel = Math.min(mc.thePlayer.getFoodStats().getFoodLevel(), 20);
        int displayHunger = foodLevel * 5;

        // 饱食度(Saturation): 0~foodLevel 的隐藏缓冲值
        float saturation = mc.thePlayer.getFoodStats().getSaturationLevel();
        int displaySaturation = Math.min((int)(saturation * 5), displayHunger);

        // 描边
        int outline = 0x66000000;
        drawRect(adjustedHudLeft - 1, hungerY - 1, adjustedHudLeft + barWidth + 1, hungerY, outline);
        drawRect(adjustedHudLeft - 1, hungerY + barHeight, adjustedHudLeft + barWidth + 1, hungerY + barHeight + 1, outline);
        drawRect(adjustedHudLeft - 1, hungerY, adjustedHudLeft, hungerY + barHeight, outline);
        drawRect(adjustedHudLeft + barWidth, hungerY, adjustedHudLeft + barWidth + 1, hungerY + barHeight, outline);

        // 背景
        drawRect(adjustedHudLeft, hungerY, adjustedHudLeft + barWidth, hungerY + barHeight, 0xFF555555);

        // 橙色条（foodLevel）
        int foodW = (int)(barWidth * (displayHunger / 100.0f));
        drawRect(adjustedHudLeft, hungerY, adjustedHudLeft + foodW, hungerY + barHeight, BetterPlayerHUD.config.hungerColor);

        // 金色半透明叠层（saturationLevel，盖在food bar上面）
        if (displaySaturation > 0) {
            int satW = (int)(barWidth * (displaySaturation / 100.0f));
            drawRect(adjustedHudLeft, hungerY, adjustedHudLeft + satW, hungerY + barHeight, 0x80FFD700);
        }

        // 数值：food / sat
        FontRenderer fr = mc.fontRendererObj;
        String hungerText = displayHunger + "§7/§e" + displaySaturation;
        fr.drawStringWithShadow(hungerText,
                adjustedHudLeft + barWidth + 5,
                hungerY + (barHeight - 8) / 2,
                0xFFFFFF);
    }

    private void renderArmorHUD(int screenWidth, int screenHeight) {
        // 获取玩家护甲值（转换为0-100范围）
        int armorValue = mc.thePlayer.getTotalArmorValue(); // 原始值0-20
        int displayArmor = armorValue * 5; // 转换为0-100

        // 计算护甲条位置（在血量条正上方）
        int hudLeft = BetterPlayerHUD.config.healthHudX;
        int hudTop;

        if (BetterPlayerHUD.config.healthHudY >= 0) {
            hudTop = BetterPlayerHUD.config.healthHudY - 10; // 在血量条上方10像素
        } else {
            hudTop = screenHeight + BetterPlayerHUD.config.healthHudY - 35; // 调整位置
        }

        int barWidth = 100;
        int barHeight = 5;
        int backgroundColor = 0xFF555555;
        int outlineColor = 0x66000000; // 半透明黑色描边

        // 计算头像偏移（如果显示头像）
        int headSize = BetterPlayerHUD.config.showPlayerHead ? BetterPlayerHUD.config.headSize : 0;
        int headSpacing = BetterPlayerHUD.config.showPlayerHead ? BetterPlayerHUD.config.headTextSpacing : 0;
        int totalLeftOffset = headSize + headSpacing;
        int adjustedHudLeft = hudLeft + totalLeftOffset;

        // 绘制描边（上下左右各扩展1像素）
        drawRect(adjustedHudLeft - 1, hudTop - 1, adjustedHudLeft + barWidth + 1, hudTop, outlineColor); // 上描边
        drawRect(adjustedHudLeft - 1, hudTop + barHeight, adjustedHudLeft + barWidth + 1, hudTop + barHeight + 1, outlineColor); // 下描边
        drawRect(adjustedHudLeft - 1, hudTop, adjustedHudLeft, hudTop + barHeight, outlineColor); // 左描边
        drawRect(adjustedHudLeft + barWidth, hudTop, adjustedHudLeft + barWidth + 1, hudTop + barHeight, outlineColor); // 右描边

        // 绘制背景
        drawRect(adjustedHudLeft, hudTop, adjustedHudLeft + barWidth, hudTop + barHeight, backgroundColor);

        // 计算护甲比例
        float armorPercentage = Math.min(displayArmor / 100.0f, 1.0f);

        // 直接使用天蓝色，无闪烁效果
        int barColor = BetterPlayerHUD.config.armorColor;

        // 绘制护甲条
        int currentWidth = (int)(barWidth * armorPercentage);
        drawRect(adjustedHudLeft, hudTop, adjustedHudLeft + currentWidth, hudTop + barHeight, barColor);

        FontRenderer fr = mc.fontRendererObj;

        // 显示护甲数值（白色，无闪烁）
        String armorText = String.valueOf(displayArmor);
        int textX = adjustedHudLeft + barWidth + 5;
        int textY = hudTop + (barHeight - 8) / 2;
        fr.drawStringWithShadow(armorText, textX, textY, 0xFFFFFF);
    }

    // 带闪烁功能的血量颜色获取
    private int getHealthColorWithBlink(float percentage) {
        int baseColor = getHealthColor(percentage);

        if (percentage > 0.3f) {
            return baseColor;
        }

        updateBlinkState(percentage);

        if (!blinkState) {
            return 0;
        }

        return baseColor;
    }

    // 带闪烁功能的文本颜色获取
    private int getTextColorWithBlink(float percentage) {
        if (percentage > 0.3f) {
            return 0xFFFFFF;
        }

        updateBlinkState(percentage);

        if (!blinkState) {
            return 0;
        }

        if (percentage > 0.1f) {
            return 0xFFFF00;
        } else {
            return 0xFF0000;
        }
    }

    // 闪烁状态更新逻辑
    private void updateBlinkState(float percentage) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastBlinkTime;

        long blinkInterval;
        if (percentage <= 0.1f) {
            blinkInterval = 200;
        } else {
            blinkInterval = 500;
        }

        if (timeDiff > blinkInterval) {
            blinkState = !blinkState;
            lastBlinkTime = currentTime;
        }
    }

    // 原有的颜色获取方法
    private int getHealthColor(float percentage) {
        if (percentage > 0.7f) {
            return BetterPlayerHUD.config.healthColorSafe;
        } else if (percentage > 0.3f) {
            return BetterPlayerHUD.config.healthColorWarning;
        } else {
            return BetterPlayerHUD.config.healthColorDanger;
        }
    }

    /** 提亮颜色 */
    private int lightenColor(int color, float factor) {
        int a = color & 0xFF000000;
        int r = Math.min(255, (int)(((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int)(((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int)((color & 0xFF) * factor));
        return a | (r << 16) | (g << 8) | b;
    }

    // 绘制矩形的方法（使用 GlStateManager + Tessellator，避免 raw GL 绕过缓存）
    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (color == 0) return;

        int temp;
        if (left < right) {
            temp = left;
            left = right;
            right = temp;
        }
        if (top < bottom) {
            temp = top;
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
}