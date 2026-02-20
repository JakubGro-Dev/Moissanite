package com.crussion.moissanite.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.crussion.moissanite.ui.data.UiCategory;
import com.crussion.moissanite.ui.data.UiButton;
import com.crussion.moissanite.ui.data.UiDropdown;
import com.crussion.moissanite.ui.data.UiEntry;
import com.crussion.moissanite.ui.data.UiInput;
import com.crussion.moissanite.ui.data.UiKeybind;
import com.crussion.moissanite.ui.data.UiNumber;
import com.crussion.moissanite.ui.data.UiSection;
import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.ui.data.UiSwitch;
import com.crussion.moissanite.ui.data.UiValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
	private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

	private final Path path;
	private final Gson gson;
	private final Map<String, Entry<?>> entries = new LinkedHashMap<>();
	private boolean loading;

	public ConfigManager(String filename) {
		this.path = FabricLoader.getInstance().getConfigDir().resolve(filename);
		this.gson = new GsonBuilder().setPrettyPrinting().create();
	}

	public void load() {
		JsonObject root = readJson();
		loading = true;
		for (Entry<?> entry : entries.values()) {
			JsonElement element = root.get(entry.key);
			entry.applyJson(element);
		}
		loading = false;
	}

	public void save() {
		JsonObject root = new JsonObject();
		for (Entry<?> entry : entries.values()) {
			root.add(entry.key, entry.toJson());
		}
		writeJson(root);
	}

	public <T> void bind(String key, UiValue<T> value, ValueAdapter<T> adapter) {
		if (key == null || key.isBlank() || value == null || adapter == null || entries.containsKey(key)) {
			return;
		}
		Entry<T> entry = new Entry<>(key, value, adapter);
		entries.put(key, entry);
		value.bind(val -> onValueChanged());
	}

	public void bind(String key, UiValue<?> value) {
		if (value == null) {
			return;
		}
		if (value instanceof UiSwitch) {
			bindTyped(key, (UiValue<Boolean>) value, ConfigAdapters.BOOLEAN);
			return;
		}
		if (value instanceof UiNumber || value instanceof UiButton || value instanceof UiKeybind) {
			bindTyped(key, (UiValue<Integer>) value, ConfigAdapters.INTEGER);
			return;
		}
		if (value instanceof UiSlider) {
			bindTyped(key, (UiValue<Double>) value, ConfigAdapters.DOUBLE);
			return;
		}
		if (value instanceof UiInput) {
			bindTyped(key, (UiValue<String>) value, ConfigAdapters.STRING);
			return;
		}
		if (value instanceof UiDropdown) {
			bindTyped(key, (UiValue<String>) value, ConfigAdapters.STRING);
			return;
		}
	}

	public void bindAll(Iterable<UiCategory> categories) {
		if (categories == null) {
			return;
		}
		Map<String, Integer> keyCounts = new LinkedHashMap<>();
		for (UiCategory category : categories) {
			if (category == null) {
				continue;
			}
			String categoryKey = normalizeSegment(category.name());
			for (UiSection section : category.sections()) {
				if (section == null) {
					continue;
				}
				String sectionKey = normalizeSegment(section.name());
				boolean includeSection = !sectionKey.equals(categoryKey);
				for (UiEntry<?> entry : section.entries()) {
					if (!(entry instanceof UiValue<?> value)) {
						continue;
					}
					String entryKey = normalizeSegment(entry.name());
					String key = includeSection
							? categoryKey + "." + sectionKey + "." + entryKey
							: categoryKey + "." + entryKey;
					key = dedupeKey(key, keyCounts);
					bind(key, value);
				}
			}
		}
	}

	private <T> void bindTyped(String key, UiValue<T> value, ValueAdapter<T> adapter) {
		bind(key, value, adapter);
	}

	private void onValueChanged() {
		if (!loading) {
			save();
		}
	}

	private JsonObject readJson() {
		if (!Files.exists(path)) {
			return new JsonObject();
		}
		try {
			String content = Files.readString(path, StandardCharsets.UTF_8);
			JsonElement parsed = gson.fromJson(content, JsonElement.class);
			return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
		} catch (Exception ignored) {
			return new JsonObject();
		}
	}

	private void writeJson(JsonObject root) {
		try {
			Files.createDirectories(path.getParent());
			String content = gson.toJson(root);
			Files.writeString(path, content, StandardCharsets.UTF_8);
		} catch (IOException ignored) {
		}
	}

	private static String dedupeKey(String key, Map<String, Integer> keyCounts) {
		int next = keyCounts.getOrDefault(key, 0) + 1;
		keyCounts.put(key, next);
		return next == 1 ? key : key + "_" + next;
	}

	private static String normalizeSegment(String raw) {
		if (raw == null || raw.isBlank()) {
			return "unnamed";
		}
		String normalized = NON_ALNUM.matcher(raw.toLowerCase()).replaceAll("_");
		normalized = trimUnderscores(normalized);
		return normalized.isEmpty() ? "unnamed" : normalized;
	}

	private static String trimUnderscores(String text) {
		int start = 0;
		int end = text.length();
		while (start < end && text.charAt(start) == '_') {
			start++;
		}
		while (end > start && text.charAt(end - 1) == '_') {
			end--;
		}
		return text.substring(start, end);
	}

	private static final class Entry<T> {
		private final String key;
		private final UiValue<T> value;
		private final ValueAdapter<T> adapter;

		private Entry(String key, UiValue<T> value, ValueAdapter<T> adapter) {
			this.key = key;
			this.value = value;
			this.adapter = adapter;
		}

		private JsonElement toJson() {
			return adapter.toJson(value.get());
		}

		private void applyJson(JsonElement element) {
			T next = adapter.fromJson(element);
			if (next != null || adapter.isNullable()) {
				value.set(next);
			}
		}
	}
}
