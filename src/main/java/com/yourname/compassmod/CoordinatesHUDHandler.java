package com.yourname.compassmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CoordinatesHUDHandler {

    private Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }

        if (mc.thePlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        // 检查是否启用坐标显示
        if (!BetterPlayerHUD.config.showCoordinatesHUD) {
            return;
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        renderCoordinatesHUD(screenWidth, screenHeight);
    }

    private void renderCoordinatesHUD(int screenWidth, int screenHeight) {
        FontRenderer fr = mc.fontRendererObj;

        // 获取玩家坐标和方向信息
        String[] coordinateTexts = getFormattedCoordinates();

        // 计算文本尺寸
        int maxWidth = 0;
        for (String text : coordinateTexts) {
            int width = fr.getStringWidth(text);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        int textHeight = coordinateTexts.length * 10;

        // 计算位置
        int xPos = calculateCoordinatesXPosition(screenWidth, maxWidth);
        int yPos = calculateCoordinatesYPosition(screenHeight, textHeight);

        // 绘制坐标文本
        for (int i = 0; i < coordinateTexts.length; i++) {
            int color = (i == 0) ? BetterPlayerHUD.config.coordinatesColor : BetterPlayerHUD.config.dimensionColor;
            fr.drawStringWithShadow(
                    coordinateTexts[i],
                    xPos,
                    yPos + (i * 10),
                    color
            );
        }
    }

    // 替换getFormattedCoordinates方法为这个增强版本
    private String[] getFormattedCoordinates() {
        if (mc.thePlayer == null) {
            return new String[]{"玩家未加载"};
        }

        // 获取玩家精确坐标
        double posX = mc.thePlayer.posX;
        double posY = mc.thePlayer.posY;
        double posZ = mc.thePlayer.posZ;

        // 格式化坐标数值
        String formatString = "%." + BetterPlayerHUD.config.coordinatesPrecision + "f";
        String xText = String.format(formatString, posX);
        String yText = String.format(formatString, posY);
        String zText = String.format(formatString, posZ);

        // 构建坐标行
        String coordinatesLine = String.format("XYZ: %s, %s, %s", xText, yText, zText);

        // 如果没有开启其他信息，只返回坐标
        if (!BetterPlayerHUD.config.showDimension && !BetterPlayerHUD.config.showFacing) {
            return new String[]{coordinatesLine};
        }

        java.util.List<String> infoLines = new java.util.ArrayList<>();

        if (BetterPlayerHUD.config.showDimension) {
            String dimensionName = getDimensionName();
            infoLines.add(dimensionName);
        }

        if (BetterPlayerHUD.config.showFacing) {
            String facing = getDetailedFacing();
            infoLines.add(facing);

            // 添加生物群系信息（可选）
            if (mc.theWorld != null) {
                net.minecraft.util.BlockPos pos = new net.minecraft.util.BlockPos(posX, posY, posZ);
                net.minecraft.world.biome.BiomeGenBase biome = mc.theWorld.getBiomeGenForCoords(pos);
                if (biome != null) {
                    infoLines.add("生物群系: " + biome.biomeName);
                }
            }
        }

        // 将信息行合并为字符串数组
        String[] result = new String[1 + infoLines.size()];
        result[0] = coordinatesLine;
        for (int i = 0; i < infoLines.size(); i++) {
            result[i + 1] = infoLines.get(i);
        }

        return result;
    }

    private String getDetailedFacing() {
        if (mc.thePlayer == null) {
            return "未知方向";
        }

        // 获取玩家朝向（0-360度）
        float yaw = (mc.thePlayer.rotationYaw % 360 + 360) % 360;
        float pitch = mc.thePlayer.rotationPitch;

        // 详细方向检测
        String horizontalFacing;
        if (yaw < 22.5) horizontalFacing = "南";
        else if (yaw < 67.5) horizontalFacing = "西南";
        else if (yaw < 112.5) horizontalFacing = "西";
        else if (yaw < 157.5) horizontalFacing = "西北";
        else if (yaw < 202.5) horizontalFacing = "北";
        else if (yaw < 247.5) horizontalFacing = "东北";
        else if (yaw < 292.5) horizontalFacing = "东";
        else if (yaw < 337.5) horizontalFacing = "东南";
        else horizontalFacing = "南";

        String verticalFacing;
        if (pitch < -45) verticalFacing = "↑"; // 向上看
        else if (pitch > 45) verticalFacing = "↓"; // 向下看
        else verticalFacing = "→"; // 平视

        return String.format("朝向: %s %s (%.1f°)", horizontalFacing, verticalFacing, yaw);
    }

    private String getDimensionName() {
        if (mc.theWorld == null || mc.theWorld.provider == null) {
            return "未知维度";
        }

        int dimensionId = mc.theWorld.provider.getDimensionId();

        switch (dimensionId) {
            case -1:
                return "下界";
            case 0:
                return "主世界";
            case 1:
                return "末地";
            default:
                return "维度 " + dimensionId;
        }
    }

    private String getFacingDirection() {
        if (mc.thePlayer == null) {
            return "未知方向";
        }

        // 获取玩家朝向（0-360度）
        float yaw = (mc.thePlayer.rotationYaw % 360 + 360) % 360;

        // 转换为基本方向
        if (yaw < 45) return "南";
        else if (yaw < 135) return "西";
        else if (yaw < 225) return "北";
        else if (yaw < 315) return "东";
        else return "南";
    }

    private int calculateCoordinatesXPosition(int screenWidth, int textWidth) {
        int offset = BetterPlayerHUD.config.coordinatesHudX;

        if (offset >= 0) {
            return offset; // 正数：从左侧计算
        } else {
            return screenWidth + offset - textWidth; // 负数：从右侧计算
        }
    }

    private int calculateCoordinatesYPosition(int screenHeight, int textHeight) {
        int offset = BetterPlayerHUD.config.coordinatesHudY;

        if (offset >= 0) {
            return offset; // 正数：从顶部计算
        } else {
            return screenHeight + offset - textHeight; // 负数：从底部计算
        }
    }
}