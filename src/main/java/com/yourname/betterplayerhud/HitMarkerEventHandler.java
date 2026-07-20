package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命中检测事件处理器（由 HitMarkerMod 移植）。
 *
 * 检测路径：
 *   1) AttackEntityEvent（客户端线程）→ 近战即时反馈
 *   2) LivingHurtEvent（单人模式服务端线程）→ 所有玩家造成的伤害
 *   3) LivingDeathEvent（单人模式）→ 击杀
 *   4) EntityJoinWorldEvent（客户端）→ 追踪抛射物
 *   5) ClientTick（客户端）→ 钓鱼竿 caughtEntity + 抛射物消失后定点血量检测
 */
@SideOnly(Side.CLIENT)
public class HitMarkerEventHandler {

    private static final Random random = new Random();
    private final ConcurrentHashMap<Integer, Long> entityHitTimestamps = new ConcurrentHashMap<>();
    private static final long INVINCIBLE_FRAME = 500;

    // ── 抛射物追踪 ──
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

    // ── 定点血量轮询 ──
    private static class HealthWatch {
        float lastHealth;
        long flaggedAt;
        long updatedAt;
        boolean hitTriggered;
        HealthWatch(float h) {
            this.lastHealth = h;
            long now = System.currentTimeMillis();
            this.flaggedAt = now;
            this.updatedAt = now;
            this.hitTriggered = false;
        }
    }
    private final ConcurrentHashMap<Integer, HealthWatch> watchedHealth = new ConcurrentHashMap<>();
    private static final long WATCH_TIMEOUT = 4000;
    private static final long CONFIDENCE_WINDOW = 300;

    // ── 钓鱼竿 ──
    private int bobberId = -1;

    // ── 击杀音效防抖 ──
    private long lastKillSoundTime = 0;
    private static final long KILL_SOUND_COOLDOWN = 800;

    // ── 僵尸末日 "+" 号 + 物品减少检测（热键栏 9 槽快照，支持秒切换枪） ──
    private final int[] prevHotbarSizes = new int[9];
    private final Item[] prevHotbarTypes = new Item[9];
    private boolean prevHotbarInitialized = false;
    private static final long PLUS_CHAT_WINDOW_MS = 200;

    // ── S19 多人命中确认 ──
    private boolean s19Registered = false;
    private volatile int s19LastAttackedEntity = -1;
    private volatile long s19LastAttackTime = 0;
    private static final long S19_TIMEOUT_MS = 1000;

    // S19 回调（Netty 线程）→ 暂存到 volatile，主线程 tick 处理
    private volatile int s19PendingEntityId = -1;
    private volatile long s19PendingTime = 0;

    /** S19 命中回调（Netty 线程 — 只存 volatile，不能调 Minecraft API） */
    private void onS19Hit(int entityId, long time) {
        if (time - s19LastAttackTime > S19_TIMEOUT_MS) return;
        if (entityId != s19LastAttackedEntity) return;
        // 暂存，等主线程 tick 处理
        s19PendingEntityId = entityId;
        s19PendingTime = time;
    }

    /** 注册 S19 监听器（懒加载） */
    private void ensureS19Registered() {
        if (!s19Registered && BetterPlayerHUD.config.hitMarkerUseS19) {
            S19HitManager.registerListener(this::onS19Hit);
            s19Registered = true;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  近战（客户端线程，即时 / S19 前端）
    // ══════════════════════════════════════════════════════════════

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.entityPlayer == null || event.target == null || !event.target.isEntityAlive()) return;
        if (!isClientPlayer(event.entityPlayer)) return;

        int targetId = event.target.getEntityId();
        if (!shouldTriggerHitMarker(targetId)) return;

        if (BetterPlayerHUD.config.hitMarkerUseS19) {
            // S19 模式：先记录目标，等服务器确认；确保 pipeline 已注入
            S19HitManager.ensureInjected();
            s19LastAttackedEntity = targetId;
            s19LastAttackTime = System.currentTimeMillis();
        } else {
            // 旧模式：直接触发
            triggerHitEffects(targetId);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  受击事件（单人模式核心）
    // ══════════════════════════════════════════════════════════════

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.entity == null || event.source == null || !event.entity.isEntityAlive()) return;

        EntityPlayer attacker = getPlayerAttacker(event.source);
        if (attacker == null || !isClientPlayerByName(attacker)) return;

        int targetId = event.entity.getEntityId();
        if (!shouldTriggerHitMarker(targetId)) return;

        entityHitTimestamps.put(targetId, System.currentTimeMillis());

        HitMarkerRendererBHUD.showHitMarker();
        playRandomHitSound();

        EntityLivingBase living = (EntityLivingBase) event.entity;
        spawnHitParticlesAt(living.posX, living.posY + living.height / 2.0, living.posZ,
                30, BetterPlayerHUD.config.hitBloodIntensity);

        float hpAfter = living.getHealth() - event.ammount;
        if (hpAfter <= 0.0F) {
            HitMarkerRendererBHUD.showKillMarker();
            playKillSound();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  击杀事件
    // ══════════════════════════════════════════════════════════════

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.entity == null || event.source == null) return;

        EntityPlayer killer = getPlayerAttacker(event.source);
        if (killer != null && isClientPlayerByName(killer)) {
            HitMarkerRendererBHUD.showKillMarker();
            playKillSound();
        }

        entityHitTimestamps.remove(event.entity.getEntityId());
    }

    // ══════════════════════════════════════════════════════════════
    //  抛射物追踪
    // ══════════════════════════════════════════════════════════════

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.world.isRemote) return;
        if (!BetterPlayerHUD.config.enableHitMarker) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        Entity e = event.entity;

        if (e instanceof EntityArrow) {
            EntityArrow a = (EntityArrow) e;
            if (a.shootingEntity == mc.thePlayer ||
                (a.shootingEntity != null && a.shootingEntity.getUniqueID().equals(mc.thePlayer.getUniqueID()))) {
                trackedProjectiles.put(e.getEntityId(), new TrackedProjectile(e));
            }
            return;
        }

        if (e instanceof EntityThrowable) {
            EntityThrowable t = (EntityThrowable) e;
            EntityLivingBase thrower = t.getThrower();
            if (thrower == mc.thePlayer ||
                (thrower != null && thrower.getUniqueID().equals(mc.thePlayer.getUniqueID()))) {
                trackedProjectiles.put(e.getEntityId(), new TrackedProjectile(e));
            }
            return;
        }

        if (e instanceof EntityFishHook) {
            EntityFishHook f = (EntityFishHook) e;
            if (f.angler == mc.thePlayer) {
                bobberId = e.getEntityId();
                trackedProjectiles.put(e.getEntityId(), new TrackedProjectile(e));
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  客户端 Tick
    // ══════════════════════════════════════════════════════════════

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!BetterPlayerHUD.config.enableHitMarker) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) { watchedHealth.clear(); return; }

        // 确保 S19 监听器已注册
        ensureS19Registered();
        // 确保 Pipeline 已注入（每 tick 重试直到成功）
        if (BetterPlayerHUD.config.hitMarkerUseS19) {
            S19HitManager.ensureInjected();
        }

        // 处理 S19 待命中击（Netty 线程暂存，主线程触发）
        if (s19PendingEntityId >= 0) {
            int eid = s19PendingEntityId;
            s19PendingEntityId = -1; // 消费掉
            triggerHitEffects(eid);
        }

        // ── "+" 号 + 物品减少检测（僵尸末日等模组服，热键栏 9 槽快照） ──
        if (BetterPlayerHUD.config.enablePlusChatDetection) {
            boolean shotFired = false;

            // 扫描全部 9 个热键栏，检测任意栏位数量减少（支持秒切换枪）
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
                int curSize = (stack == null) ? 0 : stack.stackSize;
                Item curType = (stack == null) ? null : stack.getItem();

                // 先用旧快照检测减少（必须在更新快照之前）
                if (prevHotbarInitialized) {
                    if (prevHotbarTypes[i] != null && curType == prevHotbarTypes[i] && curSize < prevHotbarSizes[i]) {
                        shotFired = true;
                    }
                    if (prevHotbarTypes[i] != null && stack == null) {
                        shotFired = true;
                    }
                }

                // 更新快照
                prevHotbarSizes[i] = curSize;
                prevHotbarTypes[i] = curType;
            }
            prevHotbarInitialized = true;

            if (shotFired) {
                if (Math.abs(System.currentTimeMillis() - HitMarkerChatListener.lastPlusChatTime) <= PLUS_CHAT_WINDOW_MS) {
                    triggerPlusChatHit();
                }
            }
        } else {
            prevHotbarInitialized = false;
        }

        long now = System.currentTimeMillis();

        // 抛射物消失检测
        trackedProjectiles.values().removeIf(tp -> {
            Entity proj = mc.theWorld.getEntityByID(tp.entityId);
            if (proj == null || proj.isDead) {
                flagNearbyEntities(tp.lastX, tp.lastY, tp.lastZ, 4.0, tp.isZeroDamage);
                return true;
            }
            if (now - tp.createdAt > PROJ_TIMEOUT) return true;
            tp.lastX = proj.posX; tp.lastY = proj.posY; tp.lastZ = proj.posZ;
            return false;
        });

        // 钓鱼竿 caughtEntity
        if (bobberId != -1) {
            Entity be = mc.theWorld.getEntityByID(bobberId);
            if (be instanceof EntityFishHook) {
                EntityFishHook fh = (EntityFishHook) be;
                if (fh.caughtEntity != null && fh.caughtEntity.isEntityAlive()) {
                    Entity caught = fh.caughtEntity;
                    int cid = caught.getEntityId();
                    if (shouldTriggerHitMarker(cid)) {
                        entityHitTimestamps.put(cid, now);
                        HitMarkerRendererBHUD.showHitMarker();
                        playRandomHitSound();
                        spawnHitParticlesAt(caught.posX, caught.posY + caught.height / 2.0,
                                caught.posZ, 20, BetterPlayerHUD.config.hitBloodIntensity);
                        bobberId = -1;
                    }
                }
            } else {
                bobberId = -1;
            }
        }

        // 定点血量轮询
        for (java.util.Map.Entry<Integer, HealthWatch> entry : watchedHealth.entrySet()) {
            HealthWatch watch = entry.getValue();
            if (now - watch.updatedAt > WATCH_TIMEOUT) continue;

            Entity entity = mc.theWorld.getEntityByID(entry.getKey());
            if (!(entity instanceof EntityLivingBase) || entity.isDead) continue;

            EntityLivingBase living = (EntityLivingBase) entity;
            float curHealth = living.getHealth();
            float diff = watch.lastHealth - curHealth;

            if (watch.hitTriggered) {
                watch.lastHealth = curHealth;
                watch.updatedAt = now;
                continue;
            }

            long timeSinceFlag = now - watch.flaggedAt;

            if (diff >= 0.5f) {
                if (timeSinceFlag <= CONFIDENCE_WINDOW) {
                    if (shouldTriggerHitMarker(entry.getKey())) {
                        entityHitTimestamps.put(entry.getKey(), now);
                        HitMarkerRendererBHUD.showHitMarker();
                        playRandomHitSound();
                        spawnHitParticlesAt(living.posX, living.posY + living.height / 2.0,
                                living.posZ, 25, BetterPlayerHUD.config.hitBloodIntensity);
                    }
                    if (curHealth <= 0.0F) {
                        HitMarkerRendererBHUD.showKillMarker();
                        playKillSound();
                    }
                }
                watch.hitTriggered = true;
            }

            if (timeSinceFlag > CONFIDENCE_WINDOW) {
                watch.hitTriggered = true;
            }

            watch.lastHealth = curHealth;
            watch.updatedAt = now;
        }

        watchedHealth.entrySet().removeIf(e -> {
            if (now - e.getValue().updatedAt > WATCH_TIMEOUT) return true;
            Entity ent = mc.theWorld.getEntityByID(e.getKey());
            return ent == null || ent.isDead;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  辅助
    // ══════════════════════════════════════════════════════════════

    private void flagNearbyEntities(double x, double y, double z, double radius, boolean isZeroDamage) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        net.minecraft.util.AxisAlignedBB box = net.minecraft.util.AxisAlignedBB.fromBounds(
                x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (EntityLivingBase target : mc.theWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (target == mc.thePlayer) continue;
            int tid = target.getEntityId();
            double dist = target.getDistance(x, y, z);

            if (isZeroDamage && dist <= 1.5 && shouldTriggerHitMarker(tid)) {
                entityHitTimestamps.put(tid, System.currentTimeMillis());
                HitMarkerRendererBHUD.showHitMarker();
                playRandomHitSound();
                spawnHitParticlesAt(target.posX, target.posY + target.height / 2.0,
                        target.posZ, 20, BetterPlayerHUD.config.hitBloodIntensity);
            }

            HealthWatch existing = watchedHealth.get(tid);
            if (existing == null) {
                watchedHealth.put(tid, new HealthWatch(target.getHealth()));
            } else {
                long now = System.currentTimeMillis();
                existing.flaggedAt = now;
                existing.updatedAt = now;
                existing.hitTriggered = false;
                existing.lastHealth = target.getHealth();
            }
        }
    }

    private boolean shouldTriggerHitMarker(int entityId) {
        Long last = entityHitTimestamps.get(entityId);
        long now = System.currentTimeMillis();
        return last == null || (now - last >= INVINCIBLE_FRAME);
    }

    private boolean isClientPlayerByName(EntityPlayer player) {
        if (player == null) return false;
        EntityPlayer clientPlayer = Minecraft.getMinecraft().thePlayer;
        return clientPlayer != null && clientPlayer.getName() != null
                && clientPlayer.getName().equals(player.getName());
    }

    private boolean isClientPlayer(EntityPlayer player) {
        return player != null && player.equals(Minecraft.getMinecraft().thePlayer);
    }

    private EntityPlayer getPlayerAttacker(net.minecraft.util.DamageSource source) {
        if (source == null) return null;
        try {
            Entity direct = source.getEntity();
            if (direct instanceof EntityPlayer)
                return (EntityPlayer) direct;
            if (direct instanceof EntityArrow) {
                EntityArrow arrow = (EntityArrow) direct;
                if (arrow.shootingEntity instanceof EntityPlayer)
                    return (EntityPlayer) arrow.shootingEntity;
            }
            if (direct instanceof EntityThrowable) {
                EntityThrowable t = (EntityThrowable) direct;
                EntityLivingBase thrower = t.getThrower();
                if (thrower instanceof EntityPlayer)
                    return (EntityPlayer) thrower;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void triggerHitEffects(int entityId) {
        entityHitTimestamps.put(entityId, System.currentTimeMillis());
        try {
            HitMarkerRendererBHUD.showHitMarker();
            playRandomHitSound();
            Entity target = Minecraft.getMinecraft().theWorld.getEntityByID(entityId);
            spawnHitParticlesAt(target.posX, target.posY + target.height / 2.0,
                    target.posZ, 30, BetterPlayerHUD.config.hitBloodIntensity);
        } catch (Exception e) {
            System.err.println("[BHUD] Hit effects failed: " + e.getMessage());
        }
    }

    /** 无目标位置的命中触发（仅标识+音效，不含粒子） */
    private void triggerPlusChatHit() {
        entityHitTimestamps.put(-1, System.currentTimeMillis());
        try {
            HitMarkerRendererBHUD.showHitMarker();
            playRandomHitSound();
        } catch (Exception e) {
            System.err.println("[BHUD] PlusChat hit effects failed: " + e.getMessage());
        }
    }

    private static final String[] HIT_SOUNDS = {
            "betterplayerhud:hit1", "betterplayerhud:hit2", "betterplayerhud:hit3"
    };

    private void playRandomHitSound() {
        try {
            if (!BetterPlayerHUD.config.enableHitSounds) return;
            Minecraft.getMinecraft().thePlayer.playSound(
                    HIT_SOUNDS[random.nextInt(HIT_SOUNDS.length)],
                    BetterPlayerHUD.config.soundVolume, 1.0F);
        } catch (Exception ignored) {}
    }

    private void playKillSound() {
        try {
            if (!BetterPlayerHUD.config.enableKillSound) return;
            long now = System.currentTimeMillis();
            if (now - lastKillSoundTime < KILL_SOUND_COOLDOWN) return;
            lastKillSoundTime = now;
            Minecraft.getMinecraft().thePlayer.playSound(
                    "betterplayerhud:kill",
                    BetterPlayerHUD.config.soundVolume, 1.0F);
        } catch (Exception ignored) {}
    }

    private void spawnHitParticlesAt(double x, double y, double z, int count, double intensity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        for (int i = 0; i < count; i++) {
            double ox = (mc.theWorld.rand.nextDouble() - 0.5) * 1.2;
            double oy = (mc.theWorld.rand.nextDouble() - 0.5) * 1.2;
            double oz = (mc.theWorld.rand.nextDouble() - 0.5) * 1.2;
            mc.theWorld.spawnParticle(net.minecraft.util.EnumParticleTypes.BLOCK_CRACK,
                    x + ox, y + oy, z + oz,
                    ox * 0.15 * intensity, oy * 0.15 * intensity, oz * 0.15 * intensity,
                    net.minecraft.block.Block.getStateId(
                            net.minecraft.init.Blocks.redstone_block.getDefaultState()));
        }
    }
}
