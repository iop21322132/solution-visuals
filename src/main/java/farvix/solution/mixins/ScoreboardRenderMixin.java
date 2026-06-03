package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.environment.Streamer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(InGameHud.class)
public class ScoreboardRenderMixin {

    // Replace any Text drawn during renderScoreboardSidebar
    @ModifyVariable(
            method = "renderScoreboardSidebar",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I",
                    ordinal = 0),
            index = 3, require = 0)
    private Text replaceScoreboardText(Text text) {
        return repText(text);
    }

    @ModifyVariable(
            method = "renderScoreboardSidebar",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I",
                    ordinal = 1),
            index = 3, require = 0)
    private Text replaceScoreboardText2(Text text) {
        return repText(text);
    }

    private static Text repText(Text text) {
        if (text == null) return null;
        try {
            Streamer s = Client.getInstance().getModuleManager().get(Streamer.class);
            if (s == null || !s.isEnabled()) return text;
            String orig = text.getString();
            String replaced = s.replace(orig);
            if (!replaced.equals(orig)) return Text.literal(replaced);
        } catch (Throwable ignored) {}
        return text;
    }
}
