using System;
using System.IO;
using System.Net.Http;
using System.Text.Json;
using System.Diagnostics;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using System.Collections.Generic;
using System.Linq;
using System.IO.Compression;
using System.Runtime.InteropServices;
using System.Text;




namespace SolutionLauncher
{
    public partial class MainWindow : Window
    {
        private static readonly HttpClient httpClient = new HttpClient();
        private static bool useGitHubMirror = false;
        private static bool useMojangMirror = false;
        private bool isLaunching = false;
        private string currentModVersion = "";

        private string GetGitHubApiUrl(string rawUrl)
        {
            if (string.IsNullOrEmpty(rawUrl) || !rawUrl.StartsWith("https://raw.githubusercontent.com/"))
            {
                return rawUrl;
            }

            string cleanUrl = rawUrl;
            int qIdx = cleanUrl.IndexOf('?');
            if (qIdx >= 0)
            {
                cleanUrl = cleanUrl.Substring(0, qIdx);
            }

            string path = cleanUrl.Substring("https://raw.githubusercontent.com/".Length);
            var parts = path.Split(new[] { '/' }, 4);
            if (parts.Length >= 4)
            {
                string user = parts[0];
                string repo = parts[1];
                string branch = parts[2];
                string filePath = parts[3];
                return $"https://api.github.com/repos/{user}/{repo}/contents/{filePath}?ref={branch}";
            }
            return rawUrl;
        }

        private static string ResolveUrl(string url)
        {
            if (string.IsNullOrEmpty(url)) return url;

            string resolved = url;
            if (useGitHubMirror && resolved.StartsWith("https://raw.githubusercontent.com/"))
            {
                if (!resolved.EndsWith(".exe", StringComparison.OrdinalIgnoreCase))
                {
                    string path = resolved.Substring("https://raw.githubusercontent.com/".Length);
                    var parts = path.Split(new[] { '/' }, 4);
                    if (parts.Length >= 4)
                    {
                        string user = parts[0];
                        string repo = parts[1];
                        string branch = parts[2];
                        string file = parts[3];
                        resolved = $"https://cdn.jsdelivr.net/gh/{user}/{repo}@{branch}/{file}";
                    }
                }
                else
                {
                    resolved = resolved.Replace("https://raw.githubusercontent.com/", "https://raw.gitmirror.com/");
                }
            }
            if (useMojangMirror)
            {
                if (resolved.StartsWith("https://launchermeta.mojang.com/"))
                    resolved = resolved.Replace("https://launchermeta.mojang.com/", "https://bmclapi2.bangbang93.com/");
                else if (resolved.StartsWith("https://launcher.mojang.com/"))
                    resolved = resolved.Replace("https://launcher.mojang.com/", "https://bmclapi2.bangbang93.com/");
                else if (resolved.StartsWith("https://piston-meta.mojang.com/"))
                    resolved = resolved.Replace("https://piston-meta.mojang.com/", "https://bmclapi2.bangbang93.com/");
                else if (resolved.StartsWith("https://piston-data.mojang.com/"))
                    resolved = resolved.Replace("https://piston-data.mojang.com/", "https://bmclapi2.bangbang93.com/");
                else if (resolved.StartsWith("https://meta.fabricmc.net/"))
                    resolved = resolved.Replace("https://meta.fabricmc.net/", "https://bmclapi2.bangbang93.com/fabric-meta/");
                else if (resolved.StartsWith("https://maven.fabricmc.net/"))
                    resolved = resolved.Replace("https://maven.fabricmc.net/", "https://bmclapi2.bangbang93.com/maven/");
            }
            return resolved;
        }

        private string SafeGetStringAsync(string url)
        {
            try
            {
                if (url.StartsWith("https://raw.githubusercontent.com/"))
                {
                    try
                    {
                        string apiUrl = GetGitHubApiUrl(url);
                        var request = new HttpRequestMessage(HttpMethod.Get, apiUrl);
                        request.Headers.Accept.ParseAdd("application/vnd.github.v3.raw");
                        using (var response = httpClient.SendAsync(request).GetAwaiter().GetResult())
                        {
                            if (response.IsSuccessStatusCode)
                            {
                                return response.Content.ReadAsStringAsync().GetAwaiter().GetResult();
                            }
                        }
                    }
                    catch {}
                }
            }
            catch {}

            string resolved = ResolveUrl(url);
            try
            {
                return httpClient.GetStringAsync(resolved).GetAwaiter().GetResult();
            }
            catch (Exception ex)
            {
                bool activatedMirror = false;
                if (url.StartsWith("https://raw.githubusercontent.com/") && !useGitHubMirror)
                {
                    useGitHubMirror = true;
                    activatedMirror = true;
                    Log($"[СЕТЬ] Ошибка подключения к GitHub ({ex.Message}). Переключение на зеркало...");
                }
                else if ((url.Contains("mojang.com") || url.Contains("fabricmc.net")) && !useMojangMirror)
                {
                    useMojangMirror = true;
                    activatedMirror = true;
                    Log($"[СЕТЬ] Ошибка подключения к Mojang/Fabric ({ex.Message}). Переключение на зеркало...");
                }

                if (activatedMirror)
                {
                    string retriedUrl = ResolveUrl(url);
                    return httpClient.GetStringAsync(retriedUrl).GetAwaiter().GetResult();
                }
                throw;
            }
        }

        private byte[] SafeGetByteArrayAsync(string url)
        {
            try
            {
                if (url.StartsWith("https://raw.githubusercontent.com/"))
                {
                    try
                    {
                        string apiUrl = GetGitHubApiUrl(url);
                        var request = new HttpRequestMessage(HttpMethod.Get, apiUrl);
                        request.Headers.Accept.ParseAdd("application/vnd.github.v3.raw");
                        using (var response = httpClient.SendAsync(request).GetAwaiter().GetResult())
                        {
                            if (response.IsSuccessStatusCode)
                            {
                                return response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult();
                            }
                        }
                    }
                    catch {}
                }
            }
            catch {}

            string resolved = ResolveUrl(url);
            try
            {
                return httpClient.GetByteArrayAsync(resolved).GetAwaiter().GetResult();
            }
            catch (Exception ex)
            {
                bool activatedMirror = false;
                if (url.StartsWith("https://raw.githubusercontent.com/") && !useGitHubMirror)
                {
                    useGitHubMirror = true;
                    activatedMirror = true;
                    Log($"[СЕТЬ] Ошибка подключения к GitHub ({ex.Message}). Переключение на зеркало...");
                }
                else if ((url.Contains("mojang.com") || url.Contains("fabricmc.net")) && !useMojangMirror)
                {
                    useMojangMirror = true;
                    activatedMirror = true;
                    Log($"[СЕТЬ] Ошибка подключения к Mojang/Fabric ({ex.Message}). Переключение на зеркало...");
                }

                if (activatedMirror)
                {
                    string retriedUrl = ResolveUrl(url);
                    return httpClient.GetByteArrayAsync(retriedUrl).GetAwaiter().GetResult();
                }
                throw;
            }
        }


        public MainWindow()
        {
            InitializeComponent();
            try
            {
                httpClient.Timeout = TimeSpan.FromSeconds(15);
                if (httpClient.DefaultRequestHeaders.UserAgent.Count == 0)
                {
                    httpClient.DefaultRequestHeaders.UserAgent.ParseAdd("SolutionLauncher/1.0");
                }
            }
            catch {}
            
            // Set Window Icon safely
            try
            {
                var iconUri = new Uri("pack://application:,,,/icon.jpg", UriKind.RelativeOrAbsolute);
                this.Icon = System.Windows.Media.Imaging.BitmapFrame.Create(iconUri);
            }
            catch {}
            
            // Set dynamic maximum for RAM slider based on physical memory to leave room for Windows
            int totalRam = GetTotalRamGb();
            int maxRam = totalRam;
            if (totalRam <= 4) maxRam = Math.Max(2, totalRam - 1);
            else if (totalRam <= 8) maxRam = totalRam - 2;
            else maxRam = totalRam - 4;
            RamSlider.Maximum = Math.Max(2, maxRam);
            
            LoadConfig();

            // Perform HWID access check
            try
            {
                CheckHWID();
            }
            catch (UnauthorizedAccessException)
            {
                return;
            }

            // Start periodic HWID background check (every 15 seconds)
            StartPeriodicHwidCheck();

            // Perform self-update check
            CheckLauncherUpdate();
        }

        private void StartPeriodicHwidCheck()
        {
            Task.Run(async () =>
            {
                while (true)
                {
                    await Task.Delay(TimeSpan.FromSeconds(15));
                    try
                    {
                        CheckHWID();
                    }
                    catch (UnauthorizedAccessException)
                    {
                        break;
                    }
                    catch {}
                }
            });
        }

        private void Window_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.LeftButton == MouseButtonState.Pressed)
            {
                DragMove();
            }
        }

        private void MinimizeButton_Click(object sender, RoutedEventArgs e)
        {
            WindowState = WindowState.Minimized;
        }

        private void CloseButton_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private void Log(string message)
        {
            Dispatcher.Invoke(() =>
            {
                ConsoleOutput.AppendText($"[{DateTime.Now:HH:mm:ss}] {message}\n");
                ConsoleScroll.ScrollToEnd();
            });
        }

        private void SetStatus(string status, double progress)
        {
            Dispatcher.Invoke(() =>
            {
                StatusLabel.Text = status;
                LaunchProgressBar.Value = progress;
                ProgressPercent.Text = $"{(int)progress}%";
                ProgressDetails.Text = status;
            });
        }

        private async void PlayButton_Click(object sender, RoutedEventArgs e)
        {
            if (isLaunching) return;

            string nickname = NicknameInput.Text.Trim();
            if (string.IsNullOrEmpty(nickname))
            {
                MessageBox.Show("Пожалуйста, введите никнейм перед началом игры.", "Ошибка", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            int ramGb = (int)RamSlider.Value;
            SaveConfig();

            isLaunching = true;
            PlayButton.IsEnabled = false;
            NicknameInput.IsEnabled = false;
            OpenSettingsButton.IsEnabled = false;

            try
            {
                await Task.Run(() => CheckHWID());
                await Task.Run(() => StartLaunchPipeline(nickname, ramGb));
            }
            catch (UnauthorizedAccessException)
            {
                // Abort launching cleanly as application shutdown is already initiated in CheckHWID
            }
            catch (Exception ex)
            {
                Log($"[ОШИБКА] Произошел сбой при запуске: {ex.Message}");
                SetStatus("Ошибка запуска", 0);
            }
            finally
            {
                Dispatcher.Invoke(() =>
                {
                    isLaunching = false;
                    PlayButton.IsEnabled = true;
                    NicknameInput.IsEnabled = true;
                    OpenSettingsButton.IsEnabled = true;
                });
            }
        }

        private bool IsFileLocked(string filePath)
        {
            if (!File.Exists(filePath)) return false;
            try
            {
                using (var stream = new FileStream(filePath, FileMode.Open, FileAccess.ReadWrite, FileShare.None))
                {
                    // File is not locked
                }
                return false;
            }
            catch (IOException)
            {
                return true;
            }
        }

        private void StartLaunchPipeline(string nickname, int ramGb)
        {
            Log("Начало процесса подготовки игры...");

            string gameDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".solutionvisuals");
            string targetModJar = Path.Combine(gameDir, "mods", "SolutionVisual.jar");
            if (IsFileLocked(targetModJar))
            {
                Log("[ОШИБКА] Обнаружена запущенная копия игры (файл SolutionVisual.jar заблокирован).");
                Dispatcher.Invoke(() =>
                {
                    MessageBox.Show(
                        "Игра уже запущена или её процесс завис в фоновом режиме.\n\nПожалуйста, закройте запущенную копию Minecraft перед повторным запуском.",
                        "Игра уже запущена",
                        MessageBoxButton.OK,
                        MessageBoxImage.Warning
                    );
                });
                return;
            }

            string currentDir = AppDomain.CurrentDomain.BaseDirectory;
            string? rootDir = FindProjectRoot(currentDir);
            bool isDevMode = rootDir != null;

            if (isDevMode)
            {
                Log($"[РЕЖИМ РАЗРАБОТЧИКА] Корневая папка проекта: {rootDir}");
                SetStatus("Сборка нашего мода...", 5);
                // 1. Build the mod using gradlew remapJar
                if (!BuildMod(rootDir!))
                {
                    Log("[ОШИБКА] Сборка мода завершилась неудачно.");
                    return;
                }
            }
            else
            {
                Log("[АВТОНОМНЫЙ РЕЖИМ] Запущен без исходного кода. Мод будет загружен автоматически.");
            }

            SetStatus("Подготовка директории игры...", 15);
            gameDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".solutionvisuals");
            Directory.CreateDirectory(gameDir);
            Directory.CreateDirectory(Path.Combine(gameDir, "versions"));
            Directory.CreateDirectory(Path.Combine(gameDir, "libraries"));
            Directory.CreateDirectory(Path.Combine(gameDir, "mods"));

            // 2. Download Minecraft 1.21.4 client and libraries
            SetStatus("Загрузка метаданных игры...", 20);
            if (!DownloadMinecraftAndLibraries(gameDir))
            {
                Log("[ОШИБКА] Загрузка файлов Minecraft завершилась неудачно.");
                return;
            }

            // 3. Download Fabric Loader
            SetStatus("Установка Fabric...", 65);
            if (!InstallFabric(gameDir))
            {
                Log("[ОШИБКА] Установка Fabric завершилась неудачно.");
                return;
            }

            // 4. Download Fabric API and copy/download SolutionVisual mod
            SetStatus("Установка модов...", 85);
            if (!InstallMods(isDevMode, rootDir, gameDir))
            {
                Log("[ОШИБКА] Установка модов завершилась неудачно.");
                return;
            }

            // 5. Launch the game
            SetStatus("Запуск игры...", 95);
            LaunchGame(gameDir, nickname, ramGb);
        }

        private string? FindProjectRoot(string startDir)
        {
            string dir = startDir;
            while (!string.IsNullOrEmpty(dir))
            {
                if (File.Exists(Path.Combine(dir, "gradlew.bat")))
                {
                    return dir;
                }
                dir = Path.GetDirectoryName(dir);
            }
            return null;
        }

        private bool BuildMod(string rootDir)
        {
            Log("Запуск сборки мода SolutionVisual...");
            var startInfo = new ProcessStartInfo
            {
                FileName = "cmd.exe",
                Arguments = "/c gradlew.bat remapJar -x test",
                WorkingDirectory = rootDir,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            using (var process = Process.Start(startInfo))
            {
                if (process == null) return false;

                process.OutputDataReceived += (s, e) => { if (e.Data != null) Log($"[Gradle] {e.Data}"); };
                process.ErrorDataReceived += (s, e) => { if (e.Data != null) Log($"[Gradle ОШИБКА] {e.Data}"); };
                
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();
                process.WaitForExit();

                return process.ExitCode == 0;
            }
        }

        private bool DownloadMinecraftAndLibraries(string gameDir)
        {
            try
            {
                Log("Запрос манифеста версий Mojang...");
                string manifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
                string manifestJson = SafeGetStringAsync(manifestUrl);
                
                using var manifestDoc = JsonDocument.Parse(manifestJson);
                var versions = manifestDoc.RootElement.GetProperty("versions");
                
                string versionUrl = "";
                foreach (var v in versions.EnumerateArray())
                {
                    if (v.GetProperty("id").GetString() == "1.21.4")
                    {
                        versionUrl = v.GetProperty("url").GetString();
                        break;
                    }
                }

                if (string.IsNullOrEmpty(versionUrl))
                {
                    Log("Версия 1.21.4 не найдена в манифесте Mojang.");
                    return false;
                }

                Log("Загрузка профиля версии 1.21.4...");
                string versionJson = SafeGetStringAsync(versionUrl);
                
                string versionDir = Path.Combine(gameDir, "versions", "1.21.4");
                Directory.CreateDirectory(versionDir);
                File.WriteAllText(Path.Combine(versionDir, "1.21.4.json"), versionJson);

                using var versionDoc = JsonDocument.Parse(versionJson);
                
                // Download Client Jar
                string clientJarPath = Path.Combine(versionDir, "1.21.4.jar");
                if (!File.Exists(clientJarPath))
                {
                    string clientUrl = versionDoc.RootElement.GetProperty("downloads").GetProperty("client").GetProperty("url").GetString();
                    Log($"Загрузка client.jar (1.21.4) из Mojang...");
                    DownloadFileWithProgress(clientUrl, clientJarPath, 20, 40).GetAwaiter().GetResult();
                }
                else
                {
                    Log("Minecraft client.jar (1.21.4) уже существует.");
                }

                // Download Libraries
                var libraries = versionDoc.RootElement.GetProperty("libraries");
                int libCount = libraries.GetArrayLength();
                int idx = 0;
                Log($"Проверка и скачивание библиотек Mojang ({libCount} шт)...");
                string officialLibDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "libraries");

                foreach (var lib in libraries.EnumerateArray())
                {
                    idx++;
                    if (!lib.TryGetProperty("downloads", out var downloads) || !downloads.TryGetProperty("artifact", out var artifact))
                    {
                        continue;
                    }

                    string path = artifact.GetProperty("path").GetString();
                    string url = artifact.GetProperty("url").GetString();
                    string targetPath = Path.Combine(gameDir, "libraries", path);

                    if (!File.Exists(targetPath))
                    {
                        Directory.CreateDirectory(Path.GetDirectoryName(targetPath));
                        string officialPath = Path.Combine(officialLibDir, path);
                        if (File.Exists(officialPath))
                        {
                            Log($"Копирование библиотеки из .minecraft ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            File.Copy(officialPath, targetPath, true);
                        }
                        else
                        {
                            Log($"Загрузка библиотеки ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            byte[] libBytes = SafeGetByteArrayAsync(url);
                            File.WriteAllBytes(targetPath, libBytes);
                        }
                    }

                    double progress = 40.0 + ((double)idx / libCount) * 15.0;
                    SetStatus("Скачивание библиотек Mojang...", progress);
                }

                // Download Asset Index JSON
                var assetIndex = versionDoc.RootElement.GetProperty("assetIndex");
                string assetIndexId = assetIndex.GetProperty("id").GetString();
                string assetIndexUrl = assetIndex.GetProperty("url").GetString();
                string assetIndexTarget = Path.Combine(gameDir, "assets", "indexes", $"{assetIndexId}.json");
                
                if (!File.Exists(assetIndexTarget))
                {
                    Directory.CreateDirectory(Path.GetDirectoryName(assetIndexTarget));
                    Log($"Загрузка индекса ресурсов: {assetIndexId}.json");
                    string assetsJson = SafeGetStringAsync(assetIndexUrl);
                    File.WriteAllText(assetIndexTarget, assetsJson);
                }

                // Copy existing objects from official .minecraft if possible to avoid huge assets downloads
                string officialAssetsDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "assets");
                if (Directory.Exists(officialAssetsDir))
                {
                    Log("Обнаружена папка ресурсов официального Minecraft, ресурсы будут слинкованы.");
                }

                return true;
            }
            catch (Exception ex)
            {
                Log($"[ОШИБКА] При загрузке Mojang файлов: {ex.Message}");
                return false;
            }
        }

        private async Task DownloadFileWithProgress(string url, string targetPath, double startPct, double endPct)
        {
            if (url.StartsWith("https://raw.githubusercontent.com/"))
            {
                try
                {
                    string apiUrl = GetGitHubApiUrl(url);
                    await DownloadFileWithProgressInternal(apiUrl, targetPath, startPct, endPct);
                    return;
                }
                catch
                {
                    // Fall back to original raw URL / mirror flow
                }
            }

            string resolved = ResolveUrl(url);
            try
            {
                await DownloadFileWithProgressInternal(resolved, targetPath, startPct, endPct);
            }
            catch (Exception ex)
            {
                bool activatedMirror = false;
                if (url.StartsWith("https://raw.githubusercontent.com/") && !useGitHubMirror)
                {
                    useGitHubMirror = true;
                    activatedMirror = true;
                    Log($"[СЕТЬ] Ошибка скачивания с GitHub ({ex.Message}). Переключение на зеркало...");
                }
                else if ((url.Contains("mojang.com") || url.Contains("fabricmc.net")) && !useMojangMirror)
                {
                    useMojangMirror = true;
                    activatedMirror = true;
                    Log($"[СЕТЬ] Ошибка скачивания с Mojang/Fabric ({ex.Message}). Переключение на зеркало...");
                }

                if (activatedMirror)
                {
                    string retriedUrl = ResolveUrl(url);
                    await DownloadFileWithProgressInternal(retriedUrl, targetPath, startPct, endPct);
                }
                else
                {
                    throw;
                }
            }
        }

        private async Task DownloadFileWithProgressInternal(string url, string targetPath, double startPct, double endPct)
        {
            var request = new HttpRequestMessage(HttpMethod.Get, url);
            if (url.StartsWith("https://api.github.com/"))
            {
                request.Headers.Accept.ParseAdd("application/vnd.github.v3.raw");
            }

            using (var response = await httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead))
            {
                response.EnsureSuccessStatusCode();
                long? totalBytes = response.Content.Headers.ContentLength;

                using (var contentStream = await response.Content.ReadAsStreamAsync())
                using (var fileStream = new FileStream(targetPath, FileMode.Create, FileAccess.Write, FileShare.None, 8192, true))
                {
                    byte[] buffer = new byte[8192];
                    long totalRead = 0;
                    int read;

                    while ((read = await contentStream.ReadAsync(buffer, 0, buffer.Length)) > 0)
                    {
                        await fileStream.WriteAsync(buffer, 0, read);
                        totalRead += read;

                        if (totalBytes.HasValue)
                        {
                            double pct = (double)totalRead / totalBytes.Value;
                            double progress = startPct + pct * (endPct - startPct);
                            SetStatus($"Загрузка файла ({totalRead / 1024 / 1024}MB / {totalBytes.Value / 1024 / 1024}MB)...", progress);
                        }
                    }
                }
            }
        }

        private bool InstallFabric(string gameDir)
        {
            try
            {
                Log("Запрос метаданных профиля Fabric Loader...");
                string fabricProfileUrl = "https://meta.fabricmc.net/v2/versions/loader/1.21.4/0.16.14/profile/json";
                string profileJson = SafeGetStringAsync(fabricProfileUrl);

                string fabricVersionDir = Path.Combine(gameDir, "versions", "fabric-loader-0.16.14-1.21.4");
                Directory.CreateDirectory(fabricVersionDir);
                File.WriteAllText(Path.Combine(fabricVersionDir, "fabric-loader-0.16.14-1.21.4.json"), profileJson);

                using var profileDoc = JsonDocument.Parse(profileJson);
                var libraries = profileDoc.RootElement.GetProperty("libraries");
                int libCount = libraries.GetArrayLength();
                int idx = 0;
                Log($"Загрузка библиотек Fabric ({libCount} шт)...");

                string officialLibDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "libraries");
                foreach (var lib in libraries.EnumerateArray())
                {
                    idx++;
                    string name = lib.GetProperty("name").GetString();
                    string urlBase = lib.GetProperty("url").GetString();
                    
                    // Convert Maven coordinate to URL path
                    string mavenPath = GetMavenPath(name);
                    string downloadUrl = urlBase + mavenPath;
                    string targetPath = Path.Combine(gameDir, "libraries", mavenPath);

                    if (!File.Exists(targetPath))
                    {
                        Directory.CreateDirectory(Path.GetDirectoryName(targetPath));
                        string officialPath = Path.Combine(officialLibDir, mavenPath);
                        if (File.Exists(officialPath))
                        {
                            Log($"Копирование Fabric либы из .minecraft ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            File.Copy(officialPath, targetPath, true);
                        }
                        else
                        {
                            Log($"Загрузка Fabric либы ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            byte[] libBytes = SafeGetByteArrayAsync(downloadUrl);
                            File.WriteAllBytes(targetPath, libBytes);
                        }
                    }

                    double progress = 65.0 + ((double)idx / libCount) * 15.0;
                    SetStatus("Загрузка библиотек Fabric...", progress);
                }

                return true;
            }
            catch (Exception ex)
            {
                Log($"[ОШИБКА] При установке Fabric: {ex.Message}");
                return false;
            }
        }

        private string GetMavenPath(string coordinate)
        {
            var parts = coordinate.Split(':');
            var group = parts[0].Replace('.', '/');
            var artifact = parts[1];
            var version = parts[2];
            
            // Check for classifier (e.g. net.fabricmc:sponge-mixin:0.15.5+mixin.0.8.7)
            if (parts.Length > 3)
            {
                var classifier = parts[3];
                return $"{group}/{artifact}/{version}/{artifact}-{version}-{classifier}.jar";
            }
            return $"{group}/{artifact}/{version}/{artifact}-{version}.jar";
        }

        private bool InstallMods(bool isDevMode, string? rootDir, string gameDir)
        {
            try
            {
                // 1. Download Fabric API 0.119.4+1.21.4
                string fabricApiTarget = Path.Combine(gameDir, "mods", "fabric-api-0.119.4+1.21.4.jar");
                if (!File.Exists(fabricApiTarget))
                {
                    Log("Загрузка Fabric API из Maven...");
                    string fabricApiUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.119.4+1.21.4/fabric-api-0.119.4+1.21.4.jar";
                    DownloadFileWithProgress(fabricApiUrl, fabricApiTarget, 85, 90).GetAwaiter().GetResult();
                }
                else
                {
                    Log("Fabric API уже установлен.");
                }

                // 2. Install SolutionVisual mod
                string targetModJar = Path.Combine(gameDir, "mods", "SolutionVisual.jar");

                if (isDevMode && rootDir != null)
                {
                    string compiledModJar = Path.Combine(rootDir, "build", "libs", "solution-1.0.0.jar");
                    if (!File.Exists(compiledModJar))
                    {
                        // Search recursively in build/libs/
                        string libsDir = Path.Combine(rootDir, "build", "libs");
                        if (Directory.Exists(libsDir))
                        {
                            var files = Directory.GetFiles(libsDir, "solution-*.jar");
                            if (files.Length > 0)
                            {
                                compiledModJar = files[0];
                            }
                        }
                    }

                    if (File.Exists(compiledModJar))
                    {
                        Log($"Копирование мода SolutionVisual из {Path.GetFileName(compiledModJar)}...");
                        File.Copy(compiledModJar, targetModJar, true);
                        Log("Мод SolutionVisual успешно скопирован.");
                    }
                    else
                    {
                        Log($"[ПРЕДУПРЕЖДЕНИЕ] Скомпилированный мод не найден по пути: {compiledModJar}");
                    }
                }
                else
                {
                    // Standalone mode: Download or update from a remote source
                    string remoteVersionUrl = "https://raw.githubusercontent.com/iop21322132/solution-visuals/main/version.txt";
                    string remoteModUrl = "https://raw.githubusercontent.com/iop21322132/solution-visuals/main/SolutionVisual.jar";
                    
                    bool needDownload = false;
                    string latestVersion = "";
                    
                    try
                    {
                        Log("Проверка обновлений мода SolutionVisual...");
                        latestVersion = SafeGetStringAsync(remoteVersionUrl).Trim();
                        if (string.IsNullOrEmpty(currentModVersion) || currentModVersion != latestVersion || !File.Exists(targetModJar))
                        {
                            needDownload = true;
                            if (!string.IsNullOrEmpty(latestVersion))
                            {
                                Log($"Доступна новая версия мода: {latestVersion} (установлена: {(string.IsNullOrEmpty(currentModVersion) ? "нет" : currentModVersion)})");
                            }
                        }
                        else
                        {
                            Log($"Мод SolutionVisual уже обновлен (версия {currentModVersion}).");
                        }
                    }
                    catch (Exception ex)
                    {
                        Log($"[ПРЕДУПРЕЖДЕНИЕ] Не удалось проверить обновления мода: {ex.Message}");
                        if (!File.Exists(targetModJar))
                        {
                            needDownload = true;
                            Log("Локальный файл мода отсутствует. Будет выполнена попытка загрузки...");
                        }
                    }
                    
                    if (needDownload)
                    {
                        Log("Загрузка готового мода SolutionVisual с удаленного сервера...");
                        try
                        {
                            DownloadFileWithProgress(remoteModUrl, targetModJar, 90, 95).GetAwaiter().GetResult();
                            Log("Мод SolutionVisual успешно загружен.");
                            if (!string.IsNullOrEmpty(latestVersion))
                            {
                                currentModVersion = latestVersion;
                                SaveConfig();
                            }
                        }
                        catch (Exception ex)
                        {
                            Log($"[ПРЕДУПРЕЖДЕНИЕ] Не удалось скачать мод по умолчанию ({ex.Message}). Будет запущен чистый Fabric.");
                        }
                    }
                }

                return true;
            }
            catch (Exception ex)
            {
                Log($"[ОШИБКА] При установке модов: {ex.Message}");
                return false;
            }
        }

        private void LaunchGame(string gameDir, string nickname, int ramGb)
        {
            try
            {
                Log("Поиск Java...");
                string javaPath = FindJavaExecutable(gameDir);
                if (string.IsNullOrEmpty(javaPath))
                {
                    Log("[ОШИБКА] Java не найдена на вашем компьютере и не удалось скачать её автоматически.");
                    return;
                }

                if (!Path.IsPathRooted(javaPath))
                {
                    javaPath = ResolveFullPath(javaPath);
                }

                int majorVersion = GetJavaMajorVersion(javaPath);
                if (majorVersion > 0 && majorVersion < 21)
                {
                    Log($"[ОШИБКА] Обнаруженная версия Java ({majorVersion}) не подходит. Требуется Java 21 или новее.");
                    Dispatcher.Invoke(() =>
                    {
                        MessageBox.Show(
                            $"Для запуска игры требуется Java 21 или выше. Обнаруженная версия на вашем ПК: {majorVersion} ({javaPath}).\n\nПожалуйста, установите Java 21 или проверьте подключение к интернету, чтобы лаунчер мог скачать её автоматически.",
                            "Несовместимая версия Java",
                            MessageBoxButton.OK,
                            MessageBoxImage.Error
                        );
                    });
                    return;
                }

                Log($"Используется Java: {javaPath}");

                // Classpath Construction
                Log("Построение classpath...");
                var classpathJars = new List<string>();

                // Add Minecraft client jar
                classpathJars.Add(Path.Combine(gameDir, "versions", "1.21.4", "1.21.4.jar"));

                // Resolve libraries from JSONs to prevent duplicate version conflicts (e.g. ASM 9.6 vs 9.8)
                var resolvedLibraries = new Dictionary<string, string>();

                bool IsAllowedOnWindows(JsonElement rulesElement)
                {
                    if (rulesElement.ValueKind != JsonValueKind.Array) return true;
                    bool allowed = false;
                    bool hasOsRule = false;
                    foreach (var rule in rulesElement.EnumerateArray())
                    {
                        string action = rule.GetProperty("action").GetString() ?? "allow";
                        if (rule.TryGetProperty("os", out var os))
                        {
                            hasOsRule = true;
                            string osName = os.TryGetProperty("name", out var name) ? name.GetString() ?? "" : "";
                            if (osName == "windows")
                            {
                                allowed = (action == "allow");
                            }
                        }
                        else
                        {
                            allowed = (action == "allow");
                        }
                    }
                    return !hasOsRule || allowed;
                }

                // 1. Read Minecraft 1.21.4 libraries
                string mcJsonPath = Path.Combine(gameDir, "versions", "1.21.4", "1.21.4.json");
                if (File.Exists(mcJsonPath))
                {
                    try
                    {
                        string mcJson = File.ReadAllText(mcJsonPath);
                        using var doc = JsonDocument.Parse(mcJson);
                        var libraries = doc.RootElement.GetProperty("libraries");
                        foreach (var lib in libraries.EnumerateArray())
                        {
                            if (lib.TryGetProperty("rules", out var rules) && !IsAllowedOnWindows(rules))
                            {
                                continue;
                            }

                            if (!lib.TryGetProperty("downloads", out var downloads) || !downloads.TryGetProperty("artifact", out var artifact))
                            {
                                continue;
                            }

                            string name = lib.GetProperty("name").GetString() ?? "";
                            string path = artifact.GetProperty("path").GetString() ?? "";
                            if (!string.IsNullOrEmpty(name) && !string.IsNullOrEmpty(path))
                            {
                                 var parts = name.Split(':');
                                 if (parts.Length >= 2)
                                 {
                                     string key = $"{parts[0]}:{parts[1]}";
                                     if (parts.Length > 3)
                                     {
                                         key += $":{parts[3]}";
                                     }
                                     resolvedLibraries[key] = Path.Combine(gameDir, "libraries", path);
                                 }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Log($"[ПРЕДУПРЕЖДЕНИЕ] Ошибка при чтении библиотек 1.21.4.json: {ex.Message}");
                    }
                }

                // 2. Read Fabric profile libraries to override Mojang libraries if duplicate
                string fabricJsonPath = Path.Combine(gameDir, "versions", "fabric-loader-0.16.14-1.21.4", "fabric-loader-0.16.14-1.21.4.json");
                if (File.Exists(fabricJsonPath))
                {
                    try
                    {
                        string fabricJson = File.ReadAllText(fabricJsonPath);
                        using var doc = JsonDocument.Parse(fabricJson);
                        var libraries = doc.RootElement.GetProperty("libraries");
                        foreach (var lib in libraries.EnumerateArray())
                        {
                            string name = lib.GetProperty("name").GetString() ?? "";
                            if (!string.IsNullOrEmpty(name))
                            {
                                 var parts = name.Split(':');
                                 if (parts.Length >= 2)
                                 {
                                     string key = $"{parts[0]}:{parts[1]}";
                                     if (parts.Length > 3)
                                     {
                                         key += $":{parts[3]}";
                                     }
                                     string mavenPath = GetMavenPath(name);
                                     resolvedLibraries[key] = Path.Combine(gameDir, "libraries", mavenPath);
                                 }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Log($"[ПРЕДУПРЕЖДЕНИЕ] Ошибка при чтении библиотек Fabric json: {ex.Message}");
                    }
                }

                // Add all resolved libraries that exist
                foreach (var libPath in resolvedLibraries.Values)
                {
                    if (File.Exists(libPath))
                    {
                        classpathJars.Add(libPath);
                    }
                    else
                    {
                        Log($"[ПРЕДУПРЕЖДЕНИЕ] Файл библиотеки не найден: {Path.GetFileName(libPath)}");
                    }
                }

                // Remove duplicates and construct classpath string
                var uniqueJars = classpathJars.Distinct().ToList();
                string classpath = string.Join(";", uniqueJars);

                // Launch Arguments
                string assetsDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "assets");
                if (!Directory.Exists(assetsDir))
                {
                    assetsDir = Path.Combine(gameDir, "assets");
                }

                string uuid = Guid.NewGuid().ToString("N");

                // Write arguments to a local file to bypass Windows command-line argument length limit (32k characters)
                string argsFilePath = Path.Combine(gameDir, "launch.args");
                Log("Запись аргументов запуска в launch.args...");

                string FormatArg(string arg)
                {
                    string clean = arg.Replace("\\", "/");
                    if (clean.StartsWith("\"") && clean.EndsWith("\"") && clean.Length >= 2)
                    {
                        clean = clean.Substring(1, clean.Length - 2);
                    }
                    clean = clean.Replace("\"", "\\\"");
                    if (clean.Contains(" ") || clean.Contains(";") || clean.Contains("="))
                    {
                        return $"\"{clean}\"";
                    }
                    return clean;
                }

                var formattedArgs = new List<string>
                {
                    $"-Xmx{ramGb}G",
                    "-Xms512M",
                    "-DFabricMcEmu=net.minecraft.client.main.Main",
                    "-cp",
                    FormatArg(classpath),
                    "net.fabricmc.loader.impl.launch.knot.KnotClient",
                    "--username",
                    FormatArg(nickname),
                    "--version",
                    "1.21.4-Fabric",
                    "--gameDir",
                    FormatArg(gameDir),
                    "--assetsDir",
                    FormatArg(assetsDir),
                    "--assetIndex",
                    "17",
                    "--uuid",
                    uuid,
                    "--accessToken",
                    "00000000000000000000000000000000"
                };

                File.WriteAllLines(argsFilePath, formattedArgs);

                Log("Запуск процесса Minecraft...");
                var startInfo = new ProcessStartInfo
                {
                    FileName = javaPath,
                    Arguments = $"@\"{argsFilePath.Replace("\\", "/")}\"",
                    WorkingDirectory = gameDir,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                var gameProcess = Process.Start(startInfo);
                if (gameProcess == null)
                {
                    Log("[ОШИБКА] Не удалось запустить процесс игры.");
                    return;
                }
                bool memoryErrorDetected = false;
                gameProcess.OutputDataReceived += (s, e) => 
                { 
                    if (e.Data != null) 
                    {
                        Log($"[Minecraft] {e.Data}");
                        if (e.Data.Contains("insufficient memory") || e.Data.Contains("commit_memory") || e.Data.Contains("errno=1455"))
                        {
                            memoryErrorDetected = true;
                        }
                    }
                };
                gameProcess.ErrorDataReceived += (s, e) => 
                { 
                    if (e.Data != null) 
                    {
                        Log($"[Minecraft ОШИБКА] {e.Data}");
                        if (e.Data.Contains("insufficient memory") || e.Data.Contains("commit_memory") || e.Data.Contains("errno=1455") || e.Data.Contains("Файл подкачки слишком мал"))
                        {
                            memoryErrorDetected = true;
                        }
                    }
                };

                gameProcess.BeginOutputReadLine();
                gameProcess.BeginErrorReadLine();

                Log("Игра запущена! Вывод консоли перенаправлен сюда.");
                SetStatus("Игра запущена", 100);

                // Wait for exit in background task
                Task.Run(() =>
                {
                    gameProcess.WaitForExit();
                    Log($"[Solution Launcher] Процесс игры завершился с кодом {gameProcess.ExitCode}");
                    SetStatus("Готов к запуску", 0);
                    if (memoryErrorDetected)
                    {
                        Dispatcher.Invoke(() =>
                        {
                            int currentRam = (int)RamSlider.Value;
                            int newRam = Math.Max(2, currentRam - 1);
                            
                            if (newRam < currentRam)
                            {
                                RamSlider.Value = newRam;
                                RamValueText.Text = $"{newRam} ГБ";
                                SaveConfig();
                                Log($"[СИСТЕМА] Автоматически уменьшено выделение RAM с {currentRam} ГБ до {newRam} ГБ из-за ошибки памяти.");
                                
                                MessageBox.Show(
                                    $"Не удалось запустить игру из-за нехватки виртуальной памяти (файла подкачки) на вашем компьютере.\n\n" +
                                    $"Мы автоматически уменьшили выделение оперативной памяти в настройках до {newRam} ГБ.\n\n" +
                                    "Пожалуйста, попробуйте нажать кнопку 'Начать игру' снова.",
                                    "Недостаточно виртуальной памяти",
                                    MessageBoxButton.OK,
                                    MessageBoxImage.Warning
                                );
                            }
                            else
                            {
                                MessageBox.Show(
                                    "Не удалось запустить игру из-за нехватки виртуальной памяти (файла подкачки) на вашем компьютере.\n\n" +
                                    "Решения:\n" +
                                    "1. Закройте все лишние программы (браузеры, Discord, другие игры).\n" +
                                    "2. Увеличьте размер файла подкачки в настройках Windows (или включите его, если он отключен).",
                                    "Недостаточно виртуальной памяти",
                                    MessageBoxButton.OK,
                                    MessageBoxImage.Warning
                                );
                            }
                        });
                    }
                    try
                    {
                        File.Delete(argsFilePath);
                    }
                    catch {}
                });
            }
            catch (Exception ex)
            {
                Log($"[ОШИБКА] Во время запуска процесса: {ex.Message}");
            }
        }

        private string FindJavaExecutable(string gameDir)
        {
            // 1. Check if portable JRE 21 is already downloaded
            string portableJreDir = Path.Combine(gameDir, "jre");
            if (Directory.Exists(portableJreDir))
            {
                var javaws = Directory.GetFiles(portableJreDir, "javaw.exe", SearchOption.AllDirectories);
                if (javaws.Length > 0 && GetJavaMajorVersion(javaws[0]) >= 21) return javaws[0];
            }

            // 2. Check JAVA_HOME
            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                string path = Path.Combine(javaHome, "bin", "javaw.exe");
                if (File.Exists(path) && GetJavaMajorVersion(path) >= 21) return path;
            }

            // 3. Check Gradle JDKs
            string gradleJdks = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".gradle", "jdks");
            if (Directory.Exists(gradleJdks))
            {
                var javaws = Directory.GetFiles(gradleJdks, "javaw.exe", SearchOption.AllDirectories);
                foreach (var javaw in javaws)
                {
                    if (GetJavaMajorVersion(javaw) >= 21) return javaw;
                }
            }

            // 4. Check common registry install directories
            string[] commonPaths = {
                @"C:\Program Files\Eclipse Adoptium",
                @"C:\Program Files\Java",
                @"C:\Program Files\BellSoft",
                @"C:\Program Files\Microsoft"
            };

            foreach (var cp in commonPaths)
            {
                if (Directory.Exists(cp))
                {
                    var javaws = Directory.GetFiles(cp, "javaw.exe", SearchOption.AllDirectories);
                    foreach (var javaw in javaws)
                    {
                        if (GetJavaMajorVersion(javaw) >= 21) return javaw;
                    }
                }
            }

            // 5. Try to download portable JRE 21 from Adoptium API
            try
            {
                Log("Java 21 не найдена. Начинаем автоматическую загрузку портативной Java 21 JRE...");
                string jreZipPath = Path.Combine(gameDir, "jre21.zip");
                string downloadUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse";

                SetStatus("Загрузка Java 21...", 5);
                DownloadFileWithProgress(downloadUrl, jreZipPath, 5, 45).GetAwaiter().GetResult();

                Log("Загрузка завершена. Распаковка Java 21...");
                SetStatus("Распаковка Java 21...", 45);

                if (Directory.Exists(portableJreDir))
                {
                    Directory.Delete(portableJreDir, true);
                }
                Directory.CreateDirectory(portableJreDir);

                ZipFile.ExtractToDirectory(jreZipPath, portableJreDir);

                try
                {
                    File.Delete(jreZipPath);
                }
                catch {}

                var javaws = Directory.GetFiles(portableJreDir, "javaw.exe", SearchOption.AllDirectories);
                if (javaws.Length > 0)
                {
                    Log("Портативная Java 21 успешно установлена.");
                    return javaws[0];
                }
            }
            catch (Exception ex)
            {
                Log($"[ОШИБКА] Не удалось скачать портативную Java 21: {ex.Message}");
            }

            // 6. Fallback to system path search
            return "javaw.exe";
        }

        private int GetJavaMajorVersion(string javawPath)
        {
            try
            {
                if (!File.Exists(javawPath)) return 0;

                // 1. Try reading FileVersionInfo (fast and safe)
                var info = FileVersionInfo.GetVersionInfo(javawPath);
                int major = info.FileMajorPart;
                if (major == 0)
                {
                    major = info.ProductMajorPart;
                }
                
                // For older versions like 1.8
                if (major == 1 && info.FileMinorPart > 1)
                {
                    major = info.FileMinorPart;
                }

                if (major > 0) return major;
            }
            catch {}

            // 2. Fallback to process execution if FileVersionInfo failed or returned 0
            try
            {
                string javaPath = javawPath.Replace("javaw.exe", "java.exe", StringComparison.OrdinalIgnoreCase);
                if (!File.Exists(javaPath)) return 0;

                var startInfo = new ProcessStartInfo
                {
                    FileName = javaPath,
                    Arguments = "-version",
                    RedirectStandardError = true,
                    RedirectStandardOutput = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using (var process = Process.Start(startInfo))
                {
                    if (process == null) return 0;
                    
                    string stdErr = process.StandardError.ReadToEnd();
                    string stdOut = process.StandardOutput.ReadToEnd();
                    
                    bool exited = process.WaitForExit(1500);
                    if (!exited)
                    {
                        try { process.Kill(); } catch {}
                    }
                    
                    string output = stdErr + "\n" + stdOut;
                    int versionIdx = output.IndexOf("version \"", StringComparison.OrdinalIgnoreCase);
                    if (versionIdx != -1)
                    {
                        string verStr = output.Substring(versionIdx + 9);
                        int endQuote = verStr.IndexOf('"');
                        if (endQuote != -1)
                        {
                            verStr = verStr.Substring(0, endQuote);
                            var parts = verStr.Split('.');
                            if (parts.Length > 0)
                            {
                                if (int.TryParse(parts[0], out int majorPart))
                                {
                                    if (majorPart == 1 && parts.Length > 1)
                                    {
                                        if (int.TryParse(parts[1], out int subMajor))
                                        {
                                            majorPart = subMajor;
                                        }
                                    }
                                    return majorPart;
                                }
                            }
                        }
                    }
                }
            }
            catch {}
            return 0;
        }

        private string ResolveFullPath(string fileName)
        {
            if (File.Exists(fileName)) return Path.GetFullPath(fileName);

            var values = Environment.GetEnvironmentVariable("PATH");
            if (values != null)
            {
                foreach (var path in values.Split(Path.PathSeparator))
                {
                    var fullPath = Path.Combine(path, fileName);
                    if (File.Exists(fullPath))
                        return fullPath;
                }
            }
            return fileName;
        }

        private void OpenSettingsButton_Click(object sender, RoutedEventArgs e)
        {
            LoginPanel.Visibility = Visibility.Collapsed;
            SettingsPanel.Visibility = Visibility.Visible;
        }

        private void CloseSettingsButton_Click(object sender, RoutedEventArgs e)
        {
            SaveConfig();
            SettingsPanel.Visibility = Visibility.Collapsed;
            LoginPanel.Visibility = Visibility.Visible;
        }

        private void RamSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (RamValueText != null)
            {
                RamValueText.Text = $"{(int)RamSlider.Value} ГБ";
            }
        }

        private string GetConfigFilePath()
        {
            string gameDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".solutionvisuals");
            Directory.CreateDirectory(gameDir);
            return Path.Combine(gameDir, "launcher_config.json");
        }

        private void LoadConfig()
        {
            try
            {
                string path = GetConfigFilePath();
                if (File.Exists(path))
                {
                    string json = File.ReadAllText(path);
                    var config = JsonSerializer.Deserialize<LauncherConfig>(json);
                    if (config != null)
                    {
                        NicknameInput.Text = config.Nickname ?? "Player";
                        int loadedRam = config.RamGb > 0 ? config.RamGb : GetDefaultRamGb();
                        RamSlider.Value = Math.Min(loadedRam, RamSlider.Maximum);
                        RamValueText.Text = $"{(int)RamSlider.Value} ГБ";
                        currentModVersion = config.ModVersion ?? "";
                        return;
                    }
                }
            }
            catch (Exception ex)
            {
                Log($"[ПРЕДУПРЕЖДЕНИЕ] Не удалось загрузить настройки: {ex.Message}");
            }
            NicknameInput.Text = "Player";
            int defaultRam = GetDefaultRamGb();
            RamSlider.Value = defaultRam;
            RamValueText.Text = $"{defaultRam} ГБ";
            currentModVersion = "";
        }

        private void SaveConfig()
        {
            if (!Dispatcher.CheckAccess())
            {
                Dispatcher.Invoke(SaveConfig);
                return;
            }
            try
            {
                string path = GetConfigFilePath();
                var config = new LauncherConfig
                {
                    Nickname = NicknameInput.Text.Trim(),
                    RamGb = (int)RamSlider.Value,
                    ModVersion = currentModVersion
                };
                string json = JsonSerializer.Serialize(config, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(path, json);
            }
            catch (Exception ex)
            {
                Log($"[ПРЕДУПРЕЖДЕНИЕ] Не удалось сохранить настройки: {ex.Message}");
            }
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
        private struct MEMORYSTATUSEX
        {
            public uint dwLength;
            public uint dwMemoryLoad;
            public ulong ullTotalPhys;
            public ulong ullAvailPhys;
            public ulong ullTotalPageFile;
            public ulong ullAvailPageFile;
            public ulong ullTotalVirtual;
            public ulong ullAvailVirtual;
            public ulong ullAvailExtendedVirtual;
        }

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool GlobalMemoryStatusEx(ref MEMORYSTATUSEX lpBuffer);

        private int GetTotalRamGb()
        {
            try
            {
                var status = new MEMORYSTATUSEX();
                status.dwLength = (uint)Marshal.SizeOf(typeof(MEMORYSTATUSEX));
                if (GlobalMemoryStatusEx(ref status))
                {
                    double totalGb = (double)status.ullTotalPhys / (1024.0 * 1024.0 * 1024.0);
                    int rounded = (int)Math.Round(totalGb);
                    if (rounded > 0) return rounded;
                }
            }
            catch {}
            return 16; // default fallback
        }

        private int GetDefaultRamGb()
        {
            int totalRam = GetTotalRamGb();
            if (totalRam <= 4) return 2;
            if (totalRam <= 8) return 3;
            if (totalRam <= 16) return 4;
            return 6;
        }

        private void CheckHWID()
        {
            string hwid = GetMachineHWID();
            
            // Developer's master HWIDs (hardcoded backup)
            var masterHwids = new HashSet<string>(StringComparer.OrdinalIgnoreCase)
            {
                "D519912FE0726A6CF8B0F09E83EF4F8223DC4EE2220BCB341843BCA3AFF057BF",
                "85130DE4CD08E74B6367A0BFF9657DE27004B5D952B7A8B73ADAAC211FD5487F"
            };
            
            if (masterHwids.Contains(hwid))
            {
                return; // Admin bypass
            }

            var allowedHwids = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            
            // Fetch remote HWID database
            string remoteUrl = "https://raw.githubusercontent.com/iop21322132/solution-visuals/main/hwid.txt";
            try
            {
                using (var client = new HttpClient())
                {
                    client.Timeout = TimeSpan.FromSeconds(10);
                    string resolvedUrl = ResolveUrl(remoteUrl);
                    string remoteData;
                    try
                    {
                        remoteData = client.GetStringAsync(resolvedUrl).GetAwaiter().GetResult();
                    }
                    catch
                    {
                        if (!useGitHubMirror)
                        {
                            useGitHubMirror = true;
                            resolvedUrl = ResolveUrl(remoteUrl);
                            remoteData = client.GetStringAsync(resolvedUrl).GetAwaiter().GetResult();
                        }
                        else
                        {
                            throw;
                        }
                    }

                    foreach (var line in remoteData.Split(new[] { '\n', '\r' }, StringSplitOptions.RemoveEmptyEntries))
                    {
                        string clean = line.Trim();
                        if (!string.IsNullOrEmpty(clean)) allowedHwids.Add(clean);
                    }
                }
            }
            catch
            {
                // If remote fetch fails, only the master HWID has access
            }
            
            if (!allowedHwids.Contains(hwid))
            {
                Dispatcher.Invoke(() =>
                {
                    try
                    {
                        Clipboard.SetText(hwid);
                    }
                    catch {}
                    
                    string errorMsg = "Вас нету в базе данных что бы вас добавили отпишите в тикет что бы вас добавили и вставьте сообщение из буфера обмена которое является вашим Hwid - ключом.\n\n" +
                                      $"Ваш HWID-ключ (уже скопирован в буфер обмена):\n{hwid}";

                    MessageBox.Show(
                        errorMsg,
                        "Доступ ограничен",
                        MessageBoxButton.OK,
                        MessageBoxImage.Error
                    );
                    
                    Application.Current.Shutdown();
                });
                throw new UnauthorizedAccessException("HWID not authorized.");
            }
        }

        private void CheckLauncherUpdate()
        {
            Task.Run(() =>
            {
                string currentVersion = "3.6.4.7";
                string remoteVersionUrl = "https://raw.githubusercontent.com/iop21322132/solution-visuals/main/launcher_version.txt";
                string remoteExeUrl = "https://raw.githubusercontent.com/iop21322132/solution-visuals/main/SolutionLauncher.exe";

                try
                {
                    using (var client = new HttpClient())
                    {
                        client.Timeout = TimeSpan.FromSeconds(10);
                        string resolvedUrl = ResolveUrl(remoteVersionUrl);
                        string latestVersion = "";
                        try
                        {
                            latestVersion = client.GetStringAsync(resolvedUrl).GetAwaiter().GetResult().Trim();
                        }
                        catch
                        {
                            if (!useGitHubMirror)
                            {
                                useGitHubMirror = true;
                                resolvedUrl = ResolveUrl(remoteVersionUrl);
                                latestVersion = client.GetStringAsync(resolvedUrl).GetAwaiter().GetResult().Trim();
                            }
                            else
                            {
                                throw;
                            }
                        }

                        if (!string.IsNullOrEmpty(latestVersion) && latestVersion != currentVersion)
                        {
                            Log($"Найдено обновление лаунчера: {latestVersion} (текущая: {currentVersion}). Скачивание...");

                            string currentExePath = Process.GetCurrentProcess().MainModule?.FileName ?? "SolutionLauncher.exe";
                            string currentDir = Path.GetDirectoryName(currentExePath) ?? AppDomain.CurrentDomain.BaseDirectory;
                            string tempExePath = Path.Combine(currentDir, "SolutionLauncher.new");

                            // Download new exe
                            byte[] newExeBytes = null;
                            var exeUrlsToTry = new List<string>
                            {
                                ResolveUrl(remoteExeUrl),
                                remoteExeUrl.Replace("https://raw.githubusercontent.com/", "https://raw.gitmirror.com/"),
                                "https://ghproxy.net/" + remoteExeUrl
                            };

                            Exception lastEx = null;
                            foreach (var urlOption in exeUrlsToTry)
                            {
                                try
                                {
                                    newExeBytes = client.GetByteArrayAsync(urlOption).GetAwaiter().GetResult();
                                    break;
                                }
                                catch (Exception ex)
                                {
                                    lastEx = ex;
                                }
                            }

                            if (newExeBytes == null)
                            {
                                if (!useGitHubMirror)
                                {
                                    useGitHubMirror = true;
                                    try
                                    {
                                        newExeBytes = client.GetByteArrayAsync(ResolveUrl(remoteExeUrl)).GetAwaiter().GetResult();
                                    }
                                    catch
                                    {
                                        throw lastEx ?? new Exception("Failed to download launcher update from all mirrors.");
                                    }
                                }
                                else
                                {
                                    throw lastEx ?? new Exception("Failed to download launcher update from all mirrors.");
                                }
                            }
                            File.WriteAllBytes(tempExePath, newExeBytes);

                            Log("Обновление лаунчера скачано. Установка и перезапуск...");

                            // Start self-replace batch script
                            string batchCommands = $"/c timeout /t 1 /nobreak && del /f /q \"{currentExePath}\" && move \"{tempExePath}\" \"{currentExePath}\" && start \"\" \"{currentExePath}\"";
                            Process.Start(new ProcessStartInfo
                            {
                                FileName = "cmd.exe",
                                Arguments = batchCommands,
                                CreateNoWindow = true,
                                UseShellExecute = false
                            });

                            Dispatcher.Invoke(() => Application.Current.Shutdown());
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log($"[ПРЕДУПРЕЖДЕНИЕ] Не удалось проверить обновления лаунчера: {ex.Message}");
                }
            });
        }

        private string GetMachineHWID()
        {
            try
            {
                string machineGuid = "";
                using (var key = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(@"SOFTWARE\Microsoft\Cryptography"))
                {
                    if (key != null)
                    {
                        machineGuid = key.GetValue("MachineGuid")?.ToString() ?? "";
                    }
                }
                
                if (string.IsNullOrEmpty(machineGuid))
                {
                    machineGuid = Environment.MachineName + Environment.UserName + Environment.ProcessorCount;
                }
                
                using (var sha256 = System.Security.Cryptography.SHA256.Create())
                {
                    byte[] bytes = Encoding.UTF8.GetBytes(machineGuid);
                    byte[] hash = sha256.ComputeHash(bytes);
                    return BitConverter.ToString(hash).Replace("-", "").ToUpper();
                }
            }
            catch
            {
                return "FALLBACK_HWID_KEY_ERROR";
            }
        }
    }

    public class LauncherConfig
    {
        public string Nickname { get; set; } = "Player";
        public int RamGb { get; set; } = 4;
        public string ModVersion { get; set; } = "";
    }
}