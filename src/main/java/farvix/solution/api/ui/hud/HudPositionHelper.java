package farvix.solution.api.ui.hud;

import net.minecraft.client.MinecraftClient;

public class HudPositionHelper {

    public static float readX(float value, float defaultPercent) {
        if (value < -0.5f) return -1f;
        if (value > 2.0f) {
            return value / 960.0f;
        }
        return value;
    }

    public static float readY(float value, float defaultPercent) {
        if (value < -0.5f) return -1f;
        if (value > 2.0f) {
            return value / 540.0f;
        }
        return value;
    }

    public static float getAbsoluteX(float percent) {
        if (percent < 0) return 0f;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return percent * 960f;
        return percent * mc.getWindow().getScaledWidth();
    }

    public static float getAbsoluteY(float percent) {
        if (percent < 0) return 0f;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return percent * 540f;
        return percent * mc.getWindow().getScaledHeight();
    }
}
