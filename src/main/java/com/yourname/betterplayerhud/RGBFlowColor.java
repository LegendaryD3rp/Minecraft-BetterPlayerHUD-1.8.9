package com.yourname.betterplayerhud;

import java.awt.Color;

/**
 * RGB 动态流光颜色工具。
 *
 * 核心逻辑：颜色沿包围盒周界连续流动。
 * 每条棱的中点色相 = (时间偏移 + 棱中点位置 / 总周界长度) % 1.0
 * 每一刻每条棱上的颜色不同，整条彩虹沿棱平滑旋转。
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
     * 获取沿周界流动的流光色。
     *
     * 每条棱的中点色相 = (时间偏移 + 棱中点位置/总周界) % 1.0
     *
     * @param perimeterMidPos  该棱中点距周界起点的距离
     * @param totalPerimeter   周界总长度
     * @param speed            完整色相周期（ms），值越小流动越快
     * @param stepMs           步进间隔（ms），0=平滑，>0=每stepMs跳变一次
     * @return ARGB 颜色值（0xFF000000 ~ 0xFFFFFFFF）
     */
    public static int getFlowColor(float perimeterMidPos, float totalPerimeter, long speed, long stepMs) {
        if (totalPerimeter <= 0) totalPerimeter = 1;
        long time = System.currentTimeMillis();

        float hue;
        if (stepMs <= 0) {
            // 平滑流动
            hue = ((time % speed) / (float) speed + perimeterMidPos / totalPerimeter) % 1.0f;
        } else {
            // 步进流动（时间量子化）
            long quantized = (time / stepMs) * stepMs;
            hue = ((quantized % speed) / (float) speed + perimeterMidPos / totalPerimeter) % 1.0f;
        }

        return 0xFF000000 | (0xFFFFFF & Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }

    /**
     * 获取沿周界流动的流光色（平滑模式）。
     */
    public static int getFlowColor(float perimeterMidPos, float totalPerimeter, long speed) {
        return getFlowColor(perimeterMidPos, totalPerimeter, speed, 0);
    }
}
