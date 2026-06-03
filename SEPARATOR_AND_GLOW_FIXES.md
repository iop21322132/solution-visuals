# Исправление Разделителей и Свечения GUI

## Проблемы

1. **Резкие полосы разделения** - разделители выглядели как резкие линии без размытия
2. **Прозрачный квадрат слева сверху** - свечение (glow) панели GUI выходило за границы и создавало артефакты

## Решения

### 1. Убрано Свечение Панели GUI

**Файл:** `src/main/java/farvix/solution/api/ui/clickgui/InterfaceScreen.java`

**Проблема:**
```java
// Старый код - создавал артефакты
blur.render(ShapeProperties.create(context.getMatrices(), 
        gx - 2f, gy - 2f,  // Выходило за границы!
        gw + 4f, gh + 4f)
        .round(radius + 1f)
        .softness(blurRadius)
        .color(new FixColor(255, 255, 255, glowAlpha).getRGB())
        .build());
```

**Решение:**
```java
// Новый код - свечение убрано
private void renderPanelGlow(DrawContext context, float uiAlpha) {
    if (uiAlpha <= 0f) return;
    // Убрано свечение - оно вызывало артефакты за границами GUI
    // Вместо этого используем только основную панель с увеличенным blur
}
```

**Эффект:**
- ❌ Убран прозрачный квадрат слева сверху
- ✅ Чистые границы GUI
- ✅ Нет артефактов за пределами панели

---

### 2. Добавлено Размытие Разделителям

#### ThemeScreen - Разделитель между пресетами и кастомом

**Файл:** `src/main/java/farvix/solution/api/ui/clickgui/impl/theme/ThemeScreen.java`

**Было:**
```java
rectangle.render(ShapeProperties.create(context.getMatrices(), cx, cy, width - 20, 1)
        .color(TempColor.getSeparatorHorizontal().alpha(alpha).getRGB())
        .build());
```

**Стало:**
```java
blur.render(ShapeProperties.create(context.getMatrices(), cx, cy, width - 20, 1)
        .round(0.5f)
        .softness(1.5f)
        .color(TempColor.getSeparatorHorizontal().alpha(alpha).getRGB())
        .build());
```

**Изменения:**
- `rectangle` → `blur` (используем размытие вместо резкого прямоугольника)
- Добавлен `round(0.5f)` - небольшое скругление краев
- Добавлен `softness(1.5f)` - мягкие, размытые края

---

#### MacroScreen - Разделитель между созданием и списком

**Файл:** `src/main/java/farvix/solution/api/ui/clickgui/impl/macro/MacroScreen.java`

**Было:**
```java
rectangle.render(ShapeProperties.create(context.getMatrices(), startX, separatorY, contentWidth, 1)
        .round(0)
        .color(TempColor.getClientColor().alpha(alpha * 0.6f).getRGB())
        .build());
```

**Стало:**
```java
blur.render(ShapeProperties.create(context.getMatrices(), startX, separatorY, contentWidth, 1)
        .round(0.5f)
        .softness(1.5f)
        .color(TempColor.getClientColor().alpha(alpha * 0.6f).getRGB())
        .build());
```

**Изменения:**
- `rectangle` → `blur`
- `round(0)` → `round(0.5f)`
- Добавлен `softness(1.5f)`

---

## Сравнение: До и После

### Разделители

| Параметр | До | После | Эффект |
|----------|-----|--------|--------|
| **Рендер** | `rectangle` | `blur` | Размытие вместо резких краев |
| **Round** | 0 | 0.5f | Слегка скругленные края |
| **Softness** | - | 1.5f | Мягкие, плавные края |

### Свечение GUI

| Параметр | До | После |
|----------|-----|--------|
| **Размер** | GUI + 4px | GUI (точно) |
| **Позиция** | GUI - 2px | GUI (точно) |
| **Артефакты** | ❌ Да | ✅ Нет |

---

## Визуальный Эффект

### До:

**Разделители:**
- ❌ Резкие, четкие линии
- ❌ Выглядят как "вырезанные"
- ❌ Не гармонируют с размытым GUI

**Свечение:**
- ❌ Прозрачный квадрат слева сверху
- ❌ Выходит за границы GUI
- ❌ Создает визуальный шум

### После:

**Разделители:**
- ✅ Мягкие, размытые линии
- ✅ Плавно интегрированы в дизайн
- ✅ Гармонируют с общим стилем GUI

**Свечение:**
- ✅ Убрано полностью
- ✅ Чистые границы GUI
- ✅ Нет артефактов

---

## Технические Детали

### Почему `blur` вместо `rectangle`?

**rectangle:**
- Рендерит резкий прямоугольник
- Нет размытия краев
- Выглядит "плоско"

**blur:**
- Рендерит с размытием
- Мягкие края благодаря `softness`
- Современный, премиальный вид

### Параметры размытия разделителей:

```java
.round(0.5f)      // Минимальное скругление (линия почти прямая)
.softness(1.5f)   // Среднее размытие (мягкие края)
```

Эти значения оптимальны для тонких линий (1px высотой).

---

## Затронутые Файлы

1. **InterfaceScreen.java** - убрано свечение панели
2. **ThemeScreen.java** - размытие разделителя
3. **MacroScreen.java** - размытие разделителя

---

## Компиляция

✅ **BUILD SUCCESSFUL** - все изменения скомпилированы без ошибок

---

## Рекомендации

### Если нужно еще более мягкие разделители:
```java
.softness(2.0f)  // Вместо 1.5f
```

### Если разделители слишком размыты:
```java
.softness(1.0f)  // Вместо 1.5f
```

### Если нужно вернуть свечение (не рекомендуется):
Можно уменьшить размер свечения:
```java
gx, gy, gw, gh  // Без расширения
```

---

## Итог

- ✅ **Убраны артефакты** - нет прозрачного квадрата
- ✅ **Мягкие разделители** - плавные, размытые линии
- ✅ **Чистый вид** - GUI выглядит профессионально
- ✅ **Гармоничный дизайн** - все элементы в едином стиле
