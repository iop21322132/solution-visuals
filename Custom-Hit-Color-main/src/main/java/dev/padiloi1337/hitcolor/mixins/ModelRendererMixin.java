package dev.padiloi1337.hitcolor.mixins;

import java.awt.Color;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import dev.padiloi1337.hitcolor.HitColor;
import dev.padiloi1337.hitcolor.change.ModelRendererable;
import dev.padiloi1337.hitcolor.change.Modelable;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;

@Mixin(ModelRenderer.class)
public abstract class ModelRendererMixin implements ModelRendererable {

	private Model save;
	
	@Shadow public boolean showModel;
	@Shadow private ObjectList<ModelRenderer.ModelBox> cubeList;
	@Shadow private ObjectList<ModelRenderer> childModels;
	
	@Shadow public abstract void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha);
	@Shadow public abstract void translateRotate(MatrixStack matrixStackIn);
	@Shadow public abstract void doRender(MatrixStack.Entry matrixEntryIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha);
	
	@Inject(method = "<init>(Lnet/minecraft/client/renderer/model/Model;)V", at = @At("TAIL"))
    private void onInit(Model model, CallbackInfo ci) {
        save = model;
    }
	
	@Inject(method = "<init>(Lnet/minecraft/client/renderer/model/Model;II)V", at = @At("TAIL"))
    private void onInit2(Model model, int texOffX, int texOffY, CallbackInfo ci) {
        save = model;
    }
	
	@Inject(method = "(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V", at = @At(value = "HEAD"), cancellable = true)
	public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha, CallbackInfo ci) {
		ci.cancel();
//		render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, color.getRed(), color.getRed(), color.getBlue(), color.getAlpha());
		ModelRenderer renderer = (ModelRenderer) (Object) this;
		ModelRendererable rendererable = (ModelRendererable) renderer;
		Color color = null;
		Model model = rendererable.getPlayerModel();
		if (model == null) {
			color = new Color(red, green, blue, alpha);
		}
		Modelable modelable = (Modelable) model;
		if (color == null) {
			color = (modelable.getEntity() != null && (modelable.getEntity().hurtTime > 0 || modelable.getEntity().deathTime > 0) && HitColor.getInstance().settings.color.getValue()) ?
					HitColor.getInstance().settings.color.getColor() : new Color(red, green, blue, alpha);
		}
		if (this.showModel) {
			if (!this.cubeList.isEmpty() || !this.childModels.isEmpty()) {
				matrixStackIn.push();
				this.translateRotate(matrixStackIn);
				if (model != null && model instanceof BipedModel) {
					if (modelable.getEntity() == null) {
						System.out.println(modelable.call());
					}
				}
				this.doRender(matrixStackIn.getLast(), bufferIn, packedLightIn, packedOverlayIn, color.getRed() / 255.f, color.getGreen() / 255.f, color.getBlue() / 255.f, color.getAlpha() / 255.f);

				for(ModelRenderer modelrenderer : this.childModels) {
					modelrenderer.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, color.getRed() / 255.f, color.getGreen() / 255.f, color.getBlue() / 255.f, color.getAlpha() / 255.f);
				}

				matrixStackIn.pop();
			}
		}
	}
	
	@Override
	public Model getPlayerModel() {
		return save;
	}
	
}
