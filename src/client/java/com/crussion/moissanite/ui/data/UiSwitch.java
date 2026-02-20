package com.crussion.moissanite.ui.data;

public final class UiSwitch extends UiValue<Boolean> {
	public UiSwitch(String name, boolean defaultValue) {
		super(name, defaultValue);
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.SWITCH;
	}

	public void toggle() {
		set(!get());
	}
}

