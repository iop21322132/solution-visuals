package farvix.solution.api.ui.clickgui.impl.settings;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.ui.clickgui.api.SettingComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ModeSettingComponent extends SettingComponent {
    
    private ModeSetting modeSetting;
    private final List<ModeComponent> modeComponents = new ArrayList<>();
    private boolean expanded;
    private float rightMargin = 5;
    private float notExpandedHeight = 18;
    private float expandedHeight = 0;
    
    public ModeSettingComponent(Setting setting, ModuleComponent moduleComponent) {
        super(setting, moduleComponent);
    }
    
    @Override
    public void init() {
        this.modeSetting = (ModeSetting) getSetting();
        this.width = moduleComponent.getWidth();
        this.height = 54f;

        modeComponents.clear();
        for (String mode : modeSetting.getModes()) {
            ModeComponent component = new ModeComponent(mode, this);
            modeComponents.add(component);
            component.init();
        }
        
        super.init();
    }

    @Override
    public float getHeight() {
        float fieldW = width - 20f;
        float startX = 10f;
        float startY = 16f;
        float gapX = 4f;
        float gapY = 4f;
        float btnH = 15f;
        
        float currentX = startX;
        float currentY = startY;
        
        for (ModeComponent component : modeComponents) {
            String translatedMode = modeSetting.getTranslatedMode(component.getMode());
            float btnW = Fonts.DEFAULT.get(13).getStringWidth(translatedMode) + 14f;
            if (btnW > fieldW) btnW = fieldW;
            
            if (currentX + btnW > startX + fieldW && currentX > startX) {
                currentX = startX;
                currentY += btnH + gapY;
            }
            currentX += btnW + gapX;
        }
        
        this.height = currentY + btnH + 4f;
        return this.height;
    }
    
    @Override
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        float alpha = getClickGUI().getAlpha().getValue();
        this.height = getHeight();

        // Название настройки (по левой стороне)
        Fonts.DEFAULT.get(14).drawString(context.getMatrices(), getTranslatedName(),
                x + 10, y + 4f,
                TempColor.getTextPrimary().alpha(alpha).getRGB());

        // Сами кнопки (на всю ширину с переносом)
        int N = modeComponents.size();
        if (N > 0) {
            float gapX = 4f;
            float gapY = 4f;
            float btnH = 15f;
            float startX = x + 10f;
            float startY = y + 16f;

            float currentX = startX;
            float currentY = startY;
            float fieldW = width - 20f;

            for (ModeComponent component : modeComponents) {
                String translatedMode = modeSetting.getTranslatedMode(component.getMode());
                float btnW = Fonts.DEFAULT.get(13).getStringWidth(translatedMode) + 14f;
                if (btnW > fieldW) btnW = fieldW;

                if (currentX + btnW > startX + fieldW && currentX > startX) {
                    currentX = startX;
                    currentY += btnH + gapY;
                }

                component.setWidth(btnW);
                component.setHeight(btnH);
                component.render(context, currentX, currentY, mouseX, mouseY, delta);

                currentX += btnW + gapX;
            }
        }

        super.render(context, x, y, mouseX, mouseY, delta);
    }
    
    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        int N = modeComponents.size();
        if (N > 0) {
            float fieldW = width - 20f;
            float startX = x + 10f;
            float startY = y + 16f;
            float gapX = 4f;
            float gapY = 4f;
            float btnH = 15f;

            float currentX = startX;
            float currentY = startY;

            for (ModeComponent component : modeComponents) {
                String translatedMode = modeSetting.getTranslatedMode(component.getMode());
                float btnW = Fonts.DEFAULT.get(13).getStringWidth(translatedMode) + 14f;
                if (btnW > fieldW) btnW = fieldW;

                if (currentX + btnW > startX + fieldW && currentX > startX) {
                    currentX = startX;
                    currentY += btnH + gapY;
                }

                component.setX(currentX);
                component.setY(currentY);
                component.setWidth(btnW);
                component.setHeight(btnH);
                component.mouseClicked(mouseX, mouseY, button);

                currentX += btnW + gapX;
            }
        }
    }
    
    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}

