package com.yourname.betterplayerhud;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.File;

@SideOnly(Side.CLIENT)
public class BetterPlayerHUDConfig {

    // ================================================================
    //  Forge Configuration 实例
    // ================================================================
    public Configuration config;

    // ================================================================
    //  模块1：罗盘 HUD
    // ================================================================
    public boolean showCompassHUD = true;
    public int xPosition = 0;
    public int yPosition = 50;
    public float scale = 1.0f;
    public static final String STYLE_DETAILED = "detailed";
    public static final String STYLE_MINIMAL = "minimal";
    public static final String STYLE_SIMPLE = "simple";
    public String displayStyle = "detailed";
    public boolean showDegreeMarks = true;
    public boolean showCompassNeedle = true;
    public int compassColor = 0xFFFFFF;
    public int degreeMarkInterval = 15;
    public boolean dynamicScaling = true;
    public boolean showHorizon = false;

    // ================================================================
    //  模块2：玩家信息 HUD（血量/护甲/头像/饥饿度）
    // ================================================================
    public boolean showHealthHUD = true;
    public int healthHudX = 10;
    public int healthHudY = -50;
    public int healthColorSafe = 0xFF00FF00;
    public int healthColorWarning = 0xFFFFFF00;
    public int healthColorDanger = 0xFFFF0000;
    public boolean showArmorHUD = true;
    public int armorColor = 0xFF00FFFF;
    public boolean showHungerHUD = true;
    public int hungerColor = 0xFFFFA500; // 橙色，100为满
    public boolean showPlayerHead = true;
    public int headSize = 16;
    public int headTextSpacing = 2;

    // ================================================================
    //  模块3：速度 HUD
    // ================================================================
    public boolean showSpeedHUD = true;
    public int speedHudX = -10;
    public int speedHudY = -60;
    public int speedColor = 0x00FF00;
    public String speedUnit = "m/s";
    public int speedPrecision = 2;
    public boolean showVerticalSpeed = true;
    public boolean showExactAngle = true;

    // ================================================================
    //  模块4：坐标 HUD
    // ================================================================
    public boolean showCoordinatesHUD = true;
    public int coordinatesHudX = 10;
    public int coordinatesHudY = 50;
    public int coordinatesColor = 0xFFFFFF;
    public int coordinatesPrecision = 1;
    public boolean showDimension = true;
    public int dimensionColor = 0xAAAAAA;
    public boolean showFacing = true;

    // ================================================================
    //  模块5：游戏时间 HUD
    // ================================================================
    public boolean showGameTimeHUD = true;
    public int gameTimeHudX = -10;
    public int gameTimeHudY = -80;
    public int gameTimeColor = 0xFFFFAA;
    public boolean gameTime24Hour = true;
    public boolean showGameDay = true;

    // ================================================================
    //  模块6：现实时间 HUD
    // ================================================================
    public boolean showRealTimeHUD = true;
    public int timeHudX = -10;
    public int timeHudY = -30;
    public int timeColor = 0xFFFFFF;
    public String timeFormat = "yyyy/M/d";

    // ================================================================
    //  模块7：距离 HUD
    // ================================================================
    public boolean showDistanceHUD = true;
    public int distanceHudX = 490;
    public int distanceHudY = 280;
    public int distanceColor = 0x55FFFF;
    public int distancePrecision = 1;
    public boolean showTargetInfo = true;
    public int targetInfoColor = 0xAAAAAA;
    public boolean showBlockCoordinates = true;
    public boolean showBlockHardness = true;
    public boolean showRequiredTool = true;
    public int blockInfoColor = 0xAAAAAA;

    // ================================================================
    //  模块8：CPS HUD
    // ================================================================
    public boolean showCPSHUD = true;
    public int cpsHudX = 10;
    public int cpsHudY = 260;
    public int leftCPSColor = 0x00AAFF;
    public int rightCPSColor = 0xFFAA00;
    public int cpsUpdateInterval = 100;

    // ================================================================
    //  模块9：按键显示
    // ================================================================
    public boolean showKeysDisplay = true;
    public int keysDisplayX = 10;
    public int keysDisplayY = 150;
    public int keysSize = 24;
    public int keysSpacing = 4;
    public int keysActiveColor = 0xFFFFFFFF;
    public int keysInactiveColor = 0x80FFFFFF;
    public int keysTextColor = 0xFFFFFF;
    public float keysScale = 1.0f;
    public boolean showKeysBackground = false;
    public float keysOpacity = 1.0f;

    // ================================================================
    //  模块10：强制疾跑
    // ================================================================
    public boolean enableForceSprint = true;
    public boolean forceSprintEnabled = false;
    public int forceSprintKey = 23; // I键
    public boolean showSprintStatus = true;
    public int sprintStatusX = 100;
    public int sprintStatusY = 100;
    public int sprintStatusColor = 0x00FF00;
    public float sprintStatusScale = 1.0f;
    public boolean showDebugInfo = false;

    // ================================================================
    //  模块11：强制潜行
    // ================================================================
    public boolean enableForceSneak = true;
    public boolean forceSneakEnabled = false;
    public int forceSneakKey = Keyboard.KEY_N;
    public boolean showSneakStatus = false;
    public int sneakStatusX = 10;
    public int sneakStatusY = 30;
    public int sneakStatusColor = 0xFFFFFF00;
    public boolean sneakPlaySound = true;

    // ================================================================
    //  模块12：药水状态 HUD
    // ================================================================
    public boolean showPotionHUD = true;
    public int potionHudX = -120;
    public int potionHudY = 30;
    public int potionBackgroundColor = 0x80000000;
    public int potionTextColor = 0xFFFFFF;
    public int potionGoodEffectColor = 0x00FF00;
    public int potionBadEffectColor = 0xFF0000;
    public int potionNeutralEffectColor = 0xFFFF00;
    public float potionScale = 1.0f;
    public boolean showPotionIcons = true;
    public boolean showPotionNames = true;
    public boolean showPotionDurations = true;
    public boolean showPotionAmplifier = true;
    public boolean showPotionBackground = true;
    public int potionSpacing = 2;
    public int potionTextOffset = 12;
    // 注意：Java字段名 potionTimeFormatString，配置文件键名 "potionTimeFormat"
    public String potionTimeFormatString = "mm:ss";
    public boolean showOnlyActivePotions = true;
    public int maxPotionDisplay = 10;
    public boolean sortByDuration = true;
    public boolean showInfiniteAsIcon = true;

    // ================================================================
    //  模块13：方块描边
    // ================================================================
    public boolean enableBlockHighlight = true;
    public int blockOutlineColor = 0xFFFFFF00;
    public float blockOutlineWidth = 3.0f;
    public boolean drawVisibleFacesOnlyBlocks = true;

    // ================================================================
    //  模块14：实体碰撞箱高亮
    // ================================================================
    public boolean enableEntityHighlight = true;
    public int entityOutlineColorHostile = 0xFFFF0000;
    public int entityOutlineColorNeutral = 0xFFFFFF00;
    public int entityOutlineColorFriendly = 0xFF00FF00;
    public float entityOutlineWidth = 2.0f;
    public boolean drawVisibleFacesOnlyEntities = false;
    // 以下为原共用字段，保留兼容性（已废弃，使用上面按阵营区分的颜色）
    public int entityOutlineColor = 0xFF00FF00;
    public boolean drawVisibleFacesOnly = false;

    // ================================================================
    //  模块14b：RGB 动态流光 & 隐身隐藏（描边/碰撞箱专用）
    // ================================================================
    public boolean enableRGBMode = true;
    public int rgbStepMs = 400;                        // 0=平滑，>0=步进间隔(ms)
    public int rgbSpeed = 500;                          // 色相循环速度（ms/周期），越大越慢
    public boolean rgbApplyBlockOutline = true;
    public boolean rgbApplyEntityHitbox = true;
    public boolean hideHitboxForInvisible = false;      // 隐身实体不画碰撞箱
    public int keyBindToggleBlockOutline = 0;           // 方块描边开关快捷键
    public int keyBindToggleEntityHitbox = 0;           // 实体碰撞箱开关快捷键
    public int keyBindToggleRGB = 0;                    // RGB流光开关快捷键

    // ================================================================
    //  模块15：物品信息 HUD
    // ================================================================
    public boolean enableItemInfoHUD = true;
    public int itemHudX = 5;
    public int itemHudY = 330;
    public int itemHudWidth = 200;
    public int itemHudHeight = 120;
    public boolean itemShowBackground = false;
    public float itemBackgroundOpacity = 0.3f;
    public boolean itemShowBorder = true;
    public int itemBorderColor = 0xFF555555;
    public boolean showMainHandCountSeparate = true;
    public int mainHandCountX = 460;
    public int mainHandCountY = 470;
    public boolean showMainHandItem = true;
    public boolean showItemIcon = true;
    public boolean showItemName = false;
    public boolean showItemType = false;
    public boolean showItemCount = true;
    public boolean showDurability = true;
    public boolean showDurabilityBar = false;
    public boolean showEnchantments = true;
    public boolean showArrowCount = true;
    // 装备栏
    public boolean showArmorItems = true;
    public boolean showArmorIcon = true;
    public boolean showArmorName = false;
    public boolean showArmorDurability = true;
    public boolean showArmorEnchantments = true;
    // 样式
    public boolean itemUseRarityColors = true;
    public int itemLowDurabilityThreshold = 20;
    // 颜色
    public int itemNameColor = 0xFFFFFF;
    public int itemCountColor = 0xFFFF55;
    public int itemDurabilityColor = 0x55FF55;
    public int itemLowDurabilityColor = 0xFF5555;
    public int itemEnchantmentColor = 0xAAAAFF;
    // 物品伤害
    public boolean showItemDamage = true;
    public boolean showDamageBreakdown = false;

    // ================================================================
    //  模块16：低血量警告
    // ================================================================
    public boolean enableLowHealthWarning = true;
    public int lowHealthThreshold = 30;
    public float darkenIntensity = 0.5f;
    public float vignetteSize = 0.7f;
    public int darkenColor = 0xAA000000;

    // ================================================================
    //  模块17：Ping HUD
    // ================================================================
    public boolean showPingHUD = true;
    public int pingHudX = -10;
    public int pingHudY = -100;
    public int pingColor = 0xFFFFFF;
    public int pingGoodColor = 0x00FF00;
    public int pingMediumColor = 0xFFFF00;
    public int pingBadColor = 0xFF0000;
    public int goodPingThreshold = 100;
    public int mediumPingThreshold = 300;
    public boolean showPingHistory = true;
    public int pingHistorySize = 50;
    public boolean showRealPing = true;
    public boolean useTrafficBasedPing = true;
    public boolean showPingSource = false;
    public int realPingUpdateInterval = 1000;
    public boolean showNetworkDetails = false;

    // ================================================================
    //  模块18：目标血量显示 (Target HP)
    // ================================================================
    public boolean targetHPEnabled = true;
    public String targetHPStyle = "BAR_AND_TEXT";       // BAR_ONLY / TEXT_ONLY / BAR_AND_TEXT
    public float targetHPMaxRange = 32.0f;
    public boolean targetHPShowPlayers = true;
    public boolean targetHPShowMobs = true;
    public boolean targetHPShowBosses = true;
    public boolean targetHPShowSelf = false;
    public boolean targetHPShowName = true;
    public int targetHPColorR = 255;
    public int targetHPColorG = 50;
    public int targetHPColorB = 50;
    public int targetHPBackColorR = 0;
    public int targetHPBackColorG = 0;
    public int targetHPBackColorB = 0;
    public int targetHPBgAlpha = 128;
    public int targetHPTextColorR = 255;
    public int targetHPTextColorG = 255;
    public int targetHPTextColorB = 255;
    public int targetHPOffsetX = 0;
    public int targetHPOffsetY = 0;
    public boolean targetHPShowArmor = true;
    public int targetHPBarWidth = 80;
    public boolean targetHPShowLabels = true;
    public boolean targetHPShowArmorLabels = true;
    public boolean targetHPShowFace = true;
    public int targetHPFaceSize = 14;

    // ================================================================
    //  颜色工具方法
    // ================================================================
    private static int packRGB(int r, int g, int b) {
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private int loadColor(String category, String keyBase, int defaultR, int defaultG, int defaultB) {
        int r = config.getInt(keyBase + "R", category, defaultR, 0, 255, "");
        int g = config.getInt(keyBase + "G", category, defaultG, 0, 255, "");
        int b = config.getInt(keyBase + "B", category, defaultB, 0, 255, "");
        return packRGB(r, g, b);
    }

    private void saveColor(String category, String keyBase, int packed) {
        config.get(category, keyBase + "R", 255).set((packed >> 16) & 0xFF);
        config.get(category, keyBase + "G", 255).set((packed >> 8) & 0xFF);
        config.get(category, keyBase + "B", 255).set(packed & 0xFF);
    }

    // ================================================================
    //  构造器 & loadConfig
    // ================================================================
    public BetterPlayerHUDConfig(File configFile) {
        config = new Configuration(configFile);
        loadConfig();
    }

    public void loadConfig() {
        config.load();
        reloadFromConfig();
    }

    /** 仅从内存 Configuration 对象读取，不重新加载文件 */
    public void reloadFromMemory() {
        reloadFromConfig();
    }

    // ================================================================
    //  reloadFromConfig — 从 Configuration 读取全部字段
    //  注意：不再在末尾自动 config.save()，加载和存盘彻底分离
    // ================================================================
    public void reloadFromConfig() {
        final String C = Configuration.CATEGORY_CLIENT;

        // --- 清理旧 hex 颜色键（RGB 迁移用，后续可删）---
        config.getCategory(C).remove("compassColor");
        config.getCategory(C).remove("healthColorSafe");
        config.getCategory(C).remove("healthColorWarning");
        config.getCategory(C).remove("healthColorDanger");
        config.getCategory(C).remove("armorColor");
        config.getCategory(C).remove("hungerColor");
        config.getCategory(C).remove("speedColor");
        config.getCategory(C).remove("coordinatesColor");
        config.getCategory(C).remove("dimensionColor");
        config.getCategory(C).remove("gameTimeColor");
        config.getCategory(C).remove("timeColor");
        config.getCategory(C).remove("distanceColor");
        config.getCategory(C).remove("targetInfoColor");
        config.getCategory(C).remove("blockInfoColor");
        config.getCategory(C).remove("leftCPSColor");
        config.getCategory(C).remove("rightCPSColor");
        config.getCategory(C).remove("keysActiveColor");
        config.getCategory(C).remove("keysInactiveColor");
        config.getCategory(C).remove("keysTextColor");
        config.getCategory(C).remove("sprintStatusColor");
        config.getCategory(C).remove("targetHPColor");
        config.getCategory(C).remove("sneakStatusColor");
        config.getCategory(C).remove("potionBackgroundColor");
        config.getCategory(C).remove("potionTextColor");
        config.getCategory(C).remove("potionGoodEffectColor");
        config.getCategory(C).remove("potionBadEffectColor");
        config.getCategory(C).remove("potionNeutralEffectColor");
        config.getCategory(C).remove("blockOutlineColor");
        config.getCategory(C).remove("entityOutlineColorHostile");
        config.getCategory(C).remove("entityOutlineColorNeutral");
        config.getCategory(C).remove("entityOutlineColorFriendly");
        config.getCategory(C).remove("entityOutlineColor");
        config.getCategory(C).remove("itemBorderColor");
        config.getCategory(C).remove("itemNameColor");
        config.getCategory(C).remove("itemCountColor");
        config.getCategory(C).remove("itemDurabilityColor");
        config.getCategory(C).remove("itemLowDurabilityColor");
        config.getCategory(C).remove("itemEnchantmentColor");
        config.getCategory(C).remove("darkenColor");
        config.getCategory(C).remove("pingColor");
        config.getCategory(C).remove("pingGoodColor");
        config.getCategory(C).remove("pingMediumColor");
        config.getCategory(C).remove("pingBadColor");

        // --- 模块1：罗盘 HUD ---
        {
            Property p = config.get(C, "showCompassHUD", true);
            p.comment = "是否显示罗盘HUD"; showCompassHUD = p.getBoolean();

            p = config.get(C, "xPositionOffset", 0);
            p.comment = "罗盘X轴偏移量"; xPosition = p.getInt();

            p = config.get(C, "yPositionOffset", 50);
            p.comment = "罗盘Y轴偏移量"; yPosition = p.getInt();

            p = config.get(C, "compassScale", 1.0f);
            p.comment = "罗盘缩放比例"; scale = (float) p.getDouble();

            p = config.get(C, "displayStyle", "detailed");
            p.comment = "显示风格"; displayStyle = p.getString();

            p = config.get(C, "showDegreeMarks", true);
            p.comment = "是否显示刻度标记"; showDegreeMarks = p.getBoolean();

            p = config.get(C, "showCompassNeedle", true);
            p.comment = "是否显示罗盘指针"; showCompassNeedle = p.getBoolean();

            compassColor = loadColor(C, "compassColor", 255, 255, 255);

            p = config.get(C, "degreeMarkInterval", 15);
            p.comment = "刻度标记间隔"; degreeMarkInterval = p.getInt();

            p = config.get(C, "dynamicScaling", true);
            p.comment = "是否动态缩放"; dynamicScaling = p.getBoolean();

            p = config.get(C, "showHorizon", false);
            p.comment = "是否显示地平线"; showHorizon = p.getBoolean();
        }

        // --- 模块2：玩家信息 HUD ---
        {
            Property p = config.get(C, "showHealthHUD", true);
            p.comment = "是否显示血量HUD"; showHealthHUD = p.getBoolean();

            p = config.get(C, "healthHudXOffset", 10);
            p.comment = "血量HUD X坐标偏移"; healthHudX = p.getInt();

            p = config.get(C, "healthHudYOffset", -50);
            p.comment = "血量HUD Y坐标偏移"; healthHudY = p.getInt();

            healthColorSafe = loadColor(C, "healthColorSafe", 0, 255, 0);

            healthColorWarning = loadColor(C, "healthColorWarning", 255, 255, 0);

            healthColorDanger = loadColor(C, "healthColorDanger", 255, 0, 0);

            p = config.get(C, "showArmorHUD", true);
            p.comment = "是否显示护甲HUD"; showArmorHUD = p.getBoolean();

            armorColor = loadColor(C, "armorColor", 0, 255, 255);

            p = config.get(C, "showHungerHUD", true);
            p.comment = "是否显示饥饿度HUD（100为满）"; showHungerHUD = p.getBoolean();

            hungerColor = loadColor(C, "hungerColor", 255, 165, 0);

            p = config.get(C, "showPlayerHead", true);
            p.comment = "是否显示玩家头像"; showPlayerHead = p.getBoolean();

            p = config.get(C, "headSize", 16);
            p.comment = "头像大小"; headSize = p.getInt();

            p = config.get(C, "headTextSpacing", 2);
            p.comment = "头像与文本间距"; headTextSpacing = p.getInt();
        }

        // --- 模块3：速度 HUD ---
        {
            Property p = config.get(C, "showSpeedHUD", true);
            p.comment = "是否显示速度HUD"; showSpeedHUD = p.getBoolean();

            p = config.get(C, "speedHudXOffset", -10);
            p.comment = "速度HUD X坐标偏移"; speedHudX = p.getInt();

            p = config.get(C, "speedHudYOffset", -60);
            p.comment = "速度HUD Y坐标偏移"; speedHudY = p.getInt();

            speedColor = loadColor(C, "speedColor", 0, 255, 0);

            p = config.get(C, "speedUnit", "m/s");
            p.comment = "速度单位"; speedUnit = p.getString();

            p = config.get(C, "speedPrecision", 2);
            p.comment = "速度小数位数"; speedPrecision = p.getInt();

            p = config.get(C, "showVerticalSpeed", true);
            p.comment = "是否显示垂直速度"; showVerticalSpeed = p.getBoolean();

            p = config.get(C, "showExactAngle", true);
            p.comment = "是否显示精确角度"; showExactAngle = p.getBoolean();
        }

        // --- 模块4：坐标 HUD ---
        {
            Property p = config.get(C, "showCoordinatesHUD", true);
            p.comment = "是否显示坐标HUD"; showCoordinatesHUD = p.getBoolean();

            p = config.get(C, "coordinatesHudXOffset", 10);
            p.comment = "坐标HUD X坐标偏移"; coordinatesHudX = p.getInt();

            p = config.get(C, "coordinatesHudYOffset", 50);
            p.comment = "坐标HUD Y坐标偏移"; coordinatesHudY = p.getInt();

            coordinatesColor = loadColor(C, "coordinatesColor", 255, 255, 255);

            p = config.get(C, "coordinatesPrecision", 1);
            p.comment = "坐标小数位数"; coordinatesPrecision = p.getInt();

            p = config.get(C, "showDimension", true);
            p.comment = "是否显示维度"; showDimension = p.getBoolean();

            dimensionColor = loadColor(C, "dimensionColor", 170, 170, 170);

            p = config.get(C, "showFacing", true);
            p.comment = "是否显示朝向"; showFacing = p.getBoolean();
        }

        // --- 模块5：游戏时间 HUD ---
        {
            Property p = config.get(C, "showGameTimeHUD", true);
            p.comment = "是否显示游戏时间HUD"; showGameTimeHUD = p.getBoolean();

            p = config.get(C, "gameTimeHudXOffset", -10);
            p.comment = "游戏时间HUD X坐标偏移"; gameTimeHudX = p.getInt();

            p = config.get(C, "gameTimeHudYOffset", -80);
            p.comment = "游戏时间HUD Y坐标偏移"; gameTimeHudY = p.getInt();

            gameTimeColor = loadColor(C, "gameTimeColor", 255, 255, 170);

            p = config.get(C, "gameTime24Hour", true);
            p.comment = "是否使用24小时制"; gameTime24Hour = p.getBoolean();

            p = config.get(C, "showGameDay", true);
            p.comment = "是否显示游戏天数"; showGameDay = p.getBoolean();
        }

        // --- 模块6：现实时间 HUD ---
        {
            Property p = config.get(C, "showRealTimeHUD", true);
            p.comment = "是否显示现实时间HUD"; showRealTimeHUD = p.getBoolean();

            p = config.get(C, "timeHudXOffset", -10);
            p.comment = "现实时间HUD X坐标偏移"; timeHudX = p.getInt();

            p = config.get(C, "timeHudYOffset", -30);
            p.comment = "现实时间HUD Y坐标偏移"; timeHudY = p.getInt();

            timeColor = loadColor(C, "timeColor", 255, 255, 255);

            p = config.get(C, "timeFormat", "yyyy/M/d\nHH:mm:ss");
            p.comment = "时间格式"; timeFormat = p.getString();
        }

        // --- 模块7：距离 HUD ---
        {
            Property p = config.get(C, "showDistanceHUD", true);
            p.comment = "是否显示距离HUD"; showDistanceHUD = p.getBoolean();

            p = config.get(C, "distanceHudXOffset", 490);
            p.comment = "距离HUD X坐标偏移"; distanceHudX = p.getInt();

            p = config.get(C, "distanceHudYOffset", 280);
            p.comment = "距离HUD Y坐标偏移"; distanceHudY = p.getInt();

            distanceColor = loadColor(C, "distanceColor", 85, 255, 255);

            p = config.get(C, "distancePrecision", 1);
            p.comment = "距离小数位数"; distancePrecision = p.getInt();

            p = config.get(C, "showTargetInfo", true);
            p.comment = "是否显示目标信息"; showTargetInfo = p.getBoolean();

            targetInfoColor = loadColor(C, "targetInfoColor", 170, 170, 170);

            p = config.get(C, "showBlockCoordinates", true);
            p.comment = "是否显示方块坐标"; showBlockCoordinates = p.getBoolean();

            p = config.get(C, "showBlockHardness", true);
            p.comment = "是否显示方块硬度"; showBlockHardness = p.getBoolean();

            p = config.get(C, "showRequiredTool", true);
            p.comment = "是否显示所需工具"; showRequiredTool = p.getBoolean();

            blockInfoColor = loadColor(C, "blockInfoColor", 170, 170, 170);
        }

        // --- 模块8：CPS HUD ---
        {
            Property p = config.get(C, "showCPSHUD", true);
            p.comment = "是否显示CPS HUD"; showCPSHUD = p.getBoolean();

            p = config.get(C, "cpsHudXOffset", 10);
            p.comment = "CPS HUD X坐标偏移"; cpsHudX = p.getInt();

            p = config.get(C, "cpsHudYOffset", 260);
            p.comment = "CPS HUD Y坐标偏移"; cpsHudY = p.getInt();

            leftCPSColor = loadColor(C, "leftCPSColor", 0, 170, 255);

            rightCPSColor = loadColor(C, "rightCPSColor", 255, 170, 0);

            p = config.get(C, "cpsUpdateInterval", 100);
            p.comment = "CPS更新间隔(ms)"; cpsUpdateInterval = p.getInt();
        }

        // --- 模块9：按键显示 ---
        {
            Property p = config.get(C, "showKeysDisplay", true);
            p.comment = "是否显示按键"; showKeysDisplay = p.getBoolean();

            p = config.get(C, "keysDisplayX", 10);
            p.comment = "按键显示X坐标"; keysDisplayX = p.getInt();

            p = config.get(C, "keysDisplayY", 150);
            p.comment = "按键显示Y坐标"; keysDisplayY = p.getInt();

            p = config.get(C, "keysSize", 24);
            p.comment = "按键大小"; keysSize = p.getInt();

            p = config.get(C, "keysSpacing", 4);
            p.comment = "按键间距"; keysSpacing = p.getInt();

            keysActiveColor = loadColor(C, "keysActiveColor", 255, 255, 255);

            keysInactiveColor = loadColor(C, "keysInactiveColor", 128, 255, 255);

            keysTextColor = loadColor(C, "keysTextColor", 255, 255, 255);

            p = config.get(C, "keysScale", 1.0f);
            p.comment = "按键整体缩放"; keysScale = (float) p.getDouble();

            p = config.get(C, "showKeysBackground", false);
            p.comment = "是否显示按键背景"; showKeysBackground = p.getBoolean();

            p = config.get(C, "keysOpacity", 1.0);
            p.comment = "按键显示透明度 (0.0~1.0)"; keysOpacity = (float) p.getDouble();
        }

        // --- 模块10：强制疾跑 ---
        {
            Property p = config.get(C, "enableForceSprint", true);
            p.comment = "是否启用强制疾跑"; enableForceSprint = p.getBoolean();

            p = config.get(C, "forceSprintEnabled", false);
            p.comment = "强制疾跑默认开启"; forceSprintEnabled = p.getBoolean();

            p = config.get(C, "forceSprintKey", 23);
            p.comment = "强制疾跑快捷键"; forceSprintKey = p.getInt();

            p = config.get(C, "showSprintStatus", true);
            p.comment = "是否显示疾跑状态"; showSprintStatus = p.getBoolean();

            p = config.get(C, "sprintStatusX", 100);
            p.comment = "疾跑状态X坐标"; sprintStatusX = p.getInt();

            p = config.get(C, "sprintStatusY", 100);
            p.comment = "疾跑状态Y坐标"; sprintStatusY = p.getInt();

            sprintStatusColor = loadColor(C, "sprintStatusColor", 0, 255, 0);

            p = config.get(C, "sprintStatusScale", 1.0f);
            p.comment = "疾跑状态缩放"; sprintStatusScale = (float) p.getDouble();

            p = config.get(C, "showDebugInfo", false);
            p.comment = "是否显示调试信息"; showDebugInfo = p.getBoolean();
        }

        // --- 模块11：强制潜行 ---
        {
            Property p = config.get(C, "enableForceSneak", true);
            p.comment = "是否启用强制潜行"; enableForceSneak = p.getBoolean();

            p = config.get(C, "forceSneakEnabled", false);
            p.comment = "强制潜行默认开启"; forceSneakEnabled = p.getBoolean();

            p = config.get(C, "forceSneakKey", Keyboard.KEY_N);
            p.comment = "强制潜行快捷键"; forceSneakKey = p.getInt();

            p = config.get(C, "showSneakStatus", false);
            p.comment = "是否显示潜行状态"; showSneakStatus = p.getBoolean();

            p = config.get(C, "sneakStatusX", 10);
            p.comment = "潜行状态X坐标"; sneakStatusX = p.getInt();

            p = config.get(C, "sneakStatusY", 30);
            p.comment = "潜行状态Y坐标"; sneakStatusY = p.getInt();

            sneakStatusColor = loadColor(C, "sneakStatusColor", 255, 255, 0);

            p = config.get(C, "sneakPlaySound", true);
            p.comment = "切换时播放音效"; sneakPlaySound = p.getBoolean();
        }

        // --- 模块12：药水状态 HUD ---
        {
            Property p = config.get(C, "showPotionHUD", true);
            p.comment = "是否显示药水状态HUD"; showPotionHUD = p.getBoolean();

            p = config.get(C, "potionHudX", -120);
            p.comment = "药水HUD X坐标"; potionHudX = p.getInt();

            p = config.get(C, "potionHudY", 30);
            p.comment = "药水HUD Y坐标"; potionHudY = p.getInt();

            potionBackgroundColor = loadColor(C, "potionBackgroundColor", 0, 0, 0);

            potionTextColor = loadColor(C, "potionTextColor", 255, 255, 255);

            potionGoodEffectColor = loadColor(C, "potionGoodEffectColor", 0, 255, 0);

            potionBadEffectColor = loadColor(C, "potionBadEffectColor", 255, 0, 0);

            potionNeutralEffectColor = loadColor(C, "potionNeutralEffectColor", 255, 255, 0);

            p = config.get(C, "potionScale", 1.0f);
            p.comment = "药水HUD缩放"; potionScale = (float) p.getDouble();

            p = config.get(C, "showPotionIcons", true);
            p.comment = "是否显示药水图标"; showPotionIcons = p.getBoolean();

            p = config.get(C, "showPotionNames", true);
            p.comment = "是否显示药水名称"; showPotionNames = p.getBoolean();

            p = config.get(C, "showPotionDurations", true);
            p.comment = "是否显示药水持续时间"; showPotionDurations = p.getBoolean();

            p = config.get(C, "showPotionAmplifier", true);
            p.comment = "是否显示药水等级"; showPotionAmplifier = p.getBoolean();

            p = config.get(C, "showPotionBackground", true);
            p.comment = "是否显示药水背景"; showPotionBackground = p.getBoolean();

            p = config.get(C, "potionSpacing", 2);
            p.comment = "药水条目间距"; potionSpacing = p.getInt();

            p = config.get(C, "potionTextOffset", 12);
            p.comment = "药水文字偏移"; potionTextOffset = p.getInt();

            // 配置文件键名 "potionTimeFormat"，Java字段名 potionTimeFormatString
            p = config.get(C, "potionTimeFormat", "mm:ss");
            p.comment = "药水时间格式"; potionTimeFormatString = p.getString();

            p = config.get(C, "showOnlyActivePotions", true);
            p.comment = "仅显示活跃药水"; showOnlyActivePotions = p.getBoolean();

            p = config.get(C, "maxPotionDisplay", 10);
            p.comment = "最大药水显示数"; maxPotionDisplay = p.getInt();

            p = config.get(C, "sortByDuration", true);
            p.comment = "按持续时间排序"; sortByDuration = p.getBoolean();

            p = config.get(C, "showInfiniteAsIcon", true);
            p.comment = "永久效果显示图标"; showInfiniteAsIcon = p.getBoolean();
        }

        // --- 模块13：方块描边 ---
        {
            Property p = config.get(C, "enableBlockHighlight", true);
            p.comment = "是否启用方块描边"; enableBlockHighlight = p.getBoolean();

            blockOutlineColor = loadColor(C, "blockOutlineColor", 255, 255, 0);

            p = config.get(C, "blockOutlineWidth", 3.0f);
            p.comment = "方块描边宽度"; blockOutlineWidth = (float) p.getDouble();

            p = config.get(C, "drawVisibleFacesOnlyBlocks", true);
            p.comment = "方块描边仅绘制可见面"; drawVisibleFacesOnlyBlocks = p.getBoolean();
        }

        // --- 模块14：实体碰撞箱高亮 ---
        {
            Property p = config.get(C, "enableEntityHighlight", true);
            p.comment = "是否启用实体高亮"; enableEntityHighlight = p.getBoolean();

            entityOutlineColorHostile = loadColor(C, "entityOutlineColorHostile", 255, 0, 0);

            entityOutlineColorNeutral = loadColor(C, "entityOutlineColorNeutral", 255, 255, 0);

            entityOutlineColorFriendly = loadColor(C, "entityOutlineColorFriendly", 0, 255, 0);

            p = config.get(C, "entityOutlineWidth", 2.0f);
            p.comment = "实体描边宽度"; entityOutlineWidth = (float) p.getDouble();

            p = config.get(C, "drawVisibleFacesOnlyEntities", false);
            p.comment = "实体描边仅绘制可见面"; drawVisibleFacesOnlyEntities = p.getBoolean();
        }

        // --- 模块14b：RGB 动态流光 & 隐身隐藏 ---
        {
            Property p = config.get(C, "enableRGBMode", true);
            p.comment = "启用 RGB 动态流光（覆盖静态颜色）"; enableRGBMode = p.getBoolean();

            p = config.get(C, "rgbStepMs", 400);
            p.comment = "步进间隔(ms,0=平滑)"; rgbStepMs = p.getInt();

            p = config.get(C, "rgbSpeed", 80);
            p.comment = "RGB 色相循环速度（毫秒/周期，越大越慢，默认500）"; rgbSpeed = p.getInt();

            p = config.get(C, "rgbApplyBlockOutline", true);
            p.comment = "RGB 应用于方块描边"; rgbApplyBlockOutline = p.getBoolean();

            p = config.get(C, "rgbApplyEntityHitbox", true);
            p.comment = "RGB 应用于实体碰撞箱"; rgbApplyEntityHitbox = p.getBoolean();

            p = config.get(C, "hideHitboxForInvisible", false);
            p.comment = "隐身生物/玩家不显示碰撞箱（默认关，可开）"; hideHitboxForInvisible = p.getBoolean();
        }

        // --- 模块14c：快捷键（描边/碰撞箱/RGB）---
        {
            Property p = config.get(C, "keyBindToggleBlockOutline", 0);
            p.comment = "方块描边开关快捷键（按键码，0=未绑定）"; keyBindToggleBlockOutline = p.getInt();

            p = config.get(C, "keyBindToggleEntityHitbox", 0);
            p.comment = "实体碰撞箱开关快捷键"; keyBindToggleEntityHitbox = p.getInt();

            p = config.get(C, "keyBindToggleRGB", 0);
            p.comment = "RGB流光开关快捷键"; keyBindToggleRGB = p.getInt();
        }

        // --- 模块15：物品信息 HUD ---
        {
            Property p = config.get(C, "enableItemInfoHUD", true);
            p.comment = "是否启用物品信息HUD"; enableItemInfoHUD = p.getBoolean();

            p = config.get(C, "itemHudX", 5);
            p.comment = "物品信息HUD X坐标"; itemHudX = p.getInt();

            p = config.get(C, "itemHudY", 330);
            p.comment = "物品信息HUD Y坐标"; itemHudY = p.getInt();

            p = config.get(C, "itemHudWidth", 200);
            p.comment = "物品信息HUD宽度"; itemHudWidth = p.getInt();

            p = config.get(C, "itemHudHeight", 120);
            p.comment = "物品信息HUD高度"; itemHudHeight = p.getInt();

            p = config.get(C, "itemShowBackground", false);
            p.comment = "是否显示背景"; itemShowBackground = p.getBoolean();

            p = config.get(C, "itemBackgroundOpacity", 0.3f);
            p.comment = "背景透明度"; itemBackgroundOpacity = (float) p.getDouble();

            p = config.get(C, "itemShowBorder", true);
            p.comment = "是否显示边框"; itemShowBorder = p.getBoolean();

            itemBorderColor = loadColor(C, "itemBorderColor", 85, 85, 85);

            p = config.get(C, "showMainHandCountSeparate", true);
            p.comment = "主手物品数量单独显示"; showMainHandCountSeparate = p.getBoolean();

            p = config.get(C, "mainHandCountX", 460);
            p.comment = "主手数量X坐标"; mainHandCountX = p.getInt();

            p = config.get(C, "mainHandCountY", 470);
            p.comment = "主手数量Y坐标"; mainHandCountY = p.getInt();

            p = config.get(C, "showMainHandItem", true);
            p.comment = "是否显示主手物品"; showMainHandItem = p.getBoolean();

            p = config.get(C, "showItemIcon", true);
            p.comment = "是否显示物品图标"; showItemIcon = p.getBoolean();

            p = config.get(C, "showItemName", false);
            p.comment = "是否显示物品名称"; showItemName = p.getBoolean();

            p = config.get(C, "showItemType", false);
            p.comment = "是否显示物品类型"; showItemType = p.getBoolean();

            p = config.get(C, "showItemCount", true);
            p.comment = "是否显示物品数量"; showItemCount = p.getBoolean();

            p = config.get(C, "showDurability", true);
            p.comment = "是否显示耐久度"; showDurability = p.getBoolean();

            p = config.get(C, "showDurabilityBar", false);
            p.comment = "是否显示耐久度进度条"; showDurabilityBar = p.getBoolean();

            p = config.get(C, "showEnchantments", true);
            p.comment = "是否显示附魔"; showEnchantments = p.getBoolean();

            p = config.get(C, "showArrowCount", true);
            p.comment = "手持弓时显示箭的数量"; showArrowCount = p.getBoolean();

            p = config.get(C, "showArmorItems", true);
            p.comment = "是否显示装备栏物品"; showArmorItems = p.getBoolean();

            p = config.get(C, "showArmorIcon", true);
            p.comment = "是否显示装备图标"; showArmorIcon = p.getBoolean();

            p = config.get(C, "showArmorName", false);
            p.comment = "是否显示装备名称"; showArmorName = p.getBoolean();

            p = config.get(C, "showArmorDurability", true);
            p.comment = "是否显示装备耐久度"; showArmorDurability = p.getBoolean();

            p = config.get(C, "showArmorEnchantments", true);
            p.comment = "是否显示装备附魔"; showArmorEnchantments = p.getBoolean();

            p = config.get(C, "itemUseRarityColors", true);
            p.comment = "是否使用稀有度颜色"; itemUseRarityColors = p.getBoolean();

            p = config.get(C, "itemLowDurabilityThreshold", 20);
            p.comment = "低耐久度警告阈值(%)"; itemLowDurabilityThreshold = p.getInt();

            itemNameColor = loadColor(C, "itemNameColor", 255, 255, 255);

            itemCountColor = loadColor(C, "itemCountColor", 255, 255, 85);

            itemDurabilityColor = loadColor(C, "itemDurabilityColor", 85, 255, 85);

            itemLowDurabilityColor = loadColor(C, "itemLowDurabilityColor", 255, 85, 85);

            itemEnchantmentColor = loadColor(C, "itemEnchantmentColor", 170, 170, 255);

            p = config.get(C, "showItemDamage", true);
            p.comment = "是否显示物品伤害"; showItemDamage = p.getBoolean();

            p = config.get(C, "showDamageBreakdown", false);
            p.comment = "是否显示伤害分解"; showDamageBreakdown = p.getBoolean();
        }

        // --- 模块16：低血量警告 ---
        {
            Property p = config.get(C, "enableLowHealthWarning", true);
            p.comment = "是否启用低血量警告"; enableLowHealthWarning = p.getBoolean();

            p = config.get(C, "lowHealthThreshold", 30);
            p.comment = "触发警告的生命值阈值"; lowHealthThreshold = p.getInt();

            p = config.get(C, "darkenIntensity", 0.5f);
            p.comment = "屏幕边缘变暗强度"; darkenIntensity = (float) p.getDouble();

            p = config.get(C, "vignetteSize", 0.7f);
            p.comment = "中心椭圆区域大小"; vignetteSize = (float) p.getDouble();

            darkenColor = loadColor(C, "darkenColor", 0, 0, 0);
        }

        // --- 模块17：Ping HUD ---
        {
            Property p = config.get(C, "showPingHUD", true);
            p.comment = "是否显示Ping HUD"; showPingHUD = p.getBoolean();

            p = config.get(C, "pingHudXOffset", -10);
            p.comment = "Ping HUD X坐标偏移"; pingHudX = p.getInt();

            p = config.get(C, "pingHudYOffset", -100);
            p.comment = "Ping HUD Y坐标偏移"; pingHudY = p.getInt();

            pingColor = loadColor(C, "pingColor", 255, 255, 255);

            pingGoodColor = loadColor(C, "pingGoodColor", 0, 255, 0);

            pingMediumColor = loadColor(C, "pingMediumColor", 255, 255, 0);

            pingBadColor = loadColor(C, "pingBadColor", 255, 0, 0);

            p = config.get(C, "goodPingThreshold", 100);
            p.comment = "良好延迟阈值(ms)"; goodPingThreshold = p.getInt();

            p = config.get(C, "mediumPingThreshold", 300);
            p.comment = "中等延迟阈值(ms)"; mediumPingThreshold = p.getInt();

            p = config.get(C, "showPingHistory", true);
            p.comment = "是否显示延迟历史"; showPingHistory = p.getBoolean();

            p = config.get(C, "pingHistorySize", 50);
            p.comment = "延迟历史记录数"; pingHistorySize = p.getInt();

            p = config.get(C, "showRealPing", true);
            p.comment = "是否测量真实延迟"; showRealPing = p.getBoolean();

            p = config.get(C, "useTrafficBasedPing", true);
            p.comment = "是否使用流量分析"; useTrafficBasedPing = p.getBoolean();

            p = config.get(C, "showPingSource", false);
            p.comment = "是否显示Ping来源(调试)"; showPingSource = p.getBoolean();

            p = config.get(C, "realPingUpdateInterval", 1000);
            p.comment = "真实Ping更新间隔(ms)"; realPingUpdateInterval = p.getInt();

            p = config.get(C, "showNetworkDetails", false);
            p.comment = "是否显示网络详情"; showNetworkDetails = p.getBoolean();
        }

        // --- 模块18：目标血量显示 ---
        {
            Property p = config.get(C, "targetHPEnabled", true);
            p.comment = "是否启用目标血量显示"; targetHPEnabled = p.getBoolean();

            p = config.get(C, "targetHPStyle", "BAR_AND_TEXT");
            p.comment = "显示样式: BAR_ONLY / TEXT_ONLY / BAR_AND_TEXT";
            targetHPStyle = p.getString();

            p = config.get(C, "targetHPMaxRange", 32.0f);
            p.comment = "最大检测距离(格)"; targetHPMaxRange = (float)p.getDouble();

            p = config.get(C, "targetHPShowPlayers", true);
            p.comment = "显示玩家"; targetHPShowPlayers = p.getBoolean();

            p = config.get(C, "targetHPShowMobs", true);
            p.comment = "显示怪物"; targetHPShowMobs = p.getBoolean();

            p = config.get(C, "targetHPShowBosses", true);
            p.comment = "显示Boss"; targetHPShowBosses = p.getBoolean();

            p = config.get(C, "targetHPShowSelf", false);
            p.comment = "显示自己"; targetHPShowSelf = p.getBoolean();

            p = config.get(C, "targetHPShowName", true);
            p.comment = "显示实体名称"; targetHPShowName = p.getBoolean();

            p = config.get(C, "targetHPColorR", 255);
            p.comment = "血条颜色 R(0-255)"; targetHPColorR = p.getInt();

            p = config.get(C, "targetHPColorG", 50);
            p.comment = "血条颜色 G(0-255)"; targetHPColorG = p.getInt();

            p = config.get(C, "targetHPColorB", 50);
            p.comment = "血条颜色 B(0-255)"; targetHPColorB = p.getInt();

            p = config.get(C, "targetHPBackColorR", 0);
            p.comment = "背景色 R(0-255)"; targetHPBackColorR = p.getInt();

            p = config.get(C, "targetHPBackColorG", 0);
            p.comment = "背景色 G(0-255)"; targetHPBackColorG = p.getInt();

            p = config.get(C, "targetHPBackColorB", 0);
            p.comment = "背景色 B(0-255)"; targetHPBackColorB = p.getInt();

            p = config.get(C, "targetHPBgAlpha", 128);
            p.comment = "背景透明度(0-255)"; targetHPBgAlpha = p.getInt();

            p = config.get(C, "targetHPTextColorR", 255);
            p.comment = "文本颜色 R(0-255)"; targetHPTextColorR = p.getInt();

            p = config.get(C, "targetHPTextColorG", 255);
            p.comment = "文本颜色 G(0-255)"; targetHPTextColorG = p.getInt();

            p = config.get(C, "targetHPTextColorB", 255);
            p.comment = "文本颜色 B(0-255)"; targetHPTextColorB = p.getInt();

            p = config.get(C, "targetHPOffsetX", 0);
            p.comment = "水平偏移(像素, 正=右)"; targetHPOffsetX = p.getInt();

            p = config.get(C, "targetHPOffsetY", 0);
            p.comment = "垂直偏移(像素, 正=下)"; targetHPOffsetY = p.getInt();

            p = config.get(C, "targetHPShowArmor", true);
            p.comment = "显示护甲值"; targetHPShowArmor = p.getBoolean();

            p = config.get(C, "targetHPBarWidth", 80);
            p.comment = "血条宽度(像素)"; targetHPBarWidth = p.getInt();

            p = config.get(C, "targetHPShowLabels", true);
            p.comment = "显示右侧数值"; targetHPShowLabels = p.getBoolean();

            p = config.get(C, "targetHPShowArmorLabels", true);
            p.comment = "显示护甲数值"; targetHPShowArmorLabels = p.getBoolean();

            p = config.get(C, "targetHPShowFace", true);
            p.comment = "对玩家显示面部头像"; targetHPShowFace = p.getBoolean();

            p = config.get(C, "targetHPFaceSize", 14);
            p.comment = "面部头像尺寸(像素)"; targetHPFaceSize = p.getInt();
        }
    }

    // ================================================================
    //  saveConfig — 将 Java 字段值写回 Configuration 并保存到磁盘
    //  主动写入每一个字段，不再依赖 Forge 的 hasChanged() 标志
    // ================================================================
    public void saveConfig() {
        final String C = Configuration.CATEGORY_CLIENT;

        // --- 模块1 ---
        config.get(C, "showCompassHUD", true).set(showCompassHUD);
        config.get(C, "xPositionOffset", 0).set(xPosition);
        config.get(C, "yPositionOffset", 50).set(yPosition);
        config.get(C, "compassScale", 1.0f).set(scale);
        config.get(C, "displayStyle", "detailed").set(displayStyle);
        config.get(C, "showDegreeMarks", true).set(showDegreeMarks);
        config.get(C, "showCompassNeedle", true).set(showCompassNeedle);
        saveColor(C, "compassColor", compassColor);
        config.get(C, "degreeMarkInterval", 15).set(degreeMarkInterval);
        config.get(C, "dynamicScaling", true).set(dynamicScaling);
        config.get(C, "showHorizon", false).set(showHorizon);

        // --- 模块2 ---
        config.get(C, "showHealthHUD", true).set(showHealthHUD);
        config.get(C, "healthHudXOffset", 10).set(healthHudX);
        config.get(C, "healthHudYOffset", -50).set(healthHudY);
        saveColor(C, "healthColorSafe", healthColorSafe);
        saveColor(C, "healthColorWarning", healthColorWarning);
        saveColor(C, "healthColorDanger", healthColorDanger);
        config.get(C, "showArmorHUD", true).set(showArmorHUD);
        saveColor(C, "armorColor", armorColor);
        config.get(C, "showHungerHUD", true).set(showHungerHUD);
        saveColor(C, "hungerColor", hungerColor);
        config.get(C, "showPlayerHead", true).set(showPlayerHead);
        config.get(C, "headSize", 16).set(headSize);
        config.get(C, "headTextSpacing", 2).set(headTextSpacing);

        // --- 模块3 ---
        config.get(C, "showSpeedHUD", true).set(showSpeedHUD);
        config.get(C, "speedHudXOffset", -10).set(speedHudX);
        config.get(C, "speedHudYOffset", -60).set(speedHudY);
        saveColor(C, "speedColor", speedColor);
        config.get(C, "speedUnit", "m/s").set(speedUnit);
        config.get(C, "speedPrecision", 2).set(speedPrecision);
        config.get(C, "showVerticalSpeed", true).set(showVerticalSpeed);
        config.get(C, "showExactAngle", true).set(showExactAngle);

        // --- 模块4 ---
        config.get(C, "showCoordinatesHUD", true).set(showCoordinatesHUD);
        config.get(C, "coordinatesHudXOffset", 10).set(coordinatesHudX);
        config.get(C, "coordinatesHudYOffset", 50).set(coordinatesHudY);
        saveColor(C, "coordinatesColor", coordinatesColor);
        config.get(C, "coordinatesPrecision", 1).set(coordinatesPrecision);
        config.get(C, "showDimension", true).set(showDimension);
        saveColor(C, "dimensionColor", dimensionColor);
        config.get(C, "showFacing", true).set(showFacing);

        // --- 模块5 ---
        config.get(C, "showGameTimeHUD", true).set(showGameTimeHUD);
        config.get(C, "gameTimeHudXOffset", -10).set(gameTimeHudX);
        config.get(C, "gameTimeHudYOffset", -80).set(gameTimeHudY);
        saveColor(C, "gameTimeColor", gameTimeColor);
        config.get(C, "gameTime24Hour", true).set(gameTime24Hour);
        config.get(C, "showGameDay", true).set(showGameDay);

        // --- 模块6 ---
        config.get(C, "showRealTimeHUD", true).set(showRealTimeHUD);
        config.get(C, "timeHudXOffset", -10).set(timeHudX);
        config.get(C, "timeHudYOffset", -30).set(timeHudY);
        saveColor(C, "timeColor", timeColor);
        config.get(C, "timeFormat", "yyyy/M/d\nHH:mm:ss").set(timeFormat);

        // --- 模块7 ---
        config.get(C, "showDistanceHUD", true).set(showDistanceHUD);
        config.get(C, "distanceHudXOffset", 490).set(distanceHudX);
        config.get(C, "distanceHudYOffset", 280).set(distanceHudY);
        saveColor(C, "distanceColor", distanceColor);
        config.get(C, "distancePrecision", 1).set(distancePrecision);
        config.get(C, "showTargetInfo", true).set(showTargetInfo);
        saveColor(C, "targetInfoColor", targetInfoColor);
        config.get(C, "showBlockCoordinates", true).set(showBlockCoordinates);
        config.get(C, "showBlockHardness", true).set(showBlockHardness);
        config.get(C, "showRequiredTool", true).set(showRequiredTool);
        saveColor(C, "blockInfoColor", blockInfoColor);

        // --- 模块8 ---
        config.get(C, "showCPSHUD", true).set(showCPSHUD);
        config.get(C, "cpsHudXOffset", 10).set(cpsHudX);
        config.get(C, "cpsHudYOffset", 260).set(cpsHudY);
        saveColor(C, "leftCPSColor", leftCPSColor);
        saveColor(C, "rightCPSColor", rightCPSColor);
        config.get(C, "cpsUpdateInterval", 100).set(cpsUpdateInterval);

        // --- 模块9 ---
        config.get(C, "showKeysDisplay", true).set(showKeysDisplay);
        config.get(C, "keysDisplayX", 10).set(keysDisplayX);
        config.get(C, "keysDisplayY", 150).set(keysDisplayY);
        config.get(C, "keysSize", 24).set(keysSize);
        config.get(C, "keysSpacing", 4).set(keysSpacing);
        saveColor(C, "keysActiveColor", keysActiveColor);
        saveColor(C, "keysInactiveColor", keysInactiveColor);
        saveColor(C, "keysTextColor", keysTextColor);
        config.get(C, "keysScale", 1.0f).set(keysScale);
        config.get(C, "showKeysBackground", false).set(showKeysBackground);
        config.get(C, "keysOpacity", 1.0).set(keysOpacity);

        // --- 模块10 ---
        config.get(C, "enableForceSprint", true).set(enableForceSprint);
        config.get(C, "forceSprintEnabled", false).set(forceSprintEnabled);
        config.get(C, "forceSprintKey", 23).set(forceSprintKey);
        config.get(C, "showSprintStatus", true).set(showSprintStatus);
        config.get(C, "sprintStatusX", 100).set(sprintStatusX);
        config.get(C, "sprintStatusY", 100).set(sprintStatusY);
        saveColor(C, "sprintStatusColor", sprintStatusColor);
        config.get(C, "sprintStatusScale", 1.0f).set(sprintStatusScale);
        config.get(C, "showDebugInfo", false).set(showDebugInfo);

        // --- 模块11 ---
        config.get(C, "enableForceSneak", true).set(enableForceSneak);
        config.get(C, "forceSneakEnabled", false).set(forceSneakEnabled);
        config.get(C, "forceSneakKey", Keyboard.KEY_N).set(forceSneakKey);
        config.get(C, "showSneakStatus", false).set(showSneakStatus);
        config.get(C, "sneakStatusX", 10).set(sneakStatusX);
        config.get(C, "sneakStatusY", 30).set(sneakStatusY);
        saveColor(C, "sneakStatusColor", sneakStatusColor);
        config.get(C, "sneakPlaySound", true).set(sneakPlaySound);

        // --- 模块12 ---
        config.get(C, "showPotionHUD", true).set(showPotionHUD);
        config.get(C, "potionHudX", -120).set(potionHudX);
        config.get(C, "potionHudY", 30).set(potionHudY);
        saveColor(C, "potionBackgroundColor", potionBackgroundColor);
        saveColor(C, "potionTextColor", potionTextColor);
        saveColor(C, "potionGoodEffectColor", potionGoodEffectColor);
        saveColor(C, "potionBadEffectColor", potionBadEffectColor);
        saveColor(C, "potionNeutralEffectColor", potionNeutralEffectColor);
        config.get(C, "potionScale", 1.0f).set(potionScale);
        config.get(C, "showPotionIcons", true).set(showPotionIcons);
        config.get(C, "showPotionNames", true).set(showPotionNames);
        config.get(C, "showPotionDurations", true).set(showPotionDurations);
        config.get(C, "showPotionAmplifier", true).set(showPotionAmplifier);
        config.get(C, "showPotionBackground", true).set(showPotionBackground);
        config.get(C, "potionSpacing", 2).set(potionSpacing);
        config.get(C, "potionTextOffset", 12).set(potionTextOffset);
        config.get(C, "potionTimeFormat", "mm:ss").set(potionTimeFormatString);
        config.get(C, "showOnlyActivePotions", true).set(showOnlyActivePotions);
        config.get(C, "maxPotionDisplay", 10).set(maxPotionDisplay);
        config.get(C, "sortByDuration", true).set(sortByDuration);
        config.get(C, "showInfiniteAsIcon", true).set(showInfiniteAsIcon);

        // --- 模块13 ---
        config.get(C, "enableBlockHighlight", true).set(enableBlockHighlight);
        saveColor(C, "blockOutlineColor", blockOutlineColor);
        config.get(C, "blockOutlineWidth", 3.0f).set(blockOutlineWidth);
        config.get(C, "drawVisibleFacesOnlyBlocks", true).set(drawVisibleFacesOnlyBlocks);

        // --- 模块14 ---
        config.get(C, "enableEntityHighlight", true).set(enableEntityHighlight);
        saveColor(C, "entityOutlineColorHostile", entityOutlineColorHostile);
        saveColor(C, "entityOutlineColorNeutral", entityOutlineColorNeutral);
        saveColor(C, "entityOutlineColorFriendly", entityOutlineColorFriendly);
        config.get(C, "entityOutlineWidth", 2.0f).set(entityOutlineWidth);
        config.get(C, "drawVisibleFacesOnlyEntities", false).set(drawVisibleFacesOnlyEntities);

        // --- 模块14b ---
        config.get(C, "enableRGBMode", true).set(enableRGBMode);
        config.get(C, "rgbSpeed", 80).set(rgbSpeed);
        config.get(C, "rgbApplyBlockOutline", true).set(rgbApplyBlockOutline);
        config.get(C, "rgbApplyEntityHitbox", true).set(rgbApplyEntityHitbox);
        config.get(C, "hideHitboxForInvisible", false).set(hideHitboxForInvisible);

        // --- 模块14c ---
        config.get(C, "keyBindToggleBlockOutline", 0).set(keyBindToggleBlockOutline);
        config.get(C, "keyBindToggleEntityHitbox", 0).set(keyBindToggleEntityHitbox);
        config.get(C, "keyBindToggleRGB", 0).set(keyBindToggleRGB);

        // --- 模块15 ---
        config.get(C, "enableItemInfoHUD", true).set(enableItemInfoHUD);
        config.get(C, "itemHudX", 5).set(itemHudX);
        config.get(C, "itemHudY", 330).set(itemHudY);
        config.get(C, "itemHudWidth", 200).set(itemHudWidth);
        config.get(C, "itemHudHeight", 120).set(itemHudHeight);
        config.get(C, "itemShowBackground", false).set(itemShowBackground);
        config.get(C, "itemBackgroundOpacity", 0.3f).set(itemBackgroundOpacity);
        config.get(C, "itemShowBorder", true).set(itemShowBorder);
        saveColor(C, "itemBorderColor", itemBorderColor);
        config.get(C, "showMainHandCountSeparate", true).set(showMainHandCountSeparate);
        config.get(C, "mainHandCountX", 460).set(mainHandCountX);
        config.get(C, "mainHandCountY", 470).set(mainHandCountY);
        config.get(C, "showMainHandItem", true).set(showMainHandItem);
        config.get(C, "showItemIcon", true).set(showItemIcon);
        config.get(C, "showItemName", false).set(showItemName);
        config.get(C, "showItemType", false).set(showItemType);
        config.get(C, "showItemCount", true).set(showItemCount);
        config.get(C, "showDurability", true).set(showDurability);
        config.get(C, "showDurabilityBar", false).set(showDurabilityBar);
        config.get(C, "showEnchantments", true).set(showEnchantments);
        config.get(C, "showArrowCount", true).set(showArrowCount);
        config.get(C, "showArmorItems", true).set(showArmorItems);
        config.get(C, "showArmorIcon", true).set(showArmorIcon);
        config.get(C, "showArmorName", false).set(showArmorName);
        config.get(C, "showArmorDurability", true).set(showArmorDurability);
        config.get(C, "showArmorEnchantments", true).set(showArmorEnchantments);
        config.get(C, "itemUseRarityColors", true).set(itemUseRarityColors);
        config.get(C, "itemLowDurabilityThreshold", 20).set(itemLowDurabilityThreshold);
        saveColor(C, "itemNameColor", itemNameColor);
        saveColor(C, "itemCountColor", itemCountColor);
        saveColor(C, "itemDurabilityColor", itemDurabilityColor);
        saveColor(C, "itemLowDurabilityColor", itemLowDurabilityColor);
        saveColor(C, "itemEnchantmentColor", itemEnchantmentColor);
        config.get(C, "showItemDamage", true).set(showItemDamage);
        config.get(C, "showDamageBreakdown", false).set(showDamageBreakdown);

        // --- 模块16 ---
        config.get(C, "enableLowHealthWarning", true).set(enableLowHealthWarning);
        config.get(C, "lowHealthThreshold", 30).set(lowHealthThreshold);
        config.get(C, "darkenIntensity", 0.5f).set(darkenIntensity);
        config.get(C, "vignetteSize", 0.7f).set(vignetteSize);
        saveColor(C, "darkenColor", darkenColor);

        // --- 模块17 ---
        config.get(C, "showPingHUD", true).set(showPingHUD);
        config.get(C, "pingHudXOffset", -10).set(pingHudX);
        config.get(C, "pingHudYOffset", -100).set(pingHudY);
        saveColor(C, "pingColor", pingColor);
        saveColor(C, "pingGoodColor", pingGoodColor);
        saveColor(C, "pingMediumColor", pingMediumColor);
        saveColor(C, "pingBadColor", pingBadColor);
        config.get(C, "goodPingThreshold", 100).set(goodPingThreshold);
        config.get(C, "mediumPingThreshold", 300).set(mediumPingThreshold);
        config.get(C, "showPingHistory", true).set(showPingHistory);
        config.get(C, "pingHistorySize", 50).set(pingHistorySize);
        config.get(C, "showRealPing", true).set(showRealPing);
        config.get(C, "useTrafficBasedPing", true).set(useTrafficBasedPing);
        config.get(C, "showPingSource", false).set(showPingSource);
        config.get(C, "realPingUpdateInterval", 1000).set(realPingUpdateInterval);
        config.get(C, "showNetworkDetails", false).set(showNetworkDetails);

        // --- 模块18 ---
        config.get(C, "targetHPEnabled", true).set(targetHPEnabled);
        config.get(C, "targetHPStyle", "BAR_AND_TEXT").set(targetHPStyle);
        config.get(C, "targetHPMaxRange", 32.0f).set(targetHPMaxRange);
        config.get(C, "targetHPShowPlayers", true).set(targetHPShowPlayers);
        config.get(C, "targetHPShowMobs", true).set(targetHPShowMobs);
        config.get(C, "targetHPShowBosses", true).set(targetHPShowBosses);
        config.get(C, "targetHPShowSelf", false).set(targetHPShowSelf);
        config.get(C, "targetHPShowName", true).set(targetHPShowName);
        config.get(C, "targetHPColorR", 255).set(targetHPColorR);
        config.get(C, "targetHPColorG", 50).set(targetHPColorG);
        config.get(C, "targetHPColorB", 50).set(targetHPColorB);
        config.get(C, "targetHPBackColorR", 0).set(targetHPBackColorR);
        config.get(C, "targetHPBackColorG", 0).set(targetHPBackColorG);
        config.get(C, "targetHPBackColorB", 0).set(targetHPBackColorB);
        config.get(C, "targetHPBgAlpha", 128).set(targetHPBgAlpha);
        config.get(C, "targetHPTextColorR", 255).set(targetHPTextColorR);
        config.get(C, "targetHPTextColorG", 255).set(targetHPTextColorG);
        config.get(C, "targetHPTextColorB", 255).set(targetHPTextColorB);
        config.get(C, "targetHPOffsetX", 0).set(targetHPOffsetX);
        config.get(C, "targetHPOffsetY", 0).set(targetHPOffsetY);
        config.get(C, "targetHPShowArmor", true).set(targetHPShowArmor);
        config.get(C, "targetHPBarWidth", 80).set(targetHPBarWidth);
        config.get(C, "targetHPShowLabels", true).set(targetHPShowLabels);
        config.get(C, "targetHPShowArmorLabels", true).set(targetHPShowArmorLabels);
        config.get(C, "targetHPShowFace", true).set(targetHPShowFace);
        config.get(C, "targetHPFaceSize", 14).set(targetHPFaceSize);

        // 持久化到磁盘
        config.save();
        System.out.println("[BetterPlayerHUD] 配置已保存");
    }

    // ================================================================
    //  saveAndReload — 先存后读，确保写入和内存同步
    // ================================================================
    public void saveAndReload() {
        saveConfig();
        reloadFromConfig();
    }
}
