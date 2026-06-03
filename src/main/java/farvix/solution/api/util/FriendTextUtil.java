package farvix.solution.api.util;

import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class FriendTextUtil {

    public static final ThreadLocal<Entity> CURRENT_ENTITY = new ThreadLocal<>();

    public static void setCurrentEntity(Entity entity) {
        CURRENT_ENTITY.set(entity);
    }

    public static Entity getCurrentEntity() {
        return CURRENT_ENTITY.get();
    }

    public static MutableText recolorName(Text text, String name) {
        MutableText result = Text.empty();
        text.visit((style, str) -> {
            if (str.contains(name)) {
                int idx = str.indexOf(name);
                if (idx > 0) result.append(Text.literal(str.substring(0, idx)).setStyle(style));
                Style friendStyle = style.withColor(TextColor.fromRgb(0x55FF55)).withBold(true);
                result.append(Text.literal(name).setStyle(friendStyle));
                if (idx + name.length() < str.length())
                    result.append(Text.literal(str.substring(idx + name.length())).setStyle(style));
            } else {
                result.append(Text.literal(str).setStyle(style));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
