package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * 命中/击杀标识渲染器（由 HitMarkerMod 移植）。
 *
 * 渲染四角线段，支持缩放、渐隐、边框描边、随机旋转。
 */
@SideOnly(Side.CLIENT)
public class HitMarkerRendererBHUD {

    private static long hitMarkerTime = 0;
    private static final long DURATION = 300;
    private static long killMarkerTime = 0;
    private static final long KILL_DURATION = 500;
    private static boolean isKillMarker = false;

    // 旋转角度：触发时定死，渲染时只读不重算
    private static float currentHitAngle = 0;
    private static float lastHitAngle = Float.NaN;

    public static void showHitMarker() {
        hitMarkerTime = System.currentTimeMillis();
        isKillMarker = false;
        currentHitAngle = generateRotateAngle(hitMarkerTime);
    }

    private static float generateRotateAngle(long seed) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.hitMarkerRandomRotate || cfg.hitMarkerRandomRotateStrength <= 0) return 0;
        Random rng = new Random(seed);
        float angle = (rng.nextBoolean() ? 1.0F : -1.0F) * (5.0F + rng.nextFloat() * 10.0F);
        // 保证连续两次角度不同
        if (angle == lastHitAngle) {
            angle = -angle;
            if (angle == 0.0F) angle = 5.0F;
        }
        lastHitAngle = angle;
        return angle;
    }

    public static void showKillMarker() {
        killMarkerTime = System.currentTimeMillis();
        isKillMarker = true;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            renderHitMarker();
        }
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            renderHitMarker();
        }
    }

    private void renderHitMarker() {
        if (!BetterPlayerHUD.config.enableHitMarker) return;

        long now = System.currentTimeMillis();
        long killElapsed = now - killMarkerTime;
        boolean showingKill = killElapsed < KILL_DURATION;
        long hitElapsed = now - hitMarkerTime;
        boolean showingHit = hitElapsed < DURATION && !showingKill;

        if (showingKill) {
            ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
            renderMarker(res.getScaledWidth() / 2, res.getScaledHeight() / 2,
                    killElapsed, KILL_DURATION, true, killMarkerTime);
        } else if (showingHit) {
            ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
            renderMarker(res.getScaledWidth() / 2, res.getScaledHeight() / 2,
                    hitElapsed, DURATION, false, hitMarkerTime);
        }
    }

    private void renderMarker(int cx, int cy, long elapsed, long duration, boolean isKill, long startTime) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;

        float lineLen = isKill ? cfg.killSize : cfg.hitSize;
        float thickness = lineLen * 0.35f;
        float gap = isKill ? lineLen * 0.35f : lineLen * 0.7f;
        float taper = lineLen * 0.25f;

        float configAlpha = isKill ? cfg.killAlpha : cfg.hitAlpha;
        float alpha = (1.0f - (float) elapsed / duration) * configAlpha;
        float translateDist = isKill ? lineLen * 0.3f : 0.0f;

        int hitPacked = cfg.hitColor;
        int killPacked = cfg.killColor;
        int color = isKill ? killPacked : hitPacked;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // ── 随机旋转（同一次命中角度固定，不同次随机；触发时已定死） ──
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0.0F);
        if (!isKill && currentHitAngle != 0) {
            GlStateManager.rotate(currentHitAngle, 0.0F, 0.0F, 1.0F);
        }
        GlStateManager.translate(-cx, -cy, 0.0F);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        float d = 0.7071f;

        // 边框
        if (cfg.hitMarkerEnableBorder && cfg.hitMarkerBorderWidth > 0) {
            int borderPacked = isKill ? cfg.hitMarkerKillBorderColor : cfg.hitMarkerBorderColor;
            float br = ((borderPacked >> 16) & 0xFF) / 255.0f;
            float bg = ((borderPacked >> 8) & 0xFF) / 255.0f;
            float bb = (borderPacked & 0xFF) / 255.0f;
            GlStateManager.color(br, bg, bb, alpha);
            GL11.glLineWidth(thickness + cfg.hitMarkerBorderWidth * 2);
            drawLine(cx, cy, d, -d, gap, lineLen, taper, translateDist);
            drawLine(cx, cy, d, d, gap, lineLen, taper, translateDist);
            drawLine(cx, cy, -d, d, gap, lineLen, taper, translateDist);
            drawLine(cx, cy, -d, -d, gap, lineLen, taper, translateDist);
        }

        // 主色
        GlStateManager.color(r, g, b, alpha);
        GL11.glLineWidth(thickness);
        drawLine(cx, cy, d, -d, gap, lineLen, taper, translateDist);
        drawLine(cx, cy, d, d, gap, lineLen, taper, translateDist);
        drawLine(cx, cy, -d, d, gap, lineLen, taper, translateDist);
        drawLine(cx, cy, -d, -d, gap, lineLen, taper, translateDist);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.popMatrix();
    }

    private void drawLine(float cx, float cy, float dx, float dy,
                          float gap, float len, float taper, float trans) {
        float ox = dx * trans, oy = dy * trans;
        float sx = cx + dx * gap + ox, sy = cy + dy * gap + oy;
        float ex = cx + dx * (gap + len) + ox, ey = cy + dy * (gap + len) + oy;
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(sx, sy);
        GL11.glVertex2f(sx + dx * (len - taper) * 0.5f, sy + dy * (len - taper) * 0.5f);
        GL11.glVertex2f(ex, ey);
        GL11.glEnd();
    }
}
