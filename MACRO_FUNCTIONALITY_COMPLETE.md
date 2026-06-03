# Функциональность Макросов - Полностью Реализована

## Проблема
Пользователь создавал макросы в GUI (сообщение + клавиша), но при нажатии клавиши сообщение не отправлялось в чат.

## Решение

### 1. Создан MacroManager
**Файл:** `src/main/java/farvix/solution/client/managers/MacroManager.java`

MacroManager - это менеджер, который:
- ✅ Хранит список всех макросов
- ✅ Слушает нажатия клавиш через EventInput
- ✅ Отправляет сообщения в чат при нажатии соответствующей клавиши
- ✅ Предоставляет API для добавления/удаления макросов

**Ключевые методы:**
```java
// Добавить макрос
addMacro(String message, int key)

// Удалить макрос по клавише
removeMacro(int key)

// Получить макрос по клавише
getMacro(int key)

// Обработчик нажатий клавиш
@EventHandler
onInput(EventInput event)
```

**Логика работы:**
1. При нажатии клавиши проверяется, есть ли макрос на эту клавишу
2. Если макрос найден, отправляется сообщение через `mc.player.networkHandler.sendChatMessage()`
3. Макросы НЕ работают когда открыт чат или другие GUI (кроме ClickUI)

### 2. Интеграция в Client.java
**Изменения:**
```java
// Добавлено поле
public farvix.solution.client.managers.MacroManager macroManager;

// Инициализация в onInitialize()
macroManager = new farvix.solution.client.managers.MacroManager();
bus.subscribe(macroManager); // Регистрируем для обработки EventInput
```

### 3. Обновлен MacroScreen
**Файл:** `src/main/java/farvix/solution/api/ui/clickgui/impl/macro/MacroScreen.java`

**Изменения:**
- ❌ Удален локальный список `List<MacroBind> macros`
- ✅ Теперь использует `Client.getInstance().macroManager.getMacros()`
- ✅ Добавление макроса: `Client.getInstance().macroManager.addMacro(message, key)`
- ✅ Удаление макроса: `Client.getInstance().macroManager.removeMacro(macro)`

## Как это работает

### Создание макроса:
1. Открываете ClickGUI (Right Shift)
2. Переходите в категорию "Macro" (⌨ внизу)
3. Вводите сообщение в поле
4. Нажимаете кнопку "KEY" и выбираете клавишу
5. Нажимаете "Добавить"
6. Макрос создан и сохранен в MacroManager

### Использование макроса:
1. Закрываете GUI
2. Нажимаете клавишу, которую привязали к макросу
3. Сообщение автоматически отправляется в чат

### Удаление макроса:
1. Открываете ClickGUI → Macro
2. Находите макрос в списке
3. Нажимаете красную кнопку "Удалить"

## Технические детали

### Обработка нажатий клавиш
```java
@EventHandler
public void onInput(EventInput event) {
    // Игнорируем если открыт GUI (кроме ClickUI)
    if (mc.currentScreen != null && !(mc.currentScreen instanceof InterfaceScreen)) {
        return;
    }
    
    // Игнорируем если открыт чат
    if (mc.currentScreen instanceof ChatScreen) {
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
```

### Отправка сообщений в чат
Используется стандартный Minecraft API:
```java
mc.player.networkHandler.sendChatMessage(message);
```

Это отправляет сообщение так, как если бы игрок сам написал его в чат.

## Что работает

✅ **Создание макросов** - через GUI
✅ **Удаление макросов** - через GUI
✅ **Отправка сообщений** - при нажатии клавиши
✅ **Защита от конфликтов** - не работает в чате и других GUI
✅ **Замена макросов** - если создать макрос на уже занятую клавишу, старый удаляется
✅ **Отображение списка** - все макросы видны в GUI
✅ **Адаптивный layout** - использует проценты, подстраивается под размер GUI

## Что еще можно добавить (опционально)

⏳ **Сохранение в конфиг** - макросы не сохраняются между сессиями
⏳ **Скроллинг** - если макросов много, нужна прокрутка
⏳ **Поддержка команд** - отличать команды (/) от обычных сообщений
⏳ **Задержка между сообщениями** - защита от спама
⏳ **Мультистроковые макросы** - отправка нескольких сообщений подряд

## Тестирование

1. Запустите игру
2. Откройте ClickGUI (Right Shift)
3. Перейдите в "Macro"
4. Создайте макрос: "Привет всем!" на клавишу H
5. Закройте GUI
6. Нажмите H
7. В чат должно отправиться "Привет всем!"

## Компиляция

✅ **BUILD SUCCESSFUL** - проект компилируется без ошибок
