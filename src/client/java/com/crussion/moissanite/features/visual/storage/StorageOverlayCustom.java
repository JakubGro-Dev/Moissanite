package com.crussion.moissanite.features.visual.storage;

import java.util.List;

import com.crussion.moissanite.mixin.client.ContainerScreenAccessor;
import com.crussion.moissanite.mixin.client.SlotAccessor;
import com.crussion.moissanite.util.customgui.CustomGui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class StorageOverlayCustom extends CustomGui {

	private final StorageBackingHandle handler;
	private final ContainerScreen screen;
	public final StorageOverlayScreen overview;

	public StorageOverlayCustom(StorageBackingHandle handler, ContainerScreen screen, StorageOverlayScreen overview) {
		this.handler = handler;
		this.screen = screen;
		this.overview = overview;
	}

	@Override
	public boolean onVoluntaryExit() {
		overview.isExiting = true;
		StorageOverlayScreen.resetScroll();
		return super.onVoluntaryExit();
	}

	@Override
	public List<ScreenRectangle> getBounds() {
		return overview.getBounds();
	}

	@Override
	public void afterSlotRender(GuiGraphics graphics, Slot slot) {
		if (!(slot.container instanceof Inventory)) {
			graphics.disableScissor();
		}
	}

	@Override
	public void beforeSlotRender(GuiGraphics context, Slot slot) {
		if (!(slot.container instanceof Inventory)) {
			ContainerScreenAccessor accessor = (ContainerScreenAccessor) screen;
			overview.createScissors(context, accessor.moissanite$getLeftPos(), accessor.moissanite$getTopPos());
		}
	}

	@Override
	public void onInit(int width, int height) {
		overview.setupDimensions(width, height);
		overview.init();

		ContainerScreenAccessor accessor = (ContainerScreenAccessor) screen;
		accessor.moissanite$setLeftPos(overview.mX);
		accessor.moissanite$setTopPos(overview.mY);
		accessor.moissanite$setImageWidth(overview.totalWidth);
		accessor.moissanite$setImageHeight(overview.totalHeight);
	}

	@Override
	public boolean isPointOverSlot(Slot slot, int leftPos, int topPos, double pointX, double pointY) {
		if (!super.isPointOverSlot(slot, leftPos, topPos, pointX, pointY)) {
			return false;
		}
		if (!(slot.container instanceof Inventory)) {
			ScreenRectangle scrollPanel = overview.getScrollPanelInner();
			if (!scrollPanel.containsPoint((int) pointX, (int) pointY)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean shouldDrawForeground() {
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		return overview.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		return overview.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean keyReleased(KeyEvent input) {
		return overview.keyReleased(input);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		return overview.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		return overview.charTyped(input);
	}

	@Override
	public boolean mouseClick(MouseButtonEvent click, boolean doubled) {
		StoragePageSlot activePage = (handler instanceof StorageBackingHandle.Page page)
				? page.storagePageSlot()
				: null;
		return overview.mouseClicked(click, doubled, activePage);
	}

	@Override
	public void render(GuiGraphics drawContext, float delta, int mouseX, int mouseY) {
		overview.drawBackgrounds(drawContext);
		StoragePageSlot excludingPage = (handler instanceof StorageBackingHandle.Page page)
				? page.storagePageSlot()
				: null;
		ContainerScreenAccessor accessor = (ContainerScreenAccessor) screen;
		int leftPos = accessor.moissanite$getLeftPos();
		int topPos = accessor.moissanite$getTopPos();
		List<Slot> activeSlots = screen.getMenu().slots
				.subList(9, Math.min(screen.getMenu().slots.size(), screen.getMenu().getRowCount() * 9));
		overview.drawPages(drawContext, mouseX, mouseY, delta,
				excludingPage, activeSlots,
				leftPos, topPos);
		overview.drawScrollBar(drawContext);
		overview.drawControls(drawContext, mouseX, mouseY);
	}

	@Override
	public void moveSlot(Slot slot, int leftPos, int topPos) {
		SlotAccessor slotAccessor = (SlotAccessor) (Object) slot;
		int index = slot.getContainerSlot();
		if (index >= 0 && index < 36) {
			int[] pos = overview.getPlayerInventorySlotPosition(index);
			slotAccessor.moissanite$setX(pos[0] - leftPos);
			slotAccessor.moissanite$setY(pos[1] - topPos);
		} else {
			slotAccessor.moissanite$setX(-100000);
			slotAccessor.moissanite$setY(-100000);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		// 1:1 Firmament: items block scrolling
		if (StorageOverlayFeature.itemsBlockScrolling()) {
			if (screen instanceof ContainerScreenAccessor accessor) {
				Slot hoveredSlot = accessor.moissanite$getHoveredSlot();
				if (hoveredSlot != null && hoveredSlot.hasItem()) {
					return false;
				}
			}
		}
		return overview.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
}
