package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import java.util.List;
import java.util.ArrayList;

public class DistanceHUDHandler {

    private Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        if (!BetterPlayerHUD.config.enableDistanceHUD) {
            if (HUDEditManager.isEditing()) {
                ScaledResolution sr = new ScaledResolution(mc);
                int sw2 = sr.getScaledWidth(), sh2 = sr.getScaledHeight();
                int bx = BetterPlayerHUD.config.distanceHudX;
                int by = BetterPlayerHUD.config.distanceHudY;
                if (bx < 0) bx = sw2 + bx;
                if (by < 0) by = sh2 + by;
                HUDEditManager.report("距离信息", bx, by, 120, 12);
            }
            return;
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        renderDistanceHUD(screenWidth, screenHeight);
    }

    private void renderDistanceHUD(int screenWidth, int screenHeight) {
        FontRenderer fr = mc.fontRendererObj;
        MovingObjectPosition mouseOver = mc.objectMouseOver;

        if (mouseOver == null || mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
            // 编辑模式下即使无目标也上报 placeholder
            if (HUDEditManager.isEditing()) {
                ScaledResolution sr2 = new ScaledResolution(mc);
                int sw2 = sr2.getScaledWidth(), sh2 = sr2.getScaledHeight();
                int bx = BetterPlayerHUD.config.distanceHudX;
                int by = BetterPlayerHUD.config.distanceHudY;
                if (bx < 0) bx = sw2 + bx;
                if (by < 0) by = sh2 + by;
                HUDEditManager.report("距离信息", bx, by, 120, 12);
            }
            return;
        }

        double distance = calculateDistance(mouseOver);
        if (distance < 0) {
            return;
        }

        // 获取所有要显示的文本行
        List<String> displayLines = getDisplayLines(mouseOver, distance);

        if (displayLines.isEmpty()) {
            return;
        }

        // 计算最大文本宽度
        int maxWidth = 0;
        for (String text : displayLines) {
            int width = fr.getStringWidth(text);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        int textHeight = displayLines.size() * 10;

        // 计算位置
        int xPos = calculateDistanceXPosition(screenWidth, maxWidth);
        int yPos = calculateDistanceYPosition(screenHeight, textHeight);

        // 绘制文本
        for (int i = 0; i < displayLines.size(); i++) {
            int color = getTextColor(i);
            fr.drawStringWithShadow(
                    displayLines.get(i),
                    xPos,
                    yPos + (i * 10),
                    color
            );
        }

        if (HUDEditManager.isEditing())
            HUDEditManager.report("距离信息", xPos, yPos, 120, displayLines.size() * 10 + 2);
    }

    private List<String> getDisplayLines(MovingObjectPosition mouseOver, double distance) {
        List<String> lines = new ArrayList<String>();

        // 第一行：距离信息
        String formatString = "%." + BetterPlayerHUD.config.distancePrecision + "f";
        String distanceText = String.format(formatString, distance) + "m";
        lines.add(distanceText);

        // 如果是方块，显示详细信息
        if (mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                mouseOver.getBlockPos() != null) {

            BlockPos pos = mouseOver.getBlockPos();
            IBlockState blockState = mc.theWorld.getBlockState(pos);
            Block block = blockState.getBlock();

            // 方块基本信息
            if (BetterPlayerHUD.config.showTargetInfo) {
                try {
                    String blockName = block.getLocalizedName();
                    if (blockName.length() > 20) {
                        blockName = blockName.substring(0, 20) + "...";
                    }
                    lines.add("方块: " + blockName);
                } catch (Exception e) {
                    lines.add("未知方块");
                }
            }

            // 方块坐标信息
            if (BetterPlayerHUD.config.showBlockCoordinates) {
                lines.add(String.format("坐标: %d, %d, %d", pos.getX(), pos.getY(), pos.getZ()));
            }

            // 方块硬度信息
            if (BetterPlayerHUD.config.showBlockHardness) {
                float hardness = block.getBlockHardness(mc.theWorld, pos);
                String hardnessText = hardness >= 0 ? String.format("%.1f", hardness) : "不可破坏";
                lines.add("硬度: " + hardnessText);
            }

            // 所需工具信息
            if (BetterPlayerHUD.config.showRequiredTool) {
                String toolInfo = getToolInfo(block, blockState);
                lines.add("工具: " + toolInfo);
            }

        } else if (mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
                BetterPlayerHUD.config.showTargetInfo) {
            // 实体信息
            if (mouseOver.entityHit != null) {
                Entity entity = mouseOver.entityHit;
                String entityName = entity.getDisplayName().getUnformattedText();
                if (entityName == null || entityName.isEmpty()) {
                    entityName = entity.getName();
                }
                if (entityName.length() > 15) {
                    entityName = entityName.substring(0, 15) + "...";
                }
                lines.add("实体: " + entityName);
            }
        }

        return lines;
    }

    private String getToolInfo(Block block, IBlockState state) {
        try {
            // 获取方块的挖掘信息
            String harvestTool = block.getHarvestTool(state);
            int harvestLevel = block.getHarvestLevel(state);

            if (harvestTool == null) {
                return "任意工具";
            }

            String toolName = "";
            switch (harvestTool) {
                case "pickaxe": toolName = "镐"; break;
                case "axe": toolName = "斧"; break;
                case "shovel": toolName = "锹"; break;
                case "hoe": toolName = "锄"; break;
                case "sword": toolName = "剑"; break;
                default: toolName = harvestTool;
            }

            if (harvestLevel > 0) {
                return toolName + " (等级 " + harvestLevel + ")";
            } else {
                return toolName;
            }

        } catch (Exception e) {
            return "未知";
        }
    }

    private int getTextColor(int lineIndex) {
        if (lineIndex == 0) {
            return BetterPlayerHUD.config.distanceColor;
        } else if (lineIndex == 1) {
            return BetterPlayerHUD.config.targetInfoColor;
        } else {
            // 其他信息行使用默认颜色或自定义颜色
            return BetterPlayerHUD.config.blockInfoColor != 0 ?
                    BetterPlayerHUD.config.blockInfoColor : 0xFFFFFF;
        }
    }

    // 使用 Minecraft 内置的 objectMouseOver（EntityRenderer 每帧免费计算），
    // 不再自建全实体扫描。
    private double calculateDistance(MovingObjectPosition mouseOver) {
        if (mouseOver.hitVec == null) {
            return -1;
        }
        Vec3 playerPos = mc.getRenderViewEntity().getPositionEyes(1.0F);
        Vec3 hitVec = mouseOver.hitVec;
        double dx = playerPos.xCoord - hitVec.xCoord;
        double dy = playerPos.yCoord - hitVec.yCoord;
        double dz = playerPos.zCoord - hitVec.zCoord;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Math.min(distance, 9999.9);
    }

    private int calculateDistanceXPosition(int screenWidth, int textWidth) {
        int offset = BetterPlayerHUD.config.distanceHudX;
        if (offset > 0) {
            return offset;
        } else if (offset < 0) {
            return screenWidth + offset - textWidth;
        } else {
            return (screenWidth - textWidth) / 2;
        }
    }

    private int calculateDistanceYPosition(int screenHeight, int textHeight) {
        int offset = BetterPlayerHUD.config.distanceHudY;
        if (offset > 0) {
            return offset;
        } else if (offset < 0) {
            return screenHeight + offset - textHeight;
        } else {
            return (screenHeight / 2) + 20;
        }
    }
}