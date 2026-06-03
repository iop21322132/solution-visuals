package farvix.solution.api.translation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import farvix.solution.api.interfaces.QuickImports;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class TranslationManager implements QuickImports {
    
    private static volatile TranslationManager instance;
    private final Map<String, String> translations = new HashMap<>();

    @Getter
    private final List<String> availableLanguages = Arrays.asList("en", "ru");

    @Getter
    private String currentLanguage = "en";
    
    private TranslationManager() {
        loadTranslations("en");
    }
    
    public static TranslationManager getInstance() {
        if (instance == null) {
            synchronized (TranslationManager.class) {
                if (instance == null) {
                    instance = new TranslationManager();
                }
            }
        }
        return instance;
    }
    
    public void setLanguage(String language) {
        if (!currentLanguage.equals(language)) {
            currentLanguage = language;
            loadTranslations(language);
        }
    }
    
    private void loadTranslations(String language) {
        translations.clear();
        
        try {
            String fileName = "/assets/solution/lang/" + language + ".json";
            InputStream inputStream = getClass().getResourceAsStream(fileName);
            
            if (inputStream == null) {
                if (!language.equals("en")) {
                    loadTranslations("en");
                }
                return;
            }
            
            JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            
            jsonObject.entrySet().forEach(entry -> 
                translations.put(entry.getKey(), entry.getValue().getAsString())
            );
            
            inputStream.close();
        } catch (Exception e) {
            log.error("Failed to load translations for language: {}", language, e);
        }
    }
    
    public String translate(String key) {
        return translations.getOrDefault(key, key);
    }
    
    public String translate(String key, Object... args) {
        String translation = translate(key);
        
        for (int i = 0; i < args.length; i++) {
            translation = translation.replace("{" + i + "}", String.valueOf(args[i]));
        }
        
        return translation;
    }
    
    public boolean hasTranslation(String key) {
        return translations.containsKey(key);
    }
}
