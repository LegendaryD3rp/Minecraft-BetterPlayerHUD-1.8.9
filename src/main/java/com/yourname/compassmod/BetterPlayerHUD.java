package com.yourname.compassmod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import java.io.File;

@Mod(modid = BetterPlayerHUD.MODID, version = BetterPlayerHUD.VERSION, guiFactory = "com.yourname.compassmod.BetterPlayerHUDGuiFactory")
public class BetterPlayerHUD {
    public static final String MODID = "betterplayerhud";
    public static final String VERSION = "1.0-elite";
    // 将config改为public static以便访问
    public static BetterPlayerHUDConfig config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 初始化配置
        File configFile = new File(event.getModConfigurationDirectory(), "compassmod.cfg");
        config = new BetterPlayerHUDConfig(configFile);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(new PlayerHUDHandler());
        MinecraftForge.EVENT_BUS.register(new CompassHUDHandler());
        MinecraftForge.EVENT_BUS.register(new RealTimeHUDHandler()); // 新增时间显示处理器
        MinecraftForge.EVENT_BUS.register(new SpeedHUDHandler());
        MinecraftForge.EVENT_BUS.register(new GameTimeHUDHandler());
        MinecraftForge.EVENT_BUS.register(new CPSHUDHandler());
        MinecraftForge.EVENT_BUS.register(new DistanceHUDHandler());
        MinecraftForge.EVENT_BUS.register(new CoordinatesHUDHandler()); // 新增坐标显示处理器
        // EnhancedPingHUDHandler 构造函数内已创建 RealPingCalculator / TrafficBasedPingCalculator
        // 并自带 pipeline 注入，但仍需注册到 EVENT_BUS 以接收网络事件和渲染事件
        MinecraftForge.EVENT_BUS.register(new EnhancedPingHUDHandler());
        KeysDisplayHandler.register();

        ForceSprintHandler forceSprintHandler = new ForceSprintHandler();
        SprintStatusHandler sprintStatusHandler = new SprintStatusHandler();

        // 设置处理器间的依赖
        sprintStatusHandler.setForceSprintHandler(forceSprintHandler);

        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(forceSprintHandler);
        MinecraftForge.EVENT_BUS.register(sprintStatusHandler);

        // 注册配置变更监听器（仅一次）
        MinecraftForge.EVENT_BUS.register(new ConfigChangeHandler(forceSprintHandler));

        MinecraftForge.EVENT_BUS.register(new PotionStatusHUDHandler());




        // 注册简化强制潜行功能
        SimpleForceSneakHandler sneakHandler = new SimpleForceSneakHandler();
        SimpleSneakStatusHUD sneakStatusHUD = new SimpleSneakStatusHUD();

        // 设置处理器间的依赖
        sneakStatusHUD.setSneakHandler(sneakHandler);

        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(sneakHandler);
        MinecraftForge.EVENT_BUS.register(sneakStatusHUD);
        // 注册方块描边处理器
        MinecraftForge.EVENT_BUS.register(new BlockOutlineHandler());
        MinecraftForge.EVENT_BUS.register(new ItemInfoHUDHandler());
        MinecraftForge.EVENT_BUS.register(new LowHealthWarningHandler());
        MinecraftForge.EVENT_BUS.register(new TargetHealthHandler());
    }
}
