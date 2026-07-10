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
        HUDEditManager.register("状态栏",    (x) -> config.healthHudX = x,     (y) -> config.healthHudY = y,       10, -50);
        HUDEditManager.register("距离信息",  (x) -> config.distanceHudX = x,   (y) -> config.distanceHudY = y,     490, 280);
        HUDEditManager.register("按键显示",  (x) -> config.keysDisplayX = x,   (y) -> config.keysDisplayY = y,     10, 150);
        HUDEditManager.register("性能检测",  (x) -> config.performanceHudX = x,(y) -> config.performanceHudY = y,  5, 65);

        // Offset 模块：编辑时的绝对坐标 → PosConverter 转成偏移值
        HUDEditManager.register("目标血量",
                (x) -> config.targetHPOffsetX = x, (y) -> config.targetHPOffsetY = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // barY = (sh-10-2-5) + offsetY = sh-17 + offsetY → offsetY = absY - sh + 17
                    // barX = sw/2 + offsetX - barWidth/2 → offsetX = absX - sw/2 + barWidth/2
                    int bw = config.targetHPBarWidth;
                    return new int[]{ absX - sw / 2 + bw / 2, absY - sh + 17 };
                });
        HUDEditManager.register("药水效果",
                (x) -> config.potionXOffset = x, (y) -> config.potionYOffset = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // x = 2 + offsetX → offsetX = absX - 2
                    // y = sh-20 + offsetY → offsetY = absY - sh + 20
                    return new int[]{ absX - 2, absY - sh + 20 };
                });
        HUDEditManager.register("装甲栏",
                (x) -> config.armorXOffset = x, (y) -> config.armorYOffset = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // hotbarLeft = sw/2 - 91 + offsetX → offsetX = absX - sw/2 + 91
                    // hotbarY = sh-22 + offsetY → offsetY = absY - sh + 22
                    return new int[]{ absX - sw / 2 + 91, absY - sh + 22 };
                });
        HUDEditManager.register("手持物品",
                (x) -> config.heldItemXOffset = x, (y) -> config.heldItemYOffset = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // x = 2 + offsetX → offsetX = absX - 2
                    // y = sh - fontHeight(9) - 2 + offsetY → offsetY = absY - sh + 11
                    return new int[]{ absX - 2, absY - sh + 11 };
                });
        // 危机警戒（偏移模式：居中坐标 + offset）
        HUDEditManager.register("危机警戒",
                (x) -> config.crisisXOffset = x, (y) -> config.crisisYOffset = y,
                0, 0,
                (absX, absY, sw, sh) -> {
                    // 居中坐标 = sw/2 - totalW/2 + offsetX
                    // 反向计算：offsetX = absX - sw/2 + totalW/2
                    // 但 totalW 取决于活跃类型数，非固定，这里简化为假定 2 个图标
                    int guessW = config.crisisIconSize * 2 + Math.max(config.crisisIconSize / 3, 4);
                    return new int[]{ absX - sw / 2 + guessW / 2, absY - sh / 2 + 60 };
                });

        // Ctrl+滚轮调大小（支持有scale/size参数的模块）
        HUDEditManager.setSize("罗盘", (d) -> {
            config.scale = Math.max(0.25f, Math.min(4.0f, config.scale + d * 0.05f));
        });
        HUDEditManager.setSize("按键显示", (d) -> {
            config.keysSize = Math.max(10, Math.min(80, config.keysSize + d * 2));
        });
        HUDEditManager.setSize("目标血量", (d) -> {
            config.targetHPBarWidth = Math.max(20, Math.min(200, config.targetHPBarWidth + d * 4));
        });
        HUDEditManager.setSize("状态栏", (d) -> {
            config.headSize = Math.max(8, Math.min(48, config.headSize + d * 2));
        });

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
