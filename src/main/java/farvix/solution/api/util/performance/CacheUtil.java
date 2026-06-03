package farvix.solution.api.util.performance;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Утилита для кэширования вычислений
 */
public class CacheUtil {
    
    private static final Map<String, CachedValue<?>> cache = new HashMap<>();
    
    /**
     * Получить закэшированное значение или вычислить новое
     * @param key ключ кэша
     * @param supplier функция для вычисления значения
     * @param ttl время жизни кэша в миллисекундах
     * @return закэшированное или новое значение
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrCompute(String key, Supplier<T> supplier, long ttl) {
        CachedValue<T> cached = (CachedValue<T>) cache.get(key);
        
        long currentTime = System.currentTimeMillis();
        
        if (cached != null && currentTime - cached.timestamp < ttl) {
            return cached.value;
        }
        
        T newValue = supplier.get();
        cache.put(key, new CachedValue<>(newValue, currentTime));
        return newValue;
    }
    
    /**
     * Очистить весь кэш
     */
    public static void clearAll() {
        cache.clear();
    }
    
    /**
     * Очистить конкретный ключ
     */
    public static void clear(String key) {
        cache.remove(key);
    }
    
    private static class CachedValue<T> {
        final T value;
        final long timestamp;
        
        CachedValue(T value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
