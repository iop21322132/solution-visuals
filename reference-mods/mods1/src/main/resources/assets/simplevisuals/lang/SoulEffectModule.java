package farvix.solution.client.modules.impl.visuals;

import farvix.solution.api.event.EventHandler;
import farvix.solution.api.module.Module;
import farvix.solution.api.module.ModuleCategory;
import farvix.solution.api.module.ModuleInfo;
import farvix.solution.api.setting.impl.NumberSetting;
import farvix.solution.client.events.EventEntityDeath;
import farvix.solution.client.events.EventRender3D;
import farvix.solution.client.events.EventUpdate;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Модуль SoulEffect для Solution Visual.
 * Создает визуальный эффект "души" при убийстве сущности.
 */
@ModuleInfo(name = "SoulEffect", description = "Создает эффект взлетающей души при убийстве", category = ModuleCategory.VISUALS)
public class SoulEffectModule extends Module {

    // Вспомогательный класс для хранения данных души
    // Изменяем record на class, чтобы добавить prevPos
    private static class SoulEntity {
        public final LivingEntity entity;
        public Vec3d pos;
        public Vec3d prevPos; // Добавляем предыдущую позицию для интерполяции
        public float rotation;
        public float prevRotation;
        public final long spawnTime;

        public SoulEntity(LivingEntity entity, Vec3d pos, long spawnTime) {
            this.entity = entity;
            this.pos = pos;
            this.prevPos = pos; // Инициализируем prevPos
            this.rotation = 0;
            this.prevRotation = 0;
            this.spawnTime = spawnTime;
        }
    }

    // Настройка скорости: от 0.5 до 5.0 с шагом 0.5
    private final NumberSetting speed = new NumberSetting("Скорость", 1.0f, 0.5f, 5.0f, 0.5f);
    private final NumberSetting rotationSpeed = new NumberSetting("Вращение", 2.0f, 0.0f, 10.0f, 0.5f);
    
    // Список активных душ (используем потокобезопасный список)
    private final CopyOnWriteArrayList<SoulEntity> souls = new CopyOnWriteArrayList<>();

    public SoulEffectModule() {
        addSettings(speed, rotationSpeed);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        // Удаляем души через 2.5 секунды для динамичности
        souls.removeIf(soul -> System.currentTimeMillis() - soul.spawnTime > 2500);

        // Обновляем позиции душ
        // Рассчитываем подъем (скорость блоков в секунду / 20 тиков)
        double liftPerTick = speed.getValue().doubleValue() / 20.0;
        
        for (SoulEntity soul : souls) {
            soul.prevPos = soul.pos; // Сохраняем текущую позицию как предыдущую
            soul.pos = soul.pos.add(0, liftPerTick, 0);
            soul.prevRotation = soul.rotation;
            soul.rotation += rotationSpeed.getValue().floatValue() * 3.0f;
        }
    }

    @EventHandler
    public void onDeath(EventEntityDeath event) {
        // Проверяем, что сущность является живым существом (игрок или моб)
        if (event.getEntity() instanceof LivingEntity living) {
            // Добавляем новую душу в список на текущей позиции существа
            souls.add(new SoulEntity(living, living.getPos(), System.currentTimeMillis()));
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (souls.isEmpty()) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        for (SoulEntity soul : souls) {
            long lifetime = System.currentTimeMillis() - soul.spawnTime;
            float progress = (float) lifetime / 2500f; // Прогресс жизни от 0 до 1
            if (progress > 1f) continue;

            // Плавное затухание и небольшое увеличение масштаба
            float alpha = 0.5f * (1.0f - progress);

            event.getMatrices().push();

            // Интерполируем позицию для плавного движения между тиками
            Vec3d interpolatedPos = MathHelper.lerp(event.getTickDelta(), soul.prevPos, soul.pos);
            event.getMatrices().translate(
                interpolatedPos.x - cameraPos.x,
                interpolatedPos.y - cameraPos.y,
                interpolatedPos.z - cameraPos.z
            );
            
            // Плавное вращение
            float renderRotation = MathHelper.lerp(event.getTickDelta(), soul.prevRotation, soul.rotation);
            event.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(renderRotation));
            
            // Настройка прозрачности (50% = 0.5f)
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            
            // Очистка состояния для души
            int origHurt = soul.entity.hurtTime;
            int origDeath = soul.entity.deathTime; // Сохраняем время смерти
            int origFire = soul.entity.getFireTicks();
            boolean origGlowing = soul.entity.isGlowing(); // Сохраняем состояние свечения
            net.minecraft.util.math.Box origBB = soul.entity.getBoundingBox(); // Сохраняем хитбокс
            net.minecraft.scoreboard.AbstractTeam team = soul.entity.getScoreboardTeam();
            net.minecraft.scoreboard.AbstractTeam.VisibilityRule origVisibility = team != null ? team.getNameTagVisibilityRule() : null;
            boolean wasOnFire = soul.entity.isOnFire();
            boolean wasSneaking = soul.entity.isSneaking();

            ItemStack head = soul.entity.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chest = soul.entity.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legs = soul.entity.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feet = soul.entity.getEquippedStack(EquipmentSlot.FEET);
            ItemStack main = soul.entity.getEquippedStack(EquipmentSlot.MAINHAND);
            ItemStack off = soul.entity.getEquippedStack(EquipmentSlot.OFFHAND);

            soul.entity.hurtTime = 0;
            soul.entity.deathTime = 0; // Обнуляем время смерти для удаления красного оттенка
            soul.entity.setFireTicks(0);
            soul.entity.setGlowing(false); // Убираем эффект свечения

            soul.entity.setFlag(0, false); // Огонь
            soul.entity.setFlag(1, true); // Принудительно включаем присед для скрытия ника

            // Скрываем имя команды (префиксы/суффиксы)
            if (team instanceof net.minecraft.scoreboard.Team scoreboardTeam) {
                scoreboardTeam.setNameTagVisibilityRule(net.minecraft.scoreboard.AbstractTeam.VisibilityRule.NEVER);
            }
            
            // Убираем имя полностью
            soul.entity.setCustomName(net.minecraft.text.Text.empty());
            soul.entity.setCustomNameVisible(false);

            // Скрываем ник, уводя хитбокс вниз
            soul.entity.setBoundingBox(new net.minecraft.util.math.Box(0, -2000, 0, 0, -2000, 0));

            soul.entity.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
            soul.entity.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
            soul.entity.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
            soul.entity.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
            soul.entity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            soul.entity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

            VertexConsumerProvider.Immediate buffer = mc.getBufferBuilders().getEntityVertexConsumers();
            
            // Рендерим модель существа в текущем состоянии
            float yaw = MathHelper.lerp(event.getTickDelta(), soul.entity.prevYaw, soul.entity.getYaw());
            dispatcher.render(
                soul.entity, 
                0, 0, 0, 
                yaw, 
                event.getMatrices(), 
                buffer, 
                15728880 // Полное освещение
            );
            
            buffer.draw();

            // Восстановление
            soul.entity.hurtTime = origHurt;
            soul.entity.deathTime = origDeath; // Восстанавливаем время смерти
            soul.entity.setFireTicks(origFire);
            soul.entity.setGlowing(origGlowing); // Восстанавливаем состояние свечения

            soul.entity.setBoundingBox(origBB); // Восстановление хитбокса

            // Восстановление видимости команды
            if (team instanceof net.minecraft.scoreboard.Team scoreboardTeam && origVisibility != null) {
                scoreboardTeam.setNameTagVisibilityRule(origVisibility);
            }

            soul.entity.setFlag(0, wasOnFire);
            soul.entity.setFlag(1, wasSneaking);

            soul.entity.equipStack(EquipmentSlot.HEAD, head);
            soul.entity.equipStack(EquipmentSlot.CHEST, chest);
            soul.entity.equipStack(EquipmentSlot.LEGS, legs);
            soul.entity.equipStack(EquipmentSlot.FEET, feet);
            soul.entity.equipStack(EquipmentSlot.MAINHAND, main);
            soul.entity.equipStack(EquipmentSlot.OFFHAND, off);
            
            // Сбрасываем настройки рендера
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            
            event.getMatrices().pop();
        }
    }
}