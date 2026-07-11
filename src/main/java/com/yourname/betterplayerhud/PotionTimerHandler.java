package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

/**
 * 模块26：药水计时器 — 屏幕正上方横向大字体
 *
 * 独立于原有的药水效果 HUD（模块22），位置、大小、风格完全不同。
 * 每个活跃药水显示：[图标] 药水名 时长
 * 水平排列，居中于屏幕顶部。
 */
@SideOnly(Side.CLIENT)
public class PotionTimerHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.enablePotionTimer) return;
        if (mc.thePlayer == null) return;

        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // 编辑模式 placeholder
        if ((effects == null || effects.isEmpty())) {
            if (HUDEditManager.isEditing()) {
                int cx = sw / 2 + cfg.potionTimerXOffset;
                int cy = 4 + cfg.potionTimerYOffset;
                HUDEditManager.report("药水计时器", cx - 80, cy, 160, 30);
            }
            return;
        }

        int centerX = sw / 2 + cfg.potionTimerXOffset;
        int topY = 4 + cfg.potionTimerYOffset;

        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        // 第一遍计算总宽度，以便居中
        int totalWidth = 0;
        int itemCount = 0;
        final int spacing = 12; // 两药水间距

        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;

            String name = getPotionLocalizedName(potion, effect);
            int timeSec = effect.getDuration() / 20;
            String timeStr = formatTime(timeSec);
            String line = name + " " + timeStr;

            int w = 22 + mc.fontRendererObj.getStringWidth(line); // icon(18) + gap(4) + text
            totalWidth += w + spacing;
            itemCount++;
        }

        if (itemCount == 0) return;
        totalWidth -= spacing; // 去掉最后一个间距
        int startX = centerX - totalWidth / 2;

        // 第二遍渲染
        int curX = startX;
        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;

            // 药水颜色（取对应药水的颜色值，用于文字染色）
            int color = potion.getLiquidColor();
            // 确保不透明
            color = (color & 0x00FFFFFF) | 0xFF000000;

            // 图标
            int iconIndex = potion.getStatusIconIndex();
            int u = iconIndex % 8 * 18;
            int v = 198 + iconIndex / 8 * 18;

            mc.getTextureManager().bindTexture(INVENTORY_TEXTURE);
            Gui.drawModalRectWithCustomSizedTexture(curX, topY, (float) u, (float) v, 18, 18, 256.0f, 256.0f);

            // 文字
            String name = getPotionLocalizedName(potion, effect);
            int timeSec = effect.getDuration() / 20;
            String timeStr = formatTime(timeSec);
            String line = name + " " + timeStr;

            int tx = curX + 22;
            int ty = topY + 5;
            mc.fontRendererObj.drawStringWithShadow(line, tx, ty, color);

            curX += 22 + mc.fontRendererObj.getStringWidth(line) + spacing;
        }

        // F7 编辑报告：固定 200px 宽居中，避免因药水数量变化导致 PosConverter 不一致
        if (HUDEditManager.isEditing()) {
            HUDEditManager.report("药水计时器", centerX - 100, topY, 200, 22);
        }
    }

    /** 获取药水本地化名（如 "力量"、"速度"） */
    private String getPotionLocalizedName(Potion potion, PotionEffect effect) {
        String name = potion.getName();
        // 去掉 "potion." 前缀
        if (name.startsWith("potion.")) name = name.substring(7);

        int amp = effect.getAmplifier();
        if (amp > 0) {
            String[] suffixes = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
            if (amp < suffixes.length) {
                name += suffixes[amp];
            } else {
                name += " " + (amp + 1);
            }
        }

        return name;
    }

    /** 格式化时间为 mm:ss 或 h:mm:ss */
    private String formatTime(int totalSec) {
        if (totalSec >= 3600) {
            return String.format("%d:%02d:%02d", totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60);
        } else {
            return String.format("%d:%02d", totalSec / 60, totalSec % 60);
        }
    }
}
