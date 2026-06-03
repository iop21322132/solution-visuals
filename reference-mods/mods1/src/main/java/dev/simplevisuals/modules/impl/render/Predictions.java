package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.DrawEvent;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render3D;
import java.awt.Color;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.*;
import net.minecraft.client.gl.ShaderProgramKeys;
import org.lwjgl.opengl.GL11;

public class Predictions extends Module implements ThemeManager.ThemeChangeListener {

    private final List<Point> points = new ArrayList<>();
    private final List<TrajectoryPoint> trajectoryPoints = new ArrayList<>();

    // Кэш для плавного перемещения
    private Vec3d previousFramePlayerPos = null;
    private Vec3d smoothPlayerPos = null;
    private Vec3d smoothPlayerMotion = Vec3d.ZERO;
    private float interpolationAlpha = 0f;
    private long lastFrameTime = 0;
    private Vec3d lastHitPos = null;
    private float hitPosLerpFactor = 0f;

    // Кэш для оптимизации
    private static final int MAX_TRAJECTORY_POINTS = 100; // Ограничение точек траектории
    private static final int MAX_PROJECTILES_TO_RENDER = 10; // Максимум снарядов для рендеринга

    private final ThemeManager themeManager;

    public Predictions() {
        super("Predictions", Category.Render, I18n.translate("module.predictions.description"));
        this.themeManager = ThemeManager.getInstance();
        themeManager.addThemeChangeListener(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // Сброс значений при включении
        previousFramePlayerPos = null;
        smoothPlayerPos = null;
        smoothPlayerMotion = Vec3d.ZERO;
        lastHitPos = null;
        hitPosLerpFactor = 0f;
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();

        for (Point point : points) {
            // Get camera and window info
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();

            int ticks = point.ticks;

            // Use Minecraft's text renderer
            net.minecraft.client.font.TextRenderer textRenderer = mc.textRenderer;
            double time = ticks * 50 / 1000.0;
            String text = String.format("%.1f", time) + " сек";
            int textWidth = textRenderer.getWidth(text);

            // Simple position in center - you'll need to implement proper projection
            float posX = screenWidth / 2f;
            float posY = screenHeight / 2f;

            float padding = 3;
            float iconSize = 8;
            float adjustedPosX = posX + textWidth / 2 - 6;
            float adjustedPosY = posY + 4;

            // Draw background
            context.fill(
                    (int)(adjustedPosX - textWidth + iconSize + padding),
                    (int)(adjustedPosY - padding),
                    (int)(adjustedPosX + padding + textWidth),
                    (int)(adjustedPosY + 10),
                    0x80000000
            );

            // Draw text
            context.drawText(textRenderer, text,
                    (int)(adjustedPosX - textWidth + 8 + padding * 2),
                    (int)(adjustedPosY + 0.5F), -1, true
            );

            // Draw item stack
            context.drawItem(point.stack,
                    (int)(adjustedPosX - textWidth - padding + 2),
                    (int)(adjustedPosY - padding)
            );
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (mc.player == null || mc.world == null) return;

        Render3D.prepare();
        points.clear();
        trajectoryPoints.clear();

        // Обновляем интерполяцию
        updateInterpolation();

        // Use matrix stack from event
        MatrixStack matrixStack = e.getMatrices();
        drawPredictionInHand(matrixStack, mc.player.getHandItems());

        // Оптимизация: ограничиваем количество снарядов
        List<Entity> projectiles = getProjectiles();
        int projectileCount = Math.min(projectiles.size(), MAX_PROJECTILES_TO_RENDER);

        for (int idx = 0; idx < projectileCount; idx++) {
            Entity entity = projectiles.get(idx);
            Vec3d motion = entity.getVelocity();
            Vec3d pos = entity.getPos();
            Vec3d prevPos = pos;

            // Для снарядов игрока добавляем плавное движение
            if (entity instanceof ProjectileEntity projectile && projectile.getOwner() == mc.player) {
                // Используем интерполированную позицию для расчета
                pos = interpolateProjectilePosition(projectile);
            }

            // Оптимизация: ограничиваем количество итераций
            int maxIterations = Math.min(300, MAX_TRAJECTORY_POINTS * 3);
            int step = Math.max(1, maxIterations / MAX_TRAJECTORY_POINTS);

            for (int i = 0; i < maxIterations; i++) {
                prevPos = pos;
                pos = pos.add(motion);
                motion = calculateMotion(entity, prevPos, motion);

                // Store point for rendering
                if (i % step == 0) {
                    BreakingBad(entity, pos, i);

                    // Store trajectory point for line rendering
                    if (i > 0 && trajectoryPoints.size() < MAX_TRAJECTORY_POINTS) {
                        trajectoryPoints.add(new TrajectoryPoint(prevPos, pos));
                    }
                }

                if (pos.y < -128) break;
            }
        }

        // Render trajectory lines
        Color themeColor = themeManager.getCurrentTheme().getBackgroundColor();
        Color trajectoryColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 200);
        int color = trajectoryColor.getRGB();
        for (TrajectoryPoint point : trajectoryPoints) {
            Render3D.drawLine(point.start, point.end, color, 2.0f);
        }

        // Рендерим с учетом глубины для работы под водой
        renderTrajectoriesWithDepth();

        // Обновляем время кадра
        lastFrameTime = System.currentTimeMillis();
    }

    public void drawPredictionInHand(MatrixStack matrix, Iterable<ItemStack> stacks) {
        if (mc.player == null) return;

        ItemStack activeItemStack = mc.player.getActiveItem();
        Item activeItem = activeItemStack != null && !activeItemStack.isEmpty() ? activeItemStack.getItem() : null;
        for (ItemStack stack : stacks) {
            List<HitResult> results = null;

            if (stack.getItem() instanceof ExperienceBottleItem) {
                results = checkTrajectory(new ExperienceBottleEntity(mc.world, mc.player, stack), 0.8);
            } else if (stack.getItem() instanceof SplashPotionItem) {
                results = checkTrajectory(new PotionEntity(mc.world, mc.player, stack), 0.55);
            } else if (stack.getItem() instanceof TridentItem) {
                results = checkTrajectory(new TridentEntity(mc.world, mc.player, stack), 2.5);
            } else if (stack.getItem() instanceof SnowballItem) {
                results = checkTrajectory(new SnowballEntity(mc.world, mc.player, stack), 1.5);
            } else if (stack.getItem() instanceof EggItem) {
                results = checkTrajectory(new EggEntity(mc.world, mc.player, stack), 1.5);
            } else if (stack.getItem() instanceof EnderPearlItem) {
                results = checkTrajectory(new EnderPearlEntity(mc.world, mc.player, stack), 1.5);
            } else if (stack.getItem() instanceof BowItem &&
                    activeItem != null && activeItem.equals(stack.getItem()) &&
                    mc.player.isUsingItem()) {
                float charge = 3 * MathHelper.clamp(
                        mc.player.getItemUseTime() / 20F, 0F, 1F
                );
                results = checkTrajectory(new ArrowEntity(mc.world, mc.player, stack, stack), charge);
            } else if (stack.getItem() instanceof CrossbowItem &&
                    CrossbowItem.isCharged(stack)) {
                ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
                List<HitResult> list = new ArrayList<>();
                if (component != null && !component.getProjectiles().isEmpty()) {
                    float velocity = component.getProjectiles().getFirst().isOf(Items.FIREWORK_ROCKET) ? 100 : 3;

                    // Get player's look vector
                    Vec3d lookVec = getSmoothLookVector();
                    list.add(checkTrajectory(lookVec, new ArrowEntity(mc.world, mc.player, stack, stack), velocity));

                    if (component.getProjectiles().size() > 2) {
                        Vec3d rotVec = getSmoothLookVector();
                        float pitchAbs = (float) (Math.asin(-rotVec.y) * 180 / Math.PI) / 90;
                        pitchAbs = Math.abs(pitchAbs);
                        float delta = pitchAbs * pitchAbs * pitchAbs * pitchAbs * pitchAbs;
                        float yawOffset = MathHelper.lerp(Math.abs(delta), 10, 90);
                        float pitchOffset = MathHelper.lerp(delta, 0, 10);

                        // Create rotated vectors
                        Vec3d leftVec = rotateVector(lookVec, -yawOffset, -pitchOffset);
                        Vec3d rightVec = rotateVector(lookVec, yawOffset * 2, 0);

                        list.add(checkTrajectory(leftVec, new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                        list.add(checkTrajectory(rightVec, new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                    }
                }
                results = list;
            }

            if (results != null) {
                results = results.stream().filter(Objects::nonNull).toList();
                if (!results.isEmpty()) {
                    renderProjectileResults(matrix, results, stack);
                }
            }
        }
    }

    // Helper method to rotate a vector
    private Vec3d rotateVector(Vec3d vec, float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);

        // Rotate around Y axis (yaw)
        double x1 = vec.x * Math.cos(yaw) - vec.z * Math.sin(yaw);
        double z1 = vec.x * Math.sin(yaw) + vec.z * Math.cos(yaw);

        // Rotate around X axis (pitch)
        double y1 = vec.y * Math.cos(pitch) - z1 * Math.sin(pitch);
        double z2 = vec.y * Math.sin(pitch) + z1 * Math.cos(pitch);

        return new Vec3d(x1, y1, z2).normalize();
    }

    private void renderProjectileResults(MatrixStack matrix, List<HitResult> results, ItemStack stack) {
        if (results.isEmpty()) return;

        // Используем интерполированную позицию глаз
        Vec3d startPos = getInterpolatedEyePosition();

        // Determine velocity and entity type based on item
        double velocity = 1.5;
        Class<? extends ProjectileEntity> entityClass = null;
        boolean isBow = false;
        boolean isCrossbow = false;

        if (stack.getItem() instanceof ExperienceBottleItem) {
            velocity = 0.8;
            entityClass = ExperienceBottleEntity.class;
        } else if (stack.getItem() instanceof SplashPotionItem) {
            velocity = 0.55;
            entityClass = PotionEntity.class;
        } else if (stack.getItem() instanceof TridentItem) {
            velocity = 2.5;
            entityClass = TridentEntity.class;
        } else if (stack.getItem() instanceof SnowballItem) {
            velocity = 1.5;
            entityClass = SnowballEntity.class;
        } else if (stack.getItem() instanceof EggItem) {
            velocity = 1.5;
            entityClass = EggEntity.class;
        } else if (stack.getItem() instanceof EnderPearlItem) {
            velocity = 1.5;
            entityClass = EnderPearlEntity.class;
        } else if (stack.getItem() instanceof BowItem) {
            float charge = 3 * MathHelper.clamp(
                    mc.player.getItemUseTime() / 20F, 0F, 1F
            );
            velocity = charge;
            entityClass = ArrowEntity.class;
            isBow = true;
        } else if (stack.getItem() instanceof CrossbowItem) {
            ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
            if (component != null && !component.getProjectiles().isEmpty()) {
                velocity = component.getProjectiles().getFirst().isOf(Items.FIREWORK_ROCKET) ? 100 : 3;
            }
            entityClass = ArrowEntity.class;
            isCrossbow = true;
        }

        if (entityClass == null) return;

        // Получаем плавное движение игрока для начальной скорости
        Vec3d motionInfluence = getMotionInfluence(isBow, isCrossbow);

        // Render trajectory for each result
        Color themeColor = themeManager.getCurrentTheme().getBackgroundColor();
        Color trajectoryColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 200);
        int color = trajectoryColor.getRGB();

        for (HitResult result : results) {
            if (result == null || result.getType() == HitResult.Type.MISS) continue;

            // Используем интерполированную позицию попадания
            Vec3d hitPos = getInterpolatedHitPosition(result.getPos());

            // Получаем вектор взгляда с интерполяцией
            Vec3d lookVec = getSmoothLookVector();
            float sqrt = MathHelper.sqrt((float) lookVec.lengthSquared());

            // Рассчитываем начальную траекторию
            Vec3d trajectory = lookVec.multiply(velocity / sqrt).add(motionInfluence);

            // Render trajectory line by tracing it
            Vec3d prevPos = startPos;
            Vec3d currentMotion = trajectory;
            Vec3d lastRenderedPos = startPos;
            int renderStep = 3;
            int maxIterations = Math.min(300, MAX_TRAJECTORY_POINTS * 3);

            for (int i = 0; i < maxIterations; i++) {
                Vec3d currentPos = prevPos.add(currentMotion);
                currentMotion = calculateMotionByClass(entityClass, prevPos, currentMotion);

                if (i % renderStep == 0 || i == 0) {
                    Render3D.drawLine(lastRenderedPos, currentPos, color, 2.0f);
                    lastRenderedPos = currentPos;
                }

                double distToHit = currentPos.squaredDistanceTo(hitPos);
                double prevDistToHit = prevPos.squaredDistanceTo(hitPos);
                if (distToHit < 0.5 || (prevDistToHit < distToHit && i > 5)) {
                    // Рисуем сглаженный маркер попадания
                    drawSmoothHitCircle(matrix, hitPos, result);
                    break;
                }

                prevPos = currentPos;

                if (currentPos.y < -128) break;
            }
        }
    }

    private Vec3d getMotionInfluence(boolean isBow, boolean isCrossbow) {
        Vec3d playerMotion = smoothPlayerMotion;

        if (isBow) {
            return playerMotion;
        } else if (isCrossbow) {
            return playerMotion.multiply(0.1);
        } else {
            return playerMotion.multiply(0.05);
        }
    }

    private List<Entity> getProjectiles() {
        List<Entity> projectiles = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if ((entity instanceof PersistentProjectileEntity ||
                    entity instanceof ThrownItemEntity ||
                    entity instanceof ItemEntity) &&
                    !visible(entity)) {
                projectiles.add(entity);
            }
        }
        return projectiles;
    }

    private List<HitResult> checkTrajectory(ProjectileEntity entity, double velocity) {
        Vec3d lookVec = getSmoothLookVector();
        HitResult result = checkTrajectory(lookVec, entity, velocity);
        return result != null ? List.of(result) : new ArrayList<>();
    }

    private HitResult checkTrajectory(Vec3d lookVec, ProjectileEntity entity, double velocity) {
        float sqrt = MathHelper.sqrt((float) lookVec.lengthSquared());

        // Влияние движения игрока
        Vec3d motionInfluence = Vec3d.ZERO;
        if (entity instanceof ArrowEntity) {
            motionInfluence = smoothPlayerMotion;
        } else if (entity instanceof ExperienceBottleEntity ||
                entity instanceof PotionEntity ||
                entity instanceof EnderPearlEntity ||
                entity instanceof SnowballEntity ||
                entity instanceof EggEntity ||
                entity instanceof TridentEntity) {
            motionInfluence = smoothPlayerMotion.multiply(0.05);
        }

        Vec3d startPos = getInterpolatedEyePosition();
        Vec3d trajectory = lookVec.multiply(velocity / sqrt).add(motionInfluence);

        return traceTrajectory(startPos, trajectory, entity);
    }

    private HitResult traceTrajectory(Vec3d pos, Vec3d motion, ProjectileEntity entity) {
        Vec3d prevPos;
        for (int i = 0; i < 300; i++) {
            prevPos = pos;
            pos = pos.add(motion);
            motion = calculateMotion(entity, prevPos, motion);

            HitResult result = mc.world.raycast(
                    new RaycastContext(
                            prevPos,
                            pos,
                            RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE,
                            mc.player
                    )
            );

            if (!result.getType().equals(HitResult.Type.MISS)) {
                return result;
            }

            for (Entity ent : mc.world.getEntities()) {
                if (ent instanceof LivingEntity living &&
                        living != entity.getOwner() &&
                        living.isAlive() &&
                        living.getBoundingBox().expand(0.3).intersects(prevPos, pos)) {
                    return new EntityHitResult(living);
                }
            }

            if (pos.y < -128) break;
        }
        return null;
    }

    private Vec3d calculateMotion(Entity entity, Vec3d prevPos, Vec3d motion) {
        boolean isInWater = mc.world.getFluidState(BlockPos.ofFloored(prevPos)).isIn(FluidTags.WATER);
        double multiply;

        if (entity instanceof TridentEntity) {
            multiply = 0.99;
        } else if (entity instanceof PersistentProjectileEntity && isInWater) {
            multiply = 0.6;
        } else {
            multiply = isInWater ? 0.8 : 0.99;
        }

        double gravity = 0.05;
        if (entity instanceof ArrowEntity) gravity = 0.05;
        if (entity instanceof TridentEntity) gravity = 0.04;
        if (entity instanceof ExperienceBottleEntity) gravity = 0.07;
        if (entity instanceof PotionEntity) gravity = 0.05;

        return motion.multiply(multiply).add(0, -gravity, 0);
    }

    private Vec3d calculateMotionByClass(Class<? extends ProjectileEntity> entityClass, Vec3d prevPos, Vec3d motion) {
        boolean isInWater = mc.world.getFluidState(BlockPos.ofFloored(prevPos)).isIn(FluidTags.WATER);
        double multiply;
        double gravity = 0.05;

        if (entityClass == TridentEntity.class) {
            multiply = 0.99;
            gravity = 0.04;
        } else if (PersistentProjectileEntity.class.isAssignableFrom(entityClass) && isInWater) {
            multiply = 0.6;
        } else {
            multiply = isInWater ? 0.8 : 0.99;
        }

        if (entityClass == ArrowEntity.class) {
            gravity = 0.05;
        } else if (entityClass == ExperienceBottleEntity.class) {
            gravity = 0.07;
        } else if (entityClass == PotionEntity.class) {
            gravity = 0.05;
        }

        return motion.multiply(multiply).add(0, -gravity, 0);
    }

    private void BreakingBad(Entity entity, Vec3d pos, int ticks) {
        if (entity instanceof ItemEntity item) {
            points.add(new Point(item.getStack(), pos, ticks));
        } else if (entity instanceof ThrownItemEntity thrown) {
            points.add(new Point(thrown.getStack(), pos, ticks));
        } else if (entity instanceof PersistentProjectileEntity persistent) {
            points.add(new Point(persistent.getItemStack(), pos, ticks));
        }
    }

    private Direction getDirection(HitResult result) {
        if (result instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getSide();
        }
        return Direction.getFacing(result.getPos().subtract(mc.player.getEyePos()).normalize());
    }

    private boolean visible(Entity entity) {
        boolean posChange = entity.getX() == entity.prevX &&
                entity.getY() == entity.prevY &&
                entity.getZ() == entity.prevZ;

        boolean itemEntityCheck = false;
        if (entity instanceof ItemEntity) {
            itemEntityCheck = entity.isOnGround() ||
                    mc.world.getBlockState(BlockPos.ofFloored(entity.getPos())).getBlock() == Blocks.WATER;
        }

        return posChange || itemEntityCheck;
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        // Handle theme change
    }

    /**
     * Обновляет интерполяционные значения
     */
    private void updateInterpolation() {
        if (mc.player == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = lastFrameTime > 0 ? (currentTime - lastFrameTime) / 1000.0f : 0.016f;
        deltaTime = Math.min(deltaTime, 0.1f);

        Vec3d currentPos = mc.player.getPos();
        Vec3d currentVelocity = mc.player.getVelocity();

        if (previousFramePlayerPos == null) {
            previousFramePlayerPos = currentPos;
            smoothPlayerPos = currentPos;
            smoothPlayerMotion = currentVelocity;
            interpolationAlpha = 0f;
            return;
        }

        // Обновляем сглаженное движение игрока
        float velocityBlend = MathHelper.clamp(deltaTime * 12f, 0f, 1f);
        smoothPlayerMotion = smoothPlayerMotion.add(currentVelocity.subtract(smoothPlayerMotion).multiply(velocityBlend));

        // Плавное обновление позиции
        float smoothFactor = MathHelper.clamp(deltaTime * 12f, 0f, 1f);
        smoothPlayerPos = lerp(smoothPlayerPos, currentPos, smoothFactor);

        previousFramePlayerPos = currentPos;

        // Обновляем альфу для интерполяции
        interpolationAlpha += deltaTime * 60;
        if (interpolationAlpha > 1.0f) {
            interpolationAlpha = 1.0f;
        }
    }

    /**
     * Получает интерполированную позицию глаз
     */
    private Vec3d getInterpolatedEyePosition() {
        if (smoothPlayerPos == null) {
            return mc.player != null ? mc.player.getEyePos() : Vec3d.ZERO;
        }

        float eyeHeight = mc.player.getEyeHeight(mc.player.getPose());
        return smoothPlayerPos.add(0, eyeHeight, 0);
    }

    /**
     * Получает интерполированную позицию попадания
     */
    private Vec3d getInterpolatedHitPosition(Vec3d newHitPos) {
        if (lastHitPos == null) {
            lastHitPos = newHitPos;
            return newHitPos;
        }

        // Плавная интерполяция позиции попадания
        hitPosLerpFactor += 0.1f;
        hitPosLerpFactor = Math.min(hitPosLerpFactor, 1.0f);

        Vec3d result = lerp(lastHitPos, newHitPos, hitPosLerpFactor);

        // Если позиция стабилизировалась, сбрасываем фактор
        if (result.squaredDistanceTo(newHitPos) < 0.001) {
            lastHitPos = newHitPos;
            hitPosLerpFactor = 0f;
        }

        return result;
    }

    /**
     * Интерполирует позицию снаряда игрока
     */
    private Vec3d interpolateProjectilePosition(ProjectileEntity projectile) {
        if (projectile == null || projectile.getOwner() != mc.player) {
            return projectile != null ? projectile.getPos() : Vec3d.ZERO;
        }

        // Берем реальную позицию снаряда и корректируем относительно плавного движения игрока
        Vec3d projectilePos = projectile.getPos();

        if (smoothPlayerPos != null && previousFramePlayerPos != null) {
            // Вычисляем смещение игрока
            Vec3d playerOffset = smoothPlayerPos.subtract(previousFramePlayerPos);
            // Добавляем часть смещения к позиции снаряда
            return projectilePos.add(playerOffset.multiply(0.7));
        }

        return projectilePos;
    }

    /**
     * Получает плавный вектор взгляда
     */
    private Vec3d getSmoothLookVector() {
        return mc.player.getRotationVec(Render3D.getTickDelta());
    }

    /**
     * Линейная интерполяция между двумя векторами
     */
    private Vec3d lerp(Vec3d start, Vec3d end, float factor) {
        factor = MathHelper.clamp(factor, 0f, 1f);
        double x = start.x + (end.x - start.x) * factor;
        double y = start.y + (end.y - start.y) * factor;
        double z = start.z + (end.z - start.z) * factor;
        return new Vec3d(x, y, z);
    }

    /**
     * Рисует сглаженный круг на месте попадания
     */
    private void drawSmoothHitCircle(MatrixStack matrix, Vec3d hitPos, HitResult result) {
        Direction face = Direction.UP;
        if (result instanceof BlockHitResult blockHit) {
            face = blockHit.getSide();
        }

        double radius = 0.3;
        int segments = 40;

        Color themeColor = themeManager.getCurrentTheme().getBackgroundColor();
        Color circleColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 200);
        int color = circleColor.getRGB();

        Vec3d normal = switch (face) {
            case UP -> new Vec3d(0, 1, 0);
            case DOWN -> new Vec3d(0, -1, 0);
            case NORTH -> new Vec3d(0, 0, -1);
            case SOUTH -> new Vec3d(0, 0, 1);
            case WEST -> new Vec3d(-1, 0, 0);
            case EAST -> new Vec3d(1, 0, 0);
        };

        Vec3d right = normal.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 0.01) {
            right = normal.crossProduct(new Vec3d(1, 0, 0));
        }
        right = right.normalize();
        Vec3d up = right.crossProduct(normal).normalize();

        double offset = 0.01;
        Vec3d circleCenter = hitPos.add(normal.multiply(offset));

        Vec3d prevPoint = null;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;

            Vec3d point = circleCenter.add(right.multiply(dx)).add(up.multiply(dz));

            if (prevPoint != null) {
                Render3D.drawLine(prevPoint, point, color, 2.0f);
            }
            prevPoint = point;
        }
    }

    /**
     * Рендерит траектории с учетом глубины для работы под водой
     */
    private void renderTrajectoriesWithDepth() {
        if (Render3D.DEBUG_LINES.isEmpty() && Render3D.QUADS.isEmpty()) return;

        List<Render3D.VertexCollection> linesToRender = new ArrayList<>(Render3D.DEBUG_LINES);
        List<Render3D.VertexCollection> quadsToRender = new ArrayList<>(Render3D.QUADS);

        Render3D.DEBUG_LINES.clear();
        Render3D.QUADS.clear();

        if (!quadsToRender.isEmpty()) {
            Render3D.QUADS.addAll(quadsToRender);
            Render3D.render();
            Render3D.QUADS.clear();
        }

        if (!linesToRender.isEmpty()) {
            renderLinesUnderwater(linesToRender, 2.0f);
        }
    }

    /**
     * Рендерит линии с правильной настройкой для работы под водой
     */
    private void renderLinesUnderwater(List<Render3D.VertexCollection> lines, float width) {
        if (lines.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (Render3D.VertexCollection collection : lines) {
            collection.vertex(buffer);
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private record Point(ItemStack stack, Vec3d pos, int ticks) {}
    private record TrajectoryPoint(Vec3d start, Vec3d end) {}
}