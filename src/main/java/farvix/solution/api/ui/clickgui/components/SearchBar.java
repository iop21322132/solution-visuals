package farvix.solution.api.ui.clickgui.components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.translation.Translations;

import java.awt.*;

@Getter @Setter
public class SearchBar implements QuickImports {
    
    private float x, y, width, height;
    private String searchText = "";
    private boolean focused = false;
    private static final int MAX_CHARACTERS = 32;

    private final Animation focusAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 200);
    private final Animation glowAnimation = new Animation(Easing.EASE_IN_OUT_SINE, 300);
    
    public SearchBar(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHovered(mouseX, mouseY);
        focusAnimation.run(focused ? 1 : 0);
        glowAnimation.run((focused || hovered) ? 1 : 0);
        
        float alpha = getClickGUI().getAlpha().getValue();
        float focusValue = focusAnimation.getValue();
        float glowValue = glowAnimation.getValue();
        
        // Мягкое свечение при фокусе/hover (уменьшено)
        if (glowValue > 0.01f) {
            blur.render(ShapeProperties.create(context.getMatrices(), 
                    x - 1.5f, y - 1.5f, width + 3, height + 3)
                    .round(5)
                    .softness(2f)
                    .color(TempColor.getClientColor().alpha(alpha * glowValue * 0.1f).getRGB())
                    .build());
        }
        
        Color borderColor = focused ? 
            TempColor.getClientColor().alpha(alpha * (0.28f + 0.3f * focusValue)).getColor() :
            TempColor.getSearchBorderUnfocused().alpha(alpha * (1f + 0.5f * glowValue)).getColor();
        
        // Основной фон с тонкой анимированной рамкой
        blur.render(ShapeProperties.create(context.getMatrices(), x - 0.5f, y - 0.5f, width + 1, height + 1)
                .round(4)
                .softness(1.5f)
                .thickness(1.25f + focusValue * 0.25f) // Уменьшено с 2.5 + 0.5 до 1.25 + 0.25
                .outlineColor(borderColor.getRGB())
                .color(TempColor.getSearchBackground().alpha(alpha).getRGB())
                .build());
        
        // Иконка поиска с анимацией
        String icon = "u";
        int iconAlpha = (int)((180 + 75 * focusValue) * alpha);
        
        Fonts.ICONS.get(14).drawString(context.getMatrices(),
                icon,
                x + 8f, y + height / 2f - 1.5f,
                TempColor.getTextSecondary().alpha(iconAlpha / 255f).getRGB());
        
        String displayText = searchText.isEmpty() ? Translations.tr("search.placeholder") : searchText;
        Color textColor = searchText.isEmpty() ? 
            TempColor.getTextPrimary().alpha(alpha * 0.65f).getColor() :  // Заметный placeholder
            TempColor.getTextPrimary().alpha(alpha).getColor();

        // Иконка поиска ярче
        Fonts.ICONS.get(14).drawString(context.getMatrices(),
                icon,
                x + 8f, y + height / 2f - 1.5f,
                TempColor.getTextPrimary().alpha(alpha * 0.85f).getRGB());

        float textX = x + 8f + Fonts.ICONS.get(14).getStringWidth(icon) + 6f;
        float textY = y + height / 2f - 2f;
        float textWidth = width - (textX - x) - 8;
        
        context.enableScissor((int) textX, (int) (y + 1), (int) (textX + textWidth + 5), (int) (y + height - 1));
        Fonts.DEFAULT.get(16).drawBoldString(context.getMatrices(), displayText, textX, textY, textColor.getRGB());
        context.disableScissor();

        // Анимированный курсор
        if (focused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = textX + Fonts.DEFAULT.get(16).getStringWidth(searchText);
            if (cursorX < textX + textWidth) {
                context.enableScissor((int) textX, (int) (y + 1), (int) (textX + textWidth), (int) (y + height - 1));
                float cursorH = 9f;
                float cursorY = y + (height - cursorH) / 2f - 0.5f;
                rectangle.render(ShapeProperties.create(context.getMatrices(), cursorX, cursorY, 1, cursorH)
                        .round(0)
                        .color(TempColor.getClientColor().alpha(alpha).getRGB())
                        .build());
                context.disableScissor();
            }
        }
    }
    
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            focused = isHovered((int) mouseX, (int) mouseY);
        }
    }
    
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return;
        

        switch (keyCode) {
            case 259 -> { 
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
            }
            case 257 -> { 
                focused = false;
            }
            case 256 -> { 
                searchText = "";
                focused = false;
            }
            case 261 -> { 
                focused = false;
            }
        }
    }
    
    public void charTyped(char codePoint, int modifiers) {
        if (!focused) return;
        
        if (codePoint >= 32 && codePoint != 127 && codePoint != 167 && searchText.length() < MAX_CHARACTERS) {
            searchText += codePoint;
        }
    }
    
    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public boolean isEmpty() {
        return searchText.isEmpty();
    }
    
    public String getSearchText() {
        return searchText.toLowerCase();
    }
    
    public boolean isFocused() {
        return focused;
    }
    
    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}
