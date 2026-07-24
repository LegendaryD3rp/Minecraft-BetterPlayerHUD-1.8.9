package com.yourname.betterplayerhud;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * F8 配置框架 — 主设置屏。
 * 
 * 继承 net.minecraft.client.gui.GuiScreen（1.8.9 原生基类）。
 * 半透明深色背景 + 白色半透明面板居中 + Tab 行 + 配置项列表（滚动） + 底部按钮。
 */
public class GuiModernConfig extends GuiScreen {

    // ── 面板尺寸 ──
    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 400;
    private static final int TAB_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LIST_TOP_OFFSET = 50;  // 标题 + Tab 行占用的高度
    private static final int LIST_BOTTOM_OFFSET = 40; // 底部按钮区

    // ── 颜色 ──
    private static final int COLOR_BG = 0xCC101010;
    private static final int COLOR_PANEL_BG = 0xF0FFFFFF;
    private static final int COLOR_TAB_ACTIVE = 0xFF4A90D9;
    private static final int COLOR_TAB_INACTIVE = 0xFF888888;
    private static final int COLOR_TAB_HOVER = 0xFF6AAAFF;
    private static final int COLOR_TEXT_DARK = 0xFF222222;
    private static final int COLOR_TEXT_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_BUTTON_NORMAL = 0xFF555555;
    private static final int COLOR_BUTTON_HOVER = 0xFF777777;
    private static final int COLOR_SAVE_BUTTON = 0xFF2E8B57;
    private static final int COLOR_SAVE_HOVER = 0xFF3CB371;
    private static final int COLOR_RESET_BUTTON = 0xFFB22222;
    private static final int COLOR_RESET_HOVER = 0xFFDC143C;
    private static final int COLOR_SCROLLBAR_BG = 0x33000000;
    private static final int COLOR_SCROLLBAR = 0x66000000;
    private static final int COLOR_ENTRY_BG = 0x08FFFFFF;
    private static final int COLOR_ENTRY_BG_HOVER = 0x15FFFFFF;
    private static final int COLOR_TOGGLE_ON = 0xFF4CAF50;
    private static final int COLOR_TOGGLE_OFF = 0xFF888888;
    private static final int COLOR_ARROW_BG = 0xFF666666;
    private static final int COLOR_ARROW_HOVER = 0xFF888888;
    private static final int COLOR_VALUE_TEXT = 0xFF333333;

    // ── 状态 ──
    private final GuiScreen parentScreen;
    private int activeTab = 0;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    // ── 脉冲动画 ──
    private String pulseLabel = null;
    private long pulseEndTime = 0L;
    // ── 搜索 ──
    private String searchText = "";
    private boolean isSearching = false;
    private final String[] tabKeys = {"config.general", "config.chat", "config.combat", "config.performance", "config.colors"};

    // ── 面板位置（动态计算） ──
    private int panelLeft, panelTop;

    // ── 按钮区域 ──
    private Rect saveBtnRect;
    private Rect resetBtnRect;
    private Rect cancelBtnRect;
    private Rect langBtnRect;

    // ── 滚动条 ──
    private int contentHeight = 0;
    private boolean scrolling = false;
    private int cachedScaleFactor = 1;

    // ── 当前 Tab 的配置条目描述 ──
    private final List<ConfigEntryDescriptor> entries = new ArrayList<>();

    // ────────────────────────────────────────────────────────────────
    //  内部类型：配置项描述
    // ────────────────────────────────────────────────────────────────
    private static class ConfigEntryDescriptor {
        enum Type { BOOLEAN, INT, FLOAT, COLOR, ENUM }
        final Type type;
        final String labelKey;                           // LangManager key
        final java.util.function.Supplier<String> getter; // 返回当前值的字符串表示
        final Object setter;                             // Consumer<Boolean>/Consumer<Integer>/Consumer<Float>/Consumer<String>

        // ── 范围约束（仅 INT/FLOAT 有效，默认无限制） ──
        final int minInt;
        final int maxInt;
        final float minFloat;
        final float maxFloat;

        // ── Enum 支持 ──
        final String[] enumValues;

        // ── Tooltip 支持（可选，null = 不显示） ──
        public String tooltipKey;

        /** 链式设置 tooltip key，方便条目注册时一行搞定 */
        public ConfigEntryDescriptor withTooltip(String tooltipKey) {
            this.tooltipKey = tooltipKey;
            return this;
        }

        // Boolean
        ConfigEntryDescriptor(String labelKey, java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
            this.type = Type.BOOLEAN;
            this.labelKey = labelKey;
            this.getter = () -> String.valueOf(getter.getAsBoolean());
            this.setter = setter;
            this.minInt = 0; this.maxInt = 0;
            this.minFloat = 0f; this.maxFloat = 0f;
            this.enumValues = null;
        }

        // Int（无范围限制）
        ConfigEntryDescriptor(String labelKey, java.util.function.IntSupplier getter, java.util.function.Consumer<Integer> setter) {
            this(labelKey, getter, setter, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        // Int（带范围限制）
        ConfigEntryDescriptor(String labelKey, java.util.function.IntSupplier getter, java.util.function.Consumer<Integer> setter, int minInt, int maxInt) {
            this.type = Type.INT;
            this.labelKey = labelKey;
            this.getter = () -> String.valueOf(getter.getAsInt());
            this.setter = setter;
            this.minInt = minInt;
            this.maxInt = maxInt;
            this.minFloat = 0f; this.maxFloat = 0f;
            this.enumValues = null;
        }

        // Float（无范围限制）
        ConfigEntryDescriptor(String labelKey, java.util.function.DoubleSupplier getter, java.util.function.Consumer<Float> setter) {
            this(labelKey, getter, setter, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        }

        // Float（带范围限制）
        ConfigEntryDescriptor(String labelKey, java.util.function.DoubleSupplier getter, java.util.function.Consumer<Float> setter, float minFloat, float maxFloat) {
            this.type = Type.FLOAT;
            this.labelKey = labelKey;
            this.getter = () -> String.format("%.1f", getter.getAsDouble());
            this.setter = setter;
            this.minInt = 0; this.maxInt = 0;
            this.minFloat = minFloat;
            this.maxFloat = maxFloat;
            this.enumValues = null;
        }

        // Color (packed ARGB int) — 多一个 boolean 标记用于区分 INT(IntSupplier)
        ConfigEntryDescriptor(String labelKey, java.util.function.IntSupplier getter, java.util.function.Consumer<Integer> setter, boolean isColor) {
            this.type = Type.COLOR;
            this.labelKey = labelKey;
            this.getter = () -> {
                int c = getter.getAsInt();
                return String.format("#%02X%02X%02X%02X",
                    (c >> 24) & 0xFF, (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            };
            this.setter = setter;
            this.minInt = 0; this.maxInt = 0;
            this.minFloat = 0f; this.maxFloat = 0f;
            this.enumValues = null;
        }

        // Enum (cycle through values)
        ConfigEntryDescriptor(String labelKey, java.util.function.Supplier<String> getter, java.util.function.Consumer<String> setter, String[] values) {
            this.type = Type.ENUM;
            this.labelKey = labelKey;
            this.getter = getter;
            this.setter = setter;
            this.enumValues = values;
            this.minInt = 0; this.maxInt = 0;
            this.minFloat = 0f; this.maxFloat = 0f;
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  构造
    // ────────────────────────────────────────────────────────────────
    public GuiModernConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        super.initGui();
        activeTab = 0;
        scrollOffset = 0;
        rebuildEntries();
        recalcLayout();
    }

    // ────────────────────────────────────────────────────────────────
    //  布局计算
    // ────────────────────────────────────────────────────────────────
    private void recalcLayout() {
        ScaledResolution sr = new ScaledResolution(mc);
        cachedScaleFactor = sr.getScaleFactor();
        panelLeft = (sr.getScaledWidth() - PANEL_WIDTH) / 2;
        panelTop = (sr.getScaledHeight() - PANEL_HEIGHT) / 2;

        int btnY = panelTop + PANEL_HEIGHT - 30;
        int btnW = 120;
        int btnSpacing = 20;
        int centerX = panelLeft + PANEL_WIDTH / 2;

        saveBtnRect  = new Rect(centerX - btnW - btnSpacing / 2, btnY, btnW, BUTTON_HEIGHT);
        resetBtnRect = new Rect(centerX - btnW / 2, btnY, btnW, BUTTON_HEIGHT);
        cancelBtnRect= new Rect(centerX + btnSpacing / 2, btnY, btnW, BUTTON_HEIGHT);

        langBtnRect = new Rect(panelLeft + PANEL_WIDTH - 50, panelTop + 8, 40, 16);

        // 计算内容高度（基于条目数）
        int entryHeight = 24;
        int listTopY = panelTop + LIST_TOP_OFFSET;
        int listBottomY = panelTop + PANEL_HEIGHT - LIST_BOTTOM_OFFSET;
        int availableHeight = listBottomY - listTopY;
        contentHeight = entries.size() * entryHeight;
        maxScrollOffset = Math.max(0, contentHeight - availableHeight);
    }

    // ────────────────────────────────────────────────────────────────
    //  条目重建 — 5 Tab × ~18 项 = ~90 项
    // ────────────────────────────────────────────────────────────────
    private void rebuildEntries() {
        entries.clear();
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;

        switch (activeTab) {
            // ═══════════════════════════════════════════════════════
            //  Tab 1: 通用 — 所有模块开关 + 位置 + 外观
            // ═══════════════════════════════════════════════════════
            case 0: {
                // ── 罗盘 ──
                entries.add(new ConfigEntryDescriptor("compass.show",
                        (BooleanSupplier) () -> cfg.showCompassHUD, (Consumer<Boolean>) (v) -> cfg.showCompassHUD = v));
                entries.add(new ConfigEntryDescriptor("compass.x",
                        (IntSupplier) () -> cfg.xPosition, (Consumer<Integer>) (v) -> cfg.xPosition = v, -200, 2000));
                entries.add(new ConfigEntryDescriptor("compass.y",
                        (IntSupplier) () -> cfg.yPosition, (Consumer<Integer>) (v) -> cfg.yPosition = v, 0, 2000));
                entries.add(new ConfigEntryDescriptor("compass.scale",
                        (DoubleSupplier) () -> (double) cfg.scale, (Consumer<Float>) (v) -> cfg.scale = v, 0.25f, 4.0f));
                entries.add(new ConfigEntryDescriptor("compass.style",
                        (java.util.function.Supplier<String>) () -> cfg.displayStyle,
                        (Consumer<String>) (v) -> cfg.displayStyle = v,
                        new String[]{"detailed", "minimal", "simple"}));
                entries.add(new ConfigEntryDescriptor("compass.degreeMarks",
                        (BooleanSupplier) () -> cfg.showDegreeMarks, (Consumer<Boolean>) (v) -> cfg.showDegreeMarks = v));
                entries.add(new ConfigEntryDescriptor("compass.needle",
                        (BooleanSupplier) () -> cfg.showCompassNeedle, (Consumer<Boolean>) (v) -> cfg.showCompassNeedle = v));
                entries.add(new ConfigEntryDescriptor("compass.interval",
                        (IntSupplier) () -> cfg.degreeMarkInterval, (Consumer<Integer>) (v) -> cfg.degreeMarkInterval = v, 5, 90));
                entries.add(new ConfigEntryDescriptor("compass.dynamicScale",
                        (BooleanSupplier) () -> cfg.dynamicScaling, (Consumer<Boolean>) (v) -> cfg.dynamicScaling = v));
                entries.add(new ConfigEntryDescriptor("compass.horizon",
                        (BooleanSupplier) () -> cfg.showHorizon, (Consumer<Boolean>) (v) -> cfg.showHorizon = v));
                entries.add(new ConfigEntryDescriptor("compass.exactAngle",
                        (BooleanSupplier) () -> cfg.showExactAngle, (Consumer<Boolean>) (v) -> cfg.showExactAngle = v));

                // ── 状态栏 ──
                entries.add(new ConfigEntryDescriptor("health.x",
                        (IntSupplier) () -> cfg.healthHudX, (Consumer<Integer>) (v) -> cfg.healthHudX = v, -500, 2000));
                entries.add(new ConfigEntryDescriptor("health.y",
                        (IntSupplier) () -> cfg.healthHudY, (Consumer<Integer>) (v) -> cfg.healthHudY = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("health.showArmor",
                        (BooleanSupplier) () -> cfg.showArmorHUD, (Consumer<Boolean>) (v) -> cfg.showArmorHUD = v));
                entries.add(new ConfigEntryDescriptor("health.showHunger",
                        (BooleanSupplier) () -> cfg.showHungerHUD, (Consumer<Boolean>) (v) -> cfg.showHungerHUD = v));
                entries.add(new ConfigEntryDescriptor("health.showHead",
                        (BooleanSupplier) () -> cfg.showPlayerHead, (Consumer<Boolean>) (v) -> cfg.showPlayerHead = v));
                entries.add(new ConfigEntryDescriptor("health.headSize",
                        (IntSupplier) () -> cfg.headSize, (Consumer<Integer>) (v) -> cfg.headSize = v, 8, 64));
                entries.add(new ConfigEntryDescriptor("health.textSpacing",
                        (IntSupplier) () -> cfg.headTextSpacing, (Consumer<Integer>) (v) -> cfg.headTextSpacing = v, 0, 20));

                // ── 距离 ──
                entries.add(new ConfigEntryDescriptor("dist.x",
                        (IntSupplier) () -> cfg.distanceHudX, (Consumer<Integer>) (v) -> cfg.distanceHudX = v, 0, 2000));
                entries.add(new ConfigEntryDescriptor("dist.y",
                        (IntSupplier) () -> cfg.distanceHudY, (Consumer<Integer>) (v) -> cfg.distanceHudY = v, 0, 2000));
                entries.add(new ConfigEntryDescriptor("dist.precision",
                        (IntSupplier) () -> cfg.distancePrecision, (Consumer<Integer>) (v) -> cfg.distancePrecision = v, 0, 5));
                entries.add(new ConfigEntryDescriptor("dist.showTarget",
                        (BooleanSupplier) () -> cfg.showTargetInfo, (Consumer<Boolean>) (v) -> cfg.showTargetInfo = v));
                entries.add(new ConfigEntryDescriptor("dist.showBlockInfo",
                        (BooleanSupplier) () -> cfg.showBlockCoordinates, (Consumer<Boolean>) (v) -> cfg.showBlockCoordinates = v));
                entries.add(new ConfigEntryDescriptor("dist.hardness",
                        (BooleanSupplier) () -> cfg.showBlockHardness, (Consumer<Boolean>) (v) -> cfg.showBlockHardness = v));
                entries.add(new ConfigEntryDescriptor("dist.showTool",
                        (BooleanSupplier) () -> cfg.showRequiredTool, (Consumer<Boolean>) (v) -> cfg.showRequiredTool = v));

                // ── 按键显示 ──
                entries.add(new ConfigEntryDescriptor("keys.x",
                        (IntSupplier) () -> cfg.keysDisplayX, (Consumer<Integer>) (v) -> cfg.keysDisplayX = v, 0, 2000));
                entries.add(new ConfigEntryDescriptor("keys.y",
                        (IntSupplier) () -> cfg.keysDisplayY, (Consumer<Integer>) (v) -> cfg.keysDisplayY = v, 0, 2000));
                entries.add(new ConfigEntryDescriptor("keys.size",
                        (IntSupplier) () -> cfg.keysSize, (Consumer<Integer>) (v) -> cfg.keysSize = v, 12, 64));
                entries.add(new ConfigEntryDescriptor("keys.spacing",
                        (IntSupplier) () -> cfg.keysSpacing, (Consumer<Integer>) (v) -> cfg.keysSpacing = v, 0, 32));
                entries.add(new ConfigEntryDescriptor("keys.scale",
                        (DoubleSupplier) () -> (double) cfg.keysScale, (Consumer<Float>) (v) -> cfg.keysScale = v, 0.25f, 4.0f));
                entries.add(new ConfigEntryDescriptor("keys.background",
                        (BooleanSupplier) () -> cfg.showKeysBackground, (Consumer<Boolean>) (v) -> cfg.showKeysBackground = v));
                entries.add(new ConfigEntryDescriptor("keys.opacity",
                        (DoubleSupplier) () -> (double) cfg.keysOpacity, (Consumer<Float>) (v) -> cfg.keysOpacity = v, 0.0f, 1.0f));
                break;
            }

            // ═══════════════════════════════════════════════════════
            //  Tab 2: 聊天 — ChromaChat 全部配置
            // ═══════════════════════════════════════════════════════
            case 1: {
                entries.add(new ConfigEntryDescriptor("module.chroma",
                        (BooleanSupplier) () -> cfg.enableChromaChat, (Consumer<Boolean>) (v) -> cfg.enableChromaChat = v));
                entries.add(new ConfigEntryDescriptor("chroma.width",
                        (IntSupplier) () -> cfg.chromaChatWidth, (Consumer<Integer>) (v) -> cfg.chromaChatWidth = v, 80, 800));
                entries.add(new ConfigEntryDescriptor("chroma.lines",
                        (IntSupplier) () -> cfg.chromaChatLineCount, (Consumer<Integer>) (v) -> cfg.chromaChatLineCount = v, 1, 50));
                entries.add(new ConfigEntryDescriptor("chroma.timestamp",
                        (BooleanSupplier) () -> cfg.chromaChatShowTimestamps, (Consumer<Boolean>) (v) -> cfg.chromaChatShowTimestamps = v));
                entries.add(new ConfigEntryDescriptor("chroma.avatar",
                        (BooleanSupplier) () -> cfg.chromaChatAvatar, (Consumer<Boolean>) (v) -> cfg.chromaChatAvatar = v));
                entries.add(new ConfigEntryDescriptor("chroma.signboard",
                        (BooleanSupplier) () -> cfg.chromaChatSignboardMerge, (Consumer<Boolean>) (v) -> cfg.chromaChatSignboardMerge = v));
                entries.add(new ConfigEntryDescriptor("chroma.maxLines",
                        (IntSupplier) () -> cfg.chromaChatMaxLines, (Consumer<Integer>) (v) -> cfg.chromaChatMaxLines = v, 20, 500));
                break;
            }

            // ═══════════════════════════════════════════════════════
            //  Tab 3: 战斗 — 准星/命中/连击/描边/高亮/RGB/方块破坏
            // ═══════════════════════════════════════════════════════
            case 2: {
                // ── 准星 ──
                entries.add(new ConfigEntryDescriptor("crosshair.style",
                        (java.util.function.Supplier<String>) () -> cfg.crosshairStyle,
                        (Consumer<String>) (v) -> cfg.crosshairStyle = v,
                        new String[]{"default", "dot", "circle", "cross", "arrow"}));
                entries.add(new ConfigEntryDescriptor("crosshair.gap",
                        (IntSupplier) () -> cfg.crosshairGap, (Consumer<Integer>) (v) -> cfg.crosshairGap = v, 0, 20));
                entries.add(new ConfigEntryDescriptor("crosshair.thickness",
                        (IntSupplier) () -> cfg.crosshairThickness, (Consumer<Integer>) (v) -> cfg.crosshairThickness = v, 1, 10));
                entries.add(new ConfigEntryDescriptor("crosshair.dot",
                        (BooleanSupplier) () -> cfg.crosshairOutline, (Consumer<Boolean>) (v) -> cfg.crosshairOutline = v));
                entries.add(new ConfigEntryDescriptor("crosshair.spread",
                        (BooleanSupplier) () -> cfg.crosshairSpread, (Consumer<Boolean>) (v) -> cfg.crosshairSpread = v));
                entries.add(new ConfigEntryDescriptor("crosshair.entityColor",
                        (BooleanSupplier) () -> cfg.crosshairEntityColor, (Consumer<Boolean>) (v) -> cfg.crosshairEntityColor = v));
                entries.add(new ConfigEntryDescriptor("crosshair.range",
                        (IntSupplier) () -> cfg.crosshairEntityRange, (Consumer<Integer>) (v) -> cfg.crosshairEntityRange = v, 1, 50));

                // ── 命中标识 ──
                entries.add(new ConfigEntryDescriptor("module.hitmarker",
                        (BooleanSupplier) () -> cfg.enableHitMarker, (Consumer<Boolean>) (v) -> cfg.enableHitMarker = v));
                entries.add(new ConfigEntryDescriptor("hitmarker.useS19",
                        (BooleanSupplier) () -> cfg.hitMarkerUseS19, (Consumer<Boolean>) (v) -> cfg.hitMarkerUseS19 = v));
                entries.add(new ConfigEntryDescriptor("hitmarker.hitSound",
                        (BooleanSupplier) () -> cfg.enableHitSounds, (Consumer<Boolean>) (v) -> cfg.enableHitSounds = v));
                entries.add(new ConfigEntryDescriptor("hitmarker.killSound",
                        (BooleanSupplier) () -> cfg.enableKillSound, (Consumer<Boolean>) (v) -> cfg.enableKillSound = v));
                entries.add(new ConfigEntryDescriptor("hitmarker.volume",
                        (DoubleSupplier) () -> (double) cfg.soundVolume, (Consumer<Float>) (v) -> cfg.soundVolume = v, 0.0f, 1.0f));
                entries.add(new ConfigEntryDescriptor("hitmarker.hitSize",
                        (IntSupplier) () -> (int) cfg.hitSize, (Consumer<Integer>) (v) -> cfg.hitSize = (float) v, 2, 32));
                entries.add(new ConfigEntryDescriptor("hitmarker.hitAlpha",
                        (DoubleSupplier) () -> (double) cfg.hitAlpha, (Consumer<Float>) (v) -> cfg.hitAlpha = v, 0.0f, 1.0f));
                entries.add(new ConfigEntryDescriptor("hitmarker.border",
                        (BooleanSupplier) () -> cfg.hitMarkerEnableBorder, (Consumer<Boolean>) (v) -> cfg.hitMarkerEnableBorder = v));
                entries.add(new ConfigEntryDescriptor("hitmarker.borderWidth",
                        (DoubleSupplier) () -> (double) cfg.hitMarkerBorderWidth, (Consumer<Float>) (v) -> cfg.hitMarkerBorderWidth = v, 0.5f, 5.0f));
                entries.add(new ConfigEntryDescriptor("hitmarker.blood",
                        (DoubleSupplier) () -> (double) cfg.hitBloodIntensity, (Consumer<Float>) (v) -> cfg.hitBloodIntensity = v, 0.0f, 1.0f));
                entries.add(new ConfigEntryDescriptor("hitmarker.randomRotate",
                        (BooleanSupplier) () -> cfg.hitMarkerRandomRotate, (Consumer<Boolean>) (v) -> cfg.hitMarkerRandomRotate = v));
                entries.add(new ConfigEntryDescriptor("hitmarker.rotateStrength",
                        (DoubleSupplier) () -> (double) cfg.hitMarkerRandomRotateStrength, (Consumer<Float>) (v) -> cfg.hitMarkerRandomRotateStrength = v, 0.0f, 90.0f));

                // ── 连击 ──
                entries.add(new ConfigEntryDescriptor("module.combo",
                        (BooleanSupplier) () -> cfg.enableCombo, (Consumer<Boolean>) (v) -> cfg.enableCombo = v));
                entries.add(new ConfigEntryDescriptor("combo.xOffset",
                        (IntSupplier) () -> cfg.comboXOffset, (Consumer<Integer>) (v) -> cfg.comboXOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("combo.yOffset",
                        (IntSupplier) () -> cfg.comboYOffset, (Consumer<Integer>) (v) -> cfg.comboYOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("combo.scale",
                        (DoubleSupplier) () -> (double) cfg.comboScale, (Consumer<Float>) (v) -> cfg.comboScale = v, 0.25f, 4.0f));

                // ── 目标血量 ──
                entries.add(new ConfigEntryDescriptor("module.target",
                        (BooleanSupplier) () -> cfg.enableTargetHealth, (Consumer<Boolean>) (v) -> cfg.enableTargetHealth = v));

                // ── 方块描边 ──
                entries.add(new ConfigEntryDescriptor("blockOutline.enable",
                        (BooleanSupplier) () -> cfg.enableBlockHighlight, (Consumer<Boolean>) (v) -> cfg.enableBlockHighlight = v));
                entries.add(new ConfigEntryDescriptor("blockOutline.width",
                        (DoubleSupplier) () -> (double) cfg.blockOutlineWidth, (Consumer<Float>) (v) -> cfg.blockOutlineWidth = v, 1.0f, 8.0f));
                entries.add(new ConfigEntryDescriptor("blockOutline.alpha",
                        (IntSupplier) () -> cfg.blockOutlineAlpha, (Consumer<Integer>) (v) -> cfg.blockOutlineAlpha = v, 0, 255));
                entries.add(new ConfigEntryDescriptor("blockOutline.visibleOnly",
                        (BooleanSupplier) () -> cfg.drawVisibleFacesOnlyBlocks, (Consumer<Boolean>) (v) -> cfg.drawVisibleFacesOnlyBlocks = v));

                // ── 实体高亮 ──
                entries.add(new ConfigEntryDescriptor("entityHL.enable",
                        (BooleanSupplier) () -> cfg.enableEntityHighlight, (Consumer<Boolean>) (v) -> cfg.enableEntityHighlight = v));
                entries.add(new ConfigEntryDescriptor("entityHL.width",
                        (DoubleSupplier) () -> (double) cfg.entityOutlineWidth, (Consumer<Float>) (v) -> cfg.entityOutlineWidth = v, 1.0f, 8.0f));
                entries.add(new ConfigEntryDescriptor("entityHL.alpha",
                        (IntSupplier) () -> cfg.entityOutlineAlpha, (Consumer<Integer>) (v) -> cfg.entityOutlineAlpha = v, 0, 255));

                // ── RGB 流光 ──
                entries.add(new ConfigEntryDescriptor("rgb.enable",
                        (BooleanSupplier) () -> cfg.enableRGBMode, (Consumer<Boolean>) (v) -> cfg.enableRGBMode = v));
                entries.add(new ConfigEntryDescriptor("rgb.speed",
                        (IntSupplier) () -> cfg.rgbSpeed, (Consumer<Integer>) (v) -> cfg.rgbSpeed = v, 50, 5000));
                entries.add(new ConfigEntryDescriptor("rgb.mode",
                        (java.util.function.Supplier<String>) () -> cfg.rgbFlowMode,
                        (Consumer<String>) (v) -> cfg.rgbFlowMode = v,
                        new String[]{"perimeter", "uniform"}));
                entries.add(new ConfigEntryDescriptor("rgb.algorithm",
                        (java.util.function.Supplier<String>) () -> cfg.rgbColorAlgo,
                        (Consumer<String>) (v) -> cfg.rgbColorAlgo = v,
                        new String[]{"hsv", "sinewave"}));

                // ── 方块破坏 ──
                entries.add(new ConfigEntryDescriptor("blockBreak.enable",
                        (BooleanSupplier) () -> cfg.enableBlockBreakIndicator, (Consumer<Boolean>) (v) -> cfg.enableBlockBreakIndicator = v));
                entries.add(new ConfigEntryDescriptor("blockBreak.x",
                        (IntSupplier) () -> cfg.blockBreakIndicatorX, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorX = v, -200, 200));
                entries.add(new ConfigEntryDescriptor("blockBreak.y",
                        (IntSupplier) () -> cfg.blockBreakIndicatorY, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorY = v, 0, 200));
                entries.add(new ConfigEntryDescriptor("blockBreak.width",
                        (IntSupplier) () -> cfg.blockBreakIndicatorWidth, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorWidth = v, 10, 200));
                entries.add(new ConfigEntryDescriptor("blockBreak.height",
                        (IntSupplier) () -> cfg.blockBreakIndicatorHeight, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorHeight = v, 2, 20));
                entries.add(new ConfigEntryDescriptor("blockBreak.showPercent",
                        (BooleanSupplier) () -> cfg.blockBreakIndicatorShowPercent, (Consumer<Boolean>) (v) -> cfg.blockBreakIndicatorShowPercent = v));
                break;
            }

            // ═══════════════════════════════════════════════════════
            //  Tab 4: 性能 — 药水/装备/危机/经验条/Boss/快捷栏/调试信息
            // ═══════════════════════════════════════════════════════
            case 3: {
                entries.add(new ConfigEntryDescriptor("module.performance",
                        (BooleanSupplier) () -> cfg.enablePerformanceHUD, (Consumer<Boolean>) (v) -> cfg.enablePerformanceHUD = v));
                entries.add(new ConfigEntryDescriptor("module.potion",
                        (BooleanSupplier) () -> cfg.enablePotionHUD, (Consumer<Boolean>) (v) -> cfg.enablePotionHUD = v));
                entries.add(new ConfigEntryDescriptor("module.potiontimer",
                        (BooleanSupplier) () -> cfg.enablePotionTimer, (Consumer<Boolean>) (v) -> cfg.enablePotionTimer = v));
                entries.add(new ConfigEntryDescriptor("potion.xOffset",
                        (IntSupplier) () -> cfg.potionXOffset, (Consumer<Integer>) (v) -> cfg.potionXOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("potion.yOffset",
                        (IntSupplier) () -> cfg.potionYOffset, (Consumer<Integer>) (v) -> cfg.potionYOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("potionTimer.xOffset",
                        (IntSupplier) () -> cfg.potionTimerXOffset, (Consumer<Integer>) (v) -> cfg.potionTimerXOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("potionTimer.yOffset",
                        (IntSupplier) () -> cfg.potionTimerYOffset, (Consumer<Integer>) (v) -> cfg.potionTimerYOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("potionTimer.iconSize",
                        (IntSupplier) () -> cfg.potionTimerIconSize, (Consumer<Integer>) (v) -> cfg.potionTimerIconSize = v, 16, 64));
                entries.add(new ConfigEntryDescriptor("module.equip",
                        (BooleanSupplier) () -> cfg.enableArmorHUD, (Consumer<Boolean>) (v) -> cfg.enableArmorHUD = v));
                entries.add(new ConfigEntryDescriptor("module.crisis",
                        (BooleanSupplier) () -> cfg.enableCrisisWarning, (Consumer<Boolean>) (v) -> cfg.enableCrisisWarning = v));
                entries.add(new ConfigEntryDescriptor("crisis.warnHealth",
                        (BooleanSupplier) () -> cfg.crisisWarnHealth, (Consumer<Boolean>) (v) -> cfg.crisisWarnHealth = v));
                entries.add(new ConfigEntryDescriptor("crisis.healthThreshold",
                        (DoubleSupplier) () -> cfg.crisisHealthThreshold, (Consumer<Float>) (v) -> cfg.crisisHealthThreshold = v, 0.5f, 20.0f));
                entries.add(new ConfigEntryDescriptor("crisis.warnHunger",
                        (BooleanSupplier) () -> cfg.crisisWarnHunger, (Consumer<Boolean>) (v) -> cfg.crisisWarnHunger = v));
                entries.add(new ConfigEntryDescriptor("crisis.hungerThreshold",
                        (IntSupplier) () -> cfg.crisisHungerThreshold, (Consumer<Integer>) (v) -> cfg.crisisHungerThreshold = v, 1, 20));
                entries.add(new ConfigEntryDescriptor("crisis.warnTnt",
                        (BooleanSupplier) () -> cfg.crisisWarnTnt, (Consumer<Boolean>) (v) -> cfg.crisisWarnTnt = v));
                entries.add(new ConfigEntryDescriptor("crisis.tntRadius",
                        (DoubleSupplier) () -> cfg.crisisTntRadius, (Consumer<Float>) (v) -> cfg.crisisTntRadius = v, 2.0f, 50.0f));
                entries.add(new ConfigEntryDescriptor("crisis.xOffset",
                        (IntSupplier) () -> cfg.crisisXOffset, (Consumer<Integer>) (v) -> cfg.crisisXOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("crisis.yOffset",
                        (IntSupplier) () -> cfg.crisisYOffset, (Consumer<Integer>) (v) -> cfg.crisisYOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("crisis.iconSize",
                        (IntSupplier) () -> cfg.crisisIconSize, (Consumer<Integer>) (v) -> cfg.crisisIconSize = v, 16, 128));

                // ── P0/P1 模块 ──
                entries.add(new ConfigEntryDescriptor("module.expbar",
                        (BooleanSupplier) () -> cfg.enableXpBarHUD, (Consumer<Boolean>) (v) -> cfg.enableXpBarHUD = v));
                entries.add(new ConfigEntryDescriptor("xpbar.offsetX",
                        (IntSupplier) () -> cfg.xpBarXOffset, (Consumer<Integer>) (v) -> cfg.xpBarXOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("xpbar.offsetY",
                        (IntSupplier) () -> cfg.xpBarYOffset, (Consumer<Integer>) (v) -> cfg.xpBarYOffset = v, -500, 500));
                entries.add(new ConfigEntryDescriptor("xpbar.width",
                        (IntSupplier) () -> cfg.xpBarWidth, (Consumer<Integer>) (v) -> cfg.xpBarWidth = v, 80, 400));
                entries.add(new ConfigEntryDescriptor("xpbar.height",
                        (IntSupplier) () -> cfg.xpBarHeight, (Consumer<Integer>) (v) -> cfg.xpBarHeight = v, 3, 20));
                entries.add(new ConfigEntryDescriptor("module.bossHP",
                        (BooleanSupplier) () -> cfg.enableBossHealthHUD, (Consumer<Boolean>) (v) -> cfg.enableBossHealthHUD = v));
                entries.add(new ConfigEntryDescriptor("module.hotbar",
                        (BooleanSupplier) () -> cfg.enableHotbarHUD, (Consumer<Boolean>) (v) -> cfg.enableHotbarHUD = v));
                entries.add(new ConfigEntryDescriptor("hotbar.offsetY",
                        (IntSupplier) () -> cfg.hotbarYOffset, (Consumer<Integer>) (v) -> cfg.hotbarYOffset = v, -200, 200));
                entries.add(new ConfigEntryDescriptor("module.tablist",
                        (BooleanSupplier) () -> cfg.enableTabListHUD, (Consumer<Boolean>) (v) -> cfg.enableTabListHUD = v));
                entries.add(new ConfigEntryDescriptor("module.debug",
                        (BooleanSupplier) () -> cfg.enableDebugInfoHUD, (Consumer<Boolean>) (v) -> cfg.enableDebugInfoHUD = v));
                entries.add(new ConfigEntryDescriptor("debug.xOffset",
                        (IntSupplier) () -> cfg.debugInfoXOffset, (Consumer<Integer>) (v) -> cfg.debugInfoXOffset = v, 0, 2000));
                entries.add(new ConfigEntryDescriptor("debug.yOffset",
                        (IntSupplier) () -> cfg.debugInfoYOffset, (Consumer<Integer>) (v) -> cfg.debugInfoYOffset = v, 0, 2000));

                // ── P2 新增模块 ──
                entries.add(new ConfigEntryDescriptor("module.jumpbar",
                        (BooleanSupplier) () -> cfg.enableJumpBar, (Consumer<Boolean>) (v) -> cfg.enableJumpBar = v));
                entries.add(new ConfigEntryDescriptor("module.airhud",
                        (BooleanSupplier) () -> cfg.enableAirHUD, (Consumer<Boolean>) (v) -> cfg.enableAirHUD = v));
                entries.add(new ConfigEntryDescriptor("module.mountHP",
                        (BooleanSupplier) () -> cfg.enableMountHP, (Consumer<Boolean>) (v) -> cfg.enableMountHP = v));
                break;
            }

            // ═══════════════════════════════════════════════════════
            //  Tab 5: 颜色 — 所有可配置颜色汇总
            // ═══════════════════════════════════════════════════════
            case 4: {
                // ── 状态栏颜色 ──
                entries.add(new ConfigEntryDescriptor("color.healthSafe",
                        (IntSupplier) () -> cfg.healthColorSafe, (Consumer<Integer>) (v) -> cfg.healthColorSafe = v, true));
                entries.add(new ConfigEntryDescriptor("color.healthWarning",
                        (IntSupplier) () -> cfg.healthColorWarning, (Consumer<Integer>) (v) -> cfg.healthColorWarning = v, true));
                entries.add(new ConfigEntryDescriptor("color.healthDanger",
                        (IntSupplier) () -> cfg.healthColorDanger, (Consumer<Integer>) (v) -> cfg.healthColorDanger = v, true));
                entries.add(new ConfigEntryDescriptor("color.armor",
                        (IntSupplier) () -> cfg.armorColor, (Consumer<Integer>) (v) -> cfg.armorColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.hunger",
                        (IntSupplier) () -> cfg.hungerColor, (Consumer<Integer>) (v) -> cfg.hungerColor = v, true));

                // ── 罗盘 / 距离 / 按键 ──
                entries.add(new ConfigEntryDescriptor("color.compass",
                        (IntSupplier) () -> cfg.compassColor, (Consumer<Integer>) (v) -> cfg.compassColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.distance",
                        (IntSupplier) () -> cfg.distanceColor, (Consumer<Integer>) (v) -> cfg.distanceColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.targetInfo",
                        (IntSupplier) () -> cfg.targetInfoColor, (Consumer<Integer>) (v) -> cfg.targetInfoColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.blockInfo",
                        (IntSupplier) () -> cfg.blockInfoColor, (Consumer<Integer>) (v) -> cfg.blockInfoColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.keysActive",
                        (IntSupplier) () -> cfg.keysActiveColor, (Consumer<Integer>) (v) -> cfg.keysActiveColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.keysInactive",
                        (IntSupplier) () -> cfg.keysInactiveColor, (Consumer<Integer>) (v) -> cfg.keysInactiveColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.keysText",
                        (IntSupplier) () -> cfg.keysTextColor, (Consumer<Integer>) (v) -> cfg.keysTextColor = v, true));

                // ── 药水 / 描边 / 实体 ──
                entries.add(new ConfigEntryDescriptor("color.potionText",
                        (IntSupplier) () -> cfg.potionTextColor, (Consumer<Integer>) (v) -> cfg.potionTextColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.blockOutline",
                        (IntSupplier) () -> cfg.blockOutlineColor, (Consumer<Integer>) (v) -> cfg.blockOutlineColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.entityHostile",
                        (IntSupplier) () -> cfg.entityOutlineColorHostile, (Consumer<Integer>) (v) -> cfg.entityOutlineColorHostile = v, true));
                entries.add(new ConfigEntryDescriptor("color.entityNeutral",
                        (IntSupplier) () -> cfg.entityOutlineColorNeutral, (Consumer<Integer>) (v) -> cfg.entityOutlineColorNeutral = v, true));
                entries.add(new ConfigEntryDescriptor("color.entityFriendly",
                        (IntSupplier) () -> cfg.entityOutlineColorFriendly, (Consumer<Integer>) (v) -> cfg.entityOutlineColorFriendly = v, true));

                // ── 准星颜色 ──
                entries.add(new ConfigEntryDescriptor("color.crosshair",
                        (IntSupplier) () -> cfg.crosshairColor, (Consumer<Integer>) (v) -> cfg.crosshairColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.crosshairPlayer",
                        (IntSupplier) () -> cfg.crosshairColorPlayer, (Consumer<Integer>) (v) -> cfg.crosshairColorPlayer = v, true));
                entries.add(new ConfigEntryDescriptor("color.crosshairHostile",
                        (IntSupplier) () -> cfg.crosshairColorHostile, (Consumer<Integer>) (v) -> cfg.crosshairColorHostile = v, true));
                entries.add(new ConfigEntryDescriptor("color.crosshairPassive",
                        (IntSupplier) () -> cfg.crosshairColorPassive, (Consumer<Integer>) (v) -> cfg.crosshairColorPassive = v, true));
                entries.add(new ConfigEntryDescriptor("color.crosshairNeutral",
                        (IntSupplier) () -> cfg.crosshairColorNeutral, (Consumer<Integer>) (v) -> cfg.crosshairColorNeutral = v, true));
                entries.add(new ConfigEntryDescriptor("color.crosshairOther",
                        (IntSupplier) () -> cfg.crosshairColorOther, (Consumer<Integer>) (v) -> cfg.crosshairColorOther = v, true));

                // ── 命中标识颜色 ──
                entries.add(new ConfigEntryDescriptor("color.hitMarker",
                        (IntSupplier) () -> cfg.hitColor, (Consumer<Integer>) (v) -> cfg.hitColor = v, true));
                entries.add(new ConfigEntryDescriptor("color.killMarker",
                        (IntSupplier) () -> cfg.killColor, (Consumer<Integer>) (v) -> cfg.killColor = v, true));

                // ── 方块破坏颜色 ──
                entries.add(new ConfigEntryDescriptor("color.breakStart",
                        (IntSupplier) () -> cfg.blockBreakIndicatorColorStart, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorColorStart = v, true));
                entries.add(new ConfigEntryDescriptor("color.breakEnd",
                        (IntSupplier) () -> cfg.blockBreakIndicatorColorEnd, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorColorEnd = v, true));
                break;
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  绘制
    // ────────────────────────────────────────────────────────────────
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. 半透明深色背景
        drawRect(0, 0, width, height, COLOR_BG);

        // 2. 白色半透明面板居中
        drawRect(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, COLOR_PANEL_BG);

        // 3. 顶部标题
        String title = LangManager.get("mod.name");
        mc.fontRendererObj.drawString(title, panelLeft + 10, panelTop + 10, COLOR_TEXT_DARK, false);

        // 4. 语言切换按钮 [中/EN]
        String langLabel = LangManager.isChinese() ? "中" : "EN";
        boolean langHover = langBtnRect.contains(mouseX, mouseY);
        drawButton(langBtnRect, langLabel, langHover ? COLOR_TAB_HOVER : COLOR_TAB_ACTIVE, COLOR_TEXT_LIGHT);

        // 5. Tab 行
        int tabStartX = panelLeft + 10;
        int tabWidth = (PANEL_WIDTH - 20) / tabKeys.length;
        for (int i = 0; i < tabKeys.length; i++) {
            int tx = tabStartX + i * tabWidth;
            int ty = panelTop + 30;
            boolean hover = mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= ty && mouseY <= ty + TAB_HEIGHT;
            int tabColor;
            if (i == activeTab) {
                tabColor = COLOR_TAB_ACTIVE;
            } else if (hover) {
                tabColor = COLOR_TAB_HOVER;
            } else {
                tabColor = COLOR_TAB_INACTIVE;
            }
            drawRect(tx, ty, tx + tabWidth, ty + TAB_HEIGHT, tabColor);
            String tabLabel = LangManager.get(tabKeys[i]);
            int textW = mc.fontRendererObj.getStringWidth(tabLabel);
            mc.fontRendererObj.drawString(tabLabel,
                    tx + (tabWidth - textW) / 2,
                    ty + (TAB_HEIGHT - mc.fontRendererObj.FONT_HEIGHT) / 2,
                    COLOR_TEXT_LIGHT, false);
        }

        // 5.5 搜索框
        int searchBoxY = panelTop + TAB_HEIGHT + 30;
        int listLeft_tmp = panelLeft + 15;
        int listRight_tmp = panelLeft + PANEL_WIDTH - 15;
        int searchBoxW = (listRight_tmp - listLeft_tmp) / 2;
        int searchBoxX = listLeft_tmp + (listRight_tmp - listLeft_tmp - searchBoxW) / 2;
        int searchBoxH = 16;
        drawRect(searchBoxX, searchBoxY, searchBoxX + searchBoxW, searchBoxY + searchBoxH, 0xFF333333);
        drawRect(searchBoxX + 1, searchBoxY + 1, searchBoxX + searchBoxW - 1, searchBoxY + searchBoxH - 1, 0xFF222222);

        String searchDisplay = searchText.isEmpty() ? "🔍 " + LangManager.get("common.search") : searchText;
        mc.fontRendererObj.drawString(searchDisplay, searchBoxX + 4, searchBoxY + (searchBoxH - mc.fontRendererObj.FONT_HEIGHT) / 2,
            searchText.isEmpty() ? 0xFF666666 : 0xFFFFFFFF, false);

        // 搜索框聚焦指示
        if (isSearching) {
            drawRect(searchBoxX + 2 + mc.fontRendererObj.getStringWidth(searchDisplay), searchBoxY + 2,
                searchBoxX + 2 + mc.fontRendererObj.getStringWidth(searchDisplay) + 1, searchBoxY + searchBoxH - 2, 0xFFFFFFFF);
        }

        // 6. 配置项列表（滚动区域裁剪）
        int listLeft = panelLeft + 15;
        int listTopY = panelTop + LIST_TOP_OFFSET;
        int listRight = panelLeft + PANEL_WIDTH - 15;
        int listBottomY = panelTop + PANEL_HEIGHT - LIST_BOTTOM_OFFSET;
        int entryHeight = 24;

        // 裁剪区域
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scissorLeft = listLeft * cachedScaleFactor;
        int scissorTop = (height - listBottomY) * cachedScaleFactor;
        int scissorW = (listRight - listLeft) * cachedScaleFactor;
        int scissorH = (listBottomY - listTopY) * cachedScaleFactor;
        GL11.glScissor(scissorLeft, scissorTop, scissorW, scissorH);

        int hoveredIdx = -1;
        int hoveredMX = 0, hoveredMY = 0;

        int visibleIdx = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (!searchMatches(entries.get(i))) continue;
            int ey = listTopY + visibleIdx * entryHeight - scrollOffset;
            visibleIdx++;
            if (ey + entryHeight < listTopY || ey > listBottomY) continue;

            boolean hover = mouseX >= listLeft && mouseX <= listRight && mouseY >= ey && mouseY <= ey + entryHeight;

            // 行背景 + 脉冲高亮
            if (hover) {
                drawRect(listLeft, ey, listRight, ey + entryHeight, COLOR_ENTRY_BG_HOVER);
                hoveredIdx = i;
                hoveredMX = mouseX;
                hoveredMY = mouseY;
            }

            ConfigEntryDescriptor entry = entries.get(i);
            String label = LangManager.get(entry.labelKey);
            mc.fontRendererObj.drawString(label, listLeft + 5, ey + (entryHeight - mc.fontRendererObj.FONT_HEIGHT) / 2, COLOR_TEXT_DARK, false);

            // 脉冲闪烁（值变化后 600ms 淡出）
            if (entry.labelKey.equals(pulseLabel) && System.currentTimeMillis() < pulseEndTime) {
                float remaining = (pulseEndTime - System.currentTimeMillis()) / 600.0f;
                int pulseColor = (Math.round(remaining * 50 + 10) << 24) | 0xFFFF88;
                drawRect(listLeft, ey, listRight, ey + entryHeight, pulseColor);
            }

            int valueX = listRight - 100;
            switch (entry.type) {
                case BOOLEAN:
                    drawBooleanToggle(valueX, ey, entryHeight, entry, mouseX, mouseY);
                    break;
                case INT:
                    drawIntSpinner(valueX, ey, entryHeight, entry, mouseX, mouseY);
                    break;
                case FLOAT:
                    drawFloatSpinner(valueX, ey, entryHeight, entry, mouseX, mouseY);
                    break;
                case COLOR:
                    drawColorPreview(valueX, ey, entryHeight, entry, mouseX, mouseY);
                    break;
                case ENUM:
                    drawEnumSpinner(valueX, ey, entryHeight, entry, mouseX, mouseY);
                    break;
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // ── Tooltip ──
        if (hoveredIdx >= 0) {
            ConfigEntryDescriptor tipEntry = entries.get(hoveredIdx);
            String tipKey = tipEntry.labelKey + ".tooltip";
            String tipText = LangManager.get(tipKey);
            if (!tipText.equals(tipKey)) {
                // 限制 tooltip 不超过面板右边界
                int tw = mc.fontRendererObj.getStringWidth(tipText);
                int tipX = Math.min(hoveredMX + 12, panelLeft + PANEL_WIDTH - tw - 8);
                int tipY = Math.max(hoveredMY - 12, panelTop + 5);
                int pad = 4;
                int b = 1; // border width
                drawRect(tipX - pad - b, tipY - pad - b, tipX + tw + pad + b, tipY + mc.fontRendererObj.FONT_HEIGHT + pad + b, 0xFF555555);
                drawRect(tipX - pad, tipY - pad, tipX + tw + pad, tipY + mc.fontRendererObj.FONT_HEIGHT + pad, 0xCC222222);
                mc.fontRendererObj.drawString(tipText, tipX, tipY, 0xFFFFFFCC, false);
            }
        }

        // 7. 滚动条（基于可见条目数重新计算）
        int visibleCount = 0;
        for (ConfigEntryDescriptor e : entries) { if (searchMatches(e)) visibleCount++; }
        int effectiveContentHeight = visibleCount * entryHeight;
        int effectiveMaxScroll = Math.max(0, effectiveContentHeight - (listBottomY - listTopY));

        int scrollBarLeft = listRight - 6;
        int scrollBarTop = listTopY;
        int scrollBarHeight = listBottomY - listTopY;
        drawRect(scrollBarLeft, scrollBarTop, scrollBarLeft + 4, scrollBarTop + scrollBarHeight, COLOR_SCROLLBAR_BG);
        if (effectiveMaxScroll > 0) {
            float thumbRatio = (float) scrollBarHeight / (scrollBarHeight + effectiveContentHeight);
            int thumbHeight = Math.max(10, (int) (scrollBarHeight * thumbRatio));
            int thumbOffset = (int) ((float) scrollOffset / effectiveMaxScroll * (scrollBarHeight - thumbHeight));
            drawRect(scrollBarLeft, scrollBarTop + thumbOffset, scrollBarLeft + 4, scrollBarTop + thumbOffset + thumbHeight, COLOR_SCROLLBAR);
        }

        // 8. 底部按钮
        boolean saveHover = saveBtnRect.contains(mouseX, mouseY);
        boolean resetHover = resetBtnRect.contains(mouseX, mouseY);
        boolean cancelHover = cancelBtnRect.contains(mouseX, mouseY);

        drawButton(saveBtnRect, LangManager.get("common.save"),
                saveHover ? COLOR_SAVE_HOVER : COLOR_SAVE_BUTTON, COLOR_TEXT_LIGHT);
        drawButton(resetBtnRect, LangManager.get("common.reset"),
                resetHover ? COLOR_RESET_HOVER : COLOR_RESET_BUTTON, COLOR_TEXT_LIGHT);
        drawButton(cancelBtnRect, LangManager.get("common.cancel"),
                cancelHover ? COLOR_BUTTON_HOVER : COLOR_BUTTON_NORMAL, COLOR_TEXT_LIGHT);
    }

    // ────────────────────────────────────────────────────────────────
    //  绘制辅助
    // ────────────────────────────────────────────────────────────────
    private void drawButton(Rect r, String label, int bgColor, int textColor) {
        drawRect(r.x, r.y, r.x + r.w, r.y + r.h, bgColor);
        int textW = mc.fontRendererObj.getStringWidth(label);
        mc.fontRendererObj.drawString(label,
                r.x + (r.w - textW) / 2,
                r.y + (r.h - mc.fontRendererObj.FONT_HEIGHT) / 2,
                textColor, false);
    }

    // ── Boolean 开关 ──
    private void drawBooleanToggle(int x, int y, int h, ConfigEntryDescriptor entry, int mouseX, int mouseY) {
        boolean value = Boolean.parseBoolean(entry.getter.get());
        int toggleW = 40;
        int toggleH = 14;
        int tx = x - toggleW;
        int ty = y + (h - toggleH) / 2;

        // 背景
        int bgColor = value ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        drawRect(tx, ty, tx + toggleW, ty + toggleH, bgColor);

        // 滑块
        int knobSize = toggleH - 4;
        int knobX = value ? tx + toggleW - knobSize - 2 : tx + 2;
        drawRect(knobX, ty + 2, knobX + knobSize, ty + 2 + knobSize, 0xFFFFFFFF);

        // 文字
        String label = LangManager.get(value ? "common.on" : "common.off");
        int labelX = tx - mc.fontRendererObj.getStringWidth(label) - 4;
        mc.fontRendererObj.drawString(label, labelX, y + (h - mc.fontRendererObj.FONT_HEIGHT) / 2, COLOR_VALUE_TEXT, false);
    }

    // ── Int 滚轮 ──
    private void drawIntSpinner(int x, int y, int h, ConfigEntryDescriptor entry, int mouseX, int mouseY) {
        int value = Integer.parseInt(entry.getter.get());
        int arrowW = 16;
        int arrowH = 16;
        int ay = y + (h - arrowH) / 2;

        // 左箭头
        int lx = x - 80;
        boolean lHover = mouseX >= lx && mouseX <= lx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;
        drawRect(lx, ay, lx + arrowW, ay + arrowH, lHover ? COLOR_ARROW_HOVER : COLOR_ARROW_BG);
        int arrowCenterX = lx + arrowW / 2;
        int arrowCenterY = ay + arrowH / 2;
        mc.fontRendererObj.drawString("\u25C0", arrowCenterX - mc.fontRendererObj.getStringWidth("\u25C0") / 2,
                arrowCenterY - mc.fontRendererObj.FONT_HEIGHT / 2, COLOR_TEXT_LIGHT, false);

        // 数值
        String valStr = String.valueOf(value);
        int valX = lx + arrowW + 5;
        mc.fontRendererObj.drawString(valStr, valX, y + (h - mc.fontRendererObj.FONT_HEIGHT) / 2, COLOR_VALUE_TEXT, false);

        // 右箭头
        int rx = valX + mc.fontRendererObj.getStringWidth(valStr) + 5;
        boolean rHover = mouseX >= rx && mouseX <= rx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;
        drawRect(rx, ay, rx + arrowW, ay + arrowH, rHover ? COLOR_ARROW_HOVER : COLOR_ARROW_BG);
        mc.fontRendererObj.drawString("\u25B6", rx + arrowW / 2 - mc.fontRendererObj.getStringWidth("\u25B6") / 2,
                arrowCenterY - mc.fontRendererObj.FONT_HEIGHT / 2, COLOR_TEXT_LIGHT, false);
    }

    // ── Color 色块预览 ──
    private void drawColorPreview(int x, int y, int h, ConfigEntryDescriptor entry, int mouseX, int mouseY) {
        String str = entry.getter.get(); // e.g. "#FF4444CC"
        int sw = mc.fontRendererObj.getStringWidth(str);
        int textRight = x - 5;
        int textX = textRight - sw;
        mc.fontRendererObj.drawString(str, textX, y + (h - mc.fontRendererObj.FONT_HEIGHT) / 2, 0xFF666666, false);

        // 色块 (16x16)
        int blockSize = 16;
        int blockX = textRight + 4;
        int blockY = y + (h - blockSize) / 2;

        // 解析颜色
        int color = 0xFFFFFFFF;
        try {
            String hex = str.replace("#", "");
            if (hex.length() >= 6) {
                color = (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ignored) {}
        // 确保 Alpha 不透明（预览用）
        color = 0xFF000000 | (color & 0x00FFFFFF);

        drawRect(blockX, blockY, blockX + blockSize, blockY + blockSize, color);
        drawRect(blockX, blockY, blockX + blockSize, blockY + blockSize, 0xFF888888);
    }

    // ── Enum 滚轮 ──
    private void drawEnumSpinner(int x, int y, int h, ConfigEntryDescriptor entry, int mouseX, int mouseY) {
        String current = entry.getter.get();
        int arrowW = 16, arrowH = 16;
        int ay = y + (h - arrowH) / 2;

        // 左箭头
        int lx = x - 80;
        boolean lHover = mouseX >= lx && mouseX <= lx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;
        drawRect(lx, ay, lx + arrowW, ay + arrowH, lHover ? COLOR_ARROW_HOVER : COLOR_ARROW_BG);
        int ax = lx + arrowW / 2;
        mc.fontRendererObj.drawString("\u25C0", ax - mc.fontRendererObj.getStringWidth("\u25C0") / 2,
                ay + (arrowH - mc.fontRendererObj.FONT_HEIGHT) / 2, COLOR_TEXT_LIGHT, false);

        // 当前值
        int valX = lx + arrowW + 5;
        mc.fontRendererObj.drawString(current, valX, y + (h - mc.fontRendererObj.FONT_HEIGHT) / 2, COLOR_VALUE_TEXT, false);

        // 右箭头
        int rx = valX + mc.fontRendererObj.getStringWidth(current) + 5;
        boolean rHover = mouseX >= rx && mouseX <= rx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;
        drawRect(rx, ay, rx + arrowW, ay + arrowH, rHover ? COLOR_ARROW_HOVER : COLOR_ARROW_BG);
        mc.fontRendererObj.drawString("\u25B6", rx + arrowW / 2 - mc.fontRendererObj.getStringWidth("\u25B6") / 2,
                ay + (arrowH - mc.fontRendererObj.FONT_HEIGHT) / 2, COLOR_TEXT_LIGHT, false);
    }

    // ── Float 滚轮 ──
    private void drawFloatSpinner(int x, int y, int h, ConfigEntryDescriptor entry, int mouseX, int mouseY) {
        double value = Double.parseDouble(entry.getter.get());
        int arrowW = 16;
        int arrowH = 16;
        int ay = y + (h - arrowH) / 2;

        // 左箭头
        int lx = x - 80;
        boolean lHover = mouseX >= lx && mouseX <= lx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;
        drawRect(lx, ay, lx + arrowW, ay + arrowH, lHover ? COLOR_ARROW_HOVER : COLOR_ARROW_BG);
        int arrowCenterX = lx + arrowW / 2;
        int arrowCenterY = ay + arrowH / 2;
        mc.fontRendererObj.drawString("\u25C0", arrowCenterX - mc.fontRendererObj.getStringWidth("\u25C0") / 2,
                arrowCenterY - mc.fontRendererObj.FONT_HEIGHT / 2, COLOR_TEXT_LIGHT, false);

        // 数值
        String valStr = String.format("%.1f", value);
        int valX = lx + arrowW + 5;
        mc.fontRendererObj.drawString(valStr, valX, y + (h - mc.fontRendererObj.FONT_HEIGHT) / 2, COLOR_VALUE_TEXT, false);

        // 右箭头
        int rx = valX + mc.fontRendererObj.getStringWidth(valStr) + 5;
        boolean rHover = mouseX >= rx && mouseX <= rx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;
        drawRect(rx, ay, rx + arrowW, ay + arrowH, rHover ? COLOR_ARROW_HOVER : COLOR_ARROW_BG);
        mc.fontRendererObj.drawString("\u25B6", rx + arrowW / 2 - mc.fontRendererObj.getStringWidth("\u25B6") / 2,
                arrowCenterY - mc.fontRendererObj.FONT_HEIGHT / 2, COLOR_TEXT_LIGHT, false);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        // 语言切换
        if (langBtnRect.contains(mouseX, mouseY)) {
            LangManager.toggle();
            return;
        }

        // 搜索框点击
        int searchBoxY_mc = panelTop + TAB_HEIGHT + 30;
        int searchBoxW_mc = (panelLeft + PANEL_WIDTH - 15 - panelLeft - 15) / 2;
        int searchBoxX_mc = (panelLeft + 15) + (panelLeft + PANEL_WIDTH - 15 - panelLeft - 15 - searchBoxW_mc) / 2;
        int searchBoxH_mc = 16;
        if (mouseX >= searchBoxX_mc && mouseX <= searchBoxX_mc + searchBoxW_mc &&
            mouseY >= searchBoxY_mc && mouseY <= searchBoxY_mc + searchBoxH_mc) {
            isSearching = true;
            return;
        } else {
            isSearching = false;
        }

        // Tab 点击
        int tabStartX = panelLeft + 10;
        int tabWidth = (PANEL_WIDTH - 20) / tabKeys.length;
        for (int i = 0; i < tabKeys.length; i++) {
            int tx = tabStartX + i * tabWidth;
            int ty = panelTop + 30;
            if (mouseX >= tx && mouseX <= tx + tabWidth && mouseY >= ty && mouseY <= ty + TAB_HEIGHT) {
                activeTab = i;
                scrollOffset = 0;
                rebuildEntries();
                return;
            }
        }

        // 按钮
        if (saveBtnRect.contains(mouseX, mouseY)) {
            BetterPlayerHUD.config.saveConfig();
            mc.displayGuiScreen(parentScreen);
            return;
        }
        if (resetBtnRect.contains(mouseX, mouseY)) {
            BetterPlayerHUD.config.loadConfig();
            rebuildEntries();
            return;
        }
        if (cancelBtnRect.contains(mouseX, mouseY)) {
            mc.displayGuiScreen(parentScreen);
            return;
        }

        // 配置项交互（点击 boolean toggle / int/float arrows）
        int listLeft = panelLeft + 15;
        int listTopY = panelTop + LIST_TOP_OFFSET;
        int listRight = panelLeft + PANEL_WIDTH - 15;
        int entryHeight = 24;

        int hoveredIdx = -1;
        int hoveredMX = 0, hoveredMY = 0;

        int visibleIdx_mc = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (!searchMatches(entries.get(i))) continue;
            int ey = listTopY + visibleIdx_mc * entryHeight - scrollOffset;
            visibleIdx_mc++;
            if (ey + entryHeight < listTopY || ey > panelTop + PANEL_HEIGHT - LIST_BOTTOM_OFFSET) continue;

            ConfigEntryDescriptor entry = entries.get(i);
            int valueX = listRight - 100;

            switch (entry.type) {
                case BOOLEAN: {
                    int toggleW = 40;
                    int toggleH = 14;
                    int tx = valueX - toggleW;
                    int ty = ey + (entryHeight - toggleH) / 2;
                    if (mouseX >= tx && mouseX <= tx + toggleW && mouseY >= ty && mouseY <= ty + toggleH) {
                        boolean current = Boolean.parseBoolean(entry.getter.get());
                        @SuppressWarnings("unchecked")
                        java.util.function.Consumer<Boolean> setter = (java.util.function.Consumer<Boolean>) entry.setter;
                        setter.accept(!current);
                        triggerPulse(entry.labelKey);
                        return;
                    }
                    break;
                }
                case INT: {
                    int arrowW = 16;
                    int arrowH = 16;
                    int ay = ey + (entryHeight - arrowH) / 2;
                    int lx = valueX - 80;
                    int rx = lx + arrowW + mc.fontRendererObj.getStringWidth(entry.getter.get()) + 5;

                    boolean lHover = mouseX >= lx && mouseX <= lx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;
                    boolean rHover = mouseX >= rx && mouseX <= rx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;

                    if (lHover || rHover) {
                        int current = Integer.parseInt(entry.getter.get());
                        boolean shiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
                        boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
                        int delta;
                        if (shiftDown && ctrlDown) {
                            delta = lHover ? -100 : 100;
                        } else if (shiftDown) {
                            delta = lHover ? -10 : 10;
                        } else if (ctrlDown) {
                            delta = lHover ? -50 : 50;
                        } else {
                            delta = lHover ? -1 : 1;
                        }
                        int newValue = current + delta;
                        newValue = Math.max(entry.minInt, Math.min(entry.maxInt, newValue));
                        @SuppressWarnings("unchecked")
                        java.util.function.Consumer<Integer> setter = (java.util.function.Consumer<Integer>) entry.setter;
                        setter.accept(newValue);
                        triggerPulse(entry.labelKey);
                        return;
                    }
                    break;
                }
                case FLOAT: {
                    int arrowW = 16;
                    int arrowH = 16;
                    int ay = ey + (entryHeight - arrowH) / 2;
                    int lx = valueX - 80;
                    int rx = lx + arrowW + mc.fontRendererObj.getStringWidth(entry.getter.get()) + 5;

                    boolean lHover = mouseX >= lx && mouseX <= lx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;
                    boolean rHover = mouseX >= rx && mouseX <= rx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;

                    if (lHover || rHover) {
                        float current = Float.parseFloat(entry.getter.get());
                        boolean shiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
                        boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
                        float delta;
                        if (shiftDown && ctrlDown) {
                            delta = lHover ? -10.0f : 10.0f;
                        } else if (shiftDown) {
                            delta = lHover ? -1.0f : 1.0f;
                        } else if (ctrlDown) {
                            delta = lHover ? -0.01f : 0.01f;
                        } else {
                            delta = lHover ? -0.1f : 0.1f;
                        }
                        float newValue = current + delta;
                        newValue = Math.max(entry.minFloat, Math.min(entry.maxFloat, newValue));
                        @SuppressWarnings("unchecked")
                        java.util.function.Consumer<Float> setter = (java.util.function.Consumer<Float>) entry.setter;
                        setter.accept(newValue);
                        triggerPulse(entry.labelKey);
                        return;
                    }
                    break;
                }
                case COLOR: {
                    // 点击色块打开 GuiColorPicker
                    int valueStrRight = valueX - 5;
                    int strW = mc.fontRendererObj.getStringWidth(entry.getter.get());
                    int blockStart = valueStrRight - strW + 4;
                    if (mouseX >= blockStart && mouseX <= valueStrRight + 20 && mouseY >= ey && mouseY <= ey + entryHeight) {
                        @SuppressWarnings("unchecked")
                        java.util.function.Consumer<Integer> setter = (java.util.function.Consumer<Integer>) entry.setter;
                        java.util.function.IntSupplier getter = () -> {
                            String hex = entry.getter.get().replace("#", "");
                            try { return (int) Long.parseLong(hex, 16); } catch (Exception e) { return 0xFFFFFFFF; }
                        };
                        mc.displayGuiScreen(new GuiColorPicker(this, getter, setter));
                        return;
                    }
                    break;
                }
                case ENUM: {
                    int arrowW = 16, arrowH = 16;
                    int ay = ey + (entryHeight - arrowH) / 2;
                    int lx = valueX - 80;
                    String current = entry.getter.get();
                    int rx = lx + arrowW + mc.fontRendererObj.getStringWidth(current) + 5;

                    boolean lHover = mouseX >= lx && mouseX <= lx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;
                    boolean rHover = mouseX >= rx && mouseX <= rx + arrowW && mouseY >= ay && mouseY <= ay + arrowH;

                    if (lHover || rHover) {
                        String[] values = entry.enumValues;
                        if (values == null || values.length == 0) break;
                        int idx = -1;
                        for (int vi = 0; vi < values.length; vi++) {
                            if (values[vi].equals(current)) { idx = vi; break; }
                        }
                        if (idx < 0) idx = 0;
                        if (lHover) {
                            idx = (idx - 1 + values.length) % values.length;
                        } else {
                            idx = (idx + 1) % values.length;
                        }
                        @SuppressWarnings("unchecked")
                        java.util.function.Consumer<String> setter = (java.util.function.Consumer<String>) entry.setter;
                        setter.accept(values[idx]);
                        triggerPulse(entry.labelKey);
                        return;
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void handleMouseInput() {
        try {
            super.handleMouseInput();
        } catch (java.io.IOException e) {
            // ignore
        }
        int scrollDelta = Mouse.getEventDWheel();
        if (scrollDelta != 0) {
            int step = 8;
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - (scrollDelta > 0 ? step : -step)));
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  键盘事件
    // ────────────────────────────────────────────────────────────────
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (isSearching) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                isSearching = false;
                searchText = "";
                scrollOffset = 0;
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN) {
                isSearching = false;
                return;
            }
            if (keyCode == Keyboard.KEY_BACK) {
                if (searchText.length() > 0) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                    scrollOffset = 0;
                }
                return;
            }
            if (typedChar >= ' ' && typedChar <= '~') {
                searchText += typedChar;
                scrollOffset = 0;
                return;
            }
            return; // 搜索模式下其他键不处理
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ────────────────────────────────────────────────────────────────
    //  脉冲动画：值变化时条目呼吸闪烁 600ms
    // ────────────────────────────────────────────────────────────────
    private void triggerPulse(String labelKey) {
        pulseLabel = labelKey;
        pulseEndTime = System.currentTimeMillis() + 600;
    }

    // ── 搜索过滤 ──
    private boolean searchMatches(ConfigEntryDescriptor entry) {
        if (searchText.isEmpty()) return true;
        String lower = searchText.toLowerCase();
        String label = LangManager.get(entry.labelKey).toLowerCase();
        if (label.contains(lower)) return true;
        // 也搜索 tooltip
        String tip = LangManager.get(entry.labelKey + ".tooltip");
        if (!tip.equals(entry.labelKey + ".tooltip") && tip.toLowerCase().contains(lower)) return true;
        return false;
    }

    // ────────────────────────────────────────────────────────────────
    //  简单矩形类
    // ────────────────────────────────────────────────────────────────
    private static class Rect {
        final int x, y, w, h;
        Rect(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
        boolean contains(int px, int py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }
}
