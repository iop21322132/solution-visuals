package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

@ModuleInfo(name = "No Render", category = ModuleCategory.VISUALS,
        description = "Убирает визуальные помехи")
public class NoRender extends Module {

    public final BooleanSetting noHurtCam  = new BooleanSetting("Выключить тряску экрана", this);
    public final BooleanSetting noFire     = new BooleanSetting("Убрать огонь",             this);
    public final BooleanSetting noTotem    = new BooleanSetting("Убрать тотем",             this);
    public final BooleanSetting noParticles= new BooleanSetting("Убрать частицы",           this);
    public final BooleanSetting noBossBar  = new BooleanSetting("Убрать босс-бар",          this);
    public final BooleanSetting noScoreboard = new BooleanSetting("Убрать скорборд",        this);

    public NoRender() {
    }
}
