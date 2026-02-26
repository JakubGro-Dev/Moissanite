package com.crussion.moissanite.ui.widget;

import java.util.ArrayList;
import java.util.List;

import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.style.Theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ListView {
	private final List<Component> items = new ArrayList<>();
	private int x;
	private int y;
	private int width;
	private int height;
	private int itemHeight = Theme.LIST_ITEM_HEIGHT;
	private int scrollOffset;
	private int selectedIndex = -1;
	private boolean visibleOnlyWhenScrollable = true;
	private boolean centered;
	private boolean bold;
	private float textScale = 1.0f;
	private float selectedScale = 1.15f;

	public void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void setItems(List<Component> items) {
		this.items.clear();
		if (items != null) {
			this.items.addAll(items);
		}
		this.scrollOffset = 0;
		this.selectedIndex = -1;
	}

	public void setItemHeight(int itemHeight) {
		this.itemHeight = Math.max(8, itemHeight);
	}

	public void setVisibleOnlyWhenScrollable(boolean value) {
		this.visibleOnlyWhenScrollable = value;
	}

	public void setCentered(boolean centered) {
		this.centered = centered;
	}

	public void setBold(boolean bold) {
		this.bold = bold;
	}

	public void setTextScale(float textScale) {
		this.textScale = textScale <= 0f ? 1.0f : textScale;
	}

	public void setSelectedScale(float selectedScale) {
		this.selectedScale = selectedScale <= 0f ? 1.0f : selectedScale;
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	public void setSelectedIndex(int index) {
		if (index < -1) {
			this.selectedIndex = -1;
			return;
		}
		this.selectedIndex = Math.min(index, this.items.size() - 1);
	}

	public int getScrollOffset() {
		return this.scrollOffset;
	}

	public void setScrollOffset(int scrollOffset) {
		int maxScroll = Math.max(0, this.items.size() * this.itemHeight - this.height);
		this.scrollOffset = clamp(scrollOffset, 0, maxScroll);
	}

	public void clearSelection() {
		selectedIndex = -1;
	}

	public Component getSelectedItem() {
		if (selectedIndex < 0 || selectedIndex >= items.size()) {
			return null;
		}
		return items.get(selectedIndex);
	}

	public boolean isScrollable() {
		return items.size() * itemHeight > height;
	}

	public boolean isVisible() {
		return !visibleOnlyWhenScrollable || isScrollable();
	}

	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		if (!isVisible()) {
			return;
		}

		int contentHeight = items.size() * itemHeight;
		int maxScroll = Math.max(0, contentHeight - height);
		scrollOffset = clamp(scrollOffset, 0, maxScroll);

		UiShapes.fillRoundedOutline(graphics, x, y, width, height, Theme.SECTION_RADIUS, 1, Colors.SECTION_OUTLINE_NESTED, Colors.LIST_BG);

		int startIndex = scrollOffset / itemHeight;
		int yOffset = y - (scrollOffset % itemHeight);
		for (int i = startIndex; i < items.size(); i++) {
			int itemY = yOffset + (i - startIndex) * itemHeight;
			if (itemY >= y + height) {
				break;
			}
			if (itemY + itemHeight <= y) {
				continue;
			}

			boolean hovered = isInside(mouseX, mouseY, x, itemY, width, itemHeight);
			int rowX = x + 4;
			int rowY = itemY + 2;
			int rowW = width - 8;
			int rowH = Math.max(8, itemHeight - 3);
			int radius = Math.max(5, rowH / 3);

			if (i == selectedIndex) {
				UiShapes.fillRoundedRect(graphics, rowX, rowY, rowW, rowH, radius, Colors.LIST_SELECTED);
				UiShapes.fillRoundedRect(graphics, rowX, rowY, rowW, 1, radius, Colors.LIST_SELECTED_GLOW);
				UiShapes.fillRoundedRect(graphics, rowX + 1, rowY + 3, 2, Math.max(2, rowH - 6), 1, Colors.ACCENT);
			} else if (hovered) {
				UiShapes.fillRoundedRect(graphics, rowX, rowY, rowW, rowH, radius, Colors.LIST_HOVER);
			}

			Component label = items.get(i);
			int baseWidth = font.width(label);
			float scale = i == selectedIndex ? selectedScale : textScale;
			int scaledWidth = Math.round(baseWidth * scale);
			int textX = x + 11;
			if (centered) {
				textX = x + (width - scaledWidth) / 2;
			}
			boolean selected = i == selectedIndex;
			int color = selected ? Colors.TEXT_SELECTED : Colors.TEXT_MUTED;
			drawLabel(graphics, font, label, textX, itemY + (itemHeight - font.lineHeight) / 2, color, bold, scale);
		}

		if (isScrollable()) {
			renderScrollbar(graphics, contentHeight, maxScroll);
		}
	}

	private void renderScrollbar(GuiGraphics graphics, int contentHeight, int maxScroll) {
		int trackX = x + width - 6;
		int trackY = y + 6;
		int trackHeight = height - 12;
		UiShapes.fillRoundedRect(graphics, trackX, trackY, 2, trackHeight, 1, Colors.DIVIDER);

		if (maxScroll <= 0) {
			return;
		}

		int thumbHeight = Math.max(12, (int) ((float) trackHeight * (float) height / (float) contentHeight));
		int scrollRange = trackHeight - thumbHeight;
		int thumbY = trackY + (int) ((float) scrollOffset / (float) maxScroll * scrollRange);
		UiShapes.fillRoundedRect(graphics, trackX - 1, thumbY, 4, thumbHeight, 2, Colors.ACCENT_SOFT);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isVisible()) {
			return false;
		}
		if (!isInside(mouseX, mouseY, x, y, width, height)) {
			return false;
		}
		int index = (int) ((mouseY - y + scrollOffset) / itemHeight);
		if (index >= 0 && index < items.size()) {
			selectedIndex = index;
			return true;
		}
		return false;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!isVisible() || !isScrollable()) {
			return false;
		}
		if (!isInside(mouseX, mouseY, x, y, width, height)) {
			return false;
		}
		scrollOffset = clamp(scrollOffset - (int) (amount * itemHeight), 0, Math.max(0, items.size() * itemHeight - height));
		return true;
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static void drawLabel(GuiGraphics graphics, Font font, Component label, int x, int y, int color, boolean bold, float scale) {
		if (scale != 1.0f) {
			graphics.pose().pushMatrix();
			graphics.pose().scale(scale, scale);
			int drawX = Math.round(x / scale);
			int drawY = Math.round(y / scale);
			graphics.drawString(font, label, drawX, drawY, color, false);
			graphics.pose().popMatrix();
			return;
		}
		graphics.drawString(font, label, x, y, color, false);
	}

}
