package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 连击计数（Combo Display）
 *
 * 原理：玩家 attack 时记下目标实体和时间。
 * 服务端确认伤害后会广播 S19PacketEntityStatus(2)，
 * 客户端的 handleStatusUpdate(2) 会将实体的
 * hurtResistantTime 设为 maxHurtResistantTime。
 * 我们在攻击后的短时间窗口内检测这个变化，确认命中。
 */
public class ComboHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final long CONFIRM_WINDOW_MS = 500;   // 攻击后500ms内检测（给足网络延迟余量）
    private static final long DEBOUNCE_MS = 100;          // 同一实体防抖100ms
    private static final long COMBO_TIMEOUT_MS = 3000;    // 3秒无命中则重置

    private int comboCount = 0;
    private Entity pendingTarget = null;
    private long lastAttackTime = 0;
    private long lastConfirmTime = 0;

    // 每个实体最后一次被确认命中的时间，防重复计数
    private final Map<Integer, Long> lastEntityConfirmTime = new HashMap<>();

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.entityPlayer != mc.thePlayer) return;
        if (mc.theWorld == null) return;

        pendingTarget = event.target;
        lastAttackTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.enableCombo) return;

        long now = System.currentTimeMillis();

        // ── 确认命中检测 ──
        if (pendingTarget != null && now - lastAttackTime < CONFIRM_WINDOW_MS) {
            if (pendingTarget instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) pendingTarget;
                int entityId = living.getEntityId();
                // 防抖：同一实体在 DEBOUNCE_MS 内不重复计数
                Long lastDebounce = lastEntityConfirmTime.get(entityId);
                if (lastDebounce != null && now - lastDebounce < DEBOUNCE_MS) {
                    // 跳过，避免重复计数
                } else if (living.hurtResistantTime >= living.maxHurtResistantTime - 5
                        && living.hurtResistantTime > 0) {
                    // 服务端确认命中：hurtResistantTime 被设为 maxHurtResistantTime（=20）
                    // 然后每 tick(50ms) 递减1，放宽到 -5 允许 ~250ms 的检测窗口
                    if (now - lastConfirmTime > COMBO_TIMEOUT_MS) {
                        comboCount = 1;
                    } else {
                        comboCount++;
                    }
                    lastConfirmTime = now;
                    lastEntityConfirmTime.put(entityId, now);
                }
            }
        }

        // ── 超时重置 ──
        if (comboCount > 0 && now - lastConfirmTime > COMBO_TIMEOUT_MS) {
            comboCount = 0;
            lastEntityConfirmTime.clear();
        }

        // ── 清理 ──
        if (pendingTarget != null && now - lastAttackTime > CONFIRM_WINDOW_MS + 200) {
            pendingTarget = null;
        }

        // ── 渲染 ──
        if (comboCount <= 0) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // 位置：右下偏移
        int baseX = sw - 50 + cfg.comboXOffset;
        int baseY = sh - 40 + cfg.comboYOffset;

        // 颜色：combo 越高越红
        int color;
        if (comboCount <= 3)       color = 0xFF55FF55;  // 绿
        else if (comboCount <= 7)  color = 0xFFFFFF55;  // 黄
        else if (comboCount <= 15) color = 0xFFFFAA55;  // 橙
        else                       color = 0xFFFF5555;  // 红

        String text = "Combo: " + comboCount;
        int tw = mc.fontRendererObj.getStringWidth(text);

        // 画出背景
        Gui.drawRect(baseX - 4, baseY - 2, baseX + tw + 4, baseY + 10, 0x66000000);

        // 上报 F7 编辑
        HUDEditManager.report("连击计数", baseX - 4, baseY - 2, tw + 8, 12);

        // 文字
        GlStateManager.enableBlend();
        mc.fontRendererObj.drawStringWithShadow(text, baseX, baseY, color);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        // 空 tick 中检查 pendingTarget 是否还在 world 中
        // 主要用来清理已离世的目标
        if (pendingTarget != null && !pendingTarget.isEntityAlive()) {
            long now = System.currentTimeMillis();
            if (now - lastAttackTime > CONFIRM_WINDOW_MS) {
                pendingTarget = null;
            }
        }
    }
}
