package farvix.solution.api.ui.solution.utils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.util.color.FixColor;

import java.awt.Color;

/**
 * Утилиты для рендеринга SolutionVisual GUI
 */
public class SolutionRenderUtils implements QuickImports {
    
    /**
     * Нарисовать скругленный прямоугольник с blur
     */
    public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height,
                                       float radius, Color color) {
        blur.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(radius)
                .softness(2f)
                .thickness(0)
                .outlineColor(0)
                .color(color.getRGB())
                .build());
    }
    
    /**
     * Нарисовать скругленный прямоугольник с эффектом frosted glass (преломление света)
     */
    public static void drawFrostedGlassRect(MatrixStack matrices, float x, float y, float width, float height,
                                            float radius, Color color, float alpha) {
        // Светлая обводка (преломление света)
        blur.render(ShapeProperties.create(matrices, x - 1, y - 1, width + 2, height + 2)
                .round(radius)
                .softness(6f)
                .thickness(1.5f)
                .outlineColor(new FixColor(255, 255, 255, (int)(20 * alpha)).getRGB())
                .color(0)
                .build());
        
        // Основной фон с сильным blur
        blur.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(radius)
                .softness(8f) // Сильное искажение
                .thickness(0)
                .outlineColor(0)
                .color(new FixColor(color.getRed(), color.getGreen(), color.getBlue(), 
                        (int)(color.getAlpha() * alpha)).getRGB())
                .build());
    }
    
    /**
     * Нарисовать скругленный прямоугольник с рамкой
     */
    public static void drawRoundedRectWithBorder(MatrixStack matrices, float x, float y, float width, float height,
                                                  float radius, Color fillColor, Color borderColor, float borderWidth) {
        // Фон
        blur.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(radius)
                .softness(2f)
                .thickness(0)
                .outlineColor(0)
                .color(fillColor.getRGB())
                .build());
        
        // Рамка
        blur.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(radius)
                .softness(1f)
                .thickness(borderWidth)
                .outlineColor(borderColor.getRGB())
                .color(0)
                .build());
    }
    
    /**
     * Нарисовать карточку модуля с frosted glass эффектом
     */
    public static void drawModuleCard(MatrixStack matrices, float x, float y, float width, float height,
                                      float radius, Color color, float alpha, boolean hovered) {
        // Эффект преломления света (тоньше чем у главного GUI)
        blur.render(ShapeProperties.create(matrices, x - 0.5f, y - 0.5f, width + 1, height + 1)
                .round(radius)
                .softness(4f)
                .thickness(1f)
                .outlineColor(new FixColor(255, 255, 255, (int)(12 * alpha)).getRGB())
                .color(0)
                .build());
        
        // Основной фон с blur
        float softnessValue = hovered ? 6f : 4f; // Больше blur при hover
        blur.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(radius)
                .softness(softnessValue)
                .thickness(0)
                .outlineColor(0)
                .color(new FixColor(color.getRed(), color.getGreen(), color.getBlue(),
                        (int)(color.getAlpha() * alpha)).getRGB())
                .build());
    }
    
    /**
     * Нарисовать тень под элементом
     */
    public static void drawShadow(MatrixStack matrices, float x, float y, float width, float height,
                                   float radius, Color shadowColor) {
        blur.render(ShapeProperties.create(matrices, x, y + 2, width, height)
                .round(radius)
                .softness(4f)
                .thickness(0)
                .outlineColor(0)
                .color(shadowColor.getRGB())
                .build());
    }
    
    /**
     * Нарисовать свечение вокруг элемента
     */
    public static void drawGlow(MatrixStack matrices, float x, float y, float width, float height,
                                 float radius, Color glowColor, float glowSize) {
        blur.render(ShapeProperties.create(matrices, x - glowSize, y - glowSize,
                        width + glowSize * 2, height + glowSize * 2)
                .round(radius + glowSize)
                .softness(6f)
                .thickness(0)
                .outlineColor(0)
                .color(glowColor.getRGB())
                .build());
    }
    
    /**
     * Нарисовать градиентный прямоугольник (вертикальный)
     */
    public static void drawVerticalGradient(MatrixStack matrices, float x, float y, float width, float height,
                                            float radius, Color topColor, Color bottomColor) {
        // Используем два прямоугольника с разными цветами
        // Верхняя половина
        blur.render(ShapeProperties.create(matrices, x, y, width, height / 2)
                .round(radius)
                .softness(2f)
                .thickness(0)
                .outlineColor(0)
                .color(topColor.getRGB())
                .build());
        
        // Нижняя половина
        blur.render(ShapeProperties.create(matrices, x, y + height / 2, width, height / 2)
                .round(radius)
                .softness(2f)
                .thickness(0)
                .outlineColor(0)
                .color(bottomColor.getRGB())
                .build());
    }
    
    /**
     * Нарисовать затемненный фон (для blur overlay)
     */
    public static void drawBlurredBackground(MatrixStack matrices, float width, float height, float alpha) {
        int backdropAlpha = (int)(150 * alpha);
        rectangle.render(ShapeProperties.create(matrices, 0f, 0f, width, height)
                .round(0)
                .softness(0)
                .thickness(0)
                .outlineColor(0)
                .color(new FixColor(0, 0, 0, backdropAlpha).getRGB())
                .build());
    }
    
    /**
     * Нарисовать линию (разделитель)
     */
    public static void drawLine(MatrixStack matrices, float x, float y, float width, float height, Color color) {
        rectangle.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(0)
                .softness(0)
                .thickness(0)
                .outlineColor(0)
                .color(color.getRGB())
                .build());
    }
    
    /**
     * Нарисовать скругленную линию (разделитель с blur)
     */
    public static void drawSoftLine(MatrixStack matrices, float x, float y, float width, float height,
                                     float radius, Color color) {
        blur.render(ShapeProperties.create(matrices, x, y, width, height)
                .round(radius)
                .softness(1.5f)
                .thickness(0)
                .outlineColor(0)
                .color(color.getRGB())
                .build());
    }
    
    /**
     * Проверить hover (находится ли мышь над элементом)
     */
    public static boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Clamp значение между min и max
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Lerp между двумя значениями
     */
    public static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }
}
