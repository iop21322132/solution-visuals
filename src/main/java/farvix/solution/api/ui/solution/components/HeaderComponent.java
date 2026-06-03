package farvix.solution.api.ui.solution.components;

import net.minecraft.client.gui.DrawContext;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.ui.solution.SolutionGuiScreen;
import farvix.solution.api.ui.solution.utils.SolutionColors;

/**
 * Компонент заголовка с логотипом "⚡ SolutionVisual"
 */
public class HeaderComponent implements QuickImports {
    
    private final SolutionGuiScreen parent;
    
    // Размеры
    private static final float HEADER_HEIGHT = 40f;
    
    // Текст логотипа
    private static final String LOGO_TEXT = "SolutionVisual";
    private static final String LOGO_ICON = "⚡"; // Иконка молнии
    
    public HeaderComponent(SolutionGuiScreen parent) {
        this.parent = parent;
    }
    
    public void init() {
        // Инициализация не требуется
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float alpha = parent.getAlpha();
        if (alpha <= 0.01f) return;
        
        float guiX = parent.getX();
        float guiY = parent.getY();
        float guiWidth = parent.getWidth();
        
        // ═══════════════════════════════════════════════════════════════════
        // ПОЗИЦИЯ ЗАГОЛОВКА (ВЫШЕ GUI, как в референсе)
        // ═══════════════════════════════════════════════════════════════════
        float headerY = guiY - 30; // 30px выше GUI
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ ИКОНКИ МОЛНИИ
        // ═══════════════════════════════════════════════════════════════════
        float iconSize = 18f;
        float textWidth = Fonts.SEMIBOLD.get(18).getStringWidth(LOGO_TEXT);
        float iconWidth = Fonts.SEMIBOLD.get((int)iconSize).getStringWidth(LOGO_ICON);
        float totalWidth = iconWidth + 5 + textWidth; // 5px отступ между иконкой и текстом
        
        float startX = guiX + (guiWidth - totalWidth) / 2f;
        
        // Иконка молнии (фиолетовый акцент)
        Fonts.SEMIBOLD.get((int)iconSize).drawString(
                context.getMatrices(),
                LOGO_ICON,
                startX,
                headerY - iconSize / 2f + 1,
                SolutionColors.withAlpha(SolutionColors.ACCENT_PRIMARY, alpha).getRGB()
        );
        
        // ═══════════════════════════════════════════════════════════════════
        // РЕНДЕРИНГ ТЕКСТА "SolutionVisual"
        // ═══════════════════════════════════════════════════════════════════
        float textX = startX + iconWidth + 5;
        
        Fonts.SEMIBOLD.get(18).drawString(
                context.getMatrices(),
                LOGO_TEXT,
                textX,
                headerY - 18 / 2f + 1,
                SolutionColors.withAlpha(SolutionColors.TEXT_PRIMARY, alpha).getRGB()
        );
        
        // Разделительная линия убрана - заголовок теперь снаружи GUI
    }
    
    /**
     * Получить высоту заголовка
     */
    public static float getHeight() {
        return HEADER_HEIGHT;
    }
}

