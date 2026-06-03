package farvix.solution.client.modules.impl.environment;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import org.json.JSONObject;

import java.time.OffsetDateTime;

@ModuleInfo(name = "Discord RPC", category = ModuleCategory.ENVIRONMENT, description = "Статус в Discord Rich Presence")
public class DiscordRPC extends Module implements IPCListener {

    private static final long APP_ID = 1497271221291716770L;

    private IPCClient client;
    private Thread rpcThread;
    private volatile boolean ready = false;
    private volatile IPCClient activeClient = null;

    @Override
    public void onEnable() {
        super.onEnable();
        ready = false;
        rpcThread = new Thread(() -> {
            // retry loop — Discord может не сразу ответить
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("[DiscordRPC] Попытка подключения...");
                    client = new IPCClient(APP_ID);
                    client.setListener(this);
                    client.connect();
                    // если connect() вернулся — ждём onReady
                    long deadline = System.currentTimeMillis() + 5000;
                    while (!ready && System.currentTimeMillis() < deadline) {
                        Thread.sleep(100);
                    }
                    if (!ready) {
                        System.out.println("[DiscordRPC] onReady не пришёл за 5с, повтор...");
                        try { client.close(); } catch (Throwable ignored) {}
                        Thread.sleep(3000);
                        continue;
                    }
                    // держим соединение без периодических обновлений
                    while (!Thread.currentThread().isInterrupted()
                            && client.getStatus() == PipeStatus.CONNECTED) {
                        Thread.sleep(5000);
                    }
                    ready = false;
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable t) {
                    System.out.println("[DiscordRPC] " + t.getClass().getSimpleName() + ": " + t.getMessage());
                    try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
                }
            }
        }, "Solution-DiscordRPC");
        rpcThread.setDaemon(true);
        rpcThread.start();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ready = false;
        // Clear activity before closing
        if (client != null) {
            try {
                JSONObject args = new JSONObject();
                args.put("pid", ProcessHandle.current().pid());
                // activity = null clears it
                JSONObject payload = new JSONObject();
                payload.put("cmd", "SET_ACTIVITY");
                payload.put("args", args);
                payload.put("nonce", java.util.UUID.randomUUID().toString());
                sendPayload(client, payload);
                Thread.sleep(200); // give Discord time to process
            } catch (Throwable ignored) {}
        }
        if (rpcThread != null) { rpcThread.interrupt(); rpcThread = null; }
        try { if (client != null) { client.close(); client = null; } } catch (Throwable ignored) {}
    }

    // ── IPCListener ──────────────────────────────────────────────────────────

    @Override
    public void onReady(IPCClient client) {
        System.out.println("[DiscordRPC] onReady! Отправляю presence...");
        ready = true;
        updatePresence(client);
    }

    private void updatePresence(IPCClient ipcClient) {
        try {
            String details = getServerInfo();

            JSONObject activityJson = new JSONObject();
            activityJson.put("details", details);
            activityJson.put("state", "Playing Minecraft 1.21.4");

            JSONObject timestamps = new JSONObject();
            timestamps.put("start", System.currentTimeMillis() / 1000L);
            activityJson.put("timestamps", timestamps);

            org.json.JSONArray buttons = new org.json.JSONArray();
            buttons.put(new JSONObject().put("label", "Discord").put("url", "https://discord.gg/jP2GKAMxZ9"));
            buttons.put(new JSONObject().put("label", "Telegram").put("url", "https://t.me/SolutionDevLogs"));
            activityJson.put("buttons", buttons);

            JSONObject args = new JSONObject();
            args.put("pid", ProcessHandle.current().pid());
            args.put("activity", activityJson);
            JSONObject payload = new JSONObject();
            payload.put("cmd", "SET_ACTIVITY");
            payload.put("args", args);
            payload.put("nonce", java.util.UUID.randomUUID().toString());

            sendPayload(ipcClient, payload);
        } catch (Throwable t) {
            System.out.println("[DiscordRPC] updatePresence error: " + t.getMessage());
        }
    }

    private String getServerInfo() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null) return "Solution Visual Client";

        // Multiplayer — get server address
        if (mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().address;
        }

        // Singleplayer
        if (mc.isInSingleplayer()) {
            return "SinglePlayer";
        }

        return "Solution Visual Client";
    }

    private void sendPayload(IPCClient ipcClient, JSONObject payload) throws Exception {
        Object pipe = null;
        for (java.lang.reflect.Field f : IPCClient.class.getDeclaredFields()) {
            f.setAccessible(true);
            Object val = f.get(ipcClient);
            if (val != null && val.getClass().getSuperclass() != null
                    && val.getClass().getSuperclass().getSimpleName().equals("Pipe")) {
                pipe = val;
                break;
            }
        }
        if (pipe == null) throw new Exception("pipe not found");

        com.jagrosh.discordipc.entities.Packet.OpCode frame =
                com.jagrosh.discordipc.entities.Packet.OpCode.FRAME;

        java.lang.reflect.Method sendMethod = com.jagrosh.discordipc.entities.pipe.Pipe.class
                .getDeclaredMethod("send",
                        com.jagrosh.discordipc.entities.Packet.OpCode.class,
                        JSONObject.class,
                        com.jagrosh.discordipc.entities.Callback.class);
        sendMethod.setAccessible(true);
        sendMethod.invoke(pipe, frame, payload, null);
    }

    @Override
    public void onClose(IPCClient client, JSONObject json) {
        System.out.println("[DiscordRPC] Закрыто: " + json);
        ready = false;
    }

    @Override
    public void onDisconnect(IPCClient client, Throwable t) {
        System.out.println("[DiscordRPC] Отключено: " + (t != null ? t.getMessage() : "null"));
        ready = false;
    }
}
