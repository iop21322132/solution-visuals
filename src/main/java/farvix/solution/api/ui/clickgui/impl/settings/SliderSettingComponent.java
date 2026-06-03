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
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;

import java.awt.*;

@Getter
public class SliderSettingComponent extends SettingComponent {
    
    private SliderSetting sliderSetting;
    private boolean dragging;

    // Drag lock: защита от "старых" x/y при перестройке/анимациях layout
    private float dragLockX;
    private float dragLockY;
    private static final float DRAG_LOCK_EPS = 0.5f;

    private float sliderWidth = 80;
    private float sliderHeight = 4;
    private Animation dragAnimation;
    private Animation hoverAnimation;

    // Чтобы при открытии ClickGUI не проигрывалась анимация "включения" для уже активных модов
    private boolean justOpened;
    
    public SliderSettingComponent(Setting setting, ModuleComponent moduleComponent) {
        super(setting, moduleComponent);
    }
    
    @Override
    public void init() {
        this.sliderSetting = (SliderSetting) getSetting();
        this.width = moduleComponent.getWidth();
        this.height = 20;
        this.dragAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 200);
        this.hoverAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 150);

        this.justOpened = true;

        // Жёсткий сброс на первом кадре после открытия GUI
        this.dragAnimation.setValue(0);
        this.hoverAnimation.setValue(0);

        super.init();
    }
    
    @Override
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        super.render(context, x, y, mouseX, mouseY, delta);
        
        float alpha = getClickGUI().getAlpha().getValue();
        this.height = 15;
        this.sliderHeight = 2.5f;

        sliderWidth = width - 20;
        float sliderX = x + 10;
        float sliderY = y + 8;
        float progress = (sliderSetting.getValue() - sliderSetting.getMin()) / (sliderSetting.getMax() - sliderSetting.getMin());
        float handleX = sliderX + progress * sliderWidth;
        
        // Check if hovering over handle
        boolean hoveringHandle = mouseX >= handleX - 4 && mouseX <= handleX + 4 && 
                                 mouseY >= sliderY - 3 && mouseY <= sliderY + 3;
        
        // Disable any visual animation for the slider itself.
        // The slider fill/handle should be fully static unless user is actively dragging.
        if (justOpened) {
            justOpened = false;
        }
        if (dragging) {
            // keep value update logic only; visuals stay static
        }

        float glowIntensity = 0f;
        
        Color sliderBg = new farvix.solution.api.util.color.FixColor(40, 40, 40, (int)(248 * alpha)).getColor();
        Color sliderActive = TempColor.getClientColor().alpha(alpha).getColor();
        Color handleColor = TempColor.getClientColor().alpha(alpha).getColor();

        // Если активна черная тема (Black), делаем ползунки чисто белыми
        if (farvix.solution.client.managers.ThemeManager.getInstance().getCurrentTheme().getName().equalsIgnoreCase("Black")) {
            sliderActive = new farvix.solution.api.util.color.FixColor(255, 255, 255, (int)(255 * alpha)).getColor();
            handleColor = new farvix.solution.api.util.color.FixColor(255, 255, 255, (int)(255 * alpha)).getColor();
        }
        
        blur.render(ShapeProperties.create(context.getMatrices(), sliderX, sliderY, sliderWidth, sliderHeight)
                .round(2)
                .softness(1.5f)
                .color(sliderBg.getRGB())
                .build());
        
        if (progress > 0.005f) {
            blur.render(ShapeProperties.create(context.getMatrices(), sliderX, sliderY, progress * sliderWidth, sliderHeight)
                    .round(2)
                    .softness(1.5f)
                    .color(sliderActive.getRGB())
                    .build());
        }
        
        // Glow around handle removed to prevent "slider animation" on GUI open.
        
        // Handle (static)
        float handleSize = 5f;
        
        blur.render(ShapeProperties.create(context.getMatrices(), 
                handleX - handleSize/2f, sliderY - (handleSize - sliderHeight)/2f, handleSize, handleSize)
                .round(handleSize/2f)
                .softness(1.5f)
                .thickness(1.5f)
                .outlineColor(handleColor.getRGB())
                .color(handleColor.getRGB())
                .build());
        
        String displayText = getTranslatedName();
        Fonts.DEFAULT.get(14).drawString(context.getMatrices(), displayText,
                x + 10, sliderY - 8,
                TempColor.getTextPrimary().alpha(alpha).getRGB());

        String displayText2 = String.format("%.1f", sliderSetting.getValue()).replace(',', '.');
        Fonts.DEFAULT.get(11).drawCenteredString(context.getMatrices(), displayText2,
                x + sliderWidth + 10f - 2.5f, sliderY - 5,
                TempColor.getTextPrimary().alpha(alpha).getRGB());
    }
    
    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;

        // Блокируем ручное взаимодействие с ползунком,
        // если настройки модуля не раскрыты (открывается ПКМ).
        if (!moduleComponent.isExpanded()) {
            dragging = false;
            return;
        }
        
        if (mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height) {
            dragging = true;

            // фиксируем layout-позицию на момент старта drag
            dragLockX = this.x;
            dragLockY = this.y;

            // updateValue сама зажимает в min/max
            updateValue(mouseX);
        }
    }
    
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }
    public void mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        if (dragging) {
            updateValue(mouseX);
        }
    }
    
    // Используется родителем при переключении expanded (ПКМ), чтобы не держать "старые" drag-состояния.
    public void cancelDrag() {
        dragging = false;
    }
    
    public void updateValue(double mouseX) {
        if (!dragging) return;
        
        float currentSliderWidth = width - 20;
        float sliderX = x + 10;
        
        float progress = (float) Math.max(0, Math.min(1, (mouseX - sliderX) / currentSliderWidth));
        float value = sliderSetting.getMin() + progress * (sliderSetting.getMax() - sliderSetting.getMin());
        
        float step = sliderSetting.getStep();
        if (step > 0) {
            value = Math.round(value / step) * step;
        }
        
        value = Math.max(sliderSetting.getMin(), Math.min(sliderSetting.getMax(), value));
        sliderSetting.setValue(value);
    }
    
    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}

