package dev.padiloi1337.hitcolor.ui;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.padiloi1337.hitcolor.Wrapper;

public abstract class Element implements Wrapper {
	
	protected double x, y, width, height;
	protected String title;
		
	protected Element(double width, double height, String title) {
		this.width = width;
		this.height = height;
		this.title = title;
	}
	
	public abstract void render(MatrixStack matrices, double mouseX, double mouseY);
			
	public abstract  boolean mouseClicked(double mouseX, double mouseY, int mouseButton);
	
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return false;
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return false;
	}
	
	public void init() {}
	
	public void onClose() {}
	
	public double getX() {
		return x;
	}
		
	public double getY() {
		return y;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public void setCoords(double x, double y) {
		this.x = x;
		this.y = y;
	}

}
