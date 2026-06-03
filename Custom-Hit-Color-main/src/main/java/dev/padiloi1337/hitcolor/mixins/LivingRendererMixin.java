package dev.padiloi1337.hitcolor.mixins;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import dev.padiloi1337.hitcolor.HitColor;
import dev.padiloi1337.hitcolor.change.Modelable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;

@Mixin(LivingRenderer.class)
public abstract class LivingRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {

	@Shadow protected List<LayerRenderer<T, M>> layerRenderers;
	
	@Shadow protected abstract float handleRotationFloat(T livingBase, float partialTicks);
	@Shadow protected abstract void applyRotations(T entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks);
	@Shadow protected abstract void preRenderCallback(T entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime);
	@Shadow protected abstract boolean isVisible(T livingEntityIn);
	@Shadow protected abstract RenderType func_230496_a_(T p_230496_1_, boolean p_230496_2_, boolean p_230496_3_, boolean p_230496_4_);
	@Shadow protected abstract float getOverlayProgress(T livingEntityIn, float partialTicks);
	
    public LivingRendererMixin(EntityRendererManager renderManagerIn) {
        super(renderManagerIn);
    }

    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    private void onRender(T entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, CallbackInfo ci) {
    	ci.cancel();
    	LivingRenderer<T, M> living = ((LivingRenderer<T, M>) (Object) this);
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre<T, M>(entityIn, living, partialTicks, matrixStackIn, bufferIn, packedLightIn))) return;
        matrixStackIn.push();
        living.getEntityModel().swingProgress = entityIn.getSwingProgress(partialTicks);

        boolean shouldSit = entityIn.isPassenger() && (entityIn.getRidingEntity() != null && entityIn.getRidingEntity().shouldRiderSit());
        living.getEntityModel().isSitting = shouldSit;
        living.getEntityModel().isChild = entityIn.isChild();
        float f = MathHelper.interpolateAngle(partialTicks, entityIn.prevRenderYawOffset, entityIn.renderYawOffset);
        float f1 = MathHelper.interpolateAngle(partialTicks, entityIn.prevRotationYawHead, entityIn.rotationYawHead);
        float f2 = f1 - f;
        if (shouldSit && entityIn.getRidingEntity() instanceof LivingEntity) {
           LivingEntity livingentity = (LivingEntity)entityIn.getRidingEntity();
           f = MathHelper.interpolateAngle(partialTicks, livingentity.prevRenderYawOffset, livingentity.renderYawOffset);
           f2 = f1 - f;
           float f3 = MathHelper.wrapDegrees(f2);
           if (f3 < -85.0F) {
              f3 = -85.0F;
           }

           if (f3 >= 85.0F) {
              f3 = 85.0F;
           }

           f = f1 - f3;
           if (f3 * f3 > 2500.0F) {
              f += f3 * 0.2F;
           }

           f2 = f1 - f;
        }

        float f6 = MathHelper.lerp(partialTicks, entityIn.prevRotationPitch, entityIn.rotationPitch);
        if (entityIn.getPose() == Pose.SLEEPING) {
           Direction direction = entityIn.getBedDirection();
           if (direction != null) {
              float f4 = entityIn.getEyeHeight(Pose.STANDING) - 0.1F;
              matrixStackIn.translate((double)((float)(-direction.getXOffset()) * f4), 0.0D, (double)((float)(-direction.getZOffset()) * f4));
           }
        }

        float f7 = handleRotationFloat(entityIn, partialTicks);
        applyRotations(entityIn, matrixStackIn, f7, f, partialTicks);
        matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
        preRenderCallback(entityIn, matrixStackIn, partialTicks);
        matrixStackIn.translate(0.0D, (double)-1.501F, 0.0D);
        float f8 = 0.0F;
        float f5 = 0.0F;
        if (!shouldSit && entityIn.isAlive()) {
           f8 = MathHelper.lerp(partialTicks, entityIn.prevLimbSwingAmount, entityIn.limbSwingAmount);
           f5 = entityIn.limbSwing - entityIn.limbSwingAmount * (1.0F - partialTicks);
           if (entityIn.isChild()) {
              f5 *= 3.0F;
           }

           if (f8 > 1.0F) {
              f8 = 1.0F;
           }
        }

        living.getEntityModel().setLivingAnimations(entityIn, f5, f8, partialTicks);
        living.getEntityModel().setRotationAngles(entityIn, f5, f8, f7, f2, f6);
        Minecraft minecraft = Minecraft.getInstance();
        boolean flag = isVisible(entityIn);
        boolean flag1 = !flag && !entityIn.isInvisibleToPlayer(minecraft.player);
        boolean flag2 = minecraft.isEntityGlowing(entityIn);
        RenderType rendertype = func_230496_a_(entityIn, flag, flag1, flag2);
        if (rendertype != null) {
           IVertexBuilder ivertexbuilder = bufferIn.getBuffer(rendertype);
           ivertexbuilder = ivertexbuilder.color(0.f, 0.f, 0.f, 0.3f);
//           Color color = ((entityIn.hurtTime > 0 || entityIn.deathTime > 0) && TopkaHitboxes.getInstance().settings.color.getValue()) ? 
//        		   TopkaHitboxes.getInstance().settings.color.getColor() : new Color(1.f, 1.f, 1.f, (flag1 ? 0.15F : 1.0F));
           Modelable modelable = (Modelable) living.getEntityModel();
           modelable.setEntity(entityIn);
           living.getEntityModel().render(matrixStackIn, ivertexbuilder, packedLightIn, 
        		   HitColor.getInstance().settings.color.getValue() ? 655360 : LivingRenderer.getPackedOverlay(entityIn, this.getOverlayProgress(entityIn, partialTicks)),
        				   1.0F, 1.0F, 1.0F, flag1 ? 0.15F : 1.0F);
        }

        if (!entityIn.isSpectator()) {
           for(LayerRenderer<T, M> layerrenderer : layerRenderers) {
              layerrenderer.render(matrixStackIn, bufferIn, packedLightIn, entityIn, f5, f8, partialTicks, f7, f2, f6);
           }
        }

        matrixStackIn.pop();
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<T, M>(entityIn, living, partialTicks, matrixStackIn, bufferIn, packedLightIn));
    
    }
}