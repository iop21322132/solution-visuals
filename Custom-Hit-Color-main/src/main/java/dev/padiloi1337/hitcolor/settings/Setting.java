package dev.padiloi1337.hitcolor.settings;

import com.google.gson.JsonElement;

import dev.padiloi1337.hitcolor.helpers.misc.DataHandlers.DataHandler;
import dev.padiloi1337.hitcolor.ui.Element;

public abstract class Setting<T> extends Element {
	
	private DataHandler<T> handler;
	protected T value;
	
	protected Setting(DataHandler<T> handler, String title, T defaultValue) {
		super(158, 16, title);
		this.handler = handler;
		this.value = defaultValue;
	}

	public T getValue() {
		return value;
	}
	
	public void setValue(T value) {
		this.value = value;
	}
	
	public void read(JsonElement je) {
		value = handler.read(je);
	}
	
	public JsonElement save() {
		return handler.save(value);
	}
	
}
