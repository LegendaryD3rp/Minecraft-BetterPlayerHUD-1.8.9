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

/**
 * 危机警戒图标 — 屏幕正上方闪烁显示危机状态
 *
 * 四种警戒：低血量 | 饥饿 | 附近TNT | 拉弓
 * 所有阈值/大小/开关均可配置
 */
public class CrisisWarningHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final ItemStack ICON_TNT = new ItemStack(Blocks.tnt);
    private static final ItemStack ICON_BOW = new ItemStack(Items.bow);
    private static final ItemStack ICON_HUNGER = new ItemStack(Items.rotten_flesh);

    /** 心形纹理在 Gui.icons 中的 UV */
    private static final int HEART_U = 70;
    private static final int HEART_V_NORMAL = 0;
    private static final int HEART_V_HARDCORE = 45;
    private static final int HALF_HEART_U = 79;

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.enableCriticalHealth) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;

        // Y 位置：屏幕正上方偏下一点，避免被文字遮住
        int topY = sr.getScaledHeight() / 2 - 60;

        // 闪烁
        boolean flashOn = (mc.theWorld.getTotalWorldTime() % cfg.crisisFlashInterval) < (cfg.crisisFlashInterval / 2);
        if (!flashOn) return;

        int iconSize = cfg.crisisIconSize;
        int gap = iconSize / 3;
        if (gap < 4) gap = 4;

        // 收集激活的图标
        java.util.List<Integer> types = new java.util.ArrayList<>(); // 0=heart, 1=hunger, 2=tnt, 3=bow
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();

        // 低血量
        if (cfg.crisisWarnHealth && mc.thePlayer.getHealth() <= (float) cfg.crisisHealthThreshold) {
            types.add(0); stacks.add(null);
        }
        // 饥饿
        if (cfg.crisisWarnHunger && mc.thePlayer.getFoodStats().getFoodLevel() <= cfg.crisisHungerThreshold) {
            types.add(1); stacks.add(ICON_HUNGER);
        }
        // 附近TNT
        if (cfg.crisisWarnTnt) {
            double radiusSq = cfg.crisisTntRadius * cfg.crisisTntRadius;
            boolean tntNear = false;
            double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
            for (Object o : mc.theWorld.loadedEntityList) {
                if (o instanceof EntityTNTPrimed) {
                    EntityTNTPrimed tnt = (EntityTNTPrimed) o;
                    double dx = tnt.posX - px, dy = tnt.posY - py, dz = tnt.posZ - pz;
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        tntNear = true; break;
                    }
                }
            }
            if (tntNear) { types.add(2); stacks.add(ICON_TNT); }
        }
        // 拉弓
        if (cfg.crisisWarnBow && mc.thePlayer.isUsingItem()
                && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
            types.add(3); stacks.add(ICON_BOW);
        }

        if (types.isEmpty()) return;

        // 计算总宽度居中
        int totalW = types.size() * iconSize + (types.size() - 1) * gap;
        int startX = centerX - totalW / 2;

        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        for (int i = 0; i < types.size(); i++) {
            int ix = startX + i * (iconSize + gap);
            int type = types.get(i);

            if (type == 0) {
                // 心形：从 Gui.icons 放大绘制
                mc.getTextureManager().bindTexture(Gui.icons);
                boolean hardcore = mc.theWorld.getWorldInfo().isHardcoreModeEnabled();
                int v = hardcore ? HEART_V_HARDCORE : HEART_V_NORMAL;
                boolean isHalf = mc.thePlayer.getHealth() <= (float) cfg.crisisHealthThreshold / 2.0f;
                int u = isHalf ? HALF_HEART_U : HEART_U;
                // Texture 源是 9x9，放大到 iconSize
                Gui.drawModalRectWithCustomSizedTexture(ix, topY, (float) u, (float) v, iconSize, iconSize, 256.0f, 256.0f);
            } else {
                // 物品图标：用 GlStateManager 缩放
                GlStateManager.pushMatrix();
                float scale = (float) iconSize / 16.0f;
                GlStateManager.translate(ix, topY, 0);
                GlStateManager.scale(scale, scale, 1.0f);
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(stacks.get(i), 0, 0);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.popMatrix();
            }
        }

        GlStateManager.disableBlend();
    }
}
