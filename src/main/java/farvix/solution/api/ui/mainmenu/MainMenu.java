package farvix.solution.api.ui.mainmenu;

import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.render.rect.impl.Blur;
import farvix.solution.api.render.rect.impl.Glass;
import farvix.solution.api.render.rect.impl.Rectangle;
import farvix.solution.api.util.color.FixColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import farvix.solution.api.render.rect.api.DrawEngine;

public class MainMenu extends Screen {

    private final ShaderProgramKey WAVES_KEY = new ShaderProgramKey(
            Identifier.of("solution", "core/waves"), VertexFormats.POSITION, Defines.EMPTY);

    private final Blur BLUR = new Blur();
    private final Glass GLASS = new Glass();
    private final Rectangle RECT = new Rectangle();
    private final float[] hov = new float[8];

    public static int themeIndex = 0;

    // 5 Gradient Theme Colors (Color1 and Color2 for each theme)
    private final float[][] themeColors1 = {
        {1.0f, 1.0f, 1.0f},      // White
        {0.12f, 0.56f, 1.0f},    // Blue
        {0.18f, 0.83f, 0.45f},   // Green
        {1.0f, 0.28f, 0.34f},    // Red
        {0.55f, 0.48f, 0.90f}     // Purple
    };

    private final float[][] themeColors2 = {
        {0.70f, 0.70f, 0.80f},   // White (Silver-Gray)
        {0.00f, 0.47f, 0.90f},   // Blue (Deep)
        {0.15f, 0.65f, 0.35f},   // Green (Emerald)
        {0.92f, 0.18f, 0.02f},   // Red (Crimson)
        {0.44f, 0.44f, 0.83f}    // Purple (Deep Lavender)
    };
    private long openTime;
    private long lastTime;

    public MainMenu() {
        super(Text.literal("Solution Visual"));
    }

    @Override
    protected void init() {
        super.init();
        openTime = System.currentTimeMillis();
        lastTime = openTime;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        this.height = MinecraftClient.getInstance().getWindow().getScaledHeight();

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTime) / 1000f, 0.1f);
        lastTime = now;

        float fade = Math.min(1f, (now - openTime) / 700f);
        float speed = 8f;

        // ── Pure Black Background ─────────────────────────────────────────────
        ctx.fill(0, 0, width, height, 0xFF000000);
        ctx.draw();

        // Setup blur framebuffer
        BLUR.setup();

        // ── 2 Waves Ambient Center Background Effect (White, Blurred & Glowing via GPU Shader) ──
        if (fade > 0.001f) {
            ShaderProgram shader = RenderSystem.setShader(WAVES_KEY);
            if (shader != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
                RenderSystem.disableCull();

                MinecraftClient mc = MinecraftClient.getInstance();
                shader.getUniformOrDefault("Resolution").set((float) mc.getWindow().getFramebufferWidth(), (float) mc.getWindow().getFramebufferHeight());
                shader.getUniformOrDefault("Time").set((now - openTime) / 1000f);
                shader.getUniformOrDefault("Fade").set(fade);

                // Set theme colors from the selected gradient theme
                float[] col1 = themeColors1[themeIndex];
                float[] col2 = themeColors2[themeIndex];
                shader.getUniformOrDefault("Color1").set(col1[0], col1[1], col1[2]);
                shader.getUniformOrDefault("Color2").set(col2[0], col2[1], col2[2]);

                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
                DrawEngine.quad(ctx.getMatrices().peek().getPositionMatrix(), buffer, 0f, 0f, width, height);

                BufferRenderer.drawWithGlobalProgram(buffer.end());
                RenderSystem.disableBlend();
            }
        }

        // Flush wave rendering to the screen framebuffer
        ctx.draw();

        // Setup glass framebuffer (now it captures the black background AND the waves!)
        GLASS.setup();

        // ── Top Left Static Theme Panel (No Outline) ──────────────────────────
        GLASS.render(ShapeProperties.create(ctx.getMatrices(), 10f, 10f, 88f, 24f)
                .round(12f)
                .softness(3.5f)
                .thickness(0.0f)
                .color(new FixColor(15, 15, 20, (int)(204 * fade)).getRGB())
                .build());

        // Draw 5 Symmetrical Theme Circles
        for (int i = 0; i < 5; i++) {
            float cx = 22f + i * 16f;
            float cy = 22f; // Y center (centered vertically in 24px panel)

            boolean active = (i == themeIndex);
            int circleColor = new FixColor(
                    (int)(themeColors1[i][0] * 255),
                    (int)(themeColors1[i][1] * 255),
                    (int)(themeColors1[i][2] * 255),
                    (int)(255 * fade)
            ).getRGB();

            float r = active ? 6f : 5f;

            // Draw main color circle
            BLUR.render(ShapeProperties.create(ctx.getMatrices(), cx - r, cy - r, r * 2f, r * 2f)
                    .round(r)
                    .softness(1.0f)
                    .color(circleColor)
                    .build());

            // Draw premium central dot if active (black dot for white theme, white dot for other themes)
            if (active) {
                int dotColor = (i == 0)
                        ? new FixColor(15, 15, 20, (int)(255 * fade)).getRGB()
                        : new FixColor(255, 255, 255, (int)(255 * fade)).getRGB();

                BLUR.render(ShapeProperties.create(ctx.getMatrices(), cx - 1.5f, cy - 1.5f, 3f, 3f)
                        .round(1.5f)
                        .softness(0.5f)
                        .color(dotColor)
                        .build());
            }
        }

        // ── Layout Dimensions ─────────────────────────────────────────────────
        float cx = width / 2f;
        float btnW = 260f, btnH = 24f, btnGap = 8f;
        float btnX = cx - btnW / 2f;
        float btnStartY = height / 2f - (btnH * 3 + btnGap * 2) / 2f + 15f;

        // ── Greeting ──────────────────────────────────────────────────────────
        String greeting = "Здравствуйте, ";
        String username = getUsername();
        float gw = Fonts.DEFAULT.get(13).getStringWidth(greeting);
        float uw = Fonts.SEMIBOLD.get(13).getStringWidth(username);
        float gx = cx - (gw + uw) / 2f;
        float gy = btnStartY - 24;

        Fonts.DEFAULT.get(13).drawString(ctx.getMatrices(), greeting, gx, gy,
                new FixColor(200, 200, 220, (int) (255 * fade)).getRGB());
        Fonts.SEMIBOLD.get(13).drawString(ctx.getMatrices(), username, gx + gw, gy,
                new FixColor(255, 255, 255, (int) (255 * fade)).getRGB());

        // ── Main Buttons ──────────────────────────────────────────────────────
        String[] mainLabels = {"Одиночная игра", "Сетевая игра", "Смена аккаунта"};
        boolean[] accented = {false, false, false};

        for (int i = 0; i < 3; i++) {
            float by = btnStartY + i * (btnH + btnGap);
            boolean h = inBox(mouseX, mouseY, btnX, by, btnW, btnH);
            hov[i] = approach(hov[i], h ? 1f : 0f, dt * speed);
            renderBtn(ctx, btnX, by, btnW, btnH, mainLabels[i], hov[i], accented[i], fade, mouseX, mouseY);
        }

        // ── Symmetrical Small Buttons (Options & Exit) ───────────────────────
        String[] smallLabels = {"Настройки", "Выход"};
        float smW = 126f, smH = 22f, smGap = 8f;
        float smX0 = btnX;
        float smY = btnStartY + 3 * (btnH + btnGap) + 4f;

        for (int i = 0; i < 2; i++) {
            float sx = smX0 + i * (smW + smGap);
            boolean h = inBox(mouseX, mouseY, sx, smY, smW, smH);
            hov[3 + i] = approach(hov[3 + i], h ? 1f : 0f, dt * speed);
            renderBtn(ctx, sx, smY, smW, smH, smallLabels[i], hov[3 + i], false, fade, mouseX, mouseY);
        }

        // ── Symmetrical Links (Discord & Telegram) ──────────────────────────
        String[] lnkLabels = {"Дискорд", "Телеграм"};
        float lnkW = 126f, lnkH = 22f, lnkGap = 8f;
        float lnkX0 = btnX;
        float lnkY = smY + smH + btnGap;

        for (int i = 0; i < 2; i++) {
            float lx = lnkX0 + i * (lnkW + lnkGap);
            boolean h = inBox(mouseX, mouseY, lx, lnkY, lnkW, lnkH);
            hov[5 + i] = approach(hov[5 + i], h ? 1f : 0f, dt * speed);
            renderBtn(ctx, lx, lnkY, lnkW, lnkH, lnkLabels[i], hov[5 + i], false, fade, mouseX, mouseY);
        }

        // ── Changelog (Top Right) ─────────────────────────────────────────────
        if (fade > 0.01f && width >= 550) {
            String[] changelog = {
                "§a[ + ] §rДобавлено так что теперь видно все бинды в HotKeys",
                "§a[ + ] §rСделали по стандарту очень легкие настройки,",
                "        §rно если вы хотите расширенную версию настроек,",
                "        §rкоторая будет ну оооооочень большая, то можете",
                "        §rнажать на кнопку в ClickGui в самом верху",
                "        §rменюшки (надпись обозначает статус)",
                "",
                "§e[ / ] §rПеределан полностью с нуля ClickGui",
                "§e[ / ] §rПеределаны настройки функций и выбор цвета в них"
            };

            int fontSize = 9;
            float lineSpacing = 9.5f;
            
            if (width < 600f) {
                fontSize = 6;
                lineSpacing = 6.5f;
            } else if (width < 750f || height < 450f) {
                fontSize = 7;
                lineSpacing = 7.5f;
            } else if (width < 950f || height < 550f) {
                fontSize = 8;
                lineSpacing = 8.5f;
            } else if (height >= 750f) {
                fontSize = 10;
                lineSpacing = 11.0f;
            }

            float maxW = 0f;
            for (String line : changelog) {
                float w = Fonts.DEFAULT.get(fontSize).getStringWidth(line);
                if (w > maxW) maxW = w;
            }

            float clX = width - maxW - 15f;
            float clY = 15f;
            int clColor = new FixColor(190, 190, 200, (int) (215 * fade)).getRGB();

            for (String line : changelog) {
                if (!line.isEmpty()) {
                    Fonts.DEFAULT.get(fontSize).drawString(ctx.getMatrices(), line, clX, clY, clColor);
                }
                clY += lineSpacing;
            }
        }

        // ── Footer ────────────────────────────────────────────────────────────
        String ver = "Solution Visual 3.6  \u00b7  Minecraft 1.21.4";
        Fonts.DEFAULT.get(10).drawString(ctx.getMatrices(), ver, 8f, height - 14f,
                new FixColor(65, 65, 85, (int) (150 * fade)).getRGB());
    }

    private void renderBtn(DrawContext ctx,
                           float x, float y, float w, float h,
                           String label, float hover, boolean accented, float fade,
                           int mouseX, int mouseY) {
        int bgA = (int) ((204 + hover * 35) * fade);
        int bg = accented
                ? new FixColor(255, 255, 255, bgA).getRGB()
                : new FixColor(15, 15, 20, bgA).getRGB();

        // Soft-edged glass background (thickness = 0, no outline)
        GLASS.render(ShapeProperties.create(ctx.getMatrices(), x, y, w, h)
                .round(h / 2f)
                .softness(3.5f)
                .thickness(0.0f)
                .color(bg)
                .build());

        // Accent typography
        int tc = accented
                ? new FixColor(10, 10, 15, (int) (255 * fade)).getRGB()
                : new FixColor(230, 230, 240, (int) ((200 + hover * 55) * fade)).getRGB();

        float tw = Fonts.SEMIBOLD.get(16).getStringWidth(label);
        float th = Fonts.SEMIBOLD.get(16).getStringHeight(label);
        float tx = x + (w - tw) / 2f;
        float ty = y + (h - th / 2f) / 2f + 3f;

        Fonts.SEMIBOLD.get(16).drawString(ctx.getMatrices(), label, tx, ty, tc);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Theme circles click (permanently active in the static panel)
        for (int i = 0; i < 5; i++) {
            float cx = 22f + i * 16f;
            // hit box: diameter 10, so 14x14 area centered around the circle
            if (inBox(mx, my, cx - 7f, 22f - 7f, 14f, 14f)) {
                themeIndex = i;
                try {
                    farvix.solution.Client.getInstance().getConfigManager().saveConfig("_autosave");
                } catch (Throwable ignored) {}
                return true;
            }
        }
        float cx = width / 2f;
        float btnW = 260f, btnH = 24f, btnGap = 8f;
        float btnX = cx - btnW / 2f;
        float btnStartY = height / 2f - (btnH * 3 + btnGap * 2) / 2f + 15f;

        // Main buttons
        if (inBox(mx, my, btnX, btnStartY, btnW, btnH))
            MinecraftClient.getInstance().setScreen(new SelectWorldScreen(this));
        else if (inBox(mx, my, btnX, btnStartY + btnH + btnGap, btnW, btnH))
            MinecraftClient.getInstance().setScreen(new MultiplayerScreen(this));
        else if (inBox(mx, my, btnX, btnStartY + (btnH + btnGap) * 2, btnW, btnH))
            MinecraftClient.getInstance().setScreen(new AccountSwitcherScreen(this));

        // Options & Exit
        float smW = 126f, smH = 22f, smGap = 8f;
        float smX0 = btnX;
        float smY = btnStartY + 3 * (btnH + btnGap) + 4f;

        if (inBox(mx, my, smX0, smY, smW, smH))
            MinecraftClient.getInstance().setScreen(new OptionsScreen(this, MinecraftClient.getInstance().options));
        else if (inBox(mx, my, smX0 + smW + smGap, smY, smW, smH))
            MinecraftClient.getInstance().scheduleStop();

        // Discord & Telegram Links
        float lnkW = 126f, lnkH = 22f, lnkGap = 8f;
        float lnkX0 = btnX;
        float lnkY = smY + smH + btnGap;

        if (inBox(mx, my, lnkX0, lnkY, lnkW, lnkH))
            openUrl("https://discord.gg/jP2GKAMxZ9");
        else if (inBox(mx, my, lnkX0 + lnkW + lnkGap, lnkY, lnkW, lnkH))
            openUrl("https://t.me/SolutionDevLogs");

        return super.mouseClicked(mx, my, btn);
    }

    private float approach(float a, float b, float step) {
        if (a < b) return Math.min(a + step, b);
        if (a > b) return Math.max(a - step, b);
        return b;
    }

    private void openUrl(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Throwable t) {
            System.out.println("[URL] Failed to open: " + url + " — " + t.getMessage());
        }
    }

    private boolean inBox(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String getUsername() {
        MinecraftClient mc2 = MinecraftClient.getInstance();
        return mc2.getSession() != null ? mc2.getSession().getUsername() : "Player";
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
