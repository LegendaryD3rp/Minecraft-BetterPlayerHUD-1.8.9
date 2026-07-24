package com.yourname.betterplayerhud;

import java.util.HashMap;

/**
 * 运行时中英文切换。硬编码 HashMap，不依赖 Minecraft I18n。
 * 
 * 先建框架 + 核心 30 条，其余后续补齐。
 * toggle(): 右上角按钮 + F9 切换
 */
public class LangManager {

    private static boolean isChinese = true;
    private static final HashMap<String, String> zh_CN = new HashMap<>();
    private static final HashMap<String, String> en_US = new HashMap<>();

    static {
        // ── Tab 标题 ──
        zh_CN.put("config.general", "通用");
        en_US.put("config.general", "General");

        zh_CN.put("config.chat", "聊天");
        en_US.put("config.chat", "Chat");

        zh_CN.put("config.combat", "战斗");
        en_US.put("config.combat", "Combat");

        zh_CN.put("config.performance", "性能");
        en_US.put("config.performance", "Performance");

        zh_CN.put("config.colors", "颜色");
        en_US.put("config.colors", "Colors");

        // ── 模块名 ──
        zh_CN.put("mod.name", "更好的玩家HUD");
        en_US.put("mod.name", "Better Player HUD");

        zh_CN.put("module.compass", "罗盘");
        en_US.put("module.compass", "Compass");

        zh_CN.put("module.health", "状态栏");
        en_US.put("module.health", "Health HUD");

        zh_CN.put("module.distance", "距离信息");
        en_US.put("module.distance", "Distance Info");

        zh_CN.put("module.keys", "按键显示");
        en_US.put("module.keys", "Key Display");

        zh_CN.put("module.performance", "性能检测");
        en_US.put("module.performance", "Performance");

        zh_CN.put("module.target", "目标血量");
        en_US.put("module.target", "Target HP");

        zh_CN.put("module.crisis", "危机警戒");
        en_US.put("module.crisis", "Crisis Warning");

        zh_CN.put("module.combo", "连击计数");
        en_US.put("module.combo", "Combo Counter");

        zh_CN.put("module.potion", "药水效果");
        en_US.put("module.potion", "Potion Effects");

        zh_CN.put("module.potiontimer", "药水计时器");
        en_US.put("module.potiontimer", "Potion Timer");

        zh_CN.put("module.equip", "装备HUD");
        en_US.put("module.equip", "Equipment HUD");

        zh_CN.put("module.chroma", "蜃楼聊天框");
        en_US.put("module.chroma", "Chroma Chat");

        zh_CN.put("module.jumpbar", "跳跃值");
        en_US.put("module.jumpbar", "Jump Bar");

        zh_CN.put("module.airhud", "空气值");
        en_US.put("module.airhud", "Air HUD");

        zh_CN.put("module.mountHP", "坐骑血量");
        en_US.put("module.mountHP", "Mount HP");

        // ── 常用词 ──
        zh_CN.put("common.on", "开");
        en_US.put("common.on", "ON");

        zh_CN.put("common.off", "关");
        en_US.put("common.off", "OFF");

        zh_CN.put("common.save", "保存并关闭");
        en_US.put("common.save", "Save & Close");

        zh_CN.put("common.reset", "重置全部");
        en_US.put("common.reset", "Reset All");

        zh_CN.put("common.cancel", "取消");
        en_US.put("common.cancel", "Cancel");

        zh_CN.put("common.search", "搜索...");
        en_US.put("common.search", "Search...");

        zh_CN.put("common.enabled", "启用");
        en_US.put("common.enabled", "Enabled");

        zh_CN.put("common.disabled", "禁用");
        en_US.put("common.disabled", "Disabled");

        zh_CN.put("common.x", "X 位置");
        en_US.put("common.x", "X Position");

        zh_CN.put("common.y", "Y 位置");
        en_US.put("common.y", "Y Position");

        zh_CN.put("common.scale", "缩放");
        en_US.put("common.scale", "Scale");

        zh_CN.put("common.color", "颜色");
        en_US.put("common.color", "Color");

        zh_CN.put("common.enable", "启用");
        en_US.put("common.enable", "Enable");

        zh_CN.put("common.disable", "禁用");
        en_US.put("common.disable", "Disable");

        // ── P0 第 3 项：经验条 / Boss 血条 / 快捷栏 ──
        zh_CN.put("module.expbar", "经验条");
        en_US.put("module.expbar", "Exp Bar");

        zh_CN.put("xpbar.offsetX", "X 偏移");
        en_US.put("xpbar.offsetX", "X Offset");

        zh_CN.put("xpbar.offsetY", "Y 偏移");
        en_US.put("xpbar.offsetY", "Y Offset");

        zh_CN.put("xpbar.width", "宽度");
        en_US.put("xpbar.width", "Width");

        zh_CN.put("xpbar.height", "高度");
        en_US.put("xpbar.height", "Height");

        zh_CN.put("module.bossHP", "Boss 血条");
        en_US.put("module.bossHP", "Boss HP");

        zh_CN.put("module.hotbar", "快捷栏");
        en_US.put("module.hotbar", "Hotbar");

        zh_CN.put("hotbar.offsetY", "Y 偏移");
        en_US.put("hotbar.offsetY", "Y Offset");

        // ── P1: TabList / DebugInfo / 条目键 ──
        zh_CN.put("module.fps", "帧率显示");
        en_US.put("module.fps", "FPS Display");
        zh_CN.put("module.debug", "调试信息");
        en_US.put("module.debug", "Debug Info");
        zh_CN.put("module.tablist", "玩家列表");
        en_US.put("module.tablist", "Player List");
        zh_CN.put("module.compass", "罗盘");
        en_US.put("module.compass", "Compass");
        zh_CN.put("module.health", "状态栏");
        en_US.put("module.health", "Health HUD");
        zh_CN.put("module.distance", "距离信息");
        en_US.put("module.distance", "Distance Info");
        zh_CN.put("module.keys", "按键显示");
        en_US.put("module.keys", "Key Display");
        zh_CN.put("module.performance", "性能检测");
        en_US.put("module.performance", "Performance");
        zh_CN.put("module.chroma", "蜃楼聊天框");
        en_US.put("module.chroma", "Chroma Chat");
        zh_CN.put("module.hitmarker", "命中标识");
        en_US.put("module.hitmarker", "Hit Marker");
        zh_CN.put("module.combo", "连击计数");
        en_US.put("module.combo", "Combo Counter");
        zh_CN.put("module.target", "目标血量");
        en_US.put("module.target", "Target HP");
        zh_CN.put("module.crisis", "危机警戒");
        en_US.put("module.crisis", "Crisis Warning");
        zh_CN.put("module.potion", "药水效果");
        en_US.put("module.potion", "Potion Effects");
        zh_CN.put("chroma.width", "聊天宽度");
        en_US.put("chroma.width", "Chat Width");
        zh_CN.put("chroma.lines", "可见行数");
        en_US.put("chroma.lines", "Visible Lines");
        zh_CN.put("chroma.timestamp", "时间戳");
        en_US.put("chroma.timestamp", "Timestamps");
        zh_CN.put("chroma.avatar", "头像");
        en_US.put("chroma.avatar", "Avatars");
        zh_CN.put("chroma.signboard", "告示牌合并");
        en_US.put("chroma.signboard", "Signboard Merge");
        zh_CN.put("color.healthSafe", "血量安全色");
        en_US.put("color.healthSafe", "Health Safe");
        zh_CN.put("color.healthWarning", "血量警戒色");
        en_US.put("color.healthWarning", "Health Warning");
        zh_CN.put("color.healthDanger", "血量危险色");
        en_US.put("color.healthDanger", "Health Danger");
        zh_CN.put("color.armor", "护甲颜色");
        en_US.put("color.armor", "Armor Color");
        zh_CN.put("color.hunger", "饥饿颜色");
        en_US.put("color.hunger", "Hunger Color");

        // ── 罗盘 (Compass) ──
        zh_CN.put("compass.show", "显示罗盘");
        en_US.put("compass.show", "Show Compass");
        zh_CN.put("compass.x", "X 位置");
        en_US.put("compass.x", "X Position");
        zh_CN.put("compass.y", "Y 位置");
        en_US.put("compass.y", "Y Position");
        zh_CN.put("compass.scale", "缩放");
        en_US.put("compass.scale", "Scale");
        zh_CN.put("compass.style", "样式");
        en_US.put("compass.style", "Style");
        zh_CN.put("compass.degreeMarks", "角度刻度");
        en_US.put("compass.degreeMarks", "Degree Marks");
        zh_CN.put("compass.needle", "指北针");
        en_US.put("compass.needle", "Needle");
        zh_CN.put("compass.color", "罗盘颜色");
        en_US.put("compass.color", "Compass Color");
        zh_CN.put("compass.interval", "刻度间隔");
        en_US.put("compass.interval", "Mark Interval");
        zh_CN.put("compass.dynamicScale", "动态缩放");
        en_US.put("compass.dynamicScale", "Dynamic Scale");
        zh_CN.put("compass.horizon", "水平线");
        en_US.put("compass.horizon", "Horizon");
        zh_CN.put("compass.exactAngle", "精确角度");
        en_US.put("compass.exactAngle", "Exact Angle");

        // ── 状态栏 (Health) ──
        zh_CN.put("health.show", "显示血量");
        en_US.put("health.show", "Show Health");
        zh_CN.put("health.x", "X 位置");
        en_US.put("health.x", "X Position");
        zh_CN.put("health.y", "Y 位置");
        en_US.put("health.y", "Y Position");
        zh_CN.put("health.showArmor", "显示护甲");
        en_US.put("health.showArmor", "Show Armor");
        zh_CN.put("health.showHunger", "显示饱食度");
        en_US.put("health.showHunger", "Show Hunger");
        zh_CN.put("health.showHead", "显示头像");
        en_US.put("health.showHead", "Show Head");
        zh_CN.put("health.headSize", "头像大小");
        en_US.put("health.headSize", "Head Size");
        zh_CN.put("health.textSpacing", "文字间距");
        en_US.put("health.textSpacing", "Text Spacing");

        // ── 距离 (Distance) ──
        zh_CN.put("dist.show", "显示距离");
        en_US.put("dist.show", "Show Distance");
        zh_CN.put("dist.x", "X 位置");
        en_US.put("dist.x", "X Position");
        zh_CN.put("dist.y", "Y 位置");
        en_US.put("dist.y", "Y Position");
        zh_CN.put("dist.color", "距离颜色");
        en_US.put("dist.color", "Distance Color");
        zh_CN.put("dist.precision", "精确度");
        en_US.put("dist.precision", "Precision");
        zh_CN.put("dist.showTarget", "显示目标");
        en_US.put("dist.showTarget", "Show Target");
        zh_CN.put("dist.targetColor", "目标颜色");
        en_US.put("dist.targetColor", "Target Color");
        zh_CN.put("dist.showBlockInfo", "方块信息");
        en_US.put("dist.showBlockInfo", "Block Info");
        zh_CN.put("dist.hardness", "硬度显示");
        en_US.put("dist.hardness", "Show Hardness");
        zh_CN.put("dist.showTool", "所需工具");
        en_US.put("dist.showTool", "Show Tool");
        zh_CN.put("dist.blockColor", "方块颜色");
        en_US.put("dist.blockColor", "Block Info Color");

        // ── 按键显示 (Keys) ──
        zh_CN.put("keys.show", "显示按键");
        en_US.put("keys.show", "Show Keys");
        zh_CN.put("keys.x", "X 位置");
        en_US.put("keys.x", "X Position");
        zh_CN.put("keys.y", "Y 位置");
        en_US.put("keys.y", "Y Position");
        zh_CN.put("keys.size", "按键大小");
        en_US.put("keys.size", "Key Size");
        zh_CN.put("keys.spacing", "按键间距");
        en_US.put("keys.spacing", "Key Spacing");
        zh_CN.put("keys.activeColor", "按键激活色");
        en_US.put("keys.activeColor", "Active Color");
        zh_CN.put("keys.inactiveColor", "按键闲置色");
        en_US.put("keys.inactiveColor", "Inactive Color");
        zh_CN.put("keys.textColor", "文字颜色");
        en_US.put("keys.textColor", "Text Color");
        zh_CN.put("keys.scale", "文字缩放");
        en_US.put("keys.scale", "Text Scale");
        zh_CN.put("keys.background", "显示背景");
        en_US.put("keys.background", "Show Background");
        zh_CN.put("keys.opacity", "背景透明度");
        en_US.put("keys.opacity", "Opacity");

        // ── 方块描边 (Block Outline) ──
        zh_CN.put("blockOutline.enable", "方块描边");
        en_US.put("blockOutline.enable", "Block Outline");
        zh_CN.put("blockOutline.color", "描边颜色");
        en_US.put("blockOutline.color", "Outline Color");
        zh_CN.put("blockOutline.width", "描边宽度");
        en_US.put("blockOutline.width", "Outline Width");
        zh_CN.put("blockOutline.alpha", "透明度");
        en_US.put("blockOutline.alpha", "Alpha");
        zh_CN.put("blockOutline.visibleOnly", "仅可见面");
        en_US.put("blockOutline.visibleOnly", "Visible Faces Only");

        // ── 实体高亮 (Entity Highlight) ──
        zh_CN.put("entityHL.enable", "实体碰撞箱");
        en_US.put("entityHL.enable", "Entity Hitbox");
        zh_CN.put("entityHL.hostileColor", "敌对颜色");
        en_US.put("entityHL.hostileColor", "Hostile Color");
        zh_CN.put("entityHL.neutralColor", "中立颜色");
        en_US.put("entityHL.neutralColor", "Neutral Color");
        zh_CN.put("entityHL.friendlyColor", "友好颜色");
        en_US.put("entityHL.friendlyColor", "Friendly Color");
        zh_CN.put("entityHL.width", "边框宽度");
        en_US.put("entityHL.width", "Border Width");
        zh_CN.put("entityHL.alpha", "透明度");
        en_US.put("entityHL.alpha", "Alpha");

        // ── RGB 流光 ──
        zh_CN.put("rgb.enable", "RGB 流光");
        en_US.put("rgb.enable", "RGB Flow");
        zh_CN.put("rgb.speed", "循环速度");
        en_US.put("rgb.speed", "Cycle Speed");
        zh_CN.put("rgb.mode", "流动模式");
        en_US.put("rgb.mode", "Flow Mode");
        zh_CN.put("rgb.algorithm", "颜色算法");
        en_US.put("rgb.algorithm", "Color Algorithm");
        zh_CN.put("rgb.applyBlock", "应用方块描边");
        en_US.put("rgb.applyBlock", "Apply Block");
        zh_CN.put("rgb.applyEntity", "应用实体框");
        en_US.put("rgb.applyEntity", "Apply Entity");

        // ── 准星 (Crosshair) ──
        zh_CN.put("crosshair.style", "准星样式");
        en_US.put("crosshair.style", "Crosshair Style");
        zh_CN.put("crosshair.color", "准星颜色");
        en_US.put("crosshair.color", "Crosshair Color");
        zh_CN.put("crosshair.gap", "准星间隙");
        en_US.put("crosshair.gap", "Crosshair Gap");
        zh_CN.put("crosshair.thickness", "准星粗细");
        en_US.put("crosshair.thickness", "Thickness");
        zh_CN.put("crosshair.dot", "中心点");
        en_US.put("crosshair.dot", "Center Dot");
        zh_CN.put("crosshair.spread", "扩散效果");
        en_US.put("crosshair.spread", "Spread");
        zh_CN.put("crosshair.entityColor", "感应变色");
        en_US.put("crosshair.entityColor", "Entity Color");
        zh_CN.put("crosshair.playerColor", "玩家颜色");
        en_US.put("crosshair.playerColor", "Player Color");
        zh_CN.put("crosshair.hostileColor", "敌对颜色");
        en_US.put("crosshair.hostileColor", "Hostile Color");
        zh_CN.put("crosshair.passiveColor", "被动颜色");
        en_US.put("crosshair.passiveColor", "Passive Color");
        zh_CN.put("crosshair.neutralColor", "中立颜色");
        en_US.put("crosshair.neutralColor", "Neutral Color");
        zh_CN.put("crosshair.otherColor", "其他颜色");
        en_US.put("crosshair.otherColor", "Other Color");
        zh_CN.put("crosshair.range", "感应距离");
        en_US.put("crosshair.range", "Range");

        // ── 药水 (Potion) ──
        zh_CN.put("potion.show", "显示药水");
        en_US.put("potion.show", "Show Potion");
        zh_CN.put("potion.textColor", "文字颜色");
        en_US.put("potion.textColor", "Text Color");
        zh_CN.put("potion.xOffset", "X 偏移");
        en_US.put("potion.xOffset", "X Offset");
        zh_CN.put("potion.yOffset", "Y 偏移");
        en_US.put("potion.yOffset", "Y Offset");

        // ── 装备 (Equipment) ──
        zh_CN.put("equip.enableArmor", "护甲显示");
        en_US.put("equip.enableArmor", "Show Armor");
        zh_CN.put("equip.enableHeld", "手持物品");
        en_US.put("equip.enableHeld", "Held Item");
        zh_CN.put("equip.armorX", "护甲 X");
        en_US.put("equip.armorX", "Armor X");
        zh_CN.put("equip.armorY", "护甲 Y");
        en_US.put("equip.armorY", "Armor Y");
        zh_CN.put("equip.heldX", "物品 X");
        en_US.put("equip.heldX", "Item X");
        zh_CN.put("equip.heldY", "物品 Y");
        en_US.put("equip.heldY", "Item Y");
        zh_CN.put("equip.showEnchants", "附魔显示");
        en_US.put("equip.showEnchants", "Show Enchants");
        zh_CN.put("equip.showArmorBg", "护甲背景");
        en_US.put("equip.showArmorBg", "Armor BG");

        // ── 危机警戒 (Crisis) ──
        zh_CN.put("crisis.warnHealth", "血量警戒");
        en_US.put("crisis.warnHealth", "Health Warning");
        zh_CN.put("crisis.healthThreshold", "血量阈值");
        en_US.put("crisis.healthThreshold", "Health Threshold");
        zh_CN.put("crisis.warnHunger", "饥饿警戒");
        en_US.put("crisis.warnHunger", "Hunger Warning");
        zh_CN.put("crisis.hungerThreshold", "饥饿阈值");
        en_US.put("crisis.hungerThreshold", "Hunger Threshold");
        zh_CN.put("crisis.warnTnt", "TNT 警戒");
        en_US.put("crisis.warnTnt", "TNT Warning");
        zh_CN.put("crisis.tntRadius", "TNT 范围");
        en_US.put("crisis.tntRadius", "TNT Radius");
        zh_CN.put("crisis.xOffset", "X 偏移");
        en_US.put("crisis.xOffset", "X Offset");
        zh_CN.put("crisis.yOffset", "Y 偏移");
        en_US.put("crisis.yOffset", "Y Offset");
        zh_CN.put("crisis.iconSize", "图标大小");
        en_US.put("crisis.iconSize", "Icon Size");

        // ── 命中标识 (Hit Marker) ──
        zh_CN.put("hitmarker.useS19", "S19 网络检测");
        en_US.put("hitmarker.useS19", "S19 Detection");
        zh_CN.put("hitmarker.hitSound", "命中音效");
        en_US.put("hitmarker.hitSound", "Hit Sound");
        zh_CN.put("hitmarker.killSound", "击杀音效");
        en_US.put("hitmarker.killSound", "Kill Sound");
        zh_CN.put("hitmarker.volume", "音量");
        en_US.put("hitmarker.volume", "Volume");
        zh_CN.put("hitmarker.hitColor", "命中颜色");
        en_US.put("hitmarker.hitColor", "Hit Color");
        zh_CN.put("hitmarker.hitSize", "命中大小");
        en_US.put("hitmarker.hitSize", "Hit Size");
        zh_CN.put("hitmarker.hitAlpha", "命中透明度");
        en_US.put("hitmarker.hitAlpha", "Hit Alpha");
        zh_CN.put("hitmarker.killColor", "击杀颜色");
        en_US.put("hitmarker.killColor", "Kill Color");
        zh_CN.put("hitmarker.killSize", "击杀大小");
        en_US.put("hitmarker.killSize", "Kill Size");
        zh_CN.put("hitmarker.killAlpha", "击杀透明度");
        en_US.put("hitmarker.killAlpha", "Kill Alpha");
        zh_CN.put("hitmarker.border", "边框");
        en_US.put("hitmarker.border", "Border");
        zh_CN.put("hitmarker.borderWidth", "边框宽度");
        en_US.put("hitmarker.borderWidth", "Border Width");
        zh_CN.put("hitmarker.borderColor", "边框颜色");
        en_US.put("hitmarker.borderColor", "Border Color");
        zh_CN.put("hitmarker.blood", "血迹强度");
        en_US.put("hitmarker.blood", "Blood Intensity");
        zh_CN.put("hitmarker.randomRotate", "随机旋转");
        en_US.put("hitmarker.randomRotate", "Random Rotate");
        zh_CN.put("hitmarker.rotateStrength", "旋转强度");
        en_US.put("hitmarker.rotateStrength", "Rotate Strength");

        // ── 连击 (Combo) ──
        zh_CN.put("combo.xOffset", "X 偏移");
        en_US.put("combo.xOffset", "X Offset");
        zh_CN.put("combo.yOffset", "Y 偏移");
        en_US.put("combo.yOffset", "Y Offset");
        zh_CN.put("combo.scale", "缩放");
        en_US.put("combo.scale", "Scale");

        // ── 方块破坏进度 (Block Break) ──
        zh_CN.put("blockBreak.enable", "破坏进度条");
        en_US.put("blockBreak.enable", "Block Break Bar");
        zh_CN.put("blockBreak.x", "X 偏移");
        en_US.put("blockBreak.x", "X Offset");
        zh_CN.put("blockBreak.y", "Y 偏移");
        en_US.put("blockBreak.y", "Y Offset");
        zh_CN.put("blockBreak.width", "进度条宽度");
        en_US.put("blockBreak.width", "Bar Width");
        zh_CN.put("blockBreak.height", "进度条高度");
        en_US.put("blockBreak.height", "Bar Height");
        zh_CN.put("blockBreak.colorStart", "起始颜色");
        en_US.put("blockBreak.colorStart", "Start Color");
        zh_CN.put("blockBreak.colorEnd", "结束颜色");
        en_US.put("blockBreak.colorEnd", "End Color");
        zh_CN.put("blockBreak.showPercent", "显示百分比");
        en_US.put("blockBreak.showPercent", "Show Percent");

        // ── 药水计时器 (Potion Timer) ──
        zh_CN.put("potionTimer.xOffset", "X 偏移");
        en_US.put("potionTimer.xOffset", "X Offset");
        zh_CN.put("potionTimer.yOffset", "Y 偏移");
        en_US.put("potionTimer.yOffset", "Y Offset");
        zh_CN.put("potionTimer.iconSize", "图标大小");
        en_US.put("potionTimer.iconSize", "Icon Size");

        // ── 调试信息 (Debug) ──
        zh_CN.put("debug.xOffset", "X 偏移");
        en_US.put("debug.xOffset", "X Offset");
        zh_CN.put("debug.yOffset", "Y 偏移");
        en_US.put("debug.yOffset", "Y Offset");

        // ── 颜色补充 ──
        zh_CN.put("color.compass", "罗盘颜色");
        en_US.put("color.compass", "Compass Color");
        zh_CN.put("color.distance", "距离颜色");
        en_US.put("color.distance", "Distance Color");
        zh_CN.put("color.targetInfo", "目标颜色");
        en_US.put("color.targetInfo", "Target Color");
        zh_CN.put("color.blockInfo", "方块信息色");
        en_US.put("color.blockInfo", "Block Info Color");
        zh_CN.put("color.keysActive", "按键激活色");
        en_US.put("color.keysActive", "Key Active");
        zh_CN.put("color.keysInactive", "按键闲置色");
        en_US.put("color.keysInactive", "Key Inactive");
        zh_CN.put("color.keysText", "按键文字色");
        en_US.put("color.keysText", "Key Text");
        zh_CN.put("color.potionText", "药水文字色");
        en_US.put("color.potionText", "Potion Text");
        zh_CN.put("color.blockOutline", "描边颜色");
        en_US.put("color.blockOutline", "Block Outline");
        zh_CN.put("color.entityHostile", "敌对碰撞箱");
        en_US.put("color.entityHostile", "Hostile Hitbox");
        zh_CN.put("color.entityNeutral", "中立碰撞箱");
        en_US.put("color.entityNeutral", "Neutral Hitbox");
        zh_CN.put("color.entityFriendly", "友好碰撞箱");
        en_US.put("color.entityFriendly", "Friendly Hitbox");
        zh_CN.put("color.crosshair", "准星颜色");
        en_US.put("color.crosshair", "Crosshair");
        zh_CN.put("color.crosshairPlayer", "玩家准星色");
        en_US.put("color.crosshairPlayer", "Player Color");
        zh_CN.put("color.crosshairHostile", "敌对准星色");
        en_US.put("color.crosshairHostile", "Hostile Color");
        zh_CN.put("color.crosshairPassive", "被动准星色");
        en_US.put("color.crosshairPassive", "Passive Color");
        zh_CN.put("color.crosshairNeutral", "中立准星色");
        en_US.put("color.crosshairNeutral", "Neutral Color");
        zh_CN.put("color.crosshairOther", "其他准星色");
        en_US.put("color.crosshairOther", "Other Color");
        zh_CN.put("color.hitMarker", "命中标识色");
        en_US.put("color.hitMarker", "Hit Marker");
        zh_CN.put("color.killMarker", "击杀标识色");
        en_US.put("color.killMarker", "Kill Marker");
        zh_CN.put("color.breakStart", "破坏起始色");
        en_US.put("color.breakStart", "Break Start");
        zh_CN.put("color.breakEnd", "破坏结束色");
        en_US.put("color.breakEnd", "Break End");

        // ── 样式枚举值 ──
        zh_CN.put("style.detailed", "详细");
        en_US.put("style.detailed", "Detailed");
        zh_CN.put("style.minimal", "极简");
        en_US.put("style.minimal", "Minimal");
        zh_CN.put("style.simple", "简单");
        en_US.put("style.simple", "Simple");
        zh_CN.put("flow.perimeter", "逐棱流动");
        en_US.put("flow.perimeter", "Perimeter");
        zh_CN.put("flow.uniform", "整框同色");
        en_US.put("flow.uniform", "Uniform");
        zh_CN.put("algo.hsv", "HSV 色环");
        en_US.put("algo.hsv", "HSV");
        zh_CN.put("algo.sinewave", "正弦波");
        en_US.put("algo.sinewave", "Sine Wave");

        // ═══════════════════════════════════════════════════════════
        //  P2 新增模块：JumpBar / AirHUD / MountHP
        // ═══════════════════════════════════════════════════════════
        zh_CN.put("module.jumpbar", "跳跃蓄力条");
        en_US.put("module.jumpbar", "Jump Bar");
        zh_CN.put("module.airhud", "氧气条");
        en_US.put("module.airhud", "Air HUD");
        zh_CN.put("module.mountHP", "坐骑血量");
        en_US.put("module.mountHP", "Mount Health");
        zh_CN.put("module.jumpbar.tooltip", "按住空格时在屏幕下方显示跳跃蓄力条");
        en_US.put("module.jumpbar.tooltip", "Show jump charge bar when holding space");
        zh_CN.put("module.airhud.tooltip", "水下时显示自定义氧气条");
        en_US.put("module.airhud.tooltip", "Show custom air bar when underwater");
        zh_CN.put("module.mountHP.tooltip", "骑乘时显示坐骑血量条");
        en_US.put("module.mountHP.tooltip", "Show mount health bar when riding");

        // ═══════════════════════════════════════════════════════════
        //  P2 组A增强：准星动态 / 血条样式 / 盔甲耐久 / 药水预警
        // ═══════════════════════════════════════════════════════════
        zh_CN.put("crosshair.dynamicSpread", "动态扩散");
        en_US.put("crosshair.dynamicSpread", "Dynamic Spread");
        zh_CN.put("crosshair.dynamicSpread.tooltip", "移动/跳跃时准星根据速度动态扩散");
        en_US.put("crosshair.dynamicSpread.tooltip", "Crosshair spreads based on movement speed");
        zh_CN.put("health.barStyle", "血条样式");
        en_US.put("health.barStyle", "Health Bar Style");
        zh_CN.put("health.barStyle.tooltip", "血量条显示样式：默认/现代/像素");
        en_US.put("health.barStyle.tooltip", "Health bar style: Default/Modern/Pixel");
        zh_CN.put("armor.showDurability", "耐久显示");
        en_US.put("armor.showDurability", "Show Durability");
        zh_CN.put("armor.showDurability.tooltip", "在装备上显示耐久度数值");
        en_US.put("armor.showDurability.tooltip", "Show durability numbers on armor items");
        zh_CN.put("potion.warning", "药水预警");
        en_US.put("potion.warning", "Potion Warning");
        zh_CN.put("potion.warning.tooltip", "药水效果即将到期时闪烁警告");
        en_US.put("potion.warning.tooltip", "Flash warning when potion effect is about to expire");
        zh_CN.put("potion.warnThreshold", "预警阈值(秒)");
        en_US.put("potion.warnThreshold", "Warning Threshold (s)");
        zh_CN.put("potion.warnThreshold.tooltip", "药水剩余时间低于此值时触发闪烁预警");
        en_US.put("potion.warnThreshold.tooltip", "Potion warning flashes when remaining time is below this");

        // ═══════════════════════════════════════════════════════════
        //  Tooltip 悬浮说明
        // ═══════════════════════════════════════════════════════════
        zh_CN.put("compass.show.tooltip", "在 HUD 上显示罗盘方向指示器");
        en_US.put("compass.show.tooltip", "Show compass direction indicator on HUD");
        zh_CN.put("compass.style.tooltip", "罗盘显示样式：详细/极简/简单");
        en_US.put("compass.style.tooltip", "Compass display style: Detailed/Minimal/Simple");
        zh_CN.put("compass.scale.tooltip", "罗盘整体缩放比例");
        en_US.put("compass.scale.tooltip", "Overall compass scale factor");
        zh_CN.put("compass.degreeMarks.tooltip", "在罗盘上显示角度刻度标记");
        en_US.put("compass.degreeMarks.tooltip", "Show degree marks on the compass arc");
        zh_CN.put("compass.needle.tooltip", "显示指北针方向指示");
        en_US.put("compass.needle.tooltip", "Show north-pointing needle indicator");
        zh_CN.put("health.showArmor.tooltip", "在状态栏中显示护甲值和耐久度");
        en_US.put("health.showArmor.tooltip", "Show armor value and durability in health HUD");
        zh_CN.put("health.showHead.tooltip", "在状态栏旁边显示玩家头像");
        en_US.put("health.showHead.tooltip", "Show player head avatar next to health HUD");
        zh_CN.put("dist.precision.tooltip", "距离数值的小数位数（0=整数，5=最精确）");
        en_US.put("dist.precision.tooltip", "Decimal places for distance (0=whole, 5=most precise)");
        zh_CN.put("dist.showBlockInfo.tooltip", "显示当前看向的方块的坐标和名称");
        en_US.put("dist.showBlockInfo.tooltip", "Show coordinates and name of the targeted block");
        zh_CN.put("keys.scale.tooltip", "按键显示的文字缩放比例");
        en_US.put("keys.scale.tooltip", "Text scale for key display overlay");
        zh_CN.put("keys.background.tooltip", "在按键后面显示半透明背景框");
        en_US.put("keys.background.tooltip", "Show semi-transparent background behind keys");

        zh_CN.put("chroma.width.tooltip", "聊天框宽度（像素）");
        en_US.put("chroma.width.tooltip", "Chat box width in pixels");
        zh_CN.put("chroma.lines.tooltip", "可见聊天行数");
        en_US.put("chroma.lines.tooltip", "Number of visible chat lines");
        zh_CN.put("chroma.timestamp.tooltip", "在每条消息前显示 [HH:mm] 时间戳");
        en_US.put("chroma.timestamp.tooltip", "Show [HH:mm] timestamp before each message");
        zh_CN.put("chroma.avatar.tooltip", "在消息左侧显示发送者头像");
        en_US.put("chroma.avatar.tooltip", "Show sender head avatar next to each message");
        zh_CN.put("chroma.signboard.tooltip", "自动合并 Hypixel 告示牌消息为单条显示");
        en_US.put("chroma.signboard.tooltip", "Merge Hypixel signboard messages into single display");

        zh_CN.put("crosshair.style.tooltip", "准星绘制样式：默认/点/圆/十字/箭头");
        en_US.put("crosshair.style.tooltip", "Crosshair draw style: Default/Dot/Circle/Cross/Arrow");
        zh_CN.put("crosshair.entityColor.tooltip", "准星根据目标实体类型自动变色");
        en_US.put("crosshair.entityColor.tooltip", "Crosshair changes color based on targeted entity type");
        zh_CN.put("hitmarker.useS19.tooltip", "使用 S19 网络包检测多人命中，更准确");
        en_US.put("hitmarker.useS19.tooltip", "Use S19 packet for multiplayer hit detection");
        zh_CN.put("hitmarker.hitSound.tooltip", "命中时播放音效反馈");
        en_US.put("hitmarker.hitSound.tooltip", "Play sound effect on hit");
        zh_CN.put("hitmarker.border.tooltip", "在命中标识周围绘制边框");
        en_US.put("hitmarker.border.tooltip", "Draw a border around the hit marker");
        zh_CN.put("hitmarker.blood.tooltip", "命中时屏幕边缘血雾效果强度（0=关闭）");
        en_US.put("hitmarker.blood.tooltip", "Blood splatter intensity on hit (0=off)");
        zh_CN.put("blockOutline.enable.tooltip", "为看向的方块绘制彩色描边轮廓");
        en_US.put("blockOutline.enable.tooltip", "Draw colored outline around the targeted block");
        zh_CN.put("entityHL.enable.tooltip", "为附近实体绘制碰撞箱轮廓线");
        en_US.put("entityHL.enable.tooltip", "Draw hitbox outlines around nearby entities");
        zh_CN.put("rgb.enable.tooltip", "启用 RGB 动态流光效果（方块描边/实体框）");
        en_US.put("rgb.enable.tooltip", "Enable RGB color cycling for block outlines/entity hitboxes");
        zh_CN.put("rgb.mode.tooltip", "逐棱流动：颜色沿边缘流动；整框同色：整体变色");
        en_US.put("rgb.mode.tooltip", "Perimeter: color flows along edges; Uniform: whole box changes");
        zh_CN.put("blockBreak.enable.tooltip", "在准星下方显示方块破坏进度条");
        en_US.put("blockBreak.enable.tooltip", "Show block breaking progress bar below crosshair");

        zh_CN.put("crisis.warnHealth.tooltip", "血量过低时屏幕显示警戒图标");
        en_US.put("crisis.warnHealth.tooltip", "Show warning icon when health is low");
        zh_CN.put("crisis.healthThreshold.tooltip", "触发血量警戒的阈值（心数）");
        en_US.put("crisis.healthThreshold.tooltip", "Health threshold to trigger warning (in hearts)");
        zh_CN.put("crisis.warnTnt.tooltip", "附近有 TNT 实体时显示警戒");
        en_US.put("crisis.warnTnt.tooltip", "Show warning when TNT entity is nearby");
        zh_CN.put("module.expbar.tooltip", "显示自定义经验条（渐变蓝色）");
        en_US.put("module.expbar.tooltip", "Show custom experience bar (blue gradient)");
        zh_CN.put("module.bossHP.tooltip", "显示自定义 Boss 血条（红色）");
        en_US.put("module.bossHP.tooltip", "Show custom boss health bar (red)");
        zh_CN.put("module.hotbar.tooltip", "显示自定义快捷栏（9 槽）");
        en_US.put("module.hotbar.tooltip", "Show custom hotbar (9 slots)");
    }

    /**
     * 根据当前语言返回翻译文本。
     */
    public static String get(String key) {
        HashMap<String, String> map = isChinese ? zh_CN : en_US;
        String value = map.get(key);
        return value != null ? value : key;
    }

    /**
     * 切换语言（中 ↔ 英）。
     */
    public static void toggle() {
        isChinese = !isChinese;
    }

    /**
     * 当前是否为中文。
     */
    public static boolean isChinese() {
        return isChinese;
    }
}
