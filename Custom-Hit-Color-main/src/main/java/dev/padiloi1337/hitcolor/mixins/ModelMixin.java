package dev.padiloi1337.hitcolor.mixins;

import org.spongepowered.asm.mixin.Mixin;

import dev.padiloi1337.hitcolor.change.Modelable;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.entity.LivingEntity;

@Mixin(Model.class)
public abstract class ModelMixin implements Modelable{

	private LivingEntity entity;

	@Override
	public LivingEntity getEntity() {
		return entity;
	}

	@Override
	public void setEntity(LivingEntity entity) {
		this.entity = entity;
	}
	
}
