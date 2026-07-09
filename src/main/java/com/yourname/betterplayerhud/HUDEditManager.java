package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HUD 拖拽编辑模式
 *
 * 按 F7 切换编辑模式 → 所有可移动模块显示彩色拖拽框 + 名称
 * 鼠标点击拖拽框 → 拖动 → 释放后自动保存配置
 */
public class HUDEditManager {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static KeyBinding keyEditMode;
    private static boolean isEditing = false;

    /** 模块名 → 当期位置 */
    private static final Map<String, Rectangle> currentPositions = new LinkedHashMap<>();
    /** 模块名 → X/Y 设置器 */
    private static final Map<String, Consumer<Integer>> xSetters = new LinkedHashMap<>();
    private static final Map<String, Consumer<Integer>> ySetters = new LinkedHashMap<>();

    /** 拖拽状态 */
    private static String dragging = null;
    private static int dragOffX, dragOffY;

    private static final int BOX_COLOR = 0x44FFFFFF;
    private static final int BORDER_COLOR = 0xCCFFFFFF;

    public static void init() {
        keyEditMode = new KeyBinding("key.hudEditMode", Keyboard.KEY_F7, "key.categories.betterplayerhud");
        ClientRegistry.registerKeyBinding(keyEditMode);
    }

    public static boolean isEditing() { return isEditing; }

    /** 注册一个可拖拽模块 */
    public static void register(String name, Consumer<Integer> setX, Consumer<Integer> setY) {
        xSetters.put(name, setX);
        ySetters.put(name, setY);
        currentPositions.put(name, new Rectangle(0, 0, 0, 0));
    }

    /** Handler 渲染完后调用，上报当前位置 */
    public static void report(String name, int x, int y, int w, int h) {
        if (!isEditing) return;
        Rectangle r = currentPositions.get(name);
        if (r != null) {
            r.setBounds(x, y, w, h);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null) return;

        while (keyEditMode.isPressed()) {
            isEditing = !isEditing;
            if (!isEditing) {
                dragging = null;
            }
        }

        if (!isEditing) return;

        boolean pressed = Mouse.isButtonDown(0);
        int rawX = Mouse.getX();
        int rawY = Mouse.getY();
        ScaledResolution sr = new ScaledResolution(mc);
        int sx = rawX * sr.getScaledWidth() / mc.displayWidth;
        int sy = mc.displayHeight - rawY * sr.getScaledHeight() / mc.displayHeight;

        if (pressed && dragging == null) {
            for (Map.Entry<String, Rectangle> e : currentPositions.entrySet()) {
                Rectangle r = e.getValue();
                if (r.contains(sx, sy)) {
                    dragging = e.getKey();
                    dragOffX = sx - r.x;
                    dragOffY = sy - r.y;
                    break;
                }
            }
        } else if (!pressed && dragging != null) {
            BetterPlayerHUD.config.saveConfig();
            dragging = null;
        }

        if (dragging != null) {
            Rectangle r = currentPositions.get(dragging);
            if (r != null) {
                int newX = sx - dragOffX;
                int newY = sy - dragOffY;
                r.setLocation(newX, newY);
                Consumer<Integer> setX = xSetters.get(dragging);
                Consumer<Integer> setY = ySetters.get(dragging);
                if (setX != null) setX.accept(newX);
                if (setY != null) setY.accept(newY);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEditing) return;
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        ScaledResolution sr = new ScaledResolution(mc);

        for (Map.Entry<String, Rectangle> e : currentPositions.entrySet()) {
            Rectangle r = e.getValue();
            if (r.width <= 0 || r.height <= 0) continue;

            int color = e.getKey().hashCode() | 0x88000000;
            Gui.drawRect(r.x - 1, r.y - 1, r.x + r.width + 1, r.y + r.height + 1, BORDER_COLOR);
            Gui.drawRect(r.x, r.y, r.x + r.width, r.y + r.height, color & 0x44FFFFFF);

            String label = "[ " + e.getKey() + " ]";
            int lw = mc.fontRendererObj.getStringWidth(label);
            int lx = r.x + r.width / 2 - lw / 2;
            int ly = r.y - mc.fontRendererObj.FONT_HEIGHT - 2;
            mc.fontRendererObj.drawStringWithShadow(label, lx, ly, 0xFFFFFFFF);
        }

        String hint = "§e§lHUD 编辑模式 — 拖拽模块调整位置 | F7 退出";
        mc.fontRendererObj.drawStringWithShadow(hint,
                sr.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(hint) / 2,
                2, 0xFFFFFF);
    }
}
