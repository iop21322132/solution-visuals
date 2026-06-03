package farvix.solution;

import lombok.Getter;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import farvix.solution.api.events.impl.game.EventMessage;
import farvix.solution.api.util.FriendManager;
import farvix.solution.api.events.impl.game.EventWorldLoad;
import farvix.solution.api.events.impl.input.EventInput;
import farvix.solution.api.events.impl.game.EventTick;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.logger.GameLogger;
import farvix.solution.api.translation.TranslationManager;
import farvix.solution.api.util.render.RenderListener;
import farvix.solution.client.modules.ModuleManager;

import java.lang.invoke.MethodHandles;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Getter
public class Client implements ModInitializer, QuickImports {
    public static final String client = "Solution Visual";

    @Getter
    public static Client instance;

    public final IEventBus bus = new EventBus();
    public ModuleManager moduleManager;
    public farvix.solution.api.config.ConfigManager configManager;
    public farvix.solution.client.managers.MacroManager macroManager;

    @Override
    public void onInitialize() {
        instance = this;

        TranslationManager.getInstance().setLanguage("ru");

        bus.registerLambdaFactory(Client.class.getPackage().getName(), (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        bus.subscribe(instance);

        moduleManager = new ModuleManager().init();
        configManager = new farvix.solution.api.config.ConfigManager();
        macroManager = new farvix.solution.client.managers.MacroManager();
        new farvix.solution.client.managers.NoteManager();

        bus.subscribe(new RenderListener());
        bus.subscribe(new GameLogger());
        bus.subscribe(macroManager); // Регистрируем MacroManager для обработки EventInput

        // Регистрируем клиентские команды /friend (видны через таб)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("friend")
                    .then(literal("list")
                        .executes(ctx -> {
                            sendFriendList(ctx.getSource());
                            return 1;
                        })
                    )
                    .then(literal("add")
                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                            .argument("ник", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                // Подсказываем ники игроков онлайн
                                net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
                                if (mc2.getNetworkHandler() != null) {
                                    mc2.getNetworkHandler().getPlayerList().forEach(p ->
                                        builder.suggest(p.getProfile().getName()));
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "ник");
                                addFriendCmd(ctx.getSource(), name);
                                return 1;
                            })
                        )
                    )
                    .then(literal("remove")
                        .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                            .argument("ник", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                // Подсказываем только тех кто уже в друзьях
                                FriendManager.getFriends().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "ник");
                                removeFriendCmd(ctx.getSource(), name);
                                return 1;
                            })
                        )
                    )
            );
        });

        // Register theme change listener to reset overridden flags of all module colors
        farvix.solution.client.managers.ThemeManager.getInstance().addThemeChangeListener(theme -> {
            if (farvix.solution.api.settings.impl.ColorSetting.syncWithTheme) {
                farvix.solution.api.settings.impl.ColorSetting.resetAllOverridden();
            }
        });

        // Auto-load last session settings — откладываем до первого тика рендера
        // чтобы Tessellator и другие рендер-объекты были инициализированы
        pendingAutoLoad = true;

        // Auto-save on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                configManager.saveConfig("_autosave");
            } catch (Throwable ignored) {}
        }, "Solution-AutoSave"));
    }

    private boolean pendingAutoLoad = false;

    private int autoSaveTick = 0;

    @EventHandler
    public void onWorldLoad(farvix.solution.api.events.impl.game.EventWorldLoad e) {
        // Сохраняем при каждой загрузке мира (перезаход, смена сервера)
        try { configManager.saveConfig("_autosave"); } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onTick(farvix.solution.api.events.impl.game.EventTick e) {
        // Загружаем конфиг при первом тике (рендер-тред уже готов)
        if (pendingAutoLoad) {
            pendingAutoLoad = false;
            try { configManager.loadConfig("_autosave"); } catch (Throwable ignored) {}
            // После загрузки конфига принудительно включаем скрытые модули
            // (конфиг мог сохранить их как выключенные)
            if (!moduleManager.get(farvix.solution.client.modules.impl.visuals.PlayerRadialMenu.class).isEnabled()) {
                moduleManager.get(farvix.solution.client.modules.impl.visuals.PlayerRadialMenu.class).setEnabled(true);
            }
            if (!moduleManager.get(farvix.solution.client.modules.impl.environment.DiscordRPC.class).isEnabled()) {
                moduleManager.get(farvix.solution.client.modules.impl.environment.DiscordRPC.class).setEnabled(true);
            }
            if (!moduleManager.get(farvix.solution.client.modules.impl.visuals.WaypointOverlay.class).isEnabled()) {
                moduleManager.get(farvix.solution.client.modules.impl.visuals.WaypointOverlay.class).setEnabled(true);
            }
        }
        // Автосохранение каждые 6000 тиков (~5 минут)
        if (++autoSaveTick >= 6000) {
            autoSaveTick = 0;
            try { configManager.saveConfig("_autosave"); } catch (Throwable ignored) {}
        }
    }

    @EventHandler
    public void onInput(EventInput eventInput) {
        // Блокируем все бинды если открыт чат или любой GUI (кроме ClickUI)
        if (mc.currentScreen != null && !(mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen)) {
            return;
        }
        
        getModuleManager().getModules().forEach(module -> {
            if (eventInput.isPressed(module.getKey())) {
                // Блокируем открытие ClickUI в меню
                if (module instanceof farvix.solution.client.modules.impl.environment.ClickUI) {
                    if (mc.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen ||
                        mc.currentScreen instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen ||
                        mc.currentScreen instanceof net.minecraft.client.gui.screen.world.SelectWorldScreen ||
                        mc.currentScreen instanceof net.minecraft.client.gui.screen.world.CreateWorldScreen) {
                        // Не открываем ClickUI в этих экранах
                        return;
                    }
                }
                module.toggle();
            }
        });
    }

    private static void sendFriendList(FabricClientCommandSource source) {
        java.util.Set<String> friends = FriendManager.getFriends();

        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.player.sendMessage(Text.literal("§f§lSolution Visual"), false);

        if (friends.isEmpty()) {
            mc.player.sendMessage(Text.literal("§7  Список друзей пуст"), false);
        } else {
            for (String name : friends) {
                mc.player.sendMessage(Text.literal("§a" + name), false);
            }
        }
    }

    private static void addFriendCmd(FabricClientCommandSource source, String name) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (FriendManager.isFriend(name)) {
            mc.player.sendMessage(Text.literal("§e" + name + " §7уже в списке друзей"), false);
        } else {
            FriendManager.addFriend(name);
            Client.getInstance().configManager.saveConfig("_autosave");
            mc.player.sendMessage(Text.literal("§a+ Добавлен в друзья: §f" + name), false);
        }
    }

    private static void removeFriendCmd(FabricClientCommandSource source, String name) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (!FriendManager.isFriend(name)) {
            mc.player.sendMessage(Text.literal("§e" + name + " §7не найден в списке друзей"), false);
        } else {
            FriendManager.removeFriend(name);
            Client.getInstance().configManager.saveConfig("_autosave");
            mc.player.sendMessage(Text.literal("§c- Удалён из друзей: §f" + name), false);
        }
    }

    @EventHandler
    public void onMessage(EventMessage e) {
        String msg = e.getMessage().trim();
        String msgLower = msg.toLowerCase();

        // .help
        if (msgLower.equals(".help")) {
            e.setCancelled(true);
            sendHelpMessage();
            return;
        }

        // .friend
        if (msgLower.equals(".friend")) {
            e.setCancelled(true);
            net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
            if (mc2.player != null) {
                mc2.player.sendMessage(Text.literal("§f§lSolution Visual §7- Использование .friend:"), false);
                mc2.player.sendMessage(Text.literal("§e.friend list §7- Показать список друзей"), false);
                mc2.player.sendMessage(Text.literal("§e.friend add <ник> §7- Добавить игрока в друзья"), false);
                mc2.player.sendMessage(Text.literal("§e.friend remove <ник> §7- Удалить игрока из друзей"), false);
            }
            return;
        }

        // .friend list
        if (msgLower.equals(".friend list")) {
            e.setCancelled(true);
            sendFriendList(null);
            return;
        }

        // .friend add <ник>
        if (msgLower.startsWith(".friend add ")) {
            e.setCancelled(true);
            String name = msg.substring(".friend add ".length()).trim();
            if (!name.isEmpty()) addFriendCmd(null, name);
            return;
        }

        // .friend remove <ник>
        if (msgLower.startsWith(".friend remove ")) {
            e.setCancelled(true);
            String name = msg.substring(".friend remove ".length()).trim();
            if (!name.isEmpty()) removeFriendCmd(null, name);
            return;
        }

        // .cfg
        if (msgLower.equals(".cfg")) {
            e.setCancelled(true);
            net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
            if (mc2.player != null) {
                mc2.player.sendMessage(Text.literal("§f§lSolution Visual §7- Использование .cfg:"), false);
                mc2.player.sendMessage(Text.literal("§e.cfg load <название> §7- Загрузить конфигурацию"), false);
                mc2.player.sendMessage(Text.literal("§e.cfg save <название> §7- Сохранить конфигурацию"), false);
                mc2.player.sendMessage(Text.literal("§e.cfg dir §7- Открыть папку с конфигурациями"), false);
            }
            return;
        }

        // .cfg load <название>
        if (msgLower.startsWith(".cfg load ")) {
            e.setCancelled(true);
            String name = msg.substring(".cfg load ".length()).trim();
            if (!name.isEmpty()) {
                try {
                    Client.getInstance().configManager.loadConfig(name);
                    net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
                    if (mc2.player != null) {
                        mc2.player.sendMessage(Text.literal("§f§lSolution Visual §7| Конфиг §a" + name + " §7успешно загружен."), false);
                    }
                } catch (Exception ex) {
                    net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
                    if (mc2.player != null) {
                        mc2.player.sendMessage(Text.literal("§f§lSolution Visual §7| §cОшибка при загрузке конфига: " + ex.getMessage()), false);
                    }
                }
            }
            return;
        }

        // .cfg save <название>
        if (msgLower.startsWith(".cfg save ")) {
            e.setCancelled(true);
            String name = msg.substring(".cfg save ".length()).trim();
            if (!name.isEmpty()) {
                try {
                    Client.getInstance().configManager.saveConfig(name);
                    net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
                    if (mc2.player != null) {
                        mc2.player.sendMessage(Text.literal("§f§lSolution Visual §7| Конфиг §a" + name + " §7успешно сохранен."), false);
                    }
                } catch (Exception ex) {
                    net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
                    if (mc2.player != null) {
                        mc2.player.sendMessage(Text.literal("§f§lSolution Visual §7| §cОшибка при сохранении конфига: " + ex.getMessage()), false);
                    }
                }
            }
            return;
        }

        // .cfg dir
        if (msgLower.equals(".cfg dir")) {
            e.setCancelled(true);
            net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
            if (mc2.player != null) {
                mc2.player.sendMessage(Text.literal("§f§lSolution Visual §7| Открываем папку с конфигурациями..."), false);
            }
            java.io.File configDir = Client.getInstance().configManager.getConfigDir();
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            new Thread(() -> {
                try {
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        Runtime.getRuntime().exec("explorer.exe \"" + configDir.getAbsolutePath() + "\"");
                    } else if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(configDir);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    private static void sendHelpMessage() {
        net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
        if (mc2.player == null) return;
        mc2.player.sendMessage(Text.literal("§f§lSolution Visual §7- Список команд:"), false);
        mc2.player.sendMessage(Text.literal("§e.help §7- Показать список команд"), false);
        mc2.player.sendMessage(Text.literal("§e.friend list §7- Показать список друзей"), false);
        mc2.player.sendMessage(Text.literal("§e.friend add <ник> §7- Добавить игрока в друзья"), false);
        mc2.player.sendMessage(Text.literal("§e.friend remove <ник> §7- Удалить игрока из друзей"), false);
        mc2.player.sendMessage(Text.literal("§e.cfg load <название> §7- Загрузить конфигурацию"), false);
        mc2.player.sendMessage(Text.literal("§e.cfg save <название> §7- Сохранить конфигурацию"), false);
        mc2.player.sendMessage(Text.literal("§e.cfg dir §7- Открыть папку с конфигурациями"), false);
        mc2.player.sendMessage(Text.literal("§e.note add <текст> §7- Добавить заметку"), false);
        mc2.player.sendMessage(Text.literal("§e.note del <номер> §7- Удалить заметку"), false);
        mc2.player.sendMessage(Text.literal("§e.note help §7- Справка по заметкам"), false);
    }
}
