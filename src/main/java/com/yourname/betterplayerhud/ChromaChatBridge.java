package com.yourname.betterplayerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;

import java.util.List;

/**
 * ChromaChat 桥接层 — 包裹当前聊天框实例（原版 GuiNewChat 或 BetterChat），
 * 拦截 printChatMessage / printChatMessageWithOptionalDeletion 转发给
 * ChromaChatManager，补全本地指令消息的捕捉路径。
 *
 * 所有其他方法直接委托给被包裹实例，不改变原有行为。
 */
@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
public class ChromaChatBridge extends GuiNewChat {

    private final GuiNewChat wrapped;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public ChromaChatBridge(GuiNewChat wrapped) {
        super(mc);
        this.wrapped = wrapped;
    }

    /** 返回被包裹的真实实例（BetterChat 或原版） */
    public GuiNewChat getWrapped() {
        return wrapped;
    }

    // ═══════════════════════════════════════════════════════════════
    //  拦截两个消息入口
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void printChatMessage(IChatComponent component) {
        ChromaChatManager.onLocalMessage(component);
        wrapped.printChatMessage(component);
    }

    @Override
    public void printChatMessageWithOptionalDeletion(IChatComponent component, int id) {
        ChromaChatManager.onLocalMessage(component);
        wrapped.printChatMessageWithOptionalDeletion(component, id);
    }

    // ═══════════════════════════════════════════════════════════════
    //  以下全部委托给 wrapped
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void drawChat(int updateCounter) {
        wrapped.drawChat(updateCounter);
    }

    @Override
    public void clearChatMessages() {
        wrapped.clearChatMessages();
    }

    @Override
    public boolean getChatOpen() {
        return wrapped.getChatOpen();
    }

    @Override
    public IChatComponent getChatComponent(int mouseX, int mouseY) {
        return wrapped.getChatComponent(mouseX, mouseY);
    }

    @Override
    public List<String> getSentMessages() {
        return wrapped.getSentMessages();
    }

    @Override
    public void addToSentMessages(String message) {
        wrapped.addToSentMessages(message);
    }

    @Override
    public void resetScroll() {
        wrapped.resetScroll();
    }

    @Override
    public void scroll(int scrollAmount) {
        wrapped.scroll(scrollAmount);
    }

    @Override
    public int getLineCount() {
        return wrapped.getLineCount();
    }

    @Override
    public void deleteChatLine(int id) {
        wrapped.deleteChatLine(id);
    }

    @Override
    public void refreshChat() {
        wrapped.refreshChat();
    }
}
