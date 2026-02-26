package com.crussion.moissanite.ui.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;

public final class UiShapes {
	private static final Map<Integer, int[]> CORNER_INSETS_CACHE = new HashMap<>();
	private static final Map<Integer, int[]> CIRCLE_EXTENTS_CACHE = new HashMap<>();

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
		if (r <= 1) {
			graphics.fill(x, y, x + width, y + height, color);
			return;
		}

		int right = x + width;
		int bottom = y + height;
		int centerLeft = x + r;
		int centerRight = right - r;
		if (centerRight > centerLeft) {
			graphics.fill(centerLeft, y, centerRight, bottom, color);
		}

		int middleTop = y + r;
		int middleBottom = bottom - r;
		if (middleBottom > middleTop) {
			graphics.fill(x, middleTop, right, middleBottom, color);
		}

		int[] insets = cornerInsets(r);
		for (int row = 0; row < r; row++) {
			int inset = insets[row];
			int lineLeft = x + inset;
			int lineRight = right - inset;
			if (lineRight <= lineLeft) {
				continue;
			}

			int topY = y + row;
			int bottomY = bottom - row - 1;
			graphics.fill(lineLeft, topY, lineRight, topY + 1, color);
			if (bottomY != topY) {
				graphics.fill(lineLeft, bottomY, lineRight, bottomY + 1, color);
			}
		}
	}

	public static void fillRoundedRectVerticalGradient(GuiGraphics graphics,
													   int x,
													   int y,
													   int width,
													   int height,
													   int radius,
													   int topColor,
													   int bottomColor) {
		if (width <= 0 || height <= 0) {
			return;
		}
		if (height == 1 || topColor == bottomColor) {
			fillRoundedRect(graphics, x, y, width, height, radius, topColor);
			return;
		}
		int r = Math.min(Math.max(0, radius), Math.min(width / 2, height / 2));
		int[] insets = r > 1 ? cornerInsets(r) : null;
		int right = x + width;
		for (int row = 0; row < height; row++) {
			int lineY = y + row;
			int left = x;
			int lineRight = right;
			if (insets != null) {
				if (row < r) {
					int inset = insets[row];
					left += inset;
					lineRight -= inset;
				} else if (row >= height - r) {
					int inset = insets[height - row - 1];
					left += inset;
					lineRight -= inset;
				}
			}
			if (lineRight <= left) {
				continue;
			}
			float progress = (float) row / (float) (height - 1);
			int color = lerpColor(topColor, bottomColor, progress);
			graphics.fill(left, lineY, lineRight, lineY + 1, color);
		}
	}

	public static void fillRoundedOutline(GuiGraphics graphics,
										  int x,
										  int y,
										  int width,
										  int height,
										  int radius,
										  int thickness,
										  int outlineColor,
										  int innerColor) {
		if (width <= 0 || height <= 0) {
			return;
		}
		int border = Math.max(1, thickness);
		fillRoundedRect(graphics, x, y, width, height, radius, outlineColor);
		fillRoundedRect(
				graphics,
				x + border,
				y + border,
				width - (border * 2),
				height - (border * 2),
				Math.max(0, radius - border),
				innerColor
		);
	}

	public static void fillCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
		if (radius <= 0) {
			return;
		}
		if (radius == 1) {
			graphics.fill(centerX, centerY - 1, centerX + 1, centerY + 2, color);
			graphics.fill(centerX - 1, centerY, centerX + 2, centerY + 1, color);
			return;
		}
		int[] extents = circleExtents(radius);
		for (int dy = 0; dy <= radius; dy++) {
			int dx = extents[dy];
			int topY = centerY - dy;
			int bottomY = centerY + dy;
			graphics.fill(centerX - dx, topY, centerX + dx + 1, topY + 1, color);
			if (bottomY != topY) {
				graphics.fill(centerX - dx, bottomY, centerX + dx + 1, bottomY + 1, color);
			}
		}
	}

	private static int[] cornerInsets(int radius) {
		return CORNER_INSETS_CACHE.computeIfAbsent(radius, UiShapes::buildCornerInsets);
	}

	private static int[] buildCornerInsets(int radius) {
		int[] insets = new int[radius];
		int rSquared = radius * radius;
		for (int row = 0; row < radius; row++) {
			int dy = radius - row - 1;
			int dx = (int) Math.floor(Math.sqrt(Math.max(0, rSquared - (dy * dy))));
			insets[row] = radius - dx;
		}
		return insets;
	}

	private static int[] circleExtents(int radius) {
		return CIRCLE_EXTENTS_CACHE.computeIfAbsent(radius, UiShapes::buildCircleExtents);
	}

	private static int[] buildCircleExtents(int radius) {
		int[] extents = new int[radius + 1];
		int rSquared = radius * radius;
		for (int dy = 0; dy <= radius; dy++) {
			extents[dy] = (int) Math.floor(Math.sqrt(Math.max(0, rSquared - (dy * dy))));
		}
		return extents;
	}

	private static int lerpColor(int from, int to, float progress) {
		int fromA = (from >>> 24) & 0xFF;
		int fromR = (from >>> 16) & 0xFF;
		int fromG = (from >>> 8) & 0xFF;
		int fromB = from & 0xFF;

		int toA = (to >>> 24) & 0xFF;
		int toR = (to >>> 16) & 0xFF;
		int toG = (to >>> 8) & 0xFF;
		int toB = to & 0xFF;

		int a = Math.round(fromA + (toA - fromA) * progress);
		int r = Math.round(fromR + (toR - fromR) * progress);
		int g = Math.round(fromG + (toG - fromG) * progress);
		int b = Math.round(fromB + (toB - fromB) * progress);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
