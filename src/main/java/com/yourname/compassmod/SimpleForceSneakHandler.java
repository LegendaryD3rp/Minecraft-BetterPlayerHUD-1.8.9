package com.yourname.compassmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class SimpleForceSneakHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private KeyBinding toggleKeyBinding;
    private boolean isForceSneaking = false;
    private boolean wasTogglePressed = false;
    private boolean wasUserSneaking = false;

    public SimpleForceSneakHandler() {
        // 注册切换快捷键（默认N键）
        toggleKeyBinding = new KeyBinding("切换强制潜行", Keyboard.KEY_N, "Compass Mod");
        ClientRegistry.registerKeyBinding(toggleKeyBinding);

        // 从配置加载初始状态
        isForceSneaking = BetterPlayerHUD.config.forceSneakEnabled;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!BetterPlayerHUD.config.enableForceSneak) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // 处理切换按键
        handleToggleKey();

        // 检测用户手动取消
        checkUserCancellation();

        // 应用强制潜行逻辑
        applyForceSneak();

        // 更新状态
        updateStates();
    }

    private void handleToggleKey() {
        boolean isTogglePressed = toggleKeyBinding.isPressed();

        // 检测按键按下事件
        if (isTogglePressed && !wasTogglePressed) {
            boolean previousState = isForceSneaking;

            // 切换强制潜行状态
            isForceSneaking = !isForceSneaking;
            BetterPlayerHUD.config.forceSneakEnabled = isForceSneaking;

            // 如果是从开启状态切换到关闭状态，释放潜行键
            if (previousState && !isForceSneaking) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }

            saveConfig();

            // 播放音效反馈
            if (BetterPlayerHUD.config.sneakPlaySound) {
                mc.thePlayer.playSound("random.click", 0.3F, isForceSneaking ? 1.0F : 0.8F);
            }


        }

        wasTogglePressed = isTogglePressed;
    }

    private void checkUserCancellation() {
        boolean isUserPressingSneak = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());

        // 当且仅当玩家正处在强制潜行状态下，按Shift会立即取消强制潜行
        if (isUserPressingSneak && !wasUserSneaking && isForceSneaking) {
            isForceSneaking = false;
            BetterPlayerHUD.config.forceSneakEnabled = false;

            // 释放潜行键
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);

            saveConfig();

         //   if (BetterPlayerHUD.config.showDebugInfo) {
          //      System.out.println("[ForceSneak] 用户手动取消");
         //   }
        }

        wasUserSneaking = isUserPressingSneak;
    }

    private void applyForceSneak() {
        if (isForceSneaking) {
            // 强制潜行开启时，按住潜行键
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        }
        // 强制潜行关闭时，不干预潜行键状态，让Shift键正常工作
    }

    private void updateStates() {
        // 同步配置状态
        BetterPlayerHUD.config.forceSneakEnabled = isForceSneaking;
    }

    private void saveConfig() {
        try {
            BetterPlayerHUD.config.config.get(Configuration.CATEGORY_CLIENT, "forceSneakEnabled", false)
                    .set(isForceSneaking);
            if (BetterPlayerHUD.config.config.hasChanged()) {
                BetterPlayerHUD.config.config.save();
            }
        } catch (Exception e) {
            System.err.println("[ForceSneak] 保存配置失败: " + e.getMessage());
        }
    }

    public boolean isForceSneaking() {
        return isForceSneaking;
    }
}