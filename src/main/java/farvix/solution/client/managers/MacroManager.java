package farvix.solution.client.managers;

import lombok.Getter;
import farvix.solution.api.events.impl.input.EventInput;
import farvix.solution.api.interfaces.QuickImports;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер макросов - управляет чат-макросами (биндами сообщений на клавиши)
 */
@Getter
public class MacroManager implements QuickImports {
    
    private static MacroManager instance;
    
    private final List<Macro> macros = new ArrayList<>();
    
    public MacroManager() {
        instance = this;
    }
    
    public static MacroManager getInstance() {
        if (instance == null) {
            instance = new MacroManager();
        }
        return instance;
    }
    
    /**
     * Добавить новый макрос
     */
    public void addMacro(String message, int key) {
        // Проверяем, нет ли уже макроса на эту клавишу
        removeMacro(key);
        macros.add(new Macro(message, key));
    }
    
    /**
     * Удалить макрос по клавише
     */
    public void removeMacro(int key) {
        macros.removeIf(macro -> macro.key == key);
    }
    
    /**
     * Удалить макрос по объекту
     */
    public void removeMacro(Macro macro) {
        macros.remove(macro);
    }
    
    /**
     * Получить макрос по клавише
     */
    public Macro getMacro(int key) {
        return macros.stream()
                .filter(macro -> macro.key == key)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Очистить все макросы
     */
    public void clear() {
        macros.clear();
    }
    
    /**
     * Обработчик нажатий клавиш
     */
    @EventHandler
    public void onInput(EventInput event) {
        // Игнорируем если открыт GUI (кроме ClickUI)
        if (mc.currentScreen != null && !(mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen)) {
            return;
        }
        
        // Игнорируем если открыт чат
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            return;
        }
        
        // Проверяем нажатие клавиши
        if (event.isPressed(event.getKey())) {
            Macro macro = getMacro(event.getKey());
            if (macro != null && mc.player != null && mc.player.networkHandler != null) {
                // Отправляем сообщение в чат
                mc.player.networkHandler.sendChatMessage(macro.message);
            }
        }
    }
    
    /**
     * Класс макроса
     */
    @Getter
    public static class Macro {
        private final String message;
        private final int key;
        
        public Macro(String message, int key) {
            this.message = message;
            this.key = key;
        }
    }
}
