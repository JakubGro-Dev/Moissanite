package com.crussion.moissanite.ui.layout;

public final class Anchors {
	private Anchors() {
	}

	public static int centerX(int screenWidth, int width) {
		return (screenWidth - width) / 2;
	}

	public static int centerY(int screenHeight, int height) {
		return (screenHeight - height) / 2;
	}
}

