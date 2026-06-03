package dev.simplevisuals.client.hooks;

import dev.simplevisuals.modules.impl.utility.NameProtect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class NameProtectChatHook {

    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        // Allow-phase: cancel original when we plan to replace it
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            NameProtect np = NameProtect.getInstance();
            if (np == null || !np.isToggled() || message == null) return true; // allow
            String before = message.getString();
            String after = np.replaceNames(before);
            if (!before.equals(after)) {
                // Cancel the original; we will re-add modified in GAME phase
                return false;
            }
            return true;
        });

        // Game-phase: add modified message when needed
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            NameProtect np = NameProtect.getInstance();
            if (np == null || !np.isToggled() || message == null) return;
            String before = message.getString();
            String after = np.replaceNames(before);
            if (!before.equals(after)) {
                MinecraftClient mc = MinecraftClient.getInstance();
                ChatHud chat = mc.inGameHud.getChatHud();
                chat.addMessage(Text.of(after));
            }
        });
    }

    private NameProtectChatHook() {}
}
