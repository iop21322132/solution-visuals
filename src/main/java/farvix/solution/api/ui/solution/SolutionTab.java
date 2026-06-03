package farvix.solution.api.ui.solution;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import farvix.solution.client.modules.api.ModuleCategory;

/**
 * Вкладки для SolutionVisual GUI
 * Маппинг на наши категории модулей
 */
@Getter
@RequiredArgsConstructor
public enum SolutionTab {
    VISUALS("Visuals", ModuleCategory.VISUALS),
    HUD("HUD", ModuleCategory.ENVIRONMENT),
    PLAYER("Player", ModuleCategory.PLAYER),
    UTILITIES("Utilities", ModuleCategory.WAYPOINTS);
    
    private final String displayName;
    private final ModuleCategory category;
}
