package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.renderer.fonts.Font;
import dev.simplevisuals.client.util.perf.Perf;
import dev.simplevisuals.client.util.renderer.fonts.Instance;

import java.awt.Color;

public class Watermark extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color accentColor;

    // Кэшированные значения для предотвращения пересчётов каждый кадр
    private float totalWidth, totalHeight;
    private final String cachedTitle = "SimpleVisuals";
    private final String cachedLink = "t.me/SimpleVisuals";
    private final float cachedLogoSize = 17.5f;
    private final float cachedPaddingX = 8f;
    private final float cachedPaddingY = 5f;
    private final float cachedFontSizeTitle = 9f;
    private final float cachedFontSizeLink = 7f;
    private final float cachedTextGap = 2f;
    private final float cachedCornerRadius = 5f;

    // Кэшированные инстансы шрифтов и шрифты
    private Instance cachedFontTitleInstance;
    private Instance cachedFontLinkInstance;
    private Instance cachedFontIconInstance;
    private Font cachedFontTitle;
    private Font cachedFontLink;

    // Флаги для отслеживания изменений
    private boolean fontsNeedUpdate = true;
    private boolean initialized = false;
    private Color lastAccentColor = null;

    public Watermark() {
        super("Watermark");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);

        // НЕ вызываем calculateDimensions() здесь, потому что шрифты ещё не загружены
        // Откладываем инициализацию до первого рендера
    }

    private void applyTheme(ThemeManager.Theme theme) {
        // Кэшируем цвета
        this.bgColor = new Color(30, 30, 30, 240);

        int brightness = (int) (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue());
        if (brightness > 200 && bgColor.getAlpha() <= 150) {
            this.textColor = new Color(0, 0, 0, 255);
        } else {
            this.textColor = theme.getTextColor();
        }

        this.accentColor = theme.getAccentColor();
        fontsNeedUpdate = true; // Тема изменилась, шрифты нужно обновить
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    private void lazyInitialize() {
        if (!initialized) {
            try {
                updateFonts();
                calculateDimensions();
                initialized = true;
            } catch (Exception e) {
                // Если шрифты ещё не загружены, пробуем снова в следующем кадре
                System.err.println("[Watermark] Failed to initialize fonts, retrying next frame: " + e.getMessage());
            }
        }
    }

    private void updateFonts() {
        if (fontsNeedUpdate && Fonts.BOLD != null) {
            try {
                // Получаем инстансы шрифтов напрямую из Fonts
                cachedFontTitleInstance = Fonts.BOLD.getFont(cachedFontSizeTitle);
                cachedFontLinkInstance = Fonts.REGULAR.getFont(cachedFontSizeLink);
                cachedFontIconInstance = Fonts.ICONS.getFont(cachedLogoSize);

                // Получаем объекты Font для расчёта размеров
                cachedFontTitle = Fonts.BOLD;
                cachedFontLink = Fonts.REGULAR;

                fontsNeedUpdate = false;
            } catch (Exception e) {
                System.err.println("[Watermark] Failed to update fonts: " + e.getMessage());
            }
        }
    }

    private void calculateDimensions() {
        // Проверяем, что шрифты загружены
        if (cachedFontTitle == null || cachedFontLink == null) {
            return;
        }

        // Используем кэшированные шрифты для расчёта размеров
        float titleWidth = cachedFontTitle.getWidth(cachedTitle, cachedFontSizeTitle);
        float linkWidth = cachedFontLink.getWidth(cachedLink, cachedFontSizeLink);
        float textWidth = Math.max(titleWidth, linkWidth);

        totalWidth = cachedPaddingX * 2 + cachedLogoSize + 6 + textWidth;
        totalHeight = cachedPaddingY * 2 + cachedFontSizeTitle + cachedFontSizeLink + cachedTextGap;

        // Обновляем границы один раз при создании или изменении размера
        setBounds(getX(), getY(), totalWidth, totalHeight);
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        // Ленивая инициализация
        lazyInitialize();

        // Если не инициализированы, пропускаем рендер
        if (!initialized || cachedFontTitleInstance == null || cachedFontLinkInstance == null || cachedFontIconInstance == null) {
            return;
        }

        // Быстрая проверка - если элемент невидим, не тратим ресурсы
        if (getX() < -totalWidth || getY() < -totalHeight) return;

        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("Watermark.onRender2D")) {

            var matrices = e.getContext().getMatrices();

            // Обновляем шрифты только если нужно
            updateFonts();

            // Получаем текущий акцентный цвет
            Color currentAccent = themeManager.getCurrentTheme().getAccentColor();

            // Рисуем фон
            Render2D.drawRoundedRect(
                    matrices,
                    getX(), getY(),
                    totalWidth, totalHeight,
                    cachedCornerRadius,
                    bgColor
            );

            // Кэшируем координаты для повторного использования
            float logoX = getX() + cachedPaddingX;
            float logoY = getY() + (totalHeight - cachedLogoSize) / 2f;

            // Рисуем glow
            Render2D.drawGlowOutline(
                    matrices,
                    logoX + 0.6f,
                    logoY,
                    cachedLogoSize,
                    cachedLogoSize,
                    cachedLogoSize / 0.5f,
                    currentAccent,
                    150,
                    15
            );

            // Рисуем иконку
            Render2D.drawFont(
                    matrices,
                    cachedFontIconInstance,
                    "R",
                    logoX,
                    logoY,
                    currentAccent
            );

            float textX = getX() + cachedPaddingX + cachedLogoSize + 6;
            float textY = getY() + cachedPaddingY;

            // Рисуем заголовок
            Render2D.drawFont(
                    matrices,
                    cachedFontTitleInstance,
                    cachedTitle,
                    textX,
                    textY,
                    Color.WHITE
            );

            // Рисуем ссылку
            Render2D.drawFont(
                    matrices,
                    cachedFontLinkInstance,
                    cachedLink,
                    textX,
                    textY + cachedFontSizeTitle + cachedTextGap,
                    new Color(200, 200, 200)
            );

            super.onRender2D(e);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Очищаем ресурсы при отключении
        themeManager.removeThemeChangeListener(this);
        fontsNeedUpdate = true;
        initialized = false;
        lastAccentColor = null;
    }

    // Метод для принудительной переинициализации
    public void reinitialize() {
        initialized = false;
        fontsNeedUpdate = true;
    }
}