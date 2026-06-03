package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

@ModuleInfo(name = "HoldMyItems", category = ModuleCategory.VISUALS, description = "Убирает анимацию опускания предмета при смене или обновлении")
public class HoldMyItems extends Module {

    public static double deltaTime = 0.0;
    private static double prevTime = 0.0;

    static {
        WorldRenderEvents.START.register(context -> {
            double currentTime = org.lwjgl.glfw.GLFW.glfwGetTime();
            if (prevTime == 0.0) {
                prevTime = currentTime;
            }
            deltaTime = currentTime - prevTime;
            prevTime = currentTime;
            if (net.minecraft.client.MinecraftClient.getInstance().isPaused()) {
                deltaTime = 0.0;
            } else {
                deltaTime = Math.min(0.05, deltaTime);
            }
        });
    }

    public final ModeSetting handMode = new ModeSetting("Редактировать", this,
            "Правая рука",
            "Левая рука"
    );

    public final SliderSetting swingSpeed = new SliderSetting("holdmyitems.swingSpeed", this, 9.0f, 6.0f, 12.0f, 1.0f);
    public final BooleanSetting swimmingAnimation = new BooleanSetting("holdmyitems.swimmingAnimation", this);
    public final BooleanSetting climbAndCrawl = new BooleanSetting("holdmyitems.climbAndCrawl", this);

    // Слайдеры для правой руки
    public final SliderSetting rightX = new SliderSetting("X (Правая)", this, -0.25f, -2.0f, 2.0f, 0.05f);
    public final SliderSetting rightY = new SliderSetting("Y (Правая)", this, -0.05f, -2.0f, 2.0f, 0.05f);
    public final SliderSetting rightZ = new SliderSetting("Z (Правая)", this, -0.6f, -2.0f, 2.0f, 0.05f);

    // Слайдеры для левой руки
    public final SliderSetting leftX = new SliderSetting("X (Левая)", this, -0.25f, -2.0f, 2.0f, 0.05f);
    public final SliderSetting leftY = new SliderSetting("Y (Левая)", this, -0.05f, -2.0f, 2.0f, 0.05f);
    public final SliderSetting leftZ = new SliderSetting("Z (Левая)", this, -0.6f, -2.0f, 2.0f, 0.05f);

    public HoldMyItems() {
        swimmingAnimation.setValue(true);
        climbAndCrawl.setValue(true);

        // Динамическая видимость слайдеров в зависимости от выбранного режима руки
        rightX.setVisible(() -> handMode.is("Правая рука"));
        rightY.setVisible(() -> handMode.is("Правая рука"));
        rightZ.setVisible(() -> handMode.is("Правая рука"));

        leftX.setVisible(() -> handMode.is("Левая рука"));
        leftY.setVisible(() -> handMode.is("Левая рука"));
        leftZ.setVisible(() -> handMode.is("Левая рука"));
    }
}
