# WayPoints - Отладка и решение проблем

## Проблема: Waypoints не видны в игре

### Шаг 1: Проверь, что модуль включен
1. Открой меню модов (обычно RShift или другая клавиша)
2. Найди модуль **WayPoints** в категории **Visuals**
3. Убедись, что он **включен** (зеленый/активный)

### Шаг 2: Проверь список waypoints
Введи в чат:
```
.gps list
```

Ты должен увидеть:
```
Solution Visual » Список waypoints (1):
Solution Visual »   dom - X:500 Z:500 [XXXm]
```

Если видишь "Нет сохраненных waypoints" - значит waypoint не был создан.

### Шаг 3: Проверь дистанцию
Waypoint может быть слишком далеко. Проверь:
1. Расстояние в списке (`.gps list`)
2. Настройку **Макс. дистанция** в модуле
3. По умолчанию: 1000 блоков

Если расстояние больше максимальной дистанции - увеличь её в настройках.

### Шаг 4: Проверь координаты
Убедись, что ты находишься в правильном месте:
1. Нажми F3 чтобы увидеть свои координаты
2. Сравни с координатами waypoint из `.gps list`
3. Если ты далеко - подойди ближе

### Шаг 5: Проверь настройки рендеринга
1. Открой настройки модуля WayPoints
2. Проверь:
   - **Цвет рамки** - не прозрачный ли? (альфа > 0)
   - **Цвет текста** - не прозрачный ли?
   - **Размер стрелки** - не слишком маленький? (попробуй 1.0)

### Шаг 6: Пересоздай waypoint
Попробуй удалить и создать заново:
```
.gps remove dom
.gps 500 500 dom
```

## Проблема: Команда не работает

### Проверь формат команды
✅ Правильно:
```
.gps 500 500 dom
```

❌ Неправильно:
```
gps 500 500 dom          (нет точки в начале)
.gps 500 64 500 dom      (три числа вместо двух)
.gps 500 500             (нет названия)
```

### Проверь, что команда не отправляется в чат
Если ты видишь `.gps 500 500 dom` в чате - значит:
1. Модуль **выключен**
2. Или есть проблема с обработкой EventMessage

## Проблема: Waypoint создается, но не рендерится

### Возможные причины:

#### 1. EventRender3D не вызывается
Проверь, что другие 3D модули работают:
- JumpCircle
- HitBubbles
- TargetESP

Если они тоже не работают - проблема в EventRender3D.

#### 2. Координаты неправильные
Waypoint рендерится относительно камеры. Проверь:
```java
Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
double renderX = wp.x - cameraPos.x;
double renderY = mc.player.getY() - cameraPos.y;
double renderZ = wp.z - cameraPos.z;
```

#### 3. Матрицы неправильные
Проверь, что MatrixStack правильно используется:
```java
ms.push();
ms.translate(renderX, renderY + 2.5, renderZ);
ms.multiply(mc.gameRenderer.getCamera().getRotation());
// рендеринг
ms.pop();
```

#### 4. Blend режим
Проверь, что blend включен:
```java
RenderSystem.enableBlend();
RenderSystem.blendFunc(
    GlStateManager.SrcFactor.SRC_ALPHA, 
    GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
);
```

## Проблема: Текст не виден

### Проверь шрифты
Убедись, что Fonts.DEFAULT инициализирован:
```java
Fonts.DEFAULT.get(16).drawString(ms, displayText, x, y, color);
```

### Проверь цвет текста
Цвет должен быть непрозрачным:
```java
new FixColor(255, 255, 255, 255).getRGB()  // белый, непрозрачный
```

### Проверь масштаб
```java
float scale = 0.02f;
ms.scale(-scale, -scale, scale);
```

## Проблема: Стрелка не видна

### Проверь текстуру
Убедись, что файл существует:
```
src/main/resources/assets/solution/textures/gps_arrow.png
```

### Проверь Identifier
```java
Identifier.of("solution", "textures/gps_arrow.png")
```

### Проверь размер стрелки
Попробуй увеличить:
```
Размер стрелки: 1.0 или 2.0
```

### Проверь поворот
Стрелка должна быть повернута на 180°:
```java
ms.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, 180));
```

## Тестирование

### Тест 1: Создание waypoint
```
.gps 0 0 Spawn
```
Ожидаемый результат: "Waypoint 'Spawn' создан на X:0 Z:0"

### Тест 2: Список waypoints
```
.gps list
```
Ожидаемый результат: Список с "Spawn"

### Тест 3: Удаление waypoint
```
.gps remove Spawn
```
Ожидаемый результат: "Waypoint 'Spawn' удален"

### Тест 4: Рендеринг
1. Создай waypoint рядом с собой: `.gps <твой X> <твой Z> Test`
2. Отойди на 10 блоков
3. Посмотри в сторону waypoint
4. Должен увидеть рамку с текстом и стрелку

## Логирование

Добавь логи в код для отладки:

### В onMessage:
```java
System.out.println("[WayPoints] Command: " + msg);
System.out.println("[WayPoints] Waypoint created: " + name + " at " + x + ", " + z);
System.out.println("[WayPoints] Total waypoints: " + waypoints.size());
```

### В onRender3D:
```java
System.out.println("[WayPoints] Rendering " + waypoints.size() + " waypoints");
System.out.println("[WayPoints] Camera pos: " + cameraPos);
System.out.println("[WayPoints] Player pos: " + mc.player.getPos());
System.out.println("[WayPoints] Rendered: " + rendered + " waypoints");
```

## Известные проблемы

### 1. Waypoints не сохраняются
**Причина:** Нет персистентности  
**Решение:** Waypoints удаляются при отключении модуля

### 2. Waypoints видны через стены
**Причина:** `RenderSystem.disableDepthTest()`  
**Решение:** Это нормально для GPS системы

### 3. Текст размытый
**Причина:** Масштаб или разрешение  
**Решение:** Попробуй другой размер шрифта (12, 14, 18)

## Контрольный список

- [ ] Модуль включен
- [ ] Waypoint создан (проверь `.gps list`)
- [ ] Дистанция в пределах максимальной
- [ ] Координаты правильные
- [ ] Цвета непрозрачные
- [ ] Текстура gps_arrow.png существует
- [ ] Другие 3D модули работают
- [ ] Нет ошибок в консоли

## Если ничего не помогло

1. Перезапусти игру
2. Пересобери мод: `./gradlew build`
3. Проверь версию Minecraft (должна быть 1.21.4)
4. Проверь версию Fabric Loader
5. Проверь, что нет конфликтов с другими модами
