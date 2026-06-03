package farvix.solution.api.ui.contextmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Примеры использования контекстного меню
 */
public class ContextMenuExample {
    
    /**
     * Простое меню с базовыми действиями
     */
    public static ContextMenu createSimpleMenu() {
        ContextMenu menu = new ContextMenu();
        
        menu.addItem("Действие 1", () -> {
            sendMessage("Выполнено действие 1");
        });
        
        menu.addItem("Действие 2", () -> {
            sendMessage("Выполнено действие 2");
        });
        
        return menu;
    }
    
    /**
     * Меню для работы с текстом
     */
    public static ContextMenu createTextMenu() {
        ContextMenu menu = new ContextMenu();
        
        menu.addItem("📋 Копировать", () -> {
            // Логика копирования
            sendMessage("§aТекст скопирован!");
        });
        
        menu.addItem("📝 Вставить", () -> {
            // Логика вставки
            sendMessage("§aТекст вставлен!");
        });
        
        menu.addItem("✂ Вырезать", () -> {
            // Логика вырезания
            sendMessage("§aТекст вырезан!");
        });
        
        menu.addItem("🔍 Найти", () -> {
            // Открыть поиск
            sendMessage("§eОткрыт поиск");
        });
        
        return menu;
    }
    
    /**
     * Меню для игрока
     */
    public static ContextMenu createPlayerMenu(String playerName) {
        ContextMenu menu = new ContextMenu();
        
        menu.addItem("👤 Профиль " + playerName, () -> {
            sendMessage("§bОткрыт профиль: " + playerName);
        });
        
        menu.addItem("💬 Написать", () -> {
            sendMessage("§eОткрыт чат с: " + playerName);
        });
        
        menu.addItem("➕ Добавить в друзья", () -> {
            sendMessage("§a" + playerName + " добавлен в друзья!");
        });
        
        menu.addItem("🚫 Заблокировать", () -> {
            sendMessage("§c" + playerName + " заблокирован");
        });
        
        return menu;
    }
    
    /**
     * Меню настроек
     */
    public static ContextMenu createSettingsMenu() {
        ContextMenu menu = new ContextMenu();
        
        menu.addItem("⚙ Основные", () -> {
            sendMessage("§eОткрыты основные настройки");
        });
        
        menu.addItem("🎨 Внешний вид", () -> {
            sendMessage("§eОткрыты настройки внешнего вида");
        });
        
        menu.addItem("🔊 Звук", () -> {
            sendMessage("§eОткрыты настройки звука");
        });
        
        menu.addItem("🎮 Управление", () -> {
            sendMessage("§eОткрыты настройки управления");
        });
        
        menu.addItem("💾 Сохранить", () -> {
            sendMessage("§aНастройки сохранены!");
        });
        
        return menu;
    }
    
    /**
     * Меню для предмета
     */
    public static ContextMenu createItemMenu(String itemName) {
        ContextMenu menu = new ContextMenu();
        
        menu.addItem("ℹ Информация", () -> {
            sendMessage("§bИнформация о: " + itemName);
        });
        
        menu.addItem("📦 Положить в сундук", () -> {
            sendMessage("§a" + itemName + " положен в сундук");
        });
        
        menu.addItem("🗑 Выбросить", () -> {
            sendMessage("§c" + itemName + " выброшен");
        });
        
        menu.addItem("🔧 Починить", () -> {
            sendMessage("§a" + itemName + " починен!");
        });
        
        return menu;
    }
    
    /**
     * Меню быстрых действий
     */
    public static ContextMenu createQuickActionsMenu() {
        ContextMenu menu = new ContextMenu();
        
        menu.addItem("🏠 Телепорт домой", () -> {
            sendMessage("§aТелепортация домой...");
        });
        
        menu.addItem("💰 Баланс", () -> {
            sendMessage("§eВаш баланс: 1000 монет");
        });
        
        menu.addItem("📊 Статистика", () -> {
            sendMessage("§bОткрыта статистика");
        });
        
        menu.addItem("🎁 Награды", () -> {
            sendMessage("§6Открыты награды");
        });
        
        menu.addItem("🔄 Обновить", () -> {
            sendMessage("§aДанные обновлены!");
        });
        
        return menu;
    }
    
    /**
     * Меню с условными пунктами
     */
    public static ContextMenu createConditionalMenu(boolean isAdmin, boolean isPremium) {
        ContextMenu menu = new ContextMenu();
        
        // Обычные пункты для всех
        menu.addItem("📋 Основное меню", () -> {
            sendMessage("§eОсновное меню");
        });
        
        // Пункты для премиум пользователей
        if (isPremium) {
            menu.addItem("⭐ Премиум функции", () -> {
                sendMessage("§6Премиум функции доступны!");
            });
        }
        
        // Пункты для администраторов
        if (isAdmin) {
            menu.addItem("🔧 Админ панель", () -> {
                sendMessage("§cОткрыта админ панель");
            });
            
            menu.addItem("👥 Управление игроками", () -> {
                sendMessage("§cОткрыто управление игроками");
            });
        }
        
        return menu;
    }
    
    /**
     * Вспомогательный метод для отправки сообщений
     */
    private static void sendMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }
}
