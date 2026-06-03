package farvix.solution.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.api.Parent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Getter
public class MultiModeSetting extends Setting {

    private final List<BooleanSetting> boolSettings;

    public MultiModeSetting(String name, Parent parent, String... values) {
        super(name, parent);
        this.boolSettings = new ArrayList<>();

        Arrays.stream(values).forEach(value -> boolSettings.add(new BooleanSetting(value, null).setVisible(() -> false)));
    }

    public BooleanSetting get(String name) {
        return boolSettings.stream().filter(value -> value.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public BooleanSetting get(int index) {
        if (index >= 0 && index < boolSettings.size()) {
            return boolSettings.get(index);
        }
        return null;
    }

    public BooleanSetting getRandomEnabledElement() {
        List<BooleanSetting> enabledElements = boolSettings.stream()
                .filter(BooleanSetting::isEnabled)
                .toList();

        if (!enabledElements.isEmpty()) {
            Random random = new Random();
            return enabledElements.get(random.nextInt(enabledElements.size()));
        }
        return null;
    }

    @Override
    public JsonElement save() {
        JsonObject object = new JsonObject();
        for (BooleanSetting setting : boolSettings) {
            JsonObject settingObject = new JsonObject();
            settingObject.addProperty("enabled", setting.isEnabled());
            settingObject.addProperty("key", setting.getKey());
            object.add(setting.getName(), settingObject);
        }
        return object;
    }

    @Override
    public void load(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        for (BooleanSetting setting : boolSettings) {
            if (object.has(setting.getName())) {
                JsonObject settingObject = object.get(setting.getName()).getAsJsonObject();
                if (settingObject.has("enabled")) {
                    setting.setEnabled(settingObject.get("enabled").getAsBoolean());
                }
                if (settingObject.has("key")) {
                    setting.setKey(settingObject.get("key").getAsInt());
                }
            }
        }
    }
}
