package com.yourname.compassmod;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;

public class EnhancedPingHUDHandler {
    private static final long PING_CACHE_DURATION = 1000L;
    private static final int MAX_PING_VALUE = 10000;

    private static final String PIPELINE_NAME = "ping_interceptor";

    private final Minecraft mc = Minecraft.getMinecraft();
    private final RealPingCalculator realPingCalc;
    private final TrafficBasedPingCalculator trafficPingCalc;

    private long lastPingUpdate = 0;
    private PingData cachedPingData;
    private boolean pipelineInjected = false;

    // 调试标志（默认关闭，如需调试可在代码中改为 true）
    private boolean debugMode = false;

    public EnhancedPingHUDHandler() {
        this.realPingCalc = new RealPingCalculator();
        this.trafficPingCalc = new TrafficBasedPingCalculator();
    }

    /**
     * 连接服务器时注入 Netty Pipeline，拦截收发包。
     */
    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        pipelineInjected = false;
        injectPipeline(event.manager);
    }

    /**
     * 断开连接时清理状态，防止断线重连后重复注入。
     */
    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        pipelineInjected = false;
        cachedPingData = null;
        lastPingUpdate = 0;
    }

    /**
     * 通过反射获取 NetworkManager.channel 字段，注入 ChannelDuplexHandler。
     * 兼容 MCP 名（channel）和 SRG 名（field_150746_k）。
     */
    private void injectPipeline(NetworkManager manager) {
        if (manager == null || pipelineInjected) return;
        try {
            Field channelField = null;
            try {
                channelField = NetworkManager.class.getDeclaredField("channel");
            } catch (NoSuchFieldException e) {
                channelField = NetworkManager.class.getDeclaredField("field_150746_k");
            }
            channelField.setAccessible(true);
            Channel channel = (Channel) channelField.get(manager);
            if (channel == null) return;

            // 避免重复注入
            if (channel.pipeline().get(PIPELINE_NAME) != null) {
                pipelineInjected = true;
                return;
            }

            channel.pipeline().addBefore("packet_handler", PIPELINE_NAME, new ChannelDuplexHandler() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if (msg instanceof Packet) {
                        onPacketSent((Packet<?>) msg);
                    }
                    super.write(ctx, msg, promise);
                }

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (msg instanceof Packet) {
                        onPacketReceived((Packet<?>) msg);
                    }
                    super.channelRead(ctx, msg);
                }
            });

            pipelineInjected = true;
            System.out.println("[BetterPlayerHUD] Ping interceptor injected into pipeline");
        } catch (Exception e) {
            System.err.println("[BetterPlayerHUD] Failed to inject ping interceptor: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (shouldSkipRendering(event)) return;

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        renderEnhancedPingHUD(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
    }

    private boolean shouldSkipRendering(RenderGameOverlayEvent event) {
        return event.type != RenderGameOverlayEvent.ElementType.TEXT ||
                mc.thePlayer == null ||
                mc.gameSettings.hideGUI ||
                !BetterPlayerHUD.config.showPingHUD;
    }

    private void renderEnhancedPingHUD(int screenWidth, int screenHeight) {
        FontRenderer fr = mc.fontRendererObj;
        PingData pingData = getCurrentPingData();

        // 调试信息：显示所有三种ping值
        if (debugMode) {
            String debugText = String.format("服务器: %dms | 真实: %dms | 流量: %dms",
                    pingData.serverPing, pingData.realPing, pingData.trafficPing);
            fr.drawStringWithShadow(debugText, 5, 5, 0xFFFFFF);
        }

        String pingText = formatPingText(pingData);
        int textWidth = fr.getStringWidth(pingText);

        int xPos = calculatePingXPosition(screenWidth, textWidth);
        int yPos = calculatePingYPosition(screenHeight, 10);
        int color = getPingColor(pingData.displayPing);

        fr.drawStringWithShadow(pingText, xPos, yPos, color);
        renderNetworkDetails(fr, xPos, yPos, pingData);
    }

    private PingData getCurrentPingData() {
        long currentTime = System.currentTimeMillis();
        if (cachedPingData != null && currentTime - lastPingUpdate < PING_CACHE_DURATION) {
            return cachedPingData;
        }

        // 确保所有三种方法都被调用
        long serverPing = getServerReportedPing();
        long realPing = realPingCalc.getRealPing();
        long trafficPing = trafficPingCalc.getTrafficBasedPing();

        // 调试输出
      //  if (debugMode) {
    //        System.out.printf("[PingDebug] 服务器: %d, 真实: %d, 流量: %d%n",
        //            serverPing, realPing, trafficPing);
     //   }

        long displayPing = selectBestPingEstimate(serverPing, realPing, trafficPing);

        cachedPingData = new PingData(serverPing, realPing, trafficPing, displayPing);
        lastPingUpdate = currentTime;
        return cachedPingData;
    }

    private long selectBestPingEstimate(long serverPing, long realPing, long trafficPing) {
        int trafficConfidence = trafficPingCalc.getDataConfidence();

        // 调试信息
        if (debugMode) {
         //   System.out.printf("[PingSelect] 流量可信度: %d%%, 流量Ping: %d, 真实Ping: %d%n",
         //           trafficConfidence, trafficPing, realPing);
        }

        // 修复：确保所有方法都有机会被选择
        boolean hasValidTrafficPing = trafficPing > 0 && trafficPing < MAX_PING_VALUE;
        boolean hasValidRealPing = realPing > 0 && realPing < MAX_PING_VALUE;
        boolean hasValidServerPing = serverPing > 0 && serverPing < MAX_PING_VALUE;

        // 优先级逻辑修复
        if (hasValidTrafficPing && trafficConfidence > 30) { // 降低阈值到30%
       //     if (debugMode) System.out.println("[PingSelect] 选择流量分析Ping");
            return trafficPing;
        }

        if (hasValidRealPing) {
         //   if (debugMode) System.out.println("[PingSelect] 选择真实Ping");
            return realPing;
        }

        if (hasValidServerPing) {
       //     if (debugMode) System.out.println("[PingSelect] 选择服务器Ping");
            return serverPing;
        }

     //   if (debugMode) System.out.println("[PingSelect] 使用默认值0");
        return 0;
    }

    private String formatPingText(PingData pingData) {
        StringBuilder sb = new StringBuilder();
        sb.append("延迟: ").append(pingData.displayPing).append("ms");

        String quality = trafficPingCalc.getPingQualityIndicator();
        String confidenceIndicator = getConfidenceIndicator(trafficPingCalc.getDataConfidence());

        sb.append(" [").append(quality).append(confidenceIndicator).append("]");

        // 检测可能的ping伪造
        if (pingData.serverPing <= 1 && (pingData.realPing > 50 || pingData.trafficPing > 50)) {
            sb.append(" *");
        }

        return sb.toString();
    }

    private String getConfidenceIndicator(int confidence) {
        if (confidence >= 80) return "✓";
        if (confidence >= 60) return "~";
        if (confidence >= 40) return "?";
        return "!";
    }

    private void renderNetworkDetails(FontRenderer fr, int xPos, int yPos, PingData pingData) {
        if (!BetterPlayerHUD.config.showNetworkDetails) return;

        String[] details = {
                "质量: " + trafficPingCalc.getPingQualityIndicator(),
                "方法: " + trafficPingCalc.getEstimationMethod(),
                "可信度: " + trafficPingCalc.getDataConfidence() + "%",
                "服务器Ping: " + pingData.serverPing + "ms",
                "真实Ping: " + pingData.realPing + "ms"
        };

        for (int i = 0; i < details.length; i++) {
            fr.drawStringWithShadow(details[i], xPos, yPos + 10 + (i * 10), 0xAAAAAA);
        }
    }

    private long getServerReportedPing() {
        if (mc.getNetHandler() == null || mc.thePlayer == null) {
        //    if (debugMode) System.out.println("[ServerPing] 无法获取服务器Ping: 网络处理器或玩家为null");
            return 0;
        }

        try {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            long ping = playerInfo != null ? playerInfo.getResponseTime() : 0;
            //       if (debugMode) System.out.println("[ServerPing] 获取到服务器Ping: " + ping + "ms");
            return ping;
        } catch (Exception e) {
      //      if (debugMode) System.out.println("[ServerPing] 获取服务器Ping异常: " + e.getMessage());
            return 0;
        }
    }

    private int getPingColor(long ping) {
        if (ping <= 0) return BetterPlayerHUD.config.pingColor;

        if (ping <= BetterPlayerHUD.config.goodPingThreshold) {
            return BetterPlayerHUD.config.pingGoodColor;
        } else if (ping <= BetterPlayerHUD.config.mediumPingThreshold) {
            return BetterPlayerHUD.config.pingMediumColor;
        } else {
            return BetterPlayerHUD.config.pingBadColor;
        }
    }

    private int calculatePingXPosition(int screenWidth, int textWidth) {
        int offset = BetterPlayerHUD.config.pingHudX;
        return offset >= 0 ? offset : screenWidth + offset - textWidth;
    }

    private int calculatePingYPosition(int screenHeight, int textHeight) {
        int offset = BetterPlayerHUD.config.pingHudY;
        return offset >= 0 ? offset : screenHeight + offset - textHeight;
    }

    // 添加方法用于数据包处理
    public void onPacketSent(net.minecraft.network.Packet<?> packet) {
        realPingCalc.onPacketSent(packet);
        trafficPingCalc.onPacketSent(packet); // 新增
    }

    public void onPacketReceived(net.minecraft.network.Packet<?> packet) {
        realPingCalc.onPacketReceived(packet);
        trafficPingCalc.onPacketReceived(packet); // 新增
    }
    private static class PingData {
        final long serverPing;
        final long realPing;
        final long trafficPing;
        final long displayPing;

        PingData(long serverPing, long realPing, long trafficPing, long displayPing) {
            this.serverPing = serverPing;
            this.realPing = realPing;
            this.trafficPing = trafficPing;
            this.displayPing = displayPing;
        }
    }
}