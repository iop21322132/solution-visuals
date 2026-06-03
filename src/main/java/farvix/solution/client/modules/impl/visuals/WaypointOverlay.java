package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.api.util.render.ProjectionUtility;
import farvix.solution.client.managers.WaypointManager;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@ModuleInfo(
    name        = "Waypoint Overlay",
    description = "Показывает вейпоинты в мире",
    category    = ModuleCategory.WAYPOINTS
)
public class WaypointOverlay extends Module implements QuickImports {

    // ── Размеры метки (фиксированные, не зависят от расстояния) ──────────────
    private static final float PAD_X  = 6f;
    private static final float PAD_Y  = 4f;
    private static final float RADIUS = 5f;

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.world == null || mc.player == null) return;

        List<WaypointManager.Waypoint> waypoints = WaypointManager.list();
        if (waypoints.isEmpty()) return;

        net.minecraft.client.gui.DrawContext context = e.getContext();
        net.minecraft.client.util.Window window = mc.getWindow();
        float sw = window.getScaledWidth();
        float sh = window.getScaledHeight();

        for (WaypointManager.Waypoint wp : waypoints) {
            Vec3d worldPos = new Vec3d(wp.getX(), wp.getY() + 1.0, wp.getZ());
            Vec3d screen = farvix.solution.api.util.render.ProjectionUtility
                    .worldSpaceToScreenSpace(worldPos);

            // Пропускаем только если точно за камерой (z > 1 в NDC = за far plane или за камерой)
            // Используем dot product для проверки "перед камерой"
            net.minecraft.client.render.Camera cam = mc.gameRenderer.getCamera();
            net.minecraft.util.math.Vec3d camPos = cam.getPos();
            net.minecraft.util.math.Vec3d toWp = worldPos.subtract(camPos).normalize();
            net.minecraft.util.math.Vec3d look = mc.player.getRotationVec(1.0f);
            if (toWp.dotProduct(look) <= 0) continue; // за спиной

            float sx = (float) screen.x;
            float sy = (float) screen.y;

            // Зажимаем в пределах экрана
            sx = Math.max(5f, Math.min(sw - 5f, sx));
            sy = Math.max(5f, Math.min(sh - 5f, sy));

            renderWaypointLabel(context, wp, sx, sy);
        }
    }

    private void renderWaypointLabel(net.minecraft.client.gui.DrawContext context,
                                      WaypointManager.Waypoint wp,
                                      float sx, float sy) {
        double dist = mc.player.getPos().distanceTo(wp.getPos());
        String distStr = dist < 1000
                ? String.format("%.0fм", dist)
                : String.format("%.1fкм", dist / 1000.0);

        String name = wp.getName();
        String sep  = " | ";

        // Одинаковый размер шрифта для симметрии
        int fontSize = 11;
        float nameW = Fonts.SEMIBOLD.get(fontSize).getStringWidth(name);
        float sepW  = Fonts.DEFAULT.get(fontSize).getStringWidth(sep);
        float distW = Fonts.SEMIBOLD.get(fontSize).getStringWidth(distStr);
        // getStringHeight возвращает значение в 2x масштабе — делим на 2 для реальных пикселей
        float lineH = Fonts.SEMIBOLD.get(fontSize).getStringHeight("Ag") / 2f;

        float padX = 8f;
        float padY = 2f;

        // Ширина фона точно по тексту + padding
        float contentW = nameW + sepW + distW;
        float totalW = contentW + padX * 2;
        float totalH = lineH + padY * 2;

        float bx = sx - totalW / 2f;
        float by = sy - totalH / 2f;

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        // Фон — акцентный цвет темы, больше прозрачности
        java.awt.Color ac = farvix.solution.client.managers.ThemeManager.getInstance()
                .getCurrentTheme().getAccentColor();
        int bgColor = new FixColor(ac.getRed(), ac.getGreen(), ac.getBlue(), 130).getRGB();

        rectangle.render(ShapeProperties.create(context.getMatrices(), bx, by, totalW, totalH)
                .round(4f)
                .softness(0f)
                .thickness(0f)
                .outlineColor(0)
                .color(bgColor)
                .build());

        // Определяем яркость фона
        float brightness = (ac.getRed() * 0.299f + ac.getGreen() * 0.587f + ac.getBlue() * 0.114f) / 255f;
        boolean lightBg = brightness > 0.5f;
        int primaryColor = lightBg ? new FixColor(20,  20,  20,  240).getRGB()
                                   : new FixColor(255, 255, 255, 240).getRGB();
        int sepColor     = lightBg ? new FixColor(60,  60,  60,  180).getRGB()
                                   : new FixColor(180, 180, 180, 180).getRGB();
        int distColor    = lightBg ? new FixColor(30,  30,  30,  255).getRGB()
                                   : new FixColor(255, 255, 255, 255).getRGB();

        // Текст строго по центру фона
        float textX = bx + (totalW - contentW) / 2f;
        float textY = by + (totalH - lineH) / 2f + 3f;

        Fonts.SEMIBOLD.get(fontSize).drawString(context.getMatrices(), name,
                textX, textY, primaryColor);
        Fonts.DEFAULT.get(fontSize).drawString(context.getMatrices(), sep,
                textX + nameW, textY, sepColor);
        Fonts.SEMIBOLD.get(fontSize).drawString(context.getMatrices(), distStr,
                textX + nameW + sepW, textY, distColor);

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }
}
