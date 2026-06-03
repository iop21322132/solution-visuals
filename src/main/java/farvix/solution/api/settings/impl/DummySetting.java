package farvix.solution.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.api.Parent;

/**
 * A no-op setting used as a placeholder for custom GUI components
 * that don't correspond to an actual setting value.
 */
public class DummySetting extends Setting {

    public DummySetting(String name, Parent parent) {
        super(name, parent);
    }

    @Override
    public JsonElement save() {
        return new JsonObject();
    }

    @Override
    public void load(JsonElement element) {
        // nothing to load
    }
}
