package com.crussion.moissanite.ui.widget;

import com.crussion.moissanite.input.KeybindKeys;
import com.crussion.moissanite.ui.data.UiKeybind;
import com.crussion.moissanite.ui.render.UiShapes;
import com.crussion.moissanite.ui.render.UiTextRenderer;
import com.crussion.moissanite.ui.style.Colors;
import com.crussion.moissanite.ui.text.UiText;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class KeybindWidget extends AbstractWidget {
	private static final int UNBOUND = InputConstants.UNKNOWN.getValue();

	private final UiKeybind keybind;
	private final Font font;
	private boolean listening;

	public KeybindWidget(Font font, int x, int y, int width, int height, UiKeybind keybind) {
		super(x, y, width, height, Component.empty());
		this.font = font;
		this.keybind = keybind;
		updateMessage();
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean focused) {
		if (event.button() == 0) {
			listening = true;
			setFocused(true);
		} else if (event.button() == 1) {
			clearBinding();
			listening = false;
			setFocused(false);
		}
		updateMessage();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!visible || !active || !listening) {
			return false;
		}
		int keyCode = KeybindKeys.normalizeCapturedKey(event.key(), event.scancode());
		if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_BACKSPACE || keyCode == InputConstants.KEY_DELETE) {
			clearBinding();
		} else {
			keybind.set(keyCode);
		}
		listening = false;
		setFocused(false);
		updateMessage();
		return true;
	}

	public boolean isListening() {
		return listening;
	}

	public void clearBindingAndStopListening() {
		clearBinding();
		listening = false;
		setFocused(false);
		updateMessage();
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused && listening) {
			listening = false;
			updateMessage();
		}
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		updateMessage();
		int radius = Math.max(4, height / 3);
		int bg = listening ? Colors.BUTTON_HOVER : (isHoveredOrFocused() ? Colors.BUTTON_HOVER : Colors.BUTTON_BG);
		UiShapes.fillRoundedRect(graphics, getX(), getY(), width, height, radius, Colors.BUTTON_OUTLINE);
		UiShapes.fillRoundedRect(graphics, getX() + 1, getY() + 1, width - 2, height - 2, Math.max(3, radius - 1), bg);
		int textX = getX() + (width - font.width(getMessage())) / 2;
		int textY = getY() + (height - font.lineHeight) / 2;
		UiTextRenderer.drawBoldString(graphics, font, getMessage(), textX, textY, Colors.TEXT_PRIMARY, 1.02f);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narration) {
		defaultButtonNarrationText(narration);
	}

	private void clearBinding() {
		keybind.set(UNBOUND);
	}

	private void updateMessage() {
		if (listening) {
			setMessage(UiText.uiTextStatic("[PRESS KEY]"));
			return;
		}
		setMessage(UiText.uiTextStatic("[" + keyName() + "]"));
	}

	private String keyName() {
		Integer keyValue = keybind.get();
		if (keyValue == null || keyValue == UNBOUND) {
			return "NONE";
		}
		return KeybindKeys.displayName(keyValue);
	}
}
