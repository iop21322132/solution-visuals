package farvix.solution.mixins;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import farvix.solution.Client;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.client.modules.impl.environment.AutoSprint;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin implements QuickImports {

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void onIsPressed(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == mc.options.sprintKey) {
           AutoSprint autoSprint = Client.getInstance().getModuleManager().get(AutoSprint.class);
           if (autoSprint.isEnabled()) {
               cir.setReturnValue(autoSprint.isCanSprint());
           }
        }
    }
}
