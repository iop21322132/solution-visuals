package dev.simplevisuals.util;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.models.User;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.simplevisuals.simplevisuals;

public class DiscordRichPresenceUtil {
    private static final String DEFAULT_APP_ID = "1278655459695267890";
    private static final String[] REQUIRED_CLASSES = {
            "dev.firstdark.rpc.DiscordRpc",
            "dev.firstdark.rpc.handlers.RPCEventHandler"
    };

    private static volatile boolean running = false;
    private static Thread rpcThread;
    private static DiscordRpc rpc;
    private static volatile Boolean libraryAvailable;

    public static String state;

    public static synchronized void discordrpc() {
        startDiscord(null);
    }

    public static synchronized void startDiscord(String applicationId) {
        if (!isLibraryAvailable()) {
            return;
        }
        if (running) return;
        String appId = applicationId;
        if (appId == null || appId.isEmpty()) {
            appId = System.getProperty("discord.app.id", System.getenv("DISCORD_APP_ID"));
        }
        if (appId == null || appId.isEmpty()) appId = DEFAULT_APP_ID;

        rpc = new DiscordRpc();
        rpc.setDebugMode(false);

        RPCEventHandler handler = new RPCEventHandler() {
            @Override
            public void ready(User user) {
                running = true;
                pushPresence();
            }

            @Override
            public void disconnected(ErrorCode errorCode, String message) {
                running = false;
            }

            @Override
            public void errored(ErrorCode errorCode, String message) {
            }
        };

        try {
            rpc.init(appId, handler, false);
        } catch (Throwable t) {
            simplevisuals.LOGGER.warn("[DiscordRPC] Failed to initialize Discord RPC: {}", t.getMessage());
            return;
        }

        rpcThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (running) pushPresence();
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
        }, "Discord-RPC-FirstDark-Thread");
        rpcThread.setDaemon(true);
        rpcThread.start();
    }

    public static synchronized void shutdownDiscord() {
        running = false;
        if (rpcThread != null) {
            rpcThread.interrupt();
            rpcThread = null;
        }
        try { if (rpc != null) rpc.shutdown(); } catch (Throwable ignored) {}
        rpc = null;
    }

    private static void pushPresence() {
        if (rpc == null) return;
        DiscordRichPresence presence = DiscordRichPresence.builder()
                .details(state != null && !state.isEmpty() ? state : "Самый лучший пвп мод на 1.21.4 fabric")
                .largeImageText("Самый лучший пвп мод на 1.21.4 fabric")
                .smallImageText("Playing")
                .activityType(ActivityType.PLAYING)
                .button(DiscordRichPresence.RPCButton.of("Скачать", "https://t.me/SimpleVisuals"))
                .build();
        try { rpc.updatePresence(presence); } catch (Throwable ignored) {}
    }

    public static boolean isLibraryAvailable() {
        Boolean cached = libraryAvailable;
        if (cached != null) return cached;
        synchronized (DiscordRichPresenceUtil.class) {
            if (libraryAvailable != null) return libraryAvailable;
            for (String className : REQUIRED_CLASSES) {
                try {
                    Class.forName(className, false, DiscordRichPresenceUtil.class.getClassLoader());
                } catch (Throwable t) {
                    libraryAvailable = false;
                    simplevisuals.LOGGER.warn("[DiscordRPC] Required class '{}' not found. Discord presence will be disabled.", className);
                    return false;
                }
            }
            libraryAvailable = true;
            return true;
        }
    }
}
