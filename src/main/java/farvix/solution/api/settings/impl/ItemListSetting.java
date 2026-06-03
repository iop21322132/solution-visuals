package farvix.solution.api.settings.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.api.Parent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Setting that holds a set of selected Minecraft items.
 * Items are stored by their registry ID (e.g. "minecraft:diamond_sword").
 */
@Getter
public class ItemListSetting extends Setting {

    private final Set<Item> selectedItems = new LinkedHashSet<>();

    public ItemListSetting(String name, Parent parent) {
        super(name, parent);
    }

    public Set<Item> getItems() {
        return Collections.unmodifiableSet(selectedItems);
    }

    public boolean contains(Item item) {
        return selectedItems.contains(item);
    }

    public void add(Item item) {
        selectedItems.add(item);
    }

    public void remove(Item item) {
        selectedItems.remove(item);
    }

    public void toggle(Item item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
    }

    public void clear() {
        selectedItems.clear();
    }

    @Override
    public JsonElement save() {
        JsonArray array = new JsonArray();
        for (Item item : selectedItems) {
            Identifier id = Registries.ITEM.getId(item);
            array.add(new JsonPrimitive(id.toString()));
        }
        return array;
    }

    @Override
    public void load(JsonElement element) {
        selectedItems.clear();
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry.isJsonPrimitive()) {
                    Identifier id = Identifier.tryParse(entry.getAsString());
                    if (id != null && Registries.ITEM.containsId(id)) {
                        selectedItems.add(Registries.ITEM.get(id));
                    }
                }
            }
        }
    }
}
