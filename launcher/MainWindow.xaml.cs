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

        private string GetJsDelivrUrl(string rawUrl)
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

            if (cleanUrl.EndsWith(".exe", StringComparison.OrdinalIgnoreCase))
            {
                return cleanUrl.Replace("https://raw.githubusercontent.com/", "https://raw.gitmirror.com/");
            }

            string path = cleanUrl.Substring("https://raw.githubusercontent.com/".Length);
            var parts = path.Split(new[] { '/' }, 4);
            if (parts.Length >= 4)
            {
                string user = parts[0];
                string repo = parts[1];
                string branch = parts[2];
                string file = parts[3];
                // Append cache buster to guarantee bypass of CDN and local caching
                return $"https://cdn.jsdelivr.net/gh/{user}/{repo}@{branch}/{file}?t={DateTime.UtcNow.Ticks}";
            }
            return rawUrl;
        }

        private string SafeGetStringAsync(string url)
        {
            if (url.StartsWith("https://raw.githubusercontent.com/"))
            {
                // 1. Try GitHub API first
                try
                {
                    string apiUrl = GetGitHubApiUrl(url);
                    var request = new HttpRequestMessage(HttpMethod.Get, apiUrl);
                    request.Headers.Accept.ParseAdd("application/vnd.github.v3.raw");
                    request.Headers.CacheControl = new System.Net.Http.Headers.CacheControlHeaderValue { NoCache = true };
                    using (var response = httpClient.SendAsync(request).GetAwaiter().GetResult())
                    {
                        if (response.IsSuccessStatusCode)
                        {
                            return response.Content.ReadAsStringAsync().GetAwaiter().GetResult();
                        }
                    }
                }
                catch {}

                // 2. Try jsDelivr CDN second
                try
                {
                    string jsdelivrUrl = GetJsDelivrUrl(url);
                    var request = new HttpRequestMessage(HttpMethod.Get, jsdelivrUrl);
                    request.Headers.CacheControl = new System.Net.Http.Headers.CacheControlHeaderValue { NoCache = true };
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

            // 3. Fallback to ResolveUrl (which handles gitmirror or Mojang mirrors)
            string resolved = ResolveUrl(url);
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Get, resolved);
                request.Headers.CacheControl = new System.Net.Http.Headers.CacheControlHeaderValue { NoCache = true };
                using (var response = httpClient.SendAsync(request).GetAwaiter().GetResult())
                {
                    response.EnsureSuccessStatusCode();
                    return response.Content.ReadAsStringAsync().GetAwaiter().GetResult();
                }
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
                    var request = new HttpRequestMessage(HttpMethod.Get, retriedUrl);
                    request.Headers.CacheControl = new System.Net.Http.Headers.CacheControlHeaderValue { NoCache = true };
                    using (var response = httpClient.SendAsync(request).GetAwaiter().GetResult())
                    {
                        response.EnsureSuccessStatusCode();
                        return response.Content.ReadAsStringAsync().GetAwaiter().GetResult();
                    }
                }
                throw;
            }
        }

        private byte[] SafeGetByteArrayAsync(string url)
        {
            if (url.StartsWith("https://raw.githubusercontent.com/"))
            {
                // 1. Try GitHub API first
                try
                {
                    string apiUrl = GetGitHubApiUrl(url);
                    var request = new HttpRequestMessage(HttpMethod.Get, apiUrl);
                    request.Headers.Accept.ParseAdd("application/vnd.github.v3.raw");
                    request.Headers.CacheControl = new System.Net.Http.Headers.CacheControlHeaderValue { NoCache = true };
                    using (var response = httpClient.SendAsync(request).GetAwaiter().GetResult())
                    {
                        if (response.IsSuccessStatusCode)
                        {
                            return response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult();
                        }
                    }
                }
                catch {}

                // 2. Try jsDelivr CDN second
                try
                {
                    string jsdelivrUrl = GetJsDelivrUrl(url);
                    var request = new HttpRequestMessage(HttpMethod.Get, jsdelivrUrl);
                    request.Headers.CacheControl = new System.Net.Http.Headers.CacheControlHeaderValue { NoCache = true };
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

            string resolved = ResolveUrl(url);
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Get, resolved);
                request.Headers.CacheControl = new System.Net.Http.Headers.CacheControlHeaderValue { NoCache = true };
                using (var response = httpClient.SendAsync(request).GetAwaiter().GetResult())
                {
                    response.EnsureSuccessStatusCode();
                    return response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult();
                }
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
                    var request = new HttpRequestMessage(HttpMethod.Get, retriedUrl);
                    request.Headers.CacheControl = new System.Net.Http.Headers.CacheControlHeaderValue { NoCache = true };
                    using (var response = httpClient.SendAsync(request).GetAwaiter().GetResult())
                    {
                        response.EnsureSuccessStatusCode();
                        return response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult();
                    }
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
                MessageBox.Show("Р СџР С•Р В¶Р В°Р В»РЎС“Р в„–РЎРѓРЎвЂљР В°, Р Р†Р Р†Р ВµР Т‘Р С‘РЎвЂљР Вµ Р Р…Р С‘Р С”Р Р…Р ВµР в„–Р С Р С—Р ВµРЎР‚Р ВµР Т‘ Р Р…Р В°РЎвЂЎР В°Р В»Р С•Р С Р С‘Р С–РЎР‚РЎвЂ№.", "Р С›РЎв‚¬Р С‘Р В±Р С”Р В°", MessageBoxButton.OK, MessageBoxImage.Warning);
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
                Log($"[Р С›Р РЃР ВР вЂР С™Р С’] Р СџРЎР‚Р С•Р С‘Р В·Р С•РЎв‚¬Р ВµР В» РЎРѓР В±Р С•Р в„– Р С—РЎР‚Р С‘ Р В·Р В°Р С—РЎС“РЎРѓР С”Р Вµ: {ex.Message}");
                SetStatus("Р С›РЎв‚¬Р С‘Р В±Р С”Р В° Р В·Р В°Р С—РЎС“РЎРѓР С”Р В°", 0);
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
            Log("Р СњР В°РЎвЂЎР В°Р В»Р С• Р С—РЎР‚Р С•РЎвЂ Р ВµРЎРѓРЎРѓР В° Р С—Р С•Р Т‘Р С–Р С•РЎвЂљР С•Р Р†Р С”Р С‘ Р С‘Р С–РЎР‚РЎвЂ№...");

            string gameDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".solutionvisuals");
            string targetModJar = Path.Combine(gameDir, "mods", "SolutionVisual.jar");
            if (IsFileLocked(targetModJar))
            {
                Log("[Р С›Р РЃР ВР вЂР С™Р С’] Р С›Р В±Р Р…Р В°РЎР‚РЎС“Р В¶Р ВµР Р…Р В° Р В·Р В°Р С—РЎС“РЎвЂ°Р ВµР Р…Р Р…Р В°РЎРЏ Р С”Р С•Р С—Р С‘РЎРЏ Р С‘Р С–РЎР‚РЎвЂ№ (РЎвЂћР В°Р в„–Р В» SolutionVisual.jar Р В·Р В°Р В±Р В»Р С•Р С”Р С‘РЎР‚Р С•Р Р†Р В°Р Р…).");
                Dispatcher.Invoke(() =>
                {
                    MessageBox.Show(
                        "Р ВР С–РЎР‚Р В° РЎС“Р В¶Р Вµ Р В·Р В°Р С—РЎС“РЎвЂ°Р ВµР Р…Р В° Р С‘Р В»Р С‘ Р ВµРЎвЂ Р С—РЎР‚Р С•РЎвЂ Р ВµРЎРѓРЎРѓ Р В·Р В°Р Р†Р С‘РЎРѓ Р Р† РЎвЂћР С•Р Р…Р С•Р Р†Р С•Р С РЎР‚Р ВµР В¶Р С‘Р СР Вµ.\n\nР СџР С•Р В¶Р В°Р В»РЎС“Р в„–РЎРѓРЎвЂљР В°, Р В·Р В°Р С”РЎР‚Р С•Р в„–РЎвЂљР Вµ Р В·Р В°Р С—РЎС“РЎвЂ°Р ВµР Р…Р Р…РЎС“РЎР‹ Р С”Р С•Р С—Р С‘РЎР‹ Minecraft Р С—Р ВµРЎР‚Р ВµР Т‘ Р С—Р С•Р Р†РЎвЂљР С•РЎР‚Р Р…РЎвЂ№Р С Р В·Р В°Р С—РЎС“РЎРѓР С”Р С•Р С.",
                        "Р ВР С–РЎР‚Р В° РЎС“Р В¶Р Вµ Р В·Р В°Р С—РЎС“РЎвЂ°Р ВµР Р…Р В°",
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
                Log($"[Р В Р вЂўР вЂ“Р ВР Сљ Р В Р С’Р вЂ”Р В Р С’Р вЂР С›Р СћР В§Р ВР С™Р С’] Р С™Р С•РЎР‚Р Р…Р ВµР Р†Р В°РЎРЏ Р С—Р В°Р С—Р С”Р В° Р С—РЎР‚Р С•Р ВµР С”РЎвЂљР В°: {rootDir}");
                SetStatus("Р РЋР В±Р С•РЎР‚Р С”Р В° Р Р…Р В°РЎв‚¬Р ВµР С–Р С• Р СР С•Р Т‘Р В°...", 5);
                // 1. Build the mod using gradlew remapJar
                if (!BuildMod(rootDir!))
                {
                    Log("[Р С›Р РЃР ВР вЂР С™Р С’] Р РЋР В±Р С•РЎР‚Р С”Р В° Р СР С•Р Т‘Р В° Р В·Р В°Р Р†Р ВµРЎР‚РЎв‚¬Р С‘Р В»Р В°РЎРѓРЎРЉ Р Р…Р ВµРЎС“Р Т‘Р В°РЎвЂЎР Р…Р С•.");
                    return;
                }
            }
            else
            {
                Log("[Р С’Р вЂ™Р СћР С›Р СњР С›Р СљР СњР В«Р в„ў Р В Р вЂўР вЂ“Р ВР Сљ] Р вЂ”Р В°Р С—РЎС“РЎвЂ°Р ВµР Р… Р В±Р ВµР В· Р С‘РЎРѓРЎвЂ¦Р С•Р Т‘Р Р…Р С•Р С–Р С• Р С”Р С•Р Т‘Р В°. Р СљР С•Р Т‘ Р В±РЎС“Р Т‘Р ВµРЎвЂљ Р В·Р В°Р С–РЎР‚РЎС“Р В¶Р ВµР Р… Р В°Р Р†РЎвЂљР С•Р СР В°РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”Р С‘.");
            }

            SetStatus("Р СџР С•Р Т‘Р С–Р С•РЎвЂљР С•Р Р†Р С”Р В° Р Т‘Р С‘РЎР‚Р ВµР С”РЎвЂљР С•РЎР‚Р С‘Р С‘ Р С‘Р С–РЎР‚РЎвЂ№...", 15);
            gameDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".solutionvisuals");
            Directory.CreateDirectory(gameDir);
            Directory.CreateDirectory(Path.Combine(gameDir, "versions"));
            Directory.CreateDirectory(Path.Combine(gameDir, "libraries"));
            Directory.CreateDirectory(Path.Combine(gameDir, "mods"));

            // 2. Download Minecraft 1.21.4 client and libraries
            SetStatus("Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Р СР ВµРЎвЂљР В°Р Т‘Р В°Р Р…Р Р…РЎвЂ№РЎвЂ¦ Р С‘Р С–РЎР‚РЎвЂ№...", 20);
            if (!DownloadMinecraftAndLibraries(gameDir))
            {
                Log("[Р С›Р РЃР ВР вЂР С™Р С’] Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° РЎвЂћР В°Р в„–Р В»Р С•Р Р† Minecraft Р В·Р В°Р Р†Р ВµРЎР‚РЎв‚¬Р С‘Р В»Р В°РЎРѓРЎРЉ Р Р…Р ВµРЎС“Р Т‘Р В°РЎвЂЎР Р…Р С•.");
                return;
            }

            // 3. Download Fabric Loader
            SetStatus("Р Р€РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р С”Р В° Fabric...", 65);
            if (!InstallFabric(gameDir))
            {
                Log("[Р С›Р РЃР ВР вЂР С™Р С’] Р Р€РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р С”Р В° Fabric Р В·Р В°Р Р†Р ВµРЎР‚РЎв‚¬Р С‘Р В»Р В°РЎРѓРЎРЉ Р Р…Р ВµРЎС“Р Т‘Р В°РЎвЂЎР Р…Р С•.");
                return;
            }

            // 4. Download Fabric API and copy/download SolutionVisual mod
            SetStatus("Р Р€РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р С”Р В° Р СР С•Р Т‘Р С•Р Р†...", 85);
            if (!InstallMods(isDevMode, rootDir, gameDir))
            {
                Log("[Р С›Р РЃР ВР вЂР С™Р С’] Р Р€РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р С”Р В° Р СР С•Р Т‘Р С•Р Р† Р В·Р В°Р Р†Р ВµРЎР‚РЎв‚¬Р С‘Р В»Р В°РЎРѓРЎРЉ Р Р…Р ВµРЎС“Р Т‘Р В°РЎвЂЎР Р…Р С•.");
                return;
            }

            // 5. Launch the game
            SetStatus("Р вЂ”Р В°Р С—РЎС“РЎРѓР С” Р С‘Р С–РЎР‚РЎвЂ№...", 95);
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
            Log("Р вЂ”Р В°Р С—РЎС“РЎРѓР С” РЎРѓР В±Р С•РЎР‚Р С”Р С‘ Р СР С•Р Т‘Р В° SolutionVisual...");
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
                process.ErrorDataReceived += (s, e) => { if (e.Data != null) Log($"[Gradle Р С›Р РЃР ВР вЂР С™Р С’] {e.Data}"); };
                
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
                Log("Р вЂ”Р В°Р С—РЎР‚Р С•РЎРѓ Р СР В°Р Р…Р С‘РЎвЂћР ВµРЎРѓРЎвЂљР В° Р Р†Р ВµРЎР‚РЎРѓР С‘Р в„– Mojang...");
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
                    Log("Р вЂ™Р ВµРЎР‚РЎРѓР С‘РЎРЏ 1.21.4 Р Р…Р Вµ Р Р…Р В°Р в„–Р Т‘Р ВµР Р…Р В° Р Р† Р СР В°Р Р…Р С‘РЎвЂћР ВµРЎРѓРЎвЂљР Вµ Mojang.");
                    return false;
                }

                Log("Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Р С—РЎР‚Р С•РЎвЂћР С‘Р В»РЎРЏ Р Р†Р ВµРЎР‚РЎРѓР С‘Р С‘ 1.21.4...");
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
                    Log($"Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° client.jar (1.21.4) Р С‘Р В· Mojang...");
                    DownloadFileWithProgress(clientUrl, clientJarPath, 20, 40).GetAwaiter().GetResult();
                }
                else
                {
                    Log("Minecraft client.jar (1.21.4) РЎС“Р В¶Р Вµ РЎРѓРЎС“РЎвЂ°Р ВµРЎРѓРЎвЂљР Р†РЎС“Р ВµРЎвЂљ.");
                }

                // Download Libraries
                var libraries = versionDoc.RootElement.GetProperty("libraries");
                int libCount = libraries.GetArrayLength();
                int idx = 0;
                Log($"Р СџРЎР‚Р С•Р Р†Р ВµРЎР‚Р С”Р В° Р С‘ РЎРѓР С”Р В°РЎвЂЎР С‘Р Р†Р В°Р Р…Р С‘Р Вµ Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С” Mojang ({libCount} РЎв‚¬РЎвЂљ)...");
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
                            Log($"Р С™Р С•Р С—Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘Р Вµ Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С”Р С‘ Р С‘Р В· .minecraft ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            File.Copy(officialPath, targetPath, true);
                        }
                        else
                        {
                            Log($"Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С”Р С‘ ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            byte[] libBytes = SafeGetByteArrayAsync(url);
                            File.WriteAllBytes(targetPath, libBytes);
                        }
                    }

                    double progress = 40.0 + ((double)idx / libCount) * 15.0;
                    SetStatus("Р РЋР С”Р В°РЎвЂЎР С‘Р Р†Р В°Р Р…Р С‘Р Вµ Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С” Mojang...", progress);
                }

                // Download Asset Index JSON
                var assetIndex = versionDoc.RootElement.GetProperty("assetIndex");
                string assetIndexId = assetIndex.GetProperty("id").GetString();
                string assetIndexUrl = assetIndex.GetProperty("url").GetString();
                string assetIndexTarget = Path.Combine(gameDir, "assets", "indexes", $"{assetIndexId}.json");
                
                if (!File.Exists(assetIndexTarget))
                {
                    Directory.CreateDirectory(Path.GetDirectoryName(assetIndexTarget));
                    Log($"Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Р С‘Р Р…Р Т‘Р ВµР С”РЎРѓР В° РЎР‚Р ВµРЎРѓРЎС“РЎР‚РЎРѓР С•Р Р†: {assetIndexId}.json");
                    string assetsJson = SafeGetStringAsync(assetIndexUrl);
                    File.WriteAllText(assetIndexTarget, assetsJson);
                }

                // Copy existing objects from official .minecraft if possible to avoid huge assets downloads
                string officialAssetsDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "assets");
                if (Directory.Exists(officialAssetsDir))
                {
                    Log("Р С›Р В±Р Р…Р В°РЎР‚РЎС“Р В¶Р ВµР Р…Р В° Р С—Р В°Р С—Р С”Р В° РЎР‚Р ВµРЎРѓРЎС“РЎР‚РЎРѓР С•Р Р† Р С•РЎвЂћР С‘РЎвЂ Р С‘Р В°Р В»РЎРЉР Р…Р С•Р С–Р С• Minecraft, РЎР‚Р ВµРЎРѓРЎС“РЎР‚РЎРѓРЎвЂ№ Р В±РЎС“Р Т‘РЎС“РЎвЂљ РЎРѓР В»Р С‘Р Р…Р С”Р С•Р Р†Р В°Р Р…РЎвЂ№.");
                }

                return true;
            }
            catch (Exception ex)
            {
                Log($"[Р С›Р РЃР ВР вЂР С™Р С’] Р СџРЎР‚Р С‘ Р В·Р В°Р С–РЎР‚РЎС“Р В·Р С”Р Вµ Mojang РЎвЂћР В°Р в„–Р В»Р С•Р Р†: {ex.Message}");
                return false;
            }
        }

        private async Task DownloadFileWithProgress(string url, string targetPath, double startPct, double endPct)
        {
            if (url.StartsWith("https://raw.githubusercontent.com/"))
            {
                // 1. Try GitHub API first
                try
                {
                    string apiUrl = GetGitHubApiUrl(url);
                    await DownloadFileWithProgressInternal(apiUrl, targetPath, startPct, endPct);
                    return;
                }
                catch {}

                // 2. Try jsDelivr CDN second
                try
                {
                    string jsdelivrUrl = GetJsDelivrUrl(url);
                    await DownloadFileWithProgressInternal(jsdelivrUrl, targetPath, startPct, endPct);
                    return;
                }
                catch {}
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
            request.Headers.CacheControl = new System.Net.Http.Headers.CacheControlHeaderValue { NoCache = true };

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
                Log("Р вЂ”Р В°Р С—РЎР‚Р С•РЎРѓ Р СР ВµРЎвЂљР В°Р Т‘Р В°Р Р…Р Р…РЎвЂ№РЎвЂ¦ Р С—РЎР‚Р С•РЎвЂћР С‘Р В»РЎРЏ Fabric Loader...");
                string fabricProfileUrl = "https://meta.fabricmc.net/v2/versions/loader/1.21.4/0.16.14/profile/json";
                string profileJson = SafeGetStringAsync(fabricProfileUrl);

                string fabricVersionDir = Path.Combine(gameDir, "versions", "fabric-loader-0.16.14-1.21.4");
                Directory.CreateDirectory(fabricVersionDir);
                File.WriteAllText(Path.Combine(fabricVersionDir, "fabric-loader-0.16.14-1.21.4.json"), profileJson);

                using var profileDoc = JsonDocument.Parse(profileJson);
                var libraries = profileDoc.RootElement.GetProperty("libraries");
                int libCount = libraries.GetArrayLength();
                int idx = 0;
                Log($"Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С” Fabric ({libCount} РЎв‚¬РЎвЂљ)...");

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
                            Log($"Р С™Р С•Р С—Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘Р Вµ Fabric Р В»Р С‘Р В±РЎвЂ№ Р С‘Р В· .minecraft ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            File.Copy(officialPath, targetPath, true);
                        }
                        else
                        {
                            Log($"Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Fabric Р В»Р С‘Р В±РЎвЂ№ ({idx}/{libCount}): {Path.GetFileName(targetPath)}");
                            byte[] libBytes = SafeGetByteArrayAsync(downloadUrl);
                            File.WriteAllBytes(targetPath, libBytes);
                        }
                    }

                    double progress = 65.0 + ((double)idx / libCount) * 15.0;
                    SetStatus("Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С” Fabric...", progress);
                }

                return true;
            }
            catch (Exception ex)
            {
                Log($"[Р С›Р РЃР ВР вЂР С™Р С’] Р СџРЎР‚Р С‘ РЎС“РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р С”Р Вµ Fabric: {ex.Message}");
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
                    Log("Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Fabric API Р С‘Р В· Maven...");
                    string fabricApiUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.119.4+1.21.4/fabric-api-0.119.4+1.21.4.jar";
                    DownloadFileWithProgress(fabricApiUrl, fabricApiTarget, 85, 90).GetAwaiter().GetResult();
                }
                else
                {
                    Log("Fabric API РЎС“Р В¶Р Вµ РЎС“РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р В»Р ВµР Р….");
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
                        Log($"Р С™Р С•Р С—Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘Р Вµ Р СР С•Р Т‘Р В° SolutionVisual Р С‘Р В· {Path.GetFileName(compiledModJar)}...");
                        File.Copy(compiledModJar, targetModJar, true);
                        Log("Р СљР С•Р Т‘ SolutionVisual РЎС“РЎРѓР С—Р ВµРЎв‚¬Р Р…Р С• РЎРѓР С”Р С•Р С—Р С‘РЎР‚Р С•Р Р†Р В°Р Р….");
                    }
                    else
                    {
                        Log($"[Р СџР В Р вЂўР вЂќР Р€Р СџР В Р вЂўР вЂ“Р вЂќР вЂўР СњР ВР вЂў] Р РЋР С”Р С•Р СР С—Р С‘Р В»Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р Р…РЎвЂ№Р в„– Р СР С•Р Т‘ Р Р…Р Вµ Р Р…Р В°Р в„–Р Т‘Р ВµР Р… Р С—Р С• Р С—РЎС“РЎвЂљР С‘: {compiledModJar}");
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
                        Log("Р СџРЎР‚Р С•Р Р†Р ВµРЎР‚Р С”Р В° Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘Р в„– Р СР С•Р Т‘Р В° SolutionVisual...");
                        latestVersion = SafeGetStringAsync(remoteVersionUrl).Trim();
                        if (string.IsNullOrEmpty(currentModVersion) || currentModVersion != latestVersion || !File.Exists(targetModJar))
                        {
                            needDownload = true;
                            if (!string.IsNullOrEmpty(latestVersion))
                            {
                                Log($"Р вЂќР С•РЎРѓРЎвЂљРЎС“Р С—Р Р…Р В° Р Р…Р С•Р Р†Р В°РЎРЏ Р Р†Р ВµРЎР‚РЎРѓР С‘РЎРЏ Р СР С•Р Т‘Р В°: {latestVersion} (РЎС“РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р В»Р ВµР Р…Р В°: {(string.IsNullOrEmpty(currentModVersion) ? "Р Р…Р ВµРЎвЂљ" : currentModVersion)})");
                            }
                        }
                        else
                        {
                            Log($"Р СљР С•Р Т‘ SolutionVisual РЎС“Р В¶Р Вµ Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р… (Р Р†Р ВµРЎР‚РЎРѓР С‘РЎРЏ {currentModVersion}).");
                        }
                    }
                    catch (Exception ex)
                    {
                        Log($"[Р СџР В Р вЂўР вЂќР Р€Р СџР В Р вЂўР вЂ“Р вЂќР вЂўР СњР ВР вЂў] Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ Р С—РЎР‚Р С•Р Р†Р ВµРЎР‚Р С‘РЎвЂљРЎРЉ Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘РЎРЏ Р СР С•Р Т‘Р В°: {ex.Message}");
                        if (!File.Exists(targetModJar))
                        {
                            needDownload = true;
                            Log("Р вЂєР С•Р С”Р В°Р В»РЎРЉР Р…РЎвЂ№Р в„– РЎвЂћР В°Р в„–Р В» Р СР С•Р Т‘Р В° Р С•РЎвЂљРЎРѓРЎС“РЎвЂљРЎРѓРЎвЂљР Р†РЎС“Р ВµРЎвЂљ. Р вЂРЎС“Р Т‘Р ВµРЎвЂљ Р Р†РЎвЂ№Р С—Р С•Р В»Р Р…Р ВµР Р…Р В° Р С—Р С•Р С—РЎвЂ№РЎвЂљР С”Р В° Р В·Р В°Р С–РЎР‚РЎС“Р В·Р С”Р С‘...");
                        }
                    }
                    
                    if (needDownload)
                    {
                        Log("Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Р С–Р С•РЎвЂљР С•Р Р†Р С•Р С–Р С• Р СР С•Р Т‘Р В° SolutionVisual РЎРѓ РЎС“Р Т‘Р В°Р В»Р ВµР Р…Р Р…Р С•Р С–Р С• РЎРѓР ВµРЎР‚Р Р†Р ВµРЎР‚Р В°...");
                        try
                        {
                            DownloadFileWithProgress(remoteModUrl, targetModJar, 90, 95).GetAwaiter().GetResult();
                            Log("Р СљР С•Р Т‘ SolutionVisual РЎС“РЎРѓР С—Р ВµРЎв‚¬Р Р…Р С• Р В·Р В°Р С–РЎР‚РЎС“Р В¶Р ВµР Р….");
                            if (!string.IsNullOrEmpty(latestVersion))
                            {
                                currentModVersion = latestVersion;
                                SaveConfig();
                            }
                        }
                        catch (Exception ex)
                        {
                            Log($"[Р СџР В Р вЂўР вЂќР Р€Р СџР В Р вЂўР вЂ“Р вЂќР вЂўР СњР ВР вЂў] Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ РЎРѓР С”Р В°РЎвЂЎР В°РЎвЂљРЎРЉ Р СР С•Р Т‘ Р С—Р С• РЎС“Р СР С•Р В»РЎвЂЎР В°Р Р…Р С‘РЎР‹ ({ex.Message}). Р вЂРЎС“Р Т‘Р ВµРЎвЂљ Р В·Р В°Р С—РЎС“РЎвЂ°Р ВµР Р… РЎвЂЎР С‘РЎРѓРЎвЂљРЎвЂ№Р в„– Fabric.");
                        }
                    }
                }

                return true;
            }
            catch (Exception ex)
            {
                Log($"[Р С›Р РЃР ВР вЂР С™Р С’] Р СџРЎР‚Р С‘ РЎС“РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р С”Р Вµ Р СР С•Р Т‘Р С•Р Р†: {ex.Message}");
                return false;
            }
        }

        private void LaunchGame(string gameDir, string nickname, int ramGb)
        {
            try
            {
                Log("Р СџР С•Р С‘РЎРѓР С” Java...");
                string javaPath = FindJavaExecutable(gameDir);
                if (string.IsNullOrEmpty(javaPath))
                {
                    Log("[Р С›Р РЃР ВР вЂР С™Р С’] Java Р Р…Р Вµ Р Р…Р В°Р в„–Р Т‘Р ВµР Р…Р В° Р Р…Р В° Р Р†Р В°РЎв‚¬Р ВµР С Р С”Р С•Р СР С—РЎРЉРЎР‹РЎвЂљР ВµРЎР‚Р Вµ Р С‘ Р Р…Р Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ РЎРѓР С”Р В°РЎвЂЎР В°РЎвЂљРЎРЉ Р ВµРЎвЂ Р В°Р Р†РЎвЂљР С•Р СР В°РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”Р С‘.");
                    return;
                }

                if (!Path.IsPathRooted(javaPath))
                {
                    javaPath = ResolveFullPath(javaPath);
                }

                int majorVersion = GetJavaMajorVersion(javaPath);
                if (majorVersion > 0 && majorVersion < 21)
                {
                    Log($"[Р С›Р РЃР ВР вЂР С™Р С’] Р С›Р В±Р Р…Р В°РЎР‚РЎС“Р В¶Р ВµР Р…Р Р…Р В°РЎРЏ Р Р†Р ВµРЎР‚РЎРѓР С‘РЎРЏ Java ({majorVersion}) Р Р…Р Вµ Р С—Р С•Р Т‘РЎвЂ¦Р С•Р Т‘Р С‘РЎвЂљ. Р СћРЎР‚Р ВµР В±РЎС“Р ВµРЎвЂљРЎРѓРЎРЏ Java 21 Р С‘Р В»Р С‘ Р Р…Р С•Р Р†Р ВµР Вµ.");
                    Dispatcher.Invoke(() =>
                    {
                        MessageBox.Show(
                            $"Р вЂќР В»РЎРЏ Р В·Р В°Р С—РЎС“РЎРѓР С”Р В° Р С‘Р С–РЎР‚РЎвЂ№ РЎвЂљРЎР‚Р ВµР В±РЎС“Р ВµРЎвЂљРЎРѓРЎРЏ Java 21 Р С‘Р В»Р С‘ Р Р†РЎвЂ№РЎв‚¬Р Вµ. Р С›Р В±Р Р…Р В°РЎР‚РЎС“Р В¶Р ВµР Р…Р Р…Р В°РЎРЏ Р Р†Р ВµРЎР‚РЎРѓР С‘РЎРЏ Р Р…Р В° Р Р†Р В°РЎв‚¬Р ВµР С Р СџР С™: {majorVersion} ({javaPath}).\n\nР СџР С•Р В¶Р В°Р В»РЎС“Р в„–РЎРѓРЎвЂљР В°, РЎС“РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р С‘РЎвЂљР Вµ Java 21 Р С‘Р В»Р С‘ Р С—РЎР‚Р С•Р Р†Р ВµРЎР‚РЎРЉРЎвЂљР Вµ Р С—Р С•Р Т‘Р С”Р В»РЎР‹РЎвЂЎР ВµР Р…Р С‘Р Вµ Р С” Р С‘Р Р…РЎвЂљР ВµРЎР‚Р Р…Р ВµРЎвЂљРЎС“, РЎвЂЎРЎвЂљР С•Р В±РЎвЂ№ Р В»Р В°РЎС“Р Р…РЎвЂЎР ВµРЎР‚ Р СР С•Р С– РЎРѓР С”Р В°РЎвЂЎР В°РЎвЂљРЎРЉ Р ВµРЎвЂ Р В°Р Р†РЎвЂљР С•Р СР В°РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”Р С‘.",
                            "Р СњР ВµРЎРѓР С•Р Р†Р СР ВµРЎРѓРЎвЂљР С‘Р СР В°РЎРЏ Р Р†Р ВµРЎР‚РЎРѓР С‘РЎРЏ Java",
                            MessageBoxButton.OK,
                            MessageBoxImage.Error
                        );
                    });
                    return;
                }

                Log($"Р ВРЎРѓР С—Р С•Р В»РЎРЉР В·РЎС“Р ВµРЎвЂљРЎРѓРЎРЏ Java: {javaPath}");

                // Classpath Construction
                Log("Р СџР С•РЎРѓРЎвЂљРЎР‚Р С•Р ВµР Р…Р С‘Р Вµ classpath...");
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
                        Log($"[Р СџР В Р вЂўР вЂќР Р€Р СџР В Р вЂўР вЂ“Р вЂќР вЂўР СњР ВР вЂў] Р С›РЎв‚¬Р С‘Р В±Р С”Р В° Р С—РЎР‚Р С‘ РЎвЂЎРЎвЂљР ВµР Р…Р С‘Р С‘ Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С” 1.21.4.json: {ex.Message}");
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
                        Log($"[Р СџР В Р вЂўР вЂќР Р€Р СџР В Р вЂўР вЂ“Р вЂќР вЂўР СњР ВР вЂў] Р С›РЎв‚¬Р С‘Р В±Р С”Р В° Р С—РЎР‚Р С‘ РЎвЂЎРЎвЂљР ВµР Р…Р С‘Р С‘ Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С” Fabric json: {ex.Message}");
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
                        Log($"[Р СџР В Р вЂўР вЂќР Р€Р СџР В Р вЂўР вЂ“Р вЂќР вЂўР СњР ВР вЂў] Р В¤Р В°Р в„–Р В» Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С”Р С‘ Р Р…Р Вµ Р Р…Р В°Р в„–Р Т‘Р ВµР Р…: {Path.GetFileName(libPath)}");
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
                Log("Р вЂ”Р В°Р С—Р С‘РЎРѓРЎРЉ Р В°РЎР‚Р С–РЎС“Р СР ВµР Р…РЎвЂљР С•Р Р† Р В·Р В°Р С—РЎС“РЎРѓР С”Р В° Р Р† launch.args...");

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

                Log("Р вЂ”Р В°Р С—РЎС“РЎРѓР С” Р С—РЎР‚Р С•РЎвЂ Р ВµРЎРѓРЎРѓР В° Minecraft...");
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
                    Log("[Р С›Р РЃР ВР вЂР С™Р С’] Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ Р В·Р В°Р С—РЎС“РЎРѓРЎвЂљР С‘РЎвЂљРЎРЉ Р С—РЎР‚Р С•РЎвЂ Р ВµРЎРѓРЎРѓ Р С‘Р С–РЎР‚РЎвЂ№.");
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
                        Log($"[Minecraft Р С›Р РЃР ВР вЂР С™Р С’] {e.Data}");
                        if (e.Data.Contains("insufficient memory") || e.Data.Contains("commit_memory") || e.Data.Contains("errno=1455") || e.Data.Contains("Р В¤Р В°Р в„–Р В» Р С—Р С•Р Т‘Р С”Р В°РЎвЂЎР С”Р С‘ РЎРѓР В»Р С‘РЎв‚¬Р С”Р С•Р С Р СР В°Р В»"))
                        {
                            memoryErrorDetected = true;
                        }
                    }
                };

                gameProcess.BeginOutputReadLine();
                gameProcess.BeginErrorReadLine();

                Log("Р ВР С–РЎР‚Р В° Р В·Р В°Р С—РЎС“РЎвЂ°Р ВµР Р…Р В°! Р вЂ™РЎвЂ№Р Р†Р С•Р Т‘ Р С”Р С•Р Р…РЎРѓР С•Р В»Р С‘ Р С—Р ВµРЎР‚Р ВµР Р…Р В°Р С—РЎР‚Р В°Р Р†Р В»Р ВµР Р… РЎРѓРЎР‹Р Т‘Р В°.");
                SetStatus("Р ВР С–РЎР‚Р В° Р В·Р В°Р С—РЎС“РЎвЂ°Р ВµР Р…Р В°", 100);

                // Wait for exit in background task
                Task.Run(() =>
                {
                    gameProcess.WaitForExit();
                    Log($"[Solution Launcher] Р СџРЎР‚Р С•РЎвЂ Р ВµРЎРѓРЎРѓ Р С‘Р С–РЎР‚РЎвЂ№ Р В·Р В°Р Р†Р ВµРЎР‚РЎв‚¬Р С‘Р В»РЎРѓРЎРЏ РЎРѓ Р С”Р С•Р Т‘Р С•Р С {gameProcess.ExitCode}");
                    SetStatus("Р вЂњР С•РЎвЂљР С•Р Р† Р С” Р В·Р В°Р С—РЎС“РЎРѓР С”РЎС“", 0);
                    if (memoryErrorDetected)
                    {
                        Dispatcher.Invoke(() =>
                        {
                            int currentRam = (int)RamSlider.Value;
                            int newRam = Math.Max(2, currentRam - 1);
                            
                            if (newRam < currentRam)
                            {
                                RamSlider.Value = newRam;
                                RamValueText.Text = $"{newRam} Р вЂњР вЂ";
                                SaveConfig();
                                Log($"[Р РЋР ВР РЋР СћР вЂўР СљР С’] Р С’Р Р†РЎвЂљР С•Р СР В°РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”Р С‘ РЎС“Р СР ВµР Р…РЎРЉРЎв‚¬Р ВµР Р…Р С• Р Р†РЎвЂ№Р Т‘Р ВµР В»Р ВµР Р…Р С‘Р Вµ RAM РЎРѓ {currentRam} Р вЂњР вЂ Р Т‘Р С• {newRam} Р вЂњР вЂ Р С‘Р В·-Р В·Р В° Р С•РЎв‚¬Р С‘Р В±Р С”Р С‘ Р С—Р В°Р СРЎРЏРЎвЂљР С‘.");
                                
                                MessageBox.Show(
                                    $"Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ Р В·Р В°Р С—РЎС“РЎРѓРЎвЂљР С‘РЎвЂљРЎРЉ Р С‘Р С–РЎР‚РЎС“ Р С‘Р В·-Р В·Р В° Р Р…Р ВµРЎвЂ¦Р Р†Р В°РЎвЂљР С”Р С‘ Р Р†Р С‘РЎР‚РЎвЂљРЎС“Р В°Р В»РЎРЉР Р…Р С•Р в„– Р С—Р В°Р СРЎРЏРЎвЂљР С‘ (РЎвЂћР В°Р в„–Р В»Р В° Р С—Р С•Р Т‘Р С”Р В°РЎвЂЎР С”Р С‘) Р Р…Р В° Р Р†Р В°РЎв‚¬Р ВµР С Р С”Р С•Р СР С—РЎРЉРЎР‹РЎвЂљР ВµРЎР‚Р Вµ.\n\n" +
                                    $"Р СљРЎвЂ№ Р В°Р Р†РЎвЂљР С•Р СР В°РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”Р С‘ РЎС“Р СР ВµР Р…РЎРЉРЎв‚¬Р С‘Р В»Р С‘ Р Р†РЎвЂ№Р Т‘Р ВµР В»Р ВµР Р…Р С‘Р Вµ Р С•Р С—Р ВµРЎР‚Р В°РЎвЂљР С‘Р Р†Р Р…Р С•Р в„– Р С—Р В°Р СРЎРЏРЎвЂљР С‘ Р Р† Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р в„–Р С”Р В°РЎвЂ¦ Р Т‘Р С• {newRam} Р вЂњР вЂ.\n\n" +
                                    "Р СџР С•Р В¶Р В°Р В»РЎС“Р в„–РЎРѓРЎвЂљР В°, Р С—Р С•Р С—РЎР‚Р С•Р В±РЎС“Р в„–РЎвЂљР Вµ Р Р…Р В°Р В¶Р В°РЎвЂљРЎРЉ Р С”Р Р…Р С•Р С—Р С”РЎС“ 'Р СњР В°РЎвЂЎР В°РЎвЂљРЎРЉ Р С‘Р С–РЎР‚РЎС“' РЎРѓР Р…Р С•Р Р†Р В°.",
                                    "Р СњР ВµР Т‘Р С•РЎРѓРЎвЂљР В°РЎвЂљР С•РЎвЂЎР Р…Р С• Р Р†Р С‘РЎР‚РЎвЂљРЎС“Р В°Р В»РЎРЉР Р…Р С•Р в„– Р С—Р В°Р СРЎРЏРЎвЂљР С‘",
                                    MessageBoxButton.OK,
                                    MessageBoxImage.Warning
                                );
                            }
                            else
                            {
                                MessageBox.Show(
                                    "Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ Р В·Р В°Р С—РЎС“РЎРѓРЎвЂљР С‘РЎвЂљРЎРЉ Р С‘Р С–РЎР‚РЎС“ Р С‘Р В·-Р В·Р В° Р Р…Р ВµРЎвЂ¦Р Р†Р В°РЎвЂљР С”Р С‘ Р Р†Р С‘РЎР‚РЎвЂљРЎС“Р В°Р В»РЎРЉР Р…Р С•Р в„– Р С—Р В°Р СРЎРЏРЎвЂљР С‘ (РЎвЂћР В°Р в„–Р В»Р В° Р С—Р С•Р Т‘Р С”Р В°РЎвЂЎР С”Р С‘) Р Р…Р В° Р Р†Р В°РЎв‚¬Р ВµР С Р С”Р С•Р СР С—РЎРЉРЎР‹РЎвЂљР ВµРЎР‚Р Вµ.\n\n" +
                                    "Р В Р ВµРЎв‚¬Р ВµР Р…Р С‘РЎРЏ:\n" +
                                    "1. Р вЂ”Р В°Р С”РЎР‚Р С•Р в„–РЎвЂљР Вµ Р Р†РЎРѓР Вµ Р В»Р С‘РЎв‚¬Р Р…Р С‘Р Вµ Р С—РЎР‚Р С•Р С–РЎР‚Р В°Р СР СРЎвЂ№ (Р В±РЎР‚Р В°РЎС“Р В·Р ВµРЎР‚РЎвЂ№, Discord, Р Т‘РЎР‚РЎС“Р С–Р С‘Р Вµ Р С‘Р С–РЎР‚РЎвЂ№).\n" +
                                    "2. Р Р€Р Р†Р ВµР В»Р С‘РЎвЂЎРЎРЉРЎвЂљР Вµ РЎР‚Р В°Р В·Р СР ВµРЎР‚ РЎвЂћР В°Р в„–Р В»Р В° Р С—Р С•Р Т‘Р С”Р В°РЎвЂЎР С”Р С‘ Р Р† Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р в„–Р С”Р В°РЎвЂ¦ Windows (Р С‘Р В»Р С‘ Р Р†Р С”Р В»РЎР‹РЎвЂЎР С‘РЎвЂљР Вµ Р ВµР С–Р С•, Р ВµРЎРѓР В»Р С‘ Р С•Р Р… Р С•РЎвЂљР С”Р В»РЎР‹РЎвЂЎР ВµР Р…).",
                                    "Р СњР ВµР Т‘Р С•РЎРѓРЎвЂљР В°РЎвЂљР С•РЎвЂЎР Р…Р С• Р Р†Р С‘РЎР‚РЎвЂљРЎС“Р В°Р В»РЎРЉР Р…Р С•Р в„– Р С—Р В°Р СРЎРЏРЎвЂљР С‘",
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
                Log($"[Р С›Р РЃР ВР вЂР С™Р С’] Р вЂ™Р С• Р Р†РЎР‚Р ВµР СРЎРЏ Р В·Р В°Р С—РЎС“РЎРѓР С”Р В° Р С—РЎР‚Р С•РЎвЂ Р ВµРЎРѓРЎРѓР В°: {ex.Message}");
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
                Log("Java 21 Р Р…Р Вµ Р Р…Р В°Р в„–Р Т‘Р ВµР Р…Р В°. Р СњР В°РЎвЂЎР С‘Р Р…Р В°Р ВµР С Р В°Р Р†РЎвЂљР С•Р СР В°РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”РЎС“РЎР‹ Р В·Р В°Р С–РЎР‚РЎС“Р В·Р С”РЎС“ Р С—Р С•РЎР‚РЎвЂљР В°РЎвЂљР С‘Р Р†Р Р…Р С•Р в„– Java 21 JRE...");
                string jreZipPath = Path.Combine(gameDir, "jre21.zip");
                string downloadUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse";

                SetStatus("Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Java 21...", 5);
                DownloadFileWithProgress(downloadUrl, jreZipPath, 5, 45).GetAwaiter().GetResult();

                Log("Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В° Р В·Р В°Р Р†Р ВµРЎР‚РЎв‚¬Р ВµР Р…Р В°. Р В Р В°РЎРѓР С—Р В°Р С”Р С•Р Р†Р С”Р В° Java 21...");
                SetStatus("Р В Р В°РЎРѓР С—Р В°Р С”Р С•Р Р†Р С”Р В° Java 21...", 45);

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
                    Log("Р СџР С•РЎР‚РЎвЂљР В°РЎвЂљР С‘Р Р†Р Р…Р В°РЎРЏ Java 21 РЎС“РЎРѓР С—Р ВµРЎв‚¬Р Р…Р С• РЎС“РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р В»Р ВµР Р…Р В°.");
                    return javaws[0];
                }
            }
            catch (Exception ex)
            {
                Log($"[Р С›Р РЃР ВР вЂР С™Р С’] Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ РЎРѓР С”Р В°РЎвЂЎР В°РЎвЂљРЎРЉ Р С—Р С•РЎР‚РЎвЂљР В°РЎвЂљР С‘Р Р†Р Р…РЎС“РЎР‹ Java 21: {ex.Message}");
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
                RamValueText.Text = $"{(int)RamSlider.Value} Р вЂњР вЂ";
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
                        RamValueText.Text = $"{(int)RamSlider.Value} Р вЂњР вЂ";
                        currentModVersion = config.ModVersion ?? "";
                        return;
                    }
                }
            }
            catch (Exception ex)
            {
                Log($"[Р СџР В Р вЂўР вЂќР Р€Р СџР В Р вЂўР вЂ“Р вЂќР вЂўР СњР ВР вЂў] Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ Р В·Р В°Р С–РЎР‚РЎС“Р В·Р С‘РЎвЂљРЎРЉ Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р в„–Р С”Р С‘: {ex.Message}");
            }
            NicknameInput.Text = "Player";
            int defaultRam = GetDefaultRamGb();
            RamSlider.Value = defaultRam;
            RamValueText.Text = $"{defaultRam} Р вЂњР вЂ";
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
                Log($"[Р СџР В Р вЂўР вЂќР Р€Р СџР В Р вЂўР вЂ“Р вЂќР вЂўР СњР ВР вЂў] Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ РЎРѓР С•РЎвЂ¦РЎР‚Р В°Р Р…Р С‘РЎвЂљРЎРЉ Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р в„–Р С”Р С‘: {ex.Message}");
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
                string remoteData = SafeGetStringAsync(remoteUrl);
                foreach (var line in remoteData.Split(new[] { '\n', '\r' }, StringSplitOptions.RemoveEmptyEntries))
                {
                    string clean = line.Trim();
                    if (!string.IsNullOrEmpty(clean)) allowedHwids.Add(clean);
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
                    
                    string errorMsg = "Р вЂ™Р В°РЎРѓ Р Р…Р ВµРЎвЂљРЎС“ Р Р† Р В±Р В°Р В·Р Вµ Р Т‘Р В°Р Р…Р Р…РЎвЂ№РЎвЂ¦ РЎвЂЎРЎвЂљР С• Р В±РЎвЂ№ Р Р†Р В°РЎРѓ Р Т‘Р С•Р В±Р В°Р Р†Р С‘Р В»Р С‘ Р С•РЎвЂљР С—Р С‘РЎв‚¬Р С‘РЎвЂљР Вµ Р Р† РЎвЂљР С‘Р С”Р ВµРЎвЂљ РЎвЂЎРЎвЂљР С• Р В±РЎвЂ№ Р Р†Р В°РЎРѓ Р Т‘Р С•Р В±Р В°Р Р†Р С‘Р В»Р С‘ Р С‘ Р Р†РЎРѓРЎвЂљР В°Р Р†РЎРЉРЎвЂљР Вµ РЎРѓР С•Р С•Р В±РЎвЂ°Р ВµР Р…Р С‘Р Вµ Р С‘Р В· Р В±РЎС“РЎвЂћР ВµРЎР‚Р В° Р С•Р В±Р СР ВµР Р…Р В° Р С”Р С•РЎвЂљР С•РЎР‚Р С•Р Вµ РЎРЏР Р†Р В»РЎРЏР ВµРЎвЂљРЎРѓРЎРЏ Р Р†Р В°РЎв‚¬Р С‘Р С Hwid - Р С”Р В»РЎР‹РЎвЂЎР С•Р С.\n\n" +
                                      $"Р вЂ™Р В°РЎв‚¬ HWID-Р С”Р В»РЎР‹РЎвЂЎ (РЎС“Р В¶Р Вµ РЎРѓР С”Р С•Р С—Р С‘РЎР‚Р С•Р Р†Р В°Р Р… Р Р† Р В±РЎС“РЎвЂћР ВµРЎР‚ Р С•Р В±Р СР ВµР Р…Р В°):\n{hwid}";

                    MessageBox.Show(
                        errorMsg,
                        "Р вЂќР С•РЎРѓРЎвЂљРЎС“Р С— Р С•Р С–РЎР‚Р В°Р Р…Р С‘РЎвЂЎР ВµР Р…",
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
                string currentVersion = "3.6.4.13";
                string remoteVersionUrl = "https://raw.githubusercontent.com/iop21322132/solution-visuals/main/launcher_version.txt";
                string remoteExeUrl = "https://raw.githubusercontent.com/iop21322132/solution-visuals/main/SolutionLauncher.exe";

                try
                {
                    string latestVersion = SafeGetStringAsync(remoteVersionUrl).Trim();

                    if (!string.IsNullOrEmpty(latestVersion) && latestVersion != currentVersion)
                    {
                        Log($"РќР°Р№РґРµРЅРѕ РѕР±РЅРѕРІР»РµРЅРёРµ Р»Р°СѓРЅС‡РµСЂР°: {latestVersion} (С‚РµРєСѓС‰Р°СЏ: {currentVersion}). РЎРєР°С‡РёРІР°РЅРёРµ...");

                        string currentExePath = Process.GetCurrentProcess().MainModule?.FileName ?? "SolutionLauncher.exe";
                        string currentDir = Path.GetDirectoryName(currentExePath) ?? AppDomain.CurrentDomain.BaseDirectory;
                        string tempExePath = Path.Combine(currentDir, "SolutionLauncher.new");

                        // Download new exe using SafeGetByteArrayAsync which handles Github API, jsDelivr, etc.
                        byte[] newExeBytes = null;
                        try
                        {
                            newExeBytes = SafeGetByteArrayAsync(remoteExeUrl);
                        }
                        catch (Exception ex)
                        {
                            // Try alternative mirror options manually if SafeGetByteArrayAsync failed
                            var exeUrlsToTry = new List<string>
                            {
                                remoteExeUrl.Replace("https://raw.githubusercontent.com/", "https://raw.gitmirror.com/"),
                                "https://ghproxy.net/" + remoteExeUrl
                            };
                            foreach (var urlOption in exeUrlsToTry)
                            {
                                try
                                {
                                    newExeBytes = httpClient.GetByteArrayAsync(urlOption).GetAwaiter().GetResult();
                                    break;
                                }
                                catch {}
                            }
                            if (newExeBytes == null)
                            {
                                throw new Exception("Failed to download launcher update from all mirrors.", ex);
                            }
                        }
                            File.WriteAllBytes(tempExePath, newExeBytes);

                            Log("Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘Р Вµ Р В»Р В°РЎС“Р Р…РЎвЂЎР ВµРЎР‚Р В° РЎРѓР С”Р В°РЎвЂЎР В°Р Р…Р С•. Р Р€РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р С”Р В° Р С‘ Р С—Р ВµРЎР‚Р ВµР В·Р В°Р С—РЎС“РЎРѓР С”...");

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
                catch (Exception ex)
                {
                    Log($"[Р СџР В Р вЂўР вЂќР Р€Р СџР В Р вЂўР вЂ“Р вЂќР вЂўР СњР ВР вЂў] Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ Р С—РЎР‚Р С•Р Р†Р ВµРЎР‚Р С‘РЎвЂљРЎРЉ Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘РЎРЏ Р В»Р В°РЎС“Р Р…РЎвЂЎР ВµРЎР‚Р В°: {ex.Message}");
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
