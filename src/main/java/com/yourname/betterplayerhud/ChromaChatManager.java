package com.yourname.betterplayerhud;

import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

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
    private int nextLineId = 1;
    // 滚轮预读（绕过 GuiChat 消耗）
    private int pendingScroll = 0;

    private static class MyChatLine {
        final IChatComponent message;
        final int updateCounter;
        final int chatLineID;
        final long receivedTimeMs;
        final String formattedTime;      // 缓存的 "[HH:MM] " 时间戳
        // 换行缓存（lazy）
        private String[] cachedLines = null;
        private int lastWrapWidth = -1;

        MyChatLine(IChatComponent msg, int ctr, int id, long timeMs) {
            this.message = msg;
            this.updateCounter = ctr;
            this.chatLineID = id;
            this.receivedTimeMs = timeMs;
            this.formattedTime = formatChatTimestamp(timeMs);
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

    // === Spring Animation (P1) ===
    private float animAmount = 1.0f;
    private float animVelocity = 0.0f;
    private long lastAnimTime = 0L;

    // === Message Animation Tracking (P1) ===
    private static final int TRACK_SIZE = 100;
    private final int[] trackCounters = new int[TRACK_SIZE];
    private final long[] trackTimesMs = new long[TRACK_SIZE];
    private int trackHead = 0;
    private int prevLineCount = 0;

    // === Hover Interaction (P2) ===
    private int hoveredLineIdx = -1;
    private int hoveredLineAbsIdx = -1;
    private long hoverUpdateTime = 0L;
    private boolean mouseBtnDown = false;
    private long mousePressTime = 0L;

    // === Message Grouping (P3) ===
    private List<MyChatLine> lastDrawnLinesRef = null;
    private GroupInfo[] groupCache = null;

    private static class GroupInfo {
        final MyChatLine line;
        final int count;
        GroupInfo(MyChatLine line, int count) { this.line = line; this.count = count; }
    }

    public ChromaChatManager() {
        INSTANCE = this;
        for (int i = 0; i < TRACK_SIZE; i++) trackCounters[i] = -1;
    }

    public void onConfigChanged() {}

    // ── 调试计数（每 300 帧 ≈ 5秒 打印一次） ──
    private int debugFrameCounter = 0;

    // =================================================================
    //  ClientChatReceivedEvent — 消息源头拦截
    // =================================================================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatReceived(ClientChatReceivedEvent event) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null || !cfg.enableChromaChat) return;

        // [DEBUG] 收到了消息
        System.out.println("[ChromaChat] intercepted: " + event.message.getUnformattedText());

        event.setCanceled(true);

        int ctr = mc.ingameGUI.getUpdateCounter();
        long nowMs = System.currentTimeMillis();  // 真实墙上时钟
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
        long nowMs = System.currentTimeMillis();
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

        // [DEBUG] must be seen in log
        debugFrameCounter++;
        if (debugFrameCounter % 300 == 0) {
            System.out.println("[ChromaChat] render: lines=" + myChatLines.size()
                + " chatOpen=" + (mc.currentScreen instanceof GuiChat)
                + " anim=" + animAmount
                + " scroll=" + myScrollPos);
        }

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
        int msgTextWidth = textWidth - timeWidth;  // 除去时间戳后的纯文本宽度

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

        // ── 滚动检测 ──
        int wheel = pendingScroll;
        pendingScroll = 0;
        if (wheel != 0) {
            int dir = (wheel > 0) ? -1 : 1;
            int maxScroll = Math.max(0, totalLines - 1);
            myScrollPos = MathHelper.clamp_int(myScrollPos + dir * 3, 0, maxScroll);
            myIsScrolled = myScrollPos > 0;
        }

        // ── 鼠标状态（多行消息的Y命中检测） ──
        int mouseSx = Mouse.getX() * res.getScaledWidth() / Math.max(mc.displayWidth, 1);
        int mouseSy = res.getScaledHeight()
                - Mouse.getY() * res.getScaledHeight() / Math.max(mc.displayHeight, 1) - 1;

        boolean inChat = mouseSx >= baseX && mouseSx <= baseX + chatWidth
                      && mouseSy >= baseY && mouseSy <= baseY + bgH;

        int newHoveredIdx = -1, newHoveredAbs = -1;
        if (inChat) {
            // Y 计算必须与 renderNormal/renderGrouped 完全一致：
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

        // ── 消息动画跟踪 ──
        trackNewMessages(myChatLines, cfg, now);

        // ── 分组缓存 ──
        boolean grouping = cfg.chromaChatMessageGrouping;
        if (grouping && myChatLines != lastDrawnLinesRef) {
            rebuildGroupCache(myChatLines);
            lastDrawnLinesRef = myChatLines;
        }

        // ═══════════════ Render ═══════════════
        GlStateManager.pushMatrix();

        // -- 背景 --
        drawRoundedRect(baseX, baseY, baseX + chatWidth, baseY + bgH,
                cfg.chromaChatBorderRadius, cfg.chromaChatBackgroundColor, cfg.chromaChatBorderColor);

        // -- 消息 --
        if (visibleCount > 0) {
            float opacity = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
            int updateCounter = mc.ingameGUI.getUpdateCounter();

            if (grouping && groupCache != null) {
                renderGrouped(cfg, myScrollPos, visibleCount, baseX, chatWidth, lineH, baseY, bgH,
                        updateCounter, chatOpen, opacity, now, mouseClicked,
                        showTime, msgTextWidth, timeWidth);
            } else {
                renderNormal(cfg, myChatLines, myScrollPos, visibleCount, baseX, chatWidth, lineH, baseY, bgH,
                        updateCounter, chatOpen, opacity, now, mouseClicked,
                        showTime, msgTextWidth, timeWidth);
            }
        }

        // -- 滚动指示器（基于文本行数比例） --
        if (myIsScrolled && totalLines > 0) {
            // 计算总文本行数（用于滚动比例条）
            int totalTextLines = 0;
            int maxLines = Math.min(totalLines, 200);  // 防长列表性能损
            for (int i = 0; i < maxLines; i++) {
                MyChatLine ml = myChatLines.get(i);
                totalTextLines += (ml != null) ? ml.getLineCount(msgTextWidth) : 1;
            }
            int visibleTL = Math.max(visibleTextLines, 1);
            int totalTL = Math.max(totalTextLines, 1);
            int scrollTL = 0;
            int scrollMax = Math.min(myScrollPos, maxLines);
            for (int i = 0; i < scrollMax; i++) {
                MyChatLine ml = myChatLines.get(i);
                scrollTL += (ml != null) ? ml.getLineCount(msgTextWidth) : 1;
            }

            int ix = baseX + chatWidth - 3;
            int ih = Math.max(4, bgH * visibleTL / totalTL);
            // 条顶部Y: scrollTL=0(未滚动)→底端, scrollTL=totalTL(最旧)→顶端
            int iy = baseY + bgH - ih - (int)((long)(bgH - ih) * scrollTL / totalTL);
            Gui.drawRect(ix, iy, ix + 2, iy + ih, 0x99FFFFFF | (0x88 << 24));
        }

        GlStateManager.popMatrix();

        // ── 上报位置给 F7 编辑模式 ──
        HUDEditManager.report("蜃楼聊天框", baseX, baseY, chatWidth, bgH);
    }

    // =================================================================
    //  Render: Normal mode
    // =================================================================
    private void renderNormal(BetterPlayerHUDConfig cfg, List<MyChatLine> lines, int scrollPos,
                              int vis, int baseX, int chatWidth, int lineH, int baseY, int bgH,
                              int updateCounter, boolean chatOpen, float opacity, long now,
                              boolean mouseClicked, boolean showTime, int textWidth, int timeWidth) {
        int drawEnd = Math.min(scrollPos + vis, lines.size());
        // 从背景框底部开始，向上排布（最新在最下）
        int y = baseY + bgH - 2;
        for (int i = scrollPos; i < drawEnd; i++) {
            MyChatLine ml = lines.get(i);
            if (ml == null) continue;

            int age = updateCounter - ml.updateCounter;
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
            for (int j = 0; j < wl.length; j++) {
                int lineY = entryTop + j * lineH;
                int tx = baseX + 2;
                if (showTime && j == 0) {
                    mc.fontRendererObj.drawString(ml.formattedTime, tx, lineY, (alpha << 24) | 0x888888);
                    tx += timeWidth;
                }
                mc.fontRendererObj.drawString(wl[j], tx, lineY, 0xFFFFFF | (alpha << 24));
            }

            y = entryTop; // 上移，给下一条消息（更旧的）腾位置
        }
    }

    // =================================================================
    //  Render: Grouped mode (P3)
    // =================================================================
    private void renderGrouped(BetterPlayerHUDConfig cfg, int scrollPos,
                               int vis, int baseX, int chatWidth, int lineH, int baseY, int bgH,
                               int updateCounter, boolean chatOpen, float opacity, long now,
                               boolean mouseClicked, boolean showTime, int textWidth, int timeWidth) {
        int drawn = 0;
        int y = baseY + bgH - 2;
        for (int gi = 0; gi < groupCache.length && drawn < vis; gi++) {
            if (gi < scrollPos) {
                drawn++;
                continue;
            }

            GroupInfo g = groupCache[gi];
            MyChatLine ml = g.line;
            if (ml == null) { drawn++; continue; }

            int age = updateCounter - ml.updateCounter;
            int alpha = calcAlpha(age, chatOpen, opacity);
            if (alpha <= 3) { drawn++; continue; }

            y += getMsgOffset(ml.updateCounter, cfg);

            String[] wl = ml.getWrappedLines(textWidth);
            int entryH = wl.length * lineH;
            int entryTop = y - entryH;
            int entryBottom = entryTop + entryH;

            // 悬停 + 点击
            if (gi == hoveredLineAbsIdx) {
                drawHover(baseX + 1, entryTop, baseX + chatWidth - 1, entryBottom, cfg);
                if (mouseClicked) {
                    forwardClick(ml.message);
                }
            }

            for (int j = 0; j < wl.length; j++) {
                int lineY = entryTop + j * lineH;
                int tx = baseX + 2;
                if (showTime && j == 0) {
                    mc.fontRendererObj.drawString(ml.formattedTime, tx, lineY, (alpha << 24) | 0x888888);
                    tx += timeWidth;
                }
                mc.fontRendererObj.drawString(wl[j], tx, lineY, 0xFFFFFF | (alpha << 24));
            }

            // 分组徽标 [Nx]（附在第一行末尾）
            if (g.count > 1) {
                String badge = "[" + g.count + "x]";
                int bw = mc.fontRendererObj.getStringWidth(badge);
                int bc = 0xAAFFFFFF | (Math.min(alpha + 40, 255) << 24);
                mc.fontRendererObj.drawString(badge, baseX + chatWidth - bw - 2, entryTop, bc);
            }

            y = entryTop;
            drawn++;
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
    //  Client tick — 提前抢读滚轮值（GuiChat 会消耗 Mouse.getDWheel）
    // =================================================================
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
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
        if (cur > prevLineCount && !lines.isEmpty()) {
            int newCount = Math.min(cur - prevLineCount, cur);
            for (int i = 0; i < newCount; i++) {
                MyChatLine ml = lines.get(i);
                if (ml == null) continue;
                boolean found = false;
                for (int j = 0; j < TRACK_SIZE; j++) {
                    if (trackCounters[j] == ml.updateCounter) { found = true; break; }
                }
                if (!found) {
                    trackCounters[trackHead] = ml.updateCounter;
                    trackTimesMs[trackHead] = now;
                    trackHead = (trackHead + 1) % TRACK_SIZE;
                }
            }
        }
        prevLineCount = cur;
    }

    private int getMsgOffset(int ctr, BetterPlayerHUDConfig cfg) {
        if (!cfg.chromaChatMsgAnimEnable) return 0;
        long now = Minecraft.getSystemTime();
        for (int i = 0; i < TRACK_SIZE; i++) {
            if (trackCounters[i] == ctr) {
                long elapsed = now - trackTimesMs[i];
                if (elapsed < cfg.chromaChatMsgAnimDuration) {
                    return (int) (6.0f * (1.0f - (float) elapsed / cfg.chromaChatMsgAnimDuration));
                }
                return 0;
            }
        }
        return 0;
    }

    // =================================================================
    //  Message grouping (P3)
    // =================================================================
    private void rebuildGroupCache(List<MyChatLine> lines) {
        if (lines == null || lines.isEmpty()) { groupCache = null; return; }
        GroupInfo[] tmp = new GroupInfo[lines.size()];
        int out = 0;
        String prevText = null;
        MyChatLine groupLine = null;
        int count = 0;

        for (MyChatLine ml : lines) {
            String text = ml.message.getFormattedText();
            if (text.equals(prevText) && groupLine != null) {
                count++;
                tmp[out - 1] = new GroupInfo(groupLine, count);
            } else {
                groupLine = ml;
                count = 1;
                prevText = text;
                tmp[out] = new GroupInfo(ml, 1);
                out++;
            }
        }

        GroupInfo[] result = new GroupInfo[out];
        System.arraycopy(tmp, 0, result, 0, out);
        groupCache = result;
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