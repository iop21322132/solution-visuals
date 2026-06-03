package farvix.solution.api.render.font;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import farvix.solution.Client;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Fonts {

    @SneakyThrows
    public static FontRenderer create(float size, String name) {
        boolean fx = name.endsWith(".ttf");
        String path = "assets/solution/fonts/" + (fx ? (name.replace(".ttf", "") + ".ttf") : (name + ".otf"));

        try (InputStream inputStream = Client.class.getClassLoader().getResourceAsStream(path)) {
            int style = Font.PLAIN;
            Font font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(inputStream))
                    .deriveFont(style, size / 2f);

            return new FontRenderer(font, size / 2f);
        }
    }

    private static final Map<FontKey, FontRenderer> fontCache = new HashMap<>();

    public static void init() {
        for (FontType type : FontType.values()) {
            for (int size = 4; size <= 32; size++) {
                fontCache.put(new FontKey(size, type), create(size, type.getFontName()));
            }
        }
    }

    private static FontRenderer getFont(int size, FontType type) {
        return fontCache.computeIfAbsent(new FontKey(size, type), k -> create(size, type.getFontName()));
    }

    public static final FontAccessor DEFAULT = new FontAccessor(FontType.DEFAULT);
    public static final FontAccessor SEMIBOLD = new FontAccessor(FontType.SEMIBOLD);
    public static final FontAccessor ICONS = new FontAccessor(FontType.ICONS);

    @Getter
    @RequiredArgsConstructor
    public enum FontType {
        DEFAULT("sfpromedium"),
        SEMIBOLD("sfprosemibold"),
        ICONS("icomoon.ttf");

        private final String fontName;
    }

    private record FontKey(int size, FontType type) {
    }

    public static class FontAccessor {
        private final FontType type;

        private FontAccessor(FontType type) {
            this.type = type;
        }

        public FontRenderer get(int size) {
            return Fonts.getFont(size, type);
        }
    }
}
