package farvix.solution.api.ui.solution.components;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.ui.solution.SolutionGuiScreen;
import farvix.solution.api.ui.solution.utils.SolutionColors;
import farvix.solution.api.util.color.FixColor;

/**
 * Поле поиска модулей
 * Этап 3.2 из плана
 */
public class SearchField implements QuickImports {
    
    private final SolutionGuiScreen parent;
    
    // ═══════════════════════════════════════════════════════════════════════
    // РАЗМЕРЫ (адаптивные, в процентах от размера экрана)
    // ═══════════════════════════════════════════════════════════════════════
    private static final float SEARCH_WIDTH_PERCENT = 0.15f;  // 15% ширины экрана
    private static final float SEARCH_HEIGHT_PERCENT = 0.035f; // 3.5% высоты экрана (больше для крупного текста)
    private static final float SEARCH_CORNER_RADIUS_PERCENT = 0.010f; // 1% высоты экрана
    private static final float PADDING_HORIZONTAL_PERCENT = 0.008f; // 0.8% ширины экрана
    private static final float ICON_SIZE_PERCENT = 0.036f; // 3.6% высоты экрана для иконки (в 2 раза больше)
    private static final float FONT_SIZE_PERCENT = 0.036f; // 3.6% высоты экрана (в 2 раза больше - 1.8% * 2)
    
    // ═══════════════════════════════════════════════════════════════════════
    // ПОЗИЦИЯ И РАЗМЕРЫ (вычисляются динамически)
    // ═══════════════════════════════════════════════════════════════════════
    private float x, y;
    private float width, height; // Динамические размеры
    
    // ═══════════════════════════════════════════════════════════════════════
    // СОСТОЯНИЕ
    // ═══════════════════════════════════════════════════════════════════════
    private String text = "";
    private boolean focused = false;
    private int cursorPosition = 0;
    private long lastCursorBlink = 0;
    
    // ═══════════════════════════════════════════════════════════════════════
    // АНИМАЦИИ
    // ═══════════════════════════════════════════════════════════════════════
    private Animation focusAnimation;
    private Animation hoverAnimation;
    
    // ═══════════════════════════════════════════════════════════════════════
    // КОНСТАНТЫ
    // ═══════════════════════════════════════════════════════════════════════
    private static final String PLACEHOLDER = "Поиск";
    private static final String SEARCH_ICON = "🔎"; // Иконка лупы
    private static final int CURSOR_BLINK_INTERVAL = 500; // ms
    
    public SearchField(SolutionGuiScreen parent, float x, float y) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        
        // Fixed dimensions based on 960x540 design space
        this.width = 960f * SEARCH_WIDTH_PERCENT;
        this.height = 540f * SEARCH_HEIGHT_PERCENT;
    }
    
    public void init() {
        // Инициализация анимаций
        this.focusAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);
        this.hoverAnimation = new Animation(Easing.EASE_OUT_SINE, 150);
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float alpha = parent.getAlpha();
        if (alpha <= 0.01f) return;
        
        // Fixed design resolution space values
        this.width = 960f * SEARCH_WIDTH_PERCENT;
        this.height = 540f * SEARCH_HEIGHT_PERCENT;
        
        // ═══════════════════════════════════════════════════════════════════
        // ОБНОВЛЕНИЕ АНИМАЦИЙ
        // ═══════════════════════════════════════════════════════════════════
        boolean isHovered = isMouseOver(mouseX, mouseY);
        focusAnimation.run(focused ? 1 : 0);
        hoverAnimation.run(isHovered ? 1 : 0);
        
        float focusProgress = focusAnimation.getValue();
        float hoverProgress = hoverAnimation.getValue();
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ ФОНА
        // ═══════════════════════════════════════════════════════════════════
        renderBackground(context, focusProgress, hoverProgress, alpha);
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ ИКОНКИ ЛУПЫ
        // ═══════════════════════════════════════════════════════════════════
        renderSearchIcon(context, alpha);
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ ТЕКСТА ИЛИ PLACEHOLDER
        // ═══════════════════════════════════════════════════════════════════
        if (text.isEmpty() && !focused) {
            renderPlaceholder(context, alpha);
        } else {
            renderText(context, alpha);
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ КУРСОРА (если в фокусе)
        // ═══════════════════════════════════════════════════════════════════
        if (focused) {
            renderCursor(context, alpha);
        }
    }
    
    /**
     * Рендерит фон поля поиска (простой темный прямоугольник без blur)
     */
    private void renderBackground(DrawContext context, float focusProgress, float hoverProgress, float alpha) {
        float cornerRadius = 540f * SEARCH_CORNER_RADIUS_PERCENT;
        
        // ═══════════════════════════════════════════════════════════════════
        // ПРОСТОЙ ТЕМНЫЙ ФОН (чуть светлее чем GUI: 10,10,12 -> 18,18,20)
        // БЕЗ BLUR ЭФФЕКТА
        // ═══════════════════════════════════════════════════════════════════
        rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                context.getMatrices(),
                x, y, width, height)
                .round(cornerRadius)
                .softness(0) // БЕЗ размытия
                .thickness(1f) // Чуть толще обводка
                .outlineColor(new java.awt.Color(200, 200, 210, (int)(60 * alpha)).getRGB()) // Ярче и заметнее белая обводка
                .color(new java.awt.Color(18, 18, 20, (int)(250 * alpha)).getRGB()) // Чуть светлее чем GUI (10,10,12)
                .build());
        
        // ═══════════════════════════════════════════════════════════════════
        // ДОПОЛНИТЕЛЬНАЯ РАМКА ПРИ ФОКУСЕ (фиолетовая)
        // ═══════════════════════════════════════════════════════════════════
        if (focusProgress > 0.01f) {
            java.awt.Color borderColor = new java.awt.Color(100, 80, 180, (int)(100 * alpha * focusProgress));
            
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                    context.getMatrices(),
                    x, y, width, height)
                    .round(cornerRadius)
                    .softness(0)
                    .thickness(1.5f) // Чуть толще при фокусе
                    .outlineColor(borderColor.getRGB())
                    .color(0)
                    .build());
        }
    }
    
    /**
    /**
     * Рендерит иконку лупы
     */
    private void renderSearchIcon(DrawContext context, float alpha) {
        int iconSize = (int)(540f * ICON_SIZE_PERCENT);
        float paddingH = 960f * PADDING_HORIZONTAL_PERCENT;
        
        float iconX = x + paddingH;
        float iconHeight = Fonts.DEFAULT.get(iconSize).getStringHeight(SEARCH_ICON);
        float iconY = y + (height - iconHeight) / 2f; // Центрируем по вертикали
        
        java.awt.Color iconColor = SolutionColors.withAlpha(new java.awt.Color(180, 180, 190), alpha);
        
        Fonts.DEFAULT.get(iconSize).drawString(
                context.getMatrices(),
                SEARCH_ICON,
                iconX,
                iconY,
                iconColor.getRGB()
        );
    }
    
    /**
     * Рендерит placeholder текст (с иконкой, центрирован + смещение вниз)
     */
    private void renderPlaceholder(DrawContext context, float alpha) {
        int fontSize = (int)(540f * FONT_SIZE_PERCENT);
        int iconSize = (int)(540f * ICON_SIZE_PERCENT);
        float paddingH = 960f * PADDING_HORIZONTAL_PERCENT;
        
        // Вычисляем ширину иконки
        float iconWidth = Fonts.DEFAULT.get(iconSize).getStringWidth(SEARCH_ICON);
        
        float textX = x + paddingH + iconWidth + 4f; // 4px отступ после иконки
        float textHeight = Fonts.DEFAULT.get(fontSize).getStringHeight(PLACEHOLDER);
        float textY = y + (height - textHeight) / 2f + 7f; // Центрируем по вертикали + 7px вниз
        
        java.awt.Color placeholderColor = SolutionColors.withAlpha(new java.awt.Color(160, 160, 170), alpha);
        
        Fonts.DEFAULT.get(fontSize).drawString(
                context.getMatrices(),
                PLACEHOLDER,
                textX,
                textY,
                placeholderColor.getRGB()
        );
    }
    
    /**
     * Рендерит введенный текст (с иконкой, центрирован + смещение вниз)
     */
    private void renderText(DrawContext context, float alpha) {
        if (text.isEmpty()) return;
        
        int fontSize = (int)(540f * FONT_SIZE_PERCENT);
        int iconSize = (int)(540f * ICON_SIZE_PERCENT);
        float paddingH = 960f * PADDING_HORIZONTAL_PERCENT;
        
        // Вычисляем ширину иконки
        float iconWidth = Fonts.DEFAULT.get(iconSize).getStringWidth(SEARCH_ICON);
        
        float textX = x + paddingH + iconWidth + 4f; // 4px отступ после иконки
        float textHeight = Fonts.DEFAULT.get(fontSize).getStringHeight(text);
        float textY = y + (height - textHeight) / 2f + 7f; // Центрируем по вертикали + 7px вниз
        
        java.awt.Color textColor = SolutionColors.withAlpha(SolutionColors.TEXT_PRIMARY, alpha);
        
        // Обрезаем текст если он слишком длинный
        String displayText = text;
        float maxWidth = width - paddingH * 2 - iconWidth - 8f; // Учитываем иконку и отступы
        float textWidth = Fonts.DEFAULT.get(fontSize).getStringWidth(displayText);
        
        if (textWidth > maxWidth) {
            // Обрезаем текст слева, показываем конец
            while (textWidth > maxWidth && displayText.length() > 0) {
                displayText = displayText.substring(1);
                textWidth = Fonts.DEFAULT.get(fontSize).getStringWidth(displayText);
            }
        }
        
        Fonts.DEFAULT.get(fontSize).drawString(
                context.getMatrices(),
                displayText,
                textX,
                textY,
                textColor.getRGB()
        );
    }
    
    /**
     * Рендерит мигающий курсор (центрирован по вертикали)
     */
    private void renderCursor(DrawContext context, float alpha) {
        // Мигание курсора
        long currentTime = System.currentTimeMillis();
        boolean showCursor = (currentTime - lastCursorBlink) % (CURSOR_BLINK_INTERVAL * 2) < CURSOR_BLINK_INTERVAL;
        
        if (!showCursor) return;
        
        int fontSize = (int)(540f * FONT_SIZE_PERCENT);
        int iconSize = (int)(540f * ICON_SIZE_PERCENT);
        float paddingH = 960f * PADDING_HORIZONTAL_PERCENT;
        
        // Вычисляем ширину иконки
        float iconWidth = Fonts.DEFAULT.get(iconSize).getStringWidth(SEARCH_ICON);
        
        // Позиция курсора
        float textX = x + paddingH + iconWidth + 4f; // 4px отступ после иконки
        float cursorX = textX;
        
        if (cursorPosition > 0 && cursorPosition <= text.length()) {
            String textBeforeCursor = text.substring(0, cursorPosition);
            cursorX += Fonts.DEFAULT.get(fontSize).getStringWidth(textBeforeCursor);
        }
        
        float cursorHeight = fontSize + 2; // Высота курсора чуть больше шрифта
        float cursorY = y + (height - cursorHeight) / 2f; // Центрируем по вертикали
        
        // Рендерим курсор
        int cursorAlpha = (int)(255 * alpha);
        rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                context.getMatrices(),
                cursorX, cursorY, 1f, cursorHeight)
                .round(0)
                .softness(0)
                .thickness(0)
                .outlineColor(0)
                .color(new FixColor(255, 255, 255, cursorAlpha).getRGB())
                .build());
    }
    
    /**
     * Проверяет, находится ли мышь над полем
     */
    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Обработка клика мыши
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false; // Только ЛКМ
        
        boolean wasOver = isMouseOver(mouseX, mouseY);
        
        if (wasOver) {
            // Устанавливаем фокус
            if (!focused) {
                focused = true;
                lastCursorBlink = System.currentTimeMillis();
            }
            
            // Устанавливаем позицию курсора
            int fontSize = (int)(540f * FONT_SIZE_PERCENT);
            int iconSize = (int)(540f * ICON_SIZE_PERCENT);
            float paddingH = 960f * PADDING_HORIZONTAL_PERCENT;
            
            // Вычисляем ширину иконки
            float iconWidth = Fonts.DEFAULT.get(iconSize).getStringWidth(SEARCH_ICON);
            
            float textX = x + paddingH + iconWidth + 4f; // 4px отступ после иконки
            float clickX = (float)mouseX - textX;
            
            cursorPosition = 0;
            float currentWidth = 0;
            
            for (int i = 0; i < text.length(); i++) {
                float charWidth = Fonts.DEFAULT.get(fontSize).getStringWidth(String.valueOf(text.charAt(i)));
                if (currentWidth + charWidth / 2f > clickX) {
                    break;
                }
                currentWidth += charWidth;
                cursorPosition++;
            }
            
            return true;
        } else {
            // Снимаем фокус
            if (focused) {
                focused = false;
            }
        }
        
        return false;
    }
    
    /**
     * Обработка нажатия клавиш
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE:
                // Удаление символа
                if (cursorPosition > 0 && !text.isEmpty()) {
                    text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                    cursorPosition--;
                    updateSearch();
                }
                return true;
                
            case GLFW.GLFW_KEY_DELETE:
                // Удаление символа справа
                if (cursorPosition < text.length()) {
                    text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
                    updateSearch();
                }
                return true;
                
            case GLFW.GLFW_KEY_LEFT:
                // Движение курсора влево
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                return true;
                
            case GLFW.GLFW_KEY_RIGHT:
                // Движение курсора вправо
                if (cursorPosition < text.length()) {
                    cursorPosition++;
                }
                return true;
                
            case GLFW.GLFW_KEY_HOME:
                // В начало
                cursorPosition = 0;
                return true;
                
            case GLFW.GLFW_KEY_END:
                // В конец
                cursorPosition = text.length();
                return true;
                
            case GLFW.GLFW_KEY_ESCAPE:
                // Снять фокус
                focused = false;
                return true;
                
            case GLFW.GLFW_KEY_ENTER:
                // Снять фокус при Enter
                focused = false;
                return true;
        }
        
        return false;
    }
    
    /**
     * Обработка ввода символов
     */
    public boolean charTyped(char codePoint, int modifiers) {
        if (!focused) return false;
        
        // Проверяем, что символ печатный
        if (codePoint < 32 || codePoint == 127) {
            return false;
        }
        
        // Добавляем символ в текст
        text = text.substring(0, cursorPosition) + codePoint + text.substring(cursorPosition);
        cursorPosition++;
        
        // Обновляем поиск
        updateSearch();
        
        return true;
    }
    
    /**
     * Обновляет поиск в родительском GUI
     */
    private void updateSearch() {
        parent.updateSearchQuery(text);
    }
    
    /**
     * Установить позицию поля поиска
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Получить текущий текст
     */
    public String getText() {
        return text;
    }
    
    /**
     * Установить текст
     */
    public void setText(String text) {
        this.text = text;
        this.cursorPosition = text.length();
        updateSearch();
    }
    
    /**
     * Очистить поле
     */
    public void clear() {
        this.text = "";
        this.cursorPosition = 0;
        updateSearch();
    }
}
