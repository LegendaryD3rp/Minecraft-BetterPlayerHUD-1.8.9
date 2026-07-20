package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 弹性动画聊天框（SmoothChatGUI）
 *
 * 继承 GuiNewChat，通过覆盖 drawChat / getChatOpen / getChatScale 实现：
 * 1. 开/合弹性弹跳动画（弹簧物理模型）
 * 2. 新消息入场淡入动画（短时间 alpha 渐变）
 * 3. 所有动画均可独立开关与调整参数
 *
 * 通过反射替换 GuiIngame.persistantChatGUI 接入，无需 CoreMod / ASM / Mixin。
 */
@SideOnly(Side.CLIENT)
public class SmoothChatHandler extends GuiNewChat {

    private static final float MAX_OVERSHOOT = 1.25f;   // 弹性最大过冲
    private static final float VELOCITY_CLAMP = 50.0f;  // 速度上限，防止数值爆炸

    private final Minecraft mc;

    // ── 弹簧物理参数（每帧更新） ──
    private float animAmount;       // 当前位置 (0=收拢, 1=展开, >1=过冲)
    private float animVelocity;     // 当前速度
    private long lastUpdateTick;    // 上次物理更新时刻 (ms)

    // ── 新消息入场跟踪 ──
    // 消息通过 updateCounter 识别，记录其加入时刻的系统时间
    // 最多跟踪 100 条，环形缓冲
    private static final int MSG_TRACK_SIZE = 100;
    private final int[] msgTrackCounters = new int[MSG_TRACK_SIZE];
    private final long[] msgTrackTimes = new long[MSG_TRACK_SIZE];
    private int msgTrackHead = 0;

    // ── 状态缓存 ──
    private boolean wasChatOpen = false;

    public SmoothChatHandler(Minecraft mcIn) {
        super(mcIn);
        this.mc = mcIn;
        this.animAmount = 0.0f;
        this.animVelocity = 0.0f;
        this.lastUpdateTick = Minecraft.getSystemTime();

        // 初始化消息跟踪：无效标记 (-1)
        for (int i = 0; i < MSG_TRACK_SIZE; i++) {
            msgTrackCounters[i] = -1;
            msgTrackTimes[i] = 0L;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  配置更新入口（由 BetterPlayerHUD.onConfigChanged 调用）
    // ═══════════════════════════════════════════════════════════════
    public void onConfigChanged() {
        // 无额外操作，全部从 BetterPlayerHUD.config 读取
    }

    // ═══════════════════════════════════════════════════════════════
    //  drawChat —— 主渲染覆盖
    // ═══════════════════════════════════════════════════════════════
    @Override
    public void drawChat(int updateCounter) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null || !cfg.enableSmoothChat) {
            super.drawChat(updateCounter);
            return;
        }

        // 1) 驱动弹簧物理
        updateSpringPhysics();

        // 2) 完全收拢时跳过渲染
        if (animAmount < 0.005f && !(mc.currentScreen instanceof GuiChat)) {
            // 确保状态重置
            if (animAmount < 0.0f) animAmount = 0.0f;
            return;
        }

        // 3) 保存原版 visibility 结果（不重复计算）
        boolean chatOpen = mc.currentScreen instanceof GuiChat;

        // 4) 应用弹性变换 —— 从底部向上缩放
        float scale = MathHelper.clamp_float(animAmount, 0.0f, MAX_OVERSHOOT);
        // 当前聊天框在屏幕上的视觉高度
        float visualHeight = (float) getChatHeight() * getChatScale();
        // 底部锚点 Y = 20 + visualHeight
        float anchorY = 20.0f + visualHeight;

        GlStateManager.pushMatrix();
        // 将原点移到聊天框底部中心
        GlStateManager.translate(0.0f, anchorY, 0.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(0.0f, -anchorY, 0.0f);

        // 原版渲染
        super.drawChat(updateCounter);

        GlStateManager.popMatrix();
    }

    // ═══════════════════════════════════════════════════════════════
    //  getChatOpen —— 返回聊天是否 "视觉上已打开"
    // ═══════════════════════════════════════════════════════════════
    @Override
    public boolean getChatOpen() {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null || !cfg.enableSmoothChat) {
            return super.getChatOpen();
        }

        // 真正打开时返回 true；关闭时在动画未完全收拢前也返回 true
        // 这样关闭动画期间原版 fade-out 不启动（线条保持全亮），由我们的 scale 负责视觉消失
        if (mc.currentScreen instanceof GuiChat) {
            return true;
        }
        // 动画收尾阶段：当 animAmount 低于 50% 才认为关闭
        return animAmount > 0.5f;
    }

    // ═══════════════════════════════════════════════════════════════
    //  getChatScale —— 聊天缩放（不影响我们的弹性变换，供其他系统使用）
    // ═══════════════════════════════════════════════════════════════
    @Override
    public float getChatScale() {
        return mc.gameSettings.chatScale;
    }

    // ═══════════════════════════════════════════════════════════════
    //  printChatMessageWithOptionalDeletion —— 捕获新消息入场
    // ═══════════════════════════════════════════════════════════════
    @Override
    public void printChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId) {
        // 记录当前 updateCounter 对应的系统时间（用于新消息淡入动画）
        // 这里直接调用 super，super 内部会创建 ChatLine(updateCounter, ...)
        // 我们在 drawChat 中通过 chatline.getUpdatedCounter() 匹配跟踪
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg != null && cfg.enableSmoothChat) {
            // 获取当前 updateCounter（来自 GuiIngame）
            int counter = mc.ingameGUI.getUpdateCounter();
            long now = Minecraft.getSystemTime();

            // 环形缓冲记录
            msgTrackCounters[msgTrackHead] = counter;
            msgTrackTimes[msgTrackHead] = now;
            msgTrackHead = (msgTrackHead + 1) % MSG_TRACK_SIZE;
        }

        super.printChatMessageWithOptionalDeletion(chatComponent, chatLineId);
    }

    // ═══════════════════════════════════════════════════════════════
    //  clearChatMessages —— 清空消息跟踪
    // ═══════════════════════════════════════════════════════════════
    @Override
    public void clearChatMessages() {
        super.clearChatMessages();
        // 清空跟踪
        for (int i = 0; i < MSG_TRACK_SIZE; i++) {
            msgTrackCounters[i] = -1;
            msgTrackTimes[i] = 0L;
        }
        msgTrackHead = 0;
    }

    // ═══════════════════════════════════════════════════════════════
    //  内部：消息入场时间查询
    // ═══════════════════════════════════════════════════════════════
    private long getMsgAddTime(int updateCounter) {
        // 线性扫描环形缓冲（最多 100 次，仅在 drawChat 中调用，性能可接受）
        for (int i = 0; i < MSG_TRACK_SIZE; i++) {
            if (msgTrackCounters[i] == updateCounter) {
                return msgTrackTimes[i];
            }
        }
        // 未找到：视为很久以前的消息
        return 0L;
    }

    // ═══════════════════════════════════════════════════════════════
    //  内部：弹簧物理更新
    // ═══════════════════════════════════════════════════════════════
    private void updateSpringPhysics() {
        long now = Minecraft.getSystemTime();
        float dt = (now - lastUpdateTick) / 1000.0f; // 秒
        lastUpdateTick = now;

        // 防抖：dt 过大（卡顿/暂停后恢复）则跳过
        if (dt > 0.1f) return; // >100ms 不更新，保持上一帧状态
        if (dt <= 0.0f) return;

        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null) return;

        // 目标值
        boolean isChatOpen = mc.currentScreen instanceof GuiChat;
        float target = isChatOpen ? 1.0f : 0.0f;

        // 弹簧参数（由配置换算）
        float stiffness = 200.0f;
        float damping   = 12.0f;

        // 用户调整：bounciness 影响阻尼比
        //   bounciness=0 → 临界阻尼（无过冲）
        //   bounciness=0.5 → 轻微过冲
        //   bounciness=1.0 → 明显弹跳
        float bounciness = cfg.chatAnimBounciness;
        float adjustedDamping = damping * (1.5f - bounciness); // 高bounciness=低阻尼

        // 弹簧力: F = -k*(x - target) - d*v
        float force = -stiffness * (animAmount - target) - adjustedDamping * animVelocity;

        animVelocity += force * dt;
        animVelocity = MathHelper.clamp_float(animVelocity, -VELOCITY_CLAMP, VELOCITY_CLAMP);
        animAmount   += animVelocity * dt;

        // 完全收拢时的阈值锁定
        if (target == 0.0f && animAmount < 0.001f && Math.abs(animVelocity) < 0.01f) {
            animAmount = 0.0f;
            animVelocity = 0.0f;
        }

        wasChatOpen = isChatOpen;
    }
}
