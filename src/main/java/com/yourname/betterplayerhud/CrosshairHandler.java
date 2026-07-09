package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemBow;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * 模块20：自定义准星 (Crosshair)
 *
 * 7种样式：dot / cross / cross_gap / csgo / circle / diamond / triangle
 * 支持：RGB 流光、动态扩散、臂独立开关、旋转、描边、偏移
 */
@SideOnly(Side.CLIENT)
public class CrosshairHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final double TWO_PI = Math.PI * 2;
    private static final float SPREAD_SMOOTHING = 0.2f; // 插值系数，越高响应越快

    private static float smoothSpread = 0;

    // ================================================================
    //  事件：拦截原版准星 + 绘制自定义准星
    // ================================================================

    @SubscribeEvent
    public void cancelOriginalCrosshair(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS
                && BetterPlayerHUD.config.enableCrosshair) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void drawCustomCrosshair(RenderGameOverlayEvent event) {
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT
                && BetterPlayerHUD.config.enableCrosshair
                && shouldShow()) {
            ScaledResolution sr = new ScaledResolution(mc);
            renderCrosshair(sr.getScaledWidth(), sr.getScaledHeight());
        }
    }

    /** 判断当前是否应该显示准星 */
    private boolean shouldShow() {
        if (mc.thePlayer == null) return false;
        if (mc.gameSettings.hideGUI) return false;
        if (mc.gameSettings.thirdPersonView != 0 && !BetterPlayerHUD.config.crosshairShowInThirdPerson) return false;
        if (mc.currentScreen != null) return false;
        return true;
    }

    // ================================================================
    //  入口渲染
    // ================================================================

    private void renderCrosshair(int screenWidth, int screenHeight) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;

        int cx = screenWidth / 2 + cfg.crosshairXOffset;
        int cy = screenHeight / 2 + cfg.crosshairYOffset;

        // 获取颜色
        int color = cfg.crosshairColor;
        if (cfg.crosshairRGB) {
            long now = System.currentTimeMillis();
            color = RGBFlowColor.getUniformColorByConfig(now, cfg.rgbSpeed, cfg.rgbStepMs, cfg.rgbColorAlgo);
        }

        // 计算扩散（带平滑插值，防止跳变）
        float spread;
        if (cfg.crosshairSpread) {
            float target = calculateSpread();
            smoothSpread += (target - smoothSpread) * SPREAD_SMOOTHING;
            spread = smoothSpread;
        } else {
            smoothSpread = 0;
            spread = 0;
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_FLAT);

        // 平移至准星中心
        GlStateManager.translate(cx, cy, 0);

        // 旋转（部分样式不受旋转影响：dot, circle）
        String style = cfg.crosshairStyle;
        boolean rotatable = !"dot".equals(style) && !"circle".equals(style);
        if (rotatable && cfg.crosshairRotation != 0) {
            GlStateManager.rotate(cfg.crosshairRotation, 0, 0, 1);
        }

        // 绘制内容（坐标相对于 0,0）
        switch (style) {
            case "dot":
                drawDot(0, 0, color, cfg.crosshairDotSize);
                break;
            case "cross":
                drawCrossGap(0, 0, color, spread, 0, cfg);
                break;
            case "cross_gap":
                drawCrossGap(0, 0, color, spread, cfg.crosshairGap, cfg);
                break;
            case "csgo":
                drawCSGO(0, 0, color, spread, cfg);
                break;
            case "circle":
                drawCircle(0, 0, color, spread, cfg);
                break;
            case "diamond":
                drawDiamond(0, 0, color, spread, cfg);
                break;
            case "triangle":
                drawTriangle(0, 0, color, spread, cfg);
                break;
        }

        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // ================================================================
    //  扩散计算
    // ================================================================

    private float calculateSpread() {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        float spread = 0;

        if (cfg.crosshairSpreadWalk) {
            boolean moving = mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
            if (moving) {
                spread += 4;
                if (mc.thePlayer.isSprinting()) spread += 2;
            }
        }

        if (cfg.crosshairSpreadJump && !mc.thePlayer.onGround) {
            spread += 6;
        }

        if (cfg.crosshairSpreadBow && mc.thePlayer.isUsingItem()) {
            if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
                int useCount = mc.thePlayer.getItemInUseCount();
                float charge = (72000 - useCount) / 20.0f;
                if (cfg.crosshairSpreadBowInverted) {
                    spread += Math.min(charge, 1) * 8;
                } else {
                    spread += (1 - Math.min(charge, 1)) * 8;
                }
            }
        }

        return spread * cfg.crosshairSpreadAmount;
    }

    // ================================================================
    //  样式绘制（全部改用 GL11 immediate mode）
    // ================================================================

    /** 点 */
    private void drawDot(int cx, int cy, int color, int size) {
        GlStateManager.color(
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f
        );
        Gui.drawRect(cx - size, cy - size, cx + size + 1, cy + size + 1, color);
    }

    /** 十字+间隙（cross / cross_gap 共用）— 立即模式 */
    private void drawCrossGap(int cx, int cy, int color, float spread, int gap, BetterPlayerHUDConfig cfg) {
        int len = cfg.crosshairLength;
        float g = gap + spread;

        // 描边（先画，更大宽度）
        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);
            if (cfg.crosshairArmUp) drawLineGL(cx, cy - g, cx, cy - g - len);
            if (cfg.crosshairArmDown) drawLineGL(cx, cy + g, cx, cy + g + len);
            if (cfg.crosshairArmLeft) drawLineGL(cx - g, cy, cx - g - len, cy);
            if (cfg.crosshairArmRight) drawLineGL(cx + g, cy, cx + g + len, cy);
        }

        // 主色
        setGlColor(color);
        GL11.glLineWidth(cfg.crosshairThickness);
        if (cfg.crosshairArmUp) drawLineGL(cx, cy - g, cx, cy - g - len);
        if (cfg.crosshairArmDown) drawLineGL(cx, cy + g, cx, cy + g + len);
        if (cfg.crosshairArmLeft) drawLineGL(cx - g, cy, cx - g - len, cy);
        if (cfg.crosshairArmRight) drawLineGL(cx + g, cy, cx + g + len, cy);
    }

    /** CSGO 式十字 — 立即模式 */
    private void drawCSGO(int cx, int cy, int color, float spread, BetterPlayerHUDConfig cfg) {
        int len = cfg.crosshairLength;
        float g = cfg.crosshairGap + spread;

        // 描边
        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);
            if (cfg.crosshairArmUp) drawLineGL(cx, cy - g, cx, cy - g - (len / 2));
            if (cfg.crosshairArmDown) drawLineGL(cx, cy + g, cx, cy + g + len);
            if (cfg.crosshairArmLeft) drawLineGL(cx - g, cy, cx - g - len, cy);
            if (cfg.crosshairArmRight) drawLineGL(cx + g, cy, cx + g + len, cy);
        }

        // 主色
        setGlColor(color);
        GL11.glLineWidth(cfg.crosshairThickness);
        if (cfg.crosshairArmUp) drawLineGL(cx, cy - g, cx, cy - g - (len / 2));
        if (cfg.crosshairArmDown) drawLineGL(cx, cy + g, cx, cy + g + len);
        if (cfg.crosshairArmLeft) drawLineGL(cx - g, cy, cx - g - len, cy);
        if (cfg.crosshairArmRight) drawLineGL(cx + g, cy, cx + g + len, cy);
    }

    /** 圆形 — immediate mode GL_LINE_LOOP */
    private void drawCircle(int cx, int cy, int color, float spread, BetterPlayerHUDConfig cfg) {
        int segments = Math.max(8, cfg.crosshairCircleSegments);
        float radius = cfg.crosshairCircleRadius + spread;

        // 描边
        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);
            drawCircleLoop(cx, cy, radius, segments);
        }

        // 主色
        setGlColor(color);
        GL11.glLineWidth(cfg.crosshairThickness);
        drawCircleLoop(cx, cy, radius, segments);
    }

    private static void drawCircleLoop(int cx, int cy, float radius, int segments) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            double angle = TWO_PI * i / segments;
            GL11.glVertex2f(cx + (float) (Math.cos(angle) * radius),
                    cy + (float) (Math.sin(angle) * radius));
        }
        GL11.glEnd();
    }

    /** 菱形 — immediate mode */
    private void drawDiamond(int cx, int cy, int color, float spread, BetterPlayerHUDConfig cfg) {
        int len = cfg.crosshairLength + (int) spread;
        if (len < 2) len = 2;

        // 描边
        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);
            drawDiamondVerts(cx, cy, len);
        }

        // 主色
        setGlColor(color);
        GL11.glLineWidth(cfg.crosshairThickness);
        drawDiamondVerts(cx, cy, len);
    }

    private static void drawDiamondVerts(int cx, int cy, int len) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(cx, cy - len);
        GL11.glVertex2f(cx + len, cy);
        GL11.glVertex2f(cx, cy + len);
        GL11.glVertex2f(cx - len, cy);
        GL11.glEnd();
    }

    /** 三角形（正三角，指向正上方）— immediate mode */
    private void drawTriangle(int cx, int cy, int color, float spread, BetterPlayerHUDConfig cfg) {
        int len = cfg.crosshairLength + (int) spread;
        if (len < 2) len = 2;

        float topY = cy - len;
        float bottomY = cy + len * 0.5f;
        float halfBase = (float) (len * Math.sqrt(3) / 2);

        // 描边
        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);
            drawTriangleVerts(cx, topY, bottomY, halfBase);
        }

        // 主色
        setGlColor(color);
        GL11.glLineWidth(cfg.crosshairThickness);
        drawTriangleVerts(cx, topY, bottomY, halfBase);
    }

    private static void drawTriangleVerts(int cx, float topY, float bottomY, float halfBase) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(cx, topY);
        GL11.glVertex2f(cx + halfBase, bottomY);
        GL11.glVertex2f(cx - halfBase, bottomY);
        GL11.glEnd();
    }

    // ================================================================
    //  工具方法
    // ================================================================

    /** 设置 GL 颜色 (ARGB) */
    private static void setGlColor(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        GlStateManager.color(r, g, b, a);
    }

    /** 画一条简单线段（GL11 immediate mode） */
    private static void drawLineGL(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
    }
}
