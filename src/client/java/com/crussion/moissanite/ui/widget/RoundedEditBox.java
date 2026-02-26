package com.crussion.moissanite.ui.widget;

import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class RoundedEditBox extends EditBox {
	private static final int TEXT_PADDING_X = 6;
	private final int lineHeight;

	public RoundedEditBox(Font font, int x, int y, int width, int height, Component message, int lineHeight) {
		super(font, x, y, width, height, message);
		this.lineHeight = lineHeight;
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int radius = Math.max(6, height / 3);
		int outline = isFocused() ? Colors.INPUT_OUTLINE_FOCUS : (isHoveredOrFocused() ? Colors.BUTTON_OUTLINE_HOVER : Colors.INPUT_OUTLINE);
		UiShapes.fillRoundedOutline(graphics, getX(), getY(), width, height, radius, 1, outline, Colors.INPUT_BG);
		int textOffset = Math.max(0, (height - lineHeight) / 2 + 2);
		graphics.pose().pushMatrix();
		graphics.pose().translate(TEXT_PADDING_X, textOffset);
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		graphics.pose().popMatrix();
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean focused) {
		super.onClick(shiftX(event, -TEXT_PADDING_X), focused);
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		super.onDrag(shiftX(event, -TEXT_PADDING_X), dragX, dragY);
	}

	@Override
	public int getInnerWidth() {
		return Math.max(1, super.getInnerWidth() - (TEXT_PADDING_X * 2));
	}

	private static MouseButtonEvent shiftX(MouseButtonEvent event, int deltaX) {
		return new MouseButtonEvent(event.x() + deltaX, event.y(), event.buttonInfo());
	}
}
