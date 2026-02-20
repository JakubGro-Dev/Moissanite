package com.crussion.moissanite.ui.data;

public final class UiKeybind extends UiValue<Integer> {
	public UiKeybind(String name, int defaultKey) {
		super(name, defaultKey);
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.KEYBIND;
	}
}
