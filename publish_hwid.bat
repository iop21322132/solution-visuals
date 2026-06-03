@echo off
chcp 65001 > nul
echo ==============================================
echo    Solution HWID - Быстрая публикация списка
echo ==============================================
echo.
echo Добавление изменений файла hwid.txt в Git...
git add hwid.txt
if %ERRORLEVEL% neq 0 (
    echo [ОШИБКА] Не удалось добавить hwid.txt в Git!
    pause
    exit /b
)

echo.
echo Создание коммита...
git commit -m "Быстрое обновление списка HWID"
if %ERRORLEVEL% neq 0 (
    echo [ПРЕДУПРЕЖДЕНИЕ] Нет изменений для коммита.
    pause
    exit /b
)

echo.
echo Отправка изменений на GitHub...
git push origin main
if %ERRORLEVEL% neq 0 (
    echo [ОШИБКА] Не удалось отправить файл на GitHub!
    pause
    exit /b
)

echo.
echo Сброс кэша CDN jsDelivr для мгновенного обновления HWID...
powershell -Command "Invoke-WebRequest -Uri 'https://purge.jsdelivr.net/gh/iop21322132/solution-visuals@main/hwid.txt' -UseBasicParsing | Out-Null"

echo.
echo ==============================================
echo    СПИСОК HWID УСПЕШНО ОБНОВЛЕН НА GITHUB!
echo    Изменения вступят в силу мгновенно у всех игроков.
echo ==============================================
echo.
pause
