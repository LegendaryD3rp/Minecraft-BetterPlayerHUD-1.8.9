package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 危机警戒图标
 *
 * 屏幕正上方闪烁显示当前危机状态图标：
 *  - ❤  低血量  (≤2 HP)
 *  - 🍖 饥饿     (≤6 饱食度)
 *  - 💣 附近TNT  (半径10格内)
 *  - 🏹 拉弓     (正在用弓瞄准)
 *
 * 闪速：每 10 ticks 交替
 */
public class CrisisWarningHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final float HEALTH_THRESHOLD = 2.0f;
    private static final int HUNGER_THRESHOLD = 6;
    private static final double TNT_RADIUS_SQ = 100.0; // 10 blocks

    private static final ItemStack ICON_TNT = new ItemStack(Blocks.tnt);
    private static final ItemStack ICON_BOW = new ItemStack(Items.bow);
    private static final ItemStack ICON_HUNGER = new ItemStack(Items.rotten_flesh);

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!BetterPlayerHUD.config.enableCriticalHealth) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int topY = sr.getScaledHeight() / 2 - 60;

        // 闪烁节拍：每 20 ticks 一个周期，前 10 ticks 亮
        boolean flashOn = (mc.theWorld.getTotalWorldTime() % 20) < 10;
        if (!flashOn) return;

        List<ItemStack> icons = new ArrayList<>();
        List<Boolean> useGuiHeart = new ArrayList<>();

        // 1. 低血量
        if (mc.thePlayer.getHealth() <= HEALTH_THRESHOLD) {
            icons.add(null); // placeholder, 用 Gui.icons 画心
            useGuiHeart.add(true);
        }

        // 2. 饥饿
        if (mc.thePlayer.getFoodStats().getFoodLevel() <= HUNGER_THRESHOLD) {
            icons.add(ICON_HUNGER);
            useGuiHeart.add(false);
        }

        // 3. 附近TNT
        boolean tntNearby = false;
        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        for (Object o : mc.theWorld.loadedEntityList) {
            if (o instanceof EntityTNTPrimed) {
                EntityTNTPrimed tnt = (EntityTNTPrimed) o;
                double dx = tnt.posX - px, dy = tnt.posY - py, dz = tnt.posZ - pz;
                if (dx * dx + dy * dy + dz * dz <= TNT_RADIUS_SQ) {
                    tntNearby = true;
                    break;
                }
            }
        }
        if (tntNearby) {
            icons.add(ICON_TNT);
            useGuiHeart.add(false);
        }

        // 4. 拉弓
        if (mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
            icons.add(ICON_BOW);
            useGuiHeart.add(false);
        }

        if (icons.isEmpty()) return;

        // 渲染图标
        int iconSize = 18;
        int gap = 6;
        int totalW = icons.size() * iconSize + (icons.size() - 1) * gap;
        int startX = centerX - totalW / 2;

        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        for (int i = 0; i < icons.size(); i++) {
            int ix = startX + i * (iconSize + gap);

            if (useGuiHeart.get(i)) {
                // 从 Gui.icons 画心
                mc.getTextureManager().bindTexture(Gui.icons);
                boolean isHardcore = mc.theWorld.getWorldInfo().isHardcoreModeEnabled();
                int texV = isHardcore ? 45 : 0;
                if (mc.thePlayer.getHealth() <= 1.0f) {
                    Gui.drawModalRectWithCustomSizedTexture(ix, topY, 79, texV, 9, 9, 256, 256);
                } else {
                    Gui.drawModalRectWithCustomSizedTexture(ix, topY, 70, texV, 9, 9, 256, 256);
                }
            } else {
                // RenderItem 渲染物品图标
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(icons.get(i), ix, topY);
                RenderHelper.disableStandardItemLighting();
            }
        }

        GlStateManager.disableBlend();
    }
}
