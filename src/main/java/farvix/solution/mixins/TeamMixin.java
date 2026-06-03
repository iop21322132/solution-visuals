package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.environment.Streamer;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractTeam.class)
public class TeamMixin {

    @Inject(method = "decorateName", at = @At("RETURN"), cancellable = true, require = 0)
    private void onDecorateName(net.minecraft.scoreboard.ScoreHolder scoreHolder,
                                 CallbackInfoReturnable<Text> cir) {
        try {
            Streamer s = Client.getInstance().getModuleManager().get(Streamer.class);
            if (s == null || !s.isEnabled()) return;
            Text result = cir.getReturnValue();
            if (result == null) return;
            String orig = result.getString();
            String replaced = s.replace(orig);
            if (!replaced.equals(orig)) cir.setReturnValue(Text.literal(replaced));
        } catch (Throwable ignored) {}
    }
}
