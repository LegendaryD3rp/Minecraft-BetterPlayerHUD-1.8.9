package com.yourname.betterplayerhud;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 性能检测 HUD — 整合 FPS / 坐标 / TPS / Ping / 服务器IP
 *
 * TPS 估算：拦截 S03PacketTimeUpdate（服务器每20tick发一次），
 *   实际间隔 / 1000ms × 20 = 实时 TPS，指数平滑。
 * Ping：NetworkPlayerInfo.getResponseTime()（服务端下发的值）。
 * FPS：Minecraft.getDebugFPS()。
 * 坐标：player.posX / posY / posZ。
 * 全部纯客户端，零额外发包。
 */
@SideOnly(Side.CLIENT)
public class PerformanceHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── TPS 估算 ──
    private long lastTimePacketMs = 0;
    private float currentTPS = 20.0f;
    private static final float TPS_SMOOTHING = 0.20f;
    private boolean prevConnected = false;

    // ── Ping / IP 缓存 ──
    private int lastPing = 0;
    private long lastPingCheck = 0;
    private static final long PING_REFRESH_MS = 500;
    private String cachedServerIP = "";

    // ================================================================
    //  ClientTickEvent — pipeline 注入 & 数据刷新
    // ================================================================
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        boolean connected = isConnected();

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

        if (connected) {
            long now = System.currentTimeMillis();
            if (now - lastPingCheck > PING_REFRESH_MS) {
                NetworkPlayerInfo info = getSelfPlayerInfo();
                lastPing = (info != null) ? info.getResponseTime() : 0;
                ServerData sd = mc.getCurrentServerData();
                cachedServerIP = (sd != null) ? sd.serverIP : "";
                lastPingCheck = now;
            }
        }
    }

    // ================================================================
    //  Netty pipeline 注入（仅 TPS）
    // ================================================================
    private void injectTpsTracker() {
        try {
            NetworkManager nm = mc.getNetHandler().getNetworkManager();
            if (nm == null) return;
            Channel ch = nm.channel();
            if (ch == null || !ch.isOpen()) return;
            if (ch.pipeline().get("tps_tracker") != null) return;

            ch.pipeline().addBefore("packet_handler", "tps_tracker",
                new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        if (msg instanceof S03PacketTimeUpdate) onTimePacket();
                        ctx.fireChannelRead(msg);
                    }
                });
        } catch (Exception e) {
            // 连接竞争条件，静默处理
        }
    }

    private void onTimePacket() {
        long now = System.currentTimeMillis();
        if (lastTimePacketMs != 0) {
            long interval = now - lastTimePacketMs;
            if (interval > 10 && interval < 60000) {
                float instantTPS = 20.0f * (1000.0f / interval);
                if (instantTPS > 20.0f) instantTPS = 20.0f;
                if (instantTPS < 0.0f) instantTPS = 0.0f;
                currentTPS = currentTPS * (1.0f - TPS_SMOOTHING) + instantTPS * TPS_SMOOTHING;
            }
        }
        lastTimePacketMs = now;
    }

    // ================================================================
    //  RenderGameOverlayEvent — 渲染
    // ================================================================
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!BetterPlayerHUD.config.showPerformanceHUD) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        ScaledResolution res = new ScaledResolution(mc);
        int sw = res.getScaledWidth(), sh = res.getScaledHeight();
        FontRenderer fr = mc.fontRendererObj;

        int baseX = BetterPlayerHUD.config.performanceHudX;
        int baseY = BetterPlayerHUD.config.performanceHudY;
        if (baseX < 0) baseX = sw + baseX;
        if (baseY < 0) baseY = sh + baseY;

        int color = BetterPlayerHUD.config.performanceTextColor;
        int lineH = 10;
        int y = baseY;

        // ── FPS ──
        if (BetterPlayerHUD.config.showFPS) {
            int fps = Minecraft.getDebugFPS();
            int fpsColor = fps >= 60 ? 0x55FF55 : (fps >= 30 ? 0xFFFF55 : 0xFF5555);
            fr.drawStringWithShadow("FPS: " + fps, baseX, y, fpsColor);
            y += lineH;
        }

        // ── 坐标 XYZ ──
        if (BetterPlayerHUD.config.showXYZ) {
            int px = (int) Math.floor(mc.thePlayer.posX);
            int py = (int) Math.floor(mc.thePlayer.posY);
            int pz = (int) Math.floor(mc.thePlayer.posZ);
            fr.drawStringWithShadow(String.format("XYZ: %d / %d / %d", px, py, pz), baseX, y, color);
            y += lineH;
        }

        // ── TPS ──
        if (BetterPlayerHUD.config.showTPS) {
            String tpsStr = String.format("TPS: %.1f", currentTPS);
            int tpsColor = getTpsColor(currentTPS);
            fr.drawStringWithShadow(tpsStr, baseX, y, tpsColor);
            y += lineH;
        }

        // ── Ping ──
        if (BetterPlayerHUD.config.showPing) {
            String pingStr = (lastPing > 0) ? String.format("Ping: %dms", lastPing) : "Ping: --";
            fr.drawStringWithShadow(pingStr, baseX, y, pingStr.contains("--") ? color : getPingColor(lastPing));
            y += lineH;
        }

        // ── 服务器 IP ──
        if (BetterPlayerHUD.config.showServerIP) {
            String ip = (cachedServerIP != null && !cachedServerIP.isEmpty()) ? cachedServerIP : "Singleplayer";
            fr.drawStringWithShadow(ip, baseX, y, color);
        }

        if (HUDEditManager.isEditing())
            HUDEditManager.report("性能检测", baseX, baseY, 130, y + 10 - baseY);
    }

    // ================================================================
    //  颜色辅助
    // ================================================================
    private int getTpsColor(float tps) {
        if (tps >= BetterPlayerHUD.config.serverInfoGoodTpsThreshold) return BetterPlayerHUD.config.serverInfoTPSGoodColor;
        if (tps >= BetterPlayerHUD.config.serverInfoMediumTpsThreshold) return BetterPlayerHUD.config.serverInfoTPSMediumColor;
        return BetterPlayerHUD.config.serverInfoTPSBadColor;
    }

    private int getPingColor(int ping) {
        if (ping <= 0) return BetterPlayerHUD.config.performanceTextColor;
        if (ping < 100) return 0x55FF55;
        if (ping < 300) return 0xFFFF55;
        return 0xFF5555;
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
