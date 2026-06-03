package farvix.solution.api.render.font.entry;

import farvix.solution.api.render.font.glyph.Glyph;

public record DrawEntry(float atX, float atY, int color, Glyph toDraw) {
}
