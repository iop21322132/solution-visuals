package farvix.solution.api.translation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Translations {
    
    private static final TranslationManager MANAGER = TranslationManager.getInstance();
    
    public static String tr(String key) {
        return MANAGER.translate(key);
    }
    
    public static String tr(String key, Object... args) {
        return MANAGER.translate(key, args);
    }
    
    public static void setLanguage(String language) {
        MANAGER.setLanguage(language);
    }
    
    public static String getCurrentLanguage() {
        return MANAGER.getCurrentLanguage();
    }
    
    public static boolean hasTranslation(String key) {
        return MANAGER.hasTranslation(key);
    }
}
