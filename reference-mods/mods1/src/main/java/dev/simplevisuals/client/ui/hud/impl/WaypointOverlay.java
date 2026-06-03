package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.managers.WaypointManager;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.world.WorldUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class WaypointOverlay implements Wrapper {

    private static final Color LABEL_BACKGROUND = new Color(20, 24, 32, 185);
    private static final Color LABEL_SHADOW = new Color(0, 0, 0, 80);
    private static final Color TEXT_PRIMARY = new Color(240, 242, 247, 255);
    private static final Color TEXT_SECONDARY = new Color(200, 206, 216, 220);
    private static final Color ACCENT_COLOR = new Color(96, 176, 255, 230);

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.world == null) return;

        int winW = mc.getWindow().getScaledWidth();
        int winH = mc.getWindow().getScaledHeight();

        float tickDelta = e.getTickDelta();

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vec3d forward = mc.player.getRotationVec(tickDelta);

        for (WaypointManager.Waypoint w : WaypointManager.list()) {
            Vec3d waypointPos = w.pos.add(0, 1.8, 0);
            Vec3d toWaypoint = waypointPos.subtract(cameraPos);
            if (toWaypoint.lengthSquared() < 1.0e-4) continue;

            Vec3d dir = toWaypoint.normalize();
            boolean inFront = forward.dotProduct(dir) > 0.0;

            Vec3d screen = WorldUtils.getPosition(waypointPos);
            if (inFront && screen.z <= 0.01) continue;

            String label = w.name;
            int meters = (int) Math.floor(mc.player.getPos().distanceTo(w.pos));
            String meta = meters + "m";

            float nameSize = 8.5f;
            float metaSize = 8.0f;
            float padX = 4f, padY = 3f;
            float nameW = Fonts.BOLD.getWidth(label, nameSize);
            float metaW = Fonts.BOLD.getWidth(meta, metaSize);
            float width = Math.max(nameW, metaW) + padX * 2f;
            float height = Fonts.BOLD.getHeight(nameSize) + Fonts.BOLD.getHeight(metaSize) + padY * 3f;
            float x;
            float y;
            float markerX;
            float markerY;

            if (!inFront) {
                float margin = 10f;

                float playerYaw = mc.player.getYaw(tickDelta);
                double targetYaw = Math.toDegrees(Math.atan2(toWaypoint.z, toWaypoint.x));
                double yawDiff = MathHelper.wrapDegrees(targetYaw - playerYaw);
                float horizontalFactor = MathHelper.clamp((float) (yawDiff / 90.0), -1f, 1f);

                float halfRange = winW / 2f - margin - width / 2f;
                float baseX = winW / 2f + horizontalFactor * halfRange;

                x = baseX - width / 2f;
                x = MathHelper.clamp(x, margin, winW - width - margin);

                y = winH - height - margin;

                markerX = MathHelper.clamp(baseX, margin + 2f, winW - margin - 2f);
                markerY = y + height + 4f;
            } else {
                x = (float) screen.x - width / 2f;
                y = (float) screen.y - height - 8f;

                x = Math.max(5f, Math.min(x, winW - width - 5f));
                y = Math.max(5f, Math.min(y, winH - height - 5f));

                markerX = Math.max(3f, Math.min((float) screen.x, winW - 3f));
                markerY = Math.max(3f, Math.min((float) screen.y, winH - 3f));
            }

            // backdrop
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x + 2, y + 2, width, height, 3f, LABEL_SHADOW);
            Render2D.drawRoundedRect(e.getContext().getMatrices(), x, y, width, height, 3f, LABEL_BACKGROUND);

            // small marker dot below/near label
            Render2D.drawRoundedRect(e.getContext().getMatrices(), markerX - 2f, markerY - 2f, 4f, 4f, 2f, ACCENT_COLOR);

            // name (top)
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(nameSize), label, x + padX, y + padY, TEXT_PRIMARY);
            // distance (bottom)
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(metaSize), meta, x + padX, y + padY + Fonts.BOLD.getHeight(nameSize) + 2f, TEXT_SECONDARY);
        }
    }
}