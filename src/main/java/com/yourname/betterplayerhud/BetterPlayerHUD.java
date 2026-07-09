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
        HUDEditManager.register("罗盘", (x) -> config.xPosition = x, (y) -> config.yPosition = y);
        HUDEditManager.register("状态栏", (x) -> config.healthHudX = x, (y) -> config.healthHudY = y);
        HUDEditManager.register("距离信息", (x) -> config.distanceHudX = x, (y) -> config.distanceHudY = y);
        HUDEditManager.register("按键显示", (x) -> config.keysDisplayX = x, (y) -> config.keysDisplayY = y);
        HUDEditManager.register("目标血量", (x) -> config.targetHPOffsetX = x, (y) -> config.targetHPOffsetY = y);
        HUDEditManager.register("性能检测", (x) -> config.performanceHudX = x, (y) -> config.performanceHudY = y);
        HUDEditManager.register("药水效果", (x) -> config.potionXOffset = x, (y) -> config.potionYOffset = y);
        HUDEditManager.register("装甲栏", (x) -> config.armorXOffset = x, (y) -> config.armorYOffset = y);
        HUDEditManager.register("手持物品", (x) -> config.heldItemXOffset = x, (y) -> config.heldItemYOffset = y);

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
