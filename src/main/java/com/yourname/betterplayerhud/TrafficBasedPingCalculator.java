package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TrafficBasedPingCalculator {
    private static final int MAX_SAMPLES = 20;
    private static final long ANALYSIS_INTERVAL = 50L;
    private static final double MOVEMENT_THRESHOLD = 0.1;
    private static final int MOVEMENT_SAMPLE_INTERVAL = 10;
    private static final int INTERACTION_SAMPLE_INTERVAL = 3;
    private static final long PACKET_TIMEOUT = 5000L;

    private final Minecraft mc = Minecraft.getMinecraft();

    // 响应时间样本存储
    private final Deque<Long> movementResponseTimes = new ArrayDeque<>(MAX_SAMPLES);
    private final Deque<Long> interactionResponseTimes = new ArrayDeque<>(10);

    // 网络包跟踪
    private final ConcurrentHashMap<String, Long> sentPackets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> sentMovementPackets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> sentInteractionPackets = new ConcurrentHashMap<>();

    private final AtomicLong estimatedPing = new AtomicLong(0);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private long lastAnalysisTime = 0;
    private double lastPosX, lastPosY, lastPosZ;
    private long lastMovementPacketTime = 0;
    private long lastInteractionTime = 0;
    private int movementCounter = 0;
    private int interactionCounter = 0;
    private int packetCounter = 0;
    private boolean debugMode = true;

    /**
     * 外部调用的包发送事件处理方法
     */
    public void onPacketSent(Packet<?> packet) {
        if (!shouldProcess()) return;

        long currentTime = System.currentTimeMillis();

        // 记录通用包发送时间
        String packetType = packet.getClass().getSimpleName();
        sentPackets.put(packetType, currentTime);

        // 特殊处理移动包
        if (packet instanceof C03PacketPlayer) {
            trackMovementPacket(packet, currentTime);
        }

        /*   if (debugMode) {
            System.out.println("[TrafficPing] 发送包: " + packetType + ", 时间: " + currentTime);
        }*/
    }

    /**
     * 外部调用的包接收事件处理方法
     */
    public void onPacketReceived(Packet<?> packet) {
        if (!shouldProcess()) return;

        long currentTime = System.currentTimeMillis();
        String packetType = packet.getClass().getSimpleName();

        // 查找对应的发送时间
        Long sentTime = sentPackets.remove(packetType);
        if (sentTime != null) {
            long roundTripTime = currentTime - sentTime;
            if (isValidResponseTime(roundTripTime)) {
                addSampleWithLock(movementResponseTimes, roundTripTime, MAX_SAMPLES);
                if (debugMode) {
          //          System.out.println("[TrafficPing] 包RTT: " + roundTripTime + "ms, 类型: " + packetType);
                }
            }
        }

        // 处理服务器返回的实体更新包
        if (packet instanceof S18PacketEntityTeleport ||
                (packet instanceof S14PacketEntity && isRelevantEntityPacket((S14PacketEntity) packet)) ||
                packet instanceof S19PacketEntityHeadLook) {
            handleEntityUpdatePacket(packet, currentTime);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !shouldProcess()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL) return;

        analyzeNetworkData(currentTime);
        cleanupOldPackets();
        lastAnalysisTime = currentTime;
    }

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        // 服务器连接时重置数据
       // if (debugMode) System.out.println("[TrafficPing] 连接到服务器，重置数据");
        reset();
    }

    @SubscribeEvent
    public void onServerDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
       // if (debugMode) System.out.println("[TrafficPing] 断开服务器连接，清理数据");
        reset();
    }

    private boolean shouldProcess() {
        return BetterPlayerHUD.config.useTrafficBasedPing;
    }

    private void analyzeNetworkData(long currentTime) {
        analyzePlayerMovement(currentTime);
        analyzeInteractions(currentTime);
        calculateComprehensivePing();
    }

    private void analyzePlayerMovement(long currentTime) {
        if (mc.thePlayer == null) return;

        double currentX = mc.thePlayer.posX;
        double currentY = mc.thePlayer.posY;
        double currentZ = mc.thePlayer.posZ;

        double distanceMoved = calculateDistance(currentX, currentY, currentZ);

        if (distanceMoved > MOVEMENT_THRESHOLD) {
            movementCounter++;

            if (movementCounter >= MOVEMENT_SAMPLE_INTERVAL && lastMovementPacketTime > 0) {
                long responseTime = currentTime - lastMovementPacketTime;
                if (isValidResponseTime(responseTime)) {
                    addSampleWithLock(movementResponseTimes, responseTime, MAX_SAMPLES);
                    if (debugMode) {
                        System.out.println("[TrafficPing] 移动分析响应时间: " + responseTime + "ms");
                    }
                }
                movementCounter = 0;
            }
        }

        updatePosition(currentX, currentY, currentZ);
    }

    private double calculateDistance(double x, double y, double z) {
        return Math.sqrt(
                Math.pow(x - lastPosX, 2) +
                        Math.pow(y - lastPosY, 2) +
                        Math.pow(z - lastPosZ, 2)
        );
    }

    private void updatePosition(double x, double y, double z) {
        lastPosX = x;
        lastPosY = y;
        lastPosZ = z;
    }

    private void analyzeInteractions(long currentTime) {
        if (mc.thePlayer == null) return;

        if (isInteracting()) {
            interactionCounter++;

            if (interactionCounter >= INTERACTION_SAMPLE_INTERVAL && lastInteractionTime > 0) {
                long responseTime = currentTime - lastInteractionTime;
                if (isValidResponseTime(responseTime)) {
                    addSampleWithLock(interactionResponseTimes, responseTime, 10);
              //      if (debugMode) {
                 //       System.out.println("[TrafficPing] 交互分析响应时间: " + responseTime + "ms");
             //       }
                }
                interactionCounter = 0;
            }
        }
    }

    private boolean isInteracting() {
        return mc.gameSettings.keyBindAttack.isPressed() ||
                mc.gameSettings.keyBindUseItem.isPressed() ||
                mc.gameSettings.keyBindSneak.isPressed() ||
                mc.gameSettings.keyBindJump.isPressed();
    }

    private boolean isValidResponseTime(long responseTime) {
        return responseTime > 0 && responseTime < 2000;
    }

    private void trackMovementPacket(Packet<?> packet, long sendTime) {
        int packetId = ++packetCounter;
        sentMovementPackets.put(packetId, sendTime);
        lastMovementPacketTime = sendTime;

     //   if (debugMode) {
      //      System.out.println("[TrafficPing] 跟踪移动包 ID: " + packetId + ", 时间: " + sendTime);
     //   }
    }

    private void handleEntityUpdatePacket(Packet<?> packet, long receiveTime) {
        Integer mostRecentPacketId = getMostRecentMovementPacketId();
        if (mostRecentPacketId != null) {
            Long sendTime = sentMovementPackets.get(mostRecentPacketId);
            if (sendTime != null) {
                long responseTime = receiveTime - sendTime;
                if (isValidResponseTime(responseTime)) {
                    addSampleWithLock(movementResponseTimes, responseTime, MAX_SAMPLES);
          //          if (debugMode) {
          //              System.out.println("[TrafficPing] 包跟踪响应时间: " + responseTime + "ms, 包ID: " + mostRecentPacketId);
           //         }
                }
                // 清理已处理的包
                sentMovementPackets.remove(mostRecentPacketId);
            }
        }
    }

    private boolean isRelevantEntityPacket(S14PacketEntity packet) {
        if (mc.thePlayer == null) return false;
        return packet.getEntity(mc.theWorld) == mc.thePlayer;
    }

    private Integer getMostRecentMovementPacketId() {
        Integer mostRecentId = null;
        long mostRecentTime = 0;

        for (ConcurrentHashMap.Entry<Integer, Long> entry : sentMovementPackets.entrySet()) {
            if (entry.getValue() > mostRecentTime) {
                mostRecentTime = entry.getValue();
                mostRecentId = entry.getKey();
            }
        }

        return mostRecentId;
    }

    private void addSampleWithLock(Deque<Long> deque, long sample, int maxSize) {
        lock.writeLock().lock();
        try {
            if (deque.size() >= maxSize) {
                deque.removeFirst();
            }
            deque.addLast(sample);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void calculateComprehensivePing() {
        long movementPing = calculateMedian(movementResponseTimes);
        long interactionPing = calculateMedian(interactionResponseTimes);

        long newPing = calculateWeightedAverage(movementPing, interactionPing);
        updateEstimatedPing(newPing);

    //    if (debugMode && getDataConfidence() > 0) {
   //         System.out.println("[TrafficPing] 综合Ping计算 - 移动: " + movementPing + "ms, 交互: " +
   //                 interactionPing + "ms, 最终: " + newPing + "ms, 可信度: " + getDataConfidence() + "%");
   //     }
    }

    private long calculateWeightedAverage(long movementPing, long interactionPing) {
        int weight = 0;
        long total = 0;

        if (movementPing > 0) {
            total += movementPing * 3;
            weight += 3;
        }

        if (interactionPing > 0) {
            total += interactionPing * 2;
            weight += 2;
        }

        return weight > 0 ? total / weight : getServerReportedPing();
    }

    private void updateEstimatedPing(long newPing) {
        long currentPing = estimatedPing.get();
        if (currentPing > 0) {
            // 指数平滑，减少抖动
            estimatedPing.set((newPing + currentPing * 3) / 4);
        } else {
            estimatedPing.set(newPing);
        }
    }

    private long calculateMedian(Deque<Long> deque) {
        lock.readLock().lock();
        try {
            if (deque.isEmpty()) return 0;

            long[] sorted = deque.stream().mapToLong(Long::longValue).sorted().toArray();
            int mid = sorted.length / 2;

            return sorted.length % 2 == 0 ? (sorted[mid - 1] + sorted[mid]) / 2 : sorted[mid];
        } finally {
            lock.readLock().unlock();
        }
    }

    private void cleanupOldPackets() {
        long currentTime = System.currentTimeMillis();
        int initialSize = sentPackets.size();

        // 清理旧的通用包跟踪
        sentPackets.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > PACKET_TIMEOUT
        );

        // 清理旧的移动包跟踪
        sentMovementPackets.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > PACKET_TIMEOUT
        );

   //     if (debugMode && initialSize != sentPackets.size()) {
   //         System.out.println("[TrafficPing] 清理了 " + (initialSize - sentPackets.size()) + " 个旧包");
  //      }
    }

    public long getTrafficBasedPing() {
        long ping = estimatedPing.get();
        int confidence = getDataConfidence();

     //   if (debugMode) {
      //      System.out.println("[TrafficPing] 获取Ping: " + ping + "ms, 可信度: " + confidence + "%");
      //  }

        return confidence < 30 ? getServerReportedPing() : Math.max(ping, 0);
    }

    public String getPingQualityIndicator() {
        long ping = getTrafficBasedPing();

        if (ping <= 0) return "未知";
        if (ping < 50) return "优秀";
        if (ping < 100) return "良好";
        if (ping < 200) return "一般";
        if (ping < 300) return "较差";
        return "极差";
    }

    public int getDataConfidence() {
        lock.readLock().lock();
        try {
            int movementConfidence = Math.min(movementResponseTimes.size() * 8, 40);
            int interactionConfidence = Math.min(interactionResponseTimes.size() * 12, 40);
            int packetTrackingConfidence = Math.min(sentPackets.size() * 5, 20);

            int totalConfidence = movementConfidence + interactionConfidence + packetTrackingConfidence;

       //     if (debugMode && totalConfidence > 0) {
       //         System.out.println("[TrafficPing] 可信度计算 - 移动: " + movementConfidence +
       //                 "%, 交互: " + interactionConfidence + "%, 包跟踪: " + packetTrackingConfidence + "%");
         //   }

            return Math.min(totalConfidence, 100);
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getEstimationMethod() {
        lock.readLock().lock();
        try {
            if (movementResponseTimes.size() >= 5) return "移动分析";
            if (interactionResponseTimes.size() >= 3) return "交互分析";
            if (!sentPackets.isEmpty()) return "包跟踪";
            return "服务器数据";
        } finally {
            lock.readLock().unlock();
        }
    }

    private long getServerReportedPing() {
      if (mc.getNetHandler() == null || mc.thePlayer == null) {
      //      if (debugMode) System.out.println("[TrafficPing] 无法获取服务器Ping");
            return 0;
        }

        try {
            net.minecraft.client.network.NetworkPlayerInfo playerInfo =
                    mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            long ping = playerInfo != null ? playerInfo.getResponseTime() : 0;
         //   if (debugMode) System.out.println("[TrafficPing] 服务器报告Ping: " + ping + "ms");
            return ping;
        } catch (Exception e) {
       //     if (debugMode) System.out.println("[TrafficPing] 获取服务器Ping异常: " + e.getMessage());
            return 0;
        }
    }

    public void reset() {
        lock.writeLock().lock();
        try {
            movementResponseTimes.clear();
            interactionResponseTimes.clear();
            sentPackets.clear();
            sentMovementPackets.clear();
            sentInteractionPackets.clear();
            estimatedPing.set(0);
            lastMovementPacketTime = 0;
            lastInteractionTime = 0;
            movementCounter = 0;
            interactionCounter = 0;
            packetCounter = 0;

         //   if (debugMode) System.out.println("[TrafficPing] 数据已重置");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getDebugInfo() {
        return String.format("方法: %s | 可信度: %d%% | 移动样本: %d | 交互样本: %d | 跟踪包数: %d",
                getEstimationMethod(), getDataConfidence(),
                movementResponseTimes.size(), interactionResponseTimes.size(),
                sentPackets.size());
    }

    // 添加手动触发交互的方法（用于测试）
    public void recordInteraction() {
        if (shouldProcess()) {
            lastInteractionTime = System.currentTimeMillis();
       //     if (debugMode) System.out.println("[TrafficPing] 记录交互时间");
        }
    }

    // 设置调试模式
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
}
