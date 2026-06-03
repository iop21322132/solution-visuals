# WayPoints - Техническая документация

## Архитектура

### Основной класс
`farvix.solution.client.modules.impl.visuals.WayPoints`

### Категория модуля
`ModuleCategory.VISUALS`

## Компоненты

### 1. Обработка команд
**Event Handler:** `onMessage(EventMessage event)`

**Логика:**
1. Проверка, начинается ли сообщение с `.gps`
2. Если просто `.gps` - пропускаем в чат
3. Парсинг аргументов: `X Z название`
4. Валидация:
   - Минимум 3 аргумента
   - Первые два - числа (X, Z)
   - Третий - НЕ число (название)
5. Создание waypoint или отправка ошибки

**Отмена отправки в чат:**
```java
event.setCancelled(true);
```

### 2. 3D рендеринг
**Event Handler:** `onRender3D(EventRender3D.Game e)`

**Этапы рендеринга:**

#### A. Фильтрация waypoints
```java
double distance = Math.sqrt(
    Math.pow(wp.x - playerPos.x, 2) + 
    Math.pow(wp.z - playerPos.z, 2)
);
if (distance > maxDistance.getValue()) continue;
```

#### B. Рендеринг UI (рамка + текст)
1. **Позиционирование:**
   - X, Z: координаты waypoint
   - Y: высота игрока + 2.5 блока

2. **Billboard эффект:**
   ```java
   ms.multiply(mc.gameRenderer.getCamera().getRotation());
   ```

3. **Масштабирование:**
   ```java
   float scale = 0.02f;
   ms.scale(-scale, -scale, scale);
   ```

4. **Элементы:**
   - Градиентный фон (`drawGradientBox`)
   - Рамка (`drawFrame`)
   - Текст (Fonts.DEFAULT.get(16))

#### C. Рендеринг стрелки
1. **Позиционирование:**
   - X, Z: координаты waypoint
   - Y: высота игрока - 0.5 блока

2. **Поворот на 180°:**
   ```java
   ms.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, 180));
   ```

3. **Текстура:**
   ```java
   Identifier.of("solution", "textures/gps_arrow.png")
   ```

4. **Quad рендеринг:**
   - POSITION_TEX_COLOR формат
   - Размер: `arrowSize.getValue()`

### 3. Хранение данных

**Структура WayPoint:**
```java
private static class WayPoint {
    final double x;
    final double z;
    final String name;
    final long createdTime;
}
```

**Хранилище:**
```java
private final CopyOnWriteArrayList<WayPoint> waypoints = new CopyOnWriteArrayList<>();
```

**Thread-safe:** Используется `CopyOnWriteArrayList` для безопасного доступа из разных потоков.

## Настройки

### ColorSetting
- `frameColor` - цвет рамки и стрелки (по умолчанию: rgba(100, 180, 255, 200))
- `textColor` - цвет текста (по умолчанию: rgba(255, 255, 255, 255))

### SliderSetting
- `maxDistance` - макс. дистанция (1000, диапазон: 100-5000)
- `arrowSize` - размер стрелки (0.5, диапазон: 0.2-2.0)

### BooleanSetting
- `showDistance` - показывать дистанцию (по умолчанию: true)

## Рендеринг деталей

### Градиентный фон
```java
int topColor = new FixColor(r, g, b, alpha).getRGB();
int bottomColor = new FixColor(r/2, g/2, b/2, alpha).getRGB();
```

### Рамка
```java
int frameColor = new FixColor(
    Math.min(255, r + 50),
    Math.min(255, g + 50),
    Math.min(255, b + 50),
    255
).getRGB();
```

### Blend режимы
```java
RenderSystem.enableBlend();
RenderSystem.blendFunc(
    GlStateManager.SrcFactor.SRC_ALPHA, 
    GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
);
```

## Сообщения в чат

### Успех
```java
Text.literal("§b§lSolution Visual §8» §f" + message)
```

### Ошибка
```java
Text.literal("§c§lSolution Visual §8» §c§l" + message)
```

## Валидация команд

### Проверка формата
```java
String[] parts = msg.substring(4).trim().split("\\s+");
if (parts.length < 3) {
    sendError("Надо ввести только X и Z координаты и название");
    return;
}
```

### Проверка на Y координату
```java
try {
    Double.parseDouble(parts[2]);
    // Если третий аргумент - число, значит ввели X Y Z
    sendError("Надо ввести только X и Z координаты и название");
    return;
} catch (NumberFormatException ignored) {
    // Это не число, значит это название - всё ок
}
```

## Производительность

### Оптимизации
1. **CopyOnWriteArrayList** - минимизирует блокировки при чтении
2. **Фильтрация по дистанции** - не рендерим далекие waypoints
3. **Batch рендеринг** - все элементы рендерятся за один проход
4. **Кэширование матриц** - используем peek() вместо создания новых

### Потребление ресурсов
- **CPU:** Низкое (только расчет дистанций и матриц)
- **GPU:** Среднее (рендеринг quad'ов с текстурами)
- **RAM:** Минимальное (~100 байт на waypoint)

## Расширение функционала

### Добавление сохранения waypoints
```java
// В onDisable():
saveWaypointsToFile();

// В onEnable():
loadWaypointsFromFile();
```

### Добавление удаления waypoints
```java
// Команда .gps remove <название>
if (msg.startsWith(".gps remove ")) {
    String name = msg.substring(12).trim();
    waypoints.removeIf(wp -> wp.name.equalsIgnoreCase(name));
}
```

### Добавление списка waypoints
```java
// Команда .gps list
if (msg.equals(".gps list")) {
    for (WayPoint wp : waypoints) {
        sendSuccess(wp.name + " - X:" + (int)wp.x + " Z:" + (int)wp.z);
    }
}
```

## Зависимости

### Внутренние
- `farvix.solution.api.events.impl.game.EventMessage`
- `farvix.solution.api.events.impl.render.EventRender3D`
- `farvix.solution.api.render.font.Fonts`
- `farvix.solution.api.util.color.FixColor`

### Minecraft
- `net.minecraft.client.util.math.MatrixStack`
- `net.minecraft.client.render.*`
- `net.minecraft.util.Identifier`
- `net.minecraft.text.Text`

### Библиотеки
- `org.joml.Matrix4f`
- `org.joml.Quaternionf`
- `com.mojang.blaze3d.systems.RenderSystem`

## Известные ограничения

1. **Нет персистентности** - waypoints не сохраняются между сессиями
2. **Нет удаления** - можно только создавать, нельзя удалять
3. **Нет редактирования** - нельзя изменить существующий waypoint
4. **Нет списка** - нет команды для просмотра всех waypoints
5. **Нет телепортации** - только визуализация, без TP

## Будущие улучшения

1. ✨ Сохранение в JSON файл
2. ✨ Команда `.gps remove <название>`
3. ✨ Команда `.gps list`
4. ✨ Команда `.gps tp <название>` (если есть права)
5. ✨ Команда `.gps edit <название> <новое_название>`
6. ✨ Цветные waypoints (разные цвета для разных типов)
7. ✨ Иконки вместо стрелок (дом, ферма, шахта и т.д.)
8. ✨ Waypoint категории (дома, фермы, шахты)
9. ✨ Импорт/экспорт waypoints
10. ✨ Sharing waypoints с друзьями
