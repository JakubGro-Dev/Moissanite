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
		int bg = isHoveredOrFocused() ? Colors.BUTTON_HOVER : Colors.BUTTON_BG;
		int outline = isHoveredOrFocused() ? Colors.BUTTON_OUTLINE_HOVER : Colors.BUTTON_OUTLINE;
		UiShapes.fillRoundedOutline(graphics, getX(), getY(), width, height, radius, 1, outline, bg);

		String current = dropdown.get();
		String value = (current == null || current.isBlank()) ? "None" : current;
		Component message = UiText.uiTextStatic(value);
		int textX = getX() + 9;
		int textY = getY() + (height - font.lineHeight) / 2;
		UiTextRenderer.drawBoldString(graphics, font, message, textX, textY, Colors.TEXT_PRIMARY, 1.0f);

		int chevronCenterX = getX() + width - 11;
		int chevronCenterY = getY() + height / 2;
		if (expanded) {
			graphics.fill(chevronCenterX - 1, chevronCenterY - 2, chevronCenterX + 2, chevronCenterY - 1, Colors.TEXT_MUTED);
			graphics.fill(chevronCenterX - 2, chevronCenterY - 1, chevronCenterX + 3, chevronCenterY, Colors.TEXT_MUTED);
			graphics.fill(chevronCenterX - 3, chevronCenterY, chevronCenterX + 4, chevronCenterY + 1, Colors.TEXT_MUTED);
		} else {
			graphics.fill(chevronCenterX - 3, chevronCenterY - 1, chevronCenterX + 4, chevronCenterY, Colors.TEXT_MUTED);
			graphics.fill(chevronCenterX - 2, chevronCenterY, chevronCenterX + 3, chevronCenterY + 1, Colors.TEXT_MUTED);
			graphics.fill(chevronCenterX - 1, chevronCenterY + 1, chevronCenterX + 2, chevronCenterY + 2, Colors.TEXT_MUTED);
		}
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
		int itemHeight = Math.max(16, height - 2);
		int menuX = getX();
		int menuY = getY() + height + 2;
		int menuW = width;
		int menuH = itemHeight * optionCount;
		UiShapes.fillRoundedOutline(graphics, menuX, menuY, menuW, menuH, 6, 1, Colors.BUTTON_OUTLINE, Colors.BUTTON_BG);

		String current = dropdown.get();
		for (int i = 0; i < optionCount; i++) {
			int rowY = menuY + (i * itemHeight);
			String option = dropdown.options().get(i);
			boolean selected = option.equals(current);
			boolean hovered = isInside(mouseX, mouseY, menuX, rowY, menuW, itemHeight);
			if (selected || hovered) {
				UiShapes.fillRoundedRect(graphics, menuX + 1, rowY + 1, menuW - 2, itemHeight - 2, 4, selected ? Colors.LIST_SELECTED : Colors.BUTTON_HOVER);
			}
			UiTextRenderer.drawBoldString(
					graphics,
					font,
					UiText.uiTextStatic(option),
					menuX + 8,
					rowY + (itemHeight - font.lineHeight) / 2,
					Colors.TEXT_PRIMARY,
					1.0f);
		}
	}

	private int optionIndexAt(double mouseX, double mouseY) {
		if (!isInsideOptions(mouseX, mouseY)) {
			return -1;
		}
		int itemHeight = Math.max(16, height - 2);
		int index = (int) ((mouseY - (getY() + height + 2)) / itemHeight);
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
		int itemHeight = Math.max(16, height - 2);
		return isInside(mouseX, mouseY, getX(), getY() + height + 2, width, itemHeight * optionCount);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
