package farvix.solution.api.events.impl.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import farvix.solution.api.events.Event;

public class EventRender3D extends Event {

    public static class Game extends Event {
        private final RenderTickCounter tickCounter;
        private final MatrixStack matrixStack;

        public Game(RenderTickCounter tickCounter, MatrixStack matrixStack) {
            this.tickCounter = tickCounter;
            this.matrixStack = matrixStack;
        }

        public float getTickDelta() {
            return tickCounter.getTickDelta(true);
        }

        public RenderTickCounter getTickCounter() {
            return tickCounter;
        }

        public MatrixStack getMatrices() {
            return matrixStack;
        }

        public MatrixStack getMatrixStack() {
            return matrixStack;
        }

        public float getPartialTicks() {
            return getTickDelta();
        }
    }

    public static class World extends Event {
        private final Camera camera;
        private final Matrix4f positionMatrix;
        private final RenderTickCounter tickCounter;

        public World(Camera camera, Matrix4f positionMatrix, RenderTickCounter tickCounter) {
            this.camera = camera;
            this.positionMatrix = positionMatrix;
            this.tickCounter = tickCounter;
        }

        public Camera getCamera() {
            return camera;
        }

        public Matrix4f getPositionMatrix() {
            return positionMatrix;
        }

        public float getTickDelta() {
            return tickCounter.getTickDelta(true);
        }

        public RenderTickCounter getTickCounter() {
            return tickCounter;
        }
    }
}
