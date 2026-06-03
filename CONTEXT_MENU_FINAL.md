# Context Menu - Финальная Реализация

## Функционал

### 1. Обычное Контекстное Меню (ContextMenu)
**Где:** Чат и пустое место вне ClickGUI

**Как открыть:** ПКМ по пустому месту

**Пункты меню:**
- 📋 Копировать
- 📝 Вставить
- ✂️ Вырезать
- 🔄 Обновить
- ⚙️ Настройки

**Анимации:**
- EASE_OUT_ELASTIC для bounce эффекта
- EASE_OUT_CUBIC для fade
- Ширина: 120px

### 2. Контекстное Меню Модуля (ModuleContextMenu)
**Где:** Модули в ClickGUI

**Как открыть:** ПКМ по заголовку модуля (верхняя часть карточки)

**Пункты меню:**
- ⚙️ Настроить - раскрывает настройки модуля (expanded = true)
- 🗑️ Удалить - выключает модуль (setEnabled(false))

**Анимации:**
- EASE_OUT_ELASTIC для scale (400ms)
- EASE_OUT_CUBIC для alpha (300ms)
- Ширина: 100px
- Высота пункта: 24px

## Логика Работы

### В ClickGUI:

1. **ПКМ по заголовку модуля** (верхняя часть карточки)
   - Открывается ModuleContextMenu
   - Показывает "Настроить" и "Удалить"

2. **ПКМ по настройкам модуля** (нижняя часть, когда раскрыто)
   - Обычное поведение настроек
   - Не открывает меню

3. **ПКМ по пустому месту в GUI**
   - Ничего не происходит

4. **ПКМ по пустому месту ВНЕ GUI**
   - Открывается обычное ContextMenu

### В Чате:

1. **ПКМ по любому месту**
   - Открывается обычное ContextMenu

## Технические Детали

### Координаты и Трансформация

**Проблема:** Модули рендерятся с offset 55 пикселей от левого края GUI, но система проверяла клики только по главному окну.

**Решение:** Добавлена проверка `clickedOnModuleArea`:
```java
boolean clickedOnModuleArea = transformedX >= x + 55 && transformedX <= x + width
        && transformedY >= y && transformedY <= y + height;
```

### Обработка Кликов

**InterfaceScreen.mouseClicked():**
1. Проверяет контекстное меню модуля (если открыто)
2. Трансформирует координаты с учетом scale
3. Проверяет клик по GUI, боковой панели или области модулей
4. Передает клик в currentScreen (ModuleScreen)

**ModuleScreen.mouseClicked():**
1. Обрабатывает клики по scrollbar
2. Передает клики всем ModuleComponent

**ModuleComponent.mouseClicked():**
1. Проверяет клик по заголовку (верхние 20px)
2. Проверяет клик по настройкам (если раскрыто)
3. ЛКМ по заголовку - toggle модуля
4. ПКМ по заголовку - открывает ModuleContextMenu
5. СКМ по заголовку - биндинг клавиши

### Анимации

**ContextMenu:**
- Scale: 0 → 1 (EASE_OUT_ELASTIC, 400ms)
- Alpha: 0 → 1 (EASE_OUT_CUBIC, 300ms)
- Wobble: EASE_OUT_SINE для дополнительного эффекта

**ModuleContextMenu:**
- Scale: 0 → 1 (EASE_OUT_ELASTIC, 400ms)
- Alpha: 0 → 1 (EASE_OUT_CUBIC, 300ms)

При каждом открытии анимации сбрасываются:
```java
animation.setValue(0f);
animation.setStartValue(0f);
animation.setDestinationValue(1f);
animation.setStartTime(System.currentTimeMillis());
animation.setFinished(false);
```

## Файлы

### Основные:
- `src/main/java/farvix/solution/api/ui/contextmenu/ContextMenu.java` - обычное меню
- `src/main/java/farvix/solution/api/ui/contextmenu/ModuleContextMenu.java` - меню модуля
- `src/main/java/farvix/solution/client/modules/impl/visuals/ContextMenuModule.java` - модуль управления

### Интеграция:
- `src/main/java/farvix/solution/api/ui/clickgui/InterfaceScreen.java` - обработка кликов в GUI
- `src/main/java/farvix/solution/api/ui/clickgui/impl/module/ModuleScreen.java` - передача кликов модулям
- `src/main/java/farvix/solution/api/ui/clickgui/ModuleComponent.java` - обработка кликов по модулю

## Использование

### Для Пользователя:

1. **Открыть обычное меню:**
   - Открыть чат (T)
   - ПКМ по любому месту
   - ИЛИ: Открыть ClickGUI, ПКМ вне окна GUI

2. **Открыть меню модуля:**
   - Открыть ClickGUI
   - Найти модуль (например "Potions")
   - ПКМ по заголовку модуля (верхняя часть карточки)
   - Выбрать "Настроить" или "Удалить"

3. **Настроить модуль:**
   - ПКМ по модулю → "Настроить"
   - Настройки раскроются автоматически
   - Меню закроется

4. **Выключить модуль:**
   - ПКМ по модулю → "Удалить"
   - Модуль выключится
   - Меню закроется

### Для Разработчика:

**Добавить пункт в обычное меню:**
```java
contextMenu.addItem("🔥 Новый пункт", () -> {
    // Действие при клике
});
```

**Изменить пункты меню модуля:**
Отредактировать `ModuleContextMenu.render()` и `mouseClicked()`:
```java
String[] items = {"⚙️ Настроить", "🗑️ Удалить", "🔥 Новое"};
```

## Известные Ограничения

1. **Topbar блокировка:** Клики в верхних 24px GUI блокируются (для searchbar)
2. **Scale трансформация:** Координаты мыши трансформируются с учетом анимации масштаба
3. **Scissor область:** Модули рендерятся внутри scissor, что может обрезать меню если оно выходит за границы

## Будущие Улучшения

- [ ] Добавить больше пунктов в меню модуля (Дублировать, Экспорт настроек)
- [ ] Добавить подменю для сложных действий
- [ ] Добавить иконки вместо emoji
- [ ] Добавить звуковые эффекты при открытии/закрытии
- [ ] Добавить настройку позиции меню (чтобы не выходило за границы экрана)
