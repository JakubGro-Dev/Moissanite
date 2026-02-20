package com.crussion.moissanite.ui.data;

public final class UiInput extends UiValue<String> {
	public UiInput(String name, String defaultValue) {
		super(name, defaultValue);
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.INPUT;
	}
}

