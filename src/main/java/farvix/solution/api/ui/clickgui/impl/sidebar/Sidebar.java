package farvix.solution.api.ui.clickgui.impl.sidebar;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector4f;
import farvix.solution.api.TempColor;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.ui.clickgui.InterfaceScreen;
import farvix.solution.api.ui.clickgui.api.MenuComponent;
import farvix.solution.api.ui.clickgui.components.SearchBar;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import net.minecraft.util.Identifier;
import farvix.solution.api.util.render.Render2D;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class Sidebar extends MenuComponent {
    

    private static final float PADDING = 5f;
    private static final float ICON_TOP_MARGIN = 10f;
    private static final float ICON_BOTTOM_MARGIN = 15f;
    private static final float SEPARATOR_BOTTOM_MARGIN = 20f;
    private static final float TAB_GAP = 22f;
    private static final float COMPONENT_TEXT_OFFSET = 9f;


    private static final float USER_INFO_HEIGHT_OFFSET = 28f; // Увеличено для лучшего размещения крупного текста
    private static final float AVATAR_SIZE = 16f; // Оригинальный размер аватара
    private static final float AVATAR_X_OFFSET = 7.5f;
    private static final float AVATAR_Y_OFFSET = 2.5f;
    private static final float STATUS_INDICATOR_SIZE = 4f; // Оригинальный размер индикатора




    private static final String USERNAME = "Solution Visual";
    private static final String USER_UID = "UID: 1";

    private final List<ModuleCategory> categories;
    private final Map<ModuleCategory, Animation> categoryHoverAnimations = new HashMap<>();
    private final Map<ModuleCategory, Animation> categoryActiveAnimations = new HashMap<>();
    private float iconTopMargin;


    private SearchBar searchBar;

    // Режимы ClickGUI и анимации для средней кнопки
    public static boolean extendedMode = true;
    private final Animation middleHoverAnim = new Animation(Easing.EASE_IN_OUT_SINE, 150);
    private final Animation middleClickAnim = new Animation(Easing.EASE_IN_OUT_SINE, 200);

    // Анимация появления описания модуля
    private final Animation descAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180);
    private String currentDesc = "";

    public Sidebar() {
        this.categories = Arrays.stream(ModuleCategory.values())
                .collect(Collectors.toList());
        
        // Инициализируем анимации для каждой категории
        for (ModuleCategory category : categories) {
            categoryHoverAnimations.put(category, new Animation(Easing.EASE_OUT_CUBIC, 200));
            categoryActiveAnimations.put(category, new Animation(Easing.EASE_OUT_CUBIC, 200));
        }
    }

    @Override
    public void init() {
        width = 42f; // Sleek narrow width
        height = getClickGUI().getHeight();
        this.x = getClickGUI().getX();
        this.y = getClickGUI().getY();

        iconTopMargin = ICON_TOP_MARGIN;

        float searchBarW = 135f;
        float searchBarH = 21f;
        float searchBarX = getClickGUI().getX() + getClickGUI().getWidth() - searchBarW - 12f;
        float searchBarY = getClickGUI().getY() + 8f;
        if (searchBar == null) {
            searchBar = new SearchBar(searchBarX, searchBarY, searchBarW, searchBarH);
        } else {
            searchBar.setX(searchBarX);
            searchBar.setY(searchBarY);
            searchBar.setWidth(searchBarW);
            searchBar.setHeight(searchBarH);
        }

        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        // Sync position to GUI window
        this.x = getClickGUI().getX();
        this.y = getClickGUI().getY();
        this.height = getClickGUI().getHeight();

        renderBackground(context);
        renderHeader(context, mouseX, mouseY);
        renderTabs(context, mouseX, mouseY);
        renderSearchBar(context, mouseX, mouseY, partialTicks);

        super.render(context, mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBar != null) {
            searchBar.mouseClicked(mouseX, mouseY, button);
        }

        // Клик по средней кнопке режима ClickGUI
        float searchBarW = 135f;
        float searchBarX = getClickGUI().getX() + getClickGUI().getWidth() - searchBarW - 12f;
        float middleW = 135f;
        float middleX = searchBarX - 10f - middleW;
        float middleY = y + 8f;
        float middleH = 21f;

        if (mouseX >= middleX && mouseX <= middleX + middleW && mouseY >= middleY && mouseY <= middleY + middleH && button == 0) {
            extendedMode = !extendedMode;
            middleClickAnim.setValue(1f);
            farvix.solution.client.modules.Module.playClickSound2();
            return;
        }
        
        float startY = y + 42f;
        float gap = 30f;

        for (int i = 0; i < categories.size(); i++) {
            ModuleCategory category = categories.get(i);
            float tabY = startY + i * gap;
            handleTabClick(mouseX, mouseY, button, category, tabY);
        }

        super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBar != null) {
            searchBar.keyPressed(keyCode, scanCode, modifiers);
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (searchBar != null) {
            searchBar.charTyped(codePoint, modifiers);
        }
        super.charTyped(codePoint, modifiers);
    }

    private InterfaceScreen getClickGUI() {
        return (InterfaceScreen) mc.currentScreen;
    }
    
    public String getSearchText() {
        return searchBar != null ? searchBar.getSearchText() : "";
    }
    
    public boolean isSearchActive() {
        return searchBar != null && !searchBar.isEmpty();
    }
    
    public void focusSearch() {
        if (searchBar != null) {
            searchBar.setFocused(true);
        }
    }
    
    public boolean isSearchFocused() {
        return searchBar != null && searchBar.isFocused();
    }

    private void renderTab(DrawContext context, float x, float y, int mouseX, int mouseY, ModuleCategory category) {
        boolean isActive = getClickGUI().getCurrentScreen().equals(category.getScreen());
        float alpha = getClickGUI().getAlpha().getValue();
        
        float tabX = x + 8f;
        float tabY = y - 1.25f;
        float tabWidth = 26f;
        float tabHeight = 28.5f;
        boolean isHovered = isHovered(tabX, tabY, tabWidth, tabHeight, mouseX, mouseY);
        
        Animation hoverAnim = categoryHoverAnimations.get(category);
        Animation activeAnim = categoryActiveAnimations.get(category);
        
        hoverAnim.run(isHovered ? 1 : 0);
        activeAnim.run(isActive ? 1 : 0);
        
        float hoverValue = hoverAnim.getValue();
        float activeValue = activeAnim.getValue();
        
        if (alpha > 0.05f) {
            // Draw active/hover background
            int bgColor = 0;
            if (isActive) {
                bgColor = new FixColor(255, 255, 255, (int)(255 * alpha * activeValue)).getRGB();
            } else if (hoverValue > 0.01f) {
                bgColor = new FixColor(255, 255, 255, (int)(25 * alpha * hoverValue)).getRGB();
            }
            
            if (bgColor != 0) {
                rectangle.render(ShapeProperties.create(context.getMatrices(), tabX, tabY, tabWidth, tabHeight)
                        .round(8f)
                        .softness(1.5f)
                        .thickness(0)
                        .outlineColor(0)
                        .color(bgColor)
                        .build());
            }

            String icon = category.getIcon();
            
            // Icon color: black if active, white/grey if inactive
            int iconColor;
            if (isActive) {
                iconColor = new FixColor(15, 15, 15, (int)(255 * alpha)).getRGB();
            } else {
                int r = (int)(150 + 75 * hoverValue);
                int g = (int)(150 + 75 * hoverValue);
                int b = (int)(150 + 75 * hoverValue);
                int a = (int)((160 + 80 * hoverValue) * alpha);
                iconColor = new FixColor(r, g, b, a).getRGB();
            }
            
            boolean isEmoji = icon.codePointAt(0) > 0xFFFF ||
                              (icon.codePointAt(0) >= 0x1F300 && icon.codePointAt(0) <= 0x1FAFF);
                              
            if (icon.equals("player")) {
                float cx = tabX + tabWidth / 2f;
                float cy = tabY + tabHeight / 2f;
                int actualColor = iconColor;
                
                // Head
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 2f, cy - 4.5f, 4f, 4f)
                        .round(2f)
                        .color(actualColor)
                        .build());
                        
                // Shoulders / Torso
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 4f, cy + 0.5f, 8f, 4.5f)
                        .round(2.25f)
                        .color(actualColor)
                        .build());
            } else if (icon.equals("⌨")) {
                float cx = tabX + tabWidth / 2f;
                float cy = tabY + tabHeight / 2f;
                int actualColor = iconColor;
                
                // Main Keyboard Frame Outline (12.5x8.6 centered, 3f thickness)
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 6.25f, cy - 4.3f, 12.5f, 8.6f)
                        .round(2.5f)
                        .thickness(3f)
                        .outlineColor(actualColor)
                        .color(0)
                        .build());
                        
                // 5 Minimalist Keys (3 at the bottom, 2 at the top)
                // Top Row (2 keys)
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 2.4f, cy - 2.3f, 2f, 1.8f)
                        .round(0.75f)
                        .color(actualColor)
                        .build());
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx + 0.4f, cy - 2.3f, 2f, 1.8f)
                        .round(0.75f)
                        .color(actualColor)
                        .build());
                        
                // Bottom Row (3 keys)
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 3.8f, cy + 0.5f, 2f, 1.8f)
                        .round(0.75f)
                        .color(actualColor)
                        .build());
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 1.0f, cy + 0.5f, 2f, 1.8f)
                        .round(0.75f)
                        .color(actualColor)
                        .build());
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx + 1.8f, cy + 0.5f, 2f, 1.8f)
                        .round(0.75f)
                        .color(actualColor)
                        .build());
            } else if (icon.equals("🌎")) {
                float cx = tabX + tabWidth / 2f;
                float cy = tabY + tabHeight / 2f;
                int actualColor = iconColor;
                
                // Waypoint Map Pin
                // Pin stem
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 0.75f, cy - 0.5f, 1.5f, 5f)
                        .round(0.75f)
                        .color(actualColor)
                        .build());
                // Pin head
                rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 2.5f, cy - 4.5f, 5f, 5f)
                        .round(2.5f)
                        .color(actualColor)
                        .build());
            } else if (isEmoji) {
                net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
                if (mc2.textRenderer != null) {
                    float emojiW = mc2.textRenderer.getWidth(icon);
                    float emojiH = mc2.textRenderer.fontHeight;
                    float emojiX = tabX + (tabWidth - emojiW) / 2f;
                    if (icon.equals("🌎")) {
                        emojiX -= 0.5f; // Сдвигаем на 0.5 пикселя влево для точного центрирования
                    }
                    float emojiY = tabY + (tabHeight - emojiH) / 2f;
                    context.drawText(mc2.textRenderer,
                            net.minecraft.text.Text.literal(icon),
                            (int)(emojiX), (int)(emojiY),
                            iconColor, false);
                }
            } else {
                float iconW = Fonts.ICONS.get(21).getStringWidth(icon);
                float iconH = Fonts.ICONS.get(21).getStringHeight(icon);
                float iconX = tabX + (tabWidth - iconW) / 2f;
                // Add vertical font baseline offset
                float iconY = tabY + (tabHeight - iconH) / 2f + 7f;
                Fonts.ICONS.get(21).drawString(context.getMatrices(), icon, iconX, iconY, iconColor);
            }
        }
    }

    private void handleTabClick(double mouseX, double mouseY, int button, ModuleCategory category, float tabY) {
        float tabX = x + 8f;
        float tabWidth = 26f;
        float tabHeight = 26f;

        if (isHovered(tabX, tabY, tabWidth, tabHeight, mouseX, mouseY) && button == 0) {
            getClickGUI().switchScreen(category);
        }
    }

    protected boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void renderBackground(DrawContext context) {
        float alpha = getClickGUI().getAlpha().getValue();
        // Sidebar background - slightly darker than main content obsidian
        int sidebarBgColor = TempColor.getSidebarBackground().alpha(alpha).getRGB();
        blur.render(ShapeProperties.create(context.getMatrices(), x, y + 0.5f, width, height - 1.0f)
                .round(new Vector4f(0, 0, getClickGUI().round, getClickGUI().round)) // round top-left and bottom-left only
                .color(sidebarBgColor)
                .build());

        // Subtle vertical separator line between sidebar and main area
        rectangle.render(ShapeProperties.create(context.getMatrices(), x + width - 0.5f, y + 0.5f, 0.5f, height - 1.0f)
                .round(0)
                .color(new FixColor(255, 255, 255, (int)(15 * alpha)).getRGB())
                .build());
    }

    private void renderHeader(DrawContext context, int mouseX, int mouseY) {
        float alpha = getClickGUI().getAlpha().getValue();
        if (alpha <= 0.05f) return;

        ModuleCategory cat = getCategory();
        if (cat == null) return;

        // Draw category badge (left box) - same size as search bar
        float searchBarW = 135f;
        float searchBarX = getClickGUI().getX() + getClickGUI().getWidth() - searchBarW - 12f;

        float middleW = 135f;
        float middleX = searchBarX - 10f - middleW;
        float middleY = y + 8f;
        float middleH = 21f;

        float badgeX = x + width + 12f;
        float badgeY = middleY;
        float badgeH = middleH;
        float badgeW = middleX - 10f - badgeX;

        // Badge background (no border, round 4, matching search background)
        blur.render(ShapeProperties.create(context.getMatrices(), badgeX, badgeY, badgeW, badgeH)
                .round(4)
                .softness(1f)
                .color(TempColor.getSearchBackground().alpha(alpha).getRGB())
                .build());

        // Draw Category Icon (size 14, matching search icon size)
        String icon = cat.getIcon();
        int iconColor = new FixColor(255, 255, 255, (int)(220 * alpha)).getRGB();
        float iconW = 0f;

        boolean isEmoji = icon.codePointAt(0) > 0xFFFF ||
                          (icon.codePointAt(0) >= 0x1F300 && icon.codePointAt(0) <= 0x1FAFF);

        float cx = badgeX + 8f + 5.5f;
        float cy = badgeY + badgeH / 2f;

        if (icon.equals("player")) {
            iconW = 11f;
            // Scale-down player shape for size 14
            // Head
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 1.5f, cy - 3.5f, 3f, 3f)
                    .round(1.5f)
                    .color(iconColor)
                    .build());
            // Shoulders
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 3f, cy + 0.5f, 6f, 3.5f)
                    .round(1.5f)
                    .color(iconColor)
                    .build());
        } else if (icon.equals("⌨")) {
            iconW = 11f;
            // Scale-down keyboard shape for size 14
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 4.5f, cy - 3f, 9f, 6f)
                    .round(2f)
                    .thickness(2f)
                    .outlineColor(iconColor)
                    .color(0)
                    .build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 2f, cy - 1.5f, 1.5f, 1.5f)
                    .round(0.5f)
                    .color(iconColor)
                    .build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx + 0.5f, cy - 1.5f, 1.5f, 1.5f)
                    .round(0.5f)
                    .color(iconColor)
                    .build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 2.8f, cy + 0.8f, 1.5f, 1.5f)
                    .round(0.5f)
                    .color(iconColor)
                    .build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 0.7f, cy + 0.8f, 1.5f, 1.5f)
                    .round(0.5f)
                    .color(iconColor)
                    .build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx + 1.4f, cy + 0.8f, 1.5f, 1.5f)
                    .round(0.5f)
                    .color(iconColor)
                    .build());
        } else if (icon.equals("🌎")) {
            iconW = 11f;
            // Scale-down map pin shape for size 14
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 0.5f, cy - 0.5f, 1f, 3.5f)
                    .round(0.5f)
                    .color(iconColor)
                    .build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), cx - 2f, cy - 3.5f, 4f, 4f)
                    .round(2f)
                    .color(iconColor)
                    .build());
        } else if (isEmoji) {
            net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
            if (mc2.textRenderer != null) {
                iconW = mc2.textRenderer.getWidth(icon);
                float emojiH = mc2.textRenderer.fontHeight;
                float emojiX = badgeX + 8f;
                float emojiY = badgeY + (badgeH - emojiH) / 2f;
                context.drawText(mc2.textRenderer,
                        net.minecraft.text.Text.literal(icon),
                        (int)(emojiX), (int)(emojiY),
                        iconColor, false);
            }
        } else {
            iconW = 11f;
            float actualIconW = Fonts.ICONS.get(14).getStringWidth(icon);
            float iconH = Fonts.ICONS.get(14).getStringHeight(icon);
            float iconX = badgeX + 13.5f - actualIconW / 2f;
            float iconY = badgeY + (badgeH - iconH) / 2f - 0.5f + 7f;
            Fonts.ICONS.get(14).drawString(context.getMatrices(), icon, iconX, iconY, iconColor);
        }

        // Draw separator and category name using DEFAULT 16 bold font (matching search bar text exactly!)
        float textX = badgeX + 8f + iconW + 6f;
        float textY = badgeY + badgeH / 2f - 2f;
        String badgeText = ">>  " + cat.getDisplayName();

        Fonts.DEFAULT.get(16).drawBoldString(context.getMatrices(), badgeText,
                textX, textY,
                new FixColor(255, 255, 255, (int)(220 * alpha)).getRGB());

        // ── Draw New Middle Box (menu) ───────────────────────────────────────
        boolean midHov = isHovered(middleX, middleY, middleW, middleH, mouseX, mouseY);
        middleHoverAnim.run(midHov ? 1f : 0f);
        middleClickAnim.run(0f);

        float middleGlow = middleHoverAnim.getValue();
        float clickVal = middleClickAnim.getValue();
        float buttonScale = 1.0f - 0.04f * clickVal;

        // Draw glow effect under the middle button
        if (middleGlow > 0.01f || clickVal > 0.01f) {
            float glowAlpha = alpha * (middleGlow * 0.12f + clickVal * 0.15f);
            blur.render(ShapeProperties.create(context.getMatrices(), 
                    middleX - 1.5f, middleY - 1.5f, middleW + 3, middleH + 3)
                    .round(5)
                    .softness(2f)
                    .color(TempColor.getClientColor().alpha(glowAlpha).getRGB())
                    .build());
        }

        context.getMatrices().push();
        float midCX = middleX + middleW / 2f;
        float midCY = middleY + middleH / 2f;
        context.getMatrices().translate(midCX, midCY, 0f);
        context.getMatrices().scale(buttonScale, buttonScale, 1.0f);
        context.getMatrices().translate(-midCX, -midCY, 0f);

        // Border color highlights on hover / click
        java.awt.Color midBorderColor = midHov || clickVal > 0.01f ?
            TempColor.getClientColor().alpha(alpha * (0.35f + 0.35f * middleGlow + 0.3f * clickVal)).getColor() :
            TempColor.getSearchBorderUnfocused().alpha(alpha).getColor();

        blur.render(ShapeProperties.create(context.getMatrices(), middleX - 0.5f, middleY - 0.5f, middleW + 1, middleH + 1)
                .round(4)
                .softness(1.5f)
                .thickness(1.25f + middleGlow * 0.25f)
                .outlineColor(midBorderColor.getRGB())
                .color(TempColor.getSearchBackground().alpha(alpha).getRGB())
                .build());

        // Draw middle text (using same 16 bold font, 1.5x fatter)
        String modeText = extendedMode ? "Расширенный" : "Упрощенный";
        float midTextW = Fonts.DEFAULT.get(16).getStringWidth(modeText);
        float midTextX = middleX + (middleW - midTextW) / 2f;
        float midTextY = middleY + middleH / 2f - 2f;

        // Draw text 1.5x bolder by offset overlay
        int textCol = new FixColor(255, 255, 255, (int)((220 + 35 * clickVal) * alpha)).getRGB();
        Fonts.DEFAULT.get(16).drawBoldString(context.getMatrices(), modeText, midTextX, midTextY, textCol);
        Fonts.DEFAULT.get(16).drawBoldString(context.getMatrices(), modeText, midTextX + 0.5f, midTextY, textCol);

        context.getMatrices().pop();
    }

    private void drawPlayerHead(DrawContext ctx, float x, float y, float size, float alpha) {
        Identifier tex;
        if (mc.player != null) {
            tex = mc.player.getSkinTextures().texture();
        } else {
            tex = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
        }

        int fullAlpha = (int)(255 * alpha);
        int rgb = new FixColor(255, 255, 255, fullAlpha).getRGB();

        float S = 64f;
        float u0 = 8/S, v0 = 8/S, u1 = 16/S, v1 = 16/S;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);

        var tess = Tessellator.getInstance();
        var buf  = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        org.joml.Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        buf.vertex(m, x,        y,        0).texture(u0, v0).color(rgb);
        buf.vertex(m, x,        y + size, 0).texture(u0, v1).color(rgb);
        buf.vertex(m, x + size, y + size, 0).texture(u1, v1).color(rgb);
        buf.vertex(m, x + size, y,        0).texture(u1, v0).color(rgb);
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());

        // Отрисовка второго слоя скина (шлем/аксессуар на голове), чтобы голова выглядела красиво с 3D волосами/шлемами
        float u0Hat = 40/S, v0Hat = 8/S, u1Hat = 48/S, v1Hat = 16/S;
        var bufHat = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufHat.vertex(m, x - 0.5f,        y - 0.5f,        0).texture(u0Hat, v0Hat).color(rgb);
        bufHat.vertex(m, x - 0.5f,        y + size + 0.5f, 0).texture(u0Hat, v1Hat).color(rgb);
        bufHat.vertex(m, x + size + 0.5f, y + size + 0.5f, 0).texture(u1Hat, v1Hat).color(rgb);
        bufHat.vertex(m, x + size + 0.5f, y,        0).texture(u1Hat, v0Hat).color(rgb);
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(bufHat.end());

        RenderSystem.disableBlend();
    }

    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        float alpha = getClickGUI().getAlpha().getValue();
        if (alpha <= 0.05f) return;

        // Отрисовка маленькой головы игрока вместо sl.png
        float logoSize = 12f;
        float logoX = x + (width - logoSize) / 2f;
        float logoY = y + 7f; // Идеально центрировано: верхнее пространство 26 пикселей (7 + 12 + 7 = 26f)
        
        drawPlayerHead(context, logoX, logoY, logoSize, alpha);

        // Отрисовка горизонтальной закруглённой линии над первой категорией (как на скриншоте)
        float lineWidth = 18f;
        float lineHeight = 1.5f;
        float lineX = x + (width - lineWidth) / 2f;
        float lineY = y + 26f; // Сбалансированное расстояние сверху (первая категория на y + 42)
        
        rectangle.render(ShapeProperties.create(context.getMatrices(), lineX, lineY, lineWidth, lineHeight)
                .round(lineHeight / 2f)
                .color(new FixColor(255, 255, 255, (int)(180 * alpha)).getRGB())
                .build());

        // Client logo removed as requested (no confusing car icon)
        float startY = y + 42f;
        float gap = 30f;

        for (int i = 0; i < categories.size(); i++) {
            ModuleCategory category = categories.get(i);
            float tabY = startY + i * gap;
            renderTab(context, x, tabY, mouseX, mouseY, category);
        }
    }

    private void renderSearchBar(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        if (searchBar == null) return;

        float searchBarW = 135f;
        float searchBarH = 21f;
        float searchBarX = getClickGUI().getX() + getClickGUI().getWidth() - searchBarW - 12f;
        float searchBarY = getClickGUI().getY() + 8f;
        searchBar.setX(searchBarX);
        searchBar.setY(searchBarY);
        searchBar.setWidth(searchBarW);
        searchBar.setHeight(searchBarH);
        searchBar.render(context, mouseX, mouseY, partialTicks);
    }

    private ModuleCategory getCategory() {
        for (ModuleCategory cat : ModuleCategory.values()) {
            if (cat.getScreen() == this.getClickGUI().getCurrentScreen()) return cat;
        }
        return null;
    }
}
