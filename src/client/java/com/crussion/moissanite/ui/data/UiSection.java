package com.crussion.moissanite.ui.data;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UiSection {
	private final String name;
	private final Map<String, UiEntry<?>> entries = new LinkedHashMap<>();

	UiSection(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	public Collection<UiEntry<?>> entries() {
		return Collections.unmodifiableCollection(entries.values());
	}

	public UiSwitch toggle(String entryName, boolean defaultValue) {
		return register(new UiSwitch(entryName, defaultValue));
	}

	public UiButton button(String entryName) {
		return register(new UiButton(entryName));
	}

	public UiButton button(String entryName, java.util.function.Consumer<UiButton> onPress) {
		return register(new UiButton(entryName, onPress));
	}

	public UiButton button(String entryName, String buttonText) {
		return register(new UiButton(entryName, buttonText));
	}

	public UiButton button(String entryName, String buttonText, java.util.function.Consumer<UiButton> onPress) {
		return register(new UiButton(entryName, buttonText, onPress));
	}

	public UiInput input(String entryName, String defaultValue) {
		return register(new UiInput(entryName, defaultValue));
	}

	public UiDropdown dropdown(String entryName, String defaultValue) {
		return register(new UiDropdown(entryName, defaultValue));
	}

	public UiKeybind keybind(String entryName, int defaultKey) {
		return register(new UiKeybind(entryName, defaultKey));
	}

	public UiSlider slider(String entryName, double min, double max, double defaultValue) {
		return register(new UiSlider(entryName, min, max, defaultValue));
	}

	public UiNumber number(String entryName, int min, int max, int defaultValue) {
		return register(new UiNumber(entryName, min, max, defaultValue));
	}

	public UiEntry<?> get(String entryName) {
		return entries.get(entryName);
	}

	@SuppressWarnings("unchecked")
	private <T extends UiEntry<?>> T register(T entry) {
		UiEntry<?> existing = entries.putIfAbsent(entry.name(), entry);
		return existing == null ? entry : (T) existing;
	}
}
