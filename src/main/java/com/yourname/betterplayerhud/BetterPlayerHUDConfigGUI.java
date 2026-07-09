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
                "bhud.compass", "bhud.compass", getCompassConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("compassColor", "罗盘颜色") },
                "Compass HUD"));

        // === 模块2：玩家信息HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.player", "bhud.player", getPlayerConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("healthColorSafe", "安全血量"),
                        new ColorPreviewHelper.ColorInfo("healthColorWarning", "警告血量"),
                        new ColorPreviewHelper.ColorInfo("healthColorDanger", "危险血量"),
                        new ColorPreviewHelper.ColorInfo("armorColor", "护甲颜色"),
                        new ColorPreviewHelper.ColorInfo("hungerColor", "饥饿度颜色"),
                }, "Player HUD"));

        // === 模块7：距离HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.distance", "bhud.distance", getDistanceConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("distanceColor", "距离颜色"),
                        new ColorPreviewHelper.ColorInfo("targetInfoColor", "目标信息颜色"),
                        new ColorPreviewHelper.ColorInfo("blockInfoColor", "方块信息颜色"),
                }, "Distance HUD"));

        // === 模块9：按键显示 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.keys", "bhud.keys", getKeysDisplayConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("keysActiveColor", "按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysInactiveColor", "未按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysTextColor", "文字颜色"),
                }, "Keys Display"));

        // === 模块13：方块描边 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.blockoutline", "bhud.blockoutline", getBlockOutlineConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("blockOutlineColor", "描边颜色") },
                "Block Outline"));

        // === 模块14：实体高亮 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.entity", "bhud.entity", getEntityHighlightConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorHostile", "敌对生物"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorNeutral", "中立生物"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorFriendly", "友好生物"),
                }, "Entity Highlight"));

        // === RGB 动态流光 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.rgb", "bhud.rgb", getRGBConfigElements(),
                new ColorPreviewHelper.ColorInfo[0], "RGB Flow"));

        // === 模块18：目标血量 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.targethp", "bhud.targethp", getTargetHPConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("targetHPColor", "血条颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPBackColor", "背景颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPTextColor", "文字颜色"),
                }, "Target HP"));

        // === 模块19：性能检测 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.performance", "bhud.performance", getPerformanceConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("performanceTextColor", "文字颜色"),
                }, "Performance"));

        // === 模块20：自定义准星 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.crosshair", "bhud.crosshair", getCrosshairConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("crosshairColor", "准星颜色") },
                "Crosshair"));

        // === 模块21：命中标识 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "bhud.hitmarker", "bhud.hitmarker", getHitMarkerConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("hitColor", "击中颜色"),
                        new ColorPreviewHelper.ColorInfo("killColor", "击杀颜色"),
                        new ColorPreviewHelper.ColorInfo("hitMarkerBorderColor", "边框颜色"),
                        new ColorPreviewHelper.ColorInfo("hitMarkerKillBorderColor", "击杀边框颜色"),
                }, "Hit Marker"));

        // === 模块22：药水效果 HUD ===
        elements.add(new DummyConfigElement.DummyCategoryElement(
                "bhud.potion", "bhud.potion", getPotionConfigElements()));

        // === 模块23：装备&手持物品 HUD ===
        elements.add(new DummyConfigElement.DummyCategoryElement(
                "bhud.equip", "bhud.equip", getEquipConfigElements()));

        return elements;
    }

    // ═══════════════════════════════════════════════════════════════
    //  罗盘 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getCompassConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.compass.basic", "bhud.compass.basic", el(
                "showCompassHUD", "xPosition", "yPosition", "scale")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.compass.display", "bhud.compass.display", el(
                "displayStyle", "degreeMarkInterval", "dynamicScaling")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.compass.elements", "bhud.compass.elements", el(
                "showCompassNeedle", "showDegreeMarks", "showHorizon")));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  玩家信息 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getPlayerConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.player.basic", "bhud.player.basic", el(
                "showHealthHUD", "showArmorHUD", "showHungerHUD")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.player.pos", "bhud.player.pos", el(
                "healthHudX", "healthHudY", "headSize", "headTextSpacing", "showPlayerHead")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "healthColorSafe");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "healthColorWarning");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "healthColorDanger");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "armorColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "hungerColor");
        list.add(ColorPreviewHelper.createPreviewCategory("bhud.player.color", "bhud.player.color", colorList,
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
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.distance.basic", "bhud.distance.basic", el(
                "showDistanceHUD", "distanceHudX", "distanceHudY", "distancePrecision")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.distance.extra", "bhud.distance.extra", el(
                "showTargetInfo", "showBlockCoordinates", "showBlockHardness", "showRequiredTool")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "distanceColor");
        list.add(ColorPreviewHelper.createPreviewCategory("bhud.distance.color", "bhud.distance.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("distanceColor", "距离颜色") }, ""));
        List<IConfigElement> colorList2 = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList2, cfg(), cat(), "targetInfoColor");
        ColorPreviewHelper.addColorElements(colorList2, cfg(), cat(), "blockInfoColor");
        list.add(ColorPreviewHelper.createPreviewCategory("bhud.distance.extra_color", "bhud.distance.extra_color", colorList2,
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
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.keys.basic", "bhud.keys.basic", el(
                "showKeysDisplay", "keysDisplayX", "keysDisplayY", "keysOpacity", "keysScale")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.keys.appearance", "bhud.keys.appearance", el(
                "keysSize", "keysSpacing", "keysTextColor", "showKeysBackground")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "keysActiveColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "keysInactiveColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "keysTextColor");
        list.add(ColorPreviewHelper.createPreviewCategory("bhud.keys.color", "bhud.keys.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("keysActiveColor", "按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysInactiveColor", "未按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysTextColor", "文字颜色"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  方块描边 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getBlockOutlineConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.blockoutline.basic", "bhud.blockoutline.basic", el(
                "enableBlockHighlight", "blockOutlineWidth")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.blockoutline.face", "bhud.blockoutline.face", el(
                "drawVisibleFacesOnly", "drawVisibleFacesOnlyBlocks")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "blockOutlineColor");
        list.add(ColorPreviewHelper.createPreviewCategory("bhud.blockoutline.color", "bhud.blockoutline.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("blockOutlineColor", "描边颜色") }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  实体高亮 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getEntityHighlightConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.entity.basic", "bhud.entity.basic", el(
                "enableEntityHighlight", "entityOutlineWidth", "hideHitboxForInvisible")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.entity.face", "bhud.entity.face", el(
                "drawVisibleFacesOnlyEntities")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "entityOutlineColorHostile");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "entityOutlineColorNeutral");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "entityOutlineColorFriendly");
        list.add(ColorPreviewHelper.createPreviewCategory("bhud.entity.color", "bhud.entity.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorHostile", "敌对"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorNeutral", "中立"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorFriendly", "友好"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  RGB — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getRGBConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.rgb.basic", "bhud.rgb.basic", el(
                "enableRGBMode", "rgbFlowMode", "rgbColorAlgo", "rgbSpeed", "rgbStepMs")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.rgb.apply", "bhud.rgb.apply", el(
                "rgbApplyBlockOutline", "rgbApplyEntityHitbox")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.rgb.keys", "bhud.rgb.keys", el(
                "keyBindToggleRGB", "keyBindToggleBlockOutline", "keyBindToggleEntityHitbox")));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  目标血量 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getTargetHPConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.targethp.basic", "bhud.targethp.basic", el(
                "targetHPEnabled", "targetHPStyle", "targetHPMaxRange")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.targethp.show", "bhud.targethp.show", el(
                "targetHPShowPlayers", "targetHPShowMobs", "targetHPShowBosses", "targetHPShowSelf")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.targethp.labels", "bhud.targethp.labels", el(
                "targetHPShowName", "targetHPShowLabels", "targetHPShowArmorLabels", "targetHPShowFace", "targetHPFaceSize")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.targethp.pos", "bhud.targethp.pos", el(
                "targetHPOffsetX", "targetHPOffsetY", "targetHPBarWidth", "targetHPShowArmor")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "targetHPColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "targetHPBackColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "targetHPTextColor");
        list.add(ColorPreviewHelper.createPreviewCategory("bhud.targethp.color", "bhud.targethp.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("targetHPColor", "血条颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPBackColor", "背景颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPTextColor", "文字颜色"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  性能检测 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getPerformanceConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.performance.basic", "bhud.performance.basic", el(
                "showPerformanceHUD", "performanceHudX", "performanceHudY")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.performance.toggles", "bhud.performance.toggles", el(
                "showFPS", "showXYZ", "showTPS", "showPing", "showServerIP")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.performance.tpscolor", "bhud.performance.tpscolor", el(
                "serverInfoGoodTpsThreshold", "serverInfoMediumTpsThreshold")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "performanceTextColor");
        list.add(ColorPreviewHelper.createPreviewCategory("bhud.performance.color", "bhud.performance.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("performanceTextColor", "文字颜色") }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  自定义准星 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getCrosshairConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.crosshair.basic", "bhud.crosshair.basic", el(
                "enableCrosshair", "crosshairStyle", "crosshairLength", "crosshairGap", "crosshairThickness",
                "crosshairAlwaysShow", "crosshairShowInThirdPerson")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.crosshair.arms", "bhud.crosshair.arms", el(
                "crosshairArmUp", "crosshairArmDown", "crosshairArmLeft", "crosshairArmRight", "crosshairXOffset", "crosshairYOffset")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.crosshair.spread", "bhud.crosshair.spread", el(
                "crosshairSpread", "crosshairSpreadAmount", "crosshairSpreadWalk", "crosshairSpreadJump",
                "crosshairSpreadBow", "crosshairSpreadBowInverted")));
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.crosshair.detail", "bhud.crosshair.detail", el(
                "crosshairDotSize", "crosshairCircleRadius", "crosshairCircleSegments", "crosshairRotation")));
        List<IConfigElement> colorList = new ArrayList<>();
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "crosshairColor");
        ColorPreviewHelper.addColorElements(colorList, cfg(), cat(), "crosshairOutlineColor");
        list.add(ColorPreviewHelper.createPreviewCategory("bhud.crosshair.color", "bhud.crosshair.color", colorList,
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("crosshairColor", "准星颜色"),
                        new ColorPreviewHelper.ColorInfo("crosshairOutlineColor", "描边颜色"),
                }, ""));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  命中标识 — 子分类
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getHitMarkerConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.hitmarker.audio", "bhud.hitmarker.audio", el(
                "enableHitMarker", "enableHitSounds", "enableKillSound", "soundVolume")));

        list.add(ColorPreviewHelper.createPreviewCategory("bhud.hitmarker.hit", "bhud.hitmarker.hit",
                el("hitColor", "hitSize", "hitAlpha", "hitBloodIntensity"),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("hitColor", "击中颜色") }, ""));

        list.add(ColorPreviewHelper.createPreviewCategory("bhud.hitmarker.kill", "bhud.hitmarker.kill",
                el("killColor", "killSize", "killAlpha"),
                new ColorPreviewHelper.ColorInfo[]{ new ColorPreviewHelper.ColorInfo("killColor", "击杀颜色") }, ""));

        list.add(ColorPreviewHelper.createPreviewCategory("bhud.hitmarker.border", "bhud.hitmarker.border",
                el("hitMarkerEnableBorder", "hitMarkerBorderColor", "hitMarkerBorderWidth", "hitMarkerKillBorderColor"),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("hitMarkerBorderColor", "边框颜色"),
                        new ColorPreviewHelper.ColorInfo("hitMarkerKillBorderColor", "击杀边框颜色"),
                }, ""));

        list.add(new DummyConfigElement.DummyCategoryElement("bhud.hitmarker.rotation", "bhud.hitmarker.rotation", el(
                "hitMarkerRandomRotate", "hitMarkerRandomRotateStrength")));
        return list;
    }

    // ================================================================
    //  模块23：装备&手持物品 HUD
    // ================================================================
    private static List<IConfigElement> getEquipConfigElements() {
        return el("enableArmorHUD", "enableHeldItemHUD");
    }

    // ================================================================
    //  模块22：药水效果 HUD
    // ================================================================
    private static List<IConfigElement> getPotionConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new DummyConfigElement.DummyCategoryElement("bhud.potion.basic", "bhud.potion.basic", el(
                "enablePotionHUD", "potionTextColor", "potionXOffset", "potionYOffset")));
        return list;
    }
}
