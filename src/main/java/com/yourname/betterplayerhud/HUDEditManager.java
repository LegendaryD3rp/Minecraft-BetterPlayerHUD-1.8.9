package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * HUD 拖拽编辑模式
 *
 * 按 F7 打开编辑 GUI（GuiEditScreen）→ 类似 ESC 菜单：
 *   - 冻结游戏 & 释放鼠标 & 不改变视角
 *   - 两步操作：点击选中模块 → 再拖拽移动
 *   - 选中模块后 Ctrl+滚轮调大小
 *   - R 键一键恢复当前模块的默认位置
 *   - Shift+R 恢复全部模块
 *   - 再次 F7 或 ESC 退出
 *
 * 坐标系一致性：
 *   所有模块在编辑模式下统一使用「屏幕绝对坐标」(0,0 左上角)
 *   拖拽松手时，PosConverter 将绝对坐标转为各模块的 config 语义
 */
public class HUDEditManager {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static KeyBinding keyEditMode;

    /** 模块名 → 当期位置（屏幕绝对坐标） */
    private static final Map<String, Rectangle> currentPositions = new LinkedHashMap<>();
    /** 模块名 → X/Y config 设置器 */
    private static final Map<String, Consumer<Integer>> xSetters = new LinkedHashMap<>();
    private static final Map<String, Consumer<Integer>> ySetters = new LinkedHashMap<>();
    /** 模块名 → 大小调整器（Ctrl+滚轮用） */
    private static final Map<String, BiConsumer<Integer, Rectangle>> sizeSetters = new LinkedHashMap<>();
    /** 模块名 → 尺寸重置器（R键用） */
    private static final Map<String, Runnable> sizeResets = new LinkedHashMap<>();
    /** 模块名 → 绝对坐标 → config 值转换器 */
    private static final Map<String, PosConverter> posConverters = new LinkedHashMap<>();
    /** 模块名 → 默认位置（用于重置） */
    private static final Map<String, int[]> defaultPositions = new LinkedHashMap<>();
    /** 模块名 → 默认placeholder尺寸（宽,高），用于从未有状态的模块 */
    private static final Map<String, int[]> defaultSizes = new LinkedHashMap<>();

    /** 单例实例（用于 Forge 事件总线注册） */
    public static final HUDEditManager INSTANCE = new HUDEditManager();
    /** 当前打开的编辑屏幕（null=未打开） */
    private static GuiEditScreen activeScreen = null;

    // ═══════════════════════════════════════════════════════════════
    //  坐标转换接口
    // ═══════════════════════════════════════════════════════════════

    @FunctionalInterface
    public interface PosConverter {
        /**
         * 将屏幕绝对坐标转换为当前模块的 config 字段值
         * @param absX      拖拽后的绝对 X（屏幕像素）
         * @param absY      拖拽后的绝对 Y
         * @param sw        缩放后的屏幕宽度
         * @param sh        缩放后的屏幕高度
         * @return [configX, configY]
         */
        int[] convert(int absX, int absY, int sw, int sh);
    }

    // ═══════════════════════════════════════════════════════════════

    private HUDEditManager() {}

    public static void init() {
        keyEditMode = new KeyBinding("key.hudEditMode", Keyboard.KEY_F7, "key.categories.betterplayerhud");
        ClientRegistry.registerKeyBinding(keyEditMode);
    }

    public static boolean isEditing() { return activeScreen != null; }

    /**
     * 注册可拖拽模块（绝对坐标模块）
     * @param name       显示名称
     * @param setX       config X 设置器
     * @param setY       config Y 设置器
     * @param defaultX   默认 X（重置时恢复）
     * @param defaultY   默认 Y
     */
    public static void register(String name, Consumer<Integer> setX, Consumer<Integer> setY,
                                int defaultX, int defaultY) {
        register(name, setX, setY, defaultX, defaultY,
                (absX, absY, sw, sh) -> new int[]{absX, absY});  // identity 转换
    }

    /**
     * 注册可拖拽模块（带坐标转换器，用于 offset 模块）
     * @param name       显示名称
     * @param setX       config X 设置器
     * @param setY       config Y 设置器
     * @param defaultX   默认 X（重置时恢复）
     * @param defaultY   默认 Y
     * @param converter  绝对坐标 → config 值转换器
     */
    public static void register(String name,
                                Consumer<Integer> setX, Consumer<Integer> setY,
                                int defaultX, int defaultY,
                                PosConverter converter) {
        xSetters.put(name, setX);
        ySetters.put(name, setY);
        currentPositions.put(name, new Rectangle(0, 0, 0, 0));
        posConverters.put(name, converter);
        defaultPositions.put(name, new int[]{defaultX, defaultY});
    }

    /** 注册大小调整器（可选，Ctrl+滚轮用） */
    public static void setSize(String name, BiConsumer<Integer, Rectangle> setSize) {
        sizeSetters.put(name, setSize);
    }

    /** 注册尺寸重置器（R键恢复默认大小时用） */
    public static void registerSizeReset(String name, Runnable resetter) {
        sizeResets.put(name, resetter);
    }

    /** 设置默认placeholder尺寸（用于从未有过状态的模块在F7中仍可显示） */
    public static void setDefaultSize(String name, int w, int h) {
        defaultSizes.put(name, new int[]{w, h});
    }

    /** 进入编辑模式时填充从未有状态的模块为placeholder矩形 */
    public static void fillPlaceholders() {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        for (String name : currentPositions.keySet()) {
            Rectangle r = currentPositions.get(name);
            if (r != null && r.width > 0 && r.height > 0) continue;

            if ("罗盘".equals(name)) {
                int offsetX = BetterPlayerHUD.config.xPosition;
                int xPos = offsetX > 0 ? offsetX : (offsetX < 0 ? sw + offsetX - 240 : (sw - 240) / 2);
                int offsetY = BetterPlayerHUD.config.yPosition;
                int yPos = offsetY >= 0 ? offsetY : sh + offsetY - 40;
                r.setBounds(xPos, yPos, 240, 30);
            } else if ("药水效果".equals(name)) {
                int px = 2 + BetterPlayerHUD.config.potionXOffset;
                int py = sh - 20 + BetterPlayerHUD.config.potionYOffset;
                r.setBounds(px, py, 160, 26);
            } else if ("按键显示".equals(name)) {
                int ks = BetterPlayerHUD.config.keysSize;
                int ksp = BetterPlayerHUD.config.keysSpacing;
                int dw = 3 * ks + 2 * ksp;
                int dh = 4 * ks + 3 * ksp;
                int offsetX = BetterPlayerHUD.config.keysDisplayX;
                int xPos = offsetX >= 0 ? offsetX : sw + offsetX - dw;
                int offsetY = BetterPlayerHUD.config.keysDisplayY;
                int yPos = offsetY >= 0 ? offsetY : sh + offsetY - dh;
                r.setBounds(xPos, yPos, dw, dh);
            } else if ("物品数量".equals(name)) {
                int hotbarLeft = sw / 2 - 91;
                int bx = hotbarLeft + 3 + BetterPlayerHUD.config.itemCountX;
                int by = sh - 22 - 10 + BetterPlayerHUD.config.itemCountY;
                r.setBounds(bx, by, 80, 10);
            } else if ("危机警戒".equals(name)) {
                int iconSize = BetterPlayerHUD.config.crisisIconSize;
                int gap = iconSize / 3;
                if (gap < 4) gap = 4;
                int rw = 5 * iconSize + 4 * gap;
                int cx = sw / 2;
                int px = cx - rw / 2 + BetterPlayerHUD.config.crisisXOffset;
                int py = sh / 2 - 60 + BetterPlayerHUD.config.crisisYOffset;
                r.setBounds(px, py, rw, iconSize);
            } else if ("目标血量".equals(name)) {
                int bw = 140;
                int bx = sw / 2 - bw / 2;
                int by = sh - 47;
                r.setBounds(bx, by, bw, 40);
            } else {
                int[] ds = defaultSizes.get(name);
                if (ds == null) continue;
                int[] dp = defaultPositions.get(name);
                r.setBounds(dp[0], dp[1], ds[0], ds[1]);
            }
        }
    }

    /** Handler 渲染完后调用，上报当前位置（屏幕绝对坐标） */
    public static void report(String name, int x, int y, int w, int h) {
        if (activeScreen == null) return;
        Rectangle r = currentPositions.get(name);
        if (r != null) {
            r.setBounds(x, y, w, h);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null) return;

        // 编辑模式下死亡 → 关闭编辑 GUI，让路给死亡屏幕
        if (activeScreen != null && mc.thePlayer.getHealth() <= 0.0F) {
            mc.displayGuiScreen(null);
            activeScreen = null;
            return;
        }

        // 编辑模式下冻结玩家移动（视角不动）
        if (activeScreen != null) {
            mc.thePlayer.movementInput.moveForward = 0.0f;
            mc.thePlayer.movementInput.moveStrafe = 0.0f;
            mc.thePlayer.movementInput.jump = false;
            mc.thePlayer.movementInput.sneak = false;
        }

        while (keyEditMode.isPressed()) {
            if (activeScreen == null) {
                fillPlaceholders();
                activeScreen = new GuiEditScreen();
                mc.displayGuiScreen(activeScreen);
            } else {
                mc.displayGuiScreen(null);
                activeScreen = null;
            }
        }

        // 如果屏幕意外关闭（比如 ESC），同步状态
        if (activeScreen != null && mc.currentScreen != activeScreen) {
            activeScreen = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  编辑 GUI 屏幕
    // ═══════════════════════════════════════════════════════════════
    private static class GuiEditScreen extends GuiScreen {

        private String selected = null;       // 当前选中的模块名
        private String dragging = null;        // 正在拖拽的模块名
        private int dragOffX, dragOffY;
        private String lastHovered = null;

        // 重置确认状态
        private long resetPromptTime = 0;
        private boolean resetAll = false;

        private static final int BORDER_COLOR = 0xCCFFFFFF;
        private static final int SELECTED_COLOR = 0xCC00FF00;
        private static final int DRAGGING_COLOR = 0xFFFFAA00;

        GuiEditScreen() {
            this.allowUserInput = true;  // 必须=true，否则 Minecraft 1.8.9 不会将鼠标事件路由到 GuiScreen
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;  // false：让 runTick() 继续跑，鼠标事件才能送达 GuiScreen
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            // 半透明背景遮罩
            drawRect(0, 0, width, height, 0x88000000);

            // 绘制所有模块的拖拽框
            for (Map.Entry<String, Rectangle> e : currentPositions.entrySet()) {
                Rectangle r = e.getValue();
                // 从未有状态的模块 → 用默认placeholder替代
                if (r == null || r.width <= 0 || r.height <= 0) {
                    int[] ds = defaultSizes.get(e.getKey());
                    if (ds == null) continue;
                    int[] dp = defaultPositions.get(e.getKey());
                    r = new Rectangle(dp[0], dp[1], ds[0], ds[1]);
                }

                String name = e.getKey();
                boolean isHovered = r.contains(mouseX, mouseY);
                boolean isDraggingThis = name.equals(dragging);
                boolean isSelectedThis = name.equals(selected) && !isDraggingThis;

                // 填充色
                int fillColor;
                if (isDraggingThis) {
                    fillColor = 0x66FFAA00;
                } else if (isSelectedThis) {
                    fillColor = 0x4400FF00;
                } else if (isHovered) {
                    fillColor = 0x44FFFFFF;
                } else {
                    fillColor = (name.hashCode() & 0x00FFFFFF) | 0x22000000;
                }

                // 外边框
                int borderColor;
                if (isDraggingThis) borderColor = DRAGGING_COLOR;
                else if (isSelectedThis) borderColor = SELECTED_COLOR;
                else borderColor = BORDER_COLOR;

                drawRect(r.x - 1, r.y - 1, r.x + r.width + 1, r.y + r.height + 1, borderColor);
                drawRect(r.x, r.y, r.x + r.width, r.y + r.height, fillColor);

                // 选中态标记
                if (isSelectedThis) {
                    // 左上角绿色小三角标
                    drawRect(r.x - 3, r.y - 3, r.x, r.y + 6, 0xFF00FF00);
                    drawRect(r.x - 3, r.y - 3, r.x + 6, r.y, 0xFF00FF00);
                }

                // 模块名称标签
                String label = "§l" + name;
                int lw = fontRendererObj.getStringWidth(label);
                int lx = r.x + r.width / 2 - lw / 2;
                int ly = r.y - fontRendererObj.FONT_HEIGHT - 3;
                drawRect(lx - 2, ly - 1, lx + lw + 2, ly + fontRendererObj.FONT_HEIGHT + 1, 0xAA000000);
                fontRendererObj.drawStringWithShadow(label, lx, ly, 0xFFFFFF);

                // 大小调整提示
                if (sizeSetters.containsKey(name)) {
                    String sizeHint = "§7Ctrl+滚轮";
                    fontRendererObj.drawStringWithShadow(sizeHint,
                            r.x + 3, r.y + r.height - fontRendererObj.FONT_HEIGHT - 2, 0xAAFFFFFF);
                }

                // 选中态下额外操作提示
                if (isSelectedThis) {
                    String selHint = "§a[ 已选中 · §fR§a重置 · §f拖拽§a移动 ]";
                    int shw = fontRendererObj.getStringWidth(selHint);
                    fontRendererObj.drawStringWithShadow(selHint,
                            r.x + r.width / 2 - shw / 2, r.y + r.height + 2, 0xFFFFFF);
                }
            }

            // ── 底部提示 ──
            String hint;
            if (selected != null) {
                hint = "§e§lHUD 编辑 — 已选中 §b" + selected + " §e| 拖拽移动 | Ctrl+滚轮调大小 | R重置 | F7/ESC退出";
            } else {
                hint = "§e§lHUD 编辑 — §7点击模块选中 | 拖拽移动 | Ctrl+滚轮调大小 | R重置全部 | F7/ESC退出";
            }
            int hw = fontRendererObj.getStringWidth(hint);
            drawRect(width / 2 - hw / 2 - 4, height - 20, width / 2 + hw / 2 + 4, height - 4, 0xAA000000);
            fontRendererObj.drawStringWithShadow(hint, width / 2 - hw / 2, height - 16, 0xFFFFFF);

            // ── 重置确认提示 ──
            if (resetPromptTime > 0 && System.currentTimeMillis() - resetPromptTime < 3000) {
                String prompt = resetAll
                        ? "§c§l重置所有模块位置？再次按 R 确认 | 其他键取消"
                        : "§c§l重置 " + selected + " 位置？再次按 R 确认 | 其他键取消";
                int pw = fontRendererObj.getStringWidth(prompt);
                drawRect(width / 2 - pw / 2 - 4, height / 2 - 14, width / 2 + pw / 2 + 4, height / 2 + 4, 0xCC000000);
                fontRendererObj.drawStringWithShadow(prompt, width / 2 - pw / 2, height / 2 - 10, 0xFF5555);
            }

            // 鼠标悬停提示
            if (lastHovered != null && !lastHovered.equals(dragging) && !lastHovered.equals(selected)) {
                String hoverTip = "§7点击选中 " + lastHovered;
                fontRendererObj.drawStringWithShadow(hoverTip, mouseX + 8, mouseY - 12, 0xFFFFFF);
            }

            // ═══════════════════════════════════════════════════════════
            //  帧率级拖拽更新（代替 mouseClickMove 的刻率级更新）
            //  改在这里更新，利用帧循环（60+ FPS）实现平滑跟随
            // ═══════════════════════════════════════════════════════════
            if (dragging != null) {
                Rectangle r = currentPositions.get(dragging);
                if (r != null) {
                    int newX = mouseX - dragOffX;
                    int newY = mouseY - dragOffY;
                    newX = Math.max(0, Math.min(newX, width - r.width));
                    newY = Math.max(0, Math.min(newY, height - r.height));
                    if (newX != r.x || newY != r.y) {
                        r.setLocation(newX, newY);
                        // 实时更新 config
                        PosConverter converter = posConverters.get(dragging);
                        Consumer<Integer> setX = xSetters.get(dragging);
                        Consumer<Integer> setY = ySetters.get(dragging);
                        if (converter != null && setX != null && setY != null) {
                            int[] cfg = converter.convert(newX, newY, width, height);
                            setX.accept(cfg[0]);
                            setY.accept(cfg[1]);
                        }
                    }
                }
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            if (mouseButton != 0) return;

            // 找到最上层被点击的模块
            String clicked = null;
            for (Map.Entry<String, Rectangle> e : currentPositions.entrySet()) {
                Rectangle r = e.getValue();
                if (r != null && r.contains(mouseX, mouseY)) {
                    clicked = e.getKey();
                    break;
                }
            }

            if (clicked == null) {
                // 点击空白 → 取消选中
                selected = null;
                return;
            }

            if (clicked.equals(selected)) {
                // 点击已选中的模块 → 开始拖拽
                Rectangle r = currentPositions.get(clicked);
                dragging = clicked;
                dragOffX = mouseX - r.x;
                dragOffY = mouseY - r.y;
            } else {
                // 点击未选中的模块 → 选中
                selected = clicked;
                dragging = null;  // 确保不拖拽
            }
        }

        @Override
        protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
            if (clickedMouseButton != 0 || dragging == null) return;

            Rectangle r = currentPositions.get(dragging);
            if (r == null) return;

            int newX = mouseX - dragOffX;
            int newY = mouseY - dragOffY;

            // 边界约束（不拖出屏幕）
            newX = Math.max(0, Math.min(newX, width - r.width));
            newY = Math.max(0, Math.min(newY, height - r.height));

            if (newX != r.x || newY != r.y) {
                r.setLocation(newX, newY);
                // 实时更新 config，让各 Handler 渲染到新位置
                PosConverter converter = posConverters.get(dragging);
                Consumer<Integer> setX = xSetters.get(dragging);
                Consumer<Integer> setY = ySetters.get(dragging);
                if (converter != null && setX != null && setY != null) {
                    int[] cfg = converter.convert(newX, newY, width, height);
                    setX.accept(cfg[0]);
                    setY.accept(cfg[1]);
                }
            }
        }

        @Override
        protected void mouseReleased(int mouseX, int mouseY, int state) {
            if (state != 0 || dragging == null) return;

            // 拖拽结束 → 用 PosConverter 将绝对坐标转成 config 值
            Rectangle r = currentPositions.get(dragging);
            if (r != null) {
                PosConverter converter = posConverters.get(dragging);
                Consumer<Integer> setX = xSetters.get(dragging);
                Consumer<Integer> setY = ySetters.get(dragging);
                if (converter != null && setX != null && setY != null) {
                    int[] cfg = converter.convert(r.x, r.y, width, height);
                    setX.accept(cfg[0]);
                    setY.accept(cfg[1]);
                }
                BetterPlayerHUD.config.saveConfig();
            }
            dragging = null;
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();

            int dWheel = Mouse.getEventDWheel();
            if (dWheel == 0) return;

            // Ctrl+滚轮调大小（选中或拖拽中的模块都生效）
            String target = (dragging != null) ? dragging : selected;
            if (target == null) return;

            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                BiConsumer<Integer, Rectangle> setSize = sizeSetters.get(target);
                if (setSize != null) {
                    int delta = dWheel > 0 ? 1 : -1;
                    Rectangle r = currentPositions.get(target);
                    setSize.accept(delta, r);
                    BetterPlayerHUD.config.saveConfig();
                }
            }
        }

        @Override
        public void onGuiClosed() {
            activeScreen = null;
            if (dragging != null) {
                BetterPlayerHUD.config.saveConfig();
                dragging = null;
            }
            selected = null;
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) throws IOException {
            // F7 退出
            if (keyCode == keyEditMode.getKeyCode()) {
                mc.displayGuiScreen(null);
                return;
            }

            // R 键：重置位置
            if (keyCode == Keyboard.KEY_R) {
                handleResetKey();
                return;
            }

            // 按其他键 → 取消重置确认
            if (resetPromptTime > 0) {
                resetPromptTime = 0;
            }

            // ESC 由父类处理
            super.keyTyped(typedChar, keyCode);
        }

        /** 处理重置按键逻辑（两次 R 确认） */
        private void handleResetKey() {
            long now = System.currentTimeMillis();

            if (selected != null) {
                // 有选中模块 → 重置单个
                if (resetPromptTime > 0 && !resetAll && now - resetPromptTime < 3000) {
                    // 二次确认 → 执行重置
                    resetModulePosition(selected);
                    resetPromptTime = 0;
                } else {
                    resetPromptTime = now;
                    resetAll = false;
                }
            } else {
                // 无选中 → 重置全部
                if (resetPromptTime > 0 && resetAll && now - resetPromptTime < 3000) {
                    for (String name : currentPositions.keySet()) {
                        resetModulePosition(name);
                    }
                    resetPromptTime = 0;
                } else {
                    resetPromptTime = now;
                    resetAll = true;
                }
            }
        }

        /** 将指定模块恢复默认位置 */
        private void resetModulePosition(String name) {
            int[] def = defaultPositions.get(name);
            if (def == null) return;

            Consumer<Integer> setX = xSetters.get(name);
            Consumer<Integer> setY = ySetters.get(name);
            if (setX != null) setX.accept(def[0]);
            if (setY != null) setY.accept(def[1]);

            // 恢复尺寸/缩放
            Runnable sizeReset = sizeResets.get(name);
            if (sizeReset != null) sizeReset.run();

            // 立即更新当前位置和尺寸
            Rectangle r = currentPositions.get(name);
            if (r != null) {
                updateDefaultRect(name, r);
            }

            BetterPlayerHUD.config.saveConfig();
        }

        /** 根据当前 config 值计算出模块的默认矩形 */
        private void updateDefaultRect(String name, Rectangle r) {
            if ("罗盘".equals(name)) {
                int xPos = calcCompassX(width, 240);
                int yPos = calcCompassY(height);
                r.setBounds(xPos, yPos, 240, 30);
            } else if ("药水效果".equals(name)) {
                int px = 2 + BetterPlayerHUD.config.potionXOffset;
                int py = height - 20 + BetterPlayerHUD.config.potionYOffset;
                r.setBounds(px, py, 160, 26);
            } else if ("物品数量".equals(name)) {
                int hotbarLeft = width / 2 - 91;
                int bx = hotbarLeft + 3 + BetterPlayerHUD.config.itemCountX;
                int by = height - 22 - 10 + BetterPlayerHUD.config.itemCountY;
                r.setBounds(bx, by, 80, 10);
            } else if ("危机警戒".equals(name)) {
                int iconSize = BetterPlayerHUD.config.crisisIconSize;
                int gap = iconSize / 3;
                if (gap < 4) gap = 4;
                int rw = 5 * iconSize + 4 * gap;
                int cx = width / 2;
                int px = cx - rw / 2 + BetterPlayerHUD.config.crisisXOffset;
                int py = height / 2 - 60 + BetterPlayerHUD.config.crisisYOffset;
                r.setBounds(px, py, rw, iconSize);
            } else if ("目标血量".equals(name)) {
                int bw = 140;
                int bx = width / 2 - bw / 2;
                int by = height - 47;
                r.setBounds(bx, by, bw, 40);
            } else if ("按键显示".equals(name)) {
                int ks = BetterPlayerHUD.config.keysSize;
                int ksp = BetterPlayerHUD.config.keysSpacing;
                int dw = 3 * ks + 2 * ksp;
                int dh = 4 * ks + 3 * ksp;
                int xPos = calcKeysX(width, dw);
                int yPos = calcKeysY(height, dh);
                r.setBounds(xPos, yPos, dw, dh);
            } else {
                int[] ds = defaultSizes.get(name);
                int[] dp = defaultPositions.get(name);
                if (ds != null && dp != null) {
                    r.setSize(ds[0], ds[1]);
                    r.setLocation(dp[0], dp[1]);
                }
            }
        }

        private int calcCompassX(int screenWidth, int elementWidth) {
            int offset = BetterPlayerHUD.config.xPosition;
            if (offset > 0) return offset;
            else if (offset < 0) return screenWidth + offset - elementWidth;
            else return (screenWidth - elementWidth) / 2;
        }

        private int calcCompassY(int screenHeight) {
            int offset = BetterPlayerHUD.config.yPosition;
            if (offset >= 0) return offset;
            else return screenHeight + offset - 40;
        }

        private int calcKeysX(int screenWidth, int displayWidth) {
            int offset = BetterPlayerHUD.config.keysDisplayX;
            if (offset >= 0) return offset;
            else return screenWidth + offset - displayWidth;
        }

        private int calcKeysY(int screenHeight, int displayHeight) {
            int offset = BetterPlayerHUD.config.keysDisplayY;
            if (offset >= 0) return offset;
            else return screenHeight + offset - displayHeight;
        }
    }
}
