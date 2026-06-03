package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.BindSetting;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "FreeLook", category = ModuleCategory.VISUALS,
        description = "Свободная камера без поворота персонажа")
public class FreeLook extends Module {

    public final BindSetting key = new BindSetting(
            "Кнопка", this, GLFW.GLFW_KEY_V);

    public final SliderSetting distance = new SliderSetting(
            "Дальность", this, 4f, 1f, 10f, 0.5f);

    public final SliderSetting rotationSpeed = new SliderSetting(
            "Скорость вращения", this, 5f, 1f, 10f, 1f);

    public final BooleanSetting invertX = new BooleanSetting("Инверт X", this);
    public final BooleanSetting invertY = new BooleanSetting("Инверт Y", this);

    // ── Состояние (читается из миксина) ───────────────────────────────────────

    /** Активен ли FreeLook прямо сейчас */
    public static boolean active = false;

    /** Угол камеры по горизонтали (yaw) */
    public static float cameraYaw   = 0f;
    /** Угол камеры по вертикали (pitch) */
    public static float cameraPitch = 0f;

    /** Сохранённый yaw игрока до активации */
    public static float savedYaw   = 0f;
    /** Сохранённый pitch игрока до активации */
    public static float savedPitch = 0f;
    /** Сохранённая перспектива до активации */
    public static Perspective savedPerspective = Perspective.FIRST_PERSON;

    /** Реальный yaw игрока (восстанавливается после Camera.update) */
    public static float realYaw   = 0f;
    /** Реальный pitch игрока (восстанавливается после Camera.update) */
    public static float realPitch = 0f;

    // ── Проверка клавиши ──────────────────────────────────────────────────────

    public static boolean isKeyHeld(FreeLook module) {
        if (module == null || !module.isEnabled()) return false;
        int k = module.key.getKey();
        if (k < 0) return false;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.currentScreen != null) return false;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), k) == GLFW.GLFW_PRESS;
    }
}
