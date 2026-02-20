package com.crussion.moissanite.ui.widget;

import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.style.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class RoundedEditBox extends EditBox {
	private final int lineHeight;

	public RoundedEditBox(Font font, int x, int y, int width, int height, Component message, int lineHeight) {
		super(font, x, y, width, height, message);
		this.lineHeight = lineHeight;
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int radius = Math.max(4, height / 3);
		UiShapes.fillRoundedRect(graphics, getX(), getY(), width, height, radius, Colors.INPUT_OUTLINE);
		UiShapes.fillRoundedRect(graphics, getX() + 1, getY() + 1, width - 2, height - 2, Math.max(3, radius - 1), Colors.INPUT_BG);
		int textOffset = Math.max(0, (height - lineHeight) / 2 + 2);
		graphics.pose().pushMatrix();
		graphics.pose().translate(0, textOffset);
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		graphics.pose().popMatrix();
	}
}
