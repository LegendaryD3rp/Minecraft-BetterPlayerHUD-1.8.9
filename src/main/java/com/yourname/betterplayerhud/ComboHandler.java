package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 模块25：连击计数（Combo Display）
 *
 * 使用 S19HitManager 获取服务器确认的命中事件，
 * 屏幕显示当前连击数。
 * 支持 F7 编辑模式：可拖拽、可缩放（Ctrl+滚轮）。
 */
@SideOnly(Side.CLIENT)
public class ComboHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── 常量 ──
    private static final long COMBO_TIMEOUT_MS = 3000;    // 3秒无命中重置
    private static final long MISS_TIMEOUT_MS = 500;      // 挥刀后500ms无S19确认→miss

    // ── 状态 ──
    private int comboCount = 0;
    private long lastConfirmTime = 0;
    private int pendingEntityId = -1;
    private boolean swingConfirmed = false;
    private long lastSwingTime = 0;

    private boolean registered = false;

    // ═══════════════════════════════════════════════════════
    //  客户端 Tick：重置状态 + 重新注入
    // ═══════════════════════════════════════════════════════

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (mc.theWorld == null) {
            comboCount = 0;
            pendingEntityId = -1;
            swingConfirmed = false;
            lastSwingTime = 0;
            lastConfirmTime = 0;
            S19HitManager.reset();
            return;
        }

        // 注册监听器（懒加载，等世界就绪）
        if (!registered && mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            S19HitManager.registerListener(this::onS19Hit);
            registered = true;
        }

        // 超时重置
        if (comboCount > 0 && System.currentTimeMillis() - lastConfirmTime > COMBO_TIMEOUT_MS) {
            comboCount = 0;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  S19 命中确认（Netty 线程回调）
    // ═══════════════════════════════════════════════════════

    private void onS19Hit(int entityId, long time) {
        if (entityId == pendingEntityId && !swingConfirmed) {
            swingConfirmed = true;
            comboCount++;
            lastConfirmTime = time;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  攻击事件：记下目标
    // ═══════════════════════════════════════════════════════

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.entityPlayer != mc.thePlayer) return;
        if (mc.theWorld == null) return;

        long now = System.currentTimeMillis();

        // 上一刀 miss 检测
        if (comboCount > 0 && !swingConfirmed
                && pendingEntityId != -1
                && now - lastSwingTime > MISS_TIMEOUT_MS) {
            comboCount = 0; // miss 断连
        }

        pendingEntityId = event.target.getEntityId();
        swingConfirmed = false;
        lastSwingTime = now;
    }

    // ═══════════════════════════════════════════════════════
    //  渲染
    // ═══════════════════════════════════════════════════════

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!BetterPlayerHUD.config.enableCombo) {
            if (HUDEditManager.isEditing()) {
                ScaledResolution sr = new ScaledResolution(mc);
                int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
                int x = sw / 2 + BetterPlayerHUD.config.comboXOffset;
                int y = sh / 2 + BetterPlayerHUD.config.comboYOffset;
                HUDEditManager.report("连击计数", x, y, 50, 20);
            }
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;

        int x = sr.getScaledWidth() / 2 + cfg.comboXOffset;
        int y = sr.getScaledHeight() / 2 + cfg.comboYOffset;

        if (comboCount <= 0) {
            if (HUDEditManager.isEditing()) {
                HUDEditManager.report("连击计数", x, y, 50, 20);
            }
            return;
        }

        String text = String.valueOf(comboCount);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(cfg.comboScale, cfg.comboScale, 1.0f);

        // 背景
        int textW = mc.fontRendererObj.getStringWidth(text);
        int bgW = textW + 10;
        int bgH = 20;
        Gui.drawRect(-bgW / 2, -bgH / 2, bgW / 2, bgH / 2, 0x88000000);

        // 文字（根据连击数变色）
        int color;
        if (comboCount >= 10) color = 0xFFFF5555;
        else if (comboCount >= 5) color = 0xFFFFFF55;
        else color = 0xFFFFFFFF;

        mc.fontRendererObj.drawString(text, -textW / 2, -4, color, true);

        GlStateManager.popMatrix();

        // F7 编辑报告
        if (HUDEditManager.isEditing()) {
            int scaledW = (int)(bgW * cfg.comboScale);
            int scaledH = (int)(bgH * cfg.comboScale);
            HUDEditManager.report("连击计数", x - scaledW / 2, y - scaledH / 2, scaledW, scaledH);
        }
    }
}
