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
		anim += (target - anim) * 0.28f;
		anim = Math.max(0f, Math.min(1f, anim));

		int trackW = Math.min(width, 40);
		int trackH = Math.max(12, Math.min(height - 4, 16));
		int trackX = getX() + (width - trackW) / 2;
		int trackY = getY() + (height - trackH) / 2;
		int radius = trackH / 2;
		int trackColor = lerpColor(Colors.SWITCH_TRACK, Colors.SWITCH_TRACK_ON, anim);
		UiShapes.fillRoundedRect(graphics, trackX, trackY, trackW, trackH, radius, trackColor);

		int dotSize = Math.max(8, trackH - 4);
		int dotX = trackX + 2 + (int) ((trackW - dotSize - 4) * anim);
		int dotY = trackY + (trackH - dotSize) / 2;
		int dotRadius = dotSize / 2;
		int dotColor = lerpColor(Colors.SWITCH_DOT_OFF, Colors.SWITCH_DOT_ON, anim);
		UiShapes.fillCircle(graphics, dotX + dotRadius, dotY + dotRadius, dotRadius, dotColor);
		UiShapes.fillCircle(graphics, dotX + dotRadius - 1, dotY + dotRadius - 1, Math.max(1, dotRadius / 3), Colors.SWITCH_DOT_HIGHLIGHT);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narration) {
		defaultButtonNarrationText(narration);
	}

	private static int lerpColor(int from, int to, float progress) {
		int fromA = (from >>> 24) & 0xFF;
		int fromR = (from >>> 16) & 0xFF;
		int fromG = (from >>> 8) & 0xFF;
		int fromB = from & 0xFF;
		int toA = (to >>> 24) & 0xFF;
		int toR = (to >>> 16) & 0xFF;
		int toG = (to >>> 8) & 0xFF;
		int toB = to & 0xFF;
		int a = Math.round(fromA + (toA - fromA) * progress);
		int r = Math.round(fromR + (toR - fromR) * progress);
		int g = Math.round(fromG + (toG - fromG) * progress);
		int b = Math.round(fromB + (toB - fromB) * progress);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
