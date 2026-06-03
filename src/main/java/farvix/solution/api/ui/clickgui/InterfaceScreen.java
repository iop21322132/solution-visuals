package farvix.solution.api.ui.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.Client;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.FontRenderer;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.ui.clickgui.api.MenuScreen;
import farvix.solution.api.ui.clickgui.impl.sidebar.Sidebar;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.managers.ThemeManager;
import farvix.solution.api.ui.clickgui.impl.theme.ThemeScreen;
import farvix.solution.client.modules.impl.environment.ClickUI;

import java.awt.*;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter @Setter
public class InterfaceScreen extends Screen implements QuickImports {
    
    public float dWheel;
    
    private float x, y, width, height, cornerRadius;
    
    private ClickUI clickGUIModule;
    
    private Animation alpha;
    private Animation scale;
    private Animation categoryTransition; // Анимация переключения категорий (старая)
    
    // Slide эффект при открытии (более выраженный)
    private float contentOffsetY = 0f;
    private Animation slideAnimation;
    
    // Летние частицы на фоне
    private java.util.List<Particle> particles = new java.util.ArrayList<>();
    private static final int MAX_PARTICLES = 40;
    
    // Частицы эффектов при действиях
    private java.util.List<ActionParticle> actionParticles = new java.util.ArrayList<>();

    /** Публичный геттер alpha для синхронизации виджетов с анимацией GUI */
    public float getGuiAlpha() {
        return alpha != null ? alpha.getValue() : 0f;
    }
    
    private Sidebar sidebar;

    private ConcurrentLinkedQueue<ModuleComponent> moduleList = new ConcurrentLinkedQueue<>();
    
    private boolean closing;
    private boolean switching = false;
    
    private MenuScreen currentScreen = ModuleCategory.VISUALS.getScreen();
    private MenuScreen lastScreen = currentScreen;

    // Persist selected category and position between open/close
    private static ModuleCategory lastCategory = ModuleCategory.VISUALS;
    public static float savedX = -1;
    public static float savedY = -1;
    public static float savedXPercent = -1000f;
    public static float savedYPercent = -1000f;

    // Drag state
    private boolean guiDragging = false;
    private double dragOffsetX, dragOffsetY;

    // Hovered module for description panel
    private farvix.solution.client.modules.Module hoveredModule = null;

    public float round = 6f;

    public InterfaceScreen() {
        super(Text.of("ClickScreen"));
    }
    
    public float getGuiScaleFactor() {
        return 1.0f;
    }
    
    @Override
    protected void init() {
        this.clickGUIModule = Client.getInstance().getModuleManager().get(ClickUI.class);
        
        this.cornerRadius = 6;
        
        float guiScale = getGuiScaleFactor();
        net.minecraft.client.util.Window window = client.getWindow();
        float screenWidth = window.getScaledWidth() / guiScale;
        float screenHeight = window.getScaledHeight() / guiScale;

        this.width = 560f;  // Static width
        this.height = 350f; // Static height
        
        float centerX = (screenWidth - width) / 2f;
        float centerY = (screenHeight - height) / 2f;

        // Restore saved position or center
        if (savedXPercent < -999f && savedX >= 0f && savedY >= 0f) {
            savedXPercent = (savedX - centerX) / screenWidth;
            savedYPercent = (savedY - centerY) / screenHeight;
            savedX = -1f;
            savedY = -1f;
        }

        if (savedXPercent > -999f && savedYPercent > -999f) {
            this.x = centerX + savedXPercent * screenWidth;
            this.y = centerY + savedYPercent * screenHeight;
            this.x = Math.max(10f, Math.min(this.x, screenWidth - width - 10f));
            this.y = Math.max(10f, Math.min(this.y, screenHeight - height - 10f));
        } else {
            this.x = centerX;
            this.y = centerY;
            savedXPercent = (this.x - centerX) / screenWidth;
            savedYPercent = (this.y - centerY) / screenHeight;
        }
        
        savedX = this.x;
        savedY = this.y;
        
        this.alpha = new Animation(Easing.EASE_OUT_CUBIC, 300); 
        this.alpha.setValue(0);
        
        this.scale = new Animation(Easing.EASE_OUT_CUBIC, 300);
        this.scale.setValue(0.8f);
        
        // Более выраженная slide анимация
        this.slideAnimation = new Animation(Easing.EASE_OUT_CUBIC, 350);
        this.slideAnimation.setValue(0);
        
        this.categoryTransition = new Animation(Easing.EASE_OUT_CUBIC, 300);
        this.categoryTransition.setValue(1);
        
        this.sidebar = new Sidebar();
        this.sidebar.init();
        this.currentScreen = lastCategory.getScreen();
        this.lastScreen = currentScreen;
        
        rebuildModules();
        
        closing = false;
        switching = false;
        
        this.alpha.reset();
        this.alpha.run(1);
        this.scale.reset();
        this.scale.run(1);
        this.slideAnimation.reset();
        this.slideAnimation.run(1);
        
        // Инициализируем частицы
        initParticles();
        
        super.init();
    }
    
    private void initParticles() {
        particles.clear();
        net.minecraft.client.util.Window window = client.getWindow();
        for (int i = 0; i < MAX_PARTICLES; i++) {
            particles.add(new Particle(window.getScaledWidth(), window.getScaledHeight()));
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float guiScale = getGuiScaleFactor();
        
        // Recalculate dimensions dynamically in real-time to support live window stretching!
        net.minecraft.client.util.Window window = client.getWindow();
        float screenWidth = window.getScaledWidth() / guiScale;
        float screenHeight = window.getScaledHeight() / guiScale;

        this.width = 560f;
        this.height = 350f;

        float cX = (screenWidth - this.width) / 2f;
        float cY = (screenHeight - this.height) / 2f;

        if (savedXPercent > -999f && savedYPercent > -999f) {
            this.x = cX + savedXPercent * screenWidth;
            this.y = cY + savedYPercent * screenHeight;
            this.x = Math.max(10f, Math.min(this.x, screenWidth - this.width - 10f));
            this.y = Math.max(10f, Math.min(this.y, screenHeight - this.height - 10f));
        } else {
            this.x = cX;
            this.y = cY;
        }
        
        if (sidebar != null) {
            sidebar.init();
        }

        if (closing) {
            alpha.run(0);
            scale.run(0.8f);
            slideAnimation.run(0);
        } else {
            alpha.run(1);
            scale.run(1);
            slideAnimation.run(1);
        }
        
        if (closing && alpha.getValue() <= 0.01f) {
            mc.setScreen(null);
            // Выключаем модуль ClickUI при закрытии GUI (только если ещё включён)
            if (clickGUIModule != null && clickGUIModule.isEnabled()) {
                clickGUIModule.setEnabled(false);
            }
            return;
        }
        

        
        // Преобразуем координаты мыши с учетом масштабирования
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float currentScale = scale.getValue();
        float uiAlpha = alpha.getValue();
        
        float designMouseX = mouseX / guiScale;
        float designMouseY = mouseY / guiScale;
        
        int transformedMouseX = (int)((designMouseX - centerX) / currentScale + centerX);
        int transformedMouseY = (int)((designMouseY - centerY) / currentScale + centerY);
        
        // Вычисляем slide эффект (снизу вверх при открытии) - для ВСЕГО GUI
        float slideProgress = slideAnimation.getValue();
        float slideY = (1f - slideProgress) * 50f; // Чистое вертикальное движение
        
        // ═══════════════════════════════════════════════════════════════════════
        // ШАГ 1: ЗАТЕМНЕНИЕ ФОНА (как в reference mod)
        // ═══════════════════════════════════════════════════════════════════════
        int backdropAlpha = (int)(140 * uiAlpha);
        if (backdropAlpha > 0) {
            rectangle.render(ShapeProperties.create(context.getMatrices(), 
                    0f, 0f, 
                    window.getScaledWidth(), 
                    window.getScaledHeight())
                    .round(0)
                    .softness(0)
                    .thickness(0)
                    .outlineColor(0)
                    .color(new farvix.solution.api.util.color.FixColor(0, 0, 0, backdropAlpha).getRGB())
                    .build());
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // ШАГ 1.5: ЛЕТНИЕ ЧАСТИЦЫ НА ФОНЕ
        // ═══════════════════════════════════════════════════════════════════════
        if (clickGUIModule != null && clickGUIModule.particles.getValue()) {
            renderParticles(context, uiAlpha, delta);
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // ШАГ 1.6: ЧАСТИЦЫ ЭФФЕКТОВ ПРИ ДЕЙСТВИЯХ
        // ═══════════════════════════════════════════════════════════════════════
        renderActionParticles(context, uiAlpha, delta);
        
        context.getMatrices().push();
        
        // Apply GUI Scale Factor to fit the design resolution space perfectly
        context.getMatrices().scale(guiScale, guiScale, 1.0f);
        
        // Применяем ТОЛЬКО slide (чистое вертикальное движение снизу вверх)
        context.getMatrices().translate(0, slideY, 0);
        
        // Масштаб применяем отдельно от центра GUI (без влияния на позицию)
        if (currentScale != 1.0f) {
            context.getMatrices().translate(centerX, centerY, 0);
            context.getMatrices().scale(currentScale, currentScale, 1);
            context.getMatrices().translate(-centerX, -centerY, 0);
        }
        
        // Обновляем фон при любой теме (градиентной или одноцветной)
        ThemeManager.Theme currentTheme = ThemeManager.getInstance().getCurrentTheme();
        if (currentTheme != null) {
            TempColor.setThemeBackground(currentTheme.getBackgroundColor(), currentTheme.getAccentColor());
        }

        // ═══════════════════════════════════════════════════════════════════════
        // ШАГ 2: МЯГКОЕ СВЕЧЕНИЕ ВОКРУГ GUI ПАНЕЛИ
        // ═══════════════════════════════════════════════════════════════════════
        renderPanelGlow(context, uiAlpha);
        
        // ═══════════════════════════════════════════════════════════════════════
        // ШАГ 3: ОСНОВНАЯ ПАНЕЛЬ GUI (с эффектом матового стекла)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Основной фон с сильным размытием (frosted glass эффект) с темным тинтом 7.5% прозрачности (236 альфа)
        int guiBgColor = TempColor.getGuiBackground().alpha(uiAlpha).getRGB();
        int guiBorderColor = new farvix.solution.api.util.color.FixColor(255, 255, 255, (int)(15 * uiAlpha)).getRGB();
        glass.render(ShapeProperties.create(context.getMatrices(), x, y, width, height)
                .round(round)
                .softness(20f)
                .thickness(1.0f)
                .outlineColor(guiBorderColor)
                .color(guiBgColor) // Задаем реальный цвет фона с 7.5% прозрачности!
                .build());

        // ═══════════════════════════════════════════════════════════════════════
        // ШАГ 4: SIDEBAR
        // ═══════════════════════════════════════════════════════════════════════
        sidebar.render(context, transformedMouseX, transformedMouseY, delta);
        
        // Старая анимация переключения категорий (простой slide снизу вверх)
        if (switching) {
            categoryTransition.run(1);
            if (categoryTransition.getValue() >= 0.99f) {
                switching = false;
            }
        }
        
        float transitionValue = categoryTransition.getValue();
        
        // Применяем небольшой slide эффект при переключении с scissor тестом
        context.enableScissor((int) (x + sidebar.getWidth()), (int) y, (int) (x + width), (int) (y + height));
        context.getMatrices().push();
        
        // Снизу вверх: контент начинается чуть ниже и поднимается вверх
        float slideOffset = (1 - transitionValue) * 20; // Небольшой offset
        context.getMatrices().translate(0, slideOffset, 0);
        
        currentScreen.render(context, transformedMouseX, transformedMouseY, delta);

        // Track hovered module — только если курсор на заголовке (не на настройках)
        hoveredModule = null;
        if (currentScreen instanceof farvix.solution.api.ui.clickgui.impl.module.ModuleScreen ms2) {
            for (ModuleComponent mc2 : ms2.getModuleComponents()) {
                if (mc2.isHeaderHovered(transformedMouseX, transformedMouseY)) {
                    hoveredModule = mc2.getModule();
                    break;
                }
            }
        }
        
        context.getMatrices().pop();
        context.disableScissor();

        context.getMatrices().pop();

        // ── Floating Tooltip убран — описание теперь в панели рядом с поиском ──

        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float guiScale = getGuiScaleFactor();
        double scaledX = mouseX / guiScale;
        double scaledY = mouseY / guiScale;
        
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float currentScale = scale.getValue();
        
        double transformedX = (scaledX - centerX) / currentScale + centerX;
        double transformedY = (scaledY - centerY) / currentScale + centerY;

        // Start drag on left click on the title bar area (top 20px of GUI)
        if (button == 0 && transformedX >= x && transformedX <= x + width
                && transformedY >= y && transformedY <= y + 20) {
            guiDragging = true;
            dragOffsetX = scaledX - x;
            dragOffsetY = scaledY - y;
        }

        boolean clickedOnMainGUI = isHovered(x, y, width, height, transformedX, transformedY);
        boolean clickedOnSidePanel = transformedX >= x + width && transformedX <= x + width + 200;
        
        // Также проверяем клик по области модулей (они рендерятся с offset 42 от x)
        boolean clickedOnModuleArea = transformedX >= x + 42 && transformedX <= x + width
                && transformedY >= y && transformedY <= y + height;

        // Topbar zone: right of sidebar, top ~24px — only searchbar is allowed there,
        // so block clicks on modules/content in that strip
        float sidebarW = 42f;
        boolean inTopbar = transformedX >= x + sidebarW && transformedX <= x + width
                && transformedY >= y && transformedY <= y + 28f;

        if (clickedOnMainGUI || clickedOnSidePanel || clickedOnModuleArea) {
            sidebar.mouseClicked(transformedX, transformedY, button);
            if (!inTopbar) {
                currentScreen.mouseClicked(transformedX, transformedY, button);
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float guiScale = getGuiScaleFactor();
        double scaledX = mouseX / guiScale;
        double scaledY = mouseY / guiScale;
        dWheel += (float) verticalAmount;
        currentScreen.mouseScrolled(scaledX, scaledY, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) guiDragging = false;

        float guiScale = getGuiScaleFactor();
        double scaledX = mouseX / guiScale;
        double scaledY = mouseY / guiScale;

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float currentScale = scale.getValue();
        
        double transformedX = (scaledX - centerX) / currentScale + centerX;
        double transformedY = (scaledY - centerY) / currentScale + centerY;
        
        currentScreen.mouseReleased(transformedX, transformedY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        float guiScale = getGuiScaleFactor();
        double scaledX = mouseX / guiScale;
        double scaledY = mouseY / guiScale;

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float currentScale = scale.getValue();

        // GUI window drag
        if (guiDragging && button == 0) {
            net.minecraft.client.util.Window currentWindow = client.getWindow();
            if (currentWindow == null) return false;
            
            x = (float)(scaledX - dragOffsetX);
            y = (float)(scaledY - dragOffsetY);
            
            float screenWidth = currentWindow.getScaledWidth() / guiScale;
            float screenHeight = currentWindow.getScaledHeight() / guiScale;
            x = Math.max(0, Math.min(x, screenWidth - width));
            y = Math.max(0, Math.min(y, screenHeight - height));
            
            // Calculate relative offset from center
            float dragCenterX = (screenWidth - width) / 2f;
            float dragCenterY = (screenHeight - height) / 2f;
            savedXPercent = (x - dragCenterX) / screenWidth;
            savedYPercent = (y - dragCenterY) / screenHeight;
            
            savedX = x;
            savedY = y;
            
            sidebar.init();
            return true;
        }

        double transformedX = (scaledX - centerX) / currentScale + centerX;
        double transformedY = (scaledY - centerY) / currentScale + centerY;
        
        if (currentScreen instanceof farvix.solution.api.ui.clickgui.impl.module.ModuleScreen) {
            ((farvix.solution.api.ui.clickgui.impl.module.ModuleScreen) currentScreen).mouseDragged(transformedX, transformedY, button);
        } else if (currentScreen instanceof farvix.solution.api.ui.clickgui.impl.theme.ThemeScreen) {
            ((farvix.solution.api.ui.clickgui.impl.theme.ThemeScreen) currentScreen).mouseDragged(transformedX, transformedY, button);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 70 && (modifiers & 0x2) != 0) { 
            sidebar.focusSearch();
            return true; 
        }
        

        if (keyCode == 256) { 
            if (hasActiveFocus()) {
                sidebar.keyPressed(keyCode, scanCode, modifiers);
                currentScreen.keyPressed(keyCode, scanCode, modifiers);
                return true; 
            } else {
                closing = true;
                return true;
            }
        }
        
        sidebar.keyPressed(keyCode, scanCode, modifiers);
        currentScreen.keyPressed(keyCode, scanCode, modifiers);
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private boolean hasActiveFocus() {
        boolean searchFocused = sidebar.isSearchFocused();
        if (searchFocused) {
            return true;
        }

        // Проверяем фокус в WaypointScreen
        if (currentScreen instanceof farvix.solution.api.ui.clickgui.impl.waypoint.WaypointScreen ws) {
            if (ws.isFocused()) return true;
        }
        
        for (ModuleComponent component : moduleList) {
            if (component.isBinding()) {
                return true;
            }
            // Check if any ItemListSettingComponent has focus or BindSettingComponent is binding
            for (farvix.solution.api.ui.clickgui.api.SettingComponent sc : component.getSettings()) {
                if (sc instanceof farvix.solution.api.ui.clickgui.impl.settings.BindSettingComponent bsc) {
                    if (bsc.isBinding()) return true;
                }
                if (sc instanceof farvix.solution.api.ui.clickgui.impl.settings.ItemListSettingComponent ilsc) {
                    if (ilsc.isSearchFocused()) return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        sidebar.charTyped(codePoint, modifiers);
        currentScreen.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    public void rebuildModules() {
        moduleList.clear();
        
        List<Module> sortedModules = new ArrayList<>(Client.getInstance().getModuleManager().getModules());
        sortedModules.sort((o1, o2) -> Collator.getInstance().compare(o1.getName(), o2.getName()));
        // Исключаем скрытые модули (всегда активны, не показываем в GUI)
        sortedModules.forEach(module -> {
            // ПОЛНОСТЬЮ исключаем заблокированные модули из списка GUI
            if (!ClickUI.shouldShowModule(module)) return;

            if (!(module instanceof farvix.solution.client.modules.impl.visuals.ContextMenuModule)
                    && !(module instanceof farvix.solution.client.modules.impl.visuals.PlayerRadialMenu)
                    && !(module instanceof farvix.solution.client.modules.impl.environment.DiscordRPC)
                    && !(module instanceof farvix.solution.client.modules.impl.visuals.WaypointOverlay)) {
                moduleList.add(new ModuleComponent(module));
            }
        });

        // Принудительно обновляем текущий экран категории, чтобы пересчитать сетку кнопок
        if (currentScreen != null && mc.currentScreen instanceof InterfaceScreen) {
            currentScreen.init();
        }
    }
    
    public void switchScreen(ModuleCategory moduleCategory) {
        if (!moduleCategory.getScreen().equals(this.currentScreen)) {
            switching = true;
            lastCategory = moduleCategory;

            categoryTransition.setValue(0);
            categoryTransition.reset();
            
            lastScreen = this.currentScreen;
            currentScreen = moduleCategory.getScreen();
            currentScreen.init();
        }
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
    
    private boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    // ── Right side panel ──────────────────────────────────────────────────────

    private void renderSidePanel(DrawContext context, float px, float py,
                                 float pw, float ph, int mouseX, int mouseY) {
        float a   = alpha.getValue();
        float pad = 10f;

        if (hoveredModule != null) {
            // ── Module name ───────────────────────────────────────────────────
            String modName = hoveredModule.getName();
            farvix.solution.api.render.font.Fonts.SEMIBOLD.get(12).drawCenteredString(
                    context.getMatrices(), modName,
                    px + pw / 2f, py + pad,
                    TempColor.getTextPrimary().alpha(a).getRGB());

            // ── Separator ─────────────────────────────────────────────────────
            float sepY = py + pad + 14f;
            rectangle.render(ShapeProperties.create(context.getMatrices(),
                    px + pad, sepY, pw - pad * 2, 0.5f)
                    .round(0)
                    .color(TempColor.getSeparatorHorizontal().alpha(a * 0.6f).getRGB())
                    .build());

            // ── Description ───────────────────────────────────────────────────
            String desc = hoveredModule.getDescription();
            if (desc == null || desc.isEmpty()) desc = "Нет описания";

            float textX    = px + pad;
            float textY    = sepY + 8f;
            float maxWidth = pw - pad * 2;

            // Word-wrap the description
            farvix.solution.api.render.font.FontRenderer font =
                    farvix.solution.api.render.font.Fonts.DEFAULT.get(20);
            java.util.List<String> lines = wrapText(desc, font, maxWidth);

            int descColor = new farvix.solution.api.util.color.FixColor(
                    180, 180, 180, (int)(200 * a)).getRGB();
            // Use font size * 0.75 as line height to avoid huge gaps
            float lineH = 20 * 0.75f + 2f;
            for (String line : lines) {
                font.drawString(context.getMatrices(), line, textX, textY, descColor);
                textY += lineH;
            }

            // ── Category badge ────────────────────────────────────────────────
            String catName = hoveredModule.getCategory().getDisplayName();
            float badgeW   = farvix.solution.api.render.font.Fonts.DEFAULT.get(9)
                    .getStringWidth(catName) + 10f;
            float badgeH   = 12f;
            float badgeX   = px + (pw - badgeW) / 2f;
            float badgeY   = py + ph - badgeH - pad;

            blur.render(ShapeProperties.create(context.getMatrices(), badgeX, badgeY, badgeW, badgeH)
                    .round(4)
                    .softness(1f)
                    .thickness(1.5f)
                    .outlineColor(TempColor.getGuiBorder().alpha(a * 0.5f).getRGB())
                    .color(TempColor.getModuleBackground().alpha(a * 0.7f).getRGB())
                    .build());

            farvix.solution.api.render.font.Fonts.DEFAULT.get(9).drawCenteredString(
                    context.getMatrices(), catName,
                    px + pw / 2f, badgeY + badgeH / 2f - 1f,
                    TempColor.getTextSecondary().alpha(a).getRGB());

        } else {
            // ── Placeholder when nothing is hovered ───────────────────────────
            String hint = "Наведите на модуль";
            farvix.solution.api.render.font.Fonts.DEFAULT.get(10).drawCenteredString(
                    context.getMatrices(), hint,
                    px + pw / 2f, py + ph / 2f - 5f,
                    TempColor.getTextSecondary().alpha(a * 0.5f).getRGB());
        }
    }

    /**
     * Splits {@code text} into lines that fit within {@code maxWidth} pixels
     * using the given font renderer.
     */
    private java.util.List<String> wrapText(String text,
                                             farvix.solution.api.render.font.FontRenderer font,
                                             float maxWidth) {
        java.util.List<String> result = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (font.getStringWidth(test) <= maxWidth) {
                current = new StringBuilder(test);
            } else {
                if (current.length() > 0) result.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }
    
    /**
     * Рендерит премиальный floating tooltip рядом с курсором при наведении на модуль.
     * Дизайн: компактный, с акцентной полоской слева, категорией и описанием.
     */
    private void renderModuleTooltip(DrawContext context, Module module,
                                      int mouseX, int mouseY, float uiAlpha) {
        if (uiAlpha <= 0.01f) return;

        farvix.solution.api.render.font.FontRenderer nameFont = farvix.solution.api.render.font.Fonts.SEMIBOLD.get(11);
        farvix.solution.api.render.font.FontRenderer descFont = farvix.solution.api.render.font.Fonts.DEFAULT.get(20);

        String name = module.getName();
        String desc = module.getDescription();
        if (desc == null || desc.isEmpty()) desc = "Нет описания";

        // ── Размеры ────────────────────────────────────────────────────────────
        float cardW   = 155f;          // ширина карточки модуля
        float maxW    = cardW - 10f;   // tooltip не шире карточки минус 5px с каждой стороны
        float padX    = 8f;
        float padY    = 6f;
        float nameDescGap = 5f;
        float nameH   = nameFont.getStringHeight(name);

        // Word-wrap описания
        float textAreaW = maxW - padX * 2;
        java.util.List<String> descLines = wrapText(desc, descFont, textAreaW);
        // Реальная экранная высота строки описания (шрифт рендерится в scale 0.5)
        float descLineH = descFont.getStringHeight("A") / 2f + 1f;

        // Итоговая высота: имя + gap + описание + 4px
        float contentH = nameH + nameDescGap + descLines.size() * descLineH;
        float tooltipH = contentH + padY + (padY - 16f); // +4px к предыдущему значению

        // Ширина: фиксированная = maxW
        float tooltipW = maxW;

        // ── Позиция: под курсором, привязан к ширине карточки ────────────────
        float tx = mouseX + 10f;
        float ty = mouseY - tooltipH / 2f;

        // Не выходим за правый край GUI
        if (tx + tooltipW > x + width - 4f) {
            tx = mouseX - tooltipW - 10f;
        }
        // Не выходим за верхний/нижний край GUI
        ty = Math.max(y + 4f, Math.min(ty, y + height - tooltipH - 4f));

        // ── Рендер: внешнее свечение ───────────────────────────────────────────
        blur.render(ShapeProperties.create(context.getMatrices(),
                tx - 3, ty - 3, tooltipW + 6, tooltipH + 6)
                .round(10f)
                .softness(7f)
                .thickness(0)
                .outlineColor(0)
                .color(TempColor.getClientColor().alpha(uiAlpha * 0.10f).getRGB())
                .build());

        // ── Рендер: основной фон ───────────────────────────────────────────────
        blur.render(ShapeProperties.create(context.getMatrices(),
                tx, ty, tooltipW, tooltipH)
                .round(7f)
                .softness(2f)
                .thickness(1f)
                .outlineColor(TempColor.getClientColor().alpha(uiAlpha * 0.22f).getRGB())
                .color(TempColor.getGuiBackground().alpha(uiAlpha * 0.97f).getRGB())
                .build());

        // ── Рендер: акцентная полоска — убрана ───────────────────────────────

        // ── Рендер: текст ─────────────────────────────────────────────────────
        float textX = tx + padX;
        float curY  = ty + padY;

        // Имя модуля — белый полужирный
        nameFont.drawString(context.getMatrices(), name,
                textX, curY,
                TempColor.getTextPrimary().alpha(uiAlpha).getRGB());
        curY += nameH + nameDescGap;

        // Описание — серый, word-wrapped, поднято на 10px
        for (String line : descLines) {
            descFont.drawString(context.getMatrices(), line,
                    textX, curY - 10f,
                    TempColor.getTextSecondary().alpha(uiAlpha * 0.80f).getRGB());
            curY += descLineH;
        }
    }

    /**
     * Рендерит многослойное свечение вокруг GUI панели (как в reference mod)
     */
    private void renderPanelGlow(DrawContext context, float uiAlpha) {
        if (uiAlpha <= 0f) return;
        
        float gx = x;
        float gy = y;
        float gw = width;
        float gh = height;
        
        // Убрано свечение - оно вызывало артефакты за границами GUI
        // Вместо этого используем только основную панель с увеличенным blur
    }
    
    /**
     * Рендерит летние частицы на фоне GUI
     */
    private void renderParticles(DrawContext context, float uiAlpha, float delta) {
        if (uiAlpha <= 0.01f) return;
        
        net.minecraft.client.util.Window window = client.getWindow();
        
        // Обновляем и рендерим частицы
        for (Particle particle : particles) {
            particle.update(delta, window.getScaledWidth(), window.getScaledHeight());
            particle.render(context, uiAlpha);
        }
    }
    
    /**
     * Рендерит частицы эффектов при действиях
     */
    private void renderActionParticles(DrawContext context, float uiAlpha, float delta) {
        if (uiAlpha <= 0.01f) return;
        
        // Обновляем и рендерим action particles
        actionParticles.removeIf(p -> {
            p.update(delta);
            if (p.isAlive()) {
                p.render(context, uiAlpha);
                return false;
            }
            return true;
        });
    }
    
    /**
     * Создает эффект частиц в указанной позиции
     */
    public void spawnActionParticles(float x, float y, java.awt.Color color, int count) {
        for (int i = 0; i < count; i++) {
            actionParticles.add(new ActionParticle(x, y, color));
        }
    }
    
    /**
     * Класс летней частицы (светящаяся точка/искра)
     */
    private static class Particle {
        private float x, y;
        private float vx, vy;
        private float size;
        private float alpha;
        private float lifetime;
        private float maxLifetime;
        private java.awt.Color color;
        
        public Particle(int screenWidth, int screenHeight) {
            reset(screenWidth, screenHeight);
        }
        
        private void reset(int screenWidth, int screenHeight) {
            this.x = (float)(Math.random() * screenWidth);
            this.y = (float)(Math.random() * screenHeight);
            this.vx = (float)(Math.random() * 0.2f - 0.1f); // Уменьшено: -0.1 до +0.1 (было -0.25 до +0.25)
            this.vy = (float)(Math.random() * 0.3f + 0.1f); // Медленное падение вниз
            this.size = (float)(Math.random() * 2f + 1.5f); // 1.5-3.5px
            this.maxLifetime = (float)(Math.random() * 3f + 2f); // 2-5 секунд
            this.lifetime = 0f;
            
            // Летние цвета: желтый, оранжевый, светло-зеленый, голубой
            int colorType = (int)(Math.random() * 4);
            switch (colorType) {
                case 0: this.color = new java.awt.Color(255, 220, 100); break; // Желтый
                case 1: this.color = new java.awt.Color(255, 180, 100); break; // Оранжевый
                case 2: this.color = new java.awt.Color(150, 255, 150); break; // Светло-зеленый
                default: this.color = new java.awt.Color(150, 200, 255); break; // Голубой
            }
        }
        
        public void update(float delta, int screenWidth, int screenHeight) {
            lifetime += delta * 0.016f; // Примерно 60 FPS
            
            // Движение
            x += vx;
            y += vy;
            
            // Легкое покачивание (уменьшено)
            x += Math.sin(lifetime * 2f) * 0.15f; // Было 0.3f
            
            // Fade in/out эффект
            float fadeIn = Math.min(1f, lifetime / 0.5f);
            float fadeOut = Math.min(1f, (maxLifetime - lifetime) / 0.5f);
            alpha = Math.min(fadeIn, fadeOut);
            
            // Респавн если вышла за границы или закончилась жизнь
            if (y > screenHeight + 10 || lifetime >= maxLifetime) {
                // Респавн сверху
                this.x = (float)(Math.random() * screenWidth);
                this.y = -10;
                this.vx = (float)(Math.random() * 0.2f - 0.1f);
                this.vy = (float)(Math.random() * 0.3f + 0.1f);
                this.lifetime = 0f;
                this.maxLifetime = (float)(Math.random() * 3f + 2f);
            }
            
            // Если ушла за левый или правый край - возвращаем с другой стороны
            if (x < -10) {
                x = screenWidth + 10;
            } else if (x > screenWidth + 10) {
                x = -10;
            }
        }
        
        public void render(DrawContext context, float guiAlpha) {
            if (alpha <= 0.01f) return;
            
            int particleAlpha = (int)(alpha * guiAlpha * 180);
            if (particleAlpha <= 0) return;
            
            int particleColor = new farvix.solution.api.util.color.FixColor(
                    color.getRed(), 
                    color.getGreen(), 
                    color.getBlue(), 
                    particleAlpha
            ).getRGB();
            
            // Рендерим как маленький круг с мягким свечением
            QuickImports.blur.render(ShapeProperties.create(context.getMatrices(),
                    x - size / 2f, y - size / 2f, size, size)
                    .round(size / 2f)
                    .softness(1.5f)
                    .color(particleColor)
                    .build());
        }
    }
    
    /**
     * Класс частицы эффекта при действиях (включение модуля, переключение и т.д.)
     */
    private static class ActionParticle {
        private float x, y;
        private float vx, vy;
        private float size;
        private float alpha;
        private float lifetime;
        private static final float MAX_LIFETIME = 0.8f; // 800ms
        private java.awt.Color color;
        
        public ActionParticle(float x, float y, java.awt.Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = (float)(Math.random() * 2f + 2f); // 2-4px
            this.lifetime = 0f;
            
            // Случайное направление во все стороны
            float angle = (float)(Math.random() * Math.PI * 2);
            float speed = (float)(Math.random() * 2f + 1f);
            this.vx = (float)(Math.cos(angle) * speed);
            this.vy = (float)(Math.sin(angle) * speed);
        }
        
        public void update(float delta) {
            lifetime += delta * 0.016f;
            
            // Движение с замедлением
            x += vx;
            y += vy;
            vx *= 0.95f; // Замедление
            vy *= 0.95f;
            
            // Гравитация
            vy += 0.1f;
            
            // Fade out
            alpha = 1f - (lifetime / MAX_LIFETIME);
        }
        
        public boolean isAlive() {
            return lifetime < MAX_LIFETIME;
        }
        
        public void render(DrawContext context, float guiAlpha) {
            if (alpha <= 0.01f) return;
            
            int particleAlpha = (int)(alpha * guiAlpha * 200);
            if (particleAlpha <= 0) return;
            
            int particleColor = new farvix.solution.api.util.color.FixColor(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    particleAlpha
            ).getRGB();
            
            // Рендерим как маленький круг с свечением
            QuickImports.blur.render(ShapeProperties.create(context.getMatrices(),
                    x - size / 2f, y - size / 2f, size, size)
                    .round(size / 2f)
                    .softness(2f)
                    .color(particleColor)
                    .build());
        }
    }
}

