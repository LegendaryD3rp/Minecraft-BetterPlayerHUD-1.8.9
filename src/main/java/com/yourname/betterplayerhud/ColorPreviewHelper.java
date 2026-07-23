package com.yourname.betterplayerhud;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.GuiConfigEntries.NumberSliderEntry;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

/**
 * 颜色工具类 — 将 RGB 三分量配置 + 实时预览盘统一管理。
 */
public class ColorPreviewHelper {

    // ═══════════════════════════════════════════════════════════════
    //  R/G/B 滑块元素（使用 Forge 原生 NumberSliderEntry）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 强制 min=0, max=255 的滑块封装，避免 Property 的 MIN_VALUE/MAX_VALUE 导致爆炸。
     */
    public static class SliderConfigElement extends ConfigElement {
        public SliderConfigElement(Property prop) {
            super(prop);
        }

        @Override
        public Class<? extends GuiConfigEntries.IConfigEntry> getConfigEntryClass() {
            return NumberSliderEntry.class;
        }

        @Override
        public Object getMinValue() {
            return "0";
        }

        @Override
        public Object getMaxValue() {
            return "255";
        }
    }

    /**
     * 为一个颜色字段生成 R / G / B 三个原生滑块 IConfigElement。
     */
    public static List<IConfigElement> createColorElements(Configuration config,
                                                            String category,
                                                            String keyBase) {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new SliderConfigElement(
                config.get(category, keyBase + "R", 255, "", 0, 255)));
        list.add(new SliderConfigElement(
                config.get(category, keyBase + "G", 255, "", 0, 255)));
        list.add(new SliderConfigElement(
                config.get(category, keyBase + "B", 255, "", 0, 255)));
        return list;
    }

    /**
     * 为一个颜色字段生成 R / G / B / A 四个原生滑块 IConfigElement（带 Alpha）。
     */
    public static List<IConfigElement> createColorElementsARGB(Configuration config,
                                                               String category,
                                                               String keyBase) {
        List<IConfigElement> list = new ArrayList<>();
        list.add(new SliderConfigElement(
                config.get(category, keyBase + "A", 255, "", 0, 255)));
        list.add(new SliderConfigElement(
                config.get(category, keyBase + "R", 255, "", 0, 255)));
        list.add(new SliderConfigElement(
                config.get(category, keyBase + "G", 255, "", 0, 255)));
        list.add(new SliderConfigElement(
                config.get(category, keyBase + "B", 255, "", 0, 255)));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    //  读 GUI 滑块实时值
    // ═══════════════════════════════════════════════════════════════

    /**
     * 从当前 GuiConfig 的 entryList 中获取某个字段的实时 int 值。
     */
    public static int getLiveInt(GuiConfig screen, String name, int def) {
        if (screen.entryList == null || screen.entryList.listEntries == null) return def;
        for (GuiConfigEntries.IConfigEntry entry : screen.entryList.listEntries) {
            try {
                if (entry.getConfigElement() != null
                        && name.equals(entry.getConfigElement().getName())) {
                    return Integer.parseInt(String.valueOf(entry.getCurrentValue()));
                }
            } catch (Exception ignored) {
            }
        }
        return def;
    }

    // ═══════════════════════════════════════════════════════════════
    //  通用预览子页面（左侧原生滑块，右侧色块预览）
    // ═══════════════════════════════════════════════════════════════

    public static class ColorModuleScreen extends GuiConfig {
        private final ColorInfo[] colorInfos;

        public ColorModuleScreen(GuiScreen parent,
                                 List<IConfigElement> childElements,
                                 ColorInfo[] colorInfos,
                                 String title) {
            super(parent, childElements, BetterPlayerHUD.MODID, false, false, title);
            this.colorInfos = colorInfos;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            super.drawScreen(mouseX, mouseY, partialTicks);

            if (colorInfos == null || colorInfos.length == 0) return;

            int px = this.width / 2 + 160; // 预览区左边缘
            int py = this.height / 6 + 10;
            int pw = 80;
            int sw = 34;

            for (ColorInfo ci : colorInfos) {
                boolean hasAlpha = ci instanceof ColorInfoARGB;
                int a = hasAlpha ? getLiveInt(this, ci.keyBase + "A", 255) : 255;
                int r = getLiveInt(this, ci.keyBase + "R", 128);
                int g = getLiveInt(this, ci.keyBase + "G", 128);
                int b = getLiveInt(this, ci.keyBase + "B", 128);

                // 色块背景
                drawRect(px - 4, py - 4, px + pw + 4, py + 82, 0xBB000000);
                // 标签
                this.fontRendererObj.drawString("\u00a7f" + ci.label,
                        px + (pw - fontRendererObj.getStringWidth(ci.label)) / 2, py, 0xFFFFFF);
                // 色块（带实际 Alpha）
                int color = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                drawRect(px + (pw - sw) / 2, py + 14, px + (pw + sw) / 2, py + 14 + sw, color);
                drawRect(px + (pw - sw) / 2 - 1, py + 14 - 1, px + (pw + sw) / 2 + 1, py + 14, 0xFFAAAAAA);
                drawRect(px + (pw - sw) / 2 - 1, py + 14 + sw, px + (pw + sw) / 2 + 1, py + 14 + sw + 1, 0xFFAAAAAA);
                drawRect(px + (pw - sw) / 2 - 1, py + 14, px + (pw - sw) / 2, py + 14 + sw, 0xFFAAAAAA);
                drawRect(px + (pw + sw) / 2, py + 14, px + (pw + sw) / 2 + 1, py + 14 + sw, 0xFFAAAAAA);
                // RGB 文本
                String s = hasAlpha ? ("\u00a77A:" + a + " R:" + r + " G:" + g + " B:" + b)
                                    : ("\u00a77R:" + r + " G:" + g + " B:" + b);
                drawCenteredString(fontRendererObj, s, px + pw / 2, py + 50, 0xAAAAAA);

                py += 92;
            }
        }

        @Override
        public void onGuiClosed() {
            super.onGuiClosed();
            if (BetterPlayerHUD.config != null) {
                BetterPlayerHUD.config.reloadFromConfig(); // Properties → Java 字段
                BetterPlayerHUD.config.saveConfig();       // Java 字段 → 磁盘
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  颜色元数据
    // ═══════════════════════════════════════════════════════════════

    public static class ColorInfo {
        public final String keyBase;
        public final String label;

        public ColorInfo(String keyBase, String label) {
            this.keyBase = keyBase;
            this.label = label;
        }
    }

    /** 带 Alpha 通道的颜色元数据（多读一个 A 滑块，渲染时使用实际 Alpha）。 */
    public static class ColorInfoARGB extends ColorInfo {
        public ColorInfoARGB(String keyBase, String label) {
            super(keyBase, label);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  带预览的 CategoryEntry / CategoryElement
    // ═══════════════════════════════════════════════════════════════

    /**
     * 工厂方法：创建一个带预览盘的 DummyCategoryElement。
     */
    public static DummyConfigElement.DummyCategoryElement
    createPreviewCategory(String name, String langKey,
                          List<IConfigElement> childElements,
                          ColorInfo[] colorInfos,
                          String title) {
        return new PreviewCategoryElement(name, langKey, childElements, colorInfos, title);
    }

    private static class PreviewCategoryElement extends DummyConfigElement.DummyCategoryElement {
        final List<IConfigElement> childElements;
        final ColorInfo[] colorInfos;
        final String title;

        PreviewCategoryElement(String name, String langKey,
                               List<IConfigElement> childElements,
                               ColorInfo[] colorInfos, String title) {
            super(name, langKey, childElements);
            this.childElements = childElements;
            this.colorInfos = colorInfos;
            this.title = title;
        }

        @Override
        public Class<? extends GuiConfigEntries.IConfigEntry> getConfigEntryClass() {
            return PreviewCategoryEntry.class;
        }

    }

    public static class PreviewCategoryEntry extends GuiConfigEntries.CategoryEntry {
        public PreviewCategoryEntry(GuiConfig owningScreen,
                                    GuiConfigEntries owningEntryList,
                                    IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
        }

        @Override
        protected GuiScreen buildChildScreen() {
            PreviewCategoryElement pce = (PreviewCategoryElement) getConfigElement();
            return new ColorModuleScreen(this.owningScreen,
                    pce.childElements, pce.colorInfos, pce.title);
        }
    }

    /**
     * 将多个 R/G/B 滑块元素展平放入目标列表。
     */
    public static void addColorElements(List<IConfigElement> target,
                                        Configuration config,
                                        String category,
                                        String keyBase) {
        target.addAll(createColorElements(config, category, keyBase));
    }
}
