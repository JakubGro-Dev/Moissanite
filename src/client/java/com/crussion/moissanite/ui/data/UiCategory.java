package com.crussion.moissanite.ui.data;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UiCategory {
	private final String name;
	private final Map<String, UiSection> sections = new LinkedHashMap<>();

	UiCategory(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	public Collection<UiSection> sections() {
		return Collections.unmodifiableCollection(sections.values());
	}

	public Collection<UiEntry<?>> entries() {
		return defaultSection().entries();
	}

	public UiSwitch toggle(String entryName, boolean defaultValue) {
		return defaultSection().toggle(entryName, defaultValue);
	}

	public UiButton button(String entryName) {
		return defaultSection().button(entryName);
	}

	public UiButton button(String entryName, java.util.function.Consumer<UiButton> onPress) {
		return defaultSection().button(entryName, onPress);
	}

	public UiButton button(String entryName, String buttonText) {
		return defaultSection().button(entryName, buttonText);
	}

	public UiButton button(String entryName, String buttonText, java.util.function.Consumer<UiButton> onPress) {
		return defaultSection().button(entryName, buttonText, onPress);
	}

	public UiInput input(String entryName, String defaultValue) {
		return defaultSection().input(entryName, defaultValue);
	}

	public UiDropdown dropdown(String entryName, String defaultValue) {
		return defaultSection().dropdown(entryName, defaultValue);
	}

	public UiKeybind keybind(String entryName, int defaultKey) {
		return defaultSection().keybind(entryName, defaultKey);
	}

	public UiSlider slider(String entryName, double min, double max, double defaultValue) {
		return defaultSection().slider(entryName, min, max, defaultValue);
	}

	public UiNumber number(String entryName, int min, int max, int defaultValue) {
		return defaultSection().number(entryName, min, max, defaultValue);
	}

	public UiEntry<?> get(String entryName) {
		return defaultSection().get(entryName);
	}

	public UiSection section(String sectionName) {
		return sections.computeIfAbsent(sectionName, UiSection::new);
	}

	private UiSection defaultSection() {
		return section(this.name);
	}
}
