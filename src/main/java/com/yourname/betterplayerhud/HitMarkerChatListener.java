package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

/**
 * 击杀聊天检测器（由 HitMarkerMod 移植）。
 *
 * 中英文击杀/弓箭命中消息 → 触发标识 / 音效。
 *
 * 同时为「+ 号命中检测」提供数据源：
 * 当服务端发送 "+..." 开头的消息时，
 * 记录时间戳供 HitMarkerEventHandler 消费。
 */
@SideOnly(Side.CLIENT)
public class HitMarkerChatListener {

    private final Random random = new Random();

    /** 最近一次 "+" 开头聊天消息的时间戳（ms），供外部读取 */
    public static volatile long lastPlusChatTime = 0;

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (!BetterPlayerHUD.config.enableHitMarker) return;

        String raw = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        if (raw == null || raw.isEmpty()) return;

        // ── "+ 号数据检测（永不触发效果，只记时间戳） ──
        if (raw.startsWith("+")) {
            lastPlusChatTime = System.currentTimeMillis();
        }

        if (!BetterPlayerHUD.config.enableChatKillDetection) return;

        String clientName = Minecraft.getMinecraft().thePlayer.getName();
        if (clientName == null || clientName.isEmpty()) return;

        // 最终击杀
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
                triggerKill();
                return;
            }
        }

        // 英文：A by B
        int by = lower.indexOf(" by ");
        if (by != -1) {
            String a = extractName(raw.substring(0, by), false);
            String b = extractName(raw.substring(by + 4), true);
            if (b != null && b.equals(clientName) && a != null && !a.equals(clientName)) {
                triggerKill();
                return;
            }
        }

        // 中文弓箭：A还剩X生命值！
        if (raw.contains("还剩") && raw.contains("生命值")) {
            int remain = raw.indexOf("还剩");
            int life = raw.indexOf("生命值");
            if (remain < life) {
                String a = extractName(raw.substring(0, remain), false);
                String hp = raw.substring(remain + 2, life).trim();
                if (a != null && !a.equals(clientName) && isValidNumber(hp)) {
                    triggerHit();
                    return;
                }
            }
        }

        // 英文弓箭：A is on X HP!
        if (lower.contains("is on") && lower.contains(" hp") && !lower.contains(" has ")) {
            int isOn = lower.indexOf("is on");
            int hpEnd = lower.indexOf(" hp", isOn + 5);
            if (isOn != -1 && hpEnd != -1 && isOn + 5 < hpEnd) {
                String a = extractName(raw.substring(0, isOn), false);
                String hp = raw.substring(isOn + 5, hpEnd).trim();
                if (a != null && !a.equals(clientName) && isValidNumber(hp)) {
                    triggerHit();
                    return;
                }
            }
        }

        // 替代英文：A has X HP!
        if (lower.contains(" has ") && lower.contains(" hp")) {
            int has = lower.indexOf(" has ");
            int hpEnd = lower.indexOf(" hp", has + 5);
            if (has != -1 && hpEnd != -1) {
                String a = extractName(raw.substring(0, has), false);
                String hp = raw.substring(has + 5, hpEnd).trim();
                if (a != null && !a.equals(clientName) && isValidNumber(hp)) {
                    triggerHit();
                    return;
                }
            }
        }
    }

    private void triggerKill() {
        HitMarkerRendererBHUD.showKillMarker();
        playKillSound();
    }

    private void triggerHit() {
        HitMarkerRendererBHUD.showHitMarker();
        playRandomHitSound();
    }

    private void playRandomHitSound() {
        if (!BetterPlayerHUD.config.enableHitSounds) return;
        String[] sounds = {"betterplayerhud:hit1", "betterplayerhud:hit2", "betterplayerhud:hit3"};
        Minecraft.getMinecraft().thePlayer.playSound(
                sounds[random.nextInt(sounds.length)],
                BetterPlayerHUD.config.soundVolume, 1.0F);
    }

    private void playKillSound() {
        if (!BetterPlayerHUD.config.enableKillSound) return;
        Minecraft.getMinecraft().thePlayer.playSound(
                "betterplayerhud:kill",
                BetterPlayerHUD.config.soundVolume, 1.0F);
    }

    private void processFinalKill(String msg, String clientName) {
        String a = extractName(msg, true);
        if (a == null || a.equals(clientName)) return;
        if (msg.contains(clientName)) {
            triggerKill();
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
        return name.length() >= 2 && name.length() <= 16 ? name : null;
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    private static boolean isValidNumber(String s) {
        if (s == null || s.isEmpty()) return false;
        s = s.trim();
        if (s.endsWith("!")) s = s.substring(0, s.length() - 1);
        try { Float.parseFloat(s); return true; } catch (NumberFormatException e) { return false; }
    }
}
