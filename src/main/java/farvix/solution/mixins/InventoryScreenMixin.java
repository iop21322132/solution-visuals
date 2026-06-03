package farvix.solution.mixins;

import farvix.solution.api.TempColor;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.rect.ShapeProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin implements QuickImports {

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void drawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        farvix.solution.client.modules.impl.visuals.HudInventory module =
                farvix.solution.Client.getInstance().getModuleManager()
                        .get(farvix.solution.client.modules.impl.visuals.HudInventory.class);

        if (module == null || !module.isDark()) return; // use vanilla background

        InventoryScreen self = (InventoryScreen)(Object)this;

        int x = (self.width  - 176) / 2;
        int y = (self.height - 166) / 2;
        int w = 176;
        int h = 166;

        glass.render(ShapeProperties.create(context.getMatrices(),
                x - 1, y - 1, w + 2, h + 2)
                .round(12)
                .softness(1.5f)
                .thickness(2.5f)
                .outlineColor(TempColor.getGuiBorder().alpha(module.getBgAlpha().getValue()).getRGB())
                .color(TempColor.getGuiBackground().alpha(module.getBgAlpha().getValue()).getRGB())
                .build());

        InventoryScreen.drawEntity(context, x + 26, y + 8, x + 75, y + 78, 30, 0.0625f, (float)mouseX, (float)mouseY, mc.player);

        ci.cancel();
    }
}
