package com.yourname.betterplayerhud;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 共享的 S19 命中确认管理器。
 *
 * 在 Netty pipeline 中注入一个只读 handler，
 * 拦截 S19PacketEntityStatus(opcode=2) 获取 entityId，
 * 通知所有注册的监听器。
 *
 * 零协议影响 — 通过 ctx.fireChannelRead(msg) 原样透传。
 */
@SideOnly(Side.CLIENT)
public class S19HitManager {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String HANDLER_NAME = "bhud_s19_manager";

    // ── 反射读取 S19PacketEntityStatus 的私有 entityId 字段 ──
    private static final Field S19_ENTITY_ID;
    static {
        Field f = null;
        try {
            f = S19PacketEntityStatus.class.getDeclaredField("entityId");
            f.setAccessible(true);
        } catch (Exception e) {
            try {
                f = S19PacketEntityStatus.class.getDeclaredField("field_149164_a");
                f.setAccessible(true);
            } catch (Exception e2) {
                // 都找不到 = 环境异常，放弃
            }
        }
        S19_ENTITY_ID = f;
    }

    // ── 跨线程通信 ──
    private static volatile int lastHitEntityId = -1;
    private static volatile long lastHitTime = 0;

    // ── 监听器 ──
    public interface S19Listener {
        /** 在 Netty 线程调用，entityId 为被命中的实体 */
        void onHitConfirmed(int entityId, long time);
    }
    private static final CopyOnWriteArrayList<S19Listener> listeners = new CopyOnWriteArrayList<>();

    private static boolean injected = false;

    // ── 公共 API ──

    public static void registerListener(S19Listener listener) {
        listeners.add(listener);
        injectIfNeeded();
    }

    public static void unregisterListener(S19Listener listener) {
        listeners.remove(listener);
    }

    /** 最近一次 S19 命中的实体 ID（可在渲染线程读） */
    public static int getLastHitEntityId() { return lastHitEntityId; }

    /** 最近一次 S19 命中的时间戳（ms） */
    public static long getLastHitTime() { return lastHitTime; }

    /** 世界断开时重置 */
    public static void reset() {
        injected = false;
        lastHitEntityId = -1;
        lastHitTime = 0;
    }

    // ── Pipeline 注入 ──

    private static void injectIfNeeded() {
        if (injected) return;
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        try {
            NetworkManager netManager = mc.thePlayer.sendQueue.getNetworkManager();
            Channel channel = netManager.channel();
            if (channel == null || !channel.isOpen()) return;

            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                injected = true;
                return;
            }

            pipeline.addBefore("packet_handler", HANDLER_NAME, new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (msg instanceof S19PacketEntityStatus && S19_ENTITY_ID != null) {
                        S19PacketEntityStatus pkt = (S19PacketEntityStatus) msg;
                        try {
                            int entityId = S19_ENTITY_ID.getInt(pkt);
                            // pkt.getOpCode() 返回 byte
                            byte opCode = pkt.getOpCode();
                            if (opCode == 2) {
                                long now = System.currentTimeMillis();
                                lastHitEntityId = entityId;
                                lastHitTime = now;
                                for (S19Listener l : listeners) {
                                    l.onHitConfirmed(entityId, now);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    ctx.fireChannelRead(msg);
                }
            });

            injected = true;
        } catch (Exception e) {
            // 注入失败 = 下次重试
        }
    }
}
