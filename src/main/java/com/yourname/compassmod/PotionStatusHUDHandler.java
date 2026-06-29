package com.yourname.compassmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class PotionStatusHUDHandler {

    private static final ResourceLocation POTION_ICONS = new ResourceLocation("textures/gui/container/inventory.png");
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        if (!BetterPlayerHUD.config.showPotionHUD) {
            return;
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        renderPotionHUD(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
    }

    private void renderPotionHUD(int screenWidth, int screenHeight) {
        Collection<PotionEffect> activePotions = mc.thePlayer.getActivePotionEffects();
        if (activePotions.isEmpty()) {
            return;
        }

        List<PotionEffect> potionsToDisplay = filterAndSortPotions(activePotions);
        if (potionsToDisplay.isEmpty()) {
            return;
        }

        GlStateManager.pushMatrix();
        float scale = BetterPlayerHUD.config.potionScale;
        GlStateManager.scale(scale, scale, 1.0f);

        screenWidth = (int)(screenWidth / scale);
        screenHeight = (int)(screenHeight / scale);

        int maxWidth = calculateMaxWidth(potionsToDisplay);
        int totalHeight = calculateTotalHeight(potionsToDisplay);

        int xPos = calculatePotionXPosition(screenWidth, maxWidth);
        int yPos = calculatePotionYPosition(screenHeight, totalHeight);

        if (BetterPlayerHUD.config.showPotionBackground) {
            drawRect(xPos - 2, yPos - 2, xPos + maxWidth + 2, yPos + totalHeight + 2, BetterPlayerHUD.config.potionBackgroundColor);
        }

        renderPotionEffects(potionsToDisplay, xPos, yPos);

        GlStateManager.popMatrix();
    }

    private List<PotionEffect> filterAndSortPotions(Collection<PotionEffect> potions) {
        List<PotionEffect> filtered = new ArrayList<>();

        for (PotionEffect effect : potions) {
            if (BetterPlayerHUD.config.showOnlyActivePotions && effect.getDuration() <= 0) {
                continue;
            }

            if (filtered.size() >= BetterPlayerHUD.config.maxPotionDisplay) {
                break;
            }

            filtered.add(effect);
        }

        if (BetterPlayerHUD.config.sortByDuration) {
            filtered.sort(Comparator.comparingInt(PotionEffect::getDuration));
        }

        return filtered;
    }

    private int calculateMaxWidth(List<PotionEffect> potions) {
        FontRenderer fr = mc.fontRendererObj;
        int maxWidth = 0;

        for (PotionEffect effect : potions) {
            String displayText = getPotionDisplayText(effect);
            int textWidth = fr.getStringWidth(displayText) + BetterPlayerHUD.config.potionTextOffset + 5;
            maxWidth = Math.max(maxWidth, textWidth);
        }

        return maxWidth;
    }

    private int calculateTotalHeight(List<PotionEffect> potions) {
        return potions.size() * (10 + BetterPlayerHUD.config.potionSpacing);
    }

    private int calculatePotionXPosition(int screenWidth, int elementWidth) {
        int offset = BetterPlayerHUD.config.potionHudX;

        if (offset >= 0) {
            return offset;
        } else {
            return screenWidth + offset - elementWidth;
        }
    }

    private int calculatePotionYPosition(int screenHeight, int totalHeight) {
        int offset = BetterPlayerHUD.config.potionHudY;

        if (offset >= 0) {
            return offset;
        } else {
            return screenHeight + offset - totalHeight;
        }
    }

    private void renderPotionEffects(List<PotionEffect> potions, int startX, int startY) {
        FontRenderer fr = mc.fontRendererObj;
        int yOffset = 0;

        for (PotionEffect effect : potions) {
            int yPos = startY + yOffset;

            if (BetterPlayerHUD.config.showPotionIcons) {
                renderPotionIcon(effect, startX, yPos);
            }

            if (BetterPlayerHUD.config.showPotionNames || BetterPlayerHUD.config.showPotionDurations) {
                renderPotionText(effect, startX + BetterPlayerHUD.config.potionTextOffset, yPos, fr);
            }

            yOffset += 10 + BetterPlayerHUD.config.potionSpacing;
        }
    }

    private void renderPotionIcon(PotionEffect effect, int x, int y) {
        Potion potion = Potion.potionTypes[effect.getPotionID()];
        if (potion == null) return;

        mc.getTextureManager().bindTexture(POTION_ICONS);

        int iconIndex = potion.getStatusIconIndex();
        int textureX = (iconIndex % 8) * 18;
        int textureY = 198 + (iconIndex / 8) * 18;

        drawTexturedModalRect(x, y, textureX, textureY, 18, 18);
    }

    private void renderPotionText(PotionEffect effect, int x, int y, FontRenderer fr) {
        String displayText = getPotionDisplayText(effect);
        int color = getPotionTextColor(effect);

        fr.drawStringWithShadow(displayText, x, y + 1, color);
    }

    private String getPotionDisplayText(PotionEffect effect) {
        StringBuilder text = new StringBuilder();
        Potion potion = Potion.potionTypes[effect.getPotionID()];

        if (potion == null) {
            return "未知效果";
        }

        if (BetterPlayerHUD.config.showPotionNames) {
            text.append(getPotionDisplayName(potion));
        }

        if (BetterPlayerHUD.config.showPotionAmplifier && effect.getAmplifier() > 0) {
            text.append(" ").append(getRomanNumber(effect.getAmplifier() + 1));
        }

        if (BetterPlayerHUD.config.showPotionDurations) {
            if (text.length() > 0) {
                text.append(" ");
            }
            text.append(getFormattedDuration(effect.getDuration()));
        }

        return text.toString();
    }

    private String getPotionDisplayName(Potion potion) {
        return potion.getName();
    }

    private String getFormattedDuration(int ticks) {
        if (ticks > 1000000) {
            return BetterPlayerHUD.config.showInfiniteAsIcon ? "∞" : "无限";
        }

        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;

        // 使用新的变量名
        switch (BetterPlayerHUD.config.potionTimeFormatString) {
            case "mm:ss":
                return String.format("%d:%02d", minutes, seconds);
            case "m:ss":
                return String.format("%d:%02d", minutes, seconds);
            case "ss":
                return String.valueOf(seconds + minutes * 60);
            case "HH:mm:ss":
                int hours = minutes / 60;
                minutes = minutes % 60;
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            default:
                return String.format("%d:%02d", minutes, seconds);
        }
    }
    private String getRomanNumber(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(number);
        }
    }

    private int getPotionTextColor(PotionEffect effect) {
        Potion potion = Potion.potionTypes[effect.getPotionID()];
        if (potion == null) return BetterPlayerHUD.config.potionTextColor;

        if (potion.isBadEffect()) {
            return BetterPlayerHUD.config.potionBadEffectColor;
        } else {
            return BetterPlayerHUD.config.potionGoodEffectColor;
        }
    }

    // 工具方法：绘制矩形（使用 GlStateManager + Tessellator，避免 raw GL 绕过 GlStateManager 缓存）
    private void drawRect(int left, int top, int right, int bottom, int color) {
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

    // 工具方法：绘制纹理矩形（使用 GlStateManager + Tessellator，避免 raw GL）
    private void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height) {
        float f = 0.00390625F;
        float f1 = 0.00390625F;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos((double)x, (double)(y + height), 0.0D)
            .tex((double)((float)textureX * f), (double)((float)(textureY + height) * f1)).endVertex();
        worldRenderer.pos((double)(x + width), (double)(y + height), 0.0D)
            .tex((double)((float)(textureX + width) * f), (double)((float)(textureY + height) * f1)).endVertex();
        worldRenderer.pos((double)(x + width), (double)y, 0.0D)
            .tex((double)((float)(textureX + width) * f), (double)((float)textureY * f1)).endVertex();
        worldRenderer.pos((double)x, (double)y, 0.0D)
            .tex((double)((float)textureX * f), (double)((float)textureY * f1)).endVertex();
        tessellator.draw();
    }
}