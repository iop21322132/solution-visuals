package farvix.solution.api.ui.solution.components;

import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.ui.solution.SolutionGuiScreen;
import farvix.solution.api.ui.solution.SolutionTab;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.api.ModuleCategory;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Панель с иконками категорий (как на втором скриншоте)
 * Располагается ниже заголовка, показывает все категории с иконками
 */
public class CategoryIconBar implements QuickImports {
    
    private final SolutionGuiScreen parent;
    
    // ═══════════════════════════════════════════════════════════════════════
    // РАЗМЕРЫ (адаптивные, в процентах от высоты экрана)
    // ═══════════════════════════════════════════════════════════════════════
    private static final float ICON_SIZE_PERCENT = 0.030f;        // 3% от высоты экрана
    private static final float ICON_BUTTON_SIZE_PERCENT = 0.055f; // 5.5% от высоты экрана
    private static final float CONTAINER_PADDING_PERCENT = 0.008f; // 0.8% от высоты экрана
    private static final float BUTTON_SPACING_PERCENT = 0.008f;   // 0.8% от высоты экрана
    private static final float BAR_LEFT_MARGIN = -30f;              // Отступ слева от края GUI (отрицательный = выходит за край влево на 30px)
    
    // ═══════════════════════════════════════════════════════════════════════
    // КАТЕГОРИИ ДЛЯ ОТОБРАЖЕНИЯ
    // ═══════════════════════════════════════════════════════════════════════
    private static final CategoryInfo[] CATEGORIES = {
        new CategoryInfo(SolutionTab.VISUALS, ModuleCategory.VISUALS),
        new CategoryInfo(SolutionTab.HUD, ModuleCategory.ENVIRONMENT),
        new CategoryInfo(SolutionTab.PLAYER, ModuleCategory.PLAYER),
        new CategoryInfo(SolutionTab.UTILITIES, ModuleCategory.WAYPOINTS),
        new CategoryInfo(null, ModuleCategory.THEMES),
        new CategoryInfo(null, ModuleCategory.CONFIGS),
        new CategoryInfo(null, ModuleCategory.MACRO)
    };
    
    // ═══════════════════════════════════════════════════════════════════════
    // АНИМАЦИИ
    // ═══════════════════════════════════════════════════════════════════════
    private final Map<Integer, Animation> hoverAnimations = new HashMap<>();
    private final Map<Integer, Animation> activeAnimations = new HashMap<>();
    
    // ═══════════════════════════════════════════════════════════════════════
    // СОСТОЯНИЕ
    // ═══════════════════════════════════════════════════════════════════════
    private int hoveredIndex = -1;
    
    public CategoryIconBar(SolutionGuiScreen parent) {
        this.parent = parent;
    }
    
    public void init() {
        // Инициализация анимаций для каждой категории
        for (int i = 0; i < CATEGORIES.length; i++) {
            hoverAnimations.put(i, new Animation(Easing.EASE_OUT_SINE, 150));
            activeAnimations.put(i, new Animation(Easing.EASE_OUT_CUBIC, 200));
        }
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float alpha = parent.getAlpha();
        if (alpha <= 0.01f) return;
        
        float guiX = parent.getX();
        float guiY = parent.getY();
        float guiWidth = parent.getWidth();
        float guiHeight = parent.getHeight();
        
        // Fixed design resolution height
        float screenHeight = 540f;
        
        float iconSize = screenHeight * ICON_SIZE_PERCENT;
        float iconButtonSize = screenHeight * ICON_BUTTON_SIZE_PERCENT;
        float containerPadding = screenHeight * CONTAINER_PADDING_PERCENT;
        float buttonSpacing = screenHeight * BUTTON_SPACING_PERCENT;
        
        // ═══════════════════════════════════════════════════════════════════
        // ПОЗИЦИЯ ПАНЕЛИ (СЛЕВА ВНУТРИ GUI, ВЕРТИКАЛЬНО)
        // ═══════════════════════════════════════════════════════════════════
        float barX = guiX + BAR_LEFT_MARGIN;
        float barStartY = guiY + 5f; // Небольшой отступ сверху (5px)
        
        // ═══════════════════════════════════════════════════════════════════
        // ВЫЧИСЛЯЕМ ОБЩУЮ ВЫСОТУ ВСЕХ ИКОНОК (вертикальное расположение)
        // ═══════════════════════════════════════════════════════════════════
        float totalHeight = (CATEGORIES.length * iconButtonSize) + ((CATEGORIES.length - 1) * buttonSpacing);
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ ПАНЕЛИ С ОЧЕНЬ МАЛЕНЬКИМИ РАМКАМИ
        // ═══════════════════════════════════════════════════════════════════
        float containerX = barX - containerPadding;
        float containerY = barStartY - containerPadding;
        float containerWidth = iconButtonSize + containerPadding * 2;
        float containerHeight = totalHeight + containerPadding * 2;
        
        // ОСНОВНОЙ BLUR С ОЧЕНЬ МАЛЕНЬКИМИ РАМКАМИ
        blur.render(farvix.solution.api.render.rect.ShapeProperties.create(
                context.getMatrices(),
                containerX, containerY, containerWidth, containerHeight)
                .round(screenHeight * 0.015f)
                .softness(6.25f) // Еще в 2 раза меньше размытие краев
                .thickness(0)
                .outlineColor(0)
                .color(new FixColor(18, 18, 22, (int)(200 * alpha)).getRGB()) // Более светлый и прозрачный
                .build());
        
        // ДОПОЛНИТЕЛЬНЫЙ СЛОЙ BLUR (для усиления эффекта)
        float inset = 1.25f; // Еще в 2 раза меньше отступ
        blur.render(farvix.solution.api.render.rect.ShapeProperties.create(
                context.getMatrices(),
                containerX + inset, containerY + inset, containerWidth - inset * 2, containerHeight - inset * 2)
                .round(screenHeight * 0.012f)
                .softness(3.75f) // Еще в 2 раза меньше размытие
                .thickness(0)
                .outlineColor(0)
                .color(new FixColor(12, 12, 18, (int)(200 * alpha)).getRGB()) // Темнее внутри
                .build());
        
        // ═══════════════════════════════════════════════════════════════════
        // ОБНОВЛЕНИЕ HOVER СОСТОЯНИЯ
        // ═══════════════════════════════════════════════════════════════════
        hoveredIndex = -1;
        for (int i = 0; i < CATEGORIES.length; i++) {
            float iconY = barStartY + i * (iconButtonSize + buttonSpacing);
            if (isMouseOverIcon(mouseX, mouseY, barX, iconY, iconButtonSize)) {
                hoveredIndex = i;
                break;
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ ИКОНОК (вертикально)
        // ═══════════════════════════════════════════════════════════════════
        for (int i = 0; i < CATEGORIES.length; i++) {
            float iconY = barStartY + i * (iconButtonSize + buttonSpacing);
            renderCategoryIcon(context, i, barX, iconY, alpha, iconSize, iconButtonSize);
        }
    }
    
    /**
     * Рендерит одну иконку категории
     */
    private void renderCategoryIcon(DrawContext context, int index, float x, float y, float alpha, float iconSize, float iconButtonSize) {
        CategoryInfo category = CATEGORIES[index];
        boolean isActive = category.tab != null && parent.getCurrentTab() == category.tab;
        boolean isHovered = hoveredIndex == index;
        
        // Обновляем анимации
        Animation hoverAnim = hoverAnimations.get(index);
        Animation activeAnim = activeAnimations.get(index);
        
        hoverAnim.run(isHovered ? 1 : 0);
        activeAnim.run(isActive ? 1 : 0);
        
        float hoverProgress = hoverAnim.getValue();
        float activeProgress = activeAnim.getValue();
        
        // Адаптивное скругление в дизайне
        float screenHeight = 540f;
        float cornerRadius = screenHeight * 0.01f;
        
        // ═══════════════════════════════════════════════════════════════════
        // ФОН ИКОНКИ (при hover или active)
        // ═══════════════════════════════════════════════════════════════════
        if (isActive || hoverProgress > 0.01f) {
            float bgAlpha = isActive ? 1f : hoverProgress * 0.5f;
            int bgColor = isActive ? 
                new FixColor(100, 80, 180, (int)(100 * alpha * bgAlpha)).getRGB() :
                new FixColor(60, 60, 70, (int)(80 * alpha * bgAlpha)).getRGB();
            
            blur.render(farvix.solution.api.render.rect.ShapeProperties.create(
                    context.getMatrices(),
                    x, y, iconButtonSize, iconButtonSize)
                    .round(cornerRadius)
                    .softness(2f)
                    .thickness(0)
                    .outlineColor(0)
                    .color(bgColor)
                    .build());
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // ИКОНКА КАТЕГОРИИ
        // ═══════════════════════════════════════════════════════════════════
        String icon = category.moduleCategory.getIcon();
        
        // Цвет иконки
        java.awt.Color iconColor;
        if (isActive) {
            iconColor = new java.awt.Color(255, 255, 255);
        } else {
            java.awt.Color baseColor = new java.awt.Color(120, 120, 130);
            java.awt.Color hoverColor = new java.awt.Color(200, 200, 210);
            iconColor = lerp(baseColor, hoverColor, hoverProgress);
        }
        iconColor = withAlpha(iconColor, alpha);
        
        // Scale эффект при hover
        float scale = 1f + hoverProgress * 0.05f;
        
        if (scale != 1f) {
            context.getMatrices().push();
            float centerX = x + iconButtonSize / 2f;
            float centerY = y + iconButtonSize / 2f;
            context.getMatrices().translate(centerX, centerY, 0);
            context.getMatrices().scale(scale, scale, 1);
            context.getMatrices().translate(-centerX, -centerY, 0);
        }
        
        if (icon.equals("player")) {
            float cx = x + iconButtonSize / 2f;
            float cy = y + iconButtonSize / 2f;
            int actualColor = iconColor.getRGB();
            
            // Head
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                    context.getMatrices(), cx - 2f, cy - 4.5f, 4f, 4f)
                    .round(2f)
                    .color(actualColor)
                    .build());
                    
            // Shoulders / Torso
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                    context.getMatrices(), cx - 4f, cy + 0.5f, 8f, 4.5f)
                    .round(2.25f)
                    .color(actualColor)
                    .build());
        } else if (icon.equals("⌨")) {
            float cx = x + iconButtonSize / 2f;
            float cy = y + iconButtonSize / 2f;
            int actualColor = iconColor.getRGB();
            
            // Main Keyboard Frame Outline (12.5x8.6 centered, 3f thickness)
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(context.getMatrices(), cx - 6.25f, cy - 4.3f, 12.5f, 8.6f)
                    .round(2.5f)
                    .thickness(3f)
                    .outlineColor(actualColor)
                    .color(0)
                    .build());
                    
            // 5 Minimalist Keys (3 at the bottom, 2 at the top)
            // Top Row (2 keys)
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(context.getMatrices(), cx - 2.4f, cy - 2.3f, 2f, 1.8f)
                    .round(0.75f)
                    .color(actualColor)
                    .build());
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(context.getMatrices(), cx + 0.4f, cy - 2.3f, 2f, 1.8f)
                    .round(0.75f)
                    .color(actualColor)
                    .build());
                    
            // Bottom Row (3 keys)
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(context.getMatrices(), cx - 3.8f, cy + 0.5f, 2f, 1.8f)
                    .round(0.75f)
                    .color(actualColor)
                    .build());
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(context.getMatrices(), cx - 1.0f, cy + 0.5f, 2f, 1.8f)
                    .round(0.75f)
                    .color(actualColor)
                    .build());
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(context.getMatrices(), cx + 1.8f, cy + 0.5f, 2f, 1.8f)
                    .round(0.75f)
                    .color(actualColor)
                    .build());
        } else if (icon.equals("🌎")) {
            float cx = x + iconButtonSize / 2f;
            float cy = y + iconButtonSize / 2f;
            int actualColor = iconColor.getRGB();
            
            // Waypoint Map Pin
            // Pin stem
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                    context.getMatrices(), cx - 0.75f, cy - 0.5f, 1.5f, 5f)
                    .round(0.75f)
                    .color(actualColor)
                    .build());
            // Pin head
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                    context.getMatrices(), cx - 2.5f, cy - 4.5f, 5f, 5f)
                    .round(2.5f)
                    .color(actualColor)
                    .build());
        } else {
            // Получаем реальные размеры иконки для точного центрирования
            float iconWidth = Fonts.ICONS.get((int)iconSize).getStringWidth(icon);
            float iconHeight = Fonts.ICONS.get((int)iconSize).getStringHeight(icon);
            
            // Позиция иконки (центрируем по обеим осям + смещение вниз на 5px)
            float iconX = x + (iconButtonSize - iconWidth) / 2f;
            float iconY = y + (iconButtonSize - iconHeight) / 2f + 5f; // +5px вниз
            
            Fonts.ICONS.get((int)iconSize).drawString(
                    context.getMatrices(),
                    icon,
                    iconX,
                    iconY,
                    iconColor.getRGB()
            );
        }
        
        if (scale != 1f) {
            context.getMatrices().pop();
        }
    }
    
    /**
     * Проверяет, находится ли мышь над иконкой
     */
    private boolean isMouseOverIcon(double mouseX, double mouseY, float iconX, float iconY, float iconButtonSize) {
        return mouseX >= iconX && mouseX <= iconX + iconButtonSize &&
               mouseY >= iconY && mouseY <= iconY + iconButtonSize;
    }
    
    /**
     * Обработка клика мыши
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false; // Только ЛКМ
        
        float guiX = parent.getX();
        float guiY = parent.getY();
        
        // Fixed design resolution height
        float screenHeight = 540f;
        
        float iconButtonSize = screenHeight * ICON_BUTTON_SIZE_PERCENT;
        float buttonSpacing = screenHeight * BUTTON_SPACING_PERCENT;
        
        float barX = guiX + BAR_LEFT_MARGIN;
        float barStartY = guiY + 5f; // Небольшой отступ сверху (5px)
        
        for (int i = 0; i < CATEGORIES.length; i++) {
            float iconY = barStartY + i * (iconButtonSize + buttonSpacing);
            if (isMouseOverIcon(mouseX, mouseY, barX, iconY, iconButtonSize)) {
                CategoryInfo category = CATEGORIES[i];
                if (category.tab != null) {
                    // Переключаем вкладку
                    parent.switchTab(category.tab);
                    return true;
                } else {
                    // Обработка других категорий (Themes, Configs, Macro)
                    System.out.println("Clicked on category: " + category.moduleCategory.getDisplayName());
                    System.out.println("TODO: Implement " + category.moduleCategory.getDisplayName() + " screen");
                    // TODO: Открыть соответствующий экран
                    return true;
                }
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
    
    /**
     * Информация о категории
     */
    private static class CategoryInfo {
        final SolutionTab tab; // null если это не основная вкладка
        final ModuleCategory moduleCategory;
        
        CategoryInfo(SolutionTab tab, ModuleCategory moduleCategory) {
            this.tab = tab;
            this.moduleCategory = moduleCategory;
        }
    }
}
