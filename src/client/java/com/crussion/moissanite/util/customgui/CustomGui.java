package com.crussion.moissanite.util.customgui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;

public abstract class CustomGui {
	public abstract void render(GuiGraphics graphics, float delta, int mouseX, int mouseY);

	public abstract boolean mouseClick(MouseButtonEvent click, boolean doubled);

	public abstract boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount);

	public abstract void moveSlot(Slot slot, int leftPos, int topPos);

	public List<ScreenRectangle> getBounds() {
		return List.of();
	}

	public boolean isClickOutsideBounds(double mouseX, double mouseY) {
		return !isWithinAnyBound(mouseX, mouseY);
	}

	public boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
		if (!isWithinAnyBound(pointX, pointY)) {
			return false;
		}
		return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
	}

	public boolean isPointOverSlot(Slot slot, int leftPos, int topPos, double pointX, double pointY) {
		return isPointWithinBounds(slot.x + leftPos, slot.y + topPos, 16, 16, pointX, pointY);
	}

	public void beforeSlotRender(GuiGraphics graphics, Slot slot) {
	}

	public void afterSlotRender(GuiGraphics graphics, Slot slot) {
	}

	public void onInit(int width, int height) {
	}

	public boolean shouldDrawForeground() {
		return true;
	}

	public boolean onVoluntaryExit() {
		return true;
	}

	public boolean mouseReleased(MouseButtonEvent click) {
		return false;
	}

	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		return false;
	}

	public boolean keyPressed(KeyEvent input) {
		return false;
	}

	public boolean keyReleased(KeyEvent input) {
		return false;
	}

	public boolean charTyped(CharacterEvent input) {
		return false;
	}

	private boolean isWithinAnyBound(double x, double y) {
		List<ScreenRectangle> bounds = getBounds();
		if (bounds.isEmpty()) {
			return true;
		}
		int px = (int) Math.floor(x);
		int py = (int) Math.floor(y);
		for (ScreenRectangle bound : bounds) {
			if (bound != null && bound.containsPoint(px, py)) {
				return true;
			}
		}
		return false;
	}
}
