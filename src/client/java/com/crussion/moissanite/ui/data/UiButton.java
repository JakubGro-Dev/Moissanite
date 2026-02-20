package com.crussion.moissanite.ui.data;

import java.util.function.Consumer;

public final class UiButton extends UiValue<Integer> {
	private final String buttonText;
	private final Consumer<UiButton> onPress;

	public UiButton(String name) {
		this(name, "Change", null);
	}

	public UiButton(String name, String buttonText) {
		this(name, buttonText, null);
	}

	public UiButton(String name, Consumer<UiButton> onPress) {
		this(name, "Change", onPress);
	}

	public UiButton(String name, String buttonText, Consumer<UiButton> onPress) {
		super(name, 0);
		this.buttonText = buttonText == null ? "" : buttonText;
		this.onPress = onPress;
	}

	@Override
	public UiEntryType type() {
		return UiEntryType.BUTTON;
	}

	public String buttonText() {
		return buttonText;
	}

	public void press() {
		set(get() + 1);
		if (onPress != null) {
			onPress.accept(this);
		}
	}
}

