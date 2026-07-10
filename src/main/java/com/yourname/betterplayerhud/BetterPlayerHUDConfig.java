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
    public boolean showExactAngle = true;

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
    public float keysOpacity = 0.5f;

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

    // ================================================================
    //  模块14b：RGB 动态流光 & 隐身隐藏（描边/碰撞箱专用）
    // ================================================================
    public boolean enableRGBMode = true;
    public int rgbStepMs = 0;                           // 0=平滑流动，>0=步进间隔(ms)
    public int rgbSpeed = 500;                          // 色相循环速度（ms/周期），越大越慢
    public boolean rgbApplyBlockOutline = true;
    public boolean rgbApplyEntityHitbox = true;
    public String rgbFlowMode = "perimeter";            // "perimeter"=逐棱流动, "uniform"=整框同色
    public String rgbColorAlgo = "hsv";                 // "hsv"=HSBtoRGB, "sinewave"=正弦波三通道
    public boolean hideHitboxForInvisible = false;      // 隐身实体不画碰撞箱
    public int keyBindToggleBlockOutline = 0;           // 方块描边开关快捷键
    public int keyBindToggleEntityHitbox = 0;           // 实体碰撞箱开关快捷键
    public int keyBindToggleRGB = 0;                    // RGB流光开关快捷键

    // ================================================================
    //  模块18：目标血量显示 (Target HP)
    // ================================================================
    public boolean targetHPEnabled = true;

    // ================================================================
    //  模块19：性能检测（FPS / 坐标 / TPS / Ping / 服务器IP）
    // ================================================================
    public boolean showPerformanceHUD = true;
    public int performanceHudX = 5;
    public int performanceHudY = 65;
    public int performanceTextColor = 0xAAAAAA;
    public boolean showFPS = true;
    public boolean showXYZ = true;
    public boolean showTPS = true;
    public boolean showPing = true;
    public boolean showServerIP = true;
    public int serverInfoTPSGoodColor = 0x55FF55;
    public int serverInfoTPSMediumColor = 0xFFFF55;
    public int serverInfoTPSBadColor = 0xFF5555;
    public int serverInfoGoodTpsThreshold = 18;
    public int serverInfoMediumTpsThreshold = 15;
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
    //  模块20：自定义准星 (Crosshair)
    // ================================================================
    public boolean enableCrosshair = true;
    public String crosshairStyle = "cross_gap";              // dot / cross / cross_gap / csgo / circle / diamond / triangle
    public boolean crosshairArmUp = true;
    public boolean crosshairArmDown = true;
    public boolean crosshairArmLeft = true;
    public boolean crosshairArmRight = true;
    public int crosshairLength = 10;
    public int crosshairGap = 2;
    public int crosshairThickness = 2;
    public int crosshairColor = 0xFFFFFFFF;
    public boolean crosshairRGB = false;
    public boolean crosshairOutline = false;
    public int crosshairOutlineColor = 0xAA000000;
    public int crosshairOutlineWidth = 1;
    public int crosshairRotation = 0;
    public boolean crosshairSpread = true;
    public float crosshairSpreadAmount = 1.0f;
    public boolean crosshairSpreadWalk = true;
    public boolean crosshairSpreadJump = true;
    public boolean crosshairSpreadBow = true;
    public boolean crosshairSpreadBowInverted = false;
    public int crosshairXOffset = 0;
    public int crosshairYOffset = 0;
    public boolean crosshairAlwaysShow = false;
    public boolean crosshairShowInThirdPerson = true;
    public int crosshairDotSize = 2;
    public int crosshairCircleRadius = 8;
    public int crosshairCircleSegments = 24;

    // ================================================================
    //  模块22：药水效果 HUD
    // ================================================================
    public boolean enablePotionHUD = true;
    public int potionTextColor = 0xFFFFFFFF;
    public int potionXOffset = 0;
    public int potionYOffset = 0;

    // ================================================================
    //  模块23：装备&手持物品 HUD
    // ================================================================
    public boolean enableArmorHUD = false;
    public boolean enableHeldItemHUD = true;
    public int armorXOffset = 0;
    public int armorYOffset = 0;
    public int heldItemXOffset = 0;
    public int heldItemYOffset = 0;
    /** 物品栏横条左右侧物品数量统计 */
    public boolean showItemCountLeft = true;
    public boolean showItemCountRight = true;
    public int itemCountX = 0;
    public int itemCountY = 0;
    public boolean showHeldItemEnchants = true;
    public boolean showArmorBackground = true;

    // ================================================================
    //  模块24：危机警戒图标
    // ================================================================
    public boolean enableCriticalHealth = true;
    public boolean crisisWarnHealth = true;
    public double crisisHealthThreshold = 2.0;
    public boolean crisisWarnHunger = true;
    public int crisisHungerThreshold = 6;
    public boolean crisisWarnTnt = true;
    public double crisisTntRadius = 10.0;
    public boolean crisisWarnBow = true;
    /** 箭矢飞行物附近警戒 */
    public boolean crisisWarnArrow = true;
    public double crisisArrowRadius = 10.0;
    /** 危机警戒偏移量（相对屏幕居中位置） */
    public int crisisXOffset = 0;
    public int crisisYOffset = 0;
    public int crisisIconSize = 72;
    public int crisisFlashInterval = 20;

    // ================================================================
    //  模块25：连击计数 (Combo Display)
    // ================================================================
    public boolean enableCombo = true;
    public int comboXOffset = 0;
    public int comboYOffset = 0;

    // ================================================================
    //  模块21：命中标识 (Hit Marker)
    // ================================================================
    public boolean enableHitMarker = true;
    // ── audio ──
    public boolean enableHitSounds = true;
    public boolean enableKillSound = true;
    public float soundVolume = 1.0f;
    // ── visual: hit ──
    public float hitAlpha = 1.0f;
    public int hitColor = 0xFFFFFFFF;
    public float hitSize = 8.0f;
    // ── visual: kill ──
    public float killAlpha = 1.0f;
    public int killColor = 0xFFFF0000;
    public float killSize = 12.0f;
    // ── border ──
    public boolean hitMarkerEnableBorder = false;
    public float hitMarkerBorderWidth = 1.5f;
    public int hitMarkerBorderColor = 0xFF000000;
    public int hitMarkerKillBorderColor = 0xFF000000;
    // ── effects ──
    public float hitBloodIntensity = 0.3f;
    // ── chat ──
    public boolean enableChatKillDetection = true;
    // ── rotation ──
    public boolean hitMarkerRandomRotate = true;
    public float hitMarkerRandomRotateStrength = 20.0f;

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
        config.getCategory(C).remove("potionTextColor");
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
        // 旧键名迁移（已改为不带 Offset 后缀 & compassScale→scale）
        config.getCategory(C).remove("xPositionOffset");
        config.getCategory(C).remove("yPositionOffset");
        config.getCategory(C).remove("compassScale");
        config.getCategory(C).remove("healthHudXOffset");
        config.getCategory(C).remove("healthHudYOffset");
        config.getCategory(C).remove("distanceHudXOffset");
        config.getCategory(C).remove("distanceHudYOffset");

        // --- 模块1：罗盘 HUD ---
        {
            Property p = config.get(C, "showCompassHUD", true);
            p.comment = "是否显示罗盘HUD"; showCompassHUD = p.getBoolean();

            p = config.get(C, "xPosition", 0);
            p.comment = "罗盘X轴偏移量"; xPosition = p.getInt();

            p = config.get(C, "yPosition", 50);
            p.comment = "罗盘Y轴偏移量"; yPosition = p.getInt();

            p = config.get(C, "scale", 1.0f);
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

            p = config.get(C, "showExactAngle", true);
            p.comment = "显示精确角度(F3角度)"; showExactAngle = p.getBoolean();
        }

        // --- 模块2：玩家信息 HUD ---
        {
            Property p = config.get(C, "showHealthHUD", true);
            p.comment = "是否显示血量HUD"; showHealthHUD = p.getBoolean();

            p = config.get(C, "healthHudX", 10);
            p.comment = "血量HUD X坐标偏移"; healthHudX = p.getInt();

            p = config.get(C, "healthHudY", -50);
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

        // --- 模块7：距离 HUD ---
        {
            Property p = config.get(C, "showDistanceHUD", true);
            p.comment = "是否显示距离HUD"; showDistanceHUD = p.getBoolean();

            p = config.get(C, "distanceHudX", 490);
            p.comment = "距离HUD X坐标偏移"; distanceHudX = p.getInt();

            p = config.get(C, "distanceHudY", 280);
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

            p = config.get(C, "keysOpacity", 0.5);
            p.comment = "按键显示透明度 (0.0~1.0)"; p.setMinValue(0.0).setMaxValue(1.0); keysOpacity = (float) p.getDouble();
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

            p = config.get(C, "rgbStepMs", 0);
            p.comment = "步进间隔(ms,0=平滑)"; rgbStepMs = p.getInt();

            p = config.get(C, "rgbSpeed", 500);
            p.comment = "RGB 色相循环速度（毫秒/周期，越大越慢）"; rgbSpeed = p.getInt();

            p = config.get(C, "rgbApplyBlockOutline", true);
            p.comment = "RGB 应用于方块描边"; rgbApplyBlockOutline = p.getBoolean();

            p = config.get(C, "rgbApplyEntityHitbox", true);
            p.comment = "RGB 应用于实体碰撞箱"; rgbApplyEntityHitbox = p.getBoolean();

            p = config.get(C, "hideHitboxForInvisible", false);
            p.comment = "隐身生物/玩家不显示碰撞箱（默认关，可开）"; hideHitboxForInvisible = p.getBoolean();

            p = config.get(C, "rgbFlowMode", "perimeter");
            p.setValidValues(new String[]{"perimeter", "uniform"});
            p.comment = "流光模式：perimeter=逐棱流动, uniform=整框同色"; rgbFlowMode = p.getString();

            p = config.get(C, "rgbColorAlgo", "hsv");
            p.setValidValues(new String[]{"hsv", "sinewave"});
            p.comment = "颜色算法：hsv=HSBtoRGB, sinewave=正弦波三通道"; rgbColorAlgo = p.getString();
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

        // --- 模块19：性能检测 ---
        {
            Property p = config.get(C, "showPerformanceHUD", true);
            p.comment = "是否显示性能检测HUD（FPS/坐标/TPS/Ping/IP）"; showPerformanceHUD = p.getBoolean();

            p = config.get(C, "performanceHudX", 5);
            p.comment = "X 坐标"; performanceHudX = p.getInt();

            p = config.get(C, "performanceHudY", 65);
            p.comment = "Y 坐标"; performanceHudY = p.getInt();

            performanceTextColor = loadColor(C, "performanceTextColor", 170, 170, 170);

            p = config.get(C, "showFPS", true);
            p.comment = "是否显示FPS"; showFPS = p.getBoolean();

            p = config.get(C, "showXYZ", true);
            p.comment = "是否显示坐标"; showXYZ = p.getBoolean();

            p = config.get(C, "showTPS", true);
            p.comment = "是否显示实时TPS"; showTPS = p.getBoolean();

            p = config.get(C, "showPing", true);
            p.comment = "是否显示延迟"; showPing = p.getBoolean();

            p = config.get(C, "showServerIP", true);
            p.comment = "是否显示服务器IP"; showServerIP = p.getBoolean();

            serverInfoTPSGoodColor = loadColor(C, "serverInfoTPSGoodColor", 85, 255, 85);
            serverInfoTPSMediumColor = loadColor(C, "serverInfoTPSMediumColor", 255, 255, 85);
            serverInfoTPSBadColor = loadColor(C, "serverInfoTPSBadColor", 255, 85, 85);

            p = config.get(C, "serverInfoGoodTpsThreshold", 18);
            p.comment = "良好TPS阈值"; serverInfoGoodTpsThreshold = p.getInt();

            p = config.get(C, "serverInfoMediumTpsThreshold", 15);
            p.comment = "中等TPS阈值"; serverInfoMediumTpsThreshold = p.getInt();
        }

        // --- 模块20：自定义准星 ---
        {
            Property p = config.get(C, "enableCrosshair", true);
            p.comment = "是否启用自定义准星（取代原版）"; enableCrosshair = p.getBoolean();

            p = config.get(C, "crosshairStyle", "cross_gap");
            p.comment = "准星样式";
            p.setValidValues(new String[]{"dot", "cross", "cross_gap", "csgo", "circle", "diamond", "triangle"});
            crosshairStyle = p.getString();

            p = config.get(C, "crosshairArmUp", true);
            p.comment = "上臂显示"; crosshairArmUp = p.getBoolean();
            p = config.get(C, "crosshairArmDown", true);
            p.comment = "下臂显示"; crosshairArmDown = p.getBoolean();
            p = config.get(C, "crosshairArmLeft", true);
            p.comment = "左臂显示"; crosshairArmLeft = p.getBoolean();
            p = config.get(C, "crosshairArmRight", true);
            p.comment = "右臂显示"; crosshairArmRight = p.getBoolean();

            p = config.get(C, "crosshairLength", 10);
            p.comment = "准星臂长(像素)"; crosshairLength = p.getInt();

            p = config.get(C, "crosshairGap", 2);
            p.comment = "中心间隙(像素)"; crosshairGap = p.getInt();

            p = config.get(C, "crosshairThickness", 2);
            p.comment = "准星粗细(像素)"; crosshairThickness = p.getInt();

            crosshairColor = loadColor(C, "crosshairColor", 255, 255, 255);

            p = config.get(C, "crosshairRGB", false);
            p.comment = "启用RGB流光（复用RGB配置的速度/算法）"; crosshairRGB = p.getBoolean();

            p = config.get(C, "crosshairOutline", false);
            p.comment = "显示描边"; crosshairOutline = p.getBoolean();

            crosshairOutlineColor = loadColor(C, "crosshairOutlineColor", 0, 0, 0);

            p = config.get(C, "crosshairOutlineWidth", 1);
            p.comment = "描边粗细(像素)"; crosshairOutlineWidth = p.getInt();

            p = config.get(C, "crosshairRotation", 0);
            p.comment = "旋转角度(0~360)"; crosshairRotation = p.getInt();

            p = config.get(C, "crosshairSpread", true);
            p.comment = "启用动态扩散"; crosshairSpread = p.getBoolean();

            p = config.get(C, "crosshairSpreadAmount", 1.0);
            p.comment = "扩散强度(倍数)"; crosshairSpreadAmount = (float) p.getDouble();

            p = config.get(C, "crosshairSpreadWalk", true);
            p.comment = "行走时扩散"; crosshairSpreadWalk = p.getBoolean();

            p = config.get(C, "crosshairSpreadJump", true);
            p.comment = "跳跃时扩散"; crosshairSpreadJump = p.getBoolean();

            p = config.get(C, "crosshairSpreadBow", true);
            p.comment = "拉弓时扩散"; crosshairSpreadBow = p.getBoolean();

            p = config.get(C, "crosshairSpreadBowInverted", false);
            p.comment = "反向拉弓扩散（随蓄力扩大）"; crosshairSpreadBowInverted = p.getBoolean();

            p = config.get(C, "crosshairXOffset", 0);
            p.comment = "水平偏移"; crosshairXOffset = p.getInt();
            p = config.get(C, "crosshairYOffset", 0);
            p.comment = "垂直偏移"; crosshairYOffset = p.getInt();

            p = config.get(C, "crosshairAlwaysShow", false);
            p.comment = "不瞄准时也显示"; crosshairAlwaysShow = p.getBoolean();

            p = config.get(C, "crosshairShowInThirdPerson", true);
            p.comment = "第三人称显示"; crosshairShowInThirdPerson = p.getBoolean();

            p = config.get(C, "crosshairDotSize", 2);
            p.comment = "点状准星尺寸"; crosshairDotSize = p.getInt();

            p = config.get(C, "crosshairCircleRadius", 8);
            p.comment = "圆形准星半径"; crosshairCircleRadius = p.getInt();

            p = config.get(C, "crosshairCircleSegments", 24);
            p.comment = "圆形分段数"; crosshairCircleSegments = p.getInt();
        }

        // --- 模块22：药水效果 HUD ---
        {
            Property p = config.get(C, "enablePotionHUD", true);
            p.comment = "是否显示药水效果HUD"; enablePotionHUD = p.getBoolean();

            potionTextColor = loadColor(C, "potionTextColor", 255, 255, 255);
            p.comment = "药水时长文字颜色";

            p = config.get(C, "potionXOffset", 0);
            p.comment = "药水HUD X轴偏移"; potionXOffset = p.getInt();

            p = config.get(C, "potionYOffset", 0);
            p.comment = "药水HUD Y轴偏移"; potionYOffset = p.getInt();
        }

        // --- 模块23：装备&手持物品 HUD ---
        {
            Property p = config.get(C, "enableArmorHUD", false);
            p.comment = "是否在物品栏两侧显示装甲"; enableArmorHUD = p.getBoolean();

            p = config.get(C, "enableHeldItemHUD", true);
            p.comment = "是否在左下方显示手持物品信息"; enableHeldItemHUD = p.getBoolean();

            p = config.get(C, "armorXOffset", 0);
            p.comment = "装甲栏X偏移"; armorXOffset = p.getInt();
            p = config.get(C, "armorYOffset", 0);
            p.comment = "装甲栏Y偏移"; armorYOffset = p.getInt();
            p = config.get(C, "heldItemXOffset", 0);
            p.comment = "手持物品信息X偏移"; heldItemXOffset = p.getInt();
            p = config.get(C, "heldItemYOffset", 0);
            p.comment = "手持物品信息Y偏移"; heldItemYOffset = p.getInt();
            p = config.get(C, "showItemCountLeft", true);
            p.comment = "显示物品栏当前格数量"; showItemCountLeft = p.getBoolean();
            p = config.get(C, "showItemCountRight", true);
            p.comment = "显示物品栏背包总数"; showItemCountRight = p.getBoolean();
            p = config.get(C, "itemCountX", 0);
            p.comment = "物品数量X偏移"; itemCountX = p.getInt();
            p = config.get(C, "itemCountY", 0);
            p.comment = "物品数量Y偏移"; itemCountY = p.getInt();
            p = config.get(C, "showHeldItemEnchants", true);
            p.comment = "手持物品是否显示附魔"; showHeldItemEnchants = p.getBoolean();
            p = config.get(C, "showArmorBackground", true);
            p.comment = "装甲栏槽位半透明背景"; showArmorBackground = p.getBoolean();
        }

        // --- 模块24：危机警戒图标 ---
        {
            Property p = config.get(C, "enableCriticalHealth", true);
            p.comment = "启用危机警戒图标"; enableCriticalHealth = p.getBoolean();
            p = config.get(C, "crisisWarnHealth", true);
            p.comment = "低血量警戒"; crisisWarnHealth = p.getBoolean();
            p = config.get(C, "crisisHealthThreshold", 2.0);
            p.comment = "低血量阈值"; crisisHealthThreshold = p.getDouble();
            p = config.get(C, "crisisWarnHunger", true);
            p.comment = "饥饿警戒"; crisisWarnHunger = p.getBoolean();
            p = config.get(C, "crisisHungerThreshold", 6);
            p.comment = "饥饿阈值"; crisisHungerThreshold = p.getInt();
            p = config.get(C, "crisisWarnTnt", true);
            p.comment = "TNT附近警戒"; crisisWarnTnt = p.getBoolean();
            p = config.get(C, "crisisTntRadius", 10.0);
            p.comment = "TNT检测半径"; crisisTntRadius = p.getDouble();
            p = config.get(C, "crisisWarnBow", true);
            p.comment = "拉弓警戒"; crisisWarnBow = p.getBoolean();
            p = config.get(C, "crisisWarnArrow", true);
            p.comment = "箭矢警戒"; crisisWarnArrow = p.getBoolean();
            p = config.get(C, "crisisArrowRadius", 10.0);
            p.comment = "箭矢检测半径"; crisisArrowRadius = p.getDouble();
            p = config.get(C, "crisisXOffset", 0);
            p.comment = "X偏移(居中偏移)"; crisisXOffset = p.getInt();
            p = config.get(C, "crisisYOffset", 0);
            p.comment = "Y偏移(居中偏移)"; crisisYOffset = p.getInt();
            p = config.get(C, "crisisIconSize", 72);
            p.comment = "图标大小(默认72，三倍显眼)"; crisisIconSize = p.getInt();
            p = config.get(C, "crisisFlashInterval", 20);
            p.comment = "闪烁周期(ticks)"; crisisFlashInterval = p.getInt();
        }

        // --- 模块25：连击计数 ---
        {
            Property p = config.get(C, "enableCombo", true);
            p.comment = "启用连击计数(Combo Display)"; enableCombo = p.getBoolean();
            p = config.get(C, "comboXOffset", 0);
            p.comment = "X偏移(右下偏移)"; comboXOffset = p.getInt();
            p = config.get(C, "comboYOffset", 0);
            p.comment = "Y偏移(右下偏移)"; comboYOffset = p.getInt();
        }

        // --- 模块21：命中标识 ---
        {
            Property p = config.get(C, "enableHitMarker", true);
            p.comment = "启用命中标识 (HitMarker)"; enableHitMarker = p.getBoolean();

            // audio
            p = config.get(C, "enableHitSounds", true);
            p.comment = "击中音效"; enableHitSounds = p.getBoolean();
            p = config.get(C, "enableKillSound", true);
            p.comment = "击杀音效"; enableKillSound = p.getBoolean();
            p = config.get(C, "soundVolume", 1.0);
            p.comment = "音效音量(0~1)"; soundVolume = (float) p.getDouble();

            // visual: hit
            p = config.get(C, "hitAlpha", 1.0);
            p.comment = "击中标识透明度"; hitAlpha = (float) p.getDouble();
            hitColor = loadColor(C, "hitColor", 255, 255, 255);
            p = config.get(C, "hitSize", 8.0);
            p.comment = "击中标识尺寸"; hitSize = (float) p.getDouble();

            // visual: kill
            p = config.get(C, "killAlpha", 1.0);
            p.comment = "击杀标识透明度"; killAlpha = (float) p.getDouble();
            killColor = loadColor(C, "killColor", 255, 0, 0);
            p = config.get(C, "killSize", 12.0);
            p.comment = "击杀标识尺寸"; killSize = (float) p.getDouble();

            // border
            p = config.get(C, "hitMarkerEnableBorder", false);
            p.comment = "显示边框"; hitMarkerEnableBorder = p.getBoolean();
            p = config.get(C, "hitMarkerBorderWidth", 1.5);
            p.comment = "边框宽度"; hitMarkerBorderWidth = (float) p.getDouble();
            hitMarkerBorderColor = loadColor(C, "hitMarkerBorderColor", 0, 0, 0);
            hitMarkerKillBorderColor = loadColor(C, "hitMarkerKillBorderColor", 0, 0, 0);

            // effects
            p = config.get(C, "hitBloodIntensity", 0.3);
            p.comment = "血迹粒子浓度(0~1)"; hitBloodIntensity = (float) p.getDouble();

            // chat
            p = config.get(C, "enableChatKillDetection", true);
            p.comment = "聊天击杀检测(中英文)"; enableChatKillDetection = p.getBoolean();

            // rotation
            p = config.get(C, "hitMarkerRandomRotate", true);
            p.comment = "随机旋转角度"; hitMarkerRandomRotate = p.getBoolean();
            p = config.get(C, "hitMarkerRandomRotateStrength", 20.0);
            p.comment = "旋转幅度(0~360)"; hitMarkerRandomRotateStrength = (float) p.getDouble();
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
        config.get(C, "xPosition", 0).set(xPosition);
        config.get(C, "yPosition", 50).set(yPosition);
        config.get(C, "scale", 1.0f).set(scale);
        config.get(C, "displayStyle", "detailed").set(displayStyle);
        config.get(C, "showDegreeMarks", true).set(showDegreeMarks);
        config.get(C, "showCompassNeedle", true).set(showCompassNeedle);
        saveColor(C, "compassColor", compassColor);
        config.get(C, "degreeMarkInterval", 15).set(degreeMarkInterval);
        config.get(C, "dynamicScaling", true).set(dynamicScaling);
        config.get(C, "showHorizon", false).set(showHorizon);
        config.get(C, "showExactAngle", true).set(showExactAngle);

        // --- 模块2 ---
        config.get(C, "showHealthHUD", true).set(showHealthHUD);
        config.get(C, "healthHudX", 10).set(healthHudX);
        config.get(C, "healthHudY", -50).set(healthHudY);
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

        // --- 模块7 ---
        config.get(C, "showDistanceHUD", true).set(showDistanceHUD);
        config.get(C, "distanceHudX", 490).set(distanceHudX);
        config.get(C, "distanceHudY", 280).set(distanceHudY);
        saveColor(C, "distanceColor", distanceColor);
        config.get(C, "distancePrecision", 1).set(distancePrecision);
        config.get(C, "showTargetInfo", true).set(showTargetInfo);
        saveColor(C, "targetInfoColor", targetInfoColor);
        config.get(C, "showBlockCoordinates", true).set(showBlockCoordinates);
        config.get(C, "showBlockHardness", true).set(showBlockHardness);
        config.get(C, "showRequiredTool", true).set(showRequiredTool);
        saveColor(C, "blockInfoColor", blockInfoColor);

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
        config.get(C, "keysOpacity", 0.5).set(keysOpacity);

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
        config.get(C, "rgbStepMs", 0).set(rgbStepMs);
        config.get(C, "rgbSpeed", 500).set(rgbSpeed);
        config.get(C, "rgbApplyBlockOutline", true).set(rgbApplyBlockOutline);
        config.get(C, "rgbApplyEntityHitbox", true).set(rgbApplyEntityHitbox);
        config.get(C, "hideHitboxForInvisible", false).set(hideHitboxForInvisible);
        config.get(C, "rgbFlowMode", "perimeter").set(rgbFlowMode);
        config.get(C, "rgbColorAlgo", "hsv").set(rgbColorAlgo);

        // --- 模块14c ---
        config.get(C, "keyBindToggleBlockOutline", 0).set(keyBindToggleBlockOutline);
        config.get(C, "keyBindToggleEntityHitbox", 0).set(keyBindToggleEntityHitbox);
        config.get(C, "keyBindToggleRGB", 0).set(keyBindToggleRGB);

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

        // --- 模块19 ---
        config.get(C, "showPerformanceHUD", true).set(showPerformanceHUD);
        config.get(C, "performanceHudX", 5).set(performanceHudX);
        config.get(C, "performanceHudY", 65).set(performanceHudY);
        saveColor(C, "performanceTextColor", performanceTextColor);
        config.get(C, "showFPS", true).set(showFPS);
        config.get(C, "showXYZ", true).set(showXYZ);
        config.get(C, "showTPS", true).set(showTPS);
        config.get(C, "showPing", true).set(showPing);
        config.get(C, "showServerIP", true).set(showServerIP);
        saveColor(C, "serverInfoTPSGoodColor", serverInfoTPSGoodColor);
        saveColor(C, "serverInfoTPSMediumColor", serverInfoTPSMediumColor);
        saveColor(C, "serverInfoTPSBadColor", serverInfoTPSBadColor);
        config.get(C, "serverInfoGoodTpsThreshold", 18).set(serverInfoGoodTpsThreshold);
        config.get(C, "serverInfoMediumTpsThreshold", 15).set(serverInfoMediumTpsThreshold);

        // --- 模块20：自定义准星 ---
        config.get(C, "enableCrosshair", true).set(enableCrosshair);
        config.get(C, "crosshairStyle", "cross_gap").set(crosshairStyle);
        config.get(C, "crosshairArmUp", true).set(crosshairArmUp);
        config.get(C, "crosshairArmDown", true).set(crosshairArmDown);
        config.get(C, "crosshairArmLeft", true).set(crosshairArmLeft);
        config.get(C, "crosshairArmRight", true).set(crosshairArmRight);
        config.get(C, "crosshairLength", 10).set(crosshairLength);
        config.get(C, "crosshairGap", 2).set(crosshairGap);
        config.get(C, "crosshairThickness", 2).set(crosshairThickness);
        saveColor(C, "crosshairColor", crosshairColor);
        config.get(C, "crosshairRGB", false).set(crosshairRGB);
        config.get(C, "crosshairOutline", false).set(crosshairOutline);
        saveColor(C, "crosshairOutlineColor", crosshairOutlineColor);
        config.get(C, "crosshairOutlineWidth", 1).set(crosshairOutlineWidth);
        config.get(C, "crosshairRotation", 0).set(crosshairRotation);
        config.get(C, "crosshairSpread", true).set(crosshairSpread);
        config.get(C, "crosshairSpreadAmount", 1.0f).set(crosshairSpreadAmount);
        config.get(C, "crosshairSpreadWalk", true).set(crosshairSpreadWalk);
        config.get(C, "crosshairSpreadJump", true).set(crosshairSpreadJump);
        config.get(C, "crosshairSpreadBow", true).set(crosshairSpreadBow);
        config.get(C, "crosshairSpreadBowInverted", false).set(crosshairSpreadBowInverted);
        config.get(C, "crosshairXOffset", 0).set(crosshairXOffset);
        config.get(C, "crosshairYOffset", 0).set(crosshairYOffset);
        config.get(C, "crosshairAlwaysShow", false).set(crosshairAlwaysShow);
        config.get(C, "crosshairShowInThirdPerson", true).set(crosshairShowInThirdPerson);
        config.get(C, "crosshairDotSize", 2).set(crosshairDotSize);
        config.get(C, "crosshairCircleRadius", 8).set(crosshairCircleRadius);
        config.get(C, "crosshairCircleSegments", 24).set(crosshairCircleSegments);

        // --- 模块22：药水效果 HUD ---
        config.get(C, "enablePotionHUD", true).set(enablePotionHUD);
        saveColor(C, "potionTextColor", potionTextColor);
        config.get(C, "potionXOffset", 0).set(potionXOffset);
        config.get(C, "potionYOffset", 0).set(potionYOffset);

        // --- 模块23：装备&手持物品 HUD ---
        config.get(C, "enableArmorHUD", true).set(enableArmorHUD);
        config.get(C, "enableHeldItemHUD", true).set(enableHeldItemHUD);
        config.get(C, "armorXOffset", 0).set(armorXOffset);
        config.get(C, "armorYOffset", 0).set(armorYOffset);
        config.get(C, "heldItemXOffset", 0).set(heldItemXOffset);
        config.get(C, "heldItemYOffset", 0).set(heldItemYOffset);
        config.get(C, "showItemCountLeft", true).set(showItemCountLeft);
        config.get(C, "showItemCountRight", true).set(showItemCountRight);
        config.get(C, "itemCountX", 0).set(itemCountX);
        config.get(C, "itemCountY", 0).set(itemCountY);
        config.get(C, "showHeldItemEnchants", true).set(showHeldItemEnchants);
        config.get(C, "showArmorBackground", true).set(showArmorBackground);

        // --- 模块24：危机警戒图标 ---
        config.get(C, "enableCriticalHealth", true).set(enableCriticalHealth);
        config.get(C, "crisisWarnHealth", true).set(crisisWarnHealth);
        config.get(C, "crisisHealthThreshold", 2.0).set(crisisHealthThreshold);
        config.get(C, "crisisWarnHunger", true).set(crisisWarnHunger);
        config.get(C, "crisisHungerThreshold", 6).set(crisisHungerThreshold);
        config.get(C, "crisisWarnTnt", true).set(crisisWarnTnt);
        config.get(C, "crisisTntRadius", 10.0).set(crisisTntRadius);
        config.get(C, "crisisWarnBow", true).set(crisisWarnBow);
        config.get(C, "crisisWarnArrow", true).set(crisisWarnArrow);
        config.get(C, "crisisArrowRadius", 10.0).set(crisisArrowRadius);
        config.get(C, "crisisXOffset", 0).set(crisisXOffset);
        config.get(C, "crisisYOffset", 0).set(crisisYOffset);
        config.get(C, "crisisIconSize", 72).set(crisisIconSize);
        config.get(C, "crisisFlashInterval", 20).set(crisisFlashInterval);

        // --- 模块25：连击计数 ---
        config.get(C, "enableCombo", true).set(enableCombo);
        config.get(C, "comboXOffset", 0).set(comboXOffset);
        config.get(C, "comboYOffset", 0).set(comboYOffset);

        // --- 模块21：命中标识 ---
        config.get(C, "enableHitMarker", true).set(enableHitMarker);
        config.get(C, "enableHitSounds", true).set(enableHitSounds);
        config.get(C, "enableKillSound", true).set(enableKillSound);
        config.get(C, "soundVolume", 1.0).set(soundVolume);
        config.get(C, "hitAlpha", 1.0).set(hitAlpha);
        saveColor(C, "hitColor", hitColor);
        config.get(C, "hitSize", 8.0).set(hitSize);
        config.get(C, "killAlpha", 1.0).set(killAlpha);
        saveColor(C, "killColor", killColor);
        config.get(C, "killSize", 12.0).set(killSize);
        config.get(C, "hitMarkerEnableBorder", false).set(hitMarkerEnableBorder);
        config.get(C, "hitMarkerBorderWidth", 1.5).set(hitMarkerBorderWidth);
        saveColor(C, "hitMarkerBorderColor", hitMarkerBorderColor);
        saveColor(C, "hitMarkerKillBorderColor", hitMarkerKillBorderColor);
        config.get(C, "hitBloodIntensity", 0.3).set(hitBloodIntensity);
        config.get(C, "enableChatKillDetection", true).set(enableChatKillDetection);
        config.get(C, "hitMarkerRandomRotate", true).set(hitMarkerRandomRotate);
        config.get(C, "hitMarkerRandomRotateStrength", 20.0).set(hitMarkerRandomRotateStrength);

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
