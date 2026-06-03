package dev.padiloi1337.hitcolor.ui;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;

import dev.padiloi1337.hitcolor.Wrapper;
import dev.padiloi1337.hitcolor.animations.LinearAnimation;
import dev.padiloi1337.hitcolor.helpers.font.FontRenderer;
import dev.padiloi1337.hitcolor.helpers.render.DrawHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

/**
 * Белое закруглённое меню с анимацией появления сверху вниз.
 * Открывается/закрывается клавишей Insert.
 */
public class RoundedMenuScreen extends Screen implements Wrapper {

    // Размеры меню
    private static final double MENU_WIDTH  = 320;
    private static final double MENU_HEIGHT = 220;
    private static final double RADIUS      = 16;

    // Цвета
    private static final Color BG_COLOR     = new Color(255, 255, 255, 255);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 80);
    private static final Color TEXT_COLOR   = new Color(30, 30, 30, 255);
    private static final Color SUB_COLOR    = new Color(120, 120, 120, 255);
    private static final Color ACCENT_COLOR = new Color(60, 60, 60, 255);

    // Анимация: 0.0 = закрыто, 1.0 = открыто
    private final LinearAnimation openAnim = new LinearAnimation(0.0);

    // Флаг: меню закрывается (анимация обратно)
    private boolean closing = false;

    // Масштаб (берём из GuiScreen логики)
    private double scale = 1.0;
    private double scaledW, scaledH;

    public RoundedMenuScreen() {
        super(new StringTextComponent(""));
    }

    @Override
    public void init() {
        // Вычисляем масштаб так же, как в GuiScreen
        switch ((int) WINDOW.getGuiScaleFactor()) {
            case 1: scale = 2 * 0.8;       break;
            case 2: scale = 1.0;            break;
            case 3: scale = 2 / 3d * 1.2;  break;
            case 4: scale = 0.5 * 1.3;     break;
            default: scale = 1.0;           break;
        }
        scaledW = WINDOW.getScaledWidth()  / scale;
        scaledH = WINDOW.getScaledHeight() / scale;

        closing = false;
        // Анимация открытия: прогресс от 0 до 1 за ~1 секунду
        // speed=1.5 при 60fps даёт ~1 сек (30/speed/fps * maxTick)
        openAnim.begin(0.0, 1.0, 1.5);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        double progress = openAnim.getAndUpdate();

        // Если анимация закрытия завершилась — закрываем экран
        if (closing && !openAnim.isAnimating()) {
            MC.displayGuiScreen(null);
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scaled(scale, scale, 1.0);

        // Центр экрана
        double cx = scaledW / 2.0;
        double cy = scaledH / 2.0;

        double menuX = cx - MENU_WIDTH  / 2.0;
        double menuY = cy - MENU_HEIGHT / 2.0;

        // Анимация: меню едет сверху вниз
        // При progress=0 меню полностью выше экрана (y = -MENU_HEIGHT)
        // При progress=1 меню на своём месте (y = menuY)
        // Используем ease-out: smoothstep
        double eased = smoothstep(progress);
        double startY = -MENU_HEIGHT;
        double animatedY = startY + (menuY - startY) * eased;

        // Прозрачность: появляется вместе с движением
        int alpha = (int) (255 * Math.min(1.0, eased * 1.5));
        alpha = Math.max(0, Math.min(255, alpha));

        Color bgColor     = withAlpha(BG_COLOR,     alpha);
        Color shadowColor = withAlpha(SHADOW_COLOR, (int)(alpha * 0.6));
        Color textColor   = withAlpha(TEXT_COLOR,   alpha);
        Color subColor    = withAlpha(SUB_COLOR,     alpha);
        Color accentColor = withAlpha(ACCENT_COLOR,  alpha);

        // Тень под меню
        DrawHelper.drawGlow(
            (int) menuX, (int)(animatedY + MENU_HEIGHT),
            (int) MENU_WIDTH, (int) MENU_HEIGHT,
            18, shadowColor
        );

        // Основной белый прямоугольник с закруглёнными углами
        DrawHelper.drawRoundedRect(
            menuX, animatedY + MENU_HEIGHT,
            MENU_WIDTH, MENU_HEIGHT,
            RADIUS, bgColor
        );

        // Разделитель под заголовком
        double divY = animatedY + 48;
        DrawHelper.drawRect(
            menuX + 20, divY + 1,
            MENU_WIDTH - 40, 1,
            withAlpha(new Color(200, 200, 200, 255), alpha)
        );

        // Заголовок
        FontRenderer.drawCenteredXString(
            matrices, DEFAULT_24,
            "Solution Visual",
            cx, animatedY + 28,
            textColor
        );

        // Подзаголовок
        FontRenderer.drawCenteredXString(
            matrices, DEFAULT_20,
            "Custom Hit Color",
            cx, animatedY + 62,
            subColor
        );

        // Три строки-плейсхолдера для будущих настроек
        String[] items = { "Hit Color", "Custom Hitbox", "Animations" };
        double itemY = animatedY + 90;
        for (String item : items) {
            // Фон строки
            DrawHelper.drawRoundedRect(
                menuX + 16, itemY + 16,
                MENU_WIDTH - 32, 16,
                6, withAlpha(new Color(240, 240, 240, 255), alpha)
            );
            FontRenderer.drawCenteredYString(
                matrices, DEFAULT_16,
                item,
                menuX + 32, itemY + 8,
                textColor
            );
            itemY += 26;
        }

        // Подсказка внизу
        FontRenderer.drawCenteredXString(
            matrices, DEFAULT_16,
            "Press INSERT to close",
            cx, animatedY + MENU_HEIGHT - 14,
            accentColor
        );

        GlStateManager.popMatrix();
    }

    /**
     * Вызывается снаружи (из HitColor.onPress) для закрытия с анимацией.
     */
    public void startClose() {
        if (!closing) {
            closing = true;
            // Анимация закрытия: прогресс от текущего значения обратно к 0
            openAnim.begin(openAnim.get(), 0.0, 1.5);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // ESC тоже запускает анимацию закрытия
        startClose();
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Утилиты ──────────────────────────────────────────────────────────────

    /** Smoothstep easing: плавное ускорение и замедление */
    private static double smoothstep(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    /** Создаёт копию цвета с заданной прозрачностью */
    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
