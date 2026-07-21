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
 * 每个活跃药水显示：[大图标]
 *                     剩余时长
 * 牛奶效果（起床战争风格）整合在此，显示：[奶桶]
 *                                          剩余秒数
 * 图标可调节大小，时长在图标下方居中显示。
 */
@SideOnly(Side.CLIENT)
public class PotionTimerHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");
    /** 缓存奶桶 ItemStack，避免每帧 new */
    private static final ItemStack MILK_ITEM = new ItemStack(Items.milk_bucket);

    /** 图标到下方文字的间距 */
    private static final int GAP = 4;
    /** 每项之间的间距 */
    private static final int SPACING = 8;

    // ── 牛奶倒计时 ──
    private int milkTicks = 0;
    private boolean pendingMilkReset = false;

    // ── 多人牛奶检测状态（起床战争等模组服） ──
    private boolean wasUsingMilk = false;
    /** 仅存 item 引用 + stackSize，不存整个 ItemStack（减分配） */
    private net.minecraft.item.Item prevMilkItemType = null;
    private int prevMilkStackSize = 0;

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

        // ── 多人牛奶检测：玩家刚喝完自定义魔法牛奶（不含 full ItemStack copy） ──
        ItemStack held = mc.thePlayer.getHeldItem();
        boolean nowUsing = mc.thePlayer.isUsingItem();
        if (wasUsingMilk && !nowUsing && prevMilkItemType != null) {
            boolean consumed = (held == null)
                    || (held.getItem() != prevMilkItemType)
                    || (prevMilkStackSize > 1 && held.stackSize < prevMilkStackSize);
            if (consumed) {
                milkTicks = 600;
            }
        }
        // 只记 item type + stackSize，不 copy ItemStack
        boolean isMilk = nowUsing && held != null && isMilkItem(held);
        wasUsingMilk = isMilk;
        if (isMilk) {
            prevMilkItemType = held.getItem();
            prevMilkStackSize = held.stackSize;
        } else {
            prevMilkItemType = null;
        }

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

    /** 条目高度：图标 + 间距 + 一行文字 */
    private static int entryHeight() {
        return iconSize() + GAP + mc.fontRendererObj.FONT_HEIGHT;
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        int eh = entryHeight();

        // 判断是否只有牛奶要渲染（药水计时器关闭但牛奶倒计时仍在）
        boolean milkOnly = !cfg.enablePotionTimer && milkTicks > 0;

        if (!cfg.enablePotionTimer && !milkOnly) {
            if (HUDEditManager.isEditing()) {
                ScaledResolution sr2 = event.resolution;
                int sw2 = sr2.getScaledWidth() / 2;
                int tY = 4 + cfg.potionTimerYOffset;
                HUDEditManager.report("药水计时器", sw2 + cfg.potionTimerXOffset - 100, tY, 200, eh);
            }
            return;
        }
        if (mc.thePlayer == null) return;

        ScaledResolution sr = event.resolution;
        int sw = sr.getScaledWidth();

        int centerX = sw / 2 + cfg.potionTimerXOffset;
        int topY = 4 + cfg.potionTimerYOffset;

        // ── 计算条目数 & 总宽度（无对象分配） ──
        int potionCount = 0;
        Collection<PotionEffect> effects = null;
        if (cfg.enablePotionTimer) {
            effects = mc.thePlayer.getActivePotionEffects();
            if (effects != null) {
                for (PotionEffect pe : effects) {
                    if (Potion.potionTypes[pe.getPotionID()] != null) potionCount++;
                }
            }
        }
        int milkCount = (milkTicks > 0) ? 1 : 0;
        int totalEntries = potionCount + milkCount;
        if (totalEntries == 0) {
            if (HUDEditManager.isEditing()) {
                HUDEditManager.report("药水计时器", centerX - 100, topY, 200, eh);
            }
            return;
        }

        int totalW = totalEntries * iconSize() + (totalEntries - 1) * SPACING;
        int startX = centerX - totalW / 2;

        // ── 渲染（内联，避免每帧 new 对象） ──
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        int curX = startX;

        // 药水
        if (effects != null) {
            for (PotionEffect pe : effects) {
                Potion potion = Potion.potionTypes[pe.getPotionID()];
                if (potion == null) continue;
                renderPotionEntry(potion, pe, curX, topY);
                curX += iconSize() + SPACING;
            }
        }

        // 牛奶
        if (milkTicks > 0) {
            renderMilkEntry(milkTicks, curX, topY);
        }

        // 编辑报告
        if (HUDEditManager.isEditing()) {
            HUDEditManager.report("药水计时器", startX, topY, totalW, eh);
        }
    }

    // ── 内联渲染（避免每帧 new 对象） ──

    private void renderPotionEntry(Potion potion, PotionEffect effect, int x, int y) {
        int iconIndex = potion.getStatusIconIndex();
        int u = iconIndex % 8 * 18;
        int v = 198 + iconIndex / 8 * 18;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        float scale = (float)iconSize() / 18.0f;
        GlStateManager.scale(scale, scale, 1.0f);
        mc.getTextureManager().bindTexture(INVENTORY_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, (float) u, (float) v, 18, 18, 256.0f, 256.0f);
        GlStateManager.popMatrix();

        int totalSec = effect.getDuration() / 20;
        String timeStr = formatTime(totalSec);
        int timeW = mc.fontRendererObj.getStringWidth(timeStr);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.fontRendererObj.drawStringWithShadow(timeStr, x + (iconSize() - timeW) / 2, y + iconSize() + GAP,
                totalSec <= 10 ? 0xFFFF5555 : 0xFFFFFFFF);
    }

    private void renderMilkEntry(int ticks, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(MILK_ITEM, x + (iconSize() - 16) / 2, y + (iconSize() - 16) / 2);
        RenderHelper.disableStandardItemLighting();

        int sec = (ticks + 19) / 20;
        String text = sec + "s";
        int textW = mc.fontRendererObj.getStringWidth(text);
        int color;
        if (sec <= 5 && sec % 2 == 0) color = 0xFFFF5555;
        else if (sec <= 5)             color = 0xFFAAAAAA;
        else                            color = 0xFFFFFFFF;
        mc.fontRendererObj.drawStringWithShadow(text, x + (iconSize() - textW) / 2, y + iconSize() + GAP, color);
    }

    // ── 工具方法 ──

    private static String formatTime(int totalSec) {
        if (totalSec >= 3600) {
            return (totalSec / 3600) + "h" + ((totalSec % 3600) / 60) + "m";
        } else if (totalSec >= 60) {
            return (totalSec / 60) + "m" + (totalSec % 60) + "s";
        } else {
            return totalSec + "s";
        }
    }

    /**
     * 判断物品是否为"牛奶"（原版桶 + 自定义名字含 milk/牛奶）。
     * 起床战争等模组服使用自定义命名药水作为魔法牛奶。
     */
    private static boolean isMilkItem(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() == Items.milk_bucket) return true;
        // 仅当物品名含关键词才做 toLowerCase，减少无用字符串分配
        String name = stack.getDisplayName();
        if (name.contains("Milk") || name.contains("milk") || name.contains("牛奶") || name.contains("MILK")) {
            return true;
        }
        return false;
    }
}
