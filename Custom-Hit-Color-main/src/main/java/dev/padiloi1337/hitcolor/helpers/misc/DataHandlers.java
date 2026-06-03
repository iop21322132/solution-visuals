package dev.padiloi1337.hitcolor.helpers.misc;

import java.awt.Color;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class DataHandlers {
	
	public static final DataHandler<Boolean> BOOLEAN = new DataHandler<Boolean>() {
		
		public Boolean read(JsonElement je) {
			return je.getAsBoolean();
		} 
		
		public JsonElement save(Boolean value) {
			return new JsonPrimitive(value);
		}
	};
	
	public static final DataHandler<Color> COLOR = new DataHandler<Color>() {
		
		public Color read(JsonElement je) {
			JsonArray arr = je.getAsJsonArray();
			return new Color(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt(), arr.get(3).getAsInt());
		}
		
		public JsonElement save(Color value) {
			JsonArray arr = new JsonArray();
			arr.add(value.getRed());
			arr.add(value.getGreen());
			arr.add(value.getBlue());
			arr.add(value.getAlpha());
			return arr;
		}
	};
	
	public static final DataHandler<Float> FLOAT = new DataHandler<Float>() {
		
		public Float read(JsonElement je) {
			return je.getAsFloat();
		} 
		
		public JsonElement save(Float value) {
			return new JsonPrimitive(value);
		}
	};
	
	public interface DataHandler<T> {
		
		JsonElement save(T value);
		
		T read(JsonElement je);
		
	}
	
}


