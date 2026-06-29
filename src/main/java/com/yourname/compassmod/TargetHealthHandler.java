package com.yourname.compassmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 在经验值栏正上方显示瞄准实体的血量条 + 护甲条 + 名称。
 * 用 RenderGameOverlayEvent (GUI HUD) 做纯 2D 屏幕空间渲染。
 *
 * 布局（从下到上）：
 *   经验条 ← 原版
 *   3px 间距
 *   血条（5px）
 *   2px 间距
 *   护甲条（3px，天蓝色）
 *   2px 间距
 *   名称文字
 */
@SideOnly(Side.CLIENT)
public class TargetHealthHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // 天蓝色
    private static final int ARMOR_COLOR = packColor(135, 206, 235, 255);

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!BetterPlayerHUD.config.targetHPEnabled) return;

        // ── 取瞄准目标 ──
        Entity hit = mc.objectMouseOver == null ? null : mc.objectMouseOver.entityHit;
        if (!(hit instanceof EntityLivingBase)) return;
        EntityLivingBase target = (EntityLivingBase) hit;
        if (!shouldShow(target)) return;

        double dist = target.getDistanceToEntity(mc.getRenderViewEntity());
        if (dist > BetterPlayerHUD.config.targetHPMaxRange) return;

        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float absorb = target.getAbsorptionAmount();
        int armorValue = target.getTotalArmorValue();

        boolean showText = BetterPlayerHUD.config.targetHPStyle.equals("TEXT_ONLY")
                        || BetterPlayerHUD.config.targetHPStyle.equals("BAR_AND_TEXT");
        boolean showBar = !BetterPlayerHUD.config.targetHPStyle.equals("TEXT_ONLY");
        boolean showArmor = BetterPlayerHUD.config.targetHPShowArmor && armorValue > 0;

        // ── 经验条位置 ──
        ScaledResolution res = new ScaledResolution(mc);
        int centerX = res.getScaledWidth() / 2;

        int expBarTop = res.getScaledHeight() - 32 + 3;   // 原版经验条顶部
        int gapToExp = 3;                                   // 血条与经验条的间距

        int barWidth = BetterPlayerHUD.config.targetHPBarWidth;
        int barHeight = 5;
        int armorBarHeight = 3;
        int armorBarGap = 2;    // 护甲条与血条的间距
        int labelGap = 4;       // 条右侧到数值文字的距离
        int textGap = 2;        // 名称文字与护甲条的间距
        int bgPadding = 2;

        int offsetX = BetterPlayerHUD.config.targetHPOffsetX;
        int offsetY = BetterPlayerHUD.config.targetHPOffsetY;

        // ── 右侧数值文字 ──
        boolean showHpLabels = BetterPlayerHUD.config.targetHPShowLabels;
        boolean showArmorLabels = BetterPlayerHUD.config.targetHPShowArmorLabels;
        String hpLabel = null;
        String armorLabel = null;
        int hpLabelW = 0;
        int armorLabelW = 0;
        int maxLabelW = 0;
        if (showHpLabels) {
            hpLabel = String.format("%.0f/%.0f", health, maxHealth);
            if (absorb > 0) hpLabel += " +" + String.format("%.0f", absorb);
            armorLabel = armorValue + "/20";
            hpLabelW = mc.fontRendererObj.getStringWidth(hpLabel);
            armorLabelW = mc.fontRendererObj.getStringWidth(armorLabel);
            maxLabelW = Math.max(hpLabelW, (showArmor && showArmorLabels) ? armorLabelW : 0);
        }

        // ── 从下往上算 Y 坐标 ──
        int barY = expBarTop - gapToExp - barHeight + offsetY;         // 血条顶部
        int armorBarY = barY - armorBarGap - armorBarHeight;           // 护甲条顶部
        int txtY = (showArmor ? armorBarY : barY) - textGap - 10;      // 名称文字顶部

        int x = centerX + offsetX - barWidth / 2;

        // ── 玩家面部头像（血条左侧） ──
        boolean showFace = target instanceof AbstractClientPlayer
                && target != mc.thePlayer
                && BetterPlayerHUD.config.targetHPShowFace;
        int faceSize = BetterPlayerHUD.config.targetHPFaceSize;
        int facePad = 3;
        if (showFace) {
            // 血条整体右移，给头像腾出空间
            x += faceSize + facePad;
        }

        int bgRight = x + barWidth + (showHpLabels ? labelGap + maxLabelW : 0) + bgPadding;

        // ── 画背景 + 条 + 数值 ──
        if (showBar) {
            // 背景左边界（如有头像则向左扩展覆盖）
            int bgLeft = x - bgPadding;
            if (showFace) {
                bgLeft = x - faceSize - facePad - bgPadding;
            }
            int bgTop = (showArmor ? armorBarY : barY) - bgPadding;
            int bgH = barHeight + bgPadding * 2;
            if (showArmor) bgH += armorBarHeight + armorBarGap;

            int bgColor = packColor(
                    BetterPlayerHUD.config.targetHPBackColorR,
                    BetterPlayerHUD.config.targetHPBackColorG,
                    BetterPlayerHUD.config.targetHPBackColorB,
                    BetterPlayerHUD.config.targetHPBgAlpha);
            Gui.drawRect(bgLeft, bgTop, bgRight, bgTop + bgH, bgColor);

            // ── 血量条 ──
            float fillRatio = Math.min(health / maxHealth, 1.0F);
            int hpColor = packColor(
                    BetterPlayerHUD.config.targetHPColorR,
                    BetterPlayerHUD.config.targetHPColorG,
                    BetterPlayerHUD.config.targetHPColorB,
                    255);
            Gui.drawRect(x, barY,
                         x + (int)(barWidth * fillRatio), barY + barHeight,
                         hpColor);

            // ── 吸收心 ──
            if (absorb > 0) {
                float absorbRatio = Math.min(absorb / maxHealth, 1.0F);
                int absorbColor = packColor(255, 255, 128, 140);
                int startX = x + (int)(barWidth * fillRatio);
                Gui.drawRect(startX, barY,
                             startX + (int)(barWidth * absorbRatio), barY + barHeight,
                             absorbColor);
            }

            // ── 血量数值（条右侧） ──
            if (showHpLabels) {
                GlStateManager.enableTexture2D();
                mc.fontRendererObj.drawString(hpLabel,
                        x + barWidth + labelGap,
                        barY + (barHeight - 10) / 2,
                        packColor(255, 255, 255, 255), true);
            }

            // ── 护甲条（天蓝色，满护甲 20） + 数值 ──
            if (showArmor) {
                float armorRatio = Math.min(armorValue / 20.0F, 1.0F);
                Gui.drawRect(x, armorBarY,
                             x + (int)(barWidth * armorRatio), armorBarY + armorBarHeight,
                             ARMOR_COLOR);

                if (showArmorLabels) {
                    mc.fontRendererObj.drawString(armorLabel,
                            x + barWidth + labelGap,
                            armorBarY + (armorBarHeight - 10) / 2,
                            packColor(255, 255, 255, 255), true);
                }
            }

            // ── 面部头像（血条左侧，护甲条同款绘制方式） ──
            if (showFace) {
                AbstractClientPlayer playerTarget = (AbstractClientPlayer) target;
                ResourceLocation skin = playerTarget.getLocationSkin();
                mc.getTextureManager().bindTexture(skin);

                // 垂直居中于血条+护甲条区域
                int barsTop = (showArmor ? armorBarY : barY);
                int barsBot = barY + barHeight;
                int barsCenterY = (barsTop + barsBot) / 2;
                int faceY = barsCenterY - faceSize / 2;
                int faceX = x - faceSize - facePad;

                GlStateManager.enableTexture2D();
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                // 皮肤纹理中面部位于 (8,8) 处，大小 8×8 像素
                // 用 drawScaledCustomSizeModalRect 精确截取面部区域并缩放到 faceSize
                Gui.drawScaledCustomSizeModalRect(faceX, faceY, 8, 8, 8, 8, faceSize, faceSize, 64, 64);
            }
        }

        // ── 画文字 ──
        if (showText && BetterPlayerHUD.config.targetHPShowName) {
            String displayName = getDisplayName(target);
            String hpStr = String.format("%.0f/%.0f", health, maxHealth);
            if (absorb > 0) hpStr += " +" + String.format("%.0f", absorb);

            String display = displayName + " " + hpStr;
            int textColor = packColor(
                    BetterPlayerHUD.config.targetHPTextColorR,
                    BetterPlayerHUD.config.targetHPTextColorG,
                    BetterPlayerHUD.config.targetHPTextColorB,
                    255);
            int txtW = mc.fontRendererObj.getStringWidth(display);
            int txtX = centerX + offsetX - txtW / 2;

            // 文字背景
            Gui.drawRect(txtX - 2, txtY - 1,
                         txtX + txtW + 2, txtY + 9,
                         0x80000000);
            mc.fontRendererObj.drawString(display, txtX, txtY, textColor, true);
        }
    }

    private boolean shouldShow(EntityLivingBase entity) {
        boolean isPlayer = entity instanceof EntityPlayer;
        boolean isSelf = isPlayer && entity == mc.thePlayer;
        if (isSelf && !BetterPlayerHUD.config.targetHPShowSelf) return false;
        if (isPlayer && !isSelf && !BetterPlayerHUD.config.targetHPShowPlayers) return false;
        if (!isPlayer && !BetterPlayerHUD.config.targetHPShowMobs) return false;
        return true;
    }

    private String getDisplayName(EntityLivingBase entity) {
        if (!BetterPlayerHUD.config.targetHPShowName) return "";
        if (entity.hasCustomName()) return entity.getCustomNameTag();
        if (entity instanceof EntityPlayer) {
            return ((EntityPlayer) entity).getDisplayName().getUnformattedText();
        }
        String n = entity.getName();
        if (n != null) {
            n = n.replace('_', ' ');
            if (n.length() > 0)
                return Character.toUpperCase(n.charAt(0)) + n.substring(1);
        }
        return "";
    }

    private static int packColor(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
