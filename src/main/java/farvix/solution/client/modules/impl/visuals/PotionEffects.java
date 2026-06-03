package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.animation.Animation;
import farvix.solution.api.animation.Easing;
import farvix.solution.api.TempColor;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.FontRenderer;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.ui.hud.IHudElement;
import farvix.solution.api.ui.hud.HudElementRegistry;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ModuleInfo(name = "Potion Effects", category = ModuleCategory.VISUALS,
        description = "Показывает активные эффекты зелий")
public class PotionEffects extends Module implements QuickImports, IHudElement {

    // ── Settings ──────────────────────────────────────────────────────────────
    public final SliderSetting scale = new SliderSetting(
            "Размер", this, 1.0f, 0.1f, 2.0f, 0.05f);

    public final SliderSetting bgAlpha = new SliderSetting(
            "Прозрачность фона", this, 0.85f, 0f, 1f, 0.05f);

    // ── HUD position ──────────────────────────────────────────────────────────
    public static float hudX = 0.0104f;
    public static float hudY = 0.185f;

    // ── Base layout (at scale 1.0) ────────────────────────────────────────────
    private static final float PAD      = 7f;
    private static final float HEADER_H = 18f;
    private static final float ROW_H    = 16f;
    private static final float ICON_SZ  = 12f;
    // Отступ между именем и временем
    private static final float TIME_GAP = 12f;

    // ── Drag ──────────────────────────────────────────────────────────────────
    private boolean dragging   = false;
    private double  dragOffX, dragOffY;
    private boolean wasPressed = false;

    // ── Animated height ───────────────────────────────────────────────────────
    private float currentH = HEADER_H + PAD * 2;
    private float currentW = 160f;

    // ── Анимация появления/исчезновения виджета ───────────────────────────────
    private final Animation visibilityAnim = new Animation(Easing.EASE_OUT_CUBIC, 300);

    // ── Анимации появления эффектов ───────────────────────────────────────────
    // Ключ — id эффекта, значение — время начала анимации (ms)
    private final java.util.Map<String, Long> effectStartTimes = new java.util.LinkedHashMap<>();
    private final java.util.Set<String> knownEffects = new java.util.HashSet<>();
    private static final long ANIM_DURATION_MS = 300L;

    @Override
    public void onEnable() {
        super.onEnable();
        // Регистрируем HUD элемент для обработки кликов
        HudElementRegistry.getInstance().register(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Удаляем из реестра
        HudElementRegistry.getInstance().unregister(this);
    }

    // ── IHudElement implementation ────────────────────────────────────────────

    @Override
    public float getHudX() {
        return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteX(hudX);
    }

    @Override
    public float getHudY() {
        return farvix.solution.api.ui.hud.HudPositionHelper.getAbsoluteY(hudY);
    }

    @Override
    public float getHudWidth() {
        return currentW * scale.getValue();
    }

    @Override
    public float getHudHeight() {
        return currentH * scale.getValue();
    }

    @Override
    public Module getModule() {
        return this;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    // Кэш для эффектов
    private List<StatusEffectInstance> cachedEffects = new ArrayList<>();
    private long lastEffectUpdate = 0;
    private static final long EFFECT_CACHE_TIME = 100; // обновляем каждые 100мс

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null) return;

        // Кэшируем список эффектов
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEffectUpdate > EFFECT_CACHE_TIME) {
            Collection<StatusEffectInstance> raw = mc.player.getStatusEffects();
            cachedEffects = new ArrayList<>(raw);
            lastEffectUpdate = currentTime;
        }
        
        List<StatusEffectInstance> effects = cachedEffects;

        // Не показываем если нет эффектов — кроме случаев когда открыт ClickGUI или чат
        boolean inGuiOrChat = mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen
                || mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

        // Анимация появления/исчезновения — 300ms как ClickGUI
        boolean shouldShow = (!effects.isEmpty() || inGuiOrChat)
                && (mc.currentScreen == null || inGuiOrChat);
        visibilityAnim.run(shouldShow ? 1f : 0f);
        float widgetAlpha = visibilityAnim.getValue();
        if (widgetAlpha <= 0.01f) return;        float s = scale.getValue();
        float topPad = PAD - 3f; // Убираем 3 пикселя сверху меню
        DrawContext ctx = e.getContext();
        effects.sort((a, b) -> Integer.compare(a.getDuration(), b.getDuration()));

        int count = effects.size();

        FontRenderer font = Fonts.DEFAULT.get(11);

        // ── Вычисляем динамическую ширину ─────────────────────────────────────
        // Ширина = PAD + ICON + gap + labelWidth + TIME_GAP + timeWidth + PAD
        float maxRowW = font.getStringWidth("Potions") + PAD * 2 + 80f; // минимум по заголовку
        for (StatusEffectInstance effect : effects) {
            StatusEffect type = effect.getEffectType().value();
            String name = type.getName().getString();
            int amp = effect.getAmplifier();
            String level = amp > 0 ? " " + toRoman(amp + 1) : "";
            String label = name + level;
            String timeStr = formatDuration(effect.getDuration());

            float rowW = PAD + ICON_SZ + 5f
                     + font.getStringWidth(label)
                     + TIME_GAP
                     + font.getStringWidth(timeStr)
                     + PAD;
            if (rowW > maxRowW) maxRowW = rowW;
        }

        float targetW = maxRowW;
        float targetH = (HEADER_H + topPad) + count * ROW_H + (count > 0 ? PAD * 0.5f : PAD);
        currentW = MathHelper.lerp(0.18f, currentW, targetW);
        currentH = targetH; // высота мгновенная — строки управляют своей анимацией сами

        float w = currentW * s;
        float h = currentH * s;

        // Получаем цвет фона для определения яркости темы
        FixColor bg = TempColor.getGuiBackground();
        boolean isLightTheme = (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3f > 128f;

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;

        // Применяем масштаб вокруг позиции HUD
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(s, s, 1f);
        ctx.getMatrices().translate(-x, -y, 0);

        // ── Фон ───────────────────────────────────────────────────────────────
        glass.render(ShapeProperties.create(ctx.getMatrices(), x, y, currentW, currentH)
                .round(8)
                .softness(1.5f)
                .thickness(0)
                .outlineColor(0)
                .color(bg.alpha(bgAlpha.getValue() * widgetAlpha).getRGB())
                .build());

        // Определяем контрастные цвета
        int textColor = isLightTheme
                ? new FixColor(20, 20, 20, (int)(widgetAlpha * 230)).getRGB()
                : TempColor.getTextPrimary().alpha(widgetAlpha * 0.9f).getRGB();

        // ── Отрисовка минималистичной иконки колбы 5x6.4 ───────────────────
        float iconX = x + PAD;
        float iconY = y + topPad + (HEADER_H - 6.4f) / 2f - 3f;

        // 1. Горлышко (губа) колбы
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), iconX + 1.3f, iconY, 2.4f, 0.6f)
                .round(0f)
                .color(textColor)
                .build());

        // 2. Горлышко
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), iconX + 1.8f, iconY + 0.6f, 1.4f, 1.2f)
                .round(0f)
                .color(textColor)
                .build());

        // 3. Корпус колбы (круглая полая рамка толщиной 3.0f с прозрачной заливкой)
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), iconX + 0.2f, iconY + 1.8f, 4.6f, 4.6f)
                .round(2.3f)
                .thickness(3f)
                .softness(0.5f)
                .outlineColor(textColor)
                .color(0) // прозрачный внутри
                .build());

        // ── Заголовок ─────────────────────────────────────────────────────────
        float titleY = y + topPad + HEADER_H / 2f - 4f;
        Fonts.SEMIBOLD.get(12).drawString(ctx.getMatrices(), "Potions",
                x + PAD + 10f, titleY, textColor);

        // Разделитель
        rectangle.render(ShapeProperties.create(ctx.getMatrices(),
                x + PAD * 0.5f, y + HEADER_H + topPad * 0.5f,
                currentW - PAD, 0.5f)
                .round(0)
                .color(TempColor.getSeparatorHorizontal().alpha(0.4f * widgetAlpha).getRGB())
                .build());

        // ── Обновляем анимации эффектов ──────────────────────────────────────
        long now = System.currentTimeMillis();
        java.util.Set<String> activeIds = new java.util.HashSet<>();
        for (StatusEffectInstance effect : effects) {
            String id = Registries.STATUS_EFFECT.getId(effect.getEffectType().value()).toString();
            activeIds.add(id);
            if (!knownEffects.contains(id)) {
                // Новый эффект — если виджет уже показан, анимируем; если только появляется — сразу завершаем
                long startTime = widgetAlpha >= 0.99f ? now : now - ANIM_DURATION_MS;
                effectStartTimes.put(id, startTime);
                knownEffects.add(id);
            } else {
                effectStartTimes.putIfAbsent(id, now - ANIM_DURATION_MS);
            }
        }
        // Удаляем исчезнувшие эффекты
        effectStartTimes.keySet().removeIf(k -> !activeIds.contains(k));
        knownEffects.removeIf(k -> !activeIds.contains(k));

        // ── Строки эффектов ───────────────────────────────────────────────────
        float rowY = y + HEADER_H + topPad;

        for (StatusEffectInstance effect : effects) {
            if (rowY + ROW_H > y + currentH + 2) break;

            StatusEffect type = effect.getEffectType().value();
            String effectId = Registries.STATUS_EFFECT
                    .getId(effect.getEffectType().value()).getPath();
            String effectKey = Registries.STATUS_EFFECT
                    .getId(effect.getEffectType().value()).toString();

            // Прогресс анимации по реальному времени (1.5 сек)
            float anim = Math.min(1f, (float)(now - effectStartTimes.getOrDefault(effectKey, now - ANIM_DURATION_MS)) / ANIM_DURATION_MS);
            // Easing: ease-out cubic
            float easedAnim = 1f - (float) Math.pow(1 - anim, 3);
            // Если виджет ещё появляется — синхронизируем с widgetAlpha
            float effectProgress = widgetAlpha < 1f ? widgetAlpha : easedAnim;
            // Slide: смещение слева уменьшается по мере анимации
            float slideOffset = (1f - effectProgress) * -20f;
            float rowAlpha = effectProgress * widgetAlpha;

            ctx.getMatrices().push();
            ctx.getMatrices().translate(slideOffset, 0, 0);

            // Иконка
            Identifier iconTex = Identifier.of("minecraft",
                    "textures/mob_effect/" + effectId + ".png");
            // Применяем alpha через цвет иконки и опускаем на 2 пикселя
            drawIconAlpha(ctx, iconTex,
                    x + PAD,
                    rowY + (ROW_H - ICON_SZ) / 2f - 3f,
                    ICON_SZ, rowAlpha);

            // Название
            String name = type.getName().getString();
            int amp = effect.getAmplifier();
            String level = amp > 0 ? " " + toRoman(amp + 1) : "";
            String label = name + level;

            int nameAlphaInt = (int)(rowAlpha * 220);
            int nameColor;
            if (isLightTheme) {
                // Светлая тема: text черный (или темно-красный для дебаффов)
                nameColor = type.isBeneficial() ? new FixColor(0, 0, 0, nameAlphaInt).getRGB() : new FixColor(150, 0, 0, nameAlphaInt).getRGB();
            } else {
                // Темная тема: текст белый (или светло-красный для дебаффов)
                nameColor = type.isBeneficial() ? new FixColor(255, 255, 255, nameAlphaInt).getRGB() : new FixColor(255, 120, 120, nameAlphaInt).getRGB();
            }

            float textY = rowY + ROW_H / 2f - 3.5f;
            font.drawString(ctx.getMatrices(), label,
                    x + PAD + ICON_SZ + 5f, textY, nameColor);

            // Время
            String timeStr = formatDuration(effect.getDuration());
            float labelW = font.getStringWidth(label);
            float timeX = x + PAD + ICON_SZ + 5f + labelW + TIME_GAP;

            int timeAlphaInt = (int)(rowAlpha * 220);
            int timeColor = effect.getDuration() < 200
                    ? new FixColor(255, 80, 80, timeAlphaInt).getRGB()
                    : new FixColor(
                        TempColor.getTextSecondary().getRed(),
                        TempColor.getTextSecondary().getGreen(),
                        TempColor.getTextSecondary().getBlue(),
                        timeAlphaInt).getRGB();

            font.drawString(ctx.getMatrices(), timeStr, timeX, textY, timeColor);

            ctx.getMatrices().pop();

            rowY += ROW_H;
        }

        ctx.getMatrices().pop();

        handleDrag(w, h);
    }

    // ── Рендер иконки ─────────────────────────────────────────────────────────

    private void drawIcon(DrawContext ctx, Identifier tex, float x, float y, float size) {
        drawIconAlpha(ctx, tex, x, y, size, 1f);
    }

    private void drawIconAlpha(DrawContext ctx, Identifier tex, float x, float y, float size, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        int a = (int)(alpha * 255);
        int col = (a << 24) | 0x00FFFFFF;
        buf.vertex(m, x,        y,        0).texture(0, 0).color(col);
        buf.vertex(m, x,        y + size, 0).texture(0, 1).color(col);
        buf.vertex(m, x + size, y + size, 0).texture(1, 1).color(col);
        buf.vertex(m, x + size, y,        0).texture(1, 0).color(col);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
    }

    // ── Drag ──────────────────────────────────────────────────────────────────

    private void handleDrag(float w, float h) {
        // Блокируем перетаскивание если открыто контекстное меню
        if (farvix.solution.api.ui.contextmenu.ContextMenuManager.getInstance().hasOpenMenu()) {
            dragging = false;
            wasPressed = false;
            return;
        }
        
        // Перетаскивание только когда открыт экран (GUI или чат)
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
        
        net.minecraft.client.util.Window currentWindow = mc.getWindow();
        if (currentWindow == null) return;
        
        float screenW = currentWindow.getScaledWidth();
        float screenH = currentWindow.getScaledHeight();
        float x = hudX * screenW;
        float y = hudY * screenH;
        
        double mx = mc.mouse.getX() / currentWindow.getScaleFactor();
        double my = mc.mouse.getY() / currentWindow.getScaleFactor();
        boolean pressed = GLFW.glfwGetMouseButton(
                currentWindow.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

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
            
            // Тащим за всю панель (не только заголовок)
            if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
                dragging = true;
                dragOffX = mx - x;
                dragOffY = my - y;
            }
        }
        if (!pressed) dragging = false;
        if (dragging && pressed) {
            hudX = Math.max(0, Math.min((float)(mx - dragOffX), screenW - w)) / screenW;
            hudY = Math.max(0, Math.min((float)(my - dragOffY), screenH - h)) / screenH;
        }
        wasPressed = pressed;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String formatDuration(int ticks) {
        if (ticks == Integer.MAX_VALUE) return "∞";
        int totalSec = ticks / 20;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%d:%02d", min, sec);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1  -> "I";   case 2  -> "II";  case 3  -> "III";
            case 4  -> "IV";  case 5  -> "V";   case 6  -> "VI";
            case 7  -> "VII"; case 8  -> "VIII"; case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}
