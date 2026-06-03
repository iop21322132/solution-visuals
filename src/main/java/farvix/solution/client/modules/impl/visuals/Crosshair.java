package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.util.hit.HitResult;
import org.joml.Matrix4f;

/**
 * Custom Crosshair — аналог Custom Crosshair Mod.
 * Типы: Classic, Circle, Dot, Cross+Dot, T-Shape, X-Shape, Square, Dynamic
 * Опции: цвет, outline, rainbow, adaptive color, dynamic spread, gap, size, thickness
 */
@ModuleInfo(name = "Crosshair", category = ModuleCategory.VISUALS,
        description = "Кастомный прицел")
public class Crosshair extends Module implements QuickImports {

    // ── Shape ─────────────────────────────────────────────────────────────────
    public final ModeSetting shape = new ModeSetting("Форма", this,
            "Classic");

    // ── Size / geometry ───────────────────────────────────────────────────────
    public final SliderSetting size = new SliderSetting(
            "Размер", this, 5f, 1f, 20f, 0.5f);

    public final SliderSetting crosshairWidth = new SliderSetting(
            "Ширина", this, 5f, 1f, 20f, 0.5f);

    public final SliderSetting crosshairHeight = new SliderSetting(
            "Высота", this, 5f, 1f, 20f, 0.5f);

    public final SliderSetting thickness = new SliderSetting(
            "Толщина", this, 1.5f, 0.5f, 5f, 0.1f);

    public final SliderSetting gap = new SliderSetting(
            "Зазор", this, 3f, 0f, 10f, 0.5f);

    // ── Outline ───────────────────────────────────────────────────────────────
    public final BooleanSetting outline = new BooleanSetting("Обводка", this);

    public final SliderSetting outlineThickness = new SliderSetting(
            "Толщина обводки", this, 1f, 0.5f, 3f, 0.1f);

    public final ColorSetting outlineColor = new ColorSetting("Цвет обводки", this,
            new FixColor(0, 0, 0, 200).getRGB());

    // ── Color ─────────────────────────────────────────────────────────────────
    public final ModeSetting colorMode = new ModeSetting("Режим цвета", this,
            "Обычный", "Радуга", "Адаптивный");

    public final ColorSetting color = new ColorSetting("Цвет", this,
            new FixColor(255, 255, 255, 220).getRGB());

    // ── Dynamic ───────────────────────────────────────────────────────────────
    public final BooleanSetting dynamic = new BooleanSetting("Динамический", this);

    public final SliderSetting dynamicMultiplier = new SliderSetting(
            "Сила динамики", this, 3f, 1f, 8f, 0.5f);

    // ── State ─────────────────────────────────────────────────────────────────
    private float rainbowHue = 0f;
    private float dynamicSpread = 0f;
    private float adaptiveAnim  = 0f;

    public Crosshair() {
        outlineThickness.setVisible(() -> outline.isEnabled());
        outlineColor.setVisible(() -> outline.isEnabled());
        dynamicMultiplier.setVisible(() -> dynamic.isEnabled());
        colorMode.setCurrentMode("Обычный");
        // size скрыт — используем отдельные ширину и высоту
        size.setVisible(() -> false);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;

        DrawContext ctx = e.getContext();
        float cx = mc.getWindow().getScaledWidth()  / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;

        // ── Rainbow hue ───────────────────────────────────────────────────────
        rainbowHue = (rainbowHue + 0.01f) % 1f;

        // ── Dynamic spread ────────────────────────────────────────────────────
        float targetSpread = 0f;
        if (dynamic.isEnabled()) {
            float cooldown = 1f - mc.player.getAttackCooldownProgress(1f);
            targetSpread = cooldown * dynamicMultiplier.getValue();
        }
        dynamicSpread = lerp(dynamicSpread, targetSpread, 0.2f);

        // ── Adaptive color animation ──────────────────────────────────────────
        boolean onEntity = mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.ENTITY;
        adaptiveAnim = lerp(adaptiveAnim, onEntity ? 1f : 0f, 0.15f);

        // ── Resolve color ─────────────────────────────────────────────────────
        int col = resolveColor();

        // ── Draw ──────────────────────────────────────────────────────────────
        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        float sz  = size.getValue();
        float szW = crosshairWidth.getValue();
        float szH = crosshairHeight.getValue();
        float th  = thickness.getValue();
        float gp  = gap.getValue() + dynamicSpread;

        switch (shape.getCurrentMode()) {
            case "Classic" -> drawClassic(mat, cx, cy, szW, szH, th, gp, col);
            case "Circle"  -> drawCircle(mat, cx, cy, (szW + szH) / 2f, th, col);
        }
    }

    // ── Color resolver ────────────────────────────────────────────────────────

    private int resolveColor() {
        return switch (colorMode.getCurrentMode()) {
            case "Радуга" -> java.awt.Color.HSBtoRGB(rainbowHue, 1f, 1f) | 0xFF000000;
            case "Адаптивный" -> {
                int base  = color.get();
                int red   = new FixColor(255, 60, 60, alpha(base)).getRGB();
                yield interpolateColor(base, red, adaptiveAnim);
            }
            default -> color.get();
        };
    }

    // ── Classic crosshair (+) ─────────────────────────────────────────────────

    private void drawClassic(Matrix4f mat, float cx, float cy,
                              float szW, float szH, float th, float gp, int col) {
        float half = th / 2f;
        // Outline first
        if (outline.isEnabled()) {
            float ot = outlineThickness.getValue();
            int oc = outlineColor.get();
            // top
            fillRect(mat, cx - half - ot, cy - gp - szH - ot, th + ot*2, szH + ot*2, oc);
            // bottom
            fillRect(mat, cx - half - ot, cy + gp - ot,        th + ot*2, szH + ot*2, oc);
            // left
            fillRect(mat, cx - gp - szW - ot, cy - half - ot, szW + ot*2, th + ot*2, oc);
            // right
            fillRect(mat, cx + gp - ot,        cy - half - ot, szW + ot*2, th + ot*2, oc);
        }
        // Fill — вертикальные линии используют высоту, горизонтальные — ширину
        fillRect(mat, cx - half, cy - gp - szH, th, szH, col); // top
        fillRect(mat, cx - half, cy + gp,        th, szH, col); // bottom
        fillRect(mat, cx - gp - szW, cy - half, szW, th, col); // left
        fillRect(mat, cx + gp,        cy - half, szW, th, col); // right
    }

    // ── Circle ────────────────────────────────────────────────────────────────

    private void drawCircle(Matrix4f mat, float cx, float cy, float r, float th, int col) {
        if (outline.isEnabled()) {
            float ot = outlineThickness.getValue();
            drawRing(mat, cx, cy, r + ot, th + ot * 2, outlineColor.get());
        }
        drawRing(mat, cx, cy, r, th, col);
    }

    private void drawRing(Matrix4f mat, float cx, float cy, float r, float th, int col) {
        int segments = 64;
        float outerR = r + th / 2f;
        float innerR = Math.max(0, r - th / 2f);
        float[] c = rgba(col);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i       / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;

            float x0o = cx + (float)(Math.cos(a0) * outerR);
            float y0o = cy + (float)(Math.sin(a0) * outerR);
            float x1o = cx + (float)(Math.cos(a1) * outerR);
            float y1o = cy + (float)(Math.sin(a1) * outerR);
            float x0i = cx + (float)(Math.cos(a0) * innerR);
            float y0i = cy + (float)(Math.sin(a0) * innerR);
            float x1i = cx + (float)(Math.cos(a1) * innerR);
            float y1i = cy + (float)(Math.sin(a1) * innerR);

            buf.vertex(mat, x0i, y0i, 0).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, x0o, y0o, 0).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, x1o, y1o, 0).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, x1i, y1i, 0).color(c[0], c[1], c[2], c[3]);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ── Dot ───────────────────────────────────────────────────────────────────

    private void drawDot(Matrix4f mat, float cx, float cy, float r, int col) {
        if (outline.isEnabled()) {
            float ot = outlineThickness.getValue();
            drawFilledCircle(mat, cx, cy, r + ot, outlineColor.get());
        }
        drawFilledCircle(mat, cx, cy, r, col);
    }

    private void drawFilledCircle(Matrix4f mat, float cx, float cy, float r, int col) {
        int segments = 48;
        float[] c = rgba(col);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i       / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            // Triangle: center, edge0, edge1
            buf.vertex(mat, cx, cy, 0).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, cx + (float)(Math.cos(a0) * r), cy + (float)(Math.sin(a0) * r), 0).color(c[0], c[1], c[2], c[3]);
            buf.vertex(mat, cx + (float)(Math.cos(a1) * r), cy + (float)(Math.sin(a1) * r), 0).color(c[0], c[1], c[2], c[3]);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ── T-Shape (top + left + right, no bottom) ───────────────────────────────

    private void drawTShape(Matrix4f mat, float cx, float cy,
                             float sz, float th, float gp, int col) {
        float half = th / 2f;
        if (outline.isEnabled()) {
            float ot = outlineThickness.getValue();
            int oc = outlineColor.get();
            fillRect(mat, cx - half - ot, cy - gp - sz - ot, th + ot*2, sz + ot*2, oc);
            fillRect(mat, cx - gp - sz - ot, cy - half - ot, sz + ot*2, th + ot*2, oc);
            fillRect(mat, cx + gp - ot,       cy - half - ot, sz + ot*2, th + ot*2, oc);
        }
        fillRect(mat, cx - half, cy - gp - sz, th, sz, col); // top
        fillRect(mat, cx - gp - sz, cy - half, sz, th, col); // left
        fillRect(mat, cx + gp,       cy - half, sz, th, col); // right
    }

    // ── X-Shape (diagonal) ────────────────────────────────────────────────────

    private void drawXShape(Matrix4f mat, float cx, float cy,
                             float sz, float th, int col) {
        if (outline.isEnabled()) {
            float ot = outlineThickness.getValue();
            int oc = outlineColor.get();
            drawLine(mat, cx - sz - ot, cy - sz - ot, cx + sz + ot, cy + sz + ot, th + ot*2, oc);
            drawLine(mat, cx + sz + ot, cy - sz - ot, cx - sz - ot, cy + sz + ot, th + ot*2, oc);
        }
        drawLine(mat, cx - sz, cy - sz, cx + sz, cy + sz, th, col);
        drawLine(mat, cx + sz, cy - sz, cx - sz, cy + sz, th, col);
    }

    // ── Square ────────────────────────────────────────────────────────────────

    private void drawSquare(Matrix4f mat, float cx, float cy,
                             float sz, float th, int col) {
        if (outline.isEnabled()) {
            float ot = outlineThickness.getValue();
            int oc = outlineColor.get();
            // outer square
            fillRect(mat, cx - sz - ot, cy - sz - ot, (sz + ot)*2, th + ot*2, oc); // top
            fillRect(mat, cx - sz - ot, cy + sz - ot, (sz + ot)*2, th + ot*2, oc); // bottom
            fillRect(mat, cx - sz - ot, cy - sz - ot, th + ot*2, (sz + ot)*2, oc); // left
            fillRect(mat, cx + sz - ot, cy - sz - ot, th + ot*2, (sz + ot)*2, oc); // right
        }
        fillRect(mat, cx - sz, cy - sz, sz*2, th, col); // top
        fillRect(mat, cx - sz, cy + sz, sz*2, th, col); // bottom
        fillRect(mat, cx - sz, cy - sz, th, sz*2, col); // left
        fillRect(mat, cx + sz, cy - sz, th, sz*2, col); // right
    }

    // ── Primitives ────────────────────────────────────────────────────────────

    private void fillRect(Matrix4f mat, float x, float y, float w, float h, int col) {
        float[] c = rgba(col);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x,     y,     0).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x,     y + h, 0).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x + w, y + h, 0).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x + w, y,     0).color(c[0], c[1], c[2], c[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void drawLine(Matrix4f mat, float x1, float y1, float x2, float y2,
                           float th, int col) {
        float[] c = rgba(col);
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        if (len == 0) return;
        float nx = -dy / len * th / 2f;
        float ny =  dx / len * th / 2f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x1 + nx, y1 + ny, 0).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x1 - nx, y1 - ny, 0).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2 - nx, y2 - ny, 0).color(c[0], c[1], c[2], c[3]);
        buf.vertex(mat, x2 + nx, y2 + ny, 0).color(c[0], c[1], c[2], c[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private int alpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    private float[] rgba(int argb) {
        return new float[]{
            ((argb >> 16) & 0xFF) / 255f,
            ((argb >> 8)  & 0xFF) / 255f,
            (argb         & 0xFF) / 255f,
            ((argb >> 24) & 0xFF) / 255f
        };
    }

    private int interpolateColor(int c1, int c2, float t) {
        int a1=(c1>>24)&0xFF, r1=(c1>>16)&0xFF, g1=(c1>>8)&0xFF, b1=c1&0xFF;
        int a2=(c2>>24)&0xFF, r2=(c2>>16)&0xFF, g2=(c2>>8)&0xFF, b2=c2&0xFF;
        return ((int)(a1+(a2-a1)*t)<<24)|((int)(r1+(r2-r1)*t)<<16)
              |((int)(g1+(g2-g1)*t)<<8)|(int)(b1+(b2-b1)*t);
    }
}
