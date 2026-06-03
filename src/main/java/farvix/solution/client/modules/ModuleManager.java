package farvix.solution.client.modules;

import lombok.Getter;
import lombok.NonNull;
import farvix.solution.client.modules.impl.environment.ClickUI;
import farvix.solution.client.modules.impl.environment.DiscordRPC;
import farvix.solution.client.modules.impl.environment.HitSound;
import farvix.solution.client.modules.impl.environment.Streamer;
import farvix.solution.client.modules.impl.environment.FastXP;
import farvix.solution.client.modules.impl.environment.Interface;
import farvix.solution.client.modules.impl.environment.ItemScroller;
import farvix.solution.client.modules.impl.environment.ModuleHotKeys;
import farvix.solution.client.modules.impl.environment.NoFriendDamage;
import farvix.solution.client.modules.impl.environment.PVPSafe;
import farvix.solution.client.modules.impl.environment.AutoSprint;
import farvix.solution.client.modules.impl.environment.CartHelper;
import farvix.solution.client.modules.impl.environment.RangeDistance;
import farvix.solution.client.modules.impl.environment.FakePlayer;

import farvix.solution.client.modules.impl.player.AutoRespawn;
import farvix.solution.client.modules.impl.visuals.*;

import java.util.*;

@Getter
public class ModuleManager {

    private List<Module> modules = new ArrayList<>();

    private final Map<Class<? extends Module>, Module> moduleByClass = new HashMap<>();
    private final Map<String, Module> moduleByName = new HashMap<>();

    public ModuleManager init() {
        registerModules(
                // Visuals
                new BlockOutline(), new AspectRatio(), new HoldMyItems(), new WorldVisuals(), new Interface(), new SwingAnimation(), new TargetESP(), new ItemHighlight(), new CustomItem(), new InventoryHUD(), new KillEffect(), new JumpCircle(), new TargetHud(), new NoRender(), new HitColor(), new HitParticles(), new WorldParticles(), new Zoom(), new PotionEffects(), new Predictions(), new ChinaHat(), new ArmorHUD(), new ModuleHotKeys(), new CustomHitbox(), new Crosshair(), new HudInventory(), new DynamicIsland(), new HitEffect(), new ContextMenuModule(), new HitBubbles(), new ScoreboardHud(), new PlayerRadialMenu(), new ShowMyName(), new WaypointOverlay(), new FreeLook(), new Saturation(), new Note(), new ChangeHand(), new NoChat(), new Wings(),

                // Environment
                new ClickUI(), new DiscordRPC(), new AutoRespawn(), new FastXP(), new ItemScroller(), new Streamer(), new HitSound(), new PVPSafe(), new NoFriendDamage(), new AutoSprint(), new CartHelper(), new RangeDistance(), new FakePlayer()
        );

        // Всегда включённые скрытые модули
        System.out.println("[ModuleManager] Enabling PlayerRadialMenu...");
        get(PlayerRadialMenu.class).setEnabled(true);
        System.out.println("[ModuleManager] PlayerRadialMenu enabled=" + get(PlayerRadialMenu.class).isEnabled());
        get(DiscordRPC.class).setEnabled(true);
        get(WaypointOverlay.class).setEnabled(true);

        return this;
    }

    public void registerModules(@NonNull Module... modules) {
        this.modules.addAll(Arrays.asList(modules));
        for (Module module : modules) {
            moduleByClass.put(module.getClass(), module);
            moduleByName.put(module.getName().toLowerCase(), module);
        }

        this.modules = Collections.unmodifiableList(this.modules);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(@NonNull final Class<T> clazz) {
        Objects.requireNonNull(clazz, "Module class cannot be null");
        return (T) moduleByClass.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(@NonNull final String name) {
        Objects.requireNonNull(name, "Module name cannot be null");
        return (T) moduleByName.get(name.toLowerCase());
    }
}
