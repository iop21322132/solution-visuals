package farvix.solution.api.ui.clickgui.impl.theme;

import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.ui.clickgui.InterfaceScreen;
import farvix.solution.api.ui.clickgui.api.MenuScreen;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.managers.ThemeManager;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class ThemeScreen extends MenuScreen implements QuickImports {

    // Premium obsidian presets
    private static final String[] PRESET_NAMES  = {"Black", "Obsidian", "Amethyst", "Emerald", "Ruby"};
    private static final Color[]  PRESET_COLORS = {
            new Color(15,  15,  15,  200), // Black
            new Color(40,  120, 220, 200), // Steel Blue
            new Color(148, 36,  255, 200), // Purple
            new Color(0,   210, 140, 200), // Green
            new Color(220, 30,  60,  200)  // Red
    };

    // Custom colors (1-3 slots)
    private final Color[] customColors = {
            new Color(40,  120, 220, 200),
            new Color(148, 36,  255, 200),
            new Color(0,   210, 140, 200)
    };
    private int customColorCount = 1;

    // Editing slot index
    private int editingSlot = -1;

    // Custom theme selected state
    private boolean customSelected = false;

    // Preset hover animations
    private final Animation[] presetHoverAnims = {
            new Animation(Easing.EASE_IN_OUT_SINE, 150),
            new Animation(Easing.EASE_IN_OUT_SINE, 150),
            new Animation(Easing.EASE_IN_OUT_SINE, 150),
            new Animation(Easing.EASE_IN_OUT_SINE, 150),
            new Animation(Easing.EASE_IN_OUT_SINE, 150)
    };
    private final Animation customHoverAnim = new Animation(Easing.EASE_IN_OUT_SINE, 150);
    private final Animation addColorAnim    = new Animation(Easing.EASE_IN_OUT_SINE, 150);
    private final Animation remColorAnim    = new Animation(Easing.EASE_IN_OUT_SINE, 150);
    private final Animation[] slotHoverAnims = {
            new Animation(Easing.EASE_IN_OUT_SINE, 150),
            new Animation(Easing.EASE_IN_OUT_SINE, 150),
            new Animation(Easing.EASE_IN_OUT_SINE, 150)
    };
    private final Animation syncToggleAnimation = new Animation(Easing.EASE_IN_OUT_QUINT, 250);

    // Cached coordinates for mouse interactions
    private float cachedContentX, cachedSlotY, cachedApplyY, cachedPickerY, cachedSyncY;

    @Override
    public void init() {
        this.x = getClickGUI().getX() + getClickGUI().getSidebar().getWidth();
        this.y = getClickGUI().getY();
        this.width  = getClickGUI().getWidth() - getClickGUI().getSidebar().getWidth();
        this.height = getClickGUI().getHeight();
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        this.x = getClickGUI().getX() + getClickGUI().getSidebar().getWidth();
        this.y = getClickGUI().getY();
        this.width  = getClickGUI().getWidth() - getClickGUI().getSidebar().getWidth();
        this.height = getClickGUI().getHeight();

        float alpha = getClickGUI().getAlpha().getValue();
        float cx = x + 10;
        float cy = y + 42;

        cachedContentX = cx;

        // --- Preset Buttons (5 columns) ---
        float btnW = (width - 44) / 5f;
        float btnH = 18f;
        float btnGap = 6f;
        String currentName = ThemeManager.getInstance().getCurrentTheme().getName();

        for (int i = 0; i < PRESET_NAMES.length; i++) {
            float bx = cx + i * (btnW + btnGap);
            boolean hov = isHov(bx, cy, btnW, btnH, mouseX, mouseY);
            presetHoverAnims[i].run(hov ? 1f : 0f);
            boolean active = !customSelected && PRESET_NAMES[i].equals(currentName);
            renderThemeBtn(context, bx, cy, btnW, btnH, PRESET_NAMES[i], PRESET_COLORS[i], active,
                    presetHoverAnims[i].getValue(), alpha);
        }
        cy += btnH + 10;

        // --- Separator ---
        blur.render(ShapeProperties.create(context.getMatrices(), cx, cy, width - 20, 1)
                .round(0.5f)
                .softness(1.5f)
                .color(TempColor.getSeparatorHorizontal().alpha(alpha).getRGB()).build());
        cy += 8;

        // --- Color Slots ---
        float slotSize = 22f;
        float slotGap  = 8f;
        cachedSlotY = cy;

        for (int i = 0; i < customColorCount; i++) {
            float sx = cx + i * (slotSize + slotGap);
            boolean hov = isHov(sx, cy, slotSize, slotSize, mouseX, mouseY);
            slotHoverAnims[i].run(hov ? 1f : 0f);
            boolean editing = (editingSlot == i);
            Color c = customColors[i];

            blur.render(ShapeProperties.create(context.getMatrices(), sx - 1, cy - 1, slotSize + 2, slotSize + 2)
                    .round(6).softness(1.5f).thickness(2.5f)
                    .outlineColor(editing
                            ? TempColor.getClientColor().alpha(alpha).getRGB()
                            : new FixColor(c).alpha(alpha * (0.5f + slotHoverAnims[i].getValue() * 0.5f)).getRGB())
                    .color(new FixColor(c).alpha(alpha).getRGB())
                    .build());

            Fonts.DEFAULT.get(10).drawCenteredString(context.getMatrices(), String.valueOf(i + 1),
                    sx + slotSize / 2f, cy + slotSize / 2f - 2,
                    contrastText(c, alpha));
        }

        // Plus / Minus Buttons
        float plusX = cx + customColorCount * (slotSize + slotGap) + 4;
        float plusY = cy + slotSize / 2f - 8;

        if (customColorCount < 3) {
            boolean hov = isHov(plusX, plusY, 16, 16, mouseX, mouseY);
            addColorAnim.run(hov ? 1f : 0f);
            renderSmallBtn(context, plusX, plusY, 16, 16, "+", addColorAnim.getValue(), alpha);
        }
        if (customColorCount > 1) {
            float minusX = plusX + (customColorCount < 3 ? 22 : 0);
            boolean hov = isHov(minusX, plusY, 16, 16, mouseX, mouseY);
            remColorAnim.run(hov ? 1f : 0f);
            renderSmallBtn(context, minusX, plusY, 16, 16, "-", remColorAnim.getValue(), alpha);
        }

        cy += slotSize + 10;

        // --- Apply Custom ---
        cachedApplyY = cy;
        float applyW = width - 20;
        float applyH = 18f;
        boolean applyHov = isHov(cx, cy, applyW, applyH, mouseX, mouseY);
        customHoverAnim.run(applyHov ? 1f : 0f);
        String applyLabel = customColorCount == 1 ? "Apply Custom" : "Apply Gradient (" + customColorCount + ")";
        renderThemeBtn(context, cx, cy, applyW, applyH, applyLabel,
                customColors[0], customSelected, customHoverAnim.getValue(), alpha);
        cy += applyH + 10;

        // --- Color picker ---
        cachedPickerY = cy;
        if (editingSlot >= 0) {
            // Scissor to GUI bounds
            context.enableScissor((int)x, (int)y, (int)(x + width), (int)(y + height));
            renderColorPicker(context, cx, cy, width - 20, mouseX, mouseY, alpha);
            context.disableScissor();
        }

        // --- Sync Colors Panel ---
        float syncW = width - 20;
        float syncH = 24f;
        float syncX = cx;
        float syncY = y + height - syncH - 8;
        cachedSyncY = syncY;

        syncToggleAnimation.run(farvix.solution.api.settings.impl.ColorSetting.syncWithTheme ? 1f : 0f);

        boolean syncHov = isHov(syncX, syncY, syncW, syncH, mouseX, mouseY);
        int borderCol = syncHov 
                ? TempColor.getClientColor().alpha(alpha * 0.7f).getRGB() 
                : TempColor.getModuleBorder().alpha(alpha).getRGB();

        blur.render(ShapeProperties.create(context.getMatrices(), syncX - 1, syncY - 1, syncW + 2, syncH + 2)
                .round(8).softness(1.5f).thickness(2f)
                .outlineColor(borderCol)
                .color(TempColor.getModuleBackground().alpha(alpha).getRGB())
                .build());

        Fonts.SEMIBOLD.get(14).drawCenteredString(context.getMatrices(), "Синхронизация цвета модулей с активной темой",
                syncX + syncW / 2f, syncY + syncH / 2f - 3.5f,
                TempColor.getTextPrimary().alpha(alpha).getRGB());

        float switchW = 20f;
        float switchH = 10f;
        float switchX = syncX + syncW - switchW - 6f;
        float switchY = syncY + syncH / 2f - switchH / 2f;

        float toggleVal = (float) syncToggleAnimation.getValue();

        Color switchOffBg = new Color(50, 50, 50, (int)(180 * alpha));
        Color clientColor = TempColor.getClientColor().getColor();
        Color switchOnBg = new Color(clientColor.getRed(), clientColor.getGreen(), clientColor.getBlue(), (int)(255 * alpha));

        Color switchColor = new Color(
                (int)(switchOffBg.getRed() + (switchOnBg.getRed() - switchOffBg.getRed()) * toggleVal),
                (int)(switchOffBg.getGreen() + (switchOnBg.getGreen() - switchOffBg.getGreen()) * toggleVal),
                (int)(switchOffBg.getBlue() + (switchOnBg.getBlue() - switchOffBg.getBlue()) * toggleVal),
                (int)(switchOffBg.getAlpha() + (switchOnBg.getAlpha() - switchOffBg.getAlpha()) * toggleVal)
        );

        blur.render(ShapeProperties.create(context.getMatrices(), switchX, switchY, switchW, switchH)
                .round(switchH / 2f).softness(1f).thickness(0f)
                .color(switchColor.getRGB())
                .build());

        blur.render(ShapeProperties.create(context.getMatrices(), switchX, switchY, switchW, switchH)
                .round(switchH / 2f).softness(1f).thickness(1f)
                .outlineColor(TempColor.getModuleBorder().alpha(alpha).getRGB())
                .color(0)
                .build());

        float knobSize = 8f;
        float knobX = switchX + 1f + toggleVal * (switchW - knobSize - 2f);
        float knobY = switchY + 1f;

        blur.render(ShapeProperties.create(context.getMatrices(), knobX, knobY, knobSize, knobSize)
                .round(knobSize / 2f).softness(0.5f).thickness(0f)
                .color(new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB())
                .build());
    }

    // ---- Theme Button ----
    private void renderThemeBtn(DrawContext context, float bx, float by, float bw, float bh,
                                String name, Color accent, boolean active, float hv, float alpha) {
        Color outlineColorValue = accent;
        if (active) {
            if (name.equalsIgnoreCase("Black")) {
                outlineColorValue = new Color(220, 220, 220);
            } else if (name.equalsIgnoreCase("Obsidian")) {
                outlineColorValue = new Color(80, 170, 255);
            } else if (name.equalsIgnoreCase("Amethyst")) {
                outlineColorValue = new Color(190, 90, 255);
            }
        }
        Color outline = active
                ? new FixColor(outlineColorValue).alpha(alpha)
                : TempColor.getModuleBorder().alpha(alpha).getColor();
        float thickness = active ? 2.5f : 2f + hv * 1f;

        int bgColor = active
                ? new FixColor(outlineColorValue).alpha(alpha * 0.35f).getRGB()
                : TempColor.getModuleBackground().alpha(alpha * (0.8f + 0.2f * hv)).getRGB();

        blur.render(ShapeProperties.create(context.getMatrices(), bx - 1, by - 1, bw + 2, bh + 2)
                .round(8).softness(1.5f).thickness(thickness)
                .outlineColor(new FixColor(outline).alpha(alpha).getRGB())
                .color(bgColor)
                .build());

        blur.render(ShapeProperties.create(context.getMatrices(), bx + 5, by + bh / 2f - 4, 8, 8)
                .round(3)
                .color(new FixColor(accent).alpha(alpha).getRGB())
                .build());

        int textColor = active
                ? contrastText(accent, alpha)
                : TempColor.getTextPrimary().alpha(alpha).getRGB();

        Fonts.DEFAULT.get(13).drawString(context.getMatrices(), name,
                bx + 18, by + bh / 2f - 3.5f, textColor);
    }

    private int contrastText(Color bg, float alpha) {
        return new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB();
    }

    private void renderSmallBtn(DrawContext context, float bx, float by, float bw, float bh,
                                String label, float hv, float alpha) {
        int outlineCol = TempColor.getClientColor().alpha(alpha * (0.5f + hv * 0.5f)).getRGB();
        int bgCol = TempColor.getClientColor().alpha(alpha * (0.15f + hv * 0.15f)).getRGB();

        blur.render(ShapeProperties.create(context.getMatrices(), bx - 1, by - 1, bw + 2, bh + 2)
                .round(5).softness(1.5f).thickness(2f + hv)
                .outlineColor(outlineCol)
                .color(bgCol)
                .build());
        Fonts.DEFAULT.get(12).drawCenteredString(context.getMatrices(), label,
                bx + bw / 2f, by + bh / 2f - 3,
                TempColor.getTextPrimary().alpha(alpha).getRGB());
    }

    // HSB state for the color picker
    private float pickerHue = 0f, pickerSat = 1f, pickerBri = 1f, pickerAlpha = 1f;
    private boolean draggingSB    = false;
    private boolean draggingHue   = false;
    private boolean draggingAlpha = false;
    private float cachedSBX, cachedSBY, cachedSBW, cachedSBH;
    private float cachedHueX, cachedHueY, cachedHueW, cachedHueH;
    private float cachedAlphaX, cachedAlphaY, cachedAlphaW, cachedAlphaH;
    private boolean editingHex = false;
    private String hexInput = "";

    private void syncPickerFromSlot() {
        if (editingSlot < 0) return;
        Color c = customColors[editingSlot];
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        if (c.getRed() != c.getGreen() || c.getGreen() != c.getBlue()) {
            pickerHue = hsb[0];
        }
        pickerSat = hsb[1]; pickerBri = hsb[2];
        pickerAlpha = c.getAlpha() / 255f;
    }

    private void applyPickerToSlot() {
        if (editingSlot < 0) return;
        int rgb = Color.HSBtoRGB(pickerHue, pickerSat, pickerBri);
        int a = Math.max(0, Math.min(255, (int)(pickerAlpha * 255)));
        customColors[editingSlot] = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, a);
        if (customSelected) applyCustom();
    }

    private static final float PADDING  = 5f;
    private static final float SLIDER_H = 8f;
    private static final float CURSOR_R = 4f;

    // ---- Color picker (HSB Square shader + Hue + Alpha Sliders) ----
    private void renderColorPicker(DrawContext context, float px, float py, float pw,
                                   int mouseX, int mouseY, float alpha) {
        if (editingSlot < 0) return;

        Fonts.DEFAULT.get(12).drawString(context.getMatrices(), "color " + (editingSlot + 1),
                px, py - 2, TempColor.getTextMenu().alpha(alpha).getRGB());
        py += 12;

        float wheelW = 160f;
        float wheelH = wheelW / 3f;
        float wheelX = px;
        float wheelY = py;

        cachedSBX = wheelX; cachedSBY = wheelY; cachedSBW = wheelW; cachedSBH = wheelH;
        cachedHueX = wheelX; cachedHueY = wheelY + wheelH + PADDING; cachedHueW = wheelW; cachedHueH = SLIDER_H;
        cachedAlphaX = wheelX; cachedAlphaY = cachedHueY + SLIDER_H + PADDING; cachedAlphaW = wheelW; cachedAlphaH = SLIDER_H;
        cachedPickerY = py;

        // 1. Draw HSB square via shader
        farvix.solution.api.render.rect.impl.ColorWheelRenderer.draw(context.getMatrices(), wheelX, wheelY, wheelW, wheelH, pickerHue, alpha);

        // Wheel border
        blur.render(ShapeProperties.create(context.getMatrices(), wheelX, wheelY, wheelW, wheelH)
                .round(4f).softness(1.5f).thickness(1.5f)
                .outlineColor(TempColor.getModuleBorder().alpha(alpha).getRGB())
                .color(0).build());

        // Cursor on square
        float curX = wheelX + pickerSat * wheelW;
        float curY = wheelY + (1f - pickerBri) * wheelH;
        
        Color fc = new Color(Color.HSBtoRGB(pickerHue, pickerSat, pickerBri));
        int cursorFillColor = new FixColor(fc.getRed(), fc.getGreen(), fc.getBlue(), (int)(pickerAlpha * 255f * alpha)).getRGB();

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        blur.render(ShapeProperties.create(context.getMatrices(), curX - CURSOR_R, curY - CURSOR_R, CURSOR_R * 2, CURSOR_R * 2)
                .round(CURSOR_R).softness(1).thickness(2f)
                .outlineColor(new FixColor(255, 255, 255, (int)(220 * alpha)).getRGB())
                .color(cursorFillColor)
                .build());
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // 2. Hue slider (Rainbow)
        float hueSliderX = wheelX;
        float hueSliderY = cachedHueY;
        float segmentW = wheelW / 6.0f;
        for (int i = 0; i < 6; i++) {
            float segX = hueSliderX + i * segmentW;
            float hLeft = i / 6.0f;
            float hRight = (i + 1) / 6.0f;
            int colorA = Color.HSBtoRGB(hLeft, 1f, 1f);
            int colorB = Color.HSBtoRGB(hRight, 1f, 1f);
            int cA = new FixColor((colorA >> 16) & 0xFF, (colorA >> 8) & 0xFF, colorA & 0xFF, (int)(255 * alpha)).getRGB();
            int cB = new FixColor((colorB >> 16) & 0xFF, (colorB >> 8) & 0xFF, colorB & 0xFF, (int)(255 * alpha)).getRGB();
            
            org.joml.Vector4f roundVec = new org.joml.Vector4f(0f);
            if (i == 0) {
                roundVec = new org.joml.Vector4f(0f, 0f, SLIDER_H / 2f, SLIDER_H / 2f);
            } else if (i == 5) {
                roundVec = new org.joml.Vector4f(SLIDER_H / 2f, SLIDER_H / 2f, 0f, 0f);
            }
            blur.render(ShapeProperties.create(context.getMatrices(), segX, hueSliderY, segmentW, SLIDER_H)
                    .round(roundVec).softness(0).thickness(0)
                    .color(new org.joml.Vector4i(cA, cA, cB, cB))
                    .outlineColor(0).build());
        }

        // Hue outline
        blur.render(ShapeProperties.create(context.getMatrices(), hueSliderX, hueSliderY, wheelW, SLIDER_H)
                .round(SLIDER_H / 2f).softness(1.5f).thickness(1.5f)
                .outlineColor(TempColor.getModuleBorder().alpha(alpha).getRGB())
                .color(0).build());

        // Hue cursor handle
        float cursorHueX = hueSliderX + pickerHue * wheelW;
        blur.render(ShapeProperties.create(context.getMatrices(), cursorHueX - CURSOR_R, hueSliderY - 2, CURSOR_R * 2, SLIDER_H + 4)
                .round(CURSOR_R).softness(1.5f).thickness(2f)
                .outlineColor(new FixColor(255, 255, 255, (int)(220 * alpha)).getRGB())
                .color(0)
                .build());

        // 3. Alpha slider
        float alphaSliderX = wheelX;
        float alphaSliderY = cachedAlphaY;
        int cAlphaLeft = new FixColor(fc.getRed(), fc.getGreen(), fc.getBlue(), 0).getRGB();
        int cAlphaRight = new FixColor(fc.getRed(), fc.getGreen(), fc.getBlue(), (int)(255 * alpha)).getRGB();

        blur.render(ShapeProperties.create(context.getMatrices(), alphaSliderX, alphaSliderY, wheelW, SLIDER_H)
                .round(SLIDER_H / 2f).softness(1.5f).thickness(0)
                .color(new org.joml.Vector4i(cAlphaLeft, cAlphaLeft, cAlphaRight, cAlphaRight))
                .outlineColor(0).build());

        // Alpha outline
        blur.render(ShapeProperties.create(context.getMatrices(), alphaSliderX, alphaSliderY, wheelW, SLIDER_H)
                .round(SLIDER_H / 2f).softness(1.5f).thickness(1.5f)
                .outlineColor(TempColor.getModuleBorder().alpha(alpha).getRGB())
                .color(0).build());

        // Alpha cursor handle
        float cursorAlphaX = alphaSliderX + pickerAlpha * wheelW;
        blur.render(ShapeProperties.create(context.getMatrices(), cursorAlphaX - CURSOR_R, alphaSliderY - 2, CURSOR_R * 2, SLIDER_H + 4)
                .round(CURSOR_R).softness(1.5f).thickness(2f)
                .outlineColor(new FixColor(255, 255, 255, (int)(220 * alpha)).getRGB())
                .color(0)
                .build());

        // Hex label under sliders
        String currentHex = String.format("#%06X", customColors[editingSlot].getRGB() & 0xFFFFFF);
        String displayHex = editingHex ? hexInput + (System.currentTimeMillis() % 1000 < 500 ? "_" : "") : currentHex;
        int labelColor = editingHex ? TempColor.getClientColor().alpha(alpha).getRGB() : TempColor.getTextSecondary().alpha(alpha).getRGB();

        float labelX = wheelX + wheelW / 2f;
        float labelY = alphaSliderY + SLIDER_H + PADDING + 1f;

        Fonts.DEFAULT.get(13).drawCenteredBoldString(context.getMatrices(), displayHex,
                labelX, labelY,
                labelColor);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        float cx = cachedContentX;

        // Sync Colors Panel Click
        float syncW = width - 20;
        float syncH = 24f;
        if (mouseX >= cx && mouseX <= cx + syncW && mouseY >= cachedSyncY && mouseY <= cachedSyncY + syncH) {
            farvix.solution.api.settings.impl.ColorSetting.syncWithTheme = !farvix.solution.api.settings.impl.ColorSetting.syncWithTheme;
            farvix.solution.client.modules.Module.playClickSound2();
            if (farvix.solution.api.settings.impl.ColorSetting.syncWithTheme) {
                farvix.solution.api.settings.impl.ColorSetting.resetAllOverridden();
            }
            try {
                farvix.solution.Client.getInstance().getConfigManager().saveConfig("_autosave");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // Hex label check first
        boolean clickedHex = false;
        if (editingSlot >= 0 && cachedSBW > 0) {
            String hex = String.format("#%06X", customColors[editingSlot].getRGB() & 0xFFFFFF);
            float labelX = cachedSBX + cachedSBW / 2f;
            float labelY = cachedAlphaY + SLIDER_H + PADDING + 1f;
            float tw = Fonts.DEFAULT.get(13).getStringWidth(hex);

            if (mouseX >= labelX - tw / 2f - 4 && mouseX <= labelX + tw / 2f + 4 &&
                mouseY >= labelY - 6 && mouseY <= labelY + 6) {
                editingHex = true;
                hexInput = hex;
                clickedHex = true;
                return;
            }
        }
        if (!clickedHex) {
            editingHex = false;
        }

        // Color picker drag start
        if (editingSlot >= 0 && cachedSBW > 0) {
            // S/B box
            if (mouseX >= cachedSBX && mouseX <= cachedSBX + cachedSBW
                    && mouseY >= cachedSBY && mouseY <= cachedSBY + cachedSBH) {
                draggingSB = true;
                updateSB(mouseX, mouseY);
                return;
            }
            // Hue bar
            if (mouseX >= cachedHueX && mouseX <= cachedHueX + cachedHueW
                    && mouseY >= cachedHueY - 2f && mouseY <= cachedHueY + cachedHueH + 2f) {
                draggingHue = true;
                updateHue(mouseX);
                return;
            }
            // Alpha bar
            if (mouseX >= cachedAlphaX && mouseX <= cachedAlphaX + cachedAlphaW
                    && mouseY >= cachedAlphaY - 2f && mouseY <= cachedAlphaY + cachedAlphaH + 2f) {
                draggingAlpha = true;
                updateAlpha(mouseX);
                return;
            }
        }

        float btnW = (width - 44) / 5f;
        float btnH = 18f, btnGap = 6f;
        float presetY = y + 42;

        // Presets click
        for (int i = 0; i < PRESET_NAMES.length; i++) {
            float bx = cx + i * (btnW + btnGap);
            if (isHov(bx, presetY, btnW, btnH, mouseX, mouseY)) {
                ThemeManager.getInstance().setTheme(new SingleColorTheme(PRESET_NAMES[i], PRESET_COLORS[i]));
                TempColor.setThemeBackground(PRESET_COLORS[i]);
                customSelected = false;
                editingSlot = -1;
                farvix.solution.client.modules.Module.playClickSound2();
                return;
            }
        }

        // Color slot click
        float slotSize = 22f, slotGap = 8f;
        for (int i = 0; i < customColorCount; i++) {
            float sx = cx + i * (slotSize + slotGap);
            if (isHov(sx, cachedSlotY, slotSize, slotSize, mouseX, mouseY)) {
                editingSlot = (editingSlot == i) ? -1 : i;
                if (editingSlot >= 0) syncPickerFromSlot();
                return;
            }
        }

        // Plus / Minus click
        float plusX = cx + customColorCount * (slotSize + slotGap) + 4;
        float plusY = cachedSlotY + slotSize / 2f - 8;
        if (customColorCount < 3 && isHov(plusX, plusY, 16, 16, mouseX, mouseY)) {
            customColorCount++;
            return;
        }
        if (customColorCount > 1) {
            float minusX = plusX + (customColorCount < 3 ? 22 : 0);
            if (isHov(minusX, plusY, 16, 16, mouseX, mouseY)) {
                if (editingSlot >= customColorCount - 1) editingSlot = -1;
                customColorCount--;
                return;
            }
        }

        // Apply Custom
        float applyW = width - 20, applyH = 18f;
        if (isHov(cx, cachedApplyY, applyW, applyH, mouseX, mouseY)) {
            applyCustom();
            customSelected = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingSB    = false;
        draggingHue   = false;
        draggingAlpha = false;
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {
        if (draggingSB)    updateSB(mouseX, mouseY);
        if (draggingHue)   updateHue(mouseX);
        if (draggingAlpha) updateAlpha(mouseX);
    }

    private void updateSB(double mx, double my) {
        pickerSat = Math.max(0f, Math.min(1f, (float)(mx - cachedSBX) / cachedSBW));
        pickerBri = 1f - Math.max(0f, Math.min(1f, (float)(my - cachedSBY) / cachedSBH));
        applyPickerToSlot();
    }

    private void updateHue(double mx) {
        pickerHue = Math.max(0f, Math.min(1f, (float)(mx - cachedHueX) / cachedHueW));
        applyPickerToSlot();
    }

    private void updateAlpha(double mx) {
        pickerAlpha = Math.max(0f, Math.min(1f, (float)(mx - cachedAlphaX) / cachedAlphaW));
        applyPickerToSlot();
    }

    private void applyCustom() {
        if (customColorCount == 1) {
            ThemeManager.getInstance().setTheme(new SingleColorTheme("Custom", customColors[0]));
            TempColor.setThemeBackground(customColors[0]);
        } else {
            Color[] cols = new Color[customColorCount];
            for (int i = 0; i < customColorCount; i++) cols[i] = customColors[i];
            ThemeManager.getInstance().setTheme(new GradientCustomTheme("Custom", cols));
            TempColor.setThemeBackground(customColors[0]);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editingHex || editingSlot < 0) {
            super.keyPressed(keyCode, scanCode, modifiers);
            return;
        }

        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            applyHex(hexInput);
            editingHex = false;
        } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            editingHex = false;
        } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (!hexInput.isEmpty()) hexInput = hexInput.substring(0, hexInput.length() - 1);
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (!editingHex || editingSlot < 0) {
            super.charTyped(codePoint, modifiers);
            return;
        }
        String allowed = "0123456789abcdefABCDEF#";
        if (allowed.indexOf(codePoint) != -1 && hexInput.length() < 7) {
            hexInput += codePoint;
        }
        super.charTyped(codePoint, modifiers);
    }

    private void applyHex(String input) {
        try {
            String clean = input.replace("#", "");
            if (clean.length() == 3) {
                clean = "" + clean.charAt(0) + clean.charAt(0) + clean.charAt(1) + clean.charAt(1) + clean.charAt(2) + clean.charAt(2);
            }
            if (clean.length() == 6) {
                int rgb = Integer.parseInt(clean, 16);
                int a = customColors[editingSlot].getAlpha();
                customColors[editingSlot] = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, a);
                syncPickerFromSlot();
                if (customSelected) applyCustom();
            }
        } catch (Exception ignored) {}
    }

    private InterfaceScreen getClickGUI() {
        return (InterfaceScreen) mc.currentScreen;
    }

    private boolean isHov(float x, float y, float w, float h, double mx, double my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ---- Themes ----

    public static class SingleColorTheme implements ThemeManager.Theme {
        private final String name;
        private final Color color;
        public SingleColorTheme(String name, Color color) { this.name = name; this.color = color; }
        @Override public Color getBackgroundColor()          { return color; }
        @Override public Color getBorderColor()              { return color; }
        @Override public Color getTextColor()                { 
            float brightness = (color.getRed() * 0.299f + color.getGreen() * 0.587f + color.getBlue() * 0.114f) / 255f;
            return brightness > 0.5f ? new Color(20, 20, 20) : Color.WHITE;
        }
        @Override public Color getAccentColor()              { return color; }
        @Override public Color getSecondaryBackgroundColor() { return color; }
        @Override public String getName()                    { return name; }
    }

    public static class GradientCustomTheme implements ThemeManager.Theme {
        private final String name;
        private final Color[] colors;
        private static final long STEP = 1000L;

        public GradientCustomTheme(String name, Color[] colors) {
            this.name = name;
            this.colors = colors;
        }

        private Color current() {
            int n = colors.length;
            float phase = (float)(System.currentTimeMillis() % (STEP * n)) / STEP;
            int idx = (int) phase;
            float t = (float) Math.sin((phase - idx) * Math.PI / 2);
            Color a = colors[idx % n], b = colors[(idx + 1) % n];
            return new Color(
                    clamp((int)(a.getRed()   + (b.getRed()   - a.getRed())   * t)),
                    clamp((int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t)),
                    clamp((int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)),
                    clamp((int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t))
            );
        }
        private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

        @Override public Color getBackgroundColor()          { return current(); }
        @Override public Color getBorderColor()              { return current(); }
        @Override public Color getTextColor()                { 
            Color c = current();
            float brightness = (c.getRed() * 0.299f + c.getGreen() * 0.587f + c.getBlue() * 0.114f) / 255f;
            return brightness > 0.5f ? new Color(20, 20, 20) : Color.WHITE;
        }
        @Override public Color getAccentColor()              { return current(); }
        @Override public Color getSecondaryBackgroundColor() { return current(); }
        @Override public String getName()                    { return name; }
    }
}
