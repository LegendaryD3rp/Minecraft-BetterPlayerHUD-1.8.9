package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

/**
 * 模块26：药水+牛奶合并计时器 — 屏幕正上方横向
 *
 * 每个活跃药水显示：[大图标] 时长（无名称）
 * 牛奶效果（起床战争风格）也整合在此，显示：[奶桶] 剩余秒数
 * 图标2倍大，整体醒目。
 */
@SideOnly(Side.CLIENT)
public class PotionTimerHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");

    /** 图标到文字的间距 */
    private static final int GAP = 6;
    /** 每项之间的间距 */
    private static final int SPACING = 16;

    // ── 牛奶倒计时 ──
    private int milkTicks = 0;
    private boolean pendingMilkReset = false;

    @SubscribeEvent
    public void onItemUseFinish(PlayerUseItemEvent.Finish event) {
        if (event.entityPlayer == mc.thePlayer
                && event.result != null
                && event.result.getItem() == Items.milk_bucket) {
            milkTicks = 600; // 30秒
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player != mc.thePlayer) return;
        if (event.phase != TickEvent.Phase.END) return;

        if (pendingMilkReset) { milkTicks = 0; pendingMilkReset = false; return; }
        if (milkTicks > 0) {
            milkTicks--;
            if (mc.thePlayer != null && mc.thePlayer.isDead) milkTicks = 0;
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.player == mc.thePlayer) pendingMilkReset = true;
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.player == mc.thePlayer) milkTicks = 0;
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.player == mc.thePlayer) milkTicks = 0;
    }

    // ───────────────────────────────────────────────
    //  渲染
    // ───────────────────────────────────────────────

    /** 获取当前图标大小（带边界限制） */
    private static int iconSize() {
        return Math.max(12, Math.min(64, BetterPlayerHUD.config.potionTimerIconSize));
    }

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

        int centerX = sw / 2 + cfg.potionTimerXOffset;
        int topY = 4 + cfg.potionTimerYOffset;

        // ── 构建条目列表 ──
        java.util.List<Entry> entries = new java.util.ArrayList<>();

        // 药水
        if (effects != null) {
            for (PotionEffect pe : effects) {
                Potion potion = Potion.potionTypes[pe.getPotionID()];
                if (potion == null) continue;
                entries.add(new PotionEntry(potion, pe));
            }
        }

        // 牛奶
        if (milkTicks > 0) {
            entries.add(new MilkEntry(milkTicks));
        }

        // 无任何条目 → placeholder（仅编辑模式）
        if (entries.isEmpty()) {
            if (HUDEditManager.isEditing()) {
                int phX = centerX - 100;
                HUDEditManager.report("药水计时器", phX, topY, 200, iconSize() + 4);
            }
            return;
        }

        // ── 计算总宽度 ──
        int totalW = 0;
        for (int i = 0; i < entries.size(); i++) {
            totalW += entries.get(i).width();
            if (i < entries.size() - 1) totalW += SPACING;
        }

        int startX = centerX - totalW / 2;

        // ── 渲染 ──
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        int curX = startX;
        for (Entry entry : entries) {
            entry.render(curX, topY);
            curX += entry.width() + SPACING;
        }

        // 编辑报告（固定框，覆盖所有条目）
        if (HUDEditManager.isEditing()) {
            int totalH = iconSize() + 4;
            HUDEditManager.report("药水计时器", startX, topY, totalW, totalH);
        }
    }

    // ═══════════════════════════════════════════════════
    //  条目抽象
    // ═══════════════════════════════════════════════════

    private interface Entry {
        int width();
        void render(int x, int y);
    }

    // ── 药水条目 ──

    private static class PotionEntry implements Entry {
        final Potion potion;
        final PotionEffect effect;
        final String timeStr;
        final int timeW;
        final int totalW;

        PotionEntry(Potion potion, PotionEffect effect) {
            this.potion = potion;
            this.effect = effect;
            int totalSec = effect.getDuration() / 20;
            this.timeStr = formatTime(totalSec);
            this.timeW = mc.fontRendererObj.getStringWidth(timeStr);
            this.totalW = iconSize() + GAP + timeW;
        }

        @Override public int width() { return totalW; }

        @Override
        public void render(int x, int y) {
            // 图标
            int iconIndex = potion.getStatusIconIndex();
            int u = iconIndex % 8 * 18;
            int v = 198 + iconIndex / 8 * 18;

            mc.getTextureManager().bindTexture(INVENTORY_TEXTURE);
            Gui.drawModalRectWithCustomSizedTexture(x, y, (float) u, (float) v, iconSize(), iconSize(), 256.0f, 256.0f);

            // 时长文字：白色，最后10秒变红
            int totalSec = effect.getDuration() / 20;
            int color = totalSec <= 10 ? 0xFFFF5555 : 0xFFFFFFFF;
            mc.fontRendererObj.drawStringWithShadow(timeStr, x + iconSize() + GAP, y + (iconSize() - 9) / 2, color);
        }
    }

    // ── 牛奶条目 ──

    private static class MilkEntry implements Entry {
        final String text;
        final int textW;
        final int totalW;

        MilkEntry(int ticks) {
            int sec = (ticks + 19) / 20;
            this.text = sec + "s";
            this.textW = mc.fontRendererObj.getStringWidth(text);
            this.totalW = iconSize() + GAP + textW;
        }

        @Override public int width() { return totalW; }

        @Override
        public void render(int x, int y) {
            // 奶桶图标（36x36）
            RenderHelper.enableGUIStandardItemLighting();
            ItemStack milk = new ItemStack(Items.milk_bucket);
            mc.getRenderItem().renderItemAndEffectIntoGUI(milk, x + (iconSize() - 16) / 2, y + (iconSize() - 16) / 2);
            RenderHelper.disableStandardItemLighting();

            // 秒数文字：白色，最后5秒闪烁
            int secNum;
            try { secNum = Integer.parseInt(text.replace("s", "")); } catch (Exception e) { secNum = 999; }
            int color;
            if (secNum <= 5 && secNum % 2 == 0) color = 0xFFFF5555;
            else if (secNum <= 5)                color = 0xFFAAAAAA;
            else                                 color = 0xFFFFFFFF;
            mc.fontRendererObj.drawStringWithShadow(text, x + iconSize() + GAP, y + (iconSize() - 9) / 2, color);
        }
    }

    // ── 工具方法 ──

    private static String formatTime(int totalSec) {
        if (totalSec >= 3600) {
            return String.format("%d:%02d:%02d", totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60);
        } else {
            return String.format("%d:%02d", totalSec / 60, totalSec % 60);
        }
    }
}
