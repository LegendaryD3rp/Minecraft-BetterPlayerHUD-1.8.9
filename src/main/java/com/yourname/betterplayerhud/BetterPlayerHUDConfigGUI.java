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
                BetterPlayerHUD.MODID, false, false, "Better Player HUD Configuration");
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

        // === 模块14b：RGB 动态流光 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "RGB 动态流光设置", "compassmod.category.rgb",
                getRGBConfigElements(),
                new ColorPreviewHelper.ColorInfo[0],
                "RGB Flow"
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

        // === 模块20：自定义准星 ===
        elements.add(ColorPreviewHelper.createPreviewCategory(
                "自定义准星设置", "compassmod.category.crosshair",
                getCrosshairConfigElements(),
                new ColorPreviewHelper.ColorInfo[]{
                        new ColorPreviewHelper.ColorInfo("crosshairColor", "准星颜色"),
                },
                "Crosshair"
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
        addEl(list, "keysOpacity");
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
        addEl(list, "hideHitboxForInvisible");
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  模块14b：RGB 动态流光
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getRGBConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableRGBMode");
        addEl(list, "rgbFlowMode");
        addEl(list, "rgbColorAlgo");
        addEl(list, "rgbStepMs");
        addEl(list, "rgbSpeed");
        addEl(list, "rgbApplyBlockOutline");
        addEl(list, "rgbApplyEntityHitbox");
        addEl(list, "keyBindToggleBlockOutline");
        addEl(list, "keyBindToggleEntityHitbox");
        addEl(list, "keyBindToggleRGB");
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

    // ═══════════════════════════════════════════════════════════════
    //  模块20：自定义准星 (Crosshair)
    // ═══════════════════════════════════════════════════════════════
    private static List<IConfigElement> getCrosshairConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        addEl(list, "enableCrosshair");
        addEl(list, "crosshairStyle");
        addEl(list, "crosshairLength");
        addEl(list, "crosshairGap");
        addEl(list, "crosshairThickness");
        addEl(list, "crosshairArmUp");
        addEl(list, "crosshairArmDown");
        addEl(list, "crosshairArmLeft");
        addEl(list, "crosshairArmRight");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "crosshairColor");
        addEl(list, "crosshairRGB");
        addEl(list, "crosshairOutline");
        ColorPreviewHelper.addColorElements(list, cfg(), cat(), "crosshairOutlineColor");
        addEl(list, "crosshairOutlineWidth");
        addEl(list, "crosshairRotation");
        addEl(list, "crosshairSpread");
        addEl(list, "crosshairSpreadAmount");
        addEl(list, "crosshairSpreadWalk");
        addEl(list, "crosshairSpreadJump");
        addEl(list, "crosshairSpreadBow");
        addEl(list, "crosshairXOffset");
        addEl(list, "crosshairYOffset");
        addEl(list, "crosshairAlwaysShow");
        addEl(list, "crosshairShowInThirdPerson");
        addEl(list, "crosshairDotSize");
        addEl(list, "crosshairCircleRadius");
        addEl(list, "crosshairCircleSegments");
        return list;
    }
}
