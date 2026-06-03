package farvix.solution.client.modules.impl.environment;

import farvix.solution.api.events.impl.entity.EventAttackEntity;
import farvix.solution.api.events.impl.render.EventRender2D;
import farvix.solution.api.interfaces.QuickImports;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.util.render.ProjectionUtility;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ModuleInfo(
    name = "CartHelper",
    category = ModuleCategory.ENVIRONMENT,
    description = "Показывает YES/NO над игроком если его можно убить вагонеткой"
)
public class CartHelper extends Module implements QuickImports {

    // Храним время последнего удара по игроку
    private final Map<UUID, Long> hitPlayers = new HashMap<>();

    @EventHandler
    public void onAttack(EventAttackEntity e) {
        if (e.getTarget() instanceof PlayerEntity player) {
            hitPlayers.put(player.getUuid(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (!ClickUI.isToolrise()) {
            this.setEnabled(false);
            return;
        }
        if (mc.world == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        // Очищаем старые удары (> 10 сек)
        hitPlayers.entrySet().removeIf(entry -> now - entry.getValue() > 10000);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;

            if (player.isInvisible()) {
                boolean hasArmor = false;
                for (ItemStack stack : player.getArmorItems()) {
                    if (!stack.isEmpty()) {
                        hasArmor = true;
                        break;
                    }
                }
                if (!hasArmor) continue;
            }

            // 1. Проверка: били ли мы его в последние 10 секунд
            if (!hitPlayers.containsKey(player.getUuid())) continue;

            // 2. Проверка стен (не показывать через стены)
            if (!mc.player.canSee(player)) continue;

            // 3. Позиция над головой (поднята на 1.0 блока выше макушки, чтобы быть над ником)
            double tickDelta = mc.getRenderTickCounter().getTickDelta(false);
            Vec3d worldPos = new Vec3d(
                MathHelper.lerp(tickDelta, player.prevX, player.getX()),
                MathHelper.lerp(tickDelta, player.prevY, player.getY()) + player.getHeight() + 1.0,
                MathHelper.lerp(tickDelta, player.prevZ, player.getZ())
            );

            // 4. Проверка: перед камерой ли игрок
            net.minecraft.client.render.Camera cam = mc.gameRenderer.getCamera();
            Vec3d toPlayer = worldPos.subtract(cam.getPos()).normalize();
            Vec3d look = mc.player.getRotationVec(1.0f);
            if (toPlayer.dotProduct(look) <= 0) continue;

            // 5. Проекция на экран
            Vec3d screenPos = ProjectionUtility.worldSpaceToScreenSpace(worldPos);
            if (screenPos.z < 0 || screenPos.z > 1) continue;

            // 6. Расчет урона, дистанции и отрисовка
            boolean canOneTap = checkOneTap(player);
            
            // Вычисляем дистанцию для перспективы
            float dist = mc.player.distanceTo(player);
            
            // Мягкая перспектива: надпись остается крупной на дистанции удара, 
            // и начинает плавно и медленно уменьшаться только при реальном отдалении.
            float scale = (float) (10.0f / (9.0f + Math.max(1.0f, dist)));
            
            renderLabel(e.getContext(), (float) screenPos.x, (float) screenPos.y, canOneTap, scale);
        }
    }

    private boolean checkOneTap(PlayerEntity player) {
        // Рассчитываем урон так, будто вагонетка взорвалась в 1 блоке от игрока.
        // Мы снижаем базовый урон до 60.0f, чтобы "YES" загоралось только при гарантированном ваншоте.
        float baseDmg = 60.0f; 

        float armor = (float) player.getAttributeValue(EntityAttributes.ARMOR);
        float toughness = (float) player.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);

        // 1. Формула уменьшения урона броней (стандарт Minecraft)
        float armorReduction = Math.min(20.0f, Math.max(armor / 5.0f, armor - baseDmg / (2.0f + toughness / 4.0f)));
        float dmgAfterArmor = baseDmg * (1.0f - armorReduction / 25.0f);

        // 2. Считываем реальные зачарования брони (Protection / Blast Protection)
        var registry = player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var protection = registry.getOptional(Enchantments.PROTECTION).orElse(null);
        var blastProtection = registry.getOptional(Enchantments.BLAST_PROTECTION).orElse(null);

        int epf = 0;
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.isEmpty()) continue;
            if (protection != null) epf += EnchantmentHelper.getLevel(protection, stack);
            if (blastProtection != null) epf += EnchantmentHelper.getLevel(blastProtection, stack) * 2;
        }
        
        float protReduction = Math.min(20.0f, (float) epf) / 25.0f;
        float finalDmg = dmgAfterArmor * (1.0f - protReduction);

        // Эффект Сопротивления (Resistance)
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int amplifier = player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1;
            finalDmg *= Math.max(0, 1.0f - (amplifier * 0.2f));
        }

        return finalDmg >= (player.getHealth() + player.getAbsorptionAmount());
    }

    private void renderLabel(DrawContext context, float x, float y, boolean canOneTap, float scale) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        
        String text = canOneTap ? "YES" : "NO";
        int color = canOneTap ? new Color(0, 255, 0).getRGB() : new Color(255, 0, 0).getRGB();
        
        // Увеличенный размер шрифта для "большой" надписи
        int fontSize = 32; 
        
        // Рисуем текст по центру проекции
        Fonts.SEMIBOLD.get(fontSize).drawCenteredString(context.getMatrices(), text, 0, 0, color);
        
        context.getMatrices().pop();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        hitPlayers.clear();
    }
}
