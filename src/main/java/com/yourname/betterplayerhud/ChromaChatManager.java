package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 「蜃楼」ChromaChat — 现代聊天框
 *
 * 取消原版聊天框渲染，叠层绘制自定义聊天框。
 * 反射读原版 GuiNewChat 数据（只读），不影响其他 mod。
 *
 * Phase 1 骨架：事件取消 + 数据桥 + 弹性动画 + 文字渲染 + 入场跟踪
 */
@SideOnly(Side.CLIENT)
public class ChromaChatManager {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ═══════════════════════════════════════════════════════════════
    //  反射数据桥
    // ═══════════════════════════════════════════════════════════════
    private GuiNewChat vanillaChat;
    private Field fieldDrawnChatLines;  // List<ChatLine>
    private Field fieldChatLines;      // List<ChatLine> (持久)
    private Field fieldScrollPos;      // int
    private Field fieldIsScrolled;     // boolean
    private boolean reflectionReady = false;

    // ═══════════════════════════════════════════════════════════════
    //  弹簧动画
    // ═══════════════════════════════════════════════════════════════
    private float animAmount = 1.0f;       // 当前开合度 (0=收, 1=展)
    private float animVelocity = 0.0f;
    private long lastAnimTime = 0L;

    // ═══════════════════════════════════════════════════════════════
    //  消息入场跟踪（环形缓冲）
    // ═══════════════════════════════════════════════════════════════
    private static final int TRACK_SIZE = 100;
    private final int[] trackCounters = new int[TRACK_SIZE];
    private final long[] trackTimesMs = new long[TRACK_SIZE];
    private int trackHead = 0;
    private int prevLineCount = 0;

    // ═══════════════════════════════════════════════════════════════
    //  构造 & 反射初始化
    // ═══════════════════════════════════════════════════════════════
    public ChromaChatManager() {
        // 清空消息跟踪
        for (int i = 0; i < TRACK_SIZE; i++) {
            trackCounters[i] = -1;
            trackTimesMs[i] = 0L;
        }
        initReflection();
    }

    public void onConfigChanged() {
        // 配置变更时无需额外操作，渲染时直接读 BetterPlayerHUD.config
    }

    private void initReflection() {
        try {
            vanillaChat = mc.ingameGUI.getChatGUI();
            Class<?> c = GuiNewChat.class;

            fieldDrawnChatLines = findField(c, "drawnChatLines", "field_146253_i");
            fieldChatLines      = findField(c, "chatLines",      "field_146252_h");
            fieldScrollPos      = findField(c, "scrollPos",      "field_146250_j");
            fieldIsScrolled     = findField(c, "isScrolled",     "field_146251_k");

            reflectionReady = (fieldDrawnChatLines != null && fieldScrollPos != null);
            if (!reflectionReady) {
                System.err.println("[ChromaChat] 反射字段未完全就绪");
            }
        } catch (Exception e) {
            System.err.println("[ChromaChat] 反射初始化失败: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  主事件 — 取消原版 + 叠层渲染
    // ═══════════════════════════════════════════════════════════════
    @SubscribeEvent
    public void onChatRender(RenderGameOverlayEvent.Chat event) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null || !cfg.enableChromaChat || !reflectionReady) return;

        // 1) 取消原版渲染
        event.setCanceled(true);

        // 2) 读原版数据
        List<ChatLine> drawnLines  = getDrawnLines();
        int scrollPos              = getScrollPos();
        boolean isScrolled         = getIsScrolled();

        // 3) 计算可见行数（去除超出 viewport 的行）
        int totalLines = (drawnLines != null) ? drawnLines.size() : 0;
        int visibleCount = Math.min(totalLines, cfg.chromaChatLineCount);

        // 4) 动画更新
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        long now = Minecraft.getSystemTime();
        updateSpring(chatOpen, now, cfg);

        // 5) 常态关闭且无消息 → 跳过渲染
        if (!chatOpen && animAmount < 0.01f && totalLines == 0) return;

        // 6) 消息入场跟踪
        trackNewMessages(drawnLines, cfg, now);

        // 7) ═══ 渲染 ═══
        ScaledResolution res = event.resolution;
        float scale = MathHelper.clamp_float(animAmount, 0.0f, 1.25f);

        int baseX = 2;           // 原版聊天框 X
        int baseY = 20;          // 原版聊天框 Y
        int chatWidth = cfg.chromaChatWidth;
        int lineH = 9;           // fontHeight
        int contentH = visibleCount * lineH;
        int bgH = contentH + 4;  // 上下各 2px padding

        // 如果缩放极小 → 只画极简
        if (scale < 0.01f) return;

        // 整体弹性变换（从底部锚点缩放）
        GlStateManager.pushMatrix();
        // 支点 = 聊天框左下角
        float anchorY = baseY + bgH;
        GlStateManager.translate(0.0f, anchorY, 0.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(0.0f, -anchorY, 0.0f);

        // ── 背景（圆角矩形） ──
        if (visibleCount > 0) {
            drawRoundedRect(baseX, baseY, baseX + chatWidth, baseY + bgH,
                    cfg.chromaChatBorderRadius, cfg.chromaChatBackgroundColor,
                    cfg.chromaChatBorderColor);
        }

        // ── 消息文本 ──
        if (drawnLines != null && !drawnLines.isEmpty()) {
            // 原版透明度
            float opacity = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
            int drawEnd = Math.min(scrollPos + visibleCount, totalLines);
            int drawStart = scrollPos;
            int y = baseY + 2; // 顶部 padding 2px

            for (int i = drawStart; i < drawEnd && i < totalLines; i++) {
                ChatLine cl = drawnLines.get(i);
                if (cl == null) { y -= lineH; continue; }

                int age = mc.ingameGUI.getUpdateCounter() - cl.getUpdatedCounter();
                int alpha = calcAlpha(age, chatOpen, opacity);

                if (alpha <= 3) { y -= lineH; continue; }

                // 新消息入场偏移
                int msgOffset = getMsgOffset(cl.getUpdatedCounter(), cfg);
                y += msgOffset; // 向下偏移（新消息从底部弹入）

                String text = cl.getChatComponent().getFormattedText();
                mc.fontRendererObj.drawString(text,
                        baseX + 2, y,
                        0xFFFFFF | (alpha << 24));

                y -= lineH;
            }
        }

        // ── 滚动指示器 ──
        if (isScrolled) {
            int indicatorX = baseX + chatWidth - 3;
            int indicatorH = Math.max(4, bgH * visibleCount / Math.max(totalLines, 1));
            int indicatorY = baseY + bgH - bgH * scrollPos / Math.max(totalLines, 1);
            Gui.drawRect(indicatorX, indicatorY,
                    indicatorX + 2, indicatorY + indicatorH,
                    0x99FFFFFF | (0x88 << 24));
        }

        GlStateManager.popMatrix();
    }

    // ═══════════════════════════════════════════════════════════════
    //  弹簧物理
    // ═══════════════════════════════════════════════════════════════
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
            animAmount = 0.0f;
            animVelocity = 0.0f;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Alpha 计算（与原版一致）
    // ═══════════════════════════════════════════════════════════════
    private int calcAlpha(int age, boolean chatOpen, float opacity) {
        int alpha;
        if (chatOpen) {
            alpha = 255;
        } else {
            double fade = (double) Math.max(0, 200 - age) / 200.0;
            fade = fade * 10.0;
            fade = MathHelper.clamp_double(fade, 0.0, 1.0);
            fade = fade * fade;
            alpha = (int) (255.0 * fade);
        }
        return (int) (alpha * opacity);
    }

    // ═══════════════════════════════════════════════════════════════
    //  消息入场跟踪
    // ═══════════════════════════════════════════════════════════════
    private void trackNewMessages(List<ChatLine> lines, BetterPlayerHUDConfig cfg, long now) {
        if (lines == null || !cfg.chromaChatMsgAnimEnable) return;
        int currentCount = lines.size();

        if (currentCount > 0 && currentCount > trackHead) {
            // 新消息排在最前面（索引 0 = 最新）
            // 比较 trackHead 和当前 count 可发现增量
            // 但更可靠的方式：检查 updateCounter
            ChatLine newest = lines.get(0);
            int ctr = newest.getUpdatedCounter();
            // 如果这个 counter 还没记录过 → 是新消息
            boolean found = false;
            for (int i = 0; i < TRACK_SIZE; i++) {
                if (trackCounters[i] == ctr) { found = true; break; }
            }
            if (!found) {
                trackCounters[trackHead] = ctr;
                trackTimesMs[trackHead] = now;
                trackHead = (trackHead + 1) % TRACK_SIZE;
            }
        }

        // 简单后备：当行数增加时，记录新行
        if (currentCount > prevLineCount) {
            int diff = currentCount - prevLineCount;
            for (int i = 0; i < diff && i < lines.size(); i++) {
                ChatLine cl = lines.get(i);
                boolean found = false;
                for (int j = 0; j < TRACK_SIZE; j++) {
                    if (trackCounters[j] == cl.getUpdatedCounter()) { found = true; break; }
                }
                if (!found) {
                    trackCounters[trackHead] = cl.getUpdatedCounter();
                    trackTimesMs[trackHead] = now;
                    trackHead = (trackHead + 1) % TRACK_SIZE;
                }
            }
        }
        prevLineCount = currentCount;
    }

    private int getMsgOffset(int updateCounter, BetterPlayerHUDConfig cfg) {
        if (!cfg.chromaChatMsgAnimEnable) return 0;
        long now = Minecraft.getSystemTime();
        for (int i = 0; i < TRACK_SIZE; i++) {
            if (trackCounters[i] == updateCounter) {
                long elapsed = now - trackTimesMs[i];
                if (elapsed < cfg.chromaChatMsgAnimDuration) {
                    float progress = (float) elapsed / cfg.chromaChatMsgAnimDuration;
                    // 从 6px 偏移 → 0px，弹性衰减
                    return (int) (6.0f * (1.0f - progress));
                }
                return 0;
            }
        }
        return 0;
    }

    // ═══════════════════════════════════════════════════════════════
    //  圆角矩形渲染
    // ═══════════════════════════════════════════════════════════════
    private void drawRoundedRect(int left, int top, int right, int bottom,
                                  int radius, int fillColor, int borderColor) {
        if (radius <= 0) {
            // 无圆角 → 直接 drawRect
            Gui.drawRect(left, top, right, bottom, fillColor);
            if ((borderColor >>> 24) > 0) {
                Gui.drawRect(left, top, right, top + 1, borderColor);
                Gui.drawRect(left, bottom - 1, right, bottom, borderColor);
                Gui.drawRect(left, top, left + 1, bottom, borderColor);
                Gui.drawRect(right - 1, top, right, bottom, borderColor);
            }
            return;
        }

        // 用 Tessellator 画圆角（单批次）
        int r = Math.min(radius, Math.min((right - left) / 2, (bottom - top) / 2));
        float f = 0.5f; // 校正像素对齐

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        // 填充色
        int fillR = (fillColor >> 16) & 0xFF;
        int fillG = (fillColor >> 8) & 0xFF;
        int fillB = fillColor & 0xFF;
        int fillA = (fillColor >> 24) & 0xFF;

        // 四个角圆弧 + 中心矩形 = 大约 5 个四边形批次
        // 中心矩形
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left + r, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(right - r, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(right - r, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(left + r, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        tess.draw();

        // 上条
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left + r, top, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(right - r, top, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(right - r, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(left + r, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        tess.draw();

        // 下条
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left + r, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(right - r, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(right - r, bottom, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(left + r, bottom, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        tess.draw();

        // 左条
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(left + r, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(left + r, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(left, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        tess.draw();

        // 右条
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(right - r, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(right, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(right, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        wr.pos(right - r, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
        tess.draw();

        // 四个圆角用矩形近似（6 个顶点每个角 = 4 个四边形）
        // 左上
        for (int angle = 0; angle < 90; angle += 10) {
            double rad1 = Math.toRadians(angle);
            double rad2 = Math.toRadians(angle + 10);
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            double x1 = left + r - r * Math.cos(rad1);
            double y1 = top + r - r * Math.sin(rad1);
            double x2 = left + r - r * Math.cos(rad2);
            double y2 = top + r - r * Math.sin(rad2);
            wr.pos(left + r, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            wr.pos(x2, y2, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            wr.pos(x1, y1, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            tess.draw();
        }
        // 右上
        for (int angle = 0; angle < 90; angle += 10) {
            double rad1 = Math.toRadians(angle);
            double rad2 = Math.toRadians(angle + 10);
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            double x1 = right - r + r * Math.cos(rad1);
            double y1 = top + r - r * Math.sin(rad1);
            double x2 = right - r + r * Math.cos(rad2);
            double y2 = top + r - r * Math.sin(rad2);
            wr.pos(right - r, top + r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            wr.pos(x1, y1, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            wr.pos(x2, y2, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            tess.draw();
        }
        // 右下
        for (int angle = 0; angle < 90; angle += 10) {
            double rad1 = Math.toRadians(angle);
            double rad2 = Math.toRadians(angle + 10);
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            double x1 = right - r + r * Math.cos(rad1);
            double y1 = bottom - r + r * Math.sin(rad1);
            double x2 = right - r + r * Math.cos(rad2);
            double y2 = bottom - r + r * Math.sin(rad2);
            wr.pos(right - r, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            wr.pos(x1, y1, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            wr.pos(x2, y2, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            tess.draw();
        }
        // 左下
        for (int angle = 0; angle < 90; angle += 10) {
            double rad1 = Math.toRadians(angle);
            double rad2 = Math.toRadians(angle + 10);
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            double x1 = left + r - r * Math.cos(rad1);
            double y1 = bottom - r + r * Math.sin(rad1);
            double x2 = left + r - r * Math.cos(rad2);
            double y2 = bottom - r + r * Math.sin(rad2);
            wr.pos(left + r, bottom - r, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            wr.pos(x2, y2, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            wr.pos(x1, y1, 0.0).color(fillR, fillG, fillB, fillA).endVertex();
            tess.draw();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // ═══════════════════════════════════════════════════════════════
    //  反射工具
    // ═══════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private List<ChatLine> getDrawnLines() {
        try {
            return (List<ChatLine>) fieldDrawnChatLines.get(vanillaChat);
        } catch (Exception e) { return null; }
    }

    private int getScrollPos() {
        try {
            return fieldScrollPos.getInt(vanillaChat);
        } catch (Exception e) { return 0; }
    }

    private boolean getIsScrolled() {
        try {
            return fieldIsScrolled.getBoolean(vanillaChat);
        } catch (Exception e) { return false; }
    }

    private static Field findField(Class<?> clazz, String mcpName, String srgName) {
        try {
            Field f = clazz.getDeclaredField(mcpName);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            try {
                Field f = clazz.getDeclaredField(srgName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e2) {
                return null;
            }
        }
    }
}
