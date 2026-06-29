package com.yourname.betterplayerhud;

import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ConfigChangeHandler {
    private final ForceSprintHandler forceSprintHandler;

    public ConfigChangeHandler(ForceSprintHandler sprintHandler) {
        this.forceSprintHandler = sprintHandler;
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(BetterPlayerHUD.MODID)) {
            // GUI 已更新 Properties → 读回 Java 字段 → 存盘
            BetterPlayerHUD.config.reloadFromConfig();
            BetterPlayerHUD.config.saveConfig();

            // 重新加载强制疾跑状态
            if (forceSprintHandler != null) {
                forceSprintHandler.reloadFromConfig();
                forceSprintHandler.forceStateUpdate();
            }
        }
    }
}
