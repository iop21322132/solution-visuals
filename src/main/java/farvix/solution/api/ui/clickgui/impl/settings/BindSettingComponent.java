package farvix.solution.api.ui.clickgui.impl.settings;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import farvix.solution.api.TempColor;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;
import farvix.solution.api.settings.Setting;
import farvix.solution.api.settings.impl.BindSetting;
import farvix.solution.api.ui.clickgui.ModuleComponent;
import farvix.solution.api.ui.clickgui.api.SettingComponent;
import farvix.solution.api.util.other.KeyUtility;

import java.awt.*;

@Getter
public class BindSettingComponent extends SettingComponent {

    private BindSetting bindSetting;
    private boolean binding;

    public BindSettingComponent(Setting setting, ModuleComponent moduleComponent) {
        super(setting, moduleComponent);
    }

    @Override
    public void init() {
        this.bindSetting = (BindSetting) getSetting();
        this.width = moduleComponent.getWidth();
        this.height = 18;
        super.init();
    }

    @Override
    public void render(DrawContext context, float x, float y, int mouseX, int mouseY, float delta) {
        super.render(context, x, y, mouseX, mouseY, delta);

        float alpha = getClickGUI().getAlpha().getValue();

        String keyName = KeyUtility.getKeyboardKey(this.getBindSetting().getKey());

        float keyTextWidth = Fonts.DEFAULT.get(13).getStringWidth(keyName);
        float bindButtonWidth = Math.max(20, keyTextWidth + 8);
        float bindButtonHeight = 13;

        float bindButtonX = x + width - bindButtonWidth - 8;
        float bindButtonY = y + (height - bindButtonHeight) / 2f;

        Color bindBg = new farvix.solution.api.util.color.FixColor(30, 33, 40, (int)(248 * alpha)).getColor();
        Color bindBorder = binding
                ? TempColor.getClientColor().alpha(alpha)
                : new farvix.solution.api.util.color.FixColor(60, 65, 75, (int)(248 * alpha)).getColor();

        blur.render(ShapeProperties.create(context.getMatrices(), bindButtonX, bindButtonY, bindButtonWidth, bindButtonHeight)
                .round(3)
                .softness(1.5f)
                .thickness(1f)
                .outlineColor(bindBorder.getRGB())
                .color(bindBg.getRGB())
                .build());

        int textColor = binding
                ? TempColor.getClientColor().alpha(alpha).getRGB()
                : new farvix.solution.api.util.color.FixColor(210, 215, 225, (int)(220 * alpha)).getRGB();
        Fonts.DEFAULT.get(13).drawCenteredBoldString(context.getMatrices(), keyName,
                bindButtonX + bindButtonWidth / 2f, bindButtonY + bindButtonHeight / 2f - 1.5f,
                textColor);

        Fonts.DEFAULT.get(15).drawBoldString(context.getMatrices(), getTranslatedName(),
                x + 10f, y + 4f,
                TempColor.getTextPrimary().alpha(alpha).getRGB());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        String keyName = KeyUtility.getKeyboardKey(this.getBindSetting().getKey());
        float keyTextWidth = Fonts.DEFAULT.get(13).getStringWidth(keyName);
        float bindButtonWidth = Math.max(20, keyTextWidth + 8);
        float bindButtonHeight = 13;
        float bindButtonX = x + width - bindButtonWidth - 8;
        float bindButtonY = y + (height - bindButtonHeight) / 2f;

        boolean clickedOnBind = mouseX >= bindButtonX && mouseX <= bindButtonX + bindButtonWidth &&
                               mouseY >= bindButtonY && mouseY <= bindButtonY + bindButtonHeight;

        if (clickedOnBind && button == 0) {
            binding = true;
        }

        if (binding && button != 0) {
            bindSetting.setKey(button);
            binding = false;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bindSetting.setKey(0);
            } else {
                bindSetting.setKey(keyCode);
            }
            binding = false;
        }
    }

    private farvix.solution.api.ui.clickgui.InterfaceScreen getClickGUI() {
        return (farvix.solution.api.ui.clickgui.InterfaceScreen) mc.currentScreen;
    }
}
