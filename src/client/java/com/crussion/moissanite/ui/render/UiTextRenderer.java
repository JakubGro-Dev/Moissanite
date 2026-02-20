package com.crussion.moissanite.ui.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class UiTextRenderer {
	private UiTextRenderer() {
	}

	public static void drawBoldString(GuiGraphics graphics, Font font, Component text, int x, int y, int color, float scale) {
		if (scale != 1.0f) {
			graphics.pose().pushMatrix();
			graphics.pose().scale(scale, scale);
			int drawX = Math.round(x / scale);
			int drawY = Math.round(y / scale);
			graphics.drawString(font, text, drawX, drawY, color, false);
			graphics.pose().popMatrix();
			return;
		}
		graphics.drawString(font, text, x, y, color, false);
	}
}
