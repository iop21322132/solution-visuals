# Руководство: Добавление Контекстного Меню для HUD Элементов

## Обзор

Система контекстных меню теперь поддерживает HUD элементы (окна функций на экране). При ПКМ по HUD элементу открывается меню с опциями "Настроить" и "Удалить".

## Как Это Работает

1. **HUD модуль** реализует интерфейс `IHudElement`
2. **При включении** модуль регистрируется в `HudElementRegistry`
3. **При ПКМ** система проверяет все зарегистрированные HUD элементы
4. **Если клик попал** в границы элемента - открывается меню модуля

## Добавление Поддержки в HUD Модуль

### Шаг 1: Реализовать IHudElement

```java
import farvix.solution.api.ui.hud.IHudElement;

@ModuleInfo(name = "My HUD", category = ModuleCategory.VISUALS)
public class MyHudModule extends Module implements QuickImports, IHudElement {
    
    // Координаты и размеры HUD элемента
    private float hudX = 10;
    private float hudY = 60;
    private float hudWidth = 150;
    private float hudHeight = 50;
    
    // Реализация IHudElement
    @Override
    public float getHudX() {
        return hudX;
    }
    
    @Override
    public float getHudY() {
        return hudY;
    }
    
    @Override
    public float getHudWidth() {
        return hudWidth;
    }
    
    @Override
    public float getHudHeight() {
        return hudHeight;
    }
    
    @Override
    public Module getModule() {
        return this;
    }
}
```

### Шаг 2: Регистрация в Реестре

```java
import farvix.solution.api.ui.hud.HudElementRegistry;

@Override
public void onEnable() {
    super.onEnable();
    // Регистрируем HUD элемент
    HudElementRegistry.getInstance().register(this);
}

@Override
public void onDisable() {
    super.onDisable();
    // Удаляем из реестра
    HudElementRegistry.getInstance().unregister(this);
}
```

### Шаг 3: Обновление Размеров

Если размеры HUD элемента меняются динамически (например при масштабировании), обновляйте поля `hudWidth` и `hudHeight` в методе рендера:

```java
@EventHandler
public void onRender2D(EventRender2D e) {
    // ... рендер кода ...
    
    // Обновляем размеры после рендера
    this.hudWidth = actualWidth * scale.getValue();
    this.hudHeight = actualHeight * scale.getValue();
}
```

## Полный Пример: TargetHud

```java
@ModuleInfo(name = "Target HUD", category = ModuleCategory.VISUALS)
public class TargetHud extends Module implements QuickImports, IHudElement {
    
    // Настройки
    public final SliderSetting scale = new SliderSetting("Scale", this, 1.0f, 0.5f, 2.0f, 0.05f);
    
    // Позиция и размеры
    public static float hudX = 10;
    public static float hudY = 60;
    private float hudWidth = 150;
    private float hudHeight = 50;
    
    // Drag state
    private boolean dragging = false;
    private double dragOffX, dragOffY;
    
    @Override
    public void onEnable() {
        super.onEnable();
        HudElementRegistry.getInstance().register(this);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        HudElementRegistry.getInstance().unregister(this);
    }
    
    @EventHandler
    public void onRender2D(EventRender2D e) {
        // Рендер HUD элемента
        float s = scale.getValue();
        float baseWidth = 150;
        float baseHeight = 50;
        
        // ... рендер код ...
        
        // Обновляем размеры с учетом масштаба
        this.hudWidth = baseWidth * s;
        this.hudHeight = baseHeight * s;
        
        // Обработка перетаскивания
        handleDragging();
    }
    
    private void handleDragging() {
        // ... код перетаскивания ...
        // При перетаскивании обновляйте hudX и hudY
    }
    
    // Реализация IHudElement
    @Override
    public float getHudX() {
        return hudX;
    }
    
    @Override
    public float getHudY() {
        return hudY;
    }
    
    @Override
    public float getHudWidth() {
        return hudWidth;
    }
    
    @Override
    public float getHudHeight() {
        return hudHeight;
    }
    
    @Override
    public Module getModule() {
        return this;
    }
}
```

## Важные Замечания

### 1. Координаты

- `getHudX()` и `getHudY()` должны возвращать **экранные координаты** (не трансформированные)
- Если используется масштабирование через MatrixStack, учитывайте это при расчете границ

### 2. Размеры

- `getHudWidth()` и `getHudHeight()` должны возвращать **реальные размеры** на экране
- Если есть масштабирование - умножайте базовые размеры на scale
- Обновляйте размеры каждый кадр если они могут меняться

### 3. Перетаскивание

- При перетаскивании обновляйте `hudX` и `hudY`
- Система автоматически будет использовать новые координаты для проверки кликов

### 4. Z-Index

- HUD элементы проверяются в обратном порядке регистрации
- Последний зарегистрированный элемент имеет приоритет
- Это означает что элементы "сверху" будут кликаться первыми

## Тестирование

1. **Включите модуль** - HUD элемент должен появиться на экране
2. **ПКМ по элементу** - должно открыться меню с "Настроить" и "Удалить"
3. **Клик "Настроить"** - должны раскрыться настройки модуля в ClickGUI
4. **Клик "Удалить"** - модуль должен выключиться
5. **ПКМ вне элемента** - должно открыться обычное меню (Копировать, Вставить...)

## Список HUD Модулей для Обновления

Добавьте поддержку `IHudElement` в следующие модули:

- [ ] TargetHud
- [ ] ArmorHUD
- [ ] InventoryHUD
- [ ] HudInventory
- [ ] ArrayList (если это HUD элемент)
- [ ] Potions (если существует)
- [ ] Другие HUD модули...

## API Классы

### IHudElement
`src/main/java/farvix/solution/api/ui/hud/IHudElement.java`

Интерфейс для HUD элементов с методами:
- `float getHudX()`
- `float getHudY()`
- `float getHudWidth()`
- `float getHudHeight()`
- `Module getModule()`
- `boolean isHovered(double mouseX, double mouseY)` (default)

### HudElementRegistry
`src/main/java/farvix/solution/api/ui/hud/HudElementRegistry.java`

Singleton реестр для управления HUD элементами:
- `register(IHudElement)` - зарегистрировать элемент
- `unregister(IHudElement)` - удалить элемент
- `findElementAt(double x, double y)` - найти элемент по координатам
- `getElements()` - получить все элементы
- `clear()` - очистить реестр

### ContextMenuManager
`src/main/java/farvix/solution/api/ui/contextmenu/ContextMenuManager.java`

Singleton менеджер контекстных меню:
- `openContextMenu(float x, float y)` - открыть обычное меню
- `openModuleContextMenu(Module, float x, float y)` - открыть меню модуля
- `closeAll()` - закрыть все меню
- `hasOpenMenu()` - проверка открытых меню
- `render(DrawContext, int mouseX, int mouseY)` - рендер меню
- `mouseClicked(double x, double y, int button)` - обработка кликов
- `keyPressed(int keyCode)` - обработка клавиш

## Troubleshooting

### Меню не открывается при ПКМ

1. Проверьте что модуль реализует `IHudElement`
2. Проверьте что модуль зарегистрирован в `HudElementRegistry`
3. Проверьте что `getHudX/Y/Width/Height` возвращают правильные значения
4. Добавьте debug логирование в `isHovered()` чтобы увидеть координаты

### Меню открывается не в том месте

1. Проверьте что координаты не трансформированы (не применен scale через MatrixStack)
2. Проверьте что размеры учитывают масштабирование

### Клик проходит "сквозь" элемент

1. Проверьте порядок регистрации элементов
2. Проверьте что элемент не перекрывается другим элементом
3. Проверьте что `isHovered()` возвращает `true` при клике

## Будущие Улучшения

- [ ] Добавить визуальную подсветку при наведении на HUD элемент
- [ ] Добавить возможность блокировки позиции HUD элементов
- [ ] Добавить сетку для выравнивания HUD элементов
- [ ] Добавить предпросмотр всех HUD элементов в специальном режиме
