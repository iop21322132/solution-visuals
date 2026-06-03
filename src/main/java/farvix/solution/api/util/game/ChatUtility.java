package farvix.solution.api.util.game;

import lombok.experimental.UtilityClass;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import farvix.solution.Client;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.util.color.FixColor;

@UtilityClass
public class ChatUtility implements QuickImports {
    private MutableText get(String message, boolean error) {
        return FixColor.gradient(Client.client, TempColor.getClientColor().getRGB(), TempColor.getClientColor().darker().getRGB()).append(Text.literal(Formatting.DARK_GRAY + " » " + Formatting.RESET + (error ? Formatting.RED : Formatting.WHITE) + message));
    }

    public void send(String message) {
        if (mc.player == null) return;
        mc.inGameHud.getChatHud().addMessage(get(message, false));
    }

    public void sendError(String message) {
        if (mc.player == null) return;
        mc.inGameHud.getChatHud().addMessage(get(message, true));
    }
}
