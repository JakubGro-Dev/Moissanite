package com.crussion.moissanite.ui.screen;

import com.crussion.moissanite.ui.layout.Layouts;
import com.crussion.moissanite.ui.render.UiShapes;
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
		graphics.fill(0, 0, width, height, Colors.BACKDROP);
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
		UiShapes.fillRoundedRect(graphics, x, y + 2, width, height, radius, Colors.PANEL_SHADOW);
		UiShapes.fillRoundedOutline(graphics, x, y, width, height, radius, 1, Colors.PANEL_OUTLINE, Colors.PANEL_BG);
		UiShapes.fillRoundedRect(graphics, x + 2, y + 2, width - 4, 2, Math.max(0, radius - 2), Colors.PANEL_HIGHLIGHT);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
