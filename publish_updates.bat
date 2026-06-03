@echo off
chcp 65001 > nul
echo ==============================================
echo    Solution Visual - Автоматическая публикация
echo ==============================================

:: 1. Auto-detect and bump version
for /f "delims=" %%i in ('powershell -Command "$v = (Get-Content version.txt).Trim(); $parts = $v.Split('.'); $parts[-1] = [int]$parts[-1] + 1; $parts -join '.'"') do set AUTO_VERSION=%%i

set /p NEW_VERSION="Введите новую версию (по умолчанию %AUTO_VERSION%, нажмите Enter): "
if "%NEW_VERSION%"=="" set NEW_VERSION=%AUTO_VERSION%

if "%NEW_VERSION%"=="" (
    echo [ОШИБКА] Версия не может быть пустой!
    pause
    exit /b
)

echo [1/6] Обновление версии в gradle.properties...
powershell -Command "(GC gradle.properties) -replace 'mod_version=.*', 'mod_version=%NEW_VERSION%' | Out-File gradle.properties -Encoding utf8"
echo %NEW_VERSION% > version.txt
echo %NEW_VERSION% > launcher_version.txt

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
powershell -Command "(GC launcher\MainWindow.xaml.cs) -replace 'string currentVersion = \".*\"', 'string currentVersion = \"%NEW_VERSION%\"' | Out-File launcher\MainWindow.xaml.cs -Encoding utf8"
powershell -Command "(GC launcher\MainWindow.xaml) -replace 'Title=\"Solution Launcher .*\"', 'Title=\"Solution Launcher %NEW_VERSION%\"' | Out-File launcher\MainWindow.xaml -Encoding utf8"
powershell -Command "(GC launcher\MainWindow.xaml) -replace 'Text=\"Solution Launcher .*\"', 'Text=\"Solution Launcher %NEW_VERSION%\"' | Out-File launcher\MainWindow.xaml -Encoding utf8"

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
echo [7/7] Сброс кэша CDN jsDelivr для мгновенного обновления...
powershell -Command "Invoke-WebRequest -Uri 'https://purge.jsdelivr.net/gh/iop21322132/solution-visuals@main/version.txt' -UseBasicParsing | Out-Null"
powershell -Command "Invoke-WebRequest -Uri 'https://purge.jsdelivr.net/gh/iop21322132/solution-visuals@main/hwid.txt' -UseBasicParsing | Out-Null"
powershell -Command "Invoke-WebRequest -Uri 'https://purge.jsdelivr.net/gh/iop21322132/solution-visuals@main/SolutionVisual.jar' -UseBasicParsing | Out-Null"
powershell -Command "Invoke-WebRequest -Uri 'https://purge.jsdelivr.net/gh/iop21322132/solution-visuals@main/launcher_version.txt' -UseBasicParsing | Out-Null"

echo ==============================================
echo    ОБНОВЛЕНИЕ УСПЕШНО ОПУБЛИКОВАНО НА GITHUB!
echo    Версия %NEW_VERSION% теперь активна для всех игроков.
echo ==============================================
pause
