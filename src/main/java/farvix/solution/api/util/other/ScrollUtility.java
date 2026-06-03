package farvix.solution.api.util.other;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.ui.clickgui.InterfaceScreen;

@Getter
public class ScrollUtility implements QuickImports {
    private float target;
    private float scroll;
    private float max;

    /**
     * Устанавливает максимальный скролл.
     * Если новый max позволяет скроллить дальше — target не трогаем.
     * Если новый max обрезает текущий target — подтягиваем target к новому max.
     */
    public void setMax(float newMax) {
        this.max = newMax;
        // Если target вышел за новый max — подтянуть
        if (target < newMax) target = newMax;
        if (scroll < newMax) scroll = newMax;
    }
    
    // Параметры плавной прокрутки
    private float smoothness = 0.08f;  // Скорость интерполяции
    private float scrollSpeed = 30f;  // Множитель скорости прокрутки колесиком

    public void handle() {
        InterfaceScreen screen = (InterfaceScreen) mc.currentScreen;
        if (screen == null) return;

        // Обновляем целевую позицию на основе колесика мыши
        float wheel = screen.dWheel * scrollSpeed;
        target += wheel;
        target = MathHelper.clamp(target, max, 0);
        
        // Плавная интерполяция к целевой позиции
        float delta = target - scroll;
        if (Math.abs(delta) > 0.01f) {
            scroll += delta * smoothness;
        } else {
            scroll = target; // Защелкиваем к целевому значению когда очень близко
        }
        
        // Ограничиваем scroll в допустимых пределах
        scroll = MathHelper.clamp(scroll, max, 0);
        
        screen.dWheel = 0;
    }
    
    /**
     * Устанавливает целевую позицию прокрутки напрямую (для ползунка)
     */
    public void setScroll(float value) {
        this.scroll = MathHelper.clamp(value, max, 0);
        this.target = this.scroll;
    }
    
    /**
     * Устанавливает целевую позицию с плавной анимацией
     */
    public void setTargetScroll(float value) {
        this.target = MathHelper.clamp(value, max, 0);
    }
    
    /**
     * Настройка плавности прокрутки
     * @param smoothness значение от 0.05 (очень плавно) до 1.0 (мгновенно)
     */
    public void setSmoothness(float smoothness) {
        this.smoothness = MathHelper.clamp(smoothness, 0.05f, 1.0f);
    }
    
    /**
     * Настройка скорости прокрутки колесиком
     * @param speed множитель скорости (рекомендуется 10-30)
     */
    public void setScrollSpeed(float speed) {
        this.scrollSpeed = Math.max(1f, speed);
    }
}
