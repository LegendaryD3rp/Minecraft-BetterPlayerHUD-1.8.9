package com.yourname.betterplayerhud;

import java.awt.Color;

/**
 * RGB 动态流光颜色工具。
 * 基于系统时间做 HSV 色相循环，生成 0xFFFFFFFF 格式的 ARGB 颜色值。
 * 调用 getColor(speed) 即可获得当前时刻的流动色。
 */
public class RGBFlowColor {

    /**
     * 获取当前时刻的流动色。
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
     * 使用默认速度（80ms）获取流动色。
     */
    public static int getColor() {
        return getColor(80L);
    }
}
