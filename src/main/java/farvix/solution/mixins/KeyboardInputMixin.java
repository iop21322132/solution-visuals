package farvix.solution.mixins;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.*;
import farvix.solution.api.events.impl.game.EventInputMove;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends Input {
    @Final @Shadow
    private GameOptions settings;

    @Unique
    private static float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        } else {
            return positive ? 1.0F : -1.0F;
        }
    }

    /**
     * @author etc1337
     * @reason sorry #2
     */

    @Overwrite()
    public void tick() {
        this.playerInput = new PlayerInput(this.settings.forwardKey.isPressed(), this.settings.backKey.isPressed(), this.settings.leftKey.isPressed(), this.settings.rightKey.isPressed(), this.settings.jumpKey.isPressed(), this.settings.sneakKey.isPressed(), this.settings.sprintKey.isPressed());
        this.movementForward = getMovementMultiplier(this.playerInput.forward(), this.playerInput.backward());
        this.movementSideways = getMovementMultiplier(this.playerInput.left(), this.playerInput.right());

        EventInputMove event = new EventInputMove(this.movementForward, this.movementSideways, this.settings.jumpKey.isPressed(), this.settings.sneakKey.isPressed(), this.settings.sprintKey.isPressed());
        event.call();

        if (event.isCancelled()) return;

        this.playerInput = new PlayerInput(this.settings.forwardKey.isPressed(),
                this.settings.backKey.isPressed(),
                this.settings.leftKey.isPressed(),
                this.settings.rightKey.isPressed(),
                event.isJump(),
                event.isSneaking(),
                event.isSprint());

        this.movementForward = event.getForward();
        this.movementSideways = event.getStrafe();
    }
}
