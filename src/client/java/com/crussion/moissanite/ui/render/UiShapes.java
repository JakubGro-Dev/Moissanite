package com.crussion.moissanite.ui.render;

import net.minecraft.client.gui.GuiGraphics;

public final class UiShapes {
	private UiShapes() {
	}

	public static void fillRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}
		if (radius <= 0) {
			graphics.fill(x, y, x + width, y + height, color);
			return;
		}

		int r = Math.min(radius, Math.min(width / 2, height / 2));
		int right = x + width;
		int bottom = y + height;
		int rSquared = r * r;

		for (int row = 0; row < height; row++) {
			int lineY = y + row;
			int left = x;
			int lineRight = right;
			if (row < r) {
				int dy = r - row - 1;
				int dx = (int) Math.floor(Math.sqrt(rSquared - (dy * dy)));
				left = x + r - dx;
				lineRight = right - (r - dx);
			} else if (row >= height - r) {
				int dy = row - (height - r);
				int dx = (int) Math.floor(Math.sqrt(rSquared - (dy * dy)));
				left = x + r - dx;
				lineRight = right - (r - dx);
			}
			if (lineRight > left) {
				graphics.fill(left, lineY, lineRight, lineY + 1, color);
			}
		}
	}

	public static void fillCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
		if (radius <= 0) {
			return;
		}
		int rSquared = radius * radius;
		for (int dy = -radius; dy <= radius; dy++) {
			int y = centerY + dy;
			int dx = (int) Math.floor(Math.sqrt(rSquared - (dy * dy)));
			graphics.fill(centerX - dx, y, centerX + dx + 1, y + 1, color);
		}
	}
}
