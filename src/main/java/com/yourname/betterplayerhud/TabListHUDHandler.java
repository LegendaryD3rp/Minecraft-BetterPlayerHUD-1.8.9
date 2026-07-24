package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 模块：玩家列表 HUD
 *
 * 取消原版 PLAYER_LIST 渲染，在 Tab 键按住时显示自定义玩家列表：
 * - 按队伍分组（不同颜色前缀）
 * - 每行：ping 色块 + 玩家名
 * - 仅当 Tab 键按住时显示
 */
@SideOnly(Side.CLIENT)
public class TabListHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int COLOR_BG = 0xAA000000;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_PING_LOW = 0xFF00FF00;    // <100ms
    private static final int COLOR_PING_MED = 0xFFFFFF00;    // <300ms
    private static final int COLOR_PING_HIGH = 0xFFFF0000;   // >=300ms

    @SubscribeEvent
    public void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (event.type == ElementType.PLAYER_LIST) {
            if (BetterPlayerHUD.config.enableTabListHUD) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;
        if (mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.enableTabListHUD) return;
        // 仅在按住 Tab 时显示
        if (!mc.gameSettings.keyBindPlayerList.isKeyDown()) return;

        Collection<NetworkPlayerInfo> playerInfoMap = mc.thePlayer.sendQueue.getPlayerInfoMap();
        if (playerInfoMap == null || playerInfoMap.isEmpty()) return;

        // 收集并按名称排序
        List<PlayerEntry> players = new ArrayList<PlayerEntry>();
        for (NetworkPlayerInfo info : playerInfoMap) {
            String name = info.getGameProfile().getName();
            int ping = info.getResponseTime();
            String colorPrefix = "\u00A7f"; // 默认白色
            ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(name);
            if (team != null) {
                String prefix = team.getColorPrefix();
                if (prefix != null && !prefix.isEmpty()) {
                    colorPrefix = prefix;
                }
            }
            players.add(new PlayerEntry(name, ping, colorPrefix));
        }
        players.sort(Comparator.comparing(e -> e.name.toLowerCase()));

        ScaledResolution sr = event.resolution;
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // 计算面板尺寸
        int entryHeight = mc.fontRendererObj.FONT_HEIGHT + 2;
        int maxNameWidth = 0;
        for (PlayerEntry p : players) {
            int w = mc.fontRendererObj.getStringWidth(p.name);
            if (w > maxNameWidth) maxNameWidth = w;
        }
        int panelWidth = Math.max(maxNameWidth + 30, 120);
        int panelHeight = players.size() * entryHeight + 10;
        int px = sw / 2 - panelWidth / 2;
        int py = sh / 2 - panelHeight / 2;

        // 背景
        Gui.drawRect(px - 5, py - 5, px + panelWidth + 5, py + panelHeight + 5, COLOR_BG);

        // 列表
        int y = py;
        for (PlayerEntry p : players) {
            // ping 色块
            int pingColor;
            if (p.ping < 100) pingColor = COLOR_PING_LOW;
            else if (p.ping < 300) pingColor = COLOR_PING_MED;
            else pingColor = COLOR_PING_HIGH;

            Gui.drawRect(px, y + 2, px + 8, y + mc.fontRendererObj.FONT_HEIGHT, pingColor);
            // 玩家名（带队伍颜色前缀）
            mc.fontRendererObj.drawString(p.colorPrefix + p.name, px + 12, y, COLOR_TEXT, true);
            y += entryHeight;
        }
    }

    private static class PlayerEntry {
        final String name;
        final int ping;
        final String colorPrefix;
        PlayerEntry(String name, int ping, String colorPrefix) {
            this.name = name;
            this.ping = ping;
            this.colorPrefix = colorPrefix;
        }
    }
}
