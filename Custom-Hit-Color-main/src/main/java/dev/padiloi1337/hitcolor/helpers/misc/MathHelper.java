package dev.padiloi1337.hitcolor.helpers.misc;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathHelper {
	
	public static double round(double value, int scale) {
		try {
			return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
		} catch(Exception ex) {
			return 1;
		}
	}
	
	public static float round(float value, int scale) {
		try {
			return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).floatValue();
		} catch(Exception ex) {
			return 1;
		}
	}
	
	public static double clamp(double v, double min, double max) {
		return Math.min(max, Math.max(min, v));
	}
	
	public static float clamp(float v, float min, float max) {
		return Math.min(max, Math.max(min, v));
	}
	
	public static int clamp(int v, int min, int max) {
		return Math.min(max, Math.max(min, v));
	}
	
	public static double cathet(double h, double a) {
		return Math.sqrt(sq(h) - sq(a));
	}

	public static double sq(double a) {
		return a * a;
	}

}
