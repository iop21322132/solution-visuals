package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.impl.EventAttackEntity;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.simplevisuals;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.gl.ShaderProgramKeys;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Identifier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.Optional;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HitBubbles extends Module implements ThemeManager.ThemeChangeListener {

    // Настройки
    private final NumberSetting size = new NumberSetting("setting.size", 0.5f, 0.2f, 2.0f, 0.05f);
    private final NumberSetting lifeTime = new NumberSetting("setting.lifeTime", 1.5f, 0.5f, 3.0f, 0.1f);
    private final NumberSetting rotationSpeed = new NumberSetting("setting.rotationSpeed", 1.0f, 0.0f, 3.0f, 0.1f);
    private final BooleanSetting additiveBlending = new BooleanSetting("setting.additiveBlending", false);

    private final Identifier bubbleTex = simplevisuals.id("textures/bubble.png");

    private final List<Particle> particles = new ArrayList<>();
    private final ThemeManager themeManager;
    private Color currentColor;

    public HitBubbles() {
        super("HitBubbles", Category.Render, "module.hitbubbles.description");
        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);

    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = theme.getBackgroundColor();
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        particles.clear();
        super.onDisable();
    }

    @EventHandler
    private void onAttackEntity(EventAttackEntity e) {
        if (fullNullCheck()) return;
        if (e.getTarget() == mc.player) return;
        if (!(e.getTarget() instanceof LivingEntity target)) return;
        if (!target.isAlive()) return;
        if (!e.isEffectsAllowed()) return;
        if (!e.canProcess()) return;

        // Получаем точку удара
        Vec3d hitPos;
        HitResult ch = mc.crosshairTarget;
        if (ch != null && ch.getType() == HitResult.Type.ENTITY) {
            EntityHitResult ehr = (EntityHitResult) ch;
            if (ehr.getEntity() == e.getTarget()) {
                hitPos = ehr.getPos();
            } else {
                hitPos = computeHitOnEntityAABB(target);
            }
        } else {
            hitPos = computeHitOnEntityAABB(target);
        }
        if (hitPos == null) {
            hitPos = target.getPos().add(0, target.getHeight() / 2f, 0);
        }

        // Создаем новую частицу
        particles.add(new Particle(particles.size(), hitPos));
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        if (particles.isEmpty()) return;

        // Удаляем старые частицы и обновляем анимации
        Iterator<Particle> iterator = particles.iterator();
        long currentTime = System.currentTimeMillis();

        while (iterator.hasNext()) {
            Particle p = iterator.next();

            // Удаляем если время жизни истекло
            if (currentTime - p.spawnTime > (long)(lifeTime.getValue() * 1000)) {
                iterator.remove();
                continue;
            }
        }

        if (particles.isEmpty()) return;

        // Настройка рендеринга
        RenderSystem.enableBlend();
        if (additiveBlending.getValue()) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        } else {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();

        // Рендерим все частицы
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, bubbleTex);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = e.getMatrices().peek().getPositionMatrix();
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();

        boolean hasVertices = false;

        for (Particle p : particles) {
            float progress = p.getProgress(currentTime);

            // Пропускаем если прозрачность 0
            if (progress <= 0.01f) continue;

            // Размер частицы
            float particleSize = size.getValue() * getSizeProgress(progress);
            float halfSize = particleSize / 2f;

            // Цвет из темы с анимацией прозрачности
            Color themeColor = themeManager.getCurrentTheme().getBackgroundColor();
            int alpha = (int)(255 * progress);
            Color drawColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha);

            float red = drawColor.getRed() / 255.0f;
            float green = drawColor.getGreen() / 255.0f;
            float blue = drawColor.getBlue() / 255.0f;
            float alphaValue = drawColor.getAlpha() / 255.0f;

            // Вращение частицы
            float rotationAngle = (float)(currentTime - p.spawnTime) / 1000f * rotationSpeed.getValue() * 360f;

            // Рассчитываем вращение
            double cosRot = Math.cos(Math.toRadians(rotationAngle));
            double sinRot = Math.sin(Math.toRadians(rotationAngle));

            // Создаем billboard, ориентированный на камеру
            Vec3d toCamera = cameraPos.subtract(p.pos).normalize();

            // Создаем локальную систему координат
            Vec3d up = new Vec3d(0, 1, 0);
            Vec3d right = up.crossProduct(toCamera).normalize();

            // Если right слишком мал (камера смотрит прямо вверх/вниз), используем другой базис
            if (right.lengthSquared() < 0.01) {
                right = new Vec3d(1, 0, 0).crossProduct(toCamera).normalize();
            }

            up = toCamera.crossProduct(right).normalize();

            // Применяем вращение вокруг оси toCamera
            Vec3d rotatedRight = right.multiply(cosRot).add(up.multiply(sinRot));
            Vec3d rotatedUp = up.multiply(cosRot).subtract(right.multiply(sinRot));

            // Масштабируем векторы
            Vec3d scaledRight = rotatedRight.multiply(halfSize);
            Vec3d scaledUp = rotatedUp.multiply(halfSize);

            // Вычисляем углы квада
            Vec3d bottomLeft = p.pos.subtract(scaledRight).subtract(scaledUp);
            Vec3d bottomRight = p.pos.add(scaledRight).subtract(scaledUp);
            Vec3d topRight = p.pos.add(scaledRight).add(scaledUp);
            Vec3d topLeft = p.pos.subtract(scaledRight).add(scaledUp);

            // Переводим в локальные координаты относительно камеры
            bottomLeft = bottomLeft.subtract(cameraPos);
            bottomRight = bottomRight.subtract(cameraPos);
            topRight = topRight.subtract(cameraPos);
            topLeft = topLeft.subtract(cameraPos);

            // Рисуем квад
            buffer.vertex(matrix, (float)bottomLeft.x, (float)bottomLeft.y, (float)bottomLeft.z)
                    .texture(0.0f, 1.0f).color(red, green, blue, alphaValue);
            buffer.vertex(matrix, (float)bottomRight.x, (float)bottomRight.y, (float)bottomRight.z)
                    .texture(1.0f, 1.0f).color(red, green, blue, alphaValue);
            buffer.vertex(matrix, (float)topRight.x, (float)topRight.y, (float)topRight.z)
                    .texture(1.0f, 0.0f).color(red, green, blue, alphaValue);
            buffer.vertex(matrix, (float)topLeft.x, (float)topLeft.y, (float)topLeft.z)
                    .texture(0.0f, 0.0f).color(red, green, blue, alphaValue);

            hasVertices = true;
        }

        // Рисуем только если были добавлены вершины
        if (hasVertices) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } else {
        }

        // Восстанавливаем настройки
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private float getSizeProgress(float progress) {
        // Плавное появление и исчезновение
        if (progress < 0.1f) {
            return progress / 0.1f; // Появление за первые 10%
        } else if (progress > 0.9f) {
            return 1.0f - ((progress - 0.9f) / 0.1f); // Исчезновение за последние 10%
        }
        return 1.0f; // Полный размер в середине анимации
    }

    private Vec3d computeHitOnEntityAABB(LivingEntity entity) {
        Vec3d start = mc.player.getEyePos();
        Vec3d dir = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(6.0));
        Box bb = entity.getBoundingBox();
        Optional<Vec3d> res = bb.raycast(start, end);
        return res.orElse(null);
    }

    private class Particle {
        final int index;
        final Vec3d pos;
        final long spawnTime;

        Particle(int index, Vec3d pos) {
            this.index = index;
            this.pos = pos;
            this.spawnTime = System.currentTimeMillis();
        }

        public float getProgress(long currentTime) {
            float lifetime = lifeTime.getValue();
            long elapsed = currentTime - spawnTime;
            float progress = (float)elapsed / (lifetime * 1000f);

            // Ограничиваем прогресс от 0 до 1
            return Math.max(0, Math.min(1, progress));
        }
    }
}