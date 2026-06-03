package farvix.solution.api.ui.clickgui.impl.settings;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.ui.clickgui.api.SettingComponent;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;

import java.awt.*;

@Getter
public class BooleanSettingComponent extends SettingComponent {
    
    private BooleanSetting booleanSetting;
    private boolean hoveringToggle;
    private Animation toggleAnimation;
    private Animation hoverAnimation;
    
    public BooleanSettingComponent(Setting setting, ModuleComponent moduleComponent) {
        super(setting, moduleComponent);
    }
    
    @Override
    public void init() {
        this.booleanSetting = (BooleanSetting) getSetting();
        this.width = moduleComponent.getWidth();
        this.height = 18;
        this.toggleAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 250);
        this.hoverAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 150);
        super.init();
    }
    
    @Override
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        super.render(context, x, y, mouseX, mouseY, delta);

        this.height = 18;
        float toggleWidth = 9f;
        float toggleHeight = 9f;
        float toggleX = x + 10;
        float toggleY = y + (height - toggleHeight) / 2f;
        
        hoveringToggle = mouseX >= x && mouseX <= x + width && 
                        mouseY >= y && mouseY <= y + height;
        
        float alpha = getClickGUI().getAlpha().getValue();
        
        // Update animations
        toggleAnimation.run(booleanSetting.isEnabled() ? 1 : 0);
        hoverAnimation.run(hoveringToggle ? 1 : 0);
        
        float toggleProgress = toggleAnimation.getValue();
        float hoverProgress = hoverAnimation.getValue();
        
        // Interpolate colors based on toggle state — используем акцент темы
        java.awt.Color accent = farvix.solution.client.managers.ThemeManager.getInstance()
                .getCurrentTheme().getAccentColor();
        int acR = accent.getRed(), acG = accent.getGreen(), acB = accent.getBlue();

        boolean isOn = booleanSetting.isEnabled();

        // Включённые — тёмные непрозрачные, выключенные — светлые прозрачные
        int boxAlpha    = (int)(248 * alpha);
        int borderAlpha = (int)(248 * alpha);
        int iconAlpha   = (int)(248 * alpha);

        int r = isOn ? (int)(20 + (acR - 20) * toggleProgress + 10 * hoverProgress) : (int)(20 + 10 * hoverProgress);
        int g = isOn ? (int)(20 + (acG - 20) * toggleProgress + 10 * hoverProgress) : (int)(20 + 10 * hoverProgress);
        int b = isOn ? (int)(20 + (acB - 20) * toggleProgress + 10 * hoverProgress) : (int)(20 + 10 * hoverProgress);
        int trackColor = new farvix.solution.api.util.color.FixColor(r, g, b, boxAlpha).getRGB();

        int borderR = isOn ? (int)(acR * (0.3f + 0.7f * toggleProgress + 0.2f * hoverProgress)) : (int)(80 + 20 * hoverProgress);
        int borderG = isOn ? (int)(acG * (0.3f + 0.7f * toggleProgress + 0.2f * hoverProgress)) : (int)(80 + 20 * hoverProgress);
        int borderB = isOn ? (int)(acB * (0.3f + 0.7f * toggleProgress + 0.2f * hoverProgress)) : (int)(80 + 20 * hoverProgress);
        int borderColor = new farvix.solution.api.util.color.FixColor(borderR, borderG, borderB, borderAlpha).getRGB();

        // Рисуем резкую и плоскую рамку БЕЗ мягкого свечения/размытия
        rectangle.render(ShapeProperties.create(context.getMatrices(), toggleX, toggleY, toggleWidth, toggleHeight)
                .round(2)
                .thickness(1.0f)
                .outlineColor(borderColor)
                .color(trackColor)
                .build());

        String icon = booleanSetting.isEnabled() ? "9" : "0";
        float iconX = toggleX + toggleWidth / 2f - Fonts.ICONS.get(8).getStringWidth(icon) / 2f;
        if (!booleanSetting.isEnabled()) {
            iconX -= 0.25f; // Крестик: сдвинут левее на 0.25 пикселя (сдвинут вправо на 0.25 по отношению к -0.5)
        }
        float iconY = toggleY + toggleHeight / 2f - Fonts.ICONS.get(8).getStringHeight(icon) / 2f + 4.25f;
        
        // Иконка: непрозрачная для включённых, полупрозрачная для выключенных
        Fonts.ICONS.get(8).drawString(context.getMatrices(), icon,
                iconX, iconY,
                new farvix.solution.api.util.color.FixColor(255, 255, 255, iconAlpha).getRGB());

        float textY = y + 6f;
        Fonts.DEFAULT.get(18).drawString(context.getMatrices(), getTranslatedName(),
                toggleX + toggleWidth + 4, textY,
                TempColor.getTextPrimary().alpha(alpha).getRGB());
    }
    
    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean clicked = mouseX >= x && mouseX <= x + width && 
                         mouseY >= y && mouseY <= y + height;
        
        if (clicked && button == 0) {
            booleanSetting.setEnabled(!booleanSetting.isEnabled());
        }
    }
    
    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}

