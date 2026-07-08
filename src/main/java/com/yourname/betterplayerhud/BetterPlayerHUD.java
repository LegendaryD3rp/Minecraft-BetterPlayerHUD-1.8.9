package com.yourname.betterplayerhud;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
        // 注册精简版 HUD 处理器
        MinecraftForge.EVENT_BUS.register(new PlayerHUDHandler());
        MinecraftForge.EVENT_BUS.register(new CompassHUDHandler());
        MinecraftForge.EVENT_BUS.register(new DistanceHUDHandler());
        MinecraftForge.EVENT_BUS.register(new BlockOutlineHandler());
        MinecraftForge.EVENT_BUS.register(new TargetHealthHandler());
        KeysDisplayHandler.register();

        // 注册配置 GUI 变更监听器（确保设置页面显示最新值）
        MinecraftForge.EVENT_BUS.register(new ConfigChangeHandler());

        // 注册服务器信息 HUD（TPS / Ping / 服务器IP）
        MinecraftForge.EVENT_BUS.register(new ServerInfoHUDHandler());

        // 注册自定义准星 HUD（模块20）
        MinecraftForge.EVENT_BUS.register(new CrosshairHandler());

        // 注册命中标识系统（模块21）— 由 HitMarkerMod 移植
        MinecraftForge.EVENT_BUS.register(new HitMarkerRendererBHUD());
        MinecraftForge.EVENT_BUS.register(new HitMarkerEventHandler());
        MinecraftForge.EVENT_BUS.register(new HitMarkerChatListener());
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
