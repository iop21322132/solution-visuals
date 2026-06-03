package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ModeSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@ModuleInfo(name = "Swing Animation", category = ModuleCategory.VISUALS, description = "Анимация удара рукой")
public class SwingAnimation extends Module {

    // Hand selection: RIGHT (default) or LEFT
    public final ModeSetting hand = new ModeSetting("Поменять руку", this,
            "Правая рука",
            "Левая рука"
    );

    // Animation mode
    public final ModeSetting mode = new ModeSetting("Режим", this,
            "Первый",
            "Второй",
            "Третий",
            "Четвёртый",
            "Шестой",
            "Седьмой",
            "Кастомный"
    );

    // Swing power
    public final SliderSetting swingPower = new SliderSetting("Сила", this, 5.0f, 1.0f, 10.0f, 0.05f);

    // Custom mode settings — position and rotation work for ALL modes
    public final SliderSetting customTranslateX = new SliderSetting("Позиция X", this, 0.0f, -2.0f, 2.0f, 0.01f);
    public final SliderSetting customTranslateY = new SliderSetting("Позиция Y", this, 0.0f, -2.0f, 2.0f, 0.01f);
    public final SliderSetting customTranslateZ = new SliderSetting("Позиция Z", this, 0.0f, -2.0f, 2.0f, 0.01f);

    public final SliderSetting customRotateX = new SliderSetting("Поворот X", this, 0.0f, -360.0f, 360.0f, 0.01f);
    public final SliderSetting customRotateY = new SliderSetting("Поворот Y", this, 0.0f, -360.0f, 360.0f, 0.01f);
    public final SliderSetting customRotateZ = new SliderSetting("Поворот Z", this, 0.0f, -360.0f, 360.0f, 0.01f);

    public final SliderSetting customScaleX = new SliderSetting("Масштаб X", this, 1.0f, 0.1f, 3.0f, 0.01f)
            .setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting customScaleY = new SliderSetting("Масштаб Y", this, 1.0f, 0.1f, 3.0f, 0.01f)
            .setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting customScaleZ = new SliderSetting("Масштаб Z", this, 1.0f, 0.1f, 3.0f, 0.01f)
            .setVisible(() -> mode.is("Кастомный"));

    // Base rotation
    public final SliderSetting customBaseYaw = new SliderSetting("Базовый Yaw", this, 0.0f, -180.0f, 180.0f, 0.01f)
            .setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting customBasePitch = new SliderSetting("Базовый Pitch", this, 0.0f, -180.0f, 180.0f, 0.01f)
            .setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting customBaseRoll = new SliderSetting("Базовый Roll", this, 0.0f, -180.0f, 180.0f, 0.01f)
            .setVisible(() -> mode.is("Кастомный"));

    // Idle rotation
    public final BooleanSetting customIdleRotate = new BooleanSetting("Вращение", this)
            .setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting customIdleRotateSpeedX = new SliderSetting("Скорость X", this, 0.0f, -720.0f, 720.0f, 0.01f)
            .setVisible(() -> mode.is("Кастомный") && customIdleRotate.isEnabled());
    public final SliderSetting customIdleRotateSpeedY = new SliderSetting("Скорость Y", this, 90.0f, -720.0f, 720.0f, 0.01f)
            .setVisible(() -> mode.is("Кастомный") && customIdleRotate.isEnabled());
    public final SliderSetting customIdleRotateSpeedZ = new SliderSetting("Скорость Z", this, 0.0f, -720.0f, 720.0f, 0.01f)
            .setVisible(() -> mode.is("Кастомный") && customIdleRotate.isEnabled());

    private static final float SCALE = 0.5f;

    /** Returns true if the animation should be applied to the given arm. */
    public boolean shouldApplyToArm(Arm arm) {
        String h = hand.getCurrentMode();
        if ("Левая рука".equals(h)) return arm == Arm.LEFT;
        return arm == Arm.RIGHT;
    }

    public void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        float power = swingPower.getValue();
        float anim = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float scaleValue = SCALE;

        // Apply custom position for ALL modes (1 to 7 and Custom)
        float px = customTranslateX.getValue();
        float py = customTranslateY.getValue();
        float pz = customTranslateZ.getValue();
        if (px != 0 || py != 0 || pz != 0) matrices.translate(px, py, pz);

        // Apply custom rotation for ALL modes (1 to 7 and Custom)
        if (customRotateX.getValue() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(customRotateX.getValue()));
        if (customRotateY.getValue() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(customRotateY.getValue()));
        if (customRotateZ.getValue() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(customRotateZ.getValue()));

        String currentMode = mode.getCurrentMode();

        switch (currentMode) {
            case "Первый" -> {
                if (swingProgress > 0) {
                    float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    matrices.translate(0.56F, equipProgress * -0.2f - 0.5F, -0.7F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -85.0F * power / 5.0f));
                    matrices.translate(-0.1F, 0.28F, 0.2F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0F));
                } else {
                    float n = -0.4f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float m = 0.2f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI * 2);
                    float f1 = -0.2f * MathHelper.sin(swingProgress * (float) Math.PI);
                    matrices.translate(n, m, f1);
                    applyEquipOffset(matrices, arm, equipProgress);
                    applySwingOffset(matrices, arm, swingProgress);
                }
            }
            case "Второй" -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f + 20f * g * power / 5.0f));
            }
            case "Третий" -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30f * (1f - g) - 30f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f));
            }
            case "Четвёртый" -> {
                float g = MathHelper.sin(swingProgress * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0.1F, -0.2F, -0.3F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30f * g * power / 5.0f - 36f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25f * g * power / 5.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(12f));
            }
            case "Шестой" -> {
                matrices.scale(scaleValue, scaleValue, scaleValue);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15 * anim));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-(power * 10) * anim));
            }
            case "Седьмой" -> {
                matrices.scale(scaleValue + 0.1f, scaleValue, scaleValue - 0.1f);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0.2f * anim, 0, -0.5f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * anim * power / 5.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-140 * anim * power / 5.0f));
            }
            case "Кастомный" -> {
                // Equip offset всегда применяется
                applyEquipOffset(matrices, arm, equipProgress);
                // Кривая анимации удара (для idle rotation)
                float curve = anim;

                // Базовые повороты (статические)
                if (customBasePitch.getValue() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(customBasePitch.getValue()));
                if (customBaseYaw.getValue() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(customBaseYaw.getValue()));
                if (customBaseRoll.getValue() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(customBaseRoll.getValue()));

                // Вращение (idle)
                if (customIdleRotate.isEnabled()) {
                    float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(time * customIdleRotateSpeedX.getValue()));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * customIdleRotateSpeedY.getValue()));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * customIdleRotateSpeedZ.getValue()));
                }

                matrices.scale(customScaleX.getValue(), customScaleY.getValue(), customScaleZ.getValue());
            }
        }
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }
}
