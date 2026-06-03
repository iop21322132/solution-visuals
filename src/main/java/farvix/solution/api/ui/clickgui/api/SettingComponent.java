package farvix.solution.api.ui.clickgui.api;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.translation.Translations;

@Getter @Setter
public abstract class SettingComponent implements QuickImports {
    
    protected float x, y, width, height = 20;
    protected boolean hoveringClickableArea;
    protected Setting setting;
    protected final ModuleComponent moduleComponent;
    
    public SettingComponent(Setting setting, ModuleComponent moduleComponent) {
        this.setting = setting;
        this.moduleComponent = moduleComponent;
    }
    
    public void init() {

    }
    
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        this.x = x;
        this.y = y;
    }
    
    public void mouseClicked(double mouseX, double mouseY, int button) {

    }
    
    public void mouseReleased(double mouseX, double mouseY, int button) {

    }
    
    public void keyPressed(int keyCode, int scanCode, int modifiers) {

    }
    
    public void charTyped(char codePoint, int modifiers) {

    }
    
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Получает переведенное название настройки
     * Если перевод не найден, возвращает оригинальное название.
     * Суффиксы .r / .g / .b / .alpha обрезаются — они нужны только для
     * автодетекта ColorPreviewComponent, но не должны отображаться в GUI.
     */
    protected String getTranslatedName() {
        String originalName = setting.getName();
        String translated = Translations.tr(originalName);
        // Убираем суффиксы цветовых каналов из отображаемого имени
        if (translated.endsWith(".r"))     return translated.substring(0, translated.length() - 2);
        if (translated.endsWith(".g"))     return translated.substring(0, translated.length() - 2);
        if (translated.endsWith(".b"))     return translated.substring(0, translated.length() - 2);
        if (translated.endsWith(".alpha")) return translated.substring(0, translated.length() - 6);
        return translated;
    }
}

