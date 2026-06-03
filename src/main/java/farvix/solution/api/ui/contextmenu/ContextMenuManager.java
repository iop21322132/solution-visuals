package farvix.solution.api.ui.contextmenu;

import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.client.modules.Module;
import farvix.solution.Client;
import net.minecraft.client.gui.DrawContext;

/**
 * Менеджер контекстных меню - управляет всеми открытыми меню
 */
public class ContextMenuManager implements QuickImports {
    
    private static ContextMenuManager instance;
    
    private ContextMenu contextMenu = null;
    private ModuleContextMenu moduleContextMenu = null;
    
    // Текущее активное меню
    private Object activeMenu = null;
    
    private ContextMenuManager() {
    }
    
    public static ContextMenuManager getInstance() {
        if (instance == null) {
            instance = new ContextMenuManager();
        }
        return instance;
    }
    
    /**
     * Открыть обычное контекстное меню
     */
    public void openContextMenu(float x, float y) {
        closeAll();
        
        // Пересоздаём каждый раз чтобы статус модулей был актуальным
        contextMenu = createContextMenu();
        
        contextMenu.open(x, y);
        activeMenu = contextMenu;
    }
    
    /**
     * Открыть контекстное меню модуля
     */
    public void openModuleContextMenu(Module module, float x, float y) {
        closeAll();
        
        if (moduleContextMenu == null) {
            moduleContextMenu = new ModuleContextMenu();
        }
        
        moduleContextMenu.open(module, x, y);
        activeMenu = moduleContextMenu;
    }
    
    /**
     * Закрыть все меню
     */
    public void closeAll() {
        if (contextMenu != null && contextMenu.isVisible()) {
            contextMenu.close();
        }
        if (moduleContextMenu != null && moduleContextMenu.isVisible()) {
            moduleContextMenu.close();
        }
        activeMenu = null;
    }
    
    /**
     * Проверка есть ли открытое меню
     */
    public boolean hasOpenMenu() {
        return activeMenu != null && 
               ((activeMenu instanceof ContextMenu && ((ContextMenu)activeMenu).isVisible()) ||
                (activeMenu instanceof ModuleContextMenu && ((ModuleContextMenu)activeMenu).isVisible()));
    }
    
    /**
     * Рендер всех меню
     */
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        // Рендерим даже если closing = true (для анимации закрытия)
        if (contextMenu != null && contextMenu.isVisibleOrClosing()) {
            contextMenu.render(ctx, mouseX, mouseY);
        }
        if (moduleContextMenu != null && moduleContextMenu.isVisibleOrClosing()) {
            moduleContextMenu.render(ctx, mouseX, mouseY);
        }
    }
    
    /**
     * Обработка клика
     * @return true если клик обработан меню
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contextMenu != null && contextMenu.isVisible()) {
            return contextMenu.mouseClicked(mouseX, mouseY, button);
        }
        if (moduleContextMenu != null && moduleContextMenu.isVisible()) {
            return moduleContextMenu.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    /**
     * Обработка перетаскивания
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (moduleContextMenu != null && moduleContextMenu.isVisible()) {
            return moduleContextMenu.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    /**
     * Обработка отпускания кнопки мыши
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (moduleContextMenu != null && moduleContextMenu.isVisible()) {
            return moduleContextMenu.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }
    
    /**
     * Обработка клавиши
     */
    public boolean keyPressed(int keyCode) {
        if (contextMenu != null && contextMenu.isVisible()) {
            return contextMenu.keyPressed(keyCode);
        }
        if (moduleContextMenu != null && moduleContextMenu.isVisible()) {
            return moduleContextMenu.keyPressed(keyCode);
        }
        return false;
    }
    
    /**
     * Создает контекстное меню со списком HUD модулей
     */
    private ContextMenu createContextMenu() {
        ContextMenu menu = new ContextMenu();
        
        // Список HUD модулей
        Class<?> [] hudModuleClasses = {
            farvix.solution.client.modules.impl.visuals.TargetHud.class,
            farvix.solution.client.modules.impl.visuals.InventoryHUD.class,
            farvix.solution.client.modules.impl.visuals.DynamicIsland.class,
            farvix.solution.client.modules.impl.environment.Interface.class,
            farvix.solution.client.modules.impl.visuals.ArmorHUD.class,
            farvix.solution.client.modules.impl.environment.ModuleHotKeys.class,
            farvix.solution.client.modules.impl.visuals.PotionEffects.class,
            farvix.solution.client.modules.impl.visuals.Note.class,
            farvix.solution.client.modules.impl.visuals.ScoreboardHud.class
        };
        
        for (Class<?> clazz : hudModuleClasses) {
            @SuppressWarnings("unchecked")
            Module mod = farvix.solution.Client.getInstance().getModuleManager()
                .get((Class<? extends Module>) clazz);
            if (mod != null) {
                menu.addModuleItem(mod);
            }
        }
        
        return menu;
    }
}
