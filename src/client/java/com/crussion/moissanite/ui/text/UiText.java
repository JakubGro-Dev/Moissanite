package com.crussion.moissanite.ui.text;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public final class UiText {
	public static final FontDescription.Resource UI_FONT =
			new FontDescription.Resource(Identifier.fromNamespaceAndPath("moissanite", "moissanite"));

	private UiText() {
	}

	public static Component uiText(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(UI_FONT));
	}

	public static Component uiTextStatic(String text) {
		return uiText(text);
	}

	public static void applyUiFont(EditBox box) {
		box.addFormatter((value, cursor) -> {
			if (value == null || value.isEmpty()) {
				return FormattedCharSequence.EMPTY;
			}
			return FormattedCharSequence.forward(value, Style.EMPTY.withFont(UI_FONT));
		});
	}
}
