# ClickGUI Redesign Plan - Solution Visuals (Minecraft 1.21.4 Fabric)

## Цель
Переделать существующий ClickGUI в стиле reference mod (simplevisuals) с красивыми шейдерами, плавными анимациями и современным дизайном. **Только визуальные улучшения, без добавления новых функций.**

---

## Текущая Архитектура

### Основные Классы
- `InterfaceScreen` - главный экран GUI
- `Sidebar` - боковая панель с категориями
- `ModuleScreen` - экран с модулями
- `ModuleComponent` - компонент отдельного модуля
- `ThemeScreen` - экран выбора тем
- `ConfigScreen` - экран конфигов

### Текущие Возможности
- ✅ Sidebar с категориями (Movement, Visuals, Themes, Environment, Configs)
- ✅ Поиск модулей
- ✅ Двухколоночная сетка модулей
- ✅ Раскрытие настроек модуля
- ✅ Система тем (ThemeManager)
- ✅ Анимации открытия/закрытия
- ✅ Blur эффекты
- ✅ Tooltip панель над GUI

---

## Что Нужно Улучшить (По Приоритету)

### 1. **Улучшенные Шейдеры и Blur Эффекты** ⭐⭐⭐
**Текущее состояние:**
- Используется `blur.render()` с базовыми параметрами
- Простые прямоугольники без градиентов

**Что добавить:**
- Многослойное свечение вокруг GUI панели (как в reference mod)
- Shader blur для фона (полноэкранный blur)
- Градиентные рамки и фоны
- Мягкие тени под элементами

**Файлы для изменения:**
- `InterfaceScreen.java` - метод `render()`
- Добавить метод `renderPanelGlow()` (как в reference mod)
- Использовать `Render2D.drawShaderBlurRect()` для фона

**Пример кода из reference mod:**
```java
// Shader blur для фона
Render2D.drawShaderBlurRect(
    context.getMatrices(),
    0f, 0f,
    mc.getWindow().getScaledWidth(),
    mc.getWindow().getScaledHeight(),
    0f, 8f,
    new Color(255, 255, 255, (int)(160f * uiAlpha))
);

// Многослойное свечение
private void renderPanelGlow(DrawContext ctx) {
    float radius = 8f;
    float blurRadius = 10f;
    int a = (int)(22 * uiAlpha);
    Render2D.drawShaderBlurRect(
        ctx.getMatrices(), 
        x - 2f, y - 2f, 
        width + 4f, height + 4f, 
        radius + 1f, blurRadius,
        new Color(255, 255, 255, a)
    );
}
```

---

### 2. **Улучшенные Кнопки Модулей** ⭐⭐⭐
**Текущее состояние:**
- Простые прямоугольники с текстом
- Базовый hover эффект

**Что добавить:**
- Градиентный фон для активных модулей
- Плавная анимация hover эффекта
- Акцентная рамка сверху (как в reference mod)
- Тонкая рамка по краям
- Иконки модулей (если есть)

**Файлы для изменения:**
- `ModuleComponent.java` - метод `render()`
- Добавить `hoverAnimation` (Animation объект)
- Использовать `drawGradientBox()` для фона

**Пример структуры:**
```java
// В ModuleComponent.java
private Animation hoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);

public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
    boolean hovered = isHovered(mouseX, mouseY);
    hoverAnimation.run(hovered ? 1 : 0);
    float hoverValue = hoverAnimation.getValue();
    
    // Фон с hover эффектом
    int bgColor = module.isEnabled() 
        ? TempColor.getModuleBackground().brighter((int)(30 * hoverValue)).getRGB()
        : TempColor.getModuleBackground().getRGB();
    
    // Градиентная рамка сверху для активных
    if (module.isEnabled()) {
        drawGradientBox(matrix, x, y, width, 3f, accentColor, accentColorDark);
    }
    
    // Тонкая рамка
    drawThinFrame(matrix, x, y, width, height, borderColor);
}
```

---

### 3. **Улучшенные Анимации** ⭐⭐
**Текущее состояние:**
- Базовые анимации открытия/закрытия
- Простой fade и scale

**Что добавить:**
- Slide эффект при открытии (снизу вверх)
- Плавная анимация переключения категорий
- Easing функции для более естественного движения
- Анимация появления модулей (stagger effect)

**Файлы для изменения:**
- `InterfaceScreen.java` - улучшить анимации в `render()`
- Добавить `contentOffsetY` для slide эффекта

**Пример из reference mod:**
```java
// Slide + scale + fade
float scale = 0.88f + 0.12f * t;
float slideY = (1f - t) * 18f;
float contentOffsetY = (1f - t) * 10f;

// Применяем
this.x = (screenWidth - width) / 2f;
this.y = (screenHeight - height) / 2f + slideY;
```

---

### 4. **Улучшенный Scrollbar** ⭐⭐
**Текущее состояние:**
- Базовый scrollbar (если есть)

**Что добавить:**
- Тонкий scrollbar справа от GUI
- Акцентный цвет темы для thumb
- Плавная анимация скролла
- Скругленные углы

**Файлы для изменения:**
- `ModuleScreen.java` - добавить рендеринг scrollbar
- Использовать `themeManager.getCurrentTheme().getAccentColor()`

**Пример из reference mod:**
```java
if (maxScroll > 0.5f) {
    float trackX = x + width - 4f;
    float trackY = contentY;
    float trackW = 2f;
    float trackH = contentH;
    
    // Track
    Render2D.drawRect(ctx.getMatrices(), trackX, trackY, trackW, trackH,
        new Color(0, 0, 0, (int)(90f * uiAlpha)));
    
    // Thumb
    float visibleRatio = contentH / (contentH + maxScroll);
    float thumbH = Math.max(14f, trackH * visibleRatio);
    float thumbY = trackY + (trackH - thumbH) * (scrollY / maxScroll);
    
    Color thumbColor = new Color(
        theme.getAccentColor().getRed(),
        theme.getAccentColor().getGreen(),
        theme.getAccentColor().getBlue(),
        (int)(180f * uiAlpha)
    );
    Render2D.drawRoundedRect(ctx.getMatrices(), 
        trackX - 0.5f, thumbY, trackW + 1f, thumbH, 1.5f, thumbColor);
}
```

---

### 5. **Улучшенные Вкладки Категорий** ⭐⭐
**Текущее состояние:**
- Sidebar с иконками категорий

**Что добавить:**
- Плавная анимация переключения
- Акцентный цвет для активной вкладки
- Hover эффект
- Скругленные углы

**Файлы для изменения:**
- `Sidebar.java` - улучшить рендеринг категорий

**Пример:**
```java
// Активная вкладка - акцентный цвет
if (active) {
    Color acc = themeManager.getCurrentTheme().getAccentColor();
    Render2D.drawRoundedRect(ctx.getMatrices(), 
        x, y, width, height, 4f,
        new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), alpha));
} else {
    // Неактивная - темный фон
    Render2D.drawRoundedRect(ctx.getMatrices(), 
        x, y, width, height, 4f,
        new Color(20, 20, 20, (int)(170f * alpha)));
}
```

---

### 6. **Снежинки (Опционально)** ⭐
**Текущее состояние:**
- Нет

**Что добавить:**
- Падающие снежинки на фоне (как в reference mod)
- Настройка включения/выключения через ClickUI модуль

**Файлы для изменения:**
- `InterfaceScreen.java` - добавить `Snowflake` класс и рендеринг

---

### 7. **Улучшенная Панель Настроек** ⭐
**Текущее состояние:**
- Настройки раскрываются внутри модуля

**Что добавить:**
- Панель настроек справа (как в reference mod)
- Slide анимация появления
- Отдельный scrollbar для настроек
- Плавное затемнение при открытии

**Файлы для изменения:**
- `ModuleScreen.java` - добавить рендеринг правой панели
- Добавить `settingsAnimation`

---

## Пошаговый План Реализации

### Этап 1: Подготовка (1 файл)
1. Создать утилиты для рендеринга (если нужно)
   - `drawGradientBox()`
   - `drawThinFrame()`
   - `drawSolidBox()`

### Этап 2: Улучшение Фона и Свечения (1 файл)
2. `InterfaceScreen.java`
   - Добавить `renderPanelGlow()` метод
   - Улучшить фоновый blur
   - Добавить затемнение фона

### Этап 3: Улучшение Анимаций (1 файл)
3. `InterfaceScreen.java`
   - Добавить slide эффект (`contentOffsetY`)
   - Улучшить scale анимацию
   - Добавить плавный fade

### Этап 4: Улучшение Кнопок Модулей (1 файл)
4. `ModuleComponent.java`
   - Добавить `hoverAnimation`
   - Градиентный фон для активных
   - Акцентная рамка сверху
   - Тонкая рамка по краям

### Этап 5: Улучшение Scrollbar (1 файл)
5. `ModuleScreen.java`
   - Добавить красивый scrollbar
   - Использовать акцентный цвет темы
   - Плавная анимация

### Этап 6: Улучшение Sidebar (1 файл)
6. `Sidebar.java`
   - Улучшить рендеринг категорий
   - Добавить hover анимации
   - Акцентный цвет для активной

### Этап 7: Панель Настроек Справа (1 файл)
7. `ModuleScreen.java`
   - Добавить правую панель настроек
   - Slide анимация
   - Отдельный scrollbar

### Этап 8: Снежинки (Опционально) (1 файл)
8. `InterfaceScreen.java`
   - Добавить `Snowflake` класс
   - Рендеринг снежинок
   - Настройка через ClickUI

---

## Технические Детали

### Используемые Классы и Методы
- `Animation` (Easing.EASE_OUT_CUBIC, duration)
- `TempColor` - система цветов
- `ThemeManager` - текущая тема
- `blur.render()` - blur эффекты
- `rectangle.render()` - прямоугольники
- `Fonts.DEFAULT.get()` - шрифты

### Цветовая Схема
```java
// Фон
TempColor.getGuiBackground()      // Основной фон GUI
TempColor.getModuleBackground()   // Фон модулей
TempColor.getGuiBorder()          // Рамки

// Текст
TempColor.getTextPrimary()        // Основной текст
TempColor.getTextSecondary()      // Вторичный текст

// Акценты
themeManager.getCurrentTheme().getAccentColor()  // Акцентный цвет темы
themeManager.getCurrentTheme().getTextColor()    // Цвет текста темы
```

### Анимации
```java
// Создание
Animation anim = new Animation(Easing.EASE_OUT_CUBIC, 300);

// Использование
anim.run(targetValue);
float value = anim.getValue();
```

---

## Файлы для Изменения (Итого)

1. ✅ `InterfaceScreen.java` - главный экран
2. ✅ `ModuleComponent.java` - компоненты модулей
3. ✅ `ModuleScreen.java` - экран модулей
4. ✅ `Sidebar.java` - боковая панель
5. ⚠️ Утилиты рендеринга (если нужно создать)

**Всего: 4-5 файлов**

---

## Что НЕ Делаем

❌ Не добавляем новые функции (waypoints, новые модули и т.д.)
❌ Не меняем логику работы модулей
❌ Не меняем систему настроек
❌ Не меняем систему конфигов
❌ Не меняем структуру категорий

✅ Только визуальные улучшения существующего GUI

---

## Ожидаемый Результат

После реализации ClickGUI будет выглядеть как в reference mod:
- 🎨 Красивые шейдеры и blur эффекты
- ✨ Плавные анимации открытия/закрытия
- 🎯 Современные кнопки модулей с градиентами
- 📜 Красивый scrollbar с акцентным цветом
- 🎭 Улучшенные вкладки категорий
- ❄️ Снежинки на фоне (опционально)
- ⚙️ Панель настроек справа

Все это при сохранении текущей функциональности!
