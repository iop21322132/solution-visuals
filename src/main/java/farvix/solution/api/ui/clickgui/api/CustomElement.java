package farvix.solution.api.ui.clickgui.api;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.interfaces.QuickImports;

@Getter @Setter
public class CustomElement implements QuickImports {
    
    public float x, y, width, height;
    private boolean isDisplayingElement;
    
    public void init() {
    }
    
    public void render(DrawContext context, int mouseX, int mouseY) {
    }
    
    public void mouseClicked(double mouseX, double mouseY, int button) {
    }
    
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }
    
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
    }
    
    public void charTyped(char codePoint, int modifiers) {
    }
    
    protected boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    protected boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
