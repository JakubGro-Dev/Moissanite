package com.crussion.moissanite.ui.screen;

import com.crussion.moissanite.features.visual.RenderImageOnScreen;
import com.crussion.moissanite.input.UiKeybinds;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ImageOnScreenMoveScreen extends Screen {
	private static final int BACKGROUND_DIM = 0xAA000000;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final double SCALE_STEP = 0.01;

	private final Screen returnScreen;
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	public ImageOnScreenMoveScreen(Screen returnScreen) {
		super(Component.literal("Move Image On Screen"));
		this.returnScreen = returnScreen;
	}

	@Override
	public void removed() {
		RenderImageOnScreen.setMoveModeActive(false);
		super.removed();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, BACKGROUND_DIM);

		RenderImageOnScreen.DrawState drawState = currentDrawState();
		if (drawState == null) {
			graphics.drawCenteredString(this.font, Component.literal("Select an image to move"), this.width / 2, this.height / 2, TEXT_COLOR);
			return;
		}

		RenderImageOnScreen.Position position = currentPosition(drawState);
		int x = position.x();
		int y = position.y();
		RenderImageOnScreen.drawImage(graphics, drawState, x, y);

		int infoX = Math.max(8, x - 120);
		int infoY = Math.max(8, y + (drawState.drawHeight() / 2) - this.font.lineHeight);
		graphics.drawString(this.font, "x " + x, infoX, infoY, TEXT_COLOR, false);
		graphics.drawString(this.font, "y " + y, infoX, infoY + this.font.lineHeight + 2, TEXT_COLOR, false);
		graphics.drawString(this.font, "scale " + formatScale(RenderImageOnScreen.getConfiguredScale()), infoX, infoY + (this.font.lineHeight + 2) * 2, TEXT_COLOR, false);
		graphics.drawString(this.font, "Drag to move | Scroll to scale | Esc to close", 8, this.height - this.font.lineHeight - 8, TEXT_COLOR, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
		if (event.button() != 0) {
			return true;
		}

		RenderImageOnScreen.DrawState drawState = currentDrawState();
		if (drawState == null) {
			return true;
		}

		RenderImageOnScreen.Position position = currentPosition(drawState);
		int x = position.x();
		int y = position.y();
		if (isInside(event.x(), event.y(), x, y, drawState.drawWidth(), drawState.drawHeight())) {
			this.dragging = true;
			this.setDragging(true);
			this.dragOffsetX = (int) Math.round(event.x() - x);
			this.dragOffsetY = (int) Math.round(event.y() - y);
			return true;
		}

		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (!this.dragging || event.button() != 0) {
			return true;
		}
		RenderImageOnScreen.DrawState drawState = currentDrawState();
		if (drawState == null) {
			return true;
		}
		int nextX = (int) Math.round(event.x() - this.dragOffsetX);
		int nextY = (int) Math.round(event.y() - this.dragOffsetY);
		RenderImageOnScreen.Position clamped = RenderImageOnScreen.clampPosition(drawState, this.width, this.height, nextX, nextY);
		RenderImageOnScreen.setConfiguredPosition(clamped.x(), clamped.y());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0) {
			this.dragging = false;
			this.setDragging(false);
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
		if (yDelta != 0.0) {
			RenderImageOnScreen.adjustConfiguredScale(yDelta * SCALE_STEP);
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (UiKeybinds.isToggleKey(event)) {
			closeToGame();
			return true;
		}
		if (event.key() == InputConstants.KEY_ESCAPE) {
			onClose();
			return true;
		}
		return false;
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(returnScreen);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private RenderImageOnScreen.DrawState currentDrawState() {
		return RenderImageOnScreen.getCurrentDrawState(this.width, this.height);
	}

	private RenderImageOnScreen.Position currentPosition(RenderImageOnScreen.DrawState drawState) {
		return RenderImageOnScreen.getVisibleConfiguredPosition(drawState, this.width, this.height, false);
	}

	private void closeToGame() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(null);
		}
	}

	private static String formatScale(double scale) {
		double rounded = Math.round(scale * 100.0) / 100.0;
		if (rounded == (long) rounded) {
			return Long.toString((long) rounded);
		}
		return Double.toString(rounded);
	}
}
