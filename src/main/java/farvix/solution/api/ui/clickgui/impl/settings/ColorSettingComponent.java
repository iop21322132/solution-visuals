package farvix.solution.api.ui.clickgui.impl.settings;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.render.rect.impl.ColorWheelRenderer;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.ui.clickgui.api.SettingComponent;
import farvix.solution.api.util.color.FixColor;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

/**
 * Color setting component with collapsible circular HSB picker.
 *
 * Layout (collapsed):
 *   Row 1: [color swatch] "Name" [Color button]
 *
 * Layout (expanded):
 *   Row 1: [color swatch] "Name" [Color button]
 *   Row 2: [HSB wheel] [brightness bar] [alpha bar]
 *   Row 3: hex label
 */
@Getter
public class ColorSettingComponent extends SettingComponent implements QuickImports {

    private ColorSetting colorSetting;

    // Layout
    private static final float HEADER_H = 18f;
    private static final float PADDING  = 5f;
    private static final float WHEEL_D  = 80f;
    private static final float SLIDER_H = 8f;
    private static final float CURSOR_R = 4f;
    private static final float SWATCH   = 10f;

    // HSB + alpha
    private float hue = 0f, sat = 1f, bri = 1f, alphaVal = 1f;

    // Drag
    private boolean draggingWheel = false;
    private boolean draggingBri   = false;
    private boolean draggingAlpha = false;

    // Toggle state
    private boolean expanded = false;

    // Manual HEX input
    private boolean editingHex = false;
    private String hexInput = "";

    public ColorSettingComponent(ColorSetting setting, ModuleComponent moduleComponent) {
        super(setting, moduleComponent);
        this.colorSetting = setting;
        syncFromColor();
    }

    @Override
    public void init() {
        this.colorSetting = (ColorSetting) getSetting();
        this.width  = moduleComponent.getWidth();
        syncFromColor();
        updateHeight();
        super.init();
    }

    private void updateHeight() {
        if (expanded) {
            float w = width > 0 ? width : (moduleComponent != null ? moduleComponent.getWidth() : 90f);
            float wheelW = w - PADDING * 2;
            float wheelH = wheelW / 3f;
            this.height = HEADER_H + PADDING + wheelH + PADDING + SLIDER_H + PADDING + SLIDER_H + PADDING + 10f + PADDING + 15f;
        } else {
            this.height = 13f;
        }
    }

    @Override
    public float getHeight() {
        updateHeight();
        return this.height;
    }

    private void syncFromColor() {
        Color c = new Color(colorSetting.get(), true);
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        if (c.getRed() != c.getGreen() || c.getGreen() != c.getBlue()) {
            hue  = hsb[0];
        }
        sat      = hsb[1];
        bri      = hsb[2];
        alphaVal = c.getAlpha() / 255f;
    }

    private void applyToColor() {
        int rgb = Color.HSBtoRGB(hue, sat, bri);
        int a   = Math.round(alphaVal * 255f);
        colorSetting.setColor((rgb & 0x00FFFFFF) | (a << 24));
        colorSetting.setOverridden(true);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        super.render(context, x, y, mouseX, mouseY, delta);
        float ga = getClickGUI().getAlpha().getValue();

        updateHeight();

        // ── Header: swatch (left) + name ─────────────────────────────────────
        float swatchX = x + 10f;
        float swatchY = expanded ? y + (HEADER_H - SWATCH) / 2f : y + (13f - SWATCH) / 2f;
        Color cur = new Color(colorSetting.get(), true);
        
        // Swatch with hover effect
        boolean swatchHovered = mouseX >= swatchX && mouseX <= swatchX + SWATCH &&
                                mouseY >= swatchY && mouseY <= swatchY + SWATCH;
        
        int swatchBorder = swatchHovered || expanded
                ? TempColor.getClientColor().alpha((int)(200 * ga)).getRGB()
                : TempColor.getModuleBorder().alpha((int)(150 * ga)).getRGB();
        
        blur.render(ShapeProperties.create(context.getMatrices(), swatchX, swatchY, SWATCH, SWATCH)
                .round(3).softness(1).thickness(swatchHovered || expanded ? 2f : 1.5f)
                .outlineColor(swatchBorder)
                .color(new FixColor(cur.getRed(), cur.getGreen(), cur.getBlue(),
                        (int)(cur.getAlpha() * ga)).getRGB())
                .build());

        String name = getTranslatedName();
        float nameY = expanded ? y + 6f : y + 5.5f;
        Fonts.DEFAULT.get(15).drawBoldString(context.getMatrices(), name,
                swatchX + SWATCH + 4, nameY,
                TempColor.getTextPrimary().alpha(ga).getRGB());

        // ── Picker area (only if expanded) ─────────────────────────────────────
        if (expanded) {
            float wheelW = width - PADDING * 2;
            float wheelH = wheelW / 3f;
            float wheelX = x + PADDING;
            float wheelY = y + HEADER_H + PADDING;

            // HSB square via shader (we pass hue as the "brightness" uniform)
            ColorWheelRenderer.draw(context.getMatrices(), wheelX, wheelY, wheelW, wheelH, hue, ga);

            // Wheel border (rounded outline)
            blur.render(ShapeProperties.create(context.getMatrices(), wheelX, wheelY, wheelW, wheelH)
                    .round(4f).softness(1.5f).thickness(1.5f)
                    .outlineColor(TempColor.getModuleBorder().alpha(ga).getRGB())
                    .color(0).build());

            // Cursor on square (Saturation is X, Brightness is 1.0 - Y)
            float curX = wheelX + sat * wheelW;
            float curY = wheelY + (1f - bri) * wheelH;
            
            // Cursor fill color matches the HSB color with selected transparency
            Color fc = new Color(Color.HSBtoRGB(hue, sat, bri));
            int cursorFillColor = new FixColor(fc.getRed(), fc.getGreen(), fc.getBlue(), (int)(alphaVal * 255f * ga)).getRGB();

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, ga);
            blur.render(ShapeProperties.create(context.getMatrices(), curX - CURSOR_R, curY - CURSOR_R, CURSOR_R * 2, CURSOR_R * 2)
                    .round(CURSOR_R).softness(1).thickness(2f)
                    .outlineColor(new FixColor(255, 255, 255, (int)(220 * ga)).getRGB())
                    .color(cursorFillColor)
                    .build());
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            // Hue slider (Rainbow)
            float hueSliderX = wheelX;
            float hueSliderY = wheelY + wheelH + PADDING;
            float segmentW = wheelW / 6.0f;
            for (int i = 0; i < 6; i++) {
                float segX = hueSliderX + i * segmentW;
                float hLeft = i / 6.0f;
                float hRight = (i + 1) / 6.0f;
                int colorA = Color.HSBtoRGB(hLeft, 1f, 1f);
                int colorB = Color.HSBtoRGB(hRight, 1f, 1f);
                int cA = new FixColor((colorA >> 16) & 0xFF, (colorA >> 8) & 0xFF, colorA & 0xFF, (int)(255 * ga)).getRGB();
                int cB = new FixColor((colorB >> 16) & 0xFF, (colorB >> 8) & 0xFF, colorB & 0xFF, (int)(255 * ga)).getRGB();
                
                org.joml.Vector4f roundVec = new org.joml.Vector4f(0f);
                if (i == 0) {
                    roundVec = new org.joml.Vector4f(0f, 0f, SLIDER_H / 2f, SLIDER_H / 2f); // Rounds left corners
                } else if (i == 5) {
                    roundVec = new org.joml.Vector4f(SLIDER_H / 2f, SLIDER_H / 2f, 0f, 0f); // Rounds right corners
                }
                blur.render(ShapeProperties.create(context.getMatrices(), segX, hueSliderY, segmentW, SLIDER_H)
                        .round(roundVec).softness(0).thickness(0)
                        .color(new org.joml.Vector4i(cA, cA, cB, cB))
                        .outlineColor(0).build());
            }

            // Hue outline
            blur.render(ShapeProperties.create(context.getMatrices(), hueSliderX, hueSliderY, wheelW, SLIDER_H)
                    .round(SLIDER_H / 2f).softness(1.5f).thickness(1.5f)
                    .outlineColor(TempColor.getModuleBorder().alpha(ga).getRGB())
                    .color(0).build());

            // Hue cursor handle
            float cursorHueX = hueSliderX + hue * wheelW;
            blur.render(ShapeProperties.create(context.getMatrices(), cursorHueX - CURSOR_R, hueSliderY - 2, CURSOR_R * 2, SLIDER_H + 4)
                    .round(CURSOR_R).softness(1.5f).thickness(2f)
                    .outlineColor(new FixColor(255, 255, 255, (int)(220 * ga)).getRGB())
                    .color(0)
                    .build());

            // Alpha slider
            float alphaSliderX = wheelX;
            float alphaSliderY = hueSliderY + SLIDER_H + PADDING;
            int cAlphaLeft = new FixColor(fc.getRed(), fc.getGreen(), fc.getBlue(), 0).getRGB();
            int cAlphaRight = new FixColor(fc.getRed(), fc.getGreen(), fc.getBlue(), (int)(255 * ga)).getRGB();

            blur.render(ShapeProperties.create(context.getMatrices(), alphaSliderX, alphaSliderY, wheelW, SLIDER_H)
                    .round(SLIDER_H / 2f).softness(1.5f).thickness(0)
                    .color(new org.joml.Vector4i(cAlphaLeft, cAlphaLeft, cAlphaRight, cAlphaRight))
                    .outlineColor(0).build());

            // Alpha outline
            blur.render(ShapeProperties.create(context.getMatrices(), alphaSliderX, alphaSliderY, wheelW, SLIDER_H)
                    .round(SLIDER_H / 2f).softness(1.5f).thickness(1.5f)
                    .outlineColor(TempColor.getModuleBorder().alpha(ga).getRGB())
                    .color(0).build());

            // Alpha cursor handle
            float cursorAlphaX = alphaSliderX + alphaVal * wheelW;
            blur.render(ShapeProperties.create(context.getMatrices(), cursorAlphaX - CURSOR_R, alphaSliderY - 2, CURSOR_R * 2, SLIDER_H + 4)
                    .round(CURSOR_R).softness(1.5f).thickness(2f)
                    .outlineColor(new FixColor(255, 255, 255, (int)(220 * ga)).getRGB())
                    .color(0)
                    .build());

            // Hex label - clickable for manual input
            String currentHex = String.format("#%06X", colorSetting.get() & 0xFFFFFF);
            String displayHex = editingHex ? hexInput + (System.currentTimeMillis() % 1000 < 500 ? "_" : "") : currentHex;
            int labelColor = editingHex ? TempColor.getClientColor().alpha(ga).getRGB() : TempColor.getTextSecondary().alpha(ga).getRGB();

            float labelX = wheelX + wheelW / 2f;
            float labelY = alphaSliderY + SLIDER_H + PADDING + 1f;

            Fonts.DEFAULT.get(13).drawCenteredBoldString(context.getMatrices(), displayHex,
                    labelX, labelY,
                    labelColor);
        }
    }

    private float lerp(float a, float b, float t) { return a + (b - a) * t; }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;

        // ── Check Swatch click ────────────────────────────────────────────────
        float swatchX = x + 10f;
        float swatchY = expanded ? y + (HEADER_H - SWATCH) / 2f : y + (13f - SWATCH) / 2f;
        
        if (mouseX >= swatchX && mouseX <= swatchX + SWATCH &&
            mouseY >= swatchY && mouseY <= swatchY + SWATCH) {
            expanded = !expanded;
            editingHex = false;
            return;
        }

        // ── If not expanded, don't handle picker clicks ────────────────────────
        if (!expanded) {
            editingHex = false;
            return;
        }

        // ── Handle picker interactions (only if expanded) ──────────────────────
        float wheelW = width - PADDING * 2;
        float wheelH = wheelW / 3f;
        float wheelX = x + PADDING;
        float wheelY = y + HEADER_H + PADDING;

        float hueSliderX = wheelX;
        float hueSliderY = wheelY + wheelH + PADDING;
        float alphaSliderX = wheelX;
        float alphaSliderY = hueSliderY + SLIDER_H + PADDING;

        if (inBounds(mouseX, mouseY, wheelX, wheelY, wheelW, wheelH)) {
            draggingWheel = true;
            updateWheel(mouseX, mouseY, wheelX, wheelY);
            return;
        }
        if (inBounds(mouseX, mouseY, hueSliderX - 4, hueSliderY - 2, wheelW + 8, SLIDER_H + 4)) {
            draggingBri = true;
            updateBri(mouseX, hueSliderX);
            return;
        }
        if (inBounds(mouseX, mouseY, alphaSliderX - 4, alphaSliderY - 2, wheelW + 8, SLIDER_H + 4)) {
            draggingAlpha = true;
            updateAlpha(mouseX, alphaSliderX);
            editingHex = false;
            return;
        }

        // Click on Hex label to start editing
        String hex = String.format("#%06X", colorSetting.get() & 0xFFFFFF);
        float labelX = wheelX + wheelW / 2f;
        float labelY = alphaSliderY + SLIDER_H + PADDING + 1f;
        float tw = Fonts.DEFAULT.get(13).getStringWidth(hex);

        if (mouseX >= labelX - tw / 2f - 4 && mouseX <= labelX + tw / 2f + 4 &&
            mouseY >= labelY - 6 && mouseY <= labelY + 6) {
            editingHex = true;
            hexInput = hex;
        } else {
            editingHex = false;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingWheel = false;
        draggingBri   = false;
        draggingAlpha = false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editingHex || !expanded) return;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            applyHex(hexInput);
            editingHex = false;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            editingHex = false;
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!hexInput.isEmpty()) hexInput = hexInput.substring(0, hexInput.length() - 1);
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (!editingHex || !expanded) return;
        String allowed = "0123456789abcdefABCDEF#";
        if (allowed.indexOf(codePoint) != -1 && hexInput.length() < 7) {
            hexInput += codePoint;
        }
    }

    private void applyHex(String input) {
        try {
            String clean = input.replace("#", "");
            if (clean.length() == 3) {
                clean = "" + clean.charAt(0) + clean.charAt(0) + clean.charAt(1) + clean.charAt(1) + clean.charAt(2) + clean.charAt(2);
            }
            if (clean.length() == 6) {
                int rgb = Integer.parseInt(clean, 16);
                int currentAlpha = (colorSetting.get() >> 24) & 0xFF;
                colorSetting.setColor((currentAlpha << 24) | (rgb & 0xFFFFFF));
                colorSetting.setOverridden(true);
                syncFromColor();
            }
        } catch (Exception ignored) {}
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {
        if (!expanded) return;

        float wheelW = width - PADDING * 2;
        float wheelH = wheelW / 3f;
        float wheelX = x + PADDING;
        float wheelY = y + HEADER_H + PADDING;

        float hueSliderX = wheelX;
        float hueSliderY = wheelY + wheelH + PADDING;
        float alphaSliderX = wheelX;
        float alphaSliderY = hueSliderY + SLIDER_H + PADDING;

        if (draggingWheel) updateWheel(mouseX, mouseY, wheelX, wheelY);
        if (draggingBri)   updateBri(mouseX, hueSliderX);
        if (draggingAlpha) updateAlpha(mouseX, alphaSliderX);
    }

    private void updateWheel(double mx, double my, float wx, float wy) {
        float wheelW = width - PADDING * 2;
        float wheelH = wheelW / 3f;
        sat = MathHelper.clamp((float) (mx - wx) / wheelW, 0f, 1f);
        bri = 1f - MathHelper.clamp((float) (my - wy) / wheelH, 0f, 1f);
        applyToColor();
    }

    private void updateBri(double mx, float barX) {
        float wheelW = width - PADDING * 2;
        hue = MathHelper.clamp((float)(mx - barX) / wheelW, 0f, 1f);
        applyToColor();
    }

    private void updateAlpha(double mx, float barX) {
        float wheelW = width - PADDING * 2;
        alphaVal = MathHelper.clamp((float)(mx - barX) / wheelW, 0f, 1f);
        applyToColor();
    }

    private boolean inBounds(double mx, double my, float bx, float by, float bw, float bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}
