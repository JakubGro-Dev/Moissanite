package com.crussion.moissanite.ui.screen;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.crussion.moissanite.input.UiKeybinds;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MoveElementScreen<S> extends Screen {
	private static final int BACKGROUND_DIM = 0xAA000000;
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	private final Screen returnScreen;
	private final MovableElementAdapter<S> adapter;
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;
	private Position previewPosition;

	public MoveElementScreen(Screen returnScreen, MovableElementAdapter<S> adapter) {
		super(adapter.title());
		this.returnScreen = returnScreen;
		this.adapter = adapter;
	}

	@Override
	public void removed() {
		adapter.onScreenClosed();
		super.removed();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, BACKGROUND_DIM);

		S drawState = adapter.getDrawState(this.width, this.height);
		if (drawState == null) {
			graphics.drawCenteredString(this.font, adapter.unavailableMessage(), this.width / 2, this.height / 2, TEXT_COLOR);
			return;
		}

		Position position = currentPosition(drawState);
		int x = position.x();
		int y = position.y();
		adapter.draw(graphics, drawState, x, y);

		List<String> infoLines = adapter.infoLines(drawState, x, y);
		if (infoLines != null && !infoLines.isEmpty()) {
			int infoX = Math.max(8, x - 120);
			int infoY = Math.max(8, y + (adapter.drawHeight(drawState) / 2) - this.font.lineHeight);
			int lineOffset = 0;
			for (String line : infoLines) {
				graphics.drawString(this.font, line, infoX, infoY + lineOffset, TEXT_COLOR, false);
				lineOffset += this.font.lineHeight + 2;
			}
		}

		graphics.drawString(this.font, adapter.footerText(), 8, this.height - this.font.lineHeight - 8, TEXT_COLOR, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
		if (event.button() != 0) {
			return true;
		}

		S drawState = adapter.getDrawState(this.width, this.height);
		if (drawState == null) {
			return true;
		}

		Position position = currentPosition(drawState);
		if (isInside(event.x(), event.y(), position.x(), position.y(), adapter.drawWidth(drawState), adapter.drawHeight(drawState))) {
			this.dragging = true;
			this.setDragging(true);
			this.dragOffsetX = (int) Math.round(event.x() - position.x());
			this.dragOffsetY = (int) Math.round(event.y() - position.y());
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (!this.dragging || event.button() != 0) {
			return true;
		}

		S drawState = adapter.getDrawState(this.width, this.height);
		if (drawState == null) {
			return true;
		}

		int nextX = (int) Math.round(event.x() - this.dragOffsetX);
		int nextY = (int) Math.round(event.y() - this.dragOffsetY);
		Position clamped = adapter.clampPosition(drawState, this.width, this.height, nextX, nextY);
		Position applied = resolveDraggedPosition(nextX, nextY, clamped);
		this.previewPosition = applied;
		adapter.applyDraggedPosition(drawState, this.width, this.height, applied.x(), applied.y());
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
		return adapter.onMouseScroll(yDelta);
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

	private Position currentPosition(S drawState) {
		if (this.previewPosition != null) {
			return this.previewPosition;
		}
		return adapter.getMovePosition(drawState, this.width, this.height);
	}

	private Position resolveDraggedPosition(int nextX, int nextY, Position clamped) {
		Minecraft client = this.minecraft;
		if (!adapter.supportsOffscreenPlacement() || client == null || !client.hasShiftDown()) {
			return clamped;
		}

		int appliedX = client.hasAltDown() ? clamped.x() : nextX;
		int appliedY = client.hasControlDown() ? clamped.y() : nextY;
		return new Position(appliedX, appliedY);
	}

	private void closeToGame() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(null);
		}
	}

	public interface MovableElementAdapter<S> {
		Component title();

		Component unavailableMessage();

		@Nullable
		S getDrawState(int screenWidth, int screenHeight);

		Position getVisiblePosition(S drawState, int screenWidth, int screenHeight, boolean persistIfAdjusted);

		default Position getMovePosition(S drawState, int screenWidth, int screenHeight) {
			return getVisiblePosition(drawState, screenWidth, screenHeight, false);
		}

		Position clampPosition(S drawState, int screenWidth, int screenHeight, int x, int y);

		int drawWidth(S drawState);

		int drawHeight(S drawState);

		void draw(GuiGraphics graphics, S drawState, int x, int y);

		void applyDraggedPosition(S drawState, int screenWidth, int screenHeight, int x, int y);

		default List<String> infoLines(S drawState, int x, int y) {
			return List.of("x " + x, "y " + y);
		}

		default String footerText() {
			return "Drag to move | Esc to close";
		}

		default boolean supportsOffscreenPlacement() {
			return false;
		}

		default boolean onMouseScroll(double yDelta) {
			return true;
		}

		default void onScreenClosed() {
		}
	}

	public record Position(int x, int y) {
	}
}
