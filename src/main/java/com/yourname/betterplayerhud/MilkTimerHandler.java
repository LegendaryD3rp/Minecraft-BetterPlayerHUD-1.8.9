package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 模块27：牛奶倒计时 — 起床战争风格
 *
 * 喝牛奶后启动30秒倒计时，模拟床战争的"牛奶"效果。
 * 图标使用奶桶，显示剩余秒数。
 * 位置：独立药水计时器右侧，或单独一行。
 */
@SideOnly(Side.CLIENT)
public class MilkTimerHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /** 倒计时剩余 tick（0 = 未激活） */
    private int timerTicks = 0;

    /** 标记是否因换服/死亡需要重置 */
    private boolean pendingReset = false;

    // ───────────────────────────────────────────────
    //  检测：喝牛奶
    // ───────────────────────────────────────────────

    @SubscribeEvent
    public void onItemUseFinish(PlayerUseItemEvent.Finish event) {
        if (event.entityPlayer == mc.thePlayer
                && event.result != null
                && event.result.getItem() == Items.milk_bucket) {
            // 启动30秒倒计时（600 ticks）
            timerTicks = 600;
        }
    }

    // ───────────────────────────────────────────────
    //  检测：换世界 / 死亡 → 重置
    // ───────────────────────────────────────────────

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player != mc.thePlayer) return;
        if (event.phase != TickEvent.Phase.END) return;

        if (pendingReset) {
            timerTicks = 0;
            pendingReset = false;
            return;
        }

        if (timerTicks > 0) {
            timerTicks--;
            // 玩家死亡则立即清除
            if (mc.thePlayer != null && mc.thePlayer.isDead) {
                timerTicks = 0;
            }
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player == mc.thePlayer) {
            pendingReset = true;
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player == mc.thePlayer) {
            timerTicks = 0;
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player == mc.thePlayer) {
            timerTicks = 0;
        }
    }

    // ───────────────────────────────────────────────
    //  渲染
    // ───────────────────────────────────────────────

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.enableMilkTimer) return;
        if (mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        int centerX = sw / 2 + cfg.milkTimerXOffset;
        int topY = 4 + cfg.milkTimerYOffset;  // 与药水计时器同水平

        // 无论是否激活都上报，确保 F7 拖拽
        if (timerTicks <= 0) {
            if (HUDEditManager.isEditing()) {
                HUDEditManager.report("牛奶倒计时", centerX - 60, topY, 120, 22);
            }
            return;
        }

        // 计算秒数
        int seconds = (timerTicks + 19) / 20;  // 向上取整，显示剩余秒数

        // 奶桶图标 + 时间文字
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        // 奶桶图标
        RenderHelper.enableGUIStandardItemLighting();
        ItemStack milkStack = new ItemStack(Items.milk_bucket);
        mc.getRenderItem().renderItemAndEffectIntoGUI(milkStack, centerX - 60, topY);
        RenderHelper.disableStandardItemLighting();

        // 文字：剩余秒数
        String text = String.format("%ds", seconds);
        if (seconds <= 5) {
            // 最后5秒红色闪烁
            if (seconds % 2 == 0) {
                mc.fontRendererObj.drawStringWithShadow(text, centerX - 36, topY + 5, 0xFF5555);
            } else {
                mc.fontRendererObj.drawStringWithShadow(text, centerX - 36, topY + 5, 0xFFAAAA);
            }
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, centerX - 36, topY + 5, 0xFFFFFF);
        }

        // F7 编辑报告（固定宽度）
        if (HUDEditManager.isEditing()) {
            HUDEditManager.report("牛奶倒计时", centerX - 60, topY, 120, 22);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
    }
}
