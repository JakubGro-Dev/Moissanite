package com.crussion.moissanite.ui.widget;

import com.crussion.moissanite.ui.data.UiColor;
import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.text.UiText;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;

public final class ColorSelectorWidget extends AbstractWidget {
	private static final int CHANNEL_COUNT = 4;
	private static final int MENU_GAP = 2;
	private static final int MENU_PADDING = 6;
	private static final int ROW_HEIGHT = 16;

	private final Font font;
	private final UiColor color;
	private boolean expanded;
	private int draggingChannel = -1;

	public ColorSelectorWidget(Font font, int x, int y, int width, int height, UiColor color) {
		super(x, y, width, height, UiText.uiTextStatic(color.name()));
		this.font = font;
		this.color = color;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean focused) {
		if (event.button() != 0) {
			return;
		}
		if (isInsideMain(event.x(), event.y())) {
			expanded = !expanded;
			draggingChannel = -1;
			return;
		}
		if (expanded) {
			int channel = channelAt(event.x(), event.y());
			if (channel >= 0) {
				draggingChannel = channel;
				applyChannelFromMouse(channel, event.x());
			} else if (!isInsideMenu(event.x(), event.y())) {
				expanded = false;
			}
		}
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		if (event.button() != 0 || draggingChannel < 0 || !expanded) {
			return;
		}
		applyChannelFromMouse(draggingChannel, event.x());
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		draggingChannel = -1;
		super.onRelease(event);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			expanded = false;
			draggingChannel = -1;
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return isInsideMain(mouseX, mouseY) || (expanded && isInsideMenu(mouseX, mouseY));
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int radius = Math.max(6, height / 3);
		int bg = isHoveredOrFocused() ? Colors.BUTTON_HOVER : Colors.BUTTON_BG;
		int outline = isHoveredOrFocused() ? Colors.BUTTON_OUTLINE_HOVER : Colors.BUTTON_OUTLINE;
		UiShapes.fillRoundedOutline(graphics, getX(), getY(), width, height, radius, 1, outline, bg);

		int previewSize = Math.max(10, height - 8);
		int previewX = getX() + 5;
		int previewY = getY() + (height - previewSize) / 2;
		UiShapes.fillRoundedOutline(graphics, previewX, previewY, previewSize, previewSize, 3, 1, Colors.INPUT_OUTLINE, color.argb());

		String hex = String.format("#%02X%02X%02X%02X", color.red(), color.green(), color.blue(), color.alpha());
		graphics.drawString(font, UiText.uiTextStatic(hex), previewX + previewSize + 7, getY() + (height - font.lineHeight) / 2, Colors.TEXT_PRIMARY, false);
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void closeSelector() {
		expanded = false;
		draggingChannel = -1;
	}

	public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!expanded) {
			return;
		}

		int menuX = getX();
		int menuY = getY() + height + MENU_GAP;
		int menuW = width;
		int menuH = menuHeight();
		UiShapes.fillRoundedOutline(graphics, menuX, menuY, menuW, menuH, 6, 1, Colors.BUTTON_OUTLINE, Colors.INPUT_BG);

		int barX = menuX + 18;
		int barW = Math.max(10, menuW - 24);
		for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
			int rowY = menuY + MENU_PADDING + (channel * ROW_HEIGHT);
			drawChannelRow(graphics, channel, rowY, barX, barW, mouseX, mouseY);
		}
	}

	private void drawChannelRow(GuiGraphics graphics, int channel, int rowY, int barX, int barW, int mouseX, int mouseY) {
		String label = switch (channel) {
			case 0 -> "R";
			case 1 -> "G";
			case 2 -> "B";
			default -> "A";
		};
		graphics.drawString(font, UiText.uiTextStatic(label), getX() + 7, rowY + 2, Colors.TEXT_MUTED, false);

		int value = channelValue(channel);
		int fillW = (int) Math.round((value / 255.0) * barW);
		int trackColor = Colors.SLIDER_TRACK;
		int activeColor = switch (channel) {
			case 0 -> 0xFFCC4D4D;
			case 1 -> 0xFF52B76B;
			case 2 -> 0xFF4B86D9;
			default -> 0xFFCACACA;
		};

		UiShapes.fillRoundedRect(graphics, barX, rowY + 3, barW, 6, 3, trackColor);
		UiShapes.fillRoundedRect(graphics, barX, rowY + 3, Math.max(0, fillW), 6, 3, activeColor);
		int knobX = barX + Math.max(0, Math.min(barW, fillW));
		UiShapes.fillCircle(graphics, knobX, rowY + 6, 3, Colors.SLIDER_KNOB);

		if (isInside(mouseX, mouseY, barX, rowY, barW, ROW_HEIGHT)) {
			graphics.drawString(font, UiText.uiTextStatic(Integer.toString(value)), barX + barW - 18, rowY + 2, Colors.TEXT_PRIMARY, false);
		}
	}

	private int channelAt(double mouseX, double mouseY) {
		if (!isInsideMenu(mouseX, mouseY)) {
			return -1;
		}
		int menuY = getY() + height + MENU_GAP;
		int relativeY = (int) (mouseY - (menuY + MENU_PADDING));
		if (relativeY < 0) {
			return -1;
		}
		int channel = relativeY / ROW_HEIGHT;
		if (channel < 0 || channel >= CHANNEL_COUNT) {
			return -1;
		}
		int barX = getX() + 18;
		int barW = Math.max(10, width - 24);
		int rowY = menuY + MENU_PADDING + (channel * ROW_HEIGHT);
		return isInside(mouseX, mouseY, barX, rowY, barW, ROW_HEIGHT) ? channel : -1;
	}

	private void applyChannelFromMouse(int channel, double mouseX) {
		int barX = getX() + 18;
		int barW = Math.max(10, width - 24);
		double normalized = (mouseX - barX) / (double) barW;
		int value = (int) Math.round(Math.max(0.0, Math.min(1.0, normalized)) * 255.0);
		switch (channel) {
			case 0 -> color.setRed(value);
			case 1 -> color.setGreen(value);
			case 2 -> color.setBlue(value);
			case 3 -> color.setAlpha(value);
			default -> {
			}
		}
	}

	private int menuHeight() {
		return (CHANNEL_COUNT * ROW_HEIGHT) + (MENU_PADDING * 2);
	}

	private int channelValue(int channel) {
		return switch (channel) {
			case 0 -> color.red();
			case 1 -> color.green();
			case 2 -> color.blue();
			default -> color.alpha();
		};
	}

	private boolean isInsideMain(double mouseX, double mouseY) {
		return isInside(mouseX, mouseY, getX(), getY(), width, height);
	}

	private boolean isInsideMenu(double mouseX, double mouseY) {
		return isInside(mouseX, mouseY, getX(), getY() + height + MENU_GAP, width, menuHeight());
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narration) {
		defaultButtonNarrationText(narration);
	}
}
