package farvix.solution.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.api.Parent;

@Setter
@Getter
public class StringSetting extends Setting {
    private String text = "";
    private int maxLength = 100;

    public StringSetting(String name, Parent parent) {
        super(name, parent);
    }

    public StringSetting(String name, String description, Parent parent) {
        super(name, description, parent);
    }

    public StringSetting(String name, String description, String defaultValue, Parent parent) {
        super(name, description, parent);
        this.text = defaultValue;
    }

    public StringSetting(String name, String description, String defaultValue, int maxLength, Parent parent) {
        super(name, description, parent);
        this.text = defaultValue;
        this.maxLength = maxLength;
    }

    @Override
    public JsonElement save() {
        JsonObject object = new JsonObject();
        object.addProperty("text", text);
        object.addProperty("maxLength", maxLength);
        return object;
    }

    @Override
    public void load(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        if (object.has("text")) {
            setText(object.get("text").getAsString());
        }
        if (object.has("maxLength")) {
            setMaxLength(object.get("maxLength").getAsInt());
        }
    }
}
