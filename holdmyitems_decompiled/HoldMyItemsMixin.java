package com.holdmyitems.mixin;

import com.holdmyitems.HoldMyItems;
import com.holdmyitems.config.ModConfig;
import com.holdmyitems.mixin.HoldMyItemsMixin.1;
import java.util.Random;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.class_1087;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_1747;
import net.minecraft.class_1764;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1803;
import net.minecraft.class_1828;
import net.minecraft.class_1839;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2286;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_2459;
import net.minecraft.class_2462;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3481;
import net.minecraft.class_3489;
import net.minecraft.class_3532;
import net.minecraft.class_3922;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_5134;
import net.minecraft.class_742;
import net.minecraft.class_759;
import net.minecraft.class_776;
import net.minecraft.class_7833;
import net.minecraft.class_811;
import net.minecraft.class_9285;
import net.minecraft.class_9334;
import net.minecraft.class_9285.class_9287;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(class_759.class)
public abstract class HoldMyItemsMixin {
   private boolean repPower = false;
   private float prevAge = 0.0F;
   private double previousRotation = 0.0;
   private float swingAngleY = 0.0F;
   private float swingAngleX = 0.0F;
   private float swingVelocityY = 0.0F;
   private float swingVelocityX = 0.0F;
   private float swingVelocityZ = 0.0F;
   private static final float GRAVITY = 0.1F;
   private static final float DAMPING = 0.88F;
   private static final float SENSITIVITY = 0.015F;
   private float vertAngleY = 0.0F;
   private float vertVelocityY = 0.0F;
   private float vertVelocityYSlime = 0.0F;
   private float vertAngleYSlime = 0.0F;
   private float riptideCounter = 0.0F;
   private float netherCounter = 0.0F;
   @Shadow
   private class_1799 field_4048;
   @Shadow
   @Final
   private class_310 field_4050;
   private float fallCounter = 0.0F;
   private float inWaterCounter = 0.0F;
   private float inspect = 0.0F;
   private float tilt = 0.0F;
   private float freezeCounter = 0.0F;
   private float clCount = 0.0F;
   private float crawlCount = 0.0F;
   private float directionalCrawlCount = 0.0F;
   private float climbCount = 0.0F;
   private float mouseHolding = 1.0F;
   private boolean isSwinging = false;
   private float swingProgress = 0.0F;
   private boolean isForward = false;
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

   private float getAttackDamage(class_1799 stack) {
      class_9285 modifiers = (class_9285)stack.method_57353().method_57829(class_9334.field_49636);
      if (modifiers == null) {
         return 0.0F;
      } else {
         float totalDamage = 0.0F;

         for (class_9287 entry : modifiers.comp_2393()) {
            if (entry.comp_2395().comp_349() == class_5134.field_23721.comp_349()) {
               totalDamage += (float)entry.comp_2396().comp_2449();
            }
         }

         return totalDamage;
      }
   }

   private void altSwing(class_4587 matrices, class_1306 arm, float swingProgress, class_1799 item) {
      class_310 client = class_310.method_1551();
      int i = arm == class_1306.field_6183 ? 1 : -1;
      float f = class_3532.method_15374(swingProgress * 3.14F);
      matrices.method_22907(class_7833.field_40716.rotationDegrees(i * (45.0F + f * 0.0F)));
      matrices.method_22907(class_7833.field_40716.rotationDegrees(i * -45.0F));
   }

   @Shadow
   public abstract void method_3233(class_1309 var1, class_1799 var2, class_811 var3, boolean var4, class_4587 var5, class_4597 var6, int var7);

   @Shadow
   protected abstract void method_65816(float var1, float var2, class_4587 var3, int var4, class_1306 var5);

   @Shadow
   protected abstract void method_3224(class_4587 var1, class_1306 var2, float var3);

   @Shadow
   protected abstract void method_3231(class_4587 var1, class_4597 var2, int var3, float var4, float var5, float var6);

   @Shadow
   protected abstract void method_3222(class_4587 var1, class_4597 var2, int var3, float var4, class_1306 var5, float var6, class_1799 var7);

   @Shadow
   protected abstract void method_3219(class_4587 var1, class_4597 var2, int var3, float var4, float var5, class_1306 var6);

   @Shadow
   protected abstract void method_3217(class_4587 var1, class_1306 var2, float var3);

   // $VF: Unable to simplify switch on enum
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   @Redirect(
      method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
      )
   )
   private void renderOverhaul(
      class_759 instance,
      class_742 player,
      float tickDelta,
      float pitch,
      class_1268 hand,
      float swingProgress,
      class_1799 item,
      float equipProgress,
      class_4587 matrices,
      class_4597 vertexConsumers,
      int light
   ) {
      if (!player.method_31550()) {
         ModConfig config = (ModConfig)AutoConfig.getConfigHolder(ModConfig.class).getConfig();
         float yaw = player.method_36454();
         double radians = Math.toRadians(yaw);
         double forwardX = -Math.sin(radians);
         double forwardZ = Math.cos(radians);
         class_243 horizontalVelocity = player.method_18798();
         double dotProduct = horizontalVelocity.field_1352 * forwardX + horizontalVelocity.field_1350 * forwardZ;
         double crossProduct = player.method_18798().method_61890().field_1352 * forwardZ - horizontalVelocity.field_1350 * forwardX;
         float al;
         if (player.method_36455() != 0.0F) {
            al = 90.0F / player.method_36455() / 10.0F;
         } else {
            al = 1.0F;
         }

         if (al > 1.0F) {
            al = 1.0F;
         }

         if (al < 0.0F) {
            al = 1.0F;
         }

         boolean bl = hand == class_1268.field_5808;
         class_1306 arm = bl ? player.method_6068() : player.method_6068().method_5928();
         float kj = bl ? 1.0F : -1.0F;
         matrices.method_22903();
         matrices.method_22903();
         matrices.method_46416(config.viewmodelXOffset * kj, config.viewmodelYOffset, config.viewmodelZOffset);
         double tt = HoldMyItems.deltaTime * 30.0;
         float swing_rot = swingProgress < 0.6
            ? class_3532.method_15374(class_3532.method_15363(swingProgress, 0.0F, 0.12506F) * 12.56F)
            : class_3532.method_15374(class_3532.method_15363(swingProgress, 0.62532F, 0.75038F) * 12.56F);
         float swing = class_3532.method_15374(swingProgress * 3.14F);
         swing = this.easeInOutBack(swing);
         if ((
               item.method_31574(class_1802.field_8287)
                  || item.method_31574(class_1802.field_49098)
                  || item.method_31574(class_1802.field_8803)
                  || item.method_31574(class_1802.field_8449)
                  || item.method_31574(class_1802.field_8543)
                  || item.method_31574(class_1802.field_8634)
                  || item.method_7909() instanceof class_1828
                  || item.method_7909() instanceof class_1803
            )
            && player.method_6079().method_7960()
            && item.method_7976() != class_1839.field_8951
            && !item.method_31574(class_1802.field_8814)
            && !player.method_5681()
            && !player.method_20448()
            && !player.method_6101()) {
            if (player.method_6068() == class_1306.field_6182) {
               bl = !bl;
            }

            float jj = bl ? 1.0F : -1.0F;
            matrices.method_22903();
            matrices.method_22907(class_7833.field_40716.rotationDegrees(-25.0F * jj));
            matrices.method_22907(class_7833.field_40714.rotationDegrees(-10.0F));
            matrices.method_22907(class_7833.field_40716.rotationDegrees(25.0F * jj * swing));
            matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F * swing));
            matrices.method_22904(-0.15 * jj, 0.1, 0.1);
            matrices.method_22904(0.0, -0.55 * swing, 0.4 * swing * 3.14F);
            this.method_3219(matrices, vertexConsumers, light, equipProgress, 0.0F, arm.method_5928());
            matrices.method_22909();
         }

         if (this.field_4050.field_1690.field_1886.method_1434() && !this.isAttacking && swingProgress == 0.0) {
            this.left = !this.left;
         }

         if (!item.method_7960()) {
            if (player.method_6068() == class_1306.field_6182) {
               bl = !bl;
            }

            float ll = bl ? 1.0F : -1.0F;
            if ((
                  this.left
                     || item.method_31573(class_3489.field_42612)
                     || item.method_7976() == class_1839.field_8951
                     || item.method_7976() == class_1839.field_8949
               )
               && !item.method_31573(class_3489.field_42615)) {
               if (item.method_31573(class_3489.field_42611) || item.method_31573(class_3489.field_42612)) {
                  matrices.method_22904(0.8 * ll * swing_rot, 0.3 * swing_rot, -0.5 * swing);
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(15.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(-20.0F * swing_rot));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(-70.0F * swing_rot * ll));
                  if (item.method_31573(class_3489.field_42611)) {
                     matrices.method_22907(class_7833.field_40713.rotationDegrees(40.0F * swing));
                  } else {
                     matrices.method_22907(class_7833.field_40713.rotationDegrees(30.0F * swing));
                  }
               } else if (item.method_7976() == class_1839.field_8951) {
                  matrices.method_22904(0.0, 0.0, 0.45 * swing_rot);
                  matrices.method_22904(-0.25 * kj * swing, -0.35 * swing_rot, -0.6 * swing);
                  matrices.method_22904(0.0, 0.1 * swing, 0.0);
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(15.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(30.0F * swing_rot * ll));
               } else if (item.method_31573(ConventionalItemTags.TOOLS)
                  && item.method_7976() != class_1839.field_8949
                  && !item.method_31573(class_3489.field_42615)) {
                  matrices.method_22904(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(-30.0F * swing_rot));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(-20.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(40.0F * swing));
               } else if (item.method_7976() != class_1839.field_8949) {
                  matrices.method_22904(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(-30.0F * swing_rot));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(-10.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(40.0F * swing));
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(10.0F * swing * ll));
               } else {
                  matrices.method_22904(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.2 * swing);
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(-10.0F * swing_rot));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(-10.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(20.0F * swing));
               }
            } else if (!item.method_31573(class_3489.field_42615)) {
               if (item.method_31573(class_3489.field_42611)) {
                  matrices.method_22904(-0.55 * ll * swing_rot, -0.8 * swing_rot, -0.77 * swing);
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(5.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(-30.0F * swing_rot));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(70.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(50.0F * swing));
               } else if (item.method_31573(ConventionalItemTags.TOOLS) && !item.method_31573(class_3489.field_42615)) {
                  matrices.method_22904(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(-30.0F * swing_rot));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(-20.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(40.0F * swing));
               } else {
                  matrices.method_22904(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(-30.0F * swing_rot));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(-10.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40713.rotationDegrees(40.0F * swing));
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(10.0F * swing * ll));
               }
            } else if (item.method_31573(class_3489.field_42615)) {
               matrices.method_22904(0.0, 0.15 * swing_rot, -0.25 * swing_rot);
               matrices.method_22904(0.0, 0.0, -0.2 * swing);
               matrices.method_22907(class_7833.field_40716.rotationDegrees(15.0F * swing_rot));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(-35.0F * swing_rot));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F * swing));
            }
         } else if (class_2248.method_9503(item.method_7909()) != class_2246.field_10124
            && (
               !item.method_31573(ConventionalItemTags.TOOLS)
                  || item.method_31573(class_3489.field_41890)
                  || item.method_31573(class_3489.field_40109)
                  || item.method_7976() == class_1839.field_8950
                  || !item.method_7923()
            )
            && item.method_7976() != class_1839.field_8953
            && item.method_7976() != class_1839.field_27079
            && this.getAttackDamage(item) == 0.0F
            && item.method_7976() != class_1839.field_8949
            && !item.method_31574(class_1802.field_23254)
            && !item.method_31574(class_1802.field_8184)
            && !item.method_31574(class_1802.field_8378)
            && !item.method_31574(class_1802.field_8868)) {
            swingProgress = (float)(swingProgress * 1.2);
            if (swingProgress > 1.0F) {
               swingProgress = 0.0F;
            }
         } else if (!item.method_31573(class_3489.field_42615)) {
            swingProgress = (float)(swingProgress * 1.5);
            if (swingProgress > 1.0F) {
               swingProgress = 0.0F;
            }
         }

         if (player.method_18798().method_1033() >= 0.08) {
            this.crawlCount = (float)(this.crawlCount + 0.1 * player.method_18798().method_1033() * 2.0 * tt);
            this.directionalCrawlCount = (float)(this.directionalCrawlCount + 0.1 * dotProduct * 4.0 * tt);
            this.directionalCrawlCount = (float)(
               this.directionalCrawlCount + (dotProduct > 0.0 ? 0.1 * Math.abs(crossProduct) * 4.0 * tt : 0.1 * Math.abs(crossProduct) * -1.0 * 4.0 * tt)
            );
         }

         if (player.method_18798().method_10214() > 0.0) {
            this.climbCount = (float)(this.climbCount + 0.1 * tt);
         }

         if (player.method_18798().method_10214() < 0.0) {
            this.climbCount = (float)(this.climbCount - 0.1 * tt);
         }

         if ((
               player.method_20448() && config.climbAndCrawl
                  || player.method_6101() && !player.method_24828() && Math.abs(player.method_18798().method_10214()) > 0.0 && config.climbAndCrawl
            )
            && !player.method_6115()
            && swingProgress == 0.0F) {
            this.clCount = (float)(this.clCount + 0.1 * tt);
            if (this.clCount > 1.0F) {
               this.clCount = 1.0F;
            }

            if (!item.method_31574(class_1802.field_16539) && !item.method_31574(class_1802.field_22016)) {
               matrices.method_22907(class_7833.field_40714.rotationDegrees(-20.0F * this.clCount));
            }
         } else {
            this.clCount = (float)(this.clCount * Math.pow(0.88F, tt));
         }

         if (swingProgress == 0.0F) {
            matrices.method_46416(bl ? player.method_36455() / 650.0F * this.clCount * -1.0F : player.method_36455() / 650.0F * this.clCount, 0.0F, 0.0F);
            matrices.method_22907(class_7833.field_40714.rotationDegrees(player.method_36455() * this.clCount));
         }

         if (!item.method_31574(class_1802.field_16539) && !item.method_31574(class_1802.field_22016)) {
            matrices.method_46416(0.0F, 0.0F, player.method_36455() / 120.0F * this.clCount);
         } else if (swingProgress == 0.0F) {
            matrices.method_46416(0.0F, 0.0F, player.method_36455() / 80.0F * this.clCount);
         }

         if (player.method_6101()
            && config.climbAndCrawl
            && !player.method_24828()
            && !item.method_31574(class_1802.field_16539)
            && !item.method_31574(class_1802.field_22016)
            && !player.method_6115()) {
            matrices.method_22904(0.0, 0.1, -0.2);
         }

         if ((player.method_52535() || player.field_27857) && !player.method_5681() && !player.method_5869()) {
            this.inWaterCounter = (float)(this.inWaterCounter + 0.1 * tt);
            if (this.inWaterCounter >= 1.0F) {
               this.inWaterCounter = 1.0F;
            }
         } else {
            this.inWaterCounter = (float)(this.inWaterCounter * Math.pow(0.88F, tt));
         }

         if (player.field_27857 && player.method_32313() > 0.1) {
            this.freezeCounter = (float)(this.freezeCounter + 0.1 * tt);
         } else {
            this.freezeCounter = (float)(this.freezeCounter * Math.pow(0.88F, tt));
         }

         matrices.method_22904(0.0, 0.02 * this.inWaterCounter, 0.0);
         matrices.method_22907(class_7833.field_40718.rotationDegrees(8.0F * kj * this.inWaterCounter));
         matrices.method_22907(class_7833.field_40714.rotationDegrees(0.3F * class_3532.method_15374(this.freezeCounter * 5.0F)));
         if (player.method_18798().method_10214() < -0.85 && item.method_31574(class_1802.field_49814) && player.method_6047() == item) {
            this.fallCounter = (float)(this.fallCounter + 0.1 * tt);
            if (this.fallCounter >= 1.0F) {
               this.fallCounter = 1.0F;
            }
         } else {
            this.fallCounter = (float)(this.fallCounter * Math.pow(0.88F, tt));
         }

         if (bl) {
            matrices.method_22907(class_7833.field_40714.rotationDegrees(45.0F * this.fallCounter));
            matrices.method_22904(0.0, -0.2 * this.fallCounter, 0.0);
         }

         this.vertAngleY = (float)(this.vertAngleY + player.method_18798().method_10214() * 0.015F * tt);
         this.vertAngleY = (float)(this.vertAngleY - 0.1F * this.vertAngleY * tt);
         this.vertAngleY = (float)(this.vertAngleY * Math.pow(0.88F, tt));
         this.vertVelocityYSlime = (float)(this.vertVelocityYSlime + player.method_18798().method_10214() * 0.015F * tt);
         this.vertVelocityYSlime = (float)(this.vertVelocityYSlime - 0.1F * this.vertAngleYSlime * tt);
         this.vertVelocityYSlime = (float)(this.vertVelocityYSlime * Math.pow(0.88F, tt));
         this.vertAngleYSlime = (float)(this.vertAngleYSlime + this.vertVelocityYSlime * tt);
         matrices.method_46416(0.0F, this.vertAngleY * -1.0F, 0.0F);
         matrices.method_22904(0.0, Math.sin(player.field_6012 * 0.1) * 0.007 * kj, 0.0);
         matrices.method_22907(class_7833.field_40716.rotationDegrees(0.15F * class_3532.method_15374(player.field_6012 * 0.15F) * kj));
         if (!item.method_7960() || player.method_20448() || player.method_6101() && !player.method_24828() || player.method_5681()) {
            if (player.method_6068() == class_1306.field_6182) {
               bl = !bl;
            }

            if (item.method_7976() == class_1839.field_8949) {
               matrices.method_46416(0.0F, 0.0F, 0.0F);
            } else {
               matrices.method_22904(0.0, -0.1, 0.1);
            }
         }

         if (item.method_31574(class_1802.field_16539) || item.method_31574(class_1802.field_22016) || item.method_31573(class_3489.field_40108)) {
            matrices.method_22904(0.0, 0.1, 0.0);
            if (player.method_5681()) {
               matrices.method_22904(0.0, -0.1, 0.1);
            }
         }

         if (player.method_5681() && swingProgress == 0.0F && config.swimmingAnimation) {
            double distance = this.crawlCount;
            double swingAmplitude = 1.5;
            double frequency = 2.0;
            double s = distance * frequency;
            double handRotation = Math.sin(s) * swingAmplitude;
            double smoothRotation = handRotation * 0.8 + this.previousRotation * 0.2;
            matrices.method_22907(class_7833.field_40716.rotationDegrees((float)(bl ? smoothRotation : -smoothRotation)));
            matrices.method_22904(0.0, 0.0, smoothRotation * 0.2F);
            double k = this.crawlCount * 2.0F;
            double a = Math.cos(k);
            double b = a;
            if (a <= 0.0) {
               b = a * 0.5;
            }

            matrices.method_22907(class_7833.field_40715.rotationDegrees((float)(bl ? b * 30.0 : b * 30.0 * -1.0)));
            matrices.method_22904(0.0, 0.0, a * 0.2F);
            if (item.method_7960() && !bl && !player.method_5767()) {
               float l = bl ? 1.0F : -1.0F;
               matrices.method_22904(1.0F * l, 0.0 - equipProgress * 0.3, 0.3);
               matrices.method_22907(class_7833.field_40716.rotationDegrees(45.0F * l));
               matrices.method_22907(class_7833.field_40718.rotationDegrees(-40.0F * l));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F));
               this.altSwing(matrices, arm, swingProgress, item);
               float c = class_3532.method_15374(equipProgress * 3.14F);
               matrices.method_22905(0.9F, 0.9F, 0.9F);
               this.method_3219(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            }

            this.previousRotation = smoothRotation;
         }

         if ((player.method_6101() && !player.method_24828() || player.method_20448() && swingProgress == 0.0F) && !player.method_6115()) {
            double sx = this.climbCount;
            float v = (float)player.method_18798().method_10214();
            float ax = class_3532.method_15362((float)sx * 2.0F);
            float left = bl ? 1.0F : -1.0F;
            if (player.method_6101()) {
               if (!item.method_31574(class_1802.field_16539) && !item.method_31574(class_1802.field_22016)) {
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(20.0F * ax * left));
               } else {
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(1.0F * ax * left));
               }
            }

            if (player.method_20448() && !player.method_6115() && swingProgress == 0.0F) {
               float crawlProgress = class_3532.method_15374(this.directionalCrawlCount * 4.0F * this.mouseHolding);
               float upAndDown = class_3532.method_15362(this.directionalCrawlCount * 4.0F * this.mouseHolding);
               if (item.method_31574(class_1802.field_16539) || item.method_31574(class_1802.field_22016)) {
                  crawlProgress *= 0.14F;
                  upAndDown *= 0.14F;
               }

               matrices.method_22904(0.2 * crawlProgress, 0.3 * crawlProgress * left, -0.2 * crawlProgress * left * al);
               matrices.method_22907(class_7833.field_40716.rotationDegrees(25.0F * crawlProgress));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(class_3532.method_15363(20.0F * upAndDown * left, 0.0F, 20.0F)));
            }

            if (item.method_7960() && !bl && !player.method_5767() && (!player.method_24828() && player.method_6101() || player.method_20448())) {
               float l = bl ? 1.0F : -1.0F;
               matrices.method_22904(1.0F * l, 0.0 - equipProgress * 0.3, 0.3);
               matrices.method_22907(class_7833.field_40716.rotationDegrees(45.0F * l));
               matrices.method_22907(class_7833.field_40718.rotationDegrees(-40.0F * l));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F));
               this.altSwing(matrices, arm, swingProgress, item);
               matrices.method_22905(0.9F, 0.9F, 0.9F);
               this.method_3219(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            }
         }

         if (item.method_7960()) {
            if (bl && !player.method_5767()) {
               if ((player.method_24828() || !player.method_6101()) && !player.method_5681() && !player.method_20448()) {
                  if (player.method_6068() == class_1306.field_6182) {
                     bl = !bl;
                  }

                  float ll = bl ? 1.0F : -1.0F;
                  matrices.method_22904(0.0, 0.2 * swing_rot, 0.15 * swing_rot);
                  matrices.method_22904(0.1 * ll * swing, 0.15 * swing, -0.45 * swing);
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(35.0F * swing * ll));
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(-30.0F * swing));
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(-10.0F * swing_rot * ll));
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(10.0F * swing_rot));
                  this.method_3219(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
               } else {
                  float l = bl ? 1.0F : -1.0F;
                  matrices.method_22904(1.0F * l, 0.0 - equipProgress * 0.3, 0.3);
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(45.0F * l));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(-40.0F * l));
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F));
                  this.altSwing(matrices, arm, swingProgress, item);
                  float c = class_3532.method_15374(equipProgress * 3.14F);
                  matrices.method_22905(0.9F, 0.9F, 0.9F);
                  this.method_3219(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
               }
            }
         } else if (item.method_57826(class_9334.field_49646)) {
            if (bl && this.field_4048.method_7960()) {
               matrices.method_22904(0.0, 0.1, 0.0);
               this.method_3231(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
            } else {
               matrices.method_22904(bl ? -0.1 : 0.1, 0.1, 0.0);
               this.method_3222(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, item);
            }
         } else if (item.method_7976() == class_1839.field_8947) {
            matrices.method_22903();
            boolean bl2 = class_1764.method_7781(item);
            boolean bl3 = arm == class_1306.field_6183;
            int i = bl3 ? 1 : -1;
            if (player.method_6115() && player.method_6014() > 0 && player.method_6058() == hand) {
               this.method_3224(matrices, arm, equipProgress);
               matrices.method_46416(i * -0.4785682F, -0.24387F, 0.05731531F);
               matrices.method_22907(class_7833.field_40714.rotationDegrees(-11.935F));
               matrices.method_22907(class_7833.field_40716.rotationDegrees(i * 65.3F));
               matrices.method_22907(class_7833.field_40718.rotationDegrees(i * 9.785F));
               float f = item.method_7935(player) - (player.method_6014() - tickDelta + 1.0F);
               float g = f / class_1764.method_7775(item, player);
               if (g > 1.0F) {
                  g = 1.0F;
               }

               if (g > 0.1F) {
                  float h = class_3532.method_15374((f - 0.1F) * 1.3F);
                  float j = g - 0.1F;
                  float kx = h * j;
                  matrices.method_46416(kx * 0.0F, kx * 0.004F, kx * 0.0F);
               }

               matrices.method_46416(g * 0.0F, g * 0.0F, g * 0.04F);
               matrices.method_22905(1.0F, 1.0F, 1.0F);
               matrices.method_22907(class_7833.field_40715.rotationDegrees(i * 45.0F));
            } else {
               this.method_65816(swingProgress, equipProgress, matrices, i, arm);
               if (bl2 && swingProgress < 0.001F && bl) {
                  matrices.method_46416(i * -0.341864F, 0.0F, 0.0F);
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(i * 10.0F));
               }
            }

            float l = bl ? 1.0F : -1.0F;
            matrices.method_46416(0.0F, 0.0F, -1.0F);
            matrices.method_22904(-0.45 * i, 0.45, 1.7);
            matrices.method_22904(1.0F * l, 0.0 - equipProgress * 0.3, 0.3);
            matrices.method_22907(class_7833.field_40716.rotationDegrees(45.0F * l));
            matrices.method_22907(class_7833.field_40718.rotationDegrees(-40.0F * l));
            matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F));
            this.altSwing(matrices, arm, swingProgress, item);
            float c = class_3532.method_15374(equipProgress * 3.14F);
            matrices.method_22905(0.9F, 0.9F, 0.9F);
            this.method_3219(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            matrices.method_22904(-0.25 * i, 1.25, 0.05);
            matrices.method_22907(class_7833.field_40716.rotationDegrees(-90 * i));
            matrices.method_22907(class_7833.field_40714.rotationDegrees(77.0F));
            matrices.method_22907(class_7833.field_40718.rotationDegrees(85 * i));
            matrices.method_22905(1.2F, 1.2F, 1.2F);
            matrices.method_22907(class_7833.field_40714.rotationDegrees(-10.0F));
            matrices.method_22904(0.0, -0.15, 0.15);
            this.method_3233(player, item, bl3 ? class_811.field_4322 : class_811.field_4321, !bl3, matrices, vertexConsumers, light);
            matrices.method_22909();
            if (player.method_6115() && player.method_6014() > 0 && player.method_6058() == hand) {
               float fx = item.method_7935(player) - (player.method_6014() - tickDelta + 1.0F);
               float gx = fx / class_1764.method_7775(item, player);
               if (gx > 1.0F) {
                  gx = 1.0F;
               }

               if (gx > 0.1F) {
                  float h = class_3532.method_15374((fx - 0.1F) * 1.3F);
                  float j = gx - 0.1F;
                  float kx = h * j;
                  matrices.method_46416(kx * 0.0F, kx * 0.004F, kx * 0.0F);
               }

               matrices.method_22907(class_7833.field_40715.rotationDegrees(gx <= 0.2 ? 75.0F * (gx * 5.0F) * i : 75 * i));
               matrices.method_22907(class_7833.field_40713.rotationDegrees(10.0F * gx * 1.5F));
               matrices.method_22904(-0.37 * i, 0.0, 0.6);
               matrices.method_22904(0.15 * gx * i, 0.0, 0.0);
               this.method_3219(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.method_5928());
            }
         } else {
            boolean bl2x = arm == class_1306.field_6183;
            int l = bl2x ? 1 : -1;
            if (player.method_6115() && player.method_6014() > 0 && player.method_6058() == hand) {
               switch (1.$SwitchMap$net$minecraft$item$consume$UseAction[item.method_7976().ordinal()]) {
                  case 1:
                     this.method_3224(matrices, arm, equipProgress);
                     break;
                  case 2:
                  case 3:
                     float u = item.method_7935(player) - (player.method_6014() - tickDelta + 1.0F);
                     float y = u / 5.0F;
                     if (y > 1.0F) {
                        y = 1.0F;
                     }

                     float q = class_3532.method_15374(u / 2.0F * 3.14F);
                     q /= 10.0F;
                     matrices.method_22904(1 * l, 0.1, 0.3);
                     matrices.method_22904(0.2 * l * y, -0.7 * y, -0.2 * y);
                     matrices.method_22904(0.0, -0.2 * q, -0.2 * q);
                     matrices.method_22904(0.0, 0.1 * this.easeInOutBack(class_3532.method_15374(y * 3.14F)), 0.0);
                     matrices.method_22907(class_7833.field_40716.rotationDegrees(45 * l));
                     matrices.method_22907(class_7833.field_40718.rotationDegrees(-40 * l));
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F));
                     this.altSwing(matrices, arm, swingProgress, item);
                     float c = class_3532.method_15374(equipProgress * 3.14F);
                     matrices.method_22905(0.9F, 0.9F, 0.9F);
                     matrices.method_22907(class_7833.field_40716.rotationDegrees(45.0F * y * l));
                     this.method_3219(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                     break;
                  case 4:
                     float kx = item.method_7935(player) - (player.method_6014() - tickDelta + 1.0F);
                     float sxx = kx / 4.0F;
                     float s2 = kx / 6.0F;
                     if (sxx > 1.0F) {
                        sxx = 1.0F;
                     }

                     if (s2 > 1.0F) {
                        s2 = 1.0F;
                     }

                     matrices.method_22904(0.0, -0.2, 0.0);
                     matrices.method_22904(1 * l, 0.0, 0.3);
                     matrices.method_22904(0.7 * sxx * l, 0.0, -1.3 * sxx);
                     matrices.method_22904(-0.2 * l * s2, 0.0, 0.0);
                     matrices.method_22907(class_7833.field_40714.rotationDegrees((float)(10.0 * Math.sin(s2 * 3.14))));
                     matrices.method_22907(class_7833.field_40716.rotationDegrees(70.0F * sxx * l));
                     matrices.method_22907(class_7833.field_40716.rotationDegrees(45 * l));
                     matrices.method_22907(class_7833.field_40718.rotationDegrees(-40 * l));
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F));
                     matrices.method_22907(class_7833.field_40716.rotationDegrees(5 * l * sxx));
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(-10.0F * sxx));
                     matrices.method_22904(0.0, 0.0, -0.2 * sxx);
                     this.altSwing(matrices, arm, swingProgress, item);
                     matrices.method_22905(0.9F, 0.9F, 0.9F);
                     this.method_3219(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                     matrices.method_22904(0.35 * l, -0.13, -0.12);
                     matrices.method_22907(class_7833.field_40718.rotationDegrees(10.0F * l));
                     matrices.method_22907(class_7833.field_40716.rotationDegrees(10.0F * l));
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(0.0F));
                     matrices.method_22904(-0.2 * l, -0.04, 0.15);
                     matrices.method_22905(1.0F, 1.0F, 1.0F);
                     break;
                  case 5:
                     matrices.method_22903();
                     if (player.method_6068() == class_1306.field_6182) {
                        bl = !bl;
                     }

                     float m1 = item.method_7935(player) - (player.method_6014() - tickDelta + 1.0F);
                     float f1 = m1 / 20.0F;
                     float fxxx = (f1 * f1 + f1 * 2.0F) / 3.0F;
                     if (f1 > 1.0F) {
                        f1 = 1.0F;
                     }

                     if (f1 > 0.1F) {
                        float g1 = class_3532.method_15374((m1 - 0.1F) * 1.3F);
                        float j1 = g1 * f1;
                        matrices.method_46416(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                     }

                     matrices.method_22904(bl ? -0.1 : 0.1, 0.0, f1 * 0.15);
                     this.method_3219(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                     matrices.method_22909();
                     matrices.method_22904(bl ? -0.5 : 0.5, -0.45, 0.1);
                     matrices.method_22907(class_7833.field_40714.rotation(0.3F));
                     if (bl) {
                        matrices.method_22907(class_7833.field_40717.rotation(-0.3F));
                        matrices.method_22907(class_7833.field_40715.rotation(1.0F));
                     } else {
                        matrices.method_22907(class_7833.field_40718.rotation(-0.3F));
                        matrices.method_22907(class_7833.field_40716.rotation(1.0F));
                     }

                     this.method_3219(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.method_5928());
                     if (bl) {
                        matrices.method_22907(class_7833.field_40715.rotation(2.5F));
                     } else {
                        matrices.method_22907(class_7833.field_40716.rotation(2.5F));
                     }

                     matrices.method_22904(bl ? -0.65 : 0.65, -0.35, 0.27);
                     if (f1 > 1.0F) {
                        f1 = 1.0F;
                     }

                     matrices.method_22909();
                     if (config.mb3DCompat) {
                        matrices.method_22907(class_7833.field_40716.rotationDegrees(10 * l));
                     }

                     matrices.method_22907(class_7833.field_40713.rotationDegrees(75.0F));
                     matrices.method_22907(class_7833.field_40717.rotationDegrees(-15 * l));
                     matrices.method_22904(0.8 * l, 0.0F - equipProgress * 0.3F, -0.1);
                     if (fxxx > 0.1F) {
                        float g1 = class_3532.method_15374((m1 - 0.1F) * 1.3F);
                        float h1 = f1 - 0.1F;
                        float j1 = g1 * h1;
                        matrices.method_46416(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                     }

                     matrices.method_22903();
                     break;
                  case 6:
                     if (player.method_6079().method_7960() && !player.method_20448() && !player.method_5681() && !player.method_6101()) {
                        matrices.method_22903();
                        matrices.method_22907(class_7833.field_40716.rotationDegrees(-25 * l));
                        matrices.method_22904(-0.15 * l, 0.1, 0.1);
                        this.method_3219(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.method_5928());
                        matrices.method_22909();
                     }

                     float m = item.method_7935(player) - (player.method_6014() - tickDelta + 1.0F);
                     float fxx = m / 10.0F;
                     if (fxx > 1.0F) {
                        fxx = 1.0F;
                     }

                     if (fxx > 0.1F) {
                        float gxx = class_3532.method_15374((m - 0.1F) * 1.3F);
                        float h = fxx - 0.1F;
                        float j = gxx * h;
                        matrices.method_46416(j * 0.0F, j * 0.004F, j * 0.0F);
                     }

                     matrices.method_22907(class_7833.field_40714.rotationDegrees(45.0F));
                     matrices.method_22907(class_7833.field_40716.rotationDegrees(25 * l));
                     matrices.method_22904(0.2 * l, 0.0, 0.8);
                     this.method_3219(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(135.0F));
                     matrices.method_22907(class_7833.field_40718.rotationDegrees(-65 * l));
                     matrices.method_22904(0.65F * l, -1.0, -0.6);
                     break;
                  case 7:
                     float f5 = player.method_6014() % 10;
                     float g5 = f5 - tickDelta + 1.0F;
                     float h5 = 1.0F - g5 / 10.0F;
                     float n = -15.0F + 75.0F * class_3532.method_15362(h5 * 2.0F * (float) Math.PI);
                     float z = item.method_7935(player) - (player.method_6014() - tickDelta + 1.0F);
                     float x = z / 4.0F;
                     if (x > 1.0F) {
                        x = 1.0F;
                     }

                     matrices.method_22907(class_7833.field_40716.rotationDegrees(25 * l * x));
                     matrices.method_22904(0.3F * l * x, 0.3 * x, 0.1 * x);
                     if (x == 1.0F) {
                        matrices.method_22907(class_7833.field_40716.rotationDegrees(n / 20.0F));
                     }

                     this.method_3219(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                     break;
                  case 8:
                     matrices.method_22904(1 * l, 0.0 - equipProgress * 0.3, 0.3);
                     matrices.method_22907(class_7833.field_40716.rotationDegrees(45 * l));
                     matrices.method_22907(class_7833.field_40718.rotationDegrees(-40 * l));
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F));
                     this.altSwing(matrices, arm, swingProgress, item);
                     matrices.method_22905(0.9F, 0.9F, 0.9F);
                     this.method_3219(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
               }
            } else if (player.method_6123() && item.method_7976() == class_1839.field_8951) {
               this.riptideCounter = (float)(this.riptideCounter + 0.15 * tt);
               float mx = item.method_7935(player) - (player.method_6014() - tickDelta + 1.0F);
               float fxxxx = mx / 10.0F;
               if (fxxxx > 1.0F) {
                  fxxxx = 1.0F;
               }

               if (fxxxx > 0.1F) {
                  float gxx = class_3532.method_15374((mx - 0.1F) * 1.3F);
                  float h = fxxxx - 0.1F;
                  float j = gxx * h;
                  matrices.method_46416(j * 0.0F, j * 0.004F, j * 0.0F);
               }

               matrices.method_22907(class_7833.field_40714.rotationDegrees(45.0F - this.riptideCounter * 2.0F));
               matrices.method_22907(class_7833.field_40716.rotationDegrees(25 * l));
               matrices.method_22904(0.2 * l, 0.0, 0.75);
               matrices.method_22904(0.0, 0.0, 0.01 * class_3532.method_15374(this.riptideCounter * 6.28F));
               this.method_3219(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
               matrices.method_22907(class_7833.field_40714.rotationDegrees(135.0F));
               matrices.method_22907(class_7833.field_40718.rotationDegrees(-65 * l));
               matrices.method_22904(0.65F * l, -1.0, -0.6);
            } else {
               this.riptideCounter = 0.0F;
               if (!item.method_31574(class_1802.field_16539) && !item.method_31574(class_1802.field_22016) && !item.method_31573(class_3489.field_40108)) {
                  if (item.method_7976() == class_1839.field_8949) {
                     matrices.method_22904(0.0, -0.2, 0.0);
                  }
               } else {
                  matrices.method_22904(0.1 * l, 0.0, -0.1);
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(10.0F));
               }

               matrices.method_22904(1 * l, 0.0 - equipProgress * 0.3, 0.3);
               matrices.method_22907(class_7833.field_40716.rotationDegrees(45 * l));
               matrices.method_22907(class_7833.field_40718.rotationDegrees(-40 * l));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F));
               this.altSwing(matrices, arm, swingProgress, item);
               matrices.method_22905(0.9F, 0.9F, 0.9F);
               this.method_3219(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            }

            matrices.method_22904(-0.3 * l, 0.65, -0.1);
            matrices.method_22907(class_7833.field_40716.rotationDegrees(-65 * l));
            matrices.method_22907(class_7833.field_40714.rotationDegrees(10.0F));
            if (item.method_31573(class_3489.field_15542)) {
               matrices.method_22904(0.2 * l, -0.1, 0.0);
            }

            if (class_2248.method_9503(item.method_7909()) == class_2246.field_10124
               || item.method_7976() == class_1839.field_8950
               || item.method_31573(ConventionalItemTags.BUCKETS)) {
               if ((
                     !item.method_31573(ConventionalItemTags.TOOLS)
                        || item.method_31573(class_3489.field_41890)
                        || item.method_31573(class_3489.field_40109)
                        || item.method_7976() == class_1839.field_8950
                        || !item.method_7923()
                  )
                  && item.method_7976() != class_1839.field_8953
                  && item.method_7976() != class_1839.field_27079
                  && this.getAttackDamage(item) == 0.0F
                  && item.method_7976() != class_1839.field_8949
                  && !item.method_31574(class_1802.field_23254)
                  && !item.method_31574(class_1802.field_8184)
                  && !item.method_31574(class_1802.field_8378)
                  && !item.method_31574(class_1802.field_8868)
                  && !item.method_31573(class_3489.field_42613)
                  && !config.mb3DCompat) {
                  if (item.method_7976() == class_1839.field_42717) {
                     matrices.method_22907(class_7833.field_40713.rotationDegrees(25.0F));
                     matrices.method_22904(bl ? 0.0 : 0.35, bl ? 0.0 : 0.25, bl ? 0.0 : 0.37);
                     if (!bl) {
                        matrices.method_22905(0.75F, 0.75F, 0.75F);
                     }

                     matrices.method_22907(class_7833.field_40717.rotationDegrees(-75 * l));
                     matrices.method_22907(class_7833.field_40713.rotationDegrees(35.0F));
                     matrices.method_22904(bl ? -0.05 : 0.85, bl ? 0.0 : 0.05, bl ? 0.08 : -0.2);
                  } else {
                     matrices.method_22907(class_7833.field_40715.rotationDegrees(5 * l));
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(15.0F));
                     matrices.method_22907(class_7833.field_40718.rotationDegrees(75 * l));
                     matrices.method_22904(0.0, -0.05, -0.1);
                     matrices.method_22905(0.7F, 0.7F, 0.7F);
                  }

                  if (item.method_31574(class_1802.field_8153) || item.method_31574(class_1802.field_8777) || item.method_31574(class_1802.field_8323)) {
                     this.vertVelocityYSlime = (float)(this.vertVelocityYSlime + swingProgress * 0.03 * (HoldMyItems.deltaTime * 30.0));
                     if ((
                           player.method_18798().method_1033() > 0.09 && player.method_24828()
                              || player.method_5681()
                              || player.method_20448()
                              || player.method_6101() && !player.method_24828()
                        )
                        && (Boolean)this.field_4050.field_1690.method_42448().method_41753()) {
                        Random random = new Random();
                        boolean randomBoolean = random.nextBoolean();
                        this.vertVelocityYSlime = this.vertVelocityYSlime
                           + (float)(-0.05 * player.method_18798().method_1033() * (HoldMyItems.deltaTime * 30.0));
                     }

                     matrices.method_22905(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
                  }
               } else if (item.method_7976() == class_1839.field_8949 && item.method_7976() != class_1839.field_8951) {
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(160 * l));
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(-60 * l));
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(-70.0F));
                  matrices.method_22905(0.75F, 0.75F, 0.75F);
                  matrices.method_22904(0.15 * l, bl ? 0.35 : 0.45, bl ? -0.15 : -0.1);
                  matrices.method_22904(0.17 * l, 0.0, 0.3);
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(-90 * l));
               } else if (item.method_7976() == class_1839.field_8951) {
                  matrices.method_22907(class_7833.field_40715.rotationDegrees(75 * l));
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(90.0F));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(45 * l));
                  matrices.method_46416(-0.3F * l, 0.0F, 0.0F);
               } else if (item.method_7976() != class_1839.field_8951) {
                  matrices.method_22907(class_7833.field_40715.rotationDegrees(75 * l));
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(70.0F));
                  matrices.method_22907(class_7833.field_40718.rotationDegrees(45 * l));
               }

               if (item.method_7976() != class_1839.field_8949) {
                  matrices.method_22905(1.2F, 1.2F, 1.2F);
               }

               if (item.method_7976() == class_1839.field_8953 && !player.method_6115()) {
                  matrices.method_22904(-0.1 * l, -0.2, 0.0);
               }

               if (item.method_31574(class_1802.field_49814)) {
                  if (config.mb3DCompat) {
                     matrices.method_22904(-0.08, 0.17, 0.0);
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(40.0F));
                  }

                  matrices.method_22904(0.1 * l, 0.0, 0.0);
                  matrices.method_22905(0.9F, 0.9F, 0.9F);
               }
            } else if (item.method_7964().toString().toLowerCase().contains("TORCH".toLowerCase())) {
               matrices.method_22905(1.5F, 1.5F, 1.5F);
               matrices.method_22907(class_7833.field_40715.rotationDegrees(25 * l));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(5.0F));
               matrices.method_22907(class_7833.field_40718.rotationDegrees(75 * l));
               matrices.method_22904(0.2 * l, 0.2, 0.05);
            } else if ((
                  item.method_31574(class_1802.field_8276)
                     || item.method_31574(class_1802.field_8725)
                     || item.method_31574(class_1802.field_8865)
                     || item.method_31574(class_1802.field_8366)
                     || class_2248.method_9503(item.method_7909()).method_9564().method_26164(ConventionalBlockTags.GLASS_PANES)
                     || class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_15463)
                     || class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_22414)
                     || item.method_31573(class_3489.field_15553)
               )
               && !class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_15503)
               && !class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_43170)
               && !class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_15501)) {
               matrices.method_22904(0.0, 0.0, -0.1);
               matrices.method_22907(class_7833.field_40715.rotationDegrees(5 * l));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(15.0F));
               matrices.method_22907(class_7833.field_40718.rotationDegrees(75 * l));
            } else if (!item.method_31574(class_1802.field_16539) && !item.method_31574(class_1802.field_22016) && !item.method_31573(class_3489.field_40108)) {
               matrices.method_22907(class_7833.field_40715.rotationDegrees(25 * l));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(5.0F));
               matrices.method_22907(class_7833.field_40718.rotationDegrees(75 * l));
               matrices.method_22904(0.2 * l, 0.2, 0.05);
               if (class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_15501)) {
                  matrices.method_22904(-0.2 * l, 0.0, 0.0);
                  matrices.method_22905(1.1F, 1.1F, 1.1F);
               }
            } else {
               float dt = (float)(HoldMyItems.deltaTime * 30.0);
               float yawDelta = player.field_6259 - player.method_5791();
               float pitchDelta = player.field_6004 - player.method_36455();
               this.swingVelocityY += yawDelta * 0.015F * dt;
               this.swingVelocityY += swingProgress * 2.0F * dt;
               this.swingVelocityX += pitchDelta * 0.015F * dt;
               this.swingVelocityY = this.swingVelocityY - 0.1F * this.swingAngleY * dt;
               this.swingVelocityX = this.swingVelocityX - 0.1F * this.swingAngleX * dt;
               this.swingVelocityY = (float)(this.swingVelocityY * Math.pow(0.88F, dt));
               this.swingVelocityX = (float)(this.swingVelocityX * Math.pow(0.88F, dt));
               this.swingAngleY = this.swingAngleY + this.swingVelocityY * dt;
               this.swingAngleX = this.swingAngleX + this.swingVelocityX * dt;
               double currentSpeed = player.method_18798().method_1033();
               this.swingVelocityZ = (float)(
                  this.swingVelocityZ
                     + (bl ? (currentSpeed * -1.0 * 15.0 - this.swingVelocityZ) * 0.1F * dt : (currentSpeed * 15.0 - this.swingVelocityZ) * 0.1F * dt)
               );
               if ((currentSpeed > 0.09 && player.method_24828() || player.method_5681() || player.method_6101() && !player.method_24828())
                  && (Boolean)this.field_4050.field_1690.method_42448().method_41753()) {
                  Random random = new Random();
                  boolean randomBoolean = random.nextBoolean();
                  this.swingVelocityY += (float)(randomBoolean ? -5.5 * currentSpeed * dt : 5.5 * currentSpeed * dt);
               }

               matrices.method_22904(0.0, 0.0, -0.1);
               matrices.method_22907(class_7833.field_40715.rotationDegrees(35 * l + this.swingAngleY));
               matrices.method_22907(class_7833.field_40714.rotationDegrees(15.0F + this.swingAngleX));
               matrices.method_22907(class_7833.field_40718.rotationDegrees(75 * l + this.swingVelocityZ));
               if (item.method_31573(class_3489.field_40108)) {
                  matrices.method_22904(0.0, -0.1, 0.0);
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(-45 * l));
               }

               matrices.method_22904(0.3 * l, -0.35, 0.0);
               matrices.method_22904(0.0, 0.0, 0.1);
               matrices.method_22905(1.5F, 1.5F, 1.5F);
            }

            if (item.method_7909() instanceof class_1747
               && (
                  !item.method_31573(ConventionalItemTags.BUCKETS)
                        && item.method_7976() != class_1839.field_8950
                        && !item.method_31573(class_3489.field_15556)
                        && !item.method_31574(class_1802.field_8276)
                        && !item.method_31574(class_1802.field_8725)
                        && !item.method_31574(class_1802.field_8865)
                        && !item.method_31574(class_1802.field_8366)
                        && !class_2248.method_9503(item.method_7909()).method_9564().method_26164(ConventionalBlockTags.GLASS_PANES)
                        && !class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_15463)
                        && !class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_22414)
                        && !item.method_31573(class_3489.field_15553)
                     || class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_15503)
               )
               && !class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_43170)) {
               class_1747 blockItem = (class_1747)item.method_7909();
               class_776 blockRenderManager = class_310.method_1551().method_1541();
               class_1087 model = blockRenderManager.method_3349(blockItem.method_7711().method_9564());
               matrices.method_22903();
               if (!bl2x) {
                  matrices.method_46416(-0.4F, 0.0F, 0.0F);
               }

               matrices.method_22905(0.4F, 0.4F, 0.4F);
               matrices.method_22904(-0.9 * l, -0.45, -0.5);
               if (class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_15493)) {
                  matrices.method_22904(0.2 * l, -0.15, -0.2);
               }

               if (class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_24076)) {
                  matrices.method_22904(0.0, 0.1, 0.0);
               }

               if (item.method_31574(class_1802.field_8828)
                  || item.method_31574(class_1802.field_21086)
                  || class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_20339)
                  || class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_15503)
                  || class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_15462)
                  || class_2248.method_9503(item.method_7909()).method_9564().method_26164(class_3481.field_44469)) {
                  this.vertVelocityYSlime = (float)(this.vertVelocityYSlime + swingProgress * 0.03 * (HoldMyItems.deltaTime * 30.0));
                  if ((
                        player.method_18798().method_1033() > 0.09 && player.method_24828()
                           || player.method_5681()
                           || player.method_20448()
                           || player.method_6101() && !player.method_24828()
                     )
                     && (Boolean)this.field_4050.field_1690.method_42448().method_41753()) {
                     Random random = new Random();
                     boolean randomBoolean = random.nextBoolean();
                     this.vertVelocityYSlime = this.vertVelocityYSlime + (float)(-0.05 * player.method_18798().method_1033() * (HoldMyItems.deltaTime * 30.0));
                  }

                  matrices.method_22905(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
               }

               class_2680 blockState = blockItem.method_7711().method_9564();
               if (player.field_6012 - this.prevAge >= 100.0F) {
                  this.repPower = !this.repPower;
                  this.prevAge = player.field_6012;
               }

               if (blockItem.method_7711() == class_2246.field_10450 && this.repPower) {
                  blockState = (class_2680)blockState.method_11657(class_2462.field_10911, true);
               }

               if (blockItem.method_7711() == class_2246.field_10377 && this.repPower) {
                  blockState = (class_2680)blockState.method_11657(class_2286.field_10911, true);
               }

               if (blockItem.method_7711() == class_2246.field_10523 && player.method_5869()) {
                  blockState = (class_2680)blockState.method_11657(class_2459.field_11446, false);
               }

               if ((blockItem.method_7711() == class_2246.field_17350 || blockItem.method_7711() == class_2246.field_23860) && player.method_5869()) {
                  blockState = (class_2680)blockState.method_11657(class_3922.field_17352, false);
               }

               if (item.method_31573(class_3489.field_16444)) {
                  if (bl) {
                     matrices.method_22904(0.9, 0.0, 0.8);
                  }

                  matrices.method_22907(class_7833.field_40716.rotationDegrees(90 * l));
               }

               blockRenderManager.method_3353(blockState, matrices, vertexConsumers, light, class_4608.field_21444);
               matrices.method_22909();
            } else {
               if (item.method_31573(ConventionalItemTags.TOOLS)
                     && !item.method_31573(class_3489.field_41890)
                     && !item.method_31573(class_3489.field_40109)
                     && item.method_7976() != class_1839.field_8950
                     && item.method_7923()
                  || item.method_7976() == class_1839.field_8953
                  || item.method_7976() == class_1839.field_27079
                  || this.getAttackDamage(item) != 0.0F
                  || item.method_7976() == class_1839.field_8949
                  || item.method_31574(class_1802.field_23254)
                  || item.method_31574(class_1802.field_8184)
                  || item.method_31574(class_1802.field_8378)
                  || item.method_31574(class_1802.field_8868)) {
                  if (item.method_31573(class_3489.field_42611)) {
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(-60.0F * swing));
                     matrices.method_22904(0.0, 0.1 * swing, -0.1 * swing);
                  }

                  if (item.method_31573(class_3489.field_42615)) {
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(-80.0F * swing_rot));
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(30.0F * swing));
                  } else if (item.method_7976() == class_1839.field_8951) {
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(-40.0F * swing_rot));
                     matrices.method_22904(0.0, 0.1 * swing_rot, -0.1 * swing_rot);
                  } else if (item.method_7976() != class_1839.field_8949) {
                     matrices.method_22907(class_7833.field_40714.rotationDegrees(-25.0F * swing));
                     matrices.method_22904(0.0, 0.05 * swing, -0.05 * swing);
                  }
               }

               if (!item.method_31574(class_1802.field_8137) && (!item.method_31574(class_1802.field_8301) || !config.mb3DCompat)) {
                  this.netherCounter = 0.0F;
               } else {
                  this.netherCounter = (float)(this.netherCounter + 0.9 * tt);
                  matrices.method_22904(0.0, 0.25 + 0.02 * class_3532.method_15374(this.netherCounter * 0.1F), 0.0);
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(3.0F * class_3532.method_15374(this.netherCounter * 0.2F)));
                  matrices.method_22905(
                     1.0F + 0.01F * class_3532.method_15374(this.netherCounter),
                     1.0F + 0.01F * class_3532.method_15374(this.netherCounter),
                     1.0F + 0.01F * class_3532.method_15374(this.netherCounter)
                  );
               }

               if (config.mb3DCompat) {
                  if (item.method_31573(class_3489.field_42611)) {
                     matrices.method_22904(0.0, 0.2, 0.0);
                  }

                  if (item.method_31574(class_1802.field_8153) || item.method_31574(class_1802.field_8777) || item.method_31574(class_1802.field_8323)) {
                     this.vertVelocityYSlime = (float)(this.vertVelocityYSlime + swingProgress * 0.03 * (HoldMyItems.deltaTime * 30.0));
                     if ((
                           player.method_18798().method_1033() > 0.09 && player.method_24828()
                              || player.method_5681()
                              || player.method_20448()
                              || player.method_6101() && !player.method_24828()
                        )
                        && (Boolean)this.field_4050.field_1690.method_42448().method_41753()) {
                        Random random = new Random();
                        boolean randomBoolean = random.nextBoolean();
                        this.vertVelocityYSlime = this.vertVelocityYSlime
                           + (float)(-0.05 * player.method_18798().method_1033() * (HoldMyItems.deltaTime * 30.0));
                     }

                     matrices.method_22905(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
                  }
               }

               if (item.method_31573(class_3489.field_42615)) {
                  matrices.method_22904(0.07 * l, 0.0, 0.05);
                  matrices.method_22907(class_7833.field_40716.rotationDegrees(90 * l));
                  matrices.method_22907(class_7833.field_40714.rotationDegrees(-15.0F));
               }

               if (item.method_31574(class_1802.field_8810)) {
                  player.method_37908()
                     .method_8406(
                        class_2398.field_11246,
                        player.method_19538().method_10216(),
                        player.method_19538().method_10214(),
                        player.method_19538().method_10215(),
                        0.1,
                        0.1,
                        0.1
                     );
               }

               this.method_3233(player, item, bl2x ? class_811.field_4322 : class_811.field_4321, !bl2x, matrices, vertexConsumers, light);
            }
         }

         matrices.method_22909();
         matrices.method_22909();
         this.isAttacking = this.field_4050.field_1690.field_1886.method_1434();
      }
   }

   @Redirect(
      method = "swingArm",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"
      )
   )
   private void applySwing(class_759 instance, class_4587 matrices, class_1306 arm, float swingProgress) {
   }
}
