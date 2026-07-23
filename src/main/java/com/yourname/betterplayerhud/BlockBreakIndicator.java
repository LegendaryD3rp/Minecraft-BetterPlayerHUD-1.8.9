package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 模块26：方块破坏进度指示器
 *
 * 在十字准星下方显示当前挖掘方块的精确进度条（0~100%）。
 * 绿色→黄色→红色 渐变，可选百分比和剩余时间文字。
 * 仅生存模式、正在挖方块时显示。
 *
 * 速率计算跑在 ClientTick（tick 级），渲染只读值，避免帧/tick 不一致。
 */
@SideOnly(Side.CLIENT)
public class BlockBreakIndicator {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── 反射缓存 ──
    private static java.lang.reflect.Field curBlockDamageField;
    private static java.lang.reflect.Field isHittingBlockField;
    static {
        try {
            curBlockDamageField = ReflectionHelper.findField(
                    PlayerControllerMP.class, "curBlockDamageMP", "field_78770_f");
            isHittingBlockField = ReflectionHelper.findField(
                    PlayerControllerMP.class, "isHittingBlock", "field_78778_j");
        } catch (Exception e) {
            System.err.println("[BHUD] 无法反射方块破坏字段: " + e.getMessage());
        }
    }

    // ── Tick 级速率跟踪 ──
    private float tickDamage = 0.0f;           // 最近 tick 的 damage 值
    private long lastTickTime = 0;             // 最近 tick 的墙上时间（ms）
    private float tickRate = 0.0f;             // 每秒 damage 增量（ex: 0.3 = 30%/s）

    // ═══════════════════════════════════════════════════════════════
    //  反射读取
    // ═══════════════════════════════════════════════════════════════
    private static float getCurBlockDamage(PlayerControllerMP ctrl) {
        if (curBlockDamageField == null || ctrl == null) return 0.0f;
        try {
            return curBlockDamageField.getFloat(ctrl);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private static boolean isHittingBlock(PlayerControllerMP ctrl) {
        if (isHittingBlockField == null || ctrl == null) return false;
        try {
            return isHittingBlockField.getBoolean(ctrl);
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Tick 级：更新速率（20tps，与 curBlockDamageMP 更新同频）
    // ═══════════════════════════════════════════════════════════════
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null || !cfg.enableBlockBreakIndicator) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) return;

        float dmg = getCurBlockDamage(mc.playerController);
        long now = System.currentTimeMillis();

        // 不活跃（没在挖）
        if (dmg <= 0.0f) {
            tickRate = 0.0f;
            tickDamage = 0.0f;
            lastTickTime = 0;
            return;
        }

        // 首个 tick 或方块/工具切换（damage 下降）
        if (lastTickTime == 0 || dmg < tickDamage - 0.005f) {
            tickDamage = dmg;
            lastTickTime = now;
            tickRate = 0.0f;
            return;
        }

        long dtMs = now - lastTickTime;
        if (dtMs < 1) dtMs = 50; // 安全兜底

        float delta = dmg - tickDamage;
        if (delta > 0.0001f) {
            // 每秒增长速率 = delta / dtMs * 1000
            float instantRate = delta * 1000.0f / dtMs;
            float alpha = cfg.blockBreakIndicatorTimeSmoothing;
            // 低通滤波
            if (tickRate < 0.0001f) {
                tickRate = instantRate; // 首值无滤波
            } else {
                tickRate = tickRate * (1.0f - alpha) + instantRate * alpha;
            }
        }
        // delta <= 0：方块/工具换了但 dmg 没降（刚换更慢工具），保持旧速率

        tickDamage = dmg;
        lastTickTime = now;
    }

    // ═══════════════════════════════════════════════════════════════
    //  帧级：渲染（60fps，只读 tickRate）
    // ═══════════════════════════════════════════════════════════════
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (cfg == null || !cfg.enableBlockBreakIndicator) return;

        // ── 条件检测 ──
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.playerController == null) return;
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        if (!isHittingBlock(mc.playerController)) return;
        if (mc.playerController.isInCreativeMode()) return;

        float dmg = getCurBlockDamage(mc.playerController);
        if (dmg <= 0.0f && !mc.gameSettings.keyBindAttack.isKeyDown()) return;

        dmg = Math.max(0.0f, Math.min(1.0f, dmg));

        ScaledResolution res = new ScaledResolution(mc);
        int sw = res.getScaledWidth();
        int sh = res.getScaledHeight();

        // ── 位置计算（相对十字准星） ──
        int cx = sw / 2 + cfg.blockBreakIndicatorX;
        int cy = sh / 2 + cfg.blockBreakIndicatorY;

        int barW = cfg.blockBreakIndicatorWidth;
        int barH = cfg.blockBreakIndicatorHeight;

        // ── 颜色插值（绿→黄→红） ──
        int barColor = lerpColor(cfg.blockBreakIndicatorColorStart, cfg.blockBreakIndicatorColorEnd, dmg);

        // ── 绘制进度条 ──
        int bx = cx - barW / 2;
        int by = cy;

        drawRect(bx - 1, by - 1, bx + barW + 1, by + barH + 1, 0xBB000000);
        int fillW = Math.round(barW * dmg);
        if (fillW > 0) {
            drawRect(bx, by, bx + fillW, by + barH, barColor);
        }

        // ── 文字（百分比 + 剩余时间） ──
        StringBuilder sb = new StringBuilder();
        if (cfg.blockBreakIndicatorShowPercent) {
            sb.append(String.format("%.0f%%", dmg * 100.0f));
        }
        if (cfg.blockBreakIndicatorShowTime && tickRate > 0.0001f) {
            // 剩余秒数 = (1 - dmg) / tickRate，tickRate 已经是每秒增量
            float secLeft = (1.0f - dmg) / tickRate;
            if (sb.length() > 0) sb.append("  ");
            if (secLeft < 60.0f) {
                sb.append(String.format("(%.1fs)", secLeft));
            } else {
                sb.append(String.format("(%dm%ds)", (int)secLeft / 60, (int)secLeft % 60));
            }
        }

        if (sb.length() > 0) {
            String text = sb.toString();
            int tw = mc.fontRendererObj.getStringWidth(text);
            int tx = cx - tw / 2;
            int ty = by + barH + 2;
            mc.fontRendererObj.drawString(text, tx + 1, ty + 1, 0x40000000, false);
            mc.fontRendererObj.drawString(text, tx, ty, 0xFFCCCCCC, false);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════════════════════════
    private static int lerpColor(int c1, int c2, float t) {
        int r = lerp((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t);
        int g = lerp((c1 >> 8)  & 0xFF, (c2 >> 8)  & 0xFF, t);
        int b = lerp((c1)       & 0xFF, (c2)       & 0xFF, t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static void drawRect(int left, int top, int right, int bottom, int color) {
        if (left >= right || top >= bottom) return;
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8)  & 0xFF;
        int b = color & 0xFF;
        net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        net.minecraft.client.renderer.Tessellator tess = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left,  top,    0.0).color(r, g, b, a).endVertex();
        wr.pos(left,  bottom, 0.0).color(r, g, b, a).endVertex();
        wr.pos(right, bottom, 0.0).color(r, g, b, a).endVertex();
        wr.pos(right, top,    0.0).color(r, g, b, a).endVertex();
        tess.draw();
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
    }
}
