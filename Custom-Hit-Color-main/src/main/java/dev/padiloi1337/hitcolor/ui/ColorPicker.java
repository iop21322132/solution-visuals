package dev.padiloi1337.hitcolor.ui;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.padiloi1337.hitcolor.animations.EaseAnimation;
import dev.padiloi1337.hitcolor.animations.EaseAnimation.Type;
import dev.padiloi1337.hitcolor.helpers.font.FontRenderer;
import dev.padiloi1337.hitcolor.helpers.misc.ColorHelper;
import dev.padiloi1337.hitcolor.helpers.misc.HoverUtil;
import dev.padiloi1337.hitcolor.helpers.misc.MathHelper;
import dev.padiloi1337.hitcolor.helpers.render.DrawHelper;
import dev.padiloi1337.hitcolor.settings.impl.ColorSetting;

public class ColorPicker extends Element {
	
	private EaseAnimation moveXAnim = new EaseAnimation(Type.OUT);
	private final double sliderHeight = 64, radius = 33, sliderWidth = 3;
	private double centerX, centerY, pointerX, pointerY, xp, sy, brightnessX, saturationX, alphaX;
	private boolean opened, pointerDragging, brightnessDragging, saturationDragging, alphaDragging;
	private float hue, saturation, brightness, alpha;
	private ColorSetting setting;
	private Button copy = new Button(34, 17, "Copy", () -> MC.keyboardListener.setClipboardString(convert()),
			(matrices, x, y, width, height, title) -> {
				DrawHelper.drawRoundedRect(x, y, width, height, 4, GuiScreen.BACKGROUND_SECONDARY);
				FontRenderer.drawCenteredXYString(matrices, DEFAULT_20, title, x + width / 2, y - height / 1.5, Color.WHITE);
			});
	private Button paste = new Button(34, 17, "Paste", () -> parse(MC.keyboardListener.getClipboardString()),
			(matrices, x, y, width, height, title) -> {
				DrawHelper.drawRoundedRect(x, y, width, height, 4, GuiScreen.BACKGROUND_SECONDARY);
				FontRenderer.drawCenteredXYString(matrices, DEFAULT_20, title, x + width / 2, y - height / 1.5, Color.WHITE);
			});

	public ColorPicker() {
		super(160, 130, "");
	}

	@Override
	public void render(MatrixStack matrices, double mouseX, double mouseY) {
		if(moveXAnim.isAnimating()) {
			x = moveXAnim.getAndUpdate();
			update();
		}
		
		// background
		DrawHelper.drawGlow(x, y, (int)width, (int)height, 14, Color.BLACK);
		DrawHelper.drawRoundedRect(x, y, width, height, 5, GuiScreen.BACKGROUND_PRIMARY);
		DrawHelper.drawRoundedRect(x + 95, sy + 3, 52, 73, 1.5, GuiScreen.BACKGROUND_SECONDARY);
		
		// title and cross
		FontRenderer.drawCenteredXYString(matrices, DEFAULT_24, title, x + width / 2, y - height + 10, Color.WHITE);
		FontRenderer.drawString(matrices, ICONS_22, "b", x + width - 18, y - height + 17, 
				HoverUtil.hovered(mouseX, mouseY, x + width - 20.5, y - height + 19, 15, 16) ? Color.RED : Color.WHITE);
		
		// color picker
		DrawHelper.drawRainbowCircle(centerX, centerY, radius, 2);
		
		// pointer
		DrawHelper.drawCircleOutline(xp + pointerX, sy - pointerY, 3, 2, GuiScreen.BACKGROUND_SECONDARY);
		
		// brightness slider
		drawSlider(brightnessX, brightness, Color.BLACK, Color.getHSBColor(hue, saturation, 1f));
		
		// saturation slider
		drawSlider(saturationX, saturation, Color.WHITE, Color.getHSBColor(hue, 1f, brightness));
		
		// alpha slider
		Color aColor = Color.getHSBColor(hue, saturation, brightness);
		drawSlider(alphaX, alpha, ColorHelper.injectAlpha(aColor, 15), aColor);
		
		// copy and paste buttons
		copy.render(matrices, mouseX, mouseY);
		paste.render(matrices, mouseX, mouseY);
		
		Color color = setting.getValue();
		DrawHelper.drawRoundedRect(x + 95, y - 7, 52, 15, 4, GuiScreen.BACKGROUND_SECONDARY);
		DrawHelper.drawRoundedRect(x + 95, y - 7, 52, 15, 4, color);
		FontRenderer.drawCenteredXYString(matrices, DEFAULT_20, ColorHelper.hex(color), x + 121, y - 14.5, 
				new Color(1-brightness, 1-brightness, 1-brightness));
	}
	
	private void drawSlider(double x, float value, Color color1, Color color2) {
		DrawHelper.drawRoundedGradientRect(x, sy - 1, 3, sliderHeight, 1, color2, color1, color2, color1);
		DrawHelper.drawCircle(x + 1.5, sy - value * sliderHeight - 1.5, 3, Color.WHITE);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if(moveXAnim.isAnimating())
			return false;
		if(HoverUtil.hovered(mouseX, mouseY, x + width - 20.5, y - height + 19, 15.5, 16)) 
			moveXAnim.begin(x, GuiScreen.getScaledWidth() / 2 - 182, 1.1, () -> opened = false);
		else if (HoverUtil.hovered(mouseX, mouseY, centerX, centerY, radius))
			pointerDragging = true;
		else if (HoverUtil.hovered(mouseX, mouseY, brightnessX - 3, sy - 1, sliderWidth + 6, sliderHeight))
			brightnessDragging = true;
		else if (HoverUtil.hovered(mouseX, mouseY, saturationX - 3, sy - 1, sliderWidth + 6, sliderHeight))
			saturationDragging = true;
		else if (HoverUtil.hovered(mouseX, mouseY, alphaX - 3, sy - 1, sliderWidth + 6, sliderHeight))
			alphaDragging = true;
		copy.mouseClicked(mouseX, mouseY, mouseButton);
		paste.mouseClicked(mouseX, mouseY, mouseButton);
		return true;
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if(pointerDragging) {
			setPointerDraggingPos(mouseX, mouseY);
			return true;
		} else if(brightnessDragging) {
			brightness = (float)(calculateSliderY(mouseY) / sliderHeight);
			saveValue();
			return true;
		} else if(saturationDragging) {
			saturation = (float)(calculateSliderY(mouseY) / sliderHeight);
			calculatePointerPosition();
			saveValue();
			return true;
		} else if(alphaDragging) {
			alpha = (float)(calculateSliderY(mouseY) / sliderHeight);
			saveValue();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		pointerDragging = false;
		saturationDragging = false;
		brightnessDragging = false;
		alphaDragging = false;
		return false;
	}
	
	private void calculatePointerPosition() {
		double angle = Math.PI * 2 * hue;
		double h = saturation * radius;
		double x1, y1;
		if (angle >= 3 * Math.PI / 2) {
			x1 = Math.cos(angle - 3 * Math.PI / 2) * h;
			y1 = MathHelper.cathet(h, x1);
		} else if (angle >= Math.PI) {
			x1 = Math.sin(angle - Math.PI) * h;
			y1 = -MathHelper.cathet(h, x1);
		} else if (angle >= Math.PI / 2) {
			x1 = -Math.cos(angle - Math.PI / 2) * h;
			y1 = -MathHelper.cathet(h, x1);
		} else {
			x1 = -Math.sin(angle) * h;
			y1 = MathHelper.cathet(h, x1);
		}
		pointerX = radius - x1;
		pointerY = radius - y1;
	}
	
	private void calculateColor() {
		double h = Math.sqrt(MathHelper.sq(pointerX - radius) + MathHelper.sq(radius - pointerY));
		double c = pointerX - radius;
		double angle;
		if (pointerX >= radius && pointerY < radius)
			angle = Math.asin(c / h);
		else if (pointerX >= radius && pointerY > radius)
			angle = Math.PI / 2 + Math.acos(c / h);
		else if (pointerX < radius && pointerY >= radius)
			angle = Math.PI + Math.asin(-c / h);
		else
			angle = 3 * Math.PI / 2 + Math.acos(-c / h);
		hue = (float) (angle / (Math.PI * 2f));
		saturation = (float) (h / radius);
		saveValue();
	}

	private void setPointerDraggingPos(double mouseX, double mouseY) {
		if (!HoverUtil.hovered(mouseX, mouseY, centerX, centerY, radius)) {
			double dX = mouseX - centerX;
			double dY = mouseY - centerY;
			double angle;
			double x1, y1;
			if (mouseX >= centerX && mouseY >= centerY) {
				angle = -Math.atan(dX / dY);
				x1 = Math.sin(angle) * radius;
				y1 = MathHelper.cathet(radius, x1);
			} else if (mouseX >= centerX && mouseY < centerY) {
				angle = Math.atan(dY / dX);
				x1 = -Math.cos(angle) * radius;
				y1 = -MathHelper.cathet(radius, x1);
			} else if (mouseX < centerX && mouseY < centerY) {
				angle = Math.atan(dX / dY);
				x1 = Math.sin(angle) * radius;
				y1 = -MathHelper.cathet(radius, x1);
			} else {
				angle = -Math.atan(dY / dX);
				x1 = Math.cos(angle) * radius;
				y1 = MathHelper.cathet(radius, x1);
			}
			pointerX = radius - x1;
			pointerY = radius - y1;
		} else {
			pointerX = mouseX - xp;
			pointerY = sy - mouseY;
		}
		calculateColor();
	}
	
	@Override
	public void init() {
		if(!moveXAnim.isAnimating() && opened) {
			x = GuiScreen.getScaledWidth() / 2 + 15;
			update();
		}
		y = (GuiScreen.getScaledHeight() + height) / 2 - 15;
		centerY = y - 66;
		sy = y - 33;
		copy.setY(y - 7);
		paste.setY(y - 7);
	}
	
	@Override
	public void onClose() {
		mouseReleased(0, 0, 0);
	}

	private double calculateSliderY(double mouseY) {
		return mouseY >= sy - 1 ? 1 : Math.min(sliderHeight - 1, sy - 1 - mouseY);
	}

	private void saveValue() {
		setting.setValue(ColorHelper.injectAlpha(Color.getHSBColor(hue, saturation, brightness), alpha));
	}

	public boolean isOpened() {
		return opened;
	}
	
	public void parse(String content) {
		try {
			String[] comps = content.split(" ");
			if(comps.length >= 3) {
				int r = Integer.valueOf(comps[0]);
				int g = Integer.valueOf(comps[1]);
				int b = Integer.valueOf(comps[2]);
				int a = comps.length >= 4 ? Integer.valueOf(comps[3]) : 255;
				apply(new Color(r, g, b, a));
				saveValue();
			}
		} catch(Exception ex) {}
	}
	
	public String convert() {
		Color color = setting.getValue();
		return String.format("%d %d %d %d", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}

	public void toggle(ColorSetting setting) {
		if(opened) {
			if(this.setting == setting) {
				moveXAnim.begin(x, GuiScreen.getScaledWidth() / 2 - 182, 1.1, () -> opened = false);
			} else {
				moveXAnim.begin(x, GuiScreen.getScaledWidth() / 2 - 182, 1.1, () -> {
					create(setting);
					moveXAnim.begin(x, GuiScreen.getScaledWidth() / 2 + 15, 1.1);
				});
			}
		} else {
			if(this.setting == setting)
				opened = true;
			else
				create(setting);
			moveXAnim.begin(GuiScreen.getScaledWidth() / 2 - 182, 
					GuiScreen.getScaledWidth() / 2 + 15, 1.1);
		}
	}
	
	private void create(ColorSetting setting) {
		this.setting = setting;
		title = setting.getTitle();
		apply(setting.getValue());
		calculatePointerPosition();
		opened = true;
	}
	
	private void apply(Color color) {
		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		hue = hsb[0];
		saturation = hsb[1];
		brightness = hsb[2];
		alpha = color.getAlpha() / 255.0f;
		calculatePointerPosition();
	}
	
	private void update() {
		centerX = x + 47;
		xp = x + 14;
		brightnessX = x + 95 + 7;
		saturationX = x + 95 + 24.5;
		alphaX = x + 95 + 42;
		copy.setX(x + 11);
		paste.setX(x + 49);
	}
	
}
