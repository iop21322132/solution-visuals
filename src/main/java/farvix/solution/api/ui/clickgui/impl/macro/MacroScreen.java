package farvix.solution.api.ui.clickgui.impl.macro;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import farvix.solution.Client;
import farvix.solution.api.TempColor;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.ui.clickgui.api.MenuScreen;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.managers.MacroManager;

import java.awt.*;
import java.util.List;

@Getter
public class MacroScreen extends MenuScreen implements QuickImports {
    
    // Поле ввода сообщения
    private String messageInput = "";
    private boolean messageInputFocused = false;
    
    // Выбор клавиши
    private int selectedKey = -1;
    private boolean selectingKey = false;
    
    // Анимации
    private Animation addButtonHover = new Animation(Easing.EASE_IN_OUT_SINE, 150);
    
    @Override
    public void init() {
        // Загрузка макросов из конфига (TODO)
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        farvix.solution.api.ui.clickgui.InterfaceScreen gui = getClickGUI();
        float alpha = gui.getAlpha().getValue();
        float localAlpha = alpha * 0.9f;
        
        // Используем координаты от InterfaceScreen
        float guiX = gui.getX();
        float guiY = gui.getY();
        float guiWidth = gui.getWidth();
        float guiHeight = gui.getHeight();
        
        float sidebarWidth = gui.getSidebar().getWidth(); // Реальная ширина sidebar
        float startX = guiX + sidebarWidth + 10; // Отступ от sidebar
        float startY = guiY + 41; // Оптимальная высота
        float contentWidth = guiWidth - sidebarWidth - 20;
        
        // ═══════════════════════════════════════════════════════════════════════
        // ВЕРХНЯЯ ПАНЕЛЬ - Создание нового макроса
        // ═══════════════════════════════════════════════════════════════════════
        
        float inputY = startY;
        
        // 1. Поле ввода сообщения (60% ширины)
        float messageFieldWidth = contentWidth * 0.6f;
        float messageFieldHeight = 30;
        
        int messageBgColor = messageInputFocused
                ? TempColor.getModuleBackground().alpha(localAlpha).getRGB()
                : TempColor.getModuleBackground().alpha(localAlpha * 0.9f).getRGB();
        
        // Основной фон
        rectangle.render(ShapeProperties.create(context.getMatrices(), startX, inputY, messageFieldWidth, messageFieldHeight)
                .round(8)
                .color(messageBgColor)
                .build());
        
        // Текст в поле или placeholder
        String displayText = messageInput.isEmpty() ? "Введите сообщение..." : messageInput;
        int textColor = messageInput.isEmpty()
                ? TempColor.getTextSecondary().alpha(localAlpha * 0.5f).getRGB()
                : TempColor.getTextPrimary().alpha(localAlpha).getRGB();
        
        Fonts.DEFAULT.get(14).drawString(context.getMatrices(), displayText,
                startX + 10, inputY + messageFieldHeight / 2f - 4,
                textColor);
        
        // Курсор если в фокусе
        if (messageInputFocused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = startX + 10 + Fonts.DEFAULT.get(14).getStringWidth(messageInput);
            rectangle.render(ShapeProperties.create(context.getMatrices(), cursorX, inputY + 8, 1, 14)
                    .round(0)
                    .color(TempColor.getClientColor().alpha(localAlpha).getRGB())
                    .build());
        }
        
        // 2. Кнопка выбора клавиши (20% ширины)
        float keyButtonX = startX + messageFieldWidth + 5;
        float keyButtonWidth = contentWidth * 0.18f;
        
        String keyText = selectingKey ? "..." : (selectedKey == -1 ? "KEY" : getKeyName(selectedKey));
        int keyBgColor = selectingKey
                ? TempColor.getClientColor().alpha(localAlpha * 0.35f).getRGB()
                : TempColor.getClientColor().alpha(localAlpha * 0.15f).getRGB();
        
        // Основной фон
        rectangle.render(ShapeProperties.create(context.getMatrices(), keyButtonX, inputY, keyButtonWidth, messageFieldHeight)
                .round(8)
                .color(keyBgColor)
                .build());
        
        Fonts.DEFAULT.get(12).drawCenteredString(context.getMatrices(), keyText,
                keyButtonX + keyButtonWidth / 2f, inputY + messageFieldHeight / 2f - 3,
                TempColor.getTextPrimary().alpha(localAlpha).getRGB());
        
        // 3. Кнопка "Добавить" (оставшаяся ширина)
        float addButtonX = keyButtonX + keyButtonWidth + 5;
        float addButtonWidth = contentWidth - messageFieldWidth - keyButtonWidth - 15; // Вычисляем оставшееся место
        
        boolean addButtonHovered = mouseX >= addButtonX && mouseX <= addButtonX + addButtonWidth
                && mouseY >= inputY && mouseY <= inputY + messageFieldHeight;
        addButtonHover.run(addButtonHovered ? 1 : 0);
        
        int addBgColor = TempColor.getClientColor().alpha(localAlpha * (0.15f + 0.2f * addButtonHover.getValue())).getRGB();
        
        // Основной фон
        rectangle.render(ShapeProperties.create(context.getMatrices(), addButtonX, inputY, addButtonWidth, messageFieldHeight)
                .round(8)
                .color(addBgColor)
                .build());
        
        Fonts.DEFAULT.get(13).drawCenteredString(context.getMatrices(), "Добавить",
                addButtonX + addButtonWidth / 2f, inputY + messageFieldHeight / 2f - 3,
                new FixColor(255, 255, 255, (int)(255 * localAlpha)).getRGB());
        
        // ═══════════════════════════════════════════════════════════════════════
        // РАЗДЕЛИТЕЛЬ (желтая линия -> акцентный цвет темы)
        // ═══════════════════════════════════════════════════════════════════════
        
        float separatorY = inputY + messageFieldHeight + 20;
        blur.render(ShapeProperties.create(context.getMatrices(), startX, separatorY, contentWidth, 1)
                .round(0.5f)
                .softness(1.5f)
                .color(TempColor.getClientColor().alpha(alpha * 0.6f).getRGB())
                .build());
        
        // ═══════════════════════════════════════════════════════════════════════
        // СПИСОК МАКРОСОВ
        // ═══════════════════════════════════════════════════════════════════════
        
        float listY = separatorY + 15;
        
        List<MacroManager.Macro> macros = Client.getInstance().macroManager.getMacros();
        
        if (macros.isEmpty()) {
            // Placeholder если нет макросов
            Fonts.DEFAULT.get(14).drawCenteredString(context.getMatrices(), "Нет макросов",
                    startX + contentWidth / 2f, listY + 20,
                    TempColor.getTextSecondary().alpha(alpha * 0.5f).getRGB());
        } else {
            // Рендерим список макросов
            for (int i = 0; i < macros.size(); i++) {
                MacroManager.Macro macro = macros.get(i);
                float macroY = listY + i * 35;
                
                renderMacroItem(context, macro, startX, macroY, contentWidth, mouseX, mouseY, localAlpha);
            }
        }
    }
    
    private void renderMacroItem(DrawContext context, MacroManager.Macro macro, float x, float y, float width, int mouseX, int mouseY, float alpha) {
        float itemHeight = 30;
        
        // Фон элемента
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, itemHeight)
                .round(8)
                .color(TempColor.getModuleBackground().alpha(alpha * 0.8f).getRGB())
                .build());
        
        // Текст сообщения (адаптивный цвет - противоположный фону темы)
        float messageWidth = width - 150;
        String displayMessage = macro.getMessage();
        if (Fonts.DEFAULT.get(14).getStringWidth(displayMessage) > messageWidth - 20) {
            while (Fonts.DEFAULT.get(14).getStringWidth(displayMessage + "...") > messageWidth - 20) {
                displayMessage = displayMessage.substring(0, displayMessage.length() - 1);
            }
            displayMessage += "...";
        }
        
        Fonts.DEFAULT.get(14).drawString(context.getMatrices(), displayMessage,
                x + 10, y + itemHeight / 2f - 4,
                TempColor.getTextPrimary().alpha(alpha).getRGB());
        
        // Клавиша (белый квадрат на скрине)
        float keyX = x + messageWidth + 5;
        float keyWidth = 60;
        
        rectangle.render(ShapeProperties.create(context.getMatrices(), keyX, y + 5, keyWidth, itemHeight - 10)
                .round(6)
                .color(TempColor.getClientColor().alpha(alpha * 0.15f).getRGB())
                .build());
        
        Fonts.DEFAULT.get(12).drawCenteredString(context.getMatrices(), getKeyName(macro.getKey()),
                keyX + keyWidth / 2f, y + itemHeight / 2f - 3,
                TempColor.getTextPrimary().alpha(alpha).getRGB());
        
        // Кнопка "Удалить" (красная на скрине)
        float deleteX = keyX + keyWidth + 5;
        float deleteWidth = 70;
        
        boolean deleteHovered = mouseX >= deleteX && mouseX <= deleteX + deleteWidth
                && mouseY >= y + 5 && mouseY <= y + itemHeight - 5;
        
        // Красный цвет с hover эффектом
        int deleteBgR = deleteHovered ? 220 : 180;
        int deleteBgG = deleteHovered ? 60 : 50;
        int deleteBgB = deleteHovered ? 60 : 50;
        int deleteBgColor = new FixColor(deleteBgR, deleteBgG, deleteBgB, (int)(200 * alpha)).getRGB();
        
        rectangle.render(ShapeProperties.create(context.getMatrices(), deleteX, y + 5, deleteWidth, itemHeight - 10)
                .round(6)
                .color(deleteBgColor)
                .build());
        
        Fonts.DEFAULT.get(11).drawCenteredString(context.getMatrices(), "Удалить",
                deleteX + deleteWidth / 2f, y + itemHeight / 2f - 3,
                new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB());
    }
    
    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        
        farvix.solution.api.ui.clickgui.InterfaceScreen gui = getClickGUI();
        
        // Используем координаты от InterfaceScreen
        float guiX = gui.getX();
        float guiY = gui.getY();
        float guiWidth = gui.getWidth();
        
        float sidebarWidth = gui.getSidebar().getWidth();
        float startX = guiX + sidebarWidth + 10;
        float startY = guiY + 41; // Оптимальная высота
        float contentWidth = guiWidth - sidebarWidth - 20;
        float inputY = startY;
        float messageFieldWidth = contentWidth * 0.6f;
        float messageFieldHeight = 30;
        
        // Клик по полю ввода сообщения
        if (mouseX >= startX && mouseX <= startX + messageFieldWidth
                && mouseY >= inputY && mouseY <= inputY + messageFieldHeight) {
            messageInputFocused = true;
            selectingKey = false;
            return;
        }
        
        // Клик по кнопке выбора клавиши
        float keyButtonX = startX + messageFieldWidth + 5;
        float keyButtonWidth = contentWidth * 0.18f;
        if (mouseX >= keyButtonX && mouseX <= keyButtonX + keyButtonWidth
                && mouseY >= inputY && mouseY <= inputY + messageFieldHeight) {
            selectingKey = true;
            messageInputFocused = false;
            return;
        }
        
        // Клик по кнопке "Добавить"
        float addButtonX = keyButtonX + keyButtonWidth + 5;
        float addButtonWidth = contentWidth - messageFieldWidth - keyButtonWidth - 15;
        if (mouseX >= addButtonX && mouseX <= addButtonX + addButtonWidth
                && mouseY >= inputY && mouseY <= inputY + messageFieldHeight) {
            addMacro();
            return;
        }
        
        // Клик по кнопкам удаления в списке
        float separatorY = inputY + messageFieldHeight + 20;
        float listY = separatorY + 15;
        
        List<MacroManager.Macro> macros = Client.getInstance().macroManager.getMacros();
        
        for (int i = 0; i < macros.size(); i++) {
            float macroY = listY + i * 35;
            float messageWidth = contentWidth - 150;
            float deleteX = startX + messageWidth + 70;
            float deleteWidth = 70;
            
            if (mouseX >= deleteX && mouseX <= deleteX + deleteWidth
                    && mouseY >= macroY + 5 && mouseY <= macroY + 25) {
                Client.getInstance().macroManager.removeMacro(macros.get(i));
                return;
            }
        }
        
        // Клик вне элементов - снять фокус
        messageInputFocused = false;
    }
    
    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectingKey) {
            // Выбор клавиши
            if (keyCode == 256) { // ESC - отмена
                selectingKey = false;
            } else {
                selectedKey = keyCode;
                selectingKey = false;
            }
            return;
        }
        
        if (messageInputFocused) {
            // Ввод текста
            if (keyCode == 259) { // Backspace
                if (!messageInput.isEmpty()) {
                    messageInput = messageInput.substring(0, messageInput.length() - 1);
                }
            } else if (keyCode == 257) { // Enter
                addMacro();
            }
        }
    }
    
    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (messageInputFocused) {
            messageInput += codePoint;
        }
    }
    
    private void addMacro() {
        if (!messageInput.isEmpty() && selectedKey != -1) {
            Client.getInstance().macroManager.addMacro(messageInput, selectedKey);
            // Сброс полей
            messageInput = "";
            selectedKey = -1;
            messageInputFocused = false;
        }
    }
    
    private String getKeyName(int keyCode) {
        if (keyCode == -1) return "NONE";
        // Используем ту же логику что в ModuleComponent
        switch (keyCode) {
            case 32: return "SPACE";
            case 257: return "ENTER";
            case 258: return "TAB";
            case 259: return "BACKSPACE";
            case 262: return "RIGHT";
            case 263: return "LEFT";
            case 264: return "DOWN";
            case 265: return "UP";
            default: {
                if (keyCode >= 65 && keyCode <= 90) {
                    return String.valueOf((char) keyCode);
                } else if (keyCode >= 48 && keyCode <= 57) {
                    return String.valueOf((char) keyCode);
                } else if (keyCode >= 290 && keyCode <= 301) {
                    return "F" + (keyCode - 289);
                } else {
                    return "KEY_" + keyCode;
                }
            }
        }
    }
    
    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}
