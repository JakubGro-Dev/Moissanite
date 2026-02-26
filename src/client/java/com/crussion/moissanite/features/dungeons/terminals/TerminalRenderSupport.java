package com.crussion.moissanite.features.dungeons.terminals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

final class TerminalRenderSupport {
	private TerminalRenderSupport() {
	}

	static void drawCenteredScaledText(GuiGraphics graphics, String text, float scale, int yOffset) {
		Minecraft client = Minecraft.getInstance();
		if (graphics == null || client == null || client.font == null || client.getWindow() == null || text == null) {
			return;
		}

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int scaledScreenWidth = Math.round(screenWidth / scale);
		int x = (scaledScreenWidth - client.font.width(text)) / 2;
		int y = Math.round(screenHeight / scale / 2.0F) + yOffset;

		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		graphics.drawString(client.font, text, x, y, 0xFFFFFFFF, true);
		graphics.pose().popMatrix();
	}
}
