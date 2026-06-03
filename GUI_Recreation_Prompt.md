# Промт для точного воссоздания GUI меню - ToolRise mod ClickGUI

## Общие требования к GUI
- **Тип приложения**: Minecraft мод (Fabric)
- **Фреймворк/Технология**: Minecraft Fabric 1.21.4, Java, LWJGL
- **Разрешение окна**: 380x237.5 пикселей (фиксированное)
- **Цветовая схема**: Темная с акцентным цветом клиента

## Структура меню

### Главное окно (InterfaceScreen)
- **Размеры**: 380x237.5 пикселей
- **Позиция**: Центр экрана
- **Фон**: TempColor.getGuiBackground() с blur эффектом
- **Рамка**: 2.5px толщина, TempColor.getGuiBorder(), радиус скругления 12px
- **Прозрачность**: Анимированная alpha от 0 до 1 (300ms, EASE_OUT_CUBIC)
- **Масштабирование**: Анимированный scale от 0.8 до 1 (300ms, EASE_OUT_CUBIC)

### Заголовок
- **Текст**: "ToolRise Mod"
- **Шрифт**: Fonts.DEFAULT.get(16)
- **Цвет**: TempColor.getTextPrimary()
- **Позиция**: Центрированный в header области sidebar
- **Выравнивание**: По центру

## Боковая панель (Sidebar)

### Размеры и позиция
- **Ширина**: 169/2f + 15 = 99.5 пикселей
- **Высота**: Полная высота GUI (237.5px)
- **Позиция**: Левая сторона GUI (x = GUI.x, y = GUI.y)
- **Фон**: TempColor.getSidebarBackground() с blur эффектом
- **Скругление**: Только левые углы (радиус 12px)

### Элементы sidebar

#### Header область
- **Высота**: 24px
- **Фон**: TempColor.getSidebarBackground()
- **Заголовок**: "ToolRise", Fonts.DEFAULT.get(16), центрированный
- **Разделитель**: Горизонтальная линия 1px, TempColor.getSeparatorHorizontal()

#### Категории модулей
- **Список категорий**: MOVEMENT, COMBAT, RENDER, PLAYER, WORLD, MISC, CONFIG
- **Размер таба**: 70x14 пикселей
- **Отступ между табами**: 22px
- **Hover анимация**: 200ms, EASE_IN_OUT_SINE
- **Активный таб**: Подсветка фоном TempColor.getModuleBackground()
- **Иконки**: Fonts.ICONS.get(14) + текст Fonts.DEFAULT.get(14)

#### User Info (внизу)
- **Позиция**: Внизу sidebar, отступ 25px от края
- **Аватар**: 16x16px, скругленный, TempColor.getAvatarColor()
- **Статус индикатор**: 4x4px зеленый кружок
- **Имя**: "ToolRise", Fonts.DEFAULT.get(12)
- **UID**: "UID: 1337", Fonts.DEFAULT.get(12), TempColor.getTextSecondary()

## Поиск (SearchBar)

### Размеры и позиция
- **Размеры**: 69x13 пикселей
- **Позиция**: Справа от sidebar (x = GUI.x + sidebar.width + 16, y = GUI.y + 5.5)
- **Фон**: TempColor.getSearchBackground()
- **Рамка**: TempColor.getSearchBorderUnfocused() / TempColor.getClientColor() (при фокусе)
- **Скругление**: 4px
- **Placeholder**: "search.placeholder" (переводимый)
- **Иконка поиска**: "u" из Fonts.ICONS.get(10)
- **Максимум символов**: 32
- **Курсор**: Мигающий каждые 500ms при фокусе
- **Горячая клавиша**: Ctrl+F для фокуса

## Компоненты модулей (ModuleComponent)

### Основной компонент модуля
- **Размеры**: 119.5x20 пикселей (базовая высота)
- **Расположение**: Двухколоночная сетка с отступом 8px
- **Фон**: TempColor.getModuleBackground()
- **Рамка**: 2.5px + hover эффект (до 4px), TempColor.getModuleBorder()
- **Скругление**: 8px (верх), 0px (низ если есть настройки)
- **Hover анимация**: 200ms, EASE_IN_OUT_SINE
- **Toggle анимация**: 750ms, EASE_IN_OUT_QUINT

### Элементы модуля
#### Название модуля
- **Шрифт**: Fonts.DEFAULT.get(14)
- **Позиция**: x+5, y+5
- **Цвет**: TempColor.getTextPrimary()

#### Кнопка бинда клавиши
- **Размеры**: Динамическая ширина (мин. 16px) x 10px
- **Позиция**: Правый верхний угол модуля
- **Фон**: TempColor.getKeyBackground()
- **Рамка**: TempColor.getKeyBorder()
- **Текст**: Fonts.DEFAULT.get(12), TempColor.getKeyText()
- **Скругление**: 2px

### Настройки модуля

#### Boolean Setting (BooleanSettingComponent)
- **Размеры**: 119.5x10 пикселей
- **Toggle**: 9x9px квадрат, скругление 2px
- **Позиция toggle**: x+5, y-3
- **Цвета**:
  - Включено: TempColor.getClientColor() фон, черная галочка
  - Выключено: TempColor.getModuleBackground() фон, белая галочка
- **Иконки**: "9" (включено), "0" (выключено) из Fonts.ICONS.get(8)
- **Текст**: Fonts.DEFAULT.get(14), справа от toggle

#### Slider Setting (SliderSettingComponent)
- **Размеры**: 119.5x15 пикселей
- **Слайдер**: Ширина = width-10, высота = 2.5px
- **Позиция**: x+5, y+8
- **Фон трека**: TempColor.getModuleBackground()
- **Активная часть**: TempColor.getClientColor()
- **Ползунок**: 4.5x4.5px круг, белый с цветной обводкой
- **Значение**: Fonts.DEFAULT.get(11), справа вверху
- **Dragging**: Поддержка перетаскивания мышью

#### Mode Setting (ModeSettingComponent)
- **Размеры**: 119.5x(динамическая высота)
- **Режимы**: Кнопки в сетке, ширина по тексту + 8px, высота 9px
- **Активный режим**: TempColor.getClientColor() фон
- **Неактивный режим**: TempColor.getGuiBackground() фон
- **Текст**: Fonts.DEFAULT.get(12), центрированный
- **Скругление**: 2px

#### String Setting (StringSettingComponent)
- **Размеры**: 119.5x20 пикселей
- **Поле ввода**: width-10 x 16px
- **Позиция поля**: x+5, y+2
- **Фон**: TempColor.getModuleBackground() (прозрачность зависит от фокуса)
- **Рамка**: TempColor.getKeyBorder() / TempColor.getClientColor() (при фокусе)
- **Курсор**: Мигающая линия 1x12px
- **Максимум символов**: Настраиваемый (StringSetting.maxLength)

#### Bind Setting (BindSettingComponent)
- **Размеры**: 119.5x10 пикселей
- **Кнопка бинда**: Динамическая ширина x 16px
- **Позиция**: Справа, x + width - buttonWidth - 5, y - 3
- **Состояния**:
  - Обычное: TempColor.getModuleBackground() фон
  - Binding: TempColor.getClientColor() фон
- **Клавиши**: Обработка ESC/DELETE для сброса

## Экраны и навигация

### ModuleScreen (основной экран модулей)
- **Позиция**: x = GUI.x + 55, y = GUI.y
- **Размеры**: Занимает оставшуюся область GUI
- **Прокрутка**: ScrollUtility с плавной анимацией
- **Сетка модулей**: 2 колонки, отступ 8px между модулями
- **Scissor**: Обрезка контента по границам GUI
- **Поиск**: Фильтрация по имени и описанию модулей

### ConfigScreen (экран конфигураций)
- **Заголовок**: "Config Manager", Fonts.DEFAULT.get(14)
- **Кнопки управления**: "Create" и "Reset" в контейнере
- **Список конфигов**: Прокручиваемый список ConfigComponent
- **Боковая панель создания**: 125px ширина, slide анимация
- **Поле ввода**: Для имени новой конфигурации
- **Кнопки**: Create, Clean, Cfg Dir

### ConfigComponent (элемент конфига)
- **Размеры**: Динамическая ширина x 55px высота
- **Информация**: Имя конфига, дата изменения
- **Кнопки**: "Load" (70px) и "Delete" (70px)
- **Hover эффекты**: Изменение прозрачности фона
- **Цвета**: Load - синий, Delete - красный

## Визуальные элементы

### Разделители
- **Горизонтальный**: y + 24px, ширина = GUI.width, высота 1px
- **Вертикальный**: x + sidebar.width, высота = GUI.height - 24px, ширина 1px
- **Цвета**: TempColor.getSeparatorHorizontal() / TempColor.getSeparatorVertical()

### Blur эффекты
- **Основной фон**: ShapeProperties с blur, softness 1.5f
- **Компоненты**: Различные уровни blur для глубины
- **Толщина обводки**: 2.5px базовая, до 4px при hover

### Иконки
- **Шрифт иконок**: Fonts.ICONS различных размеров (8, 10, 14)
- **Поиск**: "u"
- **Boolean**: "9" (включено), "0" (выключено)
- **Категории**: Уникальные иконки для каждой категории

## Логика и функционал

### Обработчики событий
```java
// Клик по модулю
@Override
public void mouseClicked(double mouseX, double mouseY, int button) {
    if (hovered) {
        switch (button) {
            case 0 -> module.toggle(); // ЛКМ - переключить модуль
            case 1 -> { /* ПКМ - открыть настройки */ }
            case 2 -> binding = true; // СКМ - биндинг клавиши
        }
    }
}

// Обработка клавиш для биндинга
@Override
public void keyPressed(int keyCode, int scanCode, int modifiers) {
    if (binding) {
        if (keyCode == 256 || keyCode == 261) { // ESC/DELETE
            module.setKey(-1);
        } else {
            module.setKey(keyCode);
        }
        binding = false;
    }
}
```

### Связи между элементами
- **Поиск влияет на список модулей**: Фильтрация по тексту поиска
- **Категория влияет на отображение**: Показ только модулей выбранной категории
- **Настройки зависят от условий**: HideCondition для условного отображения
- **Анимации связаны с состоянием**: Toggle, hover, focus анимации

### Сохранение настроек
- **Файл конфигурации**: JSON формат в папке configs/
- **Сохраняемые значения**: Состояние модулей, значения настроек, бинды клавиш
- **Автосохранение**: При изменении любой настройки
- **Загрузка**: При запуске клиента и при выборе конфига

## Анимации и эффекты

### Появление GUI
- **Тип**: Fade in + Scale up
- **Alpha**: 0 → 1 (300ms, EASE_OUT_CUBIC)
- **Scale**: 0.8 → 1 (300ms, EASE_OUT_CUBIC)
- **Центрирование**: Масштабирование относительно центра

### Переключение категорий
- **Тип**: Vertical slide + Fade
- **Длительность**: 300ms, EASE_OUT_CUBIC
- **Направление**: Вверх/вниз в зависимости от порядка категорий
- **Offset**: ±30 пикселей по Y оси

### Hover эффекты
- **Модули**: Увеличение толщины обводки (2.5px → 4px)
- **Кнопки**: Изменение прозрачности фона
- **Категории**: Подсветка фоном с анимацией 200ms
- **Настройки**: Плавное изменение цветов

### Toggle анимации
- **Модули**: 750ms, EASE_IN_OUT_QUINT
- **Boolean настройки**: Мгновенное переключение иконок
- **Цветовые переходы**: Плавное изменение цветов фона

## Адаптивность и масштабирование

### Поведение при изменении размера окна
- **Фиксированные размеры**: Да, 380x237.5 пикселей
- **Позиционирование**: Всегда по центру экрана
- **Масштабирование**: Фиксированное, не адаптивное

### Координатная система
- **Базовое разрешение**: Любое (GUI всегда фиксированного размера)
- **Центрирование**: (window.width - GUI.width) / 2, (window.height - GUI.height) / 2

## Горячие клавиши

### Клавиатурные сокращения
- **ESC**: Закрытие GUI (если нет активного фокуса)
- **Ctrl+F**: Фокус на поле поиска
- **Enter**: Подтверждение ввода в текстовых полях
- **Backspace**: Удаление символов в полях ввода
- **Delete/ESC**: Сброс биндинга клавиши (в режиме биндинга)

## Звуковые эффекты

### Звуки интерфейса
- **Звуки отсутствуют**: GUI работает без звуковых эффектов
- **Возможность добавления**: Через систему событий Minecraft

## Локализация

### Поддерживаемые языки
- **Система переводов**: Translations.tr() для всех текстов
- **Файлы переводов**: Поддержка через систему переводов клиента
- **Переводимые элементы**: Названия настроек, placeholder', кнопки

## Технические детали

### Зависимости
```gradle
// Основные зависимости для GUI
dependencies {
    minecraft "com.mojang:minecraft:1.20.1"
    mappings "net.fabricmc:yarn:1.20.1+build.10:v2"
    modImplementation "net.fabricmc:fabric-loader:0.14.21"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.83.0+1.20.1"
    
    // Для анимаций и рендеринга
    implementation 'org.joml:joml:1.10.5'
    compileOnly 'org.projectlombok:lombok:1.18.28'
    annotationProcessor 'org.projectlombok:lombok:1.18.28'
}
```

### Структура файлов
```
src/main/java/com/toolrise/
├── api/
│   ├── ui/
│   │   ├── clickgui/
│   │   │   ├── InterfaceScreen.java          // Главный GUI класс
│   │   │   ├── ModuleComponent.java          // Компонент модуля
│   │   │   ├── api/
│   │   │   │   ├── MenuScreen.java           // Базовый класс экранов
│   │   │   │   ├── MenuComponent.java        // Базовый компонент
│   │   │   │   ├── SettingComponent.java     // Базовый класс настроек
│   │   │   │   └── CustomElement.java        // Кастомные элементы
│   │   │   ├── impl/
│   │   │   │   ├── module/
│   │   │   │   │   └── ModuleScreen.java     // Экран модулей
│   │   │   │   ├── sidebar/
│   │   │   │   │   └── Sidebar.java          // Боковая панель
│   │   │   │   ├── config/
│   │   │   │   │   ├── ConfigScreen.java     // Экран конфигов
│   │   │   │   │   └── ConfigComponent.java  // Компонент конфига
│   │   │   │   └── settings/
│   │   │   │       ├── BooleanSettingComponent.java
│   │   │   │       ├── SliderSettingComponent.java
│   │   │   │       ├── ModeSettingComponent.java
│   │   │   │       ├── StringSettingComponent.java
│   │   │   │       ├── BindSettingComponent.java
│   │   │   │       ├── MultiModeSettingComponent.java
│   │   │   │       ├── ModeComponent.java
│   │   │   │       └── SubComponent.java
│   │   │   └── components/
│   │   │       └── SearchBar.java            // Поисковая строка
│   │   └── mainmenu/
│   │       └── MainMenu.java                 // Главное меню
│   ├── animation/
│   │   ├── Animation.java                    // Система анимаций
│   │   └── Easing.java                       // Типы плавности
│   ├── render/
│   │   ├── font/
│   │   │   └── Fonts.java                    // Система шрифтов
│   │   └── rect/
│   │       └── ShapeProperties.java          // Свойства фигур
│   └── TempColor.java                        // Цветовая система
```

### Основные классы

#### InterfaceScreen (главный GUI класс)
```java
public class InterfaceScreen extends Screen {
    // Основные поля
    private float x, y, width, height;
    private Animation alpha, scale, categoryTransition;
    private Sidebar sidebar;
    private MenuScreen currentScreen;
    
    // Ключевые методы
    public void init()                    // Инициализация GUI
    public void render()                  // Рендеринг
    public void switchScreen()            // Переключение экранов
    public void rebuildModules()          // Пересборка списка модулей
}
```

#### ModuleComponent (компонент модуля)
```java
public class ModuleComponent {
    // Основные поля
    private final Module module;
    private final List<SettingComponent> settings;
    private final Animation toggleAnimation, hoverAnimation;
    
    // Ключевые методы
    public void render()                  // Рендеринг модуля
    public void mouseClicked()            // Обработка кликов
    public float getTotalHeight()         // Расчет полной высоты
}
```

#### SettingComponent (базовый класс настроек)
```java
public abstract class SettingComponent {
    protected Setting setting;
    protected ModuleComponent moduleComponent;
    
    // Абстрактные методы
    public abstract void render();
    public abstract void mouseClicked();
    public abstract void init();
}
```

## Примеры кода

### Создание основного окна
```java
@Override
protected void init() {
    this.width = 380;
    this.height = 237.5f;
    this.x = (window.getScaledWidth() - width) / 2f;
    this.y = (window.getScaledHeight() - height) / 2f;
    
    this.alpha = new Animation(Easing.EASE_OUT_CUBIC, 300);
    this.scale = new Animation(Easing.EASE_OUT_CUBIC, 300);
    
    this.sidebar = new Sidebar();
    this.sidebar.init();
    
    this.alpha.run(1);
    this.scale.run(1);
}
```

### Рендеринг с blur эффектом
```java
blur.render(ShapeProperties.create(context.getMatrices(), x - 1, y - 1, width + 2, height + 2)
        .round(round)
        .softness(1.5f)
        .thickness(2.5f)
        .outlineColor(TempColor.getGuiBorder().alpha(alpha.getValue()).getRGB())
        .color(TempColor.getGuiBackground().alpha(alpha.getValue()).getRGB())
        .build());
```

### Обработка анимаций
```java
// В render методе
if (closing) {
    alpha.run(0);
    scale.run(0.8f);
} else {
    alpha.run(1);
    scale.run(1);
}

// Применение масштаба
context.getMatrices().translate(centerX, centerY, 0);
context.getMatrices().scale(currentScale, currentScale, 1);
context.getMatrices().translate(-centerX, -centerY, 0);
```

### Система цветов
```java
// Примеры использования цветовой системы
Color bgColor = TempColor.getGuiBackground().alpha(alpha).getColor();
Color textColor = TempColor.getTextPrimary().alpha(alpha).getColor();
Color clientColor = TempColor.getClientColor().alpha(alpha).getColor();
```

---

## Цветовая схема (TempColor система)

### Основные цвета
- **getClientColor()**: Акцентный цвет клиента (настраиваемый)
- **getGuiBackground()**: Основной фон GUI (темный)
- **getGuiBorder()**: Цвет рамки GUI
- **getSidebarBackground()**: Фон боковой панели
- **getModuleBackground()**: Фон компонентов модулей
- **getModuleBorder()**: Рамка модулей

### Текстовые цвета
- **getTextPrimary()**: Основной текст (белый/светлый)
- **getTextSecondary()**: Вторичный текст (серый)
- **getTextMenu()**: Текст заголовков меню
- **getTextComponent()**: Текст заголовков компонентов
- **getTextPlaceholder()**: Placeholder текст

### Специальные цвета
- **getSearchBackground()**: Фон поисковой строки
- **getSearchBorderUnfocused()**: Рамка поиска (неактивная)
- **getKeyBackground()**: Фон кнопок клавиш
- **getKeyBorder()**: Рамка кнопок клавиш
- **getKeyText()**: Текст на кнопках клавиш
- **getAvatarColor()**: Цвет аватара пользователя
- **getStatusColor()**: Цвет статус индикатора
- **getSeparatorHorizontal()**: Горизонтальные разделители
- **getSeparatorVertical()**: Вертикальные разделители
- **getCursorColor()**: Цвет курсора в текстовых полях

## Система шрифтов (Fonts)

### Основные шрифты
- **Fonts.DEFAULT**: Основной шрифт для текста
  - get(10): Мелкий текст
  - get(11): Значения слайдеров
  - get(12): Обычный текст, кнопки
  - get(13): Кнопки управления
  - get(14): Названия модулей, настроек
  - get(16): Заголовки
  - get(18): Крупные заголовки
  - get(20): Главный заголовок

- **Fonts.ICONS**: Иконочный шрифт
  - get(8): Мелкие иконки (toggle)
  - get(10): Иконка поиска
  - get(14): Иконки категорий

## Система анимаций

### Типы плавности (Easing)
- **EASE_OUT_CUBIC**: Для появления GUI, переключения экранов
- **EASE_IN_OUT_QUINT**: Для toggle анимаций модулей
- **EASE_IN_OUT_SINE**: Для hover эффектов

### Длительности
- **200ms**: Быстрые hover эффекты
- **300ms**: Появление GUI, переключение экранов
- **750ms**: Toggle анимации модулей

## Инструкция по использованию промта

1. **Точное воссоздание размеров**: Используй указанные пиксельные размеры
2. **Цветовая система**: Реализуй TempColor систему с alpha поддержкой
3. **Анимации**: Обязательно добавь все указанные анимации с правильными параметрами
4. **Структура классов**: Следуй указанной иерархии классов
5. **Обработка событий**: Реализуй все указанные обработчики мыши и клавиатуры
6. **Blur эффекты**: Используй ShapeProperties с blur для всех элементов
7. **Скругления**: Применяй указанные радиусы скругления
8. **Шрифты**: Используй правильные размеры шрифтов для каждого элемента

**Критически важно**: 
- Размеры GUI: 380x237.5px (фиксированные)
- Sidebar: 99.5px ширина
- Модули: 119.5px ширина, двухколоночная сетка
- Все анимации с указанными параметрами
- Цветовая система с alpha поддержкой
- Blur эффекты для всех элементов

**Результат**: Полностью функциональный ClickGUI для Minecraft мода с современным дизайном, плавными анимациями и всеми необходимыми компонентами настроек.