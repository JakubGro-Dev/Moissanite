package com.crussion.moissanite.ui.widget;

import java.util.Locale;

import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.text.UiText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public final class SliderWidget extends AbstractSliderButton {
	private final UiSlider slider;
	private final double min;
	private final double max;

	public SliderWidget(int x, int y, int width, int height, UiSlider slider) {
		super(x, y, width, height, Component.empty(), toNormalized(slider.get(), slider.min(), slider.max()));
		this.slider = slider;
		this.min = slider.min();
		this.max = slider.max();
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		double value = fromNormalized(this.value, min, max);
		setMessage(UiText.uiTextStatic(String.format(Locale.ROOT, "%.2f", value)));
	}

	@Override
	protected void applyValue() {
		slider.set(fromNormalized(this.value, min, max));
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

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int x = getX();
		int y = getY();
		int width = this.width;
		int height = this.height;
		int valueY = y + 1;
		int lineY = y + height - 6;

		int lineStart = x + 6;
		int lineEnd = x + width - 6;
		int knobX = (int) (lineStart + (lineEnd - lineStart) * this.value);

		graphics.fill(lineStart, lineY, knobX, lineY + 1, Colors.SLIDER_ACTIVE);
		graphics.fill(knobX, lineY, lineEnd, lineY + 1, Colors.SLIDER_TRACK);

		graphics.fill(lineStart - 1, lineY - 3, lineStart + 1, lineY + 4, Colors.SLIDER_TRACK);
		graphics.fill(lineEnd - 1, lineY - 3, lineEnd + 1, lineY + 4, Colors.SLIDER_TRACK);

		graphics.fill(knobX - 2, lineY - 2, knobX + 3, lineY + 3, Colors.SLIDER_KNOB);

		graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + width / 2, valueY, Colors.TEXT_PRIMARY);
	}
}
