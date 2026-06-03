package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.math.MathUtil;
import dev.simplevisuals.client.util.renderer.Render3D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import dev.simplevisuals.client.util.perf.Perf;
import dev.simplevisuals.simplevisuals;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Trails extends Module implements ThemeManager.ThemeChangeListener {
    // Translation keys as constants
    private static final String LINES_TRANSLATION = I18n.translate("setting.trails.lines");
    private static final String PARTICLES_TRANSLATION = I18n.translate("setting.trails.particles");

    // Настройки для типа трэйла (single select - можно выбрать только один вариант)
    private final ListSetting trailType = new ListSetting(
            "setting.trails.type",
            true, // single select = true
            new BooleanSetting(LINES_TRANSLATION, true),
            new BooleanSetting(PARTICLES_TRANSLATION, false)
    );

    // Настройки для линейного трэйла
    private final NumberSetting lineLength = new NumberSetting(I18n.translate("setting.trails.line_length"), 20f, 5f, 200f, 1f);
    private final NumberSetting lineLifetime = new NumberSetting(I18n.translate("setting.trails.line_lifetime"), 0.5f, 0.1f, 3.0f, 0.1f);
    private final NumberSetting lineWidth = new NumberSetting(I18n.translate("setting.trails.width"), 0.5f, 0.1f, 2.0f, 0.1f);

    // Настройки для партиклового трэйла
    private final NumberSetting particleLength = new NumberSetting(I18n.translate("setting.trails.particle_length"), 100f, 20f, 300f, 10f);
    private final NumberSetting particleSize = new NumberSetting(I18n.translate("setting.trails.particle_size"), 0.22f, 0.05f, 0.6f, 0.01f);
    private final NumberSetting particleLifetime = new NumberSetting(I18n.translate("setting.trails.particle_lifetime"), 1.6f, 0.5f, 5.0f, 0.1f);
    private final NumberSetting spawnDistance = new NumberSetting(I18n.translate("setting.trails.spawn_distance"), 0.25f, 0.05f, 1.0f, 0.01f);

    // Текстуры для партиклов
    private final ListSetting textures = new ListSetting(
            I18n.translate("setting.textures"),
            false,
            new BooleanSetting(I18n.translate("setting.star"), true),
            new BooleanSetting(I18n.translate("setting.heart"), false),
            new BooleanSetting(I18n.translate("setting.dollar"), false),
            new BooleanSetting(I18n.translate("setting.circle"), false),
            new BooleanSetting(I18n.translate("setting.amongus"), false)
    );

    // Идентификаторы текстур
    private static final Identifier STAR = simplevisuals.id("hud/star.png");
    private static final Identifier HEART = simplevisuals.id("hud/heart.png");
    private static final Identifier DOLLAR = simplevisuals.id("hud/dollar.png");
    private static final Identifier CIRCLE = simplevisuals.id("hud/circle.png");
    private static final Identifier AMONGUS = simplevisuals.id("hud/amongus.png");

    private record TrailPoint(Vec3d pos, long timeMs) {}

    private final class ParticlePoint {
        Vec3d pos;
        final Identifier tex;
        final long createdTime;
        final float size;
        Vec3d motion;
        Vec3d animatedMotion;
        final long aliveTimeMs;

        ParticlePoint(Vec3d position, Identifier tex, long now, float baseSize) {
            this.pos = position;
            this.tex = tex;
            this.createdTime = now;
            this.size = (float) (baseSize * MathUtil.getRandom(0.9, 1.3));
            this.aliveTimeMs = (long)(particleLifetime.getValue() * 1000);

            // лёгкое дрожание
            this.motion = new Vec3d(
                    MathUtil.getRandom(-0.004, 0.004),
                    MathUtil.getRandom(-0.003, 0.003),
                    MathUtil.getRandom(-0.004, 0.004)
            );
            this.animatedMotion = Vec3d.ZERO;
        }

        boolean isAlive(long now) {
            return now - createdTime < aliveTimeMs;
        }

        float getLifeProgress(long now) {
            return Math.min(1.0f, (float)(now - createdTime) / (float)aliveTimeMs);
        }

        void updateMotion() {
            double my = MathUtil.getRandom(-0.002, 0.002);
            motion = new Vec3d(motion.x * 0.995, my, motion.z * 0.995);

            float ax = MathUtil.fast((float) animatedMotion.x, (float) motion.x, 1f);
            float ay = MathUtil.fast((float) animatedMotion.y, (float) motion.y, 1f);
            float az = MathUtil.fast((float) animatedMotion.z, (float) motion.z, 1f);
            animatedMotion = new Vec3d(ax, ay, az);

            pos = pos.add(animatedMotion);
        }
    }

    private final ThemeManager themeManager;
    private final Map<PlayerEntity, Deque<TrailPoint>> lineTrails = new IdentityHashMap<>();
    private final Map<PlayerEntity, Deque<ParticlePoint>> particleTrails = new ConcurrentHashMap<>();
    private final Map<PlayerEntity, Vec3d> lastSamplePos = new ConcurrentHashMap<>();

    public Trails() {
        super(I18n.translate("Trail"), Category.Render, I18n.translate("module.trails.description"));
        this.themeManager = ThemeManager.getInstance();
        themeManager.addThemeChangeListener(this);

        // Добавляем настройки
        getSettings().add(trailType);

        // Линейный трэйл настройки
        getSettings().add(lineLength);
        getSettings().add(lineLifetime);
        getSettings().add(lineWidth);

        // Партикловый трэйл настройки
        getSettings().add(particleLength);
        getSettings().add(particleSize);
        getSettings().add(particleLifetime);
        getSettings().add(spawnDistance);
        getSettings().add(textures);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) { }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        try (var __ = Perf.scopeCpu("Trails.onRender3D")) {
            boolean useLines = trailType.getName(LINES_TRANSLATION).getValue();
            boolean useParticles = trailType.getName(PARTICLES_TRANSLATION).getValue();

            if (useLines) {
                renderLineTrails(e);
            } else if (useParticles) {
                renderParticleTrails(e);
            }
        }
    }

    private void renderLineTrails(EventRender3D.Game e) {
        Render3D.prepare();
        float tickDelta = e.getTickDelta();

        // Получаем актуальный цвет темы
        Color base = ThemeManager.getInstance().getCurrentTheme().getBackgroundColor();
        int baseR = base.getRed();
        int baseG = base.getGreen();
        int baseB = base.getBlue();
        int baseA = 255;

        int maxPoints = lineLength.getValue().intValue();
        long customLifetimeMs = (long)(lineLifetime.getValue() * 1000);
        float edgeWidth = lineWidth.getValue();
        long now = System.currentTimeMillis();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player) continue;
            if (mc.options.getPerspective().isFirstPerson()) continue;

            Deque<TrailPoint> q = lineTrails.computeIfAbsent(p, k -> new ArrayDeque<>());

            // Удаляем устаревшие точки
            while (!q.isEmpty() && now - q.peekFirst().timeMs > customLifetimeMs) {
                q.removeFirst();
            }

            double h = p.getEyeHeight(p.getPose());
            Vec3d origin = p.getLerpedPos(tickDelta);
            Vec3d originTop = origin.add(0.0, h, 0.0);

            if (q.isEmpty() || q.getLast().pos.squaredDistanceTo(origin) > 0.0001) {
                q.addLast(new TrailPoint(origin, now));
                while (q.size() > maxPoints) q.removeFirst();
            }

            if (q.size() >= 2) {
                List<TrailPoint> list = new ArrayList<>(q);
                int sz = list.size();

                // Рендерим квады
                for (int i = 0; i < sz - 1; i++) {
                    TrailPoint a = list.get(i);
                    TrailPoint b = list.get(i + 1);

                    float tA = (float) i / (float) (sz - 1);
                    float tB = (float) (i + 1) / (float) (sz - 1);
                    int ageA = (int) Math.min(255, Math.max(0, baseA * tA));
                    int ageB = (int) Math.min(255, Math.max(0, baseA * tB));
                    Color ca = new Color(baseR, baseG, baseB, ageA);
                    Color cb = new Color(baseR, baseG, baseB, ageB);

                    addQuadVertical(a.pos, b.pos, originTop.subtract(origin), ca, cb);
                }

                // Рендерим линии
                for (int i = 0; i < sz - 1; i++) {
                    TrailPoint a = list.get(i);
                    TrailPoint b = list.get(i + 1);
                    float t = (float) i / (float) (sz - 1);
                    int age = (int) Math.min(255, Math.max(0, baseA * t));
                    int rgba = new Color(baseR, baseG, baseB, age).getRGB();
                    Render3D.drawLine(a.pos, b.pos, rgba, edgeWidth);
                    Render3D.drawLine(a.pos.add(0, h, 0), b.pos.add(0, h, 0), rgba, edgeWidth);
                }
            }
        }
        Render3D.render();
    }

    private void renderParticleTrails(EventRender3D.Game e) {
        if (mc.world == null || mc.player == null) return;

        List<Identifier> texList = getSelectedTextures();
        if (texList.isEmpty()) return;

        int maxParticles = particleLength.getValue().intValue();
        float step = spawnDistance.getValue();
        float baseSize = particleSize.getValue();
        long now = System.currentTimeMillis();
        float tickDelta = e.getTickDelta();

        // Обновляем партиклы только для локального игрока
        PlayerEntity p = mc.player;
        if (p != null && !p.isInvisible()) {
            // Проверяем движение игрока
            double dx = p.getX() - p.prevX;
            double dz = p.getZ() - p.prevZ;

            // Спавним партиклы только если игрок движется
            if (dx * dx + dz * dz >= 0.0004) {
                Box bb = p.getBoundingBox();
                double centerX = (bb.minX + bb.maxX) * 0.5;
                double centerZ = (bb.minZ + bb.maxZ) * 0.5;
                double minY = bb.minY;
                double maxY = bb.maxY;
                double halfWidth = (bb.maxX - bb.minX) * 0.5;
                double halfDepth = (bb.maxZ - bb.minZ) * 0.5;

                // Направление взгляда и "спина"
                float yaw = p.getYaw(tickDelta);
                double yawRad = Math.toRadians(yaw);
                Vec3d forward = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
                Vec3d backDir = forward.multiply(-1.0).normalize();
                Vec3d sideDir = new Vec3d(backDir.z, 0.0, -backDir.x).normalize();

                // Базовая точка на задней грани хитбокса
                double backDist = halfDepth + 0.2;
                Vec3d baseBack = new Vec3d(centerX, 0.0, centerZ).add(backDir.multiply(backDist));

                // Рандомные смещения
                double sideT = MathUtil.getRandom(-1.0, 1.0);
                Vec3d sideOffset = sideDir.multiply(sideT * halfWidth);
                double randY = MathUtil.getRandom(minY, maxY);

                Vec3d backPos = new Vec3d(
                        baseBack.x + sideOffset.x,
                        randY,
                        baseBack.z + sideOffset.z
                );

                Vec3d last = lastSamplePos.get(p);
                if (last == null || backPos.squaredDistanceTo(last) >= step * step) {
                    lastSamplePos.put(p, backPos);

                    Deque<ParticlePoint> deque = particleTrails.computeIfAbsent(p, k -> new ArrayDeque<>());

                    // Удаляем старые партиклы
                    while (!deque.isEmpty() && !deque.peekFirst().isAlive(now)) {
                        deque.removeFirst();
                    }

                    // Добавляем новый партикл
                    if (deque.size() < maxParticles) {
                        Identifier tex = texList.get(ThreadLocalRandom.current().nextInt(texList.size()));
                        deque.addLast(new ParticlePoint(backPos, tex, now, baseSize));
                    } else {
                        // Если достигли максимума, удаляем самый старый
                        deque.removeFirst();
                        Identifier tex = texList.get(ThreadLocalRandom.current().nextInt(texList.size()));
                        deque.addLast(new ParticlePoint(backPos, tex, now, baseSize));
                    }
                }
            }
        }

        // Рендерим партиклы
        Deque<ParticlePoint> deque = particleTrails.get(p);
        if (deque != null && !deque.isEmpty()) {
            Color theme = themeManager.getCurrentTheme().getBackgroundColor();
            List<ParticlePoint> particles = new ArrayList<>(deque);
            int sz = particles.size();

            for (int i = 0; i < sz; i++) {
                ParticlePoint pt = particles.get(i);
                if (!pt.isAlive(now)) continue;

                // Обновляем движение партикла
                pt.updateMotion();

                // Рассчитываем прозрачность
                float t = (float) i / (float) Math.max(1, sz - 1);
                float life = pt.getLifeProgress(now);
                float fade = (1.0f - life) * (1.0f - t * 0.25f);
                int alpha = (int) (210 * fade);
                if (alpha <= 5) continue;

                float size = pt.size * (0.7f + 0.5f * (1.0f - t));
                Color color = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), alpha);

                // Рендерим текстуру
                Render3D.drawBillboardTexture(e.getMatrices(), pt.pos, size, pt.tex, color);
            }
        }
    }

    private List<Identifier> getSelectedTextures() {
        List<Identifier> list = new ArrayList<>();
        for (BooleanSetting b : textures.getToggled()) {
            String n = b.getName();
            if (I18n.translate("setting.star").equals(n)) list.add(STAR);
            else if (I18n.translate("setting.heart").equals(n)) list.add(HEART);
            else if (I18n.translate("setting.dollar").equals(n)) list.add(DOLLAR);
            else if (I18n.translate("setting.circle").equals(n)) list.add(CIRCLE);
            else if (I18n.translate("setting.amongus").equals(n)) list.add(AMONGUS);
        }
        return list;
    }

    private void addQuadVertical(Vec3d aBottom, Vec3d bBottom, Vec3d up, Color ca, Color cb) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d aB = aBottom.subtract(cam);
        Vec3d aT = aBottom.add(up).subtract(cam);
        Vec3d bB = bBottom.subtract(cam);
        Vec3d bT = bBottom.add(up).subtract(cam);

        var matrices = new net.minecraft.client.util.math.MatrixStack();
        var matrix = matrices.peek().getPositionMatrix();
        Render3D.Vertex[] vertices = {
                new Render3D.Vertex(matrix, (float) aB.x, (float) aB.y, (float) aB.z, ca.getRGB()),
                new Render3D.Vertex(matrix, (float) aT.x, (float) aT.y, (float) aT.z, ca.getRGB()),
                new Render3D.Vertex(matrix, (float) bT.x, (float) bT.y, (float) bT.z, cb.getRGB()),
                new Render3D.Vertex(matrix, (float) bB.x, (float) bB.y, (float) bB.z, cb.getRGB())
        };
        Render3D.QUADS.add(new Render3D.VertexCollection(vertices));
    }

    @Override
    public void onDisable() {
        lineTrails.clear();
        particleTrails.clear();
        lastSamplePos.clear();
        super.onDisable();
    }
}