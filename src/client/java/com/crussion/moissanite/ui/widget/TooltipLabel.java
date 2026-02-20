package com.crussion.moissanite.ui.widget;

import com.crussion.moissanite.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class TooltipLabel {
	private int x;
	private int y;
	private int color = Colors.TEXT_PRIMARY;
	private Component text = Component.empty();
	private Component tooltip;

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void setText(Component text) {
		this.text = text == null ? Component.empty() : text;
	}

	public void setTooltip(Component tooltip) {
		this.tooltip = tooltip;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		graphics.drawString(font, text, x, y, color, false);
		if (tooltip != null && isHovered(font, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	private boolean isHovered(Font font, int mouseX, int mouseY) {
		int width = font.width(text);
		int height = font.lineHeight;
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}

