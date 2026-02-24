package com.crussion.moissanite.ui.data;

public final class UiSlider extends UiValue<Double> {
	private final double min;
	private final double max;
	private final double step;

	public UiSlider(String name, double min, double max, double defaultValue) {
		this(name, min, max, defaultValue, 0.01);
	}

	public UiSlider(String name, double min, double max, double defaultValue, double step) {
		super(name, clampAndSnap(defaultValue, min, max, sanitizeStep(step, min, max)));
		this.min = min;
		this.max = max;
		this.step = sanitizeStep(step, min, max);
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public double step() {
		return step;
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.SLIDER;
	}

	@Override
	public void set(Double value) {
		super.set(clampAndSnap(value, min, max, step));
	}

	public double snap(double value) {
		return clampAndSnap(value, min, max, step);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clampAndSnap(double value, double min, double max, double step) {
		double clamped = clamp(value, min, max);
		double steps = Math.round((clamped - min) / step);
		double snapped = min + (steps * step);
		double normalized = clamp(snapped, min, max);
		return Math.abs(normalized) < 1.0e-9 ? 0.0 : normalized;
	}

	private static double sanitizeStep(double step, double min, double max) {
		double range = Math.abs(max - min);
		if (!Double.isFinite(step) || step <= 0.0) {
			return 0.01;
		}
		if (range <= 0.0) {
			return step;
		}
		return Math.min(step, range);
	}
}
