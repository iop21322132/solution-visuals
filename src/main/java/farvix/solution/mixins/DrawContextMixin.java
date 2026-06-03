package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.environment.Streamer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public class DrawContextMixin {

    @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private Text replaceText(Text text) {
        return rep(text);
    }

    @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private OrderedText replaceOrdered(OrderedText text) {
        if (text == null) return null;
        try {
            Streamer s = Client.getInstance().getModuleManager().get(Streamer.class);
            if (s == null || !s.isEnabled()) return text;
            StringBuilder sb = new StringBuilder();
            text.accept((i, style, cp) -> { sb.appendCodePoint(cp); return true; });
            String orig = sb.toString();
            String replaced = s.replace(orig);
            if (!replaced.equals(orig)) return Text.literal(replaced).asOrderedText();
        } catch (Throwable ignored) {}
        return text;
    }

    @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private Text replaceDrawText(Text text) {
        return rep(text);
    }

    @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private String replaceDrawString(String text) {
        if (text == null) return null;
        try {
            Streamer s = Client.getInstance().getModuleManager().get(Streamer.class);
            if (s != null) return s.replace(text);
        } catch (Throwable ignored) {}
        return text;
    }

    private static Text rep(Text text) {
        if (text == null) return null;
        try {
            Streamer s = Client.getInstance().getModuleManager().get(Streamer.class);
            if (s == null || !s.isEnabled()) return text;
            String realNick = net.minecraft.client.MinecraftClient.getInstance().getSession() != null
                    ? net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername() : null;
            if (realNick == null) return text;
            String orig = text.getString();
            if (!orig.toLowerCase().contains(realNick.toLowerCase())) return text;
            // Replace preserving style by visiting siblings
            return replaceInText(text, realNick, s.alias.getText());
        } catch (Throwable ignored) {}
        return text;
    }

    private static Text replaceInText(Text text, String nick, String alias) {
        // Use MutableText to rebuild with style preserved
        net.minecraft.text.MutableText result = net.minecraft.text.Text.empty();
        text.visit((style, str) -> {
            String replaced = str.replaceAll("(?i)" + java.util.regex.Pattern.quote(nick), alias);
            result.append(net.minecraft.text.Text.literal(replaced).setStyle(style));
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return result;
    }
}
