package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.simplevisuals;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.client.managers.ThemeManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WorldParticles extends Module implements ThemeManager.ThemeChangeListener {

    private final NumberSetting particleSize = new NumberSetting("Размер", 0.15f, 0.05f, 0.20f, 0.01f);
    private final NumberSetting maxParticles = new NumberSetting("Количество", 150f, 20f, 500f, 5f);
    private final NumberSetting spawnInterval = new NumberSetting("Интервал спавна", 60f, 10f, 200f, 10f);
    private final NumberSetting sphereRadius = new NumberSetting("Радиус сферы", 10f, 3f, 30f, 1f);
    private final NumberSetting spawnConeAngle = new NumberSetting("Угол спавна", 120f, 60f, 170f, 5f);
    private final NumberSetting speedMultiplier = new NumberSetting("Скорость", 0.5f, 0.1f, 1.0f, 0.1f);
    private final NumberSetting rotationSpeedMultiplier = new NumberSetting("Скорость вращения", 1.0f, 0f, 3.0f, 0.1f);

    private final ListSetting textureMode = new ListSetting(
            "Текстура",
            false,
            new BooleanSetting("Свечение", true),
            new BooleanSetting("Круг", false),
            new BooleanSetting("Квадрат", false),
            new BooleanSetting("Звезда", false),
            new BooleanSetting("Сердце", false),
            new BooleanSetting("Доллар", false),
            new BooleanSetting("Амонгус", false)
    );

    private final ListSetting mode = new ListSetting(
            "Режим",
            true,
            new BooleanSetting("Простой", true),
            new BooleanSetting("Взлет", false)
    );

    private final BooleanSetting randomColor = new BooleanSetting("Рандомный цвет", false);
    private final BooleanSetting additiveBlend = new BooleanSetting("Аддитивный блендинг", false);

    private static final Identifier GLOW = simplevisuals.id("hud/glow.png");
    private static final Identifier CIRCLE = simplevisuals.id("hud/circle.png");
    private static final Identifier SQUARE = simplevisuals.id("hud/quad.png");
    private static final Identifier STAR = simplevisuals.id("hud/star.png");
    private static final Identifier HEART = simplevisuals.id("hud/heart.png");
    private static final Identifier DOLLAR = simplevisuals.id("hud/dollar.png");
    private static final Identifier AMONGUS = simplevisuals.id("hud/amongus.png");

    private final ThemeManager themeManager;
    private Color currentColor;
    private final List<Particle> particles = new ArrayList<>();
    private long lastSpawnTime = 0;

    // VBO rendering system (simplified - using only batching)
    private boolean vboInitialized = false;

    private double getViewCosine() {
        double cone = Math.max(10.0, Math.min(170.0, spawnConeAngle.getValue()));
        return Math.cos(Math.toRadians(cone * 0.5));
    }

    public WorldParticles() {
        super("WorldParticles", Category.Render, "Красивые частицы снега");
        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        long now = System.currentTimeMillis();

        if (now - lastSpawnTime >= spawnInterval.getValue()) {
            spawnParticle();
            lastSpawnTime = now;
        }

        particles.removeIf(p -> {
            if (now - p.spawnTime > p.lifeTime) return true;
            p.updateMotion(isVzletMode(), speedMultiplier.getValue());
            return p.pos.y < -2;
        });

        // Render particles using VBO system
        renderParticlesVBO(e);
    }

    private void spawnParticle() {
        ensureSpace();

        double radius = sphereRadius.getValue();
        Vec3d pos;
        Vec3d vel;

        float speed = speedMultiplier.getValue();
        Vec3d lookDir = mc.player.getRotationVec(1.0f).normalize();
        double viewCosine = getViewCosine();

        if (isVzletMode()) {
            // Взлет: частицы под игроком с разбросом по сфере
            Vec3d chosenOffset = null;
            for (int attempt = 0; attempt < 8; attempt++) {
                double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
                double r = ThreadLocalRandom.current().nextDouble() * radius * 0.5;
                double offsetX = Math.cos(angle) * r;
                double offsetZ = Math.sin(angle) * r;
                Vec3d offset = new Vec3d(offsetX, -0.5, offsetZ);
                Vec3d dir = offset.normalize();
                if (dir.dotProduct(lookDir) >= viewCosine) {
                    chosenOffset = offset;
                    break;
                }
            }
            if (chosenOffset == null) {
                chosenOffset = lookDir.normalize().multiply(radius * 0.3).add(0, -0.5, 0);
            }
            pos = mc.player.getPos().add(chosenOffset);
            vel = new Vec3d(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.02 * speed,
                    (ThreadLocalRandom.current().nextDouble() * 0.08 + 0.04) * speed,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.02 * speed
            );
        } else {
            // Сфера: частицы в сфере вокруг игрока, но не перед лицом
            // Генерируем случайные сферические координаты
            Vec3d offset = null;
            for (int attempt = 0; attempt < 16; attempt++) {
                double theta = ThreadLocalRandom.current().nextDouble() * Math.PI * 2; // азимутальный угол
                double phi = Math.acos(2 * ThreadLocalRandom.current().nextDouble() - 1); // полярный угол
                double r = ThreadLocalRandom.current().nextDouble() * radius;

                double x = r * Math.sin(phi) * Math.cos(theta);
                double y = r * Math.sin(phi) * Math.sin(theta);
                double z = r * Math.cos(phi);

                Vec3d candidate = new Vec3d(x, y, z);
                if (candidate.lengthSquared() < 1e-4) continue;
                Vec3d dir = candidate.normalize();
                if (lookDir.dotProduct(dir) >= viewCosine) {
                    offset = candidate;
                    break;
                }
            }
            if (offset == null) {
                offset = randomPerpendicularOffset(lookDir, radius);
            }

            pos = mc.player.getPos().add(offset.x, offset.y + mc.player.getEyeHeight(mc.player.getPose()), offset.z);
            vel = new Vec3d(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.02 * speed,
                    -ThreadLocalRandom.current().nextDouble() * 0.02 * speed - 0.01 * speed,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.02 * speed
            );
        }

        // Live theme color so gradient themes animate; random overrides if enabled
        Color themeColor = themeManager.getCurrentTheme().getBackgroundColor();
        Color color = randomColor.getValue()
                ? new Color(ThreadLocalRandom.current().nextInt(256),
                ThreadLocalRandom.current().nextInt(256),
                ThreadLocalRandom.current().nextInt(256))
                : themeColor;

        // Get random texture from selected textures
        Identifier selectedTexture = getRandomSelectedTexture();
        particles.add(new Particle(pos, vel, 5000, color, selectedTexture));
    }

    private void ensureSpace() {
        while (particles.size() >= maxParticles.getValue() && !particles.isEmpty()) {
            particles.remove(0);
        }
    }

    private boolean isInFrustum(Vec3d pos) {
        return ((dev.simplevisuals.mixin.accessors.IWorldRenderer) mc.worldRenderer)
                .getFrustum()
                .isVisible(new net.minecraft.util.math.Box(pos.add(-0.2, -0.2, -0.2), pos.add(0.2, 0.2, 0.2)));
    }

    private boolean isVzletMode() {
        BooleanSetting vzlet = mode.getName("Взлет");
        return vzlet != null && vzlet.getValue();
    }

    private Identifier getRandomSelectedTexture() {
        List<Identifier> selectedTextures = getSelectedTextures();
        if (selectedTextures.isEmpty()) {
            return GLOW; // default if nothing selected
        }
        return selectedTextures.get(ThreadLocalRandom.current().nextInt(selectedTextures.size()));
    }

    private List<Identifier> getSelectedTextures() {
        List<Identifier> list = new ArrayList<>();
        for (BooleanSetting b : textureMode.getToggled()) {
            String n = b.getName();
            if ("Свечение".equals(n)) list.add(GLOW);
            else if ("Круг".equals(n)) list.add(CIRCLE);
            else if ("Квадрат".equals(n)) list.add(SQUARE);
            else if ("Звезда".equals(n)) list.add(STAR);
            else if ("Сердце".equals(n)) list.add(HEART);
            else if ("Доллар".equals(n)) list.add(DOLLAR);
            else if ("Амонгус".equals(n)) list.add(AMONGUS);
        }
        return list;
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = theme.getBackgroundColor();
        // Обновляем цвет у всех существующих частиц
        for (Particle p : particles) {
            p.color = this.currentColor;
        }
    }

    public void onDisable() {
        vboInitialized = false;
        particles.clear();
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

    private void renderParticlesVBO(EventRender3D.Game e) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        if (particles.isEmpty()) return;

        // Mark as initialized (simplified VBO system)
        if (!vboInitialized) {
            vboInitialized = true;
        }

        // Setup rendering state
        RenderSystem.enableBlend();
        if (additiveBlend.getValue()) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        // Group particles by texture for batching
        Map<Identifier, List<Particle>> particlesByTexture = new HashMap<>();
        for (Particle p : particles) {
            if (!isInFrustum(p.pos)) continue;
            particlesByTexture.computeIfAbsent(p.texture, k -> new ArrayList<>()).add(p);
        }

        // Render each texture batch
        for (Map.Entry<Identifier, List<Particle>> entry : particlesByTexture.entrySet()) {
            renderTextureBatch(e, entry.getKey(), entry.getValue(), cameraPos);
        }

        // Cleanup
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private void renderTextureBatch(EventRender3D.Game e, Identifier texture, List<Particle> textureParticles, Vec3d cameraPos) {
        if (textureParticles.isEmpty()) return;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(e.getMatrices().peek().getPositionMatrix());

        for (Particle p : textureParticles) {
            float life = p.getLifeProgress(System.currentTimeMillis());
            float baseSize = particleSize.getValue();

            float sizeFactor;
            if (life < 0.2f) sizeFactor = life / 0.2f;
            else if (life > 0.8f) sizeFactor = (1 - life) / 0.2f;
            else sizeFactor = 1f;

            float heightFactor = 1f;
            if (!isVzletMode() && p.pos.y < 1.5) heightFactor = (float) (p.pos.y / 1.5f);

            float size = baseSize * sizeFactor * heightFactor;
            int alpha = Math.max(0, Math.min(255, (int) (255 * sizeFactor * heightFactor)));

            Color color = new Color(
                    p.color.getRed(),
                    p.color.getGreen(),
                    p.color.getBlue(),
                    alpha
            );

            // Calculate billboard position relative to camera
            Vec3d relativePos = p.pos.subtract(cameraPos);

            matrices.push();
            matrices.translate(relativePos.x, relativePos.y, relativePos.z);
            matrices.multiply(mc.getEntityRenderDispatcher().camera.getRotation());
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotation));

            float s = size / 2f;
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            // Add quad vertices
            buffer.vertex(matrix, -s, -s, 0).texture(0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            buffer.vertex(matrix, -s, s, 0).texture(0, 1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            buffer.vertex(matrix, s, s, 0).texture(1, 1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            buffer.vertex(matrix, s, -s, 0).texture(1, 0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

            matrices.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private class Particle {
        Vec3d pos;
        Vec3d vel;
        long spawnTime;
        long lifeTime;
        Color color;
        Identifier texture;
        float rotation;
        float rotationSpeed;

        Particle(Vec3d pos, Vec3d vel, long lifeTime, Color color, Identifier texture) {
            this.pos = pos;
            this.vel = vel;
            this.spawnTime = System.currentTimeMillis();
            this.lifeTime = lifeTime;
            this.color = color;
            this.texture = texture;
            this.rotation = ThreadLocalRandom.current().nextFloat() * 360f;
            float speed = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 6f;
            this.rotationSpeed = Math.abs(speed) < 0.8f ? Math.copySign(0.8f, speed == 0 ? 1 : speed) : speed;
        }

        void updateMotion(boolean vzlet, float speedMultiplier) {
            double motionRandom = 0.002 * speedMultiplier;

            vel = vel.add(
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * motionRandom,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * motionRandom * 0.5,
                    (ThreadLocalRandom.current().nextDouble() - 0.5) * motionRandom
            );

            // Применяем множитель скорости к базовой скорости движения
            if (vzlet) {
                vel = new Vec3d(
                        vel.x * speedMultiplier,
                        0.03 * speedMultiplier,
                        vel.z * speedMultiplier
                );
            } else {
                vel = new Vec3d(
                        vel.x * speedMultiplier,
                        (-0.005 + Math.sin((System.currentTimeMillis() - spawnTime) / 500.0) * 0.002) * speedMultiplier,
                        vel.z * speedMultiplier
                );
            }

            pos = pos.add(vel);

            float rotationScale = rotationSpeedMultiplier.getValue();
            rotation += rotationSpeed * speedMultiplier * rotationScale;
            if (rotation > 360f) rotation -= 360f;
            else if (rotation < 0f) rotation += 360f;
        }

        float getLifeProgress(long now) {
            return (float) (now - spawnTime) / lifeTime;
        }
    }

    private Vec3d randomPerpendicularOffset(Vec3d lookDir, double radius) {
        Vec3d dir = lookDir.normalize();
        for (int i = 0; i < 8; i++) {
            Vec3d random = new Vec3d(
                    ThreadLocalRandom.current().nextDouble(-1.0, 1.0),
                    ThreadLocalRandom.current().nextDouble(-1.0, 1.0),
                    ThreadLocalRandom.current().nextDouble(-1.0, 1.0)
            );
            Vec3d perpendicular = random.crossProduct(dir);
            if (perpendicular.lengthSquared() < 1e-4) continue;
            perpendicular = perpendicular.normalize();
            double distance = radius * (0.4 + ThreadLocalRandom.current().nextDouble() * 0.6);
            Vec3d forward = dir.multiply(radius * 0.25);
            return forward.add(perpendicular.multiply(distance));
        }
        return dir.multiply(radius * 0.6);
    }
}