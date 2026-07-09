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
 * 选中模块后，按住 Ctrl + 鼠标滚轮 → 调大小
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
    /** 模块名 → 大小调整器（Ctrl+滚轮用） */
    private static final Map<String, Consumer<Integer>> sizeSetters = new LinkedHashMap<>();

    /** 拖拽状态 */
    private static String dragging = null;
    private static int dragOffX, dragOffY;

    private static final int BORDER_COLOR = 0xCCFFFFFF;

    /** 单例实例（用于 Forge 事件总线注册） */
    public static final HUDEditManager INSTANCE = new HUDEditManager();

    private HUDEditManager() {}

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

    /** 注册大小调整器（可选，Ctrl+滚轮用） */
    public static void setSize(String name, Consumer<Integer> setSize) {
        sizeSetters.put(name, setSize);
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
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        while (keyEditMode.isPressed()) {
            isEditing = !isEditing;
            if (!isEditing) {
                dragging = null;
                mc.mouseHelper.grabMouseCursor();
            } else {
                mc.mouseHelper.ungrabMouseCursor();
            }
        }

        // 编辑模式下冻结玩家移动
        if (isEditing && mc.thePlayer != null) {
            mc.thePlayer.movementInput.moveForward = 0.0f;
            mc.thePlayer.movementInput.moveStrafe = 0.0f;
            mc.thePlayer.movementInput.jump = false;
            mc.thePlayer.movementInput.sneak = false;
        }

        if (!isEditing) return;
        if (mc.thePlayer == null) return;

        boolean pressed = Mouse.isButtonDown(0);
        int rawX = Mouse.getX();
        int rawY = Mouse.getY();
        ScaledResolution sr = new ScaledResolution(mc);
        int sx = rawX * sr.getScaledWidth() / mc.displayWidth;
        // 正确公式：参考 GuiScreen.handleMouseInput()
        int sy = sr.getScaledHeight() - rawY * sr.getScaledHeight() / mc.displayHeight - 1;

        if (pressed && dragging == null) {
            for (Map.Entry<String, Rectangle> e : currentPositions.entrySet()) {
                Rectangle r = e.getValue();
                if (r != null && r.contains(sx, sy)) {
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
                // 按住 Ctrl + 滚轮调大小
                if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                    int dWheel = Mouse.getDWheel();
                    if (dWheel != 0) {
                        Consumer<Integer> setSize = sizeSetters.get(dragging);
                        if (setSize != null) {
                            int delta = dWheel > 0 ? 1 : -1;
                            setSize.accept(delta);
                        }
                    }
                } else {
                    // 拖拽位置（仅在位置变化时更新）
                    int newX = sx - dragOffX;
                    int newY = sy - dragOffY;
                    if (newX != r.x || newY != r.y) {
                        r.setLocation(newX, newY);
                        Consumer<Integer> setX = xSetters.get(dragging);
                        Consumer<Integer> setY = ySetters.get(dragging);
                        if (setX != null) setX.accept(newX);
                        if (setY != null) setY.accept(newY);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEditing) return;
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        // 保存 OpenGL 状态，防止破坏后续渲染管线
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        ScaledResolution sr = new ScaledResolution(mc);

        for (Map.Entry<String, Rectangle> e : currentPositions.entrySet()) {
            Rectangle r = e.getValue();
            if (r == null || r.width <= 0 || r.height <= 0) continue;

            int color = e.getKey().hashCode() | 0x88000000;

            // 外边框
            Gui.drawRect(r.x - 1, r.y - 1, r.x + r.width + 1, r.y + r.height + 1, BORDER_COLOR);
            // 内部半透明填充
            Gui.drawRect(r.x, r.y, r.x + r.width, r.y + r.height, color & 0x44FFFFFF);

            // 模块名称标签
            String label = "[ " + e.getKey() + " ]";
            int lw = mc.fontRendererObj.getStringWidth(label);
            int lx = r.x + r.width / 2 - lw / 2;
            int ly = r.y - mc.fontRendererObj.FONT_HEIGHT - 2;
            mc.fontRendererObj.drawStringWithShadow(label, lx, ly, 0xFFFFFFFF);

            // 大小调整提示
            if (sizeSetters.containsKey(e.getKey())) {
                String sizeHint = "§7Ctrl+滚轮";
                mc.fontRendererObj.drawStringWithShadow(sizeHint, r.x + 2, r.y + r.height - mc.fontRendererObj.FONT_HEIGHT - 1, 0xAAFFFFFF);
            }
        }

        // 底部提示文字
        String hint = "§e§lHUD 编辑模式 — 拖拽位置 | Ctrl+滚轮调大小 | F7 退出";
        mc.fontRendererObj.drawStringWithShadow(hint,
                sr.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(hint) / 2,
                2, 0xFFFFFF);

        // ↑ 以上 drawStringWithShadow 已开 blend，但 drawRect 会关 blend
        // 确保 blend 开启，避免影响后续 HUD 渲染
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }
}
