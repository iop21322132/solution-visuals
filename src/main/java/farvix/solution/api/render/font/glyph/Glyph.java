package farvix.solution.api.render.font.glyph;

public record Glyph(int u, int v, int width, int height, char value, GlyphMap owner) {
}
