package farvix.solution.api.ui.contextmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.TempColor;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.managers.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Контекстное меню с плавной анимацией появления/исчезновения
 * и эффектом "bounce" (колбасит)
 */
public class ContextMenu implements QuickImports {
    
    private final List<MenuItem> items = new ArrayList<>();
    private float x, y;
    private float width = 110f;  // уменьшена ширина
    private float itemHeight = 22f;
    private float padding = 6f;  // одинаковый отступ сверху и снизу
    
    // Анимации
    private final Animation scaleAnimation = new Animation(Easing.EASE_OUT_CUBIC, 400); // Ускорено в 2 раза (было 800ms)
    private final Animation alphaAnimation = new Animation(Easing.EASE_OUT_CUBIC, 400); // Ускорено в 2 раза (было 800ms)
    
    private boolean visible = false;
    private boolean closing = false;
    private long openTime = 0;
    private int hoveredIndex = -1;
    
    public ContextMenu() {
        scaleAnimation.setValue(0f);
        alphaAnimation.setValue(0f);
    }
    
    /**
     * Добавить пункт меню
     */
    public ContextMenu addItem(String label, Runnable action) {
        items.add(new MenuItem(label, null, action));
        return this;
    }
    
    /**
     * Добавить пункт меню для модуля (с toggle)
     */
    public ContextMenu addModuleItem(farvix.solution.client.modules.Module module) {
        items.add(new MenuItem(module.getName(), module, () -> module.setEnabled(!module.isEnabled())));
        return this;
    }
    
    /**
     * Открыть меню в указанной позиции
     */
    public void open(float x, float y) {
        this.x = x;
        this.y = y;
        this.visible = true;
        this.closing = false;
        this.openTime = System.currentTimeMillis();
        
        // ВАЖНО: Сбрасываем анимации при каждом открытии!
        scaleAnimation.setValue(0f);
        scaleAnimation.setStartValue(0f);
        scaleAnimation.setDestinationValue(1f);
        scaleAnimation.setStartTime(System.currentTimeMillis());
        scaleAnimation.setFinished(false);
        
        alphaAnimation.setValue(0f);
        alphaAnimation.setStartValue(0f);
        alphaAnimation.setDestinationValue(1f);
        alphaAnimation.setStartTime(System.currentTimeMillis());
        alphaAnimation.setFinished(false);
    }
    
    /**
     * Закрыть меню с анимацией
     */
    public void close() {
        if (!visible) return;
        closing = true;
        
        // Сбрасываем анимации для закрытия
        scaleAnimation.setStartValue(scaleAnimation.getValue());
        scaleAnimation.setDestinationValue(0f);
        scaleAnimation.setStartTime(System.currentTimeMillis());
        scaleAnimation.setFinished(false);
        
        alphaAnimation.setStartValue(alphaAnimation.getValue());
        alphaAnimation.setDestinationValue(0f);
        alphaAnimation.setStartTime(System.currentTimeMillis());
        alphaAnimation.setFinished(false);
    }
    
    /**
     * Проверка видимости
     */
    public boolean isVisible() {
        return visible && !closing;
    }
    
    /**
     * Проверка видимости или закрытия (для рендера)
     */
    public boolean isVisibleOrClosing() {
        return visible;
    }
    
    /**
     * Рендер меню
     */
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        if (!visible) return;
        
        // ВАЖНО: Обновляем анимации КАЖДЫЙ кадр!
        if (closing) {
            scaleAnimation.run(0f);
            alphaAnimation.run(0f);
            
            // Когда анимация закрытия завершена - скрываем меню
            if (scaleAnimation.isFinished() && alphaAnimation.isFinished()) {
                visible = false;
                closing = false;
                return;
            }
        } else {
            scaleAnimation.run(1f);
            alphaAnimation.run(1f);
        }
        
        float scale = scaleAnimation.getValue();
        float alpha = alphaAnimation.getValue();
        
        // При закрытии продолжаем рендерить даже если значения маленькие
        if (!closing && (scale <= 0.01f || alpha <= 0.01f)) return;
        
        MatrixStack ms = ctx.getMatrices();
        float height = items.size() * itemHeight + padding * 2;
        
        // Проверяем наведение на пункты
        hoveredIndex = -1;
        if (!closing) {
            for (int i = 0; i < items.size(); i++) {
                float itemY = y + padding + i * itemHeight;
                if (mouseX >= x && mouseX <= x + width && 
                    mouseY >= itemY && mouseY <= itemY + itemHeight) {
                    hoveredIndex = i;
                    break;
                }
            }
        }
        
        ms.push();
        
        // Центр масштабирования - центр меню
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        
        ms.translate(centerX, centerY, 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-centerX, -centerY, 0);
        
        // Небольшой z-offset для меню
        ms.translate(0, 0, 100);
        
        // Setup glass framebuffer
        glass.setup();
        
        // Получаем цвет темы
        Color themeCol = ThemeManager.getInstance().getCurrentTheme().getBackgroundColor();
        float brightness = (themeCol.getRed() + themeCol.getGreen() + themeCol.getBlue()) / (3f * 255f);
        boolean isLightTheme = brightness > 0.5f;

        int bgColor = TempColor.getGuiBackground().alpha(0.88f * alpha).getRGB();
        int borderColor = TempColor.getClientColor().alpha(0.78f * alpha).getRGB();
        
        // Рендерим фон с glass и закругленными углами
        glass.render(ShapeProperties.create(ms, x, y, width, height)
            .round(10)
            .softness(18.0f)
            .thickness(1.5f)
            .outlineColor(borderColor)
            .color(bgColor)
            .build());
        
        // Адаптивный цвет текста - вычисляем один раз перед циклом
        
        // Рендерим пункты меню
        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            float itemY = y + padding + i * itemHeight;
            boolean hovered = i == hoveredIndex;
            
            // Подсветка при наведении
            if (hovered) {
                int hoverColor = new FixColor(255, 255, 255, (int)(30 * alpha)).getRGB();
                drawRoundedRect(ms, x + 4, itemY + 2, width - 8, itemHeight - 4, 6, hoverColor);
            }
            
            int textColor = isLightTheme
                    ? new FixColor(10, 10, 10, (int)((hovered ? 255 : 220) * alpha)).getRGB()
                    : new FixColor(200, 200, 200, (int)((hovered ? 255 : 200) * alpha)).getRGB();
            
            float textY = itemY + (itemHeight - Fonts.SEMIBOLD.get(14).getStringHeight(item.label)) / 2f;
            
            // Если это модуль - показываем цветную точку статуса
            if (item.module != null) {
                float dotSize = 5f;
                float dotX = x + padding + 4;
                float dotY = itemY + (itemHeight - dotSize) / 2f;
                boolean enabled = item.module.isEnabled();
                int dotColor = enabled
                        ? TempColor.getClientColor().alpha(0.86f * alpha).getRGB()
                        : new FixColor(60, 60, 60, (int)(80 * alpha)).getRGB();
                drawRoundedRect(ms, dotX, dotY, dotSize, dotSize, dotSize / 2f, dotColor);
                Fonts.SEMIBOLD.get(14).drawString(ms, item.label,
                        dotX + dotSize + 4, textY, textColor);
            } else {
                Fonts.SEMIBOLD.get(14).drawString(ms, item.label,
                        x + padding + 4, textY, textColor);
            }
        }
        
        ms.pop();
    }
    
    /**
     * Обработка клика мыши
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible() || closing) return false;
        
        float height = items.size() * itemHeight + padding * 2;
        
        // Клик вне меню - закрываем
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            close();
            return false;
        }
        
        // Клик по пункту меню
        if (button == 0 && hoveredIndex >= 0 && hoveredIndex < items.size()) {
            MenuItem item = items.get(hoveredIndex);
            if (item.action != null) {
                item.action.run();
            }
            close();
            return true;
        }
        
        return true;
    }
    
    /**
     * Обработка нажатия клавиши
     */
    public boolean keyPressed(int keyCode) {
        if (!isVisible() || closing) return false;
        
        // ESC - закрываем меню
        if (keyCode == 256) { // GLFW.GLFW_KEY_ESCAPE
            close();
            return true;
        }
        
        return false;
    }
    
    /**
     * Рисует закругленный прямоугольник
     */
    private void drawRoundedRect(MatrixStack ms, float x, float y, float width, float height, float radius, int color) {
        float red = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >> 8) & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        float alpha = ((color >> 24) & 0xFF) / 255f;
        
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
        
        var tess = net.minecraft.client.render.Tessellator.getInstance();
        var buf = tess.begin(
            net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLE_FAN,
            net.minecraft.client.render.VertexFormats.POSITION_COLOR
        );
        
        // Центр
        buf.vertex(mat, x + width / 2f, y + height / 2f, 0).color(red, green, blue, alpha);
        
        // Обход по периметру с закругленными углами
        int segments = 8;
        
        // Верхний левый угол
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI + i * (Math.PI / 2) / segments);
            float cx = x + radius;
            float cy = y + radius;
            buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
                .color(red, green, blue, alpha);
        }
        
        // Верхний правый угол
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 1.5f + i * (Math.PI / 2) / segments);
            float cx = x + width - radius;
            float cy = y + radius;
            buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
                .color(red, green, blue, alpha);
        }
        
        // Нижний правый угол
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * (Math.PI / 2) / segments);
            float cx = x + width - radius;
            float cy = y + height - radius;
            buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
                .color(red, green, blue, alpha);
        }
        
        // Нижний левый угол
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI / 2 + i * (Math.PI / 2) / segments);
            float cx = x + radius;
            float cy = y + height - radius;
            buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
                .color(red, green, blue, alpha);
        }
        
        // Замыкаем на первую точку верхнего левого угла
        float angle = (float) Math.PI;
        float cx = x + radius;
        float cy = y + radius;
        buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
            .color(red, green, blue, alpha);
        
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }
    
    /**
     * Рисует обводку закругленного прямоугольника
     */
    private void drawRoundedRectOutline(MatrixStack ms, float x, float y, float width, float height, float radius, float thickness, int color) {
        float red = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >> 8) & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        float alpha = ((color >> 24) & 0xFF) / 255f;
        
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
        
        var tess = net.minecraft.client.render.Tessellator.getInstance();
        var buf = tess.begin(
            net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINE_STRIP,
            net.minecraft.client.render.VertexFormats.POSITION_COLOR
        );
        
        int segments = 8;
        
        // Верхний левый угол
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI + i * (Math.PI / 2) / segments);
            float cx = x + radius;
            float cy = y + radius;
            buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
                .color(red, green, blue, alpha);
        }
        
        // Верхний правый
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 1.5f + i * (Math.PI / 2) / segments);
            float cx = x + width - radius;
            float cy = y + radius;
            buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
                .color(red, green, blue, alpha);
        }
        
        // Нижний правый
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * (Math.PI / 2) / segments);
            float cx = x + width - radius;
            float cy = y + height - radius;
            buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
                .color(red, green, blue, alpha);
        }
        
        // Нижний левый
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI / 2 + i * (Math.PI / 2) / segments);
            float cx = x + radius;
            float cy = y + height - radius;
            buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
                .color(red, green, blue, alpha);
        }
        
        // Замыкаем на первую точку
        float angle = (float) Math.PI;
        float cx = x + radius;
        float cy = y + radius;
        buf.vertex(mat, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0)
            .color(red, green, blue, alpha);
        
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }
    
    /**
     * Пункт меню
     */
    private static class MenuItem {
        final String label;
        final farvix.solution.client.modules.Module module;
        final Runnable action;
        
        MenuItem(String label, farvix.solution.client.modules.Module module, Runnable action) {
            this.label = label;
            this.module = module;
            this.action = action;
        }
    }
}
