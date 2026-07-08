package com.yourname.betterplayerhud;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class BetterPlayerHUDConfigGUI extends GuiConfig {

    private static Configuration cfg() {
        return BetterPlayerHUD.config != null ? BetterPlayerHUD.config.config : null;
    }

    private static String cat() {
        return Configuration.CATEGORY_CLIENT;
    }

    public BetterPlayerHUDConfigGUI(GuiScreen parentScreen) {
        super(parentScreen, getConfigElements(),
                BetterPlayerHUD.MODID, false, false, "Better Player HUD Configuration");
    }

    // ═══════════════════════════════════════════════════════════════
    //  辅助
    // ═══════════════════════════════════════════════════════════════
    private static void addEl(List<IConfigElement> list, String key) {
        Configuration c = cfg();
        if (c != null && c.getCategory(cat()).containsKey(key)) {
            list.add(new ConfigElement(c.getCategory(cat()).get(key)));
        }
    }

    private static List<IConfigElement> el(String... keys) {
        List<IConfigElement> l = new ArrayList<>();
        for (String k : keys) addEl(l, k);
        return l;
    }

    private static List<IConfigElement> catEl(List<IConfigElement> items, String name, String langKey, ColorPreviewHelper.ColorInfo... colors) {
        List<IConfigElement> result = new ArrayList<>();
        result.add(ColorPreviewHelper.createPreviewCategory(name, langKey, items, colors, ""));
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  主结构
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> elements = new ArrayList<>();

        if (cfg() == null) {
            System.err.println("BHUD config not initialized!");
            return elements;
        }

        // === 模块1：罗盘HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "罗盘HUD设置", "bhud.compass", getCompassConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("compassColor", "罗盘颜色") },
                "Compass HUD"));

        // === 模块2：玩家信息HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "玩家信息HUD设置", "bhud.player", getPlayerConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("healthColorSafe", "安全血量"),
                        new ColorPreviewHelper.ColorInfo("healthColorWarning", "警告血量"),
                        new ColorPreviewHelper.ColorInfo("healthColorDanger", "危险血量"),
                        new ColorPreviewHelper.ColorInfo("armorColor", "护甲颜色"),
                        new ColorPreviewHelper.ColorInfo("hungerColor", "饥饿度颜色"),
                }, "Player HUD"));

        // === 模块7：距离HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "距离HUD设置", "bhud.distance", getDistanceConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("distanceColor", "距离颜色"),
                        new ColorPreviewHelper.ColorInfo("targetInfoColor", "目标信息颜色"),
                        new ColorPreviewHelper.ColorInfo("blockInfoColor", "方块信息颜色"),
                }, "Distance HUD"));

        // === 模块9：按键显示 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "按键显示设置", "bhud.keys", getKeysDisplayConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("keysActiveColor", "按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysInactiveColor", "未按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysTextColor", "文字颜色"),
                }, "Keys Display"));

        // === 模块13：方块描边 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "方块描边设置", "bhud.blockoutline", getBlockOutlineConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("blockOutlineColor", "描边颜色") },
                "Block Outline"));

        // === 模块14：实体高亮 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "实体碰撞箱高亮设置", "bhud.entity", getEntityHighlightConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorHostile", "敌对生物"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorNeutral", "中立生物"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorFriendly", "友好生物"),
                }, "Entity Highlight"));

        // === RGB 动态流光 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "RGB动态流光设置", "bhud.rgb", getRGBConfigElements(),
                new ColorPreviewHelper.ColorInfo[0], "RGB Flow"));

        // === 模块18：目标血量 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "目标血量显示设置", "bhud.targethp", getTargetHPConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("targetHPColor", "血条颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPBackColor", "背景颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPTextColor", "文字颜色"),
                }, "Target HP"));

        // === 模块19：服务器信息 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "服务器信息设置", "bhud.server", getServerInfoConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("serverInfoTextColor", "文字颜色"),
                }, "Server Info"));

        // === 模块20：自定义准星 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "自定义准星设置", "bhud.crosshair", getCrosshairConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("crosshairColor", "准星颜色") },
                "Crosshair"));

        // === 模块21：命中标识 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "命中标识设置", "bhud.hitmarker", getHitMarkerConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("hitColor", "击中颜色"),
                        new ColorPreviewHelper.ColorInfo("killColor", "击杀颜色"),
                        new ColorPreviewHelper.ColorInfo("hitMarkerBorderColor", "边框颜色"),
                        new ColorPreviewHelper.ColorInfo("hitMarkerKillBorderColor", "击杀边框颜色"),
                }, "Hit Marker"));

        return elements;
    }

    // ═══════════════════════════════════════════════════════════════
    //  罗盘 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getCompassConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("基本", "bhud.compass.basic", el(
                "showCompassHUD", "xPositionOffset", "yPositionOffset", "compassScale")));
        list.add(new DummyConfigElement.DummyCategoryElement("显示选项", "bhud.compass.display", el(
                "displayStyle", "degreeMarkInterval", "dynamicScaling")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "compassColor");
        list.add(new DummyConfigElement.DummyCategoryElement("元素开关", "bhud.compass.elements", el(
                "showCompassNeedle", "showDegreeMarks", "showHorizon")));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  玩家信息 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getPlayerConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("基本", "bhud.player.basic", el(
                "showHealthHUD", "showArmorHUD", "showHungerHUD", "showPlayerHead")));
        list.add(new DummyConfigElement.DummyCategoryElement("位置", "bhud.player.pos", el(
                "healthHudXOffset", "healthHudYOffset", "headSize", "headTextSpacing")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "healthColorSafe");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "healthColorWarning");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "healthColorDanger");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "armorColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "hungerColor");
        list.add(ColorPreviewHelper.createPreviewCategory("颜色", "bhud.player.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("healthColorSafe", "安全血量"),
                        new ColorPreviewHelper.ColorInfo("healthColorWarning", "警告血量"),
                        new ColorPreviewHelper.ColorInfo("healthColorDanger", "危险血量"),
                        new ColorPreviewHelper.ColorInfo("armorColor", "护甲颜色"),
                        new ColorPreviewHelper.ColorInfo("hungerColor", "饥饿度颜色"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  距离 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getDistanceConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("基本", "bhud.distance.basic", el(
                "showDistanceHUD", "distanceHudXOffset", "distanceHudYOffset", "distancePrecision")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "distanceColor");
        list.add(ColorPreviewHelper.createPreviewCategory("颜色", "bhud.distance.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("distanceColor", "距离颜色") }, ""));
        list.add(new DummyConfigElement.DummyCategoryElement("附加信息", "bhud.distance.extra", el(
                "showTargetInfo", "showBlockCoordinates", "showBlockHardness", "showRequiredTool")));
        List<IConfigElement> colorList2 = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList2, cfg(), cat(), "targetInfoColor");
        ColorPreviewHelper.addColorElements(colorList2, cfg(), cat(), "blockInfoColor");
        list.add(ColorPreviewHelper.createPreviewCategory("额外颜色", "bhud.distance.extra_color", colorList2,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("targetInfoColor", "目标信息颜色"),
                        new ColorPreviewHelper.ColorInfo("blockInfoColor", "方块信息颜色"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  按键显示 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getKeysDisplayConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("基本", "bhud.keys.basic", el(
                "showKeysDisplay", "keysDisplayX", "keysDisplayY", "keysScale")));
        list.add(new DummyConfigElement.DummyCategoryElement("外观", "bhud.keys.appearance", el(
                "keysSize", "keysSpacing", "showKeysBackground", "keysOpacity")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "keysActiveColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "keysInactiveColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "keysTextColor");
        list.add(ColorPreviewHelper.createPreviewCategory("颜色", "bhud.keys.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("keysActiveColor", "按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysInactiveColor", "未按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysTextColor", "文字颜色"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  方块描边 — 保持简单
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getBlockOutlineConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableBlockHighlight");
        addEl(list, "blockOutlineWidth");
        addEl(list, "drawVisibleFacesOnlyBlocks");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "blockOutlineColor");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  实体高亮 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getEntityHighlightConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("基本", "bhud.entity.basic", el(
                "enableEntityHighlight", "entityOutlineWidth", "hideHitboxForInvisible")));
        list.add(new DummyConfigElement.DummyCategoryElement("可见面", "bhud.entity.face", el(
                "drawVisibleFacesOnlyEntities")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "entityOutlineColorHostile");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "entityOutlineColorNeutral");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "entityOutlineColorFriendly");
        list.add(ColorPreviewHelper.createPreviewCategory("颜色（按阵营）", "bhud.entity.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorHostile", "敌对生物"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorNeutral", "中立生物"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorFriendly", "友好生物"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  RGB — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getRGBConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("基本", "bhud.rgb.basic", el(
                "enableRGBMode", "rgbFlowMode", "rgbColorAlgo", "rgbSpeed", "rgbStepMs")));
        list.add(new DummyConfigElement.DummyCategoryElement("应用对象", "bhud.rgb.apply", el(
                "rgbApplyBlockOutline", "rgbApplyEntityHitbox")));
        list.add(new DummyConfigElement.DummyCategoryElement("快捷键", "bhud.rgb.keys", el(
                "keyBindToggleBlockOutline", "keyBindToggleEntityHitbox", "keyBindToggleRGB")));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  目标血量 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getTargetHPConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("基本", "bhud.targethp.basic", el(
                "targetHPEnabled", "targetHPStyle", "targetHPMaxRange", "targetHPBarWidth")));
        list.add(new DummyConfigElement.DummyCategoryElement("显示对象", "bhud.targethp.show", el(
                "targetHPShowPlayers", "targetHPShowMobs", "targetHPShowBosses", "targetHPShowSelf")));
        list.add(new DummyConfigElement.DummyCategoryElement("标签", "bhud.targethp.labels", el(
                "targetHPShowName", "targetHPShowArmor", "targetHPShowLabels", "targetHPShowArmorLabels")));
        list.add(new DummyConfigElement.DummyCategoryElement("位置", "bhud.targethp.pos", el(
                "targetHPOffsetX", "targetHPOffsetY")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "targetHPColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "targetHPBackColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "targetHPTextColor");
        list.add(ColorPreviewHelper.createPreviewCategory("颜色", "bhud.targethp.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("targetHPColor", "血条颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPBackColor", "背景颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPTextColor", "文字颜色"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  服务器信息 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getServerInfoConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("基本", "bhud.server.basic", el(
                "serverInfoEnabled", "serverInfoOffsetX", "serverInfoOffsetY")));
        list.add(new DummyConfigElement.DummyCategoryElement("显示选项", "bhud.server.display", el(
                "serverInfoShowTPS", "serverInfoShowPing", "serverInfoShowIP",
                "serverInfoHighTpsThreshold", "serverInfoMediumTpsThreshold")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "serverInfoTextColor");
        list.add(ColorPreviewHelper.createPreviewCategory("颜色", "bhud.server.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("serverInfoTextColor", "文字颜色") }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  自定义准星 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getCrosshairConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("基本", "bhud.crosshair.basic", el(
                "enableCrosshair", "crosshairStyle", "crosshairLength", "crosshairGap", "crosshairThickness",
                "crosshairAlwaysShow", "crosshairShowInThirdPerson")));
        list.add(new DummyConfigElement.DummyCategoryElement("臂控制", "bhud.crosshair.arms", el(
                "crosshairArmUp", "crosshairArmDown", "crosshairArmLeft", "crosshairArmRight", "crosshairRotation")));
        list.add(new DummyConfigElement.DummyCategoryElement("扩散", "bhud.crosshair.spread", el(
                "crosshairSpread", "crosshairSpreadAmount", "crosshairSpreadWalk",
                "crosshairSpreadJump", "crosshairSpreadBow")));
        list.add(new DummyConfigElement.DummyCategoryElement("样式细节", "bhud.crosshair.detail", el(
                "crosshairXOffset", "crosshairYOffset", "crosshairDotSize",
                "crosshairCircleRadius", "crosshairCircleSegments")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "crosshairColor");
        addEl(colorList, "crosshairRGB");
        addEl(colorList, "crosshairOutline");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "crosshairOutlineColor");
        addEl(colorList, "crosshairOutlineWidth");
        list.add(ColorPreviewHelper.createPreviewCategory("颜色 & 描边", "bhud.crosshair.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("crosshairColor", "准星颜色"),
                        new ColorPreviewHelper.ColorInfo("crosshairOutlineColor", "描边颜色"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  命中标识 — 6 子分类（保持已有） + 开关
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getHitMarkerConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableHitMarker");

        list.add(new DummyConfigElement.DummyCategoryElement("🔊 音效", "bhud.hitmarker.audio", getAudioElements()));
        list.add(ColorPreviewHelper.createPreviewCategory("🎯 击中视觉效果", "bhud.hitmarker.hit",
                getHitVisualElements(),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("hitColor", "击中颜色") }, ""));
        list.add(ColorPreviewHelper.createPreviewCategory("💀 击杀视觉效果", "bhud.hitmarker.kill",
                getKillVisualElements(),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("killColor", "击杀颜色") }, ""));
        list.add(ColorPreviewHelper.createPreviewCategory("⬜ 边框", "bhud.hitmarker.border",
                getBorderElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("hitMarkerBorderColor", "边框颜色"),
                        new ColorPreviewHelper.ColorInfo("hitMarkerKillBorderColor", "击杀边框颜色"),
                }, ""));
        list.add(new DummyConfigElement.DummyCategoryElement("💧 粒子&聊天", "bhud.hitmarker.effects", getEffectsElements()));
        list.add(new DummyConfigElement.DummyCategoryElement("🔄 旋转", "bhud.hitmarker.rotation", getRotationElements()));
        return list;
    }

    private static List<IConfigElement> getAudioElements() {
        return el("enableHitSounds", "enableKillSound", "soundVolume");
    }

    private static List<IConfigElement> getHitVisualElements() {
        List<IConfigElement> l = new ArrayList<>();
        addEl(l, "hitAlpha"); addEl(l, "hitSize");
        ColorPreviewHelper.addColorElements(l, cfg(), cat(), "hitColor");
        return l;
    }

    private static List<IConfigElement> getKillVisualElements() {
        List<IConfigElement> l = new ArrayList<>();
        addEl(l, "killAlpha"); addEl(l, "killSize");
        ColorPreviewHelper.addColorElements(l, cfg(), cat(), "killColor");
        return l;
    }

    private static List<IConfigElement> getBorderElements() {
        List<IConfigElement> l = new ArrayList<>();
        addEl(l, "hitMarkerEnableBorder"); addEl(l, "hitMarkerBorderWidth");
        ColorPreviewHelper.addColorElements(l, cfg(), cat(), "hitMarkerBorderColor");
        ColorPreviewHelper.addColorElements(l, cfg(), cat(), "hitMarkerKillBorderColor");
        return l;
    }

    private static List<IConfigElement> getEffectsElements() {
        return el("hitBloodIntensity", "enableChatKillDetection");
    }

    private static List<IConfigElement> getRotationElements() {
        return el("hitMarkerRandomRotate", "hitMarkerRandomRotateStrength");
    }
}
