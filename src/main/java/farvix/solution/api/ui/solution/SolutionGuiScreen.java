package farvix.solution.api.ui.solution;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import farvix.solution.Client;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.ui.solution.components.*;
import farvix.solution.client.modules.impl.environment.ClickUI;

@Getter
@Setter
public class SolutionGuiScreen extends Screen implements QuickImports {
    
    // ═══════════════════════════════════════════════════════════════════════
    // РАЗМЕРЫ GUI (адаптивные, в процентах от экрана) - НОВЫЙ ДИЗАЙН
    // ═══════════════════════════════════════════════════════════════════════
    private static final float GUI_WIDTH_PERCENT = 0.40f;  // 40% ширины экрана (чуть больше)
    private static final float GUI_HEIGHT_PERCENT = 0.45f; // 45% высоты экрана (чуть больше)
    private static final int GUI_CORNER_RADIUS = 12;       // Больше скругление
    
    // ═══════════════════════════════════════════════════════════════════════
    // ПОЗИЦИЯ И РАЗМЕРЫ (вычисляются динамически)
    // ═══════════════════════════════════════════════════════════════════════
    private float x, y;
    private float width;
    private float height;
    
    // ═══════════════════════════════════════════════════════════════════════
    // КОМПОНЕНТЫ
    // ═══════════════════════════════════════════════════════════════════════
    private HeaderComponent header;
    private CategoryIconBar categoryBar; // Новая панель с иконками
    private TabNavigationBar tabBar;
    private ModuleGrid moduleGrid;
    private BottomIconBar iconBar;
    
    // ═══════════════════════════════════════════════════════════════════════
    // АНИМАЦИИ
    // ═══════════════════════════════════════════════════════════════════════
    private Animation openAnimation;    // Scale анимация
    private Animation fadeAnimation;    // Fade анимация
    private Animation slideAnimation;   // Slide снизу вверх
    private Animation tabSwitchAnimation; // Анимация переключения вкладок (свайп)
    
    // ═══════════════════════════════════════════════════════════════════════
    // СОСТОЯНИЕ
    // ═══════════════════════════════════════════════════════════════════════
    private SolutionTab currentTab = SolutionTab.VISUALS;
    private SolutionTab previousTab = SolutionTab.VISUALS;
    private int swipeDirection = 0; // -1 = влево, 1 = вправо, 0 = нет свайпа
    private String searchQuery = "";
    private String hoveredDescription = ""; // Описание модуля при наведении
    private boolean closing = false;
    
    // ═══════════════════════════════════════════════════════════════════════
    // ССЫЛКА НА МОДУЛЬ ClickUI
    // ═══════════════════════════════════════════════════════════════════════
    private ClickUI clickUIModule;
    
    public SolutionGuiScreen() {
        super(Text.of("SolutionVisual"));
    }
    
    @Override
    protected void init() {
        this.clickUIModule = Client.getInstance().getModuleManager().get(ClickUI.class);
        
        float guiScale = getGuiScaleFactor();
        net.minecraft.client.util.Window window = client.getWindow();
        float screenWidth = window.getScaledWidth() / guiScale;
        float screenHeight = window.getScaledHeight() / guiScale;

        // Sizing in percentage of screen
        this.width = 530f;  // Static width
        this.height = 325f; // Static height
        
        // Centered in the design space
        this.x = (screenWidth - width) / 2f;
        this.y = (screenHeight - height) / 2f;

        // Ensure bounds validation
        this.x = Math.max(10f, Math.min(this.x, screenWidth - width - 10f));
        this.y = Math.max(10f, Math.min(this.y, screenHeight - height - 10f));
        
        // ═══════════════════════════════════════════════════════════════════
        // ИНИЦИАЛИЗАЦИЯ АНИМАЦИЙ
        // ═══════════════════════════════════════════════════════════════════
        this.fadeAnimation = new Animation(Easing.EASE_OUT_CUBIC, 300);
        this.fadeAnimation.setValue(0);
        
        this.openAnimation = new Animation(Easing.EASE_OUT_CUBIC, 300);
        this.openAnimation.setValue(0.9f);
        
        this.slideAnimation = new Animation(Easing.EASE_OUT_CUBIC, 300);
        this.slideAnimation.setValue(0);
        
        this.tabSwitchAnimation = new Animation(Easing.EASE_OUT_CUBIC, 800); // Анимация свайпа (800ms)
        this.tabSwitchAnimation.setValue(1); // 1 = анимация завершена
        
        // ═══════════════════════════════════════════════════════════════════
        // ИНИЦИАЛИЗАЦИЯ КОМПОНЕНТОВ
        // ═══════════════════════════════════════════════════════════════════
        this.header = new HeaderComponent(this);
        this.categoryBar = new CategoryIconBar(this); // Новая панель с иконками
        this.tabBar = new TabNavigationBar(this);
        this.moduleGrid = new ModuleGrid(this);
        this.iconBar = new BottomIconBar(this);
        
        // Инициализируем компоненты
        this.header.init();
        this.categoryBar.init();
        this.tabBar.init();
        this.moduleGrid.init();
        this.iconBar.init();
        
        // ═══════════════════════════════════════════════════════════════════
        // ЗАПУСК АНИМАЦИЙ ОТКРЫТИЯ
        // ═══════════════════════════════════════════════════════════════════
        this.closing = false;
        this.fadeAnimation.reset();
        this.fadeAnimation.run(1);
        this.openAnimation.reset();
        this.openAnimation.run(1);
        this.slideAnimation.reset();
        this.slideAnimation.run(1);
        
        super.init();
    }
    
    public float getGuiScaleFactor() {
        return 1.0f;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float guiScale = getGuiScaleFactor();
        
        // Recalculate dimensions dynamically in real-time to support live window stretching!
        net.minecraft.client.util.Window window = client.getWindow();
        float screenWidth = window.getScaledWidth() / guiScale;
        float screenHeight = window.getScaledHeight() / guiScale;

        this.width = 530f;  // Static width
        this.height = 325f; // Static height
        
        // Centered in the design space
        this.x = (screenWidth - width) / 2f;
        this.y = (screenHeight - height) / 2f;

        // Ensure bounds validation
        this.x = Math.max(10f, Math.min(this.x, screenWidth - width - 10f));
        this.y = Math.max(10f, Math.min(this.y, screenHeight - height - 10f));
        
        // Also update sub-components coordinates in real time
        if (header != null) header.init();
        if (categoryBar != null) categoryBar.init();
        if (tabBar != null) tabBar.init();
        if (moduleGrid != null) moduleGrid.init();
        if (iconBar != null) iconBar.init();

        // ═══════════════════════════════════════════════════════════════════
        // ОБРАБОТКА АНИМАЦИЙ ЗАКРЫТИЯ
        // ═══════════════════════════════════════════════════════════════════
        if (closing) {
            fadeAnimation.run(0);
            openAnimation.run(0.95f);
            slideAnimation.run(0);
        } else {
            fadeAnimation.run(1);
            openAnimation.run(1);
            slideAnimation.run(1);
        }
        
        // Обновляем анимацию свайпа (если она активна)
        if (tabSwitchAnimation.getValue() < 1.0f) {
            tabSwitchAnimation.run(1); // Продолжаем анимацию до 1
        }
        
        // Если анимация закрытия завершена - закрываем GUI
        if (closing && fadeAnimation.getValue() <= 0.01f) {
            mc.setScreen(null);
            if (clickUIModule != null && clickUIModule.isEnabled()) {
                clickUIModule.setEnabled(false);
            }
            return;
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // ПОЛУЧАЕМ ЗНАЧЕНИЯ АНИМАЦИЙ
        // ═══════════════════════════════════════════════════════════════════
        float alpha = fadeAnimation.getValue();
        float scale = openAnimation.getValue();
        float slideProgress = slideAnimation.getValue();
        float slideY = (1f - slideProgress) * 20f; // Slide снизу вверх
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ ЗАТЕМНЕННОГО ФОНА (меньше blur)
        // ═══════════════════════════════════════════════════════════════════
        int backdropAlpha = (int)(80 * alpha); // Уменьшили с 150 до 80
        if (backdropAlpha > 0) {
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                    context.getMatrices(),
                    0f, 0f,
                    window.getScaledWidth(),
                    window.getScaledHeight())
                    .round(0)
                    .softness(0)
                    .thickness(0)
                    .outlineColor(0)
                    .color(new farvix.solution.api.util.color.FixColor(0, 0, 0, backdropAlpha).getRGB())
                    .build());
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // ПРИМЕНЯЕМ ТРАНСФОРМАЦИИ (SCALE + SLIDE)
        // ═══════════════════════════════════════════════════════════════════
        context.getMatrices().push();
        
        // Apply GUI Scale Factor to fit the design resolution perfectly

        context.getMatrices().scale(guiScale, guiScale, 1.0f);
        
        // Scale mouse coordinates to match the 960-wide design space
        int scaledMouseX = (int) (mouseX / guiScale);
        int scaledMouseY = (int) (mouseY / guiScale);
        
        // Slide снизу вверх
        context.getMatrices().translate(0, slideY, 0);
        
        // Scale от центра GUI
        if (scale != 1.0f) {
            float centerX = x + width / 2f;
            float centerY = y + height / 2f;
            context.getMatrices().translate(centerX, centerY, 0);
            context.getMatrices().scale(scale, scale, 1);
            context.getMatrices().translate(-centerX, -centerY, 0);
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ КОМПОНЕНТОВ КОТОРЫЕ НЕ ДВИГАЮТСЯ
        // ═══════════════════════════════════════════════════════════════════
        // Сбрасываем описание перед рендером — ModuleCard установит его если нужно
        this.hoveredDescription = "";
        header.render(context, scaledMouseX, scaledMouseY, delta);
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ СОДЕРЖИМОГО (БЕЗ АНИМАЦИИ СВАЙПА)
        // ═══════════════════════════════════════════════════════════════════
        renderMainBackground(context, alpha);
        tabBar.render(context, scaledMouseX, scaledMouseY, delta);
        moduleGrid.render(context, scaledMouseX, scaledMouseY, delta);
        iconBar.render(context, scaledMouseX, scaledMouseY, delta);
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ CATEGORYBAR ПОВЕРХ ВСЕГО (чтобы кнопки были видны)
        // ═══════════════════════════════════════════════════════════════════
        categoryBar.render(context, scaledMouseX, scaledMouseY, delta);
        
        context.getMatrices().pop();
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * Рендерит главный фон GUI в темном стиле с blur эффектом
     */
    private void renderMainBackground(DrawContext context, float alpha) {
        // ═══════════════════════════════════════════════════════════════════
        // ОЧЕНЬ ТЕМНЫЙ ФОН С BLUR ЭФФЕКТОМ (почти черный)
        // ═══════════════════════════════════════════════════════════════════
        blur.render(farvix.solution.api.render.rect.ShapeProperties.create(
                context.getMatrices(),
                x, y, width, height)
                .round(GUI_CORNER_RADIUS)
                .softness(8f) // Мягкое размытие
                .thickness(1f) // Тонкая рамка
                .outlineColor(new farvix.solution.api.util.color.FixColor(50, 50, 55, (int)(76 * alpha)).getRGB()) // Светлая рамка
                .color(new farvix.solution.api.util.color.FixColor(10, 10, 12, (int)(250 * alpha)).getRGB()) // Почти черный фон (98% прозрачности)
                .build());
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float guiScale = getGuiScaleFactor();
        double scaledX = mouseX / guiScale;
        double scaledY = mouseY / guiScale;
        
        // Трансформируем координаты мыши с учетом scale
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float currentScale = openAnimation.getValue();
        
        double transformedX = (scaledX - centerX) / currentScale + centerX;
        double transformedY = (scaledY - centerY) / currentScale + centerY;
        
        // Передаем клики компонентам (без учета swipeOffset)
        if (categoryBar.mouseClicked(transformedX, transformedY, button)) return true;
        if (tabBar.mouseClicked(transformedX, transformedY, button)) return true;
        if (moduleGrid.mouseClicked(transformedX, transformedY, button)) return true;
        if (iconBar.mouseClicked(transformedX, transformedY, button)) return true;
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float guiScale = getGuiScaleFactor();
        double scaledX = mouseX / guiScale;
        double scaledY = mouseY / guiScale;
        moduleGrid.mouseScrolled(scaledX, scaledY, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC - закрыть GUI
        if (keyCode == 256) {
            closing = true;
            return true;
        }
        
        // Передаем нажатия клавиш компонентам
        tabBar.keyPressed(keyCode, scanCode, modifiers);
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        tabBar.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Не рендерим стандартный фон
    }
    
    /**
     * Переключить вкладку (БЕЗ анимации свайпа)
     */
    public void switchTab(SolutionTab newTab) {
        if (this.currentTab != newTab) {
            this.previousTab = this.currentTab;
            this.currentTab = newTab;
            
            // Обновляем фильтр модулей
            this.moduleGrid.filterByTab(newTab);
        }
    }
    
    /**
     * Получить индекс вкладки для определения направления свайпа
     */
    private int getTabIndex(SolutionTab tab) {
        switch (tab) {
            case VISUALS: return 0;
            case HUD: return 1;
            case UTILITIES: return 2;
            default: return 0;
        }
    }
    
    /**
     * Получить смещение X для анимации свайпа
     */
    public float getSwipeOffset() {
        float progress = tabSwitchAnimation.getValue();
        if (progress >= 1.0f) {
            return 0; // Анимация завершена
        }
        
        // Свайп до краев экрана: используем ширину всего окна вместо ширины GUI
        float screenWidth = mc.getWindow().getScaledWidth() / getGuiScaleFactor();
        float offset = (1.0f - progress) * screenWidth * swipeDirection;
        
        return offset;
    }
    
    /**
     * Проверить, идет ли анимация свайпа
     */
    public boolean isSwipeAnimating() {
        return tabSwitchAnimation.getValue() < 1.0f;
    }
    
    /**
     * Обновить поисковый запрос
     */
    public void updateSearchQuery(String query) {
        this.searchQuery = query;
        this.moduleGrid.filterBySearch(query);
    }
    
    /**
     * Получить альфа анимации для синхронизации компонентов
     */
    public float getAlpha() {
        return fadeAnimation.getValue();
    }

    /**
     * Установить описание модуля при наведении (вызывается из ModuleCard)
     */
    public void setHoveredDescription(String description) {
        this.hoveredDescription = description != null ? description : "";
    }

    /**
     * Получить описание модуля при наведении
     */
    public String getHoveredDescription() {
        return hoveredDescription;
    }
}
