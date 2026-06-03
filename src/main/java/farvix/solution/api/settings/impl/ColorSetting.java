package farvix.solution.api.settings.impl;

import com.google.gson.JsonElement; // Импорт для работы с JSON
import com.google.gson.JsonObject; // Импорт для работы с JSON объектами
import com.google.gson.JsonPrimitive; // Импорт для работы с JSON
import lombok.Getter; // Lombok аннотация для автоматической генерации геттеров
import lombok.Setter; // Lombok аннотация для автоматической генерации сеттеров
import farvix.solution.api.settings.Setting; // Базовый класс для всех настроек
import farvix.solution.api.settings.api.Parent; // Интерфейс для родительского элемента настройки
import farvix.solution.api.util.color.FixColor; // Утилита для работы с цветом

@Getter @Setter
public class ColorSetting extends Setting {
    public int color = 0;
    public boolean rainbow;
    public boolean overridden = false;
    public static boolean syncWithTheme = false;

    public ColorSetting(String name, Parent parent, int color) {
        super(name, parent);
        this.color = color;
    } // Конструктор для инициализации настройки цвета


    public int get() {
        if (syncWithTheme && !overridden) {
            java.awt.Color themeAccent = farvix.solution.client.managers.ThemeManager.getInstance().getCurrentTheme().getAccentColor();
            int originalAlpha = (this.color >> 24) & 0xFF;
            int r = themeAccent.getRed();
            int g = themeAccent.getGreen();
            int b = themeAccent.getBlue();
            return (originalAlpha << 24) | (r << 16) | (g << 8) | b;
        }
        return color;
    } // Возвращает текущее значение цвета

    public String getHex() {
        return String.format("#%06X", (0xFFFFFF & get()));
    } // Возвращает цвет в шестнадцатеричном формате

    public FixColor getColor() {
        return new FixColor(get());
    } // Возвращает объект FixColor на основе текущего цвета


    @Override
    public JsonElement save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("color", color);
        obj.addProperty("overridden", overridden);
        return obj;
    } // Метод для сохранения значения цвета в JSON

    @Override
    public void load(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("color")) {
                this.color = obj.get("color").getAsInt();
            }
            if (obj.has("overridden")) {
                this.overridden = obj.get("overridden").getAsBoolean();
            }
        } else if (element != null) {
            this.color = element.getAsInt();
            this.overridden = false;
        }
    } // Метод для загрузки значения цвета из JSON

    public static void resetAllOverridden() {
        if (farvix.solution.Client.getInstance() == null || farvix.solution.Client.getInstance().getModuleManager() == null) {
            return;
        }
        for (farvix.solution.client.modules.Module module : farvix.solution.Client.getInstance().getModuleManager().getModules()) {
            for (farvix.solution.api.settings.Setting setting : module.getSettings()) {
                if (setting instanceof ColorSetting colorSetting) {
                    colorSetting.overridden = false;
                }
            }
        }
        // Автосохранение для фиксации состояния сброса
        try {
            farvix.solution.Client.getInstance().getConfigManager().saveConfig("_autosave");
        } catch (Throwable ignored) {}
    }
}
