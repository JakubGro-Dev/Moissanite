package com.crussion.moissanite.ui.widget;

import java.util.Locale;

import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.text.UiText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class SliderWidget extends AbstractSliderButton {
	private static final int VALUE_EDITOR_MAX_LENGTH = 18;

	private final UiSlider slider;
	private final double min;
	private final double max;
	private final double step;
	private final int decimals;
	private final EditBox valueEditor;
	private boolean editingValue;

	public SliderWidget(int x, int y, int width, int height, UiSlider slider) {
		super(x, y, width, height, Component.empty(), toNormalized(slider.get(), slider.min(), slider.max()));
		this.slider = slider;
		this.min = slider.min();
		this.max = slider.max();
		this.step = slider.step();
		this.decimals = decimalsFromStep(this.step);
		this.valueEditor = createValueEditor(slider.name());
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		double value = slider.snap(fromNormalized(this.value, min, max));
		String text = formatValue(value);
		setMessage(UiText.uiTextStatic(text));
		if (!editingValue && !text.equals(valueEditor.getValue())) {
			valueEditor.setValue(text);
		}
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

	private EditBox createValueEditor(String name) {
		var font = Minecraft.getInstance().font;
		EditBox box = new RoundedEditBox(font, 0, 0, 1, 1, Component.literal(name), font.lineHeight);
		UiText.applyUiFont(box);
		box.setTextColor(Colors.TEXT_PRIMARY);
		box.setBordered(false);
		box.setCentered(false);
		box.setTextShadow(false);
		box.setMaxLength(VALUE_EDITOR_MAX_LENGTH);
		box.setFilter(this::isValidEditorValue);
		return box;
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
		syncValueEditorBounds(valueBoxX, valueBoxY, valueBoxWidth, valueBoxHeight);

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

		if (editingValue) {
			valueEditor.render(graphics, mouseX, mouseY, partialTick);
			return;
		}

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
		if (isInsideValueBox(event.x(), event.y())) {
			boolean wasEditing = editingValue;
			beginValueEdit(focused);
			if (wasEditing) {
				valueEditor.onClick(event, focused);
			}
			return;
		}
		commitValueEdit();
		setValueFromTrack(event.x());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		if (editingValue) {
			valueEditor.mouseDragged(event, dragX, dragY);
			return;
		}
		setValueFromTrack(event.x());
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		if (editingValue) {
			valueEditor.mouseReleased(event);
			return;
		}
		super.onRelease(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (editingValue) {
			if (event.isConfirmation()) {
				commitValueEdit();
				return true;
			}
			if (event.isEscape()) {
				cancelValueEdit();
				return true;
			}
			return valueEditor.keyPressed(event);
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return editingValue && valueEditor.charTyped(event);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			commitValueEdit();
		}
		valueEditor.setFocused(focused && editingValue);
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

	private void beginValueEdit(boolean focused) {
		if (!editingValue) {
			editingValue = true;
			String text = formatValue(slider.get());
			valueEditor.setValue(text);
			valueEditor.moveCursorToEnd(false);
			valueEditor.setHighlightPos(0);
		}
		if (focused) {
			setFocused(true);
		}
		valueEditor.setFocused(true);
	}

	private void commitValueEdit() {
		if (!editingValue) {
			return;
		}
		String text = valueEditor.getValue().trim();
		editingValue = false;
		valueEditor.setFocused(false);
		if (!text.isEmpty() && !"-".equals(text) && !".".equals(text) && !"-.".equals(text)) {
			try {
				double parsed = Double.parseDouble(text);
				if (Double.isFinite(parsed)) {
					slider.set(parsed);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		this.value = toNormalized(slider.get(), min, max);
		updateMessage();
	}

	private void cancelValueEdit() {
		if (!editingValue) {
			return;
		}
		editingValue = false;
		valueEditor.setFocused(false);
		valueEditor.setValue(formatValue(slider.get()));
	}

	private void syncValueEditorBounds(int x, int y, int width, int height) {
		valueEditor.setRectangle(width, height, x, y);
	}

	private boolean isInsideValueBox(double mouseX, double mouseY) {
		int valueBoxWidth = valueBoxWidth(this.width);
		int valueBoxX = valueBoxX(getX(), this.width, valueBoxWidth);
		int valueBoxHeight = Math.min(20, this.height);
		int valueBoxY = getY() + (this.height - valueBoxHeight) / 2;
		return mouseX >= valueBoxX && mouseX < valueBoxX + valueBoxWidth
				&& mouseY >= valueBoxY && mouseY < valueBoxY + valueBoxHeight;
	}

	private boolean isValidEditorValue(String value) {
		if (value == null || value.length() > VALUE_EDITOR_MAX_LENGTH) {
			return false;
		}
		return value.isEmpty() || value.matches("-?(\\d+)?(\\.\\d*)?");
	}

	private String formatValue(double value) {
		return String.format(Locale.ROOT, "%." + decimals + "f", value);
	}

	private static int valueBoxWidth(int width) {
		return Math.min(76, Math.max(54, width / 3));
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
