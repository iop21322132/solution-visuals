@echo off
chcp 65001 > nul
echo ==============================================
echo    Solution Visual - Автоматическая публикация
echo ==============================================

powershell -NoProfile -ExecutionPolicy Bypass -File bump_version.ps1
for /f "delims=" %%i in (version.txt) do set NEW_VERSION=%%i

if "%NEW_VERSION%"=="" (
    echo [ОШИБКА] Версия не может быть пустой!
    pause
    exit /b
)

echo [2/6] Сборка мода с помощью Gradle...
call gradlew.bat remapJar -x test
if %ERRORLEVEL% neq 0 (
    echo [ОШИБКА] Не удалось собрать мод!
    pause
    exit /b
)

echo [3/6] Копирование файла мода в корень...
if exist "build\libs\solution-%NEW_VERSION%.jar" (
    copy /y "build\libs\solution-%NEW_VERSION%.jar" "SolutionVisual.jar" > nul
) else (
    if exist "build\libs\solution-%NEW_VERSION%-obfuscated.jar" (
        copy /y "build\libs\solution-%NEW_VERSION%-obfuscated.jar" "SolutionVisual.jar" > nul
    ) else (
        echo [ОШИБКА] Скомпилированный файл jar не найден в build\libs\!
        pause
        exit /b
    )
)

echo [4/6] Обновление версии в коде лаунчера...
:: (Версии в файлах лаунчера уже обновлены с корректной кодировкой в bump_version.ps1)

echo [5/6] Сборка лаунчера в Release...
dotnet publish launcher/SolutionLauncher.csproj -c Release -r win-x64 --self-contained true
if %ERRORLEVEL% neq 0 (
    echo [ОШИБКА] Не удалось собрать лаунчер!
    pause
    exit /b
)

:: Terminate running instances to prevent file lock
taskkill /f /im SolutionLauncher.exe > nul 2>&1

copy /y "launcher\bin\Release\net10.0-windows\win-x64\publish\SolutionLauncher.exe" "launcher\SolutionLauncher.exe" > nul
copy /y "launcher\bin\Release\net10.0-windows\win-x64\publish\SolutionLauncher.exe" "SolutionLauncher.exe" > nul

echo [6/7] Отправка изменений на GitHub...
git add .
git commit -m "Автоматическое обновление до версии %NEW_VERSION%"
git push origin main
if %ERRORLEVEL% neq 0 (
    echo [ОШИБКА] Не удалось отправить файлы на GitHub!
    pause
    exit /b
)

echo.
echo [7/7] Загрузка обновлений на веб-сервер...
curl -s -F "file=@version.txt" -F "token=SolutionSecretUpdateToken10293" http://de3.netrix.pw:19260/upload.php
echo.
curl -s -F "file=@launcher_version.txt" -F "token=SolutionSecretUpdateToken10293" http://de3.netrix.pw:19260/upload.php
echo.
curl -s -F "file=@SolutionVisual.jar" -F "token=SolutionSecretUpdateToken10293" http://de3.netrix.pw:19260/upload.php
echo.
curl -s -F "file=@SolutionLauncher.exe" -F "token=SolutionSecretUpdateToken10293" http://de3.netrix.pw:19260/upload.php
echo.

echo ==============================================
echo    ОБНОВЛЕНИЕ УСПЕШНО ОПУБЛИКОВАНО!
echo    Версия %NEW_VERSION% теперь активна для всех игроков.
echo ==============================================
pause
