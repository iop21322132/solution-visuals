package farvix.solution.api;

import lombok.Getter;
import farvix.solution.api.util.color.FixColor;

public class TempColor {

    // Accent color (default client color)
    private static final FixColor BLUE_ACCENT = new FixColor(200, 200, 200); // White accent instead of blue

    // GUI colors
    private static FixColor GUI_BACKGROUND_MUT     = new FixColor(10, 10, 10, 242); // Black main
    private static FixColor SIDEBAR_BACKGROUND_MUT = new FixColor(18, 18, 18, 253); // Sidebar
    private static FixColor MODULE_BACKGROUND_MUT  = new FixColor(0, 0, 0, 224);    // Module buttons
    private static FixColor SEARCH_BACKGROUND_MUT  = new FixColor(0, 0, 0, 250);    // Search

    // Borders
    private static FixColor GUI_BORDER_MUT             = new FixColor(50, 50, 50, 0);
    private static FixColor SEARCH_BORDER_UNFOCUSED_MUT = new FixColor(50, 50, 50, 0);
    private static FixColor MODULE_BORDER_MUT          = new FixColor(50, 50, 50, 0);
    private static FixColor TAB_BORDER_HOVER_MUT       = new FixColor(50, 50, 50, 0);

    // Text
    private static FixColor TEXT_PRIMARY     = new FixColor(240, 240, 240, 255); // Almost white
    private static FixColor TEXT_SECONDARY   = new FixColor(160, 160, 160, 255); // Grey
    private static final FixColor TEXT_HOVER       = new FixColor(255, 255, 255, 255); // White
    private static final FixColor TEXT_PLACEHOLDER = new FixColor(100, 100, 100, 120); // Dark grey
    private static final FixColor TEXT_MENU        = new FixColor(180, 180, 180, 180); // Grey
    private static final FixColor TEXT_COMPONENT   = new FixColor(160, 160, 160, 160); // Grey

    // Separators
    private static final FixColor SEPARATOR_HORIZONTAL = new FixColor(80, 80, 80, 40);
    private static final FixColor SEPARATOR_VERTICAL   = new FixColor(80, 80, 80, 40);

    // Avatar / status
    private static final FixColor AVATAR_COLOR = new FixColor(30, 80, 200, 255);
    private static final FixColor STATUS_COLOR = new FixColor(60, 220, 100, 255);

    // Keys
    private static final FixColor KEY_BACKGROUND = new FixColor(25, 25, 25, 160);
    private static final FixColor KEY_BORDER     = new FixColor(80, 80, 80, 80);
    private static final FixColor KEY_TEXT       = new FixColor(200, 200, 200, 255);

    // Cursor
    private static final FixColor CURSOR_COLOR = new FixColor(200, 200, 200, 255);

    @Getter private static FixColor clientColor;

    @Getter private static FixColor guiBackground     = GUI_BACKGROUND_MUT;
    @Getter private static FixColor sidebarBackground = SIDEBAR_BACKGROUND_MUT;
    @Getter private static FixColor moduleBackground  = MODULE_BACKGROUND_MUT;
    @Getter private static FixColor searchBackground  = SEARCH_BACKGROUND_MUT;

    @Getter private static FixColor guiBorder             = GUI_BORDER_MUT;
    @Getter private static FixColor searchBorderUnfocused = SEARCH_BORDER_UNFOCUSED_MUT;
    @Getter private static FixColor moduleBorder          = MODULE_BORDER_MUT;
    @Getter private static FixColor tabBorderHover        = TAB_BORDER_HOVER_MUT;

    @Getter private static FixColor textPrimary   = TEXT_PRIMARY;
    @Getter private static FixColor textSecondary = TEXT_SECONDARY;
    @Getter private static final FixColor textHover       = TEXT_HOVER;
    @Getter private static final FixColor textPlaceholder = TEXT_PLACEHOLDER;
    @Getter private static final FixColor textMenu        = TEXT_MENU;
    @Getter private static final FixColor textComponent   = TEXT_COMPONENT;

    @Getter private static final FixColor separatorHorizontal = SEPARATOR_HORIZONTAL;
    @Getter private static final FixColor separatorVertical   = SEPARATOR_VERTICAL;

    @Getter private static final FixColor avatarColor = AVATAR_COLOR;
    @Getter private static final FixColor statusColor = STATUS_COLOR;

    @Getter private static final FixColor keyBackground = KEY_BACKGROUND;
    @Getter private static final FixColor keyBorder     = KEY_BORDER;
    @Getter private static final FixColor keyText       = KEY_TEXT;

    @Getter private static final FixColor cursorColor = CURSOR_COLOR;

    static {
        updateColors();
        GUI_BACKGROUND_MUT     = new FixColor(10, 10, 10, 242);
        SIDEBAR_BACKGROUND_MUT = new FixColor(18, 18, 18, 253);
        MODULE_BACKGROUND_MUT  = new FixColor(0, 0, 0, 224);
        SEARCH_BACKGROUND_MUT  = new FixColor(0, 0, 0, 250);
    }

    public static void updateColors() {
        clientColor = BLUE_ACCENT;
    }

    public static void setClientColor(FixColor color) {
        clientColor = color;
    }

    public static void setThemeBackground(java.awt.Color color) {
        setThemeBackground(color, color);
    }

    public static void setThemeBackground(java.awt.Color color, java.awt.Color accentColor) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        java.awt.Color finalAccent = accentColor;
        if (accentColor.getRed() == 15 && accentColor.getGreen() == 15 && accentColor.getBlue() == 15) {
            finalAccent = new java.awt.Color(220, 220, 220); // Bright white-silver
        } else if (accentColor.getRed() == 40 && accentColor.getGreen() == 120 && accentColor.getBlue() == 220) {
            finalAccent = new java.awt.Color(80, 170, 255); // Bright steel blue
        } else if (accentColor.getRed() == 148 && accentColor.getGreen() == 36 && accentColor.getBlue() == 255) {
            finalAccent = new java.awt.Color(190, 90, 255); // Bright amethyst purple
        }

        clientColor = new FixColor(finalAccent.getRed(), finalAccent.getGreen(), finalAccent.getBlue());
        
        java.awt.Color[] presetColors = {
            new java.awt.Color(15,  15,  15),
            new java.awt.Color(40,  120, 220),
            new java.awt.Color(148, 36,  255),
            new java.awt.Color(0,   210, 140),
            new java.awt.Color(220, 30,  60)
        };

        boolean isPreset = false;
        for (java.awt.Color presetColor : presetColors) {
            if (presetColor.getRed() == r && presetColor.getGreen() == g && presetColor.getBlue() == b) {
                isPreset = true;
                break;
            }
        }

        boolean isCustom = farvix.solution.client.managers.ThemeManager.getInstance().getCurrentTheme().getName().equalsIgnoreCase("Custom");
        float factor = isCustom ? 0.35f : 1.0f;
        int cr = (int)(r * factor);
        int cg = (int)(g * factor);
        int cb = (int)(b * factor);

        if (isPreset) {
            int bgR = Math.max(5, (int)(r * 0.18f));
            int bgG = Math.max(5, (int)(g * 0.18f));
            int bgB = Math.max(5, (int)(b * 0.18f));
            guiBackground = new FixColor(bgR, bgG, bgB, 242);
            
            int sbR = Math.max(3, (int)(r * 0.14f));
            int sbG = Math.max(3, (int)(g * 0.14f));
            int sbB = Math.max(3, (int)(b * 0.14f));
            sidebarBackground = new FixColor(sbR, sbG, sbB, 253);
            
            int mobR = Math.max(3, (int)(r * 0.12f));
            int mobG = Math.max(3, (int)(g * 0.12f));
            int mobB = Math.max(3, (int)(b * 0.12f));
            moduleBackground  = new FixColor(mobR, mobG, mobB, 224);
            
            int shR = Math.max(2, (int)(r * 0.08f));
            int shG = Math.max(2, (int)(g * 0.08f));
            int shB = Math.max(2, (int)(b * 0.08f));
            searchBackground = new FixColor(shR, shG, shB, 250);
        } else {
            guiBackground = new FixColor(cr, cg, cb, 242);
            sidebarBackground = new FixColor(Math.max(0, cr - 20), Math.max(0, cg - 20), Math.max(0, cb - 20), 253);
            moduleBackground  = new FixColor(cr, cg, cb, 224);
            searchBackground = new FixColor(Math.max(0, cr - 30), Math.max(0, cg - 30), Math.max(0, cb - 30), 250);
        }

        // Adjust text brightness based on cr, cg, cb
        float colorBrightness = (cr * 0.299f + cg * 0.587f + cb * 0.114f) / 255f;
        if (colorBrightness > 0.5f) {
            textPrimary   = new FixColor(20, 20, 20, 255);
            textSecondary = new FixColor(80, 80, 80, 255);
        } else {
            textPrimary   = new FixColor(240, 240, 240, 255);
            textSecondary = new FixColor(160, 160, 160, 255);
        }

        guiBorder             = new FixColor(50, 50, 50, 0);
        searchBorderUnfocused = new FixColor(50, 50, 50, 0);
        moduleBorder          = new FixColor(50, 50, 50, 0);
        tabBorderHover        = new FixColor(50, 50, 50, 0);
    }
    
    public static void resetThemeBackground() {
        clientColor       = BLUE_ACCENT;
        guiBackground     = new FixColor(10, 10, 10, 242);
        sidebarBackground = new FixColor(18, 18, 18, 253);
        moduleBackground  = new FixColor(0, 0, 0, 224);
        searchBackground  = new FixColor(0, 0, 0, 250);
        textPrimary   = new FixColor(240, 240, 240, 255);
        textSecondary = new FixColor(160, 160, 160, 255);
        guiBorder             = new FixColor(80, 80, 80, 0);
        searchBorderUnfocused = new FixColor(80, 80, 80, 0);
        moduleBorder          = new FixColor(80, 80, 80, 0);
        tabBorderHover        = new FixColor(80, 80, 80, 0);
    }
}
