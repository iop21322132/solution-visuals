package farvix.solution.api.util.render;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.util.math.MathUtility;

@UtilityClass
public class ProjectionUtility implements QuickImports {
    public Matrix4f lastProjMat = new Matrix4f();
    public MatrixStack.Entry lastWorldSpaceMatrix = new MatrixStack().peek();

    public @NotNull Vec3d worldSpaceToScreenSpace(Vec3d pos) {
        Vector3f delta = pos.toVector3f();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        Vector4f transformedCoordinates = new Vector4f(delta.x, delta.y, delta.z, 1.f).mul(lastWorldSpaceMatrix.getPositionMatrix());
        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        matrixProj.project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

        return new Vec3d(target.x / mc.getWindow().getScaleFactor(), (mc.getWindow().getHeight() - target.y) / mc.getWindow().getScaleFactor(), target.z);
    }

    public Vector4d getVector4D(Entity ent) {
        Vector4d position = null;
        if (ent != null) {
            for (Vec3d vector : getVec3ds(ent, MathUtility.interpolate(ent))) {
                vector = worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
                if (vector.z > 0 && vector.z < 1) {
                    if (position == null) position = new Vector4d(vector.x, vector.y, vector.z, 0);
                    position.x = Math.min(vector.x, position.x);
                    position.y = Math.min(vector.y, position.y);
                    position.z = Math.max(vector.x, position.z);
                    position.w = Math.max(vector.y, position.w);
                }
            }
        }
        return position;
    }

    public @NotNull Vec3d[] getVec3ds(Entity ent, Vec3d pos) {
        Box axisAlignedBB2 = ent.getBoundingBox();
        Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + pos.x - 0.1F, axisAlignedBB2.minY - ent.getY() + pos.y - 0.1F, axisAlignedBB2.minZ - ent.getZ() + pos.z - 0.1F, axisAlignedBB2.maxX - ent.getX() + pos.x + 0.1F, axisAlignedBB2.maxY - ent.getY() + pos.y + 0.1F, axisAlignedBB2.maxZ - ent.getZ() + pos.z + 0.1F);
        return new Vec3d[]{new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
    }

    public boolean outOfScreen(Vector4d vec) {
        return vec == null || (vec.x < 0 && vec.z < 1) || (vec.y < 0 && vec.w < 1);
    }

    public double centerX(Vector4d vec) {
        return vec.x + (vec.z - vec.x) / 2;
    }

    public boolean canSee(Box box) {
        Frustum frustum = mc.worldRenderer.frustum;
        return box != null && frustum != null && frustum.isVisible(box);
    }
}
