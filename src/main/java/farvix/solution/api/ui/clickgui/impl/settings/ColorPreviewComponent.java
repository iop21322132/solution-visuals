package farvix.solution.api.ui.clickgui.impl.settings;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.ui.clickgui.api.SettingComponent;
import farvix.solution.api.util.color.FixColor;

/**
 * A pseudo-setting component that shows a live color preview swatch.
 * It reads three SliderSettings (R, G, B) from the module and draws
 * a rounded square filled with the resulting color.
 *
 * Add it to ModuleComponent after the three RGB sliders.
 */
@Getter
public class ColorPreviewComponent extends SettingComponent implements QuickImports {

    private final SliderSetting rSetting;
    private final SliderSetting gSetting;
    private final SliderSetting bSetting;
    private final SliderSetting aSetting; // nullable

    /** Legacy constructor without alpha */
    public ColorPreviewComponent(SliderSetting r, SliderSetting g, SliderSetting b,
                                 Setting dummySetting, ModuleComponent moduleComponent) {
        this(r, g, b, null, dummySetting, moduleComponent);
    }

    public ColorPreviewComponent(SliderSetting r, SliderSetting g, SliderSetting b,
                                 SliderSetting a,
                                 Setting dummySetting, ModuleComponent moduleComponent) {
        super(dummySetting, moduleComponent);
        this.rSetting = r;
        this.gSetting = g;
        this.bSetting = b;
        this.aSetting = a;
    }

    @Override
    public void init() {
        this.width  = 119.5f;
        this.height = 18f;
        super.init();
    }

    @Override
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        super.render(context, x, y, mouseX, mouseY, delta);

        float alpha = getClickGUI().getAlpha().getValue();

        int r = (int) rSetting.getValue();
        int g = (int) gSetting.getValue();
        int b = (int) bSetting.getValue();
        int a = aSetting != null ? (int) aSetting.getValue() : 255;

        // Label
        Fonts.DEFAULT.get(15).drawBoldString(context.getMatrices(), "Цвет",
                x + 5, y + 4f,
                TempColor.getTextPrimary().alpha(alpha).getRGB());

        // Color swatch — right side, 12×12
        float swatchSize = 12f;
        float swatchX = x + width - swatchSize - 5;
        float swatchY = y + (height - swatchSize) / 2f;

        // Actual color with alpha — a is 0-255, alpha is 0-1 GUI fade
        blur.render(ShapeProperties.create(context.getMatrices(), swatchX, swatchY, swatchSize, swatchSize)
                .round(4)
                .softness(1)
                .thickness(1.5f)
                .outlineColor(TempColor.getModuleBorder().alpha(alpha).getRGB())
                .color(new FixColor(r, g, b, (int)(a * alpha)).getRGB())
                .build());

        // Hex label
        String hex = aSetting != null
                ? String.format("#%02X%02X%02X%02X", r, g, b, a)
                : String.format("#%02X%02X%02X", r, g, b);
        Fonts.DEFAULT.get(13).drawBoldString(context.getMatrices(), hex,
                swatchX - Fonts.DEFAULT.get(13).getStringWidth(hex) - 4,
                y + 4f,
                TempColor.getTextSecondary().alpha(alpha).getRGB());
    }

    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}
