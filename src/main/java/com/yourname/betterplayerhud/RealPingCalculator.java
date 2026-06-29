package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public class RealPingCalculator {
    private static final long PING_EXPIRY_TIME = 5000L; // 延长到5秒
    private static final int MAX_PING = 10000;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ConcurrentHashMap<Integer, Long> sentPackets = new ConcurrentHashMap<>();

    private volatile long lastRealPing = 0;
    private volatile long lastPingUpdate = 0;
    private Field keepAliveField;
    private boolean debugMode = true;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !BetterPlayerHUD.config.showRealPing) return;
        cleanupOldPackets();
    }

    public void onPacketSent(Packet<?> packet) {
        if (!shouldProcessPacket()) return;

        long currentTime = System.currentTimeMillis();

        // 修复：确保所有相关包都被跟踪
        if (packet instanceof C03PacketPlayer) {
           // if (debugMode) System.out.println("[RealPing] 发送C03PacketPlayer");
            trackPacket(packet, currentTime);
        } else if (packet instanceof C00PacketKeepAlive) {
           // if (debugMode) System.out.println("[RealPing] 发送C00PacketKeepAlive");
            trackPacket(packet, currentTime);
        }
    }

    public void onPacketReceived(Packet<?> packet) {
        if (!shouldProcessPacket()) return;

        long currentTime = System.currentTimeMillis();

        if (packet instanceof S00PacketKeepAlive) {
           // if (debugMode) System.out.println("[RealPing] 接收S00PacketKeepAlive");
            handleKeepAlivePacket((S00PacketKeepAlive) packet, currentTime);
        } else if (packet instanceof S03PacketTimeUpdate) {
         //   if (debugMode) System.out.println("[RealPing] 接收S03PacketTimeUpdate");
            handleTimeUpdatePacket(currentTime);
        } else if (packet instanceof net.minecraft.network.play.server.S18PacketEntityTeleport) {
            // 添加更多可用于测量的包类型
           // if (debugMode) System.out.println("[RealPing] 接收S18PacketEntityTeleport");
            handleGenericResponsePacket(currentTime);
        }
    }

    private boolean shouldProcessPacket() {
        boolean shouldProcess = BetterPlayerHUD.config.showRealPing && mc.thePlayer != null;
      /*  if (debugMode && !shouldProcess) {
            System.out.println("[RealPing] 跳过包处理: showRealPing=" + BetterPlayerHUD.config.showRealPing + ", playerNull=" + (mc.thePlayer == null));
        }*/
        return shouldProcess;
    }

    private void trackPacket(Packet<?> packet, long sendTime) {
        int packetId = getPacketIdentifier(packet);
        sentPackets.put(packetId, sendTime);
      //  if (debugMode) System.out.println("[RealPing] 跟踪包 ID: " + packetId + ", 时间: " + sendTime);
    }

    private void handleKeepAlivePacket(S00PacketKeepAlive keepAlive, long receiveTime) {
        int packetId = getServerKeepAliveId(keepAlive);
        if (packetId != -1) {
            calculateRealPing(packetId, receiveTime);
        } else {
       //     if (debugMode) System.out.println("[RealPing] 无法获取KeepAlive包ID");
        }
    }

    private void handleTimeUpdatePacket(long receiveTime) {
        // 使用最近发送的包来计算延迟
        calculateRealPing(-1, receiveTime);
    }

    private void handleGenericResponsePacket(long receiveTime) {
        // 通用响应包处理
        calculateRealPing(-2, receiveTime);
    }

    private void calculateRealPing(int packetId, long receiveTime) {
        Long sendTime = null;

        if (packetId >= 0) {
            // 特定包ID匹配
            sendTime = sentPackets.remove(packetId);
        } else if (packetId == -1) {
            // 使用最近发送的KeepAlive包
            sendTime = getMostRecentSendTime(C00PacketKeepAlive.class);
        } else {
            // 使用最近发送的任何包
            sendTime = getMostRecentSendTime();
        }

        if (sendTime != null) {
            long ping = receiveTime - sendTime;
            if (isValidPing(ping)) {
                updatePing(ping);
           //     if (debugMode) System.out.println("[RealPing] 计算Ping: " + ping + "ms");
            } else {
           //     if (debugMode) System.out.println("[RealPing] 无效Ping值: " + ping + "ms");
            }
        } else {
        //    if (debugMode) System.out.println("[RealPing] 未找到对应的发送包");
        }
    }

    private Long getMostRecentSendTime() {
        return getMostRecentSendTime(null);
    }

    private Long getMostRecentSendTime(Class<?> packetClass) {
        long mostRecent = 0;
        Long mostRecentTime = null;

        for (ConcurrentHashMap.Entry<Integer, Long> entry : sentPackets.entrySet()) {
            if (entry.getValue() > mostRecent) {
                mostRecent = entry.getValue();
                mostRecentTime = entry.getValue();
            }
        }

        return mostRecentTime;
    }

    private boolean isValidPing(long ping) {
        return ping > 0 && ping < MAX_PING;
    }

    private void updatePing(long ping) {
        lastRealPing = ping;
        lastPingUpdate = System.currentTimeMillis();
    }

    private int getPacketIdentifier(Packet<?> packet) {
        if (packet instanceof C00PacketKeepAlive) {
            return ((C00PacketKeepAlive) packet).getKey();
        }
        return System.identityHashCode(packet);
    }

    private int getServerKeepAliveId(S00PacketKeepAlive keepAlive) {
        try {
            if (keepAliveField == null) {
                keepAliveField = findKeepAliveField();
            }
            if (keepAliveField != null) {
                keepAliveField.setAccessible(true);
                return (Integer) keepAliveField.get(keepAlive);
            }
        } catch (Exception e) {
    //        if (debugMode) System.out.println("[RealPing] 反射获取KeepAliveID失败: " + e.getMessage());
        }
        return -1;
    }

    private Field findKeepAliveField() {
        String[] fieldNames = {"field_149136_a", "field_149136_c", "key"};
        for (String fieldName : fieldNames) {
            try {
                Field field = S00PacketKeepAlive.class.getDeclaredField(fieldName);
              //  if (debugMode) System.out.println("[RealPing] 找到KeepAlive字段: " + fieldName);
                return field;
            } catch (NoSuchFieldException e) {
                // 继续尝试下一个
            }
        }
   //     if (debugMode) System.out.println("[RealPing] 未找到KeepAlive字段");
        return null;
    }

    private void cleanupOldPackets() {
        long currentTime = System.currentTimeMillis();
        int initialSize = sentPackets.size();
        sentPackets.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > 5000
        );
        if (debugMode && initialSize != sentPackets.size()) {
       //     System.out.println("[RealPing] 清理包: " + (initialSize - sentPackets.size()));
        }
    }

    public long getRealPing() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPingUpdate > PING_EXPIRY_TIME) {
         //   if (debugMode) System.out.println("[RealPing] Ping已过期，使用服务器Ping");
            return getServerReportedPing();
        }
    //    if (debugMode) System.out.println("[RealPing] 返回真实Ping: " + lastRealPing + "ms");
        return lastRealPing;
    }

    private long getServerReportedPing() {
        if (mc.getNetHandler() == null || mc.thePlayer == null) {
       //     if (debugMode) System.out.println("[RealPing] 无法获取服务器Ping");
            return 0;
        }

        try {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getGameProfile().getId());
            long ping = playerInfo != null ? playerInfo.getResponseTime() : 0;
        //    if (debugMode) System.out.println("[RealPing] 服务器报告Ping: " + ping + "ms");
            return ping;
        } catch (Exception e) {
        //    if (debugMode) System.out.println("[RealPing] 获取服务器Ping异常: " + e.getMessage());
            return 0;
        }
    }
}