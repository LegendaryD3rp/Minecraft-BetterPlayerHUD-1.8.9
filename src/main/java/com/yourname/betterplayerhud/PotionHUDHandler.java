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

/**
 * 模块：药水效果 HUD
 *
 * 在屏幕左下方逐行显示当前激活的药水效果：
 *   [图标] 时长 (如 3:21)
 * 从底部往上排列，图标来自 inventory.png 中的 potion sprite sheet。
 */
@SideOnly(Side.CLIENT)
public class PotionHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");

    /**
     * 注意：1.8.9 Forge 无 POTION_ICONS ElementType，
     * 原版药水图标渲染在 ALL 循环内不可单独取消。
     * 自定义药水计时器在 Post(TEXT) 中覆盖显示。
     * 如需隐藏原版药水图标，后续可通过 ASM/反射干预 GuiIngame.func_180474_b。
     */

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!BetterPlayerHUD.config.enablePotionHUD) {
            if (HUDEditManager.isEditing()) {
                ScaledResolution sr2 = event.resolution;
                int sh = sr2.getScaledHeight();
                int x = 2 + BetterPlayerHUD.config.potionXOffset;
                int y = sh - 18 - 2 + BetterPlayerHUD.config.potionYOffset;
                HUDEditManager.report("药水效果", x, y, 160, 26);
            }
            return;
        }
        if (mc.thePlayer == null) return;

        java.util.Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();

        // 编辑模式下即使无药水也上报 placeholder
        if ((effects == null || effects.isEmpty())) {
            if (HUDEditManager.isEditing()) {
                ScaledResolution sr2 = event.resolution;
                int sh = sr2.getScaledHeight();
                int x = 2 + BetterPlayerHUD.config.potionXOffset;
                int y = sh - 18 - 2 + BetterPlayerHUD.config.potionYOffset;
                HUDEditManager.report("药水效果", x, y, 160, 26);
            }
            return;
        }

        ScaledResolution sr = event.resolution;
        int screenHeight = sr.getScaledHeight();

        // 从底部往上算起始Y
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        int x = 2 + cfg.potionXOffset;
        int y = screenHeight - 18 - 2 + cfg.potionYOffset;  // 底部贴边
        int startY = y;

        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;

            // ---- 绘制图标 ----
            int iconIndex = potion.getStatusIconIndex();
            int u = iconIndex % 8 * 18;
            int v = 198 + iconIndex / 8 * 18;

            mc.getTextureManager().bindTexture(INVENTORY_TEXTURE);
            Gui.drawModalRectWithCustomSizedTexture(x, y, (float) u, (float) v, 18, 18, 256.0f, 256.0f);

            // ---- 绘制时长文字 ----
            int totalSec = effect.getDuration() / 20;
            String time;
            if (totalSec >= 3600) {
                int h = totalSec / 3600;
                int m = (totalSec % 3600) / 60;
                int s = totalSec % 60;
                time = h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
            } else {
                int m = totalSec / 60;
                int s = totalSec % 60;
                time = m + ":" + (s < 10 ? "0" : "") + s;
            }
            mc.fontRendererObj.drawStringWithShadow(time, x + 22, y + 5, cfg.potionTextColor);

            y -= 24;  // 往上排
        }

        if (HUDEditManager.isEditing())
            HUDEditManager.report("药水效果", x, startY, 160, startY - y + 24 + 2);

        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
    }
}
