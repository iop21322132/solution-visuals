package dev.simplevisuals.client.ui.hud.windows.components.impl;

import java.awt.Color;

import dev.simplevisuals.client.ui.hud.windows.components.WindowComponent;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import dev.simplevisuals.simplevisuals;

public class BooleanComponent extends WindowComponent {

    private final BooleanSetting setting;
    private final Animation toggleAnimation = new Animation(300, 1, false, Easing.simplevisuals);

    // Константы для координат переключателя (используются и в рендере и в клике)
    private static final float TOGGLE_X_OFFSET = -20f; // Смещение от правого края компонента
    private static final float TOGGLE_Y = 4.5f; // Y позиция от верхнего края компонента
    private static final float TOGGLE_WIDTH = 16f;
    private static final float TOGGLE_HEIGHT = 8f;
    private static final float KNOB_SIZE = 7f;
    private static final float KNOB_PADDING = 0.5f;

    public BooleanComponent(String name, BooleanSetting setting) {
        super(name);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        toggleAnimation.update(setting.getValue());

        // Рассчитываем позицию переключателя
        float toggleX = width + TOGGLE_X_OFFSET; // относительно компонента
        float toggleY = TOGGLE_Y;
        float absoluteToggleX = x + toggleX; // абсолютная позиция на экране
        float absoluteToggleY = y + toggleY;

        // Текст
        Color textColor = ThemeManager.getInstance().getCurrentTheme().getTextColor();
        Render2D.drawFont(
                context.getMatrices(),
                Fonts.BOLD.getFont(8f),
                I18n.translate(getName()),
                x + 5f,
                y + 4f,
                new Color(
                        textColor.getRed(),
                        textColor.getGreen(),
                        textColor.getBlue(),
                        (int) (255 * animation.getValue())
                )
        );

        // Трек переключателя (фон)
        Render2D.drawRoundedRect(
                context.getMatrices(),
                absoluteToggleX,
                absoluteToggleY,
                TOGGLE_WIDTH,
                TOGGLE_HEIGHT,
                2.5f,
                new Color(23, 23, 23, 100)
        );

        // Заливка трека (акцентным цветом)
        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        float fillWidth = TOGGLE_WIDTH * toggleAnimation.getValue();
        Render2D.drawRoundedRect(
                context.getMatrices(),
                absoluteToggleX,
                absoluteToggleY,
                fillWidth,
                TOGGLE_HEIGHT,
                2.5f,
                new Color(
                        accent.getRed(),
                        accent.getGreen(),
                        accent.getBlue(),
                        (int) (255 * toggleAnimation.getLinear())
                )
        );

        // Ползунок (кнопка)
        float knobX = absoluteToggleX + KNOB_PADDING + (TOGGLE_WIDTH - KNOB_SIZE - KNOB_PADDING * 2) * toggleAnimation.getValue();
        float knobY = absoluteToggleY + (TOGGLE_HEIGHT - KNOB_SIZE) / 2f;
        Render2D.drawRoundedRect(
                context.getMatrices(),
                knobX,
                knobY,
                KNOB_SIZE,
                KNOB_SIZE,
                2.5f,
                Color.WHITE
        );

        // Визуальная отладка области клика (раскомментируйте для отладки)
        /*
        float relativeMouseX = mouseX - x;
        float relativeMouseY = mouseY - y;
        boolean isHovered = MathUtils.isHovered(toggleX, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT, relativeMouseX, relativeMouseY);

        if (isHovered) {
            Render2D.drawRectOutline(
                context.getMatrices(),
                absoluteToggleX,
                absoluteToggleY,
                TOGGLE_WIDTH,
                TOGGLE_HEIGHT,
                1f,
                Color.GREEN
            );
        } else {
            Render2D.drawRectOutline(
                context.getMatrices(),
                absoluteToggleX,
                absoluteToggleY,
                TOGGLE_WIDTH,
                TOGGLE_HEIGHT,
                1f,
                Color.RED
            );
        }

        // Отладочный текст
        String debugText = String.format("Toggle: (%.1f, %.1f)", absoluteToggleX, absoluteToggleY);
        Render2D.drawFont(
            context.getMatrices(),
            Fonts.REGULAR.getFont(6f),
            debugText,
            x + 5f,
            y + 15f,
            Color.YELLOW
        );

        String mouseText = String.format("Mouse: (%.1f, %.1f)", relativeMouseX, relativeMouseY);
        Render2D.drawFont(
            context.getMatrices(),
            Fonts.REGULAR.getFont(6f),
            mouseText,
            x + 5f,
            y + 22f,
            Color.CYAN
        );
        */
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        // Преобразуем абсолютные координаты мыши в координаты относительно компонента
        float relativeMouseX = (float) mouseX - x;
        float relativeMouseY = (float) mouseY - y;

        // Используем те же координаты, что и в render()
        float toggleX = width + TOGGLE_X_OFFSET;
        float toggleY = TOGGLE_Y;

        System.out.println("BooleanComponent.click - " + getName());
        System.out.println("  Absolute mouse: (" + mouseX + ", " + mouseY + ")");
        System.out.println("  Component pos: (" + x + ", " + y + ")");
        System.out.println("  Relative mouse: (" + relativeMouseX + ", " + relativeMouseY + ")");
        System.out.println("  Toggle area: (" + toggleX + ", " + toggleY + ") size: " +
                          TOGGLE_WIDTH + "x" + TOGGLE_HEIGHT);

        // Проверяем, находится ли мышь в области переключателя
        boolean isHovered = MathUtils.isHovered(
                toggleX,
                toggleY,
                TOGGLE_WIDTH,
                TOGGLE_HEIGHT,
                relativeMouseX,
                relativeMouseY
        );
        System.out.println("  Is hovered: " + isHovered);
        System.out.println("  Button: " + button);

        if (isHovered && button == 0) {
            // Инвертируем значение
            boolean newValue = !setting.getValue();
            setting.setValue(newValue);
            System.out.println("  Toggled: " + !newValue + " -> " + newValue);

            // Сохраняем изменения
            try {
                simplevisuals.getInstance().getAutoSaveManager().forceSave();
            } catch (Throwable ignored) {
                // Игнорируем ошибки автосохранения
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        // Не требуется
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        // Не требуется
    }

    @Override
    public void keyReleased(int keyCode, int scanCode, int modifiers) {
        // Не требуется
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        // Не требуется
    }

    // Геттер для значения настройки (опционально)
    public boolean getValue() {
        return setting.getValue();
    }

    // Сеттер для значения настройки (опционально)
    public void setValue(boolean value) {
        setting.setValue(value);
    }
}