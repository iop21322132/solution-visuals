package farvix.solution.api.ui.clickgui.impl.settings;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.impl.StringSetting;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.ui.clickgui.api.SettingComponent;

import java.awt.*;

@Getter
public class StringSettingComponent extends SettingComponent {

    private StringSetting stringSetting;
    private boolean focused;
    private String currentText;
    private int cursorPosition;
    private int selectionStart;
    private int selectionEnd;
    private long lastCursorBlink;
    private boolean cursorVisible;

    public StringSettingComponent(Setting setting, ModuleComponent moduleComponent) {
        super(setting, moduleComponent);
    }

    @Override
    public void init() {
        this.stringSetting = (StringSetting) getSetting();
        this.width = moduleComponent.getWidth();
        this.height = 18;
        this.currentText = stringSetting.getText();
        this.cursorPosition = currentText.length();
        this.selectionStart = -1;
        this.selectionEnd = -1;
        super.init();
    }

    @Override
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        super.render(context, x, y, mouseX, mouseY, delta);

        float alpha = getClickGUI().getAlpha().getValue();

        float textFieldX = x + 3;
        float textFieldHeight = 14;
        float textFieldY = y + (height - textFieldHeight) / 2f;
        float textFieldWidth = width - 10;

        Color fieldBg = focused
                ? TempColor.getModuleBackground().alpha(alpha * 0.93f)
                : TempColor.getModuleBackground().alpha(alpha * 0.83f);
        Color fieldBorder = focused
                ? TempColor.getClientColor().alpha(alpha)
                : TempColor.getKeyBorder().alpha(alpha);

        blur.render(ShapeProperties.create(context.getMatrices(), textFieldX, textFieldY, textFieldWidth, textFieldHeight)
                .round(2)
                .thickness(1)
                .outlineColor(fieldBorder.getRGB())
                .color(fieldBg.getRGB())
                .build());

        // Название — слева внутри строки, не над полем (чтобы не накладывалось)
        if (currentText.isEmpty() && !focused) {
            // Placeholder: название настройки серым внутри поля
            Fonts.DEFAULT.get(14).drawBoldString(context.getMatrices(), getTranslatedName(),
                    textFieldX + 3, y + 4f,
                    TempColor.getTextSecondary().alpha(alpha * 0.6f).getRGB());
        }

        if (!currentText.isEmpty()) {
            Fonts.DEFAULT.get(14).drawBoldString(context.getMatrices(), currentText,
                    textFieldX + 3, y + 4f,
                    TempColor.getTextPrimary().alpha(alpha).getRGB());
        }

        if (focused) {
            updateCursorBlink();
            if (cursorVisible) {
                float cursorX = textFieldX + 3 + Fonts.DEFAULT.get(14).getStringWidth(
                        currentText.substring(0, Math.min(cursorPosition, currentText.length())));
                blur.render(ShapeProperties.create(context.getMatrices(), cursorX, textFieldY + 2, 1, textFieldHeight - 4)
                        .color(TempColor.getTextPrimary().alpha(alpha).getRGB())
                        .build());
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        float textFieldX = x + 3;
        float textFieldY = y - 1;
        float textFieldWidth = width - 10;
        float textFieldHeight = 16;
        
        boolean clickedOnField = mouseX >= textFieldX && mouseX <= textFieldX + textFieldWidth && 
                                mouseY >= textFieldY && mouseY <= textFieldY + textFieldHeight;
        
        if (clickedOnField && button == 0) {
            focused = true;

            float relativeX = (float) mouseX - textFieldX - 3;
            cursorPosition = getCursorPositionFromX(relativeX);
        } else if (button == 0) {
            focused = false;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return;
        
        switch (keyCode) {
            case 256: 
                focused = false;
                break;
            case 257: // Enter — только снимаем фокус, НЕ отправляем команду
                focused = false;
                stringSetting.setText(currentText);
                break;
            case 259: 
                if (cursorPosition > 0) {
                    currentText = currentText.substring(0, cursorPosition - 1) + currentText.substring(cursorPosition);
                    cursorPosition--;
                    stringSetting.setText(currentText);
                }
                break;
            case 261: 
                if (cursorPosition < currentText.length()) {
                    currentText = currentText.substring(0, cursorPosition) + currentText.substring(cursorPosition + 1);
                    stringSetting.setText(currentText);
                }
                break;
            case 262: 
                if (cursorPosition < currentText.length()) {
                    cursorPosition++;
                }
                break;
            case 263: 
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                break;
            case 265: 
                cursorPosition = 0;
                break;
            case 264: 
                cursorPosition = currentText.length();
                break;
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (!focused) return;
        
        if (currentText.length() < stringSetting.getMaxLength()) {
            currentText = currentText.substring(0, cursorPosition) + codePoint + currentText.substring(cursorPosition);
            cursorPosition++;
            stringSetting.setText(currentText);
        }
    }
    
    private void updateCursorBlink() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCursorBlink > 500) {
            cursorVisible = !cursorVisible;
            lastCursorBlink = currentTime;
        }
    }
    
    private int getCursorPositionFromX(float x) {
        if (x <= 0) return 0;
        
        for (int i = 0; i <= currentText.length(); i++) {
            float textWidth = Fonts.DEFAULT.get(14).getStringWidth(currentText.substring(0, i));
            if (x < textWidth) {
                return i;
            }
        }
        return currentText.length();
    }
    
    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}
