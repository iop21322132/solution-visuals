package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.api.TempColor;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.client.managers.ThemeManager;
import farvix.solution.client.modules.impl.visuals.DynamicIsland;
import farvix.solution.client.modules.impl.visuals.NoRender;
import farvix.solution.client.modules.impl.visuals.PotionEffects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements QuickImports {

    @Inject(method = "render", at = @At("RETURN"))
    public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // Обновляем цвет темы каждый кадр — чтобы градиент переливался
        // даже когда GUI закрыт
        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();
        java.awt.Color bg = theme.getBackgroundColor();
        String themeName = theme.getName();
        
        // Для Black/Dark тем — всегда чёрно-белая схема
        // Для White/Light тем — применяем цвет темы
        // Для Custom/Gradient тем — применяем кастомный цвет
        if ("Black".equals(themeName) || (bg.getRed() == 0 && bg.getGreen() == 0 && bg.getBlue() == 0)) {
            TempColor.resetThemeBackground();
        } else {
            TempColor.setThemeBackground(bg, theme.getAccentColor());
        }

        blur.setup();
        glass.setup();
        new EventRender2D(context, tickCounter).call();
        
        // Render Dynamic Island
        DynamicIsland dynamicIsland = Client.getInstance().getModuleManager().get(DynamicIsland.class);
        if (dynamicIsland != null && dynamicIsland.isEnabled()) {
            dynamicIsland.tick(); // Update media info
            dynamicIsland.render(context, tickCounter.getTickDelta(true));
        }
        
        // Рендерим контекстное меню В САМОМ КОНЦЕ - поверх всех HUD элементов
        if (mc.currentScreen != null) {
            blur.setup();
            glass.setup();
            double mouseX = mc.mouse.getX() / mc.getWindow().getScaleFactor();
            double mouseY = mc.mouse.getY() / mc.getWindow().getScaleFactor();
            // Поднимаем z выше чем у предметов инвентаря (200) и их бейджей
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 300);
            farvix.solution.api.ui.contextmenu.ContextMenuManager.getInstance()
                .render(context, (int) mouseX, (int) mouseY);
            context.getMatrices().pop();
        }
    }

    // ── Убрать ванильные иконки эффектов когда включён PotionEffects ──────────
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void hidePotionIcons(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        PotionEffects m = Client.getInstance().getModuleManager().get(PotionEffects.class);
        if (m != null && m.isEnabled()) ci.cancel();
    }

    // ── Убрать ванильный прицел ───────────────────────────────────────────────
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    public void noCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        farvix.solution.client.modules.impl.visuals.Crosshair m =
                Client.getInstance().getModuleManager().get(
                        farvix.solution.client.modules.impl.visuals.Crosshair.class);
        if (m != null && m.isEnabled()) ci.cancel();
    }

    // ── Streamer: replace nick in scoreboard ──────────────────────────────────
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // We don't cancel — just let TextRendererMixin handle the replacement
    }

    // ── Убрать скорборд ───────────────────────────────────────────────────────
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    public void noScoreboardHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        NoRender m = Client.getInstance().getModuleManager().get(NoRender.class);
        if (m != null && m.isEnabled() && m.noScoreboard.isEnabled()) ci.cancel();
        // Убираем ванильный scoreboard если включён ScoreboardHud
        farvix.solution.client.modules.impl.visuals.ScoreboardHud sh =
                Client.getInstance().getModuleManager().get(farvix.solution.client.modules.impl.visuals.ScoreboardHud.class);
        if (sh != null && sh.isEnabled()) ci.cancel();
    }

    // ── Убрать тотем (renderMiscOverlays содержит тотем) ─────────────────────
    @Inject(method = "renderMiscOverlays", at = @At("HEAD"), cancellable = true, require = 0)
    public void noTotemHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        NoRender m = Client.getInstance().getModuleManager().get(NoRender.class);
        if (m != null && m.isEnabled() && m.noTotem.isEnabled()) ci.cancel();
    }
}
