$versionFile = "version.txt"
$launcherVersionFile = "launcher_version.txt"
$gradleFile = "gradle.properties"
$launcherCsFile = "launcher\MainWindow.xaml.cs"
$launcherXamlFile = "launcher\MainWindow.xaml"

# 1. Read current version
if (Test-Path $versionFile) {
    $currentVersion = (Get-Content $versionFile).Trim()
} else {
    $currentVersion = "3.6.4.12"
}

if ($currentVersion -match "ECHO" -or $currentVersion -eq "") {
    $currentVersion = "3.6.4.12"
}

# 2. Compute next version
$parts = $currentVersion.Split('.')
if ($parts.Length -lt 4) {
    $parts = "3.6.4.12".Split('.')
}
$parts[-1] = [int]$parts[-1] + 1
$nextVersion = $parts -join '.'

# 3. Prompt for version (or use default if running non-interactively)
$newVersion = $nextVersion
try {
    # Check if host can read line (in non-interactive/piped environments, this might return empty or throw)
    $inputVersion = Read-Host "Enter new version (default: $nextVersion)"
    if ($inputVersion -ne $null -and $inputVersion.Trim() -ne "") {
        $newVersion = $inputVersion.Trim()
    }
} catch {
    # Non-interactive fallback
}

Write-Host "Selected version: $newVersion"

# 4. Update files
# gradle.properties
if (Test-Path $gradleFile) {
    $gradleContent = Get-Content $gradleFile
    $gradleContent -replace 'mod_version=.*', "mod_version=$newVersion" | Set-Content $gradleFile -Encoding utf8
}

# version.txt
$newVersion | Set-Content $versionFile -NoNewline

# launcher_version.txt
$newVersion | Set-Content $launcherVersionFile -NoNewline

# launcher\MainWindow.xaml.cs
if (Test-Path $launcherCsFile) {
    $csContent = Get-Content $launcherCsFile
    $csContent -replace 'string currentVersion = ".*"', "string currentVersion = `"$newVersion`"" | Set-Content $launcherCsFile -Encoding utf8
}

# launcher\MainWindow.xaml
if (Test-Path $launcherXamlFile) {
    $xamlContent = Get-Content $launcherXamlFile
    $xamlContent -replace 'Title="Solution Launcher .*?"', "Title=`"Solution Launcher $newVersion`"" -replace 'Text="Solution Launcher .*?"', "Text=`"Solution Launcher $newVersion`"" | Set-Content $xamlContent -Encoding utf8
}
