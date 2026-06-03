# ClickGUI Redesign Plan - SolutionVisual Style (Reference)

## 🎯 Цель Проекта

Полностью переделать существующий ClickGUI в стиле **SolutionVisual** (референс на скриншоте) с сохранением только названий модулей и категорий. Все остальное - меню, навигация, визуальные эффекты, анимации - создается с нуля по референсу.

---

## 📋 Что Сохраняем (НЕ ТРОГАЕМ)

### ✅ Категории Модулей (НАШИ)
- **Movement** (иконка: "q")
- **Visuals** (иконка: "r")  
- **Environment** (иконка: "s")
- **Themes** (иконка: "u")
- **Macro** (иконка: "⌨")
- **Configs** (иконка: "t")

### ✅ Модули
**Все существующие модули из нашей системы** - названия и функциональность остаются без изменений. Модули автоматически загружаются из `ModuleManager` по категориям.

### ✅ Функциональность Модулей
Логика работы модулей, их настройки и функции остаются без изменений.

---

## 🔥 Что Переделываем ПОЛНОСТЬЮ

### 1. **Структура GUI** (100% новая)
### 2. **Навигация** (100% новая)
### 3. **Визуальные Эффекты** (100% новые)
### 4. **Анимации** (100% новые)
### 5. **Компоненты** (100% новые)

---

## 📐 Анализ Референса (SolutionVisual)

### Структура GUI

```
┌─────────────────────────────────────────────────────────────┐
│                   ⚡ SolutionVisual                          │  ← Заголовок с логотипом
├─────────────────────────────────────────────────────────────┤
│  [Visuals] [HUD] [Utilities]        🔍 [Поиск]    ⚙        │  ← Вкладки + Поиск + Настройки
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │ Module Name     [⚪] │  │ Module Name     [⚪] │        │  ← Модули в 2 колонки
│  └──────────────────────┘  └──────────────────────┘        │    (из нашей системы)
│                                                              │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │ Module Name     [⚪] │  │ Module Name     [⚪] │        │
│  └──────────────────────┘  └──────────────────────┘        │
│                                                              │
│  ... (все модули из выбранной категории) ...                │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│         [⚪]  [✈]  [👥]  [📁]  [📋]                        │  ← Нижняя панель иконок
└─────────────────────────────────────────────────────────────┘
```

### Ключевые Особенности Референса

1. **Заголовок**
   - Логотип "⚡ SolutionVisual" по центру вверху
   - Темный фон с легким свечением

2. **Навигация**
   - Горизонтальные вкладки: Visuals, HUD, Utilities
   - Активная вкладка подсвечена белым
   - Неактивные вкладки серые

3. **Поиск**
   - Поле поиска справа от вкладок
   - Иконка 🔍 слева в поле
   - Placeholder "Поиск"

4. **Модули**
   - Сетка 2 колонки
   - Каждый модуль - прямоугольная карточка
   - Название модуля слева
   - Toggle переключатель справа (iOS стиль)
   - Темный фон карточек
   - Hover эффект (легкое осветление)
   - **Модули загружаются из ModuleManager по категориям**

5. **Toggle Переключатели**
   - iOS стиль (круглая ручка на треке)
   - Включен: белый/фиолетовый фон, ручка справа
   - Выключен: темно-серый фон, ручка слева
   - Плавная анимация переключения

6. **Нижняя Панель**
   - 5 иконок по центру внизу
   - Первая иконка активна (белая)
   - Остальные серые
   - Фиолетовая подсветка под активной

7. **Цветовая Схема**
   - Фон: очень темный (почти черный)
   - Карточки: темно-серые
   - Текст: белый (активный), серый (неактивный)
   - Акцент: белый/фиолетовый
   - Переключатели: белый (вкл), темно-серый (выкл)

8. **Эффекты**
   - Blur фон за GUI
   - Мягкие тени под карточками
   - Скругленные углы везде (8px)
   - Плавные анимации (200-300ms)
   - Hover эффекты

---

## 🏗️ Новая Архитектура

### Главные Компоненты

```
SolutionGuiScreen (главный экран)
├── HeaderComponent (заголовок с логотипом)
├── TabNavigationBar (вкладки + поиск)
│   ├── Tab (Visuals)
│   ├── Tab (HUD) 
│   ├── Tab (Utilities)
│   └── SearchField (поиск)
├── ModuleGrid (сетка модулей)
│   └── ModuleCard[] (карточки модулей - из ModuleManager)
│       ├── ModuleName (название)
│       └── ToggleSwitch (переключатель)
└── BottomIconBar (нижняя панель)
    └── IconButton[] (5 иконок)
```

### Маппинг НАШИХ Категорий на Вкладки

**Вкладки → Наши Категории:**
- **Visuals** → VISUALS (наша категория)
- **HUD** → ENVIRONMENT (наша категория)
- **Utilities** → MOVEMENT (наша категория)

**Дополнительные экраны (через нижнюю панель):**
- Иконка 1 (⚪) → Главная (Visuals/HUD/Utilities)
- Иконка 2 (✈) → MOVEMENT (альтернативный доступ)
- Иконка 3 (👥) → THEMES (наш экран тем)
- Иконка 4 (📁) → CONFIGS (наш экран конфигов)
- Иконка 5 (📋) → MACRO (наш экран макросов)

---

## 📝 Пошаговый План Реализации

### Этап 1: Подготовка и Структура (2 файла)

#### 1.1 Создать Базовые Классы
**Файл:** `src/main/java/farvix/solution/api/ui/solution/SolutionGuiScreen.java`

```java
public class SolutionGuiScreen extends Screen {
    // Размеры GUI (как в референсе)
    private static final int GUI_WIDTH = 600;
    private static final int GUI_HEIGHT = 400;
    
    // Компоненты
    private HeaderComponent header;
    private TabNavigationBar tabBar;
    private ModuleGrid moduleGrid;
    private BottomIconBar iconBar;
    
    // Анимации
    private Animation openAnimation;
    private Animation fadeAnimation;
    
    // Состояние
    private SolutionTab currentTab = SolutionTab.VISUALS;
    private String searchQuery = "";
}
```

#### 1.2 Создать Enum для Вкладок
**Файл:** `src/main/java/farvix/solution/api/ui/solution/SolutionTab.java`

```java
public enum SolutionTab {
    VISUALS("Visuals", ModuleCategory.VISUALS),
    HUD("HUD", ModuleCategory.ENVIRONMENT),
    UTILITIES("Utilities", ModuleCategory.MOVEMENT);
    
    private final String displayName;
    private final ModuleCategory category;
}
```

---

### Этап 2: Заголовок (1 файл)

**Файл:** `src/main/java/farvix/solution/api/ui/solution/components/HeaderComponent.java`

**Что рендерить:**
- Логотип "⚡ SolutionVisual" по центру
- Использовать `Fonts.SEMIBOLD.get(18)` для текста
- Иконка молнии слева от текста
- Высота: 40px

**Цвета:**
- Текст: белый (255, 255, 255)
- Фон: прозрачный (наследуется от главного фона)

---

### Этап 3: Навигационная Панель (2 файла)

#### 3.1 TabNavigationBar
**Файл:** `src/main/java/farvix/solution/api/ui/solution/components/TabNavigationBar.java`

**Что рендерить:**
- 3 вкладки горизонтально: Visuals, HUD, Utilities
- Поле поиска справа
- Иконка настроек в правом углу
- Высота: 35px

**Логика:**
- Активная вкладка: белый текст, подчеркивание снизу
- Неактивные: серый текст (180, 180, 180)
- Hover эффект: легкое осветление
- Анимация переключения: 200ms

#### 3.2 SearchField
**Файл:** `src/main/java/farvix/solution/api/ui/solution/components/SearchField.java`

**Что рендерить:**
- Прямоугольник с скругленными углами (6px)
- Иконка 🔍 слева
- Placeholder "Поиск" серым
- Курсор при фокусе
- Ширина: 150px, Высота: 25px

**Логика:**
- Фокус: белая рамка (1px)
- Без фокуса: темная рамка
- Ввод текста: фильтрация модулей в реальном времени

---

### Этап 4: Сетка Модулей (2 файла)

#### 4.1 ModuleGrid
**Файл:** `src/main/java/farvix/solution/api/ui/solution/components/ModuleGrid.java`

**Что рендерить:**
- Сетка 2 колонки
- Отступы: 10px между карточками
- Скролл если модулей много
- Scissor test для обрезки

**Логика:**
- Загрузка модулей из `Client.getInstance().getModuleManager().getModules()`
- Фильтрация по вкладке (по `module.getCategory()`)
- Фильтрация по поиску (по `module.getName()`)
- Плавный скролл с анимацией
- Виртуальный рендеринг (только видимые карточки)

#### 4.2 ModuleCard
**Файл:** `src/main/java/farvix/solution/api/ui/solution/components/ModuleCard.java`

**Что рендерить:**
- Прямоугольник с скругленными углами (8px)
- Название модуля слева (Fonts.DEFAULT.get(14))
- Toggle переключатель справа
- Высота: 35px

**Цвета:**
- Фон: темно-серый (40, 40, 45, 200)
- Hover: осветление на 10%
- Текст: белый (255, 255, 255)
- Тень: мягкая тень снизу (2px blur)

**Логика:**
- Клик ЛКМ: `module.toggle()`
- Hover анимация: 150ms
- Toggle анимация: 200ms

---

### Этап 5: Toggle Переключатель (1 файл)

**Файл:** `src/main/java/farvix/solution/api/ui/solution/components/ToggleSwitch.java`

**Что рендерить:**
- Трек: прямоугольник со скругленными углами (высота/2)
- Ручка: круг внутри трека
- Размеры: 36x18px (трек), 14x14px (ручка)

**Цвета:**
- Включен: белый/фиолетовый фон (200, 180, 255), белая ручка
- Выключен: темно-серый фон (60, 60, 65), серая ручка

**Анимация:**
- Ручка двигается слева направо: 200ms
- Цвет фона меняется: 200ms
- Easing: EASE_OUT_CUBIC

---

### Этап 6: Нижняя Панель (2 файла)

**ВАЖНО:** Нижняя панель рендерится СНАРУЖИ основного GUI (ниже красной линии), как показано на скриншоте с желтыми квадратами.

#### 6.1 BottomIconBar
**Файл:** `src/main/java/farvix/solution/api/ui/solution/components/BottomIconBar.java`

**Что рендерить:**
- 5 иконок по центру
- Расстояние между иконками: 40px
- Высота панели: 50px
- Подсветка под активной иконкой

**Иконки (НАШИ категории):**
1. ⚪ - Главная (Visuals/HUD/Utilities)
2. q - Movement (из ModuleCategory.MOVEMENT.getIcon())
3. u - Themes (из ModuleCategory.THEMES.getIcon())
4. t - Configs (из ModuleCategory.CONFIGS.getIcon())
5. ⌨ - Macro (из ModuleCategory.MACRO.getIcon())

**Логика:**
- Клик по иконке: переключить на соответствующий экран
- Анимация подсветки: 250ms
- Hover эффект: легкое увеличение (scale 1.1)

#### 6.2 IconButton
**Файл:** `src/main/java/farvix/solution/api/ui/solution/components/IconButton.java`

**Что рендерить:**
- Иконка из Icons.ttf (или из ModuleCategory.getIcon())
- Размер: 24x24px
- Подсветка снизу (если активна): 2px высота, фиолетовый цвет

**Цвета:**
- Активная: белый (255, 255, 255)
- Неактивная: серый (120, 120, 125)
- Hover: осветление на 20%

---

### Этап 7: Визуальные Эффекты (1 файл)

**Файл:** `src/main/java/farvix/solution/api/ui/solution/effects/SolutionEffects.java`

**Что добавить:**

1. **Blur Фон**
   - Полноэкранный blur за GUI
   - Интенсивность: 8px
   - Затемнение: 60% (0, 0, 0, 150)

2. **Frosted Glass Эффект (Преломление Света)**
   - Светлая обводка вокруг элементов (белая, 20 alpha)
   - Сильное искажение фона (softness 8f)
   - Увеличенная прозрачность (alpha 180 вместо 240)
   - Создает эффект матового стекла

3. **Тени**
   - Под каждой карточкой модуля
   - Offset: 0px, 2px
   - Blur: 4px
   - Цвет: (0, 0, 0, 80)

4. **Свечение**
   - Вокруг активной вкладки
   - Вокруг активной иконки
   - Цвет: фиолетовый (180, 140, 255, 100)
   - Blur: 6px

5. **Скругленные Углы**
   - Все элементы: 8px (карточки, GUI)
   - Маленькие элементы: 6px (поиск, toggle)
   - Очень маленькие: 4px (иконки)

**Параметры Frosted Glass:**
- Главный GUI: softness 8f, alpha 180
- Карточки модулей: softness 4-6f (6f при hover), alpha 160
- Светлая обводка: 1-1.5px, белая, alpha 12-20

---

### Этап 8: Анимации (1 файл)

**Файл:** `src/main/java/farvix/solution/api/ui/solution/animations/SolutionAnimations.java`

**Анимации:**

1. **Открытие GUI**
   - Fade: 0 → 1 (300ms)
   - Scale: 0.9 → 1.0 (300ms)
   - Slide: снизу вверх 20px (300ms)
   - Easing: EASE_OUT_CUBIC

2. **Закрытие GUI**
   - Fade: 1 → 0 (200ms)
   - Scale: 1.0 → 0.95 (200ms)
   - Easing: EASE_IN_CUBIC

3. **Переключение Вкладок**
   - Slide контента: влево/вправо 30px (250ms)
   - Fade: 0.5 → 1 (250ms)
   - Easing: EASE_OUT_CUBIC

4. **Toggle Переключатель**
   - Ручка: slide 22px (200ms)
   - Цвет фона: interpolate (200ms)
   - Easing: EASE_OUT_CUBIC

5. **Hover Эффекты**
   - Осветление: +10% (150ms)
   - Scale иконок: 1.0 → 1.1 (150ms)
   - Easing: EASE_OUT_SINE

6. **Скролл**
   - Плавный скролл: 400ms
   - Easing: EASE_OUT_CUBIC

---

### Этап 9: Интеграция (2 файла)

#### 9.1 Обновить ClickUI Модуль
**Файл:** `src/main/java/farvix/solution/client/modules/impl/environment/ClickUI.java`

```java
@Override
public void onEnable() {
    // Открыть новый SolutionGuiScreen вместо старого InterfaceScreen
    mc.setScreen(new SolutionGuiScreen());
}
```

#### 9.2 Создать Утилиты
**Файл:** `src/main/java/farvix/solution/api/ui/solution/utils/SolutionRenderUtils.java`

**Методы:**
- `drawBlurredBackground()` - blur фон
- `drawShadow()` - тени
- `drawGlow()` - свечение
- `drawRoundedRect()` - скругленные прямоугольники
- `drawRoundedGradient()` - градиентные прямоугольники

---

## 🎨 Цветовая Палитра (Референс)

```java
// Фоны
public static final Color BG_MAIN = new Color(18, 18, 20, 240);        // Главный фон GUI
public static final Color BG_CARD = new Color(40, 40, 45, 200);        // Фон карточек
public static final Color BG_HOVER = new Color(50, 50, 55, 200);       // Hover фон

// Текст
public static final Color TEXT_PRIMARY = new Color(255, 255, 255);     // Белый
public static final Color TEXT_SECONDARY = new Color(180, 180, 180);   // Серый
public static final Color TEXT_DISABLED = new Color(120, 120, 125);    // Темно-серый

// Акценты
public static final Color ACCENT_PRIMARY = new Color(200, 180, 255);   // Фиолетовый
public static final Color ACCENT_SECONDARY = new Color(255, 255, 255); // Белый

// Toggle
public static final Color TOGGLE_ON_BG = new Color(200, 180, 255);     // Фиолетовый фон
public static final Color TOGGLE_ON_KNOB = new Color(255, 255, 255);   // Белая ручка
public static final Color TOGGLE_OFF_BG = new Color(60, 60, 65);       // Темный фон
public static final Color TOGGLE_OFF_KNOB = new Color(120, 120, 125);  // Серая ручка

// Эффекты
public static final Color SHADOW = new Color(0, 0, 0, 80);             // Тени
public static final Color GLOW = new Color(180, 140, 255, 100);        // Свечение
public static final Color BLUR_OVERLAY = new Color(0, 0, 0, 150);      // Затемнение фона
```

---

## 📏 Размеры и Отступы (Референс)

```java
// GUI - обрезан до красной линии, нижняя панель СНАРУЖИ
public static final int GUI_WIDTH = 520;
public static final int GUI_HEIGHT = 280; // Обрезано для нижних кнопок
public static final int GUI_CORNER_RADIUS = 8;

// Заголовок
public static final int HEADER_HEIGHT = 40;

// Навигация
public static final int TAB_BAR_HEIGHT = 35;
public static final int TAB_WIDTH = 80;
public static final int TAB_SPACING = 10;

// Поиск
public static final int SEARCH_WIDTH = 150;
public static final int SEARCH_HEIGHT = 25;
public static final int SEARCH_CORNER_RADIUS = 6;

// Модули
public static final int MODULE_CARD_HEIGHT = 35;
public static final int MODULE_CARD_SPACING = 10;
public static final int MODULE_GRID_COLUMNS = 2;
public static final int MODULE_CORNER_RADIUS = 8;

// Toggle
public static final int TOGGLE_WIDTH = 36;
public static final int TOGGLE_HEIGHT = 18;
public static final int TOGGLE_KNOB_SIZE = 14;

// Нижняя панель
public static final int BOTTOM_BAR_HEIGHT = 50;
public static final int ICON_SIZE = 24;
public static final int ICON_SPACING = 40;

// Отступы
public static final int PADDING_SMALL = 5;
public static final int PADDING_MEDIUM = 10;
public static final int PADDING_LARGE = 15;
```

---

## 🔧 Технические Детали

### Используемые Классы

```java
// Анимации
Animation (Easing.EASE_OUT_CUBIC, duration)

// Рендеринг
blur.render()           // Blur эффекты
rectangle.render()      // Прямоугольники
Fonts.DEFAULT.get()     // Шрифты
Fonts.SEMIBOLD.get()    // Жирный шрифт

// Цвета
FixColor                // Фиксированные цвета

// Утилиты
ShapeProperties         // Свойства фигур
DrawContext             // Контекст рисования
MatrixStack             // Матрицы трансформаций

// Модули (НАШИ)
Client.getInstance().getModuleManager().getModules()  // Получить все модули
module.getCategory()    // Категория модуля
module.getName()        // Название модуля
module.isEnabled()      // Состояние модуля
module.toggle()         // Переключить модуль
ModuleCategory          // Enum категорий
```

### Структура Файлов

```
src/main/java/farvix/solution/api/ui/solution/
├── SolutionGuiScreen.java           (главный экран)
├── SolutionTab.java                 (enum вкладок)
├── components/
│   ├── HeaderComponent.java         (заголовок)
│   ├── TabNavigationBar.java        (навигация)
│   ├── SearchField.java             (поиск)
│   ├── ModuleGrid.java              (сетка)
│   ├── ModuleCard.java              (карточка)
│   ├── ToggleSwitch.java            (переключатель)
│   ├── BottomIconBar.java           (нижняя панель)
│   └── IconButton.java              (иконка)
├── effects/
│   └── SolutionEffects.java         (эффекты)
├── animations/
│   └── SolutionAnimations.java      (анимации)
└── utils/
    ├── SolutionRenderUtils.java     (утилиты рендеринга)
    └── SolutionColors.java          (цветовая палитра)
```

---

## ✅ Чеклист Реализации

### Этап 1: Структура ✅
- [x] Создать `SolutionGuiScreen.java`
- [x] Создать `SolutionTab.java`
- [x] Создать `SolutionColors.java`
- [x] Создать `SolutionRenderUtils.java`

### Этап 2: Компоненты ✅
- [x] `HeaderComponent.java` - заголовок ✅ ГОТОВО
- [x] `TabNavigationBar.java` - вкладки ✅ ГОТОВО
- [x] `SearchField.java` - поиск ✅ ГОТОВО
- [ ] `ModuleGrid.java` - сетка
- [ ] `ModuleCard.java` - карточка
- [ ] `ToggleSwitch.java` - переключатель
- [ ] `BottomIconBar.java` - нижняя панель
- [ ] `IconButton.java` - иконка

### Этап 3: Эффекты ✅
- [ ] Blur фон
- [ ] Тени под карточками
- [ ] Свечение вокруг активных элементов
- [ ] Скругленные углы

### Этап 4: Анимации ✅
- [ ] Открытие/закрытие GUI
- [ ] Переключение вкладок
- [ ] Toggle анимация
- [ ] Hover эффекты
- [ ] Плавный скролл

### Этап 5: Интеграция ✅
- [ ] Обновить `ClickUI.java`
- [ ] Протестировать все вкладки
- [ ] Протестировать поиск
- [ ] Протестировать переключатели
- [ ] Протестировать нижнюю панель

### Этап 6: Полировка ✅
- [ ] Оптимизация производительности
- [ ] Проверка на разных разрешениях
- [ ] Проверка всех анимаций
- [ ] Финальные правки цветов

---

## 🎯 Ожидаемый Результат

После реализации получим:

✨ **Визуально:**
- Современный темный GUI в стиле SolutionVisual
- Плавные анимации (200-300ms)
- Blur фон за GUI
- Мягкие тени под элементами
- Скругленные углы везде
- iOS-стиль toggle переключатели
- Фиолетовые акценты

🎮 **Функционально:**
- 3 главные вкладки: Visuals, HUD, Utilities
- Поиск модулей в реальном времени
- Сетка модулей 2 колонки (из нашей системы)
- Нижняя панель с 5 иконками для наших экранов (Movement, Themes, Configs, Macro)
- Плавный скролл
- Hover эффекты

🔧 **Технически:**
- Чистая архитектура (компонентный подход)
- Переиспользуемые компоненты
- Оптимизированный рендеринг
- Плавные анимации без лагов
- Поддержка разных разрешений
- Интеграция с существующей системой модулей

---

## 📊 Оценка Объема Работы

**Всего файлов:** ~15 новых файлов
**Строк кода:** ~3000-4000 строк
**Время:** 2-3 дня активной работы

**Приоритет:**
1. **Высокий:** Структура, компоненты, базовый рендеринг
2. **Средний:** Эффекты, анимации
3. **Низкий:** Полировка, оптимизация

---

## 🚀 Начало Работы

**Шаг 1:** Создать базовую структуру (SolutionGuiScreen, SolutionTab, SolutionColors)
**Шаг 2:** Реализовать простой рендеринг без эффектов
**Шаг 3:** Добавить компоненты один за другим
**Шаг 4:** Добавить эффекты и анимации
**Шаг 5:** Интегрировать и протестировать
**Шаг 6:** Полировка и оптимизация

---

## 📝 Примечания

- **НЕ ТРОГАЕМ:** Логику модулей, их настройки, функциональность
- **СОХРАНЯЕМ:** Названия модулей (из ModuleManager), НАШИ категории
- **ПЕРЕДЕЛЫВАЕМ:** Весь GUI, навигацию, визуал, анимации
- **РЕФЕРЕНС:** SolutionVisual (скриншот)
- **СТИЛЬ:** Темный, современный, минималистичный
- **АКЦЕНТ:** Фиолетовый + белый
- **МОДУЛИ:** Загружаются автоматически из существующей системы

---

**Готово к реализации! 🚀**
