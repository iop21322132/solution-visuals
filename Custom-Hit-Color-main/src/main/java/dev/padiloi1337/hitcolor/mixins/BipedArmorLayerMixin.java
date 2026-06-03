package dev.padiloi1337.hitcolor.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.padiloi1337.hitcolor.change.BipedArmored;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;

@Mixin(BipedArmorLayer.class)
public abstract class BipedArmorLayerMixin<T extends LivingEntity, M extends BipedModel<T>, A extends BipedModel<T>> implements BipedArmored<T, A>{
	
	private A a1, a2;
	
	@Override
	public A get1() {
		return a1;
	}
	
	@Override
	public A get2() {
		return a2;
	}
	
	@Override
	public void set1(A a1) {
		this.a1 = a1;
	}
	
	@Override
	public void set2(A a2) {
		this.a2 = a2;
	}
	
	@Inject(method = "<init>", at = @At("HEAD"))
	public void init(IEntityRenderer<T, M> p_i50936_1_, A p_i50936_2_, A p_i50936_3_, CallbackInfo ci) {
		set1(p_i50936_2_);
		set2(p_i50936_3_);
		System.out.println("init");
	}
	
}
