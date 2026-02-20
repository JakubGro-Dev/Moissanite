package com.crussion.moissanite.ui.data;

public final class UiSlider extends UiValue<Double> {
	private final double min;
	private final double max;

	public UiSlider(String name, double min, double max, double defaultValue) {
		super(name, clamp(defaultValue, min, max));
		this.min = min;
		this.max = max;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.SLIDER;
	}

	@Override
	public void set(Double value) {
		super.set(clamp(value, min, max));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}

