package com.yourname.betterplayerhud;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class GuiModernConfig extends GuiScreen {

    private static final int CLR_BG = 0xFF0D0D1A;
    private static final int CLR_PANEL = 0xFF1A1A2E;
    private static final int CLR_CARD = 0xFF22223A;
    private static final int CLR_CARD_HOVER = 0xFF2A2A4E;
    private static final int CLR_BLUE = 0xFF4A9EFF;
    private static final int CLR_TEXT = 0xFFCCCCCC;
    private static final int CLR_TEXT_SEC = 0xFF888888;
    private static final int CLR_DIVIDER = 0xFF2A2A3E;
    private static final int CLR_SAVE = 0xFF1A6B1A;
    private static final int CLR_RESET = 0xFF6B1A1A;
    private static final int LEFT_W = 220;
    private static final int ENTRY_H = 24;
    // Legacy color constants (used by extracted helper methods)
    private static final int COLOR_TOGGLE_ON = 0xFF4CAF50;
    private static final int COLOR_TOGGLE_OFF = 0xFF666666;
    private static final int COLOR_VALUE_TEXT = 0xFFCCCCCC;
    private static final int COLOR_ARROW_HOVER = 0xFF888888;
    private static final int COLOR_ARROW_BG = 0xFF666666;
    private static final int COLOR_TEXT_LIGHT = 0xFFCCCCCC;

    private final List<ConfigTreeNode> treeRoots = new ArrayList<>();
    private ConfigTreeNode selectedNode = null;
    private List<ConfigEntryDescriptor> localEntries = null;
    private int treeScrollOffset = 0;
    private int entriesScrollOffset = 0;
    private final GuiScreen parentScreen;
    private String pulseLabel = null;
    private long pulseEndTime = 0L;
    private ConfigEntryDescriptor hoveredEntry = null;
    private String searchQuery = "";
    private boolean isSearching = false;
    private int contentHeight = 0;

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

    public GuiModernConfig(GuiScreen parent) { this.parentScreen = parent; }
    @Override
    public void initGui() { buildTree(); selectFirstLeaf(); }
    private void buildFromConfig() { treeRoots.clear(); buildTree(); selectFirstLeaf(); }
    private void saveConfig() { BetterPlayerHUD.config.saveConfig(); triggerPulse("gui.saved"); }
    private void recalcLayout() { if (localEntries != null) contentHeight = localEntries.size() * ENTRY_H; }
    private void selectFirstLeaf() {
        for (ConfigTreeNode r : treeRoots)
            for (ConfigTreeNode c : r.children)
                if (c.isLeaf()) { selectedNode = c; localEntries = c.entries; recalcLayout(); return; }
                else for (ConfigTreeNode s : c.children)
                    if (s.isLeaf()) { selectedNode = s; localEntries = s.entries; recalcLayout(); return; }
    }
    private static class ConfigTreeNode {
        final String labelKey;
        final List<ConfigTreeNode> children = new ArrayList<>();
        final List<ConfigEntryDescriptor> entries = new ArrayList<>();
        boolean expanded = false;
        int dotColor = 0xFF888888;
        ConfigTreeNode(String labelKey) { this.labelKey = labelKey; }
        boolean isLeaf() { return children.isEmpty(); }
    }
    private int computeMaxTreeScroll() {
        int h = 28;
        for (ConfigTreeNode r : treeRoots) { h += 20;
            if (!r.expanded) continue;
            for (ConfigTreeNode c : r.children) { h += 18;
                if (!c.expanded || c.isLeaf()) continue;
                h += c.children.size() * 16;
            }
        }
        return Math.max(0, h - (height - 40));
    }
    private void buildTree() {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        treeRoots.clear();
        ConfigTreeNode combat = new ConfigTreeNode("category.combat");
        combat.dotColor = 0xFFE74C3C; combat.expanded = true;
        ConfigTreeNode crosshair = new ConfigTreeNode("module.crosshair");
        crosshair.dotColor = 0xFFE74C3C; crosshair.expanded = true;
        ConfigTreeNode xgen = new ConfigTreeNode("crosshair.group.general"); xgen.expanded = true;
        xgen.entries.add(new ConfigEntryDescriptor("crosshair.style",
(java.util.function.Supplier<String>) () -> cfg.crosshairStyle,
(Consumer<String>) (v) -> cfg.crosshairStyle = v,
new String[]{"default", "dot", "circle", "cross", "arrow"}));
        xgen.entries.add(new ConfigEntryDescriptor("crosshair.gap",
(IntSupplier) () -> cfg.crosshairGap, (Consumer<Integer>) (v) -> cfg.crosshairGap = v, 0, 20));
        xgen.entries.add(new ConfigEntryDescriptor("crosshair.thickness",
(IntSupplier) () -> cfg.crosshairThickness, (Consumer<Integer>) (v) -> cfg.crosshairThickness = v, 1, 10));
        xgen.entries.add(new ConfigEntryDescriptor("crosshair.dot",
(BooleanSupplier) () -> cfg.crosshairOutline, (Consumer<Boolean>) (v) -> cfg.crosshairOutline = v));
        xgen.entries.add(new ConfigEntryDescriptor("crosshair.spread",
(BooleanSupplier) () -> cfg.crosshairSpread, (Consumer<Boolean>) (v) -> cfg.crosshairSpread = v));
        ConfigTreeNode xrgb = new ConfigTreeNode("crosshair.group.rgb"); xrgb.expanded = true;
        xrgb.entries.add(new ConfigEntryDescriptor("entityHL.alpha",
(IntSupplier) () -> cfg.entityOutlineAlpha, (Consumer<Integer>) (v) -> cfg.entityOutlineAlpha = v, 0, 255));
        xrgb.entries.add(new ConfigEntryDescriptor("rgb.enable",
(BooleanSupplier) () -> cfg.enableRGBMode, (Consumer<Boolean>) (v) -> cfg.enableRGBMode = v));
        xrgb.entries.add(new ConfigEntryDescriptor("rgb.speed",
(IntSupplier) () -> cfg.rgbSpeed, (Consumer<Integer>) (v) -> cfg.rgbSpeed = v, 50, 5000));
        xrgb.entries.add(new ConfigEntryDescriptor("rgb.mode",
(java.util.function.Supplier<String>) () -> cfg.rgbFlowMode,
(Consumer<String>) (v) -> cfg.rgbFlowMode = v,
new String[]{"perimeter", "uniform"}));
        ConfigTreeNode xent = new ConfigTreeNode("crosshair.group.entity"); xent.expanded = true;
        xent.entries.add(new ConfigEntryDescriptor("crosshair.entityColor",
(BooleanSupplier) () -> cfg.crosshairEntityColor, (Consumer<Boolean>) (v) -> cfg.crosshairEntityColor = v));
        xent.entries.add(new ConfigEntryDescriptor("crosshair.range",
(IntSupplier) () -> cfg.crosshairEntityRange, (Consumer<Integer>) (v) -> cfg.crosshairEntityRange = v, 1, 50));
        crosshair.children.add(xgen); crosshair.children.add(xrgb); crosshair.children.add(xent);
        combat.children.add(crosshair);
        ConfigTreeNode hitmarker = new ConfigTreeNode("module.hitmarker");
        hitmarker.dotColor = 0xFFE74C3C; hitmarker.expanded = true;
        ConfigTreeNode hmgen = new ConfigTreeNode("hitmarker.group.general"); hmgen.expanded = true;
        hmgen.entries.add(new ConfigEntryDescriptor("module.hitmarker",
(BooleanSupplier) () -> cfg.enableHitMarker, (Consumer<Boolean>) (v) -> cfg.enableHitMarker = v));
        hmgen.entries.add(new ConfigEntryDescriptor("hitmarker.useS19",
(BooleanSupplier) () -> cfg.hitMarkerUseS19, (Consumer<Boolean>) (v) -> cfg.hitMarkerUseS19 = v));
        hmgen.entries.add(new ConfigEntryDescriptor("hitmarker.hitSound",
(BooleanSupplier) () -> cfg.enableHitSounds, (Consumer<Boolean>) (v) -> cfg.enableHitSounds = v));
        hmgen.entries.add(new ConfigEntryDescriptor("hitmarker.killSound",
(BooleanSupplier) () -> cfg.enableKillSound, (Consumer<Boolean>) (v) -> cfg.enableKillSound = v));
        hmgen.entries.add(new ConfigEntryDescriptor("hitmarker.volume",
(DoubleSupplier) () -> (double) cfg.soundVolume, (Consumer<Float>) (v) -> cfg.soundVolume = v, 0.0f, 1.0f));
        ConfigTreeNode hmapp = new ConfigTreeNode("hitmarker.group.appearance"); hmapp.expanded = true;
        hmapp.entries.add(new ConfigEntryDescriptor("hitmarker.hitSize",
(IntSupplier) () -> (int) cfg.hitSize, (Consumer<Integer>) (v) -> cfg.hitSize = (float) v, 2, 32));
        hmapp.entries.add(new ConfigEntryDescriptor("hitmarker.hitAlpha",
(DoubleSupplier) () -> (double) cfg.hitAlpha, (Consumer<Float>) (v) -> cfg.hitAlpha = v, 0.0f, 1.0f));
        hmapp.entries.add(new ConfigEntryDescriptor("hitmarker.border",
(BooleanSupplier) () -> cfg.hitMarkerEnableBorder, (Consumer<Boolean>) (v) -> cfg.hitMarkerEnableBorder = v));
        hmapp.entries.add(new ConfigEntryDescriptor("hitmarker.borderWidth",
(DoubleSupplier) () -> (double) cfg.hitMarkerBorderWidth, (Consumer<Float>) (v) -> cfg.hitMarkerBorderWidth = v, 0.5f, 5.0f));
        hmapp.entries.add(new ConfigEntryDescriptor("hitmarker.blood",
(DoubleSupplier) () -> (double) cfg.hitBloodIntensity, (Consumer<Float>) (v) -> cfg.hitBloodIntensity = v, 0.0f, 1.0f));
        hmapp.entries.add(new ConfigEntryDescriptor("hitmarker.randomRotate",
(BooleanSupplier) () -> cfg.hitMarkerRandomRotate, (Consumer<Boolean>) (v) -> cfg.hitMarkerRandomRotate = v));
        hmapp.entries.add(new ConfigEntryDescriptor("hitmarker.rotateStrength",
(DoubleSupplier) () -> (double) cfg.hitMarkerRandomRotateStrength, (Consumer<Float>) (v) -> cfg.hitMarkerRandomRotateStrength = v, 0.0f, 90.0f));
        hitmarker.children.add(hmgen); hitmarker.children.add(hmapp);
        combat.children.add(hitmarker);
        ConfigTreeNode comboLf = new ConfigTreeNode("module.combo");
        comboLf.dotColor = 0xFFE74C3C;
        comboLf.entries.add(new ConfigEntryDescriptor("module.combo",
(BooleanSupplier) () -> cfg.enableCombo, (Consumer<Boolean>) (v) -> cfg.enableCombo = v));
        comboLf.entries.add(new ConfigEntryDescriptor("combo.xOffset",
(IntSupplier) () -> cfg.comboXOffset, (Consumer<Integer>) (v) -> cfg.comboXOffset = v, -500, 500));
        comboLf.entries.add(new ConfigEntryDescriptor("combo.yOffset",
(IntSupplier) () -> cfg.comboYOffset, (Consumer<Integer>) (v) -> cfg.comboYOffset = v, -500, 500));
        combat.children.add(comboLf);
        ConfigTreeNode tgtLf = new ConfigTreeNode("module.target");
        tgtLf.dotColor = 0xFFE74C3C;
        tgtLf.entries.add(new ConfigEntryDescriptor("combo.scale",
(DoubleSupplier) () -> (double) cfg.comboScale, (Consumer<Float>) (v) -> cfg.comboScale = v, 0.25f, 4.0f));
        combat.children.add(tgtLf);
        ConfigTreeNode bolLf = new ConfigTreeNode("module.blockOutline");
        bolLf.dotColor = 0xFFE74C3C;
        bolLf.entries.add(new ConfigEntryDescriptor("module.target",
(BooleanSupplier) () -> cfg.enableTargetHealth, (Consumer<Boolean>) (v) -> cfg.enableTargetHealth = v));
        bolLf.entries.add(new ConfigEntryDescriptor("blockOutline.enable",
(BooleanSupplier) () -> cfg.enableBlockHighlight, (Consumer<Boolean>) (v) -> cfg.enableBlockHighlight = v));
        bolLf.entries.add(new ConfigEntryDescriptor("blockOutline.width",
(DoubleSupplier) () -> (double) cfg.blockOutlineWidth, (Consumer<Float>) (v) -> cfg.blockOutlineWidth = v, 1.0f, 8.0f));
        bolLf.entries.add(new ConfigEntryDescriptor("blockOutline.alpha",
(IntSupplier) () -> cfg.blockOutlineAlpha, (Consumer<Integer>) (v) -> cfg.blockOutlineAlpha = v, 0, 255));
        combat.children.add(bolLf);
        ConfigTreeNode ehlLf = new ConfigTreeNode("module.entityHL");
        ehlLf.dotColor = 0xFFE74C3C;
        ehlLf.entries.add(new ConfigEntryDescriptor("blockOutline.visibleOnly",
(BooleanSupplier) () -> cfg.drawVisibleFacesOnlyBlocks, (Consumer<Boolean>) (v) -> cfg.drawVisibleFacesOnlyBlocks = v));
        ehlLf.entries.add(new ConfigEntryDescriptor("entityHL.enable",
(BooleanSupplier) () -> cfg.enableEntityHighlight, (Consumer<Boolean>) (v) -> cfg.enableEntityHighlight = v));
        ehlLf.entries.add(new ConfigEntryDescriptor("entityHL.width",
(DoubleSupplier) () -> (double) cfg.entityOutlineWidth, (Consumer<Float>) (v) -> cfg.entityOutlineWidth = v, 1.0f, 8.0f));
        combat.children.add(ehlLf);
        ConfigTreeNode bbrLf = new ConfigTreeNode("module.blockBreak");
        bbrLf.dotColor = 0xFFE74C3C;
        bbrLf.entries.add(new ConfigEntryDescriptor("rgb.algorithm",
(java.util.function.Supplier<String>) () -> cfg.rgbColorAlgo,
(Consumer<String>) (v) -> cfg.rgbColorAlgo = v,
new String[]{"hsv", "sinewave"}));
        bbrLf.entries.add(new ConfigEntryDescriptor("blockBreak.enable",
(BooleanSupplier) () -> cfg.enableBlockBreakIndicator, (Consumer<Boolean>) (v) -> cfg.enableBlockBreakIndicator = v));
        bbrLf.entries.add(new ConfigEntryDescriptor("blockBreak.x",
(IntSupplier) () -> cfg.blockBreakIndicatorX, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorX = v, -200, 200));
        bbrLf.entries.add(new ConfigEntryDescriptor("blockBreak.y",
(IntSupplier) () -> cfg.blockBreakIndicatorY, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorY = v, 0, 200));
        bbrLf.entries.add(new ConfigEntryDescriptor("blockBreak.width",
(IntSupplier) () -> cfg.blockBreakIndicatorWidth, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorWidth = v, 10, 200));
        bbrLf.entries.add(new ConfigEntryDescriptor("blockBreak.height",
(IntSupplier) () -> cfg.blockBreakIndicatorHeight, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorHeight = v, 2, 20));
        bbrLf.entries.add(new ConfigEntryDescriptor("blockBreak.showPercent",
(BooleanSupplier) () -> cfg.blockBreakIndicatorShowPercent, (Consumer<Boolean>) (v) -> cfg.blockBreakIndicatorShowPercent = v));
        combat.children.add(bbrLf);
        treeRoots.add(combat);
        ConfigTreeNode info = new ConfigTreeNode("category.info");
        info.dotColor = 0xFF3498DB; info.expanded = true;
        ConfigTreeNode compass = new ConfigTreeNode("module.compass");
        compass.dotColor = 0xFF3498DB; compass.expanded = true;
        ConfigTreeNode cgen = new ConfigTreeNode("compass.group.general"); cgen.expanded = true;
        cgen.entries.add(new ConfigEntryDescriptor("compass.show",
(BooleanSupplier) () -> cfg.showCompassHUD, (Consumer<Boolean>) (v) -> cfg.showCompassHUD = v));
        cgen.entries.add(new ConfigEntryDescriptor("compass.x",
(IntSupplier) () -> cfg.xPosition, (Consumer<Integer>) (v) -> cfg.xPosition = v, -200, 2000));
        cgen.entries.add(new ConfigEntryDescriptor("compass.y",
(IntSupplier) () -> cfg.yPosition, (Consumer<Integer>) (v) -> cfg.yPosition = v, 0, 2000));
        cgen.entries.add(new ConfigEntryDescriptor("compass.scale",
(DoubleSupplier) () -> (double) cfg.scale, (Consumer<Float>) (v) -> cfg.scale = v, 0.25f, 4.0f));
        cgen.entries.add(new ConfigEntryDescriptor("compass.style",
(java.util.function.Supplier<String>) () -> cfg.displayStyle,
(Consumer<String>) (v) -> cfg.displayStyle = v,
new String[]{"detailed", "minimal", "simple"}));
        ConfigTreeNode cdisp = new ConfigTreeNode("compass.group.display"); cdisp.expanded = true;
        cdisp.entries.add(new ConfigEntryDescriptor("compass.degreeMarks",
(BooleanSupplier) () -> cfg.showDegreeMarks, (Consumer<Boolean>) (v) -> cfg.showDegreeMarks = v));
        cdisp.entries.add(new ConfigEntryDescriptor("compass.needle",
(BooleanSupplier) () -> cfg.showCompassNeedle, (Consumer<Boolean>) (v) -> cfg.showCompassNeedle = v));
        cdisp.entries.add(new ConfigEntryDescriptor("compass.interval",
(IntSupplier) () -> cfg.degreeMarkInterval, (Consumer<Integer>) (v) -> cfg.degreeMarkInterval = v, 5, 90));
        cdisp.entries.add(new ConfigEntryDescriptor("compass.dynamicScale",
(BooleanSupplier) () -> cfg.dynamicScaling, (Consumer<Boolean>) (v) -> cfg.dynamicScaling = v));
        cdisp.entries.add(new ConfigEntryDescriptor("compass.horizon",
(BooleanSupplier) () -> cfg.showHorizon, (Consumer<Boolean>) (v) -> cfg.showHorizon = v));
        cdisp.entries.add(new ConfigEntryDescriptor("compass.exactAngle",
(BooleanSupplier) () -> cfg.showExactAngle, (Consumer<Boolean>) (v) -> cfg.showExactAngle = v));
        compass.children.add(cgen); compass.children.add(cdisp);
        info.children.add(compass);
        ConfigTreeNode health = new ConfigTreeNode("module.health");
        health.dotColor = 0xFF3498DB; health.expanded = true;
        ConfigTreeNode hlay = new ConfigTreeNode("health.group.layout"); hlay.expanded = true;
        hlay.entries.add(new ConfigEntryDescriptor("health.x",
(IntSupplier) () -> cfg.healthHudX, (Consumer<Integer>) (v) -> cfg.healthHudX = v, -500, 2000));
        hlay.entries.add(new ConfigEntryDescriptor("health.y",
(IntSupplier) () -> cfg.healthHudY, (Consumer<Integer>) (v) -> cfg.healthHudY = v, -500, 500));
        hlay.entries.add(new ConfigEntryDescriptor("health.showArmor",
(BooleanSupplier) () -> cfg.showArmorHUD, (Consumer<Boolean>) (v) -> cfg.showArmorHUD = v));
        ConfigTreeNode helem = new ConfigTreeNode("health.group.elements"); helem.expanded = true;
        helem.entries.add(new ConfigEntryDescriptor("health.showHunger",
(BooleanSupplier) () -> cfg.showHungerHUD, (Consumer<Boolean>) (v) -> cfg.showHungerHUD = v));
        helem.entries.add(new ConfigEntryDescriptor("health.showHead",
(BooleanSupplier) () -> cfg.showPlayerHead, (Consumer<Boolean>) (v) -> cfg.showPlayerHead = v));
        helem.entries.add(new ConfigEntryDescriptor("health.headSize",
(IntSupplier) () -> cfg.headSize, (Consumer<Integer>) (v) -> cfg.headSize = v, 8, 64));
        helem.entries.add(new ConfigEntryDescriptor("health.textSpacing",
(IntSupplier) () -> cfg.headTextSpacing, (Consumer<Integer>) (v) -> cfg.headTextSpacing = v, 0, 20));
        health.children.add(hlay); health.children.add(helem);
        info.children.add(health);
        ConfigTreeNode dist = new ConfigTreeNode("module.distance");
        dist.dotColor = 0xFF3498DB; dist.expanded = true;
        ConfigTreeNode dcoord = new ConfigTreeNode("distance.group.coords"); dcoord.expanded = true;
        dcoord.entries.add(new ConfigEntryDescriptor("dist.x",
(IntSupplier) () -> cfg.distanceHudX, (Consumer<Integer>) (v) -> cfg.distanceHudX = v, 0, 2000));
        dcoord.entries.add(new ConfigEntryDescriptor("dist.y",
(IntSupplier) () -> cfg.distanceHudY, (Consumer<Integer>) (v) -> cfg.distanceHudY = v, 0, 2000));
        dcoord.entries.add(new ConfigEntryDescriptor("dist.precision",
(IntSupplier) () -> cfg.distancePrecision, (Consumer<Integer>) (v) -> cfg.distancePrecision = v, 0, 5));
        ConfigTreeNode ddisp = new ConfigTreeNode("distance.group.display"); ddisp.expanded = true;
        ddisp.entries.add(new ConfigEntryDescriptor("dist.showTarget",
(BooleanSupplier) () -> cfg.showTargetInfo, (Consumer<Boolean>) (v) -> cfg.showTargetInfo = v));
        ddisp.entries.add(new ConfigEntryDescriptor("dist.showBlockInfo",
(BooleanSupplier) () -> cfg.showBlockCoordinates, (Consumer<Boolean>) (v) -> cfg.showBlockCoordinates = v));
        ddisp.entries.add(new ConfigEntryDescriptor("dist.hardness",
(BooleanSupplier) () -> cfg.showBlockHardness, (Consumer<Boolean>) (v) -> cfg.showBlockHardness = v));
        ddisp.entries.add(new ConfigEntryDescriptor("dist.showTool",
(BooleanSupplier) () -> cfg.showRequiredTool, (Consumer<Boolean>) (v) -> cfg.showRequiredTool = v));
        dist.children.add(dcoord); dist.children.add(ddisp);
        info.children.add(dist);
        ConfigTreeNode keysLf = new ConfigTreeNode("module.keys");
        keysLf.dotColor = 0xFF3498DB;
        keysLf.entries.add(new ConfigEntryDescriptor("keys.x",
(IntSupplier) () -> cfg.keysDisplayX, (Consumer<Integer>) (v) -> cfg.keysDisplayX = v, 0, 2000));
        keysLf.entries.add(new ConfigEntryDescriptor("keys.y",
(IntSupplier) () -> cfg.keysDisplayY, (Consumer<Integer>) (v) -> cfg.keysDisplayY = v, 0, 2000));
        keysLf.entries.add(new ConfigEntryDescriptor("keys.size",
(IntSupplier) () -> cfg.keysSize, (Consumer<Integer>) (v) -> cfg.keysSize = v, 12, 64));
        keysLf.entries.add(new ConfigEntryDescriptor("keys.spacing",
(IntSupplier) () -> cfg.keysSpacing, (Consumer<Integer>) (v) -> cfg.keysSpacing = v, 0, 32));
        keysLf.entries.add(new ConfigEntryDescriptor("keys.scale",
(DoubleSupplier) () -> (double) cfg.keysScale, (Consumer<Float>) (v) -> cfg.keysScale = v, 0.25f, 4.0f));
        keysLf.entries.add(new ConfigEntryDescriptor("keys.background",
(BooleanSupplier) () -> cfg.showKeysBackground, (Consumer<Boolean>) (v) -> cfg.showKeysBackground = v));
        keysLf.entries.add(new ConfigEntryDescriptor("keys.opacity",
(DoubleSupplier) () -> (double) cfg.keysOpacity, (Consumer<Float>) (v) -> cfg.keysOpacity = v, 0.0f, 1.0f));
        info.children.add(keysLf);
        ConfigTreeNode chromLf = new ConfigTreeNode("module.chroma");
        chromLf.dotColor = 0xFF3498DB;
        chromLf.entries.add(new ConfigEntryDescriptor("module.chroma",
(BooleanSupplier) () -> cfg.enableChromaChat, (Consumer<Boolean>) (v) -> cfg.enableChromaChat = v));
        chromLf.entries.add(new ConfigEntryDescriptor("chroma.width",
(IntSupplier) () -> cfg.chromaChatWidth, (Consumer<Integer>) (v) -> cfg.chromaChatWidth = v, 80, 800));
        chromLf.entries.add(new ConfigEntryDescriptor("chroma.lines",
(IntSupplier) () -> cfg.chromaChatLineCount, (Consumer<Integer>) (v) -> cfg.chromaChatLineCount = v, 1, 50));
        chromLf.entries.add(new ConfigEntryDescriptor("chroma.timestamp",
(BooleanSupplier) () -> cfg.chromaChatShowTimestamps, (Consumer<Boolean>) (v) -> cfg.chromaChatShowTimestamps = v));
        chromLf.entries.add(new ConfigEntryDescriptor("chroma.avatar",
(BooleanSupplier) () -> cfg.chromaChatAvatar, (Consumer<Boolean>) (v) -> cfg.chromaChatAvatar = v));
        chromLf.entries.add(new ConfigEntryDescriptor("chroma.signboard",
(BooleanSupplier) () -> cfg.chromaChatSignboardMerge, (Consumer<Boolean>) (v) -> cfg.chromaChatSignboardMerge = v));
        chromLf.entries.add(new ConfigEntryDescriptor("chroma.maxLines",
(IntSupplier) () -> cfg.chromaChatMaxLines, (Consumer<Integer>) (v) -> cfg.chromaChatMaxLines = v, 20, 500));
        info.children.add(chromLf);
        ConfigTreeNode perfLf = new ConfigTreeNode("module.performance");
        perfLf.dotColor = 0xFF3498DB;
        perfLf.entries.add(new ConfigEntryDescriptor("module.performance",
(BooleanSupplier) () -> cfg.enablePerformanceHUD, (Consumer<Boolean>) (v) -> cfg.enablePerformanceHUD = v));
        info.children.add(perfLf);
        ConfigTreeNode tabLf = new ConfigTreeNode("module.tablist");
        tabLf.dotColor = 0xFF3498DB;
        tabLf.entries.add(new ConfigEntryDescriptor("module.tablist",
(BooleanSupplier) () -> cfg.enableTabListHUD, (Consumer<Boolean>) (v) -> cfg.enableTabListHUD = v));
        info.children.add(tabLf);
        ConfigTreeNode dbgLf = new ConfigTreeNode("module.debug");
        dbgLf.dotColor = 0xFF3498DB;
        dbgLf.entries.add(new ConfigEntryDescriptor("module.debug",
(BooleanSupplier) () -> cfg.enableDebugInfoHUD, (Consumer<Boolean>) (v) -> cfg.enableDebugInfoHUD = v));
        dbgLf.entries.add(new ConfigEntryDescriptor("debug.xOffset",
(IntSupplier) () -> cfg.debugInfoXOffset, (Consumer<Integer>) (v) -> cfg.debugInfoXOffset = v, 0, 2000));
        dbgLf.entries.add(new ConfigEntryDescriptor("debug.yOffset",
(IntSupplier) () -> cfg.debugInfoYOffset, (Consumer<Integer>) (v) -> cfg.debugInfoYOffset = v, 0, 2000));
        info.children.add(dbgLf);
        treeRoots.add(info);
        ConfigTreeNode status = new ConfigTreeNode("category.status");
        status.dotColor = 0xFF2ECC71; status.expanded = true;
        ConfigTreeNode xpLf = new ConfigTreeNode("module.expbar");
        xpLf.dotColor = 0xFF2ECC71;
        xpLf.entries.add(new ConfigEntryDescriptor("module.expbar",
(BooleanSupplier) () -> cfg.enableXpBarHUD, (Consumer<Boolean>) (v) -> cfg.enableXpBarHUD = v));
        xpLf.entries.add(new ConfigEntryDescriptor("xpbar.offsetX",
(IntSupplier) () -> cfg.xpBarXOffset, (Consumer<Integer>) (v) -> cfg.xpBarXOffset = v, -500, 500));
        xpLf.entries.add(new ConfigEntryDescriptor("xpbar.offsetY",
(IntSupplier) () -> cfg.xpBarYOffset, (Consumer<Integer>) (v) -> cfg.xpBarYOffset = v, -500, 500));
        xpLf.entries.add(new ConfigEntryDescriptor("xpbar.width",
(IntSupplier) () -> cfg.xpBarWidth, (Consumer<Integer>) (v) -> cfg.xpBarWidth = v, 80, 400));
        status.children.add(xpLf);
        ConfigTreeNode bossLf = new ConfigTreeNode("module.bossHP");
        bossLf.dotColor = 0xFF2ECC71;
        bossLf.entries.add(new ConfigEntryDescriptor("xpbar.height",
(IntSupplier) () -> cfg.xpBarHeight, (Consumer<Integer>) (v) -> cfg.xpBarHeight = v, 3, 20));
        status.children.add(bossLf);
        ConfigTreeNode hotbLf = new ConfigTreeNode("module.hotbar");
        hotbLf.dotColor = 0xFF2ECC71;
        hotbLf.entries.add(new ConfigEntryDescriptor("module.bossHP",
(BooleanSupplier) () -> cfg.enableBossHealthHUD, (Consumer<Boolean>) (v) -> cfg.enableBossHealthHUD = v));
        hotbLf.entries.add(new ConfigEntryDescriptor("module.hotbar",
(BooleanSupplier) () -> cfg.enableHotbarHUD, (Consumer<Boolean>) (v) -> cfg.enableHotbarHUD = v));
        status.children.add(hotbLf);
        ConfigTreeNode potion = new ConfigTreeNode("module.potion");
        potion.dotColor = 0xFF2ECC71; potion.expanded = true;
        ConfigTreeNode ptimer = new ConfigTreeNode("potion.group.timer"); ptimer.expanded = true;
        ptimer.entries.add(new ConfigEntryDescriptor("module.potion",
(BooleanSupplier) () -> cfg.enablePotionHUD, (Consumer<Boolean>) (v) -> cfg.enablePotionHUD = v));
        ptimer.entries.add(new ConfigEntryDescriptor("module.potiontimer",
(BooleanSupplier) () -> cfg.enablePotionTimer, (Consumer<Boolean>) (v) -> cfg.enablePotionTimer = v));
        ptimer.entries.add(new ConfigEntryDescriptor("potion.xOffset",
(IntSupplier) () -> cfg.potionXOffset, (Consumer<Integer>) (v) -> cfg.potionXOffset = v, -500, 500));
        ptimer.entries.add(new ConfigEntryDescriptor("potion.yOffset",
(IntSupplier) () -> cfg.potionYOffset, (Consumer<Integer>) (v) -> cfg.potionYOffset = v, -500, 500));
        ConfigTreeNode peff = new ConfigTreeNode("potion.group.effects"); peff.expanded = true;
        peff.entries.add(new ConfigEntryDescriptor("potionTimer.xOffset",
(IntSupplier) () -> cfg.potionTimerXOffset, (Consumer<Integer>) (v) -> cfg.potionTimerXOffset = v, -500, 500));
        peff.entries.add(new ConfigEntryDescriptor("potionTimer.yOffset",
(IntSupplier) () -> cfg.potionTimerYOffset, (Consumer<Integer>) (v) -> cfg.potionTimerYOffset = v, -500, 500));
        peff.entries.add(new ConfigEntryDescriptor("potionTimer.iconSize",
(IntSupplier) () -> cfg.potionTimerIconSize, (Consumer<Integer>) (v) -> cfg.potionTimerIconSize = v, 16, 64));
        potion.children.add(ptimer); potion.children.add(peff);
        status.children.add(potion);
        ConfigTreeNode crisisLf = new ConfigTreeNode("module.crisis");
        crisisLf.dotColor = 0xFF2ECC71;
        crisisLf.entries.add(new ConfigEntryDescriptor("module.crisis",
(BooleanSupplier) () -> cfg.enableCrisisWarning, (Consumer<Boolean>) (v) -> cfg.enableCrisisWarning = v));
        crisisLf.entries.add(new ConfigEntryDescriptor("crisis.warnHealth",
(BooleanSupplier) () -> cfg.crisisWarnHealth, (Consumer<Boolean>) (v) -> cfg.crisisWarnHealth = v));
        crisisLf.entries.add(new ConfigEntryDescriptor("crisis.healthThreshold",
(DoubleSupplier) () -> cfg.crisisHealthThreshold, (Consumer<Float>) (v) -> cfg.crisisHealthThreshold = v, 0.5f, 20.0f));
        crisisLf.entries.add(new ConfigEntryDescriptor("crisis.warnHunger",
(BooleanSupplier) () -> cfg.crisisWarnHunger, (Consumer<Boolean>) (v) -> cfg.crisisWarnHunger = v));
        crisisLf.entries.add(new ConfigEntryDescriptor("crisis.hungerThreshold",
(IntSupplier) () -> cfg.crisisHungerThreshold, (Consumer<Integer>) (v) -> cfg.crisisHungerThreshold = v, 1, 20));
        crisisLf.entries.add(new ConfigEntryDescriptor("crisis.warnTnt",
(BooleanSupplier) () -> cfg.crisisWarnTnt, (Consumer<Boolean>) (v) -> cfg.crisisWarnTnt = v));
        crisisLf.entries.add(new ConfigEntryDescriptor("crisis.tntRadius",
(DoubleSupplier) () -> cfg.crisisTntRadius, (Consumer<Float>) (v) -> cfg.crisisTntRadius = v, 2.0f, 50.0f));
        crisisLf.entries.add(new ConfigEntryDescriptor("crisis.xOffset",
(IntSupplier) () -> cfg.crisisXOffset, (Consumer<Integer>) (v) -> cfg.crisisXOffset = v, -500, 500));
        crisisLf.entries.add(new ConfigEntryDescriptor("crisis.yOffset",
(IntSupplier) () -> cfg.crisisYOffset, (Consumer<Integer>) (v) -> cfg.crisisYOffset = v, -500, 500));
        crisisLf.entries.add(new ConfigEntryDescriptor("crisis.iconSize",
(IntSupplier) () -> cfg.crisisIconSize, (Consumer<Integer>) (v) -> cfg.crisisIconSize = v, 16, 128));
        status.children.add(crisisLf);
        ConfigTreeNode jumpLf = new ConfigTreeNode("module.jumpbar");
        jumpLf.dotColor = 0xFF2ECC71;
        jumpLf.entries.add(new ConfigEntryDescriptor("module.jumpbar",
(BooleanSupplier) () -> cfg.enableJumpBar, (Consumer<Boolean>) (v) -> cfg.enableJumpBar = v));
        status.children.add(jumpLf);
        ConfigTreeNode airLf = new ConfigTreeNode("module.airhud");
        airLf.dotColor = 0xFF2ECC71;
        airLf.entries.add(new ConfigEntryDescriptor("module.airhud",
(BooleanSupplier) () -> cfg.enableAirHUD, (Consumer<Boolean>) (v) -> cfg.enableAirHUD = v));
        status.children.add(airLf);
        ConfigTreeNode mountLf = new ConfigTreeNode("module.mountHP");
        mountLf.dotColor = 0xFF2ECC71;
        mountLf.entries.add(new ConfigEntryDescriptor("module.mountHP",
(BooleanSupplier) () -> cfg.enableMountHP, (Consumer<Boolean>) (v) -> cfg.enableMountHP = v));
        status.children.add(mountLf);
        treeRoots.add(status);
        ConfigTreeNode colors = new ConfigTreeNode("category.colors");
        colors.dotColor = 0xFFF39C12; colors.expanded = true;
        colors.entries.add(new ConfigEntryDescriptor("color.healthSafe",
(IntSupplier) () -> cfg.healthColorSafe, (Consumer<Integer>) (v) -> cfg.healthColorSafe = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.healthWarning",
(IntSupplier) () -> cfg.healthColorWarning, (Consumer<Integer>) (v) -> cfg.healthColorWarning = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.healthDanger",
(IntSupplier) () -> cfg.healthColorDanger, (Consumer<Integer>) (v) -> cfg.healthColorDanger = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.armor",
(IntSupplier) () -> cfg.armorColor, (Consumer<Integer>) (v) -> cfg.armorColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.hunger",
(IntSupplier) () -> cfg.hungerColor, (Consumer<Integer>) (v) -> cfg.hungerColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.compass",
(IntSupplier) () -> cfg.compassColor, (Consumer<Integer>) (v) -> cfg.compassColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.distance",
(IntSupplier) () -> cfg.distanceColor, (Consumer<Integer>) (v) -> cfg.distanceColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.targetInfo",
(IntSupplier) () -> cfg.targetInfoColor, (Consumer<Integer>) (v) -> cfg.targetInfoColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.blockInfo",
(IntSupplier) () -> cfg.blockInfoColor, (Consumer<Integer>) (v) -> cfg.blockInfoColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.keysActive",
(IntSupplier) () -> cfg.keysActiveColor, (Consumer<Integer>) (v) -> cfg.keysActiveColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.keysInactive",
(IntSupplier) () -> cfg.keysInactiveColor, (Consumer<Integer>) (v) -> cfg.keysInactiveColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.keysText",
(IntSupplier) () -> cfg.keysTextColor, (Consumer<Integer>) (v) -> cfg.keysTextColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.potionText",
(IntSupplier) () -> cfg.potionTextColor, (Consumer<Integer>) (v) -> cfg.potionTextColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.blockOutline",
(IntSupplier) () -> cfg.blockOutlineColor, (Consumer<Integer>) (v) -> cfg.blockOutlineColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.entityHostile",
(IntSupplier) () -> cfg.entityOutlineColorHostile, (Consumer<Integer>) (v) -> cfg.entityOutlineColorHostile = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.entityNeutral",
(IntSupplier) () -> cfg.entityOutlineColorNeutral, (Consumer<Integer>) (v) -> cfg.entityOutlineColorNeutral = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.entityFriendly",
(IntSupplier) () -> cfg.entityOutlineColorFriendly, (Consumer<Integer>) (v) -> cfg.entityOutlineColorFriendly = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.crosshair",
(IntSupplier) () -> cfg.crosshairColor, (Consumer<Integer>) (v) -> cfg.crosshairColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.crosshairPlayer",
(IntSupplier) () -> cfg.crosshairColorPlayer, (Consumer<Integer>) (v) -> cfg.crosshairColorPlayer = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.crosshairHostile",
(IntSupplier) () -> cfg.crosshairColorHostile, (Consumer<Integer>) (v) -> cfg.crosshairColorHostile = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.crosshairPassive",
(IntSupplier) () -> cfg.crosshairColorPassive, (Consumer<Integer>) (v) -> cfg.crosshairColorPassive = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.crosshairNeutral",
(IntSupplier) () -> cfg.crosshairColorNeutral, (Consumer<Integer>) (v) -> cfg.crosshairColorNeutral = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.crosshairOther",
(IntSupplier) () -> cfg.crosshairColorOther, (Consumer<Integer>) (v) -> cfg.crosshairColorOther = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.hitMarker",
(IntSupplier) () -> cfg.hitColor, (Consumer<Integer>) (v) -> cfg.hitColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.killMarker",
(IntSupplier) () -> cfg.killColor, (Consumer<Integer>) (v) -> cfg.killColor = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.breakStart",
(IntSupplier) () -> cfg.blockBreakIndicatorColorStart, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorColorStart = v, true));
        colors.entries.add(new ConfigEntryDescriptor("color.breakEnd",
(IntSupplier) () -> cfg.blockBreakIndicatorColorEnd, (Consumer<Integer>) (v) -> cfg.blockBreakIndicatorColorEnd = v, true));
        treeRoots.add(colors);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(0, 0, width, height, CLR_BG);
        int rx = LEFT_W + 8, rw = width - rx - 8;
        Gui.drawRect(0, 0, LEFT_W, height, CLR_PANEL);
        Gui.drawRect(0, 0, LEFT_W, 32, 0xFF222244);
        drawCenteredString(mc.fontRendererObj, I18n.format("gui.modules"), LEFT_W / 2, 10, CLR_TEXT);
        Gui.drawRect(0, 32, LEFT_W, 33, 0xFF333355);
        int treeY = 38, y = treeY - treeScrollOffset;
        for (ConfigTreeNode root : treeRoots) {
            if (y + 18 >= treeY && y <= height) {
                Gui.drawRect(4, y, LEFT_W - 4, y + 18, 0x082A2A4E);
                Gui.drawRect(8, y + 5, 16, y + 13, root.dotColor);
                mc.fontRendererObj.drawString(I18n.format(root.labelKey), 22, y + 4, 0xFF8888AA, false);
                mc.fontRendererObj.drawString(root.expanded ? "\u25BC" : "\u25B6", LEFT_W - 18, y + 4, 0xFF555577, false);
            }
            y += 20;
            if (!root.expanded) continue;
            for (ConfigTreeNode child : root.children) {
                boolean sel = (child == selectedNode);
                if (y + 18 >= treeY && y <= height) {
                    if (sel) Gui.drawRect(4, y, LEFT_W - 4, y + 16, 0xFF333366);
                    mc.fontRendererObj.drawString(I18n.format(child.labelKey), 22, y + 2, sel ? 0xFFEEEEFF : 0xFFAAAAAA, false);
                    if (!child.isLeaf())
                        mc.fontRendererObj.drawString(child.expanded ? "\u25BC" : "\u25B6", LEFT_W - 18, y + 2, 0xFF555577, false);
                }
                y += 18;
                if (child.isLeaf() || !child.expanded) continue;
                for (ConfigTreeNode sub : child.children) {
                    boolean ssel = (sub == selectedNode);
                    if (y + 16 >= treeY && y <= height) {
                        if (ssel) Gui.drawRect(4, y, LEFT_W - 4, y + 14, 0xFF444477);
                        mc.fontRendererObj.drawString(I18n.format(sub.labelKey), 34, y + 1, ssel ? 0xFFEEEEFF : 0xFF888888, false);
                    }
                    y += 16;
                }
            }
        }
        if (selectedNode == null || localEntries == null || localEntries.isEmpty()) {
            drawCenteredString(mc.fontRendererObj, I18n.format("gui.select_category"), width / 2, height / 2, CLR_TEXT_SEC);
            super.drawScreen(mouseX, mouseY, partialTicks); return;
        }
        Gui.drawRect(rx, 0, rx + rw, 32, CLR_PANEL);
        drawCenteredString(mc.fontRendererObj, I18n.format(selectedNode.labelKey), rx + rw / 2, 10, CLR_TEXT);
        Gui.drawRect(rx, 32, rx + rw, 33, CLR_DIVIDER);
        int sx = rx + 4, sw = rw - 8;
        Gui.drawRect(sx, 36, sx + sw, 54, 0xFF2A2A3E);
        mc.fontRendererObj.drawString(isSearching ? searchQuery + "_" : I18n.format("gui.search_hint"), sx + 4, 40, CLR_TEXT_SEC, false);
        int ey = 60;
        int visStart = Math.max(0, entriesScrollOffset / ENTRY_H);
        int maxVis = (height - ey) / ENTRY_H + 2;
        int drawEnd = Math.min(localEntries.size(), visStart + maxVis);
        hoveredEntry = null;
        String searchQ = searchQuery.trim().toLowerCase();
        for (int i = visStart; i < drawEnd; i++) {
            ConfigEntryDescriptor desc = localEntries.get(i);
            if (!searchQ.isEmpty() && !I18n.format(desc.labelKey).toLowerCase().contains(searchQ)) continue;
            int yi = ey + i * ENTRY_H - entriesScrollOffset;
            if (yi + ENTRY_H < 32 || yi > height) continue;
            boolean hover = mouseX >= rx && mouseX <= rx + rw && mouseY >= yi && mouseY <= yi + ENTRY_H;
            int cardBg = hover ? CLR_CARD_HOVER : CLR_CARD;
            Gui.drawRect(rx + 2, yi + 1, rx + rw - 2, yi + ENTRY_H - 1, cardBg);
            Gui.drawRect(rx + 2, yi + 1, rx + 5, yi + ENTRY_H - 1, CLR_BLUE);
            Gui.drawRect(rx + 2, yi + ENTRY_H - 1, rx + rw - 2, yi + ENTRY_H, 0xFF111122);
            if (i < drawEnd - 1) Gui.drawRect(rx + 8, yi + ENTRY_H, rx + rw - 8, yi + ENTRY_H + 1, CLR_DIVIDER);
            if (hover) hoveredEntry = desc;
            mc.fontRendererObj.drawString(I18n.format(desc.labelKey), rx + 10, yi + (ENTRY_H - mc.fontRendererObj.FONT_HEIGHT) / 2, CLR_TEXT, false);
            int vx = rx + rw / 2 + 4;
            switch (desc.type) {
                case BOOLEAN: drawBooleanToggle(vx, yi + 2, ENTRY_H, desc, mouseX, mouseY); break;
                case INT: drawIntSpinner(vx, yi + 2, ENTRY_H, desc, mouseX, mouseY); break;
                case FLOAT: drawFloatSpinner(vx, yi + 2, ENTRY_H, desc, mouseX, mouseY); break;
                case COLOR: drawColorPreview(vx, yi + 2, ENTRY_H, desc, mouseX, mouseY); break;
                case ENUM: drawEnumSpinner(vx, yi + 2, ENTRY_H, desc, mouseX, mouseY); break;
            }
            if (pulseLabel != null && pulseLabel.equals(desc.labelKey) && System.currentTimeMillis() < pulseEndTime) {
                long rem = pulseEndTime - System.currentTimeMillis();
                int pa = (int)((rem / 600.0) * (0x3C - 0x0A) + 0x0A);
                if (pa > 0x3C) pa = 0x3C; if (pa < 0x0A) pa = 0x0A;
                Gui.drawRect(rx + 2, yi + 1, rx + rw - 2, yi + ENTRY_H - 1, (pa << 24) | 0x00FFD700);
            }
        }
        int btnY = height - 32;
        Gui.drawRect(rx + 4, btnY, rx + 84, btnY + 24, CLR_SAVE);
        drawCenteredString(mc.fontRendererObj, I18n.format("gui.save"), rx + 44, btnY + (24 - mc.fontRendererObj.FONT_HEIGHT) / 2, 0xFFAAFFAA);
        Gui.drawRect(rx + 88, btnY, rx + 168, btnY + 24, CLR_RESET);
        drawCenteredString(mc.fontRendererObj, I18n.format("gui.reset"), rx + 128, btnY + (24 - mc.fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFAAAA);
        Gui.drawRect(rx + rw - 84, btnY, rx + rw - 4, btnY + 24, 0xFF2A2A2A);
        drawCenteredString(mc.fontRendererObj, I18n.format("gui.cancel"), rx + rw - 44, btnY + (24 - mc.fontRendererObj.FONT_HEIGHT) / 2, CLR_TEXT);
        if (hoveredEntry != null && hoveredEntry.tooltipKey != null) {
            String tip = I18n.format(hoveredEntry.tooltipKey);
            int tw = mc.fontRendererObj.getStringWidth(tip) + 10;
            int tx = Math.min(mouseX + 12, width - tw - 4);
            int ty = Math.max(mouseY - 12, 4);
            Gui.drawRect(tx, ty, tx + tw, ty + 16, 0xCC222222);
            mc.fontRendererObj.drawString(tip, tx + 5, ty + 3, 0xCCFFFFAA, false);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

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

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        int rx = LEFT_W + 8, rw = width - rx - 8;
        int treeY = 38, y = treeY - treeScrollOffset;
        for (ConfigTreeNode root : treeRoots) {
            if (mouseX >= 4 && mouseX <= LEFT_W - 4 && mouseY >= y && mouseY <= y + 18) { root.expanded = !root.expanded; return; }
            y += 20;
            if (!root.expanded) continue;
            for (ConfigTreeNode child : root.children) {
                if (mouseX >= 4 && mouseX <= LEFT_W - 4 && mouseY >= y && mouseY <= y + 18) {
                    if (child.isLeaf()) { if (child != selectedNode) { selectedNode = child; localEntries = child.entries; entriesScrollOffset = 0; triggerPulse(child.labelKey); } }
                    else { child.expanded = !child.expanded; }
                    return;
                }
                y += 18;
                if (child.isLeaf() || !child.expanded) continue;
                for (ConfigTreeNode sub : child.children) {
                    if (mouseX >= 4 && mouseX <= LEFT_W - 4 && mouseY >= y && mouseY <= y + 16) {
                        if (sub != selectedNode) { selectedNode = sub; localEntries = sub.entries; entriesScrollOffset = 0; triggerPulse(sub.labelKey); }
                        return;
                    }
                    y += 16;
                }
            }
        }
        int sx = rx + 4, sw = rw - 8;
        if (mouseX >= sx && mouseX <= sx + sw && mouseY >= 36 && mouseY <= 54) { isSearching = !isSearching; if (isSearching) searchQuery = ""; return; }
        if (selectedNode != null && localEntries != null) {
            int ey = 60;
            int visStart = Math.max(0, entriesScrollOffset / ENTRY_H);
            int maxVis = (height - ey) / ENTRY_H + 2;
            int drawEnd = Math.min(localEntries.size(), visStart + maxVis);
            for (int i = visStart; i < drawEnd; i++) {
                int yi = ey + i * ENTRY_H - entriesScrollOffset;
                if (yi + ENTRY_H < 32 || yi > height) continue;
                if (mouseX < rx || mouseX > rx + rw || mouseY < yi || mouseY > yi + ENTRY_H) continue;
                ConfigEntryDescriptor desc = localEntries.get(i);
                int vx = rx + rw / 2 + 4;
                switch (desc.type) {
                    case BOOLEAN: {
                        boolean val = ((BooleanSupplier)desc.getter).getAsBoolean();
                        ((Consumer<Boolean>)desc.setter).accept(!val);
                        triggerPulse(desc.labelKey); return;
                    }
                    case INT: {
                        int cur = ((IntSupplier)desc.getter).getAsInt();
                        int step = (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54)) ? 100 : 1;
                        if (mouseX >= vx + 85 && mouseX <= vx + 100)
                            ((Consumer<Integer>)desc.setter).accept(Math.max(desc.minInt, cur - step));
                        else if (mouseX >= vx + 105 && mouseX <= vx + 120)
                            ((Consumer<Integer>)desc.setter).accept(Math.min(desc.maxInt, cur + step));
                        triggerPulse(desc.labelKey); return;
                    }
                    case FLOAT: {
                        float cur = (float)((DoubleSupplier)desc.getter).getAsDouble();
                        float step = (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54)) ? 10.0F : 1.0F;
                        if (mouseX >= vx + 85 && mouseX <= vx + 100)
                            ((Consumer<Float>)desc.setter).accept(Math.max(desc.minFloat, cur - step));
                        else if (mouseX >= vx + 105 && mouseX <= vx + 120)
                            ((Consumer<Float>)desc.setter).accept(Math.min(desc.maxFloat, cur + step));
                        triggerPulse(desc.labelKey); return;
                    }
                    case COLOR:
                        if (mouseX >= vx && mouseX <= vx + 80 && mouseY >= yi + 2 && mouseY <= yi + ENTRY_H - 2) {
                            int cur = ((IntSupplier)desc.getter).getAsInt();
                            mc.displayGuiScreen(new GuiColorPicker(this, (IntSupplier)desc.getter, (Consumer<Integer>)desc.setter));
                        }
                        return;
                    case ENUM: {
                        if (mouseX >= vx + 10 && mouseX <= vx + 100 && mouseY >= yi + 2 && mouseY <= yi + ENTRY_H - 2) {
                            String[] vals = desc.enumValues;
                            String cur = (String)((Supplier)desc.getter).get();
                            int nxtIdx = 0;
                            for (int j = 0; j < vals.length; j++) if (vals[j].equals(cur)) nxtIdx = (j + 1) % vals.length;
                            ((Consumer<String>)desc.setter).accept(vals[nxtIdx]);
                            triggerPulse(desc.labelKey);
                        }
                        return;
                    }
                }
            }
        }
        int btnY = height - 32;
        if (mouseX >= rx + 4 && mouseX <= rx + 84 && mouseY >= btnY && mouseY <= btnY + 24) { saveConfig(); return; }
        if (mouseX >= rx + 88 && mouseX <= rx + 168 && mouseY >= btnY && mouseY <= btnY + 24) { BetterPlayerHUD.config.loadConfig(); buildFromConfig(); return; }
        if (mouseX >= rx + rw - 84 && mouseX <= rx + rw - 4 && mouseY >= btnY && mouseY <= btnY + 24) { mc.displayGuiScreen(null); return; }
    }
    @Override
    public void handleMouseInput() {
        try {
            int d = Mouse.getEventDWheel();
            if (d != 0) {
                int amt = (d > 0) ? -22 : 22;
                if (Mouse.getX() * width / mc.displayWidth < LEFT_W) {
                    treeScrollOffset = Math.max(0, Math.min(computeMaxTreeScroll(), treeScrollOffset + amt));
                } else if (localEntries != null) {
                    int maxS = Math.max(0, localEntries.size() * ENTRY_H - (height - 80));
                    entriesScrollOffset = Math.max(0, Math.min(maxS, entriesScrollOffset + amt));
                }
            }
        } catch (Exception e) { }
    }
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) { mc.displayGuiScreen(null); return; }
        if (isSearching) {
            if (keyCode == 14 && searchQuery.length() > 0) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            else if (keyCode == 28 || keyCode == 15) isSearching = false;
            else if (typedChar >= 32 && typedChar < 127) searchQuery += typedChar;
            return;
        }
        if (keyCode == 60) {
            // F9 - no-op in tree version
        }
    }
    private void triggerPulse(String labelKey) {
        pulseLabel = labelKey; pulseEndTime = System.currentTimeMillis() + 600;
    }

    private boolean searchMatches(ConfigEntryDescriptor entry) {
        if (searchQuery.isEmpty()) return true;
        String lower = searchQuery.toLowerCase();
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
