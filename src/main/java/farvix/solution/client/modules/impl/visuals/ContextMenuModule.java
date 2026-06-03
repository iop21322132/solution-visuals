package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.ui.contextmenu.ContextMenu;
import farvix.solution.api.ui.contextmenu.ContextMenuManager;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/**
 * Модуль для управления контекстным меню
 */
@ModuleInfo(name = "Context Menu", category = ModuleCategory.VISUALS, description = "Контекстное меню по ПКМ")
public class ContextMenuModule extends Module implements QuickImports {
    
    private boolean wasRightPressed = false;
    private boolean wasLeftPressed = false;
    private boolean wasEscPressed = false;
    
    public ContextMenuModule() {
        // Включаем модуль по умолчанию и делаем его всегда активным
        this.setEnabled(true);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        // Модуль всегда включен, нельзя выключить
        super.setEnabled(true);
    }
    
    @Override
    public void toggle() {
        // Модуль всегда включен, toggle ничего не делает
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
    }
    
    // ── Render ────────────────────────────────────────────────────────────────

    @EventHandler(priority = 10000) // Очень высокий приоритет - рендерим ПОВЕРХ ВСЕГО (включая HUD элементы)
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.currentScreen == null) {
            // Закрываем меню если экран закрыт
            ContextMenuManager.getInstance().closeAll();
            wasRightPressed = false;
            wasLeftPressed = false;
            wasEscPressed = false;
            return;
        }
        
        // Проверяем тип экрана - разрешаем только в чате
        boolean isAllowedScreen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        
        if (!isAllowedScreen) {
            // Закрываем меню если открыт неподходящий экран
            ContextMenuManager.getInstance().closeAll();
            wasRightPressed = false;
            wasLeftPressed = false;
            wasEscPressed = false;
            return;
        }
        
        DrawContext ctx = e.getContext();
        double mouseX = mc.mouse.getX() / mc.getWindow().getScaleFactor();
        double mouseY = mc.mouse.getY() / mc.getWindow().getScaleFactor();
        
        // Обработка ПКМ - открытие/закрытие меню
        boolean rightPressed = GLFW.glfwGetMouseButton(
            mc.getWindow().getHandle(), 
            GLFW.GLFW_MOUSE_BUTTON_RIGHT
        ) == GLFW.GLFW_PRESS;
        
        if (rightPressed && !wasRightPressed) {
            // ПКМ только что нажата
            if (ContextMenuManager.getInstance().hasOpenMenu()) {
                // Если меню открыто - закрываем
                ContextMenuManager.getInstance().closeAll();
            } else {
                // Сначала проверяем клик по HUD элементам
                farvix.solution.api.ui.hud.IHudElement hudElement = 
                    farvix.solution.api.ui.hud.HudElementRegistry.getInstance().findElementAt(mouseX, mouseY);
                
                if (hudElement != null) {
                    // Клик по HUD элементу - открываем меню модуля
                    ContextMenuManager.getInstance().openModuleContextMenu(
                        hudElement.getModule(), 
                        (float) mouseX, 
                        (float) mouseY
                    );
                } else {
                    // Проверяем что клик по пустому месту (не по элементам GUI)
                    boolean clickedOnEmpty = isClickOnEmptySpace(mouseX, mouseY);
                    
                    if (clickedOnEmpty) {
                        // Если меню закрыто и клик по пустому - открываем обычное меню
                        ContextMenuManager.getInstance().openContextMenu((float) mouseX, (float) mouseY);
                    }
                }
            }
        }
        
        wasRightPressed = rightPressed;
        
        // Обработка ЛКМ для клика по меню
        boolean leftPressed = GLFW.glfwGetMouseButton(
            mc.getWindow().getHandle(), 
            GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS;
        
        if (leftPressed && !wasLeftPressed) {
            // ЛКМ только что нажата
            if (ContextMenuManager.getInstance().hasOpenMenu()) {
                ContextMenuManager.getInstance().mouseClicked(mouseX, mouseY, 0);
            }
        }
        
        // Обработка перетаскивания слайдера (пока ЛКМ зажата)
        if (leftPressed) {
            ContextMenuManager.getInstance().mouseDragged(mouseX, mouseY, 0, 0, 0);
        }
        
        // Отпускание ЛКМ
        if (!leftPressed && wasLeftPressed) {
            ContextMenuManager.getInstance().mouseReleased(mouseX, mouseY, 0);
        }
        
        wasLeftPressed = leftPressed;
        
        // Обработка ESC
        boolean escPressed = GLFW.glfwGetKey(
            mc.getWindow().getHandle(), 
            GLFW.GLFW_KEY_ESCAPE
        ) == GLFW.GLFW_PRESS;
        
        if (escPressed && !wasEscPressed) {
            // ESC только что нажата
            if (ContextMenuManager.getInstance().hasOpenMenu()) {
                ContextMenuManager.getInstance().keyPressed(256); // GLFW_KEY_ESCAPE
            }
        }
        
        wasEscPressed = escPressed;
        
        // Рендерим меню поверх всего - теперь рендер происходит в InGameHudMixin
        // после всех HUD элементов, поэтому здесь не рендерим
        // ContextMenuManager.getInstance().render(ctx, (int) mouseX, (int) mouseY);
    }
    
    /**
     * Проверяет что клик по пустому месту (не по элементам GUI)
     */
    private boolean isClickOnEmptySpace(double mouseX, double mouseY) {
        // Для ChatScreen - всегда разрешаем (там нет элементов)
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            return true;
        }
        
        // Для InterfaceScreen (ClickGUI) - проверяем что клик ВНЕ GUI
        if (mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen) {
            farvix.solution.api.ui.clickgui.InterfaceScreen gui = 
                (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
            
            float guiX = gui.getX();
            float guiY = gui.getY();
            float guiW = gui.getWidth();
            float guiH = gui.getHeight();
            
            // Проверяем клик по главному окну GUI
            boolean clickedOnMainGUI = mouseX >= guiX && mouseX <= guiX + guiW && 
                                       mouseY >= guiY && mouseY <= guiY + guiH;
            
            // Проверяем клик по боковой панели (справа от GUI)
            float panelW = guiW / 3f;
            float panelGap = 8f;
            float panelX = guiX + guiW + panelGap;
            boolean clickedOnSidePanel = mouseX >= panelX && mouseX <= panelX + panelW &&
                                         mouseY >= guiY && mouseY <= guiY + guiH;
            
            // Если клик внутри GUI или боковой панели - НЕ открываем обычное меню
            if (clickedOnMainGUI || clickedOnSidePanel) {
                return false;
            }
            
            // TODO: Проверить клик по HUD элементам (окнам функций)
            // Если клик по HUD элементу - тоже НЕ открываем обычное меню
            // (для них будет отдельное меню модуля)
            
            // Клик вне GUI и вне HUD элементов - открываем обычное меню
            return true;
        }
        
        // Для других экранов - не открываем
        return false;
    }
}
