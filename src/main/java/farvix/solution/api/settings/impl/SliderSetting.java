package farvix.solution.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.Setter;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.api.Parent;

@Getter @Setter
public class SliderSetting extends Setting {

    private float min, max, step, value;

    public SliderSetting(String name, Parent parent, float defaultValue, float min, float max, float step) {
        super(name, parent);
        this.step = step;
        this.min = min;
        this.max = max;
        this.value = defaultValue;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            this.setCurrentValue(element.getAsFloat());
        }
    }

    public void setCurrentValue(float currentValue) {
        this.value = Math.max(min, Math.min(max, currentValue));
    }
}
