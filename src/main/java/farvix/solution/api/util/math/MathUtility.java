package farvix.solution.api.util.math;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import farvix.solution.api.interfaces.QuickImports;

import static net.minecraft.util.math.MathHelper.lerp;

@UtilityClass
public class MathUtility implements QuickImports {
    public float random(float min, float max) {
        return (new CustRandom().randomNumber(0,1,false) * (max - min) + min);
    }

    public int floorNearestMulN(int x, int n) {
        return n * (int) Math.floor((double) x / (double) n);
    }

    public float textScrolling(float textWidth) {
        int speed = (int) (textWidth * 75);
        return (float) MathHelper.clamp((System.currentTimeMillis() % speed * Math.PI / speed), 0, 1) * textWidth;
    }

    public Vec3d interpolate(Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        return new Vec3d(interpolate(entity.prevX, entity.getX()), interpolate(entity.prevY, entity.getY()), interpolate(entity.prevZ, entity.getZ()));
    }

    public Vector3d interpolate(Vector3d prevPos, Vector3d pos) {
        return new Vector3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
    }

    public Vec3d interpolate(Vec3d prevPos, Vec3d pos) {
        return new Vec3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
    }

    public float interpolate(float prev, float orig) {
        return lerp(mc.getRenderTickCounter().getTickDelta(false), prev, orig);
    }

    public double interpolate(double prev, double orig) {
        return lerp(mc.getRenderTickCounter().getTickDelta(false), prev, orig);
    }
}
