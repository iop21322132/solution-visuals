package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

/**
 * Настройки производительности для оптимизации FPS
 */
@ModuleInfo(name = "Performance", category = ModuleCategory.VISUALS, 
        description = "Настройки производительности")
public class PerformanceSettings extends Module {

    // ── Blur эффекты ──────────────────────────────────────────────────────────
    public final BooleanSetting disableBlur = new BooleanSetting(
            "Отключить blur эффекты", this);

    public final SliderSetting blurQuality = new SliderSetting(
            "Качество blur", this, 20f, 5f, 40f, 5f)
            .setVisible(() -> !disableBlur.isEnabled());

    // ── Анимации ──────────────────────────────────────────────────────────────
    public final BooleanSetting reduceAnimations = new BooleanSetting(
            "Упростить анимации", this);

    public final SliderSetting animationSpeed = new SliderSetting(
            "Скорость анимаций", this, 1.0f, 0.5f, 2.0f, 0.1f)
            .setVisible(() -> !reduceAnimations.isEnabled());

    // ── Частицы ───────────────────────────────────────────────────────────────
    public final BooleanSetting reduceParticles = new BooleanSetting(
            "Уменьшить частицы", this);

    public final SliderSetting particleLimit = new SliderSetting(
            "Лимит частиц", this, 100f, 20f, 200f, 10f)
            .setVisible(() -> reduceParticles.isEnabled());

    // ── Рендеринг сущностей ───────────────────────────────────────────────────
    public final BooleanSetting optimizeEntityRendering = new BooleanSetting(
            "Оптимизировать рендер сущностей", this);

    public final SliderSetting entityRenderDistance = new SliderSetting(
            "Дистанция рендера сущностей", this, 64f, 16f, 128f, 8f)
            .setVisible(() -> optimizeEntityRendering.isEnabled());

    // ── Информация ────────────────────────────────────────────────────────────
    public final BooleanSetting showPerformanceInfo = new BooleanSetting(
            "Показывать инфо о производительности", this);

    // ── Геттеры для использования в других модулях ───────────────────────────

    /**
     * Проверяет, нужно ли отключить blur эффекты
     */
    public boolean shouldDisableBlur() {
        return isEnabled() && disableBlur.isEnabled();
    }

    /**
     * Получить качество blur (5-40)
     */
    public float getBlurQuality() {
        if (!isEnabled()) return 20f;
        return blurQuality.getValue();
    }

    /**
     * Проверяет, нужно ли упростить анимации
     */
    public boolean shouldReduceAnimations() {
        return isEnabled() && reduceAnimations.isEnabled();
    }

    /**
     * Получить множитель скорости анимаций
     */
    public float getAnimationSpeedMultiplier() {
        if (!isEnabled()) return 1.0f;
        if (shouldReduceAnimations()) return 1.5f;
        return animationSpeed.getValue();
    }

    /**
     * Получить лимит частиц
     */
    public int getParticleLimit() {
        if (!isEnabled()) return 100;
        if (reduceParticles.isEnabled()) return (int) particleLimit.getValue();
        return 100;
    }

    /**
     * Получить дистанцию рендера сущностей
     */
    public float getEntityRenderDistance() {
        if (!isEnabled()) return 128f;
        if (optimizeEntityRendering.isEnabled()) return entityRenderDistance.getValue();
        return 128f;
    }

    /**
     * Получить информацию о производительности
     */
    public String getPerformanceInfo() {
        if (!isEnabled()) return "Performance: OFF";    
        
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        int memoryPercent = (int) ((usedMemory * 100) / maxMemory);
        
        return String.format("§fRAM: §%s%d/%dMB §7(%d%%)",
                memoryPercent > 80 ? "c" : memoryPercent > 60 ? "e" : "a",
                usedMemory,
                maxMemory,
                memoryPercent
        );
    }
}
        