package farvix.solution.api.ui.solution.components;

import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.ui.solution.SolutionGuiScreen;
import farvix.solution.api.ui.solution.SolutionTab;
import farvix.solution.api.ui.solution.utils.SolutionColors;
import farvix.solution.api.util.color.FixColor;

import java.util.HashMap;
import java.util.Map;

/**
 * Панель с полем поиска и панелью описания модуля при наведении
 */
public class TabNavigationBar implements QuickImports {
    
    private final SolutionGuiScreen parent;
    
    // ═══════════════════════════════════════════════════════════════════════
    // РАЗМЕРЫ
    // ═══════════════════════════════════════════════════════════════════════
    private static final float TAB_BAR_HEIGHT = 35f;
    
    // ═══════════════════════════════════════════════════════════════════════
    // КОМПОНЕНТЫ
    // ═══════════════════════════════════════════════════════════════════════
    private SearchField searchField;

    // Анимация появления описания
    private Animation descAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180);
    private String lastDescription = "";
    
    public TabNavigationBar(SolutionGuiScreen parent) {
        this.parent = parent;
    }
    
    public void init() {
        float guiX = parent.getX();
        float searchY = parent.getY() + 5f;
        this.searchField = new SearchField(parent, guiX, searchY);
        this.searchField.init();
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float alpha = parent.getAlpha();
        if (alpha <= 0.01f) return;
        
        float screenW = 960f;
        float screenH = 540f;

        // Размеры поля поиска (из SearchField)
        float searchWidth  = screenW * 0.15f;
        float searchHeight = screenH * 0.035f;
        float guiX    = parent.getX();
        float guiWidth = parent.getWidth();
        float searchX = guiX + guiWidth - searchWidth - 5f;
        float searchY = parent.getY() + 5f;

        searchField.setPosition(searchX, searchY);
        searchField.render(context, mouseX, mouseY, delta);

        // ═══════════════════════════════════════════════════════════════════
        // ПАНЕЛЬ ОПИСАНИЯ — слева от поиска
        // ═══════════════════════════════════════════════════════════════════
        String desc = parent.getHoveredDescription();
        boolean hasDesc = desc != null && !desc.isEmpty();
        descAnimation.run(hasDesc ? 1f : 0f);
        float descAlpha = descAnimation.getValue() * alpha;

        if (descAlpha > 0.01f) {
            float cornerRadius = screenH * 0.010f;
            int fontSize = (int)(screenH * 0.036f);

            // Ширина панели описания — от левого края GUI до поля поиска минус зазор
            float gap = 6f;
            float descPanelW = searchX - guiX - gap;
            float descPanelX = guiX;
            float descPanelY = searchY;
            float descPanelH = searchHeight;

            // Фон панели описания (такой же стиль как у SearchField)
            glass.render(farvix.solution.api.render.rect.ShapeProperties.create(
                    context.getMatrices(),
                    descPanelX, descPanelY, descPanelW, descPanelH)
                    .round(cornerRadius)
                    .softness(4f)
                    .thickness(0)
                    .outlineColor(0)
                    .color(new FixColor(30, 30, 35, (int)(200 * descAlpha)).getRGB())
                    .build());

            // Тонкая обводка
            rectangle.render(farvix.solution.api.render.rect.ShapeProperties.create(
                    context.getMatrices(),
                    descPanelX, descPanelY, descPanelW, descPanelH)
                    .round(cornerRadius)
                    .softness(0)
                    .thickness(0.5f)
                    .outlineColor(new FixColor(60, 60, 70, (int)(40 * descAlpha)).getRGB())
                    .color(0)
                    .build());

            // Текст описания
            float hPad = screenW * 0.008f;
            float textH = Fonts.SEMIBOLD.get(fontSize).getStringHeight("Ag") / 2f;
            float textY = descPanelY + (descPanelH - textH) / 2f - 1f;

            // Обрезаем текст если не влезает
            String displayDesc = desc;
            float maxTextW = descPanelW - hPad * 2;
            float textW = Fonts.SEMIBOLD.get(fontSize).getStringWidth(displayDesc);
            if (textW > maxTextW) {
                while (textW > maxTextW && displayDesc.length() > 3) {
                    displayDesc = displayDesc.substring(0, displayDesc.length() - 1);
                    textW = Fonts.SEMIBOLD.get(fontSize).getStringWidth(displayDesc + "...");
                }
                displayDesc = displayDesc + "...";
            }

            Fonts.SEMIBOLD.get(fontSize).drawString(
                    context.getMatrices(),
                    displayDesc,
                    descPanelX + hPad,
                    textY,
                    new FixColor(160, 160, 170, (int)(230 * descAlpha)).getRGB()
            );
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        return searchField.mouseClicked(mouseX, mouseY, button);
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return searchField.keyPressed(keyCode, scanCode, modifiers);
    }
    
    public boolean charTyped(char codePoint, int modifiers) {
        return searchField.charTyped(codePoint, modifiers);
    }
    
    public static float getHeight() {
        return TAB_BAR_HEIGHT;
    }
}
