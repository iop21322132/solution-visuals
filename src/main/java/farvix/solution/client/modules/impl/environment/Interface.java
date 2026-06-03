package farvix.solution.client.modules.impl.environment;

import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import farvix.solution.client.modules.impl.environment.interfaces.WatermarkUI;
import farvix.solution.api.ui.clickgui.InterfaceScreen;
import net.minecraft.client.gui.screen.ChatScreen;

@Setter @Getter
@ModuleInfo(name = "Interface", category = ModuleCategory.VISUALS, description = "Ватермарка, время и FPS на экране")
public class Interface extends Module implements farvix.solution.api.ui.hud.IHudElement {

    public final BooleanSetting showWatermark = new BooleanSetting("Ватермарка", this);
    public final BooleanSetting showTime      = new BooleanSetting("Время",      this);
    public final BooleanSetting showFps       = new BooleanSetting("Кол-во кадров в секунду", this);
    public final BooleanSetting showCoords    = new BooleanSetting("Координаты", this);
    public final BooleanSetting merged        = new BooleanSetting("Слитно", this);
    public final farvix.solution.api.settings.impl.SliderSetting bgAlpha =
            new farvix.solution.api.settings.impl.SliderSetting("Прозрачность фона", this, 1.0f, 0f, 1f, 0.05f);

    {
        showWatermark.setEnabled(true);
        showTime.setEnabled(true);
        showFps.setEnabled(true);
        showCoords.setEnabled(true);
        merged.setEnabled(true);
    }

    WatermarkUI watermarkUI = new WatermarkUI();

    @EventHandler
    public void onRender2D(EventRender2D e) {
        boolean canDrag = mc.currentScreen instanceof InterfaceScreen
                || mc.currentScreen instanceof ChatScreen;
        watermarkUI.setDragEnabled(canDrag);
        watermarkUI.render(e, showWatermark.isEnabled(), showTime.isEnabled(), showFps.isEnabled(), showCoords.isEnabled(), merged.isEnabled(), bgAlpha.getValue());
    }

    // ── IHudElement implementation ────────────────────────────────────────────
    @Override
    public float getHudX() {
        return watermarkUI != null ? watermarkUI.getX() : 0;
    }

    @Override
    public float getHudY() {
        return watermarkUI != null ? watermarkUI.getY() : 0;
    }

    @Override
    public float getHudWidth() {
        return watermarkUI != null ? watermarkUI.getWidth() : 0;
    }

    @Override
    public float getHudHeight() {
        return watermarkUI != null ? watermarkUI.getHeight() : 0;
    }

    @Override
    public Module getModule() {
        return this;
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        float x = getHudX();
        float y = getHudY();
        float w = getHudWidth();
        float h = getHudHeight();
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
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
