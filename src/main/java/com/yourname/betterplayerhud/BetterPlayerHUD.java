package com.yourname.betterplayerhud;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;

@Mod(modid = BetterPlayerHUD.MODID, version = BetterPlayerHUD.VERSION, guiFactory = "com.yourname.betterplayerhud.BetterPlayerHUDGuiFactory")
public class BetterPlayerHUD {
    public static final String MODID = "betterplayerhud";
    public static final String VERSION = "1.0-elite";
    public static BetterPlayerHUDConfig config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = new File(event.getModConfigurationDirectory(), "betterplayerhud.cfg");
        config = new BetterPlayerHUDConfig(configFile);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 注册拖拽编辑模式
        HUDEditManager.init();
        MinecraftForge.EVENT_BUS.register(HUDEditManager.INSTANCE);

        // 注册可拖拽模块的偏移设置器（编辑模式使用）
        // 绝对坐标模块：编辑时的绝对坐标 = config 值（负值 = 右/下对齐）
        HUDEditManager.register("罗盘",      (x) -> config.xPosition = x,       (y) -> config.yPosition = y,       0, 50);
        HUDEditManager.setDefaultSize("罗盘", 100, 20);
        HUDEditManager.register("状态栏",    (x) -> config.healthHudX = x,     (y) -> config.healthHudY = y,       10, -50);
        HUDEditManager.setDefaultSize("状态栏", 200, 30);
        HUDEditManager.register("距离信息",  (x) -> config.distanceHudX = x,   (y) -> config.distanceHudY = y,     490, 280);
        HUDEditManager.setDefaultSize("距离信息", 120, 12);
        HUDEditManager.register("按键显示",  (x) -> config.keysDisplayX = x,   (y) -> config.keysDisplayY = y,     10, 150);
        HUDEditManager.setDefaultSize("按键显示", 100, 50);
        HUDEditManager.register("性能检测",  (x) -> config.performanceHudX = x,(y) -> config.performanceHudY = y,  5, 65);
        HUDEditManager.setDefaultSize("性能检测", 130, 80);

        // Offset 模块：编辑时的绝对坐标 → PosConverter 转成偏移值
        HUDEditManager.register("目标血量",
                (x) -> config.targetHPOffsetX = x, (y) -> config.targetHPOffsetY = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // 渲染: x = sw/2 + offsetX - barWidth/2
                    // 报告: reportX = x - 30 → offsetX = absX - sw/2 + bw/2 + 30
                    // 渲染: barY = sh - 37 + offsetY
                    // 报告: reportY = barY - 25 → offsetY = absY - sh + 62
                    int bw = config.targetHPBarWidth;
                    return new int[]{ absX - sw / 2 + bw / 2 + 30, absY - sh + 62 };
                });
        HUDEditManager.setDefaultSize("目标血量", 140, 40);
        HUDEditManager.register("药水效果",
                (x) -> config.potionXOffset = x, (y) -> config.potionYOffset = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // x = 2 + offsetX → offsetX = absX - 2
                    // y = sh-20 + offsetY → offsetY = absY - sh + 20
                    return new int[]{ absX - 2, absY - sh + 20 };
                });
        HUDEditManager.setDefaultSize("药水效果", 160, 26);
        HUDEditManager.register("装甲栏",
                (x) -> config.armorXOffset = x, (y) -> config.armorYOffset = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // reportX = hotbarLeft - gapX - slotSize = (sw/2-91+offsetX)-4-20
                    // reportY = hotbarY - slotSize - gapY = (sh-22+offsetY)-20-2
                    // → offsetX = absX - sw/2 + 115    (91+4+20)
                    // → offsetY = absY - sh + 44       (22+20+2)
                    return new int[]{ absX - sw / 2 + 115, absY - sh + 44 };
                });
        HUDEditManager.setDefaultSize("装甲栏", 100, 50);
        HUDEditManager.register("手持物品",
                (x) -> config.heldItemXOffset = x, (y) -> config.heldItemYOffset = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // x = 2 + offsetX → offsetX = absX - 2
                    // y = sh - fontHeight(9) - 2 + offsetY → offsetY = absY - sh + 11
                    return new int[]{ absX - 2, absY - sh + 11 };
                });
        HUDEditManager.setDefaultSize("手持物品", 200, 30);

        // 物品栏横条左右侧数量统计
        HUDEditManager.register("物品数量",
                (x) -> config.itemCountX = x, (y) -> config.itemCountY = y, 0, 0,
                (absX, absY, sw, sh) -> {
                    // report: baseX = hotbarLeft + 3 + itemCountX = sw/2-91+3+itemCountX
                    // report: baseY = hotbarY - 10 + itemCountY = sh-22-10+itemCountY
                    //  → itemCountX = absX - sw/2 + 88
                    //  → itemCountY = absY - sh + 32
                    return new int[]{ absX - sw/2 + 88, absY - sh + 32 };
                });
        HUDEditManager.setDefaultSize("物品数量", 80, 10);

        // 危机警戒（偏移模式：居中坐标 + offset）
        HUDEditManager.register("危机警戒",
                (x) -> config.crisisXOffset = x, (y) -> config.crisisYOffset = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // 报告使用固定宽度 = 5*iconSize + 4*gap
                    int iconSize = config.crisisIconSize;
                    int gap = iconSize / 3;
                    if (gap < 4) gap = 4;
                    int rw = 5 * iconSize + 4 * gap;  // 必须与 CrisisWarningHandler.crisisReportWidth() 一致
                    return new int[]{ absX - sw / 2 + rw / 2, absY - sh / 2 + 60 };
                });
        HUDEditManager.setDefaultSize("危机警戒", 60, 24);

        // Ctrl+滚轮调大小（支持有scale/size参数的模块）
        HUDEditManager.setSize("罗盘", (d, r) -> {
            config.scale = Math.max(0.25f, Math.min(4.0f, config.scale + d * 0.05f));
            if (r != null) {
                int cx = r.x + r.width / 2;
                int cy = r.y + r.height / 2;
                int newW = Math.round(240 * config.scale);
                int newH = Math.round(30 * config.scale);
                r.setBounds(cx - newW / 2, cy - newH / 2, newW, newH);
            }
        });
        HUDEditManager.setSize("按键显示", (d, r) -> {
            config.keysSize = Math.max(10, Math.min(80, config.keysSize + d * 2));
            if (r != null) {
                int ksp = config.keysSpacing;
                r.setSize(3 * config.keysSize + 2 * ksp, 4 * config.keysSize + 3 * ksp);
            }
        });
        HUDEditManager.setSize("目标血量", (d, r) -> {
            config.targetHPBarWidth = Math.max(20, Math.min(200, config.targetHPBarWidth + d * 4));
        });
        HUDEditManager.setSize("状态栏", (d, r) -> {
            config.headSize = Math.max(8, Math.min(48, config.headSize + d * 2));
        });
        HUDEditManager.registerSizeReset("罗盘", () -> config.scale = 1.0f);
        HUDEditManager.registerSizeReset("按键显示", () -> config.keysSize = 24);
        HUDEditManager.registerSizeReset("目标血量", () -> config.targetHPBarWidth = 80);
        HUDEditManager.registerSizeReset("状态栏", () -> config.headSize = 16);
        HUDEditManager.setSize("危机警戒", (d, r) -> {
            int oldSize = config.crisisIconSize;
            config.crisisIconSize = Math.max(12, Math.min(48, config.crisisIconSize + d * 2));
            // 更新编辑框（固定宽度占位）
            if (r != null) {
                int ns = config.crisisIconSize;
                int gap = ns / 3;
                if (gap < 4) gap = 4;
                int rw = 5 * ns + 4 * gap;
                // 保持中心不变
                int cx = r.x + r.width / 2;
                r.setBounds(cx - rw / 2, r.y, rw, ns);
            }
        });
        HUDEditManager.registerSizeReset("危机警戒", () -> config.crisisIconSize = 24);

        // 注册精简版 HUD 处理器
        MinecraftForge.EVENT_BUS.register(new PlayerHUDHandler());
        MinecraftForge.EVENT_BUS.register(new CompassHUDHandler());
        MinecraftForge.EVENT_BUS.register(new DistanceHUDHandler());
        MinecraftForge.EVENT_BUS.register(new BlockOutlineHandler());
        MinecraftForge.EVENT_BUS.register(new TargetHealthHandler());
        KeysDisplayHandler.register();

        // 注册配置 GUI 变更监听器（确保设置页面显示最新值）
        MinecraftForge.EVENT_BUS.register(new ConfigChangeHandler());

        // 注册性能检测 HUD（FPS / 坐标 / TPS / Ping / 服务器IP）
        MinecraftForge.EVENT_BUS.register(new PerformanceHUDHandler());

        // 注册自定义准星 HUD（模块20）
        MinecraftForge.EVENT_BUS.register(new CrosshairHandler());

        // 准星实体感应距离 — 在 F7 模式下将鼠标移到屏幕中央，Ctrl+滚轮调节
        HUDEditManager.register("准星感应距离", (x) -> {}, (y) -> {}, 0, 0);
        HUDEditManager.setSize("准星感应距离", (d, r) -> {
            config.crosshairEntityRange = Math.max(3, Math.min(64, config.crosshairEntityRange + d * 2));
        });
        HUDEditManager.registerSizeReset("准星感应距离", () -> config.crosshairEntityRange = 10);

        // 注册命中标识系统（模块21）— 由 HitMarkerMod 移植
        MinecraftForge.EVENT_BUS.register(new HitMarkerRendererBHUD());
        MinecraftForge.EVENT_BUS.register(new HitMarkerEventHandler());
        MinecraftForge.EVENT_BUS.register(new HitMarkerChatListener());

        // 注册药水效果 HUD（模块22）
        MinecraftForge.EVENT_BUS.register(new PotionHUDHandler());

        // 注册装备&手持物品 HUD（模块23）
        MinecraftForge.EVENT_BUS.register(new EquipHUDHandler());

        // 注册危机警戒图标（模块24）
        MinecraftForge.EVENT_BUS.register(new CrisisWarningHandler());

        // 注册连击计数（模块25）
        HUDEditManager.register("连击计数",
                (x) -> config.comboXOffset = x, (y) -> config.comboYOffset = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // 渲染: x = sw - 50 + offsetX → offsetX = absX - sw + 50
                    //        y = sh - 40 + offsetY → offsetY = absY - sh + 40
                    return new int[]{ absX - sw + 50, absY - sh + 40 };
                });
        HUDEditManager.setDefaultSize("连击计数", 80, 12);
        MinecraftForge.EVENT_BUS.register(new ComboHandler());
    }

    /**
     * 配置变更监听器 — 当用户在 GUI 点击"完成"后重新加载配置
     */
    public static class ConfigChangeHandler {
        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.modID.equals(MODID)) {
                config.reloadFromMemory();
            }
        }
    }
}
