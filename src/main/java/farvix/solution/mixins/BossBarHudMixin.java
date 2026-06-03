package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.NoRender;
import farvix.solution.client.modules.impl.visuals.DynamicIsland;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void noBossBarHook(DrawContext context, CallbackInfo ci) {
        NoRender m = Client.getInstance().getModuleManager().get(NoRender.class);
        if (m != null && m.isEnabled() && m.noBossBar.isEnabled()) {
            ci.cancel();
            return;
        }

        // Если включен Dynamic Island и активен PVP таймер, отменяем стандартный рендер BossBar
        DynamicIsland di = Client.getInstance().getModuleManager().get(DynamicIsland.class);
        if (di != null && di.isEnabled() && di.isPvpMode()) {
            ci.cancel();
        }
    }
}
