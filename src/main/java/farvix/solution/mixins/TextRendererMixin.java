package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.environment.Streamer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = TextRenderer.class, priority = 1100)
public class TextRendererMixin {

    // draw (String)
    @ModifyVariable(method = "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
            at = @At("HEAD"), argsOnly = true, index = 1, require = 0)
    private String d_str(String text) {
        if (text == null) return null;
        text = rep(text); // Используем helper для String
        for (String friendName : farvix.solution.api.util.FriendManager.getFriends()) {
            if (text.contains(friendName)) {
                text = text.replace(friendName, "§a§l" + friendName + "§r");
                break;
            }
        }
        return text;
    }

    // draw (OrderedText)
    @ModifyVariable(method = "draw(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
            at = @At("HEAD"), argsOnly = true, index = 1, require = 0)
    private net.minecraft.text.OrderedText d_ordered(net.minecraft.text.OrderedText text) { return repOrdered(text); }

    // draw (Text) - объединяем замену ника и неймтеги
    @ModifyVariable(
            method = "draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
            at = @At("HEAD"), argsOnly = true, index = 1, require = 0)
    private Text d_text_unified(Text text) {
        if (text == null) return null;
        Text modified = repText(text); // Используем хелпер для Text
        String str = modified.getString();
        for (String friendName : farvix.solution.api.util.FriendManager.getFriends()) {
            if (str.contains(friendName)) {
                return farvix.solution.api.util.FriendTextUtil.recolorName(modified, friendName);
            }
        }
        return modified;
    }

    // getWidth(String)
    @ModifyVariable(method = "getWidth(Ljava/lang/String;)I",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private String gw_str(String text) { return rep(text); }

    // Удаляем устаревшие методы, которые используют MatrixStack, так как они больше не актуальны в 1.21.4
    // и вызывают предупреждения/ошибки.
    // Если тебе нужна замена текста в этих методах, их нужно переписать под новые сигнатуры.
    // @ModifyVariable(method = "drawWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Ljava/lang/String;FFIZ)I",
    //         at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    // private String dws_str2(String text) { return rep(text); }

    // @ModifyVariable(method = "drawWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/OrderedText;FFI)I",
    //         at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    // private net.minecraft.text.OrderedText dws_ordered_legacy(net.minecraft.text.OrderedText text) {
    //     return repOrdered(text);
    // }

    // @ModifyVariable(method = "draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/OrderedText;FFI)I",
    //         at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    // private net.minecraft.text.OrderedText d_ordered_legacy(net.minecraft.text.OrderedText text) {
    //     return repOrdered(text);
    // }

    private static net.minecraft.text.OrderedText repOrdered(net.minecraft.text.OrderedText text) {
        if (text == null) return null;
        try {
            Streamer s = Client.getInstance().getModuleManager().get(Streamer.class);
            if (s == null || !s.isEnabled()) return text;
            // Convert OrderedText to string, replace, convert back
            StringBuilder sb = new StringBuilder();
            text.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
            String orig = sb.toString();
            String replaced = s.replace(orig);
            if (!replaced.equals(orig)) {
                return net.minecraft.text.Text.literal(replaced).asOrderedText();
            }
        } catch (Throwable ignored) {}
        return text;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String rep(String text) {
        if (text == null) return null;
        try {
            Streamer s = Client.getInstance().getModuleManager().get(Streamer.class);
            if (s != null) return s.replace(text);
        } catch (Throwable ignored) {}
        return text;
    }

    private static Text repText(Text text) {
        if (text == null) return null;
        try {
            Streamer s = Client.getInstance().getModuleManager().get(Streamer.class);
            if (s == null || !s.isEnabled()) return text;
            String realNick = net.minecraft.client.MinecraftClient.getInstance().getSession() != null
                    ? net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername() : null;
            if (realNick == null) return text;
            String orig = text.getString();
            if (!orig.toLowerCase().contains(realNick.toLowerCase())) return text;
            // Rebuild text preserving styles
            net.minecraft.text.MutableText result = net.minecraft.text.Text.empty();
            text.visit((style, str) -> {
                String replaced = str.replaceAll("(?i)" + java.util.regex.Pattern.quote(realNick), s.alias.getText());
                result.append(net.minecraft.text.Text.literal(replaced).setStyle(style));
                return java.util.Optional.empty();
            }, net.minecraft.text.Style.EMPTY);
            return result;
        } catch (Throwable ignored) {}
        return text;
    }
}
