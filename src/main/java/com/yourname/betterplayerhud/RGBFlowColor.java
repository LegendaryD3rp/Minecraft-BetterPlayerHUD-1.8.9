package com.yourname.betterplayerhud;

import java.awt.Color;

/**
 * RGB 动态流光颜色工具。
 * 提供两种模式：
 * 1. 整体变色（getColor）—— 整条描边同时同色
 * 2. 位置流光（getFlowColor）—— 沿包围盒周界渐变，颜色沿边缘流动
 */
public class RGBFlowColor {

    /**
     * 获取当前时刻的流动色（整体同一色）。
     *
     * @param speed 色相变化速度（毫秒/完整周期），值越小变化越快。
     *              推荐范围 30～200，默认 80。
     * @return ARGB 颜色值（0xFF000000 ~ 0xFFFFFFFF）
     */
    public static int getColor(long speed) {
        long time = System.currentTimeMillis();
        float hue = (time % speed) / (float) speed;
        return 0xFF000000 | (0xFFFFFF & Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }

    /**
     * 使用默认速度（80ms）获取整体变色。
     */
    public static int getColor() {
        return getColor(80L);
    }

    /**
     * 获取沿周界流动的流光色。
     * 色相 = (时间偏移 + 周界位置偏移) % 1.0
     * 任一瞬间，不同位置的色相不同，形成彩虹沿边缘流动的效果。
     *
     * @param perimeterPosition 该点距周界起点的距离
     * @param totalPerimeter    周界总长度
     * @param speed             流动速度（毫秒/完整周期），值越小变化越快
     * @return ARGB 颜色值（0xFF000000 ~ 0xFFFFFFFF）
     */
    public static int getFlowColor(float perimeterPosition, float totalPerimeter, long speed) {
        if (totalPerimeter <= 0) totalPerimeter = 1;
        long time = System.currentTimeMillis();
        float hue = ((time % speed) / (float) speed + perimeterPosition / totalPerimeter) % 1.0f;
        return 0xFF000000 | (0xFFFFFF & Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }
}
