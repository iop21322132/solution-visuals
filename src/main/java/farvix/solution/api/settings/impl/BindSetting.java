package farvix.solution.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.Setter;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.api.Parent;

@Getter @Setter
public class BindSetting extends Setting {
    private int key;

    public BindSetting(String name, Parent parent, int defaultKey) {
        super(name, parent);
        this.key = defaultKey;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(key);
    }

    @Override
    public void load(JsonElement element) {
        setKey(element.getAsInt());
    }
}
