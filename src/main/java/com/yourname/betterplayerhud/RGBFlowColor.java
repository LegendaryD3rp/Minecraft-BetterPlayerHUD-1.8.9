package com.yourname.betterplayerhud;

import java.awt.Color;

/**
 * RGB 动态流光颜色工具。
 *
 * 支持两种颜色算法：
 * - HSBtoRGB（Java标准，纯色感强）：hue 0→1 穿越红橙黄绿青蓝紫
 * - 正弦波三通道（BetterBlockOutline 方案，颜色柔和）：三通道正弦波相差120°
 *
 * 支持两种流动模式：
 * - perimeter：沿包围盒周界逐棱流动
 * - uniform：整框同一颜色随时间渐变
 */
public class RGBFlowColor {

    /**
     * 获取当前时刻的流动色（整体同一色，HSBtoRGB）。
     */
    public static int getColor(long speed) {
        long time = System.currentTimeMillis();
        float hue = (time % speed) / (float) speed;
        return 0xFF000000 | (0xFFFFFF & Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }

    public static int getColor() {
        return getColor(500L);
    }

    // ================================================================
    //  HSBtoRGB 算法
    // ================================================================

    /**
     * HSBtoRGB：沿周界流动，指定时刻。
     */
    public static int getFlowColorAtTime(long timeMs, float perimeterMidPos, float totalPerimeter, long speed, long stepMs) {
        if (totalPerimeter <= 0) totalPerimeter = 1;
        float hue;
        if (stepMs <= 0) {
            hue = ((timeMs % speed) / (float) speed + perimeterMidPos / totalPerimeter) % 1.0f;
        } else {
            long quantized = (timeMs / stepMs) * stepMs;
            hue = ((quantized % speed) / (float) speed + perimeterMidPos / totalPerimeter) % 1.0f;
        }
        return 0xFF000000 | (0xFFFFFF & Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }

    /**
     * HSBtoRGB：整框同色，指定时刻。
     */
    public static int getUniformColorAtTime(long timeMs, long speed, long stepMs) {
        float hue;
        if (stepMs <= 0) {
            hue = (timeMs % speed) / (float) speed;
        } else {
            long quantized = (timeMs / stepMs) * stepMs;
            hue = (quantized % speed) / (float) speed;
        }
        return 0xFF000000 | (0xFFFFFF & Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }

    // ================================================================
    //  正弦波三通道算法
    // ================================================================

    /**
     * 正弦波彩虹：生成单色值（0~255 范围）。
     * 参考 BetterBlockOutline 的 ColorUtil.getRainbow。
     * 每个通道使用 sin(pos + offset)，三通道相差 120°，
     * 产生更柔和、不刺眼的彩虹效果。
     */
    public static int getSineWaveColor(double percent) {
        double pos = percent * Math.PI * 2;
        double offSet = Math.PI * 2 / 3;
        int r = clamp((int) (Math.sin(pos) * 127 + 128));
        int g = clamp((int) (Math.sin(pos + offSet) * 127 + 128));
        int b = clamp((int) (Math.sin(pos + offSet * 2) * 127 + 128));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * 正弦波：沿周界流动，指定时刻。
     */
    public static int getSineFlowColorAtTime(long timeMs, float perimeterMidPos, float totalPerimeter, long speed, long stepMs) {
        if (totalPerimeter <= 0) totalPerimeter = 1;
        double percent;
        if (stepMs <= 0) {
            percent = ((timeMs % speed) / (double) speed + perimeterMidPos / totalPerimeter) % 1.0;
        } else {
            long quantized = (timeMs / stepMs) * stepMs;
            percent = ((quantized % speed) / (double) speed + perimeterMidPos / totalPerimeter) % 1.0;
        }
        return getSineWaveColor(percent);
    }

    /**
     * 正弦波：整框同色，指定时刻。
     */
    public static int getSineUniformColorAtTime(long timeMs, long speed, long stepMs) {
        double percent;
        if (stepMs <= 0) {
            percent = (timeMs % speed) / (double) speed;
        } else {
            long quantized = (timeMs / stepMs) * stepMs;
            percent = (quantized % speed) / (double) speed;
        }
        return getSineWaveColor(percent);
    }

    // ================================================================
    //  配色工场（根据配置自动分派）
    // ================================================================

    /**
     * 按配置获取沿周界流动的流光色（指定时刻）。
     */
    public static int getFlowColorByConfig(long timeMs, float perimeterMidPos, float totalPerimeter,
                                           long speed, long stepMs, String algo) {
        if ("sinewave".equals(algo)) {
            return getSineFlowColorAtTime(timeMs, perimeterMidPos, totalPerimeter, speed, stepMs);
        }
        return getFlowColorAtTime(timeMs, perimeterMidPos, totalPerimeter, speed, stepMs);
    }

    /**
     * 按配置获取整框同色（指定时刻）。
     */
    public static int getUniformColorByConfig(long timeMs, long speed, long stepMs, String algo) {
        if ("sinewave".equals(algo)) {
            return getSineUniformColorAtTime(timeMs, speed, stepMs);
        }
        return getUniformColorAtTime(timeMs, speed, stepMs);
    }

    // ================================================================
    //  旧版接口（兼容）
    // ================================================================

    /**
     * @deprecated 改用 getFlowColorAtTime
     */
    @Deprecated
    public static int getFlowColor(float perimeterMidPos, float totalPerimeter, long speed, long stepMs) {
        return getFlowColorAtTime(System.currentTimeMillis(), perimeterMidPos, totalPerimeter, speed, stepMs);
    }

    /**
     * @deprecated 改用 getFlowColorAtTime
     */
    @Deprecated
    public static int getFlowColor(float perimeterMidPos, float totalPerimeter, long speed) {
        return getFlowColorAtTime(System.currentTimeMillis(), perimeterMidPos, totalPerimeter, speed, 0);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
