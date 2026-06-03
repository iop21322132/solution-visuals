package dev.padiloi1337.hitcolor.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;

import dev.padiloi1337.hitcolor.animations.LinearAnimation;
import dev.padiloi1337.hitcolor.helpers.misc.HoverUtil;

public class Button extends Element {

	private LinearAnimation clickAnim = new LinearAnimation(1);
	private RenderFunc onRender;
	private Runnable onClick;
	
	public Button(double width, double height, String title, Runnable onClick, RenderFunc onRender) {
		super(width, height, title);
		this.onClick = onClick;
		this.onRender = onRender;
	}
	
	public Button(double width, double height, Runnable onClick, RenderFunc onRender) {
		this(width, height, "", onClick, onRender);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void render(MatrixStack matrices, double mouseX, double mouseY) {
		GlStateManager.pushMatrix();
		GlStateManager.translated(x + width / 2, y - height / 2, 0);
		double s = clickAnim.getAndUpdate();
		GlStateManager.scaled(s, s, 1);
		onRender.render(matrices, -width / 2, height / 2, width, height, title);
		GlStateManager.popMatrix();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if(HoverUtil.hovered(mouseX, mouseY, x, y, width, height)) {
			clickAnim.begin(clickAnim.get(), 0.86, 2.4, () -> clickAnim.begin(clickAnim.get(), 1, 2.4));
			onClick.run();
			return true;
		}
		return false;
	}
	
	@FunctionalInterface
	public interface RenderFunc {
		void render(MatrixStack matrices, double x, double y, double width, double height, String title);
	}

}
