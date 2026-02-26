package com.crussion.moissanite.ui.widget;

import java.util.Locale;

import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.text.UiText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class SliderWidget extends AbstractSliderButton {
	private final UiSlider slider;
	private final double min;
	private final double max;
	private final double step;
	private final int decimals;

	public SliderWidget(int x, int y, int width, int height, UiSlider slider) {
		super(x, y, width, height, Component.empty(), toNormalized(slider.get(), slider.min(), slider.max()));
		this.slider = slider;
		this.min = slider.min();
		this.max = slider.max();
		this.step = slider.step();
		this.decimals = decimalsFromStep(this.step);
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		double value = slider.snap(fromNormalized(this.value, min, max));
		setMessage(UiText.uiTextStatic(String.format(Locale.ROOT, "%." + decimals + "f", value)));
	}

	@Override
	protected void applyValue() {
		slider.set(fromNormalized(this.value, min, max));
		this.value = toNormalized(slider.get(), min, max);
	}

	private static double toNormalized(double value, double min, double max) {
		if (max <= min) {
			return 0.0;
		}
		return (value - min) / (max - min);
	}

	private static double fromNormalized(double value, double min, double max) {
		return min + (max - min) * value;
	}

	private static int decimalsFromStep(double step) {
		if (step >= 1.0) {
			return 0;
		}
		int decimals = 0;
		double value = step;
		while (decimals < 6 && Math.abs(Math.rint(value) - value) > 1.0e-6) {
			value *= 10.0;
			decimals++;
		}
		return Math.max(0, decimals);
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		updateMessage();
		int x = getX();
		int y = getY();
		int width = this.width;
		int height = this.height;
		int valueBoxWidth = valueBoxWidth(width);
		int valueBoxX = valueBoxX(x, width, valueBoxWidth);
		int valueBoxHeight = Math.min(20, height);
		int valueBoxY = y + (height - valueBoxHeight) / 2;

		int trackX = trackStart(x);
		int trackEnd = trackEnd(trackX, valueBoxX);
		int trackWidth = trackWidth(trackX, trackEnd);
		int trackHeight = 4;
		int trackY = y + (height - trackHeight) / 2;
		int knobX = trackX + (int) Math.round(trackWidth * this.value);
		int activeWidth = Math.max(2, knobX - trackX);

		UiShapes.fillRoundedRect(graphics, trackX, trackY, trackWidth, trackHeight, 2, Colors.SLIDER_TRACK);
		UiShapes.fillRoundedRect(graphics, trackX, trackY, activeWidth, trackHeight, 2, Colors.SLIDER_ACTIVE);
		UiShapes.fillCircle(graphics, knobX, trackY + trackHeight / 2, 5, Colors.SLIDER_KNOB);
		UiShapes.fillCircle(graphics, knobX, trackY + trackHeight / 2, 2, Colors.ACCENT);

		UiShapes.fillRoundedOutline(
				graphics,
				valueBoxX,
				valueBoxY,
				valueBoxWidth,
				valueBoxHeight,
				6,
				1,
				Colors.SLIDER_VALUE_OUTLINE,
				Colors.SLIDER_VALUE_BG);
		graphics.drawCenteredString(
				Minecraft.getInstance().font,
				getMessage(),
				valueBoxX + (valueBoxWidth / 2),
				valueBoxY + (valueBoxHeight - Minecraft.getInstance().font.lineHeight) / 2,
				Colors.TEXT_PRIMARY);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean focused) {
		setValueFromTrack(event.x());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		setValueFromTrack(event.x());
	}

	private void setValueFromTrack(double mouseX) {
		int x = getX();
		int width = this.width;
		int valueBoxWidth = valueBoxWidth(width);
		int valueBoxX = valueBoxX(x, width, valueBoxWidth);
		int trackX = trackStart(x);
		int trackEnd = trackEnd(trackX, valueBoxX);
		int trackWidth = trackWidth(trackX, trackEnd);
		if (trackWidth <= 0) {
			return;
		}
		double normalized = (mouseX - trackX) / (double) trackWidth;
		setValue(Mth.clamp(normalized, 0.0, 1.0));
	}

	private static int valueBoxWidth(int width) {
		return Math.min(66, Math.max(42, width / 3));
	}

	private static int valueBoxX(int x, int width, int valueBoxWidth) {
		return x + width - valueBoxWidth;
	}

	private static int trackStart(int x) {
		return x + 6;
	}

	private static int trackEnd(int trackStart, int valueBoxX) {
		return Math.max(trackStart + 10, valueBoxX - 8);
	}

	private static int trackWidth(int trackStart, int trackEnd) {
		return Math.max(10, trackEnd - trackStart);
	}
}
