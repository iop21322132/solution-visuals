$versionFile = "version.txt"
$launcherVersionFile = "launcher_version.txt"
$gradleFile = "gradle.properties"
$launcherCsFile = "launcher\MainWindow.xaml.cs"
$launcherXamlFile = "launcher\MainWindow.xaml"

# 1. Read current version
if (Test-Path $versionFile) {
    $currentVersion = [System.IO.File]::ReadAllText($versionFile, [System.Text.Encoding]::UTF8).Trim()
} else {
    $currentVersion = "3.7"
}

if ($currentVersion -match "ECHO" -or $currentVersion -eq "") {
    $currentVersion = "3.7"
}

# 2. Compute next version
$parts = $currentVersion.Split('.')
if ($parts.Length -lt 2) {
    $nextVersion = "3.7.1"
} else {
    $lastIndex = $parts.Length - 1
    if ($parts[$lastIndex] -match '^\d+$') {
        $parts[$lastIndex] = [int]$parts[$lastIndex] + 1
    } else {
        $parts += "1"
    }
    $nextVersion = $parts -join '.'
}

# 3. Prompt for version (or use default if running non-interactively)
$newVersion = $nextVersion
try {
    if ([System.Console]::IsInputRedirected -or $env:NON_INTERACTIVE -eq "true") {
        Write-Host "Non-interactive mode detected. Using default version: $nextVersion"
    } else {
        $inputVersion = Read-Host "Enter new version (default: $nextVersion)"
        if ($inputVersion -ne $null -and $inputVersion.Trim() -ne "") {
            $newVersion = $inputVersion.Trim()
        }
    }
} catch {
    # Non-interactive fallback
}

Write-Host "Selected version: $newVersion"

# 4. Update files
# gradle.properties
if (Test-Path $gradleFile) {
    $gradleContent = [System.IO.File]::ReadAllText($gradleFile, [System.Text.Encoding]::UTF8)
    $gradleContent = $gradleContent -replace 'mod_version=.*', "mod_version=$newVersion"
    [System.IO.File]::WriteAllText($gradleFile, $gradleContent, [System.Text.Encoding]::UTF8)
}

# version.txt
[System.IO.File]::WriteAllText($versionFile, $newVersion, [System.Text.Encoding]::ASCII)

# launcher_version.txt
[System.IO.File]::WriteAllText($launcherVersionFile, $newVersion, [System.Text.Encoding]::ASCII)

# launcher\MainWindow.xaml.cs
if (Test-Path $launcherCsFile) {
    $csContent = [System.IO.File]::ReadAllText($launcherCsFile, [System.Text.Encoding]::UTF8)
    $csContent = $csContent -replace 'string currentVersion = ".*"', "string currentVersion = `"$newVersion`""
    [System.IO.File]::WriteAllText($launcherCsFile, $csContent, [System.Text.Encoding]::UTF8)
}

# launcher\MainWindow.xaml
if (Test-Path $launcherXamlFile) {
    $xamlContent = [System.IO.File]::ReadAllText($launcherXamlFile, [System.Text.Encoding]::UTF8)
    $xamlContent = $xamlContent -replace 'Title="Solution Launcher .*?"', "Title=`"Solution Launcher $newVersion`"" -replace 'Text="Solution Launcher .*?"', "Text=`"Solution Launcher $newVersion`""
    [System.IO.File]::WriteAllText($launcherXamlFile, $xamlContent, [System.Text.Encoding]::UTF8)
}
