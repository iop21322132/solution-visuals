package dev.padiloi1337.hitcolor.helpers.misc;

public class HoverUtil {
	
    public static boolean hovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX > x && mouseY < y && mouseX < x + width && mouseY > y - height;
    }
    
    public static boolean hovered(double mouseX, double mouseY, double centerX, double centerY, double radius) {
    	return Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2)) <= radius;
    }
    
}
