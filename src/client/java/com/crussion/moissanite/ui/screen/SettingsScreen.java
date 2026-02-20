package com.crussion.moissanite.ui.screen;

import com.crussion.moissanite.ui.layout.Layouts;
import com.crussion.moissanite.ui.style.Colors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends BaseScreen {
	public SettingsScreen() {
		super(Component.literal("Moissanite Settings"));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		if (layout != null) {
			Layouts.Rect right = layout.right();
			int textX = right.x() + 8;
			int textY = right.y() + 8;
			graphics.drawString(this.font, Component.literal("Settings go here"), textX, textY, Colors.TEXT_MUTED, false);
		}
	}
}
