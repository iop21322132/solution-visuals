package dev.simplevisuals.client.util.math;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Angle {
    private float yaw;
    private float pitch;

    public Angle(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Angle addYaw(float yaw) {
        return new Angle(this.yaw + yaw, this.pitch);
    }

    public Angle addPitch(float pitch) {
        return new Angle(this.yaw, this.pitch + pitch);
    }

    public Vec3d toVector() {
        // Convert yaw and pitch to direction vector
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float g = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float h = -MathHelper.cos(-pitch * 0.017453292F);
        float i = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(g * h, i, f * h);
    }
}