package com.crussion.moissanite.ui.style;

public final class Theme {
	public static final int OUTER_PADDING = dim("OUTER_PADDING", 14);
	public static final int INNER_PADDING = dim("INNER_PADDING", 12);
	public static final int SECTION_GAP = dim("SECTION_GAP", 14);
	public static final int INPUT_HEIGHT = dim("INPUT_HEIGHT", 24);
	public static final int INPUT_LIST_GAP = dim("INPUT_LIST_GAP", 10);
	public static final int LIST_ITEM_HEIGHT = dim("LIST_ITEM_HEIGHT", 24);
	public static final int PANEL_RADIUS = dim("PANEL_RADIUS", 14);
	public static final int SECTION_RADIUS = dim("SECTION_RADIUS", 10);
	public static final int CONTROL_RADIUS = dim("CONTROL_RADIUS", 8);
	public static final int SWITCH_RADIUS = dim("SWITCH_RADIUS", 10);

	private Theme() {
	}

	private static int dim(String token, int fallback) {
		return XmlUiTheme.dimension(token, fallback);
	}
}
