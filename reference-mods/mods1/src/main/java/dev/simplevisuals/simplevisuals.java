package dev.simplevisuals;

import dev.simplevisuals.client.managers.*;
import dev.simplevisuals.client.ui.mainmenu.MainMenu;
import dev.simplevisuals.client.ui.clickgui.ClickGui;
import dev.simplevisuals.client.util.Wrapper;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class simplevisuals implements ModInitializer, Wrapper {

    private static simplevisuals instance;
    public static final Logger LOGGER = LogManager.getLogger(simplevisuals.class);

    private IEventBus eventHandler;
    private long initTime;

    // Менеджеры
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private AutoSaveManager autoSaveManager;
    private NotifyManager notifyManager;
    private PerformanceManager performanceManager;
    private ClickGui clickGui;
    private HudManager hudManager;
    @Nullable private AltManager altManager;
    private MainMenu mainMenu;
    private dev.simplevisuals.client.ui.hud.impl.WaypointOverlay waypointOverlay;

    // Оптимизация для TitleScreen проверки
    private boolean titleScreenReplaced = false;
    @Nullable private TitleScreen lastTitleScreen = null;

    // Директории
    private final File globalsDir = new File(mc.runDirectory, "simplevisuals");
    private final File configsDir = new File(globalsDir, "configs");

    @Override
    public void onInitialize() {
        LOGGER.info("[SimpleVisuals] Starting initialization.");
        initTime = System.currentTimeMillis();
        instance = this;

        createDirs(globalsDir, configsDir);
        eventHandler = new EventBus();

        eventHandler.registerLambdaFactory("dev.simplevisuals",
                (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())
        );

        // Инициализация менеджеров
        FriendsManager.init(globalsDir);
        AltManager.init(globalsDir);

        // Применение ника
        String lastAlt = AltManager.getLastUsedNickname();
        if (lastAlt != null && !lastAlt.isEmpty()) {
            AltManager.applyNickname(lastAlt);
        }

        // Создаем менеджеры
        notifyManager = new NotifyManager();
        performanceManager = new PerformanceManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        configManager = new ConfigManager();
        autoSaveManager = new AutoSaveManager();
        clickGui = new ClickGui();
        hudManager = new HudManager();
        mainMenu = new MainMenu();

        // Waypoint overlay
        waypointOverlay = new dev.simplevisuals.client.ui.hud.impl.WaypointOverlay();
        eventHandler.subscribe(waypointOverlay);

        // Загружаем автоматически сохраненную конфигурацию
        autoSaveManager.loadAutoSave();

        // Регистрация оптимизированного события для замены TitleScreen
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        LOGGER.info("[SimpleVisuals] Successfully initialized in {} ms.",
                System.currentTimeMillis() - initTime);
    }

    /**
     * Оптимизированный обработчик клиентского тика
     */
    private void onClientTick(net.minecraft.client.MinecraftClient client) {
        // Замена TitleScreen на MainMenu (однократно)
        if (client.currentScreen instanceof TitleScreen currentTitleScreen) {
            if (currentTitleScreen != lastTitleScreen) {
                // Новый экземпляр TitleScreen, сбрасываем флаг
                lastTitleScreen = currentTitleScreen;
                titleScreenReplaced = false;
            }

            if (!titleScreenReplaced && mainMenu != null) {
                client.setScreen(mainMenu);
                titleScreenReplaced = true;
            }
        } else {
            // Сбрасываем состояние, если не на TitleScreen
            if (titleScreenReplaced) {
                titleScreenReplaced = false;
                lastTitleScreen = null;
            }
        }
    }

    private void createDirs(File... dirs) {
        for (File dir : dirs) {
            if (!dir.exists() && !dir.mkdirs()) {
                LOGGER.warn("Failed to create directory: {}", dir.getAbsolutePath());
            }
        }
    }

    public static Identifier id(String path) {
        return Identifier.of("simplevisuals", path);
    }

    // ========== ГЕТТЕРЫ ДЛЯ ДОСТУПА ИЗ ДРУГИХ КЛАССОВ ==========

    public static simplevisuals getInstance() {
        return instance;
    }

    public IEventBus getEventHandler() {
        return eventHandler;
    }

    public long getInitTime() {
        return initTime;
    }

    public File getGlobalsDir() {
        return globalsDir;
    }

    public File getConfigsDir() {
        return configsDir;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AutoSaveManager getAutoSaveManager() {
        return autoSaveManager;
    }

    public NotifyManager getNotifyManager() {
        return notifyManager;
    }

    public PerformanceManager getPerformanceManager() {
        return performanceManager;
    }

    public ClickGui getClickGui() {
        return clickGui;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public AltManager getAltManager() {
        if (altManager == null) {
            altManager = new AltManager();
        }
        return altManager;
    }

    public MainMenu getMainMenu() {
        return mainMenu;
    }

    public dev.simplevisuals.client.ui.hud.impl.WaypointOverlay getWaypointOverlay() {
        return waypointOverlay;
    }

    /**
     * Очистка ресурсов для экономии памяти
     */
}