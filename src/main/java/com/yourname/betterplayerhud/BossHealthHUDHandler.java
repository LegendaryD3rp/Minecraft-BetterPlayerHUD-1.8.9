package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

/**
 * 模块：Boss 血条 HUD
 *
 * 取消原版 BOSSHEALTH 渲染，通过反射读取 GuiBossOverlay 内部
 * Map&lt;UUID, BossStatus&gt; 数据，在屏幕正上方自定义绘制。
 *
 * 每个 Boss 一条独立血条，垂直排列：
 *   [Boss 名称]
 *   [========== 红色血条 ==========]
 *
 * 1.8.9 的 GuiBossOverlay.BossStatus 包含：
 *   - name (IChatComponent)
 *   - health (float)
 *   - maxHealth (float)
 */
@SideOnly(Side.CLIENT)
public class BossHealthHUDHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── 反射缓存 ──
    private static Field bossInfoMapField = null;
    private static Field bossOverlayField = null;
    private static boolean reflectFailed = false;

    // ── 颜色常量 ──
    private static final int COLOR_BG     = 0x66000000;
    private static final int COLOR_BAR    = 0xFFFF3333;
    private static final int COLOR_TEXT   = 0xFFFFFFFF;

    // ── 布局常量 ──
    private static final int BAR_WIDTH  = 182;
    private static final int BAR_HEIGHT = 8;
    private static final int BAR_MARGIN = 14;

    // ================================================================
    //  Pre — 取消原版 Boss 血条
    // ================================================================

    @SubscribeEvent
    public void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (event.type == ElementType.BOSSHEALTH) {
            if (BetterPlayerHUD.config.enableBossHealthHUD) {
                event.setCanceled(true);
            }
        }
    }

    // ================================================================
    //  Post(TEXT) — 自定义 Boss 血条渲染
    // ================================================================

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.TEXT) return;
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (!BetterPlayerHUD.config.enableBossHealthHUD) return;

        Map<UUID, Object> bossMap = getBossInfoMap();
        if (bossMap == null || bossMap.isEmpty()) return;

        ScaledResolution sr = event.resolution;
        int sw = sr.getScaledWidth();

        int baseX = sw / 2 - BAR_WIDTH / 2;
        int baseY = 12;

        int index = 0;
        for (Map.Entry<UUID, Object> entry : bossMap.entrySet()) {
            Object raw = entry.getValue();
            if (raw == null) continue;

            // 反射读取 BossStatus 的三个字段
            Object nameObj = getFieldValue(raw, "name");
            Float health = getFloatField(raw, "health");
            Float maxHealth = getFloatField(raw, "maxHealth");

            if (health == null) continue;
            if (maxHealth == null || maxHealth <= 0) maxHealth = 1.0f;

            String name = (nameObj instanceof IChatComponent)
                    ? ((IChatComponent) nameObj).getFormattedText()
                    : "Boss";
            float ratio = Math.min(health / maxHealth, 1.0f);

            int y = baseY + index * (BAR_HEIGHT + BAR_MARGIN);

            // ── Boss 名称 ──
            int nameX = sw / 2 - mc.fontRendererObj.getStringWidth(name) / 2;
            mc.fontRendererObj.drawString(name, nameX + 1, y + 1, 0x80000000, false);
            mc.fontRendererObj.drawString(name, nameX, y, COLOR_TEXT, false);

            // ── 血条背景 + 进度 ──
            int barY = y + mc.fontRendererObj.FONT_HEIGHT + 2;
            Gui.drawRect(baseX, barY, baseX + BAR_WIDTH, barY + BAR_HEIGHT, COLOR_BG);
            int filledWidth = (int) (BAR_WIDTH * ratio);
            if (filledWidth > 0) {
                Gui.drawRect(baseX, barY, baseX + filledWidth, barY + BAR_HEIGHT, COLOR_BAR);
            }

            index++;
        }
    }

    // ================================================================
    //  反射读取 GuiBossOverlay 的 Map&lt;UUID, BossStatus&gt;
    // ================================================================

    @SuppressWarnings("unchecked")
    private Map<UUID, Object> getBossInfoMap() {
        if (reflectFailed) return null;

        try {
            if (bossOverlayField == null) {
                Field f = GuiIngame.class.getDeclaredField("field_73827_m");
                f.setAccessible(true);
                bossOverlayField = f;
            }

            if (bossInfoMapField == null) {
                Object bossOverlay = bossOverlayField.get(mc.ingameGUI);
                if (bossOverlay == null) return null;

                Class<?> clazz = bossOverlay.getClass();
                // Forge 1.8.9: field_73822_f 是 Map<UUID, BossStatus>
                for (String name : new String[]{"field_73822_f", "bossInfoMap", "map"}) {
                    try {
                        Field f = clazz.getDeclaredField(name);
                        f.setAccessible(true);
                        bossInfoMapField = f;
                        break;
                    } catch (NoSuchFieldException e) {
                        // continue
                    }
                }

                if (bossInfoMapField == null) {
                    // 兜底：找唯一 Map 类型的字段
                    for (Field f : clazz.getDeclaredFields()) {
                        if (Map.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            bossInfoMapField = f;
                            break;
                        }
                    }
                }

                if (bossInfoMapField == null) {
                    reflectFailed = true;
                    return null;
                }
            }

            Object bossOverlay = bossOverlayField.get(mc.ingameGUI);
            if (bossOverlay == null) return null;

            return (Map<UUID, Object>) bossInfoMapField.get(bossOverlay);

        } catch (Exception e) {
            reflectFailed = true;
            return null;
        }
    }

    /**
     * 反射读取对象中指定名称的 Object 类型字段。
     */
    private static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 反射读取对象中指定名称的 float 字段。
     */
    private static Float getFloatField(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.getFloat(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
