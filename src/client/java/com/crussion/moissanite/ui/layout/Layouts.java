package com.crussion.moissanite.ui.layout;

import com.crussion.moissanite.ui.style.Theme;

public final class Layouts {
	public record Rect(int x, int y, int width, int height) {
		public int right() {
			return x + width;
		}

		public int bottom() {
			return y + height;
		}

		public Rect inset(int pad) {
			return new Rect(x + pad, y + pad, width - (pad * 2), height - (pad * 2));
		}
	}

	public record Layout(Rect outer, Rect left, Rect right, Rect input, Rect list, Rect search) {
	}

	private Layouts() {
	}

	public static Layout computeMain(int screenWidth, int screenHeight) {
		int maxWidth = screenWidth - 24;
		int maxHeight = screenHeight - 24;
		int outerWidth = clamp((int) (screenWidth * 0.78f), 260, maxWidth);
		int outerHeight = clamp((int) (screenHeight * 0.72f), 200, maxHeight);

		int outerX = Anchors.centerX(screenWidth, outerWidth);
		int outerY = Anchors.centerY(screenHeight, outerHeight);
		Rect outer = new Rect(outerX, outerY, outerWidth, outerHeight);

		Rect inner = outer.inset(Theme.OUTER_PADDING);
		int searchHeight = Theme.INPUT_HEIGHT;
		int searchGap = Theme.INPUT_LIST_GAP;
		int contentHeight = Math.max(0, inner.height() - searchHeight - searchGap);
		Rect content = new Rect(inner.x(), inner.y(), inner.width(), contentHeight);
		Rect search = new Rect(inner.x(), inner.y() + contentHeight + searchGap, inner.width(), searchHeight);

		int gap = Theme.SECTION_GAP;
		int leftWidth = clamp((int) (content.width() * 0.10f), 140, content.width() - gap - 140);
		int rightWidth = content.width() - leftWidth - gap;

		Rect left = new Rect(content.x(), content.y(), leftWidth, content.height());
		Rect right = new Rect(left.right() + gap, content.y(), rightWidth, content.height());

		Rect leftInner = left.inset(Theme.INNER_PADDING);
		Rect input = new Rect(leftInner.x(), leftInner.y(), leftInner.width(), Theme.INPUT_HEIGHT);
		int listY = input.bottom() + Theme.INPUT_LIST_GAP;
		int listHeight = leftInner.bottom() - listY;
		Rect list = new Rect(leftInner.x(), listY, leftInner.width(), Math.max(0, listHeight));

		return new Layout(outer, left, right, input, list, search);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
