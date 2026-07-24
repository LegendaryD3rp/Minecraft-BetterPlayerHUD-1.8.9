package com.yourname.betterplayerhud;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * 独立颜色选择器。
 * RGB 三滑块 + 实时预览 + 预设色板 + 十六进制显示。
 */
public class GuiColorPicker extends GuiScreen {

    private final GuiScreen parent;
    private final IntSupplier getter;
    private final Consumer<Integer> setter;

    private int r, g, b, a;
    private boolean draggingR, draggingG, draggingB;

    // 预设色板（16 色）
    private static final int[] PRESETS = {
        0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55,
        0xFF55FF55, 0xFF55FFFF, 0xFF5555FF, 0xFFFF55FF,
        0xFFFFAAAA, 0xFFFFDDBB, 0xFFAAAAAA, 0xFF888888,
        0xFF555555, 0xFF00AA00, 0xFF0000AA, 0xFF000000
    };

    public GuiColorPicker(GuiScreen parent, IntSupplier getter, Consumer<Integer> setter) {
        this.parent = parent;
        this.getter = getter;
        this.setter = setter;
        int packed = getter.getAsInt();
        this.a = (packed >> 24) & 0xFF;
        this.r = (packed >> 16) & 0xFF;
        this.g = (packed >> 8) & 0xFF;
        this.b = packed & 0xFF;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xCC101010);

        int cx = width / 2;
        int cy = height / 2;

        // ── 面板 ──
        int pw = 320, ph = 260;
        int px = cx - pw / 2, py = cy - ph / 2;
        drawRect(px, py, px + pw, py + ph, 0xF0FFFFFF);
        drawRect(px, py, px + pw, py + 1, 0xFF4A90D9); // 顶边高亮

        // ── 标题 ──
        mc.fontRendererObj.drawString("颜色选择器", px + 10, py + 8, 0xFF222222, false);

        // ── 预览色块 ──
        int previewSize = 40;
        int previewX = px + 250;
        int previewY = py + 30;
        drawRect(previewX, previewY, previewX + previewSize, previewY + previewSize, packARGB());
        drawRect(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF888888); // border

        // ── R/G/B 滑块 ──
        int sliderY = py + 35;
        int sliderX = px + 30;
        int sliderW = 200;
        int sliderH = 14;

        int[] vals = {r, g, b};
        int[] colors = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        String[] labels = {"R", "G", "B"};
        boolean[] dragging = {draggingR, draggingG, draggingB};

        for (int i = 0; i < 3; i++) {
            // 渐变条背景
            for (int sx = 0; sx < sliderW; sx++) {
                float t = (float) sx / sliderW;
                int gradColor = blendColors(0, colors[i], (int) (t * 255));
                drawRect(sliderX + sx, sliderY + i * 22, sliderX + sx + 1, sliderY + i * 22 + sliderH, gradColor);
            }
            // 滑块
            float frac = vals[i] / 255f;
            int knobX = sliderX + (int) (frac * sliderW) - 3;
            drawRect(knobX, sliderY + i * 22 - 1, knobX + 6, sliderY + i * 22 + sliderH + 1, 0xFFFFFFFF);
            drawRect(knobX, sliderY + i * 22 - 1, knobX + 6, sliderY + i * 22 + sliderH + 1, 0xFF000000);

            // 拖动处理
            if (dragging[i] && Mouse.isButtonDown(0)) {
                int newVal = Math.max(0, Math.min(255, (int) (((float) (mouseX - sliderX) / sliderW) * 255)));
                if (i == 0) r = newVal;
                else if (i == 1) g = newVal;
                else b = newVal;
            } else if (dragging[i]) {
                if (i == 0) draggingR = false;
                else if (i == 1) draggingG = false;
                else draggingB = false;
            }

            // 标签 + 数值
            mc.fontRendererObj.drawString(labels[i], px + 10, sliderY + i * 22 + 2, 0xFF222222, false);
            String valStr = String.valueOf(vals[i]);
            mc.fontRendererObj.drawString(valStr, sliderX + sliderW + 8, sliderY + i * 22 + 2, 0xFF222222, false);
        }

        // ── 十六进制显示 ──
        String hex = String.format("#%02X%02X%02X%02X", a, r, g, b);
        mc.fontRendererObj.drawString(hex, px + 10, py + 110, 0xFF666666, false);

        // ── 预设色板 ──
        int presetSize = 20;
        int presetGap = 4;
        int presetsPerRow = 8;
        int presetStartX = px + 10;
        int presetStartY = py + 130;
        for (int i = 0; i < PRESETS.length; i++) {
            int col = i % presetsPerRow;
            int row = i / presetsPerRow;
            int sx = presetStartX + col * (presetSize + presetGap);
            int sy = presetStartY + row * (presetSize + presetGap);
            drawRect(sx, sy, sx + presetSize, sy + presetSize, PRESETS[i]);
            drawRect(sx, sy, sx + presetSize, sy + presetSize, 0xFF888888);
        }

        // ── 按钮 ──
        int btnY = py + 175;
        int btnW = 80, btnH = 20;
        int btnSpacing = 20;
        int totalW = btnW * 2 + btnSpacing;
        int btnStartX = px + (pw - totalW) / 2;

        // [确定]
        boolean okHover = mouseX >= btnStartX && mouseX <= btnStartX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        drawRect(btnStartX, btnY, btnStartX + btnW, btnY + btnH, okHover ? 0xFF3CB371 : 0xFF2E8B57);
        mc.fontRendererObj.drawString("确定", btnStartX + (btnW - mc.fontRendererObj.getStringWidth("确定")) / 2,
                btnY + (btnH - mc.fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFFFF, false);

        // [取消]
        int cancelX = btnStartX + btnW + btnSpacing;
        boolean cancelHover = mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        drawRect(cancelX, btnY, cancelX + btnW, btnY + btnH, cancelHover ? 0xFFDC143C : 0xFFB22222);
        mc.fontRendererObj.drawString("取消", cancelX + (btnW - mc.fontRendererObj.getStringWidth("取消")) / 2,
                btnY + (btnH - mc.fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFFFF, false);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        int cx = width / 2, cy = height / 2;
        int px = cx - 160, py = cy - 130;

        // 滑块点击
        int sliderX = px + 30;
        int sliderW = 200;
        for (int i = 0; i < 3; i++) {
            int sy = py + 35 + i * 22;
            if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= sy - 1 && mouseY <= sy + 15) {
                int newVal = Math.max(0, Math.min(255, (int) (((float) (mouseX - sliderX) / sliderW) * 255)));
                if (i == 0) { r = newVal; draggingR = true; }
                else if (i == 1) { g = newVal; draggingG = true; }
                else { b = newVal; draggingB = true; }
                return;
            }
        }

        // 预设色板
        int presetSize = 20, presetGap = 4, presetsPerRow = 8;
        int presetStartX = px + 10, presetStartY = py + 130;
        for (int i = 0; i < PRESETS.length; i++) {
            int col = i % presetsPerRow;
            int row = i / presetsPerRow;
            int sx = presetStartX + col * (presetSize + presetGap);
            int sy = presetStartY + row * (presetSize + presetGap);
            if (mouseX >= sx && mouseX <= sx + presetSize && mouseY >= sy && mouseY <= sy + presetSize) {
                int packed = PRESETS[i];
                r = (packed >> 16) & 0xFF;
                g = (packed >> 8) & 0xFF;
                b = packed & 0xFF;
                a = 0xFF;
                return;
            }
        }

        // 确定按钮
        int btnY = py + 175;
        int btnW = 80, btnH = 20;
        int btnStartX = px + (320 - btnW * 2 - 20) / 2;
        if (mouseX >= btnStartX && mouseX <= btnStartX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            setter.accept(packARGB());
            mc.displayGuiScreen(parent);
            return;
        }

        // 取消按钮
        int cancelX = btnStartX + btnW + 20;
        if (mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private int packARGB() {
        return (a << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static int blendColors(int fromA, int toColor, int amount) {
        int r1 = 0, g1 = 0, b1 = 0;
        int r2 = (toColor >> 16) & 0xFF, g2 = (toColor >> 8) & 0xFF, b2 = toColor & 0xFF;
        int rn = r1 + (r2 - r1) * amount / 255;
        int gn = g1 + (g2 - g1) * amount / 255;
        int bn = b1 + (b2 - b1) * amount / 255;
        return 0xFF000000 | (rn << 16) | (gn << 8) | bn;
    }
}
