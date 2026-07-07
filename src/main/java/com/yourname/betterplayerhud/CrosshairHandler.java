package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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

    // ================================================================
    //  事件：拦截原版准星 + 绘制自定义准星
    // ================================================================

    @SubscribeEvent
    public void onPreCrosshair(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS
                && BetterPlayerHUD.config.enableCrosshair
                && shouldShow()) {
            // 在 Pre 中直接绘制自定义准星，然后取消原版
            ScaledResolution sr = new ScaledResolution(mc);
            renderCrosshair(sr.getScaledWidth(), sr.getScaledHeight());
            event.setCanceled(true);
        }
    }

    /** 判断当前是否应该显示准星 */
    private boolean shouldShow() {
        if (mc.thePlayer == null) return false;
        if (mc.gameSettings.hideGUI) return false;
        if (mc.gameSettings.thirdPersonView != 0 && !BetterPlayerHUD.config.crosshairShowInThirdPerson) return false;
        // 游戏内菜单等 — 原版也不显示
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

        // 计算扩散
        float spread = 0;
        if (cfg.crosshairSpread) {
            spread = calculateSpread();
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
                float charge = (72000 - useCount) / 20.0f;  // 0→1 满蓄力
                spread += (1 - Math.min(charge, 1)) * 8;
            }
        }

        return spread * cfg.crosshairSpreadAmount;
    }

    // ================================================================
    //  样式绘制
    // ================================================================

    /** 点 */
    private void drawDot(int cx, int cy, int color, int size) {
        int half = Math.max(1, size);
        GlStateManager.color(
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f
        );
        Gui.drawRect(cx - half, cy - half, cx + half + 1, cy + half + 1, color);
    }

    /** 十字+间隙（cross / cross_gap 共用） */
    private void drawCrossGap(int cx, int cy, int color, float spread, int gap, BetterPlayerHUDConfig cfg) {
        setGlColor(color);
        GL11.glLineWidth(cfg.crosshairThickness);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();

        int len = cfg.crosshairLength;
        float g = gap + spread;

        // 上
        if (cfg.crosshairArmUp) drawLine(t, wr, cx, cy - g, cx, cy - g - len);
        // 下
        if (cfg.crosshairArmDown) drawLine(t, wr, cx, cy + g, cx, cy + g + len);
        // 左
        if (cfg.crosshairArmLeft) drawLine(t, wr, cx - g, cy, cx - g - len, cy);
        // 右
        if (cfg.crosshairArmRight) drawLine(t, wr, cx + g, cy, cx + g + len, cy);

        // 描边（再画一遍，更大宽度，描边色）
        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);

            if (cfg.crosshairArmUp) drawLine(t, wr, cx, cy - g, cx, cy - g - len);
            if (cfg.crosshairArmDown) drawLine(t, wr, cx, cy + g, cx, cy + g + len);
            if (cfg.crosshairArmLeft) drawLine(t, wr, cx - g, cy, cx - g - len, cy);
            if (cfg.crosshairArmRight) drawLine(t, wr, cx + g, cy, cx + g + len, cy);

            // 恢复主色线条宽度
            setGlColor(color);
            GL11.glLineWidth(cfg.crosshairThickness);
            if (cfg.crosshairArmUp) drawLine(t, wr, cx, cy - g, cx, cy - g - len);
            if (cfg.crosshairArmDown) drawLine(t, wr, cx, cy + g, cx, cy + g + len);
            if (cfg.crosshairArmLeft) drawLine(t, wr, cx - g, cy, cx - g - len, cy);
            if (cfg.crosshairArmRight) drawLine(t, wr, cx + g, cy, cx + g + len, cy);
        }
    }

    /** CSGO 式十字（与 cross_gap 类似，但上臂默认较短且独立控制） */
    private void drawCSGO(int cx, int cy, int color, float spread, BetterPlayerHUDConfig cfg) {
        // CSGO 默认上臂关闭；但尊重 armUp 设置
        setGlColor(color);
        GL11.glLineWidth(cfg.crosshairThickness);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();

        int len = cfg.crosshairLength;
        float g = cfg.crosshairGap + spread;

        if (cfg.crosshairArmUp) drawLine(t, wr, cx, cy - g, cx, cy - g - (len / 2));
        if (cfg.crosshairArmDown) drawLine(t, wr, cx, cy + g, cx, cy + g + len);
        if (cfg.crosshairArmLeft) drawLine(t, wr, cx - g, cy, cx - g - len, cy);
        if (cfg.crosshairArmRight) drawLine(t, wr, cx + g, cy, cx + g + len, cy);

        // 描边
        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);

            if (cfg.crosshairArmUp) drawLine(t, wr, cx, cy - g, cx, cy - g - (len / 2));
            if (cfg.crosshairArmDown) drawLine(t, wr, cx, cy + g, cx, cy + g + len);
            if (cfg.crosshairArmLeft) drawLine(t, wr, cx - g, cy, cx - g - len, cy);
            if (cfg.crosshairArmRight) drawLine(t, wr, cx + g, cy, cx + g + len, cy);

            setGlColor(color);
            GL11.glLineWidth(cfg.crosshairThickness);
            if (cfg.crosshairArmUp) drawLine(t, wr, cx, cy - g, cx, cy - g - (len / 2));
            if (cfg.crosshairArmDown) drawLine(t, wr, cx, cy + g, cx, cy + g + len);
            if (cfg.crosshairArmLeft) drawLine(t, wr, cx - g, cy, cx - g - len, cy);
            if (cfg.crosshairArmRight) drawLine(t, wr, cx + g, cy, cx + g + len, cy);
        }
    }

    /** 圆形 */
    private void drawCircle(int cx, int cy, int color, float spread, BetterPlayerHUDConfig cfg) {
        setGlColor(color);
        int segments = Math.max(8, cfg.crosshairCircleSegments);
        float radius = cfg.crosshairCircleRadius + spread;

        GL11.glLineWidth(cfg.crosshairThickness);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);

        for (int i = 0; i < segments; i++) {
            double angle = TWO_PI * i / segments;
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }
        t.draw();

        // 描边
        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);

            wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            for (int i = 0; i < segments; i++) {
                double angle = TWO_PI * i / segments;
                wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
            }
            t.draw();

            setGlColor(color);
            GL11.glLineWidth(cfg.crosshairThickness);
            wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            for (int i = 0; i < segments; i++) {
                double angle = TWO_PI * i / segments;
                wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
            }
            t.draw();
        }
    }

    /** 菱形 */
    private void drawDiamond(int cx, int cy, int color, float spread, BetterPlayerHUDConfig cfg) {
        setGlColor(color);
        int len = cfg.crosshairLength + (int) spread;
        if (len < 2) len = 2;

        GL11.glLineWidth(cfg.crosshairThickness);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy - len, 0).endVertex();   // 上
        wr.pos(cx + len, cy, 0).endVertex();    // 右
        wr.pos(cx, cy + len, 0).endVertex();    // 下
        wr.pos(cx - len, cy, 0).endVertex();    // 左
        t.draw();

        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);
            wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            wr.pos(cx, cy - len, 0).endVertex();
            wr.pos(cx + len, cy, 0).endVertex();
            wr.pos(cx, cy + len, 0).endVertex();
            wr.pos(cx - len, cy, 0).endVertex();
            t.draw();

            setGlColor(color);
            GL11.glLineWidth(cfg.crosshairThickness);
            wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            wr.pos(cx, cy - len, 0).endVertex();
            wr.pos(cx + len, cy, 0).endVertex();
            wr.pos(cx, cy + len, 0).endVertex();
            wr.pos(cx - len, cy, 0).endVertex();
            t.draw();
        }
    }

    /** 三角形（正三角，指向正上方） */
    private void drawTriangle(int cx, int cy, int color, float spread, BetterPlayerHUDConfig cfg) {
        setGlColor(color);
        int len = cfg.crosshairLength + (int) spread;
        if (len < 2) len = 2;

        // 等边三角形：从顶部顺时针
        float topY = cy - len;
        float bottomY = cy + len * 0.5f;
        float halfBase = (float) (len * Math.sqrt(3) / 2);

        GL11.glLineWidth(cfg.crosshairThickness);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        wr.pos(cx, topY, 0).endVertex();                    // 顶
        wr.pos(cx + halfBase, bottomY, 0).endVertex();       // 右下
        wr.pos(cx - halfBase, bottomY, 0).endVertex();       // 左下
        t.draw();

        if (cfg.crosshairOutline) {
            setGlColor(cfg.crosshairOutlineColor);
            GL11.glLineWidth(cfg.crosshairThickness + cfg.crosshairOutlineWidth * 2);
            wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            wr.pos(cx, topY, 0).endVertex();
            wr.pos(cx + halfBase, bottomY, 0).endVertex();
            wr.pos(cx - halfBase, bottomY, 0).endVertex();
            t.draw();

            setGlColor(color);
            GL11.glLineWidth(cfg.crosshairThickness);
            wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            wr.pos(cx, topY, 0).endVertex();
            wr.pos(cx + halfBase, bottomY, 0).endVertex();
            wr.pos(cx - halfBase, bottomY, 0).endVertex();
            t.draw();
        }
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

    /** 画一条简单线段（2D 坐标，使用 Tessellator GL_LINES） */
    private static void drawLine(Tessellator t, WorldRenderer wr, float x1, float y1, float x2, float y2) {
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(x1, y1, 0).endVertex();
        wr.pos(x2, y2, 0).endVertex();
        t.draw();
    }
}
