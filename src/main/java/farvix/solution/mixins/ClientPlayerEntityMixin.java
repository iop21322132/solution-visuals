package farvix.solution.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import farvix.solution.Client;
import farvix.solution.api.events.impl.game.EventMotion;
import farvix.solution.api.events.impl.game.EventUpdate;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.client.modules.impl.environment.AutoSprint;

@Mixin(value = ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity implements QuickImports {
    @Shadow
    public abstract boolean isSneaking();

    @Shadow
    public abstract boolean isUsingItem();

    @Shadow
    public abstract boolean isSubmergedInWater();

    @Shadow
    public abstract void sendAbilitiesUpdate();

    ;

    @Shadow
    protected abstract void sendSprintingPacket();

    @Shadow
    protected abstract boolean isCamera();

    @Shadow
    private double lastX;
    @Shadow
    private double lastBaseY;
    @Shadow
    private double lastZ;
    @Shadow
    private float lastYaw;
    @Shadow
    private float lastPitch;
    @Shadow
    private int ticksSinceLastPositionPacketSent;
    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;
    @Shadow
    private boolean lastOnGround;
    @Shadow
    private boolean lastHorizontalCollision;
    @Shadow
    private boolean autoJumpEnabled;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.BEFORE))
    public void tick(CallbackInfo callbackInfo) {
        new EventUpdate().call();
    }

    @Inject(method = "shouldStopSprinting", at = @At(value = "HEAD"), cancellable = true)
    public void shouldStopSprinting(CallbackInfoReturnable<Boolean> cir) {
        AutoSprint autoSprint = Client.getInstance().getModuleManager().get(AutoSprint.class);
        if (autoSprint.isEnabled() && !autoSprint.isCanSprint()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canStartSprinting", at = @At(value = "HEAD"), cancellable = true)
    public void canStartSprinting(CallbackInfoReturnable<Boolean> cir) {
        AutoSprint autoSprint = Client.getInstance().getModuleManager().get(AutoSprint.class);
        if (autoSprint.isEnabled() && !autoSprint.isCanSprint()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * @author etc1337
     * @reason sorry
     */
    @Overwrite
    private void sendMovementPackets() {
        EventMotion event = new EventMotion(getYaw(), getPitch(), isOnGround()).call();

        this.sendSprintingPacket();
        if (this.isCamera()) {
            double d = this.getX() - this.lastX;
            double e = this.getY() - this.lastBaseY;
            double f = this.getZ() - this.lastZ;
            double g = (double)(event.getYaw() - this.lastYaw);
            double h = (double)(event.getPitch() - this.lastPitch);
            ++this.ticksSinceLastPositionPacketSent;
            boolean bl = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
            boolean bl2 = g != (double)0.0F || h != (double)0.0F;
            if (bl && bl2) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(this.getX(), this.getY(), this.getZ(), event.getYaw(), event.getPitch(), event.isGround(), this.horizontalCollision));
            } else if (bl) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(this.getX(), this.getY(), this.getZ(), event.isGround(), this.horizontalCollision));
            } else if (bl2) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(event.getYaw(), event.getPitch(), event.isGround(), this.horizontalCollision));
            } else if (this.lastOnGround != event.isGround() || this.lastHorizontalCollision != this.horizontalCollision) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(event.isGround(), this.horizontalCollision));
            }

            if (bl) {
                this.lastX = this.getX();
                this.lastBaseY = this.getY();
                this.lastZ = this.getZ();
                this.ticksSinceLastPositionPacketSent = 0;
            }

            if (bl2) {
                this.lastYaw = event.getYaw();
                this.lastPitch = event.getPitch();
            }

            this.lastOnGround = event.isGround();
            this.lastHorizontalCollision = this.horizontalCollision;
            this.autoJumpEnabled = mc.options.getAutoJump().getValue();
        }
    }
}
