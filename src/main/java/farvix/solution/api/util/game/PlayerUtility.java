package farvix.solution.api.util.game;

import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import org.joml.Vector3d;
import farvix.solution.api.events.Event;
import farvix.solution.api.events.impl.game.*;
import farvix.solution.api.interfaces.QuickImports;

@UtilityClass
public class PlayerUtility implements QuickImports {
    public double getBps(Entity entity) {
        double x = entity.getX() - entity.prevX;
        double y = entity.getY() - entity.prevY;
        double z = entity.getZ() - entity.prevZ;
        return Math.sqrt((x * x) + (y * y) + (z * z)) * 20.0D;
    }
    public Block getBlock(int x, int y, int z) {
        return mc.player == null ? Blocks.AIR : mc.world.getBlockState(mc.player.getBlockPos().add(x, y, z)).getBlock();
    }
    public void setSpeed(double speed) {
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();
        if (forward == 0 && strafe == 0) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        } else {
            if (forward != 0) {
                if (strafe > 0) {
                    yaw += (float) (forward > 0 ? -45 : 45);
                } else if (strafe < 0) {
                    yaw += (float) (forward > 0 ? 45 : -45);
                }
                strafe = 0;
                if (forward > 0) {
                    forward = 1;
                } else if (forward < 0) {
                    forward = -1;
                }
            }
            double sin = MathHelper.sin((float) Math.toRadians(yaw + 90));
            double cos = MathHelper.cos((float) Math.toRadians(yaw + 90));
            mc.player.setVelocity(forward * speed * cos + strafe * speed * sin, mc.player.getVelocity().y, forward * speed * sin - strafe * speed * cos);
        }
    }

    public double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }
    public boolean collideWith(LivingEntity entity) {
        return collideWith(entity, 0);
    }

    public boolean collideWith(LivingEntity entity, float grow) {
        Box box = mc.player.getBoundingBox();
        Box targetbox = entity.getBoundingBox().expand(grow, 0, grow);

        if (box.maxX > targetbox.minX
                && box.maxY > targetbox.minY
                && box.maxZ > targetbox.minZ
                && box.minX < targetbox.maxX
                && box.minY < targetbox.maxY
                && box.minZ < targetbox.maxZ) return true;

        return false;
    }
    public Vector2f get(Vec3d target) {
        double posX = target.x - mc.player.getX();
        double posY = target.y - (mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose()));
        double posZ = target.z - mc.player.getZ();
        double sqrt = MathHelper.sqrt((float) (posX * posX + posZ * posZ));
        float yaw = (float) (Math.atan2(posZ, posX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(Math.atan2(posY, sqrt) * 180.0 / Math.PI));
        float sens = (float) (Math.pow(mc.options.getMouseSensitivity().getValue(), 1.5) * 0.05f + 0.1f);
        float pow = sens * sens * sens * 1.2F;
        yaw -= yaw % pow;
        pitch -= pitch % (pow * sens);
        return new Vector2f(yaw, pitch);
    }


    public enum Correction {
        NONE,
        SILENT,
        STRICT,
        FULL,
        CLIENT
    }
    public boolean isCollide(Block block) {
        Box playerBox = mc.player.getBoundingBox();
        int minX = (int) Math.floor(playerBox.minX);
        int minY = (int) Math.floor(playerBox.minY);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxX = (int) Math.ceil(playerBox.maxX);
        int maxY = (int) Math.ceil(playerBox.maxY);
        int maxZ = (int) Math.ceil(playerBox.maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == block) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public boolean isMoving() {
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }
}
