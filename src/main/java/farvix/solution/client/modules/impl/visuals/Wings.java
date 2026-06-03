package farvix.solution.client.modules.impl.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import farvix.solution.api.events.impl.render.EventRender3D;
import farvix.solution.api.settings.impl.BooleanSetting;
import farvix.solution.api.settings.impl.ColorSetting;
import farvix.solution.api.settings.impl.SliderSetting;
import farvix.solution.api.util.color.FixColor;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import farvix.solution.api.util.FriendManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "Wings", category = ModuleCategory.VISUALS, description = "Полупрозрачные крылья за спиной")
public class Wings extends Module {

    private static final float DEFAULT_SPREAD = 8.0f;
    private static final int  DEFAULT_ALPHA   = 220;

    private static final WingPoint[] SHAPE = {
            new WingPoint(0.08f,  0.10f,  0.88f),
            new WingPoint(0.28f,  0.34f,  0.78f),
            new WingPoint(0.56f,  0.82f,  0.62f),
            new WingPoint(0.86f,  0.30f,  0.52f),
            new WingPoint(1.14f,  0.46f,  0.40f),
            new WingPoint(1.24f,  0.04f,  0.30f),
            new WingPoint(1.02f, -0.18f,  0.28f),
            new WingPoint(1.18f, -0.64f,  0.22f),
            new WingPoint(0.86f, -0.46f,  0.20f),
            new WingPoint(0.80f, -0.98f,  0.14f),
            new WingPoint(0.54f, -0.74f,  0.16f),
            new WingPoint(0.30f, -1.16f,  0.12f),
            new WingPoint(0.10f, -0.54f,  0.18f)
    };

    private final BooleanSetting self = new BooleanSetting("На себя", this);
    private final BooleanSetting opponents = new BooleanSetting("На противников", this);
    private final BooleanSetting friends = new BooleanSetting("На друзей", this);
    private final SliderSetting size = new SliderSetting("Размер", this, 1.0f, 0.75f, 1.35f, 0.05f);
    private final ColorSetting color = new ColorSetting("Цвет", this, new FixColor(100, 150, 255, 220).getRGB());

    private float   selfBodyYaw;
    private boolean selfBodyYawInitialized;

    public Wings() {
        self.setValue(true);
        opponents.setValue(false);
        friends.setValue(false);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;

        MatrixStack stack     = event.getMatrices();
        float       tickDelta = event.getTickDelta();

        stack.push();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        if (self.getValue() && !mc.options.getPerspective().isFirstPerson()
                && mc.player.isAlive()
                && !hasElytra(mc.player)
                && shouldRenderWings(mc.player)) {
            try { renderWings(stack, mc.player, tickDelta); } catch (Exception ignored) {}
        }

        boolean renderOpponents = opponents.getValue();
        boolean renderFriends = friends.getValue();

        if (renderOpponents || renderFriends) {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof PlayerEntity player) || player == mc.player) continue;
                if (!player.isAlive() || hasElytra(player) || !shouldRenderWings(player)) continue;

                boolean isFriend = FriendManager.isFriend(player.getName().getString());
                if (isFriend && !renderFriends) continue;
                if (!isFriend && !renderOpponents) continue;

                try { renderWings(stack, player, tickDelta); } catch (Exception ignored) {}
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        stack.pop();
    }

    private boolean hasElytra(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private boolean shouldRenderWings(PlayerEntity player) {
        if (player.isInvisible()) {
            boolean hasArmor = !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                    || !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                    || !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                    || !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
            return hasArmor;
        }
        return true;
    }

    private void renderWings(MatrixStack stack, PlayerEntity player, float tickDelta) {
        Vec3d pos = player.getLerpedPos(tickDelta);

        float bodyYaw  = resolveBodyYaw(player, tickDelta);
        float move     = MathHelper.clamp(player.limbAnimator.getSpeed(tickDelta), 0f, 1f);

        WingPose pose      = resolvePose(player, tickDelta);
        if (pose == null) return;
        float flap      = (float) Math.sin((player.age + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
        float    open      = (DEFAULT_SPREAD + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
        float    wingScale = size.getValue() * pose.scaleMultiplier;

        int baseColor    = resolveBaseColor();
        int glowColor    = resolveGlowColor(baseColor);
        int coreColor    = resolveCoreColor(baseColor);
        int outlineColor = baseColor;

        stack.push();
        stack.translate(pos.x, pos.y, pos.z);
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));
        if (pose.preTranslateY != 0f || pose.preTranslateZ != 0f)
            stack.translate(0f, pose.preTranslateY, pose.preTranslateZ);
        if (pose.pitchRotation != 0f)
            stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
        if (pose.rollRotation != 0f)
            stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(pose.rollRotation));
        stack.translate(0f, pose.anchorY, pose.anchorZ);
        stack.scale(wingScale, wingScale, wingScale);

        renderWingSide(stack, -1f, open, baseColor, glowColor, coreColor, outlineColor, pose);
        renderWingSide(stack,  1f, open, baseColor, glowColor, coreColor, outlineColor, pose);
        stack.pop();
    }

    private void renderWingSide(MatrixStack stack, float side, float open,
                                int baseColor, int glowColor, int coreColor, int outlineColor,
                                WingPose pose) {
        stack.push();
        stack.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(side * pose.sideRoll));
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitch));

        int userAlpha = alpha(baseColor);

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawWingLayer(stack, side, 1.22f, setAlpha(glowColor, (int)(userAlpha * 0.22f)), setAlpha(glowColor, 0));
        drawWingLayer(stack, side, 0.84f, setAlpha(coreColor, (int)(userAlpha * 0.26f)), setAlpha(coreColor, 0));

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        drawWingLayer(stack, side, 1.0f, setAlpha(baseColor, userAlpha), setAlpha(baseColor, (int)(userAlpha * (10f / 220f))));

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawWingOutline(stack, side, 1.0f, setAlpha(outlineColor, (int)(userAlpha * 0.62f)));
        drawWingRibs(stack, side, 0.96f, setAlpha(glowColor, (int)(userAlpha * 0.20f)));

        stack.pop();
    }

    private void drawWingLayer(MatrixStack stack, float side, float scale, int rootColor, int edgeColor) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SHAPE.length; i++) {
            WingPoint cur  = SHAPE[i];
            WingPoint next = SHAPE[(i + 1) % SHAPE.length];
            vertex(buffer, matrix, 0f, 0f, 0f, rootColor);
            vertex(buffer, matrix, side * cur.x  * scale, cur.y  * scale, 0f, applyPointAlpha(edgeColor, cur.alphaMul));
            vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0f, applyPointAlpha(edgeColor, next.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawWingOutline(MatrixStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        RenderSystem.lineWidth(1.35f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (WingPoint point : SHAPE)
            vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0f, color);

        vertex(buffer, matrix, side * SHAPE[0].x * scale, SHAPE[0].y * scale, 0f, color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private void drawWingRibs(MatrixStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        int[] ribIndices = {2, 4, 7, 9, 11};
        RenderSystem.lineWidth(0.9f);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        for (int idx : ribIndices) {
            WingPoint point = SHAPE[idx];
            vertex(buffer, matrix, 0f, 0f, 0f, setAlpha(color, Math.max(8, (int)(alpha(color) * 0.75f))));
            vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0f, applyPointAlpha(color, point.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private int applyPointAlpha(int color, float multiplier) {
        return setAlpha(color, Math.max(0, Math.min(255, (int)(alpha(color) * multiplier))));
    }

    private static int setAlpha(int color, int a) {
        return (MathHelper.clamp(a, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int alpha(int color) { return (color >> 24) & 0xFF; }
    private static int red(int color)   { return (color >> 16) & 0xFF; }
    private static int green(int color) { return (color >>  8) & 0xFF; }
    private static int blue(int color)  { return  color        & 0xFF; }

    private static int getColor(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
        buffer.vertex(matrix, x, y, z)
                .color(red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f);
    }

    private int resolveBaseColor() {
        return color.get();
    }

    private int resolveGlowColor(int base) {
        return interpolateColor(base, getColor(255, 255, 255, 255), 0.28f);
    }

    private int resolveCoreColor(int base) {
        return interpolateColor(base, getColor(255, 255, 255, 255), 0.55f);
    }

    private int interpolateColor(int colorStart, int colorEnd, float t) {
        int a1 = (colorStart >> 24) & 0xFF;
        int r1 = (colorStart >> 16) & 0xFF;
        int g1 = (colorStart >> 8) & 0xFF;
        int b1 = colorStart & 0xFF;

        int a2 = (colorEnd >> 24) & 0xFF;
        int r2 = (colorEnd >> 16) & 0xFF;
        int g2 = (colorEnd >> 8) & 0xFF;
        int b2 = colorEnd & 0xFF;

        int a = (int) (a1 + t * (a2 - a1));
        int r = (int) (r1 + t * (r2 - r1));
        int g = (int) (g1 + t * (g2 - g1));
        int b = (int) (b1 + t * (b2 - b1));

        return (MathHelper.clamp(a, 0, 255) << 24) |
               (MathHelper.clamp(r, 0, 255) << 16) |
               (MathHelper.clamp(g, 0, 255) << 8) |
               MathHelper.clamp(b, 0, 255);
    }

    private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
        float target = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        if (player != mc.player) return target;
        if (!selfBodyYawInitialized || player.age < 2) {
            selfBodyYaw = target;
            selfBodyYawInitialized = true;
            return selfBodyYaw;
        }
        selfBodyYaw = approachDegrees(selfBodyYaw, target, 14f);
        return selfBodyYaw;
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = MathHelper.wrapDegrees(target - current);
        delta = MathHelper.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    private WingPose resolvePose(PlayerEntity player, float tickDelta) {
        float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        if (player.isGliding()) {
            float flightTicks    = (float) player.getGlidingTicks() + tickDelta;
            float flightProgress = MathHelper.clamp(flightTicks * flightTicks / 100f, 0f, 1f);
            float pitchRotation  = flightProgress * (-90f - pitch);
            return new WingPose(0.34f, 0.46f, 0f, 0f, pitchRotation, 0f,
                    0.76f, 0.92f, 0.10f, 0.58f, 0.05f, 0.06f, -5f, -2f, 0.13f);
        }

        if (player.isTouchingWater()) {
            return null;
        }

        if (player.isSneaking()) {
            return new WingPose(1.175f, 0f, -0.12f, 0.11f, -28.6f, 0f,
                    1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, 4f, 0.12f);
        }

        return new WingPose(0f, 0f, 1.38f, 0.10f, 0f, 0f,
                1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
    }

    @Override
    public void onDisable() {
        selfBodyYawInitialized = false;
        super.onDisable();
    }

    private static final class WingPoint {
        final float x, y, alphaMul;
        WingPoint(float x, float y, float alphaMul) { this.x = x; this.y = y; this.alphaMul = alphaMul; }
    }

    private static final class WingPose {
        final float preTranslateY, preTranslateZ;
        final float anchorY, anchorZ;
        final float pitchRotation, rollRotation;
        final float openMultiplier, scaleMultiplier;
        final float motionSpreadBoost, flapAmplitude;
        final float sideOffset, sideYOffset, sideZOffset;
        final float sideRoll, sidePitch, flapSpeed;

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ,
                 float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier,
                 float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideZOffset,
                 float sideRoll, float sidePitch, float flapSpeed) {
            this(preTranslateY, preTranslateZ, anchorY, anchorZ, pitchRotation, rollRotation,
                    openMultiplier, scaleMultiplier, motionSpreadBoost, flapAmplitude,
                    sideOffset, 0f, sideZOffset, sideRoll, sidePitch, flapSpeed);
        }

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ,
                 float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier,
                 float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideYOffset,
                 float sideZOffset, float sideRoll, float sidePitch, float flapSpeed) {
            this.preTranslateY   = preTranslateY;
            this.preTranslateZ   = preTranslateZ;
            this.anchorY         = anchorY;
            this.anchorZ         = anchorZ;
            this.pitchRotation   = pitchRotation;
            this.rollRotation    = rollRotation;
            this.openMultiplier  = openMultiplier;
            this.scaleMultiplier = scaleMultiplier;
            this.motionSpreadBoost = motionSpreadBoost;
            this.flapAmplitude   = flapAmplitude;
            this.sideOffset      = sideOffset;
            this.sideYOffset     = sideYOffset;
            this.sideZOffset     = sideZOffset;
            this.sideRoll        = sideRoll;
            this.sidePitch       = sidePitch;
            this.flapSpeed       = flapSpeed;
        }
    }
}
