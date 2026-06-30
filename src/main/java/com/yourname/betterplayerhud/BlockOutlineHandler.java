package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

public class BlockOutlineHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    // 可见性判断的容差系数，用于提高描边灵敏度。可微调（例如0.01-0.03）。
  //  private static final double FACE_VISIBILITY_EPSILON = 0.01;

    // 快捷键防连发
    private boolean wasBlockKeyPressed = false;
    private boolean wasEntityKeyPressed = false;
    private boolean wasRGBKeyPressed = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null) return;

        // 方块描边开关快捷键
        if (BetterPlayerHUD.config.keyBindToggleBlockOutline > 0) {
            boolean pressed = org.lwjgl.input.Keyboard.isKeyDown(BetterPlayerHUD.config.keyBindToggleBlockOutline);
            if (pressed && !wasBlockKeyPressed) {
                BetterPlayerHUD.config.enableBlockHighlight = !BetterPlayerHUD.config.enableBlockHighlight;
                BetterPlayerHUD.config.saveConfig();
            }
            wasBlockKeyPressed = pressed;
        }

        // 实体碰撞箱开关快捷键
        if (BetterPlayerHUD.config.keyBindToggleEntityHitbox > 0) {
            boolean pressed = org.lwjgl.input.Keyboard.isKeyDown(BetterPlayerHUD.config.keyBindToggleEntityHitbox);
            if (pressed && !wasEntityKeyPressed) {
                BetterPlayerHUD.config.enableEntityHighlight = !BetterPlayerHUD.config.enableEntityHighlight;
                BetterPlayerHUD.config.saveConfig();
            }
            wasEntityKeyPressed = pressed;
        }

        // RGB 流光开关快捷键
        if (BetterPlayerHUD.config.keyBindToggleRGB > 0) {
            boolean pressed = org.lwjgl.input.Keyboard.isKeyDown(BetterPlayerHUD.config.keyBindToggleRGB);
            if (pressed && !wasRGBKeyPressed) {
                BetterPlayerHUD.config.enableRGBMode = !BetterPlayerHUD.config.enableRGBMode;
                BetterPlayerHUD.config.saveConfig();
            }
            wasRGBKeyPressed = pressed;
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        // 如果两者都未启用，则提前返回
        if (!BetterPlayerHUD.config.enableBlockHighlight && !BetterPlayerHUD.config.enableEntityHighlight) {
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null || mc.getRenderViewEntity() == null) {
            return;
        }

        MovingObjectPosition mouseOver = mc.objectMouseOver;
        if (mouseOver == null) {
            return;
        }

        if (mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && BetterPlayerHUD.config.enableBlockHighlight) {
            BlockPos pos = mouseOver.getBlockPos();
            if (pos != null) {
                drawBlockOutline(pos, event.partialTicks);
            }
        } else if (mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && BetterPlayerHUD.config.enableEntityHighlight) {
            if (mouseOver.entityHit != null) {
                // 隐身隐藏检查
                if (BetterPlayerHUD.config.hideHitboxForInvisible && mouseOver.entityHit.isInvisible()) {
                    return;
                }
                AxisAlignedBB entityBoundingBox = mouseOver.entityHit.getEntityBoundingBox();
                if (entityBoundingBox != null) {
                    int color = getEffectiveEntityOutlineColor(mouseOver.entityHit);
                    drawEntityBoundingBox(entityBoundingBox, event.partialTicks, color);
                }
            }
        }
    }

    /**
     * 获取实体描边颜色（考虑 RGB 模式）。
     */
    private int getEffectiveEntityOutlineColor(net.minecraft.entity.Entity entity) {
        if (BetterPlayerHUD.config.enableRGBMode && BetterPlayerHUD.config.rgbApplyEntityHitbox) {
            return RGBFlowColor.getColor(BetterPlayerHUD.config.rgbSpeed);
        }
        return getEntityOutlineColor(entity);
    }

    /**
     * 根据实体类型返回对应的静态描边颜色（无 RGB）。
     */
    private int getEntityOutlineColor(net.minecraft.entity.Entity entity) {
        if (entity instanceof net.minecraft.entity.monster.IMob) {
            return BetterPlayerHUD.config.entityOutlineColorHostile; // 敌对-红
        } else if (entity instanceof net.minecraft.entity.passive.EntityAnimal ||
                entity instanceof net.minecraft.entity.passive.EntityVillager) {
            return BetterPlayerHUD.config.entityOutlineColorNeutral; // 中立-黄
        } else if (entity instanceof net.minecraft.entity.player.EntityPlayer) {
            return BetterPlayerHUD.config.entityOutlineColorFriendly; // 友好-绿
        } else {
            return BetterPlayerHUD.config.entityOutlineColorNeutral; // 默认-黄
        }
    }

    /**
     * 绘制实体碰撞箱
     */
    private void drawEntityBoundingBox(AxisAlignedBB aabb, float partialTicks, int outlineColor) {
        double playerX = mc.getRenderViewEntity().lastTickPosX + (mc.getRenderViewEntity().posX - mc.getRenderViewEntity().lastTickPosX) * partialTicks;
        double playerY = mc.getRenderViewEntity().lastTickPosY + (mc.getRenderViewEntity().posY - mc.getRenderViewEntity().lastTickPosY) * partialTicks;
        double playerZ = mc.getRenderViewEntity().lastTickPosZ + (mc.getRenderViewEntity().posZ - mc.getRenderViewEntity().lastTickPosZ) * partialTicks;

        double expand = 0.002;
        aabb = aabb.expand(expand, expand, expand);

        // 调用通用渲染方法，并指定 isEntity = true
        renderBoundingBox(aabb, playerX, playerY, playerZ, outlineColor, BetterPlayerHUD.config.entityOutlineWidth, true);
    }

    /**
     * 绘制方块轮廓
     */
    private void drawBlockOutline(BlockPos pos, float partialTicks) {
        AxisAlignedBB aabb = mc.theWorld.getBlockState(pos).getBlock().getSelectedBoundingBox(mc.theWorld, pos);
        if (aabb == null) {
            return;
        }

        double playerX = mc.getRenderViewEntity().lastTickPosX + (mc.getRenderViewEntity().posX - mc.getRenderViewEntity().lastTickPosX) * partialTicks;
        double playerY = mc.getRenderViewEntity().lastTickPosY + (mc.getRenderViewEntity().posY - mc.getRenderViewEntity().lastTickPosY) * partialTicks;
        double playerZ = mc.getRenderViewEntity().lastTickPosZ + (mc.getRenderViewEntity().posZ - mc.getRenderViewEntity().lastTickPosZ) * partialTicks;

        double expand = 0.002;
        aabb = aabb.expand(expand, expand, expand);

        // 调用通用渲染方法，并指定 isEntity = false
        renderBoundingBox(aabb, playerX, playerY, playerZ, getEffectiveBlockOutlineColor(), BetterPlayerHUD.config.blockOutlineWidth, false);
    }

    /**
     * 获取方块描边颜色（考虑 RGB 模式）。
     */
    private int getEffectiveBlockOutlineColor() {
        if (BetterPlayerHUD.config.enableRGBMode && BetterPlayerHUD.config.rgbApplyBlockOutline) {
            return RGBFlowColor.getColor(BetterPlayerHUD.config.rgbSpeed);
        }
        return BetterPlayerHUD.config.blockOutlineColor;
    }

    /**
     * 通用渲染核心：管理OpenGL状态，并调用具体的包围盒绘制逻辑。
     * @param isEntity true=实体，false=方块。用于选择对应的“仅可见面”配置。
     */
    private void renderBoundingBox(AxisAlignedBB aabb, double offsetX, double offsetY, double offsetZ, int color, float lineWidth, boolean isEntity) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(lineWidth);

        double minX = aabb.minX - offsetX;
        double minY = aabb.minY - offsetY;
        double minZ = aabb.minZ - offsetZ;
        double maxX = aabb.maxX - offsetX;
        double maxY = aabb.maxY - offsetY;
        double maxZ = aabb.maxZ - offsetZ;

        boolean useFlowing = BetterPlayerHUD.config.enableRGBMode &&
            (isEntity ? BetterPlayerHUD.config.rgbApplyEntityHitbox : BetterPlayerHUD.config.rgbApplyBlockOutline);

        if (useFlowing) {
            drawFlowingBoundingBox(minX, minY, minZ, maxX, maxY, maxZ, isEntity);
        } else {
            // 非 RGB：使用原版 drawSelectionBoundingBox 绘制 12 条棱
            setColor(color);
            net.minecraft.client.renderer.RenderGlobal.drawSelectionBoundingBox(aabb);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    /**
     * 绘制流动RGB包围盒。
     * 将12条棱按周界排列，每条棱独立着色，色相 = (时间偏移 + 周界位置偏移) % 1.0
     */
    private void drawFlowingBoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, boolean isEntity) {
        double dx = maxX - minX;
        double dy = maxY - minY;
        double dz = maxZ - minZ;
        float totalPerimeter = (float)(4 * dx + 4 * dy + 4 * dz);
        long speed = BetterPlayerHUD.config.rgbSpeed;
        long stepMs = BetterPlayerHUD.config.rgbStepMs;
        if (totalPerimeter <= 0) return;

        // ═══════════════════════════════════════════════════════════════
        //  统一采样时间，保证一帧内所有顶点颜色使用同一时间基准，
        //  避免逐棱渲染时 System.currentTimeMillis() 漂移导致首尾不衔接
        // ═══════════════════════════════════════════════════════════════
        long currentTimeMs = System.currentTimeMillis();

        boolean drawAllFaces = !isEntity ? !BetterPlayerHUD.config.drawVisibleFacesOnlyBlocks : !BetterPlayerHUD.config.drawVisibleFacesOnlyEntities;

        // 计算面可见性（用于 visible-faces-only 模式）
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;
        float eyeH = mc.thePlayer != null ? mc.thePlayer.getEyeHeight() : 1.62F;
        double camX = -centerX;
        double camY = -centerY + eyeH;  // 修正：相机在眼睛高度，不在脚底
        double camZ = -centerZ;
        boolean[] faceVis = {
            isFaceVisible( 0,-1, 0, centerX, minY, centerZ, camX, camY, camZ, eyeH),
            isFaceVisible( 0, 1, 0, centerX, maxY, centerZ, camX, camY, camZ, eyeH),
            isFaceVisible( 0, 0,-1, centerX, centerY, minZ, camX, camY, camZ, eyeH),
            isFaceVisible( 0, 0, 1, centerX, centerY, maxZ, camX, camY, camZ, eyeH),
            isFaceVisible(-1, 0, 0, minX, centerY, centerZ, camX, camY, camZ, eyeH),
            isFaceVisible( 1, 0, 0, maxX, centerY, centerZ, camX, camY, camZ, eyeH),
        };

        // 12条棱的端点坐标 [x1,y1,z1, x2,y2,z2]
        // 周界顺序：底部4条 -> 垂直4条 -> 顶部4条
        double[][] edges = {
            {minX, minY, minZ, maxX, minY, minZ},
            {maxX, minY, minZ, maxX, minY, maxZ},
            {maxX, minY, maxZ, minX, minY, maxZ},
            {minX, minY, maxZ, minX, minY, minZ},
            {minX, minY, minZ, minX, maxY, minZ},
            {maxX, minY, minZ, maxX, maxY, minZ},
            {maxX, minY, maxZ, maxX, maxY, maxZ},
            {minX, minY, maxZ, minX, maxY, maxZ},
            {minX, maxY, minZ, maxX, maxY, minZ},
            {maxX, maxY, minZ, maxX, maxY, maxZ},
            {maxX, maxY, maxZ, minX, maxY, maxZ},
            {minX, maxY, maxZ, minX, maxY, minZ},
        };

        // 每条棱属于哪些面: 0=下 1=上 2=北 3=南 4=西 5=东
        // 每根棱恰好属于2个面（立方体几何）
        int[][] edgeFaces = {
            {0,2},   // 0: 底-后     (minX,minY,minZ)-(maxX,minY,minZ)
            {0,5},   // 1: 底-右     (maxX,minY,minZ)-(maxX,minY,maxZ)
            {0,3},   // 2: 底-前     (maxX,minY,maxZ)-(minX,minY,maxZ)
            {0,4},   // 3: 底-左     (minX,minY,maxZ)-(minX,minY,minZ)
            {2,4},   // 4: 北-左(垂直)  (minX,minY,minZ)-(minX,maxY,minZ)
            {2,5},   // 5: 北-右(垂直)  (maxX,minY,minZ)-(maxX,maxY,minZ)
            {3,5},   // 6: 南-右(垂直)  (maxX,minY,maxZ)-(maxX,maxY,maxZ)
            {3,4},   // 7: 南-左(垂直)  (minX,minY,maxZ)-(minX,maxY,maxZ)
            {1,2},   // 8: 顶-后     (minX,maxY,minZ)-(maxX,maxY,minZ)
            {1,5},   // 9: 顶-右     (maxX,maxY,minZ)-(maxX,maxY,maxZ)
            {1,3},   // 10: 顶-前    (maxX,maxY,maxZ)-(minX,maxY,maxZ)
            {1,4},   // 11: 顶-左    (minX,maxY,maxZ)-(minX,maxY,minZ)
        };

        // 逐顶点 RGB：每条棱端点各自算颜色，使用 POSITION_COLOR 让 OpenGL 插值
        float pos = 0;
        for (int i = 0; i < 12; i++) {
            double[] e = edges[i];
            double len = Math.sqrt(
                (e[3]-e[0])*(e[3]-e[0]) + (e[4]-e[1])*(e[4]-e[1]) + (e[5]-e[2])*(e[5]-e[2]));
            float startPos = pos;
            float endPos = pos + (float)len;

            boolean visible = drawAllFaces;
            if (!drawAllFaces) {
                for (int fi : edgeFaces[i]) {
                    if (faceVis[fi]) { visible = true; break; }
                }
            }

            if (visible) {
                int startColor = RGBFlowColor.getFlowColorAtTime(currentTimeMs, startPos, totalPerimeter, speed, stepMs);
                int endColor = RGBFlowColor.getFlowColorAtTime(currentTimeMs, endPos, totalPerimeter, speed, stepMs);
                float sr = ((startColor >> 16) & 0xFF) / 255f;
                float sg = ((startColor >> 8) & 0xFF) / 255f;
                float sb = (startColor & 0xFF) / 255f;
                float sa = ((startColor >> 24) & 0xFF) / 255f;
                float er = ((endColor >> 16) & 0xFF) / 255f;
                float eg = ((endColor >> 8) & 0xFF) / 255f;
                float eb = (endColor & 0xFF) / 255f;
                float ea = ((endColor >> 24) & 0xFF) / 255f;
                Tessellator tess = Tessellator.getInstance();
                WorldRenderer wr = tess.getWorldRenderer();
                wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(e[0], e[1], e[2]).color(sr, sg, sb, sa).endVertex();
                wr.pos(e[3], e[4], e[5]).color(er, eg, eb, ea).endVertex();
                tess.draw();
            }
            pos += (float)len;
        }
    }


    /**
     * 判断面是否可见。
     * camToBlock = 块中心指向相机的向量（已包含眼高偏移）。
     * eyeY = 相机眼睛高度（玩家相对坐标）。
     */
    private boolean isFaceVisible(double faceNormalX, double faceNormalY, double faceNormalZ,
                                  double faceCenterX, double faceCenterY, double faceCenterZ,
                                  double camToBlockX, double camToBlockY, double camToBlockZ,
                                  double eyeY) {
        // 相机位置在玩家相对坐标中 = (0, eyeY, 0)
        // 面中心到相机的方向 = 相机 - 面中心
        double toCamX = -faceCenterX;
        double toCamY = eyeY - faceCenterY;
        double toCamZ = -faceCenterZ;
        double dot = faceNormalX * toCamX + faceNormalY * toCamY + faceNormalZ * toCamZ;

        final double EPSILON = 1e-5;
        return dot > EPSILON;
    }



    /**
     * 将ARGB整数颜色设置到OpenGL。
     */
    private void setColor(int color) {
        float alpha = (float) ((color >> 24) & 0xFF) / 255.0F;
        float red = (float) ((color >> 16) & 0xFF) / 255.0F;
        float green = (float) ((color >> 8) & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;
        GlStateManager.color(red, green, blue, alpha);
    }
}