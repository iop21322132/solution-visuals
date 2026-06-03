package dev.simplevisuals.client.ui.mainmenu;

import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.render.builders.impl.BlurBuilder;
import dev.simplevisuals.client.render.builders.impl.RectangleBuilder;
import dev.simplevisuals.client.render.builders.impl.TextBuilder;
import dev.simplevisuals.client.render.builders.states.QuadColorState;
import dev.simplevisuals.client.render.builders.states.QuadRadiusState;
import dev.simplevisuals.client.render.builders.states.SizeState;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainMenu extends Screen {

    // ===== Static session tracking =====
    private static final Animation sessionTimer = new Animation(0, 0, true, Easing.LINEAR);
    private static boolean isFirstInstance = true;
    private static boolean welcomePlayed = false;

    // ===== UI state =====
    private final List<AnimatedButton> buttons = new ArrayList<>();
    private Animation alphaAnimation;
    private boolean isExiting = false;
    private boolean isNewEntry = true;
    private int lastWidth = 0;
    private int lastHeight = 0;
    private final List<Snowflake> snowflakes = new ArrayList<>();
    private final Random snowRandom = new Random();
    private long lastSnowUpdateNanos = 0L;

    public MainMenu() {
        super(Text.literal(I18n.translate("simplevisuals.mainmenu.screen_title")));
        alphaAnimation = new Animation(500, 1.0, true, Easing.OUT_EXPO);

        if (isFirstInstance) {
            isFirstInstance = false;
        }
    }

    @Override
    protected void init() {
        super.init();

        lastWidth = this.width;
        lastHeight = this.height;
        buttons.clear();
        ensureSnowflakes();

        float buttonWidth = 125f;
        float buttonHeight = 22f;
        float margin = 3f;

        float centerX = this.width / 2f - buttonWidth / 2f;
        float centerY = this.height / 2f;

        buttons.add(new AnimatedButton(centerX, centerY - buttonHeight * 2f - margin * 2, buttonWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.singleplayer"),
                btn -> startExitAnimation(new AnimatedScreenWrapper(new SelectWorldScreen(this), this))));

        buttons.add(new AnimatedButton(centerX, centerY - buttonHeight - margin, buttonWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.multiplayer"),
                btn -> startExitAnimation(new AnimatedScreenWrapper(new MultiplayerScreen(this), this))));

        buttons.add(new AnimatedButton(centerX, centerY, buttonWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.options"),
                btn -> startExitAnimation(new AnimatedScreenWrapper(new OptionsScreen(this, this.client.options), this))));
        float halfWidth = (buttonWidth - margin) / 2f;
        buttons.add(new AnimatedButton(centerX, centerY + buttonHeight + margin, halfWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.altmanager"),
                btn -> {
                    isNewEntry = true;
                    if (this.client != null) {
                        this.client.setScreen(new AltManagerScreen(this));
                    }
                }));
        buttons.add(new AnimatedButton(centerX + halfWidth + margin, centerY + buttonHeight + margin, halfWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.quit"),
                btn -> startExitAnimation(null)));

        if (isNewEntry) {
            isExiting = false;
            isNewEntry = false;
            alphaAnimation = new Animation(500, 1.0, true, Easing.OUT_EXPO);
            buttons.forEach(AnimatedButton::resetAnimations);
        }

        if (!welcomePlayed) {
            welcomePlayed = true;
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(
                            SoundEvent.of(Identifier.of("simplevisuals:welcome")),
                            1.0f,
                            1.0f
                    )
            );
        }
    }

    private void startExitAnimation(Screen nextScreen) {
        if (isExiting) return;

        isExiting = true;
        isNewEntry = true;
        alphaAnimation.update(false);

        if (nextScreen == null) {
            sessionTimer.reset();
        }

        new Thread(() -> {
            try {
                Thread.sleep(10); // match fade out duration
                client.execute(() -> {
                    if (nextScreen == null) client.scheduleStop();
                    else client.setScreen(nextScreen);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AnimatedButton btn : buttons) {
            if (btn.isMouseOver((int) mouseX, (int) mouseY)) {
                btn.onPress();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }


    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (this.width != lastWidth || this.height != lastHeight) {
            this.init();
        }

        long nowNanos = System.nanoTime();
        if (lastSnowUpdateNanos == 0L) lastSnowUpdateNanos = nowNanos;
        float dt = (nowNanos - lastSnowUpdateNanos) / 1_000_000_000f;
        if (dt > 0.1f) dt = 0.1f;
        lastSnowUpdateNanos = nowNanos;
        ensureSnowflakes();
        updateSnowflakes(dt);
        float timeSec = nowNanos / 1_000_000_000f;

        float uiAlpha = alphaAnimation.getValue();
        float bgAlpha = 1.0f;
        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

        // Background gradient (darker at top, lighter at bottom)
        Color bgTL = movingBgColor(timeSec, 0.0f, bgAlpha);
        Color bgTR = movingBgColor(timeSec, 2.1f, bgAlpha);
        Color bgBR = movingBgColor(timeSec, 4.2f, bgAlpha);
        Color bgBL = movingBgColor(timeSec, 6.3f, bgAlpha);
        new RectangleBuilder()
                .size(new SizeState(this.width, this.height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(
                        bgTL,
                        bgTR,
                        bgBR,
                        bgBL
                ))
                .build()
                .render(matrix, 0f, 0f, 0f);

        // Размытие и фон
        new BlurBuilder()
                .size(new SizeState(this.width, this.height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(new Color(255, 255, 255, 40)))
                .blurRadius(2.6f)
                .smoothness(30f)
                .build()
                .render(matrix, 0f, 0f, 0f);

        new BlurBuilder()
                .size(new SizeState(this.width, this.height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(new Color(15, 15, 20, 10)))
                .blurRadius(1.35f)
                .smoothness(30f)
                .build()
                .render(matrix, 0f, 0f, 0f);

        renderSnowflakes(matrix, 1.0f, timeSec);

        // Заголовок
        String title = I18n.translate("simplevisuals.mainmenu.title");
        float titleX = this.width / 2f - Fonts.BOLD.getWidth(title, 10f) / 2f;
        float titleY = this.height / 2f - 75;

        int split = title.indexOf(" - ");
        if (split >= 0) {
            String brand = title.substring(0, split);
            String tagline = title.substring(split);

            renderAnimatedRedWhiteGradientText(matrix, brand, titleX, titleY, 10f, uiAlpha, timeSec);

            float brandWidth = Fonts.BOLD.getWidth(brand, 10f);
            new TextBuilder()
                    .font(Fonts.BOLD.font())
                    .text(tagline)
                    .size(10f)
                    .color(new Color(255, 255, 255, (int) (uiAlpha * 255)))
                    .smoothness(0.5f)
                    .build()
                    .render(matrix, titleX + brandWidth, titleY, 0f);
        } else {
            renderAnimatedRedWhiteGradientText(matrix, title, titleX, titleY, 10f, uiAlpha, timeSec);
        }

        // Копирайт
        String copyright = I18n.translate("simplevisuals.mainmenu.copyright");
        float copyrightX = this.width - Fonts.REGULAR.getWidth(copyright, 6f) - 5;

        new TextBuilder()
                .font(Fonts.REGULAR.font())
                .text(copyright)
                .size(6f)
                .color(new Color(255, 255, 255, (int) (uiAlpha * 255)))
                .smoothness(0.5f)
                .build()
                .render(matrix, copyrightX, this.height - 10, 0f);

        // Кнопки
        for (AnimatedButton btn : buttons) {
            btn.render(drawContext, mouseX, mouseY, delta, uiAlpha);
        }
    }

    private void ensureSnowflakes() {
        if (this.width <= 0 || this.height <= 0) return;
        int targetCount = Math.min(360, Math.max(120, (this.width * this.height) / 9000));
        if (snowflakes.size() == targetCount) return;

        snowflakes.clear();
        for (int i = 0; i < targetCount; i++) {
            Snowflake flake = new Snowflake();
            flake.respawn(snowRandom, this.width, this.height, true);
            snowflakes.add(flake);
        }
    }

    private void updateSnowflakes(float dt) {
        if (snowflakes.isEmpty()) return;
        for (Snowflake flake : snowflakes) {
            flake.y += flake.speed * dt;
            flake.x += flake.wind * dt;

            float padding = 24f;
            if (flake.x < -padding) flake.x = this.width + padding;
            if (flake.x > this.width + padding) flake.x = -padding;
            if (flake.y - flake.size > this.height + padding) {
                flake.respawn(snowRandom, this.width, this.height, false);
            }
        }
    }

    private void renderSnowflakes(Matrix4f matrix, float menuAlpha, float timeSec) {
        if (snowflakes.isEmpty()) return;

        for (Snowflake flake : snowflakes) {
            int a = (int) (255f * flake.opacity * menuAlpha);
            if (a <= 0) continue;

            float sx = flake.x + (float) Math.sin(timeSec * flake.swaySpeed + flake.phase) * flake.swayAmp;
            float sy = flake.y;
            Color color = new Color(235, 235, 255, Math.min(255, a));

            new RectangleBuilder()
                    .size(new SizeState(flake.size, flake.size))
                    .radius(new QuadRadiusState(flake.size))
                    .color(new QuadColorState(color))
                    .build()
                    .render(matrix, sx, sy, 0f);
        }
    }

    static Color movingBgColor(float timeSec, float phase, float menuAlpha) {
        // Keep the original palette: dark (10,10,15) -> light (40,40,50), but animate the mix factor.
        float tA = 0.5f + 0.5f * (float) Math.sin(timeSec * 0.22f + phase);
        float tB = 0.5f + 0.5f * (float) Math.sin(timeSec * 0.17f + phase * 1.11f + 1.7f);

        float mix = 0.25f + 0.55f * (0.65f * tA + 0.35f * tB);
        if (mix < 0.0f) mix = 0.0f;
        if (mix > 1.0f) mix = 1.0f;

        int r = clamp255((int) (10 + (40 - 10) * mix));
        int g = clamp255((int) (10 + (40 - 10) * mix));
        int b = clamp255((int) (15 + (50 - 15) * mix));
        int a = clamp255((int) (menuAlpha * 255f));

        return new Color(r, g, b, a);
    }

    static int clamp255(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    private void renderAnimatedRedWhiteGradientText(Matrix4f matrix, String text, float x, float y, float size, float menuAlpha, float timeSec) {
        if (text == null || text.isEmpty()) return;

        Color red = ThemeManager.getInstance().getThemeColor();
        Color white = new Color(245, 245, 245);

        int len = text.length();
        float speed = 0.55f;
        float phase = (timeSec * speed) % 1.0f;

        for (int i = 0; i < len; i++) {
            String ch = String.valueOf(text.charAt(i));
            float charX = x + Fonts.BOLD.getWidth(text.substring(0, i), size);

            float u = len <= 1 ? 0.5f : (i / (float) (len - 1));
            float p = (u + phase) % 1.0f;
            float tri = 1.0f - Math.abs(p * 2.0f - 1.0f);

            int a = Math.min(255, Math.max(0, (int) (menuAlpha * 255f)));
            int r = (int) (red.getRed() + (white.getRed() - red.getRed()) * tri);
            int g = (int) (red.getGreen() + (white.getGreen() - red.getGreen()) * tri);
            int b = (int) (red.getBlue() + (white.getBlue() - red.getBlue()) * tri);

            new TextBuilder()
                    .font(Fonts.BOLD.font())
                    .text(ch)
                    .size(size)
                    .color(new Color(r, g, b, a))
                    .smoothness(0.5f)
                    .build()
                    .render(matrix, charX, y, 0f);
        }
    }

    // ===== Internal button class =====
    public static class AnimatedButton {
        private final float x, y, width, height;
        private final String message;
        private final PressAction action;
        private final Animation hoverAnimation;

        public AnimatedButton(float x, float y, float width, float height, String message, PressAction action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.message = message;
            this.action = action;
            this.hoverAnimation = new Animation(200, 1.0, false, Easing.OUT_EXPO);
        }

        public void resetAnimations() {
            hoverAnimation.reset();
        }

        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta, float alpha) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            hoverAnimation.update(hovered);

            float hoverProgress = hoverAnimation.getValue();
            Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

            // Кнопка
            new RectangleBuilder()
                    .size(new SizeState(width, height))
                    .radius(new QuadRadiusState(8))
                    .color(new QuadColorState(new Color(50, 50, 60, (int) (alpha * 150 + hoverProgress * 100))))
                    .build()
                    .render(matrix, x, y, 0f);

            // Текст
            Color base = new Color(220, 220, 240);
            Color hover = new Color(255, 255, 255);
            int r = (int) (base.getRed() + (hover.getRed() - base.getRed()) * hoverProgress);
            int g = (int) (base.getGreen() + (hover.getGreen() - base.getGreen()) * hoverProgress);
            int b = (int) (base.getBlue() + (hover.getBlue() - base.getBlue()) * hoverProgress);

            Color textColor = new Color(r, g, b, (int) (alpha * 255));
            float textX = x + width / 2f - Fonts.REGULAR.getWidth(message, 7f) / 2f;
            float textY = y + height / 2f - Fonts.REGULAR.getHeight(7f) / 2f;

            new TextBuilder()
                    .font(Fonts.REGULAR.font())
                    .text(message)
                    .size(7f)
                    .color(textColor)
                    .smoothness(0.5f)
                    .build()
                    .render(matrix, textX, textY, 0f);
        }

        public boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        public void onPress() {
            action.onPress(this);
        }
    }

    public interface PressAction {
        void onPress(AnimatedButton button);
    }

    private static final class Snowflake {
        float x;
        float y;
        float size;
        float speed;
        float wind;
        float swayAmp;
        float swaySpeed;
        float phase;
        float opacity;

        void respawn(Random random, int width, int height, boolean randomY) {
            this.size = 1.5f + random.nextFloat() * 2.8f;
            this.speed = 18f + random.nextFloat() * 60f;
            this.wind = -10f + random.nextFloat() * 20f;
            this.swayAmp = 1.5f + random.nextFloat() * 6f;
            this.swaySpeed = 0.7f + random.nextFloat() * 1.6f;
            this.phase = (float) (random.nextFloat() * Math.PI * 2.0);
            this.opacity = 0.25f + random.nextFloat() * 0.55f;

            this.x = random.nextFloat() * width;
            if (randomY) {
                this.y = -random.nextFloat() * height;
            } else {
                this.y = -this.size - random.nextFloat() * (height * 0.25f);
            }
        }
    }
}