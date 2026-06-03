package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.BindSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Zoom", category = ModuleCategory.VISUALS,
        description = "Приближение камеры")
public class Zoom extends Module {

    public final BindSetting zoomKey = new BindSetting(
            "Кнопка зума", this, GLFW.GLFW_KEY_C);

    // ── Состояние (статическое — читается из миксина) ─────────────────────────

    /** Текущий FOV множитель (1.0 = нормальный, < 1.0 = приближение) */
    public static float currentFovMultiplier = 1.0f;
    /** Целевой FOV множитель */
    public static float targetFovMultiplier  = 1.0f;
    /** Уровень зума: 2–20. Чем больше — тем сильнее приближение */
    public static float zoomLevel = 4.0f;

    private static long lastTickNanos = System.nanoTime();

    // ── Анимация ──────────────────────────────────────────────────────────────

    /**
     * Вызывается каждый рендер-кадр из GameRendererMixin.
     * Использует реальное время (delta) для FPS-независимой анимации.
     */
    public static void tick() {
        long now = System.nanoTime();
        float dt = (now - lastTickNanos) / 1_000_000_000f; // секунды
        lastTickNanos = now;

        // Clamp dt чтобы не было прыжков при лагах
        dt = Math.min(dt, 0.1f);

        // Скорость анимации: ~10 единиц в секунду
        // При dt=0.016 (60fps) шаг = 0.16 — плавно за ~6 кадров
        float speed = 10.0f;
        float diff = targetFovMultiplier - currentFovMultiplier;

        if (Math.abs(diff) < 0.0001f) {
            currentFovMultiplier = targetFovMultiplier;
        } else {
            currentFovMultiplier += diff * Math.min(1.0f, speed * dt);
        }
    }

    // ── Скролл ────────────────────────────────────────────────────────────────

    /**
     * Вызывается из MouseMixin при скролле во время зума.
     * Скролл вверх = приближение (zoomLevel растёт),
     * Скролл вниз = отдаление (zoomLevel уменьшается).
     * Минимум 2 чтобы нельзя было "отзумиться" до нормального FOV.
     */
    public static void onScroll(double delta) {
        zoomLevel = (float) Math.max(2.0, Math.min(20.0, zoomLevel + delta));
        targetFovMultiplier = 1.0f / zoomLevel;
    }

    /**
     * Сбрасывает уровень зума к дефолту при отпускании кнопки.
     * Вызывается из GameRendererMixin.
     */
    public static void resetZoomLevel() {
        zoomLevel = 4.0f;
    }

    // ── Проверка клавиши ──────────────────────────────────────────────────────

    public static boolean isZoomKeyHeld(Zoom module) {
        if (module == null || !module.isEnabled()) return false;
        int key = module.zoomKey.getKey();
        if (key < 0) return false;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        // Не зумить когда открыт любой экран (инвентарь, чат и т.д.)
        if (mc.currentScreen != null) return false;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}
