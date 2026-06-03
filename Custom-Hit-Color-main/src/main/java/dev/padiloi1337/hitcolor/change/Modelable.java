package dev.padiloi1337.hitcolor.change;

import net.minecraft.entity.LivingEntity;

public interface Modelable {

	LivingEntity getEntity();
	void setEntity(LivingEntity entity);
	String call();
}
