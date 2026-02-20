package com.crussion.moissanite.ui.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class UiDropdown extends UiValue<String> {
	private final List<String> options = new ArrayList<>();

	public UiDropdown(String name, String defaultValue) {
		super(name, defaultValue == null ? "" : defaultValue);
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.DROPDOWN;
	}

	@Override
	public void set(String value) {
		super.set(value == null ? "" : value);
	}

	public void setOptions(Collection<String> values) {
		options.clear();
		if (values != null) {
			for (String value : values) {
				if (value == null) {
					continue;
				}
				String normalized = value.trim();
				if (normalized.isEmpty()) {
					continue;
				}
				if (!options.contains(normalized)) {
					options.add(normalized);
				}
			}
		}
		if (options.isEmpty()) {
			set("");
			return;
		}
		if (!options.contains(get())) {
			set(options.getFirst());
		}
	}

	public List<String> options() {
		return Collections.unmodifiableList(options);
	}

	public void selectNext() {
		if (options.isEmpty()) {
			set("");
			return;
		}
		int index = options.indexOf(get());
		if (index < 0) {
			set(options.getFirst());
			return;
		}
		set(options.get((index + 1) % options.size()));
	}

	public void selectPrevious() {
		if (options.isEmpty()) {
			set("");
			return;
		}
		int index = options.indexOf(get());
		if (index < 0) {
			set(options.getFirst());
			return;
		}
		int previous = index == 0 ? options.size() - 1 : index - 1;
		set(options.get(previous));
	}
}
