package farvix.solution.api.ui.solution.components;

import net.minecraft.client.gui.DrawContext;
import farvix.solution.Client;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.ui.solution.SolutionGuiScreen;
import farvix.solution.api.ui.solution.SolutionTab;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Компонент сетки модулей (2 колонки)
 * Этап 2 из плана
 */
public class ModuleGrid implements QuickImports {
    
    private final SolutionGuiScreen parent;
    
    // ═══════════════════════════════════════════════════════════════════════
    // РАЗМЕРЫ (адаптивные, в процентах от размера экрана)
    // ═══════════════════════════════════════════════════════════════════════
    private static final float GRID_SIDE_PADDING_PERCENT = 0.015f;  // 1.5% высоты экрана (~15px на 1080p) - отступ от краев GUI (было 10px, +5px)
    private static final float GRID_TOP_PADDING_PERCENT = 0.015f;   // 1.5% высоты экрана - отступ сверху
    private static final float GRID_GAP_PERCENT = 0.020f;           // 2.0% высоты экрана (~20px на 1080p) - отступ между карточками (было 10px, +10px)
    private static final float CARD_MIN_HEIGHT_PERCENT = 0.050f;    // 5.0% высоты экрана (уменьшено для компактности)
    private static final int COLUMNS = 2;                           // 2 колонки
    
    // ═══════════════════════════════════════════════════════════════════════
    // СОСТОЯНИЕ
    // ═══════════════════════════════════════════════════════════════════════
    private List<ModuleCard> visibleCards = new ArrayList<>();
    private List<Module> allModules = new ArrayList<>();
    private SolutionTab currentTab = SolutionTab.VISUALS;
    private String searchQuery = "";
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f; // Целевое значение скролла для плавной анимации
    private float maxScrollOffset = 0f;
    
    public ModuleGrid(SolutionGuiScreen parent) {
        this.parent = parent;
    }
    
    public void init() {
        // Получаем все модули из ModuleManager
        allModules = Client.getInstance().getModuleManager().getModules();
        
        // Фильтруем по текущей вкладке
        filterByTab(currentTab);
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float alpha = parent.getAlpha();
        if (alpha <= 0.01f) return;
        
        // ═══════════════════════════════════════════════════════════════════
        // ПЛАВНАЯ АНИМАЦИЯ СКРОЛЛА
        // ═══════════════════════════════════════════════════════════════════
        float scrollSpeed = 0.15f; // Скорость интерполяции (0.15 = плавно)
        scrollOffset += (targetScrollOffset - scrollOffset) * scrollSpeed;
        
        // Если разница меньше 0.1px, считаем что достигли цели
        if (Math.abs(targetScrollOffset - scrollOffset) < 0.1f) {
            scrollOffset = targetScrollOffset;
        }
        
        float guiX = parent.getX();
        float guiY = parent.getY();
        float guiWidth = parent.getWidth();
        float guiHeight = parent.getHeight();
        
        // Fixed design resolution height
        float screenHeight = 540f;
        
        float gridSidePadding = screenHeight * GRID_SIDE_PADDING_PERCENT;
        float gridTopPadding = screenHeight * GRID_TOP_PADDING_PERCENT;
        float gridGap = screenHeight * GRID_GAP_PERCENT;
        float cardMinHeight = screenHeight * CARD_MIN_HEIGHT_PERCENT;
        
        // ═══════════════════════════════════════════════════════════════════
        // ОБЛАСТЬ СЕТКИ (справа от CategoryBar, под Header)
        // ═══════════════════════════════════════════════════════════════════
        float gridX = guiX + gridSidePadding;
        float gridY = guiY + 30f + gridTopPadding; // 30px для header (поднимаем до красной метки)
        float gridWidth = guiWidth - gridSidePadding * 2;
        float gridHeight = guiHeight - 30f - gridTopPadding * 2; // 30px для header
        
        // ═══════════════════════════════════════════════════════════════════
        // ВЫЧИСЛЯЕМ РАЗМЕРЫ КАРТОЧЕК
        // ═══════════════════════════════════════════════════════════════════
        float cardWidth = (gridWidth - gridGap) / COLUMNS;
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ КАРТОЧЕК В 2 КОЛОНКИ
        // ═══════════════════════════════════════════════════════════════════
        float currentY = gridY - scrollOffset;
        int column = 0;
        float rowStartY = currentY;
        
        // Включаем scissor для обрезки карточек за пределами области
        // Расширяем область на 10px со всех сторон для blur эффекта
        enableScissor(context, (int)(gridX - 10), (int)(gridY - 10), (int)(gridWidth + 20), (int)(gridHeight + 20));
        
        for (ModuleCard card : visibleCards) {
            float cardX = gridX + column * (cardWidth + gridGap);
            float cardY = rowStartY;
            
            // Устанавливаем позицию и размеры карточки
            card.setPosition(cardX, cardY, cardWidth, cardMinHeight);
            
            // Рендерим только если карточка хотя бы частично видна (с запасом 20px)
            if (cardY + cardMinHeight >= gridY - 20 && cardY <= gridY + gridHeight + 20) {
                card.render(context, mouseX, mouseY, delta);
            }
            
            // Переходим к следующей колонке
            column++;
            if (column >= COLUMNS) {
                column = 0;
                rowStartY += cardMinHeight + gridGap;
            }
        }
        
        disableScissor(context);
        
        // ═══════════════════════════════════════════════════════════════════
        // ВЫЧИСЛЯЕМ МАКСИМАЛЬНЫЙ СКРОЛЛ
        // ═══════════════════════════════════════════════════════════════════
        int rows = (int)Math.ceil((double)visibleCards.size() / COLUMNS);
        float totalHeight = rows * (cardMinHeight + gridGap) - gridGap;
        maxScrollOffset = Math.max(0, totalHeight - gridHeight + 10f); // +10px дополнительного места для скролла
    }
    
    /**
     * Включает scissor для обрезки содержимого
     */
    private void enableScissor(DrawContext context, int x, int y, int width, int height) {
        double scale = mc.getWindow().getScaleFactor() * parent.getGuiScaleFactor();
        int scaledX = (int)(x * scale);
        int scaledY = (int)((mc.getWindow().getScaledHeight() - (y + height) * parent.getGuiScaleFactor()) * mc.getWindow().getScaleFactor());
        int scaledWidth = (int)(width * scale);
        int scaledHeight = (int)(height * scale);
        
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(scaledX, scaledY, scaledWidth, scaledHeight);
    }
    
    /**
     * Отключает scissor
     */
    private void disableScissor(DrawContext context) {
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Передаем клики карточкам
        for (ModuleCard card : visibleCards) {
            if (card.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }
    
    public void mouseScrolled(double mouseX, double mouseY, double delta) {
        // Скроллинг с плавной анимацией
        float scrollSpeed = 20f;
        targetScrollOffset -= (float)delta * scrollSpeed;
        
        // Ограничиваем целевой скролл
        targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScrollOffset));
    }
    
    /**
     * Фильтрует модули по вкладке
     */
    public void filterByTab(SolutionTab tab) {
        this.currentTab = tab;
        updateVisibleCards();
    }
    
    /**
     * Фильтрует модули по поисковому запросу
     */
    public void filterBySearch(String query) {
        this.searchQuery = query.toLowerCase();
        updateVisibleCards();
    }
    
    /**
     * Обновляет список видимых карточек на основе фильтров
     */
    private void updateVisibleCards() {
        // Получаем категорию для текущей вкладки
        ModuleCategory category = getModuleCategoryForTab(currentTab);
        
        // Фильтруем модули
        List<Module> filteredModules = allModules.stream()
                .filter(module -> {
                    // Фильтр по категории
                    if (category != null && module.getCategory() != category) {
                        return false;
                    }
                    
                    // Фильтр по поиску
                    if (!searchQuery.isEmpty()) {
                        String moduleName = module.getName().toLowerCase();
                        String moduleDesc = module.getDescription().toLowerCase();
                        return moduleName.contains(searchQuery) || moduleDesc.contains(searchQuery);
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        // Создаем карточки для отфильтрованных модулей
        visibleCards.clear();
        for (Module module : filteredModules) {
            ModuleCard card = new ModuleCard(parent, module);
            card.init();
            visibleCards.add(card);
        }
        
        // Сбрасываем скролл
        scrollOffset = 0f;
        targetScrollOffset = 0f;
    }
    
    /**
     * Получает категорию модулей для вкладки
     */
    private ModuleCategory getModuleCategoryForTab(SolutionTab tab) {
        switch (tab) {
            case VISUALS:
                return ModuleCategory.VISUALS;
            case HUD:
                return ModuleCategory.ENVIRONMENT;
            case UTILITIES:
                return ModuleCategory.WAYPOINTS;
            default:
                return null;
        }
    }
}

