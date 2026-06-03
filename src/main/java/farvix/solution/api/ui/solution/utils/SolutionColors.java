package farvix.solution.api.ui.solution.utils;

import java.awt.Color;

/**
 * Цветовая палитра для SolutionVisual GUI
 * Основана на референсе
 */
public class SolutionColors {
    
    // ═══════════════════════════════════════════════════════════════════════
    // ФОНЫ (с увеличенной прозрачностью для frosted glass эффекта)
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Главный фон GUI - очень темный с прозрачностью */
    public static final Color BG_MAIN = new Color(18, 18, 20, 180); // Было 240, стало 180
    
    /** Фон карточек модулей - темно-серый с прозрачностью */
    public static final Color BG_CARD = new Color(40, 40, 45, 160); // Было 200, стало 160
    
    /** Фон при hover - чуть светлее */
    public static final Color BG_HOVER = new Color(50, 50, 55, 170); // Было 200, стало 170
    
    /** Фон поля поиска */
    public static final Color BG_SEARCH = new Color(30, 30, 35, 150); // Было 200, стало 150
    
    // ═══════════════════════════════════════════════════════════════════════
    // ТЕКСТ
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Основной текст - белый */
    public static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    
    /** Вторичный текст - серый */
    public static final Color TEXT_SECONDARY = new Color(180, 180, 180);
    
    /** Отключенный текст - темно-серый */
    public static final Color TEXT_DISABLED = new Color(120, 120, 125);
    
    /** Placeholder текст */
    public static final Color TEXT_PLACEHOLDER = new Color(100, 100, 105);
    
    // ═══════════════════════════════════════════════════════════════════════
    // АКЦЕНТЫ
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Основной акцент - фиолетовый */
    public static final Color ACCENT_PRIMARY = new Color(200, 180, 255);
    
    /** Вторичный акцент - белый */
    public static final Color ACCENT_SECONDARY = new Color(255, 255, 255);
    
    /** Акцент для активных элементов */
    public static final Color ACCENT_ACTIVE = new Color(220, 200, 255);
    
    // ═══════════════════════════════════════════════════════════════════════
    // TOGGLE ПЕРЕКЛЮЧАТЕЛИ
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Фон включенного toggle - фиолетовый */
    public static final Color TOGGLE_ON_BG = new Color(200, 180, 255);
    
    /** Ручка включенного toggle - белая */
    public static final Color TOGGLE_ON_KNOB = new Color(255, 255, 255);
    
    /** Фон выключенного toggle - темный */
    public static final Color TOGGLE_OFF_BG = new Color(60, 60, 65);
    
    /** Ручка выключенного toggle - серая */
    public static final Color TOGGLE_OFF_KNOB = new Color(120, 120, 125);
    
    // ═══════════════════════════════════════════════════════════════════════
    // ЭФФЕКТЫ
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Тени под элементами */
    public static final Color SHADOW = new Color(0, 0, 0, 80);
    
    /** Свечение вокруг активных элементов */
    public static final Color GLOW = new Color(180, 140, 255, 100);
    
    /** Затемнение фона за GUI */
    public static final Color BLUR_OVERLAY = new Color(0, 0, 0, 150);
    
    /** Рамки */
    public static final Color BORDER = new Color(60, 60, 60);
    
    /** Рамки при фокусе */
    public static final Color BORDER_FOCUS = new Color(200, 180, 255);
    
    // ═══════════════════════════════════════════════════════════════════════
    // УТИЛИТЫ
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Создать цвет с альфа каналом
     */
    public static Color withAlpha(Color color, float alpha) {
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int)(255 * alpha)
        );
    }
    
    /**
     * Осветлить цвет на процент
     */
    public static Color brighten(Color color, float percent) {
        int r = Math.min(255, (int)(color.getRed() * (1 + percent)));
        int g = Math.min(255, (int)(color.getGreen() * (1 + percent)));
        int b = Math.min(255, (int)(color.getBlue() * (1 + percent)));
        return new Color(r, g, b, color.getAlpha());
    }
    
    /**
     * Затемнить цвет на процент
     */
    public static Color darken(Color color, float percent) {
        int r = Math.max(0, (int)(color.getRed() * (1 - percent)));
        int g = Math.max(0, (int)(color.getGreen() * (1 - percent)));
        int b = Math.max(0, (int)(color.getBlue() * (1 - percent)));
        return new Color(r, g, b, color.getAlpha());
    }
    
    /**
     * Интерполировать между двумя цветами
     */
    public static Color lerp(Color from, Color to, float progress) {
        int r = (int)(from.getRed() + (to.getRed() - from.getRed()) * progress);
        int g = (int)(from.getGreen() + (to.getGreen() - from.getGreen()) * progress);
        int b = (int)(from.getBlue() + (to.getBlue() - from.getBlue()) * progress);
        int a = (int)(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * progress);
        return new Color(r, g, b, a);
    }
}
