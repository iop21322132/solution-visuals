package farvix.solution.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.api.Parent;

public class BooleanSetting extends Setting {
    private boolean enabled = false;
    private int key = -1;

    public BooleanSetting(String name, Parent parent) {
        super(name, parent);
    }

    public BooleanSetting(String name, String description, Parent parent) {
        super(name, description, parent);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getValue() {
        return enabled;
    }

    public void setValue(boolean value) {
        this.enabled = value;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    @Override
    public JsonElement save() {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", enabled);
        object.addProperty("key", key);
        return object;
    }

    @Override
    public void load(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        if (object.has("enabled")) {
            setEnabled(object.get("enabled").getAsBoolean());
        }
        if (object.has("key")) {
            setKey(object.get("key").getAsInt());
        }
    }
}
