package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * 完全重写的低血量警告处理器
 * 使用真正的椭圆渐变效果，修复血量判断逻辑
 */
public class LowHealthWarningHandler {
    private final Minecraft mc = Minecraft.getMinecraft();

    // 当前效果强度
    private float currentIntensity = 0.0f;
    // 平滑过渡
    private float smoothIntensity = 0.0f;

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.gameSettings.hideGUI) {
            smoothIntensity = 0.0f;
            currentIntensity = 0.0f;
            return;
        }

        // 检查是否启用
        if (!BetterPlayerHUD.config.enableLowHealthWarning) {
            smoothIntensity = 0.0f;
            currentIntensity = 0.0f;
            return;
        }

        // 获取玩家当前生命值 - 与PlayerHUDHandler一致
        float health = mc.thePlayer.getHealth();
        float maxHealth = mc.thePlayer.getMaxHealth();

        // 阈值：6点生命值 = 3颗心
        float threshold = 6.0f; // 固定为6点，而不是百分比

        // 只在血量低于等于6点（3颗心）时才有效果
        if (health > threshold) {
            // 血量高于阈值，目标强度为0
            currentIntensity = 0.0f;
        } else {
            // 血量低于等于阈值，计算强度
            if (health <= 2.0f) {
                // 血量<=2点，最强效果
                currentIntensity = 1.0f;
            } else {
                // 血量在2-6点之间，线性计算
                currentIntensity = 1.0f - ((health - 2.0f) / 4.0f);
            }

            // 应用配置中的强度系数
            currentIntensity *= BetterPlayerHUD.config.darkenIntensity;
        }

        // 平滑过渡
        smoothIntensity = smoothIntensity + (currentIntensity - smoothIntensity) * 0.2f;

        // 如果强度足够小，跳过渲染
        if (smoothIntensity < 0.01f) {
            return;
        }

        // 渲染真正的椭圆渐变效果
        renderTrueEllipticalVignette();
    }

    /**
     * 渲染真正的椭圆晕影效果
     */
    private void renderTrueEllipticalVignette() {
        // 获取屏幕尺寸
        ScaledResolution scaled = new ScaledResolution(mc);
        int width = scaled.getScaledWidth();
        int height = scaled.getScaledHeight();

        // 保存OpenGL状态
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 获取颜色配置
        int color = BetterPlayerHUD.config.darkenColor;
        float alpha = (float) ((color >> 24) & 0xFF) / 255.0F;
        float red = (float) ((color >> 16) & 0xFF) / 255.0F;
        float green = (float) ((color >> 8) & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;

        // 应用当前强度
        alpha *= smoothIntensity;

        // 使用暗色调
        red = red * 0.1f;
        green = green * 0.1f;
        blue = blue * 0.1f;

        // 创建真正的椭圆渐变效果
        drawEllipticalGradient(width, height, red, green, blue, alpha);

        // 恢复OpenGL状态
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    /**
     * 绘制椭圆渐变效果
     * 使用三角形网格在屏幕空间中绘制渐变
     */
    private void drawEllipticalGradient(int width, int height,
                                        float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        // 中心点
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        // 椭圆大小：以屏幕对角线为直径
        float diagonal = (float) Math.sqrt(width * width + height * height);
        float ellipseRadiusX = diagonal * 0.3f; // 水平半径
        float ellipseRadiusY = ellipseRadiusX * (height / (float)width); // 垂直半径

        // 应用配置中的椭圆大小
        float vignetteMultiplier = BetterPlayerHUD.config.vignetteSize;
        ellipseRadiusX *= vignetteMultiplier;
        ellipseRadiusY *= vignetteMultiplier;

        // 网格细分
        int gridX = 20;
        int gridY = 15;
        float stepX = (float) width / gridX;
        float stepY = (float) height / gridY;

        worldRenderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < gridX; i++) {
            for (int j = 0; j < gridY; j++) {
                // 当前网格的四个顶点
                float x1 = i * stepX;
                float y1 = j * stepY;
                float x2 = (i + 1) * stepX;
                float y2 = (j + 1) * stepY;

                // 绘制两个三角形组成矩形
                addEllipticalGradientTriangle(worldRenderer, x1, y1, x2, y1, x1, y2,
                        centerX, centerY, ellipseRadiusX, ellipseRadiusY,
                        r, g, b, a);
                addEllipticalGradientTriangle(worldRenderer, x2, y1, x2, y2, x1, y2,
                        centerX, centerY, ellipseRadiusX, ellipseRadiusY,
                        r, g, b, a);
            }
        }

        tessellator.draw();
    }

    /**
     * 添加椭圆渐变三角形
     */
    private void addEllipticalGradientTriangle(WorldRenderer worldRenderer,
                                               float x1, float y1,
                                               float x2, float y2,
                                               float x3, float y3,
                                               float centerX, float centerY,
                                               float radiusX, float radiusY,
                                               float r, float g, float b, float a) {
        // 计算每个顶点到椭圆中心的距离因子
        float dist1 = getEllipticalDistance(x1, y1, centerX, centerY, radiusX, radiusY);
        float dist2 = getEllipticalDistance(x2, y2, centerX, centerY, radiusX, radiusY);
        float dist3 = getEllipticalDistance(x3, y3, centerX, centerY, radiusX, radiusY);

        // 根据距离计算透明度：椭圆中心透明度为0，边缘透明度为1
        float alpha1 = a * dist1;
        float alpha2 = a * dist2;
        float alpha3 = a * dist3;

        // 添加顶点
        worldRenderer.pos(x1, y1, 0.0)
                .color(r, g, b, alpha1)
                .endVertex();
        worldRenderer.pos(x2, y2, 0.0)
                .color(r, g, b, alpha2)
                .endVertex();
        worldRenderer.pos(x3, y3, 0.0)
                .color(r, g, b, alpha3)
                .endVertex();
    }

    /**
     * 计算点到椭圆中心的距离因子
     * 返回0.0（在椭圆中心）到1.0+（在椭圆外部）
     */
    private float getEllipticalDistance(float x, float y,
                                        float centerX, float centerY,
                                        float radiusX, float radiusY) {
        // 计算归一化的椭圆坐标
        float dx = (x - centerX) / radiusX;
        float dy = (y - centerY) / radiusY;

        // 椭圆方程：dx² + dy² = 1
        float distanceSq = dx * dx + dy * dy;

        // 使用平滑的渐变函数
        if (distanceSq <= 1.0f) {
            // 椭圆内部：二次曲线渐变
            return distanceSq * distanceSq;
        } else {
            // 椭圆外部：完全变暗
            return 1.0f;
        }
    }
}