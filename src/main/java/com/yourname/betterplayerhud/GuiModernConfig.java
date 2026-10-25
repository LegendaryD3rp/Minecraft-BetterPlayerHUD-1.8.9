package com.yourname.betterplayerhud;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.lang.reflect.Field;
import java.io.IOException;

/**
 * F8 配置界面 — v1.6.0 从零重写版
 *
 * 三层树：分类（Category）→ 模块（Module）→ 子设置组（Sub-group）
 * 摩登 UI：左侧树面板（图标色点 + 选中高亮）、右侧卡片式条目（圆角 + 分隔线）、
 *          脉冲动画、搜索过滤、HSL 颜色选择器
 *
 * 本地化：统一走 LangManager（F9 游戏内一键切换中英）
 */
public class GuiModernConfig extends GuiScreen {

    // ═══ 色板 ═══
    private static final int C_BG=0xFF0D0D1A, C_PANEL=0xFF1A1A2E, C_CARD=0xFF22223A;
    private static final int C_CARD_HOVER=0xFF2A2A4E, C_ACCENT=0xFF4A9EFF, C_TEXT=0xFFCCCCCC;
    private static final int C_TEXT_DIM=0xFF888888, C_DIVIDER=0xFF2A2A3E, C_TOGGLE_ON=0xFF4CAF50;
    private static final int C_TOGGLE_OFF=0xFF666666, C_HEADER=0xFF16162A, C_SEARCH_BG=0xFF2A2A3E;
    private static final int LEFT_W=210, HEADER_H=36, FOOTER_H=22, ENTRY_H=22;
    private static final int C_SEL_ACCENT=0xFF4A9EFF;

    // ═══ 数据模型 ═══
    public static class TreeNode {
        public String label;                     // lang key（.lang 文件）
        public int dotColor=0xFF4A9EFF;          // 树节点色点
        public List<TreeNode> children=new ArrayList<>();
        public List<ConfigEntry> entries=new ArrayList<>();
        public boolean expanded=true;
        TreeNode(String l){label=l;}
    }

    public static class ConfigEntry {
        public enum Type{BOOLEAN,INT,FLOAT,COLOR,ENUM}
        public final Type type;
        public final String label;               // lang key（.lang 文件）
        public final Supplier<String> getter;    // 当前值字符串
        public final String tooltip;             // tooltip key（LangManager），无则 null
        public final Object setter;              // Consumer<Boolean>/<Integer>/<Float>/<String>
        public final int minInt,maxInt;
        public final float minFlt,maxFlt;
        public final String[] enumValues;
        public final boolean isARGB;
        public final String colorKey;            // 颜色字段名
        public String colorKey2, colorKey3;      // 三通道颜色的 G/B 字段名
        int x,y,w,h;
        public ConfigEntry(Type t,String lb,Supplier<String> g,Object s,
                           int mn,int mx,float mnf,float mxf,String[] ev,
                           String tip,boolean argb,String ck){
            type=t;label=lb;getter=g;setter=s;minInt=mn;maxInt=mx;
            minFlt=mnf;maxFlt=mxf;enumValues=ev;tooltip=tip;isARGB=argb;colorKey=ck;
        }
    }

    // ═══ 界面状态 ═══
    private final GuiScreen parent;
    private final List<TreeNode> roots=new ArrayList<>();
    private TreeNode selectedNode=null;
    private int treeScroll=0, entryScroll=0;
    private String searchQuery="";
    private ConfigEntry hoveredEntry=null;
    private long pulseEnd=0;
    private String pulseLabel=null;
    // 窗口面板（居中，保留游戏世界可见）
    private int px,py,pw,ph;
    private void panel(){
        pw=Math.min(720,width-40); ph=Math.min(500,height-40);
        px=(width-pw)/2; py=(height-ph)/2;
    }

    public GuiModernConfig(GuiScreen p){parent=p;buildTree();selectFirst();}

    // ═══ 反射字段读写 ═══
    private static java.util.Map<String,Field> cfMap=null;
    private static Field cf(String n){
        if(cfMap==null){
            cfMap=new java.util.HashMap<>();
            try{for(Field f:BetterPlayerHUDConfig.class.getDeclaredFields()){f.setAccessible(true);cfMap.put(f.getName(),f);}}
            catch(Exception e){}
        }
        return cfMap.get(n);
    }
    private static BetterPlayerHUDConfig cfg(){return BetterPlayerHUD.config;}

    // ═══ 条目构造辅助（反射读写，避免 lambda 编译问题） ═══
    private static ConfigEntry mkBool(String lb,String fn,String tip){
        return new ConfigEntry(ConfigEntry.Type.BOOLEAN,lb,
            ()->{try{return String.valueOf(cf(fn).getBoolean(cfg()));}catch(Exception e){return "false";}},
            (Consumer<Boolean>)v->{try{cf(fn).setBoolean(cfg(),v);}catch(Exception e){}},
            0,0,0,0,null,tip,false,null);
    }
    private static ConfigEntry mkInt(String lb,String fn,int mn,int mx,String tip){
        return new ConfigEntry(ConfigEntry.Type.INT,lb,
            ()->String.valueOf(getIntField(fn)),
            (Consumer<Integer>)v->setIntField(fn,v),
            mn,mx,0,0,null,tip,false,null);
    }
    private static ConfigEntry mkFlt(String lb,String fn,float mn,float mx,String tip){
        return new ConfigEntry(ConfigEntry.Type.FLOAT,lb,
            ()->{try{return String.format("%.1f",cf(fn).getFloat(cfg()));}catch(Exception e){return "0.0";}},
            (Consumer<Float>)v->{try{cf(fn).setFloat(cfg(),v);}catch(Exception e){}},
            0,0,mn,mx,null,tip,false,null);
    }
    private static ConfigEntry mkEnm(String lb,String fn,String[] ev,String tip){
        return new ConfigEntry(ConfigEntry.Type.ENUM,lb,
            ()->{try{Object o=cf(fn).get(cfg());return o==null?"":String.valueOf(o);}catch(Exception e){return "";}},
            (Consumer<String>)v->{try{cf(fn).set(cfg(),v);}catch(Exception e){}},
            0,0,0,0,ev,tip,false,null);
    }
    private static ConfigEntry mkCol(String lb,String fn,boolean argb,String tip){
        return new ConfigEntry(ConfigEntry.Type.COLOR,lb,
            ()->String.format("#%06X",getColorField(fn)&0xFFFFFF),
            (Consumer<Integer>)v->setColorField(fn,v),
            0,0,0,0,null,tip,argb,fn);
    }

    private static ConfigEntry mkCol3(String lb,String fr,String fg,String fb,String tip){
        ConfigEntry e=new ConfigEntry(ConfigEntry.Type.COLOR,lb,
            ()->String.format("#%06X",((getIntField(fr)&0xFF)<<16)|((getIntField(fg)&0xFF)<<8)|(getIntField(fb)&0xFF)),
            (Consumer<Integer>)v->{setIntField(fr,(v>>16)&0xFF);setIntField(fg,(v>>8)&0xFF);setIntField(fb,v&0xFF);},
            0,0,0,0,null,tip,false,fr);
        e.colorKey2=fg;e.colorKey3=fb;return e;
    }

    private static int getIntField(String n){
        try{Field f=cf(n);if(f==null)return 0;
            if(f.getType()==int.class)return f.getInt(cfg());
            return (int)f.getFloat(cfg());   // 兼容 float 字段（如 hitSize）
        }catch(Exception e){return 0;}
    }
    private static void setIntField(String n,int v){
        try{Field f=cf(n);if(f==null)return;
            if(f.getType()==int.class)f.setInt(cfg(),v);
            else f.setFloat(cfg(),(float)v);
        }catch(Exception e){}
    }

    private static int getColorField(String k){
        BetterPlayerHUDConfig c=cfg();if(c==null)return 0xFFFFFFFF;
        try{Field f=cf(k);return f==null?0xFFFFFFFF:f.getInt(c);}catch(Exception e){return 0xFFFFFFFF;}
    }
    private static void setColorField(String k,int v){
        BetterPlayerHUDConfig c=cfg();if(c==null)return;
        try{Field f=cf(k);if(f!=null)f.setInt(c,v);}catch(Exception e){}
    }

    private static int rgb3(ConfigEntry e){
        return ((getIntField(e.colorKey)&0xFF)<<16)|((getIntField(e.colorKey2)&0xFF)<<8)|(getIntField(e.colorKey3)&0xFF);
    }
    private static void setRgb3(ConfigEntry e,int v){
        setIntField(e.colorKey,(v>>16)&0xFF);setIntField(e.colorKey2,(v>>8)&0xFF);setIntField(e.colorKey3,v&0xFF);
    }

    // ═══ 树构建（v1.6.0 全量 139 项，三层树） ═══
    private void buildTree(){
        roots.clear();

        // ── 战斗 ──
        TreeNode combat=new TreeNode("category.combat");combat.dotColor=0xFFE74C3C;combat.expanded=true;roots.add(combat);

        TreeNode crosshair=new TreeNode("module.crosshair");crosshair.dotColor=0xFFE74C3C;crosshair.expanded=true;combat.children.add(crosshair);
        crosshair.entries.add(mkBool("module.crosshair","enableCrosshair",null));
        TreeNode xgen=new TreeNode("crosshair.group.general");crosshair.children.add(xgen);
        xgen.entries.add(mkEnm("crosshair.style","crosshairStyle",new String[]{"default","dot","circle","cross","arrow"},null));
        xgen.entries.add(mkInt("crosshair.gap","crosshairGap",0,20,null));
        xgen.entries.add(mkInt("crosshair.thickness","crosshairThickness",1,10,null));
        xgen.entries.add(mkBool("crosshair.dot","crosshairOutline",null));
        xgen.entries.add(mkBool("crosshair.spread","crosshairSpread",null));
        TreeNode xrgb=new TreeNode("crosshair.group.rgb");crosshair.children.add(xrgb);
        xrgb.entries.add(mkBool("rgb.enable","enableRGBMode",null));
        xrgb.entries.add(mkInt("rgb.speed","rgbSpeed",50,5000,null));
        xrgb.entries.add(mkEnm("rgb.mode","rgbFlowMode",new String[]{"perimeter","uniform"},null));
        xrgb.entries.add(mkEnm("rgb.algorithm","rgbColorAlgo",new String[]{"hsv","sinewave"},null));
        TreeNode xent=new TreeNode("crosshair.group.entity");crosshair.children.add(xent);
        xent.entries.add(mkBool("crosshair.entityColor","crosshairEntityColor",null));
        xent.entries.add(mkInt("crosshair.range","crosshairEntityRange",1,50,null));

        TreeNode hitmarker=new TreeNode("module.hitmarker");hitmarker.dotColor=0xFFE74C3C;hitmarker.expanded=true;combat.children.add(hitmarker);
        TreeNode hmgen=new TreeNode("hitmarker.group.general");hitmarker.children.add(hmgen);
        hmgen.entries.add(mkBool("module.hitmarker","enableHitMarker",null));
        hmgen.entries.add(mkBool("hitmarker.useS19","hitMarkerUseS19",null));
        hmgen.entries.add(mkBool("hitmarker.hitSound","enableHitSounds",null));
        hmgen.entries.add(mkBool("hitmarker.killSound","enableKillSound",null));
        hmgen.entries.add(mkFlt("hitmarker.volume","soundVolume",0.0f,1.0f,null));
        TreeNode hmapp=new TreeNode("hitmarker.group.appearance");hitmarker.children.add(hmapp);
        hmapp.entries.add(mkInt("hitmarker.hitSize","hitSize",2,32,null));
        hmapp.entries.add(mkFlt("hitmarker.hitAlpha","hitAlpha",0.0f,1.0f,null));
        hmapp.entries.add(mkBool("hitmarker.border","hitMarkerEnableBorder",null));
        hmapp.entries.add(mkFlt("hitmarker.borderWidth","hitMarkerBorderWidth",0.5f,5.0f,null));
        hmapp.entries.add(mkFlt("hitmarker.blood","hitBloodIntensity",0.0f,1.0f,null));
        hmapp.entries.add(mkBool("hitmarker.randomRotate","hitMarkerRandomRotate",null));
        hmapp.entries.add(mkFlt("hitmarker.rotateStrength","hitMarkerRandomRotateStrength",0.0f,90.0f,null));

        TreeNode combo=new TreeNode("module.combo");combo.dotColor=0xFFE74C3C;combo.expanded=true;combat.children.add(combo);
        combo.entries.add(mkBool("module.combo","enableCombo",null));
        combo.entries.add(mkInt("combo.xOffset","comboXOffset",-500,500,null));
        combo.entries.add(mkInt("combo.yOffset","comboYOffset",-500,500,null));
        combo.entries.add(mkFlt("combo.scale","comboScale",0.25f,4.0f,null));

        TreeNode target=new TreeNode("module.target");target.dotColor=0xFFE74C3C;target.expanded=true;combat.children.add(target);
        target.entries.add(mkBool("module.target","enableTargetHealth",null));

        TreeNode bol=new TreeNode("module.blockOutline");bol.dotColor=0xFFE74C3C;bol.expanded=true;combat.children.add(bol);
        bol.entries.add(mkBool("blockOutline.enable","enableBlockHighlight",null));
        bol.entries.add(mkFlt("blockOutline.width","blockOutlineWidth",1.0f,8.0f,null));
        bol.entries.add(mkInt("blockOutline.alpha","blockOutlineAlpha",0,255,null));
        bol.entries.add(mkBool("blockOutline.visibleOnly","drawVisibleFacesOnlyBlocks",null));

        TreeNode ehl=new TreeNode("module.entityHL");ehl.dotColor=0xFFE74C3C;ehl.expanded=true;combat.children.add(ehl);
        ehl.entries.add(mkBool("entityHL.enable","enableEntityHighlight",null));
        ehl.entries.add(mkFlt("entityHL.width","entityOutlineWidth",1.0f,8.0f,null));
        ehl.entries.add(mkInt("entityHL.alpha","entityOutlineAlpha",0,255,null));

        TreeNode bb=new TreeNode("module.blockBreak");bb.dotColor=0xFFE74C3C;bb.expanded=true;combat.children.add(bb);
        bb.entries.add(mkBool("blockBreak.enable","enableBlockBreakIndicator",null));
        bb.entries.add(mkInt("blockBreak.x","blockBreakIndicatorX",-200,200,null));
        bb.entries.add(mkInt("blockBreak.y","blockBreakIndicatorY",0,200,null));
        bb.entries.add(mkInt("blockBreak.width","blockBreakIndicatorWidth",10,200,null));
        bb.entries.add(mkInt("blockBreak.height","blockBreakIndicatorHeight",2,20,null));
        bb.entries.add(mkBool("blockBreak.showPercent","blockBreakIndicatorShowPercent",null));

        // ── 信息 ──
        TreeNode info=new TreeNode("category.info");info.dotColor=0xFF3498DB;info.expanded=true;roots.add(info);

        TreeNode compass=new TreeNode("module.compass");compass.dotColor=0xFF3498DB;compass.expanded=true;info.children.add(compass);
        compass.entries.add(mkBool("module.compass","enableCompass",null));
        TreeNode cgen=new TreeNode("compass.group.general");compass.children.add(cgen);
        cgen.entries.add(mkBool("compass.show","showCompassHUD",null));
        cgen.entries.add(mkInt("compass.x","xPosition",-200,2000,null));
        cgen.entries.add(mkInt("compass.y","yPosition",0,2000,null));
        cgen.entries.add(mkFlt("compass.scale","scale",0.5f,3.0f,null));
        cgen.entries.add(mkEnm("compass.style","displayStyle",new String[]{"detailed","minimal","simple"},null));
        TreeNode cdisp=new TreeNode("compass.group.display");compass.children.add(cdisp);
        cdisp.entries.add(mkBool("compass.degreeMarks","showDegreeMarks",null));
        cdisp.entries.add(mkBool("compass.needle","showCompassNeedle",null));
        cdisp.entries.add(mkInt("compass.interval","degreeMarkInterval",5,90,null));
        cdisp.entries.add(mkBool("compass.dynamicScale","dynamicScaling",null));
        cdisp.entries.add(mkBool("compass.horizon","showHorizon",null));
        cdisp.entries.add(mkBool("compass.exactAngle","showExactAngle",null));

        TreeNode health=new TreeNode("module.health");health.dotColor=0xFF3498DB;health.expanded=true;info.children.add(health);
        health.entries.add(mkBool("module.health","enableHealthHUD",null));
        TreeNode hlay=new TreeNode("health.group.layout");health.children.add(hlay);
        hlay.entries.add(mkInt("health.x","healthHudX",-500,2000,null));
        hlay.entries.add(mkInt("health.y","healthHudY",-500,500,null));
        hlay.entries.add(mkBool("health.showArmor","showArmorHUD",null));
        TreeNode helem=new TreeNode("health.group.elements");health.children.add(helem);
        helem.entries.add(mkBool("health.showHunger","showHungerHUD",null));
        helem.entries.add(mkBool("health.showHead","showPlayerHead",null));
        helem.entries.add(mkInt("health.headSize","headSize",8,64,null));
        helem.entries.add(mkInt("health.textSpacing","headTextSpacing",0,20,null));

        TreeNode dist=new TreeNode("module.distance");dist.dotColor=0xFF3498DB;dist.expanded=true;info.children.add(dist);
        dist.entries.add(mkBool("module.distance","enableDistanceHUD",null));
        TreeNode dcoord=new TreeNode("distance.group.coords");dist.children.add(dcoord);
        dcoord.entries.add(mkInt("dist.x","distanceHudX",0,2000,null));
        dcoord.entries.add(mkInt("dist.y","distanceHudY",0,2000,null));
        dcoord.entries.add(mkInt("dist.precision","distancePrecision",0,5,null));
        TreeNode ddisp=new TreeNode("distance.group.display");dist.children.add(ddisp);
        ddisp.entries.add(mkBool("dist.showTarget","showTargetInfo",null));
        ddisp.entries.add(mkBool("dist.showBlockInfo","showBlockCoordinates",null));
        ddisp.entries.add(mkBool("dist.hardness","showBlockHardness",null));
        ddisp.entries.add(mkBool("dist.showTool","showRequiredTool",null));

        TreeNode keys=new TreeNode("module.keys");keys.dotColor=0xFF3498DB;keys.expanded=true;info.children.add(keys);
        keys.entries.add(mkInt("keys.x","keysDisplayX",0,2000,null));
        keys.entries.add(mkInt("keys.y","keysDisplayY",0,2000,null));
        keys.entries.add(mkInt("keys.size","keysSize",12,64,null));
        keys.entries.add(mkInt("keys.spacing","keysSpacing",0,32,null));
        keys.entries.add(mkFlt("keys.scale","keysScale",0.25f,4.0f,null));
        keys.entries.add(mkBool("keys.background","showKeysBackground",null));
        keys.entries.add(mkFlt("keys.opacity","keysOpacity",0.0f,1.0f,null));
        keys.entries.add(mkBool("module.keys","enableKeysDisplay",null));

        TreeNode chrom=new TreeNode("module.chroma");chrom.dotColor=0xFF3498DB;chrom.expanded=true;info.children.add(chrom);
        chrom.entries.add(mkBool("module.chroma","enableChromaChat",null));
        chrom.entries.add(mkInt("chroma.width","chromaChatWidth",80,800,null));
        chrom.entries.add(mkInt("chroma.lines","chromaChatLineCount",1,50,null));
        chrom.entries.add(mkBool("chroma.timestamp","chromaChatShowTimestamps",null));
        chrom.entries.add(mkBool("chroma.avatar","chromaChatAvatar",null));
        chrom.entries.add(mkBool("chroma.signboard","chromaChatSignboardMerge",null));
        chrom.entries.add(mkInt("chroma.maxLines","chromaChatMaxLines",20,500,null));

        TreeNode perf=new TreeNode("module.performance");perf.dotColor=0xFF3498DB;perf.expanded=true;info.children.add(perf);
        perf.entries.add(mkBool("module.performance","enablePerformanceHUD",null));

        TreeNode tab=new TreeNode("module.tablist");tab.dotColor=0xFF3498DB;tab.expanded=true;info.children.add(tab);
        tab.entries.add(mkBool("module.tablist","enableTabListHUD",null));

        TreeNode dbg=new TreeNode("module.debug");dbg.dotColor=0xFF3498DB;dbg.expanded=true;info.children.add(dbg);
        dbg.entries.add(mkBool("module.debug","enableDebugInfoHUD",null));
        dbg.entries.add(mkInt("debug.xOffset","debugInfoXOffset",0,2000,null));
        dbg.entries.add(mkInt("debug.yOffset","debugInfoYOffset",0,2000,null));

        // ── 状态 ──
        TreeNode status=new TreeNode("category.status");status.dotColor=0xFF2ECC71;status.expanded=true;roots.add(status);

        TreeNode xp=new TreeNode("module.expbar");xp.dotColor=0xFF2ECC71;xp.expanded=true;status.children.add(xp);
        xp.entries.add(mkBool("module.expbar","enableXpBarHUD",null));
        xp.entries.add(mkInt("xpbar.offsetX","xpBarXOffset",-500,500,null));
        xp.entries.add(mkInt("xpbar.offsetY","xpBarYOffset",-500,500,null));
        xp.entries.add(mkInt("xpbar.width","xpBarWidth",80,400,null));
        xp.entries.add(mkInt("xpbar.height","xpBarHeight",3,20,null));

        TreeNode boss=new TreeNode("module.bossHP");boss.dotColor=0xFF2ECC71;boss.expanded=true;status.children.add(boss);
        boss.entries.add(mkBool("module.bossHP","enableBossHealthHUD",null));

        TreeNode hotbar=new TreeNode("module.hotbar");hotbar.dotColor=0xFF2ECC71;hotbar.expanded=true;status.children.add(hotbar);
        hotbar.entries.add(mkBool("module.hotbar","enableHotbarHUD",null));

        TreeNode potion=new TreeNode("module.potion");potion.dotColor=0xFF2ECC71;potion.expanded=true;status.children.add(potion);
        TreeNode ptimer=new TreeNode("potion.group.timer");potion.children.add(ptimer);
        ptimer.entries.add(mkBool("module.potiontimer","enablePotionTimer",null));
        ptimer.entries.add(mkInt("potionTimer.xOffset","potionTimerXOffset",-500,500,null));
        ptimer.entries.add(mkInt("potionTimer.yOffset","potionTimerYOffset",-500,500,null));
        ptimer.entries.add(mkInt("potionTimer.iconSize","potionTimerIconSize",16,64,null));
        TreeNode peff=new TreeNode("potion.group.effects");potion.children.add(peff);
        peff.entries.add(mkBool("module.potion","enablePotionHUD",null));
        peff.entries.add(mkInt("potion.xOffset","potionXOffset",-500,500,null));
        peff.entries.add(mkInt("potion.yOffset","potionYOffset",-500,500,null));

        TreeNode crisis=new TreeNode("module.crisis");crisis.dotColor=0xFF2ECC71;crisis.expanded=true;status.children.add(crisis);
        crisis.entries.add(mkBool("module.crisis","enableCrisisWarning",null));
        crisis.entries.add(mkBool("crisis.warnHealth","crisisWarnHealth",null));
        crisis.entries.add(mkFlt("crisis.healthThreshold","crisisHealthThreshold",0.5f,20.0f,null));
        crisis.entries.add(mkBool("crisis.warnHunger","crisisWarnHunger",null));
        crisis.entries.add(mkInt("crisis.hungerThreshold","crisisHungerThreshold",1,20,null));
        crisis.entries.add(mkBool("crisis.warnTnt","crisisWarnTnt",null));
        crisis.entries.add(mkFlt("crisis.tntRadius","crisisTntRadius",2.0f,50.0f,null));
        crisis.entries.add(mkInt("crisis.xOffset","crisisXOffset",-500,500,null));
        crisis.entries.add(mkInt("crisis.yOffset","crisisYOffset",-500,500,null));
        crisis.entries.add(mkInt("crisis.iconSize","crisisIconSize",12,64,null));

        TreeNode jumpbar=new TreeNode("module.jumpbar");jumpbar.dotColor=0xFF2ECC71;jumpbar.expanded=true;status.children.add(jumpbar);
        jumpbar.entries.add(mkBool("module.jumpbar","enableJumpBar",null));

        TreeNode airhud=new TreeNode("module.airhud");airhud.dotColor=0xFF2ECC71;airhud.expanded=true;status.children.add(airhud);
        airhud.entries.add(mkBool("module.airhud","enableAirHUD",null));

        TreeNode mount=new TreeNode("module.mountHP");mount.dotColor=0xFF2ECC71;mount.expanded=true;status.children.add(mount);
        mount.entries.add(mkBool("module.mountHP","enableMountHP",null));

        // ── 颜色 ──
        TreeNode colors=new TreeNode("category.colors");colors.dotColor=0xFFF39C12;colors.expanded=true;roots.add(colors);
        colors.entries.add(mkCol("color.healthSafe","healthColorSafe",true,null));
        colors.entries.add(mkCol("color.healthWarning","healthColorWarning",true,null));
        colors.entries.add(mkCol("color.healthDanger","healthColorDanger",true,null));
        colors.entries.add(mkCol("color.armor","armorColor",true,null));
        colors.entries.add(mkCol("color.hunger","hungerColor",true,null));
        colors.entries.add(mkCol("color.compass","compassColor",true,null));
        colors.entries.add(mkCol("color.distance","distanceColor",true,null));
        colors.entries.add(mkCol("color.targetInfo","targetInfoColor",true,null));
        colors.entries.add(mkCol("color.blockInfo","blockInfoColor",true,null));
        colors.entries.add(mkCol("color.keysActive","keysActiveColor",true,null));
        colors.entries.add(mkCol("color.keysInactive","keysInactiveColor",true,null));
        colors.entries.add(mkCol("color.keysText","keysTextColor",true,null));
        colors.entries.add(mkCol("color.potionText","potionTextColor",true,null));
        colors.entries.add(mkCol("color.blockOutline","blockOutlineColor",true,null));
        colors.entries.add(mkCol("color.entityHostile","entityOutlineColorHostile",true,null));
        colors.entries.add(mkCol("color.entityNeutral","entityOutlineColorNeutral",true,null));
        colors.entries.add(mkCol("color.entityFriendly","entityOutlineColorFriendly",true,null));
        colors.entries.add(mkCol("color.crosshair","crosshairColor",true,null));
        colors.entries.add(mkCol("color.crosshairPlayer","crosshairColorPlayer",true,null));
        colors.entries.add(mkCol("color.crosshairHostile","crosshairColorHostile",true,null));
        colors.entries.add(mkCol("color.crosshairPassive","crosshairColorPassive",true,null));
        colors.entries.add(mkCol("color.crosshairNeutral","crosshairColorNeutral",true,null));
        colors.entries.add(mkCol("color.crosshairOther","crosshairColorOther",true,null));
        colors.entries.add(mkCol("color.hitMarker","hitColor",true,null));
        colors.entries.add(mkCol("color.killMarker","killColor",true,null));
        colors.entries.add(mkCol("color.breakStart","blockBreakIndicatorColorStart",true,null));
        colors.entries.add(mkCol("color.breakEnd","blockBreakIndicatorColorEnd",true,null));
    }

    private void selectFirst(){
        if(!roots.isEmpty()){
            selectedNode=roots.get(0);
            if(!selectedNode.children.isEmpty())selectedNode=selectedNode.children.get(0);
        }
    }

    private void selectNode(TreeNode n){selectedNode=n;entryScroll=0;}

    // ═══ 本地化（统一走 LangManager：F9 游戏内一键切换中英） ═══
    private String tr(String k){return LangManager.get(k);}
    private String tipOf(String key){String t=LangManager.get(key+".tooltip");return t.equals(key+".tooltip")?null:t;}
    private String onOff(boolean on){return LangManager.get(on?"common.on":"common.off");}

    @Override public void initGui(){Keyboard.enableRepeatEvents(true);}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);if(cfg()!=null)cfg().saveConfig();}

    @Override
    public void drawScreen(int mx,int my,float pt){
        panel();
        fillR(0,0,width,height,0x66000000);          // 半透明遮罩（游戏世界可见）
        fillR(px,py,px+pw,py+ph,C_BG);               // 面板底
        drawHeader(mx,my); drawLeftTree(mx,my); drawRightCards(mx,my); drawFooter(mx,my);
        fillR(px,py,px+pw,py+1,C_ACCENT); fillR(px,py+ph-1,px+pw,py+ph,C_ACCENT);
        fillR(px,py,px+1,py+ph,C_ACCENT); fillR(px+pw-1,py,px+pw,py+ph,C_ACCENT);
        super.drawScreen(mx,my,pt);
    }

    private void drawHeader(int mx,int my){
        fillR(px,py,px+pw,py+HEADER_H,C_HEADER); fillR(px,py+HEADER_H-1,px+pw,py+HEADER_H,C_DIVIDER);
        fr().drawStringWithShadow("←",px+8,py+10,0xFFFFFF);
        fr().drawStringWithShadow(tr("gui.modules"),px+26,py+11,0xFFFFFF);
        int sx=px+pw-96; fillR(sx,py+8,sx+80,py+28,C_SEARCH_BG); fillR(sx,py+8,sx+80,py+28,C_DIVIDER);
        fr().drawString(searchQuery.isEmpty()?"§7"+tr("gui.search_hint"):searchQuery,sx+6,py+13,0xFFFFFF);
        if(!searchQuery.isEmpty()){
            boolean cx=mx>=sx+82&&mx<=sx+92&&my>=py+8&&my<=py+28;
            fillR(sx+82,py+8,sx+92,py+28,cx?0xFF3A3A4E:C_SEARCH_BG);
            fr().drawStringWithShadow("✕",sx+86,py+13,cx?0xFFFFFFFF:0xFF888888);
        }
    }

    private void drawLeftTree(int mx,int my){
        int x0=px,y0=py+HEADER_H,tw=LEFT_W,th=ph-HEADER_H-FOOTER_H;
        fillR(x0,y0,x0+tw,y0+th,C_PANEL); fillR(x0+tw-1,y0,x0+tw,y0+th,C_DIVIDER);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr=new ScaledResolution(mc); int sf=sr.getScaleFactor();
        GL11.glScissor(x0*sf,(mc.displayHeight-(y0+th)*sf),tw*sf,th*sf);
        int y=y0+4-treeScroll;
        for(TreeNode cat:roots){
            boolean ch=mx>=x0+8&&mx<=x0+tw-8&&my>=y&&my<=y+22;
            fillR(x0+4,y,x0+tw-4,y+22,ch?C_CARD_HOVER:C_PANEL);
            fillR(x0+6,y+7,x0+12,y+15,cat.dotColor);
            fr().drawStringWithShadow(cat.expanded?"▼":"▶",x0+14,y+5,0xFF666666);
            fr().drawStringWithShadow("§l"+tr(cat.label),x0+22,y+5,0xFFFFFF);
            y+=24; if(!cat.expanded)continue;
            for(TreeNode mod:cat.children){
                boolean sel=selectedNode==mod,hov=mx>=x0+20&&mx<=x0+tw-4&&my>=y&&my<=y+20;
                if(hov||sel)fillR(x0+18,y,x0+tw-4,y+20,sel?C_SEL_ACCENT:0xFF2A2A4E);
                fr().drawStringWithShadow(tr(mod.label),x0+22,y+4,sel?0xFFFFFF:(hov?C_TEXT:C_TEXT_DIM));
                if(sel)fillR(x0+18,y+2,x0+19,y+18,C_ACCENT);
                y+=21;
                for(TreeNode grp:mod.children){
                    boolean gh=mx>=x0+32&&mx<=x0+tw-4&&my>=y&&my<=y+18;
                    fillR(x0+30,y,x0+tw-4,y+18,gh?0xFF2A2A4E:C_PANEL);
                    fr().drawStringWithShadow(grp.expanded?"▾":"▸",x0+32,y+3,0xFF666666);
                    fr().drawStringWithShadow(tr(grp.label),x0+44,y+3,C_TEXT_DIM);
                    y+=19;
                }
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawRightCards(int mx,int my){
        int x0=px+LEFT_W,y0=py+HEADER_H,cw=pw-LEFT_W,ch=ph-HEADER_H-FOOTER_H;
        hoveredEntry=null; if(selectedNode==null)return;
        fillR(x0,y0,px+pw,y0+ch,C_BG);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr=new ScaledResolution(mc); int sf=sr.getScaleFactor();
        GL11.glScissor(x0*sf,(mc.displayHeight-(y0+ch)*sf),(px+pw-x0)*sf,ch*sf);
        int cardY=y0+4-entryScroll,vx=x0+6;
        fr().drawStringWithShadow("§l"+tr(selectedNode.label),vx,cardY,C_ACCENT); cardY+=18;
        boolean searching=!searchQuery.isEmpty();
        String q=searchQuery.toLowerCase();
        int cw2=cw-12;
        // 隐式条目：直接挂在节点上（无子组）
        if(!selectedNode.entries.isEmpty()){
            for(ConfigEntry e:selectedNode.entries){
                if(searching&&!tr(e.label).toLowerCase().contains(q))continue;
                cardY=renderEntry(e,vx,cardY,cw2,mx,my);
            }
            fillR(vx+2,cardY-1,vx+cw2-2,cardY,C_DIVIDER);
            cardY+=4;
        }
        for(TreeNode grp:selectedNode.children){
            if(!grp.expanded||grp.entries.isEmpty())continue;
            int shown=0;
            for(ConfigEntry e:grp.entries){
                if(searching&&!tr(e.label).toLowerCase().contains(q))continue;
                shown++;
            }
            if(searching&&shown==0)continue;
            String gn=tr(grp.label); int gw=fr().getStringWidth(gn);
            fillR(vx,cardY,vx+gw+16,cardY+16,C_ACCENT);
            fr().drawStringWithShadow(gn,vx+8,cardY+4,0xFFFFFF); cardY+=20;
            for(ConfigEntry e:grp.entries){
                if(searching&&!tr(e.label).toLowerCase().contains(q))continue;
                cardY=renderEntry(e,vx,cardY,cw2,mx,my);
            }
            fillR(vx+2,cardY-1,vx+cw2-2,cardY,C_DIVIDER);
            cardY+=4;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** 渲染单个条目卡片，返回下一个 cardY */
    private int renderEntry(ConfigEntry e,int vx,int cardY,int cw2,int mx,int my){
        boolean hover=mx>=vx+40&&mx<=vx+cw2-4&&my>=cardY&&my<=cardY+ENTRY_H;
        fillR(vx+2,cardY,vx+cw2-2,cardY+ENTRY_H,hover?C_CARD_HOVER:C_CARD);
        fr().drawStringWithShadow(tr(e.label),vx+8,cardY+6,C_TEXT);
        if(hover)hoveredEntry=e;
        switch(e.type){
            case BOOLEAN:drawToggle(e,vx,cardY,mx,my);break;
            case INT:drawSpinner(e,vx,cardY,mx,my,false);break;
            case FLOAT:drawSpinner(e,vx,cardY,mx,my,true);break;
            case COLOR:drawColorPreview(e,vx,cardY,mx,my);break;
            case ENUM:drawEnumCycle(e,vx,cardY,mx,my);break;
        }
        fillR(vx+4,cardY+ENTRY_H-1,vx+cw2-4,cardY+ENTRY_H-1,C_DIVIDER);
        e.x=vx+2;e.y=cardY;e.w=cw2-4;e.h=ENTRY_H;
        return cardY+ENTRY_H;
    }

    private void drawFooter(int mx,int my){
        int fy=py+ph-FOOTER_H; fillR(px,fy,px+pw,py+ph,C_HEADER); fillR(px,fy,px+pw,fy+1,C_DIVIDER);
        String tip=hoveredEntry!=null?tipOf(hoveredEntry.label):null;
        if(tip!=null){
            int mw=pw-40;
            while(fr().getStringWidth(tip+"...")>mw&&tip.length()>2)tip=tip.substring(0,tip.length()-1);
            fr().drawStringWithShadow("§7"+tip,px+8,fy+7,C_TEXT_DIM);
        }
        if(pulseLabel!=null&&System.currentTimeMillis()<pulseEnd){
            long left=pulseEnd-System.currentTimeMillis();
            float p=left/400f; if(p>1)p=1;
            int alpha=(int)(120+135*p);
            fr().drawStringWithShadow("✓ "+tr(pulseLabel),px+pw-150,fy+7,0xFFFFFF|(alpha<<24));
        }
    }

    private void drawToggle(ConfigEntry e,int vx,int cy,int mx,int my){
        boolean on="true".equals(e.getter.get());
        int bx=vx+130,by=cy+3,tw=40,th=16;
        fillR(bx,by,bx+tw,by+th,on?C_TOGGLE_ON:C_TOGGLE_OFF);
        fillR(bx,by,bx+tw,by+th,0x44000000);
        int kx=on?bx+tw-16:bx; fillR(kx+2,by+2,kx+14,by+14,0xFFFFFFFF);
        fr().drawStringWithShadow(onOff(on),bx+tw+6,cy+6,on?C_TOGGLE_ON:C_TEXT_DIM);
    }

    private void drawSpinner(ConfigEntry e,int vx,int cy,int mx,int my,boolean isFloat){
        String val=e.getter.get(); int tx=vx+130,bw=16;
        fillR(tx,cy+3,tx+bw,cy+19,C_CARD_HOVER); fillR(tx,cy+3,tx+bw,cy+19,C_DIVIDER);
        fr().drawStringWithShadow("−",tx+4,cy+7,C_TEXT);
        fr().drawStringWithShadow(val,tx+bw+8,cy+6,0xFFFFFF);
        int px=tx+bw+8+fr().getStringWidth(val)+8;
        fillR(px,cy+3,px+bw,cy+19,C_CARD_HOVER); fillR(px,cy+3,px+bw,cy+19,C_DIVIDER);
        fr().drawStringWithShadow("+",px+4,cy+7,C_TEXT);
    }

    private void drawColorPreview(ConfigEntry e,int vx,int cy,int mx,int my){
        int color=e.colorKey2==null?getColorField(e.colorKey):rgb3(e); int px=vx+130,sz=14;
        fillR(px-1,cy+3,px+sz+1,cy+5+sz,0xFF000000);
        fillR(px,cy+4,px+sz,cy+4+sz,color);
        fr().drawStringWithShadow(String.format("#%06X",color&0xFFFFFF),px+sz+8,cy+6,0xFFFFFF);
    }

    private void drawEnumCycle(ConfigEntry e,int vx,int cy,int mx,int my){
        String val=e.getter.get(); int tx=vx+130,bw=16;
        fillR(tx,cy+3,tx+bw,cy+19,C_CARD_HOVER);
        fr().drawStringWithShadow("◀",tx+4,cy+7,C_TEXT);
        fr().drawStringWithShadow(val,tx+bw+8,cy+6,0xFFFFFF);
        int px=tx+bw+8+fr().getStringWidth(val)+8;
        fillR(px,cy+3,px+bw,cy+19,C_CARD_HOVER);
        fr().drawStringWithShadow("▶",px+4,cy+7,C_TEXT);
    }

    @Override
    protected void mouseClicked(int mx,int my,int btn){
        if(btn!=0)return;
        if(mx<px||mx>px+pw||my<py||my>py+ph)return;                 // 面板外不响应
        if(mx>=px+4&&mx<=px+24&&my>=py+4&&my<=py+30){mc.displayGuiScreen(parent);return;}
        int sx=px+pw-96;
        if(!searchQuery.isEmpty()&&mx>=sx+82&&mx<=sx+92&&my>=py+8&&my<=py+28){searchQuery="";return;}
        int y=py+HEADER_H+4-treeScroll;
        for(TreeNode cat:roots){
            if(mx>=px+8&&mx<=px+LEFT_W-8&&my>=y&&my<=y+22){cat.expanded=!cat.expanded;return;}
            y+=24;if(!cat.expanded)continue;
            for(TreeNode mod:cat.children){
                if(mx>=px+20&&mx<=px+LEFT_W-4&&my>=y&&my<=y+20){selectNode(mod);return;}
                y+=21;
                for(TreeNode grp:mod.children){
                    if(mx>=px+32&&mx<=px+LEFT_W-4&&my>=y&&my<=y+18){grp.expanded=!grp.expanded;selectNode(mod);return;}
                    y+=19;
                }
            }
        }
        if(selectedNode==null)return;
        int vx=px+LEFT_W+6,cardY=py+HEADER_H+4-entryScroll+18;   // 与 drawRightCards 同偏移（标题 18px）
        boolean searching=!searchQuery.isEmpty();
        String q=searchQuery.toLowerCase();
        int cw=pw-LEFT_W-12;
        // 隐式条目（直挂节点）
        for(ConfigEntry e:selectedNode.entries){
            if(searching&&!tr(e.label).toLowerCase().contains(q))continue;
            if(my>=cardY&&my<=cardY+ENTRY_H&&mx>=vx+2){handleClick(e,mx,my,vx,cardY,cw);return;}
            cardY+=ENTRY_H;
        }
        if(!selectedNode.entries.isEmpty())cardY+=4;
        for(TreeNode grp:selectedNode.children){
            if(!grp.expanded)continue;cardY+=20;
            for(ConfigEntry e:grp.entries){
                if(searching&&!tr(e.label).toLowerCase().contains(q))continue;
                if(my>=cardY&&my<=cardY+ENTRY_H&&mx>=vx+2){handleClick(e,mx,my,vx,cardY,cw);return;}
                cardY+=ENTRY_H;
            }cardY+=4;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleClick(ConfigEntry e,int mx,int my,int vx,int cy,int cw){
        if(cfg()==null)return;
        switch(e.type){
            case BOOLEAN:{ boolean v="true".equals(e.getter.get());
                ((Consumer<Boolean>)e.setter).accept(!v);pls(e.label,400);break;}
            case INT:{int v=Integer.parseInt(e.getter.get()),tx=vx+130,bw=16;
                if(mx>=tx&&mx<=tx+bw&&my>=cy+3&&my<=cy+19)((Consumer<Integer>)e.setter).accept(Math.max(e.minInt,v-1));
                else{int px=tx+bw+8+fr().getStringWidth(String.valueOf(v))+8;
                    if(mx>=px&&mx<=px+bw&&my>=cy+3&&my<=cy+19)((Consumer<Integer>)e.setter).accept(Math.min(e.maxInt,v+1));}
                pls(e.label,300);break;}
            case FLOAT:{float v=Float.parseFloat(e.getter.get());int tx=vx+130,bw=16;
                if(mx>=tx&&mx<=tx+bw&&my>=cy+3&&my<=cy+19)((Consumer<Float>)e.setter).accept(Math.max(e.minFlt,v-1f));
                else{int px=tx+bw+8+fr().getStringWidth(String.format("%.1f",v))+8;
                    if(mx>=px&&mx<=px+bw&&my>=cy+3&&my<=cy+19)((Consumer<Float>)e.setter).accept(Math.min(e.maxFlt,v+1f));}
                pls(e.label,300);break;}
            case ENUM:{String cur=e.getter.get();int idx=-1;
                for(int i=0;i<e.enumValues.length;i++)if(e.enumValues[i].equals(cur)){idx=i;break;}
                int tx=vx+130,bw=16;
                if(mx>=tx&&mx<=tx+bw)idx=(idx<=0)?e.enumValues.length-1:idx-1;
                else{int px=tx+bw+8+fr().getStringWidth(cur)+8;
                    if(mx>=px&&mx<=px+bw)idx=(idx>=e.enumValues.length-1)?0:idx+1;}
                if(idx>=0)((Consumer<String>)e.setter).accept(e.enumValues[idx]);
                pls(e.label,300);break;}
            case COLOR:{mc.displayGuiScreen(new GuiColorPicker(this,()->e.colorKey2==null?getColorField(e.colorKey):rgb3(e),v->{if(e.colorKey2==null)setColorField(e.colorKey,v);else setRgb3(e,v);}));break;}
        }
    }

    private void pls(String lb,long ms){pulseLabel=lb;pulseEnd=System.currentTimeMillis()+ms;}

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();int wheel=Mouse.getDWheel();if(wheel==0)return;
        int mx=Mouse.getX()*width/mc.displayWidth;
        int my=height-Mouse.getY()*height/mc.displayHeight-1;
        if(mx<px+LEFT_W&&mx>px)treeScroll=Math.max(0,treeScroll-wheel/120*20);
        else if(my>py+FOOTER_H&&my<py+ph)entryScroll=Math.max(0,entryScroll-wheel/120*ENTRY_H);
    }

    @Override
    protected void keyTyped(char c,int code){
        if(code==Keyboard.KEY_ESCAPE){
            if(!searchQuery.isEmpty()){searchQuery="";return;}   // 先清搜索，再关界面
            mc.displayGuiScreen(parent);return;
        }
        if(code==Keyboard.KEY_BACK){if(!searchQuery.isEmpty())searchQuery=searchQuery.substring(0,searchQuery.length()-1);return;}
        if(code==Keyboard.KEY_F8)return;
        if(Character.isLetterOrDigit(c)||c==' '||c=='.'||c=='-')searchQuery+=c;
    }

    private net.minecraft.client.gui.FontRenderer fr(){return mc.fontRendererObj;}

    private void fillR(int l,int t,int r,int b,int c){
        if(l>=r||t>=b)return;
        Tessellator tsl=Tessellator.getInstance();WorldRenderer wr=tsl.getWorldRenderer();
        GlStateManager.enableBlend();GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770,771,1,0);
        float a=(float)(c>>24&255)/255f,rc=(float)(c>>16&255)/255f,g=(float)(c>>8&255)/255f,bl=(float)(c&255)/255f;
        wr.begin(7,DefaultVertexFormats.POSITION_COLOR);
        wr.pos(l,b,0).color(rc,g,bl,a).endVertex();
        wr.pos(r,b,0).color(rc,g,bl,a).endVertex();
        wr.pos(r,t,0).color(rc,g,bl,a).endVertex();
        wr.pos(l,t,0).color(rc,g,bl,a).endVertex();
        tsl.draw();GlStateManager.enableTexture2D();GlStateManager.disableBlend();
    }
}

