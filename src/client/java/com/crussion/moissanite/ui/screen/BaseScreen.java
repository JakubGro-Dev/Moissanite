package com.crussion.moissanite.ui.screen;

import com.crussion.moissanite.ui.layout.Layouts;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.style.Theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class BaseScreen extends Screen {
	protected Layouts.Layout layout;

	protected BaseScreen(Component title) {
		super(title);
	}

	@Override
	protected void init() {
		this.layout = Layouts.computeMain(width, height);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderPanel(graphics);
		renderUnderlay(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		renderOverlay(graphics, mouseX, mouseY, partialTick);
	}

	protected void renderPanel(GuiGraphics graphics) {
		if (layout == null) {
			return;
		}
		Layouts.Rect outer = layout.outer();
		drawRoundedPanel(graphics, outer.x(), outer.y(), outer.width(), outer.height());
	}

	protected void renderUnderlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	protected void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	protected void drawRoundedPanel(GuiGraphics graphics, int x, int y, int width, int height) {
		int radius = Math.max(0, Theme.PANEL_RADIUS);
		fillRoundedRect(graphics, x, y, width, height, radius, Colors.PANEL_OUTLINE);
		fillRoundedRect(graphics, x + 1, y + 1, width - 2, height - 2, Math.max(0, radius - 1), Colors.PANEL_BG);
	}

	protected static void fillRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
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

	protected static void fillCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
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

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

