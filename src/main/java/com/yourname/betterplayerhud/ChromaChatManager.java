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
import java.util.List;

/**
 * \u201c\u86ed\u697c\u201d ChromaChat \u2014 \u73b0\u4ee3\u804a\u5929\u6846
 * <p>
 * Phase 1: \u9aa8\u67b6
 * Phase 2: \u60ac\u505c\u9ad8\u4eae + \u9f20\u6807\u4ea4\u4e92
 * Phase 3: \u6d88\u606f\u5206\u7ec4\u6298\u53e0
 * Phase 4: \u914d\u7f6e GUI \u96c6\u6210
 */
@SideOnly(Side.CLIENT)
public class ChromaChatManager {

    private static final long CLICK_THRESHOLD_MS = 200L;
    private static final Minecraft mc = Minecraft.getMinecraft();

    // === Reflection ===
    private GuiNewChat vanillaChat;
    private Field fieldDrawnChatLines;
    private Field fieldChatLines;
    private Field fieldScrollPos;
    private Field fieldIsScrolled;
    private boolean reflectionReady = false;

    // === Spring Animation (P1) ===
    private float animAmount = 1.0f;
    private float animVelocity = 0.0f;
    private long lastAnimTime = 0L;

    // === Message Tracking (P1) ===
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
    private List<ChatLine> lastDrawnLinesRef = null;
    private GroupInfo[] groupCache = null;

    private static class GroupInfo {
        final ChatLine line;
        final int count;
        GroupInfo(ChatLine line, int count) { this.line = line; this.count = count; }
    }

    // =================================================================
    //  Constructor & Reflection
    // =================================================================
    public ChromaChatManager() {
        for (int i = 0; i < TRACK_SIZE; i++) trackCounters[i] = -1;
        initReflection();
    }

    public void onConfigChanged() {}

    private void initReflection() {
        try {
            vanillaChat = mc.ingameGUI.getChatGUI();
            Class<?> c = GuiNewChat.class;
            fieldDrawnChatLines = findField(c, "drawnChatLines", "field_146253_i");
            fieldChatLines      = findField(c, "chatLines",      "field_146252_h");
            fieldScrollPos      = findField(c, "scrollPos",      "field_146250_j");
            fieldIsScrolled     = findField(c, "isScrolled",     "field_146251_k");
            reflectionReady = (fieldDrawnChatLines != null && fieldScrollPos != null);
            if (!reflectionReady)
                System.err.println("[ChromaChat] Reflection fields not ready");
        } catch (Exception e) {
            System.err.println("[ChromaChat] Reflection init failed: " + e.getMessage());
        }
    }

    // =================================================================
    //  Main Event
    // =================================================================
    @SubscribeEvent
    public void onChatRender(RenderGameOverlayEvent.Chat event) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;

        // cancel original first - even if reflection fails
        if (cfg != null && cfg.enableChromaChat) {
            event.setCanceled(true);
        } else {
            return;
        }

        // if reflection not ready, return after cancellation
        if (!reflectionReady) return;

        List<ChatLine> drawnLines = getDrawnLines();
        int scrollPos = getScrollPos();
        boolean isScrolled = getIsScrolled();
        if (drawnLines == null) return;

        int totalLines = drawnLines.size();
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        long now = Minecraft.getSystemTime();
        updateSpring(chatOpen, now, cfg);

        if (!chatOpen && animAmount < 0.01f && totalLines == 0) return;

        float scale = MathHelper.clamp_float(animAmount, 0.0f, 1.25f);
        if (scale < 0.01f) return;

        // Layout
        ScaledResolution res = event.resolution;
        int baseX = 2;
        int baseY = 20;
        int chatWidth = cfg.chromaChatWidth;
        int lineH = 9;
        int visibleCount = Math.min(totalLines - scrollPos, cfg.chromaChatLineCount);
        int contentH = visibleCount * lineH;
        int bgH = contentH + 4;

        // === P2: Mouse ===
        int mouseSx = Mouse.getX() * res.getScaledWidth() / mc.displayWidth;
        int mouseSy = res.getScaledHeight()
                - Mouse.getY() * res.getScaledHeight() / mc.displayHeight - 1;

        boolean inChat = mouseSx >= baseX && mouseSx <= baseX + chatWidth
                      && mouseSy >= baseY && mouseSy <= baseY + bgH;

        int newHoveredIdx = -1, newHoveredAbs = -1;
        if (inChat) {
            int relY = mouseSy - (baseY + 2);
            int li = relY / lineH;
            if (li >= 0 && li < visibleCount) {
                newHoveredIdx = li;
                newHoveredAbs = scrollPos + li;
            }
        }

        // Debounce
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

        // Click detection
        boolean mouseClicked = false;
        if (Mouse.isButtonDown(0)) {
            if (!mouseBtnDown) { mouseBtnDown = true; mousePressTime = now; }
        } else {
            if (mouseBtnDown && (now - mousePressTime) < CLICK_THRESHOLD_MS) {
                if (hoveredLineAbsIdx >= 0 && inChat && chatOpen) mouseClicked = true;
            }
            mouseBtnDown = false;
        }

        // === P1: Track ===
        trackNewMessages(drawnLines, cfg, now);

        // === P3: Group cache ===
        boolean grouping = cfg.chromaChatMessageGrouping;
        if (grouping && drawnLines != lastDrawnLinesRef) {
            rebuildGroupCache(drawnLines);
            lastDrawnLinesRef = drawnLines;
        }

        // =============== Render ===============
        GlStateManager.pushMatrix();
        float anchorY = baseY + bgH;
        GlStateManager.translate(0.0f, anchorY, 0.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(0.0f, -anchorY, 0.0f);

        // -- Background --
        drawRoundedRect(baseX, baseY, baseX + chatWidth, baseY + bgH,
                cfg.chromaChatBorderRadius, cfg.chromaChatBackgroundColor, cfg.chromaChatBorderColor);

        // -- Messages --
        if (visibleCount > 0) {
            float opacity = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
            int updateCounter = mc.ingameGUI.getUpdateCounter();
            int y = baseY + 2;

            if (grouping && groupCache != null) {
                renderGrouped(cfg, scrollPos, visibleCount, baseX, chatWidth, lineH, y,
                        updateCounter, chatOpen, opacity, now, mouseClicked);
            } else {
                renderNormal(cfg, drawnLines, scrollPos, visibleCount, baseX, chatWidth, lineH, y,
                        updateCounter, chatOpen, opacity, now, mouseClicked);
            }
        }

        // -- Scroll indicator --
        if (isScrolled) {
            int ix = baseX + chatWidth - 3;
            int ih = Math.max(4, bgH * visibleCount / Math.max(totalLines, 1));
            int iy = baseY + bgH - bgH * scrollPos / Math.max(totalLines, 1);
            Gui.drawRect(ix, iy, ix + 2, iy + ih, 0x99FFFFFF | (0x88 << 24));
        }

        GlStateManager.popMatrix();
    }

    // =================================================================
    //  Render: Normal mode
    // =================================================================
    private void renderNormal(BetterPlayerHUDConfig cfg, List<ChatLine> lines, int scrollPos,
                              int vis, int baseX, int chatWidth, int lineH, int y,
                              int updateCounter, boolean chatOpen, float opacity, long now,
                              boolean mouseClicked) {
        int drawEnd = Math.min(scrollPos + vis, lines.size());
        for (int i = scrollPos; i < drawEnd; i++) {
            ChatLine cl = lines.get(i);
            if (cl == null) { y -= lineH; continue; }

            int age = updateCounter - cl.getUpdatedCounter();
            int alpha = calcAlpha(age, chatOpen, opacity);
            if (alpha <= 3) { y -= lineH; continue; }

            y += getMsgOffset(cl.getUpdatedCounter(), cfg);

            // P2: Hover
            if (i == hoveredLineAbsIdx) {
                drawHover(baseX + 1, y, baseX + chatWidth - 1, y + lineH, cfg);
                if (mouseClicked) forwardClick(cl);
            }

            String text = cl.getChatComponent().getFormattedText();
            mc.fontRendererObj.drawString(text, baseX + 2, y, 0xFFFFFF | (alpha << 24));
            y -= lineH;
        }
    }

    // =================================================================
    //  Render: Grouped mode (P3)
    // =================================================================
    private void renderGrouped(BetterPlayerHUDConfig cfg, int scrollPos,
                               int vis, int baseX, int chatWidth, int lineH, int y,
                               int updateCounter, boolean chatOpen, float opacity, long now,
                               boolean mouseClicked) {
        int drawn = 0;
        for (int gi = 0; gi < groupCache.length && drawn < vis; gi++) {
            if (gi < scrollPos) continue;

            GroupInfo g = groupCache[gi];
            if (gi > scrollPos) y -= lineH * (g.count - 1);

            ChatLine cl = g.line;
            if (cl == null) { y -= lineH; drawn++; continue; }

            int age = updateCounter - cl.getUpdatedCounter();
            int alpha = calcAlpha(age, chatOpen, opacity);
            if (alpha <= 3) { y -= lineH; drawn++; continue; }

            y += getMsgOffset(cl.getUpdatedCounter(), cfg);

            // P2: Hover
            if (gi == hoveredLineAbsIdx) {
                drawHover(baseX + 1, y, baseX + chatWidth - 1, y + lineH, cfg);
                if (mouseClicked) forwardClick(cl);
            }

            String text = cl.getChatComponent().getFormattedText();
            mc.fontRendererObj.drawString(text, baseX + 2, y, 0xFFFFFF | (alpha << 24));

            // P3: Badge
            if (g.count > 1) {
                String badge = "[" + g.count + "x]";
                int bw = mc.fontRendererObj.getStringWidth(badge);
                int bc = 0xAAFFFFFF | (Math.min(alpha + 40, 255) << 24);
                mc.fontRendererObj.drawString(badge, baseX + chatWidth - bw - 2, y, bc);
            }

            y -= lineH;
            drawn++;
        }
    }

    // =================================================================
    //  Click forward (P2)
    // =================================================================
    private void forwardClick(ChatLine cl) {
        IChatComponent comp = cl.getChatComponent();
        if (comp != null && comp.getChatStyle() != null
                && comp.getChatStyle().getChatClickEvent() != null) {
            String val = comp.getChatStyle().getChatClickEvent().getValue();
            if (val != null) {
                mc.thePlayer.sendChatMessage(val.startsWith("/") ? val : "/" + val);
            }
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
            double fade = (double) Math.max(0, 200 - age) / 200.0;
            fade = fade * 10.0;
            fade = MathHelper.clamp_double(fade, 0.0, 1.0);
            fade = fade * fade;
            alpha = (int) (255.0 * fade);
        }
        return (int) (alpha * opacity);
    }

    // =================================================================
    //  Message tracking (P1)
    // =================================================================
    private void trackNewMessages(List<ChatLine> lines, BetterPlayerHUDConfig cfg, long now) {
        if (lines == null || !cfg.chromaChatMsgAnimEnable) return;
        int cur = lines.size();
        if (cur > prevLineCount) {
            int diff = cur - prevLineCount;
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
    private void rebuildGroupCache(List<ChatLine> lines) {
        if (lines == null || lines.isEmpty()) { groupCache = null; return; }
        GroupInfo[] tmp = new GroupInfo[lines.size()];
        int out = 0;
        String prevText = null;
        ChatLine groupLine = null;
        int count = 0;

        for (ChatLine cl : lines) {
            String text = cl.getChatComponent().getFormattedText();
            if (text.equals(prevText) && groupLine != null) {
                count++;
                tmp[out - 1] = new GroupInfo(groupLine, count);
            } else {
                groupLine = cl;
                count = 1;
                prevText = text;
                tmp[out] = new GroupInfo(cl, 1);
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
    //  Rounded rect (batched)
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

        // 4 corners: 9 segments each, 1 draw per corner
        // TL
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
            double x1 = right - r + r * Math.cos(r1), y1 = bottom - r + r * Math.sin(r1);
            double x2 = right - r + r * Math.cos(r2), y2 = bottom - r + r * Math.sin(r2);
            wr.pos(right - r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x1, y1, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x2, y2, 0).color(fr, fg, fb, fa).endVertex();
        }
        tess.draw();

        // BL
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int a = 0; a < 90; a += 10) {
            double r1 = Math.toRadians(a), r2 = Math.toRadians(a + 10);
            double x1 = left + r - r * Math.cos(r1), y1 = bottom - r + r * Math.sin(r1);
            double x2 = left + r - r * Math.cos(r2), y2 = bottom - r + r * Math.sin(r2);
            wr.pos(left + r, bottom - r, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x2, y2, 0).color(fr, fg, fb, fa).endVertex();
            wr.pos(x1, y1, 0).color(fr, fg, fb, fa).endVertex();
        }
        tess.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // =================================================================
    //  Reflection helpers
    // =================================================================
    @SuppressWarnings("unchecked")
    private List<ChatLine> getDrawnLines() {
        try { return (List<ChatLine>) fieldDrawnChatLines.get(vanillaChat); }
        catch (Exception e) { return null; }
    }

    private int getScrollPos() {
        try { return fieldScrollPos.getInt(vanillaChat); }
        catch (Exception e) { return 0; }
    }

    private boolean getIsScrolled() {
        try { return fieldIsScrolled.getBoolean(vanillaChat); }
        catch (Exception e) { return false; }
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
            } catch (NoSuchFieldException e2) { return null; }
        }
    }
}
