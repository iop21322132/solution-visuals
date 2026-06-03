package farvix.solution.api.ui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.render.rect.impl.Blur;
import farvix.solution.api.render.rect.impl.Glass;
import farvix.solution.api.render.rect.impl.Rectangle;
import farvix.solution.api.render.rect.api.DrawEngine;
import farvix.solution.api.util.color.FixColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AccountSwitcherScreen extends Screen {

    private final ShaderProgramKey WAVES_KEY = new ShaderProgramKey(
            Identifier.of("solution", "core/waves"), VertexFormats.POSITION, Defines.EMPTY);

    private final Screen parent;
    private final Blur blur = new Blur();
    private final Glass glass = new Glass();
    private final Rectangle rect = new Rectangle();

    // ── Saved accounts ────────────────────────────────────────────────────────
    private static final List<String> savedNicks = new ArrayList<>();
    private static final File ACCOUNTS_FILE = new File(MinecraftClient.getInstance().runDirectory, "solution_accounts.txt");

    // ── Input field ───────────────────────────────────────────────────────────
    private String inputText = "";
    private boolean inputFocused = false;

    // ── Status ────────────────────────────────────────────────────────────────
    private String statusMsg = "";
    private int statusColor = 0xFFAAAAAA;
    private long statusTime = 0;

    // ── Hover ─────────────────────────────────────────────────────────────────
    private float hovCreate = 0f, hovHome = 0f;

    // ── Fade animation ────────────────────────────────────────────────────────
    private float fadeIn = 0f;
    private boolean closing = false;
    private Screen nextScreen = null;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final float CARD_W = 200f, CARD_H = 50f;
    private static final float CARD_GAP = 10f;
    private static final float COLS = 3;
    private static final float MAX_VISIBLE_ROWS = 5;
    private static final float SKIN_SIZE = 36f;
    private static final float PAD = 8f;

    // ── Scroll ────────────────────────────────────────────────────────────────
    private float scrollOffset = 0f;
    private float scrollTarget = 0f;

    private long openTime;
    private long lastTime;

    private final float[][] themeColors1 = {
        {1.0f, 1.0f, 1.0f},
        {0.12f, 0.56f, 1.0f},
        {0.18f, 0.83f, 0.45f},
        {1.0f, 0.28f, 0.34f},
        {0.55f, 0.48f, 0.90f}
    };

    private final float[][] themeColors2 = {
        {0.70f, 0.70f, 0.80f},
        {0.00f, 0.47f, 0.90f},
        {0.15f, 0.65f, 0.35f},
        {0.92f, 0.18f, 0.02f},
        {0.44f, 0.44f, 0.83f}
    };

    public AccountSwitcherScreen(Screen parent) {
        super(Text.literal("Accounts"));
        this.parent = parent;
        
        loadAccounts();
    }

    @Override
    protected void init() {
        super.init();
        inputText = "";
        openTime = System.currentTimeMillis();
        lastTime = openTime;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTime) / 1000f, 0.1f);
        lastTime = now;

        MinecraftClient mc = MinecraftClient.getInstance();
        float cx = width / 2f;

        if (closing) {
            fadeIn = Math.max(0f, fadeIn - dt * 4f);
            if (fadeIn <= 0f && nextScreen != null) {
                mc.setScreen(nextScreen);
                return;
            }
        } else {
            fadeIn = Math.min(1f, fadeIn + dt * 4f);
        }
        float fade = fadeIn;

        ctx.fill(0, 0, width, height, 0xFF000000);
        ctx.draw();

        // Setup blur framebuffer
        blur.setup();

        // ── 2 Waves Ambient Background Effect ──
        if (fade > 0.001f) {
            ShaderProgram shader = RenderSystem.setShader(WAVES_KEY);
            if (shader != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
                RenderSystem.disableCull();

                shader.getUniformOrDefault("Resolution").set((float) mc.getWindow().getFramebufferWidth(), (float) mc.getWindow().getFramebufferHeight());
                shader.getUniformOrDefault("Time").set((now - openTime) / 1000f);
                shader.getUniformOrDefault("Fade").set(fade);

                // Set theme colors from the selected gradient theme
                float[] col1 = themeColors1[MainMenu.themeIndex];
                float[] col2 = themeColors2[MainMenu.themeIndex];
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
        glass.setup();

        // ── Top Left Static Theme Panel (No Outline) ──────────────────────────
        glass.render(ShapeProperties.create(ctx.getMatrices(), 10f, 10f, 88f, 24f)
                .round(12f)
                .softness(3.5f)
                .thickness(0.0f)
                .color(new FixColor(15, 15, 20, (int)(204 * fade)).getRGB())
                .build());

        for (int i = 0; i < 5; i++) {
            float cxCircle = 22f + i * 16f;
            float cyCircle = 22f;

            boolean active = (i == MainMenu.themeIndex);
            int circleColor = new FixColor(
                    (int)(themeColors1[i][0] * 255),
                    (int)(themeColors1[i][1] * 255),
                    (int)(themeColors1[i][2] * 255),
                    (int)(255 * fade)
            ).getRGB();

            float r = active ? 6f : 5f;

            blur.render(ShapeProperties.create(ctx.getMatrices(), cxCircle - r, cyCircle - r, r * 2f, r * 2f)
                    .round(r)
                    .softness(1.0f)
                    .color(circleColor)
                    .build());

            if (active) {
                int dotColor = (i == 0)
                        ? new FixColor(15, 15, 20, (int)(255 * fade)).getRGB()
                        : new FixColor(255, 255, 255, (int)(255 * fade)).getRGB();

                blur.render(ShapeProperties.create(ctx.getMatrices(), cxCircle - 1.5f, cyCircle - 1.5f, 3f, 3f)
                        .round(1.5f)
                        .softness(0.5f)
                        .color(dotColor)
                        .build());
            }
        }

        float headerY = 32f;
        int iconCol = new FixColor(255, 255, 255, (int)(180 * fade)).getRGB();
        ctx.fill((int)(cx - 6), (int)(headerY), (int)(cx + 6), (int)(headerY + 10), iconCol);
        ctx.fill((int)(cx - 9), (int)(headerY + 11), (int)(cx + 9), (int)(headerY + 20), iconCol);

        String title = "Смена аккаунта";
        float tw = Fonts.SEMIBOLD.get(20).getStringWidth(title);
        Fonts.SEMIBOLD.get(20).drawString(ctx.getMatrices(), title, cx - tw / 2f, headerY + 26f,
                new FixColor(255, 255, 255, (int)(255 * fade)).getRGB());

        String sub = "Создайте или выберите существующий аккаунт";
        float sw = Fonts.DEFAULT.get(13).getStringWidth(sub);
        Fonts.DEFAULT.get(13).drawString(ctx.getMatrices(), sub, cx - sw / 2f, headerY + 44f,
                new FixColor(180, 180, 195, (int)(180 * fade)).getRGB());

        float gridW = COLS * CARD_W + (COLS - 1) * CARD_GAP;
        float gridX = cx - gridW / 2f;
        float gridY = headerY + 68f;

        scrollOffset += (scrollTarget - scrollOffset) * 0.2f;

        float visibleH = MAX_VISIBLE_ROWS * (CARD_H + CARD_GAP);
        float inputY2 = height - 100f;
        float availH = inputY2 - gridY - 10f;
        float gridVisibleH = Math.min(visibleH, availH);

        int totalRows = (int) Math.ceil((double) savedNicks.size() / COLS);
        float totalH = totalRows * (CARD_H + CARD_GAP);
        float maxScroll = Math.max(0, totalH - gridVisibleH);
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));

        ctx.enableScissor((int) gridX - 5, (int) gridY,
                (int)(gridX + gridW + 5), (int)(gridY + gridVisibleH));

        for (int i = 0; i < savedNicks.size(); i++) {
            int col = i % (int)COLS;
            int row = i / (int)COLS;
            float cx2 = gridX + col * (CARD_W + CARD_GAP);
            float cy2 = gridY + row * (CARD_H + CARD_GAP) - scrollOffset;
            if (cy2 + CARD_H < gridY || cy2 > gridY + gridVisibleH) continue;
            renderAccountCard(ctx, cx2, cy2, savedNicks.get(i), mouseX, mouseY, i, fade);
        }

        ctx.disableScissor();

        if (maxScroll > 0) {
            float sbX = gridX + gridW + 6;
            float sbH = gridVisibleH;
            float thumbH = Math.max(20, sbH * (gridVisibleH / totalH));
            float thumbY = gridY + (scrollOffset / maxScroll) * (sbH - thumbH);
            glass.render(ShapeProperties.create(ctx.getMatrices(), sbX, gridY, 4, sbH)
                    .round(2).softness(3.5f)
                    .color(new FixColor(15, 15, 20, (int)(80 * fade)).getRGB()).build());
            glass.render(ShapeProperties.create(ctx.getMatrices(), sbX, thumbY, 4, thumbH)
                    .round(2).softness(3.5f)
                    .color(new FixColor(255, 255, 255, (int)(150 * fade)).getRGB()).build());
        }

        float inputY = height - 100f;
        float inputW = 280f, inputH = 36f;
        float inputX = cx - inputW / 2f - 60f;

        int fieldBg = inputFocused
                ? new FixColor(30, 30, 42, (int)(220 * fade)).getRGB()
                : new FixColor(15, 15, 20, (int)(204 * fade)).getRGB();
        int fieldBr = inputFocused
                ? new FixColor(255, 255, 255, (int)(180 * fade)).getRGB()
                : new FixColor(255, 255, 255, (int)(30 * fade)).getRGB();

        // Soft-edged glass body (thickness = 0, no outline)
        glass.render(ShapeProperties.create(ctx.getMatrices(), inputX, inputY, inputW, inputH)
                .round(18f)
                .softness(3.5f)
                .thickness(0.0f)
                .color(fieldBg)
                .build());

        String display = inputText.isEmpty() && !inputFocused ? "Введите никнейм..." : inputText;
        boolean showCursor = inputFocused && (System.currentTimeMillis() % 1000 < 500);
        if (showCursor) display += "|";
        int textCol = inputText.isEmpty() && !inputFocused
                ? new FixColor(110, 110, 130, (int)(180 * fade)).getRGB()
                : new FixColor(230, 230, 245, (int)(255 * fade)).getRGB();

        float dw = Fonts.SEMIBOLD.get(14).getStringWidth(display);
        float dh = Fonts.SEMIBOLD.get(14).getStringHeight(display) / 2f;
        Fonts.SEMIBOLD.get(14).drawString(ctx.getMatrices(), display, inputX + 18f, inputY + (inputH - dh) / 2f + 3f, textCol);

        float btnW = 110f, btnH = inputH;
        float btnX = inputX + inputW + 10f;
        boolean hC = inBox(mouseX, mouseY, btnX, inputY, btnW, btnH);
        hovCreate = approach(hovCreate, hC ? 1f : 0f, dt * 8f);

        int btnBgA = (int) ((204 + hovCreate * 35) * fade);
        int btnBg = new FixColor(15, 15, 20, btnBgA).getRGB();
        int btnBr = new FixColor(255, 255, 255, (int) ((20 + hovCreate * 30) * fade)).getRGB();

        // Soft-edged glass body (thickness = 0, no outline)
        glass.render(ShapeProperties.create(ctx.getMatrices(), btnX, inputY, btnW, btnH)
                .round(btnH / 2f)
                .softness(3.5f)
                .thickness(0.0f)
                .color(btnBg)
                .build());

        String createLabel = "Создать";
        float clw = Fonts.SEMIBOLD.get(14).getStringWidth(createLabel);
        float clh = Fonts.SEMIBOLD.get(14).getStringHeight(createLabel);
        Fonts.SEMIBOLD.get(14).drawString(ctx.getMatrices(), createLabel, btnX + btnW / 2f - clw / 2f,
                inputY + (btnH - clh / 2f) / 2f + 3f,
                new FixColor(230, 230, 240, (int) ((200 + hovCreate * 55) * fade)).getRGB());

        float homeY = height - 52f;
        float homeBtnW = 120f, homeBtnH = 24f;
        float homeBtnX = cx - homeBtnW / 2f;
        boolean hH = inBox(mouseX, mouseY, homeBtnX, homeY, homeBtnW, homeBtnH);
        hovHome = approach(hovHome, hH ? 1f : 0f, dt * 8f);

        int homeBgA = (int) ((204 + hovHome * 35) * fade);
        int homeBg = new FixColor(15, 15, 20, homeBgA).getRGB();
        int homeBr = new FixColor(255, 255, 255, (int) ((20 + hovHome * 30) * fade)).getRGB();

        // Soft-edged glass body (thickness = 0, no outline)
        glass.render(ShapeProperties.create(ctx.getMatrices(), homeBtnX, homeY, homeBtnW, homeBtnH)
                .round(homeBtnH / 2f)
                .softness(3.5f)
                .thickness(0.0f)
                .color(homeBg)
                .build());

        String homeLabel = "Назад";
        float hlw = Fonts.SEMIBOLD.get(14).getStringWidth(homeLabel);
        float hlh = Fonts.SEMIBOLD.get(14).getStringHeight(homeLabel);
        Fonts.SEMIBOLD.get(14).drawString(ctx.getMatrices(), homeLabel, cx - hlw / 2f, homeY + (homeBtnH - hlh / 2f) / 2f + 3f,
                new FixColor(230, 230, 240, (int)((200 + hovHome * 55) * fade)).getRGB());

        if (!statusMsg.isEmpty() && System.currentTimeMillis() - statusTime < 3000) {
            float stw = Fonts.DEFAULT.get(12).getStringWidth(statusMsg);
            Fonts.DEFAULT.get(12).drawString(ctx.getMatrices(), statusMsg, cx - stw / 2f, inputY - 20f, statusColor);
        }
    }

    private void renderAccountCard(DrawContext ctx, float x, float y, String nick, int mouseX, int mouseY, int idx, float fade) {
        boolean hovered = inBox(mouseX, mouseY, x, y, CARD_W, CARD_H);
        boolean isCurrent = nick.equals(getCurrentNick());

        int bg, border;
        if (isCurrent) {
            float[] col1 = themeColors1[MainMenu.themeIndex];
            bg = new FixColor((int)(col1[0] * 255), (int)(col1[1] * 255), (int)(col1[2] * 255), (int)(204 * fade)).getRGB();
            border = new FixColor((int)(col1[0] * 255), (int)(col1[1] * 255), (int)(col1[2] * 255), (int)(180 * fade)).getRGB();
        } else {
            bg = new FixColor(15, 15, 20, (int)(204 * fade)).getRGB();
            border = new FixColor(255, 255, 255, (int)((hovered ? 60 : 25) * fade)).getRGB();
        }

        // Soft-edged glass body (thickness = 0, no outline)
        glass.render(ShapeProperties.create(ctx.getMatrices(), x, y, CARD_W, CARD_H)
                .round(12f)
                .softness(3.5f)
                .thickness(0.0f)
                .color(bg)
                .build());

        Identifier skin = getSkinTexture(nick);
        float skinX = x + PAD;
        float skinY = y + (CARD_H - SKIN_SIZE) / 2f;

        if (skin != null) {
            drawSkinHead(ctx, skin, skinX, skinY, SKIN_SIZE, fade);
        } else {
            glass.render(ShapeProperties.create(ctx.getMatrices(), skinX, skinY, SKIN_SIZE, SKIN_SIZE)
                    .round(6f)
                    .softness(3.5f)
                    .color(new FixColor(40, 40, 50, (int)(200 * fade)).getRGB())
                    .build());
            String letter = nick.substring(0, 1).toUpperCase();
            float lw = Fonts.SEMIBOLD.get(16).getStringWidth(letter);
            float lh = Fonts.SEMIBOLD.get(16).getStringHeight(letter) / 2f;
            Fonts.SEMIBOLD.get(16).drawString(ctx.getMatrices(), letter,
                    skinX + SKIN_SIZE / 2f - lw / 2f,
                    skinY + (SKIN_SIZE - lh) / 2f + 3f,
                    new FixColor(255, 255, 255, (int)(255 * fade)).getRGB());
        }

        int nickColor = isCurrent
                ? new FixColor(255, 255, 255, (int)(255 * fade)).getRGB()
                : new FixColor(210, 210, 225, (int)(210 * fade)).getRGB();
        float nickH = Fonts.SEMIBOLD.get(14).getStringHeight(nick) / 2f;
        Fonts.SEMIBOLD.get(14).drawString(ctx.getMatrices(), nick,
                skinX + SKIN_SIZE + PAD,
                y + (CARD_H - nickH) / 2f + 3f,
                nickColor);

        float xBtnSize = 14f;
        float xBtnX = x + CARD_W - xBtnSize - 6;
        float xBtnY = y + 6;
        boolean hX = inBox(mouseX, mouseY, xBtnX, xBtnY, xBtnSize, xBtnSize);

        // Draw premium dark semi-transparent circular background
        int circleColor = new FixColor(10, 10, 15, (int) ((hX ? 220 : 150) * fade)).getRGB();
        glass.render(ShapeProperties.create(ctx.getMatrices(), xBtnX, xBtnY, xBtnSize, xBtnSize)
                .round(xBtnSize / 2f)
                .softness(2f)
                .color(circleColor)
                .build());

        int xCol = hX ? new FixColor(255, 90, 90, (int)(255 * fade)).getRGB()
                      : new FixColor(220, 220, 225, (int)(180 * fade)).getRGB();

        float xw = Fonts.SEMIBOLD.get(22).getStringWidth("×");
        float xh = Fonts.SEMIBOLD.get(22).getStringHeight("×") / 2f;
        Fonts.SEMIBOLD.get(22).drawString(ctx.getMatrices(), "×", xBtnX + (xBtnSize - xw) / 2f, xBtnY + (xBtnSize - xh) / 2f + 1f, xCol);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float cx = width / 2f;

        for (int i = 0; i < 5; i++) {
            float cxCircle = 22f + i * 16f;
            if (inBox(mx, my, cxCircle - 7f, 22f - 7f, 14f, 14f)) {
                MainMenu.themeIndex = i;
                try {
                    farvix.solution.Client.getInstance().getConfigManager().saveConfig("_autosave");
                } catch (Throwable ignored) {}
                return true;
            }
        }

        float inputY = height - 100f;
        float inputW = 280f, inputH = 36f;
        float inputX = cx - inputW / 2f - 60f;
        inputFocused = inBox(mx, my, inputX, inputY, inputW, inputH);

        // Create button
        float btnW = 110f;
        float btnX = inputX + inputW + 10f;
        if (inBox(mx, my, btnX, inputY, btnW, inputH)) {
            createAccount();
            return true;
        }

        // Home button
        float homeY = height - 52f;
        float homeBtnW = 100f, homeBtnH = 28f;
        float homeBtnX = cx - homeBtnW / 2f;
        if (inBox(mx, my, homeBtnX, homeY, homeBtnW, homeBtnH)) {
            navigateTo(parent);
            return true;
        }

        // Account cards
        float gridW = COLS * CARD_W + (COLS - 1) * CARD_GAP;
        float gridX = cx - gridW / 2f;
        float headerY = 40f;
        float gridY = headerY + 68f;

        float visibleH = MAX_VISIBLE_ROWS * (CARD_H + CARD_GAP);
        float inputY2 = height - 100f;
        float availH = inputY2 - gridY - 10f;
        float gridVisibleH = Math.min(visibleH, availH);

        for (int i = 0; i < savedNicks.size(); i++) {
            int col = i % (int)COLS;
            int row = i / (int)COLS;
            float cx2 = gridX + col * (CARD_W + CARD_GAP);
            float cy2 = gridY + row * (CARD_H + CARD_GAP) - scrollOffset;

            // Only click if it's within the visible vertical window
            if (cy2 + CARD_H < gridY || cy2 > gridY + gridVisibleH) continue;

            // X button
            float xBtnSize = 14f;
            float xBtnX = cx2 + CARD_W - xBtnSize - 6;
            float xBtnY = cy2 + 6;
            if (inBox(mx, my, xBtnX, xBtnY, xBtnSize, xBtnSize)) {
                savedNicks.remove(i);
                saveAccounts(); // Сохраняем после удаления
                return true;
            }

            // Card click = switch to this account
            if (inBox(mx, my, cx2, cy2, CARD_W, CARD_H)) {
                switchToNick(savedNicks.get(i), mc);
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputFocused) {
            if (keyCode == 259 && !inputText.isEmpty()) {
                inputText = inputText.substring(0, inputText.length() - 1);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { createAccount(); return true; }
            if (keyCode == 256) { inputFocused = false; return true; }
        } else if (keyCode == 256) {
            // ESC when not in input → go back to parent
            navigateTo(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (inputFocused && inputText.length() < 16 && isValidChar(chr)) {
            inputText += chr;
            return true;
        }
        return false;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void drawSkinHead(DrawContext ctx, Identifier tex, float x, float y, float size, float alpha) {
        float S = 64f;
        float u0 = 8/S, v0 = 8/S, u1 = 16/S, v1 = 16/S;
        int rgb = new FixColor(255, 255, 255, (int)(255 * alpha)).getRGB();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);

        var tess = net.minecraft.client.render.Tessellator.getInstance();
        var buf  = tess.begin(
                net.minecraft.client.render.VertexFormat.DrawMode.QUADS,
                net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR);
        org.joml.Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        buf.vertex(m, x,        y,        0).texture(u0, v0).color(rgb);
        buf.vertex(m, x,        y + size, 0).texture(u0, v1).color(rgb);
        buf.vertex(m, x + size, y + size, 0).texture(u1, v1).color(rgb);
        buf.vertex(m, x + size, y,        0).texture(u1, v0).color(rgb);
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }

    private void navigateTo(Screen screen) {
        closing = true;
        nextScreen = screen;
    }

    private void createAccount() {
        String nick = inputText.trim();
        if (nick.length() < 3) {
            status("Ник слишком короткий!", 0xFFFF6060);
            return;
        }
        if (!savedNicks.contains(nick)) {
            savedNicks.add(nick);
            saveAccounts(); // Сохраняем после добавления
        }
        switchToNick(nick, MinecraftClient.getInstance());
        inputText = "";
        inputFocused = false;
    }

    private void switchToNick(String nick, MinecraftClient mc) {
        try {
            Object currentSession = mc.getSession();
            Class<?> sessionClass = currentSession.getClass();

            java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8));

            java.lang.reflect.Constructor<?> ctor = sessionClass.getDeclaredConstructors()[0];
            ctor.setAccessible(true);

            Class<?> param3 = ctor.getParameterTypes()[ctor.getParameterTypes().length - 1];
            Object lastArg;
            if (param3 == String.class) {
                lastArg = "legacy";
            } else {
                Object[] consts = param3.getEnumConstants();
                lastArg = consts[consts.length - 1];
                for (Object e : consts) {
                    if (e.toString().toLowerCase().contains("legacy")) { lastArg = e; break; }
                }
            }

            // Build args dynamically based on constructor param count
            Object[] args;
            int paramCount = ctor.getParameterTypes().length;
            if (paramCount == 4) {
                args = new Object[]{nick, uuid, "0", lastArg};
            } else if (paramCount == 6) {
                args = new Object[]{nick, uuid, "0",
                        java.util.Optional.empty(), java.util.Optional.empty(), lastArg};
            } else {
                args = new Object[]{nick, uuid.toString(), "0", lastArg};
            }

            Object newSession = ctor.newInstance(args);

            java.lang.reflect.Field sessionField = null;
            for (java.lang.reflect.Field f : MinecraftClient.class.getDeclaredFields()) {
                if (f.getType() == sessionClass) { sessionField = f; break; }
            }
            if (sessionField == null) throw new Exception("session field not found");
            sessionField.setAccessible(true);
            sessionField.set(mc, newSession);

            // Добавляем в список если ещё нет
            if (!savedNicks.contains(nick)) {
                savedNicks.add(nick);
                saveAccounts();
            }

            status("Успешный вход: " + nick, 0xFF60DD60);
        } catch (Exception e) {
            status("Ошибка: " + e.getMessage(), 0xFFFF6060);
        }
    }

    private Identifier getSkinTexture(String nick) {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Current player — use their actual skin
        if (nick.equalsIgnoreCase(getCurrentNick()) && mc.player != null) {
            return mc.player.getSkinTextures().texture();
        }

        // Try player list (multiplayer)
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile().getName().equalsIgnoreCase(nick)) {
                    return entry.getSkinTextures().texture();
                }
            }
        }

        // Fallback: Steve skin (always available)
        return Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    }

    private String getCurrentNick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.getSession() != null ? mc.getSession().getUsername() : "";
    }

    private void status(String msg, int color) {
        statusMsg = msg;
        statusColor = color;
        statusTime = System.currentTimeMillis();
    }

    private boolean isValidChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9') || c == '_';
    }

    private boolean inBox(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private float lerp(float a, float b, float t) { return a + (b - a) * Math.min(t, 1f); }

    private float approach(float a, float b, float step) {
        if (a < b) return Math.min(a + step, b);
        if (a > b) return Math.max(a - step, b);
        return b;
    }

    // ── Save/Load accounts ────────────────────────────────────────────────────
    
    /**
     * Сохраняет список аккаунтов в файл
     */
    private static void saveAccounts() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ACCOUNTS_FILE))) {
            for (String nick : savedNicks) {
                writer.write(nick);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save accounts: " + e.getMessage());
        }
    }
    
    /**
     * Загружает список аккаунтов из файла
     */
    private static void loadAccounts() {
        if (!ACCOUNTS_FILE.exists()) {
            return;
        }
        
        savedNicks.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(ACCOUNTS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !savedNicks.contains(line)) {
                    savedNicks.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load accounts: " + e.getMessage());
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}
    @Override public boolean shouldPause()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
}
