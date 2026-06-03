package farvix.solution.api.ui.clickgui.impl.module;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.ui.clickgui.InterfaceScreen;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.ui.clickgui.api.MenuScreen;
import farvix.solution.api.util.other.ScrollUtility;
import farvix.solution.client.modules.api.ModuleCategory;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Getter
public class ModuleScreen extends MenuScreen implements QuickImports {

    public ArrayList<ModuleComponent> moduleComponents;
    private final ScrollUtility scroll = new ScrollUtility();

    // ── Scrollbar state ───────────────────────────────────────────────────────
    private static final float SCROLLBAR_W     = 3f;   // тоньше для элегантности
    private static final float SCROLLBAR_GAP   = 5f;   // больше отступ от края
    private static final float SCROLLBAR_PAD_Y = 44f;  // отступ сверху (под заголовком)

    private boolean scrollbarDragging = false;
    private double  scrollbarDragStartY;
    private float   scrollbarDragStartScroll;
    
    // Анимация hover эффекта для scrollbar
    private farvix.solution.api.animation.Animation scrollbarHoverAnim = 
            new farvix.solution.api.animation.Animation(farvix.solution.api.animation.Easing.EASE_IN_OUT_SINE, 150);

    private float margin;

    @Override
    public void init() {
        updateModuleComponents();
        margin = 10;
        this.x = getClickGUI().getX() + 42;
        this.y = getClickGUI().getY();
        super.init();
    }

    private void updateModuleComponents() {
        String searchText = getSidebar().getSearchText();

        if (!searchText.isEmpty()) {
            moduleComponents = getClickGUI().getModuleList().stream()
                    .filter(component -> {
                        String name = component.getModule().getName().toLowerCase();
                        String desc = component.getModule().getDescription().toLowerCase();
                        return name.contains(searchText) || desc.contains(searchText);
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            moduleComponents = getClickGUI().getModuleList().stream()
                    .filter(c -> c.getModule().getCategory().equals(getCategory()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        for (ModuleComponent component : moduleComponents) {
            component.init();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        this.x = getClickGUI().getX() + 42;
        this.y = getClickGUI().getY();

        updateModuleComponents();

        float guiX = getClickGUI().getX();
        float guiY = getClickGUI().getY();
        float guiW = getClickGUI().getWidth();
        float guiH = getClickGUI().getHeight();

        context.getMatrices().push();
        float transitionValue = getClickGUI().getCategoryTransition().getValue();
        float slideOffset = (1f - transitionValue) * 20f;

        context.enableScissor(
                (int)(guiX + getClickGUI().getSidebar().getWidth()),
                (int)(guiY + 44f - slideOffset),
                (int)(guiX + guiW),
                (int)(guiY + guiH - slideOffset));

        float columnGap = 16f;
        float rowGap = 6f;
        float padL   = 12f;
        float padR   = 20f;
        float sidebarW = getClickGUI().getSidebar().getWidth(); // реальная ширина сайдбара
        float contentW = guiW - sidebarW - padL - padR;
        float colW = (contentW - columnGap) / 2f; // 2 колонки
        float col0X = guiX + sidebarW + padL;
        float col1X = col0X + colW + columnGap;

        // Обновляем ширину всех компонентов под реальную ширину колонки
        for (ModuleComponent component : moduleComponents) {
            component.setWidth(colW);
        }

        float col0H = 0f, col1H = 0f;

        // Считаем реальную высоту каждой колонки (используем полную высоту, игнорируя анимацию)
        for (int i = 0; i < moduleComponents.size(); i++) {
            ModuleComponent comp = moduleComponents.get(i);
            // Берём максимально возможную высоту (expanded=true, anim=1.0)
            float h = comp.getTotalHeightExpanded() + rowGap;
            int col = i % 2;
            if (col == 0) col0H += h;
            else col1H += h;
        }
        // Максимальная высота среди колонок — именно до неё нужно скроллить
        float maxColH = Math.max(col0H, col1H);

        float availableHeight = getClickGUI().getHeight() - 44;
        if (maxColH > availableHeight) {
            scroll.setMax(-(maxColH - availableHeight + 16f));
        } else {
            scroll.setMax(0);
        }

        // Сбрасываем счётчики для рендера
        col0H = 0f; col1H = 0f;

        // Рендерим в 2 колонки
        for (int i = 0; i < moduleComponents.size(); i++) {
            ModuleComponent component = moduleComponents.get(i);
            int col = i % 2;
            float cx, cy;
            if (col == 0) {
                cx = col0X;
                cy = guiY + 44 + rowGap + col0H + scroll.getScroll();
                col0H += component.getTotalHeight() + rowGap;
            } else {
                cx = col1X;
                cy = guiY + 44 + rowGap + col1H + scroll.getScroll();
                col1H += component.getTotalHeight() + rowGap;
            }
            component.render(context, cx, cy, mouseX, mouseY, partialTicks);
        }
        scroll.handle();

        context.disableScissor();
        context.getMatrices().pop();

        // ── Scrollbar (поверх scissor, внутри правого края GUI) ──────────────
        renderScrollbar(context, mouseX, mouseY, maxColH, availableHeight);

        super.render(context, mouseX, mouseY, partialTicks);
    }

    private void renderScrollbar(DrawContext context, int mouseX, int mouseY,
                                  float overallHeight, float availableHeight) {
        if (overallHeight <= availableHeight) return;

        float alpha = getClickGUI().getAlpha().getValue();

        float guiH = getClickGUI().getHeight();
        float guiRightEdge = getClickGUI().getX() + getClickGUI().getWidth();

        float trackX = guiRightEdge - SCROLLBAR_W - SCROLLBAR_GAP;
        float trackY = getClickGUI().getY() + SCROLLBAR_PAD_Y;
        float trackH = guiH - SCROLLBAR_PAD_Y - 6f;

        // Ползунок
        float ratio      = availableHeight / overallHeight;
        float thumbH     = Math.max(20f, trackH * ratio);
        float scrollRange = scroll.getMax();
        float scrollFrac  = scrollRange != 0 ? scroll.getScroll() / scrollRange : 0f;
        float thumbY     = trackY + scrollFrac * (trackH - thumbH);

        // Зона попадания шире чем визуальный ползунок
        boolean hovered = mouseX >= trackX - 6 && mouseX <= trackX + SCROLLBAR_W + 6
                && mouseY >= thumbY && mouseY <= thumbY + thumbH;
        
        // Анимация hover
        scrollbarHoverAnim.run(hovered || scrollbarDragging ? 1 : 0);
        float hoverValue = scrollbarHoverAnim.getValue();

        // Трек — тонкая полупрозрачная линия (показываем только при hover)
        if (hoverValue > 0.01f) {
            blur.render(ShapeProperties.create(context.getMatrices(),
                    trackX, trackY, SCROLLBAR_W, trackH)
                    .round(SCROLLBAR_W / 2f)
                    .color(new farvix.solution.api.util.color.FixColor(255, 255, 255, 
                            (int)(20 * alpha * hoverValue)).getRGB())
                    .build());
        }

        // Ползунок с акцентным цветом темы
        java.awt.Color accentColor = farvix.solution.client.managers.ThemeManager.getInstance()
                .getCurrentTheme().getAccentColor();
        
        // Базовый цвет - серый, при hover - акцентный цвет темы
        int baseR = 160, baseG = 170, baseB = 190;
        int targetR = accentColor.getRed();
        int targetG = accentColor.getGreen();
        int targetB = accentColor.getBlue();
        
        int thumbR = (int)(baseR + (targetR - baseR) * hoverValue);
        int thumbG = (int)(baseG + (targetG - baseG) * hoverValue);
        int thumbB = (int)(baseB + (targetB - baseB) * hoverValue);
        int thumbAlpha = (int)((150 + 80 * hoverValue) * alpha);
        
        int thumbColor = new farvix.solution.api.util.color.FixColor(thumbR, thumbG, thumbB, thumbAlpha).getRGB();

        // Ширина ползунка увеличивается при hover
        float thumbWidth = SCROLLBAR_W + hoverValue * 1.5f;
        float thumbXOffset = (SCROLLBAR_W - thumbWidth) / 2f;

        blur.render(ShapeProperties.create(context.getMatrices(),
                trackX + thumbXOffset, thumbY, thumbWidth, thumbH)
                .round(thumbWidth / 2f)
                .color(thumbColor)
                .build());
    }

    // ── Mouse events ──────────────────────────────────────────────────────────

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float guiH = getClickGUI().getHeight();
            float guiRightEdge = getClickGUI().getX() + getClickGUI().getWidth();
            float trackX = guiRightEdge - SCROLLBAR_W - SCROLLBAR_GAP;
            float trackY = getClickGUI().getY() + SCROLLBAR_PAD_Y;
            float trackH = guiH - SCROLLBAR_PAD_Y - 6f;

            float overallHeight = getMaxColumnHeight(false);
            float availableHeight = guiH - 44;

            if (overallHeight > availableHeight) {
                float ratio  = availableHeight / overallHeight;
                float thumbH = Math.max(16f, trackH * ratio);
                float scrollFrac = scroll.getMax() != 0 ? scroll.getScroll() / scroll.getMax() : 0f;
                float thumbY = trackY + scrollFrac * (trackH - thumbH);

                if (mouseX >= trackX - 2 && mouseX <= trackX + SCROLLBAR_W + 4
                        && mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                    scrollbarDragging        = true;
                    scrollbarDragStartY      = mouseY;
                    scrollbarDragStartScroll = scroll.getScroll();
                    return;
                }

                // Клик по треку вне ползунка — плавно прокручиваем к позиции
                if (mouseX >= trackX - 2 && mouseX <= trackX + SCROLLBAR_W + 4
                        && mouseY >= trackY && mouseY <= trackY + trackH) {
                    float clickFrac = (float) ((mouseY - trackY) / trackH);
                    scroll.setTargetScroll(clickFrac * scroll.getMax());
                    return;
                }
            }
        }

        for (int i = moduleComponents.size() - 1; i >= 0; i--) {
            moduleComponents.get(i).mouseClicked(mouseX, mouseY, button);
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) scrollbarDragging = false;

        for (int i = moduleComponents.size() - 1; i >= 0; i--) {
            moduleComponents.get(i).mouseReleased(mouseX, mouseY, button);
        }
        super.mouseReleased(mouseX, mouseY, button);
    }

    public void mouseDragged(double mouseX, double mouseY, int button) {
        if (scrollbarDragging && button == 0) {
            float guiH = getClickGUI().getHeight();
            float trackH = guiH - SCROLLBAR_PAD_Y - 6f;

            float overallHeight = getMaxColumnHeight(false);
            float availableHeight = guiH - 44;

            if (overallHeight > availableHeight) {
                float ratio  = availableHeight / overallHeight;
                float thumbH = Math.max(16f, trackH * ratio);
                float movable = trackH - thumbH;

                double delta = mouseY - scrollbarDragStartY;
                float newFrac = (float)(delta / movable);
                float newScroll = scrollbarDragStartScroll + newFrac * scroll.getMax();
                
                // Используем setTargetScroll для плавной анимации при перетаскивании
                scroll.setTargetScroll(Math.max(scroll.getMax(), Math.min(0, newScroll)));
            }
            return;
        }

        for (int i = moduleComponents.size() - 1; i >= 0; i--) {
            moduleComponents.get(i).mouseDragged(mouseX, mouseY, button);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleComponent component : moduleComponents) {
            component.keyPressed(keyCode, scanCode, modifiers);
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double delta) {
        super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (ModuleComponent component : moduleComponents) {
            component.charTyped(codePoint, modifiers);
        }
        super.charTyped(codePoint, modifiers);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    
    private float getMaxColumnHeight(boolean expanded) {
        float rowGap = 6f;
        float col0H = 0f, col1H = 0f;
        for (int i = 0; i < moduleComponents.size(); i++) {
            ModuleComponent comp = moduleComponents.get(i);
            float h = (expanded ? comp.getTotalHeightExpanded() : comp.getTotalHeight()) + rowGap;
            int col = i % 2;
            if (col == 0) col0H += h;
            else col1H += h;
        }
        return Math.max(col0H, col1H);
    }

    private ModuleCategory getCategory() {
        for (ModuleCategory cat : ModuleCategory.values()) {
            if (cat.getScreen() == this.getClickGUI().getCurrentScreen()) return cat;
        }
        return null;
    }

    private InterfaceScreen getClickGUI() {
        return (InterfaceScreen) mc.currentScreen;
    }

    private farvix.solution.api.ui.clickgui.impl.sidebar.Sidebar getSidebar() {
        return getClickGUI().getSidebar();
    }
}
