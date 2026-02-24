package com.crussion.moissanite.ui.data;

public final class UiInput extends UiValue<String> {
	private int maxLength = 120;

	public UiInput(String name, String defaultValue) {
		super(name, defaultValue == null ? "" : defaultValue);
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.INPUT;
	}

	@Override
	public void set(String value) {
		super.set(value == null ? "" : value);
	}

	public UiInput maxLength(int maxLength) {
		this.maxLength = Math.max(1, maxLength);
		return this;
	}

	public int maxLength() {
		return maxLength;
	}
}
