package com.crussion.moissanite.ui.style;

public final class Colors {
	public static final int BACKDROP = color("BACKDROP", 8, 11, 20, 138);
	public static final int PANEL_SHADOW = color("PANEL_SHADOW", 6, 7, 14, 165);
	public static final int PANEL_BG = color("PANEL_BG", 44, 50, 77, 244);
	public static final int PANEL_BG_TOP = color("PANEL_BG_TOP", 38, 44, 71, 240);
	public static final int PANEL_BG_BOTTOM = color("PANEL_BG_BOTTOM", 27, 31, 50, 242);
	public static final int PANEL_OUTLINE = color("PANEL_OUTLINE", 74, 81, 118, 212);
	public static final int PANEL_HIGHLIGHT = color("PANEL_HIGHLIGHT", 154, 164, 212, 34);

	public static final int LEFT_BG = color("LEFT_BG", 45, 51, 79, 236);
	public static final int LEFT_BG_TOP = color("LEFT_BG_TOP", 42, 48, 77, 226);
	public static final int LEFT_BG_BOTTOM = color("LEFT_BG_BOTTOM", 31, 36, 59, 220);
	public static final int RIGHT_BG = color("RIGHT_BG", 45, 51, 79, 236);
	public static final int RIGHT_BG_TOP = color("RIGHT_BG_TOP", 38, 44, 70, 220);
	public static final int RIGHT_BG_BOTTOM = color("RIGHT_BG_BOTTOM", 30, 34, 56, 214);
	public static final int LIST_BG = color("LIST_BG", 41, 46, 72, 236);
	public static final int LIST_HOVER = color("LIST_HOVER", 171, 140, 255, 34);
	public static final int LIST_SELECTED = color("LIST_SELECTED", 163, 130, 247, 102);
	public static final int LIST_SELECTED_GLOW = color("LIST_SELECTED_GLOW", 188, 158, 255, 54);
	public static final int DIVIDER = color("DIVIDER", 100, 108, 145, 156);
	public static final int DIVIDER_GLOW = color("DIVIDER_GLOW", 183, 152, 255, 38);

	public static final int SECTION_BG = color("SECTION_BG", 45, 51, 79, 234);
	public static final int SECTION_BG_NESTED = color("SECTION_BG_NESTED", 40, 45, 70, 212);
	public static final int SECTION_OUTLINE = color("SECTION_OUTLINE", 81, 89, 124, 156);
	public static final int SECTION_OUTLINE_NESTED = color("SECTION_OUTLINE_NESTED", 77, 85, 119, 148);
	public static final int SECTION_HEADER_BG = color("SECTION_HEADER_BG", 45, 51, 79, 236);
	public static final int SECTION_HEADER_BG_NESTED = color("SECTION_HEADER_BG_NESTED", 44, 49, 76, 232);
	public static final int SECTION_HEADER_DIVIDER = color("SECTION_HEADER_DIVIDER", 92, 101, 138, 152);
	public static final int SECTION_HEADER_DIVIDER_NESTED = color("SECTION_HEADER_DIVIDER_NESTED", 83, 91, 126, 148);

	public static final int TEXT_PRIMARY = color("TEXT_PRIMARY", 240, 243, 255, 255);
	public static final int TEXT_MUTED = color("TEXT_MUTED", 166, 172, 202, 255);
	public static final int TEXT_SELECTED = color("TEXT_SELECTED", 236, 223, 255, 255);
	public static final int TEXT_SECTION_TITLE = color("TEXT_SECTION_TITLE", 249, 248, 255, 255);
	public static final int TEXT_SECTION_TITLE_NESTED = color("TEXT_SECTION_TITLE_NESTED", 232, 236, 255, 255);
	public static final int TEXT_ACCENT = color("TEXT_ACCENT", 208, 183, 255, 255);

	public static final int ACCENT = color("ACCENT", 168, 132, 255, 255);
	public static final int ACCENT_SOFT = color("ACCENT_SOFT", 168, 132, 255, 182);
	public static final int ACCENT_DIM = color("ACCENT_DIM", 132, 105, 204, 180);

	public static final int SWITCH_TRACK = color("SWITCH_TRACK", 73, 80, 112, 240);
	public static final int SWITCH_TRACK_ON = color("SWITCH_TRACK_ON", 166, 132, 248, 245);
	public static final int SWITCH_DOT_OFF = color("SWITCH_DOT_OFF", 232, 236, 252, 248);
	public static final int SWITCH_DOT_ON = color("SWITCH_DOT_ON", 248, 251, 255, 255);
	public static final int SWITCH_DOT_HIGHLIGHT = color("SWITCH_DOT_HIGHLIGHT", 255, 255, 255, 170);

	public static final int SLIDER_TRACK = color("SLIDER_TRACK", 78, 84, 114, 194);
	public static final int SLIDER_ACTIVE = color("SLIDER_ACTIVE", 171, 139, 255, 236);
	public static final int SLIDER_KNOB = color("SLIDER_KNOB", 246, 248, 255, 252);
	public static final int SLIDER_VALUE_BG = color("SLIDER_VALUE_BG", 55, 62, 93, 232);
	public static final int SLIDER_VALUE_OUTLINE = color("SLIDER_VALUE_OUTLINE", 97, 106, 147, 170);

	public static final int INPUT_BG = color("INPUT_BG", 55, 62, 93, 232);
	public static final int INPUT_OUTLINE = color("INPUT_OUTLINE", 93, 102, 141, 164);
	public static final int INPUT_OUTLINE_FOCUS = color("INPUT_OUTLINE_FOCUS", 177, 145, 255, 250);
	public static final int BUTTON_BG = color("BUTTON_BG", 55, 62, 93, 232);
	public static final int BUTTON_HOVER = color("BUTTON_HOVER", 63, 71, 105, 236);
	public static final int BUTTON_OUTLINE = color("BUTTON_OUTLINE", 93, 102, 141, 166);
	public static final int BUTTON_OUTLINE_HOVER = color("BUTTON_OUTLINE_HOVER", 175, 146, 255, 248);

	private Colors() {
	}

	private static int color(String token, int red, int green, int blue, int alpha) {
		return XmlUiTheme.color(token, rgba(red, green, blue, alpha));
	}

	public static int rgba(int red, int green, int blue, int alpha) {
		int a = alpha & 0xFF;
		int r = red & 0xFF;
		int g = green & 0xFF;
		int b = blue & 0xFF;
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
