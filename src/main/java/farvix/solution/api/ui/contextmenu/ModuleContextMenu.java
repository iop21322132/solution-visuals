package farvix.solution.api.ui.contextmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.TempColor;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.impl.*;
import farvix.solution.api.translation.Translations;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.managers.ThemeManager;
import farvix.solution.client.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

/**
 * Контекстное меню для модулей с опциями "Настроить" и "Выключить"
 * При нажатии "Настроить" меню расширяется и показывает настройки модуля
 */
public class ModuleContextMenu implements QuickImports {

    private Module targetModule;
    private float x, y;
    private float width = 160f;
    private float itemHeight = 18f;
    private float padding = 4f;
    private float settingRowH = 26f;

    // Анимации открытия/закрытия
    private final Animation scaleAnimation = new Animation(Easing.EASE_OUT_CUBIC, 400);
    private final Animation alphaAnimation = new Animation(Easing.EASE_OUT_CUBIC, 400);

    // Анимация расширения настроек
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 300);
    private boolean settingsExpanded = false;

    // Слайдер: перетаскивание
    private SliderSetting draggingSlider = null;
    private float sliderStartX = 0;
    private float sliderStartVal = 0;

    private boolean visible = false;
    private boolean closing = false;
    private int hoveredIndex = -1;

    public ModuleContextMenu() {
        scaleAnimation.setValue(0f);
        alphaAnimation.setValue(0f);
        expandAnimation.setValue(0f);
    }

    public void open(Module module, float x, float y) {
        this.targetModule = module;
        this.x = x;
        this.y = y;
        this.visible = true;
        this.closing = false;
        this.settingsExpanded = false;
        this.draggingSlider = null;

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

        expandAnimation.setValue(0f);
        expandAnimation.setStartValue(0f);
        expandAnimation.setDestinationValue(0f);
        expandAnimation.setStartTime(System.currentTimeMillis());
        expandAnimation.setFinished(true);
    }

    public void close() {
        if (!visible) return;
        closing = true;
        settingsExpanded = false;
        draggingSlider = null;

        scaleAnimation.setStartValue(scaleAnimation.getValue());
        scaleAnimation.setDestinationValue(0f);
        scaleAnimation.setStartTime(System.currentTimeMillis());
        scaleAnimation.setFinished(false);

        alphaAnimation.setStartValue(alphaAnimation.getValue());
        alphaAnimation.setDestinationValue(0f);
        alphaAnimation.setStartTime(System.currentTimeMillis());
        alphaAnimation.setFinished(false);
    }

    public boolean isVisible() { return visible && !closing; }
    public boolean isVisibleOrClosing() { return visible; }

    // ── Рендер ────────────────────────────────────────────────────────────────

    public void render(DrawContext ctx, int mouseX, int mouseY) {
        if (!visible || targetModule == null) return;

        if (closing) {
            scaleAnimation.run(0f);
            alphaAnimation.run(0f);
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
        if (!closing && (scale <= 0.01f || alpha <= 0.01f)) return;

        // Анимация расширения
        expandAnimation.run(settingsExpanded ? 1f : 0f);
        float expandProgress = expandAnimation.getValue();

        float settingsH = getSettingsHeight() * expandProgress;
        boolean hasSettings = hasVisibleSettings();
        // Если нет настроек - только одна кнопка "Выключить"
        int buttonCount = hasSettings ? 2 : 1;
        float baseHeight = buttonCount * itemHeight + padding * 2;
        float totalHeight = baseHeight + settingsH;

        MatrixStack ms = ctx.getMatrices();

        // Hover для кнопок
        hoveredIndex = -1;
        if (!closing) {
            int rendIdx = 0;
            float topPad = (!hasSettings) ? padding + 4 : padding;
            for (int i = 0; i < 2; i++) {
                if (i == 0 && !hasSettings) continue;
                float itemY = y + topPad + rendIdx * itemHeight;
                if (mouseX >= x && mouseX <= x + width &&
                        mouseY >= itemY && mouseY <= itemY + itemHeight) {
                    hoveredIndex = i;
                    break;
                }
                rendIdx++;
            }
        }

        ms.push();
        float centerX = x + width / 2f;
        float centerY = y + totalHeight / 2f;
        ms.translate(centerX, centerY, 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-centerX, -centerY, 0);

        // Фон - тёмно-синий под тему
        Color themeCol = ThemeManager.getInstance().getCurrentTheme().getBackgroundColor();
        float brightness = (themeCol.getRed() + themeCol.getGreen() + themeCol.getBlue()) / (3f * 255f);
        boolean isLight = brightness > 0.5f;

        int bgColor = TempColor.getGuiBackground().alpha(0.93f * alpha).getRGB();
        int borderColor = TempColor.getClientColor().alpha(0.85f * alpha).getRGB();
        
        // Цвет текста: адаптивный
        int textColorNormal = isLight
                ? new FixColor(30, 30, 30, (int)(200 * alpha)).getRGB()
                : new FixColor(200, 200, 200, (int)(200 * alpha)).getRGB();
        int textColorHover = isLight
                ? new FixColor(0, 0, 0, (int)(255 * alpha)).getRGB()
                : new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB();

        blur.render(ShapeProperties.create(ms, x, y, width, totalHeight)
                .round(8).softness(2.0f).thickness(1.5f)
                .outlineColor(borderColor).color(bgColor).build());

        // Кнопки
        String[] menuItems = {
                "⚙ " + Translations.tr("context.menu.configure"),
                "⏻ " + Translations.tr("context.menu.disable")
        };
        
        int renderedIndex = 0;
        for (int i = 0; i < menuItems.length; i++) {
            // Скрываем "Настроить" если нет настроек
            if (i == 0 && !hasSettings) continue;
            
            // Позиция по renderedIndex чтобы "Выключить" была первой если нет настроек
            // Если нет настроек - добавляем дополнительный отступ сверху для красоты
            float topPad = (!hasSettings) ? padding + 4 : padding;
            float itemY = y + topPad + renderedIndex * itemHeight;
            boolean hovered = i == hoveredIndex;
            if (hovered) {
                drawRoundedRect(ms, x + 2, itemY + 1, width - 4, itemHeight - 2, 4,
                        new FixColor(255, 255, 255, (int)(30 * alpha)).getRGB());
            }
            // "Настроить" подсвечивается белым если расширено
            int textColor;
            if (i == 0 && settingsExpanded) {
                textColor = new FixColor(220, 220, 220, (int)(255 * alpha)).getRGB();
            } else {
                textColor = hovered ? textColorHover : textColorNormal;
            }
            Fonts.SEMIBOLD.get(14).drawString(ms, menuItems[i],
                    x + padding + 4,
                    itemY + (itemHeight - Fonts.SEMIBOLD.get(14).getStringHeight(menuItems[i])) / 2f,
                    textColor);
            renderedIndex++;
        }

        // Настройки (если расширено)
        if (expandProgress > 0.01f) {
            // Разделитель
            drawRoundedRect(ms, x + 6, y + baseHeight, width - 12, 1, 0.5f,
                    new FixColor(255, 255, 255, (int)(40 * alpha * expandProgress)).getRGB());

            float settingY = y + baseHeight + 2;
            List<Setting> settings = targetModule.getSettings();
            Setting prevSetting = null;
            for (Setting setting : settings) {
                if (setting.getHideCondition() != null && !setting.getHideCondition().get()) continue;
                if (setting instanceof DummySetting) continue;
                if (settingY + settingRowH > y + totalHeight - 2) break;

                // Разделитель между boolean-группой и slider/mode-группой
                if (prevSetting instanceof BooleanSetting
                        && !(setting instanceof BooleanSetting)) {
                    settingY += 4; // доп. отступ перед слайдером
                    drawRoundedRect(ms, x + 6, settingY - 3, width - 12, 0.5f, 0.5f,
                            new FixColor(255, 255, 255, (int)(25 * alpha * expandProgress)).getRGB());
                }

                renderSetting(ms, setting, settingY, alpha * expandProgress, mouseX, mouseY);

                // Boolean строки ближе друг к другу
                float gap = (setting instanceof BooleanSetting) ? 1f : 3f;
                settingY += settingRowH + gap;
                prevSetting = setting;
            }
        }

        ms.pop();
    }

    // ── Рендер одной настройки ────────────────────────────────────────────────

    private void renderSetting(MatrixStack ms, Setting setting, float sy, float alpha, int mouseX, int mouseY) {
        float sx = x + 6;
        float sw = width - 12;

        // Hover подсветка строки
        boolean rowHovered = mouseX >= x + 2 && mouseX <= x + width - 2
                && mouseY >= sy && mouseY <= sy + settingRowH;
        if (rowHovered) {
            drawRoundedRect(ms, x + 2, sy, width - 4, settingRowH, 4,
                    new FixColor(255, 255, 255, (int)(20 * alpha)).getRGB());
        }

        String name = getSettingLabel(setting);

        // Адаптивный цвет текста
        Color themeCol = ThemeManager.getInstance().getCurrentTheme().getBackgroundColor();
        float brightness = (themeCol.getRed() + themeCol.getGreen() + themeCol.getBlue()) / (3f * 255f);
        boolean isLightTheme = brightness > 0.5f;
        int labelColor = isLightTheme
                ? new FixColor(10, 10, 10, (int)(255 * alpha)).getRGB()
                : new FixColor(200, 200, 200, (int)(255 * alpha)).getRGB();
        int accentColor = isLightTheme
                ? new FixColor(0, 100, 200, (int)(255 * alpha)).getRGB()
                : new FixColor(180, 180, 180, (int)(255 * alpha)).getRGB();

        int fontSize = 11;

        if (setting instanceof BooleanSetting bool) {
            boolean enabled = bool.isEnabled();

            // Фон кнопки — выглядит как кнопка
            int btnBg = enabled
                    ? new FixColor(50, 50, 50, (int)(60 * alpha)).getRGB()
                    : new FixColor(30, 30, 30, (int)(80 * alpha)).getRGB();
            int btnBorder = enabled
                    ? new FixColor(100, 100, 100, (int)(150 * alpha)).getRGB()
                    : new FixColor(60, 60, 60, (int)(100 * alpha)).getRGB();
            blur.render(ShapeProperties.create(ms, x + 2, sy + 1, width - 4, settingRowH - 2)
                    .round(4f).softness(0.5f).thickness(1f)
                    .outlineColor(btnBorder).color(btnBg).build());

            // Toggle справа
            float toggleW = 22f, toggleH = 11f;
            float toggleX = sx + sw - toggleW;
            float toggleY = sy + (settingRowH - toggleH) / 2f;
            int trackColor = enabled
                    ? new FixColor(50, 200, 90, (int)(220 * alpha)).getRGB()
                    : new FixColor(50, 50, 50, (int)(200 * alpha)).getRGB();
            drawRoundedRect(ms, toggleX, toggleY, toggleW, toggleH, toggleH / 2f, trackColor);
            float knobSize = toggleH - 3;
            float knobX = enabled ? toggleX + toggleW - knobSize - 1.5f : toggleX + 1.5f;
            drawRoundedRect(ms, knobX, toggleY + 1.5f, knobSize, knobSize, knobSize / 2f,
                    new FixColor(255, 255, 255, (int)(230 * alpha)).getRGB());

            // Название
            int nameColor = enabled
                    ? new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB()
                    : labelColor;
            Fonts.SEMIBOLD.get(fontSize).drawString(ms, name, sx + 4,
                    sy + (settingRowH - Fonts.SEMIBOLD.get(fontSize).getStringHeight(name)) / 2f, nameColor);

        } else if (setting instanceof SliderSetting slider) {
            float barH = 3f;
            float barY = sy + settingRowH - barH - 3;
            float pct = Math.max(0, Math.min(1, (slider.getValue() - slider.getMin()) / (slider.getMax() - slider.getMin())));

            // Трек (серый фон)
            blur.render(ShapeProperties.create(ms, sx, barY, sw, barH)
                    .round(barH / 2f).softness(1f)
                    .color(new FixColor(80, 80, 80, (int)(150 * alpha)).getRGB())
                    .build());
            // Заполнение (белое)
            if (pct > 0.01f) {
                blur.render(ShapeProperties.create(ms, sx, barY, sw * pct, barH)
                        .round(barH / 2f).softness(1f)
                        .color(new FixColor(180, 180, 180, (int)(220 * alpha)).getRGB())
                        .build());
            }

            // Название над ползунком с достаточным отступом
            String valStr = slider.getStep() < 1f
                    ? String.format("%.2f", slider.getValue())
                    : String.valueOf((int) slider.getValue());
            float textY = sy + 2;
            Fonts.SEMIBOLD.get(fontSize).drawString(ms, name, sx, textY, labelColor);
            float valW = Fonts.SEMIBOLD.get(fontSize).getStringWidth(valStr);
            Fonts.SEMIBOLD.get(fontSize).drawString(ms, valStr, sx + sw - valW, textY, accentColor);

        } else if (setting instanceof ModeSetting mode) {
            String current = mode.getTranslatedCurrentMode();
            float arrowW = 14f;
            float totalModeW = arrowW + Fonts.SEMIBOLD.get(fontSize).getStringWidth(current) + arrowW + 4;
            float modeStartX = sx + sw - totalModeW;
            float textMidY = sy + (settingRowH - Fonts.SEMIBOLD.get(fontSize).getStringHeight(current)) / 2f;

            Fonts.SEMIBOLD.get(fontSize).drawString(ms, name, sx, textMidY, labelColor);

            int arrowColor = accentColor;
            int modeColor = isLightTheme
                    ? new FixColor(30, 30, 30, (int)(255 * alpha)).getRGB()
                    : new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB();
            Fonts.SEMIBOLD.get(fontSize).drawString(ms, "<", modeStartX, textMidY, arrowColor);
            Fonts.SEMIBOLD.get(fontSize).drawString(ms, current, modeStartX + arrowW, textMidY, modeColor);
            float cw = Fonts.SEMIBOLD.get(fontSize).getStringWidth(current);
            Fonts.SEMIBOLD.get(fontSize).drawString(ms, ">", modeStartX + arrowW + cw + 2, textMidY, arrowColor);

        } else {
            Fonts.SEMIBOLD.get(fontSize).drawString(ms, name, sx,
                    sy + (settingRowH - Fonts.SEMIBOLD.get(fontSize).getStringHeight(name)) / 2f, labelColor);
        }
    }

    private static String getSettingLabel(Setting setting) {
        String key = setting.getName();
        String translated = Translations.tr(key);
        if (!translated.equals(key)) {
            return translated;
        }
        if (key.contains(".")) {
            String shortKey = key.substring(key.lastIndexOf('.') + 1);
            translated = Translations.tr(shortKey);
            if (!translated.equals(shortKey)) {
                return translated;
            }
            if (!shortKey.isEmpty()) {
                return Character.toUpperCase(shortKey.charAt(0)) + shortKey.substring(1);
            }
        }
        return key;
    }

    // ── Проверка наличия настроек ─────────────────────────────────────────────

    private boolean hasVisibleSettings() {
        if (targetModule == null) return false;
        for (Setting s : targetModule.getSettings()) {
            if (s.getHideCondition() != null && !s.getHideCondition().get()) continue;
            if (s instanceof DummySetting) continue;
            return true;
        }
        return false;
    }

    private float getSettingsHeight() {
        if (targetModule == null) return 0;
        float total = 8f;
        Setting prev = null;
        for (Setting s : targetModule.getSettings()) {
            if (s.getHideCondition() != null && !s.getHideCondition().get()) continue;
            if (s instanceof DummySetting) continue;
            if (prev instanceof BooleanSetting && !(s instanceof BooleanSetting)) total += 4f;
            float gap = (s instanceof BooleanSetting) ? 1f : 3f;
            total += settingRowH + gap;
            prev = s;
        }
        return total;
    }

    // ── Обработка кликов ──────────────────────────────────────────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible() || closing || targetModule == null) return false;

        float expandProgress = expandAnimation.getValue();
        float settingsH = getSettingsHeight() * expandProgress;
        float baseHeight = 2 * itemHeight + padding * 2;
        float totalHeight = baseHeight + settingsH;

        // Клик вне меню
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + totalHeight) {
            close();
            return false;
        }

        if (button == 0) {
            // Клик по кнопкам
            if (mouseY <= y + baseHeight) {
                if (hoveredIndex == 0) {
                    // Настроить - переключаем расширение
                    settingsExpanded = !settingsExpanded;
                    expandAnimation.setStartValue(expandAnimation.getValue());
                    expandAnimation.setDestinationValue(settingsExpanded ? 1f : 0f);
                    expandAnimation.setStartTime(System.currentTimeMillis());
                    expandAnimation.setFinished(false);
                    return true;
                } else if (hoveredIndex == 1) {
                    // Выключить
                    targetModule.setEnabled(false);
                    close();
                    return true;
                }
            }

            // Клик по настройкам
            if (settingsExpanded && expandProgress > 0.5f) {
                float settingY = y + baseHeight + 4;
                for (Setting setting : targetModule.getSettings()) {
                    if (setting.getHideCondition() != null && !setting.getHideCondition().get()) continue;
                    if (setting instanceof DummySetting) continue;

                    float sx = x + 6;
                    float sw = width - 12;

                    if (mouseY >= settingY && mouseY <= settingY + settingRowH) {
                        if (setting instanceof BooleanSetting bool) {
                            bool.toggle();
                            return true;
                        } else if (setting instanceof SliderSetting slider) {
                            draggingSlider = slider;
                            sliderStartX = (float) mouseX;
                            sliderStartVal = slider.getValue();
                            // Установить значение по позиции клика
                            float pct = Math.max(0, Math.min(1, ((float) mouseX - sx) / sw));
                            float newVal = slider.getMin() + pct * (slider.getMax() - slider.getMin());
                            newVal = Math.round(newVal / slider.getStep()) * slider.getStep();
                            slider.setCurrentValue(newVal);
                            return true;
                        } else if (setting instanceof ModeSetting mode) {
                            // Клик по < или >
                            String current = mode.getCurrentMode();
                            float arrowW = 14f;
                            float totalModeW = arrowW + Fonts.SEMIBOLD.get(15).getStringWidth(current) + arrowW + 4;
                            float modeStartX = sx + sw - totalModeW;
                            if (mouseX < modeStartX + arrowW) {
                                // Предыдущий режим
                                List<String> modes = mode.getModes();
                                int idx = modes.indexOf(mode.getCurrentMode());
                                idx = (idx - 1 + modes.size()) % modes.size();
                                mode.setCurrentMode(modes.get(idx));
                            } else {
                                // Следующий режим
                                mode.nextMode();
                            }
                            return true;
                        }
                    }
                    settingY += settingRowH + 2;
                }
            }
        }

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSlider != null && button == 0) {
            float sx = x + 6;
            float sw = width - 12;
            float pct = Math.max(0, Math.min(1, ((float) mouseX - sx) / sw));
            float newVal = draggingSlider.getMin() + pct * (draggingSlider.getMax() - draggingSlider.getMin());
            newVal = Math.round(newVal / draggingSlider.getStep()) * draggingSlider.getStep();
            draggingSlider.setCurrentValue(newVal);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingSlider != null) {
            draggingSlider = null;
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (!isVisible() || closing) return false;
        if (keyCode == 256) { // ESC
            close();
            return true;
        }
        return false;
    }

    // ── Утилиты рисования ────────────────────────────────────────────────────

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
        var buf = tess.begin(net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLE_FAN,
                net.minecraft.client.render.VertexFormats.POSITION_COLOR);

        buf.vertex(mat, x + width / 2f, y + height / 2f, 0).color(red, green, blue, alpha);
        int segments = 8;

        for (int i = 0; i <= segments; i++) {
            float angle = (float)(Math.PI + i * (Math.PI / 2) / segments);
            buf.vertex(mat, x + radius + (float)Math.cos(angle) * radius, y + radius + (float)Math.sin(angle) * radius, 0).color(red, green, blue, alpha);
        }
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(Math.PI * 1.5f + i * (Math.PI / 2) / segments);
            buf.vertex(mat, x + width - radius + (float)Math.cos(angle) * radius, y + radius + (float)Math.sin(angle) * radius, 0).color(red, green, blue, alpha);
        }
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(i * (Math.PI / 2) / segments);
            buf.vertex(mat, x + width - radius + (float)Math.cos(angle) * radius, y + height - radius + (float)Math.sin(angle) * radius, 0).color(red, green, blue, alpha);
        }
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(Math.PI / 2 + i * (Math.PI / 2) / segments);
            buf.vertex(mat, x + radius + (float)Math.cos(angle) * radius, y + height - radius + (float)Math.sin(angle) * radius, 0).color(red, green, blue, alpha);
        }
        buf.vertex(mat, x + radius + (float)Math.cos(Math.PI) * radius, y + radius + (float)Math.sin(Math.PI) * radius, 0).color(red, green, blue, alpha);

        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }
}
