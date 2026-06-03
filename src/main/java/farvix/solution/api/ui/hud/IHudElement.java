package farvix.solution.api.ui.hud;

import farvix.solution.client.modules.Module;

/**
 * Интерфейс для HUD элементов которые можно кликать
 */
public interface IHudElement {
    
    /**
     * Получить X координату элемента
     */
    float getHudX();
    
    /**
     * Получить Y координату элемента
     */
    float getHudY();
    
    /**
     * Получить ширину элемента
     */
    float getHudWidth();
    
    /**
     * Получить высоту элемента
     */
    float getHudHeight();
    
    /**
     * Получить модуль связанный с этим HUD элементом
     */
    Module getModule();
    
    /**
     * Проверить попадает ли точка в границы элемента
     */
    default boolean isHovered(double mouseX, double mouseY) {
        float x = getHudX();
        float y = getHudY();
        float w = getHudWidth();
        float h = getHudHeight();
        
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
