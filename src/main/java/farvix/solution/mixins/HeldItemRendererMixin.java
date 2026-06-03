package farvix.solution.mixins;

import farvix.solution.Client;
import farvix.solution.client.modules.impl.visuals.SwingAnimation;
import farvix.solution.client.modules.impl.visuals.HoldMyItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.consume.UseAction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.LeadItem;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Random;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow private ItemStack mainHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private ItemStack offHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float prevEquipProgressOffHand;

    @Shadow
    protected abstract void swingArm(float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm);

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Shadow
    protected abstract void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Shadow
    protected abstract void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);

    @Shadow
    protected abstract void renderMapInBothHands(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float pitch, float equipProgress, float swingProgress);

    @Shadow
    protected abstract void renderMapInOneHand(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, Arm arm, float swingProgress, ItemStack stack);

    @Shadow
    protected abstract void renderArmHoldingItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm);

    @Shadow
    protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);

    // HoldMyItems custom rendering states
    private boolean repPower = false;
    private float prevAge = 0.0F;
    private double previousRotation = 0.0;
    private float swingAngleY = 0.0F;
    private float swingAngleX = 0.0F;
    private float swingVelocityY = 0.0F;
    private float swingVelocityX = 0.0F;
    private float swingVelocityZ = 0.0F;
    private float vertAngleY = 0.0F;
    private float vertVelocityY = 0.0F;
    private float vertVelocityYSlime = 0.0F;
    private float vertAngleYSlime = 0.0F;
    private float riptideCounter = 0.0F;
    private float netherCounter = 0.0F;
    private float fallCounter = 0.0F;
    private float inWaterCounter = 0.0F;
    private float freezeCounter = 0.0F;
    private float clCount = 0.0F;
    private float crawlCount = 0.0F;
    private float directionalCrawlCount = 0.0F;
    private float climbCount = 0.0F;
    private float mouseHolding = 1.0F;
    private boolean isAttacking = false;
    private boolean left = false;

    private float easeInOutBack(float x) {
       float c1 = 1.70158F;
       float c2 = c1 * 1.525F;
       return (float)(
          x < 0.5
             ? Math.pow(2.0F * x, 2.0) * ((c2 + 1.0F) * 2.0F * x - c2) / 2.0
             : (Math.pow(2.0F * x - 2.0F, 2.0) * ((c2 + 1.0F) * (x * 2.0F - 2.0F) + c2) + 2.0) / 2.0
       );
    }

    private float getAttackDamage(ItemStack stack) {
        net.minecraft.component.type.AttributeModifiersComponent modifiers = stack.get(net.minecraft.component.DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0F;
        } else {
            float totalDamage = 0.0F;
            for (net.minecraft.component.type.AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                if (entry.attribute().equals(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE)) {
                    totalDamage += (float) entry.modifier().value();
                }
            }
            return totalDamage;
        }
    }

    private void altSwing(MatrixStack matrices, Arm arm, float swingProgress, ItemStack item) {
       int i = arm == Arm.RIGHT ? 1 : -1;
       float f = MathHelper.sin(swingProgress * 3.14F);
       matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * 0.0F)));
       matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
    }

    @Redirect(
       method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
       at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
       )
    )
    private void renderOverhaul(
       HeldItemRenderer instance,
       AbstractClientPlayerEntity player,
       float tickDelta,
       float pitch,
       Hand hand,
       float swingProgress,
       ItemStack item,
       float equipProgress,
       MatrixStack matrices,
       VertexConsumerProvider vertexConsumers,
       int light
    ) {
       HoldMyItems hmi = farvix.solution.Client.getInstance().getModuleManager().get(HoldMyItems.class);
       if (hmi != null && hmi.isEnabled()) {
          this.renderCustomHoldMyItems(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light, hmi);
       } else {
          this.renderFirstPersonItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
       }
    }

    private void renderCustomHoldMyItems(
       AbstractClientPlayerEntity player,
       float tickDelta,
       float pitch,
       Hand hand,
       float swingProgress,
       ItemStack item,
       float equipProgress,
       MatrixStack matrices,
       VertexConsumerProvider vertexConsumers,
       int light,
       HoldMyItems config
    ) {
       if (!player.isSpectator()) {
          if (!player.isUsingSpyglass()) {
             float yaw = player.getYaw();
             double radians = Math.toRadians((double)yaw);
             double forwardX = -Math.sin(radians);
             double forwardZ = Math.cos(radians);
             Vec3d horizontalVelocity = player.getVelocity();
             double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
             double crossProduct = player.getVelocity().x * forwardZ - horizontalVelocity.z * forwardX;
             float al;
             if (player.getPitch() != 0.0F) {
                al = 90.0F / player.getPitch() / 10.0F;
             } else {
                al = 1.0F;
             }

             if (al > 1.0F) {
                al = 1.0F;
             }

             if (al < 0.0F) {
                al = 1.0F;
             }

             boolean bl = hand == Hand.MAIN_HAND;
             Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
             float kj = bl ? 1.0F : -1.0F;
             matrices.push();
             matrices.push();

             boolean isRightHand = arm == Arm.RIGHT;
             float xOff = isRightHand ? config.rightX.getValue() : -config.leftX.getValue();
             float yOff = isRightHand ? config.rightY.getValue() : config.leftY.getValue();
             float zOff = isRightHand ? config.rightZ.getValue() : config.leftZ.getValue();
             matrices.translate(xOff, yOff, zOff);

             double tt = HoldMyItems.deltaTime * 30.0;
             float swing_rot = swingProgress < 0.6F
                ? MathHelper.sin(MathHelper.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F)
                : MathHelper.sin(MathHelper.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
             float swing = MathHelper.sin(swingProgress * 3.14F);
             swing = this.easeInOutBack(swing);

             if ((item.isOf(net.minecraft.item.Items.EXPERIENCE_BOTTLE)
                   || item.isOf(net.minecraft.item.Items.EGG)
                   || item.isOf(net.minecraft.item.Items.ENDER_EYE)
                   || item.isOf(net.minecraft.item.Items.SNOWBALL)
                   || item.isOf(net.minecraft.item.Items.ENDER_PEARL)
                   || item.getItem() instanceof net.minecraft.item.SplashPotionItem
                   || item.getItem() instanceof net.minecraft.item.LingeringPotionItem)
                   && player.getOffHandStack().isEmpty()
                   && item.getUseAction() != UseAction.SPEAR
                   && !item.isOf(net.minecraft.item.Items.FIRE_CHARGE)
                   && !player.isSwimming()
                   && !player.isCrawling()
                   && !player.isClimbing()) {
                if (player.getMainArm() == Arm.LEFT) {
                   bl = !bl;
                }

                float ll = bl ? 1.0F : -1.0F;
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25.0F * ll));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * ll * swing));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                matrices.translate(-0.15 * ll, 0.1, 0.1);
                matrices.translate(0.0, -0.55 * swing, 0.4 * swing * 3.14F);
                this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, 0.0F, arm.getOpposite());
                matrices.pop();
             }

             if (this.client.options.attackKey.isPressed() && !this.isAttacking && swingProgress == 0.0F) {
                this.left = !this.left;
             }

             if (!item.isEmpty()) {
                if (player.getMainArm() == Arm.LEFT) {
                   bl = !bl;
                }

                float ll = bl ? 1.0F : -1.0F;
                if ((this.left || item.isIn(net.minecraft.registry.tag.ItemTags.AXES) || item.getUseAction() == UseAction.SPEAR || item.getUseAction() == UseAction.BLOCK) && !item.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) {
                   if (!item.isIn(net.minecraft.registry.tag.ItemTags.SWORDS) && !item.isIn(net.minecraft.registry.tag.ItemTags.AXES)) {
                      if (item.getUseAction() == UseAction.SPEAR) {
                         matrices.translate(0.0F, 0.0F, 0.45 * swing_rot);
                         matrices.translate(-0.25F * kj * swing, -0.35 * swing_rot, -0.6 * swing);
                         matrices.translate(0.0F, 0.1 * swing, 0.0F);
                         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * ll));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swing_rot * ll));
                      } else if (item.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags.TOOLS) && item.getUseAction() != UseAction.BLOCK && !item.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) {
                         matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.5F * swing);
                         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * ll));
                         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                      } else if (item.getUseAction() != UseAction.BLOCK) {
                         matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * ll));
                         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * ll));
                      } else {
                         matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.2 * swing);
                         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0F * swing_rot));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * ll));
                         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * swing));
                      }
                   } else {
                      matrices.translate(0.8 * ll * swing_rot, 0.3 * swing_rot, -0.5F * swing);
                      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * ll));
                      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0F * swing_rot));
                      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * swing_rot * ll));
                      if (item.isIn(net.minecraft.registry.tag.ItemTags.SWORDS)) {
                         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                      } else {
                         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(30.0F * swing));
                      }
                   }
                } else if (!item.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) {
                   if (item.isIn(net.minecraft.registry.tag.ItemTags.SWORDS)) {
                      matrices.translate(-0.55 * ll * swing_rot, -0.8 * swing_rot, -0.77 * swing);
                      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5.0F * swing_rot * ll));
                      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(70.0F * swing_rot * ll));
                      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(50.0F * swing));
                   } else if (item.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags.TOOLS) && !item.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) {
                      matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.5F * swing);
                      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * ll));
                      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                   } else {
                      matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * ll));
                      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * ll));
                   }
                } else if (item.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) {
                   matrices.translate(0.0F, 0.15 * swing_rot, -0.25F * swing_rot);
                   matrices.translate(0.0F, 0.0F, -0.2 * swing);
                   matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot));
                   matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * swing_rot));
                   matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                }
             } else if (net.minecraft.block.Block.getBlockFromItem(item.getItem()) != net.minecraft.block.Blocks.AIR
                   && (!item.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags.TOOLS)
                      || item.isIn(net.minecraft.registry.tag.ItemTags.TRIMMABLE_ARMOR)
                      || item.isIn(net.minecraft.registry.tag.ItemTags.BOOKSHELF_BOOKS)
                      || item.getUseAction() == UseAction.EAT
                      || !item.isDamageable())
                   && item.getUseAction() != UseAction.BOW
                   && item.getUseAction() != UseAction.SPYGLASS
                   && this.getAttackDamage(item) == 0.0F
                   && item.getUseAction() != UseAction.BLOCK
                   && !item.isOf(net.minecraft.item.Items.WARPED_FUNGUS_ON_A_STICK)
                   && !item.isOf(net.minecraft.item.Items.CARROT_ON_A_STICK)
                   && !(item.getItem() instanceof net.minecraft.item.FishingRodItem)
                   && !item.isOf(net.minecraft.item.Items.SHEARS)) {
                 swingProgress = (float)(swingProgress * 1.2);
                 if (swingProgress > 1.0F) {
                     swingProgress = 0.0F;
                 }
             } else if (!item.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) {
                 swingProgress = (float)(swingProgress * 1.5F);
                 if (swingProgress > 1.0F) {
                     swingProgress = 0.0F;
                 }
             }

             if (player.getVelocity().horizontalLength() >= 0.08) {
                 this.crawlCount = (float)(this.crawlCount + 0.1 * player.getVelocity().horizontalLength() * 2.0F * tt);
                 this.directionalCrawlCount = (float)(this.directionalCrawlCount + 0.1 * dotProduct * 4.0F * tt);
                 this.directionalCrawlCount = (float)(this.directionalCrawlCount + (dotProduct > 0.0F ? 0.1 * Math.abs(crossProduct) * 4.0F * tt : 0.1 * Math.abs(crossProduct) * -1.0F * 4.0F * tt));
             }

             if (player.getVelocity().y > 0.0F) {
                 this.climbCount = (float)(this.climbCount + 0.1 * tt);
             }

             if (player.getVelocity().y < 0.0F) {
                 this.climbCount = (float)(this.climbCount - 0.1 * tt);
             }

             if ((player.isCrawling() && config.climbAndCrawl.getValue()
                   || player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().y) > 0.0F && config.climbAndCrawl.getValue())
                   && !player.isUsingItem()
                   && swingProgress == 0.0F) {
                 this.clCount = (float)(this.clCount + 0.1 * tt);
                 if (this.clCount > 1.0F) {
                     this.clCount = 1.0F;
                 }

                 if (!item.isOf(net.minecraft.item.Items.LANTERN) && !item.isOf(net.minecraft.item.Items.SOUL_LANTERN)) {
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * this.clCount));
                 }
             } else {
                 this.clCount = (float)(this.clCount * Math.pow(0.88F, tt));
             }

             if (swingProgress == 0.0F) {
                 matrices.translate(bl ? player.getPitch() / 650.0F * this.clCount * -1.0F : player.getPitch() / 650.0F * this.clCount, 0.0F, 0.0F);
                 matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.getPitch() * this.clCount));
             }

             if (!item.isOf(net.minecraft.item.Items.LANTERN) && !item.isOf(net.minecraft.item.Items.SOUL_LANTERN)) {
                 matrices.translate(0.0F, 0.0F, player.getPitch() / 120.0F * this.clCount);
             } else if (swingProgress == 0.0F) {
                 matrices.translate(0.0F, 0.0F, player.getPitch() / 80.0F * this.clCount);
             }

             if (player.isClimbing() && config.climbAndCrawl.getValue() && !player.isOnGround() && !item.isOf(net.minecraft.item.Items.LANTERN) && !item.isOf(net.minecraft.item.Items.SOUL_LANTERN) && !player.isUsingItem()) {
                 matrices.translate(0.0F, 0.1, -0.2);
             }

             if ((player.isSubmergedInWater() || player.inPowderSnow) && !player.isSwimming() && !player.isSneaking()) {
                 this.inWaterCounter = (float)(this.inWaterCounter + 0.1 * tt);
                 if (this.inWaterCounter >= 1.0F) {
                     this.inWaterCounter = 1.0F;
                 }
             } else {
                 this.inWaterCounter = (float)(this.inWaterCounter * Math.pow(0.88F, tt));
             }

             float freezingScale = MathHelper.clamp((float)player.getFrozenTicks() / (float)player.getMinFreezeDamageTicks(), 0.0F, 1.0F);
             if (player.inPowderSnow && freezingScale > 0.1) {
                 this.freezeCounter = (float)(this.freezeCounter + 0.1 * tt);
             } else {
                 this.freezeCounter = (float)(this.freezeCounter * Math.pow(0.88F, tt));
             }

             matrices.translate(0.0F, 0.02 * this.inWaterCounter, 0.0F);
             matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F * kj * this.inWaterCounter));
             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.3F * MathHelper.sin(this.freezeCounter * 5.0F)));
             if (player.getVelocity().y < -0.85 && item.isOf(net.minecraft.item.Items.MACE) && player.getMainHandStack() == item) {
                 this.fallCounter = (float)(this.fallCounter + 0.1 * tt);
                 if (this.fallCounter >= 1.0F) {
                     this.fallCounter = 1.0F;
                 }
             } else {
                 this.fallCounter = (float)(this.fallCounter * Math.pow(0.88F, tt));
             }

             if (bl) {
                 matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F * this.fallCounter));
                 matrices.translate(0.0F, -0.2 * this.fallCounter, 0.0F);
             }

             this.vertAngleY = (float)(this.vertAngleY + player.getVelocity().y * 0.015F * tt);
             this.vertAngleY = (float)(this.vertAngleY - (0.1F * this.vertAngleY) * tt);
             this.vertAngleY = (float)(this.vertAngleY * Math.pow(0.88F, tt));
             this.vertVelocityYSlime = (float)(this.vertVelocityYSlime + player.getVelocity().y * 0.015F * tt);
             this.vertVelocityYSlime = (float)(this.vertVelocityYSlime - (0.1F * this.vertAngleYSlime) * tt);
             this.vertVelocityYSlime = (float)(this.vertVelocityYSlime * Math.pow(0.88F, tt));
             this.vertAngleYSlime = (float)(this.vertAngleYSlime + this.vertVelocityYSlime * tt);
             matrices.translate(0.0F, this.vertAngleY * -1.0F, 0.0F);
             matrices.translate(0.0, Math.sin(player.age * 0.1) * 0.007 * kj, 0.0);
             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15F * MathHelper.sin(player.age * 0.15F) * kj));
             if (!item.isEmpty() || player.isCrawling() || player.isClimbing() && !player.isOnGround() || player.isSwimming()) {
                 if (player.getMainArm() == Arm.LEFT) {
                     bl = !bl;
                 }

                 if (item.getUseAction() == UseAction.BLOCK) {
                     matrices.translate(0.0F, 0.0F, 0.0F);
                 } else {
                     matrices.translate(0.0, -0.1, 0.1);
                 }
             }

             if (item.isOf(net.minecraft.item.Items.LANTERN) || item.isOf(net.minecraft.item.Items.SOUL_LANTERN) || item.isIn(net.minecraft.registry.tag.ItemTags.HANGING_SIGNS)) {
                 matrices.translate(0.0, 0.1, 0.0);
                 if (player.isSwimming()) {
                     matrices.translate(0.0, -0.1, 0.1);
                 }
             }

             if (player.isSwimming() && swingProgress == 0.0F && config.swimmingAnimation.getValue()) {
                 double s = (double)(player.age + tickDelta) * 0.1;
                 double swingAmplitude = 1.5;
                 double frequency = 2.0;
                 s *= frequency;
                 double handRotation = Math.sin(s) * swingAmplitude;
                 double smoothRotation = handRotation * 0.8 + this.previousRotation * 0.2;
                 matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(bl ? smoothRotation : -smoothRotation)));
                 matrices.translate(0.0, 0.0, smoothRotation * 0.2F);
                 double k = (double)(player.age + tickDelta) * 0.2;
                 double a = Math.cos(k);
                 double b = a;
                 if (a <= 0.0) {
                     b = a * 0.5;
                 }

                 matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(bl ? b * 30.0 : b * 30.0 * -1.0)));
                 matrices.translate(0.0, 0.0, a * 0.2F);
                 if (item.isEmpty() && !bl && !player.isInvisible()) {
                     float j1 = bl ? 1.0F : -1.0F;
                     matrices.translate(j1, 0.0F - equipProgress * 0.3F, 0.3F);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * j1));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * j1));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                     this.altSwing(matrices, arm, swingProgress, item);
                     float n = MathHelper.sin(equipProgress * 3.14F);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                 }

                 this.previousRotation = smoothRotation;
             }

             if ((player.isClimbing() && !player.isOnGround() || player.isCrawling() && swingProgress == 0.0F) && !player.isUsingItem()) {
                 double s = (double)(player.age + tickDelta) * 0.1;
                 float h = MathHelper.cos((float)s * 2.0F);
                 float j = bl ? 1.0F : -1.0F;
                 if (player.isClimbing()) {
                     if (!item.isOf(net.minecraft.item.Items.LANTERN) && !item.isOf(net.minecraft.item.Items.SOUL_LANTERN)) {
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * h * j));
                     } else {
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(1.0F * h * j));
                     }
                 }

                 if (player.isCrawling() && !player.isUsingItem() && swingProgress == 0.0F) {
                     float timeValue = (float)(player.age + tickDelta) * 0.4F;
                     float l = MathHelper.sin(timeValue * this.mouseHolding);
                     float dt = MathHelper.cos(timeValue * this.mouseHolding);
                     if (item.isOf(net.minecraft.item.Items.LANTERN) || item.isOf(net.minecraft.item.Items.SOUL_LANTERN)) {
                         l *= 0.14F;
                         dt *= 0.14F;
                     }

                     matrices.translate(0.2 * l, 0.3 * l * j, -0.2 * l * j * al);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * l));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(20.0F * dt * j, 0.0F, 20.0F)));
                 }

                 if (item.isEmpty() && !bl && !player.isInvisible() && (!player.isOnGround() && player.isClimbing() || player.isCrawling())) {
                     float l = bl ? 1.0F : -1.0F;
                     matrices.translate(l, 0.0F - equipProgress * 0.3F, 0.3F);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * l));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * l));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                     this.altSwing(matrices, arm, swingProgress, item);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                 }
             }

             if (item.isEmpty()) {
                 if (bl && !player.isInvisible()) {
                     float ll = bl ? 1.0F : -1.0F;
                     if ((player.isOnGround() || !player.isClimbing()) && !player.isSwimming() && !player.isCrawling()) {
                         if (player.getMainArm() == Arm.LEFT) {
                             bl = !bl;
                         }

                         matrices.translate(0.0F, 0.2 * swing_rot, 0.15 * swing_rot);
                         matrices.translate(0.1 * ll * swing, 0.15 * swing, -0.45 * swing);
                         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * swing * ll));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swing));
                         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0F * swing_rot * ll));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F * swing_rot));
                         this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                     } else {
                         matrices.translate(ll, 0.0F - equipProgress * 0.3F, 0.3F);
                         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * ll));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * ll));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                         this.altSwing(matrices, arm, swingProgress, item);
                         matrices.scale(0.9F, 0.9F, 0.9F);
                         this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                     }
                 }
             } else if (item.contains(net.minecraft.component.DataComponentTypes.MAP_ID)) {
                 if (bl && this.offHand.isEmpty()) {
                     matrices.translate(0.0F, 0.1, 0.0F);
                     this.renderMapInBothHands(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
                 } else {
                     matrices.translate(bl ? -0.1 : 0.1, 0.1, 0.0F);
                     this.renderMapInOneHand(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, item);
                 }
             } else if (item.getUseAction() == UseAction.CROSSBOW) {
                 matrices.push();
                 boolean bl2 = net.minecraft.item.CrossbowItem.isCharged(item);
                 boolean bl3 = arm == Arm.RIGHT;
                 int i = bl3 ? 1 : -1;
                 if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     matrices.translate((float)i * -0.4785682F, -0.24387F, 0.05731531F);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * 65.3F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i * 9.785F));
                     float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                     float g = f / (float)net.minecraft.item.CrossbowItem.getPullTime(item, player);
                     if (g > 1.0F) {
                         g = 1.0F;
                     }

                     if (g > 0.1F) {
                         float h = MathHelper.sin((f - 0.1F) * 1.3F);
                         float j = g - 0.1F;
                         float yawDelta = h * j;
                         matrices.translate(yawDelta * 0.0F, yawDelta * 0.004F, yawDelta * 0.0F);
                     }

                     matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                     matrices.scale(1.0F, 1.0F, 1.0F);
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)i * 45.0F));
                 } else {
                     this.swingArm(swingProgress, equipProgress, matrices, i, arm);
                     if (bl2 && swingProgress < 0.001F && bl) {
                         matrices.translate((float)i * -0.341864F, 0.0F, 0.0F);
                         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * 10.0F));
                     }
                 }

                 float yawDelta = bl ? 1.0F : -1.0F;
                 matrices.translate(0.0F, 0.0F, -1.0F);
                 matrices.translate(-0.45 * (double)i, 0.45, 1.7);
                 matrices.translate((double)yawDelta, (double)0.0F - (double)equipProgress * 0.3, 0.3);
                 matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * yawDelta));
                 matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * yawDelta));
                 matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                 this.altSwing(matrices, arm, swingProgress, item);
                 matrices.scale(0.9F, 0.9F, 0.9F);
                 this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                 matrices.translate((double)-0.25F * (double)i, 1.25F, 0.05);
                 matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-90 * i)));
                 matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(77.0F));
                 matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(85 * i)));
                 matrices.scale(1.2F, 1.2F, 1.2F);
                 matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
                 matrices.translate(0.0F, -0.15F, 0.15F);
                 this.renderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
                 matrices.pop();
                 if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                     float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                     float g = f / (float)net.minecraft.item.CrossbowItem.getPullTime(item, player);
                     if (g > 1.0F) {
                         g = 1.0F;
                     }

                     if (g > 0.1F) {
                         float h = MathHelper.sin((f - 0.1F) * 1.3F);
                         float j = g - 0.1F;
                         float k = h * j;
                         matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                     }

                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((double)g <= 0.2 ? 75.0F * g * 5.0F * (float)i : (float)(75 * i)));
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(10.0F * g * 1.5F));
                     matrices.translate(-0.37 * (double)i, 0.0, 0.6);
                     matrices.translate(0.15 * (double)g * (double)i, 0.0, 0.0);
                     this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                 }
             } else {
                 boolean bl2 = arm == Arm.RIGHT;
                 int l = bl2 ? 1 : -1;
                 if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                     switch (item.getUseAction()) {
                         case NONE:
                             this.applyEquipOffset(matrices, arm, equipProgress);
                             break;
                         case EAT:
                         case DRINK:
                             float yawDelta = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                             float pitchDelta = yawDelta / 5.0F;
                             if (pitchDelta > 1.0F) {
                                 pitchDelta = 1.0F;
                             }

                             float k = MathHelper.sin(yawDelta / 2.0F * 3.14F);
                             k /= 10.0F;
                             matrices.translate((double)l, 0.1, 0.3);
                             matrices.translate(0.2 * (double)l * (double)pitchDelta, -0.7 * (double)pitchDelta, -0.2 * (double)pitchDelta);
                             matrices.translate(0.0F, -0.2 * (double)k, -0.2 * (double)k);
                             matrices.translate(0.0F, 0.1 * (double)this.easeInOutBack(MathHelper.sin(pitchDelta * 3.14F)), 0.0F);
                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
                             matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                             this.altSwing(matrices, arm, swingProgress, item);
                             matrices.scale(0.9F, 0.9F, 0.9F);
                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * pitchDelta * (float)l));
                             this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                             break;
                         case BLOCK:
                             k = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                             double s = (double)(k / 4.0F);
                             float s2 = k / 6.0F;
                             if (s > 1.0) {
                                 s = 1.0;
                             }

                             if (s2 > 1.0F) {
                                 s2 = 1.0F;
                             }

                             matrices.translate(0.0F, -0.2F, 0.0F);
                             matrices.translate(l, 0.0F, 0.3F);
                             matrices.translate(0.7 * s * (double)l, 0.0, -1.3 * s);
                             matrices.translate(-0.2 * (double)l * (double)s2, 0.0, 0.0);
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(10.0F * Math.sin((double)s2 * 3.14F))));
                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(70.0F * s * (double)l)));
                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
                             matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)((double)(5 * l) * s)));
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(-10.0F * s)));
                             matrices.translate(0.0F, 0.0F, -0.2 * s);
                             this.altSwing(matrices, arm, swingProgress, item);
                             matrices.scale(0.9F, 0.9F, 0.9F);
                             this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                             matrices.translate(0.35 * (double)l, -0.13, -0.12);
                             matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10.0F * (float)l));
                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * (float)l));
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));
                             matrices.translate(-0.2 * (double)l, -0.04, 0.15);
                             matrices.scale(1.0F, 1.0F, 1.0F);
                             break;
                         case BOW:
                             matrices.push();
                             if (player.getMainArm() == Arm.LEFT) {
                                 bl = !bl;
                             }

                             float m1 = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                             float f1 = m1 / 20.0F;
                             float f = (f1 * f1 + f1 * 2.0F) / 3.0F;
                             if (f1 > 1.0F) {
                                 f1 = 1.0F;
                             }

                             if (f1 > 0.1F) {
                                 float g1 = MathHelper.sin((m1 - 0.1F) * 1.3F);
                                 float j1 = g1 * f1;
                                 matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                             }

                             matrices.translate(bl ? -0.1 : 0.1, 0.0, (double)f1 * 0.15);
                             this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                             matrices.pop();
                             matrices.push();
                             matrices.translate(bl ? -0.5F : 0.5F, -0.45, 0.1);
                             matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.3F));
                             if (bl) {
                                 matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(-0.3F));
                                 matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(1.0F));
                             } else {
                                 matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-0.3F));
                                 matrices.multiply(RotationAxis.POSITIVE_Y.rotation(1.0F));
                             }

                             this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                             if (bl) {
                                 matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(2.5F));
                             } else {
                                 matrices.multiply(RotationAxis.POSITIVE_Y.rotation(2.5F));
                             }

                             matrices.translate(bl ? -0.65 : 0.65, -0.35, 0.27);
                             if (f1 > 1.0F) {
                                 f1 = 1.0F;
                             }

                             matrices.pop();
                             matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(75.0F));
                             matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees((float)(-15 * l)));
                             matrices.translate(0.8 * (double)l, (double)(0.0F - equipProgress * 0.3F), -0.1);
                             if (f > 0.1F) {
                                 float g1 = MathHelper.sin((m1 - 0.1F) * 1.3F);
                                 float h1 = f1 - 0.1F;
                                 float j1 = g1 * h1;
                                 matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                             }

                             break;
                         case SPEAR:
                             if (player.getOffHandStack().isEmpty() && !player.isCrawling() && !player.isSwimming() && !player.isClimbing()) {
                                 matrices.push();
                                 matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-25 * l)));
                                 matrices.translate(-0.15 * (double)l, 0.1, 0.1);
                                 this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                                 matrices.pop();
                             }

                             float dt = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                             f = dt / 10.0F;
                             if (f > 1.0F) {
                                 f = 1.0F;
                             }

                             if (f > 0.1F) {
                                 float g = MathHelper.sin((dt - 0.1F) * 1.3F);
                                 float h = f - 0.1F;
                                 float j = g * h;
                                 matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                             }

                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l)));
                             matrices.translate(0.2 * (double)l, 0.0F, 0.8);
                             this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
                             matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-65 * l)));
                             matrices.translate((double)(0.65F * (float)l), -1.0, -0.6);
                             break;
                         case BRUSH:
                             float g1 = (float)(player.getItemUseTimeLeft() % 10);
                             float h1 = g1 - tickDelta + 1.0F;
                             float j1 = 1.0F - h1 / 10.0F;
                             float n = -15.0F + 75.0F * MathHelper.cos(j1 * 2.0F * (float)Math.PI);
                             float z = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                             float x = z / 4.0F;
                             if (x > 1.0F) {
                                 x = 1.0F;
                             }

                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l) * x));
                             matrices.translate((double)(0.3F * (float)l * x), 0.3 * (double)x, 0.1 * (double)x);
                             if (x == 1.0F) {
                                 matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n / 20.0F));
                             }

                             this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                             break;
                     }
                 } else if (player.isUsingRiptide() && item.getUseAction() == UseAction.SPEAR) {
                     this.riptideCounter = (float)((double)this.riptideCounter + 0.15 * tt);
                     float dt = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                     float f = dt / 10.0F;
                     if (f > 1.0F) {
                         f = 1.0F;
                     }

                     if (f > 0.1F) {
                         float g = MathHelper.sin((dt - 0.1F) * 1.3F);
                         float h = f - 0.1F;
                         float j = g * h;
                         matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                     }

                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F - this.riptideCounter * 2.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l)));
                     matrices.translate(0.2 * (double)l, 0.0F, 0.75F);
                     matrices.translate(0.0F, 0.0F, 0.01 * (double)MathHelper.sin(this.riptideCounter * 6.28F));
                     this.renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-65 * l)));
                     matrices.translate((double)(0.65F * (float)l), -1.0, -0.6);
                 } else {
                     this.riptideCounter = 0.0F;
                     if (!item.isOf(net.minecraft.item.Items.LANTERN) && !item.isOf(net.minecraft.item.Items.SOUL_LANTERN) && !item.isIn(net.minecraft.registry.tag.ItemTags.HANGING_SIGNS)) {
                         if (item.getUseAction() == UseAction.BLOCK) {
                             matrices.translate(0.0F, -0.2F, 0.0F);
                         }
                     } else {
                         matrices.translate(0.1 * (double)l, 0.0F, -0.1);
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
                     }

                     matrices.translate((double)l, 0.0F - (double)equipProgress * 0.3, 0.3);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                     this.altSwing(matrices, arm, swingProgress, item);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     this.renderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                 }

                 matrices.translate(-0.3 * (double)l, 0.65, -0.1);
                 matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-65 * l)));
                 matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
                 if (item.isIn(net.minecraft.registry.tag.ItemTags.WOOL_CARPETS)) {
                     matrices.translate(0.2 * (double)l, -0.1, 0.0F);
                 }

                 if (net.minecraft.block.Block.getBlockFromItem(item.getItem()) != net.minecraft.block.Blocks.AIR && item.getUseAction() != UseAction.EAT && !item.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags.BUCKETS)) {
                     if (item.getName().getString().toLowerCase().contains("TORCH".toLowerCase())) {
                         matrices.scale(1.5F, 1.5F, 1.5F);
                         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(25 * l)));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
                         matrices.translate(0.2 * (double)l, 0.2, 0.05);
                     } else if ((item.isOf(net.minecraft.item.Items.STRING)
                           || item.isOf(net.minecraft.item.Items.REDSTONE)
                           || item.isOf(net.minecraft.item.Items.LEVER)
                           || item.isOf(net.minecraft.item.Items.TRIPWIRE_HOOK)
                           || net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES)
                           || net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.RAILS)
                           || net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.CLIMBABLE)
                           || item.isIn(net.minecraft.registry.tag.ItemTags.DOORS))
                           && !net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.LEAVES)
                           && !net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.COMBINATION_STEP_SOUND_BLOCKS)
                           && !net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.BANNERS)) {
                         matrices.translate(0.0F, 0.0F, -0.1);
                         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(5 * l)));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
                     } else if (!item.isOf(net.minecraft.item.Items.LANTERN) && !item.isOf(net.minecraft.item.Items.SOUL_LANTERN) && !item.isIn(net.minecraft.registry.tag.ItemTags.HANGING_SIGNS)) {
                         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(25 * l)));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
                         matrices.translate(0.2 * (double)l, 0.2, 0.05);
                         if (net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.BANNERS)) {
                             matrices.translate(-0.2 * (double)l, 0.0F, 0.0F);
                             matrices.scale(1.1F, 1.1F, 1.1F);
                         }
                     } else {
                         float dt = (float)(HoldMyItems.deltaTime * 30.0);
                         float yawDelta = player.prevHeadYaw - player.getHeadYaw();
                         float pitchDelta = player.prevPitch - player.getPitch();
                         this.swingVelocityY += yawDelta * 0.015F * dt;
                         this.swingVelocityY += swingProgress * 2.0F * dt;
                         this.swingVelocityX += pitchDelta * 0.015F * dt;
                         this.swingVelocityY -= 0.1F * this.swingAngleY * dt;
                         this.swingVelocityX -= 0.1F * this.swingAngleX * dt;
                         this.swingVelocityY = (float)((double)this.swingVelocityY * Math.pow(0.88F, (double)dt));
                         this.swingVelocityX = (float)((double)this.swingVelocityX * Math.pow(0.88F, (double)dt));
                         this.swingAngleY += this.swingVelocityY * dt;
                         this.swingAngleX += this.swingVelocityX * dt;
                         double currentSpeed = player.getVelocity().horizontalLength();
                         this.swingVelocityZ = (float)((double)this.swingVelocityZ + (bl ? (currentSpeed * -1.0 * 15.0 - (double)this.swingVelocityZ) * 0.1 * (double)dt : (currentSpeed * 15.0 - (double)this.swingVelocityZ) * 0.1 * (double)dt));
                         if ((currentSpeed > 0.09 && player.isOnGround() || player.isSwimming() || player.isClimbing() && !player.isOnGround()) && this.client.options.getBobView().getValue()) {
                             Random random = new Random();
                             boolean randomBoolean = random.nextBoolean();
                             this.swingVelocityY += (float)(randomBoolean ? -5.5F * currentSpeed * (double)dt : 5.5F * currentSpeed * (double)dt);
                         }

                         matrices.translate(0.0F, 0.0F, -0.1);
                         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(35 * l) + this.swingAngleY));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F + this.swingAngleX));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l) + this.swingVelocityZ));
                         if (item.isIn(net.minecraft.registry.tag.ItemTags.HANGING_SIGNS)) {
                             matrices.translate(0.0F, -0.1, 0.0F);
                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-45 * l)));
                         }

                         matrices.translate(0.3 * (double)l, -0.35, 0.0F);
                         matrices.translate(0.0F, 0.0F, 0.1);
                         matrices.scale(1.5F, 1.5F, 1.5F);
                     }
                 } else {
                     if ((!item.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags.TOOLS)
                           || item.isIn(net.minecraft.registry.tag.ItemTags.TRIMMABLE_ARMOR)
                           || item.isIn(net.minecraft.registry.tag.ItemTags.BOOKSHELF_BOOKS)
                           || item.getUseAction() == UseAction.EAT
                           || !item.isDamageable())
                           && item.getUseAction() != UseAction.BOW
                           && item.getUseAction() != UseAction.SPYGLASS
                           && this.getAttackDamage(item) == 0.0F
                           && item.getUseAction() != UseAction.BLOCK
                           && !item.isOf(net.minecraft.item.Items.WARPED_FUNGUS_ON_A_STICK)
                           && !item.isOf(net.minecraft.item.Items.CARROT_ON_A_STICK)
                           && !(item.getItem() instanceof net.minecraft.item.FishingRodItem)
                           && !item.isOf(net.minecraft.item.Items.SHEARS)
                           && !item.isIn(net.minecraft.registry.tag.ItemTags.HOES)) {
                         if (item.getUseAction() == UseAction.BRUSH) {
                             matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(25.0F));
                             matrices.translate(bl ? 0.0F : 0.35F, bl ? 0.0F : 0.25F, bl ? 0.0F : 0.37F);
                             if (!bl) {
                                 matrices.scale(0.75F, 0.75F, 0.75F);
                             }

                             matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees((float)(-75 * l)));
                             matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(35.0F));
                             matrices.translate(bl ? -0.05F : 0.85F, bl ? 0.0F : 0.05F, bl ? 0.08F : -0.2F);
                         } else {
                             matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(5 * l)));
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                             matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
                             matrices.translate(0.0F, -0.05F, -0.1F);
                             matrices.scale(0.7F, 0.7F, 0.7F);
                         }

                         if (item.isOf(net.minecraft.item.Items.FEATHER) || item.isOf(net.minecraft.item.Items.SLIME_BALL) || item.isOf(net.minecraft.item.Items.PUFFERFISH)) {
                             this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + (double)swingProgress * 0.03 * HoldMyItems.deltaTime * 30.0);
                             if ((player.getVelocity().horizontalLength() > 0.09 && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && this.client.options.getBobView().getValue()) {
                                 this.vertVelocityYSlime += (float)(-0.05 * player.getVelocity().horizontalLength() * HoldMyItems.deltaTime * 30.0);
                             }

                             matrices.scale(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
                         }
                     } else if (item.getUseAction() == UseAction.BLOCK && item.getUseAction() != UseAction.SPEAR) {
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(160 * l)));
                         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-60 * l)));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                         matrices.scale(0.75F, 0.75F, 0.75F);
                         matrices.translate(0.15 * (double)l, bl ? 0.35 : 0.45, bl ? -0.15 : -0.1);
                         matrices.translate(0.17 * (double)l, 0.0F, 0.3);
                         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-90 * l)));
                     } else if (item.getUseAction() == UseAction.SPEAR) {
                         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(75 * l)));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(45 * l)));
                         matrices.translate(-0.3F * (float)l, 0.0F, 0.0F);
                     } else if (item.getUseAction() != UseAction.SPEAR) {
                         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(75 * l)));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
                         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(45 * l)));
                     }

                     if (item.getUseAction() != UseAction.BLOCK) {
                         matrices.scale(1.2F, 1.2F, 1.2F);
                     }

                     if (item.getUseAction() == UseAction.BOW && !player.isUsingItem()) {
                         matrices.translate(-0.1 * (double)l, -0.2, 0.0F);
                     }
                 }

                 if (item.getItem() instanceof net.minecraft.item.BlockItem && !(((net.minecraft.item.BlockItem)item.getItem()).getBlock() instanceof net.minecraft.block.AbstractSkullBlock)) {
                     net.minecraft.item.BlockItem blockItem = (net.minecraft.item.BlockItem)item.getItem();
                     if ((!item.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags.BUCKETS)
                           && item.getUseAction() != UseAction.EAT
                           && !item.isIn(net.minecraft.registry.tag.ItemTags.BANNERS)
                           && !item.isOf(net.minecraft.item.Items.STRING)
                           && !item.isOf(net.minecraft.item.Items.REDSTONE)
                           && !item.isOf(net.minecraft.item.Items.LEVER)
                           && !item.isOf(net.minecraft.item.Items.TRIPWIRE_HOOK)
                           || net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.LEAVES))
                           && !net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                         net.minecraft.client.render.block.BlockRenderManager blockRenderManager = this.client.getBlockRenderManager();
                         matrices.push();
                         if (!bl2) {
                             matrices.translate(-0.4F, 0.0F, 0.0F);
                         }

                         matrices.scale(0.4F, 0.4F, 0.4F);
                         matrices.translate(-0.9 * (double)l, -0.45, -0.5F);
                         if (net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.BUTTONS)) {
                             matrices.translate(0.2 * (double)l, -0.15, -0.2);
                         }

                         if (net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.PRESSURE_PLATES)) {
                             matrices.translate(0.0F, 0.1F, 0.0F);
                         }

                         if (item.isOf(net.minecraft.item.Items.SLIME_BLOCK)
                               || item.isOf(net.minecraft.item.Items.HONEY_BLOCK)
                               || net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.FLOWERS)
                               || net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.LEAVES)
                               || net.minecraft.block.Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(net.minecraft.registry.tag.BlockTags.SAPLINGS)) {
                             this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + (double)swingProgress * 0.03 * HoldMyItems.deltaTime * 30.0);
                             if ((player.getVelocity().horizontalLength() > 0.09 && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && this.client.options.getBobView().getValue()) {
                                 this.vertVelocityYSlime += (float)(-0.05 * player.getVelocity().horizontalLength() * HoldMyItems.deltaTime * 30.0);
                             }

                             matrices.scale(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
                         }

                         BlockState blockState = blockItem.getBlock().getDefaultState();
                         if ((float)player.age - this.prevAge >= 100.0F) {
                             this.repPower = !this.repPower;
                             this.prevAge = (float)player.age;
                         }

                         if (blockItem.getBlock() == net.minecraft.block.Blocks.REPEATER && this.repPower) {
                             blockState = (BlockState)blockState.with(net.minecraft.block.RepeaterBlock.POWERED, true);
                         }

                         if (blockItem.getBlock() == net.minecraft.block.Blocks.COMPARATOR && this.repPower) {
                             blockState = blockState.with(net.minecraft.block.ComparatorBlock.POWERED, true);
                         }

                         if (blockItem.getBlock() == net.minecraft.block.Blocks.REDSTONE_TORCH && player.isSubmergedInWater()) {
                             blockState = blockState.with(net.minecraft.block.RedstoneTorchBlock.LIT, false);
                         }

                         if ((blockItem.getBlock() == net.minecraft.block.Blocks.CAMPFIRE || blockItem.getBlock() == net.minecraft.block.Blocks.SOUL_CAMPFIRE) && player.isSubmergedInWater()) {
                             blockState = blockState.with(net.minecraft.block.CampfireBlock.LIT, false);
                         }

                         if (item.isIn(net.minecraft.registry.tag.ItemTags.BEDS)) {
                             if (bl) {
                                 matrices.translate(0.9, 0.0F, 0.8);
                             }

                             matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * l)));
                         }

                         if (blockState.getRenderType() != net.minecraft.block.BlockRenderType.MODEL) {
                            this.renderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
                         } else {
                            blockRenderManager.renderBlockAsEntity(blockState, matrices, vertexConsumers, light, net.minecraft.client.render.OverlayTexture.DEFAULT_UV);
                         }
                         matrices.pop();
                     }
                 } else {
                     if (item.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags.TOOLS)
                           && !item.isIn(net.minecraft.registry.tag.ItemTags.TRIMMABLE_ARMOR)
                           && !item.isIn(net.minecraft.registry.tag.ItemTags.BOOKSHELF_BOOKS)
                           && item.getUseAction() != UseAction.EAT
                           && item.isDamageable()
                           || item.getUseAction() == UseAction.BOW
                           || item.getUseAction() == UseAction.SPYGLASS
                           || this.getAttackDamage(item) != 0.0F
                           || item.getUseAction() == UseAction.BLOCK
                           || item.isOf(net.minecraft.item.Items.WARPED_FUNGUS_ON_A_STICK)
                           || item.isOf(net.minecraft.item.Items.CARROT_ON_A_STICK)
                           || item.getItem() instanceof net.minecraft.item.FishingRodItem
                           || item.isOf(net.minecraft.item.Items.SHEARS)) {
                         if (item.isIn(net.minecraft.registry.tag.ItemTags.SWORDS)) {
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F * swing));
                             matrices.translate(0.0, 0.1 * swing, -0.1 * swing);
                         }

                         if (item.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) {
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F * swing_rot));
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                         } else if (item.getUseAction() == UseAction.SPEAR) {
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F * swing_rot));
                             matrices.translate(0.0, 0.1 * swing_rot, -0.1 * swing_rot);
                         } else if (item.getUseAction() != UseAction.BLOCK) {
                             matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * swing));
                             matrices.translate(0.0, 0.05 * swing, -0.05 * swing);
                         }
                     }

                     if (item.isOf(net.minecraft.item.Items.NETHER_STAR) || item.isOf(net.minecraft.item.Items.END_CRYSTAL)) {
                         this.netherCounter = (float)((double)this.netherCounter + 0.9 * tt);
                         matrices.translate(0.0F, (float)(0.25F + 0.02 * (double)MathHelper.sin(this.netherCounter * 0.1F)), 0.0F);
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(3.0F * MathHelper.sin(this.netherCounter * 0.2F)));
                         matrices.scale(1.0F + 0.01F * MathHelper.sin(this.netherCounter), 1.0F + 0.01F * MathHelper.sin(this.netherCounter), 1.0F + 0.01F * MathHelper.sin(this.netherCounter));
                     } else {
                         this.netherCounter = 0.0F;
                     }

                     if (item.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) {
                         matrices.translate(0.07 * (double)l, 0.0, 0.05);
                         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * l)));
                         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0F));
                     }

                     this.renderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
                 }
             }

             matrices.pop();
             matrices.pop();
             this.isAttacking = this.client.options.attackKey.isPressed();
          }
       }
    }

    @Redirect(
       method = "swingArm",
       at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"
       )
    )
    private void applySwing(HeldItemRenderer instance, MatrixStack matrices, Arm arm, float swingProgress) {
        HoldMyItems hmi = farvix.solution.Client.getInstance().getModuleManager().get(HoldMyItems.class);
        if (hmi == null || !hmi.isEnabled()) {
            this.applySwingOffset(matrices, arm, swingProgress);
        }
    }

    @Inject(method = "updateHeldItems", at = @At("HEAD"))
    private void onUpdateHeldItems(CallbackInfo ci) {
        HoldMyItems holdMyItems = Client.getInstance().getModuleManager().get(HoldMyItems.class);
        if (holdMyItems != null && holdMyItems.isEnabled()) {
            ClientPlayerEntity player = this.client.player;
            if (player != null) {
                ItemStack currentMain = player.getMainHandStack();
                ItemStack currentOff = player.getOffHandStack();

                boolean bypassMain = false;
                if (!ItemStack.areEqual(this.mainHand, currentMain)) {
                    if (this.mainHand != null && currentMain != null && this.mainHand.getItem() == currentMain.getItem()) {
                        bypassMain = true;
                    }
                }

                if (bypassMain) {
                    this.mainHand = currentMain;
                    this.equipProgressMainHand = 1.0f;
                    this.prevEquipProgressMainHand = 1.0f;
                }

                boolean bypassOff = false;
                if (!ItemStack.areEqual(this.offHand, currentOff)) {
                    if (this.offHand != null && currentOff != null && this.offHand.getItem() == currentOff.getItem()) {
                        bypassOff = true;
                    }
                }

                if (bypassOff) {
                    this.offHand = currentOff;
                    this.equipProgressOffHand = 1.0f;
                    this.prevEquipProgressOffHand = 1.0f;
                }
            }
        }
    }

    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
                    ordinal = 2
            )
    )
    public void redirectSwingArmForCustomAnim(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        SwingAnimation swingAnimation = Client.getInstance().getModuleManager().get(SwingAnimation.class);
        if (swingAnimation != null && swingAnimation.isEnabled() && swingAnimation.shouldApplyToArm(arm)) {
            swingAnimation.renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
        } else {
            this.swingArm(swingProgress, equipProgress, matrices, armX, arm);
        }
    }
}
