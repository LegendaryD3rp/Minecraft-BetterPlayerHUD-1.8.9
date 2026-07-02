package com.yourname.betterplayerhud;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 服务器信息 HUD（TPS 估算 / 真实 Ping / 服务器 IP）。
 *
 * TPS 估算原理：
 *   S03PacketTimeUpdate 由服务器每 20 tick 发送一次。
 *   正常 20 TPS 时到达间隔 = 1000ms。
 *   TPS = 20 × (1000 / 实测间隔)。
 *   通过 Netty pipeline 注入拦截该包，不修改包内容。
 *
 * Ping 显示：
 *   mc.getNetHandler().getPlayerInfo(uuid).getResponseTime()
 *   这是服务端下发的 ping 值（更新于 S38PacketPlayerListItem）。
 *
 * 服务器 IP：
 *   mc.getCurrentServerData().serverIP
 *
 * 全部纯客户端，无额外网络请求。
 */
@SideOnly(Side.CLIENT)
public class ServerInfoHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── TPS 估算状态 ──────────────────────────────────────────────
    private long lastTimePacketMs = 0;          // 上次 S03PacketTimeUpdate 到达时间戳
    private float currentTPS = 20.0f;           // 平滑后 TPS
    private static final float TPS_SMOOTHING = 0.20f;
    private boolean prevConnected = false;      // 用于检测连接状态变化

    // ── Ping 缓存 ──────────────────────────────────────────────────
    private int lastPing = 0;
    private long lastPingCheck = 0;
    private static final long PING_REFRESH_MS = 500;   // 每半秒刷新一次

    // ── IP 缓存 ──────────────────────────────────────────────────
    private String cachedServerIP = "";

    // ================================================================
    //  ClientTickEvent — 管理 pipeline 注入 & 数据刷新
    // ================================================================
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        boolean connected = isConnected();

        // 连接状态变化时重置
        if (connected != prevConnected) {
            if (connected) {
                injectTpsTracker();
            } else {
                lastTimePacketMs = 0;
                currentTPS = 20.0f;
                lastPing = 0;
                cachedServerIP = "";
            }
            prevConnected = connected;
        }

        // 每秒刷新 Ping / IP
        if (connected) {
            long now = System.currentTimeMillis();
            if (now - lastPingCheck > PING_REFRESH_MS) {
                NetworkPlayerInfo info = getSelfPlayerInfo();
                if (info != null) {
                    lastPing = info.getResponseTime();
                } else {
                    lastPing = 0;
                }

                ServerData sd = mc.getCurrentServerData();
                cachedServerIP = (sd != null) ? sd.serverIP : "";
                lastPingCheck = now;
            }
        }
    }

    // ================================================================
    //  Netty pipeline 注入
    // ================================================================
    private void injectTpsTracker() {
        try {
            NetworkManager nm = mc.getNetHandler().getNetworkManager();
            if (nm == null) return;

            Channel ch = nm.channel();
            if (ch == null || !ch.isOpen()) return;

            // 已在 pipeline 中，跳过
            if (ch.pipeline().get("tps_tracker") != null) return;

            ch.pipeline().addBefore("packet_handler", "tps_tracker",
                new SimpleChannelInboundHandler<Packet>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
                        if (packet instanceof S03PacketTimeUpdate) {
                            onTimePacket();
                        }
                        ctx.fireChannelRead(packet);
                    }
                });
        } catch (Exception e) {
            // pipeline 注入可能因连接状态竞争条件失败，静默处理
        }
    }

    /**
     * S03PacketTimeUpdate 到达回调 — 在 Netty 事件循环线程中调用。
     */
    private void onTimePacket() {
        long now = System.currentTimeMillis();
        if (lastTimePacketMs != 0) {
            long interval = now - lastTimePacketMs;
            if (interval > 10 && interval < 60000) {
                // S03PacketTimeUpdate 每 20 服务器 tick 发送一次
                // 正常 20 TPS → interval ≈ 1000ms
                float instantTPS = 20.0f * (1000.0f / interval);
                if (instantTPS > 20.0f) instantTPS = 20.0f;
                if (instantTPS < 0.0f) instantTPS = 0.0f;
                currentTPS = currentTPS * (1.0f - TPS_SMOOTHING) + instantTPS * TPS_SMOOTHING;
            }
        }
        lastTimePacketMs = now;
    }

    // ================================================================
    //  RenderGameOverlayEvent — HUD 渲染
    // ================================================================
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!BetterPlayerHUD.config.showServerInfo) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        ScaledResolution res = new ScaledResolution(mc);
        int screenWidth = res.getScaledWidth();
        int screenHeight = res.getScaledHeight();
        FontRenderer fr = mc.fontRendererObj;

        int baseX = BetterPlayerHUD.config.serverInfoX;
        int baseY = BetterPlayerHUD.config.serverInfoY;

        // 支持负数坐标（从底部/右侧偏移）
        if (baseX < 0) baseX = screenWidth + baseX;
        if (baseY < 0) baseY = screenHeight + baseY;

        int color = BetterPlayerHUD.config.serverInfoColor;
        int lineHeight = 10;
        int y = baseY;

        // ── TPS ──
        if (BetterPlayerHUD.config.serverInfoShowTPS) {
            String tpsStr = String.format("TPS: %.1f", currentTPS);
            int tpsColor = getTpsColor(currentTPS);
            fr.drawStringWithShadow(tpsStr, baseX, y, tpsColor);
            y += lineHeight;
        }

        // ── Ping ──
        if (BetterPlayerHUD.config.serverInfoShowPing) {
            String pingStr = (lastPing > 0)
                ? String.format("Ping: %dms", lastPing)
                : "Ping: --";
            int pingColor = getPingColor(lastPing);
            fr.drawStringWithShadow(pingStr, baseX, y, pingColor);
            y += lineHeight;
        }

        // ── 服务器 IP ──
        if (BetterPlayerHUD.config.serverInfoShowServerIP) {
            String ip = cachedServerIP;
            if (ip == null || ip.isEmpty()) {
                ip = "Singleplayer";
            }
            fr.drawStringWithShadow(ip, baseX, y, color);
            // y += lineHeight;
        }
    }

    // ================================================================
    //  颜色辅助
    // ================================================================
    private int getTpsColor(float tps) {
        if (tps >= BetterPlayerHUD.config.serverInfoGoodTpsThreshold) {
            return BetterPlayerHUD.config.serverInfoTPSGoodColor;
        } else if (tps >= BetterPlayerHUD.config.serverInfoMediumTpsThreshold) {
            return BetterPlayerHUD.config.serverInfoTPSMediumColor;
        } else {
            return BetterPlayerHUD.config.serverInfoTPSBadColor;
        }
    }

    private int getPingColor(int ping) {
        if (ping <= 0) return BetterPlayerHUD.config.serverInfoColor;
        if (ping < 100) return 0x55FF55;         // good
        if (ping < 300) return 0xFFFF55;         // medium
        return 0xFF5555;                          // bad
    }

    // ================================================================
    //  工具
    // ================================================================
    private boolean isConnected() {
        return mc.getNetHandler() != null
            && mc.getNetHandler().getNetworkManager() != null
            && mc.getNetHandler().getNetworkManager().channel() != null
            && mc.getNetHandler().getNetworkManager().channel().isOpen();
    }

    private NetworkPlayerInfo getSelfPlayerInfo() {
        if (mc.getNetHandler() == null || mc.thePlayer == null) return null;
        return mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
    }
}
