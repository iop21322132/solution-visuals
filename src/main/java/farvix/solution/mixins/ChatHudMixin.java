package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.api.util.FriendManager;
import farvix.solution.client.modules.impl.environment.Streamer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.client.modules.impl.visuals.NoChat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Shadow
    @Final
    private java.util.List<net.minecraft.client.gui.hud.ChatHudLine.Visible> visibleMessages;

    private java.util.List<net.minecraft.client.gui.hud.ChatHudLine.Visible> originalVisibleMessages = null;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        try {
            NoChat noChat = Client.getInstance().getModuleManager().get(NoChat.class);
            if (noChat != null && noChat.shouldHideChat()) {
                // Save original list
                originalVisibleMessages = new java.util.ArrayList<>(visibleMessages);
                // Filter the list to only contain mod messages
                visibleMessages.removeIf(line -> {
                    if (line == null || line.content() == null) return true;
                    StringBuilder sb = new StringBuilder();
                    line.content().accept((index, style, codePoint) -> {
                        sb.appendCodePoint(codePoint);
                        return true;
                    });
                    String text = sb.toString();
                    return !text.contains("Solution Visual") && !text.contains("SolutionVisual");
                });
            }
        } catch (Throwable ignored) {}
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        try {
            if (originalVisibleMessages != null) {
                visibleMessages.clear();
                visibleMessages.addAll(originalVisibleMessages);
                originalVisibleMessages = null;
            }
        } catch (Throwable ignored) {}
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true, index = 1, require = 0)
    private Text replaceMessage(Text message) {
        if (message == null) return null;
        try {
            // Streamer замена
            Streamer s = Client.getInstance().getModuleManager().get(Streamer.class);
            if (s != null && s.isEnabled()) {
                String realNick = net.minecraft.client.MinecraftClient.getInstance().getSession() != null
                        ? net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername() : null;
                if (realNick != null && message.getString().toLowerCase().contains(realNick.toLowerCase())) {
                    MutableText result = Text.empty();
                    message.visit((style, str) -> {
                        String replaced = str.replaceAll("(?i)" + java.util.regex.Pattern.quote(realNick), s.alias.getText());
                        result.append(Text.literal(replaced).setStyle(style));
                        return java.util.Optional.empty();
                    }, Style.EMPTY);
                    message = result;
                }
            }

            // Подсветка друзей в чате
            final Text finalMsg = message;
            String msgStr = finalMsg.getString();
            for (String friendName : FriendManager.getFriends()) {
                if (msgStr.contains(friendName)) {
                    MutableText result = Text.empty();
                    finalMsg.visit((style, str) -> {
                        if (str.contains(friendName)) {
                            int idx = str.indexOf(friendName);
                            if (idx > 0) result.append(Text.literal(str.substring(0, idx)).setStyle(style));
                            Style friendStyle = style.withColor(TextColor.fromRgb(0x55FF55)).withBold(true);
                            result.append(Text.literal(friendName).setStyle(friendStyle));
                            if (idx + friendName.length() < str.length())
                                result.append(Text.literal(str.substring(idx + friendName.length())).setStyle(style));
                        } else {
                            result.append(Text.literal(str).setStyle(style));
                        }
                        return java.util.Optional.empty();
                    }, Style.EMPTY);
                    return result;
                }
            }
        } catch (Throwable ignored) {}
        return message;
    }
}
