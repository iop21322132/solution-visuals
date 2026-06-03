# PVP Safe Module

## Описание
Модуль **PVP Safe** предотвращает случайный выход из игры или использование команды `/hub` во время боя (PVP режима).

## Функциональность

### Определение боя
Модуль использует ту же логику определения боя, что и **Dynamic Island**:
- Проверяет наличие boss bar с текстом "pvp" или "пвп"
- Автоматически определяет, когда игрок входит в бой и выходит из него
- Работает в фоновом режиме без уведомлений в чат

### Блокировки

1. **Команда /hub**
   - Блокируется как через чат (`/hub`)
   - Так и через прямой вызов команды (`hub`)
   - При попытке использования показывается сообщение: `§c[PVP Safe] §fНельзя использовать /hub во время боя!`

2. **Выход из игры**
   - Блокируется на уровне MinecraftClient.disconnect()
   - Блокируется кнопка "Disconnect" в меню паузы
   - Блокируется метод disconnect в GameMenuScreen
   - При попытке выхода показывается сообщение: `§c[PVP Safe] §fНельзя выйти из игры во время боя!`

### Отладка

Модуль выводит в консоль отладочную информацию:
- Все обнаруженные boss bar'ы
- Когда обнаружен PVP режим
- Когда заблокирована попытка disconnect

## Технические детали

### Файлы
- `src/main/java/farvix/solution/client/modules/impl/environment/PVPSafe.java` - основной модуль
- `src/main/java/farvix/solution/mixins/ClientPlayNetworkHandlerMixin.java` - блокировка команд
- `src/main/java/farvix/solution/mixins/GameMenuScreenMixin.java` - блокировка кнопки выхода
- `src/main/java/farvix/solution/mixins/MinecraftClientMixin.java` - блокировка disconnect на уровне клиента

### Логика определения боя
```java
public boolean isPvpMode() {
    if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) {
        return false;
    }
    
    for (ClientBossBar bossBar : ((IBossBarHud) mc.inGameHud.getBossBarHud()).getBossBars().values()) {
        String name = bossBar.getName().getString().toLowerCase();
        if (name.contains("pvp") || name.contains("пвп")) {
            return true;
        }
    }
    return false;
}
```

## Использование

1. Включите модуль в категории **Environment**
2. Модуль автоматически начнет отслеживать состояние боя
3. Все блокировки будут применяться автоматически во время боя
4. После выхода из боя все функции разблокируются
5. Проверьте консоль для отладочной информации о boss bar'ах

## Категория
**Environment** - модули для улучшения игрового окружения и безопасности
