package farvix.solution.api.ui.solution.components;

import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.ui.solution.SolutionGuiScreen;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;

/**
 * Карточка модуля в темном стиле
 * Этап 2 из плана
 */
public class ModuleCard implements QuickImports {
    
    private final SolutionGuiScreen parent;
    private final Module module;
    
    // ═══════════════════════════════════════════════════════════════════════
    // РАЗМЕРЫ (адаптивные, в процентах от размера экрана)
    // ═══════════════════════════════════════════════════════════════════════
    private static final float CARD_MIN_HEIGHT_PERCENT = 0.050f; // 5.0% высоты экрана
    private static final float CARD_PADDING_PERCENT = 0.008f;    // 0.8% высоты экрана
    private static final float CORNER_RADIUS_PERCENT = 0.008f;   // 0.8% высоты экрана
    private static final float TITLE_FONT_SIZE_PERCENT = 0.044f; // 4.4% высоты экрана
    private static final float DESC_FONT_SIZE_PERCENT  = 0.044f; // увеличено и жирнее (= titleFontSize)
    private static final float SETTINGS_ICON_SIZE_PERCENT = 0.016f; // 1.6% высоты экрана

    // Фиксированный зазор между названием и описанием (пиксели)
    private static final float TITLE_DESC_GAP_PX = 5f;

    // Адаптивные отступы для текста
    private static final float TEXT_VERTICAL_OFFSET_PERCENT = 0.026f;
    private static final float TITLE_DESC_SPACING_PERCENT = 0.028f;
    private static final float TITLE_LIFT_PERCENT = 0.005f;
    private static final float SETTINGS_VERTICAL_OFFSET_PERCENT = 0.021f;
    private static final float TITLE_DOWN_OFFSET_PERCENT = 0.022f;
    
    // ═══════════════════════════════════════════════════════════════════════
    // ПОЗИЦИЯ И РАЗМЕРЫ (вычисляются динамически)
    // ═══════════════════════════════════════════════════════════════════════
    private float x, y;
    private float width, height;
    
    // ═══════════════════════════════════════════════════════════════════════
    // АНИМАЦИИ
    // ═══════════════════════════════════════════════════════════════════════
    private Animation hoverAnimation;
    private Animation enableAnimation;
    private Animation settingsHoverAnimation;
    
    // ═══════════════════════════════════════════════════════════════════════
    // СОСТОЯНИЕ
    // ═══════════════════════════════════════════════════════════════════════
    private boolean settingsHovered = false;
    
    public ModuleCard(SolutionGuiScreen parent, Module module) {
        this.parent = parent;
        this.module = module;
    }
    
    public void init() {
        // Инициализация анимаций
        this.hoverAnimation = new Animation(Easing.EASE_OUT_SINE, 150);
        this.enableAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);
        this.settingsHoverAnimation = new Animation(Easing.EASE_OUT_SINE, 150);
    }
    
    public void setPosition(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float alpha = parent.getAlpha();
        if (alpha <= 0.01f) return;
        
        // ═══════════════════════════════════════════════════════════════════
        // ОБНОВЛЕНИЕ АНИМАЦИЙ
        // ═══════════════════════════════════════════════════════════════════
        boolean isHovered = isMouseOver(mouseX, mouseY);
        hoverAnimation.run(isHovered ? 1 : 0);
        enableAnimation.run(module.isEnabled() ? 1 : 0);
        
        // При наведении — передаём описание в родительский экран
        if (isHovered) {
            parent.setHoveredDescription(module.getDescription());
        }
        
        // Проверяем hover на иконке настроек
        settingsHovered = isMouseOverSettings(mouseX, mouseY);
        settingsHoverAnimation.run(settingsHovered ? 1 : 0);
        
        float hoverProgress = hoverAnimation.getValue();
        float enableProgress = enableAnimation.getValue();
        float settingsHoverProgress = settingsHoverAnimation.getValue();
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ ФОНА КАРТОЧКИ
        // ═══════════════════════════════════════════════════════════════════
        renderBackground(context, hoverProgress, enableProgress, alpha);
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ СОДЕРЖИМОГО
        // ═══════════════════════════════════════════════════════════════════
        renderContent(context, settingsHoverProgress, alpha);
    }
    
    /**
     * Рендерит фон карточки (черно-белая тема, включенные чуть светлее)
     */
    private void renderBackground(DrawContext context, float hoverProgress, float enableProgress, float alpha) {
        float cornerRadius = 540f * CORNER_RADIUS_PERCENT;
        
        // Базовый темный цвет (выключенный модуль)
        java.awt.Color baseColor = new java.awt.Color(25, 25, 28);
        
        // Цвет для включенного модуля (чуть светлее/белее)
        java.awt.Color enabledColor = new java.awt.Color(45, 45, 50);
        
        // Цвет при hover (еще чуть светлее)
        java.awt.Color hoverColor = new java.awt.Color(55, 55, 60);
        
        // Интерполяция цветов
        java.awt.Color currentColor = baseColor;
        if (module.isEnabled()) {
            currentColor = enabledColor;
        }
        if (hoverProgress > 0.01f) {
            currentColor = lerp(currentColor, hoverColor, hoverProgress * 0.5f);
        }
        
        // Рендерим фон с glass
        glass.render(farvix.solution.api.render.rect.ShapeProperties.create(
                context.getMatrices(),
                x, y, width, height)
                .round(cornerRadius)
                .softness(4f)
                .thickness(0)
                .outlineColor(0)
                .color(new FixColor(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), (int)(230 * alpha)).getRGB())
                .build());
        
        // Тонкая обводка
        int outlineAlpha = module.isEnabled() ? (int)(30 * alpha) : (int)(15 * alpha);
        java.awt.Color outlineColor = new java.awt.Color(60, 60, 70);
        
        rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                context.getMatrices(),
                x, y, width, height)
                .round(cornerRadius)
                .softness(0)
                .thickness(0.5f)
                .outlineColor(new FixColor(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), outlineAlpha).getRGB())
                .color(0)
                .build());
    }
    
    /**
     * Рендерит содержимое карточки (описание сверху, название снизу, иконка настроек)
     */
    private void renderContent(DrawContext context, float settingsHoverProgress, float alpha) {
        float screenHeight = 540f;
        float padding = screenHeight * CARD_PADDING_PERCENT;
        int titleFontSize = (int)(screenHeight * TITLE_FONT_SIZE_PERCENT);
        int descFontSize  = (int)(screenHeight * DESC_FONT_SIZE_PERCENT);
        int settingsIconSize = (int)(screenHeight * SETTINGS_ICON_SIZE_PERCENT);

        float settingsVerticalOffset = screenHeight * SETTINGS_VERTICAL_OFFSET_PERCENT;

        // ── Измеряем высоту текстового блока ──────────────────────────────────
        float titleH = Fonts.SEMIBOLD.get(titleFontSize).getStringHeight("Ag") / 2f;

        // Центрируем название вертикально в карточке
        float titleX = x + padding;
        float titleY = y + (height - titleH) / 2f - 2f;

        // ═══════════════════════════════════════════════════════════════════
        // НАЗВАНИЕ МОДУЛЯ
        // ═══════════════════════════════════════════════════════════════════
        java.awt.Color titleColor = module.isEnabled() ?
                new java.awt.Color(255, 255, 255) :
                new java.awt.Color(200, 200, 210);
        titleColor = withAlpha(titleColor, alpha);

        String title = module.getName();
        if (title == null || title.isEmpty()) title = "Unknown Module";

        float maxTitleWidth = width - padding * 2 - settingsIconSize - padding;
        float titleWidth = Fonts.SEMIBOLD.get(titleFontSize).getStringWidth(title);
        if (titleWidth > maxTitleWidth) {
            while (titleWidth > maxTitleWidth && title.length() > 3) {
                title = title.substring(0, title.length() - 1);
                titleWidth = Fonts.SEMIBOLD.get(titleFontSize).getStringWidth(title + "...");
            }
            title = title + "...";
        }

        Fonts.SEMIBOLD.get(titleFontSize).drawString(
                context.getMatrices(), title, titleX, titleY, titleColor.getRGB());

        // ═══════════════════════════════════════════════════════════════════
        // ИКОНКА НАСТРОЕК (⚙️)
        // ═══════════════════════════════════════════════════════════════════
        String settingsIcon = "⚙";
        float settingsIconWidth  = Fonts.ICONS.get(settingsIconSize).getStringWidth(settingsIcon);
        float settingsIconHeight = Fonts.ICONS.get(settingsIconSize).getStringHeight(settingsIcon) / 2f;
        float settingsX = x + width - padding - settingsIconWidth;
        // Центрируем иконку по вертикали карточки — симметрично
        float settingsY = y + (height - settingsIconHeight) / 2f;
        
        // Цвет иконки (светлеет при hover)
        java.awt.Color settingsColor = lerp(
                new java.awt.Color(120, 120, 130),
                new java.awt.Color(200, 200, 210),
                settingsHoverProgress
        );
        settingsColor = withAlpha(settingsColor, alpha);
        
        // Rotation эффект при hover
        float rotation = settingsHoverProgress * 90f; // Поворот на 90 градусов
        
        if (rotation > 0.1f) {
            context.getMatrices().push();
            float centerX = settingsX + settingsIconWidth / 2f;
            float centerY = settingsY + settingsIconHeight / 2f;
            context.getMatrices().translate(centerX, centerY, 0);
            context.getMatrices().multiply(
                    net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rotation)
            );
            context.getMatrices().translate(-centerX, -centerY, 0);
        }
        
        Fonts.ICONS.get(settingsIconSize).drawString(
                context.getMatrices(),
                settingsIcon,
                settingsX,
                settingsY,
                settingsColor.getRGB()
        );
        
        if (rotation > 0.1f) {
            context.getMatrices().pop();
        }
    }
    
    /**
     * Проверяет, находится ли мышь над карточкой
     */
    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Проверяет, находится ли мышь над иконкой настроек
     */
    private boolean isMouseOverSettings(double mouseX, double mouseY) {
        float padding = 540f * CARD_PADDING_PERCENT;
        int settingsIconSize = (int)(540f * SETTINGS_ICON_SIZE_PERCENT);
        
        float settingsX = x + width - padding - settingsIconSize;
        float settingsY = y + padding;
        float settingsSize = settingsIconSize + padding;
        
        return mouseX >= settingsX && mouseX <= settingsX + settingsSize &&
               mouseY >= settingsY && mouseY <= settingsY + settingsSize;
    }
    
    /**
     * Обработка клика мыши
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false; // Только ЛКМ
        
        if (isMouseOver(mouseX, mouseY)) {
            if (isMouseOverSettings(mouseX, mouseY)) {
                // TODO: Открыть панель настроек модуля
                System.out.println("Open settings for: " + module.getName());
                return true;
            } else {
                // Переключить модуль
                module.toggle();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Интерполяция цветов
     */
    private java.awt.Color lerp(java.awt.Color from, java.awt.Color to, float progress) {
        int r = (int)(from.getRed() + (to.getRed() - from.getRed()) * progress);
        int g = (int)(from.getGreen() + (to.getGreen() - from.getGreen()) * progress);
        int b = (int)(from.getBlue() + (to.getBlue() - from.getBlue()) * progress);
        int a = (int)(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * progress);
        return new java.awt.Color(r, g, b, a);
    }
    
    /**
     * Применить альфа к цвету
     */
    private java.awt.Color withAlpha(java.awt.Color color, float alpha) {
        return new java.awt.Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int)(color.getAlpha() * alpha)
        );
    }
    
    public Module getModule() {
        return module;
    }
    
    public float getHeight() {
        return height;
    }
}
