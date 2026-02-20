package com.crussion.moissanite.ui.data;

public final class UiNumber extends UiValue<Integer> {
	private final int min;
	private final int max;

	public UiNumber(String name, int min, int max, int defaultValue) {
		super(name, clamp(defaultValue, min, max));
		this.min = min;
		this.max = max;
	}

	public int min() {
		return min;
	}

	public int max() {
		return max;
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.NUMBER;
	}

	@Override
	public void set(Integer value) {
		super.set(clamp(value, min, max));
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}

