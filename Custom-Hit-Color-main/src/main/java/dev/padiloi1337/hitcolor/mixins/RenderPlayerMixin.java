package dev.padiloi1337.hitcolor.mixins;

import java.lang.reflect.Field;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.padiloi1337.hitcolor.change.Modelable;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;

@Mixin(PlayerRenderer.class)
public class RenderPlayerMixin<T extends LivingEntity, M extends EntityModel<T>> {

	@Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
	public void render(AbstractClientPlayerEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, CallbackInfo ci) {
		List<LayerRenderer<T, M>> layerRenderers = null;
		
		try {
			Field a = LivingRenderer.class.getDeclaredFields()[2];
			a.setAccessible(true);
			layerRenderers = (List<LayerRenderer<T, M>>) a.get(this);
		} catch (Exception e) {}
		
		for (LayerRenderer<T, M> layer : layerRenderers) {
			if (layer instanceof BipedArmorLayer) {
//				BipedArmored<?, ?> biArmored = (BipedArmored<?, ?>) layer;
//				((Modelable)biArmored.get1()).setEntity(entityIn);
//				((Modelable)biArmored.get2()).setEntity(entityIn);
				
				try {
					Field a1 = BipedArmorLayer.class.getDeclaredFields()[1];
					Field a2 = BipedArmorLayer.class.getDeclaredFields()[2];
					
					a1.setAccessible(true);
					a2.setAccessible(true);
					
					((Modelable)a1.get(layer)).setEntity(entityIn);
					((Modelable)a2.get(layer)).setEntity(entityIn);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				
			}
		}
		
	}
	
}
