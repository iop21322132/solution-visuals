package farvix.solution.client.managers;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThemeManager {
    private static ThemeManager instance;
    private Theme currentTheme;
    private final List<ThemeChangeListener> listeners = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ThemeManager() {
        // По умолчанию черная тема (Black)
        this.currentTheme = new farvix.solution.api.ui.clickgui.impl.theme.ThemeScreen.SingleColorTheme(
                "Black", new Color(0, 0, 0, 200));
        startGradientUpdateTask();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        notifyListeners();
    }

    public void addThemeChangeListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }

    public void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (ThemeChangeListener listener : listeners) {
            listener.onThemeChanged(currentTheme);
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public Color getThemeColor() {
        return currentTheme.getBackgroundColor();
    }

    public Theme[] getAvailableThemes() {
        return new Theme[]{
                new LightTheme(),
                new TwilightGradientTheme(),
                new EmeraldGlowTheme(),
                new AmethystFadeTheme(),
                new SunsetBlazeTheme(),
                new OceanBreezeTheme(),
                new AuroraBorealisTheme(),
                new CyberNeonTheme(),
                new LavaCoreTheme(),
                new RGBTheme()
        };
    }

    private void startGradientUpdateTask() {
        scheduler.scheduleAtFixedRate(() -> {
            if (currentTheme instanceof GradientTheme) {
                notifyListeners();
            }
        }, 0, 25, TimeUnit.MILLISECONDS);
    }

    public interface Theme {
        Color getBackgroundColor();
        Color getBorderColor();
        Color getTextColor();
        Color getAccentColor();
        Color getSecondaryBackgroundColor();
        String getName();
    }

    public interface ThemeChangeListener {
        void onThemeChanged(Theme theme);
    }

    private static Color interpolateColor(Color start, Color end, float t) {
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * t);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * t);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
        return new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)),
                Math.max(0, Math.min(255, a))
        );
    }

    private static abstract class GradientTheme implements Theme {
        protected final Color[] backgroundColors;
        protected final Color[] borderColors;
        protected final Color[] accentColors;
        protected final Color[] secondaryBackgroundColors;
        private static final long TRANSITION_DURATION = 1000;

        protected GradientTheme(Color[] backgroundColors, Color[] borderColors,
                                Color[] accentColors, Color[] secondaryBackgroundColors) {
            this.backgroundColors = backgroundColors;
            this.borderColors = borderColors;
            this.accentColors = accentColors;
            this.secondaryBackgroundColors = secondaryBackgroundColors;
        }

        protected float getInterpolationFactor() {
            long currentTime = System.currentTimeMillis();
            float phase = (float) (currentTime % (TRANSITION_DURATION * backgroundColors.length)) / TRANSITION_DURATION;
            int index = (int) phase;
            float t = phase - index;
            t = (float) (Math.sin(t * Math.PI / 2));
            return t;
        }

        protected int getCurrentIndex() {
            long currentTime = System.currentTimeMillis();
            return (int) ((currentTime % (TRANSITION_DURATION * backgroundColors.length)) / TRANSITION_DURATION);
        }

        @Override
        public Color getBackgroundColor() {
            int currentIndex = getCurrentIndex();
            int nextIndex = (currentIndex + 1) % backgroundColors.length;
            return interpolateColor(backgroundColors[currentIndex], backgroundColors[nextIndex], getInterpolationFactor());
        }

        @Override
        public Color getBorderColor() {
            int currentIndex = getCurrentIndex();
            int nextIndex = (currentIndex + 1) % borderColors.length;
            return interpolateColor(borderColors[currentIndex], borderColors[nextIndex], getInterpolationFactor());
        }

        @Override
        public Color getAccentColor() {
            int currentIndex = getCurrentIndex();
            int nextIndex = (currentIndex + 1) % accentColors.length;
            return interpolateColor(accentColors[currentIndex], accentColors[nextIndex], getInterpolationFactor());
        }

        @Override
        public Color getSecondaryBackgroundColor() {
            int currentIndex = getCurrentIndex();
            int nextIndex = (currentIndex + 1) % secondaryBackgroundColors.length;
            return interpolateColor(secondaryBackgroundColors[currentIndex], secondaryBackgroundColors[nextIndex], getInterpolationFactor());
        }
    }

    public static class LightTheme implements Theme {
        @Override public Color getBackgroundColor()          { return new Color(255, 255, 255, 100); }
        @Override public Color getBorderColor()              { return new Color(255, 255, 255, 100); }
        @Override public Color getTextColor()                { return Color.WHITE; }
        @Override public Color getAccentColor()              { return new Color(255, 255, 255, 100); }
        @Override public Color getSecondaryBackgroundColor() { return new Color(255, 255, 255, 100); }
        @Override public String getName()                    { return "White"; }
    }

    /** Тёмная тема по умолчанию — чёрный фон, белый текст */
    public static class DarkTheme implements Theme {
        @Override public Color getBackgroundColor()          { return new Color(0, 0, 0, 0); }
        @Override public Color getBorderColor()              { return new Color(255, 255, 255, 28); }
        @Override public Color getTextColor()                { return Color.WHITE; }
        @Override public Color getAccentColor()              { return new Color(213, 180, 255, 255); }
        @Override public Color getSecondaryBackgroundColor() { return new Color(0, 0, 0, 120); }
        @Override public String getName()                    { return "Dark"; }
    }

    public static class TwilightGradientTheme extends GradientTheme {
        public TwilightGradientTheme() {
            super(
                    new Color[]{
                            new Color(80, 42, 195, 150),
                            new Color(156, 69, 211, 150),
                            new Color(179, 103, 250, 150),
                            new Color(148, 36, 255, 150)
                    },
                    new Color[]{
                            new Color(80, 42, 195, 150),
                            new Color(156, 69, 211, 150),
                            new Color(179, 103, 250, 150),
                            new Color(148, 36, 255, 150)
                    },
                    new Color[]{
                            new Color(80, 42, 195, 150),
                            new Color(156, 69, 211, 150),
                            new Color(179, 103, 250, 150),
                            new Color(148, 36, 255, 150)
                    },
                    new Color[]{
                            new Color(80, 42, 195, 150),
                            new Color(156, 69, 211, 150),
                            new Color(179, 103, 250, 150),
                            new Color(148, 36, 255, 150)
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Purple";
        }
    }

    public static class EmeraldGlowTheme extends GradientTheme {
        public EmeraldGlowTheme() {
            super(
                    new Color[]{
                            new Color(0, 204, 160, 150),
                            new Color(2, 204, 73, 150),
                            new Color(15, 227, 0, 150),
                            new Color(0, 170, 31, 150)
                    },
                    new Color[]{
                            new Color(0, 204, 160, 150),
                            new Color(2, 204, 73, 150),
                            new Color(15, 227, 0, 150),
                            new Color(0, 170, 31, 150)
                    },
                    new Color[]{
                            new Color(0, 204, 160, 150),
                            new Color(2, 204, 73, 150),
                            new Color(15, 227, 0, 150),
                            new Color(0, 170, 31, 150)
                    },
                    new Color[]{
                            new Color(0, 204, 160, 150),
                            new Color(2, 204, 73, 150),
                            new Color(15, 227, 0, 150),
                            new Color(0, 170, 31, 150)
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Emerald";
        }
    }

    public static class AmethystFadeTheme extends GradientTheme {
        public AmethystFadeTheme() {
            super(
                    new Color[]{
                            new Color(100, 50, 150, 150),
                            new Color(150, 80, 200, 150),
                            new Color(180, 120, 220, 150),
                            new Color(80, 40, 120, 150)
                    },
                    new Color[]{
                            new Color(80, 40, 120, 150),
                            new Color(120, 64, 160, 150),
                            new Color(144, 96, 176, 150),
                            new Color(64, 32, 96, 150)
                    },
                    new Color[]{
                            new Color(60, 30, 90, 150),
                            new Color(90, 48, 120, 150),
                            new Color(108, 72, 132, 150),
                            new Color(48, 24, 72, 150)
                    },
                    new Color[]{
                            new Color(120, 70, 170, 150),
                            new Color(170, 100, 220, 150),
                            new Color(200, 140, 240, 150),
                            new Color(100, 60, 140, 150)
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Amethyst";
        }
    }

    public static class SunsetBlazeTheme extends GradientTheme {
        public SunsetBlazeTheme() {
            super(
                    new Color[]{
                            new Color(200, 60, 20, 150),
                            new Color(220, 100, 40, 150),
                            new Color(240, 140, 60, 150),
                            new Color(180, 40, 10, 150)
                    },
                    new Color[]{
                            new Color(160, 48, 16, 150),
                            new Color(176, 80, 32, 150),
                            new Color(192, 112, 48, 150),
                            new Color(144, 32, 8, 150)
                    },
                    new Color[]{
                            new Color(120, 36, 12, 150),
                            new Color(132, 60, 24, 150),
                            new Color(144, 84, 36, 150),
                            new Color(108, 24, 6, 150)
                    },
                    new Color[]{
                            new Color(220, 80, 40, 150),
                            new Color(240, 120, 60, 150),
                            new Color(255, 160, 80, 150),
                            new Color(200, 60, 20, 150)
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Orange";
        }
    }

    public static class OceanBreezeTheme extends GradientTheme {
        public OceanBreezeTheme() {
            super(
                    new Color[]{
                            new Color(20, 80, 120, 150),
                            new Color(40, 120, 160, 150),
                            new Color(60, 160, 200, 150),
                            new Color(10, 60, 100, 150)
                    },
                    new Color[]{
                            new Color(16, 64, 96, 150),
                            new Color(32, 96, 128, 150),
                            new Color(48, 128, 160, 150),
                            new Color(8, 48, 80, 150)
                    },
                    new Color[]{
                            new Color(12, 48, 72, 150),
                            new Color(24, 72, 96, 150),
                            new Color(36, 96, 120, 150),
                            new Color(6, 36, 60, 150)
                    },
                    new Color[]{
                            new Color(40, 100, 140, 150),
                            new Color(60, 140, 180, 150),
                            new Color(80, 180, 220, 150),
                            new Color(30, 80, 120, 150)
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Blue";
        }
    }

    public static class AuroraBorealisTheme extends GradientTheme {
        public AuroraBorealisTheme() {
            super(
                    new Color[]{
                            new Color(10, 80, 60, 150),
                            new Color(40, 180, 120, 150),
                            new Color(80, 220, 200, 150),
                            new Color(120, 100, 220, 150)
                    },
                    new Color[]{
                            new Color(8, 64, 48, 150),
                            new Color(32, 144, 96, 150),
                            new Color(64, 176, 160, 150),
                            new Color(96, 80, 176, 150)
                    },
                    new Color[]{
                            new Color(20, 120, 90, 150),
                            new Color(60, 200, 140, 150),
                            new Color(100, 240, 220, 150),
                            new Color(150, 130, 255, 150)
                    },
                    new Color[]{
                            new Color(16, 100, 80, 150),
                            new Color(50, 170, 130, 150),
                            new Color(90, 210, 190, 150),
                            new Color(130, 115, 240, 150)
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Aurora";
        }
    }

    public static class CyberNeonTheme extends GradientTheme {
        public CyberNeonTheme() {
            super(
                    new Color[]{
                            new Color(0, 255, 200, 150),
                            new Color(255, 0, 180, 150),
                            new Color(0, 140, 255, 150),
                            new Color(255, 255, 0, 150)
                    },
                    new Color[]{
                            new Color(0, 210, 165, 150),
                            new Color(210, 0, 150, 150),
                            new Color(0, 110, 210, 150),
                            new Color(210, 210, 0, 150)
                    },
                    new Color[]{
                            new Color(50, 255, 220, 150),
                            new Color(255, 50, 200, 150),
                            new Color(50, 170, 255, 150),
                            new Color(255, 255, 50, 150)
                    },
                    new Color[]{
                            new Color(0, 235, 190, 150),
                            new Color(235, 0, 170, 150),
                            new Color(0, 130, 235, 150),
                            new Color(235, 235, 0, 150)
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Neon";
        }
    }

    public static class LavaCoreTheme extends GradientTheme {
        public LavaCoreTheme() {
            super(
                    new Color[]{
                            new Color(120, 0, 0, 150),
                            new Color(200, 20, 0, 150),
                            new Color(255, 90, 0, 150),
                            new Color(255, 180, 0, 150)
                    },
                    new Color[]{
                            new Color(90, 0, 0, 150),
                            new Color(160, 16, 0, 150),
                            new Color(210, 75, 0, 150),
                            new Color(220, 150, 0, 150)
                    },
                    new Color[]{
                            new Color(160, 10, 0, 150),
                            new Color(230, 40, 0, 150),
                            new Color(255, 120, 0, 150),
                            new Color(255, 210, 0, 150)
                    },
                    new Color[]{
                            new Color(140, 5, 0, 150),
                            new Color(210, 30, 0, 150),
                            new Color(240, 100, 0, 150),
                            new Color(245, 190, 0, 150)
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "Lava";
        }
    }

    public static class RGBTheme extends GradientTheme {
        public RGBTheme() {
            super(
                    new Color[]{
                            new Color(255, 0, 0, 150),
                            new Color(17, 255, 0, 150),
                            new Color(0, 5, 255, 150),
                            new Color(255, 0, 241, 150)
                    },
                    new Color[]{
                            new Color(255, 0, 0, 150),
                            new Color(17, 255, 0, 150),
                            new Color(0, 5, 255, 150),
                            new Color(255, 0, 241, 150)
                    },
                    new Color[]{
                            new Color(255, 0, 0, 150),
                            new Color(17, 255, 0, 150),
                            new Color(0, 5, 255, 150),
                            new Color(255, 0, 241, 150)
                    },
                    new Color[]{
                            new Color(255, 0, 0, 150),
                            new Color(17, 255, 0, 150),
                            new Color(0, 5, 255, 150),
                            new Color(255, 0, 241, 150)
                    }
            );
        }

        @Override
        public Color getTextColor() {
            return Color.WHITE;
        }

        @Override
        public String getName() {
            return "RGB";
        }
    }
}
