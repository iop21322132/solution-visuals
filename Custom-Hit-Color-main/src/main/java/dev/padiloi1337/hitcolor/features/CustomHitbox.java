package dev.padiloi1337.hitcolor.features;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.padiloi1337.hitcolor.HitColor;
import dev.padiloi1337.hitcolor.helpers.render.DrawHelper;
import dev.padiloi1337.hitcolor.settings.impl.BooleanSetting;
import dev.padiloi1337.hitcolor.settings.impl.ColorSetting;
import dev.padiloi1337.hitcolor.settings.impl.ListSetting;
import dev.padiloi1337.hitcolor.settings.impl.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CustomHitbox {
    
    private static final Minecraft MC = Minecraft.getInstance();
    
    // Настройки
    private final BooleanSetting hideEyeLine;
    private final NumberSetting size;
    private final ListSetting entityTypes;
    private final ColorSetting color;
    
    // Подкатегории для entityTypes
    private final BooleanSetting showPlayers;
    private final BooleanSetting showMobs;
    private final BooleanSetting showAnimals;
    private final BooleanSetting showItems;
    
    public CustomHitbox() {
        // Создаем подкатегории
        showPlayers = new BooleanSetting("Игроки", true);
        showMobs = new BooleanSetting("Мобы", true);
        showAnimals = new BooleanSetting("Животные", false);
        showItems = new BooleanSetting("Предметы", false);
        
        // Основные настройки
        hideEyeLine = new BooleanSetting("Убрать линию взгляда", false);
        size = new NumberSetting("Размер", 1.0f, 0.1f, 3.0f, 0.1f);
        entityTypes = new ListSetting("Что показывать", showPlayers, showMobs, showAnimals, showItems);
        color = new ColorSetting("Цвет", Color.RED);
        
        System.out.println("CustomHitbox: Constructor called");
    }
    
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        System.out.println("CustomHitbox: RenderWorldLastEvent called");
        
        // Проверяем основную настройку из HitColor.Settings
        HitColor hitColor = HitColor.getInstance();
        if (hitColor == null) {
            System.out.println("CustomHitbox: HitColor instance is null");
            return;
        }
        
        if (!hitColor.settings.customHitboxEnabled.getValue()) {
            System.out.println("CustomHitbox: Custom hitbox is disabled");
            return;
        }
        
        if (MC.world == null || MC.player == null) {
            System.out.println("CustomHitbox: World or player is null");
            return;
        }
        
        System.out.println("CustomHitbox: Rendering enabled, entities count: " + MC.world.getAllEntities().size());
        
        MatrixStack matrices = event.getMatrixStack();
        float partialTicks = event.getPartialTicks();
        
        // Получаем позицию камеры
        ActiveRenderInfo renderInfo = MC.gameRenderer.getActiveRenderInfo();
        Vector3d cameraPos = renderInfo.getProjectedView();
        
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        // Настройка рендеринга
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.defaultBlendFunc();
        
        Color hitboxColor = color.getValue();
        float sizeMultiplier = size.getValue();
        
        // Рендерим хитбоксы для всех подходящих сущностей
        int renderedCount = 0;
        for (Entity entity : MC.world.getAllEntities()) {
            if (entity == MC.player) continue; // Не показываем хитбокс игрока
            
            if (!shouldRenderEntity(entity)) continue;
            
            renderedCount++;
            System.out.println("CustomHitbox: Rendering entity: " + entity.getClass().getSimpleName());
            
            // Интерполируем позицию сущности
            double x = entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * partialTicks;
            double y = entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * partialTicks;
            double z = entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * partialTicks;
            
            // Получаем хитбокс и масштабируем его
            AxisAlignedBB boundingBox = entity.getBoundingBox();
            double width = (boundingBox.maxX - boundingBox.minX) * sizeMultiplier;
            double height = (boundingBox.maxY - boundingBox.minY) * sizeMultiplier;
            double depth = (boundingBox.maxZ - boundingBox.minZ) * sizeMultiplier;
            
            // Центрируем масштабированный хитбокс
            double halfWidth = width / 2;
            double halfDepth = depth / 2;
            
            AxisAlignedBB scaledBox = new AxisAlignedBB(
                x - halfWidth, y, z - halfDepth,
                x + halfWidth, y + height, z + halfDepth
            );
            
            // Рендерим хитбокс
            renderHitbox(matrices, scaledBox, hitboxColor);
            
            // Рендерим линию взгляда, если не отключена
            if (!hideEyeLine.getValue() && entity instanceof LivingEntity) {
                renderEyeLine(matrices, (LivingEntity) entity, x, y, z, partialTicks, hitboxColor);
            }
        }
        
        System.out.println("CustomHitbox: Rendered " + renderedCount + " entities");
        
        // Восстанавливаем состояние рендеринга
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        
        matrices.pop();
    }
    
    private boolean shouldRenderEntity(Entity entity) {
        boolean result = false;
        if (entity instanceof PlayerEntity && showPlayers.getValue()) {
            result = true;
        } else if (entity instanceof MonsterEntity && showMobs.getValue()) {
            result = true;
        } else if (entity instanceof AnimalEntity && showAnimals.getValue()) {
            result = true;
        } else if (entity instanceof ItemEntity && showItems.getValue()) {
            result = true;
        }
        
        if (result) {
            System.out.println("CustomHitbox: Should render " + entity.getClass().getSimpleName() + 
                " - Players:" + showPlayers.getValue() + 
                " Mobs:" + showMobs.getValue() + 
                " Animals:" + showAnimals.getValue() + 
                " Items:" + showItems.getValue());
        }
        
        return result;
    }
    
    private void renderHitbox(MatrixStack matrices, AxisAlignedBB box, Color color) {
        // Рендерим контур хитбокса
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;
        
        // Используем DrawHelper для рендеринга линий хитбокса
        // Это упрощенная версия - в реальности нужно будет использовать OpenGL напрямую
        // для рендеринга 3D линий
        
        // Нижние линии
        DrawHelper.drawLine(matrices, 
            (float)box.minX, (float)box.minY, (float)box.minZ,
            (float)box.maxX, (float)box.minY, (float)box.minZ, color);
        DrawHelper.drawLine(matrices,
            (float)box.maxX, (float)box.minY, (float)box.minZ,
            (float)box.maxX, (float)box.minY, (float)box.maxZ, color);
        DrawHelper.drawLine(matrices,
            (float)box.maxX, (float)box.minY, (float)box.maxZ,
            (float)box.minX, (float)box.minY, (float)box.maxZ, color);
        DrawHelper.drawLine(matrices,
            (float)box.minX, (float)box.minY, (float)box.maxZ,
            (float)box.minX, (float)box.minY, (float)box.minZ, color);
        
        // Верхние линии
        DrawHelper.drawLine(matrices,
            (float)box.minX, (float)box.maxY, (float)box.minZ,
            (float)box.maxX, (float)box.maxY, (float)box.minZ, color);
        DrawHelper.drawLine(matrices,
            (float)box.maxX, (float)box.maxY, (float)box.minZ,
            (float)box.maxX, (float)box.maxY, (float)box.maxZ, color);
        DrawHelper.drawLine(matrices,
            (float)box.maxX, (float)box.maxY, (float)box.maxZ,
            (float)box.minX, (float)box.maxY, (float)box.maxZ, color);
        DrawHelper.drawLine(matrices,
            (float)box.minX, (float)box.maxY, (float)box.maxZ,
            (float)box.minX, (float)box.maxY, (float)box.minZ, color);
        
        // Вертикальные линии
        DrawHelper.drawLine(matrices,
            (float)box.minX, (float)box.minY, (float)box.minZ,
            (float)box.minX, (float)box.maxY, (float)box.minZ, color);
        DrawHelper.drawLine(matrices,
            (float)box.maxX, (float)box.minY, (float)box.minZ,
            (float)box.maxX, (float)box.maxY, (float)box.minZ, color);
        DrawHelper.drawLine(matrices,
            (float)box.maxX, (float)box.minY, (float)box.maxZ,
            (float)box.maxX, (float)box.maxY, (float)box.maxZ, color);
        DrawHelper.drawLine(matrices,
            (float)box.minX, (float)box.minY, (float)box.maxZ,
            (float)box.minX, (float)box.maxY, (float)box.maxZ, color);
    }
    
    private void renderEyeLine(MatrixStack matrices, LivingEntity entity, double x, double y, double z, 
                              float partialTicks, Color color) {
        // Получаем направление взгляда
        Vector3d lookVec = entity.getLook(partialTicks);
        double eyeHeight = entity.getEyeHeight();
        
        // Начальная точка (глаза сущности)
        float startX = (float)x;
        float startY = (float)(y + eyeHeight);
        float startZ = (float)z;
        
        // Конечная точка (направление взгляда на 5 блоков)
        float endX = (float)(x + lookVec.x * 5);
        float endY = (float)(y + eyeHeight + lookVec.y * 5);
        float endZ = (float)(z + lookVec.z * 5);
        
        // Рендерим линию взгляда
        DrawHelper.drawLine(matrices, startX, startY, startZ, endX, endY, endZ, color);
    }
    
    // Геттеры для настроек
    public BooleanSetting getHideEyeLine() { return hideEyeLine; }
    public NumberSetting getSize() { return size; }
    public ListSetting getEntityTypes() { return entityTypes; }
    public ColorSetting getColor() { return color; }
}