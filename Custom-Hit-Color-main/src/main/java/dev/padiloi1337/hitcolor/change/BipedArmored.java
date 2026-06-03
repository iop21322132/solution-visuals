package dev.padiloi1337.hitcolor.change;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;

public interface BipedArmored<T extends LivingEntity, A extends BipedModel<T>> {
	A get1();
	A get2();
	void set1(A a);
	void set2(A a);
}
