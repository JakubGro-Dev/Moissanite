package com.crussion.moissanite.ui.data;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UiCatalog {
	private static final Map<String, UiCategory> CATEGORIES = new LinkedHashMap<>();

	private UiCatalog() {
	}

	public static UiCategory category(String name) {
		return CATEGORIES.computeIfAbsent(name, UiCategory::new);
	}

	public static Collection<UiCategory> categories() {
		return Collections.unmodifiableCollection(CATEGORIES.values());
	}

	public static UiCategory get(String name) {
		return CATEGORIES.get(name);
	}

	public static void clear() {
		CATEGORIES.clear();
	}
}

