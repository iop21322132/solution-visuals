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
        private Process gameProcess;

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
                    Log($"[РЎР•РўР¬] РћС€РёР±РєР° РїРѕРґРєР»СЋС‡РµРЅРёСЏ Рє GitHub ({ex.Message}). РџРµСЂРµРєР»СЋС‡РµРЅРёРµ РЅР° Р·РµСЂРєР°Р»Рѕ...");
                }
                else if ((url.Contains("mojang.com") || url.Contains("fabricmc.net")) && !useMojangMirror)
                {
                    useMojangMirror = true;
                    activatedMirror = true;
                    Log($"[РЎР•РўР¬] РћС€РёР±РєР° РїРѕРґРєР»СЋС‡РµРЅРёСЏ Рє Mojang/Fabric ({ex.Message}). РџРµСЂРµРєР»СЋС‡РµРЅРёРµ РЅР° Р·РµСЂРєР°Р»Рѕ...");
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
                    Log($"[РЎР•РўР¬] РћС€РёР±РєР° РїРѕРґРєР»СЋС‡РµРЅРёСЏ Рє GitHub ({ex.Message}). РџРµСЂРµРєР»СЋС‡РµРЅРёРµ РЅР° Р·РµСЂРєР°Р»Рѕ...");
                }
                else if ((url.Contains("mojang.com") || url.Contains("fabricmc.net")) && !useMojangMirror)
                {
                    useMojangMirror = true;
                    activatedMirror = true;
                    Log($"[РЎР•РўР¬] РћС€РёР±РєР° РїРѕРґРєР»СЋС‡РµРЅРёСЏ Рє Mojang/Fabric ({ex.Message}). РџРµСЂРµРєР»СЋС‡РµРЅРёРµ РЅР° Р·РµСЂРєР°Р»Рѕ...");
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
                MessageBox.Show("РџРѕР¶Р°Р»СѓР№СЃС‚Р°, РІРІРµРґРёС‚Рµ РЅРёРєРЅРµР№Рј РїРµСЂРµРґ РЅР°С‡Р°Р»РѕРј РёРіСЂС‹.", "РћС€РёР±РєР°", MessageBoxButton.OK, MessageBoxImage.Warning);
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
                Log($"[РћРЁРР‘РљРђ] РџСЂРѕРёР·РѕС€РµР» СЃР±РѕР№ РїСЂРё Р·Р°РїСѓСЃРєРµ: {ex.Message}");
                SetStatus("РћС€РёР±РєР° Р·Р°РїСѓСЃРєР°", 0);
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
            Log("РќР°С‡Р°Р»Рѕ РїСЂРѕС†РµСЃСЃР° РїРѕРґРіРѕС‚РѕРІРєРё РёРіСЂС‹...");

            string gameDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".solutionvisuals");
            string targetModJar = Path.Combine(gameDir, "mods", "SolutionVisual.jar");
            if (IsFileLocked(targetModJar))
            {
                Log("[РћРЁРР‘РљРђ] РћР±РЅР°СЂСѓР¶РµРЅР° Р·Р°РїСѓС‰РµРЅРЅР°СЏ РєРѕРїРёСЏ РёРіСЂС‹ (С„Р°Р№Р» SolutionVisual.jar Р·Р°Р±Р»РѕРєРёСЂРѕРІР°РЅ).");
                Dispatcher.Invoke(() =>
                {
                    MessageBox.Show(
                        "РРіСЂР° СѓР¶Рµ Р·Р°РїСѓС‰РµРЅР° РёР»Рё РµС‘ РїСЂРѕС†РµСЃСЃ Р·Р°РІРёСЃ РІ С„РѕРЅРѕРІРѕРј СЂРµР¶РёРјРµ.\n\nРџРѕР¶Р°Р»СѓР№СЃС‚Р°, Р·Р°РєСЂРѕР№С‚Рµ Р·Р°РїСѓС‰РµРЅРЅСѓСЋ РєРѕРїРёСЋ Minecraft РїРµСЂРµРґ РїРѕРІС‚РѕСЂРЅС‹Рј Р·Р°РїСѓСЃРєРѕРј.",
                        "РРіСЂР° СѓР¶Рµ Р·Р°РїСѓС‰РµРЅР°",
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
                Log($"[Р Р•Р–РРњ Р РђР—Р РђР‘РћРўР§РРљРђ] РљРѕСЂРЅРµРІР°СЏ РїР°РїРєР° РїСЂРѕРµРєС‚Р°: {rootDir}");
                SetStatus("РЎР±РѕСЂРєР° РЅР°С€РµРіРѕ РјРѕРґР°...", 5);
                // 1. Build the mod using gradlew remapJar
                if (!BuildMod(rootDir!))
                {
                    Log("[РћРЁРР‘РљРђ] РЎР±РѕСЂРєР° РјРѕРґР° Р·Р°РІРµСЂС€РёР»Р°СЃСЊ РЅРµСѓРґР°С‡РЅРѕ.");
                    return;
                }
            }
            else
            {
                Log("[РђР’РўРћРќРћРњРќР«Р™ Р Р•Р–РРњ] Р—Р°РїСѓС‰РµРЅ Р±РµР· РёСЃС…РѕРґРЅРѕРіРѕ РєРѕРґР°. РњРѕРґ Р±СѓРґРµС‚ Р·Р°РіСЂСѓР¶РµРЅ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё.");
            }

            SetStatus("РџРѕРґРіРѕС‚РѕРІРєР° РґРёСЂРµРєС‚РѕСЂРёРё РёРіСЂС‹...", 15);
            gameDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".solutionvisuals");
            Directory.CreateDirectory(gameDir);
            Directory.CreateDirectory(Path.Combine(gameDir, "versions"));
            Directory.CreateDirectory(Path.Combine(gameDir, "libraries"));
            Directory.CreateDirectory(Path.Combine(gameDir, "mods"));

            // 2. Download Minecraft 1.21.4 client and libraries
            SetStatus("Р—Р°РіСЂСѓР·РєР° РјРµС‚Р°РґР°РЅРЅС‹С… РёРіСЂС‹...", 20);
            if (!DownloadMinecraftAndLibraries(gameDir))
            {
                Log("[РћРЁРР‘РљРђ] Р—Р°РіСЂСѓР·РєР° С„Р°Р№Р»РѕРІ Minecraft Р·Р°РІРµСЂС€РёР»Р°СЃСЊ РЅРµСѓРґР°С‡РЅРѕ.");
                return;
            }

            // 3. Download Fabric Loader
            SetStatus("РЈСЃС‚Р°РЅРѕРІРєР° Fabric...", 65);
            if (!InstallFabric(gameDir))
            {
                Log("[РћРЁРР‘РљРђ] РЈСЃС‚Р°РЅРѕРІРєР° Fabric Р·Р°РІРµСЂС€РёР»Р°СЃСЊ РЅРµСѓРґР°С‡РЅРѕ.");
                return;
            }

            // 4. Download Fabric API and copy/download SolutionVisual mod
            SetStatus("РЈСЃС‚Р°РЅРѕРІРєР° РјРѕРґРѕРІ...", 85);
            if (!InstallMods(isDevMode, rootDir, gameDir))
            {
                Log("[РћРЁРР‘РљРђ] РЈСЃС‚Р°РЅРѕРІРєР° РјРѕРґРѕРІ Р·Р°РІРµСЂС€РёР»Р°СЃСЊ РЅРµСѓРґР°С‡РЅРѕ.");
                return;
            }

            // 5. Launch the game
            SetStatus("Р—Р°РїСѓСЃРє РёРіСЂС‹...", 95);
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
            Log("Р—Р°РїСѓСЃРє СЃР±РѕСЂРєРё РјРѕРґР° SolutionVisual...");
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
                process.ErrorDataReceived += (s, e) => { if (e.Data != null) Log($"[Gradle РћРЁРР‘РљРђ] {e.Data}"); };
                
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
                Log("Р—Р°РїСЂРѕСЃ РјР°РЅРёС„РµСЃС‚Р° РІРµСЂСЃРёР№ Mojang...");
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
                    Log("Р’РµСЂСЃРёСЏ 1.21.4 РЅРµ РЅР°Р№РґРµРЅР° РІ РјР°РЅРёС„РµСЃС‚Рµ Mojang.");
                    return false;
                }

                Log("Р—Р°РіСЂСѓР·РєР° РїСЂРѕС„РёР»СЏ РІРµСЂСЃРёРё 1.21.4...");
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
                    Log($"Р—Р°РіСЂСѓР·РєР° client.jar (1.21.4) РёР· Mojang...");
                    DownloadFileWithProgress(clientUrl, clientJarPath, 20, 40).GetAwaiter().GetResult();
                }
                else
                {
                    Log("Minecraft client.jar (1.21.4) СѓР¶Рµ СЃСѓС‰РµСЃС‚РІСѓРµС‚.");
                }

                // Download Libraries
                var libraries = versionDoc.RootElement.GetProperty("libraries");
                int libCount = libraries.GetArrayLength();
                int idx = 0;
                Log($"РџСЂРѕРІРµСЂРєР° Рё СЃРєР°С‡РёРІР°РЅРёРµ Р±РёР±Р»РёРѕС‚РµРє Mojang ({libCount} С€С‚)...");
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
                            Log($"РљРѕРїРёСЂРѕРІР°РЅРёРµ Р±РёР±Р»РёРѕС‚РµРєРё РёР· .minecraft ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            File.Copy(officialPath, targetPath, true);
                        }
                        else
                        {
                            Log($"Р—Р°РіСЂСѓР·РєР° Р±РёР±Р»РёРѕС‚РµРєРё ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            byte[] libBytes = SafeGetByteArrayAsync(url);
                            File.WriteAllBytes(targetPath, libBytes);
                        }
                    }

                    double progress = 40.0 + ((double)idx / libCount) * 15.0;
                    SetStatus("РЎРєР°С‡РёРІР°РЅРёРµ Р±РёР±Р»РёРѕС‚РµРє Mojang...", progress);
                }

                // Download Asset Index JSON
                var assetIndex = versionDoc.RootElement.GetProperty("assetIndex");
                string assetIndexId = assetIndex.GetProperty("id").GetString();
                string assetIndexUrl = assetIndex.GetProperty("url").GetString();
                string assetIndexTarget = Path.Combine(gameDir, "assets", "indexes", $"{assetIndexId}.json");
                
                if (!File.Exists(assetIndexTarget))
                {
                    Directory.CreateDirectory(Path.GetDirectoryName(assetIndexTarget));
                    Log($"Р—Р°РіСЂСѓР·РєР° РёРЅРґРµРєСЃР° СЂРµСЃСѓСЂСЃРѕРІ: {assetIndexId}.json");
                    string assetsJson = SafeGetStringAsync(assetIndexUrl);
                    File.WriteAllText(assetIndexTarget, assetsJson);
                }

                // Copy existing objects from official .minecraft if possible to avoid huge assets downloads
                string officialAssetsDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "assets");
                if (Directory.Exists(officialAssetsDir))
                {
                    Log("РћР±РЅР°СЂСѓР¶РµРЅР° РїР°РїРєР° СЂРµСЃСѓСЂСЃРѕРІ РѕС„РёС†РёР°Р»СЊРЅРѕРіРѕ Minecraft, СЂРµСЃСѓСЂСЃС‹ Р±СѓРґСѓС‚ СЃР»РёРЅРєРѕРІР°РЅС‹.");
                }

                return true;
            }
            catch (Exception ex)
            {
                Log($"[РћРЁРР‘РљРђ] РџСЂРё Р·Р°РіСЂСѓР·РєРµ Mojang С„Р°Р№Р»РѕРІ: {ex.Message}");
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
                    Log($"[РЎР•РўР¬] РћС€РёР±РєР° СЃРєР°С‡РёРІР°РЅРёСЏ СЃ GitHub ({ex.Message}). РџРµСЂРµРєР»СЋС‡РµРЅРёРµ РЅР° Р·РµСЂРєР°Р»Рѕ...");
                }
                else if ((url.Contains("mojang.com") || url.Contains("fabricmc.net")) && !useMojangMirror)
                {
                    useMojangMirror = true;
                    activatedMirror = true;
                    Log($"[РЎР•РўР¬] РћС€РёР±РєР° СЃРєР°С‡РёРІР°РЅРёСЏ СЃ Mojang/Fabric ({ex.Message}). РџРµСЂРµРєР»СЋС‡РµРЅРёРµ РЅР° Р·РµСЂРєР°Р»Рѕ...");
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
                            SetStatus($"Р—Р°РіСЂСѓР·РєР° С„Р°Р№Р»Р° ({totalRead / 1024 / 1024}MB / {totalBytes.Value / 1024 / 1024}MB)...", progress);
                        }
                    }
                }
            }
        }

        private bool InstallFabric(string gameDir)
        {
            try
            {
                Log("Р—Р°РїСЂРѕСЃ РјРµС‚Р°РґР°РЅРЅС‹С… РїСЂРѕС„РёР»СЏ Fabric Loader...");
                string fabricProfileUrl = "https://meta.fabricmc.net/v2/versions/loader/1.21.4/0.16.14/profile/json";
                string profileJson = SafeGetStringAsync(fabricProfileUrl);

                string fabricVersionDir = Path.Combine(gameDir, "versions", "fabric-loader-0.16.14-1.21.4");
                Directory.CreateDirectory(fabricVersionDir);
                File.WriteAllText(Path.Combine(fabricVersionDir, "fabric-loader-0.16.14-1.21.4.json"), profileJson);

                using var profileDoc = JsonDocument.Parse(profileJson);
                var libraries = profileDoc.RootElement.GetProperty("libraries");
                int libCount = libraries.GetArrayLength();
                int idx = 0;
                Log($"Р—Р°РіСЂСѓР·РєР° Р±РёР±Р»РёРѕС‚РµРє Fabric ({libCount} С€С‚)...");

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
                            Log($"РљРѕРїРёСЂРѕРІР°РЅРёРµ Fabric Р»РёР±С‹ РёР· .minecraft ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            File.Copy(officialPath, targetPath, true);
                        }
                        else
                        {
                            Log($"Р—Р°РіСЂСѓР·РєР° Fabric Р»РёР±С‹ ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            byte[] libBytes = SafeGetByteArrayAsync(downloadUrl);
                            File.WriteAllBytes(targetPath, libBytes);
                        }
                    }

                    double progress = 65.0 + ((double)idx / libCount) * 15.0;
                    SetStatus("Р—Р°РіСЂСѓР·РєР° Р±РёР±Р»РёРѕС‚РµРє Fabric...", progress);
                }

                return true;
            }
            catch (Exception ex)
            {
                Log($"[РћРЁРР‘РљРђ] РџСЂРё СѓСЃС‚Р°РЅРѕРІРєРµ Fabric: {ex.Message}");
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
                    Log("Р—Р°РіСЂСѓР·РєР° Fabric API РёР· Maven...");
                    string fabricApiUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.119.4+1.21.4/fabric-api-0.119.4+1.21.4.jar";
                    DownloadFileWithProgress(fabricApiUrl, fabricApiTarget, 85, 90).GetAwaiter().GetResult();
                }
                else
                {
                    Log("Fabric API СѓР¶Рµ СѓСЃС‚Р°РЅРѕРІР»РµРЅ.");
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
                        Log($"РљРѕРїРёСЂРѕРІР°РЅРёРµ РјРѕРґР° SolutionVisual РёР· {Path.GetFileName(compiledModJar)}...");
                        File.Copy(compiledModJar, targetModJar, true);
                        Log("РњРѕРґ SolutionVisual СѓСЃРїРµС€РЅРѕ СЃРєРѕРїРёСЂРѕРІР°РЅ.");
                    }
                    else
                    {
                        Log($"[РџР Р•Р”РЈРџР Р•Р–Р”Р•РќРР•] РЎРєРѕРјРїРёР»РёСЂРѕРІР°РЅРЅС‹Р№ РјРѕРґ РЅРµ РЅР°Р№РґРµРЅ РїРѕ РїСѓС‚Рё: {compiledModJar}");
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
                        Log("РџСЂРѕРІРµСЂРєР° РѕР±РЅРѕРІР»РµРЅРёР№ РјРѕРґР° SolutionVisual...");
                        latestVersion = SafeGetStringAsync(remoteVersionUrl).Trim();
                        if (string.IsNullOrEmpty(currentModVersion) || currentModVersion != latestVersion || !File.Exists(targetModJar))
                        {
                            needDownload = true;
                            if (!string.IsNullOrEmpty(latestVersion))
                            {
                                Log($"Р”РѕСЃС‚СѓРїРЅР° РЅРѕРІР°СЏ РІРµСЂСЃРёСЏ РјРѕРґР°: {latestVersion} (СѓСЃС‚Р°РЅРѕРІР»РµРЅР°: {(string.IsNullOrEmpty(currentModVersion) ? "РЅРµС‚" : currentModVersion)})");
                            }
                        }
                        else
                        {
                            Log($"РњРѕРґ SolutionVisual СѓР¶Рµ РѕР±РЅРѕРІР»РµРЅ (РІРµСЂСЃРёСЏ {currentModVersion}).");
                        }
                    }
                    catch (Exception ex)
                    {
                        Log($"[РџР Р•Р”РЈРџР Р•Р–Р”Р•РќРР•] РќРµ СѓРґР°Р»РѕСЃСЊ РїСЂРѕРІРµСЂРёС‚СЊ РѕР±РЅРѕРІР»РµРЅРёСЏ РјРѕРґР°: {ex.Message}");
                        if (!File.Exists(targetModJar))
                        {
                            needDownload = true;
                            Log("Р›РѕРєР°Р»СЊРЅС‹Р№ С„Р°Р№Р» РјРѕРґР° РѕС‚СЃСѓС‚СЃС‚РІСѓРµС‚. Р‘СѓРґРµС‚ РІС‹РїРѕР»РЅРµРЅР° РїРѕРїС‹С‚РєР° Р·Р°РіСЂСѓР·РєРё...");
                        }
                    }
                    
                    if (needDownload)
                    {
                        Log("Р—Р°РіСЂСѓР·РєР° РіРѕС‚РѕРІРѕРіРѕ РјРѕРґР° SolutionVisual СЃ СѓРґР°Р»РµРЅРЅРѕРіРѕ СЃРµСЂРІРµСЂР°...");
                        try
                        {
                            DownloadFileWithProgress(remoteModUrl, targetModJar, 90, 95).GetAwaiter().GetResult();
                            Log("РњРѕРґ SolutionVisual СѓСЃРїРµС€РЅРѕ Р·Р°РіСЂСѓР¶РµРЅ.");
                            if (!string.IsNullOrEmpty(latestVersion))
                            {
                                currentModVersion = latestVersion;
                                SaveConfig();
                            }
                        }
                        catch (Exception ex)
                        {
                            Log($"[РџР Р•Р”РЈРџР Р•Р–Р”Р•РќРР•] РќРµ СѓРґР°Р»РѕСЃСЊ СЃРєР°С‡Р°С‚СЊ РјРѕРґ РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ ({ex.Message}). Р‘СѓРґРµС‚ Р·Р°РїСѓС‰РµРЅ С‡РёСЃС‚С‹Р№ Fabric.");
                        }
                    }
                }

                return true;
            }
            catch (Exception ex)
            {
                Log($"[РћРЁРР‘РљРђ] РџСЂРё СѓСЃС‚Р°РЅРѕРІРєРµ РјРѕРґРѕРІ: {ex.Message}");
                return false;
            }
        }

        private void LaunchGame(string gameDir, string nickname, int ramGb)
        {
            try
            {
                Log("РџРѕРёСЃРє Java...");
                string javaPath = FindJavaExecutable(gameDir);
                if (string.IsNullOrEmpty(javaPath))
                {
                    Log("[РћРЁРР‘РљРђ] Java РЅРµ РЅР°Р№РґРµРЅР° РЅР° РІР°С€РµРј РєРѕРјРїСЊСЋС‚РµСЂРµ Рё РЅРµ СѓРґР°Р»РѕСЃСЊ СЃРєР°С‡Р°С‚СЊ РµС‘ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё.");
                    return;
                }

                if (!Path.IsPathRooted(javaPath))
                {
                    javaPath = ResolveFullPath(javaPath);
                }

                int majorVersion = GetJavaMajorVersion(javaPath);
                if (majorVersion > 0 && majorVersion < 21)
                {
                    Log($"[РћРЁРР‘РљРђ] РћР±РЅР°СЂСѓР¶РµРЅРЅР°СЏ РІРµСЂСЃРёСЏ Java ({majorVersion}) РЅРµ РїРѕРґС…РѕРґРёС‚. РўСЂРµР±СѓРµС‚СЃСЏ Java 21 РёР»Рё РЅРѕРІРµРµ.");
                    Dispatcher.Invoke(() =>
                    {
                        MessageBox.Show(
                            $"Р”Р»СЏ Р·Р°РїСѓСЃРєР° РёРіСЂС‹ С‚СЂРµР±СѓРµС‚СЃСЏ Java 21 РёР»Рё РІС‹С€Рµ. РћР±РЅР°СЂСѓР¶РµРЅРЅР°СЏ РІРµСЂСЃРёСЏ РЅР° РІР°С€РµРј РџРљ: {majorVersion} ({javaPath}).\n\nРџРѕР¶Р°Р»СѓР№СЃС‚Р°, СѓСЃС‚Р°РЅРѕРІРёС‚Рµ Java 21 РёР»Рё РїСЂРѕРІРµСЂСЊС‚Рµ РїРѕРґРєР»СЋС‡РµРЅРёРµ Рє РёРЅС‚РµСЂРЅРµС‚Сѓ, С‡С‚РѕР±С‹ Р»Р°СѓРЅС‡РµСЂ РјРѕРі СЃРєР°С‡Р°С‚СЊ РµС‘ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё.",
                            "РќРµСЃРѕРІРјРµСЃС‚РёРјР°СЏ РІРµСЂСЃРёСЏ Java",
                            MessageBoxButton.OK,
                            MessageBoxImage.Error
                        );
                    });
                    return;
                }

                Log($"РСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ Java: {javaPath}");

                // Classpath Construction
                Log("РџРѕСЃС‚СЂРѕРµРЅРёРµ classpath...");
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
                        Log($"[РџР Р•Р”РЈРџР Р•Р–Р”Р•РќРР•] РћС€РёР±РєР° РїСЂРё С‡С‚РµРЅРёРё Р±РёР±Р»РёРѕС‚РµРє 1.21.4.json: {ex.Message}");
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
                        Log($"[РџР Р•Р”РЈРџР Р•Р–Р”Р•РќРР•] РћС€РёР±РєР° РїСЂРё С‡С‚РµРЅРёРё Р±РёР±Р»РёРѕС‚РµРє Fabric json: {ex.Message}");
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
                        Log($"[РџР Р•Р”РЈРџР Р•Р–Р”Р•РќРР•] Р¤Р°Р№Р» Р±РёР±Р»РёРѕС‚РµРєРё РЅРµ РЅР°Р№РґРµРЅ: {Path.GetFileName(libPath)}");
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
                Log("Р—Р°РїРёСЃСЊ Р°СЂРіСѓРјРµРЅС‚РѕРІ Р·Р°РїСѓСЃРєР° РІ launch.args...");

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

                Log("Р—Р°РїСѓСЃРє РїСЂРѕС†РµСЃСЃР° Minecraft...");
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

                gameProcess = Process.Start(startInfo);
                if (gameProcess == null)
                {
                    Log("[РћРЁРР‘РљРђ] РќРµ СѓРґР°Р»РѕСЃСЊ Р·Р°РїСѓСЃС‚РёС‚СЊ РїСЂРѕС†РµСЃСЃ РёРіСЂС‹.");
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
                        Log($"[Minecraft РћРЁРР‘РљРђ] {e.Data}");
                        if (e.Data.Contains("insufficient memory") || e.Data.Contains("commit_memory") || e.Data.Contains("errno=1455") || e.Data.Contains("Р¤Р°Р№Р» РїРѕРґРєР°С‡РєРё СЃР»РёС€РєРѕРј РјР°Р»"))
                        {
                            memoryErrorDetected = true;
                        }
                    }
                };

                gameProcess.BeginOutputReadLine();
                gameProcess.BeginErrorReadLine();

                Log("РРіСЂР° Р·Р°РїСѓС‰РµРЅР°! Р’С‹РІРѕРґ РєРѕРЅСЃРѕР»Рё РїРµСЂРµРЅР°РїСЂР°РІР»РµРЅ СЃСЋРґР°.");
                SetStatus("РРіСЂР° Р·Р°РїСѓС‰РµРЅР°", 100);

                // Wait for exit in background task
                Task.Run(() =>
                {
                    gameProcess.WaitForExit();
                    Log($"[Solution Launcher] РџСЂРѕС†РµСЃСЃ РёРіСЂС‹ Р·Р°РІРµСЂС€РёР»СЃСЏ СЃ РєРѕРґРѕРј {gameProcess.ExitCode}");
                    SetStatus("Р“РѕС‚РѕРІ Рє Р·Р°РїСѓСЃРєСѓ", 0);
                    if (memoryErrorDetected)
                    {
                        Dispatcher.Invoke(() =>
                        {
                            int currentRam = (int)RamSlider.Value;
                            int newRam = Math.Max(2, currentRam - 1);
                            
                            if (newRam < currentRam)
                            {
                                RamSlider.Value = newRam;
                                RamValueText.Text = $"{newRam} Р“Р‘";
                                SaveConfig();
                                Log($"[РЎРРЎРўР•РњРђ] РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё СѓРјРµРЅСЊС€РµРЅРѕ РІС‹РґРµР»РµРЅРёРµ RAM СЃ {currentRam} Р“Р‘ РґРѕ {newRam} Р“Р‘ РёР·-Р·Р° РѕС€РёР±РєРё РїР°РјСЏС‚Рё.");
                                
                                MessageBox.Show(
                                    $"РќРµ СѓРґР°Р»РѕСЃСЊ Р·Р°РїСѓСЃС‚РёС‚СЊ РёРіСЂСѓ РёР·-Р·Р° РЅРµС…РІР°С‚РєРё РІРёСЂС‚СѓР°Р»СЊРЅРѕР№ РїР°РјСЏС‚Рё (С„Р°Р№Р»Р° РїРѕРґРєР°С‡РєРё) РЅР° РІР°С€РµРј РєРѕРјРїСЊСЋС‚РµСЂРµ.\n\n" +
                                    $"РњС‹ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё СѓРјРµРЅСЊС€РёР»Рё РІС‹РґРµР»РµРЅРёРµ РѕРїРµСЂР°С‚РёРІРЅРѕР№ РїР°РјСЏС‚Рё РІ РЅР°СЃС‚СЂРѕР№РєР°С… РґРѕ {newRam} Р“Р‘.\n\n" +
                                    "РџРѕР¶Р°Р»СѓР№СЃС‚Р°, РїРѕРїСЂРѕР±СѓР№С‚Рµ РЅР°Р¶Р°С‚СЊ РєРЅРѕРїРєСѓ 'РќР°С‡Р°С‚СЊ РёРіСЂСѓ' СЃРЅРѕРІР°.",
                                    "РќРµРґРѕСЃС‚Р°С‚РѕС‡РЅРѕ РІРёСЂС‚СѓР°Р»СЊРЅРѕР№ РїР°РјСЏС‚Рё",
                                    MessageBoxButton.OK,
                                    MessageBoxImage.Warning
                                );
                            }
                            else
                            {
                                MessageBox.Show(
                                    "РќРµ СѓРґР°Р»РѕСЃСЊ Р·Р°РїСѓСЃС‚РёС‚СЊ РёРіСЂСѓ РёР·-Р·Р° РЅРµС…РІР°С‚РєРё РІРёСЂС‚СѓР°Р»СЊРЅРѕР№ РїР°РјСЏС‚Рё (С„Р°Р№Р»Р° РїРѕРґРєР°С‡РєРё) РЅР° РІР°С€РµРј РєРѕРјРїСЊСЋС‚РµСЂРµ.\n\n" +
                                    "Р РµС€РµРЅРёСЏ:\n" +
                                    "1. Р—Р°РєСЂРѕР№С‚Рµ РІСЃРµ Р»РёС€РЅРёРµ РїСЂРѕРіСЂР°РјРјС‹ (Р±СЂР°СѓР·РµСЂС‹, Discord, РґСЂСѓРіРёРµ РёРіСЂС‹).\n" +
                                    "2. РЈРІРµР»РёС‡СЊС‚Рµ СЂР°Р·РјРµСЂ С„Р°Р№Р»Р° РїРѕРґРєР°С‡РєРё РІ РЅР°СЃС‚СЂРѕР№РєР°С… Windows (РёР»Рё РІРєР»СЋС‡РёС‚Рµ РµРіРѕ, РµСЃР»Рё РѕРЅ РѕС‚РєР»СЋС‡РµРЅ).",
                                    "РќРµРґРѕСЃС‚Р°С‚РѕС‡РЅРѕ РІРёСЂС‚СѓР°Р»СЊРЅРѕР№ РїР°РјСЏС‚Рё",
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
                Log($"[РћРЁРР‘РљРђ] Р’Рѕ РІСЂРµРјСЏ Р·Р°РїСѓСЃРєР° РїСЂРѕС†РµСЃСЃР°: {ex.Message}");
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
                Log("Java 21 РЅРµ РЅР°Р№РґРµРЅР°. РќР°С‡РёРЅР°РµРј Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєСѓСЋ Р·Р°РіСЂСѓР·РєСѓ РїРѕСЂС‚Р°С‚РёРІРЅРѕР№ Java 21 JRE...");
                string jreZipPath = Path.Combine(gameDir, "jre21.zip");
                string downloadUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse";

                SetStatus("Р—Р°РіСЂСѓР·РєР° Java 21...", 5);
                DownloadFileWithProgress(downloadUrl, jreZipPath, 5, 45).GetAwaiter().GetResult();

                Log("Р—Р°РіСЂСѓР·РєР° Р·Р°РІРµСЂС€РµРЅР°. Р Р°СЃРїР°РєРѕРІРєР° Java 21...");
                SetStatus("Р Р°СЃРїР°РєРѕРІРєР° Java 21...", 45);

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
                    Log("РџРѕСЂС‚Р°С‚РёРІРЅР°СЏ Java 21 СѓСЃРїРµС€РЅРѕ СѓСЃС‚Р°РЅРѕРІР»РµРЅР°.");
                    return javaws[0];
                }
            }
            catch (Exception ex)
            {
                Log($"[РћРЁРР‘РљРђ] РќРµ СѓРґР°Р»РѕСЃСЊ СЃРєР°С‡Р°С‚СЊ РїРѕСЂС‚Р°С‚РёРІРЅСѓСЋ Java 21: {ex.Message}");
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
                RamValueText.Text = $"{(int)RamSlider.Value} Р“Р‘";
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
                        RamValueText.Text = $"{(int)RamSlider.Value} Р“Р‘";
                        currentModVersion = config.ModVersion ?? "";
                        return;
                    }
                }
            }
            catch (Exception ex)
            {
                Log($"[РџР Р•Р”РЈРџР Р•Р–Р”Р•РќРР•] РќРµ СѓРґР°Р»РѕСЃСЊ Р·Р°РіСЂСѓР·РёС‚СЊ РЅР°СЃС‚СЂРѕР№РєРё: {ex.Message}");
            }
            NicknameInput.Text = "Player";
            int defaultRam = GetDefaultRamGb();
            RamSlider.Value = defaultRam;
            RamValueText.Text = $"{defaultRam} Р“Р‘";
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
                Log($"[РџР Р•Р”РЈРџР Р•Р–Р”Р•РќРР•] РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕС…СЂР°РЅРёС‚СЊ РЅР°СЃС‚СЂРѕР№РєРё: {ex.Message}");
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
                    
                    string errorMsg = "Р’Р°СЃ РЅРµС‚Сѓ РІ Р±Р°Р·Рµ РґР°РЅРЅС‹С… С‡С‚Рѕ Р±С‹ РІР°СЃ РґРѕР±Р°РІРёР»Рё РѕС‚РїРёС€РёС‚Рµ РІ С‚РёРєРµС‚ С‡С‚Рѕ Р±С‹ РІР°СЃ РґРѕР±Р°РІРёР»Рё Рё РІСЃС‚Р°РІСЊС‚Рµ СЃРѕРѕР±С‰РµРЅРёРµ РёР· Р±СѓС„РµСЂР° РѕР±РјРµРЅР° РєРѕС‚РѕСЂРѕРµ СЏРІР»СЏРµС‚СЃСЏ РІР°С€РёРј Hwid - РєР»СЋС‡РѕРј.\n\n" +
                                      $"Р’Р°С€ HWID-РєР»СЋС‡ (СѓР¶Рµ СЃРєРѕРїРёСЂРѕРІР°РЅ РІ Р±СѓС„РµСЂ РѕР±РјРµРЅР°):\n{hwid}";

                    MessageBox.Show(
                        errorMsg,
                        "Р”РѕСЃС‚СѓРї РѕРіСЂР°РЅРёС‡РµРЅ",
                        MessageBoxButton.OK,
                        MessageBoxImage.Error
                    );
                    
                    try
                    {
                        if (gameProcess != null && !gameProcess.HasExited)
                        {
                            gameProcess.Kill();
                        }
                    }
                    catch {}
                    Application.Current.Shutdown();
                });
                throw new UnauthorizedAccessException("HWID not authorized.");
            }
        }

        private void CheckLauncherUpdate()
        {
            Task.Run(() =>
            {
                string currentVersion = "3.6.4.10";
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
                            Log($"РќР°Р№РґРµРЅРѕ РѕР±РЅРѕРІР»РµРЅРёРµ Р»Р°СѓРЅС‡РµСЂР°: {latestVersion} (С‚РµРєСѓС‰Р°СЏ: {currentVersion}). РЎРєР°С‡РёРІР°РЅРёРµ...");

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

                            Log("РћР±РЅРѕРІР»РµРЅРёРµ Р»Р°СѓРЅС‡РµСЂР° СЃРєР°С‡Р°РЅРѕ. РЈСЃС‚Р°РЅРѕРІРєР° Рё РїРµСЂРµР·Р°РїСѓСЃРє...");

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
                    Log($"[РџР Р•Р”РЈРџР Р•Р–Р”Р•РќРР•] РќРµ СѓРґР°Р»РѕСЃСЊ РїСЂРѕРІРµСЂРёС‚СЊ РѕР±РЅРѕРІР»РµРЅРёСЏ Р»Р°СѓРЅС‡РµСЂР°: {ex.Message}");
                }
            });
        }

        protected override void OnClosed(EventArgs e)
        {
            base.OnClosed(e);
            try
            {
                if (gameProcess != null && !gameProcess.HasExited)
                {
                    gameProcess.Kill();
                }
            }
            catch {}
            Environment.Exit(0);
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
