package com.crussion.moissanite.ui.widget;

import com.crussion.moissanite.ui.data.UiSwitch;
import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.style.Colors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class SwitchWidget extends AbstractWidget {
	private final UiSwitch toggle;
	private float anim;

	public SwitchWidget(int x, int y, int width, int height, UiSwitch toggle) {
		super(x, y, width, height, Component.empty());
		this.toggle = toggle;
		this.anim = Boolean.TRUE.equals(toggle.get()) ? 1f : 0f;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean focused) {
		if (event.button() == 0) {
			toggle.toggle();
		}
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		float target = Boolean.TRUE.equals(toggle.get()) ? 1f : 0f;
		anim += (target - anim) * 0.25f;
		anim = Math.max(0f, Math.min(1f, anim));

		int trackW = Math.min(width, 34);
		int trackH = 11;
		int trackX = getX() + (width - trackW) / 2;
		int trackY = getY() + (height - trackH) / 2;
		int radius = trackH / 2;
		UiShapes.fillRoundedRect(graphics, trackX, trackY, trackW, trackH, radius, Colors.SWITCH_TRACK);

		int dotSize = 8;
		int dotX = trackX + 2 + (int) ((trackW - dotSize - 4) * anim);
		int dotY = trackY + (trackH - dotSize) / 2;
		int dotRadius = dotSize / 2;
		int dotColor = Boolean.TRUE.equals(toggle.get()) ? Colors.SWITCH_DOT_ON : Colors.SWITCH_DOT_OFF;
		UiShapes.fillCircle(graphics, dotX + dotRadius, dotY + dotRadius, dotRadius, dotColor);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narration) {
		defaultButtonNarrationText(narration);
	}
}
