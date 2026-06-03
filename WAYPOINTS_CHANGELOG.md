# WayPoints - Changelog

## Версия 2.0 (Текущая)

### ✨ Новые функции

#### Команда `.gps list`
Показывает список всех созданных waypoints с координатами и расстоянием:
```
.gps list
```

Вывод:
```
Solution Visual » Список waypoints (2):
Solution Visual »   Дом - X:100 Z:200 [50m]
Solution Visual »   Ферма - X:-500 Z:1000 [1200m]
```

#### Команда `.gps remove <название>`
Удаляет waypoint по названию:
```
.gps remove Дом
```

Вывод:
```
Solution Visual » Waypoint 'Дом' удален
```

Если waypoint не найден:
```
Solution Visual » Waypoint 'Дом' не найден
```

### 🐛 Исправления

#### Исправлен рендеринг waypoints
**Проблема:** Waypoints не отображались в игре  
**Причина:** Неправильные координаты рендеринга (использовались абсолютные вместо относительных)  
**Решение:** Теперь используются координаты относительно камеры:

```java
Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
double renderX = wp.x - cameraPos.x;
double renderY = mc.player.getY() - cameraPos.y;
double renderZ = wp.z - cameraPos.z;
```

#### Улучшена обработка команд
- Добавлена проверка на пустой список waypoints
- Улучшены сообщения об ошибках
- Добавлена справка при неправильном вводе команды

### 📝 Изменения в коде

#### Метод `onRender3D`
**Было:**
```java
Vec3d wpPos = new Vec3d(wp.x, playerPos.y, wp.z);
ms.translate(wpPos.x, wpPos.y + 2.5, wpPos.z);
```

**Стало:**
```java
Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
double renderX = wp.x - cameraPos.x;
double renderY = mc.player.getY() - cameraPos.y;
double renderZ = wp.z - cameraPos.z;
ms.translate(renderX, renderY + 2.5, renderZ);
```

#### Метод `renderArrow`
**Было:**
```java
private void renderArrow(MatrixStack ms, Vec3d wpPos) {
    ms.push();
    ms.translate(wpPos.x, wpPos.y - 0.5, wpPos.z);
    // ...
    ms.pop();
}
```

**Стало:**
```java
private void renderArrow(MatrixStack ms) {
    // translate уже выполнен в onRender3D
    ms.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, 180));
    // ...
}
```

#### Новый метод `sendInfo`
Добавлен для информационных сообщений:
```java
private void sendInfo(String message) {
    mc.inGameHud.getChatHud().addMessage(
        Text.literal("§b§lSolution Visual §8» §7" + message)
    );
}
```

### 📚 Документация

#### Новые файлы
- `WAYPOINTS_DEBUG.md` - руководство по отладке и решению проблем
- `WAYPOINTS_CHANGELOG.md` - история изменений (этот файл)

#### Обновленные файлы
- `WAYPOINTS_QUICK_GUIDE.md` - добавлены новые команды
- `WAYPOINTS_README.md` - обновлена информация о командах

---

## Версия 1.0 (Начальная)

### ✨ Функции

#### Команда `.gps X Z название`
Создание waypoint с координатами X и Z:
```
.gps 100 200 Дом
```

#### 3D визуализация
- Рамка с названием waypoint
- Градиентный фон
- Яркая рамка по краям
- Стрелка, указывающая на блок (повернута на 180°)
- Billboard эффект (всегда повернуто к игроку)

#### Настройки
- Цвет рамки
- Цвет текста
- Максимальная дистанция (100-5000 блоков)
- Показывать/скрывать дистанцию
- Размер стрелки (0.2-2.0)

#### Валидация команд
- Проверка на правильное количество аргументов
- Проверка, что X и Z - числа
- Проверка, что не введена Y координата
- Красные жирные сообщения об ошибках

#### Сообщения в чат
- Успех: `§b§lSolution Visual §8» §fWaypoint 'название' создан на X:100 Z:200`
- Ошибка: `§c§lSolution Visual §8» §c§lНадо ввести только X и Z координаты и название`

---

## Планы на будущее

### Версия 3.0 (Планируется)
- [ ] Сохранение waypoints в JSON файл
- [ ] Команда `.gps tp <название>` (телепортация, если есть права)
- [ ] Команда `.gps edit <название> <новое_название>`
- [ ] Цветные waypoints (разные цвета для разных типов)
- [ ] Иконки вместо стрелок (дом, ферма, шахта и т.д.)
- [ ] Waypoint категории
- [ ] Импорт/экспорт waypoints
- [ ] Sharing waypoints с друзьями
- [ ] Waypoint на карте (если есть мод карты)
- [ ] Звуковое уведомление при приближении к waypoint
- [ ] Автоматическое удаление waypoint при достижении

### Версия 2.1 (Ближайшие исправления)
- [ ] Добавить команду `.gps help` для справки
- [ ] Добавить команду `.gps clear` для удаления всех waypoints
- [ ] Добавить команду `.gps nearest` для поиска ближайшего waypoint
- [ ] Улучшить производительность при большом количестве waypoints
- [ ] Добавить анимацию появления/исчезновения waypoints
- [ ] Добавить пульсацию стрелки для лучшей видимости

---

## Технические детали

### Версия Minecraft
1.21.4

### Зависимости
- Fabric Loader
- Solution Visuals Client

### Файлы
- `WayPoints.java` - основной модуль
- `gps_arrow.png` - текстура стрелки

### События
- `EventMessage` - обработка команд в чате
- `EventRender3D.Game` - рендеринг в 3D мире

### Хранение данных
- `CopyOnWriteArrayList<WayPoint>` - thread-safe список waypoints
- Waypoints хранятся только в памяти (не сохраняются между сессиями)

---

## Известные ограничения

1. **Нет персистентности** - waypoints не сохраняются между сессиями
2. **Нет мультимировой поддержки** - waypoints не привязаны к конкретному миру
3. **Нет лимита** - можно создать неограниченное количество waypoints
4. **Нет сортировки** - waypoints показываются в порядке создания
5. **Нет поиска** - нельзя искать waypoint по части названия
6. **Нет групп** - нельзя группировать waypoints по категориям

---

## Благодарности

Спасибо за использование WayPoints! Если у вас есть предложения или вы нашли баг, пожалуйста, сообщите об этом.
