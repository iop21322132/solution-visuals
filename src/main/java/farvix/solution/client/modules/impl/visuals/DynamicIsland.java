package farvix.solution.client.modules.impl.visuals;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.FontRenderer;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;import farvix.solution.api.util.color.FixColor;
import farvix.solution.api.util.render.Render2D;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import farvix.solution.mixins.accessors.IBossBarHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInfo(name = "Dynamic Island", category = ModuleCategory.VISUALS, 
        description = "Остров в стиле iPhone")
public class DynamicIsland extends Module implements QuickImports, farvix.solution.api.ui.hud.IHudElement {

    // ── Настройки ─────────────────────────────────────────────────────────────
    public final farvix.solution.api.settings.impl.BooleanSetting showTime =
            new farvix.solution.api.settings.impl.BooleanSetting("Время", this);
    public final farvix.solution.api.settings.impl.BooleanSetting showPing =
            new farvix.solution.api.settings.impl.BooleanSetting("Пинг", this);

    {
        showTime.setEnabled(true);
        showPing.setEnabled(true);
    }

    private static final Pattern PVP_TIMER_PATTERN = Pattern.compile("(\\d+)");
    
    // Animation values
    private float moduleAnimationProgress = 0f;
    private float pvpAnimationProgress = 0f;
    private float mediaAnimationProgress = 0f;

    // Expand panel animation
    private boolean expanded = false;
    private float expandProgress = 0f;
    private static final float EXPAND_SPEED = 3.5f; 
    private static final float EXPAND_PANEL_H = 70f;
    private long lastTrackPosition = 0L;
    private long lastTrackPollMs = 0L;

    // Click animation for the square button
    private float clickAnimProgress = 0f;
    private boolean clickAnimActive = false;
    private static final float CLICK_ANIM_SPEED = 2.0f;
    public float lastX, lastY, lastWidth, lastBaseHeight;
    
    // Module notification
    private String currentModuleNotification = "";
    private String currentModuleNotificationClean = "";
    private long moduleNotificationTime = 0;
    private final long MODULE_NOTIFICATION_DURATION = 2000;
    
    // Media player
    private String trackName = null;
    private String artistsText = null;
    private boolean isPlaying = false;
    private IMediaSession activeSession = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean polling = new AtomicBoolean(false);
    private volatile long lastPollMs = 0L;
    private final Identifier coverTextureLocation = Identifier.of("solution", "music_cover_di");
    private NativeImageBackedTexture coverTexture = null;
    private int coverHash = 0;
    
    // Кэшируем Color объекты для избежания аллокаций каждый кадр
    private int cachedWhiteText = -1;
    private int cachedGrayText = -1;
    private int cachedLightGrayText = -1;
    private int lastCachedTheme = 0;
    private int cachedGreenDot = -1;
    private int cachedRedDot = -1;
    private int cachedPlaceholderBg = -1;
    private int cachedPvpTimerBg = -1;
    private int cachedPvpTimerText = -1;
    private int lastCachedAlpha = -1;
    private long lastFrameTime = System.currentTimeMillis();

    public DynamicIsland() {
        // Module will be rendered via InGameHudMixin
    }

    public void showModuleNotification(String moduleName, boolean enabled) {
        currentModuleNotification = moduleName + " " + (enabled ? "§aEnabled" : "§cDisabled");
        currentModuleNotificationClean = moduleName + " " + (enabled ? "Enabled" : "Disabled");
        moduleNotificationTime = System.currentTimeMillis();
        moduleAnimationProgress = 0f;
    }

    public void tick() {
        if (mc.player == null || mc.world == null) return;
        
        long now = System.currentTimeMillis();
        if (now - lastPollMs < 200L) {
            return;
        }
        lastPollMs = now;
        
        if (!polling.compareAndSet(false, true)) {
            return;
        }
        
        executor.execute(() -> {
            try {
                IMediaSession session = MediaPlayerInfo.Instance.getMediaSessions().stream()
                        .max(Comparator.comparing(s -> s.getMedia().getPlaying()))
                        .orElse(null);
                
                if (session != null) {
                    MediaInfo info = session.getMedia();
                    if (info != null && !info.getTitle().isEmpty()) {
                        String newTrackName = info.getTitle();
                        String newArtistsText = (info.getArtist() != null && !info.getArtist().isEmpty())
                                ? info.getArtist() : null;
                        boolean newPlaying = info.getPlaying();
                        long newPosition = info.getPosition();
                        long newPollTime = System.currentTimeMillis();
                        byte[] newCover = info.getArtworkPng();
                        
                        int newCoverHash = 0;
                        NativeImage decodedImage = null;
                        if (newCover != null && newCover.length > 0) {
                            try {
                                newCoverHash = Arrays.hashCode(newCover);
                                decodedImage = NativeImage.read(new ByteArrayInputStream(newCover));
                            } catch (Exception ignored) {
                                decodedImage = null;
                                newCoverHash = 0;
                            }
                        }
                        
                        NativeImage finalDecodedImage = decodedImage;
                        int finalCoverHash = newCoverHash;
                        
                        mc.execute(() -> {
                            activeSession = session;
                            trackName = newTrackName;
                            artistsText = newArtistsText;
                            isPlaying = newPlaying;
                            lastTrackPosition = newPosition;
                            lastTrackPollMs = newPollTime;
                            
                            if (newCover == null || newCover.length == 0) {
                                clearCoverTexture();
                                coverHash = 0;
                            } else {
                                if (finalDecodedImage != null) {
                                    if (finalCoverHash != coverHash) {
                                        updateCoverTexture(finalDecodedImage);
                                        coverHash = finalCoverHash;
                                    } else {
                                        try {
                                            finalDecodedImage.close();
                                        } catch (Exception ignored) {}
                                    }
                                } else {
                                    clearCoverTexture();
                                    coverHash = 0;
                                }
                            }
                        });
                    } else {
                        mc.execute(this::clearData);
                    }
                } else {
                    mc.execute(this::clearData);
                }
            } catch (Throwable e) {
                mc.execute(this::clearData);
            } finally {
                polling.set(false);
            }
        });
    }

    private void clearData() {
        trackName = null;
        artistsText = null;
        isPlaying = false;
        activeSession = null;
        clearCoverTexture();
        lastTrackPosition = 0L;
        lastTrackPollMs = 0L;
    }

    private void clearCoverTexture() {
        try {
            TextureManager tm = mc.getTextureManager();
            if (tm != null) {
                tm.destroyTexture(coverTextureLocation);
            }
            if (coverTexture != null) {
                coverTexture.close();
                coverTexture = null;
            }
        } catch (Exception ignored) {
            coverTexture = null;
        }
        coverHash = 0;
    }

    private void updateCoverTexture(NativeImage nativeImage) {
        try {
            if (nativeImage != null) {
                TextureManager tm = mc.getTextureManager();
                if (tm != null) {
                    tm.destroyTexture(coverTextureLocation);
                }
                if (coverTexture != null) {
                    coverTexture.close();
                    coverTexture = null;
                }
                coverTexture = new NativeImageBackedTexture(nativeImage);
                coverTexture.upload(); // загружаем пиксели на GPU
                if (tm != null) {
                    tm.registerTexture(coverTextureLocation, coverTexture);
                }
            }
        } catch (Exception e) {
            clearCoverTexture();
            try {
                nativeImage.close();
            } catch (Exception ignored) {}
        }
    }

    public void handleClick(double mouseX, double mouseY) {
        if (mc.currentScreen == null) return;
        boolean isChat = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean isClickGui = mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen;
        if (!isChat && !isClickGui) return;

        float totalH = lastBaseHeight + expandProgress * EXPAND_PANEL_H;

        // Клик внутри острова или раскрытой панели
        if (mouseX >= lastX && mouseX <= lastX + lastWidth &&
            mouseY >= lastY && mouseY <= lastY + totalH) {

            // Клик на верхнюю часть (сам остров) — toggle
            if (mouseY <= lastY + lastBaseHeight) {
                expanded = !expanded;
                clickAnimActive = true;
                clickAnimProgress = 0f;
                return;
            }

            // Клик внутри панели — проверяем кнопки
            if (expandProgress > 0.5f && activeSession != null) {
                float panelY = lastY + lastBaseHeight;
                float centerX = lastX + lastWidth / 2f;
                float btnGap = 18f;
                float btnStartX = centerX - btnGap;
                float btnY = panelY + 38f * expandProgress;
                float btnHitH = 12f;
                float btnHitW = 20f;

                // << (prev)
                if (mouseX >= btnStartX - btnHitW/2 && mouseX <= btnStartX + btnHitW/2 &&
                    mouseY >= btnY - 2 && mouseY <= btnY + btnHitH) {
                    try { activeSession.previous(); } catch (Exception ignored) {}
                }
                // > || (play/pause)
                else if (mouseX >= centerX - btnHitW/2 && mouseX <= centerX + btnHitW/2 &&
                         mouseY >= btnY - 2 && mouseY <= btnY + btnHitH) {
                    try { activeSession.playPause(); } catch (Exception ignored) {}
                }
                // >> (next)
                else if (mouseX >= btnStartX + btnGap*2 - btnHitW/2 && mouseX <= btnStartX + btnGap*2 + btnHitW/2 &&
                         mouseY >= btnY - 2 && mouseY <= btnY + btnHitH) {
                    try { activeSession.next(); } catch (Exception ignored) {}
                }
            }
        } else {
            // Клик снаружи — закрываем
            expanded = false;
        }
    }

    public void render(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (dt > 0.1f) dt = 0.016f;

        MatrixStack matrices = context.getMatrices();
        updateAnimations(dt);



        boolean showModuleNotification = !currentModuleNotification.isEmpty() &&
                System.currentTimeMillis() - moduleNotificationTime < MODULE_NOTIFICATION_DURATION;
        boolean isPvp = isPvpMode();
        boolean hasMedia = trackName != null && !trackName.isEmpty() && !isPvp;

        float padding = 3f;
        float baseHeight = 17f;

        FontRenderer font = Fonts.SEMIBOLD.get(11);

        String displayText;
        if (showModuleNotification) {
            displayText = currentModuleNotificationClean;
        } else if (isPvp) {
            displayText = "PVP";
        } else if (hasMedia) {
            String artist = artistsText != null ? artistsText : "";
            displayText = trackName + (artist.isEmpty() ? "" : " - " + artist);
        } else {
            displayText = "Solution";
        }

        // Рассчитываем ширину времени и пинга внутри острова
        float timeW = 0;
        String timeStr = "";
        if (showTime.isEnabled()) {
            timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            timeW = Fonts.DEFAULT.get(11).getStringWidth(timeStr) + 10f;
        }

        float pingW = 0;
        String pingStr = "";
        if (showPing.isEnabled()) {
            pingStr = getPing() + "ms";
            pingW = Fonts.SEMIBOLD.get(10).getStringWidth(pingStr) + 10f;
        }

        float textWidth = font.getStringWidth(displayText);
        float squareSize = baseHeight - padding * 2;
        // Итоговая ширина острова: [Время] + [Контент] + [Пинг]
        float width = timeW + squareSize + padding + textWidth + padding * 2 + pingW;

        // Boss bar offset
        float bossBarOffset = 0f;
        if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            // Игнорируем PVP боссбар, так как мы рендерим его внутри самого острова
            long nonPvpBossBars = ((IBossBarHud) mc.inGameHud.getBossBarHud()).getBossBars().values().stream()
                    .filter(bossBar -> {
                        String name = bossBar.getName().getString().toLowerCase();
                        return !(name.contains("pvp") || name.contains("пвп") || name.contains("схватки"));
                    }).count();
            if (nonPvpBossBars > 0) {
                bossBarOffset = 19;
            }
        }

        float x = mc.getWindow().getScaledWidth() / 2f - width / 2f;
        float y = 2f + bossBarOffset;

        // Save bounds for click detection
        lastX = x; lastY = y; lastWidth = width; lastBaseHeight = baseHeight;

        // Update expand animation
        expandProgress += (expanded ? EXPAND_SPEED : -EXPAND_SPEED) * dt;
        expandProgress = Math.max(0f, Math.min(1f, expandProgress));

        // Smoothstep
        float ep = expandProgress * expandProgress * (3f - 2f * expandProgress);

        int bgColor = TempColor.getModuleBackground().alpha(0.9f).getRGB();
        
        // Кэшируем цвета для текста (пересоздаём при изменении alpha или темы)
        float contentAlpha = Math.max(moduleAnimationProgress, Math.max(pvpAnimationProgress, mediaAnimationProgress));
        float textAlpha = (showModuleNotification || isPvp || hasMedia) ? contentAlpha : 1.0f;
        int currentAlpha = (int)(255 * textAlpha);

        int currentThemeHash = TempColor.getTextPrimary().getRGB(); // меняется при смене темы
        if (lastCachedAlpha != currentAlpha || lastCachedTheme != currentThemeHash) {
            lastCachedAlpha = currentAlpha;
            lastCachedTheme = currentThemeHash;
            float a = currentAlpha / 255f;
            cachedWhiteText     = TempColor.getTextPrimary().alpha(a).getRGB();
            cachedGrayText      = TempColor.getTextSecondary().alpha(a).getRGB();
            cachedLightGrayText = TempColor.getTextSecondary().alpha(a * 0.85f).getRGB();
            cachedGreenDot      = new Color(55, 220, 55, currentAlpha).getRGB();
            cachedRedDot        = new Color(220, 55, 55, currentAlpha).getRGB();
            cachedPlaceholderBg = TempColor.getModuleBackground().alpha(a).getRGB();
            cachedPvpTimerBg    = new Color(220, 40, 40, currentAlpha).getRGB();
            cachedPvpTimerText  = TempColor.getTextPrimary().alpha(a).getRGB();
        }

        float totalH = baseHeight + ep * EXPAND_PANEL_H;

        glass.render(ShapeProperties.create(matrices, x, y, width, totalH)
                .round(baseHeight / 2f)
                .softness(1.5f)
                .thickness(0f)
                .outlineColor(0)
                .color(bgColor)
                .build());

        float contentX = x + timeW;
        // Draw content
        if (showModuleNotification && moduleAnimationProgress > 0.01f) {
            drawModuleNotification(context, matrices, font, contentX, y, baseHeight, padding, squareSize);
        } else if (hasMedia && mediaAnimationProgress > 0.01f) {
            drawMediaPlayer(context, matrices, font, contentX, y, baseHeight, padding, squareSize, displayText);
        } else if (isPvp && pvpAnimationProgress > 0.01f) {
            drawPvpIndicator(context, matrices, font, contentX, y, baseHeight, padding, squareSize);
        } else {
            drawDefaultState(context, matrices, font, contentX, y, baseHeight, padding, squareSize);
        }

        // Draw expanded panel content
        if (ep > 0.01f && hasMedia) {
            drawExpandedPanel(context, matrices, x, y + baseHeight, width, EXPAND_PANEL_H, ep);
        }

        // Vertical center of the island for all side elements — same Y as track text
        float islandCenterY = y + baseHeight / 2f - 1f;

        // Отрисовка времени внутри
        if (showTime.isEnabled()) {
            Fonts.DEFAULT.get(11).drawString(matrices, timeStr, x + 5, islandCenterY, cachedWhiteText);
        }

        // Отрисовка пинга внутри
        if (showPing.isEnabled()) {
            int ping = getPing();
            int pingColor = ping < 50 ? 0xFF55FF55 : ping < 100 ? 0xFFFFFF55 : ping < 200 ? 0xFFFFAA00 : 0xFFFF5555;
            Fonts.SEMIBOLD.get(10).drawString(matrices, pingStr, x + width - pingW + 5, islandCenterY, pingColor);
        }
    }

    private void drawExpandedPanel(DrawContext context, MatrixStack matrices,
                                   float x, float y, float width, float maxH, float progress) {
        float alpha = progress;
        int textAlpha = (int)(255 * alpha);

        FontRenderer font = Fonts.SEMIBOLD.get(11);
        FontRenderer smallFont = Fonts.DEFAULT.get(10);

        float panelH = maxH * progress;
        float centerX = x + width / 2f;

        // Custom faded colors for premium animations
        int whiteColor = TempColor.getTextPrimary().alpha(alpha).getRGB();
        int grayColor = TempColor.getTextSecondary().alpha(alpha).getRGB();
        int lightGrayColor = TempColor.getTextSecondary().alpha(alpha * 0.85f).getRGB();

        // Separator line
        float lineY = y + 4f * progress;
        blur.render(ShapeProperties.create(matrices, x + 10, lineY, width - 20, 0.5f)
                .round(0.5f)
                .color(TempColor.getModuleBorder().alpha(0.4f * alpha).getRGB())
                .build());

        // Track name
        String track = trackName != null ? trackName : "";
        String artist = artistsText != null ? artistsText : "";
        float trackY = y + 12f * progress;

        if (trackY < y + panelH - 4) {
            float tw = font.getStringWidth(track);
            font.drawString(matrices, track,
                    centerX - tw / 2f,
                    trackY,
                    whiteColor);
        }

        if (!artist.isEmpty()) {
            float aw = smallFont.getStringWidth(artist);
            float artistY = y + 22f * progress;
            if (artistY < y + panelH - 4) {
                smallFont.drawString(matrices, artist,
                        centerX - aw / 2f,
                        artistY,
                        grayColor);
            }
        }

        // Controls: ⏮ ⏸/▶ ⏭
        float btnY = y + 38f * progress;
        if (btnY < y + panelH - 4) {
            FontRenderer iconFont = Fonts.DEFAULT.get(14);
            String playIcon = isPlaying ? "||" : ">";
            String[] icons = {"<<", playIcon, ">>"};
            float btnGap = 18f;
            float totalBtnsW = btnGap * 2;
            float btnStartX = centerX - totalBtnsW / 2f;

            for (int i = 0; i < 3; i++) {
                float bx = btnStartX + i * btnGap;
                float iw = iconFont.getStringWidth(icons[i]);
                iconFont.drawString(matrices, icons[i],
                        bx - iw / 2f,
                        btnY,
                        lightGrayColor);
            }
        }
    }

    private void drawModuleNotification(DrawContext context, MatrixStack matrices, FontRenderer font,
                                        float x, float y, float height, float padding, float squareSize) {
        int alpha = (int)(255 * moduleAnimationProgress);
        int dotColor = currentModuleNotification.contains("§a") ? cachedGreenDot : cachedRedDot;

        float scale = 1f - 0.15f * (float) Math.sin(clickAnimProgress * Math.PI);
        float offset = squareSize * (1f - scale) / 2f;
        blur.render(ShapeProperties.create(matrices, x + padding + offset, y + padding + offset, squareSize * scale, squareSize * scale)
                .round(squareSize * scale / 2f).softness(1f)
                .color(dotColor).build());

        float textY = y + height / 2f + 3f - font.getStringHeight(currentModuleNotificationClean) / 4f;
        font.drawString(matrices, currentModuleNotificationClean,
                x + padding + squareSize + padding,
                textY,
                cachedWhiteText);
    }

    private void drawMediaPlayer(DrawContext context, MatrixStack matrices, FontRenderer font,
                                 float x, float y, float height, float padding, float squareSize, String fullTrack) {
        int alpha = (int)(255 * mediaAnimationProgress);

        float scale = 1f - 0.15f * (float) Math.sin(clickAnimProgress * Math.PI);
        float offset = squareSize * (1f - scale) / 2f;
        float scaledSize = squareSize * scale;

        if (coverTexture != null) {
            float cx = x + padding + offset;
            float cy = y + padding + offset;
            float r = scaledSize / 4f;
            // Используем SDF-шейдер для закруглённой текстуры — надёжно и без stencil
            roundedTexture.render(matrices, cx, cy, scaledSize, scaledSize, r,
                    coverTexture, new Color(255, 255, 255, alpha));
        } else {
            drawCoverPlaceholder(matrices, x + padding, y + padding, squareSize, alpha);
        }

        float textY = y + height / 2f - 1f;
        font.drawString(matrices, fullTrack,
                x + padding + squareSize + padding,
                textY,
                cachedWhiteText);
    }

    private void drawCoverPlaceholder(MatrixStack matrices, float x, float y, float size, int alpha) {
        float scale = 1f - 0.15f * (float) Math.sin(clickAnimProgress * Math.PI);
        float offset = size * (1f - scale) / 2f;
        blur.render(ShapeProperties.create(matrices, x + offset, y + offset, size * scale, size * scale)
                .round(size * scale / 4f).softness(0.5f)
                .color(cachedPlaceholderBg).build());
    }

    private void drawPvpIndicator(DrawContext context, MatrixStack matrices, FontRenderer font,
                                  float x, float y, float height, float padding, float squareSize) {
        int alpha = (int)(255 * pvpAnimationProgress);

        float scale = 1f - 0.15f * (float) Math.sin(clickAnimProgress * Math.PI);
        float offset = squareSize * (1f - scale) / 2f;
        blur.render(ShapeProperties.create(matrices, x + padding + offset, y + padding + offset, squareSize * scale, squareSize * scale)
                .round(squareSize * scale / 4f).softness(1f)
                .color(cachedPvpTimerBg).build());

        FontRenderer timerFont = Fonts.SEMIBOLD.get(9);
        String pvpTimer = getPvpTimer();
        timerFont.drawCenteredString(matrices, pvpTimer,
                x + padding + squareSize / 2f,
                y + height / 2f + 3f - timerFont.getStringHeight(pvpTimer) / 4f,
                new Color(255, 255, 255, alpha).getRGB());

        float textY = y + height / 2f + 3f - font.getStringHeight("PVP") / 4f;
        font.drawString(matrices, "PVP",
                x + padding + squareSize + padding,
                textY,
                new Color(255, 255, 255, alpha).getRGB());
    }

    private void drawDefaultState(DrawContext context, MatrixStack matrices, FontRenderer font,
                                  float x, float y, float height, float padding, float squareSize) {
        int accentColor = TempColor.getClientColor().alpha(230).getRGB();
        float scale = 1f - 0.15f * (float) Math.sin(clickAnimProgress * Math.PI);
        float offset = squareSize * (1f - scale) / 2f;
        blur.render(ShapeProperties.create(matrices, x + padding + offset, y + padding + offset, squareSize * scale, squareSize * scale)
                .round(squareSize * scale / 4f).softness(1f)
                .color(accentColor).build());

        float textY = y + height / 2f + 2.5f - font.getStringHeight("Solution") / 4f;
        font.drawString(matrices, "Solution",
                x + padding + squareSize + padding,
                textY,
                cachedWhiteText);
    }

    private void updateAnimations(float dt) {
        // Module notification animation
        boolean showModuleNotification = !currentModuleNotification.isEmpty() &&
                System.currentTimeMillis() - moduleNotificationTime < MODULE_NOTIFICATION_DURATION;
        
        if (showModuleNotification) {
            moduleAnimationProgress = Math.min(1f, moduleAnimationProgress + 5.0f * dt);
        } else {
            moduleAnimationProgress = Math.max(0f, moduleAnimationProgress - 5.0f * dt);
        }
        
        // PVP animation
        boolean isPvp = isPvpMode();
        if (isPvp) {
            pvpAnimationProgress = Math.min(1f, pvpAnimationProgress + 5.0f * dt);
        } else {
            pvpAnimationProgress = Math.max(0f, pvpAnimationProgress - 5.0f * dt);
        }
        
        // Media animation
        boolean hasMedia = trackName != null && !trackName.isEmpty();
        if (hasMedia && !isPvp) {
            mediaAnimationProgress = Math.min(1f, mediaAnimationProgress + 5.0f * dt);
        } else {
            mediaAnimationProgress = Math.max(0f, mediaAnimationProgress - 5.0f * dt);
        }

        // Click animation for square button (ping-pong: go to 1 then back to 0)
        if (clickAnimActive) {
            clickAnimProgress = Math.min(1f, clickAnimProgress + CLICK_ANIM_SPEED * dt);
            if (clickAnimProgress >= 1f) {
                clickAnimActive = false;
            }
        } else if (clickAnimProgress > 0f) {
            clickAnimProgress = Math.max(0f, clickAnimProgress - CLICK_ANIM_SPEED * dt);
        }
    }

    public boolean isPvpMode() {
        if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) {
            return false;
        }
        
        for (ClientBossBar bossBar : ((IBossBarHud) mc.inGameHud.getBossBarHud()).getBossBars().values()) {
            String name = bossBar.getName().getString().toLowerCase();
            if (name.contains("pvp") 
                    || name.contains("пвп") 
                    || name.contains("схватки") 
                    || name.contains("бой") 
                    || name.contains("в бою") 
                    || name.contains("режим боя") 
                    || name.contains("combat")) {
                return true;
            }
        }
        return false;
    }

    private String getPvpTimer() {
        if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) {
            return "30";
        }
        
        for (ClientBossBar bossBar : ((IBossBarHud) mc.inGameHud.getBossBarHud()).getBossBars().values()) {
            String name = bossBar.getName().getString().toLowerCase();
            if (name.contains("pvp") 
                    || name.contains("пвп") 
                    || name.contains("схватки") 
                    || name.contains("бой") 
                    || name.contains("в бою") 
                    || name.contains("режим боя") 
                    || name.contains("combat")) {
                Matcher matcher = PVP_TIMER_PATTERN.matcher(bossBar.getName().getString());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return "30";
    }

    private int getPing() {
        if (mc.player != null && mc.getNetworkHandler() != null && 
            mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
            return mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
        }
        return 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        farvix.solution.api.ui.hud.HudElementRegistry.getInstance().register(this);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        farvix.solution.api.ui.hud.HudElementRegistry.getInstance().unregister(this);
        clearData();
    }
    
    // ── IHudElement implementation ────────────────────────────────────────────
    @Override
    public float getHudX() {
        return lastX;
    }

    @Override
    public float getHudY() {
        return lastY;
    }

    @Override
    public float getHudWidth() {
        return lastWidth;
    }

    @Override
    public float getHudHeight() {
        return lastBaseHeight + expandProgress * EXPAND_PANEL_H;
    }

    @Override
    public Module getModule() {
        return this;
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        float w = getHudWidth();
        float h = getHudHeight();
        return mouseX >= lastX && mouseX <= lastX + w && mouseY >= lastY && mouseY <= lastY + h;
    }
}
