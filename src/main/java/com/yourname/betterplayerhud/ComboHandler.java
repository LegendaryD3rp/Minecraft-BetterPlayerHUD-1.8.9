package com.yourname.betterplayerhud;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

/**
 * 连击计数（Combo Display）— 服务端确认版
 *
 * 通过 Netty Pipeline 注入拦截 S19PacketEntityStatus(opcode=2)，
 * 该包由服务端在判定伤害有效后广播，是真正可靠的命中确认。
 *
 * 安全：只读不改，Pass-through，对协议层零影响，反作弊无法检测。
 */
public class ComboHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final long COMBO_TIMEOUT_MS = 3000;    // 3秒无命中则重置
    private static final long CONFIRM_FRESH_MS = 2000;    // S19 新鲜度阈值
    private static final String HANDLER_NAME = "bhud_combo";

    // ── 反射读取 S19PacketEntityStatus 的私有 entityId 字段 ──
    private static final Field S19_ENTITY_ID;
    static {
        Field f = null;
        try {
            // MCP 名称（开发环境）
            f = S19PacketEntityStatus.class.getDeclaredField("entityId");
            f.setAccessible(true);
        } catch (Exception e) {
            try {
                // SRG 名称（生产环境）
                f = S19PacketEntityStatus.class.getDeclaredField("field_149164_a");
                f.setAccessible(true);
            } catch (Exception e2) {
                // 都找不到，放弃
            }
        }
        S19_ENTITY_ID = f;
    }

    // ── 跨线程通信（Netty 线程写，渲染线程读） ──
    private static volatile int s19EntityId = -1;
    private static volatile long s19Time = 0;

    // ── 渲染线程状态 ──
    private int comboCount = 0;
    private long lastConfirmTime = 0;
    private int pendingEntityId = -1;
    private boolean alreadyConfirmed = false;  // 当前 pending 是否已确认
    private boolean injected = false;

    // ================================================================
    //  连接管理：世界加载/重连时重置
    // ================================================================

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null) {
            // 断开连接 → 重置状态
            injected = false;
            comboCount = 0;
            pendingEntityId = -1;
            alreadyConfirmed = false;
            s19EntityId = -1;
            s19Time = 0;
        }
    }

    // ================================================================
    //  攻击事件：记下目标
    // ================================================================

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.entityPlayer != mc.thePlayer) return;
        if (mc.theWorld == null) return;

        // 记下目标实体 ID
        pendingEntityId = event.target.getEntityId();
        alreadyConfirmed = false;

        // 懒注入 Netty Pipeline
        if (!injected) {
            injectPipeline();
        }
    }

    // ================================================================
    //  Netty Pipeline 注入
    // ================================================================

    private void injectPipeline() {
        try {
            NetworkManager netManager = mc.thePlayer.sendQueue.getNetworkManager();
            Channel channel = netManager.channel();
            if (channel == null || !channel.isOpen()) return;

            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                injected = true; // 已有
                return;
            }

            pipeline.addBefore("packet_handler", HANDLER_NAME, new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (msg instanceof S19PacketEntityStatus && S19_ENTITY_ID != null) {
                        S19PacketEntityStatus pkt = (S19PacketEntityStatus) msg;
                        // opcode=2 表示 "entity was damaged by a hit"（服务端确认）
                        if (pkt.getOpCode() == 2) {
                            try {
                                int id = S19_ENTITY_ID.getInt(pkt);
                                s19EntityId = id;
                                s19Time = System.currentTimeMillis();
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    ctx.fireChannelRead(msg); // 原样放行，零影响
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                    ctx.fireExceptionCaught(cause);
                }
            });

            injected = true;
        } catch (Exception e) {
            injected = false; // 下次重试
        }
    }

    // ================================================================
    //  渲染：读取 S19 确认态，更新连击
    // ================================================================

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.enableCombo) return;

        long now = System.currentTimeMillis();

        // ── 读取 Netty 线程写的最新 S19 命中确认 ──
        int hitId = s19EntityId;
        long hitTime = s19Time;

        // 未确认、目标匹配、时效内 → 确认命中
        if (!alreadyConfirmed
                && pendingEntityId != -1
                && hitId == pendingEntityId
                && now - hitTime < CONFIRM_FRESH_MS) {

            if (now - lastConfirmTime > COMBO_TIMEOUT_MS) {
                comboCount = 1;
            } else {
                comboCount++;
            }
            lastConfirmTime = now;
            alreadyConfirmed = true;

            // 消费掉这条 S19，避免下一帧重复
            s19EntityId = -1;
        }

        // ── 超时重置 ──
        if (comboCount > 0 && now - lastConfirmTime > COMBO_TIMEOUT_MS) {
            comboCount = 0;
            pendingEntityId = -1;
            alreadyConfirmed = false;
        }

        // ── 渲染 ──
        if (comboCount <= 0) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        int baseX = sw - 50 + cfg.comboXOffset;
        int baseY = sh - 40 + cfg.comboYOffset;

        int color;
        if (comboCount <= 3)       color = 0xFF55FF55;
        else if (comboCount <= 7)  color = 0xFFFFFF55;
        else if (comboCount <= 15) color = 0xFFFFAA55;
        else                       color = 0xFFFF5555;

        String text = "Combo: " + comboCount;
        int tw = mc.fontRendererObj.getStringWidth(text);

        Gui.drawRect(baseX - 4, baseY - 2, baseX + tw + 4, baseY + 10, 0x66000000);
        HUDEditManager.report("连击计数", baseX - 4, baseY - 2, tw + 8, 12);

        GlStateManager.enableBlend();
        mc.fontRendererObj.drawStringWithShadow(text, baseX, baseY, color);
    }
}
