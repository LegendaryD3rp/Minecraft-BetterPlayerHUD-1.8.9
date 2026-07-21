package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
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
 * 使用 S19HitManager 获取服务器确认的命中事件。
 * 无背景框，纯文字，弹性弹跳动画。
 * F7 编辑模式：可拖拽、可缩放（Ctrl+滚轮）、可开关（Delete）。
 */
@SideOnly(Side.CLIENT)
public class ComboHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── 常量 ──
    private static final long COMBO_TIMEOUT_MS = 3000;    // 3秒无命中重置
    private static final long MISS_TIMEOUT_MS = 500;      // 挥刀后500ms无S19确认→miss
    private static final int BOX_W = 60;   // 固定宽度（缩放前），确保拖拽不跳动
    private static final int BOX_H = 24;   // 固定高度（缩放前）

    // ── 状态 ──
    private static ComboHandler INSTANCE;

    private int comboCount = 0;
    private long lastConfirmTime = 0;
    private int pendingEntityId = -1;
    private boolean swingConfirmed = false;
    private long lastSwingTime = 0;

    private boolean handlerRegistered = false;  // S19 监听器已注册
    private boolean injectionRequested = false; // 已请求注入

    // ── 弹跳动画 ──
    private float animScale = 1.0f;         // 当前动画缩放值
    private float animVel = 0.0f;           // 速度（弹性衰减用）
    private boolean comboJustIncremented = false;  // 这一帧是否刚增加

    public ComboHandler() {
        INSTANCE = this;
    }

    /** 供外部（如僵尸末日枪械命中）触发连击计数 */
    public static void onExternalHit() {
        if (INSTANCE != null) {
            INSTANCE.comboCount++;
            INSTANCE.lastConfirmTime = System.currentTimeMillis();
            INSTANCE.comboJustIncremented = true;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  客户端 Tick：重置状态 + 注册 + 动画更新
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
            animScale = 1.0f;
            animVel = 0.0f;
            S19HitManager.reset();
            return;
        }

        // 注册监听器 + 重试注入直到成功
        if (!handlerRegistered && mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
            S19HitManager.registerListener(this::onS19Hit);
            handlerRegistered = true;
        }
        if (handlerRegistered) {
            S19HitManager.ensureInjected();
        }

        // 超时重置
        if (comboCount > 0 && System.currentTimeMillis() - lastConfirmTime > COMBO_TIMEOUT_MS) {
            comboCount = 0;
        }

        // ── 弹性动画更新 ──
        // 每次增量触发瞬间拉高 animScale → 弹性阻尼衰减到 1.0
        float target = 1.0f;
        float diff = target - animScale;
        animVel += diff * 0.35f;     // 弹簧刚度
        animVel *= 0.72f;             // 阻尼
        animScale += animVel;

        // 防止震荡过度时负值
        if (animScale < 0.5f) animScale = 0.5f;
    }

    // ═══════════════════════════════════════════════════════
    //  S19 命中确认（Netty 线程回调）
    // ═══════════════════════════════════════════════════════

    private void onS19Hit(int entityId, long time) {
        if (entityId == pendingEntityId && !swingConfirmed) {
            swingConfirmed = true;
            comboCount++;
            lastConfirmTime = time;
            comboJustIncremented = true;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  攻击事件：记下目标 + miss 检测
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

        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        ScaledResolution sr = event.resolution;
        int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();

        // 中心锚点
        int centerX = sw / 2 + cfg.comboXOffset;
        int centerY = sh / 2 + cfg.comboYOffset;

        // 缩放后尺寸
        int scaledW = Math.round(BOX_W * cfg.comboScale);
        int scaledH = Math.round(BOX_H * cfg.comboScale);

        // 报告框（固定尺寸，不随文字宽度变化）
        int boxX = centerX - scaledW / 2;
        int boxY = centerY - scaledH / 2;
        HUDEditManager.report("连击计数", boxX, boxY, scaledW, scaledH);

        if (!cfg.enableCombo) return;
        if (comboCount <= 0) return;

        String text = String.valueOf(comboCount);
        int textColor;
        if (comboCount >= 10) textColor = 0xFFFF5555;
        else if (comboCount >= 5) textColor = 0xFFFFFF55;
        else textColor = 0xFFFFFFFF;

        // 触发弹跳（组合中刚增加的那一帧）
        if (comboJustIncremented) {
            animScale = 1.35f;
            animVel = 0.0f;
            comboJustIncremented = false;
        }

        // ── 绘制文字（无背景框） ──
        // 先应用位置缩放，再乘上弹跳缩放
        float finalScale = cfg.comboScale * animScale;

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(finalScale, finalScale, 1.0f);

        int textW = mc.fontRendererObj.getStringWidth(text);
        int drawX = -textW / 2;
        int drawY = -4;  // 垂直居中（FONT_HEIGHT=9, -4 ≈ 居中）

        // 文字阴影
        mc.fontRendererObj.drawString(text, drawX + 1, drawY + 1, 0x40000000, false);
        mc.fontRendererObj.drawString(text, drawX, drawY, textColor, true);

        GlStateManager.popMatrix();
    }
}
