package com.yourname.betterplayerhud;

import java.awt.Color;

/**
 * RGB 动态流光颜色工具。
 *
 * 两种模式：
 * 1. 平滑流动（stepMs <= 0）：色相随时间连续变化，每条棱的色相 = (时间偏移 + 棱索引/总棱数) % 1.0
 * 2. 步进流动（stepMs > 0）：时间按 stepMs 量子化，每隔 stepMs 毫秒整圈跳变一次
 *
 * 关键：每条棱独立一色（棱），而非沿周界渐变（点）。
 */
public class RGBFlowColor {

    /**
     * 获取当前时刻的流动色（整体同一色）。
     */
    public static int getColor(long speed) {
        long time = System.currentTimeMillis();
        float hue = (time % speed) / (float) speed;
        return 0xFF000000 | (0xFFFFFF & Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }

    public static int getColor() {
        return getColor(80L);
    }

    /**
     * 获取基于棱索引的流动色（步进/平滑）。
     *
     * 每条棱独占一色（棱），色相 = (时间偏移 + 棱索引/总棱数) % 1.0。
     * 步进模式：每隔 stepMs 毫秒所有棱的颜色同步跳变一次。
     *
     * @param edgeIndex 棱索引（0 ~ totalEdges-1）
     * @param totalEdges 总棱数（通常为12）
     * @param speed 完整色相周期（ms），值越小流动越快
     * @param stepMs 步进间隔（ms），0=平滑流动
     * @return ARGB 颜色值
     */
    public static int getFlowColor(int edgeIndex, int totalEdges, long speed, long stepMs) {
        if (totalEdges <= 0) totalEdges = 1;
        long time = System.currentTimeMillis();
        float hue;

        if (stepMs <= 0) {
            // 平滑流动：时间连续
            hue = ((time % speed) / (float) speed + (float) edgeIndex / totalEdges) % 1.0f;
        } else {
            // 步进流动：时间量子化
            long quantizedTime = (time / stepMs) * stepMs;
            hue = ((quantizedTime % speed) / (float) speed + (float) edgeIndex / totalEdges) % 1.0f;
        }

        return 0xFF000000 | (0xFFFFFF & Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }
}
