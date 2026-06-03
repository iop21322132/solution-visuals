package dev.padiloi1337.hitcolor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import dev.padiloi1337.hitcolor.settings.Setting;

public final class ConfigManager implements Wrapper {
	
	public static final String CONFIG_FILE_NAME = "";
	public static final File CONFIG_DIRECTORY = new File(clean(MC.gameDir.getAbsoluteFile()), "config/");
	public static final File CONFIG_FILE_PATH = new File(CONFIG_DIRECTORY, CONFIG_FILE_NAME);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final JsonParser GSON_PARSER = new JsonParser();
	
	public static void save() {
		JsonObject cfg = new JsonObject();;
		if(!CONFIG_FILE_PATH.exists()) {
			try {
				CONFIG_FILE_PATH.createNewFile();
			} catch (IOException ex) {ex.printStackTrace();}
		} 
		for(Setting<?> setting : CLIENT.settings.values) {
			cfg.add(setting.getTitle(), setting.save());
		}
		writeJson(cfg);
	}
	
	public static void load() {
		if(!CONFIG_FILE_PATH.exists())
			return;
		JsonObject cfg = readAsJson(CONFIG_FILE_PATH);
		for(Entry<String, JsonElement> entry : cfg.entrySet()) {
			Setting<?> setting = CLIENT.settings.values.stream()
					.filter(s -> s.getTitle().equalsIgnoreCase(entry.getKey())).findFirst().orElse(null);
			if(setting != null)
				setting.read(entry.getValue());
		}
	}
	
	public static JsonObject readAsJson(File file) {
		StringBuilder content = new StringBuilder();
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
			bufferedReader.lines().forEach(l -> content.append(l));
			bufferedReader.close();
		} catch (IOException ex) {ex.printStackTrace();}
		return parse(content.toString());
	}

	public static void writeJson(JsonObject jo) {
		String content = GSON.toJson(jo);
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(CONFIG_FILE_PATH));
			bufferedWriter.write(content);
			bufferedWriter.close();
		} catch (IOException ex) {ex.printStackTrace();}
	}

	public static JsonObject parse(String str) {
		try {
			return GSON_PARSER.parse(str).getAsJsonObject();
		} catch (JsonParseException | IllegalStateException ex) {
			ex.printStackTrace();
			return new JsonObject();
		}
	}
	
	public static File clean(File file) {
		try {
			return file.getCanonicalFile();
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

}
