package farvix.solution.api.ui.clickgui.impl.config;

import net.minecraft.client.gui.DrawContext;
import farvix.solution.Client;
import farvix.solution.api.TempColor;
import farvix.solution.api.config.Config;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.ui.clickgui.api.MenuScreen;
import farvix.solution.api.ui.clickgui.InterfaceScreen;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.util.other.ScrollUtility;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends MenuScreen {
    
    private final List<ConfigComponent> configComponents = new ArrayList<>();
    private final ScrollUtility scroll = new ScrollUtility();
    
    private boolean isCreatingNew = false;
    private String newConfigName = "";
    
    private final Animation createPanelAnimation = new Animation(Easing.EASE_OUT_CUBIC, 300);
    
    @Override
    public void init() {
        this.x = getClickGUI().getX() + 42;
        this.y = getClickGUI().getY() + 25;
        this.width = getClickGUI().getWidth() - 42;
        this.height = getClickGUI().getHeight() - 25;
        
        rebuildConfigs();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Sync position to GUI window
        this.x = getClickGUI().getX() + 42;
        this.y = getClickGUI().getY() + 25;
        this.width = getClickGUI().getWidth() - 42;
        this.height = getClickGUI().getHeight() - 25;

        // Обработка плавной прокрутки
        scroll.handle();
        
        // Анимация панели создания
        createPanelAnimation.run(isCreatingNew ? 1 : 0);
        
        // Большой отступ слева чтобы не вылезать на sidebar
        float leftPadding = 50;
        
        // Заголовок убран
        
        // Контейнер для кнопок управления (без scissor)
        float containerY = y + 30;
        float containerHeight = 25;
        
        ShapeProperties.create(context.getMatrices(), x + leftPadding, containerY, width - leftPadding - 10, containerHeight)
                .round(10)
                .thickness(1)
                .outlineColor(TempColor.getModuleBorder().getRGB())
                .color(TempColor.getModuleBackground().getRGB())
                .build();
        
        // Две кнопки управления внутри контейнера
        float btnWidth = (width - leftPadding - 40) / 2f;
        float btnHeight = 18;
        float btnY = containerY + 3.5f;
        float btnSpacing = 10;
        
        float createBtnX = x + leftPadding + 10;
        float resetBtnX = createBtnX + btnWidth + btnSpacing;
        
        boolean createHovered = isHovered(createBtnX, btnY, btnWidth, btnHeight, mouseX, mouseY);
        boolean resetHovered = isHovered(resetBtnX, btnY, btnWidth, btnHeight, mouseX, mouseY);
        
        // Определяем тему для контраста
        java.awt.Color themeCol2 = farvix.solution.client.managers.ThemeManager.getInstance()
                .getCurrentTheme().getBackgroundColor();
        float themeBr2 = (themeCol2.getRed() * 0.299f + themeCol2.getGreen() * 0.587f
                + themeCol2.getBlue() * 0.114f) / 255f;
        boolean isLightCfg = themeBr2 > 0.45f;
        double createOutlineAlpha = createHovered ? (isLightCfg ? 0.8 : 0.5) : (isLightCfg ? 0.5 : 0.3);
        double resetOutlineAlpha  = resetHovered  ? (isLightCfg ? 0.8 : 0.5) : (isLightCfg ? 0.5 : 0.3);

        // Create button
        blur.render(ShapeProperties.create(context.getMatrices(), createBtnX - 1, btnY - 1, btnWidth + 2, btnHeight + 2)
                .round(6)
                .softness(1.5f)
                .thickness(2.5f)
                .outlineColor(TempColor.getClientColor().alpha(createOutlineAlpha).getRGB())
                .color(TempColor.getModuleBackground().getRGB())
                .build());
        
        Fonts.DEFAULT.get(13).drawCenteredString(context.getMatrices(), "Create",
                createBtnX + btnWidth / 2, btnY + btnHeight / 2 - 3,
                TempColor.getTextPrimary().getRGB());
        
        // Reset button
        blur.render(ShapeProperties.create(context.getMatrices(), resetBtnX - 1, btnY - 1, btnWidth + 2, btnHeight + 2)
                .round(6)
                .softness(1.5f)
                .thickness(2.5f)
                .outlineColor(new farvix.solution.api.util.color.FixColor(255, 100, 100).alpha(resetOutlineAlpha).getRGB())
                .color(TempColor.getModuleBackground().getRGB())
                .build());
        
        Fonts.DEFAULT.get(13).drawCenteredString(context.getMatrices(), "Reset",
                resetBtnX + btnWidth / 2, btnY + btnHeight / 2 - 3,
                TempColor.getTextPrimary().getRGB());
        
        // Разделитель
        float separatorY = containerY + containerHeight + 2;
        ShapeProperties.create(context.getMatrices(), x + leftPadding, separatorY, width - leftPadding - 10, 1)
                .color(TempColor.getClientColor().alpha(0.2).getRGB())
                .build();
        
        // Заголовок списка убран
        
        // Список конфигов с scissor ТОЛЬКО для списка
        float listY = separatorY + 24;
        float itemHeight = 55;
        float spacing = 8;
        
        if (configComponents.isEmpty()) {
            // Показываем сообщение если нет конфигов
            Fonts.DEFAULT.get(14).drawCenteredString(context.getMatrices(), "No configs yet. Create one!", 
                    x + leftPadding + (width - leftPadding - 10) / 2, listY + 50, 0x80FFFFFF);
        } else {
            // Вычисляем максимальную прокрутку
            float totalHeight = configComponents.size() * (itemHeight + spacing);
            float availableHeight = height - (listY - y) - 10;
            if (totalHeight > availableHeight) {
                scroll.setMax(-(totalHeight - availableHeight));
            } else {
                scroll.setMax(0);
            }
            
            // Включаем scissor ТОЛЬКО для списка конфигов
            int scissorX1 = (int)(x + leftPadding);
            int scissorY1 = (int)listY;
            int scissorX2 = (int)(x + width - 10);
            int scissorY2 = (int)(y + height - 10);
            
            context.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
            
            float currentY = listY + scroll.getScroll();
            
            for (ConfigComponent component : configComponents) {
                component.setX(x + leftPadding + 5);
                component.setY(currentY);
                component.setWidth(width - leftPadding - 15);
                component.setHeight(itemHeight);
                
                component.render(context, mouseX, mouseY, delta);
                
                currentY += itemHeight + spacing;
            }
            
            context.disableScissor();
        }
        
        // Боковая панель создания конфига (справа от GUI)
        if (createPanelAnimation.getValue() > 0.01) {
            renderCreatePanel(context, mouseX, mouseY);
        }
    }
    
    private void renderCreatePanel(DrawContext context, int mouseX, int mouseY) {
        InterfaceScreen gui = getClickGUI();
        
        float panelWidth = 125;
        float panelHeight = gui.getHeight();
        float panelX = gui.getX() + gui.getWidth() + 5 + (panelWidth + 5) * (1 - (float)createPanelAnimation.getValue());
        float panelY = gui.getY();
        
        // Фон панели
        blur.render(ShapeProperties.create(context.getMatrices(), panelX - 1, panelY - 1, panelWidth + 2, panelHeight + 2)
                .round(10)
                .softness(1.5f)
                .thickness(2.5f)
                .outlineColor(TempColor.getClientColor().alpha(0.3).getRGB())
                .color(TempColor.getModuleBackground().getRGB())
                .build());
        
        // Заголовок
        Fonts.DEFAULT.get(16).drawCenteredString(context.getMatrices(), "Create Config", 
                panelX + panelWidth / 2, panelY + 15, TempColor.getClientColor().getRGB());
        
        // Подзаголовок
        Fonts.DEFAULT.get(11).drawCenteredString(context.getMatrices(), "Enter config name", 
                panelX + panelWidth / 2, panelY + 35, TempColor.getTextSecondary().getRGB());
        
        // Поле ввода
        float inputX = panelX + 10;
        float inputY = panelY + 55;
        float inputW = panelWidth - 20;
        float inputH = 30;
        
        blur.render(ShapeProperties.create(context.getMatrices(), inputX - 1, inputY - 1, inputW + 2, inputH + 2)
                .round(8)
                .softness(1.5f)
                .thickness(2.5f)
                .outlineColor(TempColor.getClientColor().alpha(0.2).getRGB())
                .color(TempColor.getGuiBackground().alpha(0.5).getRGB())
                .build());
        
        String displayText = newConfigName.isEmpty() ? "my_config" : newConfigName;
        int textColor = newConfigName.isEmpty() ? 0x60FFFFFF : -1;
        
        // Добавляем мигающий курсор если панель активна
        String displayWithCursor = displayText;
        if (isCreatingNew && System.currentTimeMillis() % 1000 < 500) {
            displayWithCursor = displayText + "|";
        }
        
        Fonts.DEFAULT.get(13).drawString(context.getMatrices(), displayWithCursor, 
                inputX + 10, inputY + inputH / 2 - 4,
                newConfigName.isEmpty() ? TempColor.getTextSecondary().getRGB() : TempColor.getTextPrimary().getRGB());
        
        // Кнопки
        float btnY = panelY + 100;
        float btnW = panelWidth - 20;
        float btnH = 26;
        float btnSpacing = 8;
        
        float createX = panelX + 10;
        float cleanY = btnY + btnH + btnSpacing;
        float cfgDirY = cleanY + btnH + btnSpacing;
        
        boolean createHovered = isHovered(createX, btnY, btnW, btnH, mouseX, mouseY);
        boolean cleanHovered = isHovered(createX, cleanY, btnW, btnH, mouseX, mouseY);
        boolean cfgDirHovered = isHovered(createX, cfgDirY, btnW, btnH, mouseX, mouseY);
        
        // Create button
        blur.render(ShapeProperties.create(context.getMatrices(), createX - 1, btnY - 1, btnW + 2, btnH + 2)
                .round(6)
                .softness(1.5f)
                .thickness(2.5f)
                .outlineColor(TempColor.getClientColor().alpha(createHovered ? 0.5 : 0.3).getRGB())
                .color(TempColor.getModuleBackground().getRGB())
                .build());
        
        Fonts.DEFAULT.get(13).drawCenteredString(context.getMatrices(), "Create", 
                createX + btnW / 2, btnY + btnH / 2 - 3, TempColor.getTextPrimary().getRGB());
        
        // Clean button
        blur.render(ShapeProperties.create(context.getMatrices(), createX - 1, cleanY - 1, btnW + 2, btnH + 2)
                .round(6)
                .softness(1.5f)
                .thickness(2.5f)
                .outlineColor(TempColor.getClientColor().alpha(cleanHovered ? 0.5 : 0.3).getRGB())
                .color(TempColor.getModuleBackground().getRGB())
                .build());
        
        Fonts.DEFAULT.get(13).drawCenteredString(context.getMatrices(), "Clean", 
                createX + btnW / 2, cleanY + btnH / 2 - 3, TempColor.getTextPrimary().getRGB());
        
        // Cfg Dir button
        blur.render(ShapeProperties.create(context.getMatrices(), createX - 1, cfgDirY - 1, btnW + 2, btnH + 2)
                .round(6)
                .softness(1.5f)
                .thickness(2.5f)
                .outlineColor(TempColor.getClientColor().alpha(cfgDirHovered ? 0.5 : 0.3).getRGB())
                .color(TempColor.getModuleBackground().getRGB())
                .build());
        
        Fonts.DEFAULT.get(13).drawCenteredString(context.getMatrices(), "Cfg Dir", 
                createX + btnW / 2, cfgDirY + btnH / 2 - 3, TempColor.getTextPrimary().getRGB());
    }
    
    
    private void renderCreateDialog(DrawContext context, int mouseX, int mouseY) {
        InterfaceScreen gui = getClickGUI();
        float dialogW = 350;
        float dialogH = 180;
        float dialogX = gui.getX() + (gui.getWidth() - dialogW) / 2;
        float dialogY = gui.getY() + (gui.getHeight() - dialogH) / 2;
        
        // Затемнение фона
        ShapeProperties.create(context.getMatrices(), 
                gui.getX(), gui.getY(), gui.getWidth(), gui.getHeight())
                .color(TempColor.getGuiBackground().alpha(0.8).getRGB())
                .build();
        
        // Диалоговое окно
        ShapeProperties.create(context.getMatrices(), dialogX, dialogY, dialogW, dialogH)
                .round(12)
                .thickness(2)
                .outlineColor(TempColor.getClientColor().alpha(0.5).getRGB())
                .color(TempColor.getGuiBackground().getRGB())
                .build();
        
        // Заголовок
        Fonts.DEFAULT.get(18).drawCenteredString(context.getMatrices(), "Create New Config", 
                dialogX + dialogW / 2, dialogY + 20, TempColor.getClientColor().getRGB());
        
        // Подзаголовок
        Fonts.DEFAULT.get(12).drawCenteredString(context.getMatrices(), "Enter a name for your config", 
                dialogX + dialogW / 2, dialogY + 40, 0x80FFFFFF);
        
        // Поле ввода
        float inputX = dialogX + 25;
        float inputY = dialogY + 65;
        float inputW = dialogW - 50;
        float inputH = 35;
        
        ShapeProperties.create(context.getMatrices(), inputX, inputY, inputW, inputH)
                .round(8)
                .thickness(1)
                .outlineColor(TempColor.getClientColor().alpha(0.3).getRGB())
                .color(TempColor.getModuleBackground().alpha(0.8).getRGB())
                .build();
        
        String displayText = newConfigName.isEmpty() ? "my_config" : newConfigName;
        int textColor = newConfigName.isEmpty() ? 0x60FFFFFF : -1;
        Fonts.DEFAULT.get(14).drawString(context.getMatrices(), displayText, 
                inputX + 12, inputY + inputH / 2 - 4, textColor);
        
        // Кнопки
        float btnY = dialogY + 120;
        float btnW = 140;
        float btnH = 35;
        float btnSpacing = 20;
        
        float cancelX = dialogX + (dialogW - btnW * 2 - btnSpacing) / 2;
        float saveX = cancelX + btnW + btnSpacing;
        
        boolean cancelHovered = isHovered(cancelX, btnY, btnW, btnH, mouseX, mouseY);
        boolean saveHovered = isHovered(saveX, btnY, btnW, btnH, mouseX, mouseY);
        
        // Cancel button
        ShapeProperties.create(context.getMatrices(), cancelX, btnY, btnW, btnH)
                .round(8)
                .thickness(1)
                .outlineColor(TempColor.getTextSecondary().alpha(cancelHovered ? 0.5 : 0.3).getRGB())
                .color(TempColor.getModuleBackground().alpha(cancelHovered ? 0.6 : 0.4).getRGB())
                .build();
        
        Fonts.DEFAULT.get(14).drawCenteredString(context.getMatrices(), "Cancel", 
                cancelX + btnW / 2, btnY + btnH / 2 - 3, -1);
        
        // Save button
        ShapeProperties.create(context.getMatrices(), saveX, btnY, btnW, btnH)
                .round(8)
                .thickness(1)
                .outlineColor(TempColor.getClientColor().alpha(saveHovered ? 0.7 : 0.5).getRGB())
                .color(TempColor.getClientColor().alpha(saveHovered ? 0.3 : 0.2).getRGB())
                .build();
        
        Fonts.DEFAULT.get(14).drawCenteredString(context.getMatrices(), "Create", 
                saveX + btnW / 2, btnY + btnH / 2 - 3, -1);
    }
    
    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        // СНАЧАЛА проверяем боковую панель
        if (isCreatingNew && createPanelAnimation.getValue() > 0.01) {
            InterfaceScreen gui = getClickGUI();
            float panelWidth = 125;
            float panelX = gui.getX() + gui.getWidth() + 5 + (panelWidth + 5) * (1 - (float)createPanelAnimation.getValue());
            float panelY = gui.getY();
            
            // Кнопки
            float btnY = panelY + 100;
            float btnW = panelWidth - 20;
            float btnH = 26;
            float btnSpacing = 8;
            
            float createX = panelX + 10;
            float cleanY = btnY + btnH + btnSpacing;
            float cfgDirY = cleanY + btnH + btnSpacing;
            
            // Cfg Dir button - ОТКРЫВАЕТ папку
            if (mouseX >= createX && mouseX <= createX + btnW && 
                mouseY >= cfgDirY && mouseY <= cfgDirY + btnH) {
                try {
                    java.io.File configDir = Client.getInstance().getConfigManager().getConfigDir();
                    if (!configDir.exists()) {
                        configDir.mkdirs();
                    }
                    
                    String absolutePath = configDir.getAbsolutePath();
                    
                    // Сворачиваем окно Minecraft через GLFW
                    org.lwjgl.glfw.GLFW.glfwIconifyWindow(mc.getWindow().getHandle());
                    
                    // Пробуем открыть через Desktop API
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(configDir);
                    } else {
                        // Альтернативный способ для Windows
                        String os = System.getProperty("os.name").toLowerCase();
                        if (os.contains("win")) {
                            Runtime.getRuntime().exec("explorer.exe \"" + absolutePath + "\"");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            
            // Clean button - ЗАКРЫВАЕТ панель
            if (mouseX >= createX && mouseX <= createX + btnW && 
                mouseY >= cleanY && mouseY <= cleanY + btnH) {
                isCreatingNew = false;
                newConfigName = "";
                return;
            }
            
            // Create button - СОЗДАЕТ конфиг
            if (mouseX >= createX && mouseX <= createX + btnW && 
                mouseY >= btnY && mouseY <= btnY + btnH) {
                if (!newConfigName.isEmpty()) {
                    Client.getInstance().getConfigManager().saveConfig(newConfigName);
                    rebuildConfigs();
                    isCreatingNew = false;
                    newConfigName = "";
                }
                return;
            }
            
            // Клик в любом месте панели - не закрываем
            if (mouseX >= panelX && mouseX <= panelX + panelWidth) {
                return;
            }
        }
        
        float leftPadding = 50;
        float containerY = y + 30;
        float btnWidth = (width - leftPadding - 40) / 2f;
        float btnHeight = 18;
        float btnY = containerY + 3.5f;
        float btnSpacing = 10;
        
        float createBtnX = x + leftPadding + 10;
        float resetBtnX = createBtnX + btnWidth + btnSpacing;
        
        // Create (открывает боковую панель)
        if (isHovered(createBtnX, btnY, btnWidth, btnHeight, mouseX, mouseY)) {
            isCreatingNew = true;
            newConfigName = "";
            return;
        }
        
        // Reset
        if (isHovered(resetBtnX, btnY, btnWidth, btnHeight, mouseX, mouseY)) {
            resetAllModules();
            return;
        }
        
        // Клики по компонентам (используем копию списка чтобы избежать ConcurrentModificationException)
        for (ConfigComponent component : new ArrayList<>(configComponents)) {
            component.mouseClicked(mouseX, mouseY, button);
        }
    }
    
    
    private void handleCreateDialogClick(double mouseX, double mouseY, int button) {
        InterfaceScreen gui = getClickGUI();
        float dialogW = 350;
        float dialogH = 180;
        float dialogX = gui.getX() + (gui.getWidth() - dialogW) / 2;
        float dialogY = gui.getY() + (gui.getHeight() - dialogH) / 2;
        
        float btnY = dialogY + 120;
        float btnW = 140;
        float btnH = 35;
        float btnSpacing = 20;
        
        float cancelX = dialogX + (dialogW - btnW * 2 - btnSpacing) / 2;
        float saveX = cancelX + btnW + btnSpacing;
        
        // Cancel
        if (isHovered(cancelX, btnY, btnW, btnH, mouseX, mouseY)) {
            isCreatingNew = false;
            newConfigName = "";
            return;
        }
        
        // Save
        if (isHovered(saveX, btnY, btnW, btnH, mouseX, mouseY)) {
            if (!newConfigName.isEmpty()) {
                Client.getInstance().getConfigManager().saveConfig(newConfigName);
                rebuildConfigs();
                isCreatingNew = false;
                newConfigName = "";
            }
        }
    }
    
    @Override
    public void mouseScrolled(double mouseX, double mouseY, double amount) {
        // ScrollUtility автоматически обработает прокрутку через InterfaceScreen.dWheel
        super.mouseScrolled(mouseX, mouseY, amount);
    }
    
    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isCreatingNew) {
            if (keyCode == 256) { // ESC - закрыть панель
                isCreatingNew = false;
                newConfigName = "";
            } else if (keyCode == 257 || keyCode == 335) { // ENTER - создать конфиг
                if (!newConfigName.isEmpty()) {
                    Client.getInstance().getConfigManager().saveConfig(newConfigName);
                    rebuildConfigs();
                    isCreatingNew = false;
                    newConfigName = "";
                }
            } else if (keyCode == 259) { // BACKSPACE - удалить символ
                if (!newConfigName.isEmpty()) {
                    newConfigName = newConfigName.substring(0, newConfigName.length() - 1);
                }
            }
        }
    }
    
    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (isCreatingNew) {
            if (Character.isLetterOrDigit(codePoint) || codePoint == '_' || codePoint == '-') {
                if (newConfigName.length() < 20) {
                    newConfigName += codePoint;
                }
            }
        }
    }
    
    private void rebuildConfigs() {
        configComponents.clear();
        Client.getInstance().getConfigManager().refreshConfigs();
        
        for (Config config : Client.getInstance().getConfigManager().getConfigs()) {
            configComponents.add(new ConfigComponent(config, this));
        }
    }
    
    private void resetAllModules() {
        Client.getInstance().getModuleManager().getModules().forEach(module -> {
            // Не сбрасываем ClickUI и Player Menu (чтобы быстрое авто доб меню работало всегда)
            if (!module.getName().equals("Click UI") 
                    && !module.getName().equals("Player Menu")
                    && !module.getName().equals("Discord RPC")
                    && !module.getName().equals("Waypoint Overlay")) {
                module.setEnabled(false);
                module.setKey(-1); // -1 означает "нет бинда", 0 - это ЛКМ
            }
        });
    }
    
    public void onConfigDeleted() {
        rebuildConfigs();
    }
    
    private InterfaceScreen getClickGUI() {
        return (InterfaceScreen) mc.currentScreen;
    }
}
