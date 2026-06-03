package farvix.solution.client.modules.impl.environment;

import com.mojang.authlib.GameProfile;
import farvix.solution.api.events.impl.game.EventUpdate;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import java.util.UUID;

@ModuleInfo(name = "FakePlayer", category = ModuleCategory.ENVIRONMENT, description = "Создает локального неуязвимого неподвижного бота")
public class FakePlayer extends Module {

    private boolean spawned = false;
    private ClientFakePlayer fakePlayer;

    @EventHandler
    private void onUpdate(EventUpdate e) {
        if (!spawned) {
            spawnFakePlayer();
            spawned = true;
        }
    }

    private void spawnFakePlayer() {
        if (mc.world == null || mc.player == null) return;
        UUID uuid = UUID.nameUUIDFromBytes("1337_fake_player".getBytes());
        fakePlayer = new ClientFakePlayer(mc.world, new GameProfile(uuid, "FakePlayer"));
        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setHeadYaw(mc.player.headYaw);
        fakePlayer.setBodyYaw(mc.player.bodyYaw);
        
        // Копируем инвентарь (включая всю броню и предметы в руках)
        fakePlayer.getInventory().clone(mc.player.getInventory());
        
        fakePlayer.setHealth(20.0f);
        fakePlayer.setId(-1337);
        mc.world.addEntity(fakePlayer);
    }

    @Override
    public void onDisable() {
        removeFakePlayer();
        spawned = false;
        super.onDisable();
    }

    private void removeFakePlayer() {
        if (fakePlayer != null) {
            fakePlayer.discard();
            if (mc.world != null) {
                mc.world.removeEntity(fakePlayer.getId(), net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            }
            fakePlayer = null;
        }
        if (mc.world != null) {
            mc.world.removeEntity(-1337, net.minecraft.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    public static class ClientFakePlayer extends OtherClientPlayerEntity {
        public ClientFakePlayer(ClientWorld world, GameProfile profile) {
            super(world, profile);
        }

        @Override
        public boolean collidesWith(net.minecraft.entity.Entity other) {
            return false;
        }

        @Override
        public boolean isPushable() {
            return false;
        }

        @Override
        public void takeKnockback(double strength, double x, double z) {
            // Блокируем отбрасывание полностью, чтобы бот не сдвигался с места
        }

        @Override
        public boolean damage(ServerWorld world, DamageSource source, float amount) {
            return damageLocal(source);
        }

        // Перегрузка для совместимости с различными версиями маппингов (с/без ServerWorld)
        public boolean damage(DamageSource source, float amount) {
            return damageLocal(source);
        }

        private boolean damageLocal(DamageSource source) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                // Воспроизводим стандартный звук ранения игрока
                client.world.playSound(this.getX(), this.getY(), this.getZ(),
                        net.minecraft.sound.SoundEvents.ENTITY_PLAYER_HURT,
                        net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f, false);
                
                // Визуальный красный флеш урона
                this.handleStatus((byte) 2);
                
                // Устанавливаем параметры анимации получения урона (для шейдеров и оверлеев)
                this.hurtTime = 10;
                this.maxHurtTime = 10;
                
                // Наклон тела при получении урона в зависимости от положения атакующего
                net.minecraft.entity.Entity attacker = source.getAttacker();
                if (attacker != null) {
                    double d = attacker.getX() - this.getX();
                    double e = attacker.getZ() - this.getZ();
                    this.damageTiltYaw = (float)(net.minecraft.util.math.MathHelper.atan2(e, d) * 57.2957763671875) - this.getYaw();
                } else {
                    this.damageTiltYaw = (float)(Math.random() * 360.0);
                }

                // Дополнительные эффекты если атакует наш локальный игрок
                if (attacker instanceof net.minecraft.entity.player.PlayerEntity player && player == client.player) {
                    float cooldown = player.getAttackCooldownProgress(0.5F);
                    boolean isWeak = cooldown < 0.9F;
                    boolean isSprint = player.isSprinting() && cooldown > 0.9F;

                    // Проверяем критический удар: игрок падает, не на земле, не на лестнице, не в воде, без слепоты, не на транспорте
                    boolean isCrit = !isWeak 
                            && player.fallDistance > 0.0F 
                            && !player.isOnGround() 
                            && !player.isClimbing() 
                            && !player.isSubmergedInWater() 
                            && !player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS) 
                            && !player.hasVehicle();

                    if (isCrit) {
                        // Проверяем, воспроизведет ли HitSound свой кастомный звук, чтобы не дублировать
                        boolean hitSoundWillPlay = false;
                        try {
                            farvix.solution.client.modules.impl.environment.HitSound hitSound =
                                    farvix.solution.Client.getInstance().getModuleManager().get(
                                            farvix.solution.client.modules.impl.environment.HitSound.class);
                            if (hitSound != null && hitSound.isEnabled()) {
                                String hitSoundTarget = hitSound.target.getCurrentMode();
                                if (hitSoundTarget.equals("Все") || hitSoundTarget.equals("Игроки")) {
                                    hitSoundWillPlay = true;
                                }
                            }
                        } catch (Exception ignored) {}

                        if (!hitSoundWillPlay) {
                            // Звук критического удара (из текущего ресурс-пака)
                            client.world.playSound(this.getX(), this.getY(), this.getZ(),
                                    net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                                    net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f, false);
                        }

                        // Ванильные частицы критического удара
                        double halfHeight = this.getHeight() / 2.0;
                        for (int i = 0; i < 15; ++i) {
                            double px = this.getX() + (this.random.nextDouble() - 0.5) * this.getWidth();
                            double py = this.getY() + halfHeight + (this.random.nextDouble() - 0.5) * this.getHeight();
                            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * this.getWidth();
                            double vx = (this.random.nextDouble() - 0.5) * 0.5;
                            double vy = (this.random.nextDouble() - 0.5) * 0.5;
                            double vz = (this.random.nextDouble() - 0.5) * 0.5;
                            client.world.addParticle(net.minecraft.particle.ParticleTypes.CRIT, px, py, pz, vx, vy, vz);
                        }
                    } else if (isWeak) {
                        // Слабый удар (быстрый спам / начало комбо с низким кулдауном)
                        client.world.playSound(this.getX(), this.getY(), this.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_WEAK,
                                net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f, false);
                    } else if (isSprint) {
                        // Сильный удар с разбега (Knockback / Combo)
                        client.world.playSound(this.getX(), this.getY(), this.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                                net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f, false);
                    } else {
                        // Обычный сильный удар
                        client.world.playSound(this.getX(), this.getY(), this.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                                net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f, false);
                    }

                    // Проверка зачарования оружия для спавна магических критов
                    net.minecraft.item.ItemStack mainHandStack = player.getMainHandStack();
                    if (mainHandStack != null && !mainHandStack.isEmpty() && mainHandStack.hasEnchantments()) {
                        // Ванильные частицы волшебного удара (острота и т.д.)
                        double halfHeight = this.getHeight() / 2.0;
                        for (int i = 0; i < 10; ++i) {
                            double px = this.getX() + (this.random.nextDouble() - 0.5) * this.getWidth();
                            double py = this.getY() + halfHeight + (this.random.nextDouble() - 0.5) * this.getHeight();
                            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * this.getWidth();
                            double vx = (this.random.nextDouble() - 0.5) * 0.5;
                            double vy = (this.random.nextDouble() - 0.5) * 0.5;
                            double vz = (this.random.nextDouble() - 0.5) * 0.5;
                            client.world.addParticle(net.minecraft.particle.ParticleTypes.ENCHANTED_HIT, px, py, pz, vx, vy, vz);
                        }
                    }
                }
            }

            // Бот полностью неподвижен
            this.setVelocity(0, 0, 0);
            this.setHealth(20.0f); // Бесконечное ХП
            
            return true; // Возвращаем true, чтобы урон регистрировался на стороне игрока (криты, свип, частицы, звук)
        }

        @Override
        public void tick() {
            MinecraftClient client = MinecraftClient.getInstance();
            
            if (client.player != null) {
                // Поведение: Повторение приседаний
                this.setSneaking(client.player.isSneaking());
            }

            // Полностью отключаем физику гравитации и перемещения для бота, чтобы он оставался на высоте спавна
            this.setVelocity(0, 0, 0);
            
            super.tick();
        }
    }
}
