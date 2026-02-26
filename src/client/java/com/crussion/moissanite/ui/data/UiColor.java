package com.crussion.moissanite.ui.data;

public final class UiColor extends UiValue<Integer> {
	public UiColor(String name, int defaultArgb) {
		super(name, sanitize(defaultArgb));
	}

	public UiColor(String name, int red, int green, int blue, int alpha) {
		this(name, argb(red, green, blue, alpha));
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.COLOR;
	}

	@Override
	public void set(Integer value) {
		super.set(sanitize(value == null ? 0xFFFFFFFF : value));
	}

	public int argb() {
		return sanitize(get() == null ? 0xFFFFFFFF : get());
	}

	public int red() {
		return (argb() >>> 16) & 0xFF;
	}

	public int green() {
		return (argb() >>> 8) & 0xFF;
	}

	public int blue() {
		return argb() & 0xFF;
	}

	public int alpha() {
		return (argb() >>> 24) & 0xFF;
	}

	public void setRed(int red) {
		set(argb(red, green(), blue(), alpha()));
	}

	public void setGreen(int green) {
		set(argb(red(), green, blue(), alpha()));
	}

	public void setBlue(int blue) {
		set(argb(red(), green(), blue, alpha()));
	}

	public void setAlpha(int alpha) {
		set(argb(red(), green(), blue(), alpha));
	}

	public static int argb(int red, int green, int blue, int alpha) {
		int a = clampChannel(alpha);
		int r = clampChannel(red);
		int g = clampChannel(green);
		int b = clampChannel(blue);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int sanitize(int argb) {
		return argb(
				(argb >>> 16) & 0xFF,
				(argb >>> 8) & 0xFF,
				argb & 0xFF,
				(argb >>> 24) & 0xFF);
	}

	private static int clampChannel(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
