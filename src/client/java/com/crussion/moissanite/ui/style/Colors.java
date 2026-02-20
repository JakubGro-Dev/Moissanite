package com.crussion.moissanite.ui.style;

public final class Colors {
	public static final int PANEL_BG = rgba(18, 26, 40, 200);
	public static final int PANEL_OUTLINE = rgba(90, 140, 200, 140);
	public static final int LEFT_BG = rgba(16, 22, 34, 170);
	public static final int RIGHT_BG = rgba(14, 20, 32, 165);
	public static final int LIST_BG = rgba(10, 16, 26, 150);
	public static final int LIST_HOVER = rgba(140, 190, 240, 40);
	public static final int LIST_SELECTED = rgba(90, 150, 220, 150);
	public static final int DIVIDER = rgba(90, 140, 200, 90);
	public static final int DIVIDER_GLOW = rgba(120, 190, 255, 28);

	public static final int TEXT_PRIMARY = rgba(236, 244, 255, 255);
	public static final int TEXT_MUTED = rgba(170, 190, 210, 255);
	public static final int TEXT_SELECTED = rgba(210, 235, 255, 255);

	public static final int SWITCH_TRACK = rgba(40, 50, 70, 180);
	public static final int SWITCH_DOT_OFF = rgba(220, 80, 80, 240);
	public static final int SWITCH_DOT_ON = rgba(90, 220, 140, 240);
	public static final int SWITCH_DOT_HIGHLIGHT = rgba(240, 250, 255, 210);

	public static final int SLIDER_TRACK = rgba(80, 110, 150, 120);
	public static final int SLIDER_ACTIVE = rgba(120, 200, 255, 210);
	public static final int SLIDER_KNOB = rgba(210, 235, 255, 235);

	public static final int INPUT_BG = rgba(18, 26, 40, 200);
	public static final int INPUT_OUTLINE = rgba(110, 170, 230, 140);
	public static final int BUTTON_BG = rgba(28, 38, 56, 210);
	public static final int BUTTON_HOVER = rgba(42, 66, 92, 230);
	public static final int BUTTON_OUTLINE = rgba(120, 180, 240, 170);

	private Colors() {
	}

	public static int rgba(int red, int green, int blue, int alpha) {
		int a = alpha & 0xFF;
		int r = red & 0xFF;
		int g = green & 0xFF;
		int b = blue & 0xFF;
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}

