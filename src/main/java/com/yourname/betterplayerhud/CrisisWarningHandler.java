package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CrisisWarningHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final ItemStack ICON_TNT = new ItemStack(Blocks.tnt);
    private static final ItemStack ICON_BOW = new ItemStack(Items.bow);
    private static final ItemStack ICON_ARROW = new ItemStack(Items.arrow);

    /** 心形纹理在 Gui.icons 中的 UV（9x9 区域） */
    private static final int HEART_U = 52;
    private static final int HEART_V_NORMAL = 0;
    private static final int HEART_V_HARDCORE = 45;
    private static final int HALF_HEART_U = 61;

    /** 箭头检测消退计时器（tick），让警告持续一段时间 */
    private int arrowWarnCooldown = 0;

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.enableCriticalHealth) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int screenHeight = sr.getScaledHeight();

        // Y 位置：屏幕上半部分 + 偏移（向上偏移，更显眼）
        int topY = screenHeight / 2 - 120 + cfg.crisisYOffset;

        // 闪烁
        boolean flashOn = (mc.theWorld.getTotalWorldTime() % cfg.crisisFlashInterval) < (cfg.crisisFlashInterval / 2);
        if (!flashOn) return;

        int iconSize = cfg.crisisIconSize;
        int gap = iconSize / 3;
        if (gap < 4) gap = 4;

        // 收集激活的图标（0=heart, 1=hunger, 2=tnt, 3=bow, 4=arrow）
        java.util.List<Integer> types = new java.util.ArrayList<>();
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();

        // 低血量
        if (cfg.crisisWarnHealth && mc.thePlayer.getHealth() <= (float) cfg.crisisHealthThreshold) {
            types.add(0); stacks.add(null);
        }
        // 饥饿
        if (cfg.crisisWarnHunger && mc.thePlayer.getFoodStats().getFoodLevel() <= cfg.crisisHungerThreshold) {
            types.add(1); stacks.add(null);
        }
        // 附近即将引爆的爆炸物：TNT 或 苦力怕（点燃状态）
        if (cfg.crisisWarnTnt) {
            double radiusSq = cfg.crisisTntRadius * cfg.crisisTntRadius;
            boolean explosiveNear = false;
            double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
            for (Object o : mc.theWorld.loadedEntityList) {
                if (o instanceof EntityTNTPrimed) {
                    EntityTNTPrimed tnt = (EntityTNTPrimed) o;
                    double dx = tnt.posX - px, dy = tnt.posY - py, dz = tnt.posZ - pz;
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        explosiveNear = true; break;
                    }
                }
                if (o instanceof EntityCreeper) {
                    EntityCreeper creeper = (EntityCreeper) o;
                    if (creeper.getCreeperState() > 0) { // 点燃状态（即将爆炸）
                        double dx = creeper.posX - px, dy = creeper.posY - py, dz = creeper.posZ - pz;
                        if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                            explosiveNear = true; break;
                        }
                    }
                }
                // 飞行火球（烈焰弹/恶魂火球）→ 同属爆炸危险
                if (o instanceof EntityFireball) {
                    EntityFireball fb = (EntityFireball) o;
                    double dx = fb.posX - px, dy = fb.posY - py, dz = fb.posZ - pz;
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        explosiveNear = true; break;
                    }
                }
            }
            if (explosiveNear) { types.add(2); stacks.add(ICON_TNT); }
        }
        // 附近非己射出的飞行箭矢（带消退计时，持续约4秒）
        if (cfg.crisisWarnArrow) {
            double radiusSq = cfg.crisisArrowRadius * cfg.crisisArrowRadius;
            boolean arrowNear = false;
            double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
            for (Object o : mc.theWorld.loadedEntityList) {
                if (o instanceof EntityArrow) {
                    EntityArrow arrow = (EntityArrow) o;
                    // 排除自己射出的箭
                    if (arrow.shootingEntity == mc.thePlayer) continue;
                    double dx = arrow.posX - px, dy = arrow.posY - py, dz = arrow.posZ - pz;
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        arrowNear = true; break;
                    }
                }
            }
            if (arrowNear) {
                arrowWarnCooldown = 80; // persist 4 seconds (80 ticks)
                types.add(4); stacks.add(ICON_BOW);
            } else if (arrowWarnCooldown > 0) {
                arrowWarnCooldown--;
                types.add(4); stacks.add(ICON_BOW);
            }
        }

        if (types.isEmpty()) {
            // 编辑模式下显示 placeholder（固定最大宽度，保证拖拽不偏移）
            if (HUDEditManager.isEditing()) {
                int rw = crisisReportWidth(iconSize);
                int px = centerX - rw / 2 + cfg.crisisXOffset;
                HUDEditManager.report("危机警戒", px, topY, rw, iconSize);
            }
            return;
        }

        // 计算总宽度居中 + 偏移
        int totalW = types.size() * iconSize + (types.size() - 1) * gap;
        int startX = centerX - totalW / 2 + cfg.crisisXOffset;

        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        for (int i = 0; i < types.size(); i++) {
            int ix = startX + i * (iconSize + gap);
            int type = types.get(i);

            if (type == 0) {
                // 心形：从 Gui.icons 取 9x9 区域放大到 iconSize
                mc.getTextureManager().bindTexture(Gui.icons);
                boolean hardcore = mc.theWorld.getWorldInfo().isHardcoreModeEnabled();
                int v = hardcore ? HEART_V_HARDCORE : HEART_V_NORMAL;
                boolean isHalf = mc.thePlayer.getHealth() <= (float) cfg.crisisHealthThreshold / 2.0f;
                int u = isHalf ? HALF_HEART_U : HEART_U;
                Gui.drawScaledCustomSizeModalRect(ix, topY, (float) u, (float) v, 9, 9, iconSize, iconSize, 256.0f, 256.0f);
            } else if (type == 1) {
                // 饥饿 BUFF 图标（绿鸡腿）：从 inventory.png 取 potion 图标
                mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation("textures/gui/container/inventory.png"));
                Potion hunger = Potion.hunger;
                int idx = hunger.getStatusIconIndex();
                int pu = idx % 8 * 18;
                int pv = 198 + idx / 8 * 18;
                Gui.drawScaledCustomSizeModalRect(ix, topY, (float) pu, (float) pv, 18, 18, iconSize, iconSize, 256.0f, 256.0f);
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

        // 上报固定宽度矩形（编辑模式用，保证拖拽不偏移）
        if (HUDEditManager.isEditing()) {
            int rw = crisisReportWidth(iconSize);
            int rx = centerX - rw / 2 + cfg.crisisXOffset;
            HUDEditManager.report("危机警戒", rx, topY, rw, iconSize);
        }

        GlStateManager.disableBlend();
    }

    /** 计算编辑模式下的固定报告宽度（基于最多5个图标的占位） */
    private static int crisisReportWidth(int iconSize) {
        int gap = iconSize / 3;
        if (gap < 4) gap = 4;
        return 5 * iconSize + 4 * gap;
    }
}
