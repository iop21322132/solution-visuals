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
echo Загрузка списка HWID на веб-сервер...
curl -s -H "token: SolutionSecretUpdateToken10293" -H "filename: hwid.txt" --data-binary @hwid.txt http://de3.netrix.pw:19260/upload
echo.

echo ==============================================
echo    СПИСОК HWID УСПЕШНО ОБНОВЛЕН!
echo    Изменения вступят в силу мгновенно у всех игроков.
echo ==============================================
echo.
pause
