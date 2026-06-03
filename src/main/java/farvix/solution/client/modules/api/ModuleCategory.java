package farvix.solution.client.modules.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import farvix.solution.api.ui.clickgui.api.MenuScreen;
import farvix.solution.api.ui.clickgui.impl.module.ModuleScreen;
import farvix.solution.api.ui.clickgui.impl.config.ConfigScreen;
import farvix.solution.api.ui.clickgui.impl.theme.ThemeScreen;
import farvix.solution.api.ui.clickgui.impl.macro.MacroScreen;
import farvix.solution.api.ui.clickgui.impl.waypoint.WaypointScreen;

@Getter
@RequiredArgsConstructor
public enum ModuleCategory {
    VISUALS("Visuals",      "r", new ModuleScreen()),
    ENVIRONMENT("Environment", "s", new ModuleScreen()),
    PLAYER("Player",        "player", new ModuleScreen()),
    MACRO("Macro",          "⌨", new MacroScreen()),
    WAYPOINTS("Waypoints",  "🌎", new WaypointScreen()),
    THEMES("Themes",        "u", new ThemeScreen()),
    CONFIGS("Configs",      "t", new ConfigScreen());

    final String displayName;
    final String icon;
    final MenuScreen screen;
}
