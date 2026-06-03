package farvix.solution.api.settings;

import com.google.gson.JsonElement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import farvix.solution.api.settings.api.Parent;
import farvix.solution.client.modules.Module;

import java.util.function.Supplier;


@Getter @Setter @RequiredArgsConstructor
public abstract class Setting {

    protected String name;
    protected String description;

    protected Supplier<Boolean> hideCondition;

    protected int key = -1;
    protected Parent parent;

    public abstract JsonElement save();

    public abstract void load(JsonElement element);

    public Setting(String name, Parent parent) {
        this(name, null, parent);
    }

    public Setting(String name, String description, Parent parent) {
        this.name = name;
        this.description = description;
        this.parent = parent;

        if (parent instanceof Module module) {
            module.getSettings().add(this);
            return;
        }
    }

    public <T extends Setting> T setVisible(Supplier<Boolean> hide) {
        this.hideCondition = hide;
        return (T) this;
    }
}
