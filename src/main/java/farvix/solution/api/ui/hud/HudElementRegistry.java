package farvix.solution.api.ui.hud;

import java.util.ArrayList;
import java.util.List;

/**
 * Реестр HUD элементов для обработки кликов
 */
public class HudElementRegistry {
    
    private static HudElementRegistry instance;
    private final List<IHudElement> elements = new ArrayList<>();
    
    private HudElementRegistry() {
    }
    
    public static HudElementRegistry getInstance() {
        if (instance == null) {
            instance = new HudElementRegistry();
        }
        return instance;
    }
    
    /**
     * Зарегистрировать HUD элемент
     */
    public void register(IHudElement element) {
        if (!elements.contains(element)) {
            elements.add(element);
        }
    }
    
    /**
     * Удалить HUD элемент из реестра
     */
    public void unregister(IHudElement element) {
        elements.remove(element);
    }
    
    /**
     * Найти HUD элемент по координатам клика
     * @return HUD элемент или null если не найден
     */
    public IHudElement findElementAt(double mouseX, double mouseY) {
        // Проверяем в обратном порядке (последние добавленные сверху)
        for (int i = elements.size() - 1; i >= 0; i--) {
            IHudElement element = elements.get(i);
            if (element.isHovered(mouseX, mouseY)) {
                return element;
            }
        }
        return null;
    }
    
    /**
     * Получить все зарегистрированные элементы
     */
    public List<IHudElement> getElements() {
        return new ArrayList<>(elements);
    }
    
    /**
     * Очистить реестр
     */
    public void clear() {
        elements.clear();
    }
}
