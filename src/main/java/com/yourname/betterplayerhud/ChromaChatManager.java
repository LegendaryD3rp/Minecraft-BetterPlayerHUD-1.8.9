package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "蜃楼" ChromaChat — 现代聊天框
 * <p>
 * Phase 1: 骨架
 * Phase 2: 悬停高亮 + 鼠标交互
 * Phase 3: 消息分组折叠
 * Phase 4: 配置 GUI 集成
 * <p>
 * 数据通路: ClientChatReceivedEvent + 自管列表 (不依赖 GuiNewChat/任何覆盖模组)
 */
@SideOnly(Side.CLIENT)
public class ChromaChatManager {

    private static final long CLICK_THRESHOLD_MS = 200L;
    private static final Minecraft mc = Minecraft.getMinecraft();

    /** 单例引用，供 ChromaChatBridge 调用 */
    static ChromaChatManager INSTANCE = null;

    // =================================================================
    //  Own message list (完全独立于 BetterChat / 原版 GuiNewChat)
    // =================================================================
    private static final int MAX_LINES = 100;
    final List<MyChatLine> myChatLines = new ArrayList<MyChatLine>();
    private int myScrollPos = 0;
    private boolean myIsScrolled = false;
    private boolean scrollBtnDown = false;
    private boolean scrollDragging = false;
    private int nextLineId = 1;
    // 滚轮预读（在 tick 阶段抢在 GuiChat.handleMouseInput 读取/消耗事件后，依然可读）
    private int pendingScroll = 0;
    // ── 发送者名称提取正则 ──
    private static final Pattern SENDER_PATTERN = Pattern.compile("^<(.+?)>");

    // ── 头像缓存 ──
    private final HashMap<String, ResourceLocation> avatarCache = new HashMap<String, ResourceLocation>();

    private static class MyChatLine {
        final IChatComponent message;
        final int updateCounter;
        final int chatLineID;
        final long receivedTimeMs;
        final String formattedTime;      // 缓存的 "[HH:MM] " 时间戳
        final String senderName;         // 发送者名（系统消息为 null）
        int groupCount;                  // 物理去重计数（>1 = 被折叠的消息）
        int lastDedupTick;               // 最近一次去重时的 updateCounter（防淡出）
        // 换行缓存（lazy）
        private String[] cachedLines = null;
        private int lastWrapWidth = -1;

        MyChatLine(IChatComponent msg, int ctr, int id, long timeMs) {
            this(msg, ctr, id, timeMs, extractSenderName(msg));
        }

        MyChatLine(IChatComponent msg, int ctr, int id, long timeMs, String sender) {
            this.message = msg;
            this.updateCounter = ctr;
            this.chatLineID = id;
            this.receivedTimeMs = timeMs;
            this.formattedTime = formatChatTimestamp(timeMs);
            this.senderName = sender;
            this.groupCount = 1;
            this.lastDedupTick = 0;
        }

        /** 增加去重计数（物理折叠时调用） */
        void incrementGroup() {
            groupCount++;
        }

        /** 获取换行后的文本行（缓存） */
        String[] getWrappedLines(int wrapWidth) {
            if (wrapWidth <= 0) {
                return new String[]{message.getFormattedText()};
            }
            if (wrapWidth != lastWrapWidth || cachedLines == null) {
                java.util.List<String> list = mc.fontRendererObj.listFormattedStringToWidth(
                    message.getFormattedText(), wrapWidth);
                cachedLines = list.toArray(new String[list.size()]);
                lastWrapWidth = wrapWidth;
            }
            return cachedLines;
        }

        /** 本消息占用的文本行数 */
        int getLineCount(int wrapWidth) {
            return getWrappedLines(wrapWidth).length;
        }

        /** 本消息占用的像素高度 */
        int getHeight(int wrapWidth, int lineH) {
            return getLineCount(wrapWidth) * (lineH > 0 ? lineH : 1);
        }
    }

    /** 把毫秒时间格式化为 "[HH:MM] " */
    private static String formatChatTimestamp(long ms) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("[HH:mm] ");
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        return sdf.format(new java.util.Date(ms));
    }

    // =================================================================
    //  发送者名称提取
    // =================================================================
    private static String extractSenderName(IChatComponent msg) {
        if (msg == null) return null;
        for (IChatComponent sibling : msg.getSiblings()) {
            if (sibling.getChatStyle() != null && sibling.getChatStyle().getChatClickEvent() != null) {
                ClickEvent.Action action = sibling.getChatStyle().getChatClickEvent().getAction();
                if (action == ClickEvent.Action.SUGGEST_COMMAND) {
                    String text = sibling.getUnformattedText();
                    if (text != null && !text.isEmpty()) return text;
                }
            }
        }
        String text = msg.getUnformattedText();
        if (text != null) {
            Matcher m = SENDER_PATTERN.matcher(text);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    // =================================================================
    //  玩家头像获取
    //  直接使用 AbstractClientPlayer.getLocationSkin(name) 生成标准皮肤路径。
    //  TextureManager 在 bindTexture 时自动异步下载，下载完成前显示默认皮肤。
    // =================================================================
    private ResourceLocation getAvatar(String name) {
        if (name == null) return null;
        ResourceLocation cached = avatarCache.get(name);
        if (cached != null) return cached;
        ResourceLocation loc = net.minecraft.client.entity.AbstractClientPlayer.getLocationSkin(name);
        if (avatarCache.size() >= 50) avatarCache.clear();
        avatarCache.put(name, loc);
        return loc;
    }

    /** 绘制纹理矩形（归一化 UV 坐标，同 PlayerHUDHandler.renderPlayerHead） */
    private static void drawTextureRect(int x, int y, int w, int h,
                                        float uMin, float vMin, float uMax, float vMax) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x,       y + h, 0.0D).tex(uMin, vMax).endVertex();
        wr.pos(x + w,   y + h, 0.0D).tex(uMax, vMax).endVertex();
        wr.pos(x + w,   y,     0.0D).tex(uMax, vMin).endVertex();
        wr.pos(x,       y,     0.0D).tex(uMin, vMin).endVertex();
        tess.draw();
    }

    // === Spring Animation (P1) ===
    private float animAmount = 1.0f;
    private float animVelocity = 0.0f;
    private long lastAnimTime = 0L;

    // === Message Animation Tracking (P1) — HashMap O(1) 替代 O(100) 数组遍历 ===
    private int prevLineCount = 0;
    private final java.util.HashMap<Integer, Long> msgAnimMap = new java.util.HashMap<Integer, Long>();
    // === 去重脉冲动画 ===
    private final java.util.HashMap<Integer, Long> dedupPulseMap = new java.util.HashMap<Integer, Long>();

    // === Hover Interaction (P2) ===
    private int hoveredLineIdx = -1;
    private int hoveredLineAbsIdx = -1;
    private long hoverUpdateTime = 0L;
    private boolean mouseBtnDown = false;
    private long mousePressTime = 0L;

    public ChromaChatManager() {
        INSTANCE = this;
    }

    public void onConfigChanged() {}

    // =================================================================
    //  ClientChatReceivedEvent — 消息源头拦截
    // =================================================================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatReceived(ClientChatReceivedEvent event) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null || !cfg.enableChromaChat) return;

        event.setCanceled(true);

        int ctr = mc.ingameGUI.getUpdateCounter();
        long nowMs = System.currentTimeMillis();  // 真实世界时间（用于时间戳）
        long nowSysMs = Minecraft.getSystemTime(); // 游戏时间（用于动画脉冲）

        // 物理去重：与最新消息（index 0）比对发送者 + 文本
        if (cfg.chromaChatDedup && !myChatLines.isEmpty()) {
            MyChatLine latest = myChatLines.get(0);
            if (latest != null) {
                String newSender = extractSenderName(event.message);
                String newText = event.message.getUnformattedText();
                String oldSender = latest.senderName;
                String oldText = latest.message.getUnformattedText();
                if (newText.equals(oldText) &&
                    (newSender == null ? oldSender == null : newSender.equals(oldSender))) {
                    latest.incrementGroup();
                    latest.lastDedupTick = ctr;
                    if (cfg.chromaChatDedupAnim) {
                        dedupPulseMap.put(latest.chatLineID, Minecraft.getSystemTime());
                    }
                    return;
                }
            }
        }

        myChatLines.add(0, new MyChatLine(event.message, ctr, nextLineId++, nowMs));

        while (myChatLines.size() > MAX_LINES) {
            myChatLines.remove(myChatLines.size() - 1);
        }

        // 新消息 → 回滚到最新
        myScrollPos = 0;
        myIsScrolled = false;
    }

    // =================================================================
    //  onLocalMessage — 供 ChromaChatBridge 调用（本地指令消息入口）
    // =================================================================
    static void onLocalMessage(IChatComponent component) {
        ChromaChatManager cc = INSTANCE;
        if (cc == null) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null || !cfg.enableChromaChat) return;

        int ctr = mc.ingameGUI.getUpdateCounter();
        long nowMs = System.currentTimeMillis();  // 真实世界时间（用于时间戳）
        long nowSysMs = Minecraft.getSystemTime(); // 游戏时间（用于动画脉冲）

        // 物理去重（与最新消息比对）
        if (cfg.chromaChatDedup && !cc.myChatLines.isEmpty()) {
            MyChatLine latest = cc.myChatLines.get(0);
            if (latest != null) {
                String newSender = extractSenderName(component);
                String newText = component.getUnformattedText();
                String oldSender = latest.senderName;
                String oldText = latest.message.getUnformattedText();
                if (newText.equals(oldText) &&
                    (newSender == null ? oldSender == null : newSender.equals(oldSender))) {
                    latest.incrementGroup();
                    latest.lastDedupTick = ctr;
                    if (cfg.chromaChatDedupAnim) {
                        cc.dedupPulseMap.put(latest.chatLineID, nowSysMs);
                    }
                    return;
                }
            }
        }

        cc.myChatLines.add(0, new MyChatLine(component, ctr, cc.nextLineId++, nowMs));

        while (cc.myChatLines.size() > MAX_LINES) {
            cc.myChatLines.remove(cc.myChatLines.size() - 1);
        }

        cc.myScrollPos = 0;
        cc.myIsScrolled = false;
    }

    // =================================================================
    //  Main Render Event
    // =================================================================
    @SubscribeEvent
    public void onChatRender(RenderGameOverlayEvent.Chat event) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null || !cfg.enableChromaChat) return;

        event.setCanceled(true);

        // ── 早返：没消息 或 关闭且全过期 ──
        if (myChatLines.isEmpty()) return;
        if (!(mc.currentScreen instanceof GuiChat)) {
            MyChatLine newest = myChatLines.get(0);
            if (newest != null && (mc.ingameGUI.getUpdateCounter() - newest.updateCounter) >= 200) return;
        }

        ScaledResolution res = event.resolution;
        int baseX = cfg.chromaChatXOffset;
        int baseY = cfg.chromaChatYOffset;
        int chatWidth = cfg.chromaChatWidth;

        // ── 布局参数（含自动换行） ──
        int totalLines = myChatLines.size();
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        long now = Minecraft.getSystemTime();
        int lineH = 9;  // FONT_HEIGHT + 2 约等于 9

        // 文本宽度 = 聊天框宽度 - 左右边距
        int textWidth = cfg.chromaChatWidth - 2;
        boolean showTime = cfg.chromaChatShowTimestamps;
        int timeWidth = showTime ? mc.fontRendererObj.getStringWidth("[00:00] ") : 0;
        int avatarOffset = cfg.chromaChatAvatar ? cfg.chromaChatAvatarSize + 2 : 0;
        int msgTextWidth = textWidth - timeWidth - avatarOffset;  // 除去时间戳和头像后的纯文本宽度

        // 从 scrollPos 开始，累计换行行数直到填满 chromaChatLineCount
        int visibleCount = 0;       // 可见消息数
        int visibleTextLines = 0;   // 可见文本行数（含换行）
        int contentH = 0;           // 实际内容像素高度
        {
            int accumLines = 0;
            int maxVis = cfg.chromaChatLineCount;
            for (int i = myScrollPos; i < totalLines && accumLines < maxVis; i++) {
                MyChatLine ml = myChatLines.get(i);
                int n = (ml != null) ? ml.getLineCount(msgTextWidth) : 1;
                accumLines += n;
                visibleCount++;
            }
            visibleTextLines = Math.min(accumLines, maxVis);
            int minH = chatOpen ? 20 : 0;
            contentH = Math.max(minH, visibleTextLines * lineH);
        }
        int bgH = contentH + 4;

        // ── 弹性动画（类 ComboHandler：每帧更新 animAmount） ──
        updateSpring(chatOpen, now, cfg);

        // ── 滚动检测 ──
        // Mouse.getDWheel() 返回自上次 poll() 以来的累积滚轮增量。
        // 不管 GuiChat 是否消耗了事件队列，getDWheel() 的值都会保留到下一轮 poll()。
        // 所以直接在渲染时读取即可，不需要 TickEvent 预读。
        int wheel = 0;
        if (pendingScroll != 0) {
            wheel = pendingScroll;
            pendingScroll = 0;
        } else {
            // Fallback: 在非 GuiChat 模式下直接从 getDWheel 读
            wheel = Mouse.getDWheel();
        }
        if (wheel != 0 && (mc.currentScreen instanceof GuiChat || myScrollPos > 0)) {
            // 方向约定：wheel > 0 = 物理向上（滚轮远离用户=向上推）
            //   Natural scrolling 反转该符号，此处硬编码不做适配
            int dir = (wheel > 0) ? -1 : 1;  // 向上滚=看更旧消息=scrollPos↑
            int maxScroll = Math.max(0, totalLines - Math.min(8, totalLines));
            int prev = myScrollPos;
            myScrollPos = MathHelper.clamp_int(prev + dir * 3, 0, maxScroll);
            myIsScrolled = myScrollPos > 0;
        }

        // ── 鼠标状态（多行消息的Y命中检测） ──
        int mouseSx = Mouse.getX() * res.getScaledWidth() / Math.max(mc.displayWidth, 1);
        int mouseSy = res.getScaledHeight()
                - Mouse.getY() * res.getScaledHeight() / Math.max(mc.displayHeight, 1) - 1;

        boolean inChat = mouseSx >= baseX && mouseSx <= baseX + chatWidth
                      && mouseSy >= baseY && mouseSy <= baseY + bgH;

        int newHoveredIdx = -1, newHoveredAbs = -1;
        if (inChat && chatOpen) {
            // Y 计算必须与 renderNormal 完全一致：
            //   从 bg 底部向上排（最新在最下）
            int yCursor = baseY + bgH - 2;
            for (int i = myScrollPos; i < myScrollPos + visibleCount; i++) {
                if (i >= totalLines) break;
                MyChatLine ml = myChatLines.get(i);
                int n = (ml != null) ? ml.getLineCount(msgTextWidth) : 1;
                int entryH = n * lineH;
                int entryTop = yCursor - entryH;
                if (mouseSy >= entryTop && mouseSy < entryTop + entryH) {
                    newHoveredIdx = i - myScrollPos;
                    newHoveredAbs = i;
                    break;
                }
                yCursor = entryTop;
            }
        }

        // 消抖
        long hdt = now - hoverUpdateTime;
        if (newHoveredAbs != hoveredLineAbsIdx) {
            if (hdt > 80L) {
                hoveredLineIdx = newHoveredIdx;
                hoveredLineAbsIdx = newHoveredAbs;
                hoverUpdateTime = now;
            }
        } else {
            hoverUpdateTime = now;
        }

        // 点击检测
        boolean mouseClicked = false;
        if (Mouse.isButtonDown(0)) {
            if (!mouseBtnDown) { mouseBtnDown = true; mousePressTime = now; }
        } else {
            if (mouseBtnDown && (now - mousePressTime) < CLICK_THRESHOLD_MS) {
                if (hoveredLineAbsIdx >= 0 && inChat && chatOpen) mouseClicked = true;
            }
            mouseBtnDown = false;
        }

        // ── Ctrl+C 复制选中消息 ──
        boolean ctrlDown = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL)
                        || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL);
        if (ctrlDown && org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_C)) {
            if (chatOpen && hoveredLineAbsIdx >= 0 && hoveredLineAbsIdx < myChatLines.size()) {
                MyChatLine ml = myChatLines.get(hoveredLineAbsIdx);
                if (ml != null) {
                    String txt = ml.message.getUnformattedText();
                    if (txt != null && !txt.isEmpty()) {
                        java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(txt);
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                    }
                }
            }
        }

        // ── 消息动画跟踪 ──
        trackNewMessages(myChatLines, cfg, now);

        // ═══════════════ Render ═══════════════
        GlStateManager.pushMatrix();

        // -- 背景（animAmount 控制淡入淡出） --
        float bgAlpha = 0.15f + 0.85f * animAmount; // 关闭时保留 15% 底色
        int bgColor = fadeAlpha(cfg.chromaChatBackgroundColor, bgAlpha);
        int borderColor = fadeAlpha(cfg.chromaChatBorderColor, bgAlpha);
        drawRoundedRect(baseX, baseY, baseX + chatWidth, baseY + bgH,
                cfg.chromaChatBorderRadius, bgColor, borderColor);

        // -- 消息 --
        if (visibleCount > 0) {
            float opacity = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
            int updateCounter = mc.ingameGUI.getUpdateCounter();

            renderNormal(cfg, myChatLines, myScrollPos, visibleCount, baseX, chatWidth, lineH, baseY, bgH,
                    updateCounter, chatOpen, opacity, now, mouseClicked,
                    showTime, msgTextWidth, timeWidth);
        }

        // -- 滚动指示器（有更多消息时显示） --
        if (totalLines > visibleCount && visibleCount > 0 && chatOpen) {
            int sw = 6; // 滑块宽度（可点中）
            int ix = baseX + chatWidth - sw - 2;
            int totalScrollable = totalLines - Math.min(8, totalLines);
            float ratio = totalScrollable > 0 ? (float) myScrollPos / totalScrollable : 0f;
            // 稳定轨道高度：固定 120px，不超过 bgH
            int stableTrackH = Math.min(120, bgH - 8);
            if (stableTrackH > 0) {
                int ih = Math.min(16, stableTrackH / 3);
                int trackY = baseY + (bgH - stableTrackH) / 2;
                int iy = trackY + (int)((stableTrackH - ih) * ratio);
                iy = MathHelper.clamp_int(iy, trackY, trackY + stableTrackH - ih);
                // 轨道背景
                Gui.drawRect(ix, trackY, ix + sw, trackY + stableTrackH, 0x33FFFFFF | (0x44 << 24));
                // 滑块
                Gui.drawRect(ix, iy, ix + sw, iy + ih, 0xAAFFFFFF | (0x88 << 24));
                // 滚动条交互：拖拽 + 点击跳转
                if (Mouse.isButtonDown(0)) {
                    boolean onTrack = mouseSx >= ix && mouseSx <= ix + sw
                                   && mouseSy >= trackY && mouseSy <= trackY + stableTrackH;
                    if (!scrollBtnDown) {
                        scrollBtnDown = true;
                        if (onTrack) {
                            scrollDragging = true;
                        }
                    }
                    if (scrollDragging) {
                        // 拖拽中：跟随鼠标 Y（不管鼠标是否还在轨道范围内）
                        int clickRatio = (int)((float)(mouseSy - trackY - ih / 2) / stableTrackH * totalScrollable);
                        myScrollPos = MathHelper.clamp_int(clickRatio, 0, totalScrollable);
                        myIsScrolled = myScrollPos > 0;
                    }
                } else {
                    scrollBtnDown = false;
                    scrollDragging = false;
                }
            }
        }

        // ── 悬停事件提示（ChatHoverEvent） ──
        if (chatOpen && hoveredLineAbsIdx >= 0 && hoveredLineAbsIdx < myChatLines.size()) {
            MyChatLine ml = myChatLines.get(hoveredLineAbsIdx);
            if (ml != null && ml.message.getChatStyle() != null) {
                net.minecraft.event.HoverEvent he = ml.message.getChatStyle().getChatHoverEvent();
                if (he != null) {
                    net.minecraft.util.IChatComponent val = he.getValue();
                    if (val != null) {
                        String tip = val.getFormattedText();
                        if (tip != null && !tip.isEmpty()) {
                            String[] tlines = tip.split("\n");
                            net.minecraft.client.gui.FontRenderer fr = mc.fontRendererObj;
                            int tw = 0;
                            for (String s : tlines) tw = Math.max(tw, fr.getStringWidth(s));
                            int tx = mouseSx + 12;
                            int ty = mouseSy - tlines.length * 10 - 8;
                            if (tx + tw > res.getScaledWidth()) tx = res.getScaledWidth() - tw - 4;
                            if (ty < 0) ty = 0;
                            int ph = tlines.length * 10 + 4;
                            Gui.drawRect(tx - 2, ty - 2, tx + tw + 2, ty + ph + 2, 0xC0000000);
                            Gui.drawRect(tx - 3, ty - 3, tx - 2, ty + ph + 3, 0x505000FF);
                            Gui.drawRect(tx + tw + 2, ty - 3, tx + tw + 3, ty + ph + 3, 0x505000FF);
                            Gui.drawRect(tx - 3, ty - 3, tx + tw + 3, ty - 2, 0x505000FF);
                            Gui.drawRect(tx - 3, ty + ph + 2, tx + tw + 3, ty + ph + 3, 0x505000FF);
                            for (int li = 0; li < tlines.length; li++) {
                                fr.drawString(tlines[li], tx, ty + li * 10, 0xFFFFFFFF);
                            }
                        }
                    }
                }
            }
        }

        GlStateManager.popMatrix();

        // ── 上报位置给 F7 编辑模式 ──
        HUDEditManager.report("蜃楼聊天框", baseX, baseY, chatWidth, bgH);
    }

    // =================================================================
    //  Render: Normal mode（含头像 + 去重徽标）
    // =================================================================
    private void renderNormal(BetterPlayerHUDConfig cfg, List<MyChatLine> lines, int scrollPos,
                              int vis, int baseX, int chatWidth, int lineH, int baseY, int bgH,
                              int updateCounter, boolean chatOpen, float opacity, long now,
                              boolean mouseClicked, boolean showTime, int textWidth, int timeWidth) {
        boolean showAvatar = cfg.chromaChatAvatar;
        int avatarSize = showAvatar ? cfg.chromaChatAvatarSize : 0;
        int avatarOffset = showAvatar ? avatarSize + 2 : 0;

        int drawEnd = Math.min(scrollPos + vis, lines.size());
        // 从背景框底部开始，向上排布（最新在最下）
        int y = baseY + bgH - 2;
        for (int i = scrollPos; i < drawEnd; i++) {
            MyChatLine ml = lines.get(i);
            if (ml == null) continue;

            int age = updateCounter - Math.max(ml.updateCounter, ml.lastDedupTick);
            int alpha = calcAlpha(age, chatOpen, opacity);
            if (alpha <= 3) continue;

            y += getMsgOffset(ml.updateCounter, cfg);

            String[] wl = ml.getWrappedLines(textWidth);
            int entryH = wl.length * lineH;
            int entryTop = y - entryH;           // 本消息块顶部Y
            int entryBottom = entryTop + entryH; // 底部Y（= 调整前的y）

            // 悬停 + 点击
            if (i == hoveredLineAbsIdx) {
                drawHover(baseX + 1, entryTop, baseX + chatWidth - 1, entryBottom, cfg);
                if (mouseClicked) {
                    forwardClick(ml.message);
                }
            }

            // 逐行绘制（从上往下）
            // 文本统一起始位置：头像 + 时间戳之后，确保多行消息左对齐
            int textTx = baseX + 2;
            if (showAvatar && ml.senderName != null) textTx += avatarOffset;
            if (showTime) textTx += timeWidth;

            for (int j = 0; j < wl.length; j++) {
                int lineY = entryTop + j * lineH;

                // ── 头像（仅第一行，同护甲条头像渲染：归一化 UV 固定采 8×8 面部区域） ──
                if (showAvatar && j == 0 && ml.senderName != null) {
                    ResourceLocation skin = getAvatar(ml.senderName);
                    if (skin != null) {
                        mc.getTextureManager().bindTexture(skin);
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                        // 面部（归一化 UV，固定取 8/64~16/64，不受 avatarSize 影响）
                        drawTextureRect(baseX + 2, lineY, avatarSize, avatarSize,
                            8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F);
                        // 帽子 overlay（归一化 UV：u=40/64~48/64, v=8/64~16/64）
                        drawTextureRect(baseX + 2, lineY, avatarSize, avatarSize,
                            40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F);
                        GlStateManager.disableBlend();
                    }
                }

                if (showTime && j == 0) {
                    mc.fontRendererObj.drawString(ml.formattedTime, baseX + 2 + (showAvatar && ml.senderName != null ? avatarOffset : 0), lineY, (alpha << 24) | 0x888888);
                }

                // ── 消息文字 ──
                mc.fontRendererObj.drawString(wl[j], textTx, lineY, 0xFFFFFF | (alpha << 24));

                // ── 去重徽标 [Nx]（仅第一行） ──
                if (j == 0 && ml.groupCount > 1) {
                    String badge = "[" + ml.groupCount + "x]";
                    int bw = mc.fontRendererObj.getStringWidth(badge);
                    int bc = cfg.chromaChatDedupBadgeColor | (Math.min(alpha + 40, 255) << 24);
                    // 去重脉冲：短暂高亮背景
                    Long pulseStart = dedupPulseMap.get(ml.chatLineID);
                    if (pulseStart != null) {
                        long pulseElapsed = Minecraft.getSystemTime() - pulseStart;
                        if (pulseElapsed < 400L) {
                            float pulse = 1.0f - (float)pulseElapsed / 400.0f;
                            int pulseAlpha = (int)(40 * pulse);
                            if (pulseAlpha > 3) {
                                Gui.drawRect(baseX + 1, entryTop, baseX + chatWidth - 1, entryBottom, pulseAlpha << 24);
                            }
                        } else {
                            dedupPulseMap.remove(ml.chatLineID);
                        }
                    }
                    mc.fontRendererObj.drawString(badge, baseX + chatWidth - bw - 2, lineY, bc);
                }
            }

            y = entryTop; // 上移，给下一条消息（更旧的）腾位置
        }
    }

    // =================================================================
    //  Click forward (P2) — 递归查找子组件的 ClickEvent
    // =================================================================
    private void forwardClick(IChatComponent comp) {
        if (comp == null) return;

        // 先查当前节点
        if (comp.getChatStyle() != null && comp.getChatStyle().getChatClickEvent() != null) {
            String val = comp.getChatStyle().getChatClickEvent().getValue();
            if (val != null && !val.isEmpty()) {
                mc.thePlayer.sendChatMessage(val.startsWith("/") ? val : "/" + val);
                return;
            }
        }

        // 递归查子节点（大部分 clickable 在 sibling 上）
        for (IChatComponent child : comp.getSiblings()) {
            forwardClick(child);
        }
    }

    // =================================================================
    //  Client tick — 提前抢读滚轮值
    //  GuiChat.handleMouseInput() 会调用 Mouse.next() 遍历事件队列，但
    //  Mouse.getDWheel()（累积值）不受 next() 影响，此处读取同样可靠。
    //  TickEvent 时机早于渲染，确保滚轮值在 render 时已被捕获。
    // =================================================================
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && mc.currentScreen instanceof GuiChat) {
            int w = Mouse.getDWheel();
            if (w != 0) pendingScroll = w;
        }
    }

    // =================================================================
    //  Spring physics (P1)
    // =================================================================
    private void updateSpring(boolean chatOpen, long now, BetterPlayerHUDConfig cfg) {
        if (lastAnimTime == 0L) { lastAnimTime = now; return; }
        float dt = (now - lastAnimTime) / 1000.0f;
        lastAnimTime = now;
        if (dt > 0.1f || dt <= 0.0f) return;

        float target = chatOpen ? 1.0f : 0.0f;
        float stiffness = 200.0f;
        float damping = 12.0f * (1.5f - cfg.chromaChatAnimBounciness);
        float force = -stiffness * (animAmount - target) - damping * animVelocity;

        animVelocity += force * dt;
        animVelocity = MathHelper.clamp_float(animVelocity, -50.0f, 50.0f);
        animAmount += animVelocity * dt;

        if (target == 0.0f && animAmount < 0.001f && Math.abs(animVelocity) < 0.01f) {
            animAmount = 0.0f; animVelocity = 0.0f;
        }
    }

    // =================================================================
    //  Alpha calculation (P1)
    // =================================================================
    private int calcAlpha(int age, boolean chatOpen, float opacity) {
        int alpha;
        if (chatOpen) {
            alpha = 255;
        } else {
            // 原版 200 ticks ≈ 10 秒淡出，但这里使用更平滑的曲线
            double fade = (double) Math.max(0, 200 - age) / 200.0;
            fade = MathHelper.clamp_double(fade, 0.0, 1.0);
            fade = fade * fade; // 平方缓出
            alpha = (int) (255.0 * fade);
        }
        return (int) (alpha * opacity);
    }

    // =================================================================
    //  Message tracking (P1)
    // =================================================================
    private void trackNewMessages(List<MyChatLine> lines, BetterPlayerHUDConfig cfg, long now) {
        if (lines == null || !cfg.chromaChatMsgAnimEnable) return;
        int cur = lines.size();
        int prev = prevLineCount;
        prevLineCount = cur;
        if (cur <= prev || lines.isEmpty()) return;

        int newCount = Math.min(cur - prev, cur);
        for (int i = 0; i < newCount; i++) {
            MyChatLine ml = lines.get(i);
            if (ml == null) continue;
            if (!msgAnimMap.containsKey(ml.updateCounter)) {
                msgAnimMap.put(ml.updateCounter, now);
            }
        }
    }

    private int getMsgOffset(int ctr, BetterPlayerHUDConfig cfg) {
        if (!cfg.chromaChatMsgAnimEnable) return 0;
        Long start = msgAnimMap.get(ctr);
        if (start == null) return 0;

        long elapsed = Minecraft.getSystemTime() - start;
        float dur = cfg.chromaChatMsgAnimDuration;
        if (elapsed >= dur) {
            msgAnimMap.remove(ctr);
            return 0;
        }

        // easeOutBack: overshoot 到 1.2 然后回弹到 1.0, 映射到 6px→0px
        float t = (float) elapsed / dur;
        float ease = 1.0f + 2.70158f * (float)Math.pow(t - 1.0, 3) + 1.70158f * (float)Math.pow(t - 1.0, 2);
        return (int)(6.0f * (1.0f - ease));
    }

    // =================================================================
    //  Render helpers
    // =================================================================
    private void drawHover(int left, int top, int right, int bottom, BetterPlayerHUDConfig cfg) {
        if (cfg == null || !cfg.chromaChatHoverHighlight) return;
        Gui.drawRect(left, top, right, bottom, cfg.chromaChatHoverColor);
    }

    // =================================================================
    //  Rounded rect (Tessellator batched)
    // =================================================================
    // ── 将颜色的 Alpha 通道乘以 factor ──
    private static int fadeAlpha(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        a = (int)(a * factor);
        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return (a << 24) | (color & 0xFFFFFF);
    }

    private void drawRoundedRect(int left, int top, int right, int bottom,
                                  int radius, int fillColor, int borderColor) {
        if (radius <= 0) {
            Gui.drawRect(left, top, right, bottom, fillColor);
            if ((borderColor >>> 24) > 0) {
                Gui.drawRect(left, top, right, top + 1, borderColor);
                Gui.drawRect(left, bottom - 1, right, bottom, borderColor);
                Gui.drawRect(left, top, left + 1, bottom, borderColor);
                Gui.drawRect(right - 1, top, right, bottom, borderColor);
            }
            return;
        }

        int r = Math.min(radius, Math.min((right - left) / 2, (bottom - top) / 2));
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        int fr = (fillColor >> 16) & 0xFF, fg = (fillColor >> 8) & 0xFF,
            fb = fillColor & 0xFF, fa = (fillColor >>> 24) & 0xFF;

        // Center rect
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left + r, top + r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(right - r, top + r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(right - r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(left + r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
        tess.draw();

        // Top bar
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left + r, top, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(right - r, top, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(right - r, top + r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(left + r, top + r, 0).color(fr, fg, fb, fa).endVertex();
        tess.draw();

        // Bottom bar
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left + r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(right - r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(right - r, bottom, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(left + r, bottom, 0).color(fr, fg, fb, fa).endVertex();
        tess.draw();

        // Left bar
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left, top + r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(left + r, top + r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(left + r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(left, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
        tess.draw();

        // Right bar
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(right - r, top + r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(right, top + r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(right, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
        wr.pos(right - r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
        tess.draw();

        // 4 corners: TL
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int a = 0; a < 90; a += 10) {
            double r1 = Math.toRadians(a), r2 = Math.toRadians(a + 10);
            double x1 = left + r - r * Math.cos(r1), y1 = top + r - r * Math.sin(r1);
            double x2 = left + r - r * Math.cos(r2), y2 = top + r - r * Math.sin(r2);
            wr.pos(left + r, top + r, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x2, y2, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x1, y1, 0).color(fr, fg, fb, fa).endVertex();
        }
        tess.draw();

        // TR
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int a = 0; a < 90; a += 10) {
            double r1 = Math.toRadians(a), r2 = Math.toRadians(a + 10);
            double x1 = right - r + r * Math.cos(r1), y1 = top + r - r * Math.sin(r1);
            double x2 = right - r + r * Math.cos(r2), y2 = top + r - r * Math.sin(r2);
            wr.pos(right - r, top + r, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x1, y1, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x2, y2, 0).color(fr, fg, fb, fa).endVertex();
        }
        tess.draw();

        // BR
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int a = 0; a < 90; a += 10) {
            double r1 = Math.toRadians(a), r2 = Math.toRadians(a + 10);
            double x1 = right - r + r * Math.cos(r1 + 90), y1 = bottom - r + r * Math.sin(r1 + 90);
            double x2 = right - r + r * Math.cos(r2 + 90), y2 = bottom - r + r * Math.sin(r2 + 90);
            wr.pos(right - r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x1, y1, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x2, y2, 0).color(fr, fg, fb, fa).endVertex();
        }
        tess.draw();

        // BL
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int a = 0; a < 90; a += 10) {
            double r1 = Math.toRadians(a + 90), r2 = Math.toRadians(a + 100);
            double x1 = left + r - r * Math.cos(r1), y1 = bottom - r + r * Math.sin(r1);
            double x2 = left + r - r * Math.cos(r2), y2 = bottom - r + r * Math.sin(r2);
            wr.pos(left + r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x1, y1, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x2, y2, 0).color(fr, fg, fb, fa).endVertex();
        }
        tess.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}