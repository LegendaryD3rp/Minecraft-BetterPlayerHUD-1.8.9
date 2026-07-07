package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块21：命中标识 (Hit Marker)
 *
 * 整合原 HitMarkerMod 全部功能，作为 BHUD 单一模块：
 * - 击中/击杀标识渲染
 * - 击中/击杀音效
 * - 碰撞箱边框
 * - 抛射物追踪（箭/雪球/蛋/药水/钓竿）
 * - 击杀聊天解析（中英文）
 */
@SideOnly(Side.CLIENT)
public class HitMarkerHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();

    // ═══════════════════════════════════════════════════════════════
    //  标识渲染状态
    // ═══════════════════════════════════════════════════════════════

    private static long hitMarkerTime = 0;
    private static long killMarkerTime = 0;
    private static final long HIT_DURATION = 300;
    private static final long KILL_DURATION = 500;

    // ═══════════════════════════════════════════════════════════════
    //  近战命中保护（防连发）
    // ═══════════════════════════════════════════════════════════════

    private final ConcurrentHashMap<Integer, Long> entityHitTimestamps = new ConcurrentHashMap<>();
    private static final long INVINCIBLE_FRAME = 500;

    // ═══════════════════════════════════════════════════════════════
    //  抛射物追踪
    // ═══════════════════════════════════════════════════════════════

    private static class TrackedProjectile {
        final int entityId;
        double lastX, lastY, lastZ;
        long createdAt;
        final boolean isZeroDamage;
        TrackedProjectile(Entity e) {
            this.entityId = e.getEntityId();
            this.lastX = e.posX; this.lastY = e.posY; this.lastZ = e.posZ;
            this.createdAt = System.currentTimeMillis();
            this.isZeroDamage = e instanceof EntityThrowable && !(e instanceof EntityArrow);
        }
    }
    private final ConcurrentHashMap<Integer, TrackedProjectile> trackedProjectiles = new ConcurrentHashMap<>();
    private static final long PROJ_TIMEOUT = 10000;

    private static class HealthWatch {
        float lastHealth;
        long flaggedAt;
        boolean hitTriggered;
        HealthWatch(float health) { this.lastHealth = health; this.flaggedAt = System.currentTimeMillis(); }
    }
    private final ConcurrentHashMap<Integer, HealthWatch> healthWatches = new ConcurrentHashMap<>();
    private static final long HEALTH_WATCH_TIMEOUT = 3000;

    // ═══════════════════════════════════════════════════════════════
    //  API — 供 ChatListener 调用
    // ═══════════════════════════════════════════════════════════════

    public static void showHit() {
        hitMarkerTime = System.currentTimeMillis();
    }

    public static void showKill() {
        killMarkerTime = System.currentTimeMillis();
    }

    public static void playHitSound() {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.hitMarkerEnableHitSounds) return;
        String[] sounds = {"hitmarkermod:hit1", "hitmarkermod:hit2", "hitmarkermod:hit3"};
        String sound = sounds[random.nextInt(sounds.length)];
        mc.thePlayer.playSound(sound, cfg.hitMarkerSoundVolume, 1.0F);
    }

    public static void playKillSound() {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.hitMarkerEnableKillSound) return;
        mc.thePlayer.playSound("hitmarkermod:kill", cfg.hitMarkerSoundVolume, 1.0F);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Render
    // ═══════════════════════════════════════════════════════════════

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS
                && event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!BetterPlayerHUD.config.enableHitMarker) return;
        if (mc.thePlayer == null) return;

        long now = System.currentTimeMillis();
        long killElapsed = now - killMarkerTime;
        boolean showingKill = killElapsed < KILL_DURATION;
        long hitElapsed = now - hitMarkerTime;
        boolean showingHit = hitElapsed < HIT_DURATION && !showingKill;

        if (!showingKill && !showingHit) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int cx = sr.getScaledWidth() / 2;
        int cy = sr.getScaledHeight() / 2;

        if (showingKill) {
            renderMarker(cx, cy, killElapsed, KILL_DURATION, true);
        } else {
            renderMarker(cx, cy, hitElapsed, HIT_DURATION, false);
        }
    }

    private void renderMarker(int cx, int cy, long elapsed, long duration, boolean isKill) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        float progress = Math.min(1.0f, (float) elapsed / duration);
        float scale = 1.0f + progress * 0.5f;
        float alpha = 1.0f - progress;

        float size = isKill ? cfg.hitMarkerKillSize : cfg.hitMarkerHitSize;
        int color = isKill ? cfg.hitMarkerKillColor : cfg.hitMarkerHitColor;
        float colorAlpha = (isKill ? cfg.hitMarkerKillAlpha : cfg.hitMarkerHitAlpha) * alpha;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.translate(cx, cy, 0);
        GlStateManager.scale(scale, scale, scale);

        // 边框
        if (cfg.hitMarkerEnableBorder) {
            int borderColor = isKill ? cfg.hitMarkerKillBorderColor : cfg.hitMarkerBorderColor;
            float ba = ((borderColor >> 24) & 0xFF) / 255f * alpha;
            GlStateManager.color(
                    ((borderColor >> 16) & 0xFF) / 255f,
                    ((borderColor >> 8) & 0xFF) / 255f,
                    (borderColor & 0xFF) / 255f, ba);
            GL11.glLineWidth(cfg.hitMarkerBorderWidth * 2 + 2);
            drawMarkerLines(size + 2);
        }

        // 主色
        float cr = ((color >> 16) & 0xFF) / 255f;
        float cg = ((color >> 8) & 0xFF) / 255f;
        float cb = (color & 0xFF) / 255f;
        GlStateManager.color(cr, cg, cb, colorAlpha);
        GL11.glLineWidth(2.0f);
        drawMarkerLines(size);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawMarkerLines(float size) {
        // 四角线段 ─ 左上、右上、左下、右下
        float gap = size * 0.3f;
        float len = size;

        GL11.glBegin(GL11.GL_LINES);
        // 左上
        GL11.glVertex2f(-gap, -gap - len);
        GL11.glVertex2f(-gap, -gap);
        GL11.glVertex2f(-gap - len, -gap);
        GL11.glVertex2f(-gap, -gap);
        // 右上
        GL11.glVertex2f(gap, -gap - len);
        GL11.glVertex2f(gap, -gap);
        GL11.glVertex2f(gap + len, -gap);
        GL11.glVertex2f(gap, -gap);
        // 左下
        GL11.glVertex2f(-gap, gap + len);
        GL11.glVertex2f(-gap, gap);
        GL11.glVertex2f(-gap - len, gap);
        GL11.glVertex2f(-gap, gap);
        // 右下
        GL11.glVertex2f(gap, gap + len);
        GL11.glVertex2f(gap, gap);
        GL11.glVertex2f(gap + len, gap);
        GL11.glVertex2f(gap, gap);
        GL11.glEnd();
    }

    // ═══════════════════════════════════════════════════════════════
    //  命中检测
    // ═══════════════════════════════════════════════════════════════

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!BetterPlayerHUD.config.enableHitMarker) return;

        Entity target = event.target;
        if (target == null) return;

        int id = target.getEntityId();
        long now = System.currentTimeMillis();
        Long lastHit = entityHitTimestamps.get(id);
        if (lastHit != null && now - lastHit < INVINCIBLE_FRAME) return;
        entityHitTimestamps.put(id, now);

        hitMarkerTime = now;
        playHitSound();
        spawnHitBlood(target);
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (!BetterPlayerHUD.config.enableHitMarker) return;

        Entity source = event.source.getEntity();
        if (source != mc.thePlayer) return;

        killMarkerTime = System.currentTimeMillis();
        playKillSound();
        spawnKillBlood(event.entity);
    }

    // ═══════════════════════════════════════════════════════════════
    //  抛射物追踪
    // ═══════════════════════════════════════════════════════════════

    @SubscribeEvent
    public void onEntitySpawn(EntityJoinWorldEvent event) {
        if (!BetterPlayerHUD.config.enableHitMarker) return;
        if (!BetterPlayerHUD.config.hitMarkerEnableProjectileTracking) return;

        Entity e = event.entity;
        if (e instanceof EntityArrow && ((EntityArrow) e).shootingEntity == mc.thePlayer) {
            trackedProjectiles.put(e.getEntityId(), new TrackedProjectile(e));
        } else if (e instanceof EntityThrowable) {
            EntityThrowable th = (EntityThrowable) e;
            if (th.getThrower() == mc.thePlayer) {
                trackedProjectiles.put(e.getEntityId(), new TrackedProjectile(e));
            }
        } else if (e instanceof EntityFishHook && ((EntityFishHook) e).angler == mc.thePlayer) {
            trackedProjectiles.put(e.getEntityId(), new TrackedProjectile(e));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean enabled = BetterPlayerHUD.config.enableHitMarker
                && BetterPlayerHUD.config.hitMarkerEnableProjectileTracking;
        long now = System.currentTimeMillis();

        if (!enabled) {
            if (!trackedProjectiles.isEmpty()) trackedProjectiles.clear();
            return;
        }

        // 清理超时抛射物
        trackedProjectiles.values().removeIf(p -> now - p.createdAt > PROJ_TIMEOUT);

        // 更新抛射物位置 & 检测消失
        trackedProjectiles.forEach((id, proj) -> {
            Entity e = mc.theWorld.getEntityByID(id);
            if (e == null || e.isDead) {
                // 抛射物消失：在最后位置附近找实体
                trackedProjectiles.remove(id);
                findAndFlagEntity(proj.lastX, proj.lastY, proj.lastZ);
            } else {
                proj.lastX = e.posX; proj.lastY = e.posY; proj.lastZ = e.posZ;
            }
        });

        // 钓竿甩钩回缩检测
        if (mc.thePlayer != null && mc.thePlayer.fishEntity != null) {
            EntityFishHook hook = mc.thePlayer.fishEntity;
            if (hook.caughtEntity != null) {
                findAndFlagEntity(hook.posX, hook.posY, hook.posZ);
            }
        }

        // 血量轮询：只检查被标记的实体
        healthWatches.forEach((id, watch) -> {
            if (now - watch.flaggedAt > HEALTH_WATCH_TIMEOUT) {
                healthWatches.remove(id);
                return;
            }
            Entity e = mc.theWorld.getEntityByID(id);
            if (e instanceof EntityLivingBase) {
                float hp = ((EntityLivingBase) e).getHealth();
                if (watch.lastHealth - hp > 0.1f && !watch.hitTriggered) {
                    watch.hitTriggered = true;
                    hitMarkerTime = now;
                    playHitSound();
                }
                watch.lastHealth = hp;
            }
        });
    }

    private void findAndFlagEntity(double x, double y, double z) {
        if (mc.theWorld == null) return;
        for (Entity e : mc.theWorld.loadedEntityList) {
            if (e == mc.thePlayer) continue;
            double dx = e.posX - x, dy = e.posY - y, dz = e.posZ - z;
            if (dx * dx + dy * dy + dz * dz < 9.0 && e instanceof EntityLivingBase) {
                float hp = ((EntityLivingBase) e).getHealth();
                if (!healthWatches.containsKey(e.getEntityId())) {
                    healthWatches.put(e.getEntityId(), new HealthWatch(hp));
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  血迹粒子效果
    // ═══════════════════════════════════════════════════════════════

    private void spawnHitBlood(Entity target) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.hitMarkerEnableHitBlood) return;
        if (mc.theWorld == null) return;
        for (int i = 0; i < (int)(cfg.hitMarkerHitBloodIntensity * 10); i++) {
            mc.theWorld.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.REDSTONE,
                    target.posX + (random.nextDouble() - 0.5) * target.width,
                    target.posY + random.nextDouble() * target.height,
                    target.posZ + (random.nextDouble() - 0.5) * target.width,
                    0, 0, 0);
        }
    }

    private void spawnKillBlood(Entity target) {
        BetterPlayerHUDConfig cfg = BetterPlayerHUD.config;
        if (!cfg.hitMarkerEnableKillBlood) return;
        if (mc.theWorld == null) return;
        for (int i = 0; i < (int)(cfg.hitMarkerKillBloodIntensity * 20); i++) {
            mc.theWorld.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.REDSTONE,
                    target.posX + (random.nextDouble() - 0.5) * target.width,
                    target.posY + random.nextDouble() * target.height,
                    target.posZ + (random.nextDouble() - 0.5) * target.width,
                    (random.nextDouble() - 0.5) * 0.5,
                    random.nextDouble() * 0.5,
                    (random.nextDouble() - 0.5) * 0.5);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  击杀聊天解析
    // ═══════════════════════════════════════════════════════════════

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (!BetterPlayerHUD.config.enableHitMarker) return;
        if (!BetterPlayerHUD.config.hitMarkerEnableChatKill) return;

        String raw = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        String clientName = mc.thePlayer.getName();
        if (clientName == null || clientName.isEmpty()) return;

        String lower = raw.toLowerCase();
        if (raw.contains("最终击杀") || lower.contains("final kill")) {
            processFinalKill(raw, clientName);
            return;
        }

        // 中文：A被B
        int bei = raw.indexOf("被");
        if (bei != -1) {
            String a = extractName(raw.substring(0, bei), false);
            String b = extractName(raw.substring(bei + 1), true);
            if (b != null && b.equals(clientName) && a != null && !a.equals(clientName)) {
                killMarkerTime = System.currentTimeMillis();
                playKillSound();
                return;
            }
        }

        // 英文：A by B
        int by = lower.indexOf(" by ");
        if (by != -1) {
            String a = extractName(raw.substring(0, by), false);
            String b = extractName(raw.substring(by + 4), true);
            if (b != null && b.equals(clientName) && a != null && !a.equals(clientName)) {
                killMarkerTime = System.currentTimeMillis();
                playKillSound();
                return;
            }
        }

        // 中文弓箭命中：A还剩X生命值！
        int remain = raw.indexOf("还剩");
        int life = raw.indexOf("生命值");
        if (remain != -1 && life != -1 && remain < life) {
            String a = extractName(raw.substring(0, remain), false);
            String hpStr = raw.substring(remain + 2, life).trim();
            if (a != null && !a.equals(clientName) && isValidHp(hpStr)) {
                hitMarkerTime = System.currentTimeMillis();
                playHitSound();
                return;
            }
        }

        // 英文弓箭命中：A is on X HP!
        int isOn = lower.indexOf("is on");
        int hpIdx = lower.indexOf(" hp");
        if (isOn != -1 && hpIdx != -1 && isOn < hpIdx) {
            String a = extractName(raw.substring(0, isOn), false);
            String hpStr = raw.substring(isOn + 5, hpIdx).trim();
            if (a != null && !a.equals(clientName) && isValidHp(hpStr)) {
                hitMarkerTime = System.currentTimeMillis();
                playHitSound();
            }
        }
    }

    private void processFinalKill(String msg, String clientName) {
        String a = extractName(msg, true);
        if (a == null || a.equals(clientName)) return;
        if (msg.contains(clientName)) {
            killMarkerTime = System.currentTimeMillis();
            playKillSound();
        }
    }

    private static String extractName(String text, boolean fromStart) {
        if (text == null || text.trim().isEmpty()) return null;
        text = text.trim();
        StringBuilder sb = new StringBuilder();
        if (fromStart) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (isNameChar(c)) sb.append(c);
                else if (sb.length() > 0) break;
            }
        } else {
            for (int i = text.length() - 1; i >= 0; i--) {
                char c = text.charAt(i);
                if (isNameChar(c)) sb.insert(0, c);
                else if (sb.length() > 0) break;
            }
        }
        String name = sb.toString();
        return name.length() >= 3 && name.length() <= 16 ? name : null;
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    private static boolean isValidHp(String s) {
        if (s == null || s.isEmpty()) return false;
        s = s.trim();
        if (s.endsWith("!")) s = s.substring(0, s.length() - 1);
        try { Float.parseFloat(s); return true; } catch (NumberFormatException e) { return false; }
    }
}
