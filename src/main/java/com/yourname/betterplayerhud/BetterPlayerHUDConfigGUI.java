package com.yourname.betterplayerhud;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.IConfigElement;
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
                BetterPlayerHUD.MODID, false, false, "Compass Mod Configuration");
    }

    // ═══════════════════════════════════════════════════════════════
    //  辅助：向列表添加一个普通元素
    // ═══════════════════════════════════════════════════════════════
    private static void addEl(List<IConfigElement> list, String key) {
        Configuration c = cfg();
        if (c != null && c.getCategory(cat()).containsKey(key)) {
            list.add(new ConfigElement(c.getCategory(cat()).get(key)));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  主结构
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> elements = new ArrayList<>();

        Configuration c = cfg();
        if (c == null) {
            System.err.println("Compass config not initialized!");
            return elements;
        }

        // === 模块1：罗盘HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "罗盘HUD设置", "compassmod.category.compass",
                getCompassConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("compassColor", "罗盘颜色")
                },
                "Compass HUD"
        ));

        // === 模块2：玩家信息HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "玩家信息HUD设置", "compassmod.category.player",
                getPlayerConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("healthColorSafe", "安全血量"),
                        new ColorPreviewHelper.ColorInfo("healthColorWarning", "警告血量"),
                        new ColorPreviewHelper.ColorInfo("healthColorDanger", "危险血量"),
                        new ColorPreviewHelper.ColorInfo("armorColor", "护甲颜色"),
                        new ColorPreviewHelper.ColorInfo("hungerColor", "饥饿度颜色"),
                },
                "Player HUD"
        ));

        // === 模块3：速度HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "速度HUD设置", "compassmod.category.speed",
                getSpeedConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("speedColor", "速度颜色"),
                },
                "Speed HUD"
        ));

        // === 模块4：坐标HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "坐标HUD设置", "compassmod.category.coordinates",
                getCoordinatesConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("coordinatesColor", "坐标颜色"),
                        new ColorPreviewHelper.ColorInfo("dimensionColor", "维度颜色"),
                },
                "Coordinates HUD"
        ));

        // === 模块5：游戏时间HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "游戏时间HUD设置", "compassmod.category.gametime",
                getGameTimeConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("gameTimeColor", "游戏时间颜色"),
                },
                "Game Time HUD"
        ));

        // === 模块6：现实时间HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "现实时间HUD设置", "compassmod.category.realtime",
                getRealTimeConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("timeColor", "现实时间颜色"),
                },
                "Real Time HUD"
        ));

        // === 模块7：距离HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "距离HUD设置", "compassmod.category.distance",
                getDistanceConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("distanceColor", "距离颜色"),
                        new ColorPreviewHelper.ColorInfo("targetInfoColor", "目标信息颜色"),
                        new ColorPreviewHelper.ColorInfo("blockInfoColor", "方块信息颜色"),
                },
                "Distance HUD"
        ));

        // === 模块8：CPS HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "CPS（点击速度）HUD设置", "compassmod.category.cps",
                getCPSConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("leftCPSColor", "左键CPS"),
                        new ColorPreviewHelper.ColorInfo("rightCPSColor", "右键CPS"),
                },
                "CPS HUD"
        ));

        // === 模块9：按键显示 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "按键显示设置", "compassmod.category.keys",
                getKeysDisplayConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("keysActiveColor", "按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysInactiveColor", "未按下颜色"),
                        new ColorPreviewHelper.ColorInfo("keysTextColor", "文字颜色"),
                },
                "Keys Display"
        ));

        // === 模块10：强制疾跑 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "强制疾跑设置", "compassmod.category.forcesprint",
                getForceSprintConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("sprintStatusColor", "疾跑状态颜色"),
                },
                "Force Sprint"
        ));

        // === 模块11：强制潜行 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "强制潜行设置", "compassmod.category.forcesneak",
                getForceSneakConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("sneakStatusColor", "潜行状态颜色"),
                },
                "Force Sneak"
        ));

        // === 模块12：药水状态HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "药水状态HUD设置", "compassmod.category.potion",
                getPotionConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("potionBackgroundColor", "背景颜色"),
                        new ColorPreviewHelper.ColorInfo("potionTextColor", "文字颜色"),
                        new ColorPreviewHelper.ColorInfo("potionGoodEffectColor", "正面效果"),
                        new ColorPreviewHelper.ColorInfo("potionBadEffectColor", "负面效果"),
                        new ColorPreviewHelper.ColorInfo("potionNeutralEffectColor", "中性效果"),
                },
                "Potion HUD"
        ));

        // === 模块13：方块描边 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "方块描边设置", "compassmod.category.blockoutline",
                getBlockOutlineConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("blockOutlineColor", "描边颜色"),
                },
                "Block Outline"
        ));

        // === 模块14：实体高亮 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "实体碰撞箱高亮设置", "compassmod.category.entityhighlight",
                getEntityHighlightConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorHostile", "敌对生物"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorNeutral", "中立生物"),
                        new ColorPreviewHelper.ColorInfo("entityOutlineColorFriendly", "友好生物"),
                },
                "Entity Highlight"
        ));

        // === 模块15：物品信息HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "物品信息HUD设置", "compassmod.category.iteminfo",
                getItemInfoConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("itemBorderColor", "边框颜色"),
                        new ColorPreviewHelper.ColorInfo("itemNameColor", "物品名称"),
                        new ColorPreviewHelper.ColorInfo("itemCountColor", "物品数量"),
                        new ColorPreviewHelper.ColorInfo("itemDurabilityColor", "耐久度"),
                        new ColorPreviewHelper.ColorInfo("itemLowDurabilityColor", "低耐久度"),
                        new ColorPreviewHelper.ColorInfo("itemEnchantmentColor", "附魔颜色"),
                },
                "Item Info HUD"
        ));

        // === 模块16：低血量警告 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "低血量警告设置", "compassmod.category.lowhealth",
                getLowHealthConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("darkenColor", "变暗颜色"),
                },
                "Low Health Warning"
        ));

        // === 模块17：Ping HUD ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "Ping HUD设置", "compassmod.category.ping",
                getPingConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("pingColor", "默认颜色"),
                        new ColorPreviewHelper.ColorInfo("pingGoodColor", "良好延迟"),
                        new ColorPreviewHelper.ColorInfo("pingMediumColor", "中等延迟"),
                        new ColorPreviewHelper.ColorInfo("pingBadColor", "高延迟"),
                },
                "Ping HUD"
        ));

        // === 模块18：目标血量显示 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "目标血量显示设置", "compassmod.category.targethp",
                getTargetHPConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("targetHPColor", "血条颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPBackColor", "背景颜色"),
                        new ColorPreviewHelper.ColorInfo("targetHPTextColor", "文字颜色"),
                },
                "Target HP"
        ));

        return elements;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块1：罗盘HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getCompassConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showCompassHUD");
        addEl(list, "xPositionOffset");
        addEl(list, "yPositionOffset");
        addEl(list, "compassScale");
        addEl(list, "displayStyle");
        addEl(list, "showDegreeMarks");
        addEl(list, "showCompassNeedle");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "compassColor");
        addEl(list, "degreeMarkInterval");
        addEl(list, "dynamicScaling");
        addEl(list, "showHorizon");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块2：玩家信息HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getPlayerConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showHealthHUD");
        addEl(list, "healthHudXOffset");
        addEl(list, "healthHudYOffset");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "healthColorSafe");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "healthColorWarning");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "healthColorDanger");
        addEl(list, "showArmorHUD");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "armorColor");
        addEl(list, "showHungerHUD");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "hungerColor");
        addEl(list, "showPlayerHead");
        addEl(list, "headSize");
        addEl(list, "headTextSpacing");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块3：速度HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getSpeedConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showSpeedHUD");
        addEl(list, "speedHudXOffset");
        addEl(list, "speedHudYOffset");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "speedColor");
        addEl(list, "speedUnit");
        addEl(list, "speedPrecision");
        addEl(list, "showVerticalSpeed");
        addEl(list, "showExactAngle");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块4：坐标HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getCoordinatesConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showCoordinatesHUD");
        addEl(list, "coordinatesHudXOffset");
        addEl(list, "coordinatesHudYOffset");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "coordinatesColor");
        addEl(list, "coordinatesPrecision");
        addEl(list, "showDimension");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "dimensionColor");
        addEl(list, "showFacing");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块5：游戏时间HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getGameTimeConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showGameTimeHUD");
        addEl(list, "gameTimeHudXOffset");
        addEl(list, "gameTimeHudYOffset");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "gameTimeColor");
        addEl(list, "gameTime24Hour");
        addEl(list, "showGameDay");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块6：现实时间HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getRealTimeConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showRealTimeHUD");
        addEl(list, "timeHudXOffset");
        addEl(list, "timeHudYOffset");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "timeColor");
        addEl(list, "timeFormat");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块7：距离HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getDistanceConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showDistanceHUD");
        addEl(list, "distanceHudXOffset");
        addEl(list, "distanceHudYOffset");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "distanceColor");
        addEl(list, "distancePrecision");
        addEl(list, "showTargetInfo");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "targetInfoColor");
        addEl(list, "showBlockCoordinates");
        addEl(list, "showBlockHardness");
        addEl(list, "showRequiredTool");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "blockInfoColor");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块8：CPS HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getCPSConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showCPSHUD");
        addEl(list, "cpsHudXOffset");
        addEl(list, "cpsHudYOffset");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "leftCPSColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "rightCPSColor");
        addEl(list, "cpsUpdateInterval");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块9：按键显示
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getKeysDisplayConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showKeysDisplay");
        addEl(list, "keysDisplayX");
        addEl(list, "keysDisplayY");
        addEl(list, "keysSize");
        addEl(list, "keysSpacing");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "keysActiveColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "keysInactiveColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "keysTextColor");
        addEl(list, "keysScale");
        addEl(list, "showKeysBackground");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块10：强制疾跑
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getForceSprintConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableForceSprint");
        addEl(list, "forceSprintEnabled");
        addEl(list, "forceSprintKey");
        addEl(list, "showSprintStatus");
        addEl(list, "sprintStatusX");
        addEl(list, "sprintStatusY");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "sprintStatusColor");
        addEl(list, "sprintStatusScale");
        addEl(list, "showDebugInfo");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块11：强制潜行
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getForceSneakConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableForceSneak");
        addEl(list, "forceSneakEnabled");
        addEl(list, "forceSneakKey");
        addEl(list, "showSneakStatus");
        addEl(list, "sneakStatusX");
        addEl(list, "sneakStatusY");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "sneakStatusColor");
        addEl(list, "sneakPlaySound");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块12：药水状态HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getPotionConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showPotionHUD");
        addEl(list, "potionHudX");
        addEl(list, "potionHudY");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "potionBackgroundColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "potionTextColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "potionGoodEffectColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "potionBadEffectColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "potionNeutralEffectColor");
        addEl(list, "potionScale");
        addEl(list, "showPotionIcons");
        addEl(list, "showPotionNames");
        addEl(list, "showPotionDurations");
        addEl(list, "showPotionAmplifier");
        addEl(list, "showPotionBackground");
        addEl(list, "potionSpacing");
        addEl(list, "potionTextOffset");
        addEl(list, "potionTimeFormat");
        addEl(list, "showOnlyActivePotions");
        addEl(list, "maxPotionDisplay");
        addEl(list, "sortByDuration");
        addEl(list, "showInfiniteAsIcon");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块13：方块描边
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getBlockOutlineConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableBlockHighlight");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "blockOutlineColor");
        addEl(list, "blockOutlineWidth");
        addEl(list, "drawVisibleFacesOnlyBlocks");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块14：实体高亮
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getEntityHighlightConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableEntityHighlight");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "entityOutlineColorHostile");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "entityOutlineColorNeutral");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "entityOutlineColorFriendly");
        addEl(list, "entityOutlineWidth");
        addEl(list, "drawVisibleFacesOnlyEntities");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块15：物品信息HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getItemInfoConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableItemInfoHUD");
        addEl(list, "itemHudX");
        addEl(list, "itemHudY");
        addEl(list, "itemHudWidth");
        addEl(list, "itemHudHeight");
        addEl(list, "itemShowBackground");
        addEl(list, "itemBackgroundOpacity");
        addEl(list, "itemShowBorder");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "itemBorderColor");
        addEl(list, "showMainHandCountSeparate");
        addEl(list, "mainHandCountX");
        addEl(list, "mainHandCountY");
        addEl(list, "showMainHandItem");
        addEl(list, "showItemIcon");
        addEl(list, "showItemName");
        addEl(list, "showItemType");
        addEl(list, "showItemCount");
        addEl(list, "showDurability");
        addEl(list, "showDurabilityBar");
        addEl(list, "showEnchantments");
        addEl(list, "showArrowCount");
        addEl(list, "showArmorItems");
        addEl(list, "showArmorIcon");
        addEl(list, "showArmorName");
        addEl(list, "showArmorDurability");
        addEl(list, "showArmorEnchantments");
        addEl(list, "itemUseRarityColors");
        addEl(list, "itemLowDurabilityThreshold");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "itemNameColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "itemCountColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "itemDurabilityColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "itemLowDurabilityColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "itemEnchantmentColor");
        addEl(list, "showItemDamage");
        addEl(list, "showDamageBreakdown");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块16：低血量警告
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getLowHealthConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableLowHealthWarning");
        addEl(list, "lowHealthThreshold");
        addEl(list, "darkenIntensity");
        addEl(list, "vignetteSize");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "darkenColor");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块17：Ping HUD
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getPingConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "showPingHUD");
        addEl(list, "pingHudXOffset");
        addEl(list, "pingHudYOffset");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "pingColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "pingGoodColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "pingMediumColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "pingBadColor");
        addEl(list, "goodPingThreshold");
        addEl(list, "mediumPingThreshold");
        addEl(list, "showPingHistory");
        addEl(list, "pingHistorySize");
        addEl(list, "showRealPing");
        addEl(list, "useTrafficBasedPing");
        addEl(list, "showPingSource");
        addEl(list, "realPingUpdateInterval");
        addEl(list, "showNetworkDetails");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块18：目标血量显示 (Target HP)
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getTargetHPConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "targetHPEnabled");
        addEl(list, "targetHPStyle");
        addEl(list, "targetHPMaxRange");
        addEl(list, "targetHPShowPlayers");
        addEl(list, "targetHPShowMobs");
        addEl(list, "targetHPShowBosses");
        addEl(list, "targetHPShowSelf");
        addEl(list, "targetHPShowName");
        addEl(list, "targetHPOffsetX");
        addEl(list, "targetHPOffsetY");
        addEl(list, "targetHPShowArmor");
        addEl(list, "targetHPBarWidth");
        addEl(list, "targetHPShowLabels");
        addEl(list, "targetHPShowArmorLabels");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "targetHPColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "targetHPBackColor");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "targetHPTextColor");
        return list;
    }
}
