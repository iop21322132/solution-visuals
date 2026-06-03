package farvix.solution.api.util.performance;

/**
 * Утилита для оптимизации рендера
 */
public class RenderOptimizer {
    
    private static long lastFrameTime = 0;
    private static int frameSkipCounter = 0;
    private static final long MIN_FRAME_TIME = 16; // ~60 FPS
    
    /**
     * Проверяет нужно ли пропустить рендер для оптимизации
     * @param priority приоритет рендера (0 = высокий, 1 = средний, 2 = низкий)
     * @return true если нужно рендерить, false если пропустить
     */
    public static boolean shouldRender(int priority) {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastFrameTime;
        
        // Если FPS высокий - рендерим все
        if (deltaTime >= MIN_FRAME_TIME) {
            lastFrameTime = currentTime;
            frameSkipCounter = 0;
            return true;
        }
        
        // Если FPS низкий - пропускаем низкоприоритетные элементы
        frameSkipCounter++;
        
        switch (priority) {
            case 0: // Высокий приоритет - всегда рендерим
                return true;
            case 1: // Средний - рендерим каждый 2й кадр при низком FPS
                return frameSkipCounter % 2 == 0;
            case 2: // Низкий - рендерим каждый 3й кадр при низком FPS
                return frameSkipCounter % 3 == 0;
            default:
                return true;
        }
    }
    
    /**
     * Сброс счетчика (вызывать в начале каждого кадра)
     */
    public static void resetFrame() {
        frameSkipCounter = 0;
    }
}
