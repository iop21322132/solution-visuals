# Руководство: Переорганизация Настроек Модуля

## Как работает порядок настроек

Настройки в GUI отображаются в **точном порядке**, в котором они добавлены в модуль через `getSettings().add()`.

## Пример: Правильный порядок

```java
public class MyModule extends Module {
    
    // 1️⃣ Текстуры ПЕРВЫМИ
    private final ListSetting textures = new ListSetting(
        "Текстура",
        false,
        new BooleanSetting("Звезда", true),
        new BooleanSetting("Круг", false),
        new BooleanSetting("Сердце", false)
    );

    // 2️⃣ Ползунки (Sliders) ВТОРЫМИ
    private final NumberSetting size = new NumberSetting("Размер", 1.0f, 0.1f, 3.0f, 0.1f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    private final NumberSetting alpha = new NumberSetting("Прозрачность", 1.0f, 0.0f, 1.0f, 0.1f);

    // 3️⃣ Выбор цвета ТРЕТЬИМ
    private final ColorSetting color = new ColorSetting("Цвет", Color.RED.getRGB());

    // 4️⃣ Другие настройки (булевы, бинды и т.д.)
    private final BooleanSetting randomColor = new BooleanSetting("Рандомный цвет", false);
    private final BindSetting toggle = new BindSetting("Активировать", Bind.UNBOUND);

    public MyModule() {
        super("My Module", Category.Render, "Описание модуля");

        // ⚠️ ПОРЯДОК ИМЕЕТ ЗНАЧЕНИЕ! Добавляйте в нужной последовательности:
        
        // 1. Текстуры
        getSettings().add(textures);
        
        // 2. Ползунки
        getSettings().add(size);
        getSettings().add(speed);
        getSettings().add(alpha);
        
        // 3. Цвета
        getSettings().add(color);
        
        // 4. Остальное
        getSettings().add(randomColor);
        getSettings().add(toggle);
    }
}
```

## Визуальный результат

```
┌─────────────────────────────────────────┐
│ My Module                           [ON] │
├─────────────────────────────────────────┤
│ Текстура             [Звезда ▼ + ○ ♥]   │  ← ListSetting (текстуры)
├─────────────────────────────────────────┤
│ Размер              [=========○====== ] │  ← Слайдер 1
│ Скорость            [=====○========== ] │  ← Слайдер 2
│ Прозрачность        [========○======= ] │  ← Слайдер 3
├─────────────────────────────────────────┤
│ Цвет                              [●] ▼ │  ← ColorSetting (с кнопкой)
│  [Круг + 2 ползунка]                    │  ← Появляется при клике
├─────────────────────────────────────────┤
│ Рандомный цвет                    ☑ OFF │  ← BooleanSetting
│ Активировать                    [UNBOUND]│ ← BindSetting
└─────────────────────────────────────────┘
```

## Типы настроек и их предпочтительный порядок

### 1. **ListSetting** (выбор текстур/режимов)
   - Добавляйте ПЕРВЫМ
   - Люди выбирают "что использовать" перед настройкой деталей

### 2. **NumberSetting / SliderSetting** (ползунки)
   - Добавляйте ВТОРЫМИ
   - Группируйте похожие ползунки вместе
   - Порядок: размер → скорость → прозрачность

### 3. **ColorSetting** (выбор цвета)
   - Добавляйте ТРЕТЬИМ
   - С коллапсирующимся цветником (новая функция!)

### 4. **BooleanSetting** (включить/выключить)
   - Добавляйте ЧЕТВЁРТЫМИ
   - Для отдельных опций типа "рандомный цвет"

### 5. **BindSetting** (горячие клавиши)
   - Добавляйте ПОСЛЕДНИМИ
   - Обычно не нужны в подавляющем большинстве модулей

## Пример из Trails модуля

```java
public Trails() {
    super("Trails", Category.Render, "...");
    
    // 1. Тип трейла (выбор)
    getSettings().add(trailType);
    
    // 2. Параметры для линий
    getSettings().add(lineLength);
    getSettings().add(lineLifetime);
    getSettings().add(lineWidth);
    
    // 3. Параметры для частиц
    getSettings().add(particleLength);
    getSettings().add(particleSize);
    getSettings().add(particleLifetime);
    getSettings().add(spawnDistance);
    
    // 4. Текстуры для частиц
    getSettings().add(textures);
}
```

## Как использовать новую collapse-функцию ColorSetting

✨ **НОВОЕ**: ColorSettingComponent теперь имеет кнопку "Цвет" которая открывает/закрывает цветник!

- **Закрыто**: занимает 1 строку (компактно)
- **Открыто**: показывает круг с выбором цвета + 2 ползунка
- **Клик на кнопку снова**: закрывается обратно

```java
// В модуле просто добавьте ColorSetting как обычно
private final ColorSetting mainColor = new ColorSetting("Основной цвет", Color.RED.getRGB());

public MyModule() {
    getSettings().add(mainColor);  // Кнопка появится автоматически!
}
```

## Миграция существующих модулей

Если у вас уже есть модуль с настройками:

1. **Определите текущий порядок** в методе `getSettings()` добавлений
2. **Переупорядочьте** в рекомендуемом порядке
3. **Проверьте в GUI** что всё выглядит правильно

### Пример переустановки:

**БЫЛО:**
```java
getSettings().add(color);        // ColorSetting в начале ❌
getSettings().add(size);         // Слайдеры в конце ❌
getSettings().add(textures);     // Текстуры в середине ❌
```

**СТАЛО:**
```java
getSettings().add(textures);     // Текстуры первыми ✅
getSettings().add(size);         // Слайдеры вторыми ✅
getSettings().add(color);        // Цвета третьими ✅
```

## Проверка

После внесения изменений:
1. Откройте клик-GUI (по умолчанию `Н` или настроенная клавиша)
2. Найдите ваш модуль
3. Раскройте его нажатием
4. Проверьте порядок настроек — они должны быть в логической последовательности

---

**Вопросы?** Все работает через простой порядок добавления в `getSettings()`. Новая collapse-функция для ColorSetting применяется автоматически ко всем ColorSetting!
