package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.HitResult;

import java.awt.*;

public class Crosshair extends Module implements ThemeManager.ThemeChangeListener {

    private static Crosshair instance;

    private final NumberSetting thickness = new NumberSetting("Толщина", 1f, 0.5f, 3f, 0.1f);
    private final NumberSetting length = new NumberSetting("Длина", 3f, 1f, 8f, 0.5f);
    private final NumberSetting gap = new NumberSetting("Разрыв", 2f, 0f, 5f, 0.5f);
    private final BooleanSetting dynamicGap = new BooleanSetting("Динамический разрыв", false);
    private final BooleanSetting useEntityColor = new BooleanSetting("Цвет при наведении", false);

    private final ThemeManager themeManager;
    private Color currentColor;
    private final Color entityColor = new Color(255, 0, 0); // Красный без прозрачности

    public Crosshair() {
        super("Crosshair", Category.Render, "Кастомный прицел");
        instance = this;
        themeManager = ThemeManager.getInstance();
        // Принудительно устанавливаем непрозрачный цвет
        currentColor = getOpaqueColor(themeManager.getThemeColor());
        themeManager.addThemeChangeListener(this);
    }

    public static Crosshair getInstance() {
        return instance;
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.world == null) return;

        if (!mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        float x = sw * 0.5f;
        float y = sh * 0.5f;

        float currentGap = gap.getValue();
        if (dynamicGap.getValue()) {
            float cooldown = 1f - mc.player.getAttackCooldownProgress(0);
            currentGap = Math.min(currentGap + 8f * cooldown, 10f);
        }

        float w = thickness.getValue();
        float l = length.getValue();

        // Гарантируем непрозрачность цвета
        Color color = useEntityColor.getValue() &&
                mc.crosshairTarget != null &&
                mc.crosshairTarget.getType() == HitResult.Type.ENTITY
                ? entityColor : getOpaqueColor(currentColor);

        var matrices = e.getContext().getMatrices();
        Render2D.drawRect(matrices, x - w / 2, y - currentGap - l, w, l, color);
        Render2D.drawRect(matrices, x - w / 2, y + currentGap, w, l, color);
        Render2D.drawRect(matrices, x - currentGap - l, y - w / 2, l, w, color);
        Render2D.drawRect(matrices, x + currentGap, y - w / 2, l, w, color);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        // Полностью заменяем цвет на непрозрачный
        this.currentColor = getOpaqueColor(theme.getBackgroundColor());
    }

    /**
     * Гарантирует, что цвет будет полностью непрозрачным
     */
    private Color getOpaqueColor(Color color) {
        // Если цвет null, возвращаем белый непрозрачный
        if (color == null) {
            return Color.WHITE;
        }
        // Создаем новый цвет с теми же RGB значениями, но без прозрачности
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }
}