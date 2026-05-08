package com.crussion.moissanite.ui.widget;

import com.crussion.moissanite.ui.data.UiDropdown;
import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.render.UiTextRenderer;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.text.UiText;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class DropdownWidget extends AbstractWidget {
	private static final int CHEVRON_AREA_WIDTH = 25;
	private static final int MENU_GAP = 1;
	private static final int MENU_PADDING_Y = 3;
	private static final int MENU_PADDING_X = 3;
	private static final int MENU_BG = Colors.rgba(36, 41, 66, 252);
	private static final int MENU_SHADOW = Colors.rgba(5, 7, 16, 120);
	private static final int MENU_OUTLINE = Colors.rgba(126, 136, 184, 225);
	private static final int MENU_HOVER = Colors.rgba(81, 91, 132, 244);
	private static final int MENU_SELECTED = Colors.rgba(154, 127, 238, 150);

	private final Font font;
	private final UiDropdown dropdown;
	private boolean expanded;

	public DropdownWidget(Font font, int x, int y, int width, int height, UiDropdown dropdown) {
		super(x, y, width, height, Component.empty());
		this.font = font;
		this.dropdown = dropdown;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean focused) {
		if (event.button() != 0) {
			return;
		}
		if (isInsideMain(event.x(), event.y())) {
			expanded = !expanded;
			return;
		}
		if (expanded) {
			int optionIndex = optionIndexAt(event.x(), event.y());
			if (optionIndex >= 0 && optionIndex < dropdown.options().size()) {
				dropdown.set(dropdown.options().get(optionIndex));
			}
			expanded = false;
		}
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int radius = Math.max(6, height / 3);
		boolean hovered = isHoveredOrFocused() || expanded;
		int bg = hovered ? Colors.BUTTON_HOVER : Colors.BUTTON_BG;
		int outline = hovered ? Colors.BUTTON_OUTLINE_HOVER : Colors.BUTTON_OUTLINE;
		UiShapes.fillRoundedOutline(graphics, getX(), getY(), width, height, radius, 1, outline, bg);

		String current = dropdown.get();
		String value = (current == null || current.isBlank()) ? "None" : current;
		int textX = getX() + 9;
		int textY = getY() + (height - font.lineHeight) / 2;
		int textMaxWidth = Math.max(8, width - CHEVRON_AREA_WIDTH - 14);
		UiTextRenderer.drawBoldString(graphics, font, UiText.uiTextStatic(fitText(value, textMaxWidth)), textX, textY, Colors.TEXT_PRIMARY, 1.0f);

		int dividerX = getX() + width - CHEVRON_AREA_WIDTH;
		graphics.fill(dividerX, getY() + 4, dividerX + 1, getY() + height - 4, Colors.DIVIDER);
		int chevronCenterX = getX() + width - 11;
		int chevronCenterY = getY() + height / 2;
		drawChevron(graphics, chevronCenterX, chevronCenterY, expanded, expanded ? Colors.TEXT_PRIMARY : Colors.TEXT_MUTED);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narration) {
		defaultButtonNarrationText(narration);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			expanded = false;
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return isInsideMain(mouseX, mouseY) || (expanded && isInsideOptions(mouseX, mouseY));
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void closeDropdown() {
		expanded = false;
	}

	public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
		if (expanded) {
			renderOptions(graphics, mouseX, mouseY);
		}
	}

	private void renderOptions(GuiGraphics graphics, int mouseX, int mouseY) {
		int optionCount = dropdown.options().size();
		if (optionCount <= 0) {
			return;
		}
		int itemHeight = itemHeight();
		int menuX = getX();
		int menuY = getY() + height + MENU_GAP;
		int menuW = width;
		int menuH = menuHeight(optionCount);
		UiShapes.fillRoundedRect(graphics, menuX + 1, menuY + 2, menuW, menuH, 6, MENU_SHADOW);
		UiShapes.fillRoundedOutline(graphics, menuX, menuY, menuW, menuH, 6, 1, MENU_OUTLINE, MENU_BG);

		String current = dropdown.get();
		for (int i = 0; i < optionCount; i++) {
			int rowX = menuX + MENU_PADDING_X;
			int rowY = menuY + MENU_PADDING_Y + (i * itemHeight);
			int rowW = menuW - (MENU_PADDING_X * 2);
			String option = dropdown.options().get(i);
			boolean selected = option.equals(current);
			boolean hovered = isInside(mouseX, mouseY, rowX, rowY, rowW, itemHeight);
			if (selected || hovered) {
				UiShapes.fillRoundedRect(graphics, rowX, rowY + 1, rowW, itemHeight - 2, 4, selected ? MENU_SELECTED : MENU_HOVER);
			}
			if (selected) {
				graphics.fill(rowX + 3, rowY + 5, rowX + 4, rowY + itemHeight - 5, Colors.ACCENT);
			}
			int textColor = selected ? Colors.TEXT_SELECTED : (hovered ? Colors.TEXT_PRIMARY : Colors.TEXT_MUTED);
			UiTextRenderer.drawBoldString(
					graphics,
					font,
					UiText.uiTextStatic(fitText(option, rowW - 14)),
					rowX + 8,
					rowY + (itemHeight - font.lineHeight) / 2,
					textColor,
					1.0f);
		}
	}

	private int optionIndexAt(double mouseX, double mouseY) {
		if (!isInsideOptions(mouseX, mouseY)) {
			return -1;
		}
		int itemHeight = itemHeight();
		double relativeY = mouseY - (getY() + height + MENU_GAP + MENU_PADDING_Y);
		if (relativeY < 0.0 || relativeY >= itemHeight * dropdown.options().size()) {
			return -1;
		}
		int index = (int) (relativeY / itemHeight);
		return index;
	}

	private boolean isInsideMain(double mouseX, double mouseY) {
		return isInside(mouseX, mouseY, getX(), getY(), width, height);
	}

	private boolean isInsideOptions(double mouseX, double mouseY) {
		int optionCount = dropdown.options().size();
		if (optionCount <= 0) {
			return false;
		}
		return isInside(mouseX, mouseY, getX(), getY() + height + MENU_GAP, width, menuHeight(optionCount));
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private int itemHeight() {
		return Math.max(18, height - 1);
	}

	private int menuHeight(int optionCount) {
		return (itemHeight() * optionCount) + (MENU_PADDING_Y * 2);
	}

	private String fitText(String text, int maxWidth) {
		if (text == null || text.isEmpty() || font.width(text) <= maxWidth) {
			return text == null ? "" : text;
		}
		String suffix = "...";
		int suffixWidth = font.width(suffix);
		if (maxWidth <= suffixWidth) {
			return font.plainSubstrByWidth(text, Math.max(0, maxWidth));
		}
		return font.plainSubstrByWidth(text, maxWidth - suffixWidth) + suffix;
	}

	private static void drawChevron(GuiGraphics graphics, int centerX, int centerY, boolean expanded, int color) {
		if (expanded) {
			graphics.fill(centerX - 1, centerY - 3, centerX + 2, centerY - 2, color);
			graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY - 1, color);
			graphics.fill(centerX - 3, centerY - 1, centerX + 4, centerY, color);
			return;
		}
		graphics.fill(centerX - 3, centerY - 1, centerX + 4, centerY, color);
		graphics.fill(centerX - 2, centerY, centerX + 3, centerY + 1, color);
		graphics.fill(centerX - 1, centerY + 1, centerX + 2, centerY + 2, color);
	}
}
