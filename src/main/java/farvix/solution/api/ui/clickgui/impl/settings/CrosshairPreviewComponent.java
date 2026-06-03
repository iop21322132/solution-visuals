package farvix.solution.api.ui.clickgui.impl.settings;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import farvix.solution.api.TempColor;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.DummySetting;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.ui.clickgui.api.SettingComponent;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.impl.visuals.Crosshair;

/**
 * Превью прицела в настройках модуля Crosshair.
 * Рендерит текущий прицел в маленьком тёмном окошке.
 */
public class CrosshairPreviewComponent extends SettingComponent {

    private final Crosshair crosshair;
    private float rainbowHue = 0f;

    public CrosshairPreviewComponent(Crosshair crosshair, ModuleComponent moduleComponent) {
        super(new DummySetting("crosshair.preview", null), moduleComponent);
        this.crosshair = crosshair;
    }

    @Override
    public void init() {
        this.width  = moduleComponent.getWidth();
        this.height = 60f; // высота превью-блока
    }

    @Override
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        super.render(context, x, y, mouseX, mouseY, delta);

        float alpha = getClickGUI().getAlpha().getValue();
        float pad   = 5f;

        // Расширяем превью на 1 пиксель влево и вправо
        float previewX = x - 1;
        float previewWidth = width + 2;
        float previewHeight = height - 4;

        // ── Фон превью (черный полупрозрачный) ────────────────────────────────
        int blackBgColor = new FixColor(0, 0, 0, (int)(120 * alpha)).getRGB(); // Черный с прозрачностью
        
        rectangle.render(ShapeProperties.create(context.getMatrices(),
                previewX, y, previewWidth, previewHeight)
                .round(0f)
                .color(blackBgColor)
                .build());
        
        // Обводка превью
        blur.render(ShapeProperties.create(context.getMatrices(),
                previewX, y, previewWidth, previewHeight)
                .round(0f)
                .softness(1f).thickness(1.5f)
                .outlineColor(TempColor.getClientColor().alpha(alpha * 0.4f).getRGB())
                .color(0)
                .build());

        // Метка "Превью"
        Fonts.DEFAULT.get(14).drawBoldString(context.getMatrices(), "Превью",
                x + 4, y + 4f,
                TempColor.getTextSecondary().alpha(alpha * 0.7f).getRGB());

        // ── Центр превью ──────────────────────────────────────────────────────
        float cx = x + width / 2f;
        float cy = y + (height - 4) / 2f + 3f;

        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        // Разрешаем цвет
        rainbowHue = (rainbowHue + 0.008f) % 1f;
        int col = resolveColor(alpha);

        float szW = crosshair.crosshairWidth.getValue();
        float szH = crosshair.crosshairHeight.getValue();
        float th = crosshair.thickness.getValue();
        float gp = crosshair.gap.getValue();

        // Масштабируем чтобы влезло в превью (макс размер ~25px)
        float maxExtent = Math.max(szW, szH) + gp + th;
        float scale = maxExtent > 22f ? 22f / maxExtent : 1f;
        szW *= scale; szH *= scale; th *= scale; gp *= scale;

        switch (crosshair.shape.getCurrentMode()) {
            case "Classic" -> drawClassic(mat, cx, cy, szW, szH, th, gp, col, alpha);
            case "Circle"  -> drawCircle(mat, cx, cy, (szW + szH) / 2f, th, col, alpha);
        }
    }

    // ── Color ─────────────────────────────────────────────────────────────────

    private int resolveColor(float alpha) {
        int base = switch (crosshair.colorMode.getCurrentMode()) {
            case "Радуга"     -> java.awt.Color.HSBtoRGB(rainbowHue, 1f, 1f) | 0xFF000000;
            case "Адаптивный" -> crosshair.color.get(); // в превью без адаптации
            default           -> crosshair.color.get();
        };
        // Применяем alpha GUI
        float[] c = rgba(base);
        return new FixColor(c[0]*255, c[1]*255, c[2]*255, c[3]*255*alpha).getRGB();
    }

    // ── Shapes (копия из Crosshair.java, но с alpha GUI) ──────────────────────

    private void drawClassic(Matrix4f mat, float cx, float cy,
                              float szW, float szH, float th, float gp, int col, float alpha) {
        float half = th / 2f;
        if (crosshair.outline.isEnabled()) {
            float ot = crosshair.outlineThickness.getValue();
            int oc = applyAlpha(crosshair.outlineColor.get(), alpha);
            fillRect(mat, cx - half - ot, cy - gp - szH - ot, th + ot*2, szH + ot*2, oc);
            fillRect(mat, cx - half - ot, cy + gp - ot,        th + ot*2, szH + ot*2, oc);
            fillRect(mat, cx - gp - szW - ot, cy - half - ot, szW + ot*2, th + ot*2, oc);
            fillRect(mat, cx + gp - ot,        cy - half - ot, szW + ot*2, th + ot*2, oc);
        }
        fillRect(mat, cx - half, cy - gp - szH, th, szH, col);
        fillRect(mat, cx - half, cy + gp,        th, szH, col);
        fillRect(mat, cx - gp - szW, cy - half, szW, th, col);
        fillRect(mat, cx + gp,        cy - half, szW, th, col);
    }

    private void drawCircle(Matrix4f mat, float cx, float cy, float r, float th, int col, float alpha) {
        if (crosshair.outline.isEnabled()) {
            float ot = crosshair.outlineThickness.getValue();
            drawRing(mat, cx, cy, r + ot, th + ot*2, applyAlpha(crosshair.outlineColor.get(), alpha));
        }
        drawRing(mat, cx, cy, r, th, col);
    }

    private void drawRing(Matrix4f mat, float cx, float cy, float r, float th, int col) {
        int segments = 48;
        float outerR = r + th / 2f;
        float innerR = Math.max(0, r - th / 2f);
        float[] c = rgba(col);

        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i+1) / segments;
            float x0o = cx+(float)(Math.cos(a0)*outerR), y0o = cy+(float)(Math.sin(a0)*outerR);
            float x1o = cx+(float)(Math.cos(a1)*outerR), y1o = cy+(float)(Math.sin(a1)*outerR);
            float x0i = cx+(float)(Math.cos(a0)*innerR), y0i = cy+(float)(Math.sin(a0)*innerR);
            float x1i = cx+(float)(Math.cos(a1)*innerR), y1i = cy+(float)(Math.sin(a1)*innerR);
            buf.vertex(mat,x0i,y0i,0).color(c[0],c[1],c[2],c[3]);
            buf.vertex(mat,x0o,y0o,0).color(c[0],c[1],c[2],c[3]);
            buf.vertex(mat,x1o,y1o,0).color(c[0],c[1],c[2],c[3]);
            buf.vertex(mat,x1i,y1i,0).color(c[0],c[1],c[2],c[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest(); RenderSystem.disableBlend();
    }

    private void drawDot(Matrix4f mat, float cx, float cy, float r, int col, float alpha) {
        if (crosshair.outline.isEnabled()) {
            float ot = crosshair.outlineThickness.getValue();
            drawFilledCircle(mat, cx, cy, r + ot, applyAlpha(crosshair.outlineColor.get(), alpha));
        }
        drawFilledCircle(mat, cx, cy, r, col);
    }

    private void drawFilledCircle(Matrix4f mat, float cx, float cy, float r, int col) {
        int segments = 48;
        float[] c = rgba(col);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i       / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            buf.vertex(mat, cx, cy, 0).color(c[0],c[1],c[2],c[3]);
            buf.vertex(mat, cx+(float)(Math.cos(a0)*r), cy+(float)(Math.sin(a0)*r), 0).color(c[0],c[1],c[2],c[3]);
            buf.vertex(mat, cx+(float)(Math.cos(a1)*r), cy+(float)(Math.sin(a1)*r), 0).color(c[0],c[1],c[2],c[3]);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest(); RenderSystem.disableBlend();
    }

    private void drawTShape(Matrix4f mat, float cx, float cy,
                             float sz, float th, float gp, int col, float alpha) {
        float half = th / 2f;
        if (crosshair.outline.isEnabled()) {
            float ot = crosshair.outlineThickness.getValue();
            int oc = applyAlpha(crosshair.outlineColor.get(), alpha);
            fillRect(mat, cx-half-ot, cy-gp-sz-ot, th+ot*2, sz+ot*2, oc);
            fillRect(mat, cx-gp-sz-ot, cy-half-ot, sz+ot*2, th+ot*2, oc);
            fillRect(mat, cx+gp-ot,    cy-half-ot, sz+ot*2, th+ot*2, oc);
        }
        fillRect(mat, cx-half, cy-gp-sz, th, sz, col);
        fillRect(mat, cx-gp-sz, cy-half, sz, th, col);
        fillRect(mat, cx+gp,    cy-half, sz, th, col);
    }

    private void drawXShape(Matrix4f mat, float cx, float cy,
                             float sz, float th, int col, float alpha) {
        if (crosshair.outline.isEnabled()) {
            float ot = crosshair.outlineThickness.getValue();
            int oc = applyAlpha(crosshair.outlineColor.get(), alpha);
            drawLine(mat, cx-sz-ot, cy-sz-ot, cx+sz+ot, cy+sz+ot, th+ot*2, oc);
            drawLine(mat, cx+sz+ot, cy-sz-ot, cx-sz-ot, cy+sz+ot, th+ot*2, oc);
        }
        drawLine(mat, cx-sz, cy-sz, cx+sz, cy+sz, th, col);
        drawLine(mat, cx+sz, cy-sz, cx-sz, cy+sz, th, col);
    }

    private void drawSquare(Matrix4f mat, float cx, float cy,
                             float sz, float th, int col, float alpha) {
        if (crosshair.outline.isEnabled()) {
            float ot = crosshair.outlineThickness.getValue();
            int oc = applyAlpha(crosshair.outlineColor.get(), alpha);
            fillRect(mat, cx-sz-ot, cy-sz-ot, (sz+ot)*2, th+ot*2, oc);
            fillRect(mat, cx-sz-ot, cy+sz-ot, (sz+ot)*2, th+ot*2, oc);
            fillRect(mat, cx-sz-ot, cy-sz-ot, th+ot*2, (sz+ot)*2, oc);
            fillRect(mat, cx+sz-ot, cy-sz-ot, th+ot*2, (sz+ot)*2, oc);
        }
        fillRect(mat, cx-sz, cy-sz, sz*2, th, col);
        fillRect(mat, cx-sz, cy+sz, sz*2, th, col);
        fillRect(mat, cx-sz, cy-sz, th, sz*2, col);
        fillRect(mat, cx+sz, cy-sz, th, sz*2, col);
    }

    // ── Primitives ────────────────────────────────────────────────────────────

    private void fillRect(Matrix4f mat, float x, float y, float w, float h, int col) {
        float[] c = rgba(col);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x,   y,   0).color(c[0],c[1],c[2],c[3]);
        buf.vertex(mat, x,   y+h, 0).color(c[0],c[1],c[2],c[3]);
        buf.vertex(mat, x+w, y+h, 0).color(c[0],c[1],c[2],c[3]);
        buf.vertex(mat, x+w, y,   0).color(c[0],c[1],c[2],c[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest(); RenderSystem.disableBlend();
    }

    private void drawLine(Matrix4f mat, float x1, float y1, float x2, float y2, float th, int col) {
        float[] c = rgba(col);
        float dx = x2-x1, dy = y2-y1;
        float len = (float)Math.sqrt(dx*dx+dy*dy);
        if (len == 0) return;
        float nx = -dy/len*th/2f, ny = dx/len*th/2f;
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x1+nx, y1+ny, 0).color(c[0],c[1],c[2],c[3]);
        buf.vertex(mat, x1-nx, y1-ny, 0).color(c[0],c[1],c[2],c[3]);
        buf.vertex(mat, x2-nx, y2-ny, 0).color(c[0],c[1],c[2],c[3]);
        buf.vertex(mat, x2+nx, y2+ny, 0).color(c[0],c[1],c[2],c[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest(); RenderSystem.disableBlend();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float[] rgba(int argb) {
        return new float[]{
            ((argb>>16)&0xFF)/255f,
            ((argb>>8) &0xFF)/255f,
            (argb      &0xFF)/255f,
            ((argb>>24)&0xFF)/255f
        };
    }

    private int applyAlpha(int argb, float alpha) {
        int a = (int)(((argb>>24)&0xFF) * alpha);
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}
