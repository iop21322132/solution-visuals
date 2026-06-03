package farvix.solution.api.ui.clickgui.impl.settings;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import farvix.solution.api.TempColor;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.ItemListSetting;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.ui.clickgui.api.SettingComponent;
import farvix.solution.api.util.color.FixColor;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.*;
import java.util.List;

@Getter
public class ItemListSettingComponent extends SettingComponent {

    private static final int   MAX_VISIBLE = 5;
    private static final float ROW_H       = 18f;
    private static final float PADDING     = 4f;
    private static final float TAG_H       = 18f;
    private static final float FIELD_H     = 18f;

    private ItemListSetting itemListSetting;

    @Getter private boolean searchFocused = false;
    private String searchText = "";

    private final List<Item> filteredItems = new ArrayList<>();

    // ── Russian keyword → registry path fragment map ──────────────────────────
    // Covers the most common items a player would search for in Russian.
    private static final Map<String, String> RU_KEYWORDS = buildRuKeywords();

    public ItemListSettingComponent(ItemListSetting setting, ModuleComponent moduleComponent) {
        super(setting, moduleComponent);
        this.itemListSetting = setting;
    }

    @Override
    public void init() {
        this.itemListSetting = (ItemListSetting) getSetting();
        this.width = moduleComponent.getWidth();
        rebuildFilter();
        recalcHeight();
        super.init();
    }

    // ── Height ────────────────────────────────────────────────────────────────

    private void recalcHeight() {
        float h = PADDING + FIELD_H + PADDING;
        if (!searchText.isEmpty()) {
            h += Math.min(filteredItems.size(), MAX_VISIBLE) * (ROW_H + 2) + PADDING;
        }
        if (!itemListSetting.getItems().isEmpty()) {
            h += PADDING + computeTagRows() * (TAG_H + 3);
        }
        h += PADDING;
        this.height = h;
    }

    private int computeTagRows() {
        if (itemListSetting.getItems().isEmpty()) return 0;
        float rowWidth = 0;
        int rows = 1;
        for (Item item : itemListSetting.getItems()) {
            float tw = tagWidth(item);
            if (rowWidth + tw + 3 > width - 10 && rowWidth > 0) {
                rows++;
                rowWidth = 0;
            }
            rowWidth += tw + 3;
        }
        return rows;
    }

    private float tagWidth(Item item) {
        return 16 + 3 + Fonts.DEFAULT.get(13).getStringWidth(getShortName(item)) + 6;
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    private void rebuildFilter() {
        filteredItems.clear();
        String raw = searchText.toLowerCase().trim();
        if (raw.isEmpty()) return;

        // Resolve query: if Russian input matches a keyword, use its English equivalent.
        // Also try transliteration as fallback.
        String enQuery = resolveQuery(raw);

        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            Identifier id = Registries.ITEM.getId(item);
            String path = id.getPath(); // e.g. "diamond_sword"

            // Match against English registry path
            if (path.contains(enQuery)) {
                filteredItems.add(item);
            }
            // Also match the display name (English) against the original raw query
            // so typing "dia" still finds diamond
            else if (!enQuery.equals(raw) && path.contains(raw)) {
                filteredItems.add(item);
            }

            if (filteredItems.size() >= 50) break;
        }
    }

    /**
     * Resolves a (possibly Russian) query to an English registry path fragment.
     *
     * Strategy:
     * 1. Check if the raw string starts with any Russian keyword → return its English value.
     * 2. Otherwise transliterate character-by-character (covers phonetic input like "диамонд").
     * 3. If the result is the same as input (no Cyrillic), return as-is.
     */
    private String resolveQuery(String raw) {
        // 1. Keyword lookup (longest match first)
        String best = null;
        int bestLen = 0;
        for (Map.Entry<String, String> e : RU_KEYWORDS.entrySet()) {
            String ruWord = e.getKey();
            if (raw.startsWith(ruWord) && ruWord.length() > bestLen) {
                best = e.getValue();
                bestLen = ruWord.length();
            }
        }
        if (best != null) return best;

        // 2. Character-by-character transliteration
        return transliterate(raw);
    }

    private static String transliterate(String input) {
        Map<String, String> t = TRANSLIT;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            // Try 2-char sequences first (е, ё etc. are single chars but sch is 3 latin)
            String c = String.valueOf(input.charAt(i));
            String mapped = t.get(c);
            sb.append(mapped != null ? mapped : c);
            i++;
        }
        return sb.toString();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        super.render(context, x, y, mouseX, mouseY, delta);
        rebuildFilter();
        recalcHeight();

        float alpha = getClickGUI().getAlpha().getValue();
        float cx = x + 5;
        float cw = width - 10;
        float cy = y + PADDING;

        // ── Search field ──────────────────────────────────────────────────────
        Color fieldBg = searchFocused
                ? TempColor.getModuleBackground().alpha(alpha).getColor()
                : TempColor.getGuiBackground().alpha(alpha * 0.9f).getColor();
        Color fieldBorder = searchFocused
                ? TempColor.getClientColor().alpha(alpha * 0.87f).getColor()
                : TempColor.getModuleBorder().alpha(alpha * 0.8f).getColor();

        blur.render(ShapeProperties.create(context.getMatrices(), cx, cy, cw, FIELD_H)
                .round(3).softness(1).thickness(1.5f)
                .outlineColor(fieldBorder.getRGB())
                .color(fieldBg.getRGB())
                .build());

        String displaySearch = searchText.isEmpty() && !searchFocused
                ? "Поиск предметов..."
                : searchText + (searchFocused ? "|" : "");
        Color searchColor = searchText.isEmpty() && !searchFocused
                ? TempColor.getTextSecondary().alpha(alpha * 0.87f)
                : TempColor.getTextPrimary().alpha(alpha);
        Fonts.DEFAULT.get(13).drawBoldString(context.getMatrices(), displaySearch,
                cx + 3, cy + FIELD_H / 2f - 3.5f, searchColor.getRGB());

        cy += FIELD_H + PADDING;

        // ── Results ───────────────────────────────────────────────────────────
        if (!searchText.isEmpty()) {
            int visible = Math.min(filteredItems.size(), MAX_VISIBLE);
            for (int i = 0; i < visible; i++) {
                Item item = filteredItems.get(i);
                boolean selected = itemListSetting.contains(item);
                boolean hovered  = mouseX >= cx && mouseX <= cx + cw
                        && mouseY >= cy && mouseY <= cy + ROW_H;

                if (selected || hovered) {
                    Color rowBg = selected
                            ? TempColor.getClientColor().alpha(alpha * 0.75f).getColor()
                            : TempColor.getModuleBackground().alpha(alpha * 0.83f).getColor();
                    blur.render(ShapeProperties.create(context.getMatrices(), cx, cy, cw, ROW_H)
                            .round(2).color(rowBg.getRGB()).build());
                }

                context.drawItem(item.getDefaultStack(), (int)(cx + 1), (int)(cy + (ROW_H - 16) / 2f));

                String label = getShortName(item);
                Color labelColor = selected
                        ? TempColor.getClientColor().alpha(alpha).getColor()
                        : TempColor.getTextPrimary().alpha(alpha).getColor();
                Fonts.DEFAULT.get(13).drawBoldString(context.getMatrices(), label,
                        cx + 14 + 2, cy + ROW_H / 2f - 3.5f, labelColor.getRGB());

                cy += ROW_H + 2;
            }
            cy += PADDING;
        }

        // ── Selected tags ─────────────────────────────────────────────────────
        if (!itemListSetting.getItems().isEmpty()) {
            cy += PADDING;
            float tagX = cx;
            float tagY = cy;

            for (Item item : itemListSetting.getItems()) {
                float tw = tagWidth(item);

                if (tagX + tw + 3 > x + width - 5 && tagX > cx) {
                    tagX = cx;
                    tagY += TAG_H + 3;
                }

                boolean tagHovered = mouseX >= tagX && mouseX <= tagX + tw
                        && mouseY >= tagY && mouseY <= tagY + TAG_H;

                Color tagBg = tagHovered
                        ? TempColor.getClientColor().alpha(alpha * 0.83f).getColor()
                        : TempColor.getClientColor().alpha(alpha * 0.75f).getColor();

                blur.render(ShapeProperties.create(context.getMatrices(), tagX, tagY, tw, TAG_H)
                        .round(3).color(tagBg.getRGB()).build());

                // Icon
                float iconY = tagY + (TAG_H - 16) / 2f;
                context.drawItem(item.getDefaultStack(), (int)(tagX + 1), (int)iconY);

                // Name
                Fonts.DEFAULT.get(13).drawBoldString(context.getMatrices(), getShortName(item),
                        tagX + 16 + 3, tagY + TAG_H / 2f - 3.5f,
                        TempColor.getTextPrimary().alpha(alpha).getRGB());

                tagX += tw + 3;
            }
        }
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        float cx = x + 5;
        float cw = width - 10;
        float cy = y + PADDING;

        searchFocused = mouseX >= cx && mouseX <= cx + cw
                && mouseY >= cy && mouseY <= cy + FIELD_H;

        cy += FIELD_H + PADDING;

        if (!searchText.isEmpty()) {
            int visible = Math.min(filteredItems.size(), MAX_VISIBLE);
            for (int i = 0; i < visible; i++) {
                Item item = filteredItems.get(i);
                if (mouseX >= cx && mouseX <= cx + cw
                        && mouseY >= cy && mouseY <= cy + ROW_H) {
                    itemListSetting.toggle(item);
                    return;
                }
                cy += ROW_H + 2;
            }
            cy += PADDING;
        }

        if (!itemListSetting.getItems().isEmpty()) {
            cy += PADDING;
            float tagX = cx;
            float tagY = cy;
            List<Item> toRemove = new ArrayList<>();
            for (Item item : itemListSetting.getItems()) {
                float tw = tagWidth(item);
                if (tagX + tw + 3 > x + width - 5 && tagX > cx) {
                    tagX = cx;
                    tagY += TAG_H + 3;
                }
                if (mouseX >= tagX && mouseX <= tagX + tw
                        && mouseY >= tagY && mouseY <= tagY + TAG_H) {
                    toRemove.add(item);
                }
                tagX += tw + 3;
            }
            toRemove.forEach(itemListSetting::remove);
        }
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!searchFocused) return;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                rebuildFilter();
            }
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            searchFocused = false;
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (!searchFocused) return;
        if (searchText.length() < 32) {
            searchText += codePoint;
            rebuildFilter();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getShortName(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        String path = id.getPath();
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) sb.append(p.substring(1));
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }

    // ── Russian keyword → English registry path fragment ─────────────────────
    // Covers common items. Key = Russian word (or prefix), Value = English path fragment.
    private static Map<String, String> buildRuKeywords() {
        Map<String, String> m = new LinkedHashMap<>();
        // Weapons & tools
        m.put("меч", "sword");
        m.put("лук", "bow");
        m.put("арбалет", "crossbow");
        m.put("стрел", "arrow");
        m.put("топор", "axe");
        m.put("кирка", "pickaxe");
        m.put("лопата", "shovel");
        m.put("мотыга", "hoe");
        m.put("трезубец", "trident");
        // Armor
        m.put("шлем", "helmet");
        m.put("нагрудник", "chestplate");
        m.put("кираса", "chestplate");
        m.put("поножи", "leggings");
        m.put("ботинки", "boots");
        m.put("сапоги", "boots");
        m.put("броня", "armor");
        // Materials
        m.put("алмаз", "diamond");
        m.put("железо", "iron");
        m.put("железн", "iron");
        m.put("золото", "gold");
        m.put("золот", "gold");
        m.put("золотой", "golden");
        m.put("золотая", "golden");
        m.put("золотое", "golden");
        m.put("незерит", "netherite");
        m.put("камень", "stone");
        m.put("дерево", "wood");
        m.put("деревян", "wooden");
        m.put("кожа", "leather");
        m.put("кожан", "leather");
        m.put("цепная", "chainmail");
        m.put("цепной", "chainmail");
        // Food
        m.put("яблоко", "apple");
        m.put("яблок", "apple");
        m.put("хлеб", "bread");
        m.put("морковь", "carrot");
        m.put("морков", "carrot");
        m.put("картофель", "potato");
        m.put("картошка", "potato");
        m.put("мясо", "beef");
        m.put("говядина", "beef");
        m.put("курица", "chicken");
        m.put("свинина", "pork");
        m.put("рыба", "fish");
        m.put("лосось", "salmon");
        m.put("треска", "cod");
        // Blocks & materials
        m.put("камень", "stone");
        m.put("булыжник", "cobblestone");
        m.put("дерево", "log");
        m.put("доски", "planks");
        m.put("песок", "sand");
        m.put("гравий", "gravel");
        m.put("земля", "dirt");
        m.put("трава", "grass");
        m.put("обсидиан", "obsidian");
        m.put("стекло", "glass");
        m.put("лед", "ice");
        m.put("снег", "snow");
        m.put("лава", "lava");
        m.put("вода", "water");
        // Special items
        m.put("тотем", "totem");
        m.put("жемчуг", "ender_pearl");
        m.put("эндер", "ender");
        m.put("зелье", "potion");
        m.put("книга", "book");
        m.put("перо", "feather");
        m.put("нить", "string");
        m.put("кость", "bone");
        m.put("уголь", "coal");
        m.put("изумруд", "emerald");
        m.put("рубин", "ruby");
        m.put("лазурит", "lapis");
        m.put("редстоун", "redstone");
        m.put("кварц", "quartz");
        m.put("слиток", "ingot");
        m.put("самородок", "nugget");
        m.put("порошок", "dust");
        m.put("семена", "seeds");
        m.put("семя", "seeds");
        m.put("факел", "torch");
        m.put("лестница", "ladder");
        m.put("дверь", "door");
        m.put("люк", "trapdoor");
        m.put("сундук", "chest");
        m.put("печь", "furnace");
        m.put("верстак", "crafting");
        m.put("наковальня", "anvil");
        m.put("щит", "shield");
        m.put("седло", "saddle");
        m.put("поводок", "lead");
        m.put("компас", "compass");
        m.put("часы", "clock");
        m.put("карта", "map");
        m.put("удочка", "fishing_rod");
        m.put("ножницы", "shears");
        m.put("ведро", "bucket");
        m.put("фонарь", "lantern");
        m.put("колокол", "bell");
        return Collections.unmodifiableMap(m);
    }

    // ── Character transliteration table (fallback) ────────────────────────────
    private static final Map<String, String> TRANSLIT = buildTranslit();

    private static Map<String, String> buildTranslit() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("а","a"); m.put("б","b"); m.put("в","v"); m.put("г","g");
        m.put("д","d"); m.put("е","e"); m.put("ё","yo");m.put("ж","zh");
        m.put("з","z"); m.put("и","i"); m.put("й","y"); m.put("к","k");
        m.put("л","l"); m.put("м","m"); m.put("н","n"); m.put("о","o");
        m.put("п","p"); m.put("р","r"); m.put("с","s"); m.put("т","t");
        m.put("у","u"); m.put("ф","f"); m.put("х","h"); m.put("ц","ts");
        m.put("ч","ch");m.put("ш","sh");m.put("щ","sch");m.put("ъ","");
        m.put("ы","y"); m.put("ь",""); m.put("э","e"); m.put("ю","yu");
        m.put("я","ya");
        m.put("А","a"); m.put("Б","b"); m.put("В","v"); m.put("Г","g");
        m.put("Д","d"); m.put("Е","e"); m.put("Ё","yo");m.put("Ж","zh");
        m.put("З","z"); m.put("И","i"); m.put("Й","y"); m.put("К","k");
        m.put("Л","l"); m.put("М","m"); m.put("Н","n"); m.put("О","o");
        m.put("П","p"); m.put("Р","r"); m.put("С","s"); m.put("Т","t");
        m.put("У","u"); m.put("Ф","f"); m.put("Х","h"); m.put("Ц","ts");
        m.put("Ч","ch");m.put("Ш","sh");m.put("Щ","sch");m.put("Ъ","");
        m.put("Ы","y"); m.put("Ь",""); m.put("Э","e"); m.put("Ю","yu");
        m.put("Я","ya");
        return Collections.unmodifiableMap(m);
    }
}
