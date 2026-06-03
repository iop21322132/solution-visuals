package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.TempColor;
import farvix.solution.api.events.impl.entity.EventAttackEntity;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.managers.ThemeManager;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInfo(name = "Target HUD", category = ModuleCategory.PLAYER, description = "Показывает HP цели")
public class TargetHud extends Module implements QuickImports, farvix.solution.api.ui.hud.IHudElement {

    // ── Settings ──────────────────────────────────────────────────────────────
    public final SliderSetting scale = new SliderSetting(
            "Размер", this, 1.0f, 0.5f, 2.0f, 0.05f);

    public final SliderSetting fadeTime = new SliderSetting(
            "Время жизни", this, 8f, 1f, 30f, 1f);

    public final SliderSetting bgAlpha = new SliderSetting(
            "Прозрачность фона", this, 0.85f, 0f, 1f, 0.05f);

    // ── Layout (Classic) ─────────────────────────────────────────────────────
    private static final float MIN_WIDTH = 110f;
    private static final float HEAD_SIZE = 18f;
    private static final float PAD       = 4f;
    private static final float BAR_H     = 3f;
    private static final float GAP       = 1f;

    // ── HUD position ──────────────────────────────────────────────────────────
    public static float hudX = 0.0104f;
    public static float hudY = 0.1111f;

    // ── Анимация появления/исчезновения ───────────────────────────────────────
    private final Animation visibilityAnim = new Animation(Easing.EASE_OUT_CUBIC, 300);
    // Анимация уменьшения после истечения времени
    private final Animation shrinkAnim = new Animation(Easing.EASE_IN_CUBIC, 400);

    // ── State ─────────────────────────────────────────────────────────────────
    private LivingEntity target     = null;
    private long         lastAttack = 0L;
    private float        lastW      = MIN_WIDTH;
    private float        lastH      = 30f;

    private float healthAnimation = 1f;
    private float hitFlash        = 0f;
    private float lastHealth      = -1f;
    private final List<HitParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private long lastFrameTime = System.currentTimeMillis();

    private static class HitParticle {
        float x, y, vx, vy, life, maxLife, size;
        HitParticle(float x, float y) {
            this.x = x; this.y = y;
            double a = Math.random() * Math.PI * 2;
            float spd = 0.1f + (float)Math.random() * 0.3f;
            vx = (float)Math.cos(a) * spd; vy = (float)Math.sin(a) * spd;
            maxLife = 60f + (float)Math.random() * 40f; life = maxLife;
            size = 1f + (float)Math.random() * 1.2f;
        }
        void update() { x += vx; y += vy; vx *= 0.95f; vy *= 0.95f; life--; }
    }

    // ── Drag ──────────────────────────────────────────────────────────────────
    private boolean dragging   = false;
    private double  dragOffX, dragOffY;
    private boolean wasPressed = false;

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (!(e.getTarget() instanceof LivingEntity living)) return;
        target     = living;
        lastAttack = System.currentTimeMillis();
    }

    // Кэш для проверок экрана
    private boolean lastInGuiOrChat = false;
    private long lastScreenCheck = 0;
    private static final long SCREEN_CHECK_INTERVAL = 50; // проверяем каждые 50мс

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null) return;
        
        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        long fadeAfterMs = (long)(fadeTime.getValue() * 1000f);
        if (target != null && (!target.isAlive() || target.isRemoved())) target = null;

        if (now - lastScreenCheck > SCREEN_CHECK_INTERVAL) {
            lastInGuiOrChat = mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen
                    || mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
            lastScreenCheck = now;
        }

        boolean inGuiOrChat = lastInGuiOrChat;
        boolean hasTarget = target != null;
        boolean timeExpired = hasTarget && (now - lastAttack) > fadeAfterMs;

        // Когда время истекло — запускаем анимацию уменьшения
        shrinkAnim.run(timeExpired ? 0f : 1f);
        float shrinkScale = shrinkAnim.getValue(); // 1 = нормальный, 0 = исчез

        // Убираем цель только когда анимация завершена
        if (timeExpired && shrinkScale <= 0.01f) {
            target = null;
            hasTarget = false;
        }

        boolean shouldShow = (hasTarget || inGuiOrChat);
        visibilityAnim.run(shouldShow ? 1f : 0f);
        float visAlpha = visibilityAnim.getValue();
        if (visAlpha <= 0.01f && shrinkScale <= 0.01f) return;

        float alpha = visAlpha;
        updateParticles();

        render(e, alpha * visAlpha, shrinkScale, dt);
        handleDrag();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void render(EventRender2D e, float alpha, float shrinkScale, float dt) {
        var ctx = e.getContext();
        var ms  = ctx.getMatrices();
        
        boolean inGuiOrChat = mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen
                || mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        LivingEntity ent = (target == null && inGuiOrChat) ? mc.player : target;
        if (ent == null) return;

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;

        // Урон и частицы
        float currentHp = ent.getHealth();
        if (lastHealth > 0 && currentHp < lastHealth) {
            hitFlash = 1f;
            spawnHitParticles(x, y);
        }
        lastHealth = currentHp;
        if (hitFlash > 0) hitFlash = Math.max(0f, hitFlash - 0.06f * (dt * 60f));

        healthAnimation = MathHelper.clamp(
                MathHelper.lerp(0.1f * (dt * 60f), healthAnimation, currentHp / ent.getMaxHealth()), 0f, 1f);

        // Сбор предметов
        List<ItemStack> allItems = new ArrayList<>();
        if (ent instanceof PlayerEntity p) {
            for (int i = 3; i >= 0; i--) {
                ItemStack s = p.getInventory().armor.get(i);
                if (!s.isEmpty()) allItems.add(s);
            }
            if (!p.getMainHandStack().isEmpty()) allItems.add(p.getMainHandStack());
            if (!p.getOffHandStack().isEmpty())  allItems.add(p.getOffHandStack());
        }

        // Размеры
        float nameFont = 6f;
        float hpFont   = 5.5f;
        float itemSize = 8f;
        float itemGap  = 0.5f;

        String name = ent.getName().getString();
        float nameW  = Fonts.SEMIBOLD.get(12).getStringWidth(name) * 0.5f;
        String hpPct = (int)(healthAnimation * 100) + "%";
        float hpPctW = Fonts.DEFAULT.get(11).getStringWidth(hpPct) * 0.5f;

        float itemsRowW  = allItems.isEmpty() ? 0f : allItems.size() * (itemSize + itemGap) - itemGap;
        float contentW    = Math.max(nameW + 4f + hpPctW, itemsRowW);
        float totalW = PAD + HEAD_SIZE + PAD + contentW + PAD;
        totalW = Math.max(totalW, MIN_WIDTH);
        
        float innerH   = Math.max(HEAD_SIZE, nameFont + GAP + itemSize);
        float totalH   = PAD + innerH + PAD + BAR_H + 2f;

        lastW = totalW; lastH = totalH;

        float s = scale.getValue() * shrinkScale;
        float centerX = x + totalW * scale.getValue() / 2f;
        float centerY = y + totalH * scale.getValue() / 2f;

        ms.push();
        ms.translate(centerX, centerY, 0);
        ms.scale(shrinkScale, shrinkScale, 1f);
        ms.translate(-centerX, -centerY, 0);
        ms.translate(x, y, 0);
        ms.scale(scale.getValue(), scale.getValue(), 1f); 
        ms.translate(-x, -y, 0);

        int fullAlpha = (int)(alpha * 255f);

        // Фон по теме ClickGUI (используем глобальный цвет фона темы)
        int bgColor = TempColor.getGuiBackground().alpha(bgAlpha.getValue() * alpha).getRGB();

        // Определяем яркость фона для адаптации текста
        FixColor bg = TempColor.getGuiBackground();
        float brightness = (bg.getRed() * 0.299f + bg.getGreen() * 0.587f + bg.getBlue() * 0.114f) / 255f;
        boolean isLightTheme = brightness > 0.5f;
        int primaryColor = isLightTheme ? new FixColor(0, 0, 0, fullAlpha).getRGB() : new FixColor(255, 255, 255, fullAlpha).getRGB();
        int secondaryColor = isLightTheme ? new FixColor(0, 0, 0, (int)(fullAlpha * 0.8f)).getRGB() : new FixColor(255, 255, 255, (int)(fullAlpha * 0.8f)).getRGB();

        glass.render(ShapeProperties.create(ms, x, y, totalW, totalH)
                .round(5f).softness(1.5f).thickness(0).color(bgColor).build());

        // Голова
        float headX = x + PAD;
        float headY = y + PAD + (innerH - HEAD_SIZE) / 2f;

        int headTint;
        if (hitFlash > 0) {
            int red = (int)(255 * hitFlash);
            headTint = new FixColor(255, Math.max(0, 255 - red), Math.max(0, 255 - red), fullAlpha).getRGB();
        } else {
            headTint = new FixColor(255, 255, 255, fullAlpha).getRGB();
        }

        if (ent instanceof AbstractClientPlayerEntity player) {
            drawSkinHead(ctx, player.getSkinTextures().texture(), headX, headY, HEAD_SIZE, headTint);
        } else {
            drawSkinHead(ctx, Identifier.of("solution", "textures/steve.png"), headX, headY, HEAD_SIZE, headTint);
        }

        renderParticles(ms, alpha);

        // Текст
        float cx = headX + HEAD_SIZE + PAD;
        float topY = y + PAD + (innerH - (6f + GAP + itemSize)) / 2f;

        Fonts.SEMIBOLD.get(12).drawString(ms, name, cx, topY, primaryColor);

        float hpPctX = x + totalW - PAD - hpPctW - 8f;
        Fonts.DEFAULT.get(11).drawString(ms, hpPct, hpPctX, topY + 0.5f, secondaryColor);

        // Предметы
        float itemsY = topY + 6f + GAP;
        if (!allItems.isEmpty()) {
            float itemScale = itemSize / 16f;
            for (int i = 0; i < allItems.size(); i++) {
                float ix = cx + i * (itemSize + itemGap);
                ms.push();
                ms.translate(ix, itemsY, 150f);
                ms.scale(itemScale, itemScale, 1f);
                DiffuseLighting.disableGuiDepthLighting();
                RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
                ctx.drawItem(allItems.get(i), 0, 0);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                ms.pop();
            }
        }

        // Полоска здоровья
        float barY = y + totalH - BAR_H - 2f;
        float barX = x + 3f;
        float barW = totalW - 6f;

        blur.render(ShapeProperties.create(ms, barX, barY, barW, BAR_H)
                .round(BAR_H / 2f).color(new FixColor(0, 0, 0, (int)(60 * alpha)).getRGB()).build());
        
        float fillW = barW * healthAnimation;
        if (fillW > 0) {
            // Динамический цвет хп: зеленый -> желтый -> красный
            int hpColor = getHealthColor(healthAnimation, fullAlpha);
            blur.render(ShapeProperties.create(ms, barX, barY, fillW, BAR_H)
                    .round(BAR_H / 2f).color(hpColor).build());
        }

        ms.pop();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float lerp(float t, float a, float b) { return a + t * (b - a); }

    private int getHealthColor(float pct, int alpha) {
        if (pct > 0.5f) {
            // Зеленый -> Желтый
            float t = (pct - 0.5f) * 2f;
            int r = (int)(255 * (1f - t));
            return new FixColor(r, 210, 0, alpha).getRGB();
        } else {
            // Желтый -> Красный
            float t = pct * 2f;
            int g = (int)(210 * t);
            return new FixColor(220, g, 0, alpha).getRGB();
        }
    }

    private void drawSkinHead(net.minecraft.client.gui.DrawContext ctx,
                               Identifier tex, float x, float y, float size, int rgb) {
        float S = 64f;
        float u0 = 8/S, v0 = 8/S, u1 = 16/S, v1 = 16/S;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);

        var tess = Tessellator.getInstance();
        var buf  = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        buf.vertex(m, x,        y,        0).texture(u0, v0).color(rgb);
        buf.vertex(m, x,        y + size, 0).texture(u0, v1).color(rgb);
        buf.vertex(m, x + size, y + size, 0).texture(u1, v1).color(rgb);
        buf.vertex(m, x + size, y,        0).texture(u1, v0).color(rgb);
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }

    private void spawnHitParticles(float x, float y) {
        float headX = x + PAD;
        float headY = y + PAD;
        for (int i = 0; i < 5; i++) {
            particles.add(new HitParticle(
                    headX + random.nextFloat() * HEAD_SIZE,
                    headY + random.nextFloat() * HEAD_SIZE));
        }
    }

    private void updateParticles() {
        particles.removeIf(p -> { p.update(); return p.life <= 0; });
    }

    private void renderParticles(MatrixStack ms, float anim) {
        for (HitParticle p : particles) {
            float alpha = (p.life / p.maxLife) * anim * 0.8f;
            if (alpha <= 0) continue;
            
            Color primary = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
            int c = new FixColor(primary.getRed(), primary.getGreen(), primary.getBlue(), (int)(255 * alpha)).getRGB();
            
            blur.render(ShapeProperties.create(ms, p.x - p.size / 2f, p.y - p.size / 2f, p.size, p.size)
                    .round(p.size / 2f).color(c).build());
            
            int glow = new FixColor(primary.getRed(), primary.getGreen(), primary.getBlue(), (int)(50 * alpha)).getRGB();
            blur.render(ShapeProperties.create(ms, p.x - p.size, p.y - p.size, p.size * 2f, p.size * 2f)
                    .round(p.size).color(glow).build());
        }
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    private void handleDrag() {
        // Блокируем перетаскивание если открыто контекстное меню
        if (farvix.solution.api.ui.contextmenu.ContextMenuManager.getInstance().hasOpenMenu()) {
            dragging = false;
            wasPressed = false;
            return;
        }
        
        if (mc.currentScreen == null) {
            dragging = false;
            wasPressed = false;
            return;
        }
        
        // Запрет перетаскивания в контейнерах (сундуки, печки и т.д.), но разрешаем в инвентаре игрока
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen &&
            !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) {
            dragging = false;
            wasPressed = false;
            return;
        }
        
        net.minecraft.client.util.Window win = mc.getWindow();
        if (win == null) return;

        float screenW = win.getScaledWidth();
        float screenH = win.getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;

        double mx = mc.mouse.getX() / win.getScaleFactor();
        double my = mc.mouse.getY() / win.getScaleFactor();
        boolean pressed = GLFW.glfwGetMouseButton(win.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        float s = scale.getValue();
        float cw = lastW * s, ch = lastH * s;

        if (pressed && !wasPressed) {
            // Проверяем, не внутри ли ClickGUI
            if (mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen) {
                farvix.solution.api.ui.clickgui.InterfaceScreen gui = 
                    (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
                float guiX = gui.getX();
                float guiY = gui.getY();
                float guiW = gui.getWidth();
                float guiH = gui.getHeight();
                
                // Если клик внутри ClickGUI, не начинаем перетаскивание HUD элемента
                if (mx >= guiX && mx <= guiX + guiW && my >= guiY && my <= guiY + guiH) {
                    return;
                }
            }
            
            if (mx >= x && mx <= x + cw && my >= y && my <= y + ch) {
                dragging = true;
                dragOffX = mx - x;
                dragOffY = my - y;
            }
        }
        if (!pressed) dragging = false;
        if (dragging && pressed) {
            hudX = Math.max(0, Math.min((float)(mx - dragOffX), screenW - cw)) / screenW;
            hudY = Math.max(0, Math.min((float)(my - dragOffY), screenH - ch)) / screenH;
        }
        wasPressed = pressed;
    }

    // ── IHudElement ───────────────────────────────────────────────────────────

    @Override public float getHudX()      { return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteX(hudX); }
    @Override public float getHudY()      { return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteY(hudY); }
    @Override public float getHudWidth()  { return lastW  * scale.getValue(); }
    @Override public float getHudHeight() { return lastH * scale.getValue(); }
    @Override public Module getModule()   { return this; }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        float x = getHudX();
        float y = getHudY();
        return mouseX >= x && mouseX <= x + getHudWidth()
            && mouseY >= y && mouseY <= y + getHudHeight();
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
    }
}
