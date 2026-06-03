package dev.padiloi1337.hitcolor.mixins;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.padiloi1337.hitcolor.change.Modelable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;

@Mixin(BipedModel.class)
public abstract class BipedModelMixin<T extends LivingEntity> implements Modelable{

	private LivingEntity entity;
	private String call;
	
	@Override
	public LivingEntity getEntity() {
		return entity;
	}

	@Override
	public void setEntity(LivingEntity entity) {
		this.entity = entity;
	}
	
	@Override
	public String call() {
		return call;
	}
	
	@Inject(method = "<init>(Ljava/util/function/Function;FFII)V", at = @At("TAIL"))
	private void init(Function<ResourceLocation, RenderType> renderTypeIn, float modelSizeIn, float yOffsetIn, int textureWidthIn, int textureHeightIn, CallbackInfo ci) {
		call = Thread.currentThread().getStackTrace()[4].toString();
	}
	
	@Inject(method = "setLivingAnimations", at = @At("HEAD"))
	public void setLivingAnimations(T entityIn, float limbSwing, float limbSwingAmount, float partialTick, CallbackInfo ci) {
		if (entityIn != null) {
			setEntity(entityIn);
		}
	}
	
	@Inject(method = "setRotationAngles", at = @At("HEAD"))
	public void setRotationAngles(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (entityIn != null) {
			setEntity(entityIn);
		}
	}
	
}
