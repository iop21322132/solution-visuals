package farvix.solution.api.ui.clickgui.impl.settings;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.ui.clickgui.api.CustomElement;
import farvix.solution.api.translation.Translations;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;

import java.awt.*;

@Getter
public class SubComponent extends CustomElement {

    private final BooleanSetting setting;
    private final MultiModeSettingComponent parent;
    private Animation hoverAnimation;

    public SubComponent(BooleanSetting setting, MultiModeSettingComponent parent) {
        this.setting = setting;
        this.parent = parent;
    }

    @Override
    public void init() {
        String translatedName = Translations.tr(setting.getName());
        this.width = Fonts.DEFAULT.get(13).getStringWidth(translatedName) + 14; 
        this.height = 15; 
        this.hoverAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 150);
        super.init();
    }

    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        this.x = x;
        this.y = y;


        float alpha = getClickGUI().getAlpha().getValue();
        

        boolean hovering = mouseX >= x && mouseX <= x + width && 
                          mouseY >= y && mouseY <= y + height;
        
        hoverAnimation.run(hovering ? 1 : 0);
        float hoverProgress = hoverAnimation.getValue();

        boolean isEnabled = setting.isEnabled();

        boolean isBlackTheme = farvix.solution.client.managers.ThemeManager.getInstance().getCurrentTheme().getName().equalsIgnoreCase("Black");

        int borderCol;
        int bgColorVal;
        float thicknessVal;
        if (isEnabled) {
            bgColorVal = isBlackTheme
                    ? new farvix.solution.api.util.color.FixColor(255, 255, 255, (int)(alpha * 64)).getRGB()
                    : TempColor.getClientColor().alpha(alpha * 0.25f).getRGB();
            borderCol = new farvix.solution.api.util.color.FixColor(255, 255, 255, (int)(alpha * 180)).getRGB();
            thicknessVal = 1.0f;
        } else {
            bgColorVal = isBlackTheme
                    ? new farvix.solution.api.util.color.FixColor(255, 255, 255, (int)(alpha * 25 * hoverProgress)).getRGB()
                    : TempColor.getClientColor().alpha(alpha * 0.10f * hoverProgress).getRGB();
            borderCol = new farvix.solution.api.util.color.FixColor(255, 255, 255, (int)(alpha * (76 + 38 * hoverProgress))).getRGB();
            thicknessVal = 2.0f;
        }

        int tVal = 245;
        int textColor = new farvix.solution.api.util.color.FixColor(tVal, tVal, tVal, (int)(248 * alpha)).getRGB();

        // Рисуем плоскую скругленную кнопку с точной прозрачностью
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, height)
                .round(5)
                .thickness(thicknessVal)
                .outlineColor(borderCol)
                .color(bgColorVal)
                .build());
        

        String translatedName = Translations.tr(setting.getName());
        Fonts.DEFAULT.get(13).drawCenteredBoldString(context.getMatrices(), translatedName,
                x + width / 2f, y + height / 2f - 2f,
                textColor);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean hovering = mouseX >= x && mouseX <= x + width && 
                          mouseY >= y && mouseY <= y + height;
        
        if (hovering && button == 0) {
            setting.setEnabled(!setting.isEnabled());
        }
    }
    
    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}
