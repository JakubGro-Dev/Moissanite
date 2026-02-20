package com.crussion.moissanite.ui.screen.overlay;

import com.crussion.moissanite.ui.data.UiButton;
import com.crussion.moissanite.ui.data.UiDropdown;
import com.crussion.moissanite.ui.data.UiEntry;
import com.crussion.moissanite.ui.data.UiInput;
import com.crussion.moissanite.ui.data.UiKeybind;
import com.crussion.moissanite.ui.data.UiNumber;
import com.crussion.moissanite.ui.data.UiSlider;
import com.crussion.moissanite.ui.data.UiSwitch;
import com.crussion.moissanite.ui.widget.DropdownWidget;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.text.UiText;
import com.crussion.moissanite.ui.widget.KeybindWidget;
import com.crussion.moissanite.ui.widget.RoundedActionButton;
import com.crussion.moissanite.ui.widget.RoundedEditBox;
import com.crussion.moissanite.ui.widget.SliderWidget;
import com.crussion.moissanite.ui.widget.SwitchWidget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class UiEntryWidgetFactory implements OverlayRightPanel.EntryWidgetFactory {
	private final Font font;

	public UiEntryWidgetFactory(Font font) {
		this.font = font;
	}

	@Override
	public AbstractWidget create(UiEntry<?> entry, int x, int y, int width, int height) {
		if (entry instanceof UiSwitch sw) {
			return new SwitchWidget(x, y, width, height, sw);
		}
		if (entry instanceof UiSlider slider) {
			return new SliderWidget(x, y, width, height, slider);
		}
		if (entry instanceof UiNumber number) {
			return createNumberInput(number, x, y, width, height);
		}
		if (entry instanceof UiDropdown dropdown) {
			return new DropdownWidget(this.font, x, y, width, height, dropdown);
		}
		if (entry instanceof UiInput inputEntry) {
			return createTextInput(inputEntry, x, y, width, height);
		}
		if (entry instanceof UiKeybind keybind) {
			return new KeybindWidget(this.font, x, y, width, height, keybind);
		}
		if (entry instanceof UiButton button) {
			return new RoundedActionButton(this.font, x, y, width, height, button);
		}
		return null;
	}

	private EditBox createNumberInput(UiNumber number, int x, int y, int width, int height) {
		EditBox box = new RoundedEditBox(this.font, x, y, width, height, Component.literal(number.name()), this.font.lineHeight);
		UiText.applyUiFont(box);
		box.setValue(Integer.toString(number.get()));
		boolean allowNegative = number.min() < 0;
		box.setFilter(value -> value.isEmpty() || value.matches(allowNegative ? "-?\\d+" : "\\d+"));
		box.setResponder(value -> {
			if (value == null || value.isBlank() || "-".equals(value)) {
				return;
			}
			try {
				number.set(Integer.parseInt(value));
			} catch (NumberFormatException ignored) {
			}
		});
		configureInput(box);
		return box;
	}

	private EditBox createTextInput(UiInput inputEntry, int x, int y, int width, int height) {
		EditBox box = new RoundedEditBox(this.font, x, y, width, height, Component.literal(inputEntry.name()), this.font.lineHeight);
		UiText.applyUiFont(box);
		box.setValue(inputEntry.get());
		box.setResponder(inputEntry::set);
		box.setMaxLength(120);
		configureInput(box);
		return box;
	}

	private static void configureInput(EditBox box) {
		box.setTextColor(Colors.TEXT_PRIMARY);
		box.setBordered(false);
		box.setCentered(true);
	}
}
