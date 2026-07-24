package com.yourname.betterplayerhud;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 取消 1.8.9 中原版无需模块替代、直接隐藏的渲染元素：
 * HELMET（南瓜罩） / PORTAL（传送门）
 *
 * 注意：EXPERIENCE / BOSSHEALTH / HOTBAR 由各自模块 Handler 条件取消。
 *       VIGNETTE / SUBTITLES 在 1.8.9 中无对应 ElementType，不可取消。
 */
public class VanillaCancelHandler {

    @SubscribeEvent
    public void onPreRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        ElementType type = event.type;
        if (type == ElementType.HELMET ||
            type == ElementType.PORTAL) {
            event.setCanceled(true);
        }
    }
}
